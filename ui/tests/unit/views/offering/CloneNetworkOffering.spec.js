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

import { flushPromises } from '@vue/test-utils'
import { axios } from '@/utils/request'
import { mixinForm } from '@/utils/mixin'
import CloneNetworkOffering from '@/views/offering/CloneNetworkOffering'

jest.mock('@/utils/request', () => ({ axios: jest.fn() }))
jest.mock('@/vue-app', () => ({
  vueProps: { $localStorage: { get: jest.fn() } }
}))

const profiles = {
  nsxipdiscoveryprofileid: 'ip-profile',
  nsxmacdiscoveryprofileid: 'mac-profile',
  nsxsegmentsecurityprofileid: 'security-profile'
}

describe('CloneNetworkOffering detail request parameters', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    axios.mockResolvedValue({})
  })

  it.each([
    ['all NSX profiles', profiles],
    ['IP discovery only', { nsxipdiscoveryprofileid: profiles.nsxipdiscoveryprofileid }],
    ['MAC discovery only', { nsxmacdiscoveryprofileid: profiles.nsxmacdiscoveryprofileid }],
    ['segment security only', { nsxsegmentsecurityprofileid: profiles.nsxsegmentsecurityprofileid }],
    ['no optional details', {}],
    ['NSX profiles together with existing details', {
      ...profiles,
      promiscuousmode: 'false',
      macaddresschanges: 'true',
      forgedtransmits: 'false',
      maclearning: 'true'
    }]
  ])('submits %s using the network-offering API map format', async (description, details) => {
    const form = {
      name: 'cloned-offering',
      provider: 'NSX',
      nsxipdiscoveryprofileid: '',
      nsxmacdiscoveryprofileid: '',
      nsxsegmentsecurityprofileid: '',
      promiscuousmode: '',
      macaddresschanges: '',
      forgedtransmits: '',
      maclearning: '',
      ...details
    }
    const context = {
      form,
      formRef: { value: { validate: () => Promise.resolve() } },
      $refs: Object.fromEntries(Object.keys(form).map(key => [key, {}])),
      handleRemoveFields: mixinForm.methods.handleRemoveFields,
      resource: { id: 'source-offering', traffictype: 'Guest' },
      selectedServiceProviderMap: {},
      registeredServicePackages: [],
      loading: false,
      $t: key => key,
      $message: { success: jest.fn() },
      $emit: jest.fn(),
      $notifyError: jest.fn(),
      closeAction: jest.fn()
    }

    CloneNetworkOffering.methods.handleSubmit.call(context)
    await flushPromises()

    expect(axios).toHaveBeenCalledTimes(1)
    const request = axios.mock.calls[0][0]
    expect(request.method).toBe('POST')
    expect(request.data.get('command')).toBe('cloneNetworkOffering')
    expect(request.data.get('sourceofferingid')).toBe('source-offering')
    expect(request.data.get('provider')).toBe('NSX')
    const submittedDetails = Object.fromEntries(
      [...request.data.entries()].filter(([key]) => key.startsWith('details['))
    )
    expect(submittedDetails).toEqual(Object.fromEntries(
      Object.entries(details).map(([key, value]) => [`details[0].${key}`, value])
    ))
    expect(context.$notifyError).not.toHaveBeenCalled()
  })
})
