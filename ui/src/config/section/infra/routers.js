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
  name: 'router',
  title: 'label.virtual.routers',
  icon: 'fork-outlined',
  docHelp: 'adminguide/systemvm.html#virtual-router',
  permission: ['listRouters'],
  params: { projectid: '-1' },
  columns: () => {
    var columns = ['name', 'state', 'publicip', 'guestnetworkname', 'vpcname', 'redundantstate', 'softwareversion', 'hostname', 'account', 'zonename', 'requiresupgrade']
    columns.splice(6, 0, { field: 'version', customTitle: 'templateversion' })
    return columns
  },
  searchFilters: ['name', 'zoneid', 'podid', 'clusterid'],
  details: ['name', 'id', 'version', 'softwareversion', 'requiresupgrade', 'guestnetworkname', 'vpcname', 'publicip', 'guestipaddress', 'linklocalip', 'serviceofferingname', 'networkdomain', 'isredundantrouter', 'redundantstate', 'hostname', 'account', 'zonename', 'created', 'hostcontrolstate'],
  resourceType: 'VirtualRouter',
  filters: () => {
    const filters = ['starting', 'running', 'stopping', 'stopped', 'destroyed', 'expunging', 'migrating', 'error', 'unknown', 'shutdown']
    return filters
  },
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }, {
    name: 'metrics',
    resourceType: 'DomainRouter',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/StatsTab.vue'))),
    show: () => { return store.getters.features.instancesstatsuseronly === false }
  }, {
    name: 'nics',
    component: shallowRef(defineAsyncComponent(() => import('@/views/network/NicsTable.vue')))
  }, {
    name: 'router.health.checks',
    show: (record, route, user) => { return ['Running'].includes(record.state) && ['Admin'].includes(user.roletype) },
    component: shallowRef(defineAsyncComponent(() => import('@views/infra/routers/RouterHealthCheck.vue')))
  }, {
    name: 'volume',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/VolumesTab.vue')))
  }, {
    name: 'events',
    resourceType: 'DomainRouter',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
    show: () => { return 'listEvents' in store.getters.apis }
  }, {
    name: 'comments',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
  }],
  related: [{
    name: 'vm',
    title: 'label.instances',
    param: 'networkid',
    value: 'guestnetworkid'
  }],
  actions: [
    {
      api: 'startRouter',
      icon: 'caret-right-outlined',
      label: 'label.action.start.router',
      message: 'message.action.start.router',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'stopRouter',
      icon: 'poweroff-outlined',
      label: 'label.action.stop.router',
      message: 'message.action.stop.router',
      dataView: true,
      args: ['forced'],
      show: (record) => { return record.state === 'Running' },
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
    },
    {
      api: 'rebootRouter',
      icon: 'sync-outlined',
      label: 'label.action.reboot.router',
      message: 'message.action.reboot.router',
      dataView: true,
      args: ['forced'],
      hidden: (record) => { return record.state === 'Running' },
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
    },
    {
      api: 'restartNetwork',
      icon: 'diff-outlined',
      label: 'label.action.patch.systemvm',
      message: 'message.action.patch.router',
      dataView: true,
      show: (record) => { return record.state === 'Running' && !('vpcid' in record) },
      mapping: {
        id: {
          value: (record) => { return record.guestnetworkid }
        },
        livepatch: {
          value: (record) => { return true }
        }
      },
      groupAction: true,
      popup: true,
      groupMap: (selection, values, record) => {
        return selection.map(x => {
          const data = record.filter(y => { return y.id === x })
          return {
            id: data[0].guestnetworkid, livepatch: true
          }
        })
      }
    },
    {
      api: 'restartVPC',
      icon: 'diff-outlined',
      label: 'label.action.patch.systemvm.vpc',
      message: 'message.action.patch.router',
      dataView: true,
      show: (record) => { return record.state === 'Running' && ('vpcid' in record) },
      mapping: {
        id: {
          value: (record) => { return record.vpcid }
        },
        livepatch: {
          value: (record) => { return true }
        }
      },
      groupAction: true,
      popup: true,
      groupMap: (selection, values, record) => {
        return selection.map(x => {
          const data = record.filter(y => { return y.id === x })
          return {
            id: data[0].vpcid, livepatch: true
          }
        })
      }
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt-outlined',
      label: 'label.change.service.offering',
      message: 'message.confirm.scale.up.router.vm',
      dataView: true,
      args: ['serviceofferingid'],
      show: (record) => { return record.hypervisor !== 'KVM' },
      mapping: {
        serviceofferingid: {
          api: 'listServiceOfferings',
          params: (record) => {
            return {
              virtualmachineid: record.id,
              issystem: true,
              systemvmtype: 'domainrouter'
            }
          }
        }
      }
    },
    {
      api: 'upgradeRouterTemplate',
      icon: 'fullscreen-outlined',
      label: 'label.upgrade.router.newer.template',
      message: 'message.confirm.upgrade.router.newer.template',
      docHelp: 'adminguide/systemvm.html#upgrading-virtual-routers',
      dataView: true,
      groupAction: true,
      // show: (record) => { return record.requiresupgrade },
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag-outlined',
      label: 'label.action.migrate.router',
      message: 'message.migrate.router.confirm',
      dataView: true,
      show: (record, store) => { return record.state === 'Running' && ['Admin'].includes(store.userInfo.roletype) },
      disabled: (record) => { return record.hostcontrolstate === 'Offline' },
      component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateWizard'))),
      popup: true
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag-outlined',
      label: 'label.action.migrate.systemvm.to.ps',
      dataView: true,
      show: (record, store) => { return ['Stopped'].includes(record.state) && ['VMware', 'KVM'].includes(record.hypervisor) },
      disabled: (record) => { return record.hostcontrolstate === 'Offline' },
      component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateVMStorage'))),
      popup: true
    },
    {
      api: 'runDiagnostics',
      icon: 'reconciliation-outlined',
      label: 'label.action.run.diagnostics',
      dataView: true,
      show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
      args: ['targetid', 'type', 'ipaddress', 'params'],
      mapping: {
        targetid: {
          value: (record) => { return record.id }
        },
        type: {
          options: ['ping', 'ping6', 'traceroute', 'traceroute6', 'arping']
        }
      },
      response: (result) => { return result && result.diagnostics ? `<strong>Output</strong>:<br/>${result.diagnostics.stdout}<br/><strong>Error</strong>: ${result.diagnostics.stderr}<br/><strong>Exit Code</strong>: ${result.diagnostics.exitcode}` : 'Invalid response' }
    },
    {
      api: 'getDiagnosticsData',
      icon: 'download-outlined',
      label: 'label.action.get.diagnostics',
      dataView: true,
      show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
      args: ['targetid', 'files'],
      mapping: {
        targetid: {
          value: (record) => { return record.id }
        }
      },
      response: (result) => { return result && result.diagnostics && result.diagnostics.url ? `Please click the link to download the retrieved diagnostics: <p><a href='${result.diagnostics.url}'>${result.diagnostics.url}</a></p>` : 'Invalid response' }
    },
    {
      api: 'destroyRouter',
      icon: 'delete-outlined',
      label: 'label.destroy.router',
      message: 'message.confirm.destroy.router',
      dataView: true,
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    }
  ]
}
