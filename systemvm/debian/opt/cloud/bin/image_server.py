#!/usr/bin/env python3
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
POC "imageio-like" HTTP server backed by NBD over TCP.

How to run
----------
- Install dependency:
  dnf install python3-libnbd
  or
  apt install python3-libnbd

- Run server:
  python image_server.py --listen 0.0.0.0 --port 54323

Example curl commands
--------------------
- OPTIONS:
  curl -i -X OPTIONS http://127.0.0.1:54323/images/demo

- GET full image:
  curl -v http://127.0.0.1:54323/images/demo -o demo.img

- GET a byte range:
  curl -v -H "Range: bytes=0-1048575" http://127.0.0.1:54323/images/demo -o first_1MiB.bin

- PUT full image (Content-Length must equal export size exactly):
  curl -v -T demo.img http://127.0.0.1:54323/images/demo

- GET extents (POC-level; may return a single allocated extent):
  curl -s http://127.0.0.1:54323/images/demo/extents | jq .

- POST flush:
  curl -s -X POST http://127.0.0.1:54323/images/demo/flush | jq .
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import socket
import threading
import time
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict, Optional, Tuple
import nbd

CHUNK_SIZE = 256 * 1024  # 256 KiB

# Concurrency limits across ALL images.
MAX_PARALLEL_READS = 8
MAX_PARALLEL_WRITES = 1

_READ_SEM = threading.Semaphore(MAX_PARALLEL_READS)
_WRITE_SEM = threading.Semaphore(MAX_PARALLEL_WRITES)

# In-memory per-image lock: single lock gates both read and write.
_IMAGE_LOCKS: Dict[str, threading.Lock] = {}
_IMAGE_LOCKS_GUARD = threading.Lock()


# Dynamic image_id(transferId) -> NBD export mapping:
# CloudStack writes a JSON file at /tmp/imagetransfer/<transferId> with:
#   {"host": "...", "port": 10809, "export": "vda"}
#
# This server reads that file on-demand.
_CFG_DIR = "/tmp/imagetransfer"
_CFG_CACHE: Dict[str, Tuple[float, Dict[str, Any]]] = {}
_CFG_CACHE_GUARD = threading.Lock()


