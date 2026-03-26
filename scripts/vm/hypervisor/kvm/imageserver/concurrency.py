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

import threading
from typing import Dict, NamedTuple


class _ImageState(NamedTuple):
    read_sem: threading.Semaphore
    write_sem: threading.Semaphore
    lock: threading.Lock


class ConcurrencyManager:
    """
    Manages per-image read/write semaphores and per-image mutual-exclusion locks.

    Each image_id gets its own independent pool of read slots (default MAX_PARALLEL_READS)
    and write slots (default MAX_PARALLEL_WRITES), so concurrent transfers to different images
    do not contend with each other.

    The per-image lock serialises operations that must not overlap on the
    same image (e.g. flush while writing, extents while writing).
    """

    def __init__(self, max_reads: int, max_writes: int):
        self._max_reads = max_reads
        self._max_writes = max_writes
        self._images: Dict[str, _ImageState] = {}
        self._guard = threading.Lock()

    def _state_for(self, image_id: str) -> _ImageState:
        with self._guard:
            state = self._images.get(image_id)
            if state is None:
                state = _ImageState(
                    read_sem=threading.Semaphore(self._max_reads),
                    write_sem=threading.Semaphore(self._max_writes),
                    lock=threading.Lock(),
                )
                self._images[image_id] = state
            return state

    def acquire_read(self, image_id: str, blocking: bool = False) -> bool:
        return self._state_for(image_id).read_sem.acquire(blocking=blocking)

    def release_read(self, image_id: str) -> None:
        self._state_for(image_id).read_sem.release()

    def acquire_write(self, image_id: str, blocking: bool = False) -> bool:
        return self._state_for(image_id).write_sem.acquire(blocking=blocking)

    def release_write(self, image_id: str) -> None:
        self._state_for(image_id).write_sem.release()

    def get_image_lock(self, image_id: str) -> threading.Lock:
        return self._state_for(image_id).lock
