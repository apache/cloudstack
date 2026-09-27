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
import VolumesTab from '@/components/view/VolumesTab.vue'

const router = common.createMockRouter()
const i18n = common.createMockI18n('en')

describe('Components > View > VolumesTab.vue', () => {
  beforeEach(() => {
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

    wrapper.unmount()
  })
})
