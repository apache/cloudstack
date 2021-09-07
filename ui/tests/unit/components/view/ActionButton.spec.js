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

import mockAxios from '../../../mock/mockAxios'
import ActionButton from '@/components/view/ActionButton'
import common from '../../../common'
import mockData from '../../../mockData/ActionButton.mock.json'

jest.mock('axios', () => mockAxios)

let router, store, i18n
const state = {
  user: {
    apis: mockData.apis
  }
}
router = common.createMockRouter(mockData.routes)
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
  })

  describe('Template', () => {
    it('Button action is show', () => {
      const expected = '<i aria-label="icon: plus" class="anticon anticon-plus">'
      const wrapper = factory()

      wrapper.vm.$nextTick(() => {
        const received = wrapper.html()

        expect(received).not.toContain(expected)
      })
    })

    it('Normal button action is show', () => {
      const expected = '<i aria-label="icon: plus" class="anticon anticon-plus">'
      const propsData = {
        actions: [
          {
            label: 'label.action',
            api: 'test-api-case-1',
            showBadge: false,
            icon: 'plus',
            dataView: false,
            listView: true
          }
        ],
        dataView: false,
        listView: true
      }

      const wrapper = factory({ props: propsData })

      wrapper.vm.$nextTick(() => {
        const received = wrapper.html()

        expect(received).toContain(expected)
      })
    })

    it('Badge button action is show', (done) => {
      const expected = '<span class="button-action-badge ant-badge">'
      const propsData = {
        actions: [
          {
            label: 'label.action',
            api: 'test-api-case-2',
            showBadge: true,
            icon: 'plus',
            dataView: true
          }
        ],
        dataView: true
      }
      const dataMock = {
        testapinameresponse: {
          count: 0,
          testapiname: []
        }
      }

      mockAxios.mockImplementation(() => Promise.resolve(dataMock))

      const wrapper = factory({ props: propsData })

      wrapper.vm.$nextTick(() => {
        const wrapperHtml = wrapper.html()
        const received = common.decodeHtml(wrapperHtml)

        expect(received).toContain(expected)

        done()
      })
    })
  })

  describe('Method', () => {
    describe('handleShowBadge()', () => {
      it('check the api is called and returned is not null', (done) => {
        const postData = new URLSearchParams()
        const expected = { 'test-api-case-3': { badgeNum: 2 } }
        const dataMock = { testapinameresponse: { count: 2 } }
        const propsData = {
          actions: [
            {
              label: 'label.action',
              api: 'test-api-case-3',
              showBadge: true,
              icon: 'plus',
              dataView: true
            }
          ],
          dataView: true
        }

        mockAxios.mockResolvedValue(dataMock)

        const wrapper = factory({ props: propsData })

        setTimeout(() => {
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
      })

      it('check the api is called returned is null', (done) => {
        const postData = new URLSearchParams()
        const expected = { 'test-api-case-4': { badgeNum: 0 } }
        const dataMock = { data: [] }
        const propsData = {
          actions: [
            {
              label: 'label.action',
              api: 'test-api-case-4',
              showBadge: true,
              icon: 'plus',
              dataView: true
            }
          ],
          dataView: true
        }

        mockAxios.mockResolvedValue(dataMock)

        const wrapper = factory({ props: propsData })

        setTimeout(() => {
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
      })

      it('check the api is called and throws error', (done) => {
        const postData = new URLSearchParams()
        const propsData = {
          actions: [
            {
              label: 'label.action',
              api: 'test-api-case-5',
              showBadge: true,
              icon: 'plus',
              dataView: true
            }
          ],
          dataView: true
        }
        const errorMessage = 'errMethodMessage'

        mockAxios.mockImplementationOnce(() => Promise.reject(errorMessage))

        const wrapper = factory({ props: propsData })

        setTimeout(() => {
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
    })

    describe('execAction()', () => {
      it('check emitted events are executed', async () => {
        const expected = {
          icon: 'plus',
          label: 'label.action',
          api: 'test-api-case-6',
          showBadge: false,
          dataView: true,
          resource: {
            id: 'test-resource-id'
          }
        }
        const propsData = {
          actions: [
            {
              icon: 'plus',
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

        const wrapper = factory({ props: propsData })

        await wrapper.find('button').trigger('click')
        await wrapper.vm.$nextTick()

        expect(wrapper.emitted()['exec-action'][0]).toEqual([expected])
      })
    })
  })

  describe('Watcher', () => {
    describe('handleShowBadge()', () => {
      it('check handleShowBadge() is not called with empty resource', async () => {
        const wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id'
            }
          }
        })
        const handleShowBadge = jest.spyOn(wrapper.vm, 'handleShowBadge')
        wrapper.setProps({
          resource: null
        })
        await wrapper.vm.$nextTick()
        expect(handleShowBadge).not.toBeCalled()
      })

      it('check handleShowBadge() is not called with resource containing id null', async () => {
        const wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id'
            }
          }
        })

        const handleShowBadge = jest.spyOn(wrapper.vm, 'handleShowBadge')
        wrapper.setProps({
          resource: { id: null }
        })
        await wrapper.vm.$nextTick()
        expect(handleShowBadge).not.toBeCalled()
      })

      it('check handleShowBadge() is not called with changed resource data', async () => {
        const wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id-1'
            }
          }
        })

        wrapper.setProps({
          resource: {
            id: 'test-resource-id-2'
          }
        })
        const handleShowBadge = jest.spyOn(wrapper.vm, 'handleShowBadge')
        await wrapper.vm.$nextTick()
        expect(handleShowBadge).toHaveBeenCalledTimes(1)
      })
    })
  })
})
