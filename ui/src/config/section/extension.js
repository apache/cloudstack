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
  title: 'label.external',
  icon: 'partition-outlined',
  docHelp: 'adminguide/extensions.html',
  children: [
    {
      name: 'xaas',
      title: 'label.xaas',
      icon: 'rocket-outlined',
      docHelp: 'adminguide/extensions.html',
      permission: ['listExtensions'],
      params: { type: 'Orchestrator' },
      resourceType: 'Extension',
      columns: () => {
        var fields = ['name', 'type', 'script', 'created']
        return fields
      },
      details: () => {
        var fields = ['name', 'id', 'type', 'details', 'script', 'extensionresourceid', 'created']
        return fields
      },
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      },
      {
        name: 'events',
        resourceType: 'Extension',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in store.getters.apis }
      },
      {
        name: 'comments',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
      }],
      actions: [
        {
          api: 'createExtension',
          icon: 'plus-outlined',
          label: 'label.action.create.extension',
          docHelp: 'adminguide/extensions.html',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/extension/CreateExtension.vue')))
        },
        {
          api: 'registerExtension',
          icon: 'api-outlined',
          label: 'label.register.extension',
          message: 'message.action.register.extension',
          dataView: true,
          popup: true,
          args: ['resourcetype', 'resourceid', 'externaldetails', 'extensionid'],
          mapping: {
            resourcetype: {
              options: ['Cluster']
            },
            extensionid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteExtension',
          icon: 'delete-outlined',
          label: 'label.delete.extension',
          message: 'message.action.delete.extension',
          dataView: true,
          popup: true
        }
      ]
    }
  ]
}
