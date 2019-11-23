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
  name: 'iam',
  title: 'Identity and Access',
  icon: 'solution',
  permission: ['listAccounts', 'listUsers', 'listDomains', 'listRoles'],
  children: [
    {
      name: 'accountuser',
      title: 'Users',
      icon: 'user',
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
          args: ['id', 'username', 'email', 'firstname', 'lastname', 'timezone']
        },
        {
          api: 'updateUser',
          icon: 'key',
          label: 'Change Password',
          dataView: true,
          args: ['id', 'currentpassword', 'password']
        },
        {
          api: 'registerUserKeys',
          icon: 'file-protect',
          label: 'Generate Keys',
          dataView: true,
          args: ['id']
        },
        {
          api: 'enableUser',
          icon: 'play-circle',
          label: 'Enable User',
          dataView: true,
          show: (record) => { return record.state === 'disabled' },
          args: ['id']
        },
        {
          api: 'disableUser',
          icon: 'pause-circle',
          label: 'Disable User',
          dataView: true,
          args: ['id'],
          show: (record) => { return record.state === 'enabled' }
        },
        {
          api: 'deleteUser',
          icon: 'delete',
          label: 'Delete user',
          dataView: true,
          args: ['id']
        }
      ]
    },
    {
      name: 'account',
      title: 'Accounts',
      icon: 'team',
      permission: ['listAccounts'],
      columns: ['name', 'state', 'firstname', 'lastname', 'rolename', 'roletype', 'domain'],
      details: ['name', 'id', 'rolename', 'roletype', 'domain', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'receivedbytes', 'sentbytes', 'vmlimit', 'iplimit', 'volumelimit', 'snapshotlimit', 'templatelimit', 'vpclimit', 'cpulimit', 'memorylimit', 'networklimit', 'primarystoragelimit', 'secondarystoragelimit'],
      actions: [
        {
          api: 'createAccount',
          icon: 'plus',
          label: 'label.add.account',
          listView: true,
          args: ['username', 'password', 'password', 'email', 'firstname', 'lastname', 'domainid', 'account', 'roleid', 'timezone', 'networkdomain']
        },
        {
          api: 'updateAccount',
          icon: 'edit',
          label: 'label.update.account',
          dataView: true,
          args: ['id', 'newname', 'domainid', 'roleid', 'networkdomain', 'details']
        },
        {
          api: 'updateResourceCount',
          icon: 'sync',
          label: 'Update Resource Count',
          dataView: true,
          args: ['account', 'domainid']
        },
        {
          api: 'enableAccount',
          icon: 'play-circle',
          label: 'Enable Account',
          dataView: true,
          show: (record) => { return record.state === 'disabled' || record.state === 'locked' },
          args: ['id'],
          params: { lock: 'false' }
        },
        {
          api: 'disableAccount',
          icon: 'pause-circle',
          label: 'Disable Account',
          dataView: true,
          show: (record) => { return record.state === 'enabled' },
          args: ['id'],
          params: { lock: 'false' }
        },
        {
          api: 'disableAccount',
          icon: 'lock',
          label: 'Lock account',
          dataView: true,
          show: (record) => { return record.state === 'enabled' },
          args: ['id', 'lock']
        },
        {
          api: 'deleteAccount',
          icon: 'delete',
          label: 'Delete account',
          dataView: true,
          hidden: (record) => { return record.name === 'admin' },
          args: [
            'id'
          ]
        }
      ]
    },
    {
      name: 'domain',
      title: 'Domains',
      icon: 'block',
      permission: ['listDomains'],
      resourceType: 'Domain',
      columns: ['name', 'state', 'path', 'parentdomainname', 'level'],
      details: ['name', 'id', 'path', 'parentdomainname', 'level', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'vmlimit', 'iplimit', 'volumelimit', 'snapshotlimit', 'templatelimit', 'vpclimit', 'cpulimit', 'memorylimit', 'networklimit', 'primarystoragelimit', 'secondarystoragelimit'],
      related: [{
        name: 'account',
        title: 'Accounts',
        param: 'domainid'
      }],
      actions: [
        {
          api: 'createDomain',
          icon: 'plus',
          label: 'label.add.domain',
          listView: true,
          args: ['parentdomainid', 'name', 'networkdomain', 'domainid']
        },
        {
          api: 'updateDomain',
          icon: 'edit',
          label: 'label.action.edit.domain',
          dataView: true,
          args: ['id', 'name', 'networkdomain']
        },
        {
          api: 'updateResourceCount',
          icon: 'sync',
          label: 'label.action.update.resource.count',
          dataView: true,
          args: ['domainid']
        },
        {
          api: 'deleteDomain',
          icon: 'delete',
          label: 'label.delete.domain',
          dataView: true,
          show: (record) => { return record.level !== 0 },
          args: ['id', 'cleanup']
        }
      ]
    },
    {
      name: 'role',
      title: 'Roles',
      icon: 'idcard',
      permission: ['listRoles'],
      columns: ['name', 'type', 'description'],
      details: ['name', 'id', 'type', 'description'],
      actions: [
        {
          api: 'createRole',
          icon: 'plus',
          label: 'Create Role',
          listView: true,
          args: ['name', 'description', 'type']
        },
        {
          api: 'updateRole',
          icon: 'edit',
          label: 'Edit Role',
          dataView: true,
          args: ['id', 'name', 'description', 'type']
        },
        {
          api: 'deleteRole',
          icon: 'delete',
          label: 'label.delete.role',
          dataView: true,
          args: ['id']
        }
      ]
    }
  ]
}
