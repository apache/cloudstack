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
import os
import threading
import time
from contextlib import contextmanager
from typing import Any, Dict, Iterator, List, Optional

from .constants import DEFAULT_IDLE_TIMEOUT_SECONDS


def parse_idle_timeout_seconds(obj: dict) -> int:
    """Seconds of idle time (no completed HTTP requests) before unregister."""
    v = obj.get("idle_timeout_seconds", DEFAULT_IDLE_TIMEOUT_SECONDS)
    if not isinstance(v, int):
        raise ValueError("idle_timeout_seconds must be an integer")
    v = int(v)
    if v < 1:
        v = 86400
    return v


def validate_transfer_config(obj: dict) -> dict:
    """
    Validate and normalize a transfer config dict received over the control
    socket. Returns the cleaned config or raises ValueError.
    """
    idle_sec = parse_idle_timeout_seconds(obj)

    backend = obj.get("backend")
    if backend is None:
        backend = "nbd"
    if not isinstance(backend, str):
        raise ValueError("invalid backend type")
    backend = backend.lower()
    if backend not in ("nbd", "file"):
        raise ValueError(f"unsupported backend: {backend}")

    if backend == "file":
        file_path = obj.get("file")
        if not isinstance(file_path, str) or not file_path.strip():
            raise ValueError("missing/invalid file path for file backend")
        return {"backend": "file", "file": file_path.strip(), "idle_timeout_seconds": idle_sec}

    socket_path = obj.get("socket")
    export = obj.get("export")
    export_bitmap = obj.get("export_bitmap")
    if not isinstance(socket_path, str) or not socket_path.strip():
        raise ValueError("missing/invalid socket path for nbd backend")
    if export is not None and (not isinstance(export, str) or not export):
        raise ValueError("invalid export name")
    return {
        "backend": "nbd",
        "socket": socket_path.strip(),
        "export": export,
        "export_bitmap": export_bitmap,
        "idle_timeout_seconds": idle_sec,
    }


def safe_transfer_id(image_id: str) -> Optional[str]:
    """
    Only allow a single filename component to avoid path traversal.
    Rejects anything containing '/' or '\\'.
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


class TransferRegistry:
    """
    Thread-safe in-memory registry for active image transfer configurations.

    The cloudstack-agent registers/unregisters transfers via the Unix domain
    control socket.  The HTTP handler looks up configs through get().

    Each transfer may specify idle_timeout_seconds (default DEFAULT_IDLE_TIMEOUT_SECONDS).
    After no in-flight HTTP requests have completed for that idle period, the transfer
    is removed (same effect as unregister).
    """

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._transfers: Dict[str, Dict[str, Any]] = {}
        self._last_activity: Dict[str, float] = {}
        self._inflight: Dict[str, int] = {}

    def register(self, transfer_id: str, config: Dict[str, Any]) -> bool:
        safe_id = safe_transfer_id(transfer_id)
        if safe_id is None:
            logging.error("register rejected invalid transfer_id=%r", transfer_id)
            return False
        with self._lock:
            self._transfers[safe_id] = config
            self._last_activity[safe_id] = time.monotonic()
            self._inflight.pop(safe_id, None)
            logging.info("registered transfer_id=%s active=%d", safe_id, len(self._transfers))
            return True

    def unregister(self, transfer_id: str) -> int:
        """Remove a transfer and return the number of remaining active transfers."""
        safe_id = safe_transfer_id(transfer_id)
        if safe_id is None:
            logging.error("unregister rejected invalid transfer_id=%r", transfer_id)
            with self._lock:
                return len(self._transfers)
        with self._lock:
            self._transfers.pop(safe_id, None)
            self._last_activity.pop(safe_id, None)
            self._inflight.pop(safe_id, None)
            remaining = len(self._transfers)
            logging.info("unregistered transfer_id=%s active=%d", safe_id, remaining)
            return remaining

    def get(self, transfer_id: str) -> Optional[Dict[str, Any]]:
        safe_id = safe_transfer_id(transfer_id)
        if safe_id is None:
            return None
        with self._lock:
            return self._transfers.get(safe_id)

    def active_count(self) -> int:
        with self._lock:
            return len(self._transfers)

    @contextmanager
    def request_lifecycle(self, transfer_id: str) -> Iterator[None]:
        """
        Track an HTTP request for idle-timeout purposes.

        Expiry is based on time since the last request *completed* (all in-flight
        work for this transfer_id finished). Transfers with active requests are
        never expired.
        """
        safe_id = safe_transfer_id(transfer_id)
        if safe_id is None:
            yield
            return
        with self._lock:
            if safe_id not in self._transfers:
                yield
                return
            self._inflight[safe_id] = self._inflight.get(safe_id, 0) + 1
        try:
            yield
        finally:
            now = time.monotonic()
            with self._lock:
                count = self._inflight.get(safe_id, 1) - 1
                if count <= 0:
                    self._inflight.pop(safe_id, None)
                    if safe_id in self._transfers:
                        self._last_activity[safe_id] = now
                else:
                    self._inflight[safe_id] = count

    def sweep_expired_transfers(self) -> None:
        """Remove transfers that exceeded idle_timeout_seconds with no in-flight HTTP work."""
        now = time.monotonic()
        with self._lock:
            expired: List[str] = []
            for tid, cfg in list(self._transfers.items()):
                if self._inflight.get(tid, 0) > 0:
                    continue
                timeout = int(cfg.get("idle_timeout_seconds", DEFAULT_IDLE_TIMEOUT_SECONDS))
                last = self._last_activity.get(tid, now)
                if now - last >= timeout:
                    expired.append(tid)
            for tid in expired:
                self._transfers.pop(tid, None)
                self._last_activity.pop(tid, None)
                self._inflight.pop(tid, None)
                logging.info(
                    "idle expiry: unregistered transfer_id=%s active=%d",
                    tid,
                    len(self._transfers),
                )
