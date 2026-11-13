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

const sourceProfiles = {
  NsxIpDiscoveryProfileId: 'ip-profile',
  NsxMacDiscoveryProfileId: 'mac-profile',
  NsxSegmentSecurityProfileId: 'security-profile'
}

const createCloneContext = (details, provider = 'Nsx') => {
  const context = {
    ...CloneNetworkOffering.data(),
    ...CloneNetworkOffering.methods,
    resource: {
      id: 'source-offering',
      name: 'source-offering',
      traffictype: 'Guest',
      guestiptype: 'Isolated',
      networkmode: 'NATTED',
      service: [{ name: 'Firewall', provider: [{ name: provider }] }],
      details: { ...details }
    },
    handleRemoveFields: mixinForm.methods.handleRemoveFields,
    $t: key => key,
    $nextTick: callback => callback(),
    $message: { success: jest.fn() },
    $emit: jest.fn(),
    $notifyError: jest.fn(),
    closeAction: jest.fn()
  }
  context.initForm()
  context.populateFormFromResource()
  context.$refs = Object.fromEntries(Object.keys(context.form).map(key => [key, {}]))
  context.formRef.value = { validate: () => Promise.resolve() }
  return context
}

const submitClone = async context => {
  context.handleSubmit()
  await flushPromises()
  expect(context.$notifyError).not.toHaveBeenCalled()
  expect(axios).toHaveBeenCalledTimes(1)
  return axios.mock.calls[0][0].data
}

const getSubmittedDetails = request => Object.fromEntries(
  [...request.entries()].filter(([key]) => key.startsWith('details['))
)

const selectReplacementPackage = context => {
  context.selectedServiceProviderMap = { Lb: 'Netscaler' }
  context.registeredServicePackages = [{ id: 'old-package' }, { id: 'new-package' }]
  context.form.netscalerservicepackages = 1
  context.form.netscalerservicepackagesdescription = 'new package description'
  // These request-serialization tests exercise package selection independently of autoscaling capabilities.
  delete context.form.vmautoscalingcapability
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

  it('retains all source profiles when the populated form is unchanged', async () => {
    const context = createCloneContext(sourceProfiles)

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({
      'details[0].nsxipdiscoveryprofileid': 'ip-profile',
      'details[0].nsxmacdiscoveryprofileid': 'mac-profile',
      'details[0].nsxsegmentsecurityprofileid': 'security-profile'
    })
    expect(request.has('cleanupdetails')).toBe(false)
  })

  it('explicitly clears all source profiles when all profile fields are emptied', async () => {
    const context = createCloneContext(sourceProfiles)
    context.form.nsxipdiscoveryprofileid = ''
    context.form.nsxmacdiscoveryprofileid = ''
    context.form.nsxsegmentsecurityprofileid = ''

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({})
    expect(request.get('cleanupdetails')).toBe('true')
    expect(context.resource.details).toEqual(sourceProfiles)
  })

  it.each(Object.keys(sourceProfiles))('clears the last configured %s profile', async key => {
    const context = createCloneContext({ [key]: sourceProfiles[key] })
    context.form[key.toLowerCase()] = ''

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({})
    expect(request.get('cleanupdetails')).toBe('true')
  })

  it('removes one source profile while retaining the other profiles', async () => {
    const context = createCloneContext(sourceProfiles)
    context.form.nsxipdiscoveryprofileid = ''

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({
      'details[0].nsxmacdiscoveryprofileid': 'mac-profile',
      'details[0].nsxsegmentsecurityprofileid': 'security-profile'
    })
    expect(request.has('cleanupdetails')).toBe(false)
  })

  it('preserves unexposed details while excluding fields with independent API parameters', async () => {
    const context = createCloneContext({
      NsxIpDiscoveryProfileId: 'ip-profile',
      InternalLbProvider: 'InternalLbVm',
      internetProtocol: 'ipv4',
      domainid: ['domain-id'],
      zoneid: ['zone-id']
    })
    context.form.nsxipdiscoveryprofileid = ''

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({ 'details[0].internallbprovider': 'InternalLbVm' })
    expect(request.has('cleanupdetails')).toBe(false)
  })

  it('replaces form-owned detail keys without retaining differently cased source copies', async () => {
    const context = createCloneContext({ NSXIPDISCOVERYPROFILEID: 'source-profile' })
    expect(context.form.nsxipdiscoveryprofileid).toBe('source-profile')
    context.form.nsxipdiscoveryprofileid = 'replacement-profile'

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({ 'details[0].nsxipdiscoveryprofileid': 'replacement-profile' })
  })

  it('preserves existing VMware details on a non-NSX clone', async () => {
    const context = createCloneContext({
      PromiscuousMode: 'false',
      MacAddressChanges: 'true',
      ForgedTransmits: 'false',
      MacLearning: 'true',
      RelatedNetworkOffering: 'related-offering'
    }, 'VirtualRouter')

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({
      'details[0].promiscuousmode': 'false',
      'details[0].macaddresschanges': 'true',
      'details[0].forgedtransmits': 'false',
      'details[0].maclearning': 'true',
      'details[0].relatednetworkoffering': 'related-offering'
    })
    expect(request.has('cleanupdetails')).toBe(false)
    expect(request.has('provider')).toBe(false)
  })

  it.each([
    ['canonical', 'servicepackageuuid', 'servicepackagedescription'],
    ['mixed-case', 'ServicePackageUuid', 'SERVICEPACKAGEDESCRIPTION']
  ])('keeps the selected NetScaler package over %s source details and merges request indices', async (description, uuidKey, descriptionKey) => {
    const sourceDetails = {
      [uuidKey]: 'old-package',
      [descriptionKey]: 'old package description',
      RelatedNetworkOffering: 'related-offering'
    }
    const context = createCloneContext(sourceDetails, 'VirtualRouter')
    selectReplacementPackage(context)

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({
      'details[0].servicepackageuuid': 'new-package',
      'details[0].servicepackagedescription': 'new package description',
      'details[0].relatednetworkoffering': 'related-offering'
    })
    expect(request.has('cleanupdetails')).toBe(false)
    expect(context.resource.details).toEqual(sourceDetails)
  })

  it('retains a selected package when the last editable source detail is cleared', async () => {
    const context = createCloneContext({ PromiscuousMode: 'false' }, 'VirtualRouter')
    context.form.promiscuousmode = ''
    selectReplacementPackage(context)

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({
      'details[0].servicepackageuuid': 'new-package',
      'details[0].servicepackagedescription': 'new package description'
    })
    expect(request.has('cleanupdetails')).toBe(false)
  })

  it('omits cleanup when the source has no optional details', async () => {
    const context = createCloneContext({})

    const request = await submitClone(context)

    expect(getSubmittedDetails(request)).toEqual({})
    expect(request.has('cleanupdetails')).toBe(false)
  })
})
