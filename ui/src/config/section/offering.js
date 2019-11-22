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
      columns: ['name', 'displaytext', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone'],
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
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteServiceOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    },
    {
      name: 'systemoffering',
      title: 'System Offerings',
      icon: 'setting',
      permission: ['listServiceOfferings', 'listInfrastructure'],
      params: { issystem: 'true' },
      columns: ['name', 'systemvmtype', 'cpunumber', 'cpuspeed', 'memory', 'storagetype', 'tags'],
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
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteServiceOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        params: { issystem: 'true' },
        args: ['id']
      }]
    },
    {
      name: 'diskoffering',
      title: 'Disk Offerings',
      icon: 'hdd',
      permission: ['listDiskOfferings'],
      columns: ['name', 'displaytext', 'disksize', 'tags', 'domain', 'zone'],
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
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteDiskOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    },
    {
      name: 'networkoffering',
      title: 'Network Offerings',
      icon: 'wifi',
      permission: ['listNetworkOfferings'],
      columns: ['name', 'state', 'guestiptype', 'traffictype', 'networkrate', 'tags', 'domain', 'zone'],
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
        args: ['id', 'name', 'displaytext', 'availability']
      }, {
        api: 'deleteNetworkOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    },
    {
      name: 'vpcoffering',
      title: 'VPC Offerings',
      icon: 'deployment-unit',
      permission: ['listVPCOfferings'],
      resourceType: 'VpcOffering',
      columns: ['name', 'state', 'displaytext', 'domain', 'zone'],
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
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteVPCOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    }
  ]
}
