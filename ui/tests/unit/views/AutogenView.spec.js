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
import { ref } from 'vue'

import mockAxios from '../../mock/mockAxios'
import AutogenView from '@/views/AutogenView'
import user from '@/store/modules/user'
import common from '../../common'
import mockData from '../../mockData/AutogenView.mock.json'

jest.mock('axios', () => mockAxios)
jest.mock('@/vue-app', () => ({
  vueProps: {
    $localStorage: {
      set: jest.fn((key, value) => {}),
      get: jest.fn((key) => {
        switch (key) {
          case 'HEADER_NOTICES':
            return []
          default:
            return null
        }
      })
    }
  }
}))
user.state.apis = mockData.apis

let router = null
let store = null
let i18n = null
let mocks = null
let wrapper = null
let originalFunc = {}

const spyConsole = {
  log: null,
  warn: null
}

const state = {
  user: {
    apis: mockData.apis,
    info: mockData.info,
    defaultListViewPageSize: 20,
    headerNotices: [],
    customColumns: []
  }
}

const mutations = {
  SET_HEADER_NOTICES: (state, jobsJsonArray) => {
    state.user.headerNotices = jobsJsonArray
  },
  SET_CUSTOM_COLUMNS: (state, customColumns) => {
    state.user.customColumns = customColumns
  }
}

const actions = {
  SetProject: jest.fn(({ commit }, project) => {}),
  ToggleTheme: jest.fn(({ commit }, theme) => {}),
  SetCustomColumns: jest.fn(({ commit }, columns) => {})
}

mockData.routes.push({
  name: 'testRouter15',
  path: '/test-router-15',
  meta: {
    title: 'label.title',
    icon: 'play-circle-outlined',
    permission: ['testApiNameCase1'],
    columns: [
      'id',
      'name',
      {
        column1: (record) => {
          return record.name
        }
      }
    ]
  },
  component: {},
  children: [{
    name: 'testRouter15-1',
    path: '/test-router-15/:id',
    meta: {
      title: 'label.title',
      icon: 'play-circle-outlined'
    },
    component: {}
  }]
})

router = common.createMockRouter(mockData.routes)
store = common.createMockStore(state, actions, mutations)
i18n = common.createMockI18n('en', mockData.messages)

mocks = {
  $notifyError: jest.fn((error) => {
    return error
  }),
  $notification: {
    error: jest.fn((option) => {
      return {
        message: option.message,
        description: 'test-description-error',
        duration: option.duration
      }
    }),
    info: jest.fn((option) => {
      return {
        message: option.message,
        description: 'test-description-info',
        duration: option.duration
      }
    }),
    success: jest.fn((option) => {
      return {
        message: option.message,
        description: 'test-description-success',
        duration: option.duration
      }
    }),
    warning: jest.fn((option) => {
      return {
        message: option.message,
        description: 'test-description-warning',
        duration: option.duration
      }
    })
  },
  $message: {
    success: jest.fn((obj) => {
      return obj
    }),
    error: jest.fn((obj) => {
      return obj
    }),
    info: jest.fn((obj) => {
      return obj
    })
  }
}

const factory = (opts = {}) => {
  router = opts.router || router
  i18n = opts.i18n || i18n
  store = opts.store || store
  mocks = opts.mocks || mocks

  return common.createFactory(AutogenView, {
    router,
    i18n,
    store,
    mocks,
    props: opts.props || {},
    data: opts.data || {}
  })
}

const { ResizeObserver, ls } = window
router.push('/')

