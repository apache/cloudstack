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
  name: 'offering',
  title: 'label.menu.service.offerings',
  icon: 'shopping-outlined',
  permission: ['listServiceOfferings', 'listDiskOfferings'],
  children: [
    {
      name: 'computeoffering',
      title: 'label.compute.offerings',
      docHelp: 'adminguide/service_offerings.html#compute-and-disk-service-offerings',
      icon: 'cloud-outlined',
      permission: ['listServiceOfferings'],
      searchFilters: ['name', 'zoneid', 'domainid', 'cpunumber', 'cpuspeed', 'memory'],
      params: () => {
        var params = {}
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          params = { isrecursive: 'true' }
        }
        return params
      },
      filters: ['active', 'inactive'],
      columns: ['name', 'displaytext', 'state', 'cpunumber', 'cpuspeed', 'memory', 'domain', 'zone', 'order'],
      details: () => {
        var fields = ['name', 'id', 'displaytext', 'offerha', 'provisioningtype', 'storagetype', 'iscustomized', 'iscustomizediops', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'hosttags', 'tags', 'storagetags', 'domain', 'zone', 'created', 'dynamicscalingenabled', 'diskofferingstrictness', 'encryptroot', 'purgeresources']
        if (store.getters.apis.createServiceOffering &&
          store.getters.apis.createServiceOffering.params.filter(x => x.name === 'storagepolicy').length > 0) {
          fields.splice(6, 0, 'vspherestoragepolicy')
        }
        if (store.getters.apis.createServiceOffering &&
          store.getters.apis.createServiceOffering.params.filter(x => x.name === 'rootdisksize').length > 0) {
          fields.splice(12, 0, 'rootdisksize')
        }
        const detailFields = ['minmemory', 'maxmemory', 'mincpunumber', 'maxcpunumber']
        for (const field of detailFields) {
          if (store.getters.apis.createServiceOffering &&
              store.getters.apis.createServiceOffering.params.filter(x => field === x.name).length > 0) {
            fields.push(field)
          }
        }
        return fields
      },
      resourceType: 'ServiceOffering',
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'ServiceOffering',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue'))),
          show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) }
        }
      ],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'serviceofferingid'
      }],
      actions: [{
        api: 'createServiceOffering',
        icon: 'plus-outlined',
        label: 'label.add.compute.offering',
        docHelp: 'adminguide/service_offerings.html#creating-a-new-compute-offering',
        listView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/AddComputeOffering.vue')))
      }, {
        api: 'updateServiceOffering',
        icon: 'edit-outlined',
        label: 'label.edit',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        args: ['name', 'displaytext', 'storagetags', 'hosttags']
      }, {
        api: 'updateServiceOffering',
        icon: 'lock-outlined',
        label: 'label.action.update.offering.access',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/UpdateOfferingAccess.vue')))
      },
      {
        api: 'updateServiceOffering',
        icon: 'play-circle-outlined',
        label: 'label.action.enable.service.offering',
        message: 'message.action.enable.service.offering',
        dataView: true,
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Active' }
          }
        },
        groupAction: true,
        popup: true,
        show: (record) => { return record.state !== 'Active' },
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Active' } }) }
      }, {
        api: 'deleteServiceOffering',
        icon: 'pause-circle-outlined',
        label: 'label.action.disable.service.offering',
        message: 'message.action.disable.service.offering',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        groupAction: true,
        popup: true,
        show: (record) => { return record.state === 'Active' },
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      }]
    },
    {
      name: 'systemoffering',
      title: 'label.system.offerings',
      icon: 'setting-outlined',
      docHelp: 'adminguide/service_offerings.html#system-service-offerings',
      permission: ['listServiceOfferings', 'listInfrastructure'],
      searchFilters: ['name', 'zoneid', 'domainid', 'cpunumber', 'cpuspeed', 'memory'],
      params: { issystem: 'true', isrecursive: 'true' },
      columns: ['name', 'state', 'systemvmtype', 'cpunumber', 'cpuspeed', 'memory', 'storagetype', 'order'],
      filters: ['active', 'inactive'],
      details: ['name', 'id', 'displaytext', 'systemvmtype', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'storagetags', 'hosttags', 'tags', 'domain', 'zone', 'created', 'dynamicscalingenabled', 'diskofferingstrictness'],
      resourceType: 'ServiceOffering',
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'ServiceOffering',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue'))),
          show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) }
        }
      ],
      actions: [{
        api: 'createServiceOffering',
        icon: 'plus-outlined',
        label: 'label.add.system.service.offering',
        docHelp: 'adminguide/service_offerings.html#creating-a-new-system-service-offering',
        listView: true,
        params: { issystem: 'true' },
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/AddComputeOffering.vue')))
      }, {
        api: 'updateServiceOffering',
        icon: 'edit-outlined',
        label: 'label.edit',
        dataView: true,
        params: { issystem: 'true' },
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        args: ['name', 'displaytext', 'storagetags', 'hosttags']
      }, {
        api: 'updateServiceOffering',
        icon: 'play-circle-outlined',
        label: 'label.action.enable.system.service.offering',
        message: 'message.action.enable.system.service.offering',
        dataView: true,
        params: { issystem: 'true' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Active' }
          }
        },
        groupAction: true,
        popup: true,
        show: (record) => { return record.state !== 'Active' },
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Active' } }) }
      }, {
        api: 'deleteServiceOffering',
        icon: 'pause-circle-outlined',
        label: 'label.action.disable.system.service.offering',
        message: 'message.action.disable.system.service.offering',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        params: { issystem: 'true' },
        groupAction: true,
        popup: true,
        show: (record) => { return record.state === 'Active' },
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      }]
    },
    {
      name: 'diskoffering',
      title: 'label.disk.offerings',
      icon: 'hdd-outlined',
      docHelp: 'adminguide/service_offerings.html#compute-and-disk-service-offerings',
      permission: ['listDiskOfferings'],
      searchFilters: ['name', 'zoneid', 'domainid', 'storageid'],
      params: () => {
        var params = {}
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          params = { isrecursive: 'true' }
        }
        return params
      },
      columns: ['name', 'displaytext', 'state', 'disksize', 'domain', 'zone', 'order'],
      filters: ['active', 'inactive'],
      details: () => {
        var fields = ['name', 'id', 'displaytext', 'disksize', 'provisioningtype', 'storagetype', 'iscustomized', 'disksizestrictness', 'iscustomizediops',
          'diskIopsReadRate', 'diskIopsWriteRate', 'diskBytesReadRate', 'diskBytesReadRateMax', 'diskBytesWriteRate', 'diskBytesWriteRateMax', 'miniops', 'maxiops', 'tags',
          'domain', 'zone', 'created', 'encrypt']
        if (store.getters.apis.createDiskOffering &&
          store.getters.apis.createDiskOffering.params.filter(x => x.name === 'storagepolicy').length > 0) {
          fields.splice(6, 0, 'vspherestoragepolicy')
        }
        return fields
      },
      resourceType: 'DiskOffering',
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'DiskOffering',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue'))),
          show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) }
        }
      ],
      related: [{
        name: 'volume',
        title: 'label.volumes',
        param: 'diskofferingid'
      }],
      actions: [{
        api: 'createDiskOffering',
        icon: 'plus-outlined',
        label: 'label.add.disk.offering',
        docHelp: 'adminguide/service_offerings.html#creating-a-new-disk-offering',
        listView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/AddDiskOffering.vue')))
      }, {
        api: 'updateDiskOffering',
        icon: 'edit-outlined',
        label: 'label.edit',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        args: ['name', 'displaytext', 'tags']
      }, {
        api: 'updateDiskOffering',
        icon: 'lock-outlined',
        label: 'label.action.update.offering.access',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/UpdateOfferingAccess.vue')))
      }, {
        api: 'updateDiskOffering',
        icon: 'play-circle-outlined',
        label: 'label.action.enable.disk.offering',
        message: 'message.action.enable.disk.offering',
        dataView: true,
        params: { issystem: 'true' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Active' }
          }
        },
        groupAction: true,
        popup: true,
        show: (record) => { return record.state !== 'Active' },
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Active' } }) }
      }, {
        api: 'deleteDiskOffering',
        icon: 'pause-circle-outlined',
        label: 'label.action.disable.disk.offering',
        message: 'message.action.disable.disk.offering',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        groupAction: true,
        popup: true,
        show: (record) => { return record.state === 'Active' },
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      }]
    },
    {
      name: 'backupoffering',
      title: 'label.backup.offerings',
      icon: 'cloud-upload-outlined',
      docHelp: 'adminguide/virtual_machines.html#backup-offerings',
      permission: ['listBackupOfferings'],
      searchFilters: ['zoneid'],
      columns: ['name', 'description', 'zonename'],
      details: ['name', 'id', 'description', 'externalid', 'zone', 'allowuserdrivenbackups', 'created'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'backupofferingid'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'BackupOffering',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        }
      ],
      actions: [{
        api: 'importBackupOffering',
        icon: 'plus-outlined',
        label: 'label.import.backup.offering',
        docHelp: 'adminguide/virtual_machines.html#importing-backup-offerings',
        listView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/ImportBackupOffering.vue')))
      }, {
        api: 'updateBackupOffering',
        icon: 'edit-outlined',
        label: 'label.edit',
        dataView: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
        args: ['name', 'description', 'allowuserdrivenbackups']
      }, {
        api: 'deleteBackupOffering',
        icon: 'delete-outlined',
        label: 'label.action.delete.backup.offering',
        message: 'message.action.delete.backup.offering',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      }]
    },
    {
      name: 'networkoffering',
      title: 'label.network.offerings',
      icon: 'wifi-outlined',
      docHelp: 'adminguide/networking.html#network-offerings',
      permission: ['listNetworkOfferings'],
      searchFilters: ['name', 'zoneid', 'domainid', 'tags'],
      columns: ['name', 'state', 'guestiptype', 'traffictype', 'networkrate', 'domain', 'zone', 'order'],
      details: ['name', 'id', 'displaytext', 'guestiptype', 'traffictype', 'internetprotocol', 'networkrate', 'ispersistent', 'egressdefaultpolicy', 'availability', 'conservemode', 'specifyvlan', 'routingmode', 'specifyasnumber', 'specifyipranges', 'supportspublicaccess', 'supportsstrechedl2subnet', 'forvpc', 'fornsx', 'networkmode', 'service', 'tags', 'domain', 'zone'],
      resourceType: 'NetworkOffering',
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'NetworkOffering',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue'))),
          show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) }
        }
      ],
      actions: [{
        api: 'createNetworkOffering',
        icon: 'plus-outlined',
        label: 'label.add.network.offering',
        docHelp: 'adminguide/networking.html#creating-a-new-network-offering',
        listView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/AddNetworkOffering.vue')))
      }, {
        api: 'updateNetworkOffering',
        icon: 'edit-outlined',
        label: 'label.edit',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        args: ['name', 'displaytext', 'availability', 'tags'],
        mapping: {
          availability: {
            options: ['Optional', 'Required']
          }
        }
      }, {
        api: 'updateNetworkOffering',
        icon: 'play-circle-outlined',
        label: 'label.enable.network.offering',
        message: 'message.confirm.enable.network.offering',
        dataView: true,
        show: (record) => { return record.state === 'Disabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Enabled' }
          }
        },
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Enabled' } }) }
      }, {
        api: 'updateNetworkOffering',
        icon: 'pause-circle-outlined',
        label: 'label.disable.network.offering',
        message: 'message.confirm.disable.network.offering',
        dataView: true,
        show: (record) => { return record.state === 'Enabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Disabled' }
          }
        },
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Disabled' } }) }
      }, {
        api: 'updateNetworkOffering',
        icon: 'lock-outlined',
        label: 'label.action.update.offering.access',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/UpdateOfferingAccess.vue')))
      }, {
        api: 'deleteNetworkOffering',
        icon: 'delete-outlined',
        label: 'label.remove.network.offering',
        message: 'message.confirm.remove.network.offering',
        docHelp: 'adminguide/service_offerings.html#modifying-or-deleting-a-service-offering',
        dataView: true,
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      }]
    },
    {
      name: 'vpcoffering',
      title: 'label.vpc.offerings',
      icon: 'deployment-unit-outlined',
      docHelp: 'plugins/nuage-plugin.html?#vpc-offerings',
      permission: ['listVPCOfferings'],
      searchFilters: ['name', 'zoneid', 'domainid'],
      resourceType: 'VpcOffering',
      columns: ['name', 'state', 'displaytext', 'domain', 'zone', 'order'],
      details: ['name', 'id', 'displaytext', 'internetprotocol', 'distributedvpcrouter', 'tags', 'routingmode', 'specifyasnumber', 'service', 'fornsx', 'networkmode', 'domain', 'zone', 'created'],
      related: [{
        name: 'vpc',
        title: 'label.vpc',
        param: 'vpcofferingid'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'VpcOffering',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        }
      ],
      actions: [{
        api: 'createVPCOffering',
        icon: 'plus-outlined',
        docHelp: 'plugins/nuage-plugin.html?#optional-create-and-enable-vpc-offering',
        label: 'label.add.vpc.offering',
        listView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/AddVpcOffering.vue')))
      }, {
        api: 'updateVPCOffering',
        icon: 'edit-outlined',
        label: 'label.edit',
        dataView: true,
        args: ['name', 'displaytext']
      }, {
        api: 'updateVPCOffering',
        icon: 'play-circle-outlined',
        label: 'label.enable.vpc.offering',
        message: 'message.confirm.enable.vpc.offering',
        dataView: true,
        show: (record) => { return record.state === 'Disabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Enabled' }
          }
        },
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Enabled' } }) }
      }, {
        api: 'updateVPCOffering',
        icon: 'pause-circle-outlined',
        label: 'label.disable.vpc.offering',
        message: 'message.confirm.disable.vpc.offering',
        dataView: true,
        show: (record) => { return record.state === 'Enabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Disabled' }
          }
        },
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x, state: 'Disabled' } }) }
      }, {
        api: 'updateVPCOffering',
        icon: 'lock-outlined',
        label: 'label.action.update.offering.access',
        dataView: true,
        popup: true,
        component: shallowRef(defineAsyncComponent(() => import('@/views/offering/UpdateOfferingAccess.vue')))
      }, {
        api: 'deleteVPCOffering',
        icon: 'delete-outlined',
        label: 'label.remove.vpc.offering',
        message: 'message.confirm.remove.vpc.offering',
        dataView: true,
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      }]
    }
  ]
}
