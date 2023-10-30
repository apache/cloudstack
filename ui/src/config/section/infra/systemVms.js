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
  name: 'systemvm',
  title: 'label.system.vms',
  icon: 'thunderbolt-outlined',
  docHelp: 'adminguide/systemvm.html',
  permission: ['listSystemVms'],
  columns: ['name', 'state', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'hostname', 'zonename'],
  details: ['name', 'id', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'gateway', 'hostname', 'zonename', 'created', 'activeviewersessions', 'isdynamicallyscalable', 'hostcontrolstate'],
  resourceType: 'SystemVm',
  filters: () => {
    const filters = ['starting', 'running', 'stopping', 'stopped', 'destroyed', 'expunging', 'migrating', 'error', 'unknown', 'shutdown']
    return filters
  },
  tabs: [
    {
      name: 'details',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
    },
    {
      name: 'metrics',
      resourceType: 'SystemVm',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/StatsTab.vue'))),
      show: () => { return store.getters.features.instancesstatsuseronly === false }
    },
    {
      name: 'volume',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/VolumesTab.vue')))
    },
    {
      name: 'events',
      resourceType: 'SystemVm',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
      show: () => { return 'listEvents' in store.getters.apis }
    },
    {
      name: 'comments',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
    }
  ],
  actions: [
    {
      api: 'startSystemVm',
      icon: 'caret-right-outlined',
      label: 'label.action.start.systemvm',
      message: 'message.action.start.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'stopSystemVm',
      icon: 'poweroff-outlined',
      label: 'label.action.stop.systemvm',
      message: 'message.action.stop.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Running' },
      args: ['forced'],
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
    },
    {
      api: 'rebootSystemVm',
      icon: 'sync-outlined',
      label: 'label.action.reboot.systemvm',
      message: 'message.action.reboot.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Running' },
      args: ['forced'],
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt-outlined',
      label: 'label.change.service.offering',
      message: 'message.confirm.scale.up.system.vm',
      dataView: true,
      show: (record) => { return record.state === 'Running' && record.hypervisor === 'VMware' || record.state === 'Stopped' },
      args: ['serviceofferingid'],
      mapping: {
        serviceofferingid: {
          api: 'listServiceOfferings',
          params: (record) => { return { virtualmachineid: record.id, issystem: true, systemvmtype: record.systemvmtype } }
        }
      }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag-outlined',
      label: 'label.action.migrate.systemvm',
      message: 'message.migrate.systemvm.confirm',
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
      show: (record) => { return record.state === 'Running' },
      args: ['targetid', 'type', 'ipaddress', 'params'],
      mapping: {
        targetid: {
          value: (record) => { return record.id }
        },
        type: {
          options: ['ping', 'traceroute', 'arping']
        }
      },
      response: (result) => { return result && result.diagnostics ? `<strong>Output</strong>:<br/>${result.diagnostics.stdout}<br/><strong>Error</strong>: ${result.diagnostics.stderr}<br/><strong>Exit Code</strong>: ${result.diagnostics.exitcode}` : 'Invalid response' }
    },
    {
      api: 'getDiagnosticsData',
      icon: 'download-outlined',
      label: 'label.action.get.diagnostics',
      dataView: true,
      show: (record) => { return record.state === 'Running' },
      args: ['targetid', 'files'],
      mapping: {
        targetid: {
          value: (record) => { return record.id }
        }
      },
      response: (result) => { return result && result.diagnostics && result.diagnostics.url ? `Please click the link to download the retrieved diagnostics: <p><a href='${result.diagnostics.url}'>${result.diagnostics.url}</a></p>` : 'Invalid response' }
    },
    {
      api: 'patchSystemVm',
      icon: 'diff-outlined',
      label: 'label.action.patch.systemvm',
      message: 'message.action.patch.systemvm',
      dataView: true,
      show: (record) => { return ['Running'].includes(record.state) },
      args: ['forced'],
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'destroySystemVm',
      icon: 'delete-outlined',
      label: 'label.action.destroy.systemvm',
      message: 'message.action.destroy.systemvm',
      dataView: true,
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    }
  ]
}
