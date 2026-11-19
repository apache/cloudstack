// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
// The GPU capacity heatmap (SPEC.md section 9).
//
// "Heatmap by zone and class: allocated, free, unhealthy, quarantined."
// The point of a heatmap is that an operator takes it in at a glance,
// so the status a cell gets is what decides its colour — and broken
// hardware has to win over a merely busy class, or a failing GPU hides
// behind a green cell.

// What CloudStack sends, which is not what this file used to read.
// A response field is serialised under its lower-cased name, so the
// engine's `gpus_total` arrives as `gpustotal` — and `allocated` and
// `unhealthy` are not sent at all. They are differences, derived here
// so the arithmetic lives in one place.
//
// Reading the wrong names was silent: every cell took `undefined` for
// its counts, which made utilisation 0 and the status `healthy`, so a
// zone with every GPU allocated and a dead node in it drew the same
// green cell as an idle one.
function countsOf (capacity) {
  const total = capacity.gpustotal ?? 0
  const free = capacity.gpusfree ?? 0
  const nodes = capacity.nodestotal ?? 0
  const healthy = capacity.nodeshealthy ?? 0

  return {
    total,
    free,
    allocated: Math.max(total - free, 0),
    unhealthy: Math.max(nodes - healthy, 0)
  }
}

/** Fraction of a class's GPUs that are allocated, 0 to 1. */
function utilisationOf (counts) {
  // A declared class whose hosts are not tagged yet has no GPUs at all.
  // Dividing by zero here puts NaN% on the operator's first screen.
  if (!counts.total) return 0
  return counts.allocated / counts.total
}

export function cellFor (capacity) {
  const counts = countsOf(capacity)
  const utilisation = utilisationOf(counts)

  let status = 'healthy'
  if (counts.unhealthy > 0) {
    // Outranks everything: a node in this state is not coming back
    // without a drain, and a quiet class should not hide it.
    status = 'unhealthy'
  } else if (counts.free === 0 && counts.total > 0) {
    status = 'exhausted'
  } else if (utilisation >= 0.9) {
    status = 'tight'
  }

  return {
    zone: capacity.zone,
    gpuclass: capacity.gpuclass,
    ...counts,
    utilisation,
    status
  }
}

/** One row per zone, one cell per GPU class, in a stable order. */
export function heatmapRows (capacities) {
  const zones = new Map()
  for (const capacity of capacities) {
    if (!zones.has(capacity.zone)) zones.set(capacity.zone, [])
    zones.get(capacity.zone).push(cellFor(capacity))
  }
  return [...zones.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([zone, cells]) => ({
      zone,
      cells: cells.sort((a, b) => a.gpuclass.localeCompare(b.gpuclass))
    }))
}
