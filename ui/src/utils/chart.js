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

import { TIME_UNITS } from './units'

export const defaultDisplayFormats = {
  day: 'DD MMM YYYY',
  week: 'DD MMM YYYY',
  month: 'MMM YYYY',
  quarter: 'MMM YYYY',
  year: 'YYYY'
}

export const getUnitToTimeCartesianAxis = (baseUnit, dataLength) => {
  const maxLabels = 15
  if (dataLength <= maxLabels) {
    return baseUnit
  }

  const units = [
    'millisecond',
    'second',
    'minute',
    'hour',
    'day',
    'week',
    'month',
    'quarter',
    'year'
  ]

  let index = units.indexOf(baseUnit)

  let unitToReturn = baseUnit
  if (index >= 0 && index < units.length) {
    let unitTime = 0
    for (index; index < units.length; index++) {
      unitTime = TIME_UNITS[units[index]]
      const nextUnitTime = TIME_UNITS[units[index + 1]]

      if ((dataLength / (nextUnitTime / unitTime)) <= maxLabels) {
        return units[index + 1]
      }

      unitToReturn = units[index]
    }
  }

  return unitToReturn
}

export const getChartColorObject = (hexColor = '#1890FF') => ({
  backgroundColor: hexColor.concat('80'),
  borderColor: hexColor,
  borderWidth: 1.5
})
