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
from typing import Any, Callable, Dict, List, Set, Tuple


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


def merge_dirty_zero_extents(
    allocation_extents: List[Tuple[int, int, bool]],
    dirty_extents: List[Tuple[int, int, bool]],
    size: int,
) -> List[Dict[str, Any]]:
    """
    Merge allocation (start, length, zero) and dirty (start, length, dirty) extents
    into a single list of {start, length, dirty, zero} with unified boundaries.
    """
    boundaries: Set[int] = {0, size}
    for start, length, _ in allocation_extents:
        boundaries.add(start)
        boundaries.add(start + length)
    for start, length, _ in dirty_extents:
        boundaries.add(start)
        boundaries.add(start + length)
    sorted_boundaries = sorted(boundaries)

    def make_lookup(
        extents: List[Tuple[int, int, bool]], default: bool
    ) -> Callable[[int], bool]:
        # offsets are looked up in ascending order, so one pass over the sorted extents is enough
        ordered = sorted(extents, key=lambda e: e[0])
        pos = 0

        def lookup(offset: int) -> bool:
            nonlocal pos
            while pos < len(ordered) and ordered[pos][0] + ordered[pos][1] <= offset:
                pos += 1
            if pos < len(ordered) and ordered[pos][0] <= offset:
                return ordered[pos][2]
            return default

        return lookup

    dirty_at = make_lookup(dirty_extents, False)
    zero_at = make_lookup(allocation_extents, False)
    result: List[Dict[str, Any]] = []
    for i in range(len(sorted_boundaries) - 1):
        a, b = sorted_boundaries[i], sorted_boundaries[i + 1]
        if a >= b:
            continue
        result.append(
            {
                "start": a,
                "length": b - a,
                "dirty": dirty_at(a),
                "zero": zero_at(a),
            }
        )
    return coalesce_dirty_zero_extents(result)


def _is_byte_count(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and value >= 0


def read_extents_file(path: str, size: int) -> List[Dict[str, Any]]:
    """
    Read the extents a storage listed for an export: a JSON list of objects with "start" and "length"
    in bytes and the optional flags "dirty" (changed since the last backup) and "zero" (reads as
    zeros), like the extents of the ovirt-imageio API. An extent without a flag counts as dirty and
    as data, and so does a range the list leaves out. Returns extents with both flags that cover
    0..size without gaps, clipped to size. Raises ValueError if the content is not such a list or
    extents overlap.
    """
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise ValueError(f"{path}: not a list of extents")
    listed: List[Tuple[int, int, bool, bool]] = []
    for e in data:
        if (
            not isinstance(e, dict)
            or not _is_byte_count(e.get("start"))
            or not _is_byte_count(e.get("length"))
            or not all(isinstance(e.get(flag, False), bool) for flag in ("dirty", "zero"))
        ):
            raise ValueError(f"{path}: invalid extent {e!r}")
        listed.append((e["start"], e["length"], e.get("dirty", True), e.get("zero", False)))
    listed.sort()

    result: List[Dict[str, Any]] = []
    pos = 0
    for start, length, dirty, zero in listed:
        if start < pos:
            raise ValueError(f"{path}: overlapping extent at {start}")
        if start >= size:
            break
        if start > pos:
            result.append({"start": pos, "length": start - pos, "dirty": True, "zero": False})
        end = min(start + length, size)
        if end > start:
            result.append({"start": start, "length": end - start, "dirty": dirty, "zero": zero})
        pos = start + length
    if pos < size:
        result.append({"start": pos, "length": size - pos, "dirty": True, "zero": False})
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
