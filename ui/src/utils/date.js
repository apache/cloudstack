// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import store from '@/store'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'

dayjs.extend(utc)

export function parseDayJsObject ({ value, format }) {
  if (!format) {
    return value
  }
  return value.format(format)
}

export function parseDateToDatePicker (value) {
  if (!value) {
    return null
  }

  const format = 'YYYY-MM-DD'
  return dayjs(value, format)
}

export function toLocalDate ({ date, timezoneoffset = store.getters.timezoneoffset, usebrowsertimezone = store.getters.usebrowsertimezone }) {
  if (usebrowsertimezone) {
    // Since GMT+530 is returned as -330 (mins to GMT)
    timezoneoffset = new Date().getTimezoneOffset() / -60
  }

  const milliseconds = Date.parse(date)
  // e.g. "Tue, 08 Jun 2010 19:13:49 GMT", "Tue, 25 May 2010 12:07:01 UTC"
  return new Date(milliseconds + (timezoneoffset * 60 * 60 * 1000))
}

export function toLocaleDate ({ date, timezoneoffset = store.getters.timezoneoffset, usebrowsertimezone = store.getters.usebrowsertimezone, dateOnly = false, hourOnly = false }) {
  if (!date) {
    return null
  }

  let dateWithOffset = toLocalDate({ date, timezoneoffset, usebrowsertimezone }).toUTCString()
  // e.g. "08 Jun 2010 19:13:49 GMT", "25 May 2010 12:07:01 UTC"
  dateWithOffset = dateWithOffset.substring(dateWithOffset.indexOf(', ') + 2)
  // e.g. "08 Jun 2010 19:13:49", "25 May 2010 12:10:16"
  dateWithOffset = dateWithOffset.substring(0, dateWithOffset.length - 4)

  if (dateOnly) {
    return dateWithOffset.substring(0, dateWithOffset.length - 9)
  }

  if (hourOnly) {
    return dateWithOffset.substring(dateWithOffset.length - 8, dateWithOffset.length)
  }

  return dateWithOffset
}

export { dayjs }
