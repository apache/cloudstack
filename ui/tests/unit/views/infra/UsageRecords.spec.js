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

import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import UsageRecords from '@/views/infra/UsageRecords'

dayjs.extend(utc)

const dateWithOffset = (date, offsetMinutes) => dayjs(date).utcOffset(offsetMinutes, true)

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
  describe('getParams()', () => {
    it('uses both selected dates when converting a local-timezone range to UTC', () => {
      const params = getParams([
        dateWithOffset('2026-07-26', 60),
        dateWithOffset('2026-08-02', 60)
      ], true)

      expect(params.startdate).toBe('2026-07-25 23:00:00')
      expect(params.enddate).toBe('2026-08-02 22:59:59')
    })

    it('uses the selected end date when the range crosses daylight-saving time', () => {
      const params = getParams([
        dateWithOffset('2026-03-28', 0),
        dateWithOffset('2026-03-30', 60)
      ], true)

      expect(params.startdate).toBe('2026-03-28 00:00:00')
      expect(params.enddate).toBe('2026-03-30 22:59:59')
    })

    it('preserves both selected dates when browser-timezone conversion is disabled', () => {
      const params = getParams(['2026-07-26', '2026-08-02'], false)

      expect(params.startdate).toBe('2026-07-26 00:00:00')
      expect(params.enddate).toBe('2026-08-02 23:59:59')
    })

    it('omits date parameters when no range is selected', () => {
      const params = getParams([], true)

      expect(params).not.toHaveProperty('startdate')
      expect(params).not.toHaveProperty('enddate')
    })

    it('omits date parameters when the selected range is incomplete', () => {
      const params = getParams([dateWithOffset('2026-07-26', 60)], true)

      expect(params).not.toHaveProperty('startdate')
      expect(params).not.toHaveProperty('enddate')
    })
  })
})
