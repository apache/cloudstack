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

import Status from '@/components/widgets/Status'
import common from '../../../common'
import mockData from '../../../mockData/Status.mock.json'

let router, i18n

router = common.createMockRouter()
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
      it('getText() is called and the value returned is null', () => {
        const propsData = {
          text: 'Running',
          displayText: false
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text"></span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Running', () => {
        const propsData = {
          text: 'Running',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Running</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Stopped', () => {
        const propsData = {
          text: 'Stopped',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Stopped</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Starting', () => {
        const propsData = {
          text: 'Starting',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Starting</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Stopping', () => {
        const propsData = {
          text: 'Stopping',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Stopping</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Suspended', () => {
        const propsData = {
          text: 'Suspended',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Suspended</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Pending', () => {
        const propsData = {
          text: 'Pending',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Pending</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Expunging', () => {
        const propsData = {
          text: 'Expunging',
          displayText: true
        }

        const wrapper = factory({ props: propsData })

        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Expunging</span>'

        expect(received).toContain(expected)
      })

      it('getText() is called with state equal Error', () => {
        const propsData = {
          text: 'Error',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-text">Error</span>'

        expect(received).toContain(expected)
      })
    })

    describe('getBadgeStatus()', () => {
      it('getBadgeStatus() is called and the value returned is default status', () => {
        const propsData = {
          text: 'Another',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-default"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is success status', () => {
        const propsData = {
          text: 'Active',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-success"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is error status', () => {
        const propsData = {
          text: 'Disabled',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-error"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is processing status', () => {
        const propsData = {
          text: 'Migrating',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge-status-dot ant-badge-status-processing"></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is error status', () => {
        const propsData = {
          text: 'Alert',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-error"></span><span class="ant-badge-status-text">Alert</span></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is warning status with state equal Allocated', () => {
        const propsData = {
          text: 'Allocated',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-warning"></span><span class="ant-badge-status-text">Allocated</span></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is success status with state equal Allocated', () => {
        const propsData = {
          text: 'Allocated',
          displayText: true
        }

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/publicip',
          meta: {
            icon: 'test-router-1'
          }
        }])
        router.push({ name: 'testRouter1' })

        const wrapper = factory({ router: router, props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Allocated</span></span>'

        expect(received).toContain(expected)
      })

      it('getBadgeStatus() is called and the value returned is warning status with state equal Created', () => {
        const propsData = {
          text: 'Created',
          displayText: true
        }

        const wrapper = factory({ props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-warning"></span><span class="ant-badge-status-text">Created</span></span>'

        expect(received).toContain(expected)
      })
    })

    describe('getTooltip()', () => {
      it('getTooltip() is called with `$route.path` equal `/vmsnapshot`', () => {
        const propsData = {
          text: 'Active',
          displayText: true
        }

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/vmsnapshot',
          meta: {
            icon: 'test-router-1'
          }
        }])
        router.push({ name: 'testRouter1' })

        const wrapper = factory({ router: router, props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/vm`', () => {
        const propsData = {
          text: 'Active',
          displayText: true
        }

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/vm',
          meta: {
            icon: 'test-router-1'
          }
        }])
        router.push({ name: 'testRouter1' })

        const wrapper = factory({ router: router, props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/volume`', () => {
        const propsData = {
          text: 'Active',
          displayText: true
        }

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/volume',
          meta: {
            icon: 'test-router-1'
          }
        }])
        router.push({ name: 'testRouter1' })

        const wrapper = factory({ router: router, props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/guestnetwork`', () => {
        const propsData = {
          text: 'Active',
          displayText: true
        }

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/guestnetwork',
          meta: {
            icon: 'test-router-1'
          }
        }])
        router.push({ name: 'testRouter1' })

        const wrapper = factory({ router: router, props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })

      it('getTooltip() is called with `$route.path` equal `/publicip`', () => {
        const propsData = {
          text: 'Active',
          displayText: true
        }

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/publicip',
          meta: {
            icon: 'test-router-1'
          }
        }])
        router.push({ name: 'testRouter1' })

        const wrapper = factory({ router: router, props: propsData })
        const received = wrapper.html()
        const expected = '<span class="ant-badge ant-badge-status ant-badge-not-a-wrapper" style="display: inline-flex;"><span class="ant-badge-status-dot ant-badge-status-success"></span><span class="ant-badge-status-text">Active</span></span>'

        expect(received).toContain(expected)
      })
    })
  })
})
