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

"""Unit tests for imageserver.util extent coalescing helpers."""

import json
import os
import tempfile
import unittest

from imageserver.util import (
    coalesce_allocation_extents,
    coalesce_dirty_zero_extents,
    merge_dirty_zero_extents,
    read_extents_file,
)


class TestCoalesceAllocationExtents(unittest.TestCase):
    def test_empty(self):
        self.assertEqual(coalesce_allocation_extents([]), [])

    def test_single(self):
        inp = [{"start": 0, "length": 4096, "zero": False}]
        out = coalesce_allocation_extents(inp)
        self.assertEqual(out, [{"start": 0, "length": 4096, "zero": False}])
        self.assertIsNot(out[0], inp[0])

    def test_merges_contiguous_same_zero(self):
        inp = [
            {"start": 0, "length": 10, "zero": False},
            {"start": 10, "length": 5, "zero": False},
            {"start": 15, "length": 100, "zero": False},
        ]
        self.assertEqual(
            coalesce_allocation_extents(inp),
            [{"start": 0, "length": 115, "zero": False}],
        )

    def test_does_not_merge_different_zero(self):
        inp = [
            {"start": 0, "length": 64, "zero": False},
            {"start": 64, "length": 64, "zero": True},
            {"start": 128, "length": 64, "zero": False},
        ]
        self.assertEqual(coalesce_allocation_extents(inp), inp)

    def test_does_not_merge_gap(self):
        inp = [
            {"start": 0, "length": 100, "zero": False},
            {"start": 200, "length": 50, "zero": False},
        ]
        self.assertEqual(coalesce_allocation_extents(inp), inp)

    def test_does_not_merge_same_zero_with_gap(self):
        inp = [
            {"start": 0, "length": 10, "zero": True},
            {"start": 20, "length": 10, "zero": True},
        ]
        self.assertEqual(coalesce_allocation_extents(inp), inp)


class TestCoalesceDirtyZeroExtents(unittest.TestCase):
    def test_empty(self):
        self.assertEqual(coalesce_dirty_zero_extents([]), [])

    def test_single(self):
        inp = [{"start": 0, "length": 8192, "dirty": True, "zero": False}]
        out = coalesce_dirty_zero_extents(inp)
        self.assertEqual(
            out, [{"start": 0, "length": 8192, "dirty": True, "zero": False}]
        )

    def test_merges_contiguous_same_flags(self):
        inp = [
            {"start": 0, "length": 50, "dirty": True, "zero": False},
            {"start": 50, "length": 50, "dirty": True, "zero": False},
        ]
        self.assertEqual(
            coalesce_dirty_zero_extents(inp),
            [{"start": 0, "length": 100, "dirty": True, "zero": False}],
        )

    def test_does_not_merge_differing_dirty(self):
        inp = [
            {"start": 0, "length": 32, "dirty": False, "zero": False},
            {"start": 32, "length": 32, "dirty": True, "zero": False},
        ]
        self.assertEqual(coalesce_dirty_zero_extents(inp), inp)

    def test_does_not_merge_differing_zero(self):
        inp = [
            {"start": 0, "length": 16, "dirty": False, "zero": False},
            {"start": 16, "length": 16, "dirty": False, "zero": True},
        ]
        self.assertEqual(coalesce_dirty_zero_extents(inp), inp)


