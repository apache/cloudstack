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
  name: 'zone',
  title: 'label.zones',
  icon: 'global-outlined',
  docHelp: 'conceptsandterminology/concepts.html#about-zones',
  permission: ['listZonesMetrics'],
  columns: () => {
    const fields = ['name', 'allocationstate', 'type', 'networktype', 'clusters']
    const metricsFields = ['cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    fields.push('order')
    return fields
  },
  details: ['name', 'id', 'allocationstate', 'type', 'networktype', 'guestcidraddress', 'localstorageenabled', 'securitygroupsenabled', 'dns1', 'dns2', 'internaldns1', 'internaldns2'],
  related: [{
    name: 'pod',
    title: 'label.pods',
    param: 'zoneid',
    show: (record) => {
      return record.type !== 'Edge'
    }
  }, {
    name: 'cluster',
    title: 'label.clusters',
    param: 'zoneid',
    show: (record) => {
      return record.type !== 'Edge'
    }
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
    param: 'zoneid',
    show: (record) => {
      return record.type !== 'Edge'
    }
  }],
  resourceType: 'Zone',
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }, {
    name: 'physical.network',
    component: shallowRef(defineAsyncComponent(() => import('@/views/infra/zone/PhysicalNetworksTab.vue')))
  }, {
    name: 'system.vms',
    component: shallowRef(defineAsyncComponent(() => import('@/views/infra/zone/SystemVmsTab.vue'))),
    show: (record) => { return record.isEdge !== true }
  }, {
    name: 'resources',
    component: shallowRef(defineAsyncComponent(() => import('@/views/infra/Resources.vue')))
  }, {
    name: 'settings',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/SettingsTab.vue')))
  }, {
    name: 'events',
    resourceType: 'Zone',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
    show: () => { return 'listEvents' in store.getters.apis }
  }, {
    name: 'comments',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
  }],
  actions: [
    {
      api: 'createZone',
      icon: 'plus-outlined',
      label: 'label.add.zone',
      docHelp: 'installguide/configuration.html#adding-a-zone',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/zone/ZoneWizard.vue')))
    },
    {
      api: 'updateZone',
      icon: 'edit-outlined',
      label: 'label.action.edit.zone',
      dataView: true,
      args: ['name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'guestcidraddress', 'domain', 'localstorageenabled'],
      show: (record) => { return record.networktype === 'Advanced' }
    },
    {
      api: 'updateZone',
      icon: 'edit-outlined',
      label: 'label.action.edit.zone',
      dataView: true,
      args: ['name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'domain', 'localstorageenabled'],
      show: (record) => { return record.networktype === 'Basic' }
    },
    {
      api: 'updateZone',
      icon: 'pause-circle-outlined',
      label: 'label.action.disable.zone',
      message: 'message.action.disable.zone',
      docHelp: 'adminguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      defaultArgs: { allocationstate: 'Disabled' },
      show: (record) => { return record.allocationstate === 'Enabled' }
    },
    {
      api: 'updateZone',
      icon: 'play-circle-outlined',
      label: 'label.action.enable.zone',
      message: 'message.action.enable.zone',
      docHelp: 'adminguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      defaultArgs: { allocationstate: 'Enabled' },
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'enableOutOfBandManagementForZone',
      icon: 'plus-circle-outlined',
      label: 'label.outofbandmanagement.enable',
      message: 'label.outofbandmanagement.enable',
      dataView: true,
      show: (record) => {
        return record?.resourcedetails?.outOfBandManagementEnabled === 'false'
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
      icon: 'minus-circle-outlined',
      label: 'label.outofbandmanagement.disable',
      message: 'label.outofbandmanagement.disable',
      dataView: true,
      show: (record) => {
        return !(record?.resourcedetails?.outOfBandManagementEnabled === 'false')
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
      icon: 'eye-outlined',
      label: 'label.ha.enable',
      message: 'label.ha.enable',
      dataView: true,
      show: (record) => {
        return record?.resourcedetails?.resourceHAEnabled === 'false'
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
      icon: 'eye-invisible-outlined',
      label: 'label.ha.disable',
      message: 'label.ha.disable',
      dataView: true,
      show: (record) => {
        return !(record?.resourcedetails?.resourceHAEnabled === 'false')
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
      icon: 'block-outlined',
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
      icon: 'block-outlined',
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
      icon: 'minus-square-outlined',
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
      icon: 'setting-outlined',
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
      icon: 'delete-outlined',
      label: 'label.action.delete.zone',
      message: 'message.action.delete.zone',
      dataView: true
    }
  ]
}
