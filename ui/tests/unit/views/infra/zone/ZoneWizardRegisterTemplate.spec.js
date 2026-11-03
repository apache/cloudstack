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

import { mount, flushPromises } from '@vue/test-utils'
import { getAPI, postAPI } from '@/api'
import ZoneWizardRegisterTemplate, { normalizeUrl } from '@/views/infra/zone/ZoneWizardRegisterTemplate.vue'

jest.mock('@/api', () => ({
  getAPI: jest.fn(),
  postAPI: jest.fn()
}))

const CATALOG = [
  { id: 'a-x86', name: 'Alpha', arch: 'x86_64', url: 'https://example.com/a.qcow2' },
  { id: 'a-arm', name: 'Alpha', arch: 'aarch64', url: 'https://example.com/a-arm.qcow2' },
  { id: 'b-x86', name: 'Beta', arch: 'x86_64', url: 'https://example.com/b.img' },
  { id: 'b-arm', name: 'Beta', arch: 'aarch64', url: 'https://example.com/b-arm.img' }
]

const ZONES = [
  { id: 'core-x86', name: 'core-x86', type: 'Core', allocationstate: 'Enabled' },
  { id: 'core-xen', name: 'core-xen', type: 'Core', allocationstate: 'Enabled' },
  { id: 'edge-multi', name: 'edge-multi', type: 'Edge', allocationstate: 'Enabled' },
  { id: 'core-arm', name: 'core-arm', type: 'Core', allocationstate: 'Enabled' },
  { id: 'core-empty', name: 'core-empty', type: 'Core', allocationstate: 'Enabled' }
]

const CLUSTERS = [
  { zoneid: 'core-x86', hypervisortype: 'KVM', arch: 'x86_64' },
  { zoneid: 'core-xen', hypervisortype: 'XenServer', arch: 'x86_64' },
  { zoneid: 'edge-multi', hypervisortype: 'KVM', arch: 'x86_64' },
  { zoneid: 'edge-multi', hypervisortype: 'KVM', arch: 'aarch64' },
  { zoneid: 'core-arm', hypervisortype: 'KVM', arch: 'aarch64' }
]

const mockApis = ({ templates = [], imageStores = [{ id: 'store' }] } = {}) => {
  getAPI.mockImplementation((command) => {
    switch (command) {
      case 'listZones':
        return Promise.resolve({ listzonesresponse: { zone: ZONES } })
      case 'listClusters':
        return Promise.resolve({ listclustersresponse: { cluster: CLUSTERS } })
      case 'listImageStores':
        return Promise.resolve({ listimagestoresresponse: { imagestore: imageStores } })
      case 'listTemplates':
        return Promise.resolve({ listtemplatesresponse: { template: templates } })
      default:
        return Promise.resolve({ listostypesresponse: { ostype: [{ id: 'os-1', name: 'Other' }] } })
    }
  })
  postAPI.mockImplementation(() => Promise.resolve({ registertemplateresponse: { template: [{ id: 'registered' }] } }))
}

