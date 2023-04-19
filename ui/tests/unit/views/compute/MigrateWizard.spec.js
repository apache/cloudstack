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
import mockData from '../../../mockData/MigrateWizard.mock'
import MigrateWizard from '@/views/compute/MigrateWizard'

jest.mock('axios', () => mockAxios)

let i18n
let store
let mocks
let router
let wrapper
const originalFunc = {}

router = common.createMockRouter(mockData.routes)
i18n = common.createMockI18n('en', mockData.messages)
store = common.createMockStore()
mocks = {
  $message: {
    error: jest.fn((message) => {})
  },
  $notification: {
    error: jest.fn((option) => {
      return option
    }),
    info: jest.fn((option) => {
      return option
    }),
    success: jest.fn((option) => {
      return option
    }),
    warning: jest.fn((option) => {
      return option
    })
  },
  $pollJob: jest.fn((obj) => {
    switch (obj.jobId) {
      case 'test-job-id-case-1':
        if ('successMethod' in obj) {
          obj.successMethod()
        }
        break
      case 'test-job-id-case-2':
        if ('errorMethod' in obj) {
          obj.errorMethod()
        }
        break
      case 'test-job-id-case-3':
        if ('catchMethod' in obj) {
          obj.catchMethod()
        }
        break
    }
  })
}

const factory = (opts = {}) => {
  router = opts.router || router
  i18n = opts.i18n || i18n
  store = opts.store || store
  mocks = opts.mocks || mocks

  return common.createFactory(MigrateWizard, {
    router,
    i18n,
    store,
    mocks,
    props: opts.props || {},
    data: opts.data || {}
  })
}

