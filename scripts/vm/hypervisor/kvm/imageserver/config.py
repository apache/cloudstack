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
from typing import Any, Dict, Optional


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
    """

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._transfers: Dict[str, Dict[str, Any]] = {}

    def register(self, transfer_id: str, config: Dict[str, Any]) -> bool:
        safe_id = safe_transfer_id(transfer_id)
        if safe_id is None:
            logging.error("register rejected invalid transfer_id=%r", transfer_id)
            return False
        with self._lock:
            self._transfers[safe_id] = config
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
