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
  name: 'pod',
  title: 'Pods',
  icon: 'appstore',
  permission: ['listPods'],
  columns: ['name', 'allocationstate', 'gateway', 'netmask', 'zonename'],
  details: ['name', 'id', 'allocationstate', 'netmask', 'gateway', 'zonename'],
  actions: [
    {
      api: 'createPod',
      icon: 'plus',
      label: 'label.add.pod',
      listView: true,
      args: ['zoneid', 'name', 'gateway', 'netmask', 'startip', 'endip']
    },
    {
      api: 'updatePod',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: ['id', 'name', 'netmask', 'gateway']
    },
    {
      api: 'dedicatePod',
      icon: 'user-add',
      label: 'label.dedicate.pod',
      dataView: true,
      args: ['podid', 'domainid', 'account'],
      show: (record) => { return !record.domainid }
    },
    {
      api: 'releaseDedicatedPod',
      icon: 'user-delete',
      label: 'label.release.dedicated.pod',
      dataView: true,
      args: ['podid'],
      show: (record) => { return record.domainid }
    },
    {
      api: 'updatePod',
      icon: 'play-circle',
      label: 'label.action.enable.pod',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'updatePod',
      icon: 'pause-circle',
      label: 'label.action.disable.pod',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.allocationstate === 'Enabled' },
      defaultArgs: { allocationstate: 'Disabled' }
    },
    {
      api: 'deletePod',
      icon: 'delete',
      label: 'label.action.delete.pod',
      dataView: true,
      args: ['id']
    }
  ]
}
