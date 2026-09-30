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

import mockAxios from '../../../mock/mockAxios'
import common from '../../../common'
import eventBus from '@/config/eventBus'
import TooltipButton from '@/components/widgets/TooltipButton'
import VpcTiersTab from '@/views/network/VpcTiersTab'

jest.mock('axios', () => mockAxios)
jest.mock('@/vue-app', () => ({
  vueProps: {
    $localStorage: {
      get: jest.fn(() => null)
    }
  }
}))

const networkSection = require('@/config/section/network').default

const vpc = {
  id: 'vpc-id',
  zoneid: 'zone-id',
  vpcofferingid: 'vpc-offering-id',
  service: [],
  network: [{
    id: 'tier-id',
    zoneid: 'zone-id',
    networkofferingid: 'network-offering-id',
    name: 'tier',
    state: 'Implemented',
    cidr: '10.0.0.0/24',
    aclid: 'acl-id',
    aclname: 'ACL',
    service: []
  }]
}

const registeredReplaceAction = networkSection.children
  .find(section => section.name === 'guestnetwork')
  .actions.find(action => action.api === 'replaceNetworkACLList')

const createReplaceAction = (overrides = {}) => ({
  ...registeredReplaceAction,
  ...overrides
})

const createWrapper = ({ permission = true, action = createReplaceAction() } = {}) => {
  const store = common.createMockStore({
    user: {
      apis: permission ? { replaceNetworkACLList: {} } : {},
      info: {}
    }
  })
  const router = {
    resolve: jest.fn(() => ({
      meta: { actions: action ? [action] : [] }
    }))
  }
  const wrapper = shallowMount(VpcTiersTab, {
    props: { resource: vpc },
    global: {
      renderStubDefaultSlot: true,
      plugins: [store],
      mocks: {
        $t: key => key,
        $route: { path: '/vpc/vpc-id', query: {}, params: {} },
        $router: router,
        $notifyError: jest.fn()
      },
      provide: {
        parentFetchData: jest.fn()
      }
    }
  })
  return { wrapper, router }
}

const findReplaceButton = wrapper => wrapper.findAllComponents(TooltipButton)
  .find(button => button.props('tooltip') === 'label.replace.acl')

describe('VPC tier ACL action reuse', () => {
  let execAction

  beforeEach(() => {
    execAction = jest.fn()
    eventBus.on('exec-action', execAction)
    mockAxios.mockReset()
    mockAxios.mockImplementation(request => {
      switch (request.params?.command) {
        case 'listZones':
          return Promise.resolve({ listzonesresponse: { zone: [{}] } })
        case 'listVPCOfferings':
          return Promise.resolve({ listvpcofferingsresponse: { vpcoffering: [{}] } })
        case 'listLoadBalancers':
          return Promise.resolve({ listloadbalancersresponse: { loadbalancer: [], count: 0 } })
        case 'listVirtualMachines':
          return Promise.resolve({ listvirtualmachinesresponse: { virtualmachine: [], count: 0 } })
        case 'listNetworkOfferings':
          return Promise.resolve({ listnetworkofferingsresponse: { networkoffering: [{ supportsinternallb: false }] } })
        case 'listNetworks':
          return Promise.resolve({ listnetworksresponse: { network: [] } })
        default:
          return Promise.resolve({})
      }
    })
  })

  afterEach(() => {
    eventBus.off('exec-action', execAction)
  })

  it('dispatches the existing registered action with tier-specific mappings', async () => {
    const registeredAction = createReplaceAction()
    const originalAclParams = registeredAction.mapping.aclid.params
    const originalNetworkValue = registeredAction.mapping.networkid.value
    const { wrapper, router } = createWrapper({ action: registeredAction })
    await flushPromises()

    const replaceButton = findReplaceButton(wrapper)
    expect(replaceButton).toBeDefined()
    expect(replaceButton.props('disabled')).toBe(false)

    mockAxios.mockClear()
    replaceButton.vm.$emit('onClick')
    await wrapper.vm.$nextTick()

    expect(router.resolve).toHaveBeenCalledWith({ path: '/guestnetwork/tier-id' })
    expect(execAction).toHaveBeenCalledTimes(1)
    const payload = execAction.mock.calls[0][0]
    expect(payload.isGroupAction).toBe(false)
    expect(payload.action.api).toBe('replaceNetworkACLList')
    expect(payload.action).not.toBe(registeredAction)
    expect(payload.action.resource).toEqual(vpc)
    expect(payload.action.resource.id).toBe('vpc-id')
    expect(payload.action.mapping).not.toBe(registeredAction.mapping)
    expect(payload.action.mapping.aclid.params(payload.action.resource)).toEqual({ vpcid: 'vpc-id' })
    expect(payload.action.mapping.networkid.value(payload.action.resource)).toBe('tier-id')
    expect(payload.action.mapping.aclid.params).not.toBe(originalAclParams)
    expect(payload.action.mapping.networkid.value).not.toBe(originalNetworkValue)
    expect(registeredAction.mapping.aclid.params).toBe(originalAclParams)
    expect(registeredAction.mapping.networkid.value).toBe(originalNetworkValue)
    expect(mockAxios).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('keeps the shortcut disabled and does not dispatch without API permission', async () => {
    const { wrapper } = createWrapper({ permission: false })
    await flushPromises()

    const replaceButton = findReplaceButton(wrapper)
    expect(replaceButton).toBeDefined()
    expect(replaceButton.props('disabled')).toBe(true)

    wrapper.vm.handleOpenReplaceAclAction(vpc.network[0])
    expect(execAction).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it.each([
    ['missing', null],
    ['hidden', createReplaceAction({ show: () => false })],
    ['disabled', createReplaceAction({ disabled: () => true })],
    ['non-data-view', createReplaceAction({ dataView: false })],
    ['ACL mapping without parameters', createReplaceAction({
      mapping: {
        ...registeredReplaceAction.mapping,
        aclid: { api: 'listNetworkACLLists' }
      }
    })],
    ['network mapping without a value', createReplaceAction({
      mapping: {
        ...registeredReplaceAction.mapping,
        networkid: {}
      }
    })]
  ])('does not dispatch a %s registered action', async (name, action) => {
    const { wrapper } = createWrapper({ action })
    await flushPromises()

    wrapper.vm.handleOpenReplaceAclAction(vpc.network[0])
    expect(execAction).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
