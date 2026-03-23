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

import json
import logging
import os
import threading
from typing import Any, Dict, Optional, Tuple

from .constants import CFG_DIR


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


class TransferConfigLoader:
    """
    Loads and caches per-image transfer configuration from JSON files.

    CloudStack writes a JSON file at <cfg_dir>/<transferId> with:
      - NBD backend: {"backend": "nbd", "socket": "...", "export": "vda", "export_bitmap": "..."}
      - File backend: {"backend": "file", "file": "/path/to/image.qcow2"}
    """

    def __init__(self, cfg_dir: str = CFG_DIR):
        self._cfg_dir = cfg_dir
        self._cache: Dict[str, Tuple[float, Dict[str, Any]]] = {}
        self._cache_guard = threading.Lock()

    @property
    def cfg_dir(self) -> str:
        return self._cfg_dir

    def load(self, image_id: str) -> Optional[Dict[str, Any]]:
        safe_id = safe_transfer_id(image_id)
        if safe_id is None:
            return None

        cfg_path = os.path.join(self._cfg_dir, safe_id)
        try:
            st = os.stat(cfg_path)
        except FileNotFoundError:
            return None
        except OSError as e:
            logging.error("cfg stat failed image_id=%s err=%r", image_id, e)
            return None

        with self._cache_guard:
            cached = self._cache.get(safe_id)
            if cached is not None:
                cached_mtime, cached_cfg = cached
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
            cfg: Dict[str, Any] = {"backend": "file", "file": file_path.strip()}
        else:
            socket_path = obj.get("socket")
            export = obj.get("export")
            export_bitmap = obj.get("export_bitmap")
            if not isinstance(socket_path, str) or not socket_path.strip():
                logging.error("cfg missing/invalid socket path for nbd backend image_id=%s", image_id)
                return None
            socket_path = socket_path.strip()
            if export is not None and (not isinstance(export, str) or not export):
                logging.error("cfg missing/invalid export image_id=%s", image_id)
                return None
            cfg = {
                "backend": "nbd",
                "socket": socket_path,
                "export": export,
                "export_bitmap": export_bitmap,
            }

        with self._cache_guard:
            self._cache[safe_id] = (float(st.st_mtime), cfg)
        return cfg
