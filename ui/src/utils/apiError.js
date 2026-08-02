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

function cleanLogValue (value) {
  if (typeof value !== 'string') {
    return undefined
  }
  return value.replace(/[\n\r\t]/g, '_').slice(0, 256)
}

function getCommand (config) {
  if (config?.params?.command) {
    return config.params.command
  }

  const data = config?.data
  if (typeof URLSearchParams !== 'undefined' && data instanceof URLSearchParams) {
    return data.get('command')
  }
  if (typeof data === 'string') {
    return new URLSearchParams(data).get('command')
  }
}

export function getSafeApiErrorDetails (error) {
  const response = error?.response
  const config = response?.config || error?.config
  const method = cleanLogValue(config?.method)

  return {
    name: cleanLogValue(error?.name),
    code: cleanLogValue(error?.code),
    status: Number.isInteger(response?.status) ? response.status : undefined,
    statusText: cleanLogValue(response?.statusText),
    method: typeof method === 'string' ? method.toUpperCase() : method,
    command: cleanLogValue(getCommand(config))
  }
}

export function logApiError (error) {
  console.error('CloudStack API request failed', getSafeApiErrorDetails(error))
}
