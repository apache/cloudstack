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
        name: 'publicip',
        title: 'IP Addresses',
        param: 'associatednetworkid'
      }, {
        name: 'router',
        title: 'Routers',
        param: 'networkid'
      }, {
        name: 'vm',
        title: 'Instances',
        param: 'networkid'
      }],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'egress-rules',
        component: () => import('@/views/network/EgressConfigure.vue')
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
      }],
      tabs: [{
        name: 'configure',
        component: () => import('@/views/network/VpcConfigure.vue')
      }, {
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
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
        name: 'ingress-rules',
        component: () => import('@/views/network/IngressRuleConfigure.vue')
      }, {
        name: 'egress-rules',
        component: () => import('@/views/network/EgressRuleConfigure.vue')
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
        name: 'configure',
        component: () => import('@/views/network/IpConfigure.vue')
      }, {
        name: 'vpn',
        component: () => import('@/views/network/VpnDetails.vue')
      }, {
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
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
          api: 'createRemoteAccessVpn',
          icon: 'link',
          label: 'Enable Remote Access VPN',
          dataView: true,
          args: ['publicipid', 'domainid', 'account'],
          mapping: {
            publicipid: {
              value: (record) => { return record.id }
            },
            domainid: {
              value: (record) => { return record.domainid }
            },
            account: {
              value: (record) => { return record.account }
            }
          }
        },
        {
          api: 'deleteRemoteAccessVpn',
          icon: 'disconnect',
          label: 'Disable Remove Access VPN',
          dataView: true,
          args: ['publicipid', 'domainid'],
          mapping: {
            publicipid: {
              value: (record) => { return record.id }
            },
            domainid: {
              value: (record) => { return record.domainid }
            }
          }
        },
        {
          api: 'enableStaticNat',
          icon: 'plus-circle',
          label: 'Enable Static NAT',
          dataView: true,
          show: (record) => { return !record.virtualmachineid && !record.issourcenat },
          args: ['ipaddressid', 'virtualmachineid', 'vmguestip'],
          mapping: {
            ipaddressid: {
              value: (record) => { return record.id }
            }
          }
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
          label: 'Delete IP',
          dataView: true,
          show: (record) => { return !record.issourcenat }
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
      name: 'vpngateway',
      title: 'VPN Gateway',
      icon: 'lock',
      permission: ['listVpnCustomerGateways'],
      resourceType: 'VpnGateway',
      columns: ['name', 'ipaddress', 'gateway', 'cidrlist', 'ipsecpsk', 'account', 'domain'],
      details: ['name', 'id', 'ipaddress', 'gateway', 'cidrlist', 'ipsecpsk', 'account', 'domain'],
      actions: [
        {
          api: 'createVpnCustomerGateway',
          icon: 'plus',
          label: 'Add VPN Customer Gateway',
          listView: true,
          args: ['name', 'gateway', 'cidrlist', 'ipsecpsk', 'ikelifetime', 'esplifetime', 'dpd', 'forceencap', 'ikepolicy', 'esppolicy']
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
