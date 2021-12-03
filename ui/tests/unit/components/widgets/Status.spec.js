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

import common from '../../../common'
import mockData from '../../../mockData/Status.mock.json'
import Status from '@/components/widgets/Status'

let router, i18n

router = common.createMockRouter(mockData.routes)
i18n = common.createMockI18n('en', mockData.messages)

const factory = (opts = {}) => {
  router = opts.router || router
  i18n = opts.i18n || i18n

  return common.createFactory(Status, {
    router,
    i18n,
    props: opts.props || {},
    data: opts.data || {}
  })
}

describe('Components > Widgets > Status.vue', () => {
  describe('Methods', () => {
    describe('getText()', () => {
      it('getText() is called and return value is empty', async () => {
        const wrapper = factory({ props: { text: 'Running', displayText: false } })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text"></span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Running`', async () => {
        const wrapper = factory({
          props: { text: 'Running', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Running</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Stopped`', async () => {
        const wrapper = factory({
          props: { text: 'Stopped', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Stopped</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Starting`', async () => {
        const wrapper = factory({
          props: { text: 'Starting', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Starting</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Stopping`', async () => {
        const wrapper = factory({
          props: { text: 'Stopping', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Stopping</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Suspended`', async () => {
        const wrapper = factory({
          props: { text: 'Suspended', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Suspended</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Pending`', async () => {
        const wrapper = factory({
          props: { text: 'Pending', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Pending</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Expunging`', async () => {
        const wrapper = factory({
          props: { text: 'Expunging', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Expunging</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called and return value equal `Error`', async () => {
        const wrapper = factory({
          props: { text: 'Error', displayText: true }
        })
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Error</span>'

        expect(received).toContain(expected)
      })
    })

    describe('getBadgeStatus()', () => {
      it('getBadgeStatus() is called and return is default status', () => {
        const wrapper = factory({
          props: {
            text: 'Another',
            displayText: true
          }
        })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-default"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and return is success status', () => {
        const wrapper = factory({
          props: {
            text: 'Active',
            displayText: true
          }
        })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-success"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and return is error status', () => {
        const wrapper = factory({
          props: {
            text: 'Disabled',
            displayText: true
          }
        })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-error"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and return is processing status', () => {
        const wrapper = factory({
          props: {
            text: 'Migrating',
            displayText: true
          }
        })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-processing"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and return is error status with state equal `Alert`', () => {
        const wrapper = factory({
          props: {
            text: 'Alert',
            displayText: true
          }
        })
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-error"></span><span class="ant-badge-status-text">Alert</span></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and return is success status with state equal `Allocated`', async () => {
        const wrapper = factory({
          props: {
            text: 'Allocated',
            displayText: true
          }
        })
        router.push({ name: 'testRouter1' })
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Allocated</span></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and return is warning status with state equal `Allocated`', async () => {
        const wrapper = factory({
          props: {
            text: 'Allocated',
            displayText: true
          }
        })
        router.push('/')
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-warning"></span><span class="ant-badge-status-text">Allocated</span></span>'

        expect(received).toContain(expected)
      })
    })

    describe('getTooltip()', () => {
      it('getTooltip() is called with `$route.path` equal `/vmsnapshot`', async () => {
        const wrapper = factory({ props: { text: 'Active', displayText: true } })
        router.push({ name: 'testRouter2' })
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/vm`', async () => {
        const wrapper = factory({ props: { text: 'Active', displayText: true } })
        router.push({ name: 'testRouter3' })
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/volume`', async () => {
        const wrapper = factory({ props: { text: 'Active', displayText: true } })
        router.push({ name: 'testRouter4' })
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/guestnetwork`', async () => {
        const wrapper = factory({ props: { text: 'Active', displayText: true } })
        router.push({ name: 'testRouter5' })
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/publicip`', async () => {
        const wrapper = factory({ props: { text: 'Active', displayText: true } })
        router.push({ name: 'testRouter1' })
        await router.isReady()
        await flushPromises()
        const received = wrapper.html()
        const expected = '<span style="display: inline-flex;" class="ant-badge ant-badge-status ant-badge-not-a-wrapper"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })
    })
  })
})