describe('Views > AutogenView.vue', () => {
  beforeEach(async () => {
    jest.clearAllMocks()
    jest.spyOn(console, 'warn').mockImplementation(() => {})

    delete window.ResizeObserver
    delete window.ls

    if (!wrapper) {
      await router.isReady()
      wrapper = factory()
    }

    window.ResizeObserver = jest.fn().mockImplementation(() => ({
      observe: jest.fn(),
      unobserve: jest.fn(),
      disconnect: jest.fn()
    }))

    window.ls = {
      get: jest.fn((key) => []),
      set: jest.fn()
    }

    state.user.info.roletype = 'Normal'
    if (i18n.global.locale !== 'en') i18n.global.locale = 'en'
  })

  afterEach(() => {
    window.ResizeObserver = ResizeObserver
    window.ResizeObserver = ls

    if (wrapper) {
      wrapper.vm.currentAction = {}
      wrapper.vm.resource = {}
      wrapper.vm.searchParams = {}
      wrapper.vm.actionData = {}
      wrapper.vm.dataView = false
      wrapper.vm.showAction = false
      wrapper.vm.selectedRowKeys = []
      wrapper.vm.selectedRowKeys = []
      wrapper.vm.items = []
      wrapper.vm.promises = []
      wrapper.vm.form = {}
      wrapper.vm.rules = {}
    }

    if (Object.keys(originalFunc).length > 0) {
      Object.keys(originalFunc).forEach(key => {
        switch (key) {
          case 'fetchData':
            wrapper.vm.fetchData = originalFunc[key]
            break
          case 'listUuidOpts':
            wrapper.vm.listUuidOpts = originalFunc[key]
            break
          case 'fillEditFormFieldValues':
            wrapper.vm.fillEditFormFieldValues = originalFunc[key]
            break
          case 'validateTwoPassword':
            wrapper.vm.validateTwoPassword = originalFunc[key]
            break
          case 'handleResponse':
            wrapper.vm.handleResponse = originalFunc[key]
            break
          case 'closeAction':
            wrapper.vm.closeAction = originalFunc[key]
            break
          case 'getFirstIndexFocus':
            wrapper.vm.getFirstIndexFocus = originalFunc[key]
            break
          case 'setRules':
            wrapper.vm.setRules = originalFunc[key]
            break
          case 'RefValidateFields':
            wrapper.vm.formRef.value.validateFields = originalFunc[key]
            break
          case 'formRef':
            wrapper.vm.formRef = originalFunc[key]
            break
          case 'switchProject':
            wrapper.vm.switchProject = originalFunc[key]
            break
          case 'execSubmit':
            wrapper.vm.execSubmit = originalFunc[key]
            break
          case 'shouldNavigateBack':
            wrapper.vm.shouldNavigateBack = originalFunc[key]
            break
          case 'routerGo':
            wrapper.vm.$router.go = originalFunc[key]
            break
        }
      })

      originalFunc = {}
    }
    if (spyConsole.log) {
      spyConsole.log.mockClear()
      spyConsole.log.mockRestore()
    }
    if (spyConsole.warn) {
      spyConsole.warn.mockClear()
      spyConsole.warn.mockRestore()
    }
  })

  describe('Navigation Guard', () => {
    it('beforeRouteUpdate() should be called', async (done) => {
      originalFunc.fetchData = wrapper.vm.fetchData
      wrapper.vm.fetchData = jest.fn((args) => {})

      const nextFun = jest.fn()
      const beforeRouteUpdate = wrapper.vm.$options.beforeRouteUpdate

      await router.push({ name: 'testRouter1' })
      await beforeRouteUpdate.call(wrapper.vm, {}, {}, nextFun)
      await flushPromises()

      expect(wrapper.vm.currentPath).toEqual('/test-router-1')
      expect(nextFun).toHaveBeenCalled()
      done()
    })

    it('beforeRouteLeave() should be called', async (done) => {
      originalFunc.fetchData = wrapper.vm.fetchData
      wrapper.vm.fetchData = jest.fn((args) => {})

      const nextFun = jest.fn()
      const beforeRouteLeave = wrapper.vm.$options.beforeRouteLeave

      await router.push({ name: 'testRouter2' })
      await beforeRouteLeave.call(wrapper.vm, {}, {}, nextFun)
      await flushPromises()

      expect(wrapper.vm.currentPath).toEqual('/test-router-2')
      expect(nextFun).toHaveBeenCalled()
      done()
    })
  })

  describe('Watchers', () => {
    describe('$route', () => {
      it('The wrapper data does not change when $router do not change', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        await wrapper.setData({ page: 2, itemCount: 10 })
        await flushPromises()

        expect(wrapper.vm.page).toEqual(2)
        expect(wrapper.vm.itemCount).toEqual(10)
        expect(fetchData).not.toBeCalled()
        done()
      })

      it('The wrapper data changes when $router changes', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        await wrapper.setData({ page: 2, itemCount: 10 })
        await router.push({ name: 'testRouter3' })
        await flushPromises()

        expect(wrapper.vm.page).toEqual(1)
        expect(wrapper.vm.itemCount).toEqual(0)
        expect(fetchData).toBeCalled()
        done()
      })

      it('switchProject() should be called when $route changes with projectid in query', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        originalFunc.switchProject = wrapper.vm.switchProject
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.switchProject = jest.fn((projectid) => {})

        const switchProject = jest.spyOn(wrapper.vm, 'switchProject')

        await router.push({ name: 'testRouter30', query: { projectid: 'test-project-id' } })
        await flushPromises()

        expect(switchProject).toHaveBeenCalledTimes(1)
        expect(switchProject).toHaveBeenCalledWith('test-project-id')
        done()
      })
    })

    describe('$i18n.locale', () => {
      it('Check language and fetchData() when not changing locale', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        expect(wrapper.vm.$t('labelname')).toEqual('test-name-en')
        expect(fetchData).not.toBeCalled()
        done()
      })

      it('Check languages and fetchData() when changing locale', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        i18n.global.locale = 'de'
        await flushPromises()

        expect(wrapper.vm.$t('labelname')).toEqual('test-name-de')
        expect(fetchData).toBeCalled()
        done()
      })
    })
  })

  describe('Methods', () => {
    describe('switchProject', () => {
      it('API not called when switchProject() is called with not have projectId', async (done) => {
        await wrapper.vm.switchProject()
        await flushPromises()

        expect(mockAxios).not.toBeCalled()
        done()
      })

      it('API not called when switchProject() is called with projectId empty', async (done) => {
        await wrapper.vm.switchProject('')
        await flushPromises()

        expect(mockAxios).not.toBeCalled()
        done()
      })

      it('API not called when switchProject() is called with projectId length not equal 36', async (done) => {
        await wrapper.vm.switchProject('test-project-id')
        await flushPromises()

        expect(mockAxios).not.toBeCalled()
        done()
      })

      it('API will be called when switchProject() is called with projectId satisfying the condition', async (done) => {
        mockAxios.mockResolvedValue({})

        await router.push({ name: 'testRouter31' })
        await wrapper.vm.switchProject('111111111111111111111111111111111111')
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'listProjects',
            id: '111111111111111111111111111111111111',
            listall: true,
            details: 'min',
            response: 'json'
          }
        })
        done()
      })

      it('check $router not changes when API response with result empty', async (done) => {
        mockAxios.mockResolvedValue({ listprojectsresponse: {} })

        await router.push({ name: 'testRouter32' })
        await wrapper.vm.switchProject('111111111111111111111111111111111111')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-32')
        expect(router.currentRoute.value.query).toEqual({})
        done()
      })

      it('check $router, $store event when API response with result not empty', async (done) => {
        mockAxios.mockResolvedValue({ listprojectsresponse: { project: [{ id: 'project-id' }] } })

        await router.push({ name: 'testRouter33' })
        await wrapper.vm.switchProject('111111111111111111111111111111111111')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-33')
        expect(router.currentRoute.value.query).toEqual({})
        done()
      })
    })

    describe('fetchData()', () => {
      it('fetchData() should be return empty when $route.name equal `deployVirtualMachine`', async (done) => {
        await router.push({ name: 'deployVirtualMachine' })
        await wrapper.vm.fetchData()

        expect(wrapper.vm.items).toEqual([])
        done()
      })

      it('check routeName when fetchData() is called with $route.name is not empty', async (done) => {
        await router.push({ name: 'testRouter4' })
        await flushPromises()

        expect(wrapper.vm.routeName).toEqual('testRouter4')
        expect(wrapper.vm.items).toEqual([])
        done()
      })

      it('check routeName when fetchData() is called with $route.name is empty', async (done) => {
        await router.replace('/test-router-5')
        await flushPromises()

        expect(wrapper.vm.routeName).toEqual('testRouter5')
        done()
      })

      it('check resource, dataView when fetchData() is called with $route.meta.params is not empty', async (done) => {
        await router.push({ name: 'testRouter6', params: { id: 'test-id' } })
        await flushPromises()

        expect(wrapper.vm.resource).toEqual({})
        expect(wrapper.vm.dataView).toBeTruthy()
        done()
      })

      it('check columnKeys, actions when fetchData() is called with $route.meta.actions, route.meta.columns is not empty', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 0,
            testapinamecase1: []
          }
        })
        await router.push({ name: 'testRouter7' })
        await flushPromises()

        expect(wrapper.vm.columnKeys.length).toEqual(3)
        expect(wrapper.vm.actions.length).toEqual(1)
        expect(wrapper.vm.columnKeys).toEqual(['column1', 'column2', 'column3'])
        expect(wrapper.vm.actions).toEqual([{
          label: 'labelname',
          api: 'testApiNameCase1',
          icon: 'plus-outlined',
          listView: true
        }])
        done()
      })

      it('check columnKeys assign by store.getters.apis when fetchData() is called', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase4response: {
            count: 0,
            testapinamecase4: []
          }
        })
        await router.push({ name: 'testRouter8' })
        await flushPromises()

        expect(wrapper.vm.columnKeys.length).toEqual(3)
        expect(wrapper.vm.columnKeys).toEqual(['column1', 'column2', 'column3'])
        done()
      })

      it('check columnKeys assign by $route.meta.columns when fetchData() is called', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 0,
            testapinamecase1: []
          }
        })
        await router.push({ name: 'testRouter9' })
        await flushPromises()

        expect(wrapper.vm.columns.length).toEqual(2)
        expect(wrapper.vm.columns[0].key).toEqual('name')
        expect(wrapper.vm.columns[0].title).toEqual('name-en')
        expect(wrapper.vm.columns[0].dataIndex).toEqual('name')
        expect(typeof wrapper.vm.columns[0].sorter).toBe('function')
        expect(wrapper.vm.columns[1].key).toEqual('filtercolumn')
        expect(wrapper.vm.columns[1].dataIndex).toEqual('filtercolumn')
        expect(wrapper.vm.columns[1].customFilterDropdown).toBeTruthy()
        done(0)
      })

      it('API should be called with params assign by $route.query', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase2response: {
            count: 0,
            testapinamecase2: []
          }
        })
        await router.push({ name: 'testRouter10', query: { key: 'test-value' } })
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          data: new URLSearchParams(),
          method: 'GET',
          params: {
            command: 'testApiNameCase2',
            listall: true,
            key: 'test-value',
            page: 1,
            pagesize: 20,
            response: 'json'
          },
          url: '/'
        })

        done()
      })

      it('API should be called with params assign by $route.meta.params', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase3response: {
            count: 0,
            testapinamecase3: []
          }
        })
        await router.push({ name: 'testRouter11' })
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          data: new URLSearchParams(),
          method: 'GET',
          params: {
            command: 'testApiNameCase3',
            listall: true,
            key: 'test-value',
            page: 1,
            pagesize: 20,
            response: 'json'
          },
          url: '/'
        })
        done()
      })

      // it('API should be called with params has item id, name when $route.path startWith /ssh/', async (done) => {
      //   await mockAxios.mockResolvedValue({
      //     testapinamecase1response: {
      //       count: 0,
      //       testapinamecase1: [{
      //         id: 'test-id-1',
      //         name: 'test-name-1'
      //       }]
      //     }
      //   })
      //   await router.push({ name: 'testRouter12', params: { id: 'test-id' } })
      //   await flushPromises()

      //   expect(mockAxios).toHaveBeenCalled()
      //   expect(mockAxios).toHaveBeenLastCalledWith({
      //     url: '/',
      //     method: 'GET',
      //     data: new URLSearchParams(),
      //     params: {
      //       command: 'testApiNameCase1',
      //       listall: true,
      //       id: 'test-id',
      //       name: 'test-id',
      //       page: 1,
      //       pagesize: 20,
      //       response: 'json'
      //     }
      //   })
      //   done()
      // })

      it('API should be called with params has item id, hostname when $route.path startWith /ldapsetting/', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 0,
            testapinamecase1: [{
              id: 'test-id-1',
              name: 'test-name-1'
            }]
          }
        })
        await router.push({ name: 'testRouter13', params: { id: 'test-id' } })
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            listall: true,
            id: 'test-id',
            hostname: 'test-id',
            page: 1,
            pagesize: 20,
            response: 'json'
          }
        })
        done()
      })

      it('check items, resource when api is called with result is not empty', async (done) => {
        await mockAxios.mockResolvedValue({
          listtemplatesresponse: {
            count: 2,
            templates: [{
              id: 'uuid1',
              templateid: 'templateid-1',
              name: 'template-test-1'
            }, {
              id: 'uuid2',
              templateid: 'templateid-2',
              name: 'template-test-2'
            }]
          }
        })
        await router.push({ name: 'testRouter14' })
        await flushPromises()

        expect(wrapper.vm.items.length).toEqual(2)
        expect(wrapper.vm.items).toEqual([
          {
            id: 'uuid1',
            templateid: 'templateid-1',
            name: 'template-test-1',
            key: 0
          },
          {
            id: 'uuid2',
            templateid: 'templateid-2',
            name: 'template-test-2',
            key: 1
          }
        ])
        expect(wrapper.vm.resource).toEqual({
          id: 'uuid1',
          templateid: 'templateid-1',
          name: 'template-test-1',
          key: 0
        })
        done()
      })

      it('check items, resource when api is called and $route.meta.columns has function', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 1,
            testapinamecase1: [{
              id: 'test-id',
              name: 'test-name-value'
            }]
          }
        })
        await router.push({ name: 'testRouter15' })
        await flushPromises()

        expect(wrapper.vm.items).toEqual([{
          id: 'test-id',
          name: 'test-name-value',
          key: 0
        }])
        expect(wrapper.vm.resource).toEqual({
          id: 'test-id',
          name: 'test-name-value',
          key: 0
        })
        done()
      })

      // it('check items, resource when api is called and $route.path startWith /ssh', async (done) => {
      //   await mockAxios.mockResolvedValue({
      //     testapinamecase1response: {
      //       count: 1,
      //       testapinamecase1: [{
      //         name: 'test-name-value'
      //       }]
      //     }
      //   })
      //   await router.push({ name: 'testRouter16' })
      //   await flushPromises()
      //
      //   expect(wrapper.vm.items).toEqual([{
      //     id: 'test-name-value',
      //     name: 'test-name-value',
      //     key: 0
      //   }])
      //   expect(wrapper.vm.resource).toEqual({
      //     id: 'test-name-value',
      //     name: 'test-name-value',
      //     key: 0
      //   })
      //   done()
      // })

      it('check items, resource when api is called and $route.path startWith /ldapsetting', async (done) => {
        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 1,
            testapinamecase1: [{
              name: 'test-name-value',
              hostname: 'test-hostname-value'
            }]
          }
        })
        await router.push({ name: 'testRouter17' })
        await flushPromises()

        expect(wrapper.vm.items).toEqual([{
          id: 'test-hostname-value',
          name: 'test-name-value',
          hostname: 'test-hostname-value',
          key: 0
        }])
        expect(wrapper.vm.resource).toEqual({
          id: 'test-hostname-value',
          name: 'test-name-value',
          hostname: 'test-hostname-value',
          key: 0
        })
        done()
      })

      it('check $notifyError is called when api is called with throw error', async (done) => {
        const errorMock = {
          response: {},
          message: 'Error: throw exception error'
        }

        await mockAxios.mockRejectedValue(errorMock)
        await router.push({ name: 'testRouter18' })
        await flushPromises()

        expect(mocks.$notifyError).toHaveBeenCalledTimes(1)
        expect(mocks.$notifyError).toHaveBeenCalledWith(errorMock)
        done()
      })

      it('check $notifyError is called and router path = /exception/403 when api is called with throw error', async (done) => {
        const errorMock = {
          response: {
            status: 405
          },
          message: 'Error: Method Not Allowed'
        }

        await mockAxios.mockRejectedValue(errorMock)
        await router.push({ name: 'testRouter19' })
        await flushPromises()

        expect(mocks.$notifyError).toHaveBeenCalledTimes(1)
        expect(mocks.$notifyError).toHaveBeenCalledWith(errorMock)
        expect(router.currentRoute.value.path).toEqual('/exception/403')
        done()
      })

      it('check $notifyError is called and router path = /exception/404 when api is called with throw error', async (done) => {
        const errorMock = {
          response: {
            status: 430
          },
          message: 'Error: Request Header Fields Too Large'
        }

        await mockAxios.mockRejectedValue(errorMock)
        await router.push({ name: 'testRouter19' })
        await flushPromises()

        expect(mocks.$notifyError).toHaveBeenCalledTimes(1)
        expect(mocks.$notifyError).toHaveBeenCalledWith(errorMock)
        expect(router.currentRoute.value.path).toEqual('/exception/404')
        done()
      })

      it('check $notifyError is called and router path = /exception/500 when api is called with throw error', async (done) => {
        const errorMock = {
          response: {
            status: 530
          },
          message: 'Error: Site is frozen'
        }

        await mockAxios.mockRejectedValue(errorMock)
        await router.push({ name: 'testRouter19' })
        await flushPromises()

        expect(mocks.$notifyError).toHaveBeenCalledTimes(1)
        expect(mocks.$notifyError).toHaveBeenCalledWith(errorMock)
        expect(router.currentRoute.value.path).toEqual('/exception/500')
        done()
      })
    })

    describe('onSearch()', () => {
      it('check router when onSearch() is called with args empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter20', query: { page: 1, pagesize: 20 } })
        await wrapper.vm.onSearch()
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-20')
        expect(router.currentRoute.value.query).toEqual({ page: '1', pagesize: '20' })
        done()
      })

      it('check router when onSearch() is called with args not empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter21', query: { page: 1, pagesize: 20 } })
        await wrapper.vm.onSearch({ value: 'test-value' })
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-21')
        expect(router.currentRoute.value.query).toEqual({ page: '1', pagesize: '20', value: 'test-value' })
        done()
      })

      it('check router when onSearch() is called with args have searchQuery', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter22', query: { page: 1, pagesize: 20 } })
        await wrapper.vm.onSearch({ searchQuery: 'test-value' })
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-22')
        expect(router.currentRoute.value.query).toEqual({
          page: '1',
          pagesize: '20',
          keyword: 'test-value',
          q: 'test-value'
        })
        done()
      })

      it('check router when onSearch() is called with args have searchQuery and route.name equal `role`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'role' })
        await wrapper.vm.onSearch({ searchQuery: 'test-value' })
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/role')
        expect(router.currentRoute.value.query).toEqual({ keyword: 'test-value', q: 'test-value', page: '1', pagesize: '20' })
        done()
      })

      it('check router when onSearch() is called with args have searchQuery and route.name equal `quotaemailtemplate`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'quotaemailtemplate' })
        await wrapper.vm.onSearch({ searchQuery: 'test-value' })
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/quotaemailtemplate')
        expect(router.currentRoute.value.query).toEqual({ templatetype: 'test-value', q: 'test-value', page: '1', pagesize: '20' })
        done()
      })

      it('check router when onSearch() is called with args have searchQuery and route.name equal `quotaemailtemplate`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'globalsetting' })
        await wrapper.vm.onSearch({ searchQuery: 'test-value' })
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/globalsetting')
        expect(router.currentRoute.value.query).toEqual({ name: 'test-value', q: 'test-value', page: '1', pagesize: '20' })
        done()
      })

      it('fetchData() should be called when onSearch() is called', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')
        await router.push({ name: 'testRouter23', query: { page: 1, pagesize: 20, value: 'test-value' } })
        await wrapper.vm.onSearch({ page: '1', pagesize: '20', value: 'test-value' })
        await flushPromises()

        expect(fetchData).toHaveBeenLastCalledWith({ page: '1', pagesize: '20', value: 'test-value' })
        done()
      })
    })

    describe('closeAction()', () => {
      it('check currentAction, showAction when closeAction() is called', async (done) => {
        await wrapper.setData({
          currentAction: {
            label: 'label.name',
            loading: true,
            paramFields: []
          },
          showAction: true
        })
        await wrapper.vm.closeAction()
        await flushPromises()

        expect(wrapper.vm.currentAction).toEqual({})
        expect(wrapper.vm.showAction).toBeFalsy()
        done()
      })
    })

    describe('execAction()', () => {
      it('check showAction, actionData and router name when execAction() is called', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        await wrapper.setData({ actionData: { name: 'test-add-action' } })
        await wrapper.vm.execAction({
          label: 'labelname',
          icon: 'plus-outlined',
          component: {},
          api: 'testRouter24',
          popup: false
        })
        await flushPromises()

        expect(wrapper.vm.showAction).toBeFalsy()
        expect(router.currentRoute.value.name).toEqual('testRouter24')
        done()
      })

      it('check currentAction params and paramsField when execAction() is called', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        await wrapper.vm.execAction({
          label: 'label.name',
          api: 'testApiNameCase5',
          paramFields: []
        })
        await flushPromises()

        expect(wrapper.vm.currentAction.params).toEqual([
          { name: 'id', type: 'string' },
          { name: 'name', type: 'string' },
          { name: 'column1', type: 'string' },
          { name: 'column2', type: 'string' },
          { name: 'column3', type: 'string' }
        ])
        expect(wrapper.vm.currentAction.paramFields).toEqual([])
        expect(wrapper.vm.showAction).toBeTruthy()
        done()
      })

      it('check currentAction params and paramsField when execAction() is called with args is exists', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        await wrapper.vm.execAction({
          api: 'testApiNameCase5',
          args: ['column1', 'column2', 'column3']
        })
        await flushPromises()

        expect(wrapper.vm.currentAction.params).toEqual([
          { name: 'column1', type: 'string' },
          { name: 'column2', type: 'string' },
          { name: 'column3', type: 'string' },
          { name: 'name', type: 'string' },
          { name: 'id', type: 'string' }
        ])
        expect(wrapper.vm.currentAction.paramFields).toEqual([
          { name: 'column1', type: 'string' },
          { name: 'column2', type: 'string' },
          { name: 'column3', type: 'string' }
        ])
        expect(wrapper.vm.showAction).toBeTruthy()
        done()
      })

      it('check currentAction params and paramsField when execAction() is called with args is function', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        await wrapper.vm.execAction({
          api: 'testApiNameCase5',
          resource: { id: 'test-id-value', name: 'test-name-value' },
          args: (record, store) => {
            return ['Admin'].includes(store.userInfo.roletype) ? ['column1', 'column2', 'column3'] : ['id', 'name']
          }
        })
        await flushPromises()

        expect(wrapper.vm.currentAction.params).toEqual([
          { name: 'id', type: 'string' },
          { name: 'name', type: 'string' },
          { name: 'column1', type: 'string' },
          { name: 'column2', type: 'string' },
          { name: 'column3', type: 'string' }
        ])
        expect(wrapper.vm.currentAction.paramFields).toEqual([
          { name: 'id', type: 'string' },
          { name: 'name', type: 'string' }
        ])
        expect(wrapper.vm.showAction).toBeTruthy()
        done()
      })

      it('check currentAction paramsField and listUuidOpts() is called when execAction() is called', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        const listUuidOpts = jest.spyOn(wrapper.vm, 'listUuidOpts')
        await wrapper.vm.execAction({
          api: 'testApiNameCase6',
          args: ['id', 'tags', 'column1', 'column2', 'account'],
          mapping: {
            column2: () => {
              return 'test-value'
            }
          }
        })
        await flushPromises()

        expect(wrapper.vm.currentAction.paramFields).toEqual([
          { name: 'id', type: 'uuid' },
          { name: 'tags', type: 'string' },
          { name: 'column1', type: 'list' },
          { name: 'column2', type: 'string' },
          { name: 'account', type: 'string' }
        ])
        expect(wrapper.vm.showAction).toBeTruthy()
        expect(listUuidOpts).toHaveBeenCalledTimes(4)
        expect(listUuidOpts).toHaveBeenCalledWith({ name: 'id', type: 'uuid' })
        expect(listUuidOpts).toHaveBeenCalledWith({ name: 'column1', type: 'list' })
        expect(listUuidOpts).toHaveBeenCalledWith({ name: 'column2', type: 'string' })
        expect(listUuidOpts).toHaveBeenCalledWith({ name: 'account', type: 'string' })
        done()
      })

      it('check fillEditFormFieldValues() is called when execAction() is called', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        const fillEditFormFieldValues = jest.spyOn(wrapper.vm, 'fillEditFormFieldValues')

        await wrapper.vm.execAction({
          api: 'testApiNameCase6',
          dataView: true,
          icon: 'edit-outlined'
        })
        await flushPromises()

        expect(fillEditFormFieldValues).toHaveBeenCalled()
        done()
      })

      it('check currentAction paramFields when execAction() is called args has confirmpassword field', async (done) => {
        originalFunc.getFirstIndexFocus = wrapper.vm.getFirstIndexFocus
        originalFunc.setRules = wrapper.vm.setRules
        originalFunc.listUuidOpts = wrapper.vm.listUuidOpts
        originalFunc.fillEditFormFieldValues = wrapper.vm.fillEditFormFieldValues

        wrapper.vm.getFirstIndexFocus = jest.fn()
        wrapper.vm.setRules = jest.fn((param) => {})
        wrapper.vm.listUuidOpts = jest.fn((param) => {})
        wrapper.vm.fillEditFormFieldValues = jest.fn()

        await wrapper.vm.execAction({
          api: 'testApiNameCase6',
          args: ['confirmpassword'],
          mapping: {}
        })
        await flushPromises()

        expect(wrapper.vm.currentAction.paramFields).toEqual([
          { name: 'confirmpassword', type: 'password', required: true, description: 'confirmpassword-description-en' }
        ])
        done()
      })
    })

    describe('listUuidOpts()', () => {
      it('API not called when listUuidOpts() is called with currentAction.mapping.id is null', async (done) => {
        await wrapper.setData({
          currentAction: {
            mapping: {
              id: () => { return '' }
            }
          }
        })
        await wrapper.vm.listUuidOpts({ name: 'id', type: 'uuid' })
        await flushPromises()

        expect(mockAxios).not.toHaveBeenCalled()
        done()
      })

      it('API not called when listUuidOpts() is called with currentAction.mapping is empty', async (done) => {
        await wrapper.setData({
          currentAction: {
            mapping: {}
          }
        })
        await wrapper.vm.listUuidOpts({ name: 'test-name', type: 'uuid' })
        await flushPromises()

        expect(mockAxios).not.toHaveBeenCalled()
        done()
      })

      it('API should be called and param.opts when listUuidOpts() is called with currentAction.mapping[param.name].api', async (done) => {
        const param = { name: 'template', type: 'uuid' }

        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 1,
            testapinamecase1: [{
              id: 'test-id-value',
              name: 'test-name-value'
            }]
          }
        })
        await wrapper.setData({
          currentAction: {
            mapping: {
              template: {
                api: 'testApiNameCase1'
              }
            }
          }
        })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            listall: true,
            showicon: true,
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'template',
          type: 'uuid',
          loading: false,
          opts: [{
            id: 'test-id-value',
            name: 'test-name-value'
          }]
        })
        done()
      })

      it('API should be called when listUuidOpts() is called with store apis has api startWith param.name', async (done) => {
        const param = { name: 'testapiname', type: 'uuid' }

        await mockAxios.mockResolvedValue({
          listtestapinamesresponse: {
            count: 1,
            testapiname: [{
              id: 'test-id-value',
              name: 'test-name-value'
            }]
          }
        })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'listTestApiNames',
            listall: true,
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'testapiname',
          type: 'uuid',
          loading: false,
          opts: [{
            id: 'test-id-value',
            name: 'test-name-value'
          }]
        })
        done()
      })

      it('API should be called with params has item name and value assign by resource', async (done) => {
        const param = { name: 'template', type: 'uuid' }

        await mockAxios.mockResolvedValue({
          testapinamecase1response: {
            count: 0,
            testapinamecase1: [{
              id: 'test-id-value',
              name: 'test-name-value'
            }]
          }
        })
        await wrapper.setData({
          currentAction: {
            mapping: {
              template: {
                api: 'testApiNameCase1',
                params: (record) => {
                  return {
                    name: record.name
                  }
                }
              }
            }
          },
          resource: {
            id: 'test-id-value',
            name: 'test-name-value'
          }
        })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            listall: true,
            name: 'test-name-value',
            showicon: true,
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'template',
          type: 'uuid',
          loading: false,
          opts: [{
            id: 'test-id-value',
            name: 'test-name-value'
          }]
        })
        done()
      })

      it('API should be called with params has item templatefilter when apiName is listTemplates', async (done) => {
        const param = { name: 'id', type: 'uuid' }

        await mockAxios.mockResolvedValue({
          listtemplateresponse: {
            count: 1,
            templates: [{
              id: 'test-id-value',
              name: 'test-name-value'
            }]
          }
        })
        await wrapper.setData({
          apiName: 'listTemplates'
        })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'listTemplates',
            listall: true,
            templatefilter: 'executable',
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'id',
          type: 'uuid',
          loading: false,
          opts: [{
            id: 'test-id-value',
            name: 'test-name-value'
          }]
        })
        done()
      })

      it('API should be called with params has item isofilter when apiName is listIsos', async (done) => {
        const param = { name: 'id', type: 'uuid' }

        await mockAxios.mockResolvedValue({
          listisosresponse: {
            count: 1,
            iso: [{
              id: 'test-id-value',
              name: 'test-name-value'
            }]
          }
        })
        await wrapper.setData({ apiName: 'listIsos' })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'listIsos',
            listall: true,
            isofilter: 'executable',
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'id',
          type: 'uuid',
          loading: false,
          opts: [{
            id: 'test-id-value',
            name: 'test-name-value'
          }]
        })
        done()
      })

      it('API should be called with params has item type = routing when apiName is listHosts', async (done) => {
        const param = { name: 'id', type: 'uuid' }

        await mockAxios.mockResolvedValue({
          listhostresponse: {
            count: 1,
            hosts: [{
              id: 'test-id-value',
              name: 'test-name-value'
            }]
          }
        })
        await wrapper.setData({ apiName: 'listHosts' })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'listHosts',
            listall: true,
            type: 'routing',
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'id',
          type: 'uuid',
          loading: false,
          opts: [{
            id: 'test-id-value',
            name: 'test-name-value'
          }]
        })
        done()
      })

      it('API should be called and param.opts is empty when api throw error', async (done) => {
        const param = { name: 'id', type: 'uuid', loading: true }
        spyConsole.log = jest.spyOn(console, 'log').mockImplementation(() => {})

        await mockAxios.mockRejectedValue({
          response: {},
          stack: 'Error: throw exception error'
        })
        await wrapper.setData({ apiName: 'testApiNameCase1' })
        await wrapper.vm.listUuidOpts(param)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            listall: true,
            response: 'json'
          }
        })
        expect(param).toEqual({
          name: 'id',
          type: 'uuid',
          loading: false,
          opts: []
        })

        done()
      })
    })

    describe('pollActionCompletion()', () => {
      it('check $notification when pollActionCompletion() is called with action is empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        await mockAxios.mockResolvedValue({
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        })
        await wrapper.vm.pollActionCompletion('test-job-id', { label: 'label.name' })
        await flushPromises()

        expect(fetchData).toHaveBeenCalled()
        expect(mocks.$notification.info).not.toHaveBeenCalled()
        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'queryAsyncJobResult',
            jobId: 'test-job-id',
            response: 'json'
          }
        })
        done()
      })

      it('check $notification when pollActionCompletion() is called with action is not empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})
        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        await mockAxios.mockResolvedValue({
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        })
        await wrapper.vm.pollActionCompletion('test-job-id', {
          label: 'labelname',
          response: (jobResult) => {
            return jobResult.name
          }
        })
        await flushPromises()

        expect(fetchData).toHaveBeenCalled()
        expect(mocks.$notification.info).toHaveBeenCalled()
        expect(mocks.$notification.info).toHaveLastReturnedWith({
          message: 'test-name-en',
          description: 'test-description-info',
          duration: 0
        })
        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'queryAsyncJobResult',
            jobId: 'test-job-id',
            response: 'json'
          }
        })

        done()
      })

      it('fetchData() should be called when $pollJob error response', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})
        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        await mockAxios.mockResolvedValue({
          queryasyncjobresultresponse: {
            jobstatus: 2,
            jobresult: {
              errortext: 'test-error-message'
            }
          }
        })
        await wrapper.vm.pollActionCompletion('test-job-id', {
          label: 'labelname',
          response: (jobResult) => {
            return jobResult.name
          }
        })
        await flushPromises()

        expect(fetchData).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'queryAsyncJobResult',
            jobId: 'test-job-id',
            response: 'json'
          }
        })
        done()
      })
    })

    describe('fillEditFormFieldValues()', () => {
      it('form data should be empty when currentAction.paramFields empty', async (done) => {
        await wrapper.setData({ currentAction: { paramFields: [] } })
        await wrapper.vm.fillEditFormFieldValues()
        await flushPromises()

        expect(wrapper.vm.form).toEqual({})
        done()
      })

      it('form data should not empty when currentAction.paramFields has item type equal `list`', async (done) => {
        await wrapper.setData({
          currentAction: {
            paramFields: [
              { name: 'domainids', type: 'list' }
            ]
          },
          resource: {
            domainname: ['test-domain-value-1', 'test-domain-value-2']
          }
        })
        await wrapper.vm.fillEditFormFieldValues()
        await flushPromises()

        expect(wrapper.vm.form).toEqual({ domainids: ['test-domain-value-1', 'test-domain-value-2'] })
        done()
      })

      it('form data should not empty when currentAction.paramFields has item type equal `account`', async (done) => {
        await wrapper.setData({
          currentAction: {
            paramFields: [
              { name: 'account', type: 'string' }
            ]
          },
          resource: {
            account: 'test-account-value'
          }
        })
        await wrapper.vm.fillEditFormFieldValues()
        await flushPromises()

        expect(wrapper.vm.form).toEqual({ account: 'test-account-value' })
        done()
      })

      it('form data should not empty when currentAction.paramFields has item type not in [`list`, `account`]', async (done) => {
        await wrapper.setData({
          currentAction: {
            paramFields: [
              { name: 'name', type: 'string' }
            ]
          },
          resource: {
            name: 'test-name-value'
          }
        })
        await wrapper.vm.fillEditFormFieldValues()
        await flushPromises()

        expect(wrapper.vm.form).toEqual({ name: 'test-name-value' })
        done()
      })

      it('form data should be empty when currentAction.paramFields has item not exist in resource', async (done) => {
        await wrapper.setData({
          currentAction: {
            paramFields: [
              { name: 'templatename', type: 'string' }
            ]
          },
          resource: {
            name: 'test-name-value'
          }
        })
        await wrapper.vm.fillEditFormFieldValues()
        await flushPromises()

        expect(wrapper.vm.form).toEqual({})
        done()
      })
    })

    describe('changeFilter()', () => {
      it('check `route.query` when changeFilter() is called with empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter25' })
        await wrapper.vm.changeFilter()
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-25')
        expect(router.currentRoute.value.query).toEqual({
          filter: undefined,
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with $route has query', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter25', query: { templatefilter: 'template', account: 'test-account' } })
        await wrapper.vm.changeFilter('filter')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-25')
        expect(router.currentRoute.value.query).toEqual({
          filter: 'filter',
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with filter not empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter25' })
        await wrapper.vm.changeFilter('filter')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-25')
        expect(router.currentRoute.value.query).toEqual({
          filter: 'filter',
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with $route.name equal `template`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'template' })
        await wrapper.vm.changeFilter('filter')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/template')
        expect(router.currentRoute.value.query).toEqual({
          filter: 'filter',
          templatefilter: 'filter',
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with $route.name equal `iso`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'iso' })
        await wrapper.vm.changeFilter('filter')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/iso')
        expect(router.currentRoute.value.query).toEqual({
          filter: 'filter',
          isofilter: 'filter',
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with $route.name equal `guestnetwork`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'guestnetwork' })
        await wrapper.vm.changeFilter('filter')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/guestnetwork')
        expect(router.currentRoute.value.query).toEqual({
          filter: 'filter',
          networkfilter: 'filter',
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with filter equal `self` and $route.name equal `vm`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'vm' })
        await wrapper.vm.changeFilter('self')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/vm')
        expect(router.currentRoute.value.query).toEqual({
          account: 'test-account',
          domainid: 'test-domain-id',
          filter: 'self',
          page: '1',
          pagesize: '20'
        })
        done()
      })

      it('check `route.query` when changeFilter() is called with filter equal `running` and $route.name equal `vm`', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'vm' })
        await wrapper.vm.changeFilter('running')
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/vm')
        expect(router.currentRoute.value.query).toEqual({
          state: 'running',
          filter: 'running',
          page: '1',
          pagesize: '20'
        })
        done()
      })
    })

    describe('changePage()', () => {
      it('check $route query when changePage() is called with args not empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter26' })
        await wrapper.vm.changePage(2, 10)
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-26')
        expect(router.currentRoute.value.query).toEqual({ page: '2', pagesize: '10' })
        done()
      })

      it('check $route query when changePage() is called with args empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter26', query: { page: 1, pagesize: 10 } })
        await wrapper.vm.changePage()
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-26')
        expect(router.currentRoute.value.query).toEqual({ page: undefined, pagesize: undefined })
        done()
      })
    })

    describe('changePageSize()', () => {
      it('check $route query when changePageSize() is called with args empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter27' })
        await wrapper.vm.changePageSize()
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-27')
        expect(router.currentRoute.value.query).toEqual({ page: undefined, pagesize: undefined })
        done()
      })

      it('check $route query when changePageSize() is called with args not empty', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})

        await router.push({ name: 'testRouter27' })
        await wrapper.vm.changePageSize(2, 10)
        await flushPromises()

        expect(router.currentRoute.value.path).toEqual('/test-router-27')
        expect(router.currentRoute.value.query).toEqual({ page: '2', pagesize: '10' })
        done()
      })
    })

    describe('start()', () => {
      it('check loading, selectedRowKeys, fetchData() when start() is called', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn((args) => {})
        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')

        await wrapper.setData({ selectedRowKeys: [{ id: 'test-id' }] })
        await wrapper.vm.start()
        await flushPromises()

        expect(fetchData).toHaveBeenCalledTimes(1)

        setTimeout(() => {
          expect(wrapper.vm.loading).toBeFalsy()
          expect(wrapper.vm.selectedRowKeys).toEqual([])
          done()
        }, 1000)
      })
    })

    describe('toggleLoading()', () => {
      it('check loading when toggleLoading() is called', async (done) => {
        await wrapper.setData({ loading: false })
        await wrapper.vm.toggleLoading()
        await flushPromises()

        expect(wrapper.vm.loading).toBeTruthy()
        done()
      })
    })

    describe('startLoading()', () => {
      it('check loading when startLoading() is called', async (done) => {
        await wrapper.setData({ loading: false })
        await wrapper.vm.startLoading()
        await flushPromises()

        expect(wrapper.vm.loading).toBeTruthy()
        done()
      })
    })

    describe('finishLoading()', () => {
      it('check loading when finishLoading() is called', async (done) => {
        await wrapper.setData({ loading: true })
        await wrapper.vm.finishLoading()
        await flushPromises()

        expect(wrapper.vm.loading).toBeFalsy()
        done()
      })
    })

    describe('handleConfirmBlur()', () => {
      it('check confirmDirty value when handleConfirmBlur() is called with args empty', async (done) => {
        await wrapper.setData({ confirmDirty: undefined })
        await wrapper.vm.handleConfirmBlur()
        await flushPromises()

        expect(wrapper.vm.confirmDirty).toBeUndefined()
        done()
      })

      it('check confirmDirty value when handleConfirmBlur() is called with args name not equal `confirmpassword`', async (done) => {
        const event = document.createEvent('Event')

        await wrapper.setData({ confirmDirty: undefined })
        await wrapper.vm.handleConfirmBlur(event, 'test')
        await flushPromises()

        expect(wrapper.vm.confirmDirty).toBeUndefined()
        done()
      })

      it('check confirmDirty value when handleConfirmBlur() is called with args name equal `confirmpassword`', async (done) => {
        await wrapper.setData({ confirmDirty: false })
        await wrapper.vm.handleConfirmBlur({ target: { value: true } }, 'confirmpassword')
        await flushPromises()

        expect(wrapper.vm.confirmDirty).toBeTruthy()
        done()
      })
    })

    describe('validateTwoPassword()', () => {
      it('validate password result is empty when validateTwoPassword() calling with value empty', async (done) => {
        wrapper.vm.form = {}

        const result = await wrapper.vm.validateTwoPassword({}, null)
        await flushPromises()

        expect(result).toBeUndefined()
        done()
      })

      it('validate field `confirmpassword` not valid when validateTwoPassword() is called with `password` not empty', async (done) => {
        let result = null
        wrapper.vm.form = { password: 'abc123' }

        try {
          result = await wrapper.vm.validateTwoPassword({ field: 'confirmpassword' }, '123abc')
          await flushPromises()
        } catch (e) {
          result = e
        }

        expect(result).not.toBeUndefined()
        expect(result).toEqual('message validate password')
        done()
      })

      it('validate field `confirmpassword` valid and result empty when validateTwoPassword() is called with `password` not empty', async (done) => {
        wrapper.vm.form = { password: 'abc123' }

        const result = await wrapper.vm.validateTwoPassword({ field: 'confirmpassword' }, 'abc123')
        await flushPromises()

        expect(result).toBeUndefined()
        done()
      })

      it('validate field `password` valid when validateTwoPassword() is called with `confirmpassword` is empty', async (done) => {
        wrapper.vm.form = { confirmpassword: '' }

        const result = await wrapper.vm.validateTwoPassword({ field: 'password' }, 'abc123')
        await flushPromises()

        expect(result).toBeUndefined()
        done()
      })

      it('validate field `password` valid when validateTwoPassword() is called with `confirmpassword` not empty', async (done) => {
        wrapper.vm.form = { confirmpassword: 'abc123' }

        const result = await wrapper.vm.validateTwoPassword({ field: 'password' }, 'abc123')
        await flushPromises()

        expect(result).toBeUndefined()
        done()
      })

      it('validate field `confirmpassword` a when validateTwoPassword() is called with confirmDirty equal true', async (done) => {
        wrapper.vm.form = { confirmpassword: '123abc' }
        originalFunc.RefValidateFields = wrapper.vm.formRef.value.validateFields
        wrapper.vm.formRef.value.validateFields = jest.fn((field) => {})

        const formRefValidate = jest.spyOn(wrapper.vm.formRef.value, 'validateFields')
        await wrapper.setData({ confirmDirty: true })
        const result = await wrapper.vm.validateTwoPassword({ field: 'password' }, 'abc123')
        await flushPromises()

        expect(result).toBeUndefined()
        expect(formRefValidate).toHaveBeenCalledTimes(1)
        expect(formRefValidate).toHaveBeenLastCalledWith('confirmpassword')
        done()
      })

      it('validate field `password` valid when validateTwoPassword() is called with confirmDirty equal false', async (done) => {
        wrapper.vm.form = { confirmpassword: 'abc123' }

        await wrapper.setData({ confirmDirty: false })
        const result = await wrapper.vm.validateTwoPassword({ field: 'password' }, 'abc123')
        await flushPromises()

        expect(result).toBeUndefined()
        done()
      })

      it('validate result empty when validateTwoPassword() is called with rules.field not equals `password` or `confirmpassword`', async (done) => {
        wrapper.vm.form = {}

        const result = await wrapper.vm.validateTwoPassword({ field: 'name' }, 'abc123')
        await flushPromises()

        expect(result).toBeUndefined()
        done()
      })
    })

    describe('setRules()', () => {
      it('check rules when setRules() is called with args empty', async (done) => {
        await wrapper.vm.setRules()
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({})
        done()
      })

      it('check rules when setRules() is called with args not empty', async (done) => {
        await wrapper.vm.setRules({ name: 'field', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ field: [{ required: true, message: 'required-input' }] })
        done()
      })

      it('check rules when setRules() is called with args field.type equal `boolean`', async (done) => {
        await wrapper.vm.setRules({ name: 'field', required: true, type: 'boolean' })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ field: [{ required: true, message: 'required-input' }] })
        done()
      })

      it('check rules when setRules() is called with currentAction.mapping not empty', async (done) => {
        await wrapper.setData({
          currentAction: {
            mapping: {
              field: {
                options: []
              }
            }
          }
        })
        await wrapper.vm.setRules({ name: 'field', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ field: [{ required: true, message: 'required-select' }] })
        done()
      })

      it('check rules when setRules() is called with field.name equal `keypair`', async (done) => {
        await wrapper.vm.setRules({ name: 'keypair', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ keypair: [{ required: true, message: 'required-select' }] })
        done()
      })

      it('check rules when setRules() is called with field.name equal `account`', async (done) => {
        await wrapper.setData({ currentAction: { api: 'testApiNameCase1' } })
        await wrapper.vm.setRules({ name: 'account', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ account: [{ required: true, message: 'required-select' }] })
        done()
      })

      it('check rules when setRules() is called with field.type equal `uuid`', async (done) => {
        await wrapper.vm.setRules({ name: 'field', required: true, type: 'uuid' })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ field: [{ required: true, message: 'required-select' }] })
        done()
      })

      it('check rules when setRules() is called with field.type equal `list`', async (done) => {
        await wrapper.vm.setRules({ name: 'field', required: true, type: 'list' })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ field: [{ type: 'array', required: true, message: 'required-select' }] })
        done()
      })

      it('check rules when setRules() is called with field.type equal `long`', async (done) => {
        await wrapper.vm.setRules({ name: 'field', required: true, type: 'long' })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({ field: [{ type: 'number', required: true, message: 'required-number' }] })
        done()
      })

      it('check rules when setRules() is called with field.name equal `password`', async (done) => {
        originalFunc.validateTwoPassword = wrapper.vm.validateTwoPassword
        wrapper.vm.validateTwoPassword = jest.fn()

        await wrapper.vm.setRules({ name: 'password', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({
          password: [
            { required: true, message: 'required-input' },
            { validator: wrapper.vm.validateTwoPassword }
          ]
        })
        done()
      })

      it('check rules when setRules() is called with field.name equal `currentpassword`', async (done) => {
        originalFunc.validateTwoPassword = wrapper.vm.validateTwoPassword
        wrapper.vm.validateTwoPassword = jest.fn()

        await wrapper.vm.setRules({ name: 'currentpassword', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({
          currentpassword: [
            { required: true, message: 'required-input' },
            { validator: wrapper.vm.validateTwoPassword }
          ]
        })
        done()
      })

      it('check rules when setRules() is called with field.name equal `confirmpassword`', async (done) => {
        originalFunc.validateTwoPassword = wrapper.vm.validateTwoPassword
        wrapper.vm.validateTwoPassword = jest.fn()

        await wrapper.vm.setRules({ name: 'confirmpassword', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({
          confirmpassword: [
            { required: true, message: 'required-input' },
            { validator: wrapper.vm.validateTwoPassword }
          ]
        })
        done()
      })

      it('check rules when setRules() is called with field.name equal `certificate`', async (done) => {
        await wrapper.vm.setRules({ name: 'certificate', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({
          certificate: [
            { required: true, message: 'required-input' }
          ]
        })
        done()
      })

      it('check rules when setRules() is called with field.name equal `privatekey`', async (done) => {
        await wrapper.vm.setRules({ name: 'privatekey', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({
          privatekey: [
            { required: true, message: 'required-input' }
          ]
        })
        done()
      })

      it('check rules when setRules() is called with field.name equal `certchain`', async (done) => {
        await wrapper.vm.setRules({ name: 'certchain', required: true })
        await flushPromises()

        expect(wrapper.vm.rules).toEqual({
          certchain: [
            { required: true, message: 'required-input' }
          ]
        })
        done()
      })
    })

    describe('handleSubmit', () => {
      it('execSubmit() should be called when handleSubmit() is called in resource view', async (done) => {
        originalFunc.execSubmit = wrapper.vm.execSubmit
        wrapper.vm.execSubmit = jest.fn((event) => {})

        const execSubmit = jest.spyOn(wrapper.vm, 'execSubmit')
        const event = document.createEvent('Event')
        await wrapper.setData({ dataView: true })
        await wrapper.vm.handleSubmit(event)
        await flushPromises()

        expect(execSubmit).toHaveBeenCalledTimes(1)
        expect(execSubmit).toHaveBeenCalledWith(event)
        done()
      })

      it('formRef makes validation calls when handleSubmit() is called in list view', async (done) => {
        originalFunc.callGroupApi = wrapper.vm.callGroupApi
        originalFunc.fetchData = wrapper.vm.fetchData
        originalFunc.formRef = wrapper.vm.formRef
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.callGroupApi = jest.fn((params, resourceName) => {
          return new Promise(resolve => {
            resolve()
          })
        })
        if (!wrapper.vm.formRef) {
          wrapper.vm.formRef = ref()
        }
        wrapper.vm.formRef.value.validate = jest.fn((params, resourceName) => {
          return new Promise(resolve => {
            resolve()
          })
        })

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')
        const callGroupApi = jest.spyOn(wrapper.vm, 'callGroupApi')
        const event = document.createEvent('Event')

        await wrapper.setData({
          dataView: false,
          currentAction: {
            label: 'label.name',
            groupAction: true,
            groupMap: (selection) => {
              return selection.map(x => { return { id: x } })
            }
          },
          selectedRowKeys: ['test-id-value-1'],
          items: [{
            id: 'test-id-value-1',
            name: 'test-name-value-1'
          }],
          columns: [{
            key: 'column1',
            dataIndex: 'column1',
            title: 'column1'
          }]
        })
        await wrapper.vm.handleSubmit(event)
        await flushPromises()

        expect(wrapper.vm.promises).toHaveLength(1)
        expect(callGroupApi).toHaveBeenCalledTimes(1)
        expect(callGroupApi).toHaveBeenCalledWith({ id: 'test-id-value-1' }, 'test-name-value-1')
        expect(fetchData).toHaveBeenCalledTimes(1)
        expect(mocks.$message.info).toHaveBeenCalledTimes(1)
        expect(mocks.$message.info).toHaveBeenCalledWith({
          content: 'name-en',
          key: 'label.name',
          duration: 3
        })
        done()
      })
    })

    describe('execSubmit()', () => {
      it('API should be called with params has item id equal resource.id', async (done) => {
        mockAxios.mockResolvedValue({})
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()

        const event = document.createEvent('Event')
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            icon: 'plus-outlined',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-resource-id'
          }
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            id: 'test-resource-id',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key not exist in currentAction.params', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-resource-id'
          }
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            id: 'test-resource-id',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key exist in currentAction.params, type is boolean and value is undefined', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: undefined }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'boolean' }],
            paramFields: [{ name: 'column1', type: 'boolean' }]
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: false,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key exist in currentAction.params, type is boolean and value is null', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: null }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'boolean' }],
            paramFields: [{ name: 'column1', type: 'boolean' }]
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: false,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key exist in currentAction.params, type is boolean and value is empty', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: '' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'boolean' }],
            paramFields: [{ name: 'column1', type: 'boolean' }]
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: false,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has not input tag', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: '' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'string' }],
            paramFields: [{ name: 'column1', type: 'string' }]
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key exist in currentAction.mapping', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: 1 }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'list' }],
            paramFields: [{ name: 'column1', type: 'list' }],
            mapping: {
              column1: {
                options: ['column-value1', 'column-value2']
              }
            }
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: 'column-value2',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key not exist in currentAction.mapping, type is list and currentAction.params[input] has id', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: [1, 2] }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [
              {
                name: 'column1',
                type: 'list',
                opts: [
                  { id: 'test-id-1', value: 'test-value-1' },
                  { id: 'test-id-2', value: 'test-value-2' },
                  { id: 'test-id-3', value: 'test-value-3' }
                ]
              }
            ],
            paramFields: [
              {
                name: 'column1',
                type: 'list',
                opts: [
                  { id: 'test-id-1', value: 'test-value-1' },
                  { id: 'test-id-2', value: 'test-value-2' },
                  { id: 'test-id-3', value: 'test-value-3' }
                ]
              }
            ],
            mapping: {}
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: 'test-id-2,test-id-3',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key has name = account, currentAction.api = createAccount', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { account: 'test-account-value' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'createAccount',
            label: 'label.name',
            params: [{ name: 'account', type: 'string' }],
            paramFields: [{ name: 'account', type: 'string' }],
            mapping: {}
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'createAccount',
            account: 'test-account-value',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key has name = keypair, currentAction.api = addAccountToProject', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { keypair: 'test-keypair-value' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'addAccountToProject',
            label: 'label.name',
            params: [{ name: 'keypair', type: 'string' }],
            paramFields: [{ name: 'keypair', type: 'string' }],
            mapping: {}
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'addAccountToProject',
            keypair: 'test-keypair-value',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key name = (account | keypair), currentAction.api != (addAccountToProject | createAccount)', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { keypair: 1 }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [
              {
                name: 'keypair',
                type: 'string',
                opts: [
                  { id: 'test-id-1', name: 'test-name-1' },
                  { id: 'test-id-2', name: 'test-name-2' }
                ]
              }
            ],
            paramFields: [
              {
                name: 'keypair',
                type: 'string',
                opts: [
                  { id: 'test-id-1', name: 'test-name-1' },
                  { id: 'test-id-2', name: 'test-name-2' }
                ]
              }
            ],
            mapping: {}
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            keypair: 'test-name-2',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when form has input key do not fall under special condition.', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: 'test-column-value' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'string' }],
            paramFields: [{ name: 'column1', type: 'string' }],
            mapping: {}
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: 'test-column-value',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when currentAction has defaultArgs', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: 'test-column1-value' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'string' }],
            paramFields: [{ name: 'column1', type: 'string' }],
            mapping: {},
            defaultArgs: {
              column2: 'test-column2-value'
            }
          },
          resource: {}
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: 'test-column1-value',
            column2: 'test-column2-value',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called when currentAction.mapping has value and value is function', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.form = { column1: 'test-column1-value' }

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'column1', type: 'string' }],
            paramFields: [{ name: 'column1', type: 'string' }],
            mapping: {
              column2: {
                value: (record, params) => {
                  return record.name
                }
              }
            }
          },
          resource: {
            id: 'test-id-value',
            name: 'test-name-value'
          }
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'testApiNameCase1',
            column1: 'test-column1-value',
            column2: 'test-name-value',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with post method', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.fetchData = wrapper.vm.fetchData
        originalFunc.closeAction = wrapper.vm.closeAction
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.fetchData = jest.fn()
        wrapper.vm.closeAction = jest.fn()

        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }],
            post: true
          },
          resource: {
            id: 'test-id-value'
          }
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        const postData = new URLSearchParams()
        postData.append('id', 'test-id-value')

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'POST',
          data: postData,
          params: {
            command: 'testApiNameCase1',
            response: 'json'
          }
        })
        done()
      })

      it('handleResponse() & closeAction() should be called when API response success', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.closeAction = wrapper.vm.closeAction
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, params, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.closeAction = jest.fn()
        wrapper.vm.fetchData = jest.fn()

        const handleResponse = jest.spyOn(wrapper.vm, 'handleResponse')
        const closeAction = jest.spyOn(wrapper.vm, 'closeAction')
        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-id-value'
          }
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(handleResponse).toHaveBeenCalledTimes(1)
        expect(handleResponse).toHaveBeenLastCalledWith(
          {},
          'test-id-value',
          'test-id-value',
          {
            api: 'testApiNameCase1',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          }
        )
        expect(closeAction).toHaveBeenCalledTimes(1)
        done()
      })

      it('$router should go back when API response success with shouldNavigateBack() return true', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.closeAction = wrapper.vm.closeAction
        originalFunc.fetchData = wrapper.vm.fetchData
        originalFunc.routerGo = wrapper.vm.$router.go
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return true })
        wrapper.vm.$router.go = jest.fn((number) => {})
        wrapper.vm.closeAction = jest.fn()
        wrapper.vm.fetchData = jest.fn()

        const routerGo = jest.spyOn(wrapper.vm.$router, 'go')
        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            icon: 'delete-outlined',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-id-value'
          },
          dataView: true
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(routerGo).toHaveBeenCalledTimes(1)
        expect(routerGo).toHaveBeenLastCalledWith(-1)
        done()
      })

      it('$router should go back when API response success with action.api equal `archiveEvents`', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.closeAction = wrapper.vm.closeAction
        originalFunc.fetchData = wrapper.vm.fetchData
        originalFunc.routerGo = wrapper.vm.$router.go
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return true })
        wrapper.vm.$router.go = jest.fn((number) => {})
        wrapper.vm.closeAction = jest.fn()
        wrapper.vm.fetchData = jest.fn()

        const routerGo = jest.spyOn(wrapper.vm.$router, 'go')
        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'archiveEvents',
            icon: 'plus-outlined',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-id-value'
          },
          dataView: true
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(routerGo).toHaveBeenCalledTimes(1)
        expect(routerGo).toHaveBeenLastCalledWith(-1)
        done()
      })

      it('fetchData() should be called when API response success with jobId empty', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.closeAction = wrapper.vm.closeAction
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve() })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.closeAction = jest.fn()
        wrapper.vm.fetchData = jest.fn()

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')
        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            icon: 'plus-outlined',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-id-value'
          },
          dataView: true
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(fetchData).toHaveBeenCalled()
        done()
      })

      it('fetchData() not called when API response success with jobId not empty', async (done) => {
        originalFunc.handleResponse = wrapper.vm.handleResponse
        originalFunc.shouldNavigateBack = wrapper.vm.shouldNavigateBack
        originalFunc.closeAction = wrapper.vm.closeAction
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.handleResponse = jest.fn(async (json, resourceName, action) => { return Promise.resolve('test-job-id') })
        wrapper.vm.shouldNavigateBack = jest.fn((args) => { return false })
        wrapper.vm.closeAction = jest.fn()
        wrapper.vm.fetchData = jest.fn()

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData')
        const event = document.createEvent('Event')
        await mockAxios.mockResolvedValue({})
        await wrapper.setData({
          showAction: true,
          currentAction: {
            api: 'testApiNameCase1',
            icon: 'plus-outlined',
            label: 'label.name',
            params: [{ name: 'id', type: 'uuid' }],
            paramFields: [{ name: 'id', type: 'uuid' }]
          },
          resource: {
            id: 'test-id-value'
          },
          dataView: true
        })
        await wrapper.vm.execSubmit(event)
        await flushPromises()

        expect(fetchData).not.toHaveBeenCalled()
        done()
      })
    })
  })
})
