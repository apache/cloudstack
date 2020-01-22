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
  name: 'cluster',
  title: 'Clusters',
  icon: 'cluster',
  permission: ['listClustersMetrics', 'listClusters'],
  columns: ['name', 'state', 'clustertype', 'hypervisortype', 'hosts', 'cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal', 'podname', 'zonename'],
  details: ['name', 'id', 'allocationstate', 'clustertype', 'hypervisortype', 'podname', 'zonename'],
  related: [{
    name: 'host',
    title: 'Hosts',
    param: 'clusterid'
  }],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'Settings',
    component: () => import('@/components/view/SettingsTab.vue')
  }],
  actions: [
    {
      api: 'addCluster',
      icon: 'plus',
      label: 'label.add.cluster',
      listView: true,
      args: ['zoneid', 'hypervisor', 'podid', 'clustername']
    },
    {
      api: 'updateCluster',
      icon: 'play-circle',
      label: 'label.action.enable.cluster',
      dataView: true,
      defaultArgs: { allocationstate: 'Enabled' },
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'updateCluster',
      icon: 'pause-circle',
      label: 'label.action.disable.cluster',
      dataView: true,
      defaultArgs: { allocationstate: 'Disabled' },
      show: (record) => { return record.allocationstate === 'Enabled' }
    },
    {
      api: 'updateCluster',
      icon: 'plus-square',
      label: 'Manage Cluster',
      dataView: true,
      defaultArgs: { managedstate: 'Managed' },
      show: (record) => { return record.clustertype === 'CloudManaged' && ['PrepareUnmanaged', 'Unmanaged'].includes(record.state) }
    },
    {
      api: 'updateCluster',
      icon: 'minus-square',
      label: 'Unmanage Cluster',
      dataView: true,
      defaultArgs: { managedstate: 'Unmanaged' },
      show: (record) => { return record.clustertype === 'CloudManaged' && record.state === 'Enabled' }
    },
    {
      api: 'enableOutOfBandManagementForCluster',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.outOfBandManagementEnabled ||
          record.resourcedetails.outOfBandManagementEnabled === 'false'
      },
      args: ['clusterid'],
      mapping: {
        clusterid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'disableOutOfBandManagementForCluster',
      icon: 'minus-circle',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      },
      args: ['clusterid'],
      mapping: {
        clusterid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'enableHAForCluster',
      icon: 'eye',
      label: 'label.ha.enable',
      dataView: true,
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.resourceHAEnabled ||
          record.resourcedetails.resourceHAEnabled === 'false'
      },
      args: ['clusterid'],
      mapping: {
        clusterid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'disableHAForCluster',
      icon: 'eye-invisible',
      label: 'label.ha.disable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'true'
      },
      args: ['clusterid'],
      mapping: {
        clusterid: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'deleteCluster',
      icon: 'delete',
      label: 'label.action.delete.cluster',
      dataView: true
    }
  ]
}
