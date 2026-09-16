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

const createContext = (template, fallbackHypervisor = 'KVM') => ({
  imageType: null,
  form: {
    hypervisor: fallbackHypervisor,
    isoid: 'old-iso',
    snapshotid: 'old-snapshot',
    templateid: null,
    volumeid: 'old-volume'
  },
  options: {
    bootModes: [],
    templates: {
      featured: {
        template: template ? [template] : []
      }
    }
  },
  dataPreFill: {},
  fetchBootModes: jest.fn(),
  resetFromTemplateConfiguration: jest.fn(),
  resetTemplateAssociatedResources: jest.fn(),
  updateTemplateLinkedUserData: jest.fn(),
  updateTemplateParameters: jest.fn(),
  updateSelectedTemplateHypervisor:
    DeployVM.methods.updateSelectedTemplateHypervisor
})

const createTemplate = (id, hypervisor) => ({
  id,
  details: {},
  hypervisor,
  isdynamicallyscalable: false,
  size: 0
})

describe('DeployVM selected template hypervisor', () => {
  it.each(['KVM', 'XenServer', 'VMware', 'External'])(
    'updates the deployment form to %s from selected template metadata',
    hypervisor => {
      const template = createTemplate('selected-template', hypervisor)
      const context = createContext(template)

      DeployVM.methods.updateFieldValue.call(
        context,
        'templateid',
        template.id
      )

      expect(context.form).toMatchObject({
        hypervisor,
        isoid: null,
        snapshotid: null,
        templateid: template.id,
        volumeid: null
      })
      expect(context.dataPreFill.hypervisorType).toBe(hypervisor)
    }
  )

  it('keeps the zone fallback when the selected template is not in the loaded results', () => {
    const context = createContext(null)

    DeployVM.methods.updateFieldValue.call(
      context,
      'templateid',
      'missing-template'
    )

    expect(context.form.hypervisor).toBe('KVM')
  })

  it('uses selected template metadata when no zone fallback is available', () => {
    const template = createTemplate('xen-template', 'XenServer')
    const context = createContext(template, null)

    DeployVM.methods.updateFieldValue.call(
      context,
      'templateid',
      template.id
    )

    expect(context.form.hypervisor).toBe('XenServer')
  })

  it('keeps the zone fallback when selected template metadata is incomplete', () => {
    const template = createTemplate('incomplete-template')
    const context = createContext(template)

    DeployVM.methods.updateFieldValue.call(
      context,
      'templateid',
      template.id
    )

    expect(context.form.hypervisor).toBe('KVM')
  })

  it('reconciles a preselected template after its metadata loads', async () => {
    const template = createTemplate('prefilled-template', 'XenServer')
    const context = createContext(null)
    context.form.templateid = template.id
    context.getImageFilters = jest.fn(() => ['featured'])
    context.fetchTemplates = jest.fn(() => Promise.resolve({
      listtemplatesresponse: {
        count: 1,
        template: [template]
      }
    }))
    context.loading = { templates: false }

    await DeployVM.methods.fetchAllTemplates.call(context)

    expect(context.form.hypervisor).toBe('XenServer')
    expect(context.loading.templates).toBe(false)
  })
})
