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
  name: 'systemvm',
  title: 'label.system.vms',
  icon: 'thunderbolt',
  docHelp: 'adminguide/systemvm.html',
  permission: ['listSystemVms'],
  columns: ['name', 'state', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'hostname', 'zonename'],
  details: ['name', 'id', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'gateway', 'hostname', 'zonename', 'created', 'activeviewersessions', 'isdynamicallyscalable'],
  actions: [
    {
      api: 'startSystemVm',
      icon: 'caret-right',
      label: 'label.action.start.systemvm',
      message: 'message.action.start.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' }
    },
    {
      api: 'stopSystemVm',
      icon: 'poweroff',
      label: 'label.action.stop.systemvm',
      message: 'message.action.stop.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Running' },
      args: ['forced']
    },
    {
      api: 'rebootSystemVm',
      icon: 'sync',
      label: 'label.action.reboot.systemvm',
      message: 'message.action.reboot.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Running' },
      args: ['forced']
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt',
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
      icon: 'drag',
      label: 'label.action.migrate.systemvm',
      message: 'message.migrate.systemvm.confirm',
      dataView: true,
      show: (record, store) => { return record.state === 'Running' && ['Admin'].includes(store.userInfo.roletype) },
      component: () => import('@/views/compute/MigrateWizard'),
      popup: true
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag',
      label: 'label.action.migrate.systemvm.to.ps',
      dataView: true,
      show: (record, store) => { return ['Stopped'].includes(record.state) && ['VMware'].includes(record.hypervisor) },
      component: () => import('@/views/compute/MigrateVMStorage'),
      popup: true
    },
    {
      api: 'runDiagnostics',
      icon: 'reconciliation',
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
      icon: 'download',
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
      api: 'destroySystemVm',
      icon: 'delete',
      label: 'label.action.destroy.systemvm',
      message: 'message.action.destroy.systemvm',
      dataView: true,
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) }
    }
  ]
}
