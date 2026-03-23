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

import os
from io import BufferedReader
from typing import Any, Dict, List, Optional

from ..constants import CHUNK_SIZE
from .base import BackendSession, ImageBackend


class FileSession(BackendSession):
    """
    Holds a single file handle open for the duration of a streaming read.
    Returns empty bytes at EOF (file semantics).
    """

    def __init__(self, path: str):
        self._path = path
        self._fh: Optional[BufferedReader] = open(path, "rb")
        self._size = os.path.getsize(path)

    def size(self) -> int:
        return self._size

    def read(self, offset: int, length: int) -> bytes:
        if self._fh is None:
            raise RuntimeError("session is closed")
        self._fh.seek(offset)
        return self._fh.read(length)

    def close(self) -> None:
        if self._fh is not None:
            self._fh.close()
            self._fh = None


class FileBackend(ImageBackend):
    """
    ImageBackend implementation backed by a local file (qcow2 or raw).
    Supports full read/write and flush.  Does not support extents or range writes.
    """

    def __init__(self, file_path: str):
        self._path = file_path

    @property
    def supports_extents(self) -> bool:
        return False

    @property
    def supports_range_write(self) -> bool:
        return False

    def size(self) -> int:
        return os.path.getsize(self._path)

    def read(self, offset: int, length: int) -> bytes:
        with open(self._path, "rb") as f:
            f.seek(offset)
            return f.read(length)

    def write(self, data: bytes, offset: int) -> None:
        raise NotImplementedError("file backend does not support range writes")

    def write_full(self, stream: Any, content_length: int, flush: bool) -> int:
        bytes_written = 0
        remaining = content_length
        with open(self._path, "wb") as f:
            while remaining > 0:
                chunk = stream.read(min(CHUNK_SIZE, remaining))
                if not chunk:
                    raise IOError(
                        f"request body ended early at {bytes_written} bytes"
                    )
                f.write(chunk)
                bytes_written += len(chunk)
                remaining -= len(chunk)
            if flush:
                f.flush()
                os.fsync(f.fileno())
        return bytes_written

    def flush(self) -> None:
        with open(self._path, "rb") as f:
            f.flush()
            os.fsync(f.fileno())

    def zero(self, offset: int, length: int) -> None:
        raise NotImplementedError("file backend does not support zero")

    def get_capabilities(self) -> Dict[str, bool]:
        return {
            "read_only": False,
            "can_flush": True,
            "can_zero": False,
        }

    def get_allocation_extents(self) -> List[Dict[str, Any]]:
        raise NotImplementedError("file backend does not support extents")

    def get_dirty_extents(self, dirty_bitmap_context: str) -> List[Dict[str, Any]]:
        raise NotImplementedError("file backend does not support extents")

    def open_session(self) -> FileSession:
        return FileSession(self._path)

    def close(self) -> None:
        pass
