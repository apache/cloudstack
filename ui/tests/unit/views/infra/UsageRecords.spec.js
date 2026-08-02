// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import UsageRecords from '@/views/infra/UsageRecords'

const originalTimezone = process.env.TZ

const getParams = (dateRange, useBrowserTimezone) => {
  return UsageRecords.methods.getParams.call({
    form: { dateRange },
    handleRemoveFields: values => values,
    page: 1,
    pageSize: 20,
    $store: {
      getters: { usebrowsertimezone: useBrowserTimezone }
    }
  })
}

describe('Views > infra > UsageRecords.vue', () => {
  beforeAll(() => {
    process.env.TZ = 'Europe/London'
  })

  afterAll(() => {
    if (originalTimezone === undefined) {
      delete process.env.TZ
    } else {
      process.env.TZ = originalTimezone
    }
  })

  describe('getParams()', () => {
    it('uses both selected dates when converting a local-timezone range to UTC', () => {
      const params = getParams(['2026-07-26', '2026-08-02'], true)

      expect(params.startdate).toBe('2026-07-25 23:00:00')
      expect(params.enddate).toBe('2026-08-02 22:59:59')
    })

    it('uses the selected end date when the range crosses daylight-saving time', () => {
      const params = getParams(['2026-03-28', '2026-03-30'], true)

      expect(params.startdate).toBe('2026-03-28 00:00:00')
      expect(params.enddate).toBe('2026-03-30 22:59:59')
    })

    it('preserves both selected dates when browser-timezone conversion is disabled', () => {
      const params = getParams(['2026-07-26', '2026-08-02'], false)

      expect(params.startdate).toBe('2026-07-26 00:00:00')
      expect(params.enddate).toBe('2026-08-02 23:59:59')
    })
  })
})
