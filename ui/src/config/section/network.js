export default {
  name: 'network',
  title: 'Network',
  icon: 'wifi',
  children: [
    {
      name: 'guestnetwork',
      title: 'Guest Networks',
      icon: 'gateway',
      permission: [ 'listNetworks' ],
      resourceType: 'Network',
      columns: ['name', 'state', 'type', 'cidr', 'ip6cidr', 'broadcasturi', 'account', 'zonename'],
      details: ['name', 'id', 'description', 'type', 'traffictype', 'vpcid', 'vlan', 'broadcasturi', 'cidr', 'ip6cidr', 'netmask', 'gateway', 'ispersistent', 'restartrequired', 'reservediprange', 'redundantrouter', 'networkdomain', 'zonename', 'account', 'domain'],
      related: [{
        name: 'publicip',
        title: 'IP Addresses',
        param: 'associatedNetworkId'
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
          args: ['id', 'name', 'displaytext', 'guestvmcidr']
        },
        {
          api: 'restartNetwork',
          icon: 'sync',
          label: 'Restart Network',
          dataView: true,
          args: ['id', 'makeredundant', 'cleanup']
        },
        {
          api: 'deleteNetwork',
          icon: 'delete',
          label: 'Delete Network',
          args: ['id'],
          dataView: true
        }
      ]
    },
    {
      name: 'vpc',
      title: 'VPC',
      icon: 'deployment-unit',
      permission: [ 'listVPCs' ],
      resourceType: 'Vpc',
      columns: ['name', 'state', 'displaytext', 'cidr', 'account', 'zonename'],
      details: ['name', 'id', 'displaytext', 'cidr', 'networkdomain', 'ispersistent', 'redundantvpcrouter', 'restartrequired', 'zonename', 'account', 'domain'],
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
          args: ['id', 'name', 'displaytext']
        },
        {
          api: 'restartVPC',
          icon: 'sync',
          label: 'Restart VPC',
          dataView: true,
          args: ['id', 'makeredundant', 'cleanup']
        },
        {
          api: 'deleteVPC',
          icon: 'delete',
          label: 'Delete VPC',
          args: ['id'],
          dataView: true
        }
      ]
    },
    {
      name: 'securitygroups',
      title: 'Security Groups',
      icon: 'fire',
      permission: [ 'listSecurityGroups' ],
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
          args: ['id'],
          dataView: true,
          show: (record) => { return record.name !== 'default' }
        }
      ]
    },
    {
      name: 'publicip',
      title: 'Public IP Addresses',
      icon: 'environment',
      permission: [ 'listPublicIpAddresses' ],
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
          args: ['publicipid', 'domainid', 'account']
        },
        {
          api: 'deleteRemoteAccessVpn',
          icon: 'disconnect',
          label: 'Disable Remove Access VPN',
          dataView: true,
          args: ['publicipid', 'domainid']
        },
        {
          api: 'enableStaticNat',
          icon: 'plus-circle',
          label: 'Enable Static NAT',
          dataView: true,
          args: ['ipaddressid', 'virtualmachineid', 'vmguestip'],
          show: (record) => { return !record.virtualmachineid && !record.issourcenat }
        },
        {
          api: 'disableStaticNat',
          icon: 'minus-circle',
          label: 'Disable Static NAT',
          dataView: true,
          args: ['ipaddressid'],
          show: (record) => { return record.virtualmachineid }
        },
        {
          api: 'disassociateIpAddress',
          icon: 'delete',
          label: 'Delete IP',
          args: ['id'],
          dataView: true,
          show: (record) => { return !record.issourcenat }
        }
      ]
    },
    {
      name: 'vpnuser',
      title: 'VPN Users',
      icon: 'user',
      permission: [ 'listVpnUsers' ],
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
          args: ['username', 'domainid', 'account']
        }
      ]
    },
    {
      name: 'vpngateway',
      title: 'VPN Gateway',
      icon: 'lock',
      permission: [ 'listVpnCustomerGateways' ],
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
        }
      ]
    }
  ]
}