def _json_bytes(obj: Any) -> bytes:
    return json.dumps(obj, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def _get_image_lock(image_id: str) -> threading.Lock:
    with _IMAGE_LOCKS_GUARD:
        lock = _IMAGE_LOCKS.get(image_id)
        if lock is None:
            lock = threading.Lock()
            _IMAGE_LOCKS[image_id] = lock
        return lock


def _now_s() -> float:
    return time.monotonic()


def _safe_transfer_id(image_id: str) -> Optional[str]:
    """
    Only allow a single filename component to avoid path traversal.
    We intentionally keep validation simple: reject anything containing '/' or '\\'.
    """
    if not image_id:
        return None
    if image_id != os.path.basename(image_id):
        return None
    if "/" in image_id or "\\" in image_id:
        return None
    if image_id in (".", ".."):
        return None
    return image_id


def _load_image_cfg(image_id: str) -> Optional[Dict[str, Any]]:
    safe_id = _safe_transfer_id(image_id)
    if safe_id is None:
        return None

    cfg_path = os.path.join(_CFG_DIR, safe_id)
    try:
        st = os.stat(cfg_path)
    except FileNotFoundError:
        return None
    except OSError as e:
        logging.warning("cfg stat failed image_id=%s err=%r", image_id, e)
        return None

    with _CFG_CACHE_GUARD:
        cached = _CFG_CACHE.get(safe_id)
        if cached is not None:
            cached_mtime, cached_cfg = cached
            # Use cached config if the file hasn't changed.
            if float(st.st_mtime) == float(cached_mtime):
                return cached_cfg

    try:
        with open(cfg_path, "rb") as f:
            raw = f.read(4096)
    except OSError as e:
        logging.warning("cfg read failed image_id=%s err=%r", image_id, e)
        return None

    try:
        obj = json.loads(raw.decode("utf-8"))
    except Exception as e:
        logging.warning("cfg parse failed image_id=%s err=%r", image_id, e)
        return None

    if not isinstance(obj, dict):
        logging.warning("cfg invalid type image_id=%s type=%s", image_id, type(obj).__name__)
        return None

    host = obj.get("host")
    port = obj.get("port")
    export = obj.get("export")
    if not isinstance(host, str) or not host:
        logging.warning("cfg missing/invalid host image_id=%s", image_id)
        return None
    try:
        port_i = int(port)
    except Exception:
        logging.warning("cfg missing/invalid port image_id=%s", image_id)
        return None
    if port_i <= 0 or port_i > 65535:
        logging.warning("cfg out-of-range port image_id=%s port=%r", image_id, port)
        return None
    if export is not None and (not isinstance(export, str) or not export):
        logging.warning("cfg missing/invalid export image_id=%s", image_id)
        return None

    cfg: Dict[str, Any] = {"host": host, "port": port_i, "export": export}

    with _CFG_CACHE_GUARD:
        _CFG_CACHE[safe_id] = (float(st.st_mtime), cfg)
    return cfg


class _NbdConn:
    """
    Small helper to connect to NBD using an already-open TCP socket.
    Opens a fresh handle per request, per POC requirements.
    """

    def __init__(self, host: str, port: int, export: Optional[str]):
        self._sock = socket.create_connection((host, port))
        self._nbd = nbd.NBD()

        # Select export name if supported/needed.
        if export and hasattr(self._nbd, "set_export_name"):
            self._nbd.set_export_name(export)

        self._connect_existing_socket(self._sock)

    def _connect_existing_socket(self, sock: socket.socket) -> None:
        # Requirement: attach libnbd to an existing socket / FD (no qemu-nbd).
        # libnbd python API varies slightly by version, so try common options.
        last_err: Optional[BaseException] = None
        if hasattr(self._nbd, "connect_socket"):
            try:
                self._nbd.connect_socket(sock)
                return
            except Exception as e:  # pragma: no cover (depends on binding)
                last_err = e
                try:
                    self._nbd.connect_socket(sock.fileno())
                    return
                except Exception as e2:  # pragma: no cover
                    last_err = e2
        if hasattr(self._nbd, "connect_fd"):
            try:
                self._nbd.connect_fd(sock.fileno())
                return
            except Exception as e:  # pragma: no cover
                last_err = e
        raise RuntimeError(
            "Unable to connect libnbd using existing socket/fd; "
            f"binding missing connect_socket/connect_fd or call failed: {last_err!r}"
        )

    def size(self) -> int:
        return int(self._nbd.get_size())

    def pread(self, length: int, offset: int) -> bytes:
        # Expected signature: pread(length, offset)
        try:
            return self._nbd.pread(length, offset)
        except TypeError:  # pragma: no cover (binding differences)
            return self._nbd.pread(offset, length)

    def pwrite(self, buf: bytes, offset: int) -> None:
        # Expected signature: pwrite(buf, offset)
        try:
            self._nbd.pwrite(buf, offset)
        except TypeError:  # pragma: no cover (binding differences)
            self._nbd.pwrite(offset, buf)

    def flush(self) -> None:
        if hasattr(self._nbd, "flush"):
            self._nbd.flush()
            return
        if hasattr(self._nbd, "fsync"):
            self._nbd.fsync()
            return
        raise RuntimeError("libnbd binding has no flush/fsync method")

    def close(self) -> None:
        # Best-effort; bindings may differ.
        try:
            if hasattr(self._nbd, "shutdown"):
                self._nbd.shutdown()
        except Exception:
            pass
        try:
            if hasattr(self._nbd, "close"):
                self._nbd.close()
        except Exception:
            pass
        try:
            self._sock.close()
        except Exception:
            pass

    def __enter__(self) -> "_NbdConn":
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.close()


class Handler(BaseHTTPRequestHandler):
    server_version = "imageio-poc/0.1"

    # Keep BaseHTTPRequestHandler from printing noisy default logs
    def log_message(self, fmt: str, *args: Any) -> None:
        logging.info("%s - - %s", self.address_string(), fmt % args)

    def _send_imageio_headers(self) -> None:
        # Include these headers for compatibility with the imageio contract.
        self.send_header("Access-Control-Allow-Methods", "GET, PUT, OPTIONS")
        self.send_header("Accept-Ranges", "bytes")

    def _send_json(self, status: int, obj: Any) -> None:
        body = _json_bytes(obj)
        self.send_response(status)
        self._send_imageio_headers()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except BrokenPipeError:
            pass

    def _send_error_json(self, status: int, message: str) -> None:
        self._send_json(status, {"error": message})

    def _send_range_not_satisfiable(self, size: int) -> None:
        # RFC 7233: reply with Content-Range: bytes */<size>
        self.send_response(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
        self._send_imageio_headers()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Range", f"bytes */{size}")
        body = _json_bytes({"error": "range not satisfiable"})
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except BrokenPipeError:
            pass

    def _parse_single_range(self, range_header: str, size: int) -> Tuple[int, int]:
        """
        Parse a single HTTP byte range (RFC 7233) and return (start, end_inclusive).

        Supported:
        - Range: bytes=START-END
        - Range: bytes=START-
        - Range: bytes=-SUFFIX

        Raises ValueError for invalid headers. Caller handles 416 vs 400.
        """
        if size < 0:
            raise ValueError("invalid size")
        if not range_header:
            raise ValueError("empty Range")
        if "," in range_header:
            raise ValueError("multiple ranges not supported")

        prefix = "bytes="
        if not range_header.startswith(prefix):
            raise ValueError("only bytes ranges supported")
        spec = range_header[len(prefix) :].strip()
        if "-" not in spec:
            raise ValueError("invalid bytes range")

        left, right = spec.split("-", 1)
        left = left.strip()
        right = right.strip()

        if left == "":
            # Suffix range: last N bytes.
            if right == "":
                raise ValueError("invalid suffix range")
            try:
                suffix_len = int(right, 10)
            except ValueError as e:
                raise ValueError("invalid suffix length") from e
            if suffix_len <= 0:
                raise ValueError("invalid suffix length")
            if size == 0:
                # Nothing to serve
                raise ValueError("unsatisfiable")
            if suffix_len >= size:
                return 0, size - 1
            return size - suffix_len, size - 1

        # START is present
        try:
            start = int(left, 10)
        except ValueError as e:
            raise ValueError("invalid range start") from e
        if start < 0:
            raise ValueError("invalid range start")
        if start >= size:
            raise ValueError("unsatisfiable")

        if right == "":
            # START-
            return start, size - 1

        try:
            end = int(right, 10)
        except ValueError as e:
            raise ValueError("invalid range end") from e
        if end < start:
            raise ValueError("unsatisfiable")
        if end >= size:
            end = size - 1
        return start, end

    def _parse_route(self) -> Tuple[Optional[str], Optional[str]]:
        # Returns (image_id, tail) where tail is:
        #   None => /images/{id}
        #   "extents" => /images/{id}/extents
        #   "flush" => /images/{id}/flush
        path = self.path.split("?", 1)[0]
        parts = [p for p in path.split("/") if p]
        if len(parts) < 2 or parts[0] != "images":
            return None, None
        image_id = parts[1]
        tail = parts[2] if len(parts) >= 3 else None
        if len(parts) > 3:
            return None, None
        return image_id, tail

    def _image_cfg(self, image_id: str) -> Optional[Dict[str, Any]]:
        return _load_image_cfg(image_id)

    def do_OPTIONS(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return
        if self._image_cfg(image_id) is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return
        # todo: get capabilities from backend later. this is just for upload to work
        features = ["extents", "zero", "flush"]
        response = {
            "unix_socket": None,  # Not used in this implementation
            "features": features,
            "max_readers": MAX_PARALLEL_READS,
            "max_writers": MAX_PARALLEL_WRITES,
        }
        self._send_json(HTTPStatus.OK, response)

    def do_GET(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        if tail == "extents":
            self._handle_get_extents(image_id, cfg)
            return
        if tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        range_header = self.headers.get("Range")
        self._handle_get_image(image_id, cfg, range_header)

    def do_PUT(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        if self.headers.get("Range") is not None or self.headers.get("Content-Range") is not None:
            self._send_error_json(
                HTTPStatus.BAD_REQUEST, "Range/Content-Range not supported; full writes only"
            )
            return

        content_length_hdr = self.headers.get("Content-Length")
        if content_length_hdr is None:
            self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length required")
            return
        try:
            content_length = int(content_length_hdr)
        except ValueError:
            self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
            return
        if content_length < 0:
            self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
            return

        self._handle_put_image(image_id, cfg, content_length)

    def do_POST(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        if tail == "flush":
            self._handle_post_flush(image_id, cfg)
            return
        self._send_error_json(HTTPStatus.NOT_FOUND, "not found")

    def _handle_get_image(
            self, image_id: str, cfg: Dict[str, Any], range_header: Optional[str]
    ) -> None:
        lock = _get_image_lock(image_id)
        if not lock.acquire(blocking=False):
            self._send_error_json(HTTPStatus.CONFLICT, "image busy")
            return

        if not _READ_SEM.acquire(blocking=False):
            lock.release()
            self._send_error_json(HTTPStatus.SERVICE_UNAVAILABLE, "too many parallel reads")
            return

        start = _now_s()
        bytes_sent = 0
        try:
            logging.info("GET start image_id=%s range=%s", image_id, range_header or "-")
            with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                size = conn.size()

                start_off = 0
                end_off_incl = size - 1 if size > 0 else -1
                status = HTTPStatus.OK
                content_length = size
                if range_header is not None:
                    try:
                        start_off, end_off_incl = self._parse_single_range(range_header, size)
                    except ValueError as e:
                        if str(e) == "unsatisfiable":
                            self._send_range_not_satisfiable(size)
                            return
                        if "unsatisfiable" in str(e):
                            self._send_range_not_satisfiable(size)
                            return
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "invalid Range header")
                        return
                    status = HTTPStatus.PARTIAL_CONTENT
                    content_length = (end_off_incl - start_off) + 1

                self.send_response(status)
                self._send_imageio_headers()
                self.send_header("Content-Type", "application/octet-stream")
                self.send_header("Content-Length", str(content_length))
                if status == HTTPStatus.PARTIAL_CONTENT:
                    self.send_header("Content-Range", f"bytes {start_off}-{end_off_incl}/{size}")
                self.end_headers()

                offset = start_off
                end_excl = end_off_incl + 1
                while offset < end_excl:
                    to_read = min(CHUNK_SIZE, end_excl - offset)
                    data = conn.pread(to_read, offset)
                    if not data:
                        raise RuntimeError("backend returned empty read")
                    try:
                        self.wfile.write(data)
                    except BrokenPipeError:
                        logging.info("GET client disconnected image_id=%s at=%d", image_id, offset)
                        break
                    offset += len(data)
                    bytes_sent += len(data)
        except Exception as e:
            # If headers already sent, we can't return JSON reliably; just log.
            logging.warning("GET error image_id=%s err=%r", image_id, e)
            try:
                if not self.wfile.closed:
                    self.close_connection = True
            except Exception:
                pass
        finally:
            _READ_SEM.release()
            lock.release()
            dur = _now_s() - start
            logging.info(
                "GET end image_id=%s bytes=%d duration_s=%.3f", image_id, bytes_sent, dur
            )

    def _handle_put_image(self, image_id: str, cfg: Dict[str, Any], content_length: int) -> None:
        lock = _get_image_lock(image_id)
        if not lock.acquire(blocking=False):
            self._send_error_json(HTTPStatus.CONFLICT, "image busy")
            return

        if not _WRITE_SEM.acquire(blocking=False):
            lock.release()
            self._send_error_json(HTTPStatus.SERVICE_UNAVAILABLE, "too many parallel writes")
            return

        start = _now_s()
        bytes_written = 0
        try:
            logging.info("PUT start image_id=%s content_length=%d", image_id, content_length)
            with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                offset = 0
                remaining = content_length
                while remaining > 0:
                    chunk = self.rfile.read(min(CHUNK_SIZE, remaining))
                    if not chunk:
                        self._send_error_json(
                            HTTPStatus.BAD_REQUEST,
                            f"request body ended early at {offset} bytes",
                        )
                        return
                    conn.pwrite(chunk, offset)
                    offset += len(chunk)
                    remaining -= len(chunk)
                    bytes_written += len(chunk)

                # POC-level: do not auto-flush on PUT; expose explicit /flush endpoint.
                self._send_json(HTTPStatus.OK, {"ok": True, "bytes_written": bytes_written})
        except Exception as e:
            logging.warning("PUT error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            _WRITE_SEM.release()
            lock.release()
            dur = _now_s() - start
            logging.info(
                "PUT end image_id=%s bytes=%d duration_s=%.3f", image_id, bytes_written, dur
            )

    def _handle_get_extents(self, image_id: str, cfg: Dict[str, Any]) -> None:
        # Keep deterministic and simple (POC): report entire image allocated.
        # No per-image lock required by spec, but we still take it to avoid racing
        # with a write and to keep behavior consistent.
        lock = _get_image_lock(image_id)
        if not lock.acquire(blocking=False):
            self._send_error_json(HTTPStatus.CONFLICT, "image busy")
            return

        start = _now_s()
        try:
            logging.info("EXTENTS start image_id=%s", image_id)
            with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                size = conn.size()
            self._send_json(
                HTTPStatus.OK,
                [{"start": 0, "length": size, "allocated": True}],
            )
        except Exception as e:
            logging.warning("EXTENTS error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            lock.release()
            dur = _now_s() - start
            logging.info("EXTENTS end image_id=%s duration_s=%.3f", image_id, dur)

    def _handle_post_flush(self, image_id: str, cfg: Dict[str, Any]) -> None:
        lock = _get_image_lock(image_id)
        if not lock.acquire(blocking=False):
            self._send_error_json(HTTPStatus.CONFLICT, "image busy")
            return

        start = _now_s()
        try:
            logging.info("FLUSH start image_id=%s", image_id)
            with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                conn.flush()
            self._send_json(HTTPStatus.OK, {"ok": True})
        except Exception as e:
            logging.warning("FLUSH error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            lock.release()
            dur = _now_s() - start
            logging.info("FLUSH end image_id=%s duration_s=%.3f", image_id, dur)


def main() -> None:
    parser = argparse.ArgumentParser(description="POC imageio-like HTTP server backed by NBD")
    parser.add_argument("--listen", default="127.0.0.1", help="Address to bind")
    parser.add_argument("--port", type=int, default=54323, help="Port to listen on")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )

    addr = (args.listen, args.port)
    httpd = ThreadingHTTPServer(addr, Handler)
    logging.info("listening on http://%s:%d", args.listen, args.port)
    logging.info("image configs are read from %s/<transferId>", _CFG_DIR)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
