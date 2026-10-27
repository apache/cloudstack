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
  name: 'customaction',
  title: 'label.custom.actions',
  icon: 'play-square-outlined',
  docHelp: 'adminguide/extensions.html#custom-actions',
  permission: ['listCustomActions'],
  resourceType: 'ExtensionCustomAction',
  hidden: true,
  columns: ['name', 'extensionname', 'enabled', 'created'],
  details: ['name', 'id', 'description', 'extensionname', 'allowedroletypes', 'resourcetype', 'parameters', 'timeout', 'successmessage', 'errormessage', 'details', 'created'],
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  },
  {
    name: 'events',
    resourceType: 'ExtensionCustomAction',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
    show: () => { return 'listEvents' in store.getters.apis }
  }],
  actions: [
    {
      api: 'addCustomAction',
      icon: 'plus-outlined',
      label: 'label.add.custom.action',
      docHelp: 'adminguide/extensions.html#custom-actions',
      listView: true,
      popup: true,
      show: (record) => { return false }, // Hidden for now
      component: shallowRef(defineAsyncComponent(() => import('@/views/extension/AddCustomAction.vue')))
    },
    {
      api: 'updateCustomAction',
      icon: 'edit-outlined',
      label: 'label.update.custom.action',
      message: 'message.action.update.extension',
      dataView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/extension/UpdateCustomAction.vue')))
    },
    {
      api: 'updateCustomAction',
      icon: 'play-circle-outlined',
      label: 'label.enable.custom.action',
      message: 'message.confirm.enable.custom.action',
      dataView: true,
      groupAction: true,
      popup: true,
      defaultArgs: { enabled: true },
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
      show: (record) => { return !record.enabled }
    },
    {
      api: 'updateCustomAction',
      icon: 'pause-circle-outlined',
      label: 'label.disable.custom.action',
      message: 'message.confirm.disable.custom.action',
      dataView: true,
      groupAction: true,
      popup: true,
      defaultArgs: { enabled: false },
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
      show: (record) => { return record.enabled }
    },
    {
      api: 'deleteCustomAction',
      icon: 'delete-outlined',
      label: 'label.delete.custom.action',
      message: 'message.action.delete.custom.action',
      dataView: true,
      popup: true
    }
  ]
}
