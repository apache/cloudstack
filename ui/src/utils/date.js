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
import * as momentLib from 'moment'
import store from '@/store/'

export function getMomentFormattedAndNormalized ({ value, keepMoment = true, format = true }) {
  console.log('value =', value)
  console.log('keepMoment =', keepMoment)
  console.log('format =', format)

  // if (typeof value === 'string') {
  value = moment(value)
  // }

  console.log('moment value before =', value)

  if (!store.getters.usebrowsertimezone) {
    value = value.utc(keepMoment)
  }

  console.log('moment value after =', value)
  console.log('moment value formatted =', value.format())

  if (format) {
    return value.format()
  }

  return value
}

export const moment = momentLib
