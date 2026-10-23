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
import time
from typing import Any, Dict, List, Tuple


def coalesce_allocation_extents(
    extents: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Merge contiguous extents that share the same ``zero`` flag."""
    if not extents:
        return []
    out: List[Dict[str, Any]] = [dict(extents[0])]
    for e in extents[1:]:
        prev = out[-1]
        if (
            prev["start"] + prev["length"] == e["start"]
            and prev["zero"] == e["zero"]
        ):
            prev["length"] += e["length"]
        else:
            out.append({"start": e["start"], "length": e["length"], "zero": e["zero"]})
    return out


def coalesce_dirty_zero_extents(
    extents: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Merge contiguous extents that share the same ``dirty`` and ``zero`` flags."""
    if not extents:
        return []
    out: List[Dict[str, Any]] = [dict(extents[0])]
    for e in extents[1:]:
        prev = out[-1]
        if (
            prev["start"] + prev["length"] == e["start"]
            and prev["dirty"] == e["dirty"]
            and prev["zero"] == e["zero"]
        ):
            prev["length"] += e["length"]
        else:
            out.append(
                {
                    "start": e["start"],
                    "length": e["length"],
                    "dirty": e["dirty"],
                    "zero": e["zero"],
                }
            )
    return out


def json_bytes(obj: Any) -> bytes:
    return json.dumps(obj, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def _normalize_extents(
    extents: List[Tuple[int, int, bool]], size: int
) -> List[Tuple[int, int, bool]]:
    """
    Sort (start, length, flag) extents, clip them to [0, size) and drop
    overlaps (the earliest extent covering a byte wins).  Returns
    (start, end_exclusive, flag) tuples; gaps are left uncovered.
    """
    out: List[Tuple[int, int, bool]] = []
    pos = 0
    for start, length, flag in sorted(extents, key=lambda e: e[0]):
        begin = max(start, pos)
        end = min(start + length, size)
        if begin >= end:
            continue
        out.append((begin, end, flag))
        pos = end
    return out


def append_coalesced(
    result: List[Dict[str, Any]], extents: List[Dict[str, Any]]
) -> None:
    """
    Append dirty/zero *extents* to *result*, extending the last extent of
    *result* when the first new one continues it with the same flags.
    """
    for e in extents:
        if result:
            prev = result[-1]
            if (
                prev["start"] + prev["length"] == e["start"]
                and prev["dirty"] == e["dirty"]
                and prev["zero"] == e["zero"]
            ):
                prev["length"] += e["length"]
                continue
        result.append(e)


def merge_dirty_zero_extents(
    allocation_extents: List[Tuple[int, int, bool]],
    dirty_extents: List[Tuple[int, int, bool]],
    size: int,
    start: int = 0,
) -> List[Dict[str, Any]]:
    """
    Merge allocation (start, length, zero) and dirty (start, length, dirty) extents
    into a single list of {start, length, dirty, zero} with unified boundaries,
    covering [start, size).

    Ranges not covered by any dirty extent are reported dirty, and ranges not
    covered by any allocation extent are reported as data (zero=False), so a
    missing or short block status reply can never hide changed data.

    Runs in O(n log n + m log m) (sorting) plus a single linear sweep.
    """
    alloc = _normalize_extents(allocation_extents, size)
    dirty = _normalize_extents(dirty_extents, size)

    def flag_at(
        intervals: List[Tuple[int, int, bool]], idx: int, pos: int, default: bool
    ) -> Tuple[int, bool, int]:
        """Return (new_idx, flag at pos, end of that flag run)."""
        while idx < len(intervals) and intervals[idx][1] <= pos:
            idx += 1
        if idx < len(intervals):
            begin, end, flag = intervals[idx]
            if begin <= pos:
                return idx, flag, end
            return idx, default, begin
        return idx, default, size

    result: List[Dict[str, Any]] = []
    ai = di = 0
    pos = start
    while pos < size:
        ai, zero, alloc_end = flag_at(alloc, ai, pos, False)
        di, is_dirty, dirty_end = flag_at(dirty, di, pos, True)
        end = min(alloc_end, dirty_end)
        result.append({"start": pos, "length": end - pos, "dirty": is_dirty, "zero": zero})
        pos = end
    return coalesce_dirty_zero_extents(result)


def is_fallback_dirty_response(extents: List[Dict[str, Any]]) -> bool:
    """True if extents is the single-extent fallback (dirty=false, zero=false)."""
    return (
        len(extents) == 1
        and extents[0].get("dirty") is False
        and extents[0].get("zero") is False
    )


def now_s() -> float:
    return time.monotonic()
