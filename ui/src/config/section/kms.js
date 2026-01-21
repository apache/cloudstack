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
  name: 'kms',
  title: 'label.kms',
  icon: 'hdd-outlined',
  children: [
    {
      name: 'KMS key',
      title: 'label.kms.keys',
      icon: 'file-text-outlined',
      permission: ['listKMSKeys'],
      resourceType: 'KMSKey',
      columns: () => {
        const fields = ['name', 'state', 'account', 'domain', 'purpose']
        return fields
      },
      details: ['id', 'name', 'description', 'state', 'account', 'domain', 'created'],
      searchFilters: () => {
        var filters = ['zoneid']
        if (store.getters.userInfo.roletype === 'Admin') {
          filters.push('accountid', 'domainid')
        }
        return filters
      },
      actions: [
        {
          api: 'createKMSKey',
          icon: 'plus-outlined',
          label: 'label.create.kms.key',
          listView: true,
          popup: true,
          dataView: true,
          args: (record, store, group) => {
            var fields = ['zoneid', 'name', 'description', 'purpose', 'hsmprofileid', 'keybits']
            return (['Admin'].includes(store.userInfo.roletype))
              ? fields.concat(['domainid', 'account']) : fields
          }
        },
        {
          api: 'updateKMSKey',
          icon: 'edit-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.update.kms.ket',
          dataView: true,
          popup: true,
          args: ['id', 'name', 'description', 'state'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        },
        {
          api: 'deleteKMSKey',
          icon: 'delete-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.delete.kms.key',
          message: 'message.action.delete.kms.key',
          dataView: true,
          popup: true,
          args: ['id'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        }
      ]
    },
    {
      name: 'hsmprofile',
      title: 'label.hsm.profile',
      icon: 'file-text-outlined',
      permission: ['listHSMProfiles'],
      resourceType: 'HSMProfile',
      columns: () => {
        const fields = ['name', 'state']
        return fields
      },
      details: ['id', 'name', 'description', 'state', 'account', 'domain', 'created'],
      searchFilters: () => {
        var filters = ['zoneid']
        return filters
      },
      actions: [
        {
          api: 'addHSMProfile',
          icon: 'plus-outlined',
          label: 'label.create.hsmprofile',
          listView: true,
          popup: true,
          dataView: true,
          args: (record, store, group) => {
            return (['Admin'].includes(store.userInfo.roletype))
              ? ['zoneid', 'name', 'vendorname', 'domainid', 'accountid', 'details', 'protocol'] : ['zoneid', 'name', 'vendorname', 'details', 'protocol']
          }
        },
        {
          api: 'updateHSMProfile',
          icon: 'edit-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.update.hsm.profile',
          dataView: true,
          popup: true,
          args: ['id', 'name', 'details', 'enabled'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        },
        {
          api: 'deleteHSMProfile',
          icon: 'delete-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.delete.hsm.profile',
          message: 'message.action.delete.hsm.profile',
          dataView: true,
          popup: true,
          args: ['id'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        }
      ]
    }
  ]
}
