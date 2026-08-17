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

"""
Tests for NBD block status entry parsing.
"""

import os
import shutil
import subprocess
import tempfile
import unittest

from imageserver.backends.nbd import NbdConnection, _entries_to_pairs
from imageserver.util import is_fallback_dirty_response

class TestRealLibnbdBlockStatus(unittest.TestCase):
    """
    Creates a 1 MiB qcow2 with a persistent dirty bitmap, dirties two
    64 KiB regions, exports it via qemu-nbd --bitmap, and asserts both the
    raw callback entry format and the parsed extents.  Fails loudly if a
    new libnbd version changes the block status response.
    """

    REAL_BITMAP = "bm0"
    KB64 = 65536
    REAL_SIZE = 16 * KB64  # 1 MiB

    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.mkdtemp(prefix="nbd_entries_test_")
        cls._img = os.path.join(cls._tmp, "img.qcow2")
        cls._sock = os.path.join(cls._tmp, "nbd.sock")
        cls._proc = None

        def run(*cmd):
            subprocess.run(cmd, check=True, capture_output=True)

        try:
            run("qemu-img", "create", "-f", "qcow2", cls._img, str(cls.REAL_SIZE))
            # allocate 0-192 KiB before the bitmap exists (clean, allocated)
            run("qemu-io", "-f", "qcow2", "-c", "write -P 0xaa 0 192k", cls._img)
            run("qemu-img", "bitmap", "--add", cls._img, cls.REAL_BITMAP)
            # dirty two 64 KiB regions while the bitmap is recording
            run("qemu-io", "-f", "qcow2", "-c", "write -P 0xbb 128k 64k", cls._img)
            run("qemu-io", "-f", "qcow2", "-c", "write -P 0xcc 512k 64k", cls._img)

            cls._proc = subprocess.Popen(
                [
                    "qemu-nbd",
                    "--socket", cls._sock,
                    "--format", "qcow2",
                    "--persistent",
                    "--shared=0",
                    "--read-only",
                    f"--bitmap={cls.REAL_BITMAP}",
                    cls._img,
                ],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            from .test_base import _wait_for_nbd_socket

            _wait_for_nbd_socket(cls._sock)
        except BaseException:
            cls._teardown()
            raise

    @classmethod
    def tearDownClass(cls):
        cls._teardown()

    @classmethod
    def _teardown(cls):
        if cls._proc is not None:
            for pipe in (cls._proc.stdout, cls._proc.stderr):
                if pipe:
                    pipe.close()
            cls._proc.terminate()
            try:
                cls._proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                cls._proc.kill()
                cls._proc.wait(timeout=5)
            cls._proc = None
        shutil.rmtree(cls._tmp, ignore_errors=True)

    def _connect(self):
        return NbdConnection(
            self._sock,
            None,
            need_block_status=True,
            extra_meta_contexts=[f"qemu:dirty-bitmap:{self.REAL_BITMAP}"],
        )

    def test_raw_entries_format_is_known(self):
        """The raw callback entries must be flat ints or (length, flags) pairs."""
        with self._connect() as conn:
            size = conn.size()
            captured = []

            def cb(*args, **kwargs):
                self.assertGreaterEqual(len(args), 3)
                captured.append((args[0], list(args[2])))
                return 0

            fn = getattr(
                conn._nbd,
                "block_status_64",
                getattr(conn._nbd, "block_status", None),
            )
            self.assertIsNotNone(
                fn, "libnbd no longer exposes block_status/block_status_64"
            )
            fn(size, 0, cb)

        self.assertTrue(captured, "block status delivered no callbacks")
        contexts = {ctx for ctx, _ in captured}
        self.assertIn("base:allocation", contexts)
        self.assertIn(f"qemu:dirty-bitmap:{self.REAL_BITMAP}", contexts)

        for ctx, entries in captured:
            self.assertTrue(entries, f"empty entries for context {ctx}")
            if isinstance(entries[0], (tuple, list)):
                for item in entries:
                    self.assertIsInstance(item, (tuple, list))
                    self.assertEqual(len(item), 2)
                    self.assertIsInstance(item[0], int)
                    self.assertIsInstance(item[1], int)
            else:
                self.assertTrue(all(isinstance(v, int) for v in entries))
                self.assertEqual(len(entries) % 2, 0)
            # entries must parse and tile part of the queried range
            pairs = _entries_to_pairs(entries)
            self.assertTrue(pairs)
            covered = sum(length for length, _ in pairs)
            self.assertGreater(covered, 0)
            self.assertLessEqual(covered, size)

    def test_allocation_extents_tile_image(self):
        with self._connect() as conn:
            size = conn.size()
            extents = conn.get_allocation_extents()
        self.assertEqual(size, self.REAL_SIZE)
        pos = 0
        for e in extents:
            self.assertEqual(e["start"], pos)
            self.assertGreater(e["length"], 0)
            pos += e["length"]
        self.assertEqual(pos, size)
        self.assertEqual(
            extents,
            [
                {"start": 0, "length": 3 * self.KB64, "zero": False},
                {"start": 3 * self.KB64, "length": 5 * self.KB64, "zero": True},
                {"start": 8 * self.KB64, "length": self.KB64, "zero": False},
                {"start": 9 * self.KB64, "length": 7 * self.KB64, "zero": True},
            ],
        )

    def test_dirty_extents_match_writes(self):
        with self._connect() as conn:
            extents = conn.get_extents_dirty_and_zero(
                f"qemu:dirty-bitmap:{self.REAL_BITMAP}"
            )
        self.assertEqual(
            extents,
            [
                {"start": 0, "length": 2 * self.KB64, "dirty": False, "zero": False},
                {"start": 2 * self.KB64, "length": self.KB64, "dirty": True, "zero": False},
                {"start": 3 * self.KB64, "length": 5 * self.KB64, "dirty": False, "zero": True},
                {"start": 8 * self.KB64, "length": self.KB64, "dirty": True, "zero": False},
                {"start": 9 * self.KB64, "length": 7 * self.KB64, "dirty": False, "zero": True},
            ],
        )
        self.assertFalse(is_fallback_dirty_response(extents))


if __name__ == "__main__":
    unittest.main()
