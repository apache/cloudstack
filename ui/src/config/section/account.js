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
  name: 'account',
  title: 'label.accounts',
  icon: 'team-outlined',
  docHelp: 'adminguide/accounts.html',
  permission: ['listAccounts'],
  columns: ['name', 'state', 'rolename', 'roletype', 'domainpath'],
  details: ['name', 'id', 'rolename', 'roletype', 'domainpath', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'receivedbytes', 'sentbytes', 'created'],
  related: [{
    name: 'accountuser',
    title: 'label.users',
    param: 'account'
  }],
  filters: () => {
    const filters = ['enabled', 'disabled', 'locked']
    return filters
  },
  tabs: [
    {
      name: 'details',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
    },
    {
      name: 'resources',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/ResourceCountUsage.vue')))
    },
    {
      name: 'limits',
      show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) },
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/ResourceLimitTab.vue')))
    },
    {
      name: 'certificate',
      component: shallowRef(defineAsyncComponent(() => import('@/views/iam/SSLCertificateTab.vue')))
    },
    {
      name: 'settings',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/SettingsTab.vue'))),
      show: () => { return 'listConfigurations' in store.getters.apis }
    },
    {
      name: 'events',
      resourceType: 'Account',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
      show: () => { return 'listEvents' in store.getters.apis }
    }
  ],
  actions: [
    {
      api: 'createAccount',
      icon: 'plus-outlined',
      label: 'label.add.account',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/iam/AddAccount.vue')))
    },
    {
      api: 'ldapCreateAccount',
      icon: 'user-add-outlined',
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
      icon: 'edit-outlined',
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
      icon: 'sync-outlined',
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
      icon: 'play-circle-outlined',
      label: 'label.action.enable.account',
      message: 'message.enable.account',
      dataView: true,
      show: (record, store) => {
        return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !record.isdefault &&
          !(record.domain === 'ROOT' && record.name === 'admin' && record.accounttype === 1) &&
          (record.state === 'disabled' || record.state === 'locked')
      },
      params: { lock: 'false' },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'disableAccount',
      icon: 'pause-circle-outlined',
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
      },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x, lock: false } }) }
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
      },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x, lock: true } }) }
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
      icon: 'delete-outlined',
      label: 'label.action.delete.account',
      message: 'message.delete.account',
      dataView: true,
      disabled: (record, store) => {
        return record.id !== 'undefined' && store.userInfo.accountid === record.id
      },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    }
  ]
}
