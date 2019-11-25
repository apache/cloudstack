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
  permission: ['listZonesMetrics', 'listZones'],
  columns: ['name', 'state', 'networktype', 'clusters', 'cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal'],
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
  actions: [
    {
      api: 'createZone',
      icon: 'plus',
      label: 'Add Zone',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/ZoneWizard.vue')
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
      api: 'dedicateZone',
      icon: 'user-add',
      label: 'label.dedicate.zone',
      dataView: true,
      args: ['zoneid', 'domainid', 'account'],
      show: (record) => { return !record.domainid }
    },
    {
      api: 'releaseDedicatedZone',
      icon: 'user-delete',
      label: 'label.release.dedicated.zone',
      dataView: true,
      args: ['zoneid'],
      show: (record) => { return record.domainid }
    },
    {
      api: 'enableOutOfBandManagementForZone',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      args: ['zoneid'],
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.outOfBandManagementEnabled ||
          record.resourcedetails.outOfBandManagementEnabled === 'false'
      }
    },
    {
      api: 'disableOutOfBandManagementForZone',
      icon: 'minus-circle',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      args: ['zoneid'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      }
    },
    {
      api: 'enableHAForZone',
      icon: 'eye',
      label: 'label.ha.enable',
      dataView: true,
      args: ['zoneid'],
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.resourceHAEnabled ||
          record.resourcedetails.resourceHAEnabled === 'false'
      }
    },
    {
      api: 'disableHAForZone',
      icon: 'eye-invisible',
      label: 'label.ha.disable',
      dataView: true,
      args: ['zoneid'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'true'
      }
    },
    {
      api: 'addVmwareDc',
      icon: 'block',
      label: 'label.add.vmware.datacenter',
      dataView: true,
      args: ['zoneid', 'name', 'vcenter', 'username', 'password'],
      show: (record) => { return !record.vmwaredcid }
    },
    {
      api: 'updateVmwareDc',
      icon: 'block',
      label: 'label.update.vmware.datacenter',
      dataView: true,
      args: ['zoneid', 'name', 'vcenter', 'username', 'password', 'isrecursive'],
      show: (record) => { return record.vmwaredcid }
    },
    {
      api: 'removeVmwareDc',
      icon: 'minus-square',
      label: 'label.remove.vmware.datacenter',
      dataView: true,
      args: ['zoneid'],
      show: (record) => { return record.vmwaredcid }
    },
    {
      api: 'deleteZone',
      icon: 'delete',
      label: 'label.action.delete.zone',
      dataView: true
    }
  ]
}
