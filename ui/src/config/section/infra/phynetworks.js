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
  name: 'physicalnetwork',
  title: 'Physical Networks',
  icon: 'api',
  hidden: true,
  permission: ['listPhysicalNetworks'],
  columns: ['name', 'state', 'isolationmethods', 'vlan', 'broadcastdomainrange', 'zoneid'],
  details: ['name', 'state', 'isolationmethods', 'vlan', 'broadcastdomainrange', 'zoneid'],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'Network',
    component: () => import('@/views/infra/network/NetworkTab.vue')
  }, {
    name: 'Dedicated VLAN/VNI Ranges',
    component: () => import('@/views/infra/network/DedicatedVLANTab.vue')
  }],
  related: [{
    name: 'guestnetwork',
    title: 'Networks',
    param: 'physicalnetworkid'
  }],
  actions: [
    {
      api: 'createPhysicalNetwork',
      icon: 'plus',
      label: 'Create Physical Network',
      listView: true,
      args: ['name', 'zoneid', 'isolationmethods', 'vlan', 'tags', 'networkspeed', 'broadcastdomainrange'],
      mapping: {
        isolationmethods: {
          options: ['VLAN', 'VXLAN', 'GRE', 'STT', 'BCF_SEGMENT', 'SSP', 'ODL', 'L3VPN', 'VCS']
        }
      }
    },
    {
      api: 'updatePhysicalNetwork',
      icon: 'play-circle',
      label: 'Enable Physical Network',
      dataView: true,
      args: ['state'],
      show: (record) => { return record.state === 'Disabled' },
      mapping: {
        state: {
          value: (record) => { return 'Enabled' }
        }
      }
    },
    {
      api: 'updatePhysicalNetwork',
      icon: 'stop',
      label: 'Disable Physical Network',
      dataView: true,
      args: ['state'],
      show: (record) => { return record.state === 'Enabled' },
      mapping: {
        state: {
          value: (record) => { return 'Disabled' }
        }
      }
    },
    {
      api: 'updatePhysicalNetwork',
      icon: 'edit',
      label: 'Update Physical Network',
      dataView: true,
      args: ['vlan', 'tags']
    },
    {
      api: 'addTrafficType',
      icon: 'plus-square',
      label: 'Add Traffic Type',
      dataView: true,
      args: ['traffictype', 'physicalnetworkid', 'isolationmethod'],
      mapping: {
        traffictype: {
          options: ['Public', 'Guest', 'Management', 'Storage']
        },
        physicalnetworkid: {
          value: (record) => { return record.id }
        },
        isolationmethod: {
          options: ['', 'vlan', 'vxlan']
        }
      }
    },
    {
      api: 'updateTrafficType',
      icon: 'branches',
      label: 'Update Traffic Labels',
      dataView: true,
      args: ['id', 'kvmnetworklabel', 'vmwarenetworklabel', 'xennetworklabel', 'hypervnetworklabel', 'ovm3networklabel'],
      mapping: {
        id: {
          api: 'listTrafficTypes',
          params: (record) => { return { physicalnetworkid: record.id } }
        }
      }
    },
    {
      api: 'deletePhysicalNetwork',
      icon: 'delete',
      label: 'Delete Physical Network',
      dataView: true
    }
  ]
}
