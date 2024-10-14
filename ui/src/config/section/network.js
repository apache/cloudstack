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
import tungsten from '@/assets/icons/tungsten.svg?inline'
import { isAdmin } from '@/role'

export default {
  name: 'network',
  title: 'label.network',
  icon: 'wifi-outlined',
  docHelp: 'adminguide/networking_and_traffic.html#advanced-zone-physical-network-configuration',
  children: [
    {
      name: 'guestnetwork',
      title: 'label.guest.networks',
      icon: 'apartment-outlined',
      docHelp: 'adminguide/networking_and_traffic.html#adding-an-additional-guest-network',
      permission: ['listNetworks'],
      resourceType: 'Network',
      columns: () => {
        var fields = ['name', 'state', 'type', 'vpcname', 'cidr', 'ip6cidr', 'broadcasturi', 'domainpath']
        if (!isAdmin()) {
          fields = fields.filter(function (e) { return e !== 'broadcasturi' })
        }
        if (store.getters.listAllProjects) {
          fields.push('project')
        } else {
          fields.push('account')
        }
        fields.push('zonename')
        return fields
      },
      details: () => {
        var fields = ['name', 'id', 'description', 'type', 'traffictype', 'vpcid', 'vlan', 'broadcasturi', 'cidr', 'ip6cidr', 'netmask', 'gateway', 'asnumber', 'aclname', 'ispersistent', 'restartrequired', 'reservediprange', 'redundantrouter', 'networkdomain', 'egressdefaultpolicy', 'zonename', 'account', 'domainpath', 'associatednetwork', 'associatednetworkid', 'ip4routing', 'ip6routing', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'publicmtu', 'privatemtu']
        if (!isAdmin()) {
          fields = fields.filter(function (e) { return e !== 'broadcasturi' })
        }
        return fields
      },
      filters: ['all', 'account', 'domainpath', 'shared'],
      searchFilters: ['keyword', 'zoneid', 'domainid', 'account', 'type', 'restartrequired', 'tags'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'networkid'
      }],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'egress.rules',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/EgressRulesTab.vue'))),
        show: (record, route, user) => { return record.type === 'Isolated' && !record.ip4routing && !('vpcname' in record) && 'listEgressFirewallRules' in store.getters.apis && (['Admin', 'DomainAdmin'].includes(user.roletype) || record.account === user.account || record.projectid) }
      }, {
        name: 'bgp.peers',
        component: shallowRef(defineAsyncComponent(() => import('@/views/infra/zone/BgpPeersTab.vue'))),
        show: (record, route, user) => { return !record.vpcid && ['Admin'].includes(user.roletype) && record.ip4routing === 'Dynamic' }
      }, {
        name: 'routing.firewall',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/RoutingFirewallRulesTab.vue'))),
        show: (record, route, user) => { return record.type === 'Isolated' && record.ip4routing && !('vpcname' in record) && 'listRoutingFirewallRules' in store.getters.apis && (['Admin', 'DomainAdmin'].includes(user.roletype) || record.account === user.account || record.projectid) }
      }, {
        name: 'ip.v6.firewall',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/Ipv6FirewallRulesTab.vue'))),
        show: (record, route, user) => { return record.type === 'Isolated' && ['IPv6', 'DualStack'].includes(record.internetprotocol) && !('vpcid' in record) && 'listIpv6FirewallRules' in store.getters.apis && (['Admin', 'DomainAdmin'].includes(user.roletype) || record.account === user.account || record.projectid) }
      }, {
        name: (record) => { return record.type === 'Shared' ? 'ip.addresses' : 'public.ip.addresses' },
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/IpAddressesTab.vue'))),
        show: (record, route, user) => { return 'listPublicIpAddresses' in store.getters.apis && (record.type === 'Shared' || (record.type === 'Isolated' && !record.ip4routing && !('vpcname' in record) && (['Admin', 'DomainAdmin'].includes(user.roletype) || record.account === user.account || record.projectid))) }
      }, {
        name: 'virtual.routers',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/RoutersTab.vue'))),
        show: (record) => { return (record.type === 'Isolated' || record.type === 'Shared') && 'listRouters' in store.getters.apis && isAdmin() }
      }, {
        name: 'vnf.appliances',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/VnfAppliancesTab.vue'))),
        show: () => { return 'listVnfAppliances' in store.getters.apis }
      }, {
        name: 'guest.ip.range',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/GuestIpRanges.vue'))),
        show: (record) => { return 'listVlanIpRanges' in store.getters.apis && (record.type === 'Shared' || (record.service && record.service.filter(x => x.name === 'SourceNat').count === 0)) }
      }, {
        name: 'network.policy',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/NetworkPolicyTab.vue'))),
        show: (record) => {
          return ('listTungstenFabricPolicy' in store.getters.apis) && (record.broadcasturi === 'tf://tf' && record.type !== 'Shared')
        }
      }, {
        name: 'tungsten.logical.router',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/LogicalRouterTab.vue'))),
        show: (record) => {
          return ('listTungstenFabricLogicalRouter' in store.getters.apis) && (record.broadcasturi === 'tf://tf' && record.type !== 'Shared')
        }
      }, {
        name: 'network.permissions',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/NetworkPermissions.vue'))),
        show: (record, route, user) => { return 'listNetworkPermissions' in store.getters.apis && record.acltype === 'Account' && !('vpcid' in record) && (['Admin', 'DomainAdmin'].includes(user.roletype) || record.account === user.account) && !record.projectid }
      },
      {
        name: 'events',
        resourceType: 'Network',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in store.getters.apis }
      },
      {
        name: 'comments',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
      }],
      actions: [
        {
          api: 'createNetwork',
          icon: 'plus-outlined',
          label: 'label.add.network',
          docHelp: 'adminguide/networking_and_traffic.html#configure-guest-traffic-in-an-advanced-zone',
          listView: true,
          popup: true,
          show: () => {
            if (!store.getters.zones || store.getters.zones.length === 0) {
              return false
            }
            const AdvancedZones = store.getters.zones.filter(zone => zone.networktype === 'Advanced')
            const AdvancedZonesWithoutSG = store.getters.zones.filter(zone => zone.securitygroupsenabled === false)
            if ((isAdmin() && AdvancedZones && AdvancedZones.length > 0) || (AdvancedZonesWithoutSG && AdvancedZonesWithoutSG.length > 0)) {
              return true
            }
            return false
          },
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/CreateNetwork.vue')))
        },
        {
          api: 'updateNetwork',
          icon: 'edit-outlined',
          label: 'label.update.network',
          dataView: true,
          disabled: (record, user) => {
            return !record.projectid && (record.account !== user.userInfo.account && !['Admin', 'DomainAdmin'].includes(user.userInfo.roletype))
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/UpdateNetwork.vue')))
        },
        {
          api: 'restartNetwork',
          icon: 'sync-outlined',
          label: 'label.restart.network',
          message: 'message.restart.network',
          dataView: true,
          disabled: (record, user) => {
            return !record.projectid && (record.account !== user.userInfo.account && !['Admin', 'DomainAdmin'].includes(user.userInfo.roletype))
          },
          args: (record, store, isGroupAction) => {
            var fields = []
            if (isGroupAction || record.vpcid == null) {
              fields.push('cleanup')
            }
            fields.push('livepatch')
            return fields
          },
          show: (record) => record.type !== 'L2',
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, cleanup: values.cleanup } }) }
        },
        {
          api: 'replaceNetworkACLList',
          icon: 'swap-outlined',
          label: 'label.replace.acl.list',
          message: 'message.confirm.replace.acl.new.one',
          docHelp: 'adminguide/networking_and_traffic.html#configuring-network-access-control-list',
          dataView: true,
          show: (record) => { return record.vpcid },
          args: ['aclid', 'networkid'],
          mapping: {
            aclid: {
              api: 'listNetworkACLLists',
              params: (record) => { return { vpcid: record.vpcid } }
            },
            networkid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteNetwork',
          icon: 'delete-outlined',
          label: 'label.action.delete.network',
          message: 'message.action.delete.network',
          dataView: true,
          disabled: (record, user) => {
            return !record.projectid && (record.account !== user.userInfo.account && !['Admin', 'DomainAdmin'].includes(user.userInfo.roletype))
          },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'vpc',
      title: 'label.vpc',
      icon: 'deployment-unit-outlined',
      docHelp: 'adminguide/networking_and_traffic.html#configuring-a-virtual-private-cloud',
      permission: ['listVPCs'],
      resourceType: 'Vpc',
      columns: () => {
        var fields = ['name', 'state', 'displaytext', 'cidr', 'account']
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push(...['domain', 'zonename'])
        return fields
      },
      details: ['name', 'id', 'displaytext', 'cidr', 'networkdomain', 'ip4routing', 'ip4routes', 'ip6routes', 'ispersistent', 'redundantvpcrouter', 'restartrequired', 'zonename', 'account', 'domain', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'publicmtu'],
      searchFilters: ['name', 'zoneid', 'domainid', 'account', 'restartrequired', 'tags'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'vpcid'
      }, {
        name: 'router',
        title: 'label.virtual.routers',
        param: 'vpcid'
      }, {
        name: 'ilbvm',
        title: 'label.internal.lb',
        param: 'vpcid'
      }],
      tabs: [{
        name: 'vpc',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/VpcTab.vue')))
      }],
      actions: [
        {
          api: 'createVPC',
          icon: 'plus-outlined',
          label: 'label.add.vpc',
          docHelp: 'adminguide/networking_and_traffic.html#adding-a-virtual-private-cloud',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/CreateVpc.vue')))
        },
        {
          api: 'updateVPC',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          args: ['name', 'displaytext', 'publicmtu', 'sourcenatipaddress']
        },
        {
          api: 'restartVPC',
          icon: 'sync-outlined',
          label: 'label.restart.vpc',
          message: (record) => { return record.redundantvpcrouter ? 'message.restart.vpc' : 'message.restart.vpc.remark' },
          dataView: true,
          args: (record) => {
            var fields = ['cleanup']
            if (!record.redundantvpcrouter) {
              fields.push('makeredundant')
            }
            fields.push('livepatch')
            return fields
          },
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, cleanup: values.cleanup, makeredundant: values.makeredundant } }) }
        },
        {
          api: 'deleteVPC',
          icon: 'delete-outlined',
          label: 'label.remove.vpc',
          message: 'message.remove.vpc',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'securitygroups',
      title: 'label.security.groups',
      icon: 'fire-outlined',
      docHelp: 'adminguide/networking_and_traffic.html#security-groups',
      permission: ['listSecurityGroups'],
      resourceType: 'SecurityGroup',
      columns: ['name', 'description', 'account', 'domain'],
      details: ['name', 'id', 'description', 'account', 'domain'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'ingress.rule',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/IngressEgressRuleConfigure.vue')))
      }, {
        name: 'egress.rule',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/IngressEgressRuleConfigure.vue')))
      }],
      show: () => {
        if (!store.getters.zones || store.getters.zones.length === 0) {
          return false
        }
        const listZoneHaveSGEnabled = store.getters.zones.filter(zone => zone.securitygroupsenabled === true)
        return (listZoneHaveSGEnabled && listZoneHaveSGEnabled.length > 0) || store.getters.showSecurityGroups
      },
      actions: [
        {
          api: 'createSecurityGroup',
          icon: 'plus-outlined',
          label: 'label.add.security.group',
          docHelp: 'adminguide/networking_and_traffic.html#adding-a-security-group',
          listView: true,
          args: ['name', 'description']
        },
        {
          api: 'updateSecurityGroup',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          args: ['name'],
          show: (record) => { return record.name !== 'default' }
        },
        {
          api: 'deleteSecurityGroup',
          icon: 'delete-outlined',
          label: 'label.action.delete.security.group',
          message: 'message.action.delete.security.group',
          dataView: true,
          show: (record) => { return record.name !== 'default' }
        }
      ]
    },
    {
      name: 'vnfapp',
      title: 'label.vnf.appliances',
      icon: 'gateway-outlined',
      permission: ['listVnfAppliances'],
      resourceType: 'UserVm',
      params: () => {
        return { details: 'servoff,tmpl,nics', isvnf: true }
      },
      columns: () => {
        const fields = ['name', 'state', 'ipaddress']
        if (store.getters.userInfo.roletype === 'Admin') {
          fields.splice(2, 0, 'instancename')
          fields.push('account')
          if (store.getters.listAllProjects) {
            fields.push('project')
          }
          fields.push('domain')
          fields.push('hostname')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
          if (store.getters.listAllProjects) {
            fields.push('project')
          }
        } else {
          fields.push('serviceofferingname')
        }
        fields.push('zonename')
        return fields
      },
      searchFilters: ['name', 'zoneid', 'domainid', 'account', 'groupid', 'tags'],
      details: () => {
        var fields = ['name', 'displayname', 'id', 'state', 'ipaddress', 'ip6address', 'templatename', 'ostypename',
          'serviceofferingname', 'isdynamicallyscalable', 'haenable', 'hypervisor', 'boottype', 'bootmode', 'account',
          'domain', 'zonename', 'userdataid', 'userdataname', 'userdataparams', 'userdatadetails', 'userdatapolicy', 'hostcontrolstate']
        const listZoneHaveSGEnabled = store.getters.zones.filter(zone => zone.securitygroupsenabled === true)
        if (!listZoneHaveSGEnabled || listZoneHaveSGEnabled.length === 0) {
          return fields
        }
        fields.push('securitygroup')
        return fields
      },
      tabs: [{
        component: shallowRef(defineAsyncComponent(() => import('@/views/compute/InstanceTab.vue')))
      }],
      actions: [
        {
          api: 'deployVnfAppliance',
          icon: 'plus-outlined',
          label: 'label.vnf.appliance.add',
          docHelp: 'adminguide/networking/vnf_templates_appliances.html#deploying-vnf-appliances',
          listView: true,
          component: () => import('@/views/compute/DeployVnfAppliance.vue')
        },
        {
          api: 'updateVirtualMachine',
          icon: 'edit-outlined',
          label: 'label.vnf.app.action.edit',
          docHelp: 'adminguide/virtual_machines.html#changing-the-vm-name-os-or-group',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/EditVM.vue')))
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right-outlined',
          label: 'label.vnf.app.action.start',
          message: 'message.action.start.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, considerlasthost: values.considerlasthost } }) },
          args: ['considerlasthost'],
          show: (record) => { return ['Stopped'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/StartVirtualMachine.vue')))
        },
        {
          api: 'stopVirtualMachine',
          icon: 'poweroff-outlined',
          label: 'label.vnf.app.action.stop',
          message: 'message.action.stop.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) },
          args: ['forced'],
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'rebootVirtualMachine',
          icon: 'reload-outlined',
          label: 'label.vnf.app.action.reboot',
          message: 'message.action.reboot.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          args: (record, store) => {
            var fields = []
            fields.push('forced')
            if (record.hypervisor === 'VMware') {
              if (store.apis.rebootVirtualMachine.params.filter(x => x.name === 'bootintosetup').length > 0) {
                fields.push('bootintosetup')
              }
            }
            return fields
          },
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'sync-outlined',
          label: 'label.vnf.app.action.reinstall',
          message: 'message.reinstall.vm',
          dataView: true,
          args: ['virtualmachineid', 'templateid'],
          filters: (record) => {
            var filters = {}
            var filterParams = {}
            filterParams.hypervisortype = record.hypervisor
            filterParams.zoneid = record.zoneid
            filters.templateid = filterParams
            return filters
          },
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) },
          mapping: {
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          successMethod: (obj, result) => {
            const vm = result.jobresult.virtualmachine || {}
            if (result.jobstatus === 1 && vm.password) {
              const name = vm.displayname || vm.name || vm.id
              obj.$notification.success({
                message: `${obj.$t('label.reinstall.vm')}: ` + name,
                description: `${obj.$t('label.password.reset.confirm')}: ` + vm.password,
                duration: 0
              })
            }
          }
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera-outlined',
          label: 'label.action.vmsnapshot.create',
          docHelp: 'adminguide/virtual_machines.html#virtual-machine-snapshots',
          dataView: true,
          args: ['virtualmachineid', 'name', 'description', 'snapshotmemory', 'quiescevm'],
          show: (record) => {
            return ((['Running'].includes(record.state) && record.hypervisor !== 'LXC') ||
              (['Stopped'].includes(record.state) && ((record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC') ||
                (record.hypervisor === 'KVM' && record.pooltype === 'PowerFlex'))))
          },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' && record.hypervisor === 'KVM' },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'createSnapshot',
          icon: ['fas', 'camera-retro'],
          label: 'label.action.vmstoragesnapshot.create',
          docHelp: 'adminguide/virtual_machines.html#virtual-machine-snapshots',
          dataView: true,
          popup: true,
          show: (record) => {
            return ((['Running'].includes(record.state) && record.hypervisor !== 'LXC') ||
              (['Stopped'].includes(record.state) && !['KVM', 'LXC'].includes(record.hypervisor)))
          },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' && record.hypervisor === 'KVM' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/CreateSnapshotWizard.vue')))
        },
        {
          api: 'assignVirtualMachineToBackupOffering',
          icon: 'folder-add-outlined',
          label: 'label.backup.offering.assign',
          message: 'label.backup.offering.assign',
          docHelp: 'adminguide/virtual_machines.html#backup-offerings',
          dataView: true,
          args: ['virtualmachineid', 'backupofferingid'],
          show: (record) => { return !record.backupofferingid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'createBackup',
          icon: 'cloud-upload-outlined',
          label: 'label.create.backup',
          message: 'message.backup.create',
          docHelp: 'adminguide/virtual_machines.html#creating-vm-backups',
          dataView: true,
          args: ['virtualmachineid'],
          show: (record) => { return record.backupofferingid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'createBackupSchedule',
          icon: 'schedule-outlined',
          label: 'label.backup.configure.schedule',
          docHelp: 'adminguide/virtual_machines.html#creating-vm-backups',
          dataView: true,
          popup: true,
          show: (record) => { return record.backupofferingid },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/BackupScheduleWizard.vue'))),
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            },
            intervaltype: {
              options: ['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY']
            }
          }
        },
        {
          api: 'removeVirtualMachineFromBackupOffering',
          icon: 'scissor-outlined',
          label: 'label.backup.offering.remove',
          message: 'label.backup.offering.remove',
          docHelp: 'adminguide/virtual_machines.html#restoring-vm-backups',
          dataView: true,
          args: ['virtualmachineid', 'forced'],
          show: (record) => { return record.backupofferingid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'attachIso',
          icon: 'paper-clip-outlined',
          label: 'label.action.attach.iso',
          docHelp: 'adminguide/templates.html#attaching-an-iso-to-a-vm',
          dataView: true,
          popup: true,
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && !record.isoid },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' || record.hostcontrolstate === 'Maintenance' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AttachIso.vue')))
        },
        {
          api: 'detachIso',
          icon: 'link-outlined',
          label: 'label.action.detach.iso',
          message: 'message.detach.iso.confirm',
          dataView: true,
          args: (record, store) => {
            var args = ['virtualmachineid']
            if (record && record.hypervisor && record.hypervisor === 'VMware') {
              args.push('forced')
            }
            return args
          },
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && 'isoid' in record && record.isoid },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' || record.hostcontrolstate === 'Maintenance' },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'updateVMAffinityGroup',
          icon: 'swap-outlined',
          label: 'label.change.affinity',
          docHelp: 'adminguide/virtual_machines.html#change-affinity-group-for-an-existing-vm',
          dataView: true,
          args: ['affinitygroupids'],
          show: (record) => { return ['Stopped'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ChangeAffinity'))),
          popup: true
        },
        {
          api: 'scaleVirtualMachine',
          icon: 'arrows-alt-outlined',
          label: 'label.vnf.app.action.scale',
          docHelp: 'adminguide/virtual_machines.html#how-to-dynamically-scale-cpu-and-ram',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) || (['Running'].includes(record.state) && record.hypervisor !== 'LXC') },
          disabled: (record) => { return record.state === 'Running' && !record.isdynamicallyscalable },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ScaleVM.vue')))
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag-outlined',
          label: 'label.vnf.app.action.migrate.to.host',
          docHelp: 'adminguide/virtual_machines.html#moving-vms-between-hosts-manual-live-migration',
          dataView: true,
          show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateWizard.vue')))
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag-outlined',
          label: 'label.vnf.app.action.migrate.to.ps',
          message: 'message.migrate.instance.to.ps',
          docHelp: 'adminguide/virtual_machines.html#moving-vms-between-hosts-manual-live-migration',
          dataView: true,
          show: (record, store) => { return ['Stopped'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateVMStorage'))),
          popup: true
        },
        {
          api: 'resetPasswordForVirtualMachine',
          icon: 'key-outlined',
          label: 'label.action.reset.password',
          message: 'message.action.instance.reset.password',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) && record.passwordenabled },
          response: (result) => {
            return {
              message: result.virtualmachine && result.virtualmachine.password ? `The password of VM <b>${result.virtualmachine.displayname}</b> is <b>${result.virtualmachine.password}</b>` : null,
              copybuttontext: result.virtualmachine.password ? 'label.copy.password' : null,
              copytext: result.virtualmachine.password ? result.virtualmachine.password : null
            }
          }
        },
        {
          api: 'resetSSHKeyForVirtualMachine',
          icon: 'lock-outlined',
          label: 'label.reset.ssh.key.pair',
          message: 'message.desc.reset.ssh.key.pair',
          docHelp: 'adminguide/virtual_machines.html#resetting-ssh-keys',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ResetSshKeyPair')))
        },
        {
          api: 'resetUserDataForVirtualMachine',
          icon: 'solution-outlined',
          label: 'label.reset.userdata.on.vm',
          message: 'message.desc.reset.userdata',
          docHelp: 'adminguide/virtual_machines.html#resetting-userdata',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ResetUserData')))
        },
        {
          api: 'assignVirtualMachine',
          icon: 'user-add-outlined',
          label: 'label.assign.instance.another',
          dataView: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AssignInstance'))),
          popup: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'recoverVirtualMachine',
          icon: 'medicine-box-outlined',
          label: 'label.vnf.app.action.recover',
          message: 'message.recover.vm',
          dataView: true,
          show: (record, store) => { return ['Destroyed'].includes(record.state) && store.features.allowuserexpungerecovervm }
        },
        {
          api: 'unmanageVirtualMachine',
          icon: 'disconnect-outlined',
          label: 'label.action.unmanage.virtualmachine',
          message: 'message.action.unmanage.virtualmachine',
          dataView: true,
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && record.hypervisor === 'VMware' }
        },
        {
          api: 'expungeVirtualMachine',
          icon: 'delete-outlined',
          label: 'label.vnf.app.action.expunge',
          message: (record) => { return record.backupofferingid ? 'message.action.expunge.instance.with.backups' : 'message.action.expunge.instance' },
          docHelp: 'adminguide/virtual_machines.html#deleting-vms',
          dataView: true,
          show: (record, store) => { return ['Destroyed', 'Expunging'].includes(record.state) && store.features.allowuserexpungerecovervm }
        },
        {
          api: 'destroyVirtualMachine',
          icon: 'delete-outlined',
          label: 'label.vnf.app.action.destroy',
          message: 'message.action.destroy.instance',
          docHelp: 'adminguide/virtual_machines.html#deleting-vms',
          dataView: true,
          groupAction: true,
          args: (record, store, group) => {
            return (['Admin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervm)
              ? ['expunge'] : []
          },
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, expunge: values.expunge } }) },
          show: (record) => { return ['Running', 'Stopped', 'Error'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/DestroyVM.vue')))
        }
      ]
    },
    {
      name: 'publicip',
      title: 'label.public.ip.addresses',
      icon: 'environment-outlined',
      docHelp: 'adminguide/networking_and_traffic.html#reserving-public-ip-addresses-and-vlans-for-accounts',
      permission: ['listPublicIpAddresses'],
      searchFilters: ['ipaddress', 'zoneid', 'account', 'domainid', 'vlanid', 'tags'],
      resourceType: 'PublicIpAddress',
      columns: () => {
        var fields = ['ipaddress', 'state', 'associatednetworkname', 'vpcname', 'virtualmachinename', 'allocated', 'account']
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push(...['domain', 'zonename'])
        return fields
      },
      details: ['ipaddress', 'id', 'associatednetworkname', 'virtualmachinename', 'networkid', 'issourcenat', 'isstaticnat', 'virtualmachinename', 'vmipaddress', 'vlan', 'allocated', 'account', 'domain', 'zonename'],
      filters: ['allocated', 'reserved', 'free'],
      component: shallowRef(() => import('@/views/network/PublicIpResource.vue')),
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'firewall',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/FirewallRules.vue'))),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'Firewall').length > 0,
        groupAction: true,
        popup: true,
        groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      },
      {
        name: 'portforwarding',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/PortForwarding.vue'))),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'PortForwarding').length > 0
      }, {
        name: 'loadbalancing',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/LoadBalancing.vue'))),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'Lb').length > 0
      }, {
        name: 'vpn',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/VpnDetails.vue'))),
        show: (record) => { return record.issourcenat }
      },
      {
        name: 'events',
        resourceType: 'IpAddress',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in store.getters.apis }
      },
      {
        name: 'comments',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
      }],
      actions: [
        {
          api: 'enableStaticNat',
          icon: 'plus-circle-outlined',
          label: 'label.action.enable.static.nat',
          docHelp: 'adminguide/networking_and_traffic.html#enabling-or-disabling-static-nat',
          dataView: true,
          show: (record) => { return record.state === 'Allocated' && !record.virtualmachineid && !record.issourcenat },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/EnableStaticNat.vue')))
        },
        {
          api: 'disableStaticNat',
          icon: 'minus-circle-outlined',
          label: 'label.action.disable.static.nat',
          message: 'message.action.disable.static.nat',
          docHelp: 'adminguide/networking_and_traffic.html#enabling-or-disabling-static-nat',
          dataView: true,
          show: (record) => { return record.virtualmachineid },
          args: ['ipaddressid'],
          mapping: {
            ipaddressid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'disassociateIpAddress',
          icon: 'delete-outlined',
          label: 'label.action.release.ip',
          message: 'message.action.release.ip',
          docHelp: 'adminguide/networking_and_traffic.html#releasing-an-ip-address-alloted-to-a-vpc',
          dataView: true,
          show: (record) => { return record.state === 'Allocated' && !record.issourcenat },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'reserveIpAddress',
          icon: 'lock-outlined',
          label: 'label.action.reserve.ip',
          dataView: true,
          show: (record) => { return record.state === 'Free' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/ReservePublicIP.vue')))
        },
        {
          api: 'releaseIpAddress',
          icon: 'usergroup-delete-outlined',
          label: 'label.action.release.reserved.ip',
          message: 'message.action.release.reserved.ip',
          dataView: true,
          show: (record) => { return record.state === 'Reserved' },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'asnumbers',
      title: 'label.asnumbers',
      icon: 'partition-outlined',
      permission: ['listASNumbers'],
      show: () => {
        return ['Admin'].includes(store.getters.userInfo.roletype)
      },
      filters: ['all', 'allocatedonly', 'free'],
      columns: ['asnumber', 'allocationstate', 'asnrange', 'associatednetworkname', 'vpcname', 'allocated', 'account', 'domain', 'zonename'],
      searchFilters: ['zoneid', 'associatednetworkid', 'account', 'domainid'],
      resourceType: 'ASNumber',
      actions: [
        {
          api: 'releaseASNumber',
          icon: 'delete-outlined',
          label: 'label.action.release.asnumber',
          message: 'message.action.release.asnumber',
          show: (record) => { return record.allocationstate === 'Allocated' },
          args: ['zoneid', 'asnumber'],
          mapping: {
            zoneid: {
              value: (record) => { return record.zoneid }
            },
            asnumber: {
              value: (record) => { return record.asnumber }
            }
          },
          dataView: true,
          groupAction: true,
          popup: true,
          groupShow: (selectedItems, storegetters) => {
            return selectedItems.length === 1 && selectedItems[0].allocationstate === 'Allocated'
          },
          groupMap: (selectedId, values, records) => {
            const record = records.filter(x => { return x.id === selectedId[0] })
            return record
          }
        }
      ]
    },
    {
      name: 'privategw',
      title: 'label.private.gateway',
      icon: 'gateway-outlined',
      hidden: true,
      permission: ['listPrivateGateways'],
      columns: ['ipaddress', 'state', 'gateway', 'netmask', 'account', 'domain'],
      details: ['ipaddress', 'gateway', 'netmask', 'vlan', 'sourcenatsupported', 'aclname', 'account', 'domain', 'zone', 'associatednetwork', 'associatednetworkid'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'static.routes',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/StaticRoutesTab.vue'))),
        show: () => true
      }],
      actions: [
        {
          api: 'createPrivateGateway',
          icon: 'plus-outlined',
          label: 'label.add.private.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#adding-a-private-gateway-to-a-vpc',
          listView: true,
          args: (record, store) => {
            var fields = ['vpcid', 'physicalnetworkid', 'vlan', 'ipaddress', 'gateway', 'netmask', 'sourcenatsupported', 'aclid']
            if (store.apis.createPrivateGateway.params.filter(x => x.name === 'bypassvlanoverlapcheck').length > 0) {
              fields.push('bypassvlanoverlapcheck')
            }
            return fields
          },
          mapping: {
            aclid: {
              api: 'listNetworkACLLists'
            }
          }
        },
        {
          api: 'replaceNetworkACLList',
          icon: 'swap-outlined',
          label: 'label.replace.acl.list',
          message: 'message.confirm.replace.acl.new.one',
          docHelp: 'adminguide/networking_and_traffic.html#acl-on-private-gateway',
          dataView: true,
          args: ['aclid', 'gatewayid'],
          mapping: {
            aclid: {
              api: 'listNetworkACLLists',
              params: (record) => { return { vpcid: record.vpcid } }
            },
            gatewayid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deletePrivateGateway',
          icon: 'delete-outlined',
          label: 'label.delete.gateway',
          message: 'message.delete.gateway',
          dataView: true
        }
      ]
    },
    {
      name: 's2svpn',
      title: 'label.site.to.site.vpn',
      icon: 'lock-outlined',
      permission: ['listVpnGateways'],
      columns: ['publicip', 'account', 'domain'],
      details: ['publicip', 'account', 'domain'],
      actions: [
        {
          api: 'createVpnGateway',
          icon: 'plus-outlined',
          label: 'label.add.vpn.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#creating-a-vpn-gateway-for-the-vpc',
          listView: true,
          args: ['vpcid']
        },
        {
          api: 'deleteVpnGateway',
          icon: 'delete-outlined',
          label: 'label.delete.vpn.gateway',
          message: 'message.delete.vpn.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#restarting-and-removing-a-vpn-connection',
          dataView: true
        }
      ]
    },
    {
      name: 's2svpnconn',
      title: 'label.site.to.site.vpn.connections',
      docHelp: 'adminguide/networking_and_traffic.html#setting-up-a-site-to-site-vpn-connection',
      icon: 'sync-outlined',
      hidden: true,
      permission: ['listVpnConnections'],
      columns: ['publicip', 'state', 'gateway', 'ipsecpsk', 'ikepolicy', 'esppolicy'],
      details: ['publicip', 'gateway', 'passive', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'esppolicy', 'ikelifetime', 'ikeversion', 'esplifetime', 'dpd', 'splitconnections', 'forceencap', 'created'],
      actions: [
        {
          api: 'createVpnConnection',
          icon: 'plus-outlined',
          label: 'label.create.vpn.connection',
          docHelp: 'adminguide/networking_and_traffic.html#creating-a-vpn-connection',
          listView: true,
          args: ['s2scustomergatewayid', 's2svpngatewayid', 'passive'],
          mapping: {
            s2scustomergatewayid: {
              api: 'listVpnCustomerGateways'
            },
            s2svpngatewayid: {
              api: 'listVpnGateways'
            }
          }
        },
        {
          api: 'resetVpnConnection',
          icon: 'reload-outlined',
          label: 'label.reset.vpn.connection',
          message: 'message.reset.vpn.connection',
          docHelp: 'adminguide/networking_and_traffic.html#restarting-and-removing-a-vpn-connection',
          dataView: true
        },
        {
          api: 'deleteVpnConnection',
          icon: 'delete-outlined',
          label: 'label.delete.vpn.connection',
          message: 'message.delete.vpn.connection',
          docHelp: 'adminguide/networking_and_traffic.html#restarting-and-removing-a-vpn-connection',
          dataView: true
        }
      ]
    },
    {
      name: 'acllist',
      title: 'label.network.acl.lists',
      icon: 'bars-outlined',
      docHelp: 'adminguide/networking_and_traffic.html#configuring-network-access-control-list',
      hidden: true,
      permission: ['listNetworkACLLists'],
      columns: ['name', 'description', 'id'],
      details: ['name', 'description', 'id'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'acl.list.rules',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/AclListRulesTab.vue'))),
        show: () => true
      }],
      actions: [
        {
          api: 'createNetworkACLList',
          icon: 'plus-outlined',
          label: 'label.add.acl.list',
          docHelp: 'adminguide/networking_and_traffic.html#creating-acl-lists',
          listView: true,
          args: ['name', 'description', 'vpcid']
        },
        {
          api: 'updateNetworkACLList',
          icon: 'edit-outlined',
          label: 'label.edit.acl.list',
          dataView: true,
          args: ['name', 'description']
        },
        {
          api: 'deleteNetworkACLList',
          icon: 'delete-outlined',
          label: 'label.delete.acl.list',
          message: 'message.confirm.delete.acl.list',
          dataView: true
        }
      ]
    },
    {
      name: 'ilb',
      title: 'label.internal.lb',
      docHelp: 'adminguide/networking_and_traffic.html#load-balancing-across-tiers',
      icon: 'share-alt-outlined',
      hidden: true,
      permission: ['listLoadBalancers'],
      columns: ['name', 'sourceipaddress', 'loadbalancerrule', 'algorithm', 'account', 'domain'],
      details: ['name', 'sourceipaddress', 'loadbalancerrule', 'algorithm', 'account', 'domain'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'loadbalancerinstance',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/InternalLBAssignedVmTab.vue'))),
        show: () => true
      }, {
        name: 'events',
        resourceType: 'LoadBalancerRule',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in store.getters.apis }
      }],
      actions: [
        {
          api: 'createLoadBalancer',
          icon: 'plus-outlined',
          label: 'label.add.internal.lb',
          docHelp: 'adminguide/networking_and_traffic.html#creating-an-internal-lb-rule',
          listView: true,
          args: ['name', 'description', 'sourceipaddress', 'sourceport', 'instanceport', 'algorithm', 'networkid', 'sourceipaddressnetworkid', 'scheme'],
          mapping: {
            algorithm: {
              options: ['source', 'roundrobin', 'leastconn']
            },
            scheme: {
              value: (record) => { return 'Internal' }
            },
            networkid: {
              api: 'listNetworks',
              params: (record) => { return { forvpc: true } }
            },
            sourceipaddressnetworkid: {
              api: 'listNetworks',
              params: (record) => { return { forvpc: true } }
            }
          }
        },
        {
          api: 'assignToLoadBalancerRule',
          icon: 'plus-outlined',
          label: 'label.assign.vms',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/InternalLBAssignVmForm.vue')))
        },
        {
          api: 'deleteLoadBalancer',
          icon: 'delete-outlined',
          label: 'label.delete.internal.lb',
          message: 'message.confirm.delete.internal.lb',
          dataView: true
        }
      ]
    },
    {
      name: 'vpnuser',
      title: 'label.vpn.users',
      icon: 'user-switch-outlined',
      permission: ['listVpnUsers'],
      columns: ['username', 'state', 'account', 'domain'],
      details: ['username', 'state', 'account', 'domain'],
      actions: [
        {
          api: 'addVpnUser',
          icon: 'plus-outlined',
          label: 'label.add.vpn.user',
          listView: true,
          args: (record, store) => {
            if (store.userInfo.roletype === 'User') {
              return ['username', 'password']
            }

            return ['username', 'password', 'domainid', 'account']
          }
        },
        {
          api: 'removeVpnUser',
          icon: 'delete-outlined',
          label: 'label.delete.vpn.user',
          message: 'message.action.delete.vpn.user',
          dataView: true,
          args: ['username', 'domainid', 'account'],
          mapping: {
            username: {
              value: (record) => { return record.username }
            },
            domainid: {
              value: (record) => { return record.domainid }
            },
            account: {
              value: (record) => { return record.account }
            }
          },
          groupAction: true,
          popup: true,
          groupMap: (selection, values, record) => {
            return selection.map(x => {
              const data = record.filter(y => { return y.id === x })
              return {
                username: data[0].username, account: data[0].account, domainid: data[0].domainid
              }
            })
          }
        }
      ]
    },
    {
      name: 'vpncustomergateway',
      title: 'label.vpncustomergatewayid',
      icon: 'lock-outlined',
      permission: ['listVpnCustomerGateways'],
      columns: () => {
        var fields = ['name', 'gateway', 'cidrlist', 'ipsecpsk', 'account']
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push('domain')
        return fields
      },
      details: ['name', 'id', 'gateway', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'ikelifetime', 'ikeversion', 'esppolicy', 'esplifetime', 'dpd', 'splitconnections', 'forceencap', 'account', 'domain'],
      searchFilters: ['keyword', 'domainid', 'account'],
      resourceType: 'VPNCustomerGateway',
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'VpnCustomerGateway',
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
          api: 'createVpnCustomerGateway',
          icon: 'plus-outlined',
          label: 'label.add.vpn.customer.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#creating-and-updating-a-vpn-customer-gateway',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/CreateVpnCustomerGateway.vue')))
        },
        {
          api: 'updateVpnCustomerGateway',
          icon: 'edit-outlined',
          label: 'label.edit',
          docHelp: 'adminguide/networking_and_traffic.html#updating-and-removing-a-vpn-customer-gateway',
          dataView: true,
          args: ['name', 'gateway', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'ikelifetime', 'ikeversion', 'esppolicy', 'esplifetime', 'dpd', 'splitconnections', 'forceencap'],
          mapping: {
            ikeversion: {
              options: ['ike', 'ikev1', 'ikev2']
            }
          }
        },
        {
          api: 'deleteVpnCustomerGateway',
          icon: 'delete-outlined',
          label: 'label.delete.vpn.customer.gateway',
          message: 'message.delete.vpn.customer.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#updating-and-removing-a-vpn-customer-gateway',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'tungstenfabric',
      title: 'label.tungsten.fabric',
      icon: shallowRef(tungsten),
      permission: ['listTungstenFabricProviders'],
      columns: [
        {
          field: 'name',
          customTitle: 'tungsten.fabric.provider'
        },
        'zonename'
      ],
      details: ['name', 'tungstengateway', 'tungstenproviderhostname', 'tungstenproviderintrospectport', 'tungstenproviderport', 'tungstenprovideruuid', 'tungstenprovidervrouterport', 'zonename'],
      resourceType: 'TungstenFabric',
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'tungsten.fabric',
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/TungstenFabric.vue'))),
          show: (record) => { return !record.securitygroupsenabled }
        }
      ]
    },
    {
      name: 'tungstenpolicy',
      title: 'label.network.policy',
      icon: shallowRef(tungsten),
      hidden: true,
      permission: ['listTungstenFabricPolicy'],
      columns: ['name', 'zonename'],
      details: ['name', 'zonename'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'rule',
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/TungstenFabricPolicyRule.vue')))
        },
        {
          name: 'tag',
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/TungstenFabricPolicyTag.vue')))
        }
      ],
      actions: [
        {
          api: 'deleteTungstenFabricPolicy',
          icon: 'delete-outlined',
          label: 'label.delete.tungsten.policy',
          message: 'label.confirm.delete.tungsten.policy',
          dataView: true,
          mapping: {
            policyuuid: {
              value: (record) => { return record.uuid }
            },
            zoneid: {
              value: (record) => { return record.zoneid }
            }
          }
        }
      ]
    },
    {
      name: 'tungstenpolicyset',
      title: 'label.application.policy.set',
      icon: shallowRef(tungsten),
      hidden: true,
      permission: ['listTungstenFabricApplicationPolicySet'],
      columns: ['name', 'zonename'],
      details: ['name', 'zonename'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'firewall.policy',
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/FirewallPolicyTab.vue')))
        },
        {
          name: 'tag',
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/FirewallTagTab.vue')))
        }
      ],
      actions: [
        {
          api: 'deleteTungstenFabricApplicationPolicySet',
          icon: 'delete-outlined',
          label: 'label.delete.tungsten.policy.set',
          message: 'label.confirm.delete.tungsten.policy.set',
          dataView: true,
          mapping: {
            applicationpolicysetuuid: {
              value: (record) => { return record.uuid }
            },
            zoneid: {
              value: (record) => { return record.zoneid }
            }
          }
        }
      ]
    },
    {
      name: 'tungstenfirewallpolicy',
      title: 'label.firewall.policy',
      icon: shallowRef(tungsten),
      hidden: true,
      permission: ['listTungstenFabricFirewallPolicy'],
      columns: ['name', 'zonename'],
      details: ['uuid', 'name', 'zonename'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'firewallrule',
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/tungsten/FirewallRuleTab.vue')))
        }
      ],
      actions: [
        {
          api: 'deleteTungstenFabricFirewallPolicy',
          icon: 'delete-outlined',
          label: 'label.delete.tungsten.firewall.policy',
          message: 'label.confirm.delete.tungsten.firewall.policy',
          dataView: true,
          mapping: {
            firewallpolicyuuid: {
              value: (record) => { return record.uuid }
            },
            zoneid: {
              value: (record) => { return record.zoneid }
            }
          }
        }
      ]
    },
    {
      name: 'guestvlans',
      title: 'label.guest.vlan',
      icon: 'folder-outlined',
      docHelp: 'conceptsandterminology/network_setup.html#vlan-allocation-example',
      permission: ['listGuestVlans'],
      resourceType: 'GuestVlan',
      filters: ['allocatedonly', 'all'],
      columns: ['vlan', 'allocationstate', 'physicalnetworkname', 'taken', 'account', 'project', 'domain', 'zonename', 'guest.networks'],
      details: ['vlan', 'allocationstate', 'physicalnetworkname', 'taken', 'account', 'project', 'domain', 'isdedicated', 'zonename'],
      searchFilters: ['zoneid'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'guest.networks',
        component: shallowRef(defineAsyncComponent(() => import('@/views/network/GuestVlanNetworksTab.vue'))),
        show: (record) => { return (record.allocationstate === 'Allocated') }
      }],
      show: () => {
        if (!store.getters.zones || store.getters.zones.length === 0) {
          return false
        }
        return true
      }
    },
    {
      name: 'ipv4subnets',
      title: 'label.ipv4.subnets',
      icon: 'pic-center-outlined',
      permission: ['listIpv4SubnetsForGuestNetwork'],
      columns: ['subnet', 'zonename', 'parentsubnet', 'networkname', 'vpcname', 'created', 'allocated'],
      details: ['subnet', 'zonename', 'zoneid', 'parentsubnet', 'networkname', 'networkid', 'vpcname', 'vpcid', 'created', 'allocated', 'state'],
      searchFilters: ['zoneid'],
      show: () => {
        if (!store.getters.zones || store.getters.zones.length === 0) {
          return false
        }
        return isAdmin()
      },
      actions: [
        {
          api: 'createIpv4SubnetForGuestNetwork',
          icon: 'plus-outlined',
          label: 'label.add.ipv4.subnet',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/CreateIpv4SubnetForNetwork.vue')))
        },
        {
          api: 'deleteIpv4SubnetForGuestNetwork',
          icon: 'delete-outlined',
          label: 'label.delete.ipv4.subnet',
          message: 'message.action.delete.ipv4.subnet',
          dataView: true,
          show: (record) => { return !record.networkid },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    }
  ]
}
