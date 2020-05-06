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
  name: 'zone',
  title: 'Zones',
  icon: 'global',
  permission: ['listZonesMetrics'],
  columns: ['name', 'state', 'allocationstate', 'networktype', 'clusters', 'cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal', 'order'],
  details: ['name', 'id', 'allocationstate', 'networktype', 'guestcidraddress', 'localstorageenabled', 'securitygroupsenabled', 'dns1', 'dns2', 'internaldns1', 'internaldns2'],
  related: [{
    name: 'pod',
    title: 'Pods',
    param: 'zoneid'
  }, {
    name: 'cluster',
    title: 'Clusters',
    param: 'zoneid'
  }, {
    name: 'host',
    title: 'Hosts',
    param: 'zoneid'
  }, {
    name: 'storagepool',
    title: 'Primate Storage',
    param: 'zoneid'
  }, {
    name: 'imagestore',
    title: 'Secondary Storage',
    param: 'zoneid'
  }],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'Physical Networks',
    component: () => import('@/views/infra/zone/PhysicalNetworksTab.vue')
  }, {
    name: 'System VMs',
    component: () => import('@/views/infra/zone/SystemVmsTab.vue')
  }, {
    name: 'resources',
    component: () => import('@/views/infra/zone/ZoneResources.vue')
  }, {
    name: 'settings',
    component: () => import('@/components/view/SettingsTab.vue')
  }],
  actions: [
    {
      api: 'createZone',
      icon: 'plus',
      label: 'Add Zone',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/zone/ZoneWizard.vue')
    },
    {
      api: 'updateZone',
      icon: 'edit',
      label: 'Edit Zone',
      dataView: true,
      args: ['name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'guestcidraddress', 'domain', 'localstorageenabled'],
      show: (record) => { return record.networktype === 'Advanced' }
    },
    {
      api: 'updateZone',
      icon: 'edit',
      label: 'Edit Zone',
      dataView: true,
      args: ['name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'domain', 'localstorageenabled'],
      show: (record) => { return record.networktype === 'Basic' }
    },
    {
      api: 'updateZone',
      icon: 'pause-circle',
      label: 'label.action.disable.zone',
      dataView: true,
      defaultArgs: { allocationstate: 'Disabled' },
      show: (record) => { return record.allocationstate === 'Enabled' }
    },
    {
      api: 'updateZone',
      icon: 'play-circle',
      label: 'label.action.enable.zone',
      dataView: true,
      defaultArgs: { allocationstate: 'Enabled' },
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'enableOutOfBandManagementForZone',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.outOfBandManagementEnabled ||
          record.resourcedetails.outOfBandManagementEnabled === 'false'
      },
      args: ['zoneid'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'disableOutOfBandManagementForZone',
      icon: 'minus-circle',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      },
      args: ['zoneid'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'enableHAForZone',
      icon: 'eye',
      label: 'label.ha.enable',
      dataView: true,
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.resourceHAEnabled ||
          record.resourcedetails.resourceHAEnabled === 'false'
      },
      args: ['zoneid'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'disableHAForZone',
      icon: 'eye-invisible',
      label: 'label.ha.disable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'true'
      },
      args: ['zoneid'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'addVmwareDc',
      icon: 'block',
      label: 'label.add.vmware.datacenter',
      dataView: true,
      show: record => !record.vmwaredc,
      args: ['zoneid', 'name', 'vcenter', 'username', 'password'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'updateVmwareDc',
      icon: 'block',
      label: 'label.update.vmware.datacenter',
      dataView: true,
      show: record => record.vmwaredc,
      args: ['zoneid', 'name', 'vcenter', 'username', 'password'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'removeVmwareDc',
      icon: 'minus-square',
      label: 'label.remove.vmware.datacenter',
      dataView: true,
      show: record => record.vmwaredc,
      args: ['zoneid'],
      mapping: {
        zoneid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'deleteZone',
      icon: 'delete',
      label: 'label.action.delete.zone',
      dataView: true
    }
  ]
}
