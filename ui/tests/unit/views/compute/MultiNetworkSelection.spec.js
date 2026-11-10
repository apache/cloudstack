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

import mockAxios from '../../../mock/mockAxios'
import common from '../../../common'
import MultiNetworkSelection from '@/views/compute/wizard/MultiNetworkSelection'

jest.mock('axios', () => mockAxios)
jest.mock('@/vue-app', () => ({
  vueProps: {
    $localStorage: {
      set: jest.fn(),
      get: jest.fn(() => null)
    }
  }
}))

let router
let i18n
let store
let wrapper

const factory = (opts = {}) => {
  return common.createFactory(MultiNetworkSelection, {
    router,
    i18n,
    store,
    props: opts.props || {},
    data: opts.data || {}
  })
}

describe('Components > Compute > MultiNetworkSelection.vue', () => {
  beforeEach(() => {
    jest.clearAllMocks()

    router = common.createMockRouter({})
    i18n = common.createMockI18n('en', {})
    store = common.createMockStore()

    mockAxios.mockResolvedValue({
      listnetworksresponse: {
        count: 0,
        network: []
      }
    })
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
      wrapper = null
    }
  })

  describe('fetchNetworks()', () => {
    it('API should be called with projectid when project scope is selected', async () => {
      wrapper = factory({
        props: {
          items: [],
          zoneId: 'zone-1',
          domainid: 'domain-1',
          account: 'account-1',
          projectid: 'project-1'
        }
      })

      await wrapper.vm.fetchNetworks()
      await flushPromises()

      expect(mockAxios).toHaveBeenLastCalledWith(
        expect.objectContaining({
          url: '/',
          method: 'GET',
          params: expect.objectContaining({
            command: 'listNetworks',
            zoneid: 'zone-1',
            listall: true,
            projectid: 'project-1'
          })
        })
      )

      const request = mockAxios.mock.calls[mockAxios.mock.calls.length - 1][0]

      expect(request.params).not.toHaveProperty('account')
      expect(request.params).not.toHaveProperty('domainid')
    })

    it('API should be called with domainid and account when project scope is not selected', async () => {
      wrapper = factory({
        props: {
          items: [],
          zoneId: 'zone-1',
          domainid: 'domain-1',
          account: 'account-1',
          projectid: ''
        }
      })

      await wrapper.vm.fetchNetworks()
      await flushPromises()

      expect(mockAxios).toHaveBeenLastCalledWith(
        expect.objectContaining({
          url: '/',
          method: 'GET',
          params: expect.objectContaining({
            command: 'listNetworks',
            zoneid: 'zone-1',
            listall: true,
            domainid: 'domain-1',
            account: 'account-1'
          })
        })
      )

      const request = mockAxios.mock.calls[mockAxios.mock.calls.length - 1][0]

      expect(request.params).not.toHaveProperty('projectid')
    })
  })

  describe('projectid watcher', () => {
    it('should refresh networks when project changes', async () => {
      wrapper = factory({
        props: {
          items: [],
          zoneId: 'zone-1',
          projectid: 'project-1'
        }
      })

      await flushPromises()

      mockAxios.mockClear()

      await wrapper.setProps({
        projectid: 'project-2'
      })

      await flushPromises()

      expect(mockAxios).toHaveBeenCalled()
    })
  })
})
