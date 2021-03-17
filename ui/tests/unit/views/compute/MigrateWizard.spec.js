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
import MigrateWizard from '@/views/compute/MigrateWizard'
import common from '../../../common'
import mockData from '../../../mockData/MigrateWizard.mock'

jest.mock('axios', () => mockAxios)

let wrapper, i18n, store, mocks, router

const state = {}
const actions = {
  AddAsyncJob: jest.fn((jobObject) => {})
}
mocks = {
  $message: {
    error: jest.fn((message) => {})
  },
  $notification: {
    error: jest.fn((message) => {})
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
i18n = common.createMockI18n('en', mockData.messages)
store = common.createMockStore(state, actions)

const factory = (opts = {}) => {
  i18n = opts.i18n || i18n
  store = opts.store || store
  mocks = opts.mocks || mocks
  router = opts.router || router

  return common.createFactory(MigrateWizard, {
    router,
    i18n,
    store,
    mocks,
    props: opts.props || {},
    data: opts.data || {}
  })
}

describe('Views > compute > MigrateWizard.vue', () => {
  jest.spyOn(console, 'warn').mockImplementation(() => {})

  beforeEach(() => {
    jest.clearAllMocks()

    if (wrapper) {
      wrapper.destroy()
    }
    if (router && router.currentRoute.name !== 'home') {
      router.replace({ name: 'home' })
    }

    if (i18n.locale !== 'en') {
      i18n.locale = 'en'
    }
  })

  describe('Methods', () => {
    describe('fetchData()', () => {
      it('check api is called with resource is empty and searchQuery is null', () => {
        const mockData = {
          findhostsformigrationresponse: {
            count: 0,
            host: []
          }
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({
          props: {
            resource: {}
          }
        })

        wrapper.vm.$nextTick(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
        })
      })

      it('check api is called with resource.id is null and searchQuery is null', () => {
        const mockData = {
          findhostsformigrationresponse: {
            count: 0,
            host: []
          }
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({
          props: {
            resource: { id: null }
          }
        })

        wrapper.vm.$nextTick(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
        })
      })

      it('check api is called with resource.id is not null and searchQuery is null', () => {
        const mockData = {
          findhostsformigrationresponse: {
            count: 0,
            host: []
          }
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({
          props: {
            resource: { id: 'test-id-value' }
          }
        })

        wrapper.vm.$nextTick(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
        })
      })

      it('check api is called with resource.id is not null and searchQuery is not null', () => {
        const mockData = {
          findhostsformigrationresponse: {
            count: 0,
            host: []
          }
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({
          props: { resource: { id: 'test-id-value' } },
          data: { searchQuery: 'test-query-value' }
        })

        wrapper.vm.$nextTick(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
        })
      })

      it('check api is called with params assign by resource, searchQuery, page, pageSize', () => {
        const mockData = {
          findhostsformigrationresponse: {
            count: 0,
            host: []
          }
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({
          props: { resource: { id: 'test-id-value' } },
          data: {
            searchQuery: 'test-query-value',
            page: 2,
            pageSize: 20
          }
        })

        wrapper.vm.$nextTick(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
            url: '/',
            method: 'GET',
            data: new URLSearchParams(),
            params: {
              command: 'findHostsForMigration',
              virtualmachineid: 'test-id-value',
              keyword: 'test-query-value',
              page: 2,
              pagesize: 20,
              response: 'json'
            }
          })
        })
      })

      it('check hosts, totalCount when api is called with response result is empty', async (done) => {
        const mockData = {
          findhostsformigrationresponse: {
            count: 0,
            host: []
          }
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({ props: { resource: {} } })

        await wrapper.vm.$nextTick()

        setTimeout(() => {
          expect(wrapper.vm.hosts).toEqual([])
          expect(wrapper.vm.totalCount).toEqual(0)

          done()
        })
      })

      it('check hosts, totalCount when api is called with response result is not empty', async (done) => {
        const mockData = {
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
        }

        mockAxios.mockResolvedValue(mockData)
        wrapper = factory({ props: { resource: {} } })

        await wrapper.vm.$nextTick()

        setTimeout(() => {
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
      })

      it('check $message.error is called when api is called with throw error', async (done) => {
        const mockError = 'Error: throw error message'
        console.error = jest.fn()

        mockAxios.mockRejectedValue(mockError)
        wrapper = factory({ props: { resource: {} } })

        await wrapper.vm.$nextTick()

        setTimeout(() => {
          expect(mocks.$message.error).toHaveBeenCalled()
          expect(mocks.$message.error).toHaveBeenCalledWith(`${i18n.t('message.load.host.failed')}: ${mockError}`)

          done()
        })
      })
    })

    describe('submitForm()', () => {
      it('check api is called when selectedHost.requiresStorageMotion is true and isUserVm=true', async (done) => {
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

        router = common.createMockRouter([{
          name: 'testRouter1',
          path: '/test-router-1',
          meta: {
            name: 'vm'
          }
        }])
        wrapper = factory({
          router: router,
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: true,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        router.push({ name: 'testRouter1' })

        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        mockAxios.mockResolvedValue(mockData)
        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
      })

      it('check api is called when selectedHost.requiresStorageMotion is false and isUserVm=true', async (done) => {
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

        router = common.createMockRouter([{
          name: 'testRouter2',
          path: '/test-router-2',
          meta: {
            name: 'vm'
          }
        }])
        wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: false,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        router.push({ name: 'testRouter2' })
        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})

        mockAxios.mockResolvedValue(mockData)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
      })

      it('check api is called when isUserVm=false', async (done) => {
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

        router = common.createMockRouter([{
          name: 'testRouter3',
          path: '/test-router-3',
          meta: {
            name: 'test'
          }
        }])
        wrapper = factory({
          router: router,
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: true,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        router.push({ name: 'testRouter3' })

        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        mockAxios.mockResolvedValue(mockData)
        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(mockAxios).toHaveBeenCalled()
          expect(mockAxios).toHaveBeenCalledWith({
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
      })

      it('check store dispatch `AddAsyncJob` and $pollJob have successMethod() is called with requiresStorageMotion is true', async (done) => {
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

        router = common.createMockRouter([{
          name: 'testRouter4',
          path: '/test-router-4',
          meta: {
            name: 'vm'
          }
        }])
        wrapper = factory({
          router: router,
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: true,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        router.push({ name: 'testRouter4' })

        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        mockAxios.mockResolvedValue(mockData)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(actions.AddAsyncJob).toHaveBeenCalled()
          expect(mocks.$pollJob).toHaveBeenCalled()
          expect(wrapper.emitted()['close-action'][0]).toEqual([])

          done()
        })
      })

      it('check store dispatch `AddAsyncJob` and $pollJob have successMethod() is called with requiresStorageMotion is false', async (done) => {
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

        router = common.createMockRouter([{
          name: 'testRouter5',
          path: '/test-router-5',
          meta: {
            name: 'vm'
          }
        }])
        wrapper = factory({
          router: router,
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: false,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        router.push({ name: 'testRouter5' })

        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        mockAxios.mockResolvedValue(mockData)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(actions.AddAsyncJob).toHaveBeenCalled()
          expect(mocks.$pollJob).toHaveBeenCalled()
          expect(wrapper.emitted()['close-action'][0]).toEqual([])

          done()
        })
      })

      it('check store dispatch `AddAsyncJob` and $pollJob have successMethod() is called with isUserVm is false', async (done) => {
        const mockData = {
          migratesystemvmresponse: {
            jobid: 'test-job-id-case-2'
          },
          queryasyncjobresultresponse: {
            jobstatus: 1,
            jobresult: {
              name: 'test-name-value'
            }
          }
        }

        router = common.createMockRouter([{
          name: 'testRouter6',
          path: '/test-router-6',
          meta: {
            name: 'test'
          }
        }])
        wrapper = factory({
          router: router,
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: false,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        router.push({ name: 'testRouter6' })

        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})
        mockAxios.mockResolvedValue(mockData)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(actions.AddAsyncJob).toHaveBeenCalled()
          expect(mocks.$pollJob).toHaveBeenCalled()
          expect(wrapper.emitted()['close-action'][0]).toEqual([])

          done()
        })
      })

      it('check store dispatch `AddAsyncJob` and $pollJob have errorMethod() is called', async (done) => {
        const mockData = {
          migratesystemvmresponse: {
            jobid: 'test-job-id-case-3'
          },
          queryasyncjobresultresponse: {
            jobstatus: 2,
            jobresult: {
              errortext: 'test-error-message'
            }
          }
        }
        wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: true,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})

        mockAxios.mockResolvedValue(mockData)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(actions.AddAsyncJob).toHaveBeenCalled()
          expect(mocks.$pollJob).toHaveBeenCalled()
          expect(wrapper.emitted()['close-action'][0]).toEqual([])

          done()
        })
      })

      it('check store dispatch `AddAsyncJob` and $pollJob have catchMethod() is called', async (done) => {
        const mockData = {
          migratesystemvmresponse: {
            jobid: 'test-job-id-case-4'
          }
        }
        wrapper = factory({
          props: {
            resource: {
              id: 'test-resource-id',
              name: 'test-resource-name'
            }
          },
          data: {
            selectedHost: {
              requiresStorageMotion: true,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})

        mockAxios.mockResolvedValue(mockData)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(actions.AddAsyncJob).toHaveBeenCalled()
          expect(mocks.$pollJob).toHaveBeenCalled()
          expect(wrapper.emitted()['close-action'][0]).toEqual([])

          done()
        })
      })

      it('check $message.error is called when api is called with throw error', async (done) => {
        const mockError = {
          message: 'Error: throw error message'
        }

        wrapper = factory({
          props: {
            resource: {}
          },
          data: {
            selectedHost: {
              requiresStorageMotion: true,
              id: 'test-host-id',
              name: 'test-host-name'
            }
          }
        })
        jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})

        mockAxios.mockRejectedValue(mockError)

        await wrapper.vm.$nextTick()
        await wrapper.vm.submitForm()

        setTimeout(() => {
          expect(mocks.$notification.error).toHaveBeenCalled()
          expect(mocks.$notification.error).toHaveBeenCalledWith({
            message: i18n.t('message.request.failed'),
            description: 'Error: throw error message',
            duration: 0
          })

          done()
        })
      })
    })

    describe('handleChangePage()', () => {
      it('check page, pageSize and fetchData() when handleChangePage() is called', () => {
        wrapper = factory({
          props: {
            resource: {}
          },
          data: {
            page: 1,
            pageSize: 10
          }
        })
        const spyFetchData = jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})

        wrapper.vm.$nextTick(() => {
          wrapper.vm.handleChangePage(2, 20)

          expect(wrapper.vm.page).toEqual(2)
          expect(wrapper.vm.pageSize).toEqual(20)
          expect(spyFetchData).toBeCalled()
        })
      })
    })

    describe('handleChangePageSize()', () => {
      it('check page, pageSize and fetchData() when handleChangePageSize() is called', () => {
        wrapper = factory({
          props: {
            resource: {}
          },
          data: {
            page: 1,
            pageSize: 10
          }
        })
        const spyFetchData = jest.spyOn(wrapper.vm, 'fetchData').mockImplementation(() => {})

        wrapper.vm.$nextTick(() => {
          wrapper.vm.handleChangePageSize(2, 20)

          expect(wrapper.vm.page).toEqual(2)
          expect(wrapper.vm.pageSize).toEqual(20)
          expect(spyFetchData).toBeCalled()
        })
      })
    })
  })
})
