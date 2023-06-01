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

import { shallowRef, defineAsyncComponent } from 'vue'
import store from '@/store'

export default {
  name: 'zones',
  title: 'label.zones',
  icon: 'global-outlined',
  permission: ['listZones'],
  params: { showicon: true },
  show: () => {
    return ['DomainAdmin', 'User'].includes(store.getters.userInfo.roletype)
  },
  columns: () => {
    const fields = ['name', 'allocationstate', 'type', 'networktype']
    return fields
  },
  details: ['name', 'id', 'allocationstate', 'type', 'networktype', 'guestcidraddress', 'localstorageenabled', 'securitygroupsenabled', 'dns1', 'dns2', 'internaldns1', 'internaldns2'],
  related: [{
    name: 'vm',
    title: 'label.vms',
    param: 'zoneid'
  }],
  resourceType: 'Zone',
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }]
}
