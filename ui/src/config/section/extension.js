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
  name: 'external',
  title: 'label.external',
  icon: 'partition-outlined',
  children: [
    {
      name: 'xaas',
      title: 'label.extension',
      icon: 'node-expand-outlined',
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
      columns: ['name', 'type', 'entrypoint', 'entrypointsync', 'isuserdefined', 'created'],
      details: ['name', 'id', 'type', 'details', 'entrypoint', 'entrypointsync', 'isuserdefined', 'created'],
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
          }
        }
      ]
    },
    {
      name: 'extca',
      title: 'label.custom.actions',
      icon: 'play-square-outlined',
      docHelp: 'adminguide/extensions.html#custom-actions',
      permission: ['listCustomActions'],
      resourceType: 'ExtensionCustomAction',
      hidden: true,
      columns: ['name', 'extensionname', 'enabled', 'created'],
      details: ['name', 'id', 'description', 'extensionname', 'roles', 'resourcetype', 'parameters', 'details', 'created'],
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
        // {
        //   api: 'addCustomAction',
        //   icon: 'plus-outlined',
        //   label: 'label.add.custom.action',
        //   docHelp: 'adminguide/extensions.html#custom-actions',
        //   listView: true,
        //   popup: true,
        //   component: shallowRef(defineAsyncComponent(() => import('@/views/extension/AddCustomAction.vue')))
        // },
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
          api: 'deleteCustomAction',
          icon: 'delete-outlined',
          label: 'label.delete.custom.action',
          message: 'message.action.delete.custom.action',
          dataView: true,
          popup: true
        }
      ]
    }
  ]
}
