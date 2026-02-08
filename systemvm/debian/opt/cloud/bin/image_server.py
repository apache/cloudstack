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
POC "imageio-like" HTTP server backed by NBD over TCP or a local file.

Supports two backends (see config payload):
- nbd: proxy to an NBD server (port, export, export_bitmap); supports range reads/writes, extents, zero, flush.
- file: read/write a local qcow2 (or raw) file path; full PUT only (no range writes), GET with optional ranges, flush.

How to run
----------
- Install dependency:
  dnf install python3-libnbd
  or
  apt install python3-libnbd

- Run server:
  createImageTransfer will start the server as a systemd service 'cloudstack-image-server'

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

- GET extents (zero/hole extents from NBD base:allocation):
  curl -s http://127.0.0.1:54323/images/demo/extents | jq .

- GET extents with dirty and zero (requires export_bitmap in config):
  curl -s "http://127.0.0.1:54323/images/demo/extents?context=dirty" | jq .

- POST flush:
  curl -s -X POST http://127.0.0.1:54323/images/demo/flush | jq .

- PATCH zero (zero a byte range; application/json body):
  curl -k -X PATCH \
    -H "Content-Type: application/json" \
    --data-binary '{"op": "zero", "offset": 4096, "size": 8192}' \
    http://127.0.0.1:54323/images/demo

  Zero at offset 1 GiB, 4096 bytes, no flush:
  curl -k -X PATCH \
    -H "Content-Type: application/json" \
    --data-binary '{"op": "zero", "offset": 1073741824, "size": 4096}' \
    http://127.0.0.1:54323/images/demo

  Zero entire disk and flush:
  curl -k -X PATCH \
    -H "Content-Type: application/json" \
    --data-binary '{"op": "zero", "size": 107374182400, "flush": true}' \
    http://127.0.0.1:54323/images/demo

- PATCH flush (flush data to storage; operates on entire image):
  curl -k -X PATCH \
    -H "Content-Type: application/json" \
    --data-binary '{"op": "flush"}' \
    http://127.0.0.1:54323/images/demo

- PATCH range (write binary body at byte range; Range + Content-Length required):
  curl -v -X PATCH -H "Range: bytes=0-1048576" --data-binary @chunk.bin \
    http://127.0.0.1:54323/images/demo
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
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import parse_qs
import nbd

CHUNK_SIZE = 256 * 1024  # 256 KiB

# NBD base:allocation flags (hole=1, zero=2; hole|zero=3)
_NBD_STATE_HOLE = 1
_NBD_STATE_ZERO = 2
# NBD qemu:dirty-bitmap flags (dirty=1)
_NBD_STATE_DIRTY = 1

# Concurrency limits across ALL images.
MAX_PARALLEL_READS = 8
MAX_PARALLEL_WRITES = 1

_READ_SEM = threading.Semaphore(MAX_PARALLEL_READS)
_WRITE_SEM = threading.Semaphore(MAX_PARALLEL_WRITES)

# In-memory per-image lock: single lock gates both read and write.
_IMAGE_LOCKS: Dict[str, threading.Lock] = {}
_IMAGE_LOCKS_GUARD = threading.Lock()


# Dynamic image_id(transferId) -> backend mapping:
# CloudStack writes a JSON file at /tmp/imagetransfer/<transferId> with:
#   - NBD backend: {"backend": "nbd", "host": "...", "port": 10809, "export": "vda", "export_bitmap": "..."}
#   - File backend: {"backend": "file", "file": "/path/to/image.qcow2"}
#
# This server reads that file on-demand.
_CFG_DIR = "/tmp/imagetransfer"
_CFG_CACHE: Dict[str, Tuple[float, Dict[str, Any]]] = {}
_CFG_CACHE_GUARD = threading.Lock()


