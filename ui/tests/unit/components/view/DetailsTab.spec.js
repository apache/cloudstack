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
import DetailsTab from '@/components/view/DetailsTab.vue'

const i18n = common.createMockI18n('en')

describe('Components > View > DetailsTab.vue', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => {})
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('displays backup volume sizes in GiB when the API provides bytes', async () => {
    const router = common.createMockRouter([{
      path: '/backup/:id',
      name: 'backup',
      meta: { name: 'backup', details: ['volumes'] },
      component: { template: '<div />' }
    }])
    await router.push('/backup/backup-1')
    await router.isReady()

    const wrapper = common.createFactory(DetailsTab, {
      router,
      i18n,
      props: {
        resource: {
          volumes: JSON.stringify([{
            uuid: 'volume-1',
            type: 'ROOT',
            path: 'root.qcow2',
            size: 2 * 1024 * 1024 * 1024
          }]),
          vmbackupofferingremoved: true
        }
      }
    })

    await flushPromises()

    expect(wrapper.text()).toContain('2.0 GiB')
    expect(wrapper.text()).not.toContain('2.0 GB')

    wrapper.unmount()
  })
})
