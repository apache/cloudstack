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

import { cellFor, heatmapRows } from '@/utils/capacity'
import { engineReason } from '@/utils/errors'
import { describeParameter, sourceLabel } from '@/utils/profiles'
import { canProceed, groupChecks } from '@/utils/setup'

describe('the GPU capacity heatmap', () => {
  // What listAiGpuCapacity actually answers with. This fixture used to
  // say `total`, `allocated`, `free` and `unhealthy`, none of which
  // CloudStack sends — so every test agreed with the code and the live
  // screen drew "undefined / undefined allocated" in a green cell for
  // every class, whatever the hardware was doing. Allocated and
  // unhealthy are not sent at all; they are differences.
  const capacity = (over = {}) => ({
    zone: 'zone-east-1',
    gpuclass: 'h100-8x',
    gpustotal: 16,
    gpusfree: 8,
    nodestotal: 2,
    nodeshealthy: 2,
    ...over
  })

  it('reports utilisation as a fraction', () => {
    expect(cellFor(capacity()).utilisation).toBeCloseTo(0.5)
  })

  it('derives what is allocated, because the engine sends free and total', () => {
    expect(cellFor(capacity()).allocated).toBe(8)
  })

  it('counts a node that is not healthy as unhealthy capacity', () => {
    expect(cellFor(capacity({ nodeshealthy: 1 })).unhealthy).toBe(1)
  })

  it('a class with no GPUs is not an error, just empty', () => {
    // A declared class whose hosts are not tagged yet; dividing by zero
    // here would render NaN% in the operator's first screen.
    expect(cellFor(capacity({ gpustotal: 0, gpusfree: 0 })).utilisation).toBe(0)
  })

  it('full capacity reads as full', () => {
    expect(cellFor(capacity({ gpusfree: 0 })).utilisation).toBe(1)
  })

  it('any unhealthy GPU outranks utilisation in the status', () => {
    // An operator scanning the heatmap needs broken hardware to stand
    // out even when the class is otherwise quiet.
    const cell = cellFor(capacity({ gpusfree: 15, nodeshealthy: 1 }))
    expect(cell.status).toBe('unhealthy')
  })

  it('a full class is flagged before it is exhausted', () => {
    expect(cellFor(capacity({ gpusfree: 1 })).status).toBe('tight')
  })

  it('an exhausted class is flagged as such', () => {
    expect(cellFor(capacity({ gpusfree: 0 })).status).toBe('exhausted')
  })

  it('a quiet class is healthy', () => {
    expect(cellFor(capacity({ gpusfree: 14 })).status).toBe('healthy')
  })

  it('groups classes into one row per zone', () => {
    const rows = heatmapRows([
      capacity(),
      capacity({ gpuclass: 'l40s-1x' }),
      capacity({ zone: 'zone-west-1' })
    ])

    expect(rows.map((r) => r.zone)).toEqual(['zone-east-1', 'zone-west-1'])
    expect(rows[0].cells).toHaveLength(2)
  })

  it('has nothing to show for no capacity', () => {
    expect(heatmapRows([])).toEqual([])
  })
})

describe('effective parameters', () => {
  it('names the level a value came from', () => {
    // SPEC.md section 6: getAiEffectiveParameters reports both the
    // resolved value and its source level. A bare number is useless —
    // the question is always "why is it that".
    expect(sourceLabel('account')).toBe('Account')
    expect(sourceLabel('apikey')).toBe('API key')
  })

  it('falls back to the raw level rather than hiding it', () => {
    expect(sourceLabel('something-new')).toBe('something-new')
  })

  it('describes a resolved parameter', () => {
    const described = describeParameter({
      name: 'temperature', value: 0.2, source: 'domain', policy: 'locked'
    })

    expect(described.value).toBe(0.2)
    expect(described.source).toBe('Domain')
    expect(described.locked).toBe(true)
  })

  it('marks a clamped value as adjustable within bounds', () => {
    const described = describeParameter({
      name: 'temperature', value: 0.7, source: 'account', policy: 'clamp'
    })

    expect(described.locked).toBe(false)
    expect(described.editable).toBe(true)
  })

  it('a locked value cannot be edited', () => {
    const described = describeParameter({
      name: 'temperature', value: 0.2, source: 'domain', policy: 'locked'
    })

    expect(described.editable).toBe(false)
  })

  it('a default is editable', () => {
    expect(describeParameter({
      name: 'top_p', value: 0.9, source: 'platform', policy: 'default'
    }).editable).toBe(true)
  })
})

describe('the setup wizard', () => {
  const check = (status, over = {}) => ({
    name: 'model registry', status, detail: 'd', remedy: '', ...over
  })

  it('groups checks by status', () => {
    const grouped = groupChecks([check('pass'), check('fail'), check('pass')])

    expect(grouped.pass).toHaveLength(2)
    expect(grouped.fail).toHaveLength(1)
  })

  it('always has every group, so the UI need not guard', () => {
    const grouped = groupChecks([])

    expect(Object.keys(grouped).sort()).toEqual(['fail', 'pass', 'skipped', 'warn'])
  })

  it('may proceed when nothing failed', () => {
    expect(canProceed([check('pass'), check('warn')])).toBe(true)
  })

  it('may not proceed with a failure', () => {
    expect(canProceed([check('pass'), check('fail')])).toBe(false)
  })

  it('may proceed with skipped checks', () => {
    // A new zone has not been observed yet; that is the normal first
    // run, not a reason to block the wizard.
    expect(canProceed([check('skipped'), check('pass')])).toBe(true)
  })

  it('may proceed when there is nothing to check', () => {
    expect(canProceed([])).toBe(true)
  })
})

describe('the reason a screen could not load', () => {
  // Measured off the wire against a live management server, not
  // imagined: this is byte for byte what listAiGpuCapacity answers on
  // a zone whose capacity has never been observed.
  const asCloudStackRefuses = {
    message: 'Request failed with status code 534',
    response: {
      status: 534,
      data: {
        listaigpucapacityresponse: {
          uuidList: [],
          errorcode: 534,
          cserrorcode: 9999,
          errortext: 'the AI Services engine returned 503 for /v1/capacity: capacity has not been observed yet'
        }
      }
    }
  }

  it('is read out of the wrapper CloudStack puts it in', () => {
    expect(engineReason(asCloudStackRefuses)).toContain('capacity has not been observed yet')
  })

  it('is never the transport\'s own message when a reason exists', () => {
    // The whole point. A screen reading `.message` shows a status code
    // and no remedy, which is what every one of them used to do.
    expect(engineReason(asCloudStackRefuses)).not.toContain('status code')
  })

  it('falls back to the transport when there is no reason to read', () => {
    // A proxy, a dropped connection or a non-CloudStack answer. Saying
    // something imperfect beats a blank screen.
    expect(engineReason(new Error('Network Error'))).toBe('Network Error')
  })

  it('reads a refusal that never reached the command', () => {
    // Dispatch refuses under "errorresponse", not under a key named
    // after a command that was never run.
    const denied = {
      message: 'Request failed with status code 432',
      response: { data: { errorresponse: { errorcode: 432, errortext: 'command not available' } } }
    }
    expect(engineReason(denied)).toBe('command not available')
  })

  it('says nothing rather than inventing something', () => {
    expect(engineReason(undefined)).toBe('')
    expect(engineReason({ response: { data: 'not json at all' } })).toBe('')
  })
})