const passthrough = { template: '<div><slot /></div>' }
const buttonStub = {
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" :class="{ loading }" @click="$emit(\'click\')"><slot /></button>'
}

const mounted = []
const factory = async (props = {}) => {
  const notification = { success: jest.fn(), error: jest.fn() }
  const wrapper = mount(ZoneWizardRegisterTemplate, {
    props,
    global: {
      mocks: {
        $t: (key, params) => params ? `${key} ${JSON.stringify(params)}` : key,
        $notification: notification
      },
      stubs: {
        'a-card': passthrough,
        'a-select': { template: '<div class="zone-select-stub"><slot /></div>' },
        'a-select-option': passthrough,
        'a-tag': passthrough,
        'a-alert': passthrough,
        'a-switch': true,
        'a-checkbox': true,
        'a-empty': passthrough,
        'a-table': true,
        'a-modal': passthrough,
        'a-divider': true,
        'a-button': buttonStub,
        OsLogo: true
      }
    }
  })
  mounted.push(wrapper)
  await flushPromises()
  return { wrapper, notification }
}

const selectZone = async (wrapper, zoneId) => {
  wrapper.vm.selectedZoneId = zoneId
  await wrapper.vm.refreshZone()
  await flushPromises()
}

const registerCalls = () => postAPI.mock.calls.filter(([command]) => command === 'registerTemplate')

describe('Views > infra > zone > ZoneWizardRegisterTemplate.vue', () => {
  afterEach(() => {
    mounted.splice(0).forEach(wrapper => wrapper.unmount())
  })

  beforeEach(() => {
    jest.clearAllMocks()
    mockApis()
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(CATALOG) }))
  })

  describe('wizard mode', () => {
    it('renders no zone select and filters rows by the arch prop', async () => {
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'aarch64', zoneSuperType: 'Core' })

      expect(wrapper.find('.zone-select').exists()).toBe(false)
      expect(wrapper.vm.visibleTemplates.map(t => t.id)).toEqual(['a-arm', 'b-arm'])
      expect(getAPI).not.toHaveBeenCalledWith('listZones', expect.anything())
    })

    it('shows Skip, then Done once a row is ticked', async () => {
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })

      expect(wrapper.find('.button-back').text()).toBe('label.skip')
      wrapper.vm.selectedRowKeys = ['a-x86']
      await flushPromises()
      expect(wrapper.find('.button-back').text()).toBe('label.done')
    })

    it('registers into the zoneid prop with ispublic and isfeatured on by default', async () => {
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()

      const [, params] = registerCalls()[0]
      expect(params).toMatchObject({ zoneid: 'new-zone', hypervisor: 'KVM', ispublic: true, isfeatured: true })
    })

    it('sends ispublic and isfeatured following the switches', async () => {
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.makePublic = false
      wrapper.vm.makeFeatured = false
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()

      const [, params] = registerCalls()[0]
      expect(params).toMatchObject({ ispublic: false, isfeatured: false })
    })
  })

  describe('standalone mode', () => {
    it('renders the zone select, shows Close and disables Register until a row is ticked', async () => {
      const { wrapper } = await factory()

      expect(wrapper.find('.zone-select').exists()).toBe(true)
      expect(wrapper.find('.button-back').text()).toBe('label.close')
      await selectZone(wrapper, 'core-x86')
      expect(wrapper.find('.register-button').attributes('disabled')).toBeDefined()
      wrapper.vm.selectedRowKeys = ['a-x86']
      await flushPromises()
      expect(wrapper.find('.register-button').attributes('disabled')).toBeUndefined()
    })

    it('disables the Public and Featured checkboxes while Register is disabled', async () => {
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')
      const disabled = () => wrapper.findAll('a-checkbox-stub').map(box => box.attributes('disabled') === 'true')

      expect(wrapper.vm.registerDisabled).toBe(true)
      expect(disabled()).toEqual([true, true])
      wrapper.vm.selectedRowKeys = ['a-x86']
      await flushPromises()
      expect(wrapper.vm.registerDisabled).toBe(false)
      expect(disabled()).toEqual([false, false])
    })

    it('disables zones without KVM clusters but keeps zones with no clusters selectable', async () => {
      const { wrapper } = await factory()
      const eligible = Object.fromEntries(ZONES.map(zone => [zone.id, wrapper.vm.isZoneEligible(zone)]))

      expect(eligible).toEqual({
        'core-x86': true,
        'core-xen': false,
        'edge-multi': true,
        'core-arm': true,
        'core-empty': true
      })
    })

    it('shows rows for the zone KVM archs and reveals the others disabled with a reason', async () => {
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')

      expect(wrapper.vm.visibleTemplates.map(t => t.id)).toEqual(['a-x86', 'b-x86'])
      wrapper.vm.showOtherArchs = true
      const others = wrapper.vm.visibleTemplates.filter(t => t.arch === 'aarch64')
      expect(others).toHaveLength(2)
      others.forEach(t => {
        expect(t.disabled).toBe(true)
        expect(t.reason).toContain('message.catalog.reason.arch')
      })
    })

    it('lists both archs for a multi-arch zone and all archs for a zone without clusters', async () => {
      const { wrapper } = await factory()

      await selectZone(wrapper, 'edge-multi')
      expect(wrapper.vm.visibleTemplates).toHaveLength(4)
      await selectZone(wrapper, 'core-empty')
      expect(wrapper.vm.visibleTemplates).toHaveLength(4)
      expect(wrapper.vm.noClusters).toBe(true)
    })

    it('rebuilds rows and clears the selection when the zone changes', async () => {
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')
      wrapper.vm.selectedRowKeys = ['a-x86']
      await selectZone(wrapper, 'core-arm')

      expect(wrapper.vm.selectedRowKeys).toEqual([])
      expect(wrapper.vm.visibleTemplates.map(t => t.id)).toEqual(['a-arm', 'b-arm'])
    })

    it('rewrites https to http with directdownload for Edge and restores https for Core', async () => {
      const { wrapper } = await factory()

      await selectZone(wrapper, 'edge-multi')
      expect(wrapper.vm.visibleTemplates.every(t => t.url.startsWith('http://'))).toBe(true)
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()
      expect(registerCalls()[0][1]).toMatchObject({ directdownload: true, url: 'http://example.com/a.qcow2' })

      await selectZone(wrapper, 'core-x86')
      expect(wrapper.vm.visibleTemplates.every(t => t.url.startsWith('https://'))).toBe(true)
      expect(wrapper.vm.catalog.every(t => t.url.startsWith('https://'))).toBe(true)
    })

    it('hides templates already registered in the zone and shows them with their state on demand', async () => {
      mockApis({ templates: [{ url: 'http://EXAMPLE.com/a.qcow2/', isready: false, status: 'Downloading 45%' }] })
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')

      expect(wrapper.vm.visibleTemplates.map(t => t.id)).toEqual(['b-x86'])
      wrapper.vm.showRegistered = true
      const row = wrapper.vm.visibleTemplates.find(t => t.id === 'a-x86')
      expect(row.disabled).toBe(true)
      expect(wrapper.vm.statusText(row)).toContain('Downloading 45%')
    })

    it('warns and disables Register when a Core zone has no image store', async () => {
      mockApis({ imageStores: [] })
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')
      wrapper.vm.selectedRowKeys = ['a-x86']
      await flushPromises()

      expect(wrapper.vm.hasImageStore).toBe(false)
      expect(wrapper.find('.register-button').attributes('disabled')).toBeDefined()
    })

    it('reports a catalog fetch failure and keeps Register disabled', async () => {
      global.fetch = jest.fn(() => Promise.resolve({ ok: false, status: 404 }))
      jest.spyOn(console, 'error').mockImplementation(() => {})
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')

      expect(wrapper.vm.catalogError).toContain('404')
      expect(wrapper.vm.registerDisabled).toBe(true)
    })

    it('registers into the selected zone and follows the public and featured switches', async () => {
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')
      wrapper.vm.makePublic = false
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()

      expect(registerCalls()[0][1]).toMatchObject({ zoneid: 'core-x86', ispublic: false, isfeatured: true })
    })

    it('emits refresh-data and close-action on Close only after something was registered', async () => {
      const { wrapper } = await factory()
      await selectZone(wrapper, 'core-x86')
      wrapper.vm.handleClose()
      expect(wrapper.emitted('refresh-data')).toBeUndefined()
      expect(wrapper.emitted('close-action')).toHaveLength(1)

      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()
      wrapper.vm.handleClose()
      expect(wrapper.emitted('refresh-data')).toHaveLength(1)
    })
  })

  describe('registration errors', () => {
    it('leaves a row selectable with the API error shown when registerTemplate fails', async () => {
      postAPI.mockImplementation(() => Promise.reject({ response: { headers: { 'x-description': 'Unreachable URL' } } })) // eslint-disable-line prefer-promise-reject-errors
      const { wrapper, notification } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()

      const row = wrapper.vm.predefinedTemplates.find(t => t.id === 'a-x86')
      expect(row.disabled).toBeFalsy()
      expect(wrapper.vm.selectedRowKeys).toEqual(['a-x86'])
      expect(wrapper.vm.statusText(row)).toContain('Unreachable URL')
      expect(notification.error).toHaveBeenCalledTimes(1)
    })

    it('settles on a network error with no response and resets submitting', async () => {
      postAPI.mockImplementation(() => Promise.reject(new Error('Network Error')))
      const { wrapper, notification } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()

      expect(notification.error).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.submitting).toBe(false)
      expect(wrapper.vm.rowErrors['a-x86']).toBe('Network Error')
    })

    it('shows the state returned by registerTemplate, or Pending when none is returned', async () => {
      postAPI.mockImplementationOnce(() => Promise.resolve({ registertemplateresponse: { template: [{ id: 'x', isready: false, status: 'Download Pending' }] } }))
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()
      expect(wrapper.vm.statusText({ id: 'a-x86' })).toContain('Download Pending')

      postAPI.mockImplementationOnce(() => Promise.resolve({ registertemplateresponse: { template: [{ id: 'y' }] } }))
      wrapper.vm.selectedRowKeys = ['b-x86']
      await wrapper.vm.stepRegisterTemplates()
      expect(wrapper.vm.statusText({ id: 'b-x86' })).toContain('label.catalog.status.pending')
    })

    it('retries a failed row successfully', async () => {
      postAPI.mockImplementationOnce(() => Promise.reject(new Error('boom')))
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.selectedRowKeys = ['a-x86']
      await wrapper.vm.stepRegisterTemplates()
      await wrapper.vm.stepRegisterTemplates()

      expect(registerCalls()).toHaveLength(2)
      expect(wrapper.vm.deployedTemplates['a-x86']).toBe(true)
      expect(wrapper.vm.rowErrors['a-x86']).toBeUndefined()
    })

    it('sends no extra registerTemplate calls on a second click while submitting', async () => {
      const releases = []
      postAPI.mockImplementation(() => new Promise(resolve => {
        releases.push(() => resolve({ registertemplateresponse: { template: [{ id: 'registered' }] } }))
      }))
      const { wrapper } = await factory({ zoneid: 'new-zone', arch: 'x86_64', zoneSuperType: 'Core' })
      wrapper.vm.selectedRowKeys = ['a-x86', 'b-x86']

      const first = wrapper.vm.stepRegisterTemplates()
      await flushPromises()
      await wrapper.vm.stepRegisterTemplates()
      expect(registerCalls()).toHaveLength(2)
      expect(wrapper.vm.submitting).toBe(true)

      releases.forEach(release => release())
      await first
      expect(wrapper.vm.submitting).toBe(false)
    })
  })

  describe('normalizeUrl()', () => {
    it('treats scheme, host case, trailing slash and query string as equivalent', () => {
      const expected = normalizeUrl('https://a/b.img')

      expect(normalizeUrl('http://A/b.img/')).toBe(expected)
      expect(normalizeUrl('https://a/b.img?x=1')).toBe(expected)
    })

    it('keeps the path case-sensitive and distinguishes different paths', () => {
      expect(normalizeUrl('https://a/B.img')).not.toBe(normalizeUrl('https://a/b.img'))
      expect(normalizeUrl('https://a/b.img')).not.toBe(normalizeUrl('https://a/c.img'))
    })
  })
})
