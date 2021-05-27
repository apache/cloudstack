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

import store from '@/store'

export default {
  name: 'network',
  title: 'label.network',
  icon: 'wifi',
  docHelp: 'adminguide/networking_and_traffic.html#advanced-zone-physical-network-configuration',
  children: [
    {
      name: 'guestnetwork',
      title: 'label.guest.networks',
      icon: 'apartment',
      permission: ['listNetworks'],
      resourceType: 'Network',
      columns: ['name', 'state', 'type', 'vpcname', 'cidr', 'ip6cidr', 'broadcasturi', 'domain', 'account', 'zonename'],
      details: ['name', 'id', 'description', 'type', 'traffictype', 'vpcid', 'vlan', 'broadcasturi', 'cidr', 'ip6cidr', 'netmask', 'gateway', 'aclname', 'ispersistent', 'restartrequired', 'reservediprange', 'redundantrouter', 'networkdomain', 'zonename', 'account', 'domain'],
      filters: ['all', 'isolated', 'shared', 'l2'],
      searchFilters: ['keyword', 'zoneid', 'domainid', 'account', 'tags'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'networkid'
      }],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'egress.rules',
        component: () => import('@/views/network/EgressRulesTab.vue'),
        show: (record) => { return record.type === 'Isolated' && !('vpcid' in record) && 'listEgressFirewallRules' in store.getters.apis }
      }, {
        name: 'public.ip.addresses',
        component: () => import('@/views/network/IpAddressesTab.vue'),
        show: (record) => { return (record.type === 'Isolated' || record.type === 'Shared') && !('vpcid' in record) && 'listPublicIpAddresses' in store.getters.apis }
      }, {
        name: 'virtual.routers',
        component: () => import('@/views/network/RoutersTab.vue'),
        show: (record) => { return (record.type === 'Isolated' || record.type === 'Shared') && 'listRouters' in store.getters.apis }
      }, {
        name: 'guest.ip.range',
        component: () => import('@/views/network/GuestIpRanges.vue'),
        show: (record) => { return 'listVlanIpRanges' in store.getters.apis && (record.type === 'Shared' || (record.service && record.service.filter(x => x.name === 'SourceNat').count === 0)) }
      }],
      actions: [
        {
          api: 'createNetwork',
          icon: 'plus',
          label: 'label.add.network',
          docHelp: 'adminguide/networking_and_traffic.html#configure-guest-traffic-in-an-advanced-zone',
          listView: true,
          popup: true,
          component: () => import('@/views/network/CreateNetwork.vue')
        },
        {
          api: 'updateNetwork',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: (record) => {
            var fields = ['name', 'displaytext', 'guestvmcidr']
            if (record.type === 'Isolated') {
              fields.push(...['networkofferingid', 'networkdomain'])
            }
            return fields
          }
        },
        {
          api: 'restartNetwork',
          icon: 'sync',
          label: 'label.restart.network',
          message: 'message.restart.network',
          dataView: true,
          args: ['cleanup'],
          show: (record) => record.type !== 'L2'
        },
        {
          api: 'replaceNetworkACLList',
          icon: 'swap',
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
          icon: 'delete',
          label: 'label.action.delete.network',
          message: 'message.action.delete.network',
          dataView: true
        }
      ]
    },
    {
      name: 'vpc',
      title: 'label.vpc',
      icon: 'deployment-unit',
      docHelp: 'adminguide/networking_and_traffic.html#configuring-a-virtual-private-cloud',
      permission: ['listVPCs'],
      resourceType: 'Vpc',
      columns: ['name', 'state', 'displaytext', 'cidr', 'account', 'zonename'],
      details: ['name', 'id', 'displaytext', 'cidr', 'networkdomain', 'ispersistent', 'redundantvpcrouter', 'restartrequired', 'zonename', 'account', 'domain'],
      searchFilters: ['name', 'zoneid', 'domainid', 'account', 'tags'],
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
        component: () => import('@/views/network/VpcTab.vue')
      }],
      actions: [
        {
          api: 'createVPC',
          icon: 'plus',
          label: 'label.add.vpc',
          docHelp: 'adminguide/networking_and_traffic.html#adding-a-virtual-private-cloud',
          listView: true,
          popup: true,
          component: () => import('@/views/network/CreateVpc.vue')
        },
        {
          api: 'updateVPC',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['name', 'displaytext']
        },
        {
          api: 'restartVPC',
          icon: 'sync',
          label: 'label.restart.vpc',
          message: (record) => { return record.redundantvpcrouter ? 'message.restart.vpc' : 'message.restart.vpc.remark' },
          dataView: true,
          args: (record) => {
            var fields = ['cleanup']
            if (!record.redundantvpcrouter) {
              fields.push('makeredundant')
            }
            return fields
          }
        },
        {
          api: 'deleteVPC',
          icon: 'delete',
          label: 'label.remove.vpc',
          message: 'message.remove.vpc',
          dataView: true
        }
      ]
    },
    {
      name: 'securitygroups',
      title: 'label.security.groups',
      icon: 'fire',
      docHelp: 'adminguide/networking_and_traffic.html#security-groups',
      permission: ['listSecurityGroups'],
      resourceType: 'SecurityGroup',
      columns: ['name', 'description', 'account', 'domain'],
      details: ['name', 'id', 'description', 'account', 'domain'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'ingress.rule',
        component: () => import('@/views/network/IngressEgressRuleConfigure.vue')
      }, {
        name: 'egress.rule',
        component: () => import('@/views/network/IngressEgressRuleConfigure.vue')
      }],
      show: () => {
        if (!store.getters.zones || store.getters.zones.length === 0) {
          return false
        }
        const listZoneHaveSGEnabled = store.getters.zones.filter(zone => zone.securitygroupsenabled === true)
        if (!listZoneHaveSGEnabled || listZoneHaveSGEnabled.length === 0) {
          return false
        }
        return true
      },
      actions: [
        {
          api: 'createSecurityGroup',
          icon: 'plus',
          label: 'label.add.security.group',
          docHelp: 'adminguide/networking_and_traffic.html#adding-a-security-group',
          listView: true,
          args: ['name', 'description']
        },
        {
          api: 'updateSecurityGroup',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['name'],
          show: (record) => { return record.name !== 'default' }
        },
        {
          api: 'deleteSecurityGroup',
          icon: 'delete',
          label: 'label.action.delete.security.group',
          message: 'message.action.delete.security.group',
          dataView: true,
          show: (record) => { return record.name !== 'default' }
        }
      ]
    },
    {
      name: 'publicip',
      title: 'label.public.ip.addresses',
      icon: 'environment',
      docHelp: 'adminguide/networking_and_traffic.html#reserving-public-ip-addresses-and-vlans-for-accounts',
      permission: ['listPublicIpAddresses'],
      resourceType: 'PublicIpAddress',
      columns: ['ipaddress', 'state', 'associatednetworkname', 'virtualmachinename', 'allocated', 'account', 'zonename'],
      details: ['ipaddress', 'id', 'associatednetworkname', 'virtualmachinename', 'networkid', 'issourcenat', 'isstaticnat', 'virtualmachinename', 'vmipaddress', 'vlan', 'allocated', 'account', 'zonename'],
      component: () => import('@/views/network/PublicIpResource.vue'),
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'firewall',
        component: () => import('@/views/network/FirewallRules.vue'),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'Firewall').length > 0
      },
      {
        name: 'portforwarding',
        component: () => import('@/views/network/PortForwarding.vue'),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'PortForwarding').length > 0
      }, {
        name: 'loadbalancing',
        component: () => import('@/views/network/LoadBalancing.vue'),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'Lb').length > 0
      }, {
        name: 'vpn',
        component: () => import('@/views/network/VpnDetails.vue'),
        show: (record) => { return record.issourcenat }
      }],
      actions: [
        {
          api: 'enableStaticNat',
          icon: 'plus-circle',
          label: 'label.action.enable.static.nat',
          docHelp: 'adminguide/networking_and_traffic.html#enabling-or-disabling-static-nat',
          dataView: true,
          show: (record) => { return !record.virtualmachineid && !record.issourcenat },
          popup: true,
          component: () => import('@/views/network/EnableStaticNat.vue')
        },
        {
          api: 'disableStaticNat',
          icon: 'minus-circle',
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
          icon: 'delete',
          label: 'label.action.release.ip',
          message: 'message.action.release.ip',
          docHelp: 'adminguide/networking_and_traffic.html#releasing-an-ip-address-alloted-to-a-vpc',
          dataView: true,
          show: (record) => { return !record.issourcenat }
        }
      ]
    },
    {
      name: 'privategw',
      title: 'label.private.gateway',
      icon: 'gateway',
      hidden: true,
      permission: ['listPrivateGateways'],
      columns: ['ipaddress', 'state', 'gateway', 'netmask', 'account'],
      details: ['ipaddress', 'gateway', 'netmask', 'vlan', 'sourcenatsupported', 'aclname', 'account', 'domain', 'zone'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'static.routes',
        component: () => import('@/views/network/StaticRoutesTab.vue'),
        show: () => true
      }],
      actions: [
        {
          api: 'createPrivateGateway',
          icon: 'plus',
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
          icon: 'swap',
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
          icon: 'delete',
          label: 'label.delete.gateway',
          message: 'message.delete.gateway',
          dataView: true
        }
      ]
    },
    {
      name: 's2svpn',
      title: 'label.site.to.site.vpn',
      icon: 'lock',
      hidden: true,
      permission: ['listVpnGateways'],
      columns: ['publicip', 'account', 'domain'],
      details: ['publicip', 'account', 'domain'],
      actions: [
        {
          api: 'createVpnGateway',
          icon: 'plus',
          label: 'label.add.vpn.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#creating-a-vpn-gateway-for-the-vpc',
          listView: true,
          args: ['vpcid']
        },
        {
          api: 'deleteVpnGateway',
          icon: 'delete',
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
      icon: 'sync',
      hidden: true,
      permission: ['listVpnConnections'],
      columns: ['publicip', 'state', 'gateway', 'ipsecpsk', 'ikepolicy', 'esppolicy'],
      details: ['publicip', 'gateway', 'passive', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'esppolicy', 'ikelifetime', 'ikeversion', 'esplifetime', 'dpd', 'splitconnections', 'forceencap', 'created'],
      actions: [
        {
          api: 'createVpnConnection',
          icon: 'plus',
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
          icon: 'reload',
          label: 'label.reset.vpn.connection',
          message: 'message.reset.vpn.connection',
          docHelp: 'adminguide/networking_and_traffic.html#restarting-and-removing-a-vpn-connection',
          dataView: true
        },
        {
          api: 'deleteVpnConnection',
          icon: 'delete',
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
      icon: 'bars',
      docHelp: 'adminguide/networking_and_traffic.html#configuring-network-access-control-list',
      hidden: true,
      permission: ['listNetworkACLLists'],
      columns: ['name', 'description', 'id'],
      details: ['name', 'description', 'id'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'acl.list.rules',
        component: () => import('@/views/network/AclListRulesTab.vue'),
        show: () => true
      }],
      actions: [
        {
          api: 'createNetworkACLList',
          icon: 'plus',
          label: 'label.add.acl.list',
          docHelp: 'adminguide/networking_and_traffic.html#creating-acl-lists',
          listView: true,
          args: ['name', 'description', 'vpcid']
        },
        {
          api: 'updateNetworkACLList',
          icon: 'edit',
          label: 'label.edit.acl.list',
          dataView: true,
          args: ['name', 'description']
        },
        {
          api: 'deleteNetworkACLList',
          icon: 'delete',
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
      icon: 'share-alt',
      hidden: true,
      permission: ['listLoadBalancers'],
      columns: ['name', 'sourceipaddress', 'loadbalancerrule', 'algorithm', 'account', 'domain'],
      details: ['name', 'sourceipaddress', 'loadbalancerrule', 'algorithm', 'account', 'domain'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'loadbalancerinstance',
        component: () => import('@/views/network/InternalLBAssignedVmTab.vue'),
        show: () => true
      }],
      actions: [
        {
          api: 'createLoadBalancer',
          icon: 'plus',
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
          icon: 'plus',
          label: 'label.assign.vms',
          dataView: true,
          popup: true,
          component: () => import('@/views/network/InternalLBAssignVmForm.vue')
        },
        {
          api: 'deleteLoadBalancer',
          icon: 'delete',
          label: 'label.delete.internal.lb',
          message: 'message.confirm.delete.internal.lb',
          dataView: true
        }
      ]
    },
    {
      name: 'vpnuser',
      title: 'label.vpn.users',
      icon: 'user',
      permission: ['listVpnUsers'],
      hidden: true,
      columns: ['username', 'state', 'account', 'domain'],
      details: ['username', 'state', 'account', 'domain'],
      actions: [
        {
          api: 'addVpnUser',
          icon: 'plus',
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
          icon: 'delete',
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
          }
        }
      ]
    },
    {
      name: 'vpncustomergateway',
      title: 'label.vpncustomergatewayid',
      icon: 'lock',
      permission: ['listVpnCustomerGateways'],
      columns: ['name', 'gateway', 'cidrlist', 'ipsecpsk', 'account'],
      details: ['name', 'id', 'gateway', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'ikelifetime', 'ikeversion', 'esppolicy', 'esplifetime', 'dpd', 'splitconnections', 'forceencap', 'account', 'domain'],
      searchFilters: ['keyword', 'domainid', 'account'],
      actions: [
        {
          api: 'createVpnCustomerGateway',
          icon: 'plus',
          label: 'label.add.vpn.customer.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#creating-and-updating-a-vpn-customer-gateway',
          listView: true,
          popup: true,
          component: () => import('@/views/network/CreateVpnCustomerGateway.vue')
        },
        {
          api: 'updateVpnCustomerGateway',
          icon: 'edit',
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
          icon: 'delete',
          label: 'label.delete.vpn.customer.gateway',
          message: 'message.delete.vpn.customer.gateway',
          docHelp: 'adminguide/networking_and_traffic.html#updating-and-removing-a-vpn-customer-gateway',
          dataView: true
        }
      ]
    }
  ]
}
