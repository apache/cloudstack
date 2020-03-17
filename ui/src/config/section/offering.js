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
  name: 'offering',
  title: 'Offerings',
  icon: 'shopping',
  permission: ['listServiceOfferings'],
  children: [
    {
      name: 'computeoffering',
      title: 'Compute Offerings',
      icon: 'cloud',
      permission: ['listServiceOfferings'],
      params: { isrecursive: 'true' },
      columns: ['name', 'displaytext', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'order'],
      details: ['name', 'id', 'displaytext', 'offerha', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'created'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'serviceofferingid'
      }],
      actions: [{
        api: 'createServiceOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddComputeOffering.vue')
      }, {
        api: 'updateServiceOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['name', 'displaytext']
      }, {
        api: 'updateServiceOffering',
        icon: 'lock',
        label: 'Update Offering Access',
        dataView: true,
        popup: true,
        component: () => import('@/views/offering/UpdateOfferingAccess.vue')
      }, {
        api: 'deleteServiceOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true
      }]
    },
    {
      name: 'systemoffering',
      title: 'System Offerings',
      icon: 'setting',
      permission: ['listServiceOfferings', 'listInfrastructure'],
      params: { issystem: 'true', isrecursive: 'true' },
      columns: ['name', 'systemvmtype', 'cpunumber', 'cpuspeed', 'memory', 'storagetype', 'tags', 'order'],
      details: ['name', 'id', 'displaytext', 'systemvmtype', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'created'],
      actions: [{
        api: 'createServiceOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        params: { issystem: 'true' },
        popup: true,
        component: () => import('@/views/offering/AddComputeOffering.vue')
      }, {
        api: 'updateServiceOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        params: { issystem: 'true' },
        args: ['name', 'displaytext']
      }, {
        api: 'deleteServiceOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        params: { issystem: 'true' }
      }]
    },
    {
      name: 'diskoffering',
      title: 'Disk Offerings',
      icon: 'hdd',
      permission: ['listDiskOfferings'],
      params: { isrecursive: 'true' },
      columns: ['name', 'displaytext', 'disksize', 'tags', 'domain', 'zone', 'order'],
      details: ['name', 'id', 'displaytext', 'disksize', 'provisioningtype', 'storagetype', 'iscustomized', 'tags', 'domain', 'zone', 'created'],
      related: [{
        name: 'volume',
        title: 'Volumes',
        param: 'diskofferingid'
      }],
      actions: [{
        api: 'createDiskOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddDiskOffering.vue')
      }, {
        api: 'updateDiskOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['name', 'displaytext']
      }, {
        api: 'updateDiskOffering',
        icon: 'lock',
        label: 'Update Offering Access',
        dataView: true,
        popup: true,
        component: () => import('@/views/offering/UpdateOfferingAccess.vue')
      }, {
        api: 'deleteDiskOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true
      }]
    },
    {
      name: 'networkoffering',
      title: 'Network Offerings',
      icon: 'wifi',
      permission: ['listNetworkOfferings'],
      params: { isrecursive: 'true' },
      columns: ['name', 'state', 'guestiptype', 'traffictype', 'networkrate', 'tags', 'domain', 'zone', 'order'],
      details: ['name', 'id', 'displaytext', 'guestiptype', 'traffictype', 'networkrate', 'ispersistent', 'egressdefaultpolicy', 'availability', 'conservemode', 'specifyvlan', 'specifyipranges', 'supportspublicaccess', 'supportsstrechedl2subnet', 'service', 'tags', 'domain', 'zone'],
      actions: [{
        api: 'createNetworkOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddNetworkOffering.vue')
      }, {
        api: 'updateNetworkOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['name', 'displaytext', 'availability'],
        mapping: {
          availability: {
            options: ['Optional', 'Required']
          }
        }
      }, {
        api: 'updateNetworkOffering',
        icon: 'play-circle',
        label: 'Enable Offering',
        dataView: true,
        show: (record) => { return record.state === 'Disabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Enabled' }
          }
        }
      }, {
        api: 'updateNetworkOffering',
        icon: 'pause-circle',
        label: 'Disable Offering',
        dataView: true,
        show: (record) => { return record.state === 'Enabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Disabled' }
          }
        }
      }, {
        api: 'updateNetworkOffering',
        icon: 'lock',
        label: 'Update Offering Access',
        dataView: true,
        popup: true,
        component: () => import('@/views/offering/UpdateOfferingAccess.vue')
      }, {
        api: 'deleteNetworkOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true
      }]
    },
    {
      name: 'vpcoffering',
      title: 'VPC Offerings',
      icon: 'deployment-unit',
      permission: ['listVPCOfferings'],
      params: { isrecursive: 'true' },
      resourceType: 'VpcOffering',
      columns: ['name', 'state', 'displaytext', 'domain', 'zone', 'order'],
      details: ['name', 'id', 'displaytext', 'distributedvpcrouter', 'service', 'tags', 'domain', 'zone', 'created'],
      related: [{
        name: 'vpc',
        title: 'VPCs',
        param: 'vpcofferingid'
      }],
      actions: [{
        api: 'createVPCOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddVpcOffering.vue')
      }, {
        api: 'updateVPCOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['name', 'displaytext']
      }, {
        api: 'updateVPCOffering',
        icon: 'play-circle',
        label: 'Enable Offering',
        dataView: true,
        show: (record) => { return record.state === 'Disabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Enabled' }
          }
        }
      }, {
        api: 'updateVPCOffering',
        icon: 'pause-circle',
        label: 'Disable Offering',
        dataView: true,
        show: (record) => { return record.state === 'Enabled' },
        args: ['state'],
        mapping: {
          state: {
            value: (record) => { return 'Disabled' }
          }
        }
      }, {
        api: 'updateVPCOffering',
        icon: 'lock',
        label: 'Update Offering Access',
        dataView: true,
        popup: true,
        component: () => import('@/views/offering/UpdateOfferingAccess.vue')
      }, {
        api: 'deleteVPCOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true
      }]
    }
  ]
}
