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
from typing import Any, Dict, List, Set, Tuple


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

    def lookup(
        extents: List[Tuple[int, int, bool]], offset: int, default: bool
    ) -> bool:
        for start, length, flag in extents:
            if start <= offset < start + length:
                return flag
        return default

    result: List[Dict[str, Any]] = []
    for i in range(len(sorted_boundaries) - 1):
        a, b = sorted_boundaries[i], sorted_boundaries[i + 1]
        if a >= b:
            continue
        result.append(
            {
                "start": a,
                "length": b - a,
                "dirty": lookup(dirty_extents, a, False),
                "zero": lookup(allocation_extents, a, False),
            }
        )
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
