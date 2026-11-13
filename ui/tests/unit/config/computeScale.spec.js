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

import compute from '@/config/section/compute'

jest.mock('@/store', () => ({}))
jest.mock('@/api', () => ({}))
jest.mock('@/utils/zone', () => ({}))
jest.mock('@/utils/acsrepo', () => ({}))
jest.mock('@/utils/util', () => ({}))
jest.mock('@/assets/icons/kubernetes.svg?inline', () => ({}))

const action = compute.children.find(section => section.name === 'vm').actions
  .find(action => action.api === 'scaleVirtualMachine')

describe('instance compute offering changes', () => {
  const vm = { hypervisor: 'KVM', state: 'Stopped' }

  it.each(['Stopped', 'Running'])('hides CloudManaged nodes in %s state', state => {
    expect(action.show({ ...vm, state, vmtype: 'cksnode', kubernetesclustertype: 'CloudManaged' })).toBe(false)
    expect(action.show({ ...vm, state, kubernetesclustertype: 'CloudManaged' })).toBe(false)
  })

  it('keeps ExternalManaged and ordinary instance actions', () => {
    expect(action.show(vm)).toBe(true)
    expect(action.show({ ...vm, kubernetesclustertype: 'ExternalManaged' })).toBe(true)
    expect(action.show({ ...vm, vmtype: 'cksnode', kubernetesclustertype: 'ExternalManaged' })).toBe(true)
  })

  it('falls back to the existing VM type for older servers', () => {
    expect(action.show({ ...vm, vmtype: 'cksnode' })).toBe(false)
  })

  it('retains existing hypervisor and state restrictions', () => {
    expect(action.show({ ...vm, hypervisor: 'External' })).toBe(false)
    expect(action.show({ ...vm, state: 'Running', hypervisor: 'LXC' })).toBe(false)
    expect(action.show({ ...vm, state: 'Destroyed' })).toBe(false)
    expect(action.show({ ...vm, vmtype: 'sharedfsvm' })).toBe(false)
    expect(action.disabled({ ...vm, state: 'Running', isdynamicallyscalable: false })).toBe(true)
  })
})
