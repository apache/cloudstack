# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""
Shared infrastructure for the image-server test suite (stdlib unittest only).

Provides:
- A singleton image server process started once for the entire test run.
- Server stdout/stderr appended to ``<tmp>/imageserver.log``.
- On shutdown: stop the child process, close the log handle, unlink the control socket;
  the temp directory and ``imageserver.log`` are left on disk.
- Control-socket helpers using pure-Python AF_UNIX.
- qemu-nbd server management.
- Transfer registration / teardown helpers.
- HTTP helper functions.
"""

import atexit
import functools
import json
import logging
import os
import random
import signal
import socket
import subprocess
import sys
import tempfile
import time
import unittest
import uuid
from pathlib import Path
from typing import Any, Dict, Optional, TextIO

IMAGE_SIZE = 1 * 1024 * 1024  # 1 MiB
SERVER_STARTUP_TIMEOUT = 10
QEMU_NBD_STARTUP_TIMEOUT = 5
HTTP_TIMEOUT = 30  # seconds per HTTP request

logging.basicConfig(
    level=logging.INFO,
    stream=sys.stderr,
    format="%(asctime)s [TEST] %(message)s",
)
log = logging.getLogger(__name__)


def randbytes(seed, n):
    """Generate n deterministic pseudo-random bytes (works on Python 3.6+)."""
    rng = random.Random(seed)
    return rng.getrandbits(8 * n).to_bytes(n, "big")


def test_timeout(seconds):
    """Decorator that fails a test if it exceeds *seconds* (SIGALRM, Unix only)."""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            def _alarm(signum, frame):
                raise TimeoutError(
                    "{} timed out after {}s".format(func.__qualname__, seconds)
                )
            prev = signal.signal(signal.SIGALRM, _alarm)
            signal.alarm(seconds)
            try:
                return func(*args, **kwargs)
            finally:
                signal.alarm(0)
                signal.signal(signal.SIGALRM, prev)
        return wrapper
    return decorator

# ── Singleton state shared across all test modules ──────────────────────

_tmp_dir: Optional[str] = None
_server_proc: Optional[subprocess.Popen] = None
_server_info: Optional[Dict[str, Any]] = None
_server_log_fp: Optional[TextIO] = None
_server_log_path: Optional[str] = None
_atexit_registered: bool = False


def _free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def control_socket_send(sock_path: str, message: dict, retries: int = 5) -> dict:
    """Send a JSON message to the control socket and return the parsed response."""
    payload = (json.dumps(message) + "\n").encode("utf-8")
    last_err = None
    for attempt in range(retries):
        try:
            with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as s:
                s.settimeout(5)
                s.connect(sock_path)
                s.sendall(payload)
                s.shutdown(socket.SHUT_WR)
                data = b""
                while True:
                    chunk = s.recv(4096)
                    if not chunk:
                        break
                    data += chunk
            return json.loads(data.strip())
        except (BlockingIOError, ConnectionRefusedError, OSError) as e:
            last_err = e
            time.sleep(0.1 * (attempt + 1))
    raise last_err


def _wait_for_control_socket(sock_path: str, timeout: float = SERVER_STARTUP_TIMEOUT) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            resp = control_socket_send(sock_path, {"action": "status"})
            if resp.get("status") == "ok":
                return
        except (ConnectionRefusedError, FileNotFoundError, OSError):
            pass
        time.sleep(0.2)
    raise RuntimeError(
        f"Image server control socket at {sock_path} not ready within {timeout}s"
    )


def _wait_for_nbd_socket(sock_path: str, timeout: float = QEMU_NBD_STARTUP_TIMEOUT) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if os.path.exists(sock_path):
            try:
                with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as s:
                    s.settimeout(1)
                    s.connect(sock_path)
                    return
            except (ConnectionRefusedError, OSError):
                pass
        time.sleep(0.2)
    raise RuntimeError(
        f"qemu-nbd socket at {sock_path} not ready within {timeout}s"
    )


def get_tmp_dir() -> str:
    global _tmp_dir
    if _tmp_dir is None:
        _tmp_dir = tempfile.mkdtemp(prefix="imageserver_test_")
    return _tmp_dir


def _read_log_tail(path: str, max_bytes: int = 65536) -> str:
    """Return up to *max_bytes* of UTF-8 text from the end of *path*."""
    try:
        with open(path, "rb") as f:
            f.seek(0, os.SEEK_END)
            size = f.tell()
            f.seek(max(0, size - max_bytes))
            return f.read().decode("utf-8", errors="replace")
    except OSError as e:
        return f"(could not read log: {e})"


def get_image_server() -> Dict[str, Any]:
    """Return the singleton image-server info dict, starting it if needed."""
    global _server_proc, _server_info, _server_log_fp, _server_log_path, _atexit_registered

    if _server_info is not None:
        return _server_info

    tmp = get_tmp_dir()
    port = _free_port()
    ctrl_sock = os.path.join(tmp, "ctrl.sock")
    log_path = os.path.join(tmp, "imageserver.log")
    _server_log_path = log_path

    imageserver_pkg = str(Path(__file__).resolve().parent.parent)
    parent_dir = str(Path(imageserver_pkg).parent)

    env = os.environ.copy()
    env["PYTHONPATH"] = parent_dir + os.pathsep + env.get("PYTHONPATH", "")

    _server_log_fp = open(
        log_path, "a", encoding="utf-8", buffering=1, errors="replace"
    )
    try:
        _server_log_fp.write(
            "\n========== imageserver test subprocess log ==========\n"
        )
        _server_log_fp.flush()
    except OSError:
        pass

    proc = subprocess.Popen(
        [
            sys.executable, "-m", "imageserver",
            "--listen", "127.0.0.1",
            "--port", str(port),
            "--control-socket", ctrl_sock,
        ],
        cwd=parent_dir,
        env=env,
        stdout=_server_log_fp,
        stderr=_server_log_fp,
    )
    _server_proc = proc

    try:
        _wait_for_control_socket(ctrl_sock)
    except RuntimeError:
        proc.kill()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait(timeout=5)
        try:
            _server_log_fp.flush()
        except OSError:
            pass
        tail = _read_log_tail(log_path)
        try:
            _server_log_fp.close()
        except OSError:
            pass
        _server_log_fp = None
        _server_proc = None
        raise RuntimeError(
            "Image server failed to start.\n"
            f"Log file: {log_path}\n"
            f"--- log tail ---\n{tail}"
        )

    def send(msg: dict) -> dict:
        return control_socket_send(ctrl_sock, msg)

    _server_info = {
        "base_url": f"http://127.0.0.1:{port}",
        "port": port,
        "ctrl_sock": ctrl_sock,
        "send": send,
        "imageserver_log": log_path,
    }
    if not _atexit_registered:
        atexit.register(shutdown_image_server)
        _atexit_registered = True
    sys.stdout.write(
        "\n[IMAGESERVER_TEST] child image server log file: %s\n\n" % log_path
    )
    sys.stdout.flush()
    return _server_info


def shutdown_image_server() -> None:
    global _server_proc, _server_info, _tmp_dir, _server_log_fp, _server_log_path
    ctrl_sock: Optional[str] = None
    if _server_info is not None:
        ctrl_sock = _server_info.get("ctrl_sock")

    if _server_proc is not None:
        _server_proc.terminate()
        try:
            _server_proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            _server_proc.kill()
            _server_proc.wait(timeout=5)
        _server_proc = None
    if _server_log_fp is not None:
        try:
            _server_log_fp.flush()
            _server_log_fp.close()
        except OSError:
            pass
        _server_log_fp = None
    _server_info = None
    _server_log_path = None

    if ctrl_sock:
        try:
            os.unlink(ctrl_sock)
        except FileNotFoundError:
            pass

    # Leave temp dir and imageserver.log on disk for debugging; clear pointer only.
    _tmp_dir = None


# ── qemu-nbd server ────────────────────────────────────────────────────

class QemuNbdServer:
    """Manages a qemu-nbd process exporting a disk image over a Unix socket."""

    def __init__(
        self,
        image_path: str,
        socket_path: str,
        image_size: int = IMAGE_SIZE,
        image_format: str = "raw",
    ):
        self.image_path = image_path
        self.socket_path = socket_path
        self.image_size = image_size
        self.image_format = image_format
        self._proc: Optional[subprocess.Popen] = None

    def start(self) -> None:
        if not os.path.exists(self.image_path):
            if self.image_format == "raw":
                with open(self.image_path, "wb") as f:
                    f.truncate(self.image_size)
            else:
                raise FileNotFoundError(
                    f"disk image not found for format {self.image_format!r}: {self.image_path}"
                )

        self._proc = subprocess.Popen(
            [
                "qemu-nbd",
                "--socket", self.socket_path,
                "--format", self.image_format,
                "--persistent",
                "--shared=0",
                "--cache=none",
                self.image_path,
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        _wait_for_nbd_socket(self.socket_path)

    def stop(self) -> None:
        if self._proc is not None:
            for pipe in (self._proc.stdout, self._proc.stderr):
                if pipe:
                    try:
                        pipe.close()
                    except Exception:
                        pass
            self._proc.terminate()
            try:
                self._proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._proc.kill()
                self._proc.wait(timeout=5)
            self._proc = None


# ── Factory helpers ─────────────────────────────────────────────────────

def make_tmp_image(data=None, image_size=IMAGE_SIZE) -> str:
    """Create a temp raw image file in the shared tmp dir; return path."""
    tmp = get_tmp_dir()
    path = os.path.join(tmp, f"img_{uuid.uuid4().hex[:8]}.raw")
    if data is not None:
        with open(path, "wb") as f:
            f.write(data)
    else:
        with open(path, "wb") as f:
            f.write(randbytes(42, image_size))
    return path


def make_file_transfer(data=None, image_size=IMAGE_SIZE, idle_timeout_seconds=None):
    """
    Create a temp file + register a file-backend transfer.
    Returns (transfer_id, url, file_path, cleanup_callable).

    If *idle_timeout_seconds* is set, it is sent in the transfer config (for idle expiry tests).
    """
    srv = get_image_server()
    path = make_tmp_image(data=data, image_size=image_size)
    transfer_id = f"file-{uuid.uuid4().hex[:8]}"
    cfg = {"backend": "file", "file": path}
    if idle_timeout_seconds is not None:
        cfg["idle_timeout_seconds"] = idle_timeout_seconds
    resp = srv["send"]({
        "action": "register",
        "transfer_id": transfer_id,
        "config": cfg,
    })
    assert resp["status"] == "ok", f"register failed: {resp}"
    url = f"{srv['base_url']}/images/{transfer_id}"

    def cleanup():
        srv["send"]({"action": "unregister", "transfer_id": transfer_id})
        try:
            os.unlink(path)
        except FileNotFoundError:
            pass

    return transfer_id, url, path, cleanup


def make_nbd_transfer(image_size=IMAGE_SIZE):
    """
    Create a qemu-nbd server + register an NBD-backend transfer.
    Returns (transfer_id, url, QemuNbdServer, cleanup_callable).
    """
    srv = get_image_server()
    tmp = get_tmp_dir()
    img_path = os.path.join(tmp, f"nbd_{uuid.uuid4().hex[:8]}.raw")
    sock_path = os.path.join(tmp, f"nbd_{uuid.uuid4().hex[:8]}.sock")

    server = QemuNbdServer(img_path, sock_path, image_size=image_size)
    server.start()

    transfer_id = f"nbd-{uuid.uuid4().hex[:8]}"
    resp = srv["send"]({
        "action": "register",
        "transfer_id": transfer_id,
        "config": {"backend": "nbd", "socket": sock_path},
    })
    assert resp["status"] == "ok", f"register failed: {resp}"
    url = f"{srv['base_url']}/images/{transfer_id}"

    def cleanup():
        srv["send"]({"action": "unregister", "transfer_id": transfer_id})
        server.stop()
        for p in (img_path, sock_path):
            try:
                os.unlink(p)
            except FileNotFoundError:
                pass

    return transfer_id, url, server, cleanup


def make_nbd_transfer_existing_disk(image_path: str, image_format: str = "qcow2"):
    """
    Start qemu-nbd for an existing on-disk image (e.g. qcow2) and register a transfer.

    Does not delete *image_path* on cleanup (only the Unix socket under tmp).

    Returns (transfer_id, url, QemuNbdServer, cleanup_callable).
    """
    srv = get_image_server()
    tmp = get_tmp_dir()
    sock_path = os.path.join(tmp, f"nbd_{uuid.uuid4().hex[:8]}.sock")

    server = QemuNbdServer(
        image_path, sock_path, image_format=image_format
    )
    server.start()

    transfer_id = f"nbd-{uuid.uuid4().hex[:8]}"
    resp = srv["send"]({
        "action": "register",
        "transfer_id": transfer_id,
        "config": {"backend": "nbd", "socket": sock_path},
    })
    assert resp["status"] == "ok", f"register failed: {resp}"
    url = f"{srv['base_url']}/images/{transfer_id}"

    def cleanup():
        srv["send"]({"action": "unregister", "transfer_id": transfer_id})
        server.stop()
        try:
            os.unlink(sock_path)
        except FileNotFoundError:
            pass

    return transfer_id, url, server, cleanup


# ── HTTP helpers ────────────────────────────────────────────────────────

import urllib.request
import urllib.error


def http_get(url, headers=None, timeout=HTTP_TIMEOUT):
    req = urllib.request.Request(url, headers=headers or {})
    return urllib.request.urlopen(req, timeout=timeout)


def http_put(url, data, headers=None, timeout=HTTP_TIMEOUT):
    hdrs = {"Content-Length": str(len(data))}
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(url, data=data, headers=hdrs, method="PUT")
    return urllib.request.urlopen(req, timeout=timeout)


def http_post(url, data=b"", headers=None, timeout=HTTP_TIMEOUT):
    hdrs = {}
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(url, data=data, headers=hdrs, method="POST")
    return urllib.request.urlopen(req, timeout=timeout)


def http_options(url, timeout=HTTP_TIMEOUT):
    req = urllib.request.Request(url, method="OPTIONS")
    return urllib.request.urlopen(req, timeout=timeout)


def http_patch(url, data, headers=None, timeout=HTTP_TIMEOUT):
    hdrs = {}
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(url, data=data, headers=hdrs, method="PATCH")
    return urllib.request.urlopen(req, timeout=timeout)


# ── Base TestCase with shared setUp/tearDown ────────────────────────────

class ImageServerTestCase(unittest.TestCase):
    """
    Base class for image-server tests.

    Ensures the image server is running before any test method.
    Subclasses that need a file or NBD transfer should set them up
    in setUp() and tear down in tearDown().
    """

    @classmethod
    def setUpClass(cls):
        cls.server = get_image_server()
        cls.base_url = cls.server["base_url"]

    def ctrl(self, msg):
        """Send a control-socket message; wraps server['send'] to avoid descriptor issues."""
        return self.server["send"](msg)

    def _make_tmp_image(self, data=None):
        return make_tmp_image(data=data)

    def _register_file_transfer(self, data=None):
        return make_file_transfer(data=data)

    def _register_nbd_transfer(self):
        return make_nbd_transfer()

    @staticmethod
    def dump_server_logs(max_bytes: int = 256 * 1024):
        """Print a tail of the image-server log file (shared by all tests in the run)."""
        path = _server_log_path
        if not path or not os.path.isfile(path):
            return
        try:
            if _server_log_fp is not None:
                _server_log_fp.flush()
        except OSError:
            pass
        try:
            data = _read_log_tail(path, max_bytes=max_bytes)
            if data.strip():
                sys.stderr.write("\n=== IMAGE SERVER LOG (tail) ===\n")
                sys.stderr.write(data)
                if not data.endswith("\n"):
                    sys.stderr.write("\n")
                sys.stderr.write("=== END IMAGE SERVER LOG ===\n")
        except Exception:
            pass
