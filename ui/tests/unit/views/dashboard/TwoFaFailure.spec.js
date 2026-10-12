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

import { postAPI } from '@/api'
import VerifyTwoFa from '@/views/dashboard/VerifyTwoFa'
import SetupTwoFaAtLogin from '@/views/dashboard/SetupTwoFaAtLogin'

jest.mock('@/api', () => ({
  getAPI: jest.fn(),
  postAPI: jest.fn()
}))
jest.mock('@/store', () => ({}))
jest.mock('@/config/eventBus', () => ({}))
jest.mock('vue-qrious', () => ({}))

const flush = () => new Promise(resolve => setTimeout(resolve, 0))

function wrongCode (component, method) {
  let finishLogout
  const ctx = {
    form: { code: '000000' },
    formRef: { value: { validate: () => Promise.resolve() } },
    $t: (key) => key,
    $store: {
      dispatch: jest.fn(() => new Promise(resolve => { finishLogout = resolve })),
      commit: jest.fn()
    },
    $router: { replace: jest.fn(() => Promise.resolve()), push: jest.fn() },
    $notification: { error: jest.fn(), destroy: jest.fn() },
    $message: { success: jest.fn() },
    $emit: jest.fn()
  }
  postAPI.mockReturnValue(Promise.reject(new Error('wrong code')))
  component.methods[method].call(ctx)
  return { ctx, finishLogout: () => finishLogout() }
}

describe('2FA wrong code', () => {
  it.each([
    [VerifyTwoFa, 'handleSubmit', 'message.two.factor.authorization.failed'],
    [SetupTwoFaAtLogin, 'submitPin', 'message.error.setup.2fa']
  ])('shows the error only after logout has cleared old notifications (%#)', async (component, method, description) => {
    const { ctx, finishLogout } = wrongCode(component, method)
    await flush()

    expect(ctx.$store.dispatch).toHaveBeenCalledWith('Logout')
    expect(ctx.$notification.error).not.toHaveBeenCalled()

    finishLogout()
    await flush()

    expect(ctx.$router.replace).toHaveBeenCalledWith({ path: '/user/login' })
    expect(ctx.$notification.error).toHaveBeenCalledWith({
      message: 'message.request.failed',
      description
    })
  })
})
