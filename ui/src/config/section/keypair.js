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
  name: 'keypair',
  identifier: 'keypairid',
  title: 'label.apikeypairs',
  icon: 'key-outlined',
  hidden: true,
  docHelp: 'adminguide/accounts.html#keypairs',
  permission: ['listUserKeys'],
  columns: [
    'name',
    { field: 'startdate', customTitle: 'apikeypair.startdate' },
    { field: 'enddate', customTitle: 'apikeypair.enddate' },
    'username', 'rolename'
  ],
  details: [
    'id', 'name', 'description',
    'domain', 'role', 'roletype',
    { field: 'accountname', customTitle: 'account' }, 'username',
    { field: 'startdate', customTitle: 'apikeypair.startdate' },
    { field: 'enddate', customTitle: 'apikeypair.enddate' },
    'created'
  ],
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }, {
    name: 'rules',
    component: shallowRef(defineAsyncComponent(() => import('@/views/iam/ApiKeyPairPermissionTable.vue'))),
    show: () => { return 'listUserKeyRules' in store.getters.apis }
  }],
  actions: [
    {
      api: 'deleteUserKeys',
      icon: 'delete-outlined',
      label: 'label.action.delete.keypair',
      message: 'message.delete.keypair',
      dataView: true,
      args: ['keypairid'],
      mapping: {
        keypairid: {
          value: (record) => { return record.id }
        }
      },
      show: () => {
        return 'deleteUserKeys' in store.getters.apis
      }
    }
  ]
}
