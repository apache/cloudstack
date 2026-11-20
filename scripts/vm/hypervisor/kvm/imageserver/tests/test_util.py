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

import time
import unittest

from imageserver.util import (
    coalesce_allocation_extents,
    coalesce_dirty_zero_extents,
    merge_dirty_zero_extents,
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


class TestMergeDirtyZeroExtents(unittest.TestCase):
    def test_unified_boundaries(self):
        allocation = [(0, 100, False), (100, 100, True)]
        dirty = [(0, 50, False), (50, 100, True), (150, 50, False)]
        self.assertEqual(
            merge_dirty_zero_extents(allocation, dirty, 200),
            [
                {"start": 0, "length": 50, "dirty": False, "zero": False},
                {"start": 50, "length": 50, "dirty": True, "zero": False},
                {"start": 100, "length": 50, "dirty": True, "zero": True},
                {"start": 150, "length": 50, "dirty": False, "zero": True},
            ],
        )

    def test_range_without_dirty_extent_is_reported_dirty(self):
        """A short dirty-bitmap reply must never hide changed data."""
        allocation = [(0, 128, False)]
        dirty = [(0, 64, False)]
        self.assertEqual(
            merge_dirty_zero_extents(allocation, dirty, 128),
            [
                {"start": 0, "length": 64, "dirty": False, "zero": False},
                {"start": 64, "length": 64, "dirty": True, "zero": False},
            ],
        )

    def test_range_without_allocation_extent_is_reported_as_data(self):
        allocation = [(0, 64, True)]
        dirty = [(0, 128, True)]
        self.assertEqual(
            merge_dirty_zero_extents(allocation, dirty, 128),
            [
                {"start": 0, "length": 64, "dirty": True, "zero": True},
                {"start": 64, "length": 64, "dirty": True, "zero": False},
            ],
        )

    def test_unsorted_overlapping_and_overshooting_extents(self):
        allocation = [(64, 100, True), (0, 80, False)]
        dirty = [(0, 300, False)]
        self.assertEqual(
            merge_dirty_zero_extents(allocation, dirty, 128),
            [
                {"start": 0, "length": 80, "dirty": False, "zero": False},
                {"start": 80, "length": 48, "dirty": False, "zero": True},
            ],
        )

    def test_unchanged_fully_allocated_disk(self):
        self.assertEqual(
            merge_dirty_zero_extents([(0, 4096, False)], [(0, 4096, False)], 4096),
            [{"start": 0, "length": 4096, "dirty": False, "zero": False}],
        )

    def test_many_extents_are_merged_in_linear_time(self):
        n = 200000
        allocation = [(i * 64, 64, i % 2 == 0) for i in range(n)]
        dirty = [(i * 64, 64, i % 3 == 0) for i in range(n)]
        start = time.monotonic()
        merged = merge_dirty_zero_extents(allocation, dirty, n * 64)
        elapsed = time.monotonic() - start
        self.assertEqual(sum(e["length"] for e in merged), n * 64)
        self.assertLess(elapsed, 10)


if __name__ == "__main__":
    unittest.main()
