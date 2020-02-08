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
  name: 'ilbvm',
  title: 'Internal LB VMs',
  icon: 'share-alt',
  permission: ['listInternalLoadBalancerVMs'],
  columns: ['name', 'state', 'publicip', 'guestnetworkname', 'vpcname', 'version', 'hostname', 'account', 'zonename', 'requiresupgrade'],
  details: ['name', 'id', 'version', 'requiresupgrade', 'guestnetworkname', 'vpcname', 'publicip', 'guestipaddress', 'linklocalip', 'serviceofferingname', 'networkdomain', 'isredundantrouter', 'redundantstate', 'hostname', 'account', 'zonename', 'created'],
  actions: [
    {
      api: 'startInternalLoadBalancerVM',
      icon: 'caret-right',
      label: 'label.action.start.router',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' }
    },
    {
      api: 'stopInternalLoadBalancerVM',
      icon: 'stop',
      label: 'label.action.stop.router',
      dataView: true,
      args: ['forced'],
      show: (record) => { return record.state === 'Running' }
    }
  ]
}
