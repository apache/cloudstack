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

export function exportDataToCsv ({ data = null, keys = null, headers = null, columnDelimiter = ',', lineDelimiter = '\n', fileName = 'data', dateFormat = undefined }) {
  if (data === null || !data.length || keys === null || !keys.filter(key => key !== null && key !== '').length) {
    return null
  }

  let dataParsed = ''
  dataParsed += (headers || keys).join(columnDelimiter)
  dataParsed += lineDelimiter

  data.forEach(item => {
    keys.forEach(key => {
      if (item[key] === undefined) {
        item[key] = ''
      }

      if (typeof item[key] === 'string' && item[key].includes(columnDelimiter)) {
        dataParsed += `"${item[key]}"`
      } else if (dateFormat && item[key] instanceof dayjs) {
        dataParsed += `"${item[key].format(dateFormat)}"`
      } else {
        dataParsed += item[key]
      }

      dataParsed += columnDelimiter
    })
    dataParsed = dataParsed.slice(0, -1)
    dataParsed += lineDelimiter
  })

  const hiddenElement = document.createElement('a')
  hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(dataParsed)
  hiddenElement.target = '_blank'
  hiddenElement.download = `${fileName}.csv`
  hiddenElement.click()
  hiddenElement.remove()
}
