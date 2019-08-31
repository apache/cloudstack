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
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'type', 'cidr', 'ip6cidr', 'broadcasturi', 'account', 'zonename'],
      actions: [
        {
          api: 'deleteNetwork',
          icon: 'delete',
          label: 'Delete Network',
          params: ['id'],
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
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'displaytext', 'cidr', 'account', 'zonename']
    },
    {
      name: 'securitygroups',
      title: 'Security Groups',
      icon: 'fire',
      permission: [ 'listSecurityGroups' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'description', 'account', 'domain']
    },
    {
      name: 'publicip',
      title: 'Public IP Addresses',
      icon: 'environment',
      permission: [ 'listPublicIpAddresses' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['ipaddress', 'state', 'associatednetworkname', 'virtualmachinename', 'allocated', 'account', 'zonename']
    },
    {
      name: 'vpngateway',
      title: 'VPN Gateway',
      icon: 'lock',
      permission: [ 'listVpnCustomerGateways' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'ipaddress', 'gateway', 'cidrlist', 'ipsecpsk', 'account', 'domain']
    }
  ]
}