router.push('/')
describe('Views > compute > MigrateWizard.vue', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    jest.spyOn(console, 'warn').mockImplementation(() => {})

    if (!wrapper) {
      mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
      wrapper = factory({
        props: { resource: {} }
      })
    }

    if (i18n.global.locale !== 'en') i18n.global.locale = 'en'
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.vm.searchQuery = ''
      wrapper.vm.page = 1
      wrapper.vm.pageSize = 10
      wrapper.vm.selectedHost = {}
    }

    if (Object.keys(originalFunc).length > 0) {
      Object.keys(originalFunc).forEach(key => {
        switch (key) {
          case 'fetchData':
            wrapper.vm.fetchData = originalFunc[key]
            break
          default:
            break
        }
      })
    }
  })

  describe('Methods', () => {
    describe('fetchData()', () => {
      it('API should be called with resource is empty and searchQuery is empty', async (done) => {
        await mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
        await wrapper.setProps({ resource: {} })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'findHostsForMigration',
            virtualmachineid: undefined,
            keyword: '',
            page: 1,
            pagesize: 10,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with resource.id is empty and searchQuery is empty', async (done) => {
        await mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
        await wrapper.setProps({ resource: { id: null } })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'findHostsForMigration',
            virtualmachineid: null,
            keyword: '',
            page: 1,
            pagesize: 10,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with resource.id is not empty and searchQuery is empty', async (done) => {
        await mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
        await wrapper.setProps({ resource: { id: 'test-id-value' } })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'findHostsForMigration',
            virtualmachineid: 'test-id-value',
            keyword: '',
            page: 1,
            pagesize: 10,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with resource.id is not empty and searchQuery is not empty', async (done) => {
        await mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
        await wrapper.setProps({ resource: { id: 'test-id-value' } })
        await wrapper.setData({ searchQuery: 'test-query-value' })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'findHostsForMigration',
            virtualmachineid: 'test-id-value',
            keyword: 'test-query-value',
            page: 1,
            pagesize: 10,
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with params assign by resource, searchQuery, page, pageSize', async (done) => {
        await mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
        await wrapper.setProps({ resource: { id: 'test-id-value' } })
        await wrapper.setData({
          searchQuery: 'test-query-value',
          page: 2,
          pageSize: 20
        })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'findHostsForMigration',
            virtualmachineid: 'test-id-value',
            keyword: 'test-query-value',
            page: 1,
            pagesize: 20,
            response: 'json'
          }
        })
        done()
      })

      it('check hosts, totalCount when api is called with response result is empty', async (done) => {
        await mockAxios.mockResolvedValue({ findhostsformigrationresponse: { count: 0, host: [] } })
        await wrapper.setProps({ resource: {} })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(wrapper.vm.hosts).toEqual([])
        expect(wrapper.vm.totalCount).toEqual(0)
        done()
      })

      it('check hosts, totalCount when api is called with response result is empty', async (done) => {
        await mockAxios.mockResolvedValue({
          findhostsformigrationresponse: {
            count: 1,
            host: [{
              id: 'test-host-id',
              name: 'test-host-name',
              suitability: 'test-host-suitability',
              cpuused: 'test-host-cpuused',
              memused: 'test-host-memused',
              select: 'test-host-select'
            }]
          }
        })
        await wrapper.setProps({ resource: {} })
        await wrapper.vm.fetchData()
        await flushPromises()

        expect(wrapper.vm.hosts).toEqual([{
          id: 'test-host-id',
          name: 'test-host-name',
          suitability: 'test-host-suitability',
          cpuused: 'test-host-cpuused',
          memused: 'test-host-memused',
          select: 'test-host-select'
        }])
        expect(wrapper.vm.totalCount).toEqual(1)
        done()
      })

      it('check $message.error should be called when api is called with throw error', async (done) => {
        const mockError = 'Error: throw error message'

        await mockAxios.mockRejectedValue(mockError)
        await wrapper.setProps({ resource: {} })
        await wrapper.vm.fetchData()
        await flushPromises()
        await flushPromises()

        expect(mocks.$message.error).toHaveBeenCalled()
        expect(mocks.$message.error).toHaveBeenLastCalledWith(`${i18n.global.t('message.load.host.failed')}: ${mockError}`)
        done()
      })
    })

    describe('submitForm()', () => {
      it('API should be called with selectedHost.requiresStorageMotion is true', async (done) => {
        const mockData = {
          migratevirtualmachineresponse: {
            jobid: 'test-job-id'
          },
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        }

        await router.push({ name: 'testRouter1' })
        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: true,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'migrateVirtualMachineWithVolume',
            hostid: 'test-host-id',
            virtualmachineid: 'test-resource-id',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with selectedHost.requiresStorageMotion is false', async (done) => {
        const mockData = {
          migratevirtualmachineresponse: {
            jobid: 'test-job-id'
          },
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        }

        await router.push({ name: 'testRouter2' })
        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: false,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'migrateVirtualMachine',
            hostid: 'test-host-id',
            virtualmachineid: 'test-resource-id',
            response: 'json'
          }
        })
        done()
      })

      it('API should be called with $route.meta.name not equals `vm`', async (done) => {
        const mockData = {
          migratesystemvmresponse: {
            jobid: 'test-job-id'
          },
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        }

        await router.push({ name: 'testRouter3' })
        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: false,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mockAxios).toHaveBeenCalled()
        expect(mockAxios).toHaveBeenLastCalledWith({
          url: '/',
          method: 'GET',
          data: new URLSearchParams(),
          params: {
            command: 'migrateSystemVm',
            hostid: 'test-host-id',
            virtualmachineid: 'test-resource-id',
            response: 'json'
          }
        })
        done()
      })

      it('$pollJob have successMethod() should be called with requiresStorageMotion is true', async (done) => {
        const mockData = {
          migratevirtualmachinewithvolumeresponse: {
            jobid: 'test-job-id-case-1'
          },
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        }

        await router.push({ name: 'testRouter4' })
        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: true,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mocks.$pollJob).toHaveBeenCalled()
        expect(wrapper.emitted()['close-action'][0]).toEqual([])
        done()
      })

      it('$pollJob have successMethod() should be called with requiresStorageMotion is false', async (done) => {
        const mockData = {
          migratevirtualmachineresponse: {
            jobid: 'test-job-id-case-2'
          },
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        }

        await router.push({ name: 'testRouter5' })
        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: false,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mocks.$pollJob).toHaveBeenCalled()
        expect(wrapper.emitted()['close-action'][0]).toEqual([])
        done()
      })

      it('$pollJob have errorMethod() should be called', async (done) => {
        const mockData = {
          migratevirtualmachinewithvolumeresponse: {
            jobid: 'test-job-id-case-3'
          },
          queryasyncjobresultresponse: {
            jobstatus: 2,
            jobresult: {
              errortext: 'test-error-message'
            }
          }
        }

        await router.push({ name: 'testRouter6' })
        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: true,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mocks.$pollJob).toHaveBeenCalled()
        expect(wrapper.emitted()['close-action'][0]).toEqual([])
        done()
      })

      it('$pollJob have catchMethod() should be called', async (done) => {
        const mockData = {
          migratevirtualmachinewithvolumeresponse: {
            jobid: 'test-job-id-case-4'
          }
        }

        await mockAxios.mockResolvedValue(mockData)
        await wrapper.setProps({
          resource: {
            id: 'test-resource-id',
            name: 'test-resource-name'
          }
        })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: true,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mocks.$pollJob).toHaveBeenCalled()
        expect(wrapper.emitted()['close-action'][0]).toEqual([])
        done()
      })

      it('$message.error should be called when api is called with throw error', async (done) => {
        const mockError = { message: 'Error: throw error message' }

        await mockAxios.mockRejectedValue(mockError)
        await wrapper.setProps({ resource: {} })
        await wrapper.setData({
          selectedHost: {
            requiresStorageMotion: true,
            id: 'test-host-id',
            name: 'test-host-name'
          }
        })
        await wrapper.vm.submitForm()
        await flushPromises()

        expect(mocks.$notification.error).toHaveBeenCalled()
        expect(mocks.$notification.error).toHaveBeenCalledWith({
          message: i18n.global.t('message.request.failed'),
          description: 'Error: throw error message',
          duration: 0
        })
        done()
      })
    })

    describe('handleChangePage()', () => {
      it('check page, pageSize and fetchData() when handleChangePage() is called', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn()

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        await wrapper.setProps({ resource: {} })
        await wrapper.setData({
          page: 1,
          pageSize: 10
        })
        await wrapper.vm.handleChangePage(2, 20)
        await flushPromises()

        expect(wrapper.vm.page).toEqual(2)
        expect(wrapper.vm.pageSize).toEqual(20)
        expect(fetchData).toBeCalled()
        done()
      })
    })

    describe('handleChangePageSize()', () => {
      it('check page, pageSize and fetchData() when handleChangePageSize() is called', async (done) => {
        originalFunc.fetchData = wrapper.vm.fetchData
        wrapper.vm.fetchData = jest.fn()

        const fetchData = jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        await wrapper.setProps({ resource: {} })
        await wrapper.setData({
          page: 1,
          pageSize: 10
        })
        await wrapper.vm.handleChangePage(2, 20)
        await flushPromises()

        expect(wrapper.vm.page).toEqual(2)
        expect(wrapper.vm.pageSize).toEqual(20)
        expect(fetchData).toBeCalled()
        done()
      })
    })
  })
})
