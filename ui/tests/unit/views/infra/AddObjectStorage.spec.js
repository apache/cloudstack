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

import mockAxios from '../../../mock/mockAxios'
import AddObjectStorage from '@/views/infra/AddObjectStorage'

jest.mock('axios', () => mockAxios)
jest.mock('@/vue-app', () => ({
  vueProps: {
    $localStorage: {
      get: jest.fn(() => null)
    }
  }
}))

describe('Views > infra > AddObjectStorage.vue', () => {
  beforeEach(() => {
    mockAxios.mockReset()
  })

  it('submits addObjectStoragePool using POST with the request in the body', async () => {
    mockAxios.mockResolvedValue({})

    const params = {
      name: 'test-store',
      provider: 'MinIO',
      url: 'https://object-storage.example.test',
      'details[0].key': 'accesskey',
      'details[0].value': 'test-access-key',
      'details[1].key': 'secretkey',
      'details[1].value': 'test-secret-key'
    }

    await AddObjectStorage.methods.addObjectStore(params)

    expect(mockAxios).toHaveBeenCalledTimes(1)
    const request = mockAxios.mock.calls[0][0]
    expect(request).toMatchObject({
      url: '/',
      method: 'POST'
    })
    expect(request.params).toBeUndefined()
    expect(request.data).toBeInstanceOf(URLSearchParams)
    expect(request.data.get('command')).toBe('addObjectStoragePool')
    expect(request.data.get('response')).toBe('json')
    expect(request.data.get('url')).toBe(params.url)
    expect(request.data.get('details[0].value')).toBe(params['details[0].value'])
    expect(request.data.get('details[1].value')).toBe(params['details[1].value'])
  })

  it('propagates an addObjectStoragePool API failure', async () => {
    const error = new Error('request failed')
    mockAxios.mockRejectedValue(error)

    await expect(AddObjectStorage.methods.addObjectStore({})).rejects.toBe(error)
  })
})
