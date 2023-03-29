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
  name: 'storagepool',
  title: 'label.primary.storage',
  icon: 'database-outlined',
  docHelp: 'adminguide/storage.html#primary-storage',
  permission: ['listStoragePoolsMetrics'],
  columns: () => {
    const fields = ['name', 'state', 'ipaddress', 'scope', 'type', 'path']
    const metricsFields = ['disksizeusedgb', 'disksizetotalgb', 'disksizeallocatedgb', 'disksizeunallocatedgb']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    fields.push('clustername')
    fields.push('zonename')
    return fields
  },
  details: ['name', 'id', 'ipaddress', 'type', 'scope', 'tags', 'path', 'provider', 'hypervisor', 'overprovisionfactor', 'disksizetotal', 'disksizeallocated', 'disksizeused', 'clustername', 'podname', 'zonename', 'created'],
  related: [{
    name: 'volume',
    title: 'label.volumes',
    param: 'storageid'
  }],
  resourceType: 'PrimaryStorage',
  filters: () => {
    const filters = ['initial', 'initialized', 'creating', 'attaching', 'up', 'prepareformaintenance', 'errorinmaintenance', 'cancelmaintenance', 'maintenance', 'disabled', 'removed']
    return filters
  },
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }, {
    name: 'settings',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/SettingsTab.vue')))
  }, {
    name: 'events',
    resourceType: 'StoragePool',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
    show: () => { return 'listEvents' in store.getters.apis }
  }, {
    name: 'comments',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
  }],
  actions: [
    {
      api: 'createStoragePool',
      icon: 'plus-outlined',
      docHelp: 'installguide/configuration.html#add-primary-storage',
      label: 'label.add.primary.storage',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/AddPrimaryStorage.vue')))
    },
    {
      api: 'updateStoragePool',
      icon: 'edit-outlined',
      label: 'label.edit',
      dataView: true,
      args: ['name', 'tags', 'capacitybytes', 'capacityiops']
    },
    {
      api: 'updateStoragePool',
      icon: 'pause-circle-outlined',
      label: 'label.disable.storage',
      message: 'message.confirm.disable.storage',
      dataView: true,
      defaultArgs: { enabled: false },
      show: (record) => { return record.state === 'Up' }
    },
    {
      api: 'updateStoragePool',
      icon: 'play-circle-outlined',
      label: 'label.enable.storage',
      message: 'message.confirm.enable.storage',
      dataView: true,
      defaultArgs: { enabled: true },
      show: (record) => { return record.state === 'Disabled' }
    },
    {
      api: 'syncStoragePool',
      icon: 'sync-outlined',
      label: 'label.sync.storage',
      message: 'message.confirm.sync.storage',
      dataView: true,
      show: (record) => { return record.state === 'Up' && record.type === 'DatastoreCluster' }
    },
    {
      api: 'enableStorageMaintenance',
      icon: 'plus-square-outlined',
      label: 'label.action.enable.maintenance.mode',
      message: 'message.action.primarystorage.enable.maintenance.mode',
      dataView: true,
      show: (record) => { return ['Up', 'Connecting', 'Down', 'ErrorInMaintenance'].includes(record.state) }
    },
    {
      api: 'cancelStorageMaintenance',
      icon: 'minus-square-outlined',
      label: 'label.action.cancel.maintenance.mode',
      message: 'message.action.cancel.maintenance.mode',
      dataView: true,
      show: (record) => { return ['Maintenance', 'PrepareForMaintenance', 'ErrorInMaintenance'].includes(record.state) }
    },
    {
      api: 'deleteStoragePool',
      icon: 'delete-outlined',
      label: 'label.action.delete.primary.storage',
      dataView: true,
      args: ['forced'],
      show: (record) => { return (record.state === 'Down' || record.state === 'Maintenance' || record.state === 'Disconnected') },
      displayName: (record) => { return record.name || record.displayName || record.id }
    }
  ]
}
