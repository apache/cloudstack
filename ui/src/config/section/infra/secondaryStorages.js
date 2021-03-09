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
  name: 'imagestore',
  title: 'label.secondary.storage',
  icon: 'picture',
  docHelp: 'adminguide/storage.html#secondary-storage',
  permission: ['listImageStores'],
  columns: () => {
    var fields = ['name', 'url', 'protocol', 'scope', 'zonename']
    if (store.getters.apis.listImageStores.params.filter(x => x.name === 'readonly').length > 0) {
      fields.push({
        field: 'readonly',
        customTitle: 'access'
      })
    }
    return fields
  },
  details: () => {
    var fields = ['name', 'id', 'url', 'protocol', 'provider', 'scope', 'zonename']
    if (store.getters.apis.listImageStores.params.filter(x => x.name === 'readonly').length > 0) {
      fields.push('readonly')
    }
    return fields
  },
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'settings',
    component: () => import('@/components/view/SettingsTab.vue')
  }],
  actions: [
    {
      api: 'migrateSecondaryStorageData',
      icon: 'drag',
      label: 'label.migrate.data.from.image.store',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/MigrateData.vue')
    },
    {
      api: 'addImageStore',
      icon: 'plus',
      docHelp: 'installguide/configuration.html#add-secondary-storage',
      label: 'label.add.secondary.storage',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/AddSecondaryStorage.vue')
    },
    {
      api: 'updateImageStore',
      icon: 'stop',
      label: 'label.action.image.store.read.only',
      message: 'message.action.secondary.storage.read.only',
      dataView: true,
      defaultArgs: { readonly: true },
      show: (record) => { return record.readonly === false }
    },
    {
      api: 'updateImageStore',
      icon: 'check-circle',
      label: 'label.action.image.store.read.write',
      message: 'message.action.secondary.storage.read.write',
      dataView: true,
      defaultArgs: { readonly: false },
      show: (record) => { return record.readonly === true }
    },
    {
      api: 'deleteImageStore',
      icon: 'delete',
      label: 'label.action.delete.secondary.storage',
      message: 'message.action.delete.secondary.storage',
      dataView: true
    }
  ]
}
