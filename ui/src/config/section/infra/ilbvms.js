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
export default {
  name: 'ilbvm',
  title: 'label.internal.lb',
  icon: 'share-alt-outlined',
  docHelp: 'adminguide/networking_and_traffic.html#creating-an-internal-lb-rule',
  permission: ['listInternalLoadBalancerVMs'],
  params: { projectid: '-1' },
  columns: ['name', 'state', 'publicip', 'guestnetworkname', 'vpcname', 'version', 'softwareversion', 'hostname', 'account', 'zonename', 'requiresupgrade'],
  details: ['name', 'id', 'version', 'softwareversion', 'requiresupgrade', 'guestnetworkname', 'vpcname', 'publicip', 'guestipaddress', 'linklocalip', 'serviceofferingname', 'networkdomain', 'isredundantrouter', 'redundantstate', 'hostname', 'account', 'zonename', 'created', 'hostcontrolstate'],
  actions: [
    {
      api: 'startInternalLoadBalancerVM',
      icon: 'caret-right-outlined',
      label: 'label.action.start.router',
      message: 'message.confirm.start.lb.vm',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'stopInternalLoadBalancerVM',
      icon: 'poweroff-outlined',
      label: 'label.action.stop.router',
      dataView: true,
      args: ['forced'],
      show: (record) => { return record.state === 'Running' },
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag-outlined',
      label: 'label.action.migrate.router',
      dataView: true,
      show: (record, store) => { return record.state === 'Running' && ['Admin'].includes(store.userInfo.roletype) },
      disabled: (record) => { return record.hostcontrolstate === 'Offline' },
      component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateWizard'))),
      popup: true
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag',
      label: 'label.action.migrate.systemvm.to.ps',
      dataView: true,
      show: (record, store) => { return ['Stopped'].includes(record.state) && ['VMware'].includes(record.hypervisor) },
      disabled: (record) => { return record.hostcontrolstate === 'Offline' },
      component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateVMStorage'))),
      popup: true
    }
  ]
}