def _json_bytes(obj: Any) -> bytes:
    return json.dumps(obj, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def _merge_dirty_zero_extents(
    allocation_extents: List[Tuple[int, int, bool]],
    dirty_extents: List[Tuple[int, int, bool]],
    size: int,
) -> List[Dict[str, Any]]:
    """
    Merge allocation (start, length, zero) and dirty (start, length, dirty) extents
    into a single list of {start, length, dirty, zero} with unified boundaries.
    """
    boundaries: set[int] = {0, size}
    for start, length, _ in allocation_extents:
        boundaries.add(start)
        boundaries.add(start + length)
    for start, length, _ in dirty_extents:
        boundaries.add(start)
        boundaries.add(start + length)
    sorted_boundaries = sorted(boundaries)

    def lookup(
        extents: List[Tuple[int, int, bool]], offset: int, default: bool
    ) -> bool:
        for start, length, flag in extents:
            if start <= offset < start + length:
                return flag
        return default

    result: List[Dict[str, Any]] = []
    for i in range(len(sorted_boundaries) - 1):
        a, b = sorted_boundaries[i], sorted_boundaries[i + 1]
        if a >= b:
            continue
        result.append(
            {
                "start": a,
                "length": b - a,
                "dirty": lookup(dirty_extents, a, False),
                "zero": lookup(allocation_extents, a, False),
            }
        )
    return result


def _is_fallback_dirty_response(extents: List[Dict[str, Any]]) -> bool:
    """True if extents is the single-extent fallback (dirty=false, zero=false)."""
    return (
        len(extents) == 1
        and extents[0].get("dirty") is False
        and extents[0].get("zero") is False
    )


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
        logging.error("cfg stat failed image_id=%s err=%r", image_id, e)
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
        logging.error("cfg read failed image_id=%s err=%r", image_id, e)
        return None

    try:
        obj = json.loads(raw.decode("utf-8"))
    except Exception as e:
        logging.error("cfg parse failed image_id=%s err=%r", image_id, e)
        return None

    if not isinstance(obj, dict):
        logging.error("cfg invalid type image_id=%s type=%s", image_id, type(obj).__name__)
        return None

    backend = obj.get("backend")
    if backend is None:
        backend = "nbd"
    if not isinstance(backend, str):
        logging.error("cfg invalid backend type image_id=%s", image_id)
        return None
    backend = backend.lower()
    if backend not in ("nbd", "file"):
        logging.error("cfg unsupported backend image_id=%s backend=%s", image_id, backend)
        return None

    if backend == "file":
        file_path = obj.get("file")
        if not isinstance(file_path, str) or not file_path.strip():
            logging.error("cfg missing/invalid file path for file backend image_id=%s", image_id)
            return None
        cfg = {"backend": "file", "file": file_path.strip()}
    else:
        host = obj.get("host")
        port = obj.get("port")
        export = obj.get("export")
        export_bitmap = obj.get("export_bitmap")
        if not isinstance(host, str) or not host:
            logging.error("cfg missing/invalid host image_id=%s", image_id)
            return None
        try:
            port_i = int(port)
        except Exception:
            logging.error("cfg missing/invalid port image_id=%s", image_id)
            return None
        if port_i <= 0 or port_i > 65535:
            logging.error("cfg out-of-range port image_id=%s port=%r", image_id, port)
            return None
        if export is not None and (not isinstance(export, str) or not export):
            logging.error("cfg missing/invalid export image_id=%s", image_id)
            return None
        cfg = {
            "backend": "nbd",
            "host": host,
            "port": port_i,
            "export": export,
            "export_bitmap": export_bitmap,
        }

    with _CFG_CACHE_GUARD:
        _CFG_CACHE[safe_id] = (float(st.st_mtime), cfg)
    return cfg


class _NbdConn:
    """
    Small helper to connect to NBD using an already-open TCP socket.
    Opens a fresh handle per request, per POC requirements.
    """

    def __init__(
        self,
        host: str,
        port: int,
        export: Optional[str],
        need_block_status: bool = False,
        extra_meta_contexts: Optional[List[str]] = None,
    ):
        self._sock = socket.create_connection((host, port))
        self._nbd = nbd.NBD()

        # Select export name if supported/needed.
        if export and hasattr(self._nbd, "set_export_name"):
            self._nbd.set_export_name(export)

        # Request meta contexts before connect (for block status / dirty bitmap).
        if need_block_status and hasattr(self._nbd, "add_meta_context"):
            for ctx in ["base:allocation"] + (extra_meta_contexts or []):
                try:
                    self._nbd.add_meta_context(ctx)
                except Exception as e:
                    logging.warning("add_meta_context %r failed: %r", ctx, e)

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

    def get_capabilities(self) -> Dict[str, bool]:
        """
        Query NBD export capabilities (read_only, can_flush, can_zero) from the
        server handshake. Returns dict with keys read_only, can_flush, can_zero.
        Uses getattr for binding name variations (is_read_only/get_read_only, etc.).
        """
        out: Dict[str, bool] = {
            "read_only": True,
            "can_flush": False,
            "can_zero": False,
        }
        for name, keys in [
            ("read_only", ("is_read_only", "get_read_only")),
            ("can_flush", ("can_flush", "get_can_flush")),
            ("can_zero", ("can_zero", "get_can_zero")),
        ]:
            for attr in keys:
                if hasattr(self._nbd, attr):
                    try:
                        val = getattr(self._nbd, attr)()
                        out[name] = bool(val)
                    except Exception:
                        pass
                    break
        return out

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

    def pzero(self, offset: int, size: int) -> None:
        """
        Zero a byte range. Uses NBD WRITE_ZEROES when available (efficient/punch hole),
        otherwise falls back to writing zero bytes via pwrite.
        """
        if size <= 0:
            return
        # Try libnbd pwrite_zeros / zero; argument order varies by binding.
        for name in ("pwrite_zeros", "zero"):
            if not hasattr(self._nbd, name):
                continue
            fn = getattr(self._nbd, name)
            try:
                fn(size, offset)
                return
            except TypeError:
                try:
                    fn(offset, size)
                    return
                except TypeError:
                    pass
        # Fallback: write zeros in chunks.
        remaining = size
        pos = offset
        zero_buf = b"\x00" * min(CHUNK_SIZE, size)
        while remaining > 0:
            chunk = min(len(zero_buf), remaining)
            self.pwrite(zero_buf[:chunk], pos)
            pos += chunk
            remaining -= chunk

    def flush(self) -> None:
        if hasattr(self._nbd, "flush"):
            self._nbd.flush()
            return
        if hasattr(self._nbd, "fsync"):
            self._nbd.fsync()
            return
        raise RuntimeError("libnbd binding has no flush/fsync method")

    def get_zero_extents(self) -> List[Dict[str, Any]]:
        """
        Query NBD block status (base:allocation) and return extents that are
        hole or zero in imageio format: [{"start": ..., "length": ..., "zero": true}, ...].
        Returns [] if block status is not supported; fallback to one full-image
        zero extent when we have size but block status fails.
        """
        size = self.size()
        if size == 0:
            return []

        if not hasattr(self._nbd, "block_status") and not hasattr(
            self._nbd, "block_status_64"
        ):
            logging.error("get_zero_extents: no block_status/block_status_64")
            return self._fallback_zero_extent(size)
        if hasattr(self._nbd, "can_meta_context") and not self._nbd.can_meta_context(
            "base:allocation"
        ):
            logging.error(
                "get_zero_extents: server did not negotiate base:allocation"
            )
            return self._fallback_zero_extent(size)

        zero_extents: List[Dict[str, Any]] = []
        chunk = min(size, 64 * 1024 * 1024)  # 64 MiB
        offset = 0

        def extent_cb(*args: Any, **kwargs: Any) -> int:
            # Binding typically passes (metacontext, offset, entries[, nr_entries][, error]).
            metacontext = None
            off = 0
            entries = None
            if len(args) >= 3:
                metacontext, off, entries = args[0], args[1], args[2]
            else:
                for a in args:
                    if isinstance(a, str):
                        metacontext = a
                    elif isinstance(a, int):
                        off = a
                    elif a is not None and hasattr(a, "__iter__"):
                        entries = a
            if metacontext != "base:allocation" or entries is None:
                return 0
            current = off
            try:
                flat = list(entries)
                for i in range(0, len(flat), 2):
                    if i + 1 >= len(flat):
                        break
                    length = int(flat[i])
                    flags = int(flat[i + 1])
                    if (flags & (_NBD_STATE_HOLE | _NBD_STATE_ZERO)) != 0:
                        zero_extents.append(
                            {"start": current, "length": length, "zero": True}
                        )
                    current += length
            except (TypeError, ValueError, IndexError):
                pass
            return 0

        block_status_fn = getattr(
            self._nbd, "block_status_64", getattr(self._nbd, "block_status", None)
        )
        if block_status_fn is None:
            return self._fallback_zero_extent(size)

        try:
            while offset < size:
                count = min(chunk, size - offset)
                # Try (count, offset, callback) then (offset, count, callback)
                try:
                    block_status_fn(count, offset, extent_cb)
                except TypeError:
                    block_status_fn(offset, count, extent_cb)
                offset += count
        except Exception as e:
            logging.error("get_zero_extents block_status failed: %r", e)
            return self._fallback_zero_extent(size)
        if not zero_extents:
            return self._fallback_zero_extent(size)
        return zero_extents

    def _fallback_zero_extent(self, size: int) -> List[Dict[str, Any]]:
        """Return one zero extent covering the whole image when block status unavailable."""
        return [{"start": 0, "length": size, "zero": True}]

    def get_allocation_extents(self) -> List[Dict[str, Any]]:
        """
        Query base:allocation and return all extents (allocated and hole/zero)
        as [{"start": ..., "length": ..., "zero": bool}, ...].
        Fallback when block status unavailable: one extent with zero=False.
        """
        size = self.size()
        if size == 0:
            return []
        if not hasattr(self._nbd, "block_status") and not hasattr(
            self._nbd, "block_status_64"
        ):
            return [{"start": 0, "length": size, "zero": False}]
        if hasattr(self._nbd, "can_meta_context") and not self._nbd.can_meta_context(
            "base:allocation"
        ):
            return [{"start": 0, "length": size, "zero": False}]

        allocation_extents: List[Dict[str, Any]] = []
        chunk = min(size, 64 * 1024 * 1024)
        offset = 0

        def extent_cb(*args: Any, **kwargs: Any) -> int:
            if len(args) < 3:
                return 0
            metacontext, off, entries = args[0], args[1], args[2]
            if metacontext != "base:allocation" or entries is None:
                return 0
            current = off
            try:
                flat = list(entries)
                for i in range(0, len(flat), 2):
                    if i + 1 >= len(flat):
                        break
                    length = int(flat[i])
                    flags = int(flat[i + 1])
                    zero = (flags & (_NBD_STATE_HOLE | _NBD_STATE_ZERO)) != 0
                    allocation_extents.append(
                        {"start": current, "length": length, "zero": zero}
                    )
                    current += length
            except (TypeError, ValueError, IndexError):
                pass
            return 0

        block_status_fn = getattr(
            self._nbd, "block_status_64", getattr(self._nbd, "block_status", None)
        )
        if block_status_fn is None:
            return [{"start": 0, "length": size, "zero": False}]
        try:
            while offset < size:
                count = min(chunk, size - offset)
                try:
                    block_status_fn(count, offset, extent_cb)
                except TypeError:
                    block_status_fn(offset, count, extent_cb)
                offset += count
        except Exception as e:
            logging.warning("get_allocation_extents block_status failed: %r", e)
            return [{"start": 0, "length": size, "zero": False}]
        if not allocation_extents:
            return [{"start": 0, "length": size, "zero": False}]
        return allocation_extents

    def get_extents_dirty_and_zero(
        self, dirty_bitmap_context: str
    ) -> List[Dict[str, Any]]:
        """
        Query block status for base:allocation and qemu:dirty-bitmap:<context>,
        merge boundaries, and return extents with dirty and zero flags.
        Format: [{"start": ..., "length": ..., "dirty": bool, "zero": bool}, ...].
        """
        size = self.size()
        if size == 0:
            return []
        if not hasattr(self._nbd, "block_status") and not hasattr(
            self._nbd, "block_status_64"
        ):
            return self._fallback_dirty_zero_extents(size)
        if hasattr(self._nbd, "can_meta_context"):
            if not self._nbd.can_meta_context("base:allocation"):
                return self._fallback_dirty_zero_extents(size)
            if not self._nbd.can_meta_context(dirty_bitmap_context):
                logging.warning(
                    "dirty bitmap context %r not negotiated", dirty_bitmap_context
                )
                return self._fallback_dirty_zero_extents(size)

        allocation_extents: List[Tuple[int, int, bool]] = []  # (start, length, zero)
        dirty_extents: List[Tuple[int, int, bool]] = []  # (start, length, dirty)
        chunk = min(size, 64 * 1024 * 1024)
        offset = 0

        def extent_cb(*args: Any, **kwargs: Any) -> int:
            if len(args) < 3:
                return 0
            metacontext, off, entries = args[0], args[1], args[2]
            if entries is None or not hasattr(entries, "__iter__"):
                return 0
            current = off
            try:
                flat = list(entries)
                for i in range(0, len(flat), 2):
                    if i + 1 >= len(flat):
                        break
                    length = int(flat[i])
                    flags = int(flat[i + 1])
                    if metacontext == "base:allocation":
                        zero = (flags & (_NBD_STATE_HOLE | _NBD_STATE_ZERO)) != 0
                        allocation_extents.append((current, length, zero))
                    elif metacontext == dirty_bitmap_context:
                        dirty = (flags & _NBD_STATE_DIRTY) != 0
                        dirty_extents.append((current, length, dirty))
                    current += length
            except (TypeError, ValueError, IndexError):
                pass
            return 0

        block_status_fn = getattr(
            self._nbd, "block_status_64", getattr(self._nbd, "block_status", None)
        )
        if block_status_fn is None:
            return self._fallback_dirty_zero_extents(size)
        try:
            while offset < size:
                count = min(chunk, size - offset)
                try:
                    block_status_fn(count, offset, extent_cb)
                except TypeError:
                    block_status_fn(offset, count, extent_cb)
                offset += count
        except Exception as e:
            logging.warning("get_extents_dirty_and_zero block_status failed: %r", e)
            return self._fallback_dirty_zero_extents(size)
        return _merge_dirty_zero_extents(allocation_extents, dirty_extents, size)

    def _fallback_dirty_zero_extents(self, size: int) -> List[Dict[str, Any]]:
        """One extent: whole image, dirty=false, zero=false when bitmap unavailable."""
        return [{"start": 0, "length": size, "dirty": False, "zero": False}]

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

    def _send_imageio_headers(
        self, allowed_methods: Optional[str] = None
    ) -> None:
        # Include these headers for compatibility with the imageio contract.
        if allowed_methods is None:
            allowed_methods = "GET, PUT, OPTIONS"
        self.send_header("Access-Control-Allow-Methods", allowed_methods)
        self.send_header("Accept-Ranges", "bytes")

    def _send_json(
        self,
        status: int,
        obj: Any,
        allowed_methods: Optional[str] = None,
    ) -> None:
        body = _json_bytes(obj)
        self.send_response(status)
        self._send_imageio_headers(allowed_methods)
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

    def _parse_query(self) -> Dict[str, List[str]]:
        """Parse query string from self.path into a dict of name -> list of values."""
        if "?" not in self.path:
            return {}
        query = self.path.split("?", 1)[1]
        return parse_qs(query, keep_blank_values=True)

    def _image_cfg(self, image_id: str) -> Optional[Dict[str, Any]]:
        return _load_image_cfg(image_id)

    def _is_file_backend(self, cfg: Dict[str, Any]) -> bool:
        return cfg.get("backend") == "file"

    def do_OPTIONS(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return
        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return
        if self._is_file_backend(cfg):
            # File backend: full PUT only, no range writes; GET with ranges allowed; flush supported.
            allowed_methods = "GET, PUT, POST, OPTIONS"
            features = ["flush"]
            max_writers = MAX_PARALLEL_WRITES
            response = {
                "unix_socket": None,
                "features": features,
                "max_readers": MAX_PARALLEL_READS,
                "max_writers": max_writers,
            }
            self._send_json(HTTPStatus.OK, response, allowed_methods=allowed_methods)
            return
        # Query NBD backend for capabilities (like nbdinfo); fall back to config.
        read_only = True
        can_flush = False
        can_zero = False
        try:
            with _NbdConn(
                cfg["host"],
                int(cfg["port"]),
                cfg.get("export"),
            ) as conn:
                caps = conn.get_capabilities()
                read_only = caps["read_only"]
                can_flush = caps["can_flush"]
                can_zero = caps["can_zero"]
        except Exception as e:
            logging.warning("OPTIONS: could not query NBD capabilities: %r", e)
            read_only = bool(cfg.get("read_only"))
            if not read_only:
                can_flush = True
                can_zero = True
        # Report options for this image from NBD: read-only => no PUT; only advertise supported features.
        if read_only:
            allowed_methods = "GET, OPTIONS"
            features = ["extents"]
            max_writers = 0
        else:
            # PATCH: JSON (zero/flush) and Range+binary (write byte range).
            allowed_methods = "GET, PUT, PATCH, OPTIONS"
            features = ["extents"]
            if can_zero:
                features.append("zero")
            if can_flush:
                features.append("flush")
            max_writers = MAX_PARALLEL_WRITES if not read_only else 0
        response = {
            "unix_socket": None,  # Not used in this implementation
            "features": features,
            "max_readers": MAX_PARALLEL_READS,
            "max_writers": max_writers,
        }
        self._send_json(HTTPStatus.OK, response, allowed_methods=allowed_methods)

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
            if self._is_file_backend(cfg):
                self._send_error_json(
                    HTTPStatus.BAD_REQUEST, "extents not supported for file backend"
                )
                return
            query = self._parse_query()
            context = (query.get("context") or [None])[0]
            self._handle_get_extents(image_id, cfg, context=context)
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

    def do_PATCH(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return
        if self._is_file_backend(cfg):
            self._send_error_json(
                HTTPStatus.BAD_REQUEST,
                "range writes and PATCH not supported for file backend; use PUT for full upload",
            )
            return

        content_type = self.headers.get("Content-Type", "").split(";")[0].strip().lower()
        range_header = self.headers.get("Range")

        # Binary PATCH: Range + body writes bytes at that range (e.g. curl -X PATCH -H "Range: bytes=0-1048576" --data-binary @chunk.bin).
        if range_header is not None and content_type != "application/json":
            content_length_hdr = self.headers.get("Content-Length")
            if content_length_hdr is None:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length required")
                return
            try:
                content_length = int(content_length_hdr)
            except ValueError:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
                return
            if content_length <= 0:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length must be positive")
                return
            self._handle_patch_range(image_id, cfg, range_header, content_length)
            return

        # JSON PATCH: application/json with op (zero, flush).
        if content_type != "application/json":
            self._send_error_json(
                HTTPStatus.UNSUPPORTED_MEDIA_TYPE,
                "PATCH requires Content-Type: application/json (for zero/flush) or Range with binary body",
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
        if content_length <= 0 or content_length > 64 * 1024:
            self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
            return

        body = self.rfile.read(content_length)
        if len(body) != content_length:
            self._send_error_json(HTTPStatus.BAD_REQUEST, "request body truncated")
            return

        try:
            payload = json.loads(body.decode("utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError) as e:
            self._send_error_json(HTTPStatus.BAD_REQUEST, f"invalid JSON: {e}")
            return

        if not isinstance(payload, dict):
            self._send_error_json(HTTPStatus.BAD_REQUEST, "body must be a JSON object")
            return

        op = payload.get("op")
        if op == "flush":
            # Flush entire image; offset and size are ignored (per spec).
            self._handle_post_flush(image_id, cfg)
            return
        if op != "zero":
            self._send_error_json(
                HTTPStatus.BAD_REQUEST,
                "unsupported op; only \"zero\" and \"flush\" are supported",
            )
            return

        try:
            size = int(payload.get("size"))
        except (TypeError, ValueError):
            self._send_error_json(HTTPStatus.BAD_REQUEST, "missing or invalid \"size\"")
            return
        if size <= 0:
            self._send_error_json(HTTPStatus.BAD_REQUEST, "\"size\" must be positive")
            return

        offset = payload.get("offset")
        if offset is None:
            offset = 0
        else:
            try:
                offset = int(offset)
            except (TypeError, ValueError):
                self._send_error_json(HTTPStatus.BAD_REQUEST, "invalid \"offset\"")
                return
            if offset < 0:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "\"offset\" must be non-negative")
                return

        flush = bool(payload.get("flush", False))

        self._handle_patch_zero(image_id, cfg, offset=offset, size=size, flush=flush)

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
            if self._is_file_backend(cfg):
                file_path = cfg["file"]
                try:
                    size = os.path.getsize(file_path)
                except OSError as e:
                    logging.error("GET file size error image_id=%s path=%s err=%r", image_id, file_path, e)
                    self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "failed to access file")
                    return
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
                with open(file_path, "rb") as f:
                    f.seek(offset)
                    while offset < end_excl:
                        to_read = min(CHUNK_SIZE, end_excl - offset)
                        data = f.read(to_read)
                        if not data:
                            break
                        try:
                            self.wfile.write(data)
                        except BrokenPipeError:
                            logging.info("GET client disconnected image_id=%s at=%d", image_id, offset)
                            break
                        offset += len(data)
                        bytes_sent += len(data)
            else:
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
            logging.error("GET error image_id=%s err=%r", image_id, e)
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
            if self._is_file_backend(cfg):
                file_path = cfg["file"]
                remaining = content_length
                with open(file_path, "wb") as f:
                    while remaining > 0:
                        chunk = self.rfile.read(min(CHUNK_SIZE, remaining))
                        if not chunk:
                            self._send_error_json(
                                HTTPStatus.BAD_REQUEST,
                                f"request body ended early at {bytes_written} bytes",
                            )
                            return
                        f.write(chunk)
                        bytes_written += len(chunk)
                        remaining -= len(chunk)
                self._send_json(HTTPStatus.OK, {"ok": True, "bytes_written": bytes_written})
            else:
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
            logging.error("PUT error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            _WRITE_SEM.release()
            lock.release()
            dur = _now_s() - start
            logging.info(
                "PUT end image_id=%s bytes=%d duration_s=%.3f", image_id, bytes_written, dur
            )

    def _handle_get_extents(
        self, image_id: str, cfg: Dict[str, Any], context: Optional[str] = None
    ) -> None:
        # context=dirty: return extents with dirty and zero from base:allocation + bitmap.
        # Otherwise: return zero/hole extents from base:allocation only.
        lock = _get_image_lock(image_id)
        if not lock.acquire(blocking=False):
            self._send_error_json(HTTPStatus.CONFLICT, "image busy")
            return

        start = _now_s()
        try:
            logging.info("EXTENTS start image_id=%s context=%s", image_id, context)
            if context == "dirty":
                export_bitmap = cfg.get("export_bitmap")
                if not export_bitmap:
                    # Fallback: same structure as zero extents but dirty=true for all ranges
                    with _NbdConn(
                        cfg["host"],
                        int(cfg["port"]),
                        cfg.get("export"),
                        need_block_status=True,
                    ) as conn:
                        allocation = conn.get_allocation_extents()
                    extents = [
                        {"start": e["start"], "length": e["length"], "dirty": True, "zero": e["zero"]}
                        for e in allocation
                    ]
                else:
                    dirty_bitmap_ctx = f"qemu:dirty-bitmap:{export_bitmap}"
                    extra_contexts: List[str] = [dirty_bitmap_ctx]
                    with _NbdConn(
                        cfg["host"],
                        int(cfg["port"]),
                        cfg.get("export"),
                        need_block_status=True,
                        extra_meta_contexts=extra_contexts,
                    ) as conn:
                        extents = conn.get_extents_dirty_and_zero(dirty_bitmap_ctx)
                    # When bitmap not actually available, same fallback: zero structure + dirty=true
                    if _is_fallback_dirty_response(extents):
                        with _NbdConn(
                            cfg["host"],
                            int(cfg["port"]),
                            cfg.get("export"),
                            need_block_status=True,
                        ) as conn:
                            allocation = conn.get_allocation_extents()
                        extents = [
                            {
                                "start": e["start"],
                                "length": e["length"],
                                "dirty": True,
                                "zero": e["zero"],
                            }
                            for e in allocation
                        ]
            else:
                with _NbdConn(
                    cfg["host"],
                    int(cfg["port"]),
                    cfg.get("export"),
                    need_block_status=True,
                ) as conn:
                    extents = conn.get_zero_extents()
            self._send_json(HTTPStatus.OK, extents)
        except Exception as e:
            logging.error("EXTENTS error image_id=%s err=%r", image_id, e)
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
            if self._is_file_backend(cfg):
                file_path = cfg["file"]
                with open(file_path, "rb") as f:
                    f.flush()
                    os.fsync(f.fileno())
                self._send_json(HTTPStatus.OK, {"ok": True})
            else:
                with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                    conn.flush()
                self._send_json(HTTPStatus.OK, {"ok": True})
        except Exception as e:
            logging.error("FLUSH error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            lock.release()
            dur = _now_s() - start
            logging.info("FLUSH end image_id=%s duration_s=%.3f", image_id, dur)

    def _handle_patch_zero(
        self,
        image_id: str,
        cfg: Dict[str, Any],
        offset: int,
        size: int,
        flush: bool,
    ) -> None:
        lock = _get_image_lock(image_id)
        if not lock.acquire(blocking=False):
            self._send_error_json(HTTPStatus.CONFLICT, "image busy")
            return

        if not _WRITE_SEM.acquire(blocking=False):
            lock.release()
            self._send_error_json(HTTPStatus.SERVICE_UNAVAILABLE, "too many parallel writes")
            return

        start = _now_s()
        try:
            logging.info(
                "PATCH zero start image_id=%s offset=%d size=%d flush=%s",
                image_id, offset, size, flush,
            )
            with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                image_size = conn.size()
                if offset >= image_size:
                    self._send_error_json(
                        HTTPStatus.BAD_REQUEST,
                        "offset must be less than image size",
                    )
                    return
                zero_size = min(size, image_size - offset)
                conn.pzero(offset, zero_size)
                if flush:
                    conn.flush()
            self._send_json(HTTPStatus.OK, {"ok": True})
        except Exception as e:
            logging.error("PATCH zero error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            _WRITE_SEM.release()
            lock.release()
            dur = _now_s() - start
            logging.info("PATCH zero end image_id=%s duration_s=%.3f", image_id, dur)

    def _handle_patch_range(
        self,
        image_id: str,
        cfg: Dict[str, Any],
        range_header: str,
        content_length: int,
    ) -> None:
        """Write request body to the image at the byte range from Range header."""
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
            logging.info(
                "PATCH range start image_id=%s range=%s content_length=%d",
                image_id, range_header, content_length,
            )
            with _NbdConn(cfg["host"], int(cfg["port"]), cfg.get("export")) as conn:
                image_size = conn.size()
                try:
                    start_off, end_inclusive = self._parse_single_range(
                        range_header, image_size
                    )
                except ValueError as e:
                    if "unsatisfiable" in str(e).lower():
                        self._send_range_not_satisfiable(image_size)
                    else:
                        self._send_error_json(
                            HTTPStatus.BAD_REQUEST, f"invalid Range header: {e}"
                        )
                    return
                expected_len = end_inclusive - start_off + 1
                if content_length != expected_len:
                    self._send_error_json(
                        HTTPStatus.BAD_REQUEST,
                        f"Content-Length ({content_length}) must equal range length ({expected_len})",
                    )
                    return
                offset = start_off
                remaining = content_length
                while remaining > 0:
                    chunk = self.rfile.read(min(CHUNK_SIZE, remaining))
                    if not chunk:
                        self._send_error_json(
                            HTTPStatus.BAD_REQUEST,
                            f"request body ended early at {bytes_written} bytes",
                        )
                        return
                    conn.pwrite(chunk, offset)
                    n = len(chunk)
                    offset += n
                    remaining -= n
                    bytes_written += n
            self._send_json(HTTPStatus.OK, {"ok": True, "bytes_written": bytes_written})
        except Exception as e:
            logging.error("PATCH range error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            _WRITE_SEM.release()
            lock.release()
            dur = _now_s() - start
            logging.info(
                "PATCH range end image_id=%s bytes=%d duration_s=%.3f",
                image_id, bytes_written, dur,
            )


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
