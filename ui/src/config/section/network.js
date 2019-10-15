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
          args: ['ipaddressid', 'virtualmachineid', 'vmguestip'],
          show: (record) => { return !record.virtualmachineid }
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