class TestMergeDirtyZeroExtentsCoalescing(unittest.TestCase):
    def test_coalesces_adjacent_identical_flags_after_boundary_merge(self):
        """Boundary grid can split one logical run; coalesce should reunite."""
        allocation = [(0, 200, False)]
        dirty = [(0, 100, False), (100, 100, False)]
        merged = merge_dirty_zero_extents(allocation, dirty, 200)
        self.assertEqual(
            merged,
            [{"start": 0, "length": 200, "dirty": False, "zero": False}],
        )

    def test_unsorted_dirty_extents(self):
        allocation = [(0, 100, False), (100, 100, True)]
        dirty = [(150, 10, True), (20, 30, True)]
        merged = merge_dirty_zero_extents(allocation, dirty, 200)
        self.assertEqual(
            merged,
            [
                {"start": 0, "length": 20, "dirty": False, "zero": False},
                {"start": 20, "length": 30, "dirty": True, "zero": False},
                {"start": 50, "length": 50, "dirty": False, "zero": False},
                {"start": 100, "length": 50, "dirty": False, "zero": True},
                {"start": 150, "length": 10, "dirty": True, "zero": True},
                {"start": 160, "length": 40, "dirty": False, "zero": True},
            ],
        )

    def test_many_dirty_extents(self):
        size = 4096 * 200000
        allocation = [(0, size, False)]
        dirty = [(i * 8192, 4096, True) for i in range(size // 8192)]
        merged = merge_dirty_zero_extents(allocation, dirty, size)
        self.assertEqual(len(merged), size // 4096)
        self.assertEqual(sum(e["length"] for e in merged if e["dirty"]), size // 2)


class TestReadExtentsFile(unittest.TestCase):
    def _read(self, content, size):
        fd, path = tempfile.mkstemp()
        self.addCleanup(os.unlink, path)
        with os.fdopen(fd, "w") as f:
            f.write(content if isinstance(content, str) else json.dumps(content))
        return read_extents_file(path, size)

    @staticmethod
    def _extent(start, length, dirty, zero):
        return {"start": start, "length": length, "dirty": dirty, "zero": zero}

    def test_empty_list_is_all_dirty_data(self):
        self.assertEqual(self._read([], 1000), [self._extent(0, 1000, True, False)])

    def test_dirty_and_zero_flags(self):
        listed = [
            {"start": 0, "length": 100, "dirty": False, "zero": True},
            {"start": 100, "length": 50, "dirty": True, "zero": False},
            {"start": 150, "length": 850, "dirty": False, "zero": False},
        ]
        self.assertEqual(self._read(listed, 1000), [self._extent(**e) for e in listed])

    def test_missing_flags_count_as_dirty_data(self):
        # the extents of a full backup only know which ranges read as zeros
        self.assertEqual(
            self._read([{"start": 0, "length": 600, "zero": True}, {"start": 600, "length": 400, "zero": False}], 1000),
            [self._extent(0, 600, True, True), self._extent(600, 400, True, False)],
        )

    def test_sorts_joins_and_fills_gaps(self):
        listed = [
            {"start": 500, "length": 100, "dirty": False, "zero": True},
            {"start": 0, "length": 100, "dirty": False, "zero": True},
            {"start": 100, "length": 100, "dirty": False, "zero": True},
        ]
        self.assertEqual(
            self._read(listed, 1000),
            [
                self._extent(0, 200, False, True),
                self._extent(200, 300, True, False),
                self._extent(500, 100, False, True),
                self._extent(600, 400, True, False),
            ],
        )

    def test_clips_to_size(self):
        # the storage device can be larger than the volume
        listed = [
            {"start": 0, "length": 900, "dirty": False, "zero": False},
            {"start": 900, "length": 200, "dirty": True, "zero": False},
            {"start": 1100, "length": 4096, "dirty": False, "zero": True},
        ]
        self.assertEqual(
            self._read(listed, 1000),
            [self._extent(0, 900, False, False), self._extent(900, 100, True, False)],
        )

    def test_drops_empty(self):
        listed = [{"start": 0, "length": 0, "dirty": False, "zero": True}, {"start": 0, "length": 1000, "dirty": False}]
        self.assertEqual(self._read(listed, 1000), [self._extent(0, 1000, False, False)])

    def test_rejects_overlaps(self):
        with self.assertRaises(ValueError):
            self._read([{"start": 0, "length": 200}, {"start": 100, "length": 200}], 1000)

    def test_rejects_invalid(self):
        for content in (
            {"start": 0},
            [[0, 1]],
            [{"start": 0}],
            [{"start": 0, "length": -1}],
            [{"start": "0", "length": 1}],
            [{"start": 0, "length": True}],
            [{"start": 0, "length": 1.5}],
            [{"start": 0, "length": 1, "zero": 1}],
            [{"start": 0, "length": 1, "dirty": "yes"}],
            "not json",
        ):
            with self.assertRaises(ValueError, msg=repr(content)):
                self._read(content, 1000)


if __name__ == "__main__":
    unittest.main()
