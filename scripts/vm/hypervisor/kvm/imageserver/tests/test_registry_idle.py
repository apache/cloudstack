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

"""Unit tests for transfer idle timeout (no image server / nbd dependency)."""

import unittest
from unittest.mock import patch

from imageserver.config import (
    TransferRegistry,
    parse_idle_timeout_seconds,
    validate_transfer_config,
)
from imageserver.constants import DEFAULT_IDLE_TIMEOUT_SECONDS


class TestParseIdleTimeout(unittest.TestCase):
    def test_default_600(self):
        self.assertEqual(parse_idle_timeout_seconds({}), DEFAULT_IDLE_TIMEOUT_SECONDS)

    def test_explicit(self):
        self.assertEqual(
            parse_idle_timeout_seconds({"idle_timeout_seconds": 30}), 30
        )

    def test_zero_timeout(self):
        self.assertEqual(
            parse_idle_timeout_seconds({"idle_timeout_seconds": 0}), 86400
        )


class TestValidateTransferConfig(unittest.TestCase):
    def test_file_merges_idle(self):
        c = validate_transfer_config(
            {"backend": "file", "file": "/tmp/x", "idle_timeout_seconds": 3}
        )
        self.assertEqual(c["idle_timeout_seconds"], 3)
        self.assertEqual(c["backend"], "file")


class TestRegistryIdleSweep(unittest.TestCase):
    def test_sweep_unregisters_after_idle(self):
        clock = [0.0]

        def mono():
            return clock[0]

        with patch("imageserver.config.time.monotonic", mono):
            r = TransferRegistry()
            r.register(
                "t1",
                validate_transfer_config(
                    {"backend": "file", "file": "/x", "idle_timeout_seconds": 2}
                ),
            )
            clock[0] = 5.0
            r.sweep_expired_transfers()
            self.assertIsNone(r.get("t1"))

    def test_inflight_prevents_sweep_until_request_ends(self):
        clock = [0.0]

        def mono():
            return clock[0]

        with patch("imageserver.config.time.monotonic", mono):
            r = TransferRegistry()
            r.register(
                "t1",
                validate_transfer_config(
                    {"backend": "file", "file": "/x", "idle_timeout_seconds": 2}
                ),
            )
            clock[0] = 1.0
            ctx = r.request_lifecycle("t1")
            ctx.__enter__()
            clock[0] = 100.0
            r.sweep_expired_transfers()
            self.assertIsNotNone(r.get("t1"))
            ctx.__exit__(None, None, None)
            clock[0] = 103.0
            r.sweep_expired_transfers()
            self.assertIsNone(r.get("t1"))


if __name__ == "__main__":
    unittest.main()
