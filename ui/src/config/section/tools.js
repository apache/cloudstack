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
  name: 'tools',
  title: 'label.tools',
  icon: 'tool-outlined',
  children: [
    {
      name: 'comment',
      title: 'label.comments',
      icon: 'message-outlined',
      docHelp: 'adminguide/events.html',
      permission: ['listAnnotations'],
      columns: () => {
        const cols = ['entityid', 'entitytype', 'annotation', 'created', 'username']
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          cols.push('adminsonly')
        }
        return cols
      },
      searchFilters: ['entitytype', 'keyword'],
      params: () => { return { annotationfilter: 'self' } },
      filters: () => {
        const filters = ['self', 'all']
        return filters
      },
      actions: [
        {
          api: 'removeAnnotation',
          icon: 'delete-outlined',
          label: 'label.remove.annotation',
          message: 'message.remove.annotation',
          dataView: false,
          groupAction: true,
          popup: true,
          groupShow: (selectedItems, storegetters) => {
            if (['Admin'].includes(store.getters.userInfo.roletype)) {
              return true
            }
            // Display only if the selected items are comments created by the user
            return selectedItems.filter(x => { return x.username !== store.getters.userInfo.username }).length === 0
          },
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'manageinstances',
      title: 'label.action.import.export.instances',
      icon: 'interaction-outlined',
      docHelp: 'adminguide/virtual_machines.html#importing-and-unmanaging-virtual-machine',
      resourceType: 'UserVm',
      permission: ['listInfrastructure', 'listUnmanagedInstances'],
      component: () => import('@/views/tools/ManageInstances.vue')
    }
  ]
}
