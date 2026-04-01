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

"""Integration tests for per-transfer HTTP idle timeout (requires image server deps e.g. nbd)."""

import time
import urllib.error

from .test_base import (
    ImageServerTestCase,
    http_options,
    make_file_transfer,
)


class TestTransferIdleExpiry(ImageServerTestCase):
    def test_transfer_expires_after_idle(self):
        """No HTTP activity after registration: transfer is unregistered after idle_timeout_seconds."""
        _tid, url, _path, cleanup = make_file_transfer(idle_timeout_seconds=2)
        try:
            time.sleep(3.5)
            with self.assertRaises(urllib.error.HTTPError) as ctx:
                http_options(url)
            self.assertEqual(ctx.exception.code, 404)
            st = self.ctrl({"action": "status"})
            self.assertEqual(st.get("status"), "ok")
        finally:
            cleanup()

    def test_http_activity_resets_idle_deadline(self):
        """Completing a request resets the idle timer; transfer stays past a single interval."""
        _tid, url, _path, cleanup = make_file_transfer(idle_timeout_seconds=2)
        try:
            http_options(url)
            time.sleep(1.2)
            http_options(url)
            time.sleep(1.2)
            http_options(url)
            time.sleep(1.2)
            resp = http_options(url)
            self.assertEqual(resp.status, 200)
        finally:
            cleanup()
