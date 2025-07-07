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
  name: 'extension',
  title: 'label.extensions',
  icon: 'appstore-add-outlined',
  docHelp: 'adminguide/extensions.html',
  permission: ['listExtensions'],
  params: (dataView) => {
    const params = {}
    if (!dataView) {
      params.details = 'min'
    }
    return params
  },
  resourceType: 'Extension',
  columns: () => {
    var fields = ['name', 'state', 'type', 'path',
      {
        availability: (record) => {
          if (record.pathready) {
            return 'Ready'
          }
          return 'Not Ready'
        }
      }, 'created']
    return fields
  },
  details: ['name', 'description', 'id', 'type', 'details', 'path', 'pathready', 'isuserdefined', 'orchestratorrequirespreparevm', 'created'],
  filters: ['orchestrator'],
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  },
  {
    name: 'resources',
    component: shallowRef(defineAsyncComponent(() => import('@/views/extension/ExtensionResourcesTab.vue')))
  },
  {
    name: 'customactions',
    component: shallowRef(defineAsyncComponent(() => import('@/views/extension/ExtensionCustomActionsTab.vue')))
  },
  {
    name: 'events',
    resourceType: 'Extension',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
    show: () => { return 'listEvents' in store.getters.apis }
  }],
  related: [
    {
      name: 'vm',
      title: 'label.instances',
      param: 'extensionid'
    },
    {
      name: 'template',
      title: 'label.templates',
      param: 'extensionid'
    }
  ],
  actions: [
    {
      api: 'createExtension',
      icon: 'plus-outlined',
      label: 'label.create.extension',
      docHelp: 'adminguide/extensions.html',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/extension/CreateExtension.vue')))
    },
    {
      api: 'updateExtension',
      icon: 'edit-outlined',
      label: 'label.update.extension',
      dataView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/extension/UpdateExtension.vue')))
    },
    {
      api: 'registerExtension',
      icon: 'api-outlined',
      label: 'label.register.extension',
      message: 'message.action.register.extension',
      dataView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/extension/RegisterExtension.vue')))
    },
    {
      api: 'updateExtension',
      icon: 'play-circle-outlined',
      label: 'label.enable.extension',
      message: 'message.confirm.enable.extension',
      dataView: true,
      groupAction: true,
      popup: true,
      defaultArgs: { state: 'Enabled' },
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
      show: (record) => { return ['Disabled'].includes(record.state) }
    },
    {
      api: 'updateExtension',
      icon: 'pause-circle-outlined',
      label: 'label.disable.extension',
      message: 'message.confirm.disable.extension',
      dataView: true,
      groupAction: true,
      popup: true,
      defaultArgs: { state: 'Disabled' },
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
      show: (record) => { return ['Enabled'].includes(record.state) }
    },
    {
      api: 'deleteExtension',
      icon: 'delete-outlined',
      label: 'label.delete.extension',
      message: 'message.action.delete.extension',
      dataView: true,
      popup: true,
      args: ['id', 'cleanup'],
      mapping: {
        id: {
          value: (record, params) => { return record.id }
        },
        cleanup: false
      },
      show: (record) => { return record.isuserdefined }
    }
  ]
}
