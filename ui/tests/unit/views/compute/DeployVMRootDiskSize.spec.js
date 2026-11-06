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

import DeployVM from '@/views/compute/DeployVM'

jest.mock('@views/compute/wizard/OwnershipSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/DeployButtons', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/ZoneBlockRadioGroupSelect', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/ComputeOfferingSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/ComputeSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/DiskOfferingSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/DiskSizeSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/MultiDiskSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/TemplateIsoSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/OsBasedImageSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/AffinityGroupSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/NetworkSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/NetworkConfiguration', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/SshKeyPairSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/UserDataSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/SecurityGroupSelection', () => ({}), { virtual: true })
jest.mock('@views/compute/wizard/DeployInstanceBackupSelection', () => ({}), { virtual: true })

const createContext = (form) => {
  const template = { id: 'template-1', details: {}, hypervisor: 'KVM', size: 8 * 1024 * 1024 * 1024 }
  return {
    form,
    instanceConfig: form,
    options: { bootModes: [], templates: { featured: { template: [template] } } },
    params: { serviceOfferings: { options: {} } },
    dataPreFill: { minrootdisksize: 8 },
    deleteFrom: DeployVM.methods.deleteFrom,
    resetFromTemplateConfiguration: DeployVM.methods.resetFromTemplateConfiguration,
    handleSearchFilter: jest.fn(),
    fetchBootModes: jest.fn(),
    resetTemplateAssociatedResources: jest.fn(),
    updateTemplateLinkedUserData: jest.fn(),
    updateTemplateParameters: jest.fn()
  }
}

describe('DeployVM root disk size on template selection', () => {
  it('keeps the root disk size when the same template is selected again', () => {
    const context = createContext({ templateid: 'template-1', rootdisksize: 8 })

    DeployVM.methods.updateFieldValue.call(context, 'templateid', 'template-1')

    expect(context.form.rootdisksize).toBe(8)
  })

  it('clears the root disk size when another template is selected', () => {
    const context = createContext({ templateid: 'template-0', rootdisksize: 20 })

    DeployVM.methods.updateFieldValue.call(context, 'templateid', 'template-1')

    expect(context.form.rootdisksize).toBeUndefined()
  })
})
