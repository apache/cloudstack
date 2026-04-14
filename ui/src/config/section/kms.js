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
  name: 'kms',
  title: 'label.kms',
  icon: 'hdd-outlined',
  show: () => {
    return ['Admin'].includes(store.getters.userInfo.roletype) || store.getters.features.hashsmprofiles
  },
  children: [
    {
      name: 'kmskey',
      title: 'label.kms.keys',
      icon: 'file-text-outlined',
      permission: ['listKMSKeys'],
      resourceType: 'KMSKey',
      columns: () => {
        const fields = ['name', 'enabled', 'purpose', 'hsmprofile']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
        }
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push('domain')
        return fields
      },
      details: ['id', 'name', 'description', 'version', 'enabled', 'account', 'domain', 'project', 'created', 'hsmprofile'],
      related: [
        {
          name: 'volume',
          title: 'label.volumes',
          param: 'kmskeyid'
        }
      ],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'KmsKey',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => {
            return 'listEvents' in store.getters.apis
          }
        }
      ],
      searchFilters: () => {
        var filters = ['zoneid', 'hsmprofileid']
        if (store.getters.userInfo.roletype === 'Admin') {
          filters.push('account', 'domainid', 'projectid')
        }
        return filters
      },
      actions: [
        {
          api: 'createKMSKey',
          icon: 'plus-outlined',
          label: 'label.create.kms.key',
          docHelp: 'adminguide/kms.html#creating-a-kms-key',
          listView: true,
          popup: true,
          dataView: false,
          args: (record, store, group) => {
            return ['Admin'].includes(store.userInfo.roletype)
              ? ['zoneid', 'domainid', 'account', 'projectid', 'name', 'description', 'hsmprofileid', 'keybits']
              : ['zoneid', 'name', 'description', 'hsmprofileid', 'keybits']
          }
        },
        {
          api: 'updateKMSKey',
          icon: 'edit-outlined',
          label: 'label.update.kms.key',
          dataView: true,
          popup: true,
          args: ['id', 'name', 'description', 'enabled'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        },
        {
          api: 'rotateKMSKey',
          icon: 'sync-outlined',
          docHelp: 'adminguide/kms.html#rotating-a-kms-key',
          label: 'label.rotate.kms.key',
          dataView: true,
          popup: true,
          args: ['id', 'keybits', 'hsmprofileid'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        },
        {
          api: 'migrateVolumesToKMS',
          icon: 'swap-outlined',
          docHelp: 'adminguide/kms.html#migrating-existing-volumes-to-kms',
          label: 'label.migrate.volumes.to.kms',
          message: 'message.action.migrate.volumes.to.kms',
          dataView: true,
          popup: true,
          show: (record, store) => {
            return ['Admin'].includes(store.userInfo.roletype)
          },
          args: (record, store) => {
            var fields = ['zoneid', 'kmskeyid', 'volumeids']
            if (['Admin'].includes(store.userInfo.roletype)) {
              fields = fields.concat(['account', 'domainid'])
            }
            return fields
          },
          mapping: {
            kmskeyid: {
              value: (record) => record.id
            }
          }
        },
        {
          api: 'deleteKMSKey',
          icon: 'delete-outlined',
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
      icon: 'safety-outlined',
      permission: ['listHSMProfiles'],
      show: () => { return ['Admin'].includes(store.getters.userInfo.roletype) },
      resourceType: 'HSMProfile',
      columns: () => {
        const fields = ['name', 'enabled']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
        }
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push('domain')
        return fields
      },
      details: ['id', 'name', 'description', 'enabled', 'account', 'domain', 'project', 'created', 'details'],
      related: [
        {
          name: 'kmskey',
          title: 'label.kms.keys',
          param: 'hsmprofileid'
        }
      ],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'HsmProfile',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => {
            return 'listEvents' in store.getters.apis
          }
        }
      ],
      searchFilters: () => {
        var filters = ['zoneid']
        if (store.getters.userInfo.roletype === 'Admin') {
          filters.push('account', 'domainid', 'projectid')
        }
        return filters
      },
      actions: [
        {
          api: 'createHSMProfile',
          icon: 'plus-outlined',
          docHelp: 'adminguide/kms.html#adding-an-hsm-profile',
          label: 'label.create.hsmprofile',
          listView: true,
          popup: true,
          dataView: false,
          show: (record, store) => {
            return ['Admin'].includes(store.userInfo.roletype)
          },
          args: (record, store, group) => {
            return ['Admin'].includes(store.userInfo.roletype)
              ? ['name', 'zoneid', 'vendorname', 'domainid', 'account', 'projectid', 'details', 'system']
              : ['name', 'zoneid', 'vendorname', 'details']
          },
          mapping: {
            details: {
              optionalKeys: ['pin', 'library', 'slot', 'slot_list_index', 'token_label']
            }
          }
        },
        {
          api: 'updateHSMProfile',
          icon: 'edit-outlined',
          label: 'label.update.hsm.profile',
          dataView: true,
          popup: true,
          show: (record, store) => {
            return ['Admin'].includes(store.userInfo.roletype)
          },
          args: ['id', 'name', 'enabled'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        },
        {
          api: 'deleteHSMProfile',
          icon: 'delete-outlined',
          label: 'label.delete.hsm.profile',
          message: 'message.action.delete.hsm.profile',
          dataView: true,
          popup: true,
          show: (record, store) => {
            return ['Admin'].includes(store.userInfo.roletype)
          },
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
