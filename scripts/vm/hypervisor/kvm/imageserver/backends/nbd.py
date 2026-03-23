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

import logging
import socket
from typing import Any, Dict, List, Optional, Tuple

import nbd

from ..constants import (
    CHUNK_SIZE,
    NBD_BLOCK_STATUS_CHUNK,
    NBD_STATE_DIRTY,
    NBD_STATE_HOLE,
    NBD_STATE_ZERO,
)
from ..util import merge_dirty_zero_extents
from .base import BackendSession, ImageBackend


class NbdConnection:
    """
    Low-level helper to connect to an NBD server over a Unix socket.
    Opens a fresh handle per connection.
    """

    def __init__(
        self,
        socket_path: str,
        export: Optional[str],
        need_block_status: bool = False,
        extra_meta_contexts: Optional[List[str]] = None,
    ):
        self._sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self._sock.connect(socket_path)
        self._nbd = nbd.NBD()

        if export and hasattr(self._nbd, "set_export_name"):
            self._nbd.set_export_name(export)

        if need_block_status and hasattr(self._nbd, "add_meta_context"):
            for ctx in ["base:allocation"] + (extra_meta_contexts or []):
                try:
                    self._nbd.add_meta_context(ctx)
                except Exception as e:
                    logging.warning("add_meta_context %r failed: %r", ctx, e)

        self._connect_existing_socket(self._sock)

    def _connect_existing_socket(self, sock: socket.socket) -> None:
        last_err: Optional[BaseException] = None
        if hasattr(self._nbd, "connect_socket"):
            try:
                self._nbd.connect_socket(sock)
                return
            except Exception as e:
                last_err = e
                try:
                    self._nbd.connect_socket(sock.fileno())
                    return
                except Exception as e2:
                    last_err = e2
        if hasattr(self._nbd, "connect_fd"):
            try:
                self._nbd.connect_fd(sock.fileno())
                return
            except Exception as e:
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
        server handshake.  Uses getattr for binding name variations.
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
        try:
            return self._nbd.pread(length, offset)
        except TypeError:
            return self._nbd.pread(offset, length)

    def pwrite(self, buf: bytes, offset: int) -> None:
        try:
            self._nbd.pwrite(buf, offset)
        except TypeError:
            self._nbd.pwrite(offset, buf)

    def pzero(self, offset: int, size: int) -> None:
        """
        Zero a byte range.  Uses NBD WRITE_ZEROES when available,
        otherwise falls back to writing zero bytes via pwrite.
        """
        if size <= 0:
            return
        for fn_name in ("pwrite_zeros", "zero"):
            if not hasattr(self._nbd, fn_name):
                continue
            fn = getattr(self._nbd, fn_name)
            try:
                fn(size, offset)
                return
            except TypeError:
                try:
                    fn(offset, size)
                    return
                except TypeError:
                    pass
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

    def get_allocation_extents(self) -> List[Dict[str, Any]]:
        """
        Query base:allocation and return all extents as
        [{"start": ..., "length": ..., "zero": bool}, ...].
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
        chunk = min(size, NBD_BLOCK_STATUS_CHUNK)
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
                    zero = (flags & (NBD_STATE_HOLE | NBD_STATE_ZERO)) != 0
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
        Query block status for base:allocation and a dirty bitmap context,
        merge boundaries, and return extents with dirty and zero flags.
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

        allocation_extents: List[Tuple[int, int, bool]] = []
        dirty_extents: List[Tuple[int, int, bool]] = []
        chunk = min(size, NBD_BLOCK_STATUS_CHUNK)
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
                        zero = (flags & (NBD_STATE_HOLE | NBD_STATE_ZERO)) != 0
                        allocation_extents.append((current, length, zero))
                    elif metacontext == dirty_bitmap_context:
                        dirty = (flags & NBD_STATE_DIRTY) != 0
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
        return merge_dirty_zero_extents(allocation_extents, dirty_extents, size)

    @staticmethod
    def _fallback_dirty_zero_extents(size: int) -> List[Dict[str, Any]]:
        return [{"start": 0, "length": size, "dirty": False, "zero": False}]

    def close(self) -> None:
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

    def __enter__(self) -> "NbdConnection":
        return self

    def __exit__(self, exc_type: Any, exc: Any, tb: Any) -> None:
        self.close()


class NbdSession(BackendSession):
    """
    Holds a single NbdConnection open for the duration of a streaming operation.
    Raises RuntimeError if pread returns empty data (NBD should never do this).
    """

    def __init__(self, conn: NbdConnection):
        self._conn = conn

    def size(self) -> int:
        return self._conn.size()

    def read(self, offset: int, length: int) -> bytes:
        data = self._conn.pread(length, offset)
        if not data:
            raise RuntimeError("backend returned empty read")
        return data

    def close(self) -> None:
        self._conn.close()


class NbdBackend(ImageBackend):
    """
    ImageBackend implementation that proxies to an NBD server via Unix socket.
    Each public method opens a fresh NbdConnection (per the original design).
    """

    def __init__(
        self,
        socket_path: str,
        export: Optional[str] = None,
        export_bitmap: Optional[str] = None,
    ):
        self._socket_path = socket_path
        self._export = export
        self._export_bitmap = export_bitmap

    @property
    def supports_extents(self) -> bool:
        return True

    @property
    def supports_range_write(self) -> bool:
        return True

    @property
    def export_bitmap(self) -> Optional[str]:
        return self._export_bitmap

    def _connect(
        self,
        need_block_status: bool = False,
        extra_meta_contexts: Optional[List[str]] = None,
    ) -> NbdConnection:
        return NbdConnection(
            self._socket_path,
            self._export,
            need_block_status=need_block_status,
            extra_meta_contexts=extra_meta_contexts,
        )

    def size(self) -> int:
        with self._connect() as conn:
            return conn.size()

    def read(self, offset: int, length: int) -> bytes:
        with self._connect() as conn:
            return conn.pread(length, offset)

    def write(self, data: bytes, offset: int) -> None:
        with self._connect() as conn:
            conn.pwrite(data, offset)

    def write_full(self, stream: Any, content_length: int, flush: bool) -> int:
        bytes_written = 0
        with self._connect() as conn:
            offset = 0
            remaining = content_length
            while remaining > 0:
                chunk = stream.read(min(CHUNK_SIZE, remaining))
                if not chunk:
                    raise IOError(
                        f"request body ended early at {offset} bytes"
                    )
                conn.pwrite(chunk, offset)
                offset += len(chunk)
                remaining -= len(chunk)
                bytes_written += len(chunk)
            if flush:
                conn.flush()
        return bytes_written

    def write_range(self, stream: Any, start_off: int, content_length: int) -> int:
        """
        Write *content_length* bytes from *stream* to the image starting at *start_off*.
        Returns bytes written.  Raises ValueError if offset/length is out of bounds.
        """
        bytes_written = 0
        with self._connect() as conn:
            image_size = conn.size()
            if start_off >= image_size:
                raise ValueError(f"offset {start_off} >= image size {image_size}")
            max_len = image_size - start_off
            if content_length > max_len:
                raise ValueError(
                    f"content_length {content_length} exceeds available space {max_len}"
                )
            offset = start_off
            remaining = content_length
            while remaining > 0:
                chunk = stream.read(min(CHUNK_SIZE, remaining))
                if not chunk:
                    raise IOError(
                        f"request body ended early at {bytes_written} bytes"
                    )
                conn.pwrite(chunk, offset)
                n = len(chunk)
                offset += n
                remaining -= n
                bytes_written += n
        return bytes_written

    def flush(self) -> None:
        with self._connect() as conn:
            conn.flush()

    def zero(self, offset: int, length: int) -> None:
        with self._connect() as conn:
            image_size = conn.size()
            if offset >= image_size:
                raise ValueError("offset must be less than image size")
            zero_size = min(length, image_size - offset)
            conn.pzero(offset, zero_size)

    def get_capabilities(self) -> Dict[str, bool]:
        with self._connect() as conn:
            return conn.get_capabilities()

    def get_allocation_extents(self) -> List[Dict[str, Any]]:
        with self._connect(need_block_status=True) as conn:
            return conn.get_allocation_extents()

    def get_dirty_extents(self, dirty_bitmap_context: str) -> List[Dict[str, Any]]:
        extra_contexts: List[str] = [dirty_bitmap_context]
        with self._connect(
            need_block_status=True, extra_meta_contexts=extra_contexts
        ) as conn:
            return conn.get_extents_dirty_and_zero(dirty_bitmap_context)

    def open_session(self) -> NbdSession:
        return NbdSession(self._connect())

    def close(self) -> None:
        pass
