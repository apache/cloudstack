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
  title: 'System VMs',
  icon: 'thunderbolt',
  permission: ['listSystemVms'],
  columns: ['name', 'state', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'hostname', 'zonename'],
  details: ['name', 'id', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'gateway', 'hostname', 'zonename', 'created', 'activeviewersessions'],
  actions: [
    {
      api: 'startSystemVm',
      icon: 'caret-right',
      label: 'label.action.start.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' }
    },
    {
      api: 'stopSystemVm',
      icon: 'stop',
      label: 'label.action.stop.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'rebootSystemVm',
      icon: 'sync',
      label: 'label.action.reboot.systemvm',
      dataView: true,
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt',
      label: 'label.change.service.offering',
      dataView: true,
      args: ['serviceofferingid'],
      show: (record) => { return record.hypervisor !== 'KVM' }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag',
      label: 'label.action.migrate.systemvm',
      dataView: true,
      args: ['virtualmachineid', 'hostid'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'runDiagnostics',
      icon: 'reconciliation',
      label: 'label.action.run.diagnostics',
      dataView: true,
      args: ['targetid', 'type', 'ipaddress', 'params'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'destroySystemVm',
      icon: 'delete',
      label: 'label.action.destroy.systemvm',
      dataView: true,
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) }
    }
  ]
}
