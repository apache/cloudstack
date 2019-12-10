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

export default {
  name: 'imagestore',
  title: 'Secondary Storages',
  icon: 'picture',
  permission: ['listImageStores'],
  columns: ['name', 'url', 'protocol', 'scope', 'zonename'],
  details: ['name', 'id', 'url', 'protocol', 'provider', 'scope', 'zonename'],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'Settings',
    component: () => import('@/components/view/SettingsTab.vue')
  }],
  actions: [
    {
      api: 'addImageStore',
      icon: 'plus',
      label: 'label.add.secondary.storage',
      listView: true,
      args: ['name', 'provider', 'zoneid', 'url', 'details']
    },
    {
      api: 'deleteImageStore',
      icon: 'delete',
      label: 'label.action.delete.secondary.storage',
      dataView: true
    }
  ]
}
