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
  name: 'accountuser',
  title: 'Users',
  icon: 'user',
  hidden: true,
  permission: ['listUsers'],
  columns: ['username', 'state', 'firstname', 'lastname', 'email', 'account', 'domain'],
  details: ['username', 'id', 'firstname', 'lastname', 'email', 'usersource', 'timezone', 'rolename', 'roletype', 'account', 'domain', 'created'],
  actions: [
    {
      api: 'createUser',
      icon: 'plus',
      label: 'label.add.user',
      listView: true,
      args: ['username', 'password', 'password', 'email', 'firstname', 'lastname', 'timezone', 'account', 'domainid']
    },
    {
      api: 'updateUser',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: ['username', 'email', 'firstname', 'lastname', 'timezone']
    },
    {
      api: 'updateUser',
      icon: 'key',
      label: 'label.action.change.password',
      dataView: true,
      popup: true,
      component: () => import('@/views/iam/ChangeUserPassword.vue')
    },
    {
      api: 'registerUserKeys',
      icon: 'file-protect',
      label: 'label.action.generate.keys',
      dataView: true
    },
    {
      api: 'enableUser',
      icon: 'play-circle',
      label: 'label.action.enable.user',
      dataView: true,
      show: (record) => { return record.state === 'disabled' }
    },
    {
      api: 'disableUser',
      icon: 'pause-circle',
      label: 'label.action.disable.user',
      dataView: true,
      show: (record) => { return record.state === 'enabled' }
    },
    {
      api: 'deleteUser',
      icon: 'delete',
      label: 'label.action.delete.user',
      dataView: true
    }
  ]
}
