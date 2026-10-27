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

from abc import ABC, abstractmethod
from typing import Any, Dict, List


class BackendSession(ABC):
    """
    A session that holds an open connection/file handle for the duration of
    an operation (e.g. a full GET streaming read).  Use as a context manager.
    """

    @abstractmethod
    def size(self) -> int:
        """Return the image size in bytes."""
        ...

    @abstractmethod
    def read(self, offset: int, length: int) -> bytes:
        """
        Read *length* bytes starting at *offset*.

        For NBD backends, raises RuntimeError if the server returns empty data.
        For file backends, returns empty bytes at EOF.
        """
        ...

    @abstractmethod
    def close(self) -> None:
        """Release the underlying connection or file handle."""
        ...

    def __enter__(self) -> "BackendSession":
        return self

    def __exit__(self, exc_type: Any, exc: Any, tb: Any) -> None:
        self.close()


class ImageBackend(ABC):
    """
    Abstract base class for image storage backends.

    Each backend (NBD, file, etc.) implements this interface so the HTTP handler
    can operate uniformly without backend-specific branching.
    """

    @property
    @abstractmethod
    def supports_extents(self) -> bool:
        """Whether this backend supports querying allocation/dirty extents."""
        ...

    @property
    @abstractmethod
    def supports_range_write(self) -> bool:
        """Whether this backend supports writing at arbitrary byte offsets."""
        ...

    @abstractmethod
    def size(self) -> int:
        """Return the image size in bytes."""
        ...

    @abstractmethod
    def read(self, offset: int, length: int) -> bytes:
        """Read *length* bytes starting at *offset*."""
        ...

    @abstractmethod
    def write(self, data: bytes, offset: int) -> None:
        """Write *data* at *offset*."""
        ...

    @abstractmethod
    def write_full(self, stream, content_length: int, flush: bool) -> int:
        """
        Consume *content_length* bytes from *stream* and write the full image.
        Returns bytes written.  Raises on short read.
        """
        ...

    @abstractmethod
    def flush(self) -> None:
        """Flush pending data to stable storage."""
        ...

    @abstractmethod
    def zero(self, offset: int, length: int) -> None:
        """Zero *length* bytes starting at *offset*."""
        ...

    @abstractmethod
    def get_capabilities(self) -> Dict[str, bool]:
        """
        Return backend capabilities dict with keys:
        read_only, can_flush, can_zero.
        """
        ...

    @abstractmethod
    def get_allocation_extents(self) -> List[Dict[str, Any]]:
        """
        Return allocation extents as [{"start": int, "length": int, "zero": bool}, ...].
        """
        ...

    @abstractmethod
    def get_dirty_extents(self, dirty_bitmap_context: str) -> List[Dict[str, Any]]:
        """
        Return merged dirty+zero extents as
        [{"start": int, "length": int, "dirty": bool, "zero": bool}, ...].
        """
        ...

    @abstractmethod
    def open_session(self) -> BackendSession:
        """
        Open a session that holds a single connection/file handle for the
        duration of a streaming operation (e.g. GET).
        """
        ...

    @abstractmethod
    def close(self) -> None:
        """Release any resources held by this backend."""
        ...

    def __enter__(self) -> "ImageBackend":
        return self

    def __exit__(self, exc_type: Any, exc: Any, tb: Any) -> None:
        self.close()
