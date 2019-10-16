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
  name: 'host',
  title: 'Hosts',
  icon: 'desktop',
  permission: [ 'listHostsMetrics', 'listHosts' ],
  resourceType: 'Host',
  params: { 'type': 'routing' },
  columns: [ 'name', 'state', 'resourcestate', 'powerstate', 'ipaddress', 'hypervisor', 'instances', 'cpunumber', 'cputotalghz', 'cpuusedghz', 'cpuallocatedghz', 'memorytotalgb', 'memoryusedgb', 'memoryallocatedgb', 'networkread', 'networkwrite', 'clustername', 'zonename' ],
  details: [ 'name', 'id', 'resourcestate', 'ipaddress', 'hypervisor', 'hypervisorversion', 'version', 'type', 'oscategoryname', 'hosttags', 'clustername', 'podname', 'zonename', 'created' ],
  actions: [
    {
      api: 'addHost',
      icon: 'plus',
      label: 'label.add.host',
      listView: true,
      args: ['zoneid', 'podid', 'clusterid', 'hypervisor', 'username', 'password', 'url', 'hosttags']
    },
    {
      api: 'updateHost',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: ['id', 'hosttags', 'oscategoryid']
    },
    {
      api: 'provisionCertificate',
      icon: 'safety-certificate',
      label: 'label.action.secure.host',
      dataView: true,
      args: ['hostid'],
      show: (record) => { return record.hypervisor === 'KVM' }
    },
    {
      api: 'reconnectHost',
      icon: 'forward',
      label: 'label.action.force.reconnect',
      dataView: true,
      args: ['id'],
      show: (record) => { return ['Disconnected', 'Up'].includes(record.state) }
    },
    {
      api: 'updateHost',
      icon: 'pause-circle',
      label: 'Disable Host',
      dataView: true,
      args: ['id'],
      defaultArgs: { allocationstate: 'Disable' },
      show: (record) => { return record.resourcestate === 'Enabled' }
    },
    {
      api: 'updateHost',
      icon: 'play-circle',
      label: 'Enable Host',
      dataView: true,
      args: [ 'id' ],
      defaultArgs: { allocationstate: 'Enable' },
      show: (record) => { return record.resourcestate === 'Disabled' }
    },
    {
      api: 'dedicateHost',
      icon: 'user-add',
      label: 'label.dedicate.host',
      dataView: true,
      args: ['hostid', 'domainid', 'account'],
      show: (record) => { return !record.domainid }
    },
    {
      api: 'releaseDedicatedHost',
      icon: 'user-delete',
      label: 'label.release.dedicated.host',
      dataView: true,
      args: ['hostid'],
      show: (record) => { return record.domainid }
    },
    {
      api: 'prepareHostForMaintenance',
      icon: 'plus-square',
      label: 'label.action.enable.maintenance.mode',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.resourcestate === 'Enabled' }
    },
    {
      api: 'cancelHostMaintenance',
      icon: 'minus-square',
      label: 'label.action.cancel.maintenance.mode',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.resourcestate === 'Maintenance' || record.resourcestate === 'ErrorInMaintenance' || record.resourcestate === 'PrepareForMaintenance' }
    },
    {
      api: 'configureOutOfBandManagement',
      icon: 'setting',
      label: 'label.outofbandmanagement.configure',
      dataView: true,
      args: ['hostid', 'address', 'port', 'username', 'password', 'driver']
    },
    {
      api: 'enableOutOfBandManagementForHost',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      args: ['hostid'],
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.outOfBandManagementEnabled ||
          record.resourcedetails.outOfBandManagementEnabled === 'false'
      }
    },
    {
      api: 'disableOutOfBandManagementForHost',
      icon: 'minus-circle',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      args: ['hostid'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      }
    },
    {
      api: 'issueOutOfBandManagementPowerAction',
      icon: 'login',
      label: 'label.outofbandmanagement.action.issue',
      dataView: true,
      args: ['hostid', 'action'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      }
    },
    {
      api: 'changeOutOfBandManagementPassword',
      icon: 'key',
      label: 'label.outofbandmanagement.changepassword',
      dataView: true,
      args: ['hostid', 'password'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      }
    },
    {
      api: 'configureHAForHost',
      icon: 'tool',
      label: 'label.ha.configure',
      dataView: true,
      args: ['hostid', 'provider']
    },
    {
      api: 'enableHAForHost',
      icon: 'eye',
      label: 'label.ha.enable',
      dataView: true,
      args: ['hostid'],
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.resourceHAEnabled ||
          record.resourcedetails.resourceHAEnabled === 'false'
      }
    },
    {
      api: 'disableHAForHost',
      icon: 'eye-invisible',
      label: 'label.ha.disable',
      dataView: true,
      args: ['hostid'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'true'
      }
    },
    {
      api: 'deleteHost',
      icon: 'delete',
      label: 'Remove Host',
      dataView: true,
      args: ['id', 'forced'],
      show: (record) => { return ['Maintenance', 'Disabled', 'Down', 'Alert', 'Disconnected'].includes(record.resourcestate) }
    }
  ]
}
