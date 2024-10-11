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

import mockAxios from '../../../mock/mockAxios'
import common from '../../../common'
import mockData from '../../../mockData/ActionButton.mock.json'
import ActionButton from '@/components/view/ActionButton'

jest.mock('axios', () => mockAxios)

let router, store, i18n
const state = {
  user: {
    apis: mockData.apis
  }
}

store = common.createMockStore(state)
i18n = common.createMockI18n('en', mockData.messages)

const factory = (opts = {}) => {
  router = opts.router || router
  store = opts.store || store
  i18n = opts.i18n || i18n

  return common.createFactory(ActionButton, {
    router,
    store,
    i18n,
    props: opts.props || {},
    data: opts.data || {}
  })
}

describe('Components > View > ActionButton.vue', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    jest.spyOn(console, 'warn').mockImplementation(() => {})
  })

  describe('Template', () => {
    it('The action button is displayed', () => {
      const wrapper = factory()

      const expected = '<i aria-label="icon: plus" class="anticon anticon-plus">'
      const received = wrapper.html()
      expect(received).not.toContain(expected)
    })

    it('The normal action button is displayed', () => {
      const wrapper = factory({
        props: {
          actions: [{
            label: 'label.action',
            api: 'test-api-case-1',
            showBadge: false,
            icon: 'plus-outlined',
            dataView: false,
            listView: true
          }],
          dataView: false,
          listView: true
        }
      })

      const expected = '<span role="img" aria-label="plus" class="anticon anticon-plus">'
      const received = wrapper.html()
      expect(received).toContain(expected)
    })

    it('The action button badge  is displayed', (done) => {
      mockAxios.mockImplementation(() => Promise.resolve({
        testapinameresponse: {
          count: 0,
          testapiname: []
        }
      }))
      const wrapper = factory({
        props: {
          actions: [
            {
              label: 'label.action',
              api: 'test-api-case-2',
              showBadge: true,
              icon: 'plus-outlined',
              dataView: true
            }
          ],
          dataView: true
        }
      })

      const wrapperHtml = wrapper.html()
      const received = common.decodeHtml(wrapperHtml)
      const expected = '<span class="ant-badge button-action-badge" disabled="false">'

      expect(received).toContain(expected)

      done()
    })
  })

  describe('Method', () => {
    describe('handleShowBadge()', () => {
      it('API should be called and return not empty', async (done) => {
        const postData = new URLSearchParams()
        mockAxios.mockResolvedValue({ testapinameresponse: { count: 2 } })
        const wrapper = factory({
          props: {
            actions: [
              {
                label: 'label.action',
                api: 'test-api-case-3',
                showBadge: true,
                icon: 'plus-outlined',
                dataView: true
              }
            ],
            dataView: true
          }
        })
        const expected = { 'test-api-case-3': { badgeNum: 2 } }

        await flushPromises()
        expect(mockAxios).toHaveBeenCalledTimes(1)
        expect(mockAxios).toHaveBeenCalledWith({
          data: postData,
          method: 'GET',
          params: {
            command: 'test-api-case-3',
            response: 'json'
          },
          url: '/'
        })
        expect(wrapper.vm.actionBadge).toEqual(expected)

        done()
      })

      it('API should be called and return empty', async (done) => {
        const postData = new URLSearchParams()
        mockAxios.mockResolvedValue({ data: [] })
        const wrapper = factory({
          props: {
            actions: [
              {
                label: 'label.action',
                api: 'test-api-case-4',
                showBadge: true,
                icon: 'plus-outlined',
                dataView: true
              }
            ],
            dataView: true
          }
        })
        const expected = { 'test-api-case-4': { badgeNum: 0 } }

        await flushPromises()
        expect(mockAxios).toHaveBeenCalledTimes(1)
        expect(mockAxios).toHaveBeenCalledWith({
          data: postData,
          method: 'GET',
          params: {
            command: 'test-api-case-4',
            response: 'json'
          },
          url: '/'
        })
        expect(wrapper.vm.actionBadge).toEqual(expected)

        done()
      })

      it('API should be called and throw eror', async (done) => {
        const postData = new URLSearchParams()
        mockAxios.mockRejectedValue('errMethodMessage')
        const wrapper = factory({
          props: {
            actions: [
              {
                label: 'label.action',
                api: 'test-api-case-5',
                showBadge: true,
                icon: 'plus-outlined',
                dataView: true
              }
            ],
            dataView: true
          }
        })

        await flushPromises()
        expect(mockAxios).toHaveBeenCalledTimes(1)
        expect(mockAxios).toHaveBeenCalledWith({
          data: postData,
          method: 'GET',
          params: {
            command: 'test-api-case-5',
            response: 'json'
          },
          url: '/'
        })
        expect(wrapper.vm.actionBadge).toEqual({})

        done()
      })
    })

    describe('execAction()', () => {
      it('check emitted events are executed', async () => {
        router = common.createMockRouter()
        const wrapper = factory({
          router,
          props: {
            actions: [
              {
                icon: 'plus-outlined',
                label: 'label.action',
                api: 'test-api-case-6',
                showBadge: false,
                dataView: true
              }
            ],
            dataView: true,
            resource: {
              id: 'test-resource-id'
            }
          }
        })
        const expected = {
          icon: 'plus-outlined',
          label: 'label.action',
          api: 'test-api-case-6',
          showBadge: false,
          dataView: true,
          resource: {
            id: 'test-resource-id'
          }
        }

        await wrapper.find('button').trigger('click')
        await flushPromises()

        expect(wrapper.emitted()['exec-action'][0]).toEqual([expected])
      })
    })
  })

  describe('Watcher', () => {
    describe('handleShowBadge()', () => {
      it('The handleShowBadge() is not called with an empty resource', async () => {
        const wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id'
            }
          }
        })
        wrapper.vm.hasOwnProperty = () => Object.hasOwnProperty
        const handleShowBadge = jest.spyOn(wrapper.vm, 'handleShowBadge')
        await wrapper.setProps({ resource: null })

        expect(handleShowBadge).not.toBeCalled()
      })

      it('The handleShowBadge() is not called with a resource have containing id null', async () => {
        const wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id'
            }
          }
        })
        wrapper.vm.hasOwnProperty = () => Object.hasOwnProperty
        const handleShowBadge = jest.spyOn(wrapper.vm, 'handleShowBadge')
        await wrapper.setProps({ resource: { id: null } })

        expect(handleShowBadge).not.toBeCalled()
      })

      it('The handleShowBadge() should be called with changed resource data', async () => {
        const wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id-1'
            }
          }
        })
        wrapper.vm.hasOwnProperty = () => Object.hasOwnProperty
        const handleShowBadge = jest.spyOn(wrapper.vm, 'handleShowBadge')
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id-2'
          }
        })

        expect(handleShowBadge).toHaveBeenCalledTimes(1)
      })
    })
  })
})
