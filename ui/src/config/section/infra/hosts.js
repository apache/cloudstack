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
  name: 'host',
  title: 'label.hosts',
  icon: 'desktop',
  permission: ['listHostsMetrics'],
  resourceType: 'Host',
  params: { type: 'routing' },
  columns: () => {
    const fields = ['name', 'state', 'resourcestate', 'ipaddress', 'hypervisor', 'instances', 'powerstate']
    const metricsFields = ['cpunumber', 'cputotalghz', 'cpuusedghz', 'cpuallocatedghz', 'memorytotalgb', 'memoryusedgb', 'memoryallocatedgb', 'networkread', 'networkwrite']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    fields.push('clustername')
    fields.push('zonename')
    return fields
  },
  details: ['name', 'id', 'resourcestate', 'ipaddress', 'hypervisor', 'type', 'clustername', 'podname', 'zonename', 'disconnected', 'created'],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }],
  related: [{
    name: 'vm',
    title: 'label.instances',
    param: 'hostid'
  }],
  actions: [
    {
      api: 'addHost',
      icon: 'plus',
      label: 'label.add.host',
      docHelp: 'adminguide/installguide/configuration.html#adding-a-host',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/HostAdd.vue')
    },
    {
      api: 'updateHost',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: ['name', 'hosttags', 'oscategoryid'],
      mapping: {
        oscategoryid: {
          api: 'listOsCategories'
        }
      }
    },
    {
      api: 'provisionCertificate',
      icon: 'safety-certificate',
      label: 'label.action.secure.host',
      message: 'message.action.secure.host',
      dataView: true,
      show: (record) => { return record.hypervisor === 'KVM' },
      args: ['hostid'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'reconnectHost',
      icon: 'forward',
      label: 'label.action.force.reconnect',
      message: 'message.confirm.action.force.reconnect',
      dataView: true,
      show: (record) => { return ['Disconnected', 'Up'].includes(record.state) }
    },
    {
      api: 'updateHost',
      icon: 'pause-circle',
      label: 'label.disable.host',
      message: 'message.confirm.disable.host',
      dataView: true,
      defaultArgs: { allocationstate: 'Disable' },
      show: (record) => { return record.resourcestate === 'Enabled' }
    },
    {
      api: 'updateHost',
      icon: 'play-circle',
      label: 'label.enable.host',
      message: 'message.confirm.enable.host',
      dataView: true,
      defaultArgs: { allocationstate: 'Enable' },
      show: (record) => { return record.resourcestate === 'Disabled' }
    },
    {
      api: 'prepareHostForMaintenance',
      icon: 'plus-square',
      label: 'label.action.enable.maintenance.mode',
      message: 'message.action.host.enable.maintenance.mode',
      docHelp: 'adminguide/hosts.html#maintaining-hypervisors-on-hosts',
      dataView: true,
      show: (record) => { return record.resourcestate === 'Enabled' }
    },
    {
      api: 'cancelHostMaintenance',
      icon: 'minus-square',
      label: 'label.action.cancel.maintenance.mode',
      message: 'message.action.cancel.maintenance.mode',
      docHelp: 'adminguide/hosts.html#maintaining-hypervisors-on-hosts',
      dataView: true,
      show: (record) => { return record.resourcestate === 'Maintenance' || record.resourcestate === 'ErrorInMaintenance' || record.resourcestate === 'PrepareForMaintenance' }
    },
    {
      api: 'configureOutOfBandManagement',
      icon: 'setting',
      label: 'label.outofbandmanagement.configure',
      message: 'label.outofbandmanagement.configure',
      docHelp: 'adminguide/hosts.html#out-of-band-management',
      dataView: true,
      args: ['hostid', 'address', 'port', 'username', 'password', 'driver'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        },
        driver: {
          options: ['ipmitool', 'nestedcloudstack', 'redfish']
        }
      }
    },
    {
      api: 'enableOutOfBandManagementForHost',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      message: 'label.outofbandmanagement.enable',
      docHelp: 'adminguide/hosts.html#out-of-band-management',
      dataView: true,
      show: (record) => {
        return !(record.outofbandmanagement && record.outofbandmanagement.enabled &&
          record.outofbandmanagement.enabled === true)
      },
      args: ['hostid'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'disableOutOfBandManagementForHost',
      icon: 'minus-circle',
      label: 'label.outofbandmanagement.disable',
      message: 'label.outofbandmanagement.disable',
      docHelp: 'adminguide/hosts.html#out-of-band-management',
      dataView: true,
      show: (record) => {
        return record.outofbandmanagement && record.outofbandmanagement.enabled &&
          record.outofbandmanagement.enabled === true
      },
      args: ['hostid'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'issueOutOfBandManagementPowerAction',
      icon: 'login',
      label: 'label.outofbandmanagement.action.issue',
      message: 'label.outofbandmanagement.action.issue',
      docHelp: 'adminguide/hosts.html#out-of-band-management',
      dataView: true,
      show: (record) => {
        return record.outofbandmanagement && record.outofbandmanagement.enabled &&
          record.outofbandmanagement.enabled === true
      },
      args: ['hostid', 'action'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        },
        action: {
          options: ['ON', 'OFF', 'CYCLE', 'RESET', 'SOFT', 'STATUS']
        }
      }
    },
    {
      api: 'changeOutOfBandManagementPassword',
      icon: 'key',
      label: 'label.outofbandmanagement.changepassword',
      message: 'label.outofbandmanagement.changepassword',
      docHelp: 'adminguide/hosts.html#out-of-band-management',
      dataView: true,
      show: (record) => {
        return record.outofbandmanagement && record.outofbandmanagement.enabled &&
          record.outofbandmanagement.enabled === true
      },
      args: ['hostid', 'password'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'configureHAForHost',
      icon: 'tool',
      label: 'label.ha.configure',
      message: 'label.ha.configure',
      docHelp: 'adminguide/reliability.html#ha-for-hosts',
      dataView: true,
      show: (record) => { return ['KVM', 'Simulator'].includes(record.hypervisor) },
      args: ['hostid', 'provider'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        },
        provider: {
          options: ['KVMHAProvider']
        }
      }
    },
    {
      api: 'enableHAForHost',
      icon: 'eye',
      label: 'label.ha.enable',
      message: 'label.ha.enable',
      docHelp: 'adminguide/reliability.html#ha-for-hosts',
      dataView: true,
      show: (record) => {
        return !(record.hostha && record.hostha.haenable &&
          record.hostha.haenable === true)
      },
      args: ['hostid'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'disableHAForHost',
      icon: 'eye-invisible',
      label: 'label.ha.disable',
      message: 'label.ha.disable',
      docHelp: 'adminguide/reliability.html#ha-for-hosts',
      dataView: true,
      show: (record) => {
        return record.hostha && record.hostha.haenable &&
        record.hostha.haenable === true
      },
      args: ['hostid'],
      mapping: {
        hostid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'startRollingMaintenance',
      icon: 'setting',
      label: 'label.start.rolling.maintenance',
      message: 'label.start.rolling.maintenance',
      docHelp: 'adminguide/hosts.html#kvm-rolling-maintenance',
      dataView: true,
      show: (record) => {
        return record.hypervisor === 'KVM' && (record.resourcestate === 'Enabled' || record.resourcestate === 'ErrorInMaintenance')
      },
      args: ['timeout', 'payload', 'forced', 'hostids'],
      mapping: {
        hostids: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'deleteHost',
      icon: 'delete',
      label: 'label.action.remove.host',
      docHelp: 'adminguide/hosts.html#removing-hosts',
      dataView: true,
      args: ['forced'],
      show: (record) => { return ['Maintenance', 'Disabled', 'Down', 'Alert', 'Disconnected'].includes(record.resourcestate) }
    }
  ]
}
