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

export default {
  name: 'account',
  title: 'label.accounts',
  icon: 'TeamOutlined',
  docHelp: 'adminguide/accounts.html',
  permission: ['listAccounts'],
  columns: ['name', 'state', 'rolename', 'roletype', 'domainpath'],
  details: ['name', 'id', 'rolename', 'roletype', 'domainpath', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'receivedbytes', 'sentbytes'],
  related: [{
    name: 'accountuser',
    title: 'label.users',
    param: 'account'
  }],
  tabs: [
    {
      name: 'details',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
    }
  ],
  actions: [
    {
      api: 'createAccount',
      icon: 'PlusOutlined',
      label: 'label.add.account',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/iam/AddAccount.vue')))
    },
    {
      api: 'ldapCreateAccount',
      icon: 'UserAddOutlined',
      label: 'label.add.ldap.account',
      docHelp: 'adminguide/accounts.html#using-an-ldap-server-for-user-authentication',
      listView: true,
      popup: true,
      show: (record, store) => {
        return store.isLdapEnabled
      },
      component: shallowRef(defineAsyncComponent(() => import('@/views/iam/AddLdapAccount.vue')))
    },
    {
      api: 'updateAccount',
      icon: 'EditOutlined',
      label: 'label.action.edit.account',
      dataView: true,
      args: ['newname', 'account', 'domainid', 'networkdomain'],
      mapping: {
        account: {
          value: (record) => { return record.name }
        },
        domainid: {
          value: (record) => { return record.domainid }
        }
      }
    },
    {
      api: 'updateResourceCount',
      icon: 'SyncOutlined',
      label: 'label.action.update.resource.count',
      message: 'message.update.resource.count',
      dataView: true,
      show: (record, store) => { return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) },
      args: ['account', 'domainid'],
      mapping: {
        account: {
          value: (record) => { return record.name }
        },
        domainid: {
          value: (record) => { return record.domainid }
        }
      }
    },
    {
      api: 'enableAccount',
      icon: 'PlayCircleOutlined',
      label: 'label.action.enable.account',
      message: 'message.enable.account',
      dataView: true,
      show: (record, store) => {
        return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !record.isdefault &&
          !(record.domain === 'ROOT' && record.name === 'admin' && record.accounttype === 1) &&
          (record.state === 'disabled' || record.state === 'locked')
      },
      params: { lock: 'false' }
    },
    {
      api: 'disableAccount',
      icon: 'PauseCircleOutlined',
      label: 'label.action.disable.account',
      message: 'message.disable.account',
      dataView: true,
      show: (record, store) => {
        return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !record.isdefault &&
          !(record.domain === 'ROOT' && record.name === 'admin' && record.accounttype === 1) &&
          record.state === 'enabled'
      },
      args: ['lock'],
      mapping: {
        lock: {
          value: (record) => { return false }
        }
      }
    },
    {
      api: 'disableAccount',
      icon: 'LockOutlined',
      label: 'label.action.lock.account',
      message: 'message.lock.account',
      dataView: true,
      show: (record, store) => {
        return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !record.isdefault &&
          !(record.domain === 'ROOT' && record.name === 'admin' && record.accounttype === 1) &&
          record.state === 'enabled'
      },
      args: ['lock'],
      mapping: {
        lock: {
          value: (record) => { return true }
        }
      }
    },
    {
      api: 'uploadSslCert',
      icon: 'SafetyCertificateOutlined',
      label: 'label.add.certificate',
      dataView: true,
      args: ['name', 'certificate', 'privatekey', 'certchain', 'password', 'account', 'domainid'],
      post: true,
      show: (record) => { return record.state === 'enabled' },
      mapping: {
        account: {
          value: (record) => { return record.name }
        },
        domainid: {
          value: (record) => { return record.domainid }
        }
      }
    },
    {
      api: 'deleteAccount',
      icon: 'DeleteOutlined',
      label: 'label.action.delete.account',
      message: 'message.delete.account',
      dataView: true,
      show: (record, store) => {
        return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !record.isdefault &&
          !(record.domain === 'ROOT' && record.name === 'admin' && record.accounttype === 1)
      }
    }
  ]
}
