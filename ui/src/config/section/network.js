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
  title: 'Network',
  icon: 'wifi',
  children: [
    {
      name: 'guestnetwork',
      title: 'Guest Networks',
      icon: 'gateway',
      permission: ['listNetworks'],
      resourceType: 'Network',
      columns: ['name', 'state', 'type', 'cidr', 'ip6cidr', 'broadcasturi', 'account', 'zonename'],
      details: ['name', 'id', 'description', 'type', 'traffictype', 'vpcid', 'vlan', 'broadcasturi', 'cidr', 'ip6cidr', 'netmask', 'gateway', 'ispersistent', 'restartrequired', 'reservediprange', 'redundantrouter', 'networkdomain', 'zonename', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'networkid'
      }],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'Egress Rules',
        component: () => import('@/views/network/EgressConfigure.vue'),
        show: (record) => { return record.type === 'Isolated' && 'listEgressFirewallRules' in store.getters.apis }
      }, {
        name: 'Public IP Addresses',
        component: () => import('@/views/network/IpAddressesTab.vue'),
        show: (record) => { return record.type === 'Isolated' && 'listPublicIpAddresses' in store.getters.apis }
      }, {
        name: 'Virtual Routers',
        component: () => import('@/views/network/RoutersTab.vue'),
        show: (record) => { return (record.type === 'Isolated' || record.type === 'Shared') && 'listRouters' in store.getters.apis }
      }],
      actions: [
        {
          api: 'createNetwork',
          icon: 'plus',
          label: 'Add Network',
          listView: true,
          popup: true,
          component: () => import('@/views/network/CreateNetwork.vue')
        },
        {
          api: 'associateIpAddress',
          icon: 'plus',
          label: 'Acquire New IP',
          dataView: true,
          show: (record) => { return record && record.service && record.service.filter(x => x.name && ['StaticNat', 'SourceNat', 'Firewall', 'PortForwarding', 'Lb'].includes(x.name)).length > 0 },
          args: ['networkid'],
          mapping: {
            networkid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'updateNetwork',
          icon: 'edit',
          label: 'Update Network',
          dataView: true,
          args: ['name', 'displaytext', 'guestvmcidr']
        },
        {
          api: 'restartNetwork',
          icon: 'sync',
          label: 'Restart Network',
          dataView: true,
          args: ['makeredundant', 'cleanup']
        },
        {
          api: 'replaceNetworkACLList',
          icon: 'swap',
          label: 'Replace ACL List',
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
          label: 'Delete Network',
          dataView: true
        }
      ]
    },
    {
      name: 'vpc',
      title: 'VPC',
      icon: 'deployment-unit',
      permission: ['listVPCs'],
      resourceType: 'Vpc',
      columns: ['name', 'state', 'displaytext', 'cidr', 'account', 'zonename'],
      details: ['name', 'id', 'displaytext', 'cidr', 'networkdomain', 'ispersistent', 'redundantvpcrouter', 'restartrequired', 'zonename', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'vpcid'
      }, {
        name: 'router',
        title: 'Virtual Routers',
        param: 'vpcid'
      }, {
        name: 'ilbvm',
        title: 'Internal LB VMs',
        param: 'vpcid'
      }],
      tabs: [{
        name: 'VPC',
        component: () => import('@/views/network/VpcTab.vue')
      }],
      actions: [
        {
          api: 'createVPC',
          icon: 'plus',
          label: 'Add VPC',
          listView: true,
          args: ['name', 'displaytext', 'zoneid', 'cidr', 'networkdomain', 'vpcofferingid', 'start']
        },
        {
          api: 'updateVPC',
          icon: 'edit',
          label: 'Update VPC',
          dataView: true,
          args: ['name', 'displaytext']
        },
        {
          api: 'restartVPC',
          icon: 'sync',
          label: 'Restart VPC',
          dataView: true,
          args: ['makeredundant', 'cleanup']
        },
        {
          api: 'deleteVPC',
          icon: 'delete',
          label: 'Delete VPC',
          dataView: true
        }
      ]
    },
    {
      name: 'securitygroups',
      title: 'Security Groups',
      icon: 'fire',
      permission: ['listSecurityGroups'],
      resourceType: 'SecurityGroup',
      columns: ['name', 'description', 'account', 'domain'],
      details: ['name', 'id', 'description', 'account', 'domain'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'Ingress Rule',
        component: () => import('@/views/network/IngressEgressRuleConfigure.vue')
      }, {
        name: 'Egress Rule',
        component: () => import('@/views/network/IngressEgressRuleConfigure.vue')
      }],
      actions: [
        {
          api: 'createSecurityGroup',
          icon: 'plus',
          label: 'Add Security Group',
          listView: true,
          args: ['name', 'description']
        },
        {
          api: 'deleteSecurityGroup',
          icon: 'delete',
          label: 'Delete Security Group',
          dataView: true,
          show: (record) => { return record.name !== 'default' }
        }
      ]
    },
    {
      name: 'publicip',
      title: 'Public IP Addresses',
      icon: 'environment',
      permission: ['listPublicIpAddresses'],
      resourceType: 'PublicIpAddress',
      columns: ['ipaddress', 'state', 'associatednetworkname', 'virtualmachinename', 'allocated', 'account', 'zonename'],
      details: ['ipaddress', 'id', 'associatednetworkname', 'virtualmachinename', 'networkid', 'issourcenat', 'isstaticnat', 'virtualmachinename', 'vmipaddress', 'vlan', 'allocated', 'account', 'zonename'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'Firewall',
        component: () => import('@/views/network/FirewallRules.vue'),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'Firewall').length > 0
      }, {
        name: 'Port Forwarding',
        component: () => import('@/views/network/PortForwarding.vue'),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'PortForwarding').length > 0
      }, {
        name: 'Load Balancing',
        component: () => import('@/views/network/LoadBalancing.vue'),
        networkServiceFilter: networkService => networkService.filter(x => x.name === 'Lb').length > 0
      }, {
        name: 'VPN',
        component: () => import('@/views/network/VpnDetails.vue')
      }],
      actions: [
        {
          api: 'associateIpAddress',
          icon: 'plus',
          label: 'Acquire New IP',
          listView: true,
          args: ['networkid']
        },
        {
          api: 'enableStaticNat',
          icon: 'plus-circle',
          label: 'Enable Static NAT',
          dataView: true,
          show: (record) => { return !record.virtualmachineid && !record.issourcenat },
          popup: true,
          component: () => import('@/views/network/EnableStaticNat.vue')
        },
        {
          api: 'disableStaticNat',
          icon: 'minus-circle',
          label: 'Disable Static NAT',
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
          label: 'Release IP',
          dataView: true,
          show: (record) => { return !record.issourcenat }
        }
      ]
    },
    {
      name: 'privategw',
      title: 'Private Gateway',
      icon: 'branches',
      hidden: true,
      permission: ['listPrivateGateways'],
      columns: ['ipaddress', 'state', 'gateway', 'netmask', 'account', 'domain'],
      details: ['ipaddress', 'gateway', 'netmask', 'vlan', 'sourcenatsupported', 'aclid', 'account', 'domain', 'zone'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'Static Routes',
        component: () => import('@/views/network/StaticRoutesTab.vue'),
        show: () => true
      }],
      actions: [
        {
          api: 'createPrivateGateway',
          icon: 'plus',
          label: 'Add Private Gateway',
          listView: true,
          args: ['physicalnetworkid', 'vlan', 'ipaddress', 'gateway', 'netmask', 'sourcenatsupported', 'aclid'],
          mapping: {
            aclid: {
              api: 'listNetworkACLLists'
            }
          }
        },
        {
          api: 'replaceNetworkACLList',
          icon: 'swap',
          label: 'Replace ACL List',
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
          label: 'Delete Private Gateway',
          dataView: true
        }
      ]
    },
    {
      name: 's2svpn',
      title: 'Site-to-Site VPNs',
      icon: 'lock',
      hidden: true,
      permission: ['listVpnGateways'],
      columns: ['publicip', 'account', 'domain'],
      details: ['publicip', 'account', 'domain'],
      actions: [
        {
          api: 'createVpnGateway',
          icon: 'plus',
          label: 'Create VPN Gateway',
          listView: true,
          args: ['vpcid']
        },
        {
          api: 'deleteVpnGateway',
          icon: 'delete',
          label: 'Delete VPN Gateway',
          dataView: true
        }
      ]
    },
    {
      name: 's2svpnconn',
      title: 'Site-to-Site VPN Connections',
      icon: 'sync',
      hidden: true,
      permission: ['listVpnConnections'],
      columns: ['publicip', 'state', 'gateway', 'ipsecpsk', 'ikepolicy', 'esppolicy'],
      details: ['publicip', 'gateway', 'passive', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'esppolicy', 'ikelifetime', 'esplifetime', 'dpd', 'forceencap', 'created'],
      actions: [
        {
          api: 'createVpnConnection',
          icon: 'plus',
          label: 'Create VPN Connection',
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
          label: 'Reset VPN Connection',
          dataView: true
        },
        {
          api: 'deleteVpnConnection',
          icon: 'delete',
          label: 'Delete VPN Connection',
          dataView: true
        }
      ]
    },
    {
      name: 'acllist',
      title: 'Network ACL Lists',
      icon: 'bars',
      hidden: true,
      permission: ['listNetworkACLLists'],
      columns: ['name', 'description', 'id'],
      details: ['name', 'description', 'id'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'ACL List Rules',
        component: () => import('@/views/network/AclListRulesTab.vue'),
        show: () => true
      }],
      actions: [
        {
          api: 'createNetworkACLList',
          icon: 'plus',
          label: 'Add ACL List',
          listView: true,
          args: ['name', 'description', 'vpcid']
        },
        {
          api: 'updateNetworkACLList',
          icon: 'edit',
          label: 'Edit ACL List',
          dataView: true,
          args: ['name', 'description']
        },
        {
          api: 'deleteNetworkACLList',
          icon: 'delete',
          label: 'Delete ACL List',
          dataView: true
        }
      ]
    },
    {
      name: 'ilb',
      title: 'Internal LB',
      icon: 'share-alt',
      hidden: true,
      permission: ['listLoadBalancers'],
      columns: ['name', 'sourceipaddress', 'loadbalancerrule', 'algorithm', 'account', 'domain'],
      details: ['name', 'sourceipaddress', 'loadbalancerrule', 'algorithm', 'account', 'domain'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'Assigned VMs',
        component: () => import('@/views/network/InternalLBAssignedVmTab.vue'),
        show: () => true
      }],
      actions: [
        {
          api: 'createLoadBalancer',
          icon: 'plus',
          label: 'Add Internal LB',
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
          label: 'Assign VMs',
          dataView: true,
          popup: true,
          component: () => import('@/views/network/InternalLBAssignVmForm.vue')
        },
        {
          api: 'deleteLoadBalancer',
          icon: 'delete',
          label: 'Delete LB',
          dataView: true
        }
      ]
    },
    {
      name: 'vpnuser',
      title: 'VPN Users',
      icon: 'user',
      permission: ['listVpnUsers'],
      columns: ['username', 'state', 'account', 'domain'],
      details: ['username', 'state', 'account', 'domain'],
      actions: [
        {
          api: 'addVpnUser',
          icon: 'plus',
          label: 'Add VPN User',
          listView: true,
          args: ['username', 'password', 'domainid', 'account']
        },
        {
          api: 'removeVpnUser',
          icon: 'delete',
          label: 'Delete VPN User',
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
      title: 'VPN Customer Gateway',
      icon: 'lock',
      permission: ['listVpnCustomerGateways'],
      columns: ['name', 'gateway', 'cidrlist', 'ipsecpsk', 'account', 'domain'],
      details: ['name', 'id', 'gateway', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'ikelifetime', 'esppolicy', 'esplifetime', 'dpd', 'forceencap', 'account', 'domain'],
      actions: [
        {
          api: 'createVpnCustomerGateway',
          icon: 'plus',
          label: 'Add VPN Customer Gateway',
          listView: true,
          popup: true,
          component: () => import('@/views/network/CreateVpnCustomerGateway.vue')
        },
        {
          api: 'updateVpnCustomerGateway',
          icon: 'edit',
          label: 'Edit VPN Customer Gateway',
          dataView: true,
          args: ['name', 'gateway', 'cidrlist', 'ipsecpsk', 'ikepolicy', 'ikelifetime', 'esppolicy', 'esplifetime', 'dpd', 'forceencap']
        },
        {
          api: 'deleteVpnCustomerGateway',
          icon: 'delete',
          label: 'Delete VPN Customer Gateway',
          dataView: true
        }
      ]
    }
  ]
}
