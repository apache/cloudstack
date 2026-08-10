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

import ImportUnmanagedInstance from '@/views/tools/ImportUnmanagedInstance'

jest.mock('@views/compute/wizard/ComputeOfferingSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/ComputeSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/MultiDiskSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/MultiNetworkSelection', () => ({}), { virtual: true })

describe('Views > tools > ImportUnmanagedInstance.vue', () => {
  it('clears project and network selections when an account domain is selected', () => {
    const context = {
      selectedDomainId: null,
      nicsNetworksMapping: { 'nic-1': { network: 'network-1' } },
      updateFieldValue: jest.fn()
    }

    ImportUnmanagedInstance.methods.handleDomainChange.call(context, 'domain-1')

    expect(context.selectedDomainId).toBe('domain-1')
    expect(context.updateFieldValue).toHaveBeenCalledWith('account', undefined)
    expect(context.updateFieldValue).toHaveBeenCalledWith('projectid', undefined)
    expect(context.nicsNetworksMapping).toEqual({})
  })

  it('clears account, domain and network selections when a project is selected', () => {
    const context = {
      selectedDomainId: 'domain-1',
      nicsNetworksMapping: { 'nic-1': { network: 'network-1' } },
      updateFieldValue: jest.fn()
    }

    ImportUnmanagedInstance.methods.handleProjectChange.call(context, 'project-1')

    expect(context.selectedDomainId).toBeNull()
    expect(context.updateFieldValue).toHaveBeenCalledWith('domainid', undefined)
    expect(context.updateFieldValue).toHaveBeenCalledWith('account', undefined)
    expect(context.nicsNetworksMapping).toEqual({})
  })
})
