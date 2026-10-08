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

import notification from 'ant-design-vue/es/notification'
import router from '@/router'
import store from '@/store'
import { axios } from '@/utils/request'

jest.mock('ant-design-vue/es/notification', () => ({
  error: jest.fn(),
  warning: jest.fn(),
  warn: jest.fn()
}))
jest.mock('@/router', () => ({
  currentRoute: { value: { path: '/dashboard' } },
  push: jest.fn()
}))
jest.mock('@/store', () => ({
  getters: { countNotify: 0 },
  commit: jest.fn(),
  dispatch: jest.fn(() => Promise.resolve())
}))
jest.mock('@/locales', () => ({
  i18n: { global: { t: (key) => key } }
}))
jest.mock('@/vue-app', () => ({
  vueProps: { $localStorage: { get: () => null } }
}))

const unauthorizedAdapter = (config) => Promise.reject(Object.assign(new Error('Request failed with status code 401'), {
  config,
  isAxiosError: true,
  response: {
    status: 401,
    config,
    data: { listvirtualmachinesresponse: { errorcode: 401, errortext: 'unable to verify user credentials and/or request signature' } }
  }
}))

const callApi = () => axios.get('/client/api', {
  params: { command: 'listVirtualMachines' },
  adapter: unauthorizedAdapter
}).catch(() => {})

describe('utils > request.js', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('shows a session expired warning when the session times out', async () => {
    router.currentRoute.value.path = '/dashboard'
    await callApi()

    expect(notification.error).not.toHaveBeenCalled()
    expect(notification.warning).toHaveBeenCalledTimes(1)
    const shown = notification.warning.mock.calls[0][0]
    expect(shown.message).toBe('label.session.expired')
    expect(shown.description).toBe('message.session.expired')
    expect(shown.duration).toBeUndefined()
    expect(store.dispatch).toHaveBeenCalledWith('Logout')
  })

  it('shows nothing when already on the login page', async () => {
    router.currentRoute.value.path = '/user/login'
    await callApi()

    expect(notification.error).not.toHaveBeenCalled()
    expect(notification.warning).not.toHaveBeenCalled()
  })
})
