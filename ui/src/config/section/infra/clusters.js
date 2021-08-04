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
  name: 'cluster',
  title: 'label.clusters',
  icon: 'cluster',
  permission: ['listClustersMetrics'],
  columns: () => {
    const fields = ['name', 'state', 'allocationstate', 'clustertype', 'hypervisortype', 'hosts']
    const metricsFields = ['cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    fields.push('podname')
    fields.push('zonename')
    return fields
  },
  details: ['name', 'id', 'allocationstate', 'clustertype', 'managedstate', 'hypervisortype', 'podname', 'zonename'],
  related: [{
    name: 'host',
    title: 'label.hosts',
    param: 'clusterid'
  }],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'resources',
    component: () => import('@/views/infra/Resources.vue')
  }, {
    name: 'settings',
    component: () => import('@/components/view/SettingsTab.vue')
  }],
  actions: [
    {
      api: 'addCluster',
      icon: 'plus',
      label: 'label.add.cluster',
      docHelp: 'adminguide/installguide/configuration.html#adding-a-cluster',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/ClusterAdd.vue')
    },
    {
      api: 'updateCluster',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: ['clustername']
    },
    {
      api: 'updateCluster',
      icon: 'play-circle',
      label: 'label.action.enable.cluster',
      message: 'message.action.enable.cluster',
      docHelp: 'adminguide/installguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      defaultArgs: { allocationstate: 'Enabled' },
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'updateCluster',
      icon: 'pause-circle',
      label: 'label.action.disable.cluster',
      message: 'message.action.disable.cluster',
      docHelp: 'adminguide/installguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      defaultArgs: { allocationstate: 'Disabled' },
      show: (record) => { return record.allocationstate === 'Enabled' }
    },
    {
      api: 'updateCluster',
      icon: 'plus-square',
      label: 'label.action.manage.cluster',
      message: 'message.action.manage.cluster',
      dataView: true,
      defaultArgs: { managedstate: 'Managed' },
      show: (record) => { return record.managedstate !== 'Managed' }
    },
    {
      api: 'updateCluster',
      icon: 'minus-square',
      label: 'label.action.unmanage.cluster',
      message: 'message.action.unmanage.cluster',
      dataView: true,
      defaultArgs: { managedstate: 'Unmanaged' },
      show: (record) => { return record.managedstate === 'Managed' }
    },
    {
      api: 'enableOutOfBandManagementForCluster',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      message: 'label.outofbandmanagement.enable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
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
      message: 'label.outofbandmanagement.disable',
      dataView: true,
      show: (record) => {
        return !(record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'false')
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
      message: 'label.ha.enable',
      dataView: true,
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
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
      message: 'label.ha.disable',
      dataView: true,
      show: (record) => {
        return !(record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'false')
      },
      args: ['clusterid'],
      mapping: {
        clusterid: {
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
      args: ['timeout', 'payload', 'forced', 'clusterids'],
      mapping: {
        clusterids: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'deleteCluster',
      icon: 'delete',
      label: 'label.action.delete.cluster',
      message: 'message.action.delete.cluster',
      dataView: true
    }
  ]
}
