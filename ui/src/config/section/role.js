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
  name: 'role',
  title: 'label.roles',
  icon: 'idcard',
  docHelp: 'adminguide/accounts.html#roles',
  permission: ['listRoles', 'listRolePermissions'],
  columns: ['name', 'type', 'description'],
  details: ['name', 'id', 'type', 'description'],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'rules',
    component: () => import('@/views/iam/RolePermissionTab.vue')
  }],
  actions: [
    {
      api: 'createRole',
      icon: 'plus',
      label: 'label.add.role',
      listView: true,
      popup: true,
      component: () => import('@/views/iam/CreateRole.vue')
    },
    {
      api: 'importRole',
      icon: 'cloud-upload',
      label: 'label.import.role',
      listView: true,
      popup: true,
      component: () => import('@/views/iam/ImportRole.vue')
    },
    {
      api: 'updateRole',
      icon: 'edit',
      label: 'label.edit.role',
      dataView: true,
      args: ['name', 'description', 'type'],
      mapping: {
        type: {
          options: ['Admin', 'DomainAdmin', 'User']
        }
      }
    },
    {
      api: 'deleteRole',
      icon: 'delete',
      label: 'label.delete.role',
      message: 'label.delete.role',
      dataView: true
    }
  ]
}
