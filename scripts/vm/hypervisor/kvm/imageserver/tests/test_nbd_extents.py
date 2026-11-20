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
Unit tests for building dirty/zero extents from NBD block status, using a fake
libnbd handle (no qemu-nbd needed).
"""

import random
import unittest
from unittest import mock

from imageserver.backends import nbd as nbd_backend
from imageserver.backends.nbd import NbdConnection
from imageserver.util import merge_dirty_zero_extents

KiB = 1024
CTX = "qemu:dirty-bitmap:ckp-1"


class FakeNbd:
    """
    Minimal stand-in for an nbd.NBD handle. *layout* maps a context name to
    (length, flags) runs that tile the image; replies are cut at the request.
    """

    def __init__(self, layout):
        self.layout = layout
        self.size = sum(length for length, _ in layout["base:allocation"])
        self.calls = 0

    def get_size(self):
        return self.size

    def can_meta_context(self, ctx):
        return ctx in self.layout

    def block_status_64(self, count, offset, cb):
        self.calls += 1
        end = offset + count
        for ctx, runs in self.layout.items():
            entries = []
            pos = 0
            for length, flags in runs:
                run_end = pos + length
                if run_end > offset and pos < end:
                    entries.append((min(run_end, end) - max(pos, offset), flags))
                pos = run_end
            cb(ctx, offset, entries, 0)


def make_conn(fake):
    conn = NbdConnection.__new__(NbdConnection)
    conn._nbd = fake
    return conn


def whole_disk_merge(layout, size):
    """The extents as one merge over the whole image computes them."""
    def tiles(ctx, bit):
        out, pos = [], 0
        for length, flags in layout[ctx]:
            out.append((pos, length, (flags & bit) != 0))
            pos += length
        return out
    return merge_dirty_zero_extents(tiles("base:allocation", 3), tiles(CTX, 1), size)


def random_runs(rng, size, unit, flag_values):
    runs, pos = [], 0
    while pos < size:
        length = min(rng.randint(1, 8) * unit, size - pos)
        runs.append((length, rng.choice(flag_values)))
        pos += length
    return runs


class TestDirtyExtentsPerStep(unittest.TestCase):
    def test_same_extents_as_one_merge_over_the_whole_image(self):
        rng = random.Random(4321)
        size = 1024 * 64 * KiB
        layout = {
            "base:allocation": random_runs(rng, size, 64 * KiB, [0, 3]),
            CTX: random_runs(rng, size, 32 * KiB, [0, 1]),
        }
        fake = FakeNbd(layout)
        with mock.patch.object(nbd_backend, "NBD_BLOCK_STATUS_CHUNK", 5 * 1024 * KiB):
            extents = make_conn(fake).get_extents_dirty_and_zero(CTX)

        self.assertGreater(fake.calls, 10)
        self.assertEqual(extents, whole_disk_merge(layout, size))

    def test_extent_across_steps_is_joined(self):
        layout = {"base:allocation": [(4 * 1024 * KiB, 0)], CTX: [(1024 * KiB, 0), (3 * 1024 * KiB, 1)]}
        with mock.patch.object(nbd_backend, "NBD_BLOCK_STATUS_CHUNK", 1024 * KiB):
            extents = make_conn(FakeNbd(layout)).get_extents_dirty_and_zero(CTX)

        self.assertEqual(extents, [
            {"start": 0, "length": 1024 * KiB, "dirty": False, "zero": False},
            {"start": 1024 * KiB, "length": 3 * 1024 * KiB, "dirty": True, "zero": False},
        ])


class TestMergeWindow(unittest.TestCase):
    def test_merge_covers_only_the_window(self):
        extents = merge_dirty_zero_extents([(100, 50, False)], [(100, 50, True)], 150, start=100)
        self.assertEqual(extents, [{"start": 100, "length": 50, "dirty": True, "zero": False}])


if __name__ == "__main__":
    unittest.main()
