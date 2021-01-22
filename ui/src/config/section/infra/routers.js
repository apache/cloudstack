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
  name: 'router',
  title: 'label.virtual.routers',
  icon: 'fork',
  docHelp: 'adminguide/systemvm.html#virtual-router',
  permission: ['listRouters'],
  params: { projectid: '-1' },
  columns: ['name', 'state', 'publicip', 'guestnetworkname', 'vpcname', 'redundantstate', 'version', 'hostname', 'account', 'zonename', 'requiresupgrade'],
  searchFilters: ['name', 'zoneid', 'podid', 'clusterid'],
  details: ['name', 'id', 'version', 'requiresupgrade', 'guestnetworkname', 'vpcname', 'publicip', 'guestipaddress', 'linklocalip', 'serviceofferingname', 'networkdomain', 'isredundantrouter', 'redundantstate', 'hostname', 'account', 'zonename', 'created'],
  tabs: [{
    name: 'details',
    component: () => import('@/components/view/DetailsTab.vue')
  }, {
    name: 'nics',
    component: () => import('@/views/network/NicsTable.vue')
  }, {
    name: 'router.health.checks',
    show: (record, route, user) => { return ['Running'].includes(record.state) && ['Admin'].includes(user.roletype) },
    component: () => import('@views/infra/routers/RouterHealthCheck.vue')
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
      icon: 'caret-right',
      label: 'label.action.start.router',
      message: 'message.action.start.router',
      dataView: true,
      show: (record) => { return record.state === 'Stopped' }
    },
    {
      api: 'stopRouter',
      icon: 'poweroff',
      label: 'label.action.stop.router',
      message: 'message.action.stop.router',
      dataView: true,
      args: ['forced'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'rebootRouter',
      icon: 'sync',
      label: 'label.action.reboot.router',
      message: 'message.action.reboot.router',
      dataView: true,
      hidden: (record) => { return record.state === 'Running' }
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt',
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
      icon: 'fullscreen',
      label: 'label.upgrade.router.newer.template',
      message: 'message.confirm.upgrade.router.newer.template',
      docHelp: 'adminguide/systemvm.html#upgrading-virtual-routers',
      dataView: true,
      groupAction: true,
      show: (record) => { return record.requiresupgrade }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag',
      label: 'label.action.migrate.router',
      dataView: true,
      show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
      args: ['virtualmachineid', 'hostid'],
      mapping: {
        virtualmachineid: {
          value: (record) => { return record.id }
        },
        hostid: {
          api: 'findHostsForMigration',
          params: (record) => { return { virtualmachineid: record.id } }
        }
      }
    },
    {
      api: 'runDiagnostics',
      icon: 'reconciliation',
      label: 'label.action.run.diagnostics',
      dataView: true,
      show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
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
      icon: 'delete',
      label: 'label.destroy.router',
      message: 'message.confirm.destroy.router',
      dataView: true,
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) }
    }
  ]
}
