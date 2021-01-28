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
  name: 'zone',
  title: 'label.zones',
  icon: 'global',
  permission: ['listZonesMetrics'],
  columns: () => {
    const fields = ['name', 'allocationstate', 'networktype', 'clusters']
    const metricsFields = ['cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    fields.push('order')
    return fields
  },
  details: ['name', 'id', 'allocationstate', 'networktype', 'guestcidraddress', 'localstorageenabled', 'securitygroupsenabled', 'dns1', 'dns2', 'internaldns1', 'internaldns2'],
  related: [{
    name: 'pod',
    title: 'label.pods',
    param: 'zoneid'
  }, {
    name: 'cluster',
    title: 'label.clusters',
    param: 'zoneid'
  }, {
    name: 'host',
    title: 'label.hosts',
    param: 'zoneid'
  }, {
    name: 'storagepool',
    title: 'label.primary.storage',
    param: 'zoneid'
  }, {
    name: 'imagestore',
    title: 'label.secondary.storage',
    param: 'zoneid'
  }],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'physical.network',
    component: () => import('@/views/infra/zone/PhysicalNetworksTab.vue')
  }, {
    name: 'system.vms',
    component: () => import('@/views/infra/zone/SystemVmsTab.vue')
  }, {
    name: 'resources',
    component: () => import('@/views/infra/Resources.vue')
  }, {
    name: 'settings',
    component: () => import('@/components/view/SettingsTab.vue')
  }],
  actions: [
    {
      api: 'createZone',
      icon: 'plus',
      label: 'label.add.zone',
      docHelp: 'installguide/configuration.html#adding-a-zone',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/zone/ZoneWizard.vue')
    },
    {
      api: 'updateZone',
      icon: 'edit',
      label: 'label.action.edit.zone',
      dataView: true,
      args: ['name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'guestcidraddress', 'domain', 'localstorageenabled'],
      show: (record) => { return record.networktype === 'Advanced' }
    },
    {
      api: 'updateZone',
      icon: 'edit',
      label: 'label.action.edit.zone',
      dataView: true,
      args: ['name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'domain', 'localstorageenabled'],
      show: (record) => { return record.networktype === 'Basic' }
    },
    {
      api: 'updateZone',
      icon: 'pause-circle',
      label: 'label.action.disable.zone',
      message: 'message.action.disable.zone',
      docHelp: 'adminguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      defaultArgs: { allocationstate: 'Disabled' },
      show: (record) => { return record.allocationstate === 'Enabled' }
    },
    {
      api: 'updateZone',
      icon: 'play-circle',
      label: 'label.action.enable.zone',
      message: 'message.action.enable.zone',
      docHelp: 'adminguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      defaultArgs: { allocationstate: 'Enabled' },
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'enableOutOfBandManagementForZone',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      message: 'label.outofbandmanagement.enable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
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
      message: 'label.outofbandmanagement.disable',
      dataView: true,
      show: (record) => {
        return !(record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'false')
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
      message: 'label.ha.enable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
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
      message: 'label.ha.disable',
      dataView: true,
      show: (record) => {
        return !(record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'false')
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
      message: 'message.restart.mgmt.server',
      additionalMessage: 'message.restart.mgmt.server',
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
      message: 'message.confirm.remove.vmware.datacenter',
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
      api: 'startRollingMaintenance',
      icon: 'setting',
      label: 'label.start.rolling.maintenance',
      message: 'label.start.rolling.maintenance',
      dataView: true,
      args: ['timeout', 'payload', 'forced', 'zoneids'],
      mapping: {
        zoneids: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'deleteZone',
      icon: 'delete',
      label: 'label.action.delete.zone',
      message: 'message.action.delete.zone',
      dataView: true
    }
  ]
}
