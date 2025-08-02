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

import store from '@/store'

export default {
  name: 'imagecachestore',
  title: 'label.secondary.staging.storage',
  icon: 'file-image-outlined',
  docHelp: 'adminguide/storage.html#secondary-storage',
  permission: ['listSecondaryStagingStores'],
  searchFilters: ['name', 'zoneid', 'provider'],
  hidden: true,
  columns: () => {
    var fields = ['name', 'url', 'protocol', 'scope', 'zonename']
    if (store.getters.apis.listSecondaryStagingStores.params.filter(x => x.name === 'readonly').length > 0) {
      fields.push({
        field: 'readonly',
        customTitle: 'access'
      })
    }
    return fields
  },
  details: () => {
    var fields = ['name', 'id', 'url', 'protocol', 'provider', 'scope', 'zonename']
    if (store.getters.apis.listSecondaryStagingStores.params.filter(x => x.name === 'readonly').length > 0) {
      fields.push('readonly')
    }
    return fields
  },
  resourceType: 'SecondaryStagingStorage',
  actions: [
    {
      api: 'createSecondaryStagingStore',
      icon: 'plus-outlined',
      docHelp: 'installguide/configuration.html#add-secondary-storage',
      label: 'label.add.secondary.staging.storage',
      listView: true,
      args: ['url', 'zoneid', 'scope', 'provider']
    },
    {
      api: 'deleteSecondaryStagingStore',
      icon: 'delete-outlined',
      label: 'label.action.delete.secondary.staging.storage',
      message: 'message.action.delete.secondary.staging.storage',
      dataView: true,
      displayName: (record) => { return record.name || record.displayName || record.id }
    }
  ]
}
