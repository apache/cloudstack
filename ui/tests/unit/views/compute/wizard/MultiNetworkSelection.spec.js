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

import { flushPromises, shallowMount } from '@vue/test-utils'

import { getAPI } from '@/api'
import MultiNetworkSelection from '@/views/compute/wizard/MultiNetworkSelection'

jest.mock('@/api', () => ({
  getAPI: jest.fn()
}))

const responseWith = (...networks) => ({
  listnetworksresponse: {
    count: networks.length,
    network: networks
  }
})

const network = id => ({
  id,
  name: id,
  displaytext: id,
  state: 'Implemented',
  type: 'Isolated'
})

const factory = (props = {}) => shallowMount(MultiNetworkSelection, {
  global: {
    mocks: {
      $t: key => key
    }
  },
  props: {
    items: [{ id: 'nic-1', name: 'nic-1' }],
    zoneId: 'zone-1',
    selectionEnabled: false,
    ...props
  }
})

describe('Views > compute > wizard > MultiNetworkSelection.vue', () => {
  beforeEach(() => {
    getAPI.mockReset()
    getAPI.mockResolvedValue(responseWith(network('network-1')))
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('lists networks in the selected account and domain scope', async () => {
    const wrapper = factory({ domainid: 'domain-1', account: 'account-1' })
    await flushPromises()

    expect(getAPI).toHaveBeenLastCalledWith('listNetworks', {
      zoneid: 'zone-1',
      listall: true,
      domainid: 'domain-1',
      account: 'account-1'
    })
    wrapper.unmount()
  })

  it('uses project scope instead of account and domain scope', async () => {
    const wrapper = factory({
      domainid: 'domain-1',
      account: 'account-1',
      projectid: 'project-1'
    })
    await flushPromises()

    expect(getAPI).toHaveBeenLastCalledWith('listNetworks', {
      zoneid: 'zone-1',
      listall: true,
      projectid: 'project-1'
    })
    wrapper.unmount()
  })

  it('refetches networks when the selected domain changes', async () => {
    const wrapper = factory({ domainid: 'domain-1', account: 'account-1' })
    await flushPromises()

    await wrapper.setProps({ domainid: 'domain-2' })
    await flushPromises()

    expect(getAPI).toHaveBeenLastCalledWith('listNetworks', {
      zoneid: 'zone-1',
      listall: true,
      domainid: 'domain-2',
      account: 'account-1'
    })
    wrapper.unmount()
  })

  it('clears a stale selection while an account scope change is pending', async () => {
    const wrapper = factory({ domainid: 'domain-1', account: 'account-1' })
    await flushPromises()
    expect(wrapper.vm.networks).toHaveLength(1)

    jest.useFakeTimers()
    await wrapper.setProps({ account: '' })

    expect(wrapper.vm.networks).toEqual([])
    expect(wrapper.emitted('select-multi-network').at(-1)).toEqual([{}])
    expect(getAPI).toHaveBeenCalledTimes(1)

    jest.advanceTimersByTime(750)
    await Promise.resolve()
    await Promise.resolve()
    await wrapper.vm.$nextTick()

    expect(getAPI).toHaveBeenLastCalledWith('listNetworks', {
      zoneid: 'zone-1',
      listall: true
    })
    wrapper.unmount()
  })

  it('ignores an older response after the target scope changes', async () => {
    let resolveAdminRequest
    let resolveProjectRequest
    getAPI
      .mockReset()
      .mockImplementationOnce(() => new Promise(resolve => { resolveAdminRequest = resolve }))
      .mockImplementationOnce(() => new Promise(resolve => { resolveProjectRequest = resolve }))

    const wrapper = factory()
    await wrapper.setProps({ projectid: 'project-1' })

    resolveProjectRequest(responseWith(network('project-network')))
    await flushPromises()
    expect(wrapper.vm.networks.map(item => item.id)).toEqual(['project-network'])

    resolveAdminRequest(responseWith(network('admin-network')))
    await flushPromises()
    expect(wrapper.vm.networks.map(item => item.id)).toEqual(['project-network'])
    wrapper.unmount()
  })
})
