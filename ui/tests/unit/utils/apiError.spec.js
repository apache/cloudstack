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

import { getSafeApiErrorDetails, logApiError } from '@/utils/apiError'

describe('API error logging', () => {
  const accessKey = 'SYNTHETIC_ACCESS_KEY'
  const secretKey = 'SYNTHETIC_SECRET_KEY'
  const sessionKey = 'SYNTHETIC_SESSION_KEY'

  function createAxiosError (data) {
    return {
      name: 'Error',
      code: 'ERR_BAD_RESPONSE',
      isAxiosError: true,
      response: {
        status: 500,
        statusText: 'Internal Server Error',
        data: {
          errorresponse: {
            errortext: secretKey
          }
        },
        config: {
          method: 'post',
          url: '/',
          data
        }
      }
    }
  }

  it('keeps useful POST failure metadata without request or response secrets', () => {
    const data = new URLSearchParams()
    data.append('command', 'addObjectStoragePool')
    data.append('details[0].value', accessKey)
    data.append('details[1].value', secretKey)
    data.append('sessionkey', sessionKey)
    const error = createAxiosError(data.toString())

    const details = getSafeApiErrorDetails(error)
    const serializedDetails = JSON.stringify(details)

    expect(details).toEqual({
      name: 'Error',
      code: 'ERR_BAD_RESPONSE',
      status: 500,
      statusText: 'Internal Server Error',
      method: 'POST',
      command: 'addObjectStoragePool'
    })
    expect(serializedDetails).not.toContain(accessKey)
    expect(serializedDetails).not.toContain(secretKey)
    expect(serializedDetails).not.toContain(sessionKey)
  })

  it('logs only the safe failure summary', () => {
    const data = new URLSearchParams()
    data.append('command', 'addObjectStoragePool')
    data.append('details[1].value', secretKey)
    const error = createAxiosError(data.toString())
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {})

    try {
      logApiError(error)

      expect(consoleError).toHaveBeenCalledWith('CloudStack API request failed', {
        name: 'Error',
        code: 'ERR_BAD_RESPONSE',
        status: 500,
        statusText: 'Internal Server Error',
        method: 'POST',
        command: 'addObjectStoragePool'
      })
      expect(JSON.stringify(consoleError.mock.calls)).not.toContain(secretKey)
    } finally {
      consoleError.mockRestore()
    }
  })

  it('does not copy GET parameters into the safe failure summary', () => {
    const error = createAxiosError()
    error.response.config.method = 'get'
    error.response.config.params = {
      command: 'addObjectStoragePool',
      accesskey: accessKey,
      secretkey: secretKey,
      sessionkey: sessionKey
    }

    const serializedDetails = JSON.stringify(getSafeApiErrorDetails(error))

    expect(serializedDetails).toContain('addObjectStoragePool')
    expect(serializedDetails).not.toContain(accessKey)
    expect(serializedDetails).not.toContain(secretKey)
    expect(serializedDetails).not.toContain(sessionKey)
  })

  it('does not serialize unexpected objects from the error or request metadata', () => {
    const secret = 'SYNTHETIC_NESTED_SECRET'
    const error = createAxiosError()
    error.name = { secret }
    error.code = { secret }
    error.response.status = { secret }
    error.response.statusText = { secret }
    error.response.config.method = { secret }
    error.response.config.params = { command: { secret } }

    const serializedDetails = JSON.stringify(getSafeApiErrorDetails(error))

    expect(serializedDetails).not.toContain(secret)
  })
})
