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

function loadTimeZone () {
  const timeZoneJson = require.context('./', true, /[A-Za-z0-9-_,\s]+\.json$/i)
  const data = []

  timeZoneJson.keys().forEach(key => {
    const matched = key.match(/([A-Za-z0-9-_]+)\./i)
    if (matched && matched.length > 1) {
      const json = timeZoneJson(key)
      for (const index in json) {
        data.push({
          id: index,
          name: json[index]
        })
      }
    }
  })

  return data
}

function getFullTimeZone (strQuery) {
  if (!strQuery || strQuery === '') {
    return []
  }

  const timeZoneJson = require.context('./', true, /[A-Za-z0-9-_,\s]+\.json$/i)
  const data = []
  timeZoneJson.keys().forEach(key => {
    const matched = key.match(/([A-Za-z0-9-_]+)\./i)
    if (matched && matched.length > 1) {
      const json = timeZoneJson(key)
      for (const index in json) {
        if (index.toLowerCase() === strQuery.toLowerCase()) {
          data.push({
            id: index,
            name: json[index]
          })

          break
        }
      }
    }
  })

  return data
}

export function timeZone () {
  return new Promise(resolve => {
    const dataTimeZone = loadTimeZone()
    resolve(dataTimeZone)
  })
}

export function timeZoneName (key) {
  const dataTimeZone = getFullTimeZone(key)

  if (dataTimeZone && dataTimeZone[0]) {
    return dataTimeZone[0].name
  }

  return ''
}
