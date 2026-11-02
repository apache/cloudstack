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

"""Tests for the Unix domain control socket protocol (register / unregister / status)."""

import json
import socket
import unittest
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

from .test_base import ImageServerTestCase, make_tmp_image, shutdown_image_server, test_timeout


class TestStatus(ImageServerTestCase):
    def test_status_returns_ok(self):
        resp = self.ctrl({"action": "status"})
        self.assertEqual(resp["status"], "ok")
        self.assertIn("active_transfers", resp)

    def test_status_count_is_integer(self):
        resp = self.ctrl({"action": "status"})
        self.assertIsInstance(resp["active_transfers"], int)
        self.assertGreaterEqual(resp["active_transfers"], 0)


class TestRegister(ImageServerTestCase):
    def test_register_file_backend(self):
        img = make_tmp_image()
        tid = f"test-{uuid.uuid4().hex[:8]}"
        try:
            resp = self.ctrl({
                "action": "register",
                "transfer_id": tid,
                "config": {"backend": "file", "file": img},
            })
            self.assertEqual(resp["status"], "ok")
            self.assertGreaterEqual(resp["active_transfers"], 1)
        finally:
            self.ctrl({"action": "unregister", "transfer_id": tid})

    def test_register_nbd_backend(self):
        tid = f"test-{uuid.uuid4().hex[:8]}"
        try:
            resp = self.ctrl({
                "action": "register",
                "transfer_id": tid,
                "config": {"backend": "nbd", "socket": "/tmp/fake.sock"},
            })
            self.assertEqual(resp["status"], "ok")
        finally:
            self.ctrl({"action": "unregister", "transfer_id": tid})

    def test_register_increments_active_count(self):
        img = make_tmp_image()
        before = self.ctrl({"action": "status"})["active_transfers"]
        tid = f"test-{uuid.uuid4().hex[:8]}"
        try:
            self.ctrl({
                "action": "register",
                "transfer_id": tid,
                "config": {"backend": "file", "file": img},
            })
            after = self.ctrl({"action": "status"})["active_transfers"]
            self.assertEqual(after, before + 1)
        finally:
            self.ctrl({"action": "unregister", "transfer_id": tid})

    def test_register_missing_transfer_id(self):
        img = make_tmp_image()
        resp = self.ctrl({
            "action": "register",
            "config": {"backend": "file", "file": img},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_empty_transfer_id(self):
        img = make_tmp_image()
        resp = self.ctrl({
            "action": "register",
            "transfer_id": "",
            "config": {"backend": "file", "file": img},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_missing_config(self):
        resp = self.ctrl({
            "action": "register",
            "transfer_id": f"test-{uuid.uuid4().hex[:8]}",
        })
        self.assertEqual(resp["status"], "error")

    def test_register_invalid_backend(self):
        resp = self.ctrl({
            "action": "register",
            "transfer_id": f"test-{uuid.uuid4().hex[:8]}",
            "config": {"backend": "invalid"},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_file_missing_path(self):
        resp = self.ctrl({
            "action": "register",
            "transfer_id": f"test-{uuid.uuid4().hex[:8]}",
            "config": {"backend": "file"},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_nbd_missing_socket(self):
        resp = self.ctrl({
            "action": "register",
            "transfer_id": f"test-{uuid.uuid4().hex[:8]}",
            "config": {"backend": "nbd"},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_path_traversal_rejected(self):
        img = make_tmp_image()
        resp = self.ctrl({
            "action": "register",
            "transfer_id": "../etc/passwd",
            "config": {"backend": "file", "file": img},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_dot_rejected(self):
        img = make_tmp_image()
        resp = self.ctrl({
            "action": "register",
            "transfer_id": ".",
            "config": {"backend": "file", "file": img},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_slash_rejected(self):
        img = make_tmp_image()
        resp = self.ctrl({
            "action": "register",
            "transfer_id": "a/b",
            "config": {"backend": "file", "file": img},
        })
        self.assertEqual(resp["status"], "error")

    def test_register_duplicate_replaces(self):
        img = make_tmp_image()
        tid = f"test-{uuid.uuid4().hex[:8]}"
        try:
            self.ctrl({
                "action": "register",
                "transfer_id": tid,
                "config": {"backend": "file", "file": img},
            })
            count_before = self.ctrl({"action": "status"})["active_transfers"]
            self.ctrl({
                "action": "register",
                "transfer_id": tid,
                "config": {"backend": "file", "file": img},
            })
            count_after = self.ctrl({"action": "status"})["active_transfers"]
            self.assertEqual(count_after, count_before)
        finally:
            self.ctrl({"action": "unregister", "transfer_id": tid})


class TestUnregister(ImageServerTestCase):
    def test_unregister_existing(self):
        img = make_tmp_image()
        tid = f"test-{uuid.uuid4().hex[:8]}"
        self.ctrl({
            "action": "register",
            "transfer_id": tid,
            "config": {"backend": "file", "file": img},
        })
        before = self.ctrl({"action": "status"})["active_transfers"]
        resp = self.ctrl({"action": "unregister", "transfer_id": tid})
        self.assertEqual(resp["status"], "ok")
        self.assertEqual(resp["active_transfers"], before - 1)

    def test_unregister_nonexistent(self):
        resp = self.ctrl({"action": "unregister", "transfer_id": "does-not-exist"})
        self.assertEqual(resp["status"], "ok")

    def test_unregister_missing_id(self):
        resp = self.ctrl({"action": "unregister"})
        self.assertEqual(resp["status"], "error")


class TestUnknownAction(ImageServerTestCase):
    def test_unknown_action(self):
        resp = self.ctrl({"action": "foobar"})
        self.assertEqual(resp["status"], "error")
        self.assertIn("unknown", resp.get("message", "").lower())


class TestMalformed(ImageServerTestCase):
    def test_malformed_json(self):
        sock_path = self.server["ctrl_sock"]
        payload = b"not valid json\n"
        with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as s:
            s.settimeout(5)
            s.connect(sock_path)
            s.sendall(payload)
            s.shutdown(socket.SHUT_WR)
            data = b""
            while True:
                chunk = s.recv(4096)
                if not chunk:
                    break
                data += chunk
        resp = json.loads(data.strip())
        self.assertEqual(resp["status"], "error")


class TestConcurrentRegistrations(ImageServerTestCase):
    @test_timeout(60)
    def test_concurrent_registers(self):
        img = make_tmp_image()
        tids = [f"conc-{uuid.uuid4().hex[:8]}" for _ in range(20)]
        results = []

        def register_one(tid):
            return self.ctrl({
                "action": "register",
                "transfer_id": tid,
                "config": {"backend": "file", "file": img},
            })

        try:
            with ThreadPoolExecutor(max_workers=10) as pool:
                futures = {pool.submit(register_one, tid): tid for tid in tids}
                for f in as_completed(futures, timeout=30):
                    results.append(f.result())

            self.assertTrue(all(r["status"] == "ok" for r in results))
        finally:
            for tid in tids:
                self.ctrl({"action": "unregister", "transfer_id": tid})


if __name__ == "__main__":
    try:
        unittest.main()
    finally:
        shutdown_image_server()
