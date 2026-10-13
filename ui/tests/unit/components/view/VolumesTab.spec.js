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

import { createServer } from 'http'
import { flushPromises } from '@vue/test-utils'

import common from '../../../common'
import VolumesTab from '@/components/view/VolumesTab.vue'
import { axios } from '@/utils/request'
import { CURRENT_PROJECT } from '@/store/mutation-types'
import { vueProps } from '@/vue-app'

jest.mock('@/vue-app', () => ({
  vueProps: {
    $localStorage: {
      get: jest.fn(() => null)
    }
  }
}))

const router = common.createMockRouter()
const i18n = common.createMockI18n('en')
const projectVm = { id: 'vm-1', projectid: 'project-1' }
const accountVm = { id: 'vm-2' }
const systemVm = { id: 'vm-3', systemvmtype: 'secondarystoragevm' }
let caller = 'domain admin'
let requests = []

// Mirrors buildACLSearchParameters in server/src/main/java/com/cloud/user/AccountManagerImpl.java for
// listVolumes with listall. The domain admin owns none of the instances and belongs to no project.
// Without projectid, project resources are skipped. projectid=-1 reaches only the caller's own account
// and project accounts, so it hides every instance from a domain admin, while a root admin is not
// restricted. projectid=<id> reaches that project, which a domain admin may open.
const canList = (vm, projectid) => {
  if (!projectid) {
    return !vm.projectid
  }
  if (projectid === '-1') {
    return caller === 'root admin'
  }
  return projectid === vm.projectid
}

const server = createServer((request, response) => {
  const params = Object.fromEntries(new URL(request.url, 'http://localhost').searchParams)
  requests.push(params)
  const vm = [projectVm, accountVm, systemVm].find(vm => vm.id === params.virtualmachineid)
  const volume = vm && canList(vm, params.projectid)
    ? [
      { id: 'data-volume', name: 'Data volume', deviceid: 1 },
      { id: 'root-volume', name: 'Root volume', deviceid: 0 }
    ]
    : []
  response.setHeader('Content-Type', 'application/json')
  response.end(JSON.stringify({ listvolumesresponse: { count: volume.length, volume } }))
})

describe('Components > View > VolumesTab.vue', () => {
  beforeAll(async () => {
    await new Promise((resolve, reject) => {
      server.once('error', reject)
      server.listen(0, '127.0.0.1', resolve)
    })
    axios.defaults.baseURL = `http://127.0.0.1:${server.address().port}`
    axios.defaults.adapter = require('axios/lib/adapters/http')
  })

  afterAll(() => new Promise(resolve => server.close(resolve)))

  beforeEach(() => {
    caller = 'domain admin'
    requests = []
    jest.clearAllMocks()
    vueProps.$localStorage.get.mockReturnValue(null)
    jest.spyOn(console, 'warn').mockImplementation(() => {})
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('displays byte-based volume sizes in GiB', async () => {
    const wrapper = common.createFactory(VolumesTab, {
      router,
      i18n,
      props: {
        resource: { id: 'vm-1' },
        items: [{
          id: 'volume-1',
          name: 'Data volume',
          state: 'Ready',
          type: 'DATADISK',
          size: 2 * 1024 * 1024 * 1024
        }]
      }
    })

    await flushPromises()

    expect(wrapper.text()).toContain('2.00 GiB')
    expect(wrapper.text()).not.toContain('2.00 GB')
    expect(requests).toHaveLength(0)

    wrapper.unmount()
  })

  it.each([
    ['project instance in Default view', 'domain admin', projectVm, null],
    ['project instance in Default view', 'root admin', projectVm, null],
    ['project instance in its project', 'domain admin', projectVm, projectVm.projectid],
    ['project instance in another project', 'domain admin', projectVm, 'project-2'],
    ['account instance in Default view', 'domain admin', accountVm, null],
    ['system VM in Default view', 'root admin', systemVm, null]
  ])('shows volume rows for %s as a %s', async (name, role, resource, selectedProjectId) => {
    caller = role
    vueProps.$localStorage.get.mockImplementation(key => {
      return key === CURRENT_PROJECT && selectedProjectId ? { id: selectedProjectId } : null
    })
    let interceptor
    const response = new Promise((resolve, reject) => {
      interceptor = axios.interceptors.response.use(data => {
        resolve(data)
        return data
      }, error => {
        reject(error)
        return Promise.reject(error)
      })
    })
    const wrapper = common.createFactory(VolumesTab, {
      router,
      i18n,
      props: { resource }
    })

    try {
      await response
      await flushPromises()

      expect(wrapper.findAll('tbody tr[data-row-key]')).toHaveLength(2)
      expect(requests).toEqual([{
        command: 'listVolumes',
        response: 'json',
        listall: 'true',
        listsystemvms: 'true',
        virtualmachineid: resource.id,
        ...(resource.projectid && { projectid: resource.projectid })
      }])
      expect(wrapper.vm.volumes.map(volume => volume.id)).toEqual(['root-volume', 'data-volume'])
      expect(wrapper.text()).toContain('Root volume')
      expect(wrapper.text()).toContain('Data volume')
    } finally {
      wrapper.unmount()
      axios.interceptors.response.eject(interceptor)
    }
  })
})
