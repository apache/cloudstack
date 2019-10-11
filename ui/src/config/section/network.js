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
      actions: [
        {
          api: 'deleteNetwork',
          icon: 'delete',
          label: 'Delete Network',
          args: ['id'],
          listView: true,
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
      columns: ['name', 'state', 'displaytext', 'cidr', 'account', 'zonename']
    },
    {
      name: 'securitygroups',
      title: 'Security Groups',
      icon: 'fire',
      permission: [ 'listSecurityGroups' ],
      resourceType: 'SecurityGroup',
      columns: ['name', 'description', 'account', 'domain']
    },
    {
      name: 'publicip',
      title: 'Public IP Addresses',
      icon: 'environment',
      permission: [ 'listPublicIpAddresses' ],
      resourceType: 'PublicIpAddress',
      columns: ['ipaddress', 'state', 'associatednetworkname', 'virtualmachinename', 'allocated', 'account', 'zonename']
    },
    {
      name: 'vpngateway',
      title: 'VPN Gateway',
      icon: 'lock',
      permission: [ 'listVpnCustomerGateways' ],
      resourceType: 'VpnGateway',
      columns: ['name', 'ipaddress', 'gateway', 'cidrlist', 'ipsecpsk', 'account', 'domain']
    }
  ]
}
