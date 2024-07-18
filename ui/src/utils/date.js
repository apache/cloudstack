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

export function parseDayJsObject ({ value, format = true, keepMoment = true }) {
  if (!value) {
    return null
  }

  if (typeof value === 'string') {
    value = dayjs(value)
  }

  if (!store.getters.usebrowsertimezone) {
    value = value.utc(keepMoment)
  }

  if (!format) {
    return value
  }

  return value.format()
}

/**
 * When passing a string/dayjs to the date picker component, it converts the value to the browser timezone; therefore,
 * we need to normalize the value to UTC if user is not using browser's timezone.
 * @param {*} value The datetime to normalize.
 * @returns A dayjs object with the datetime normalized to UTC if user is not using browser's timezone;
 * otherwise, a correspondent dayjs object based on the value passed.
 */
export function parseDateToDatePicker (value) {
  if (!value) {
    return null
  }

  if (typeof value === 'string') {
    value = dayjs(value)
  }

  if (store.getters.usebrowsertimezone) {
    return value
  }

  return value.utc(false)
}

export function toLocalDate ({ date, timezoneoffset = store.getters.timezoneoffset, usebrowsertimezone = store.getters.usebrowsertimezone }) {
  if (usebrowsertimezone) {
    // Since GMT+530 is returned as -330 (minutes to GMT)
    timezoneoffset = new Date().getTimezoneOffset() / -60
  }

  const milliseconds = Date.parse(date)
  // e.g. "Tue, 08 Jun 2010 19:13:49 GMT"; "Tue, 25 May 2010 12:07:01 UTC"
  return new Date(milliseconds + (timezoneoffset * 60 * 60 * 1000))
}

export function toLocaleDate ({ date, timezoneoffset = store.getters.timezoneoffset, usebrowsertimezone = store.getters.usebrowsertimezone, dateOnly = false, hourOnly = false }) {
  if (!date) {
    return null
  }

  let dateWithOffset = toLocalDate({ date, timezoneoffset, usebrowsertimezone }).toUTCString()

  // e.g. "Mon, 03 Jun 2024 19:22:55 GMT" -> "03 Jun 2024 19:22:55 GMT"
  dateWithOffset = dateWithOffset.substring(dateWithOffset.indexOf(', ') + 2)

  // e.g. "03 Jun 2024 19:22:55 GMT" -> "03 Jun 2024 19:22:55"
  dateWithOffset = dateWithOffset.substring(0, dateWithOffset.length - 4)

  if (dateOnly) {
    // e.g. "03 Jun 2024 19:22:55" -> "03 Jun 2024"
    return dateWithOffset.substring(0, dateWithOffset.length - 9)
  }

  if (hourOnly) {
    // e.g. "03 Jun 2024 19:22:55" -> "19:22:55"
    return dateWithOffset.substring(dateWithOffset.length - 8, dateWithOffset.length)
  }

  return dateWithOffset
}

export { dayjs }
