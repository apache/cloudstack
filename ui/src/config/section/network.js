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
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'vpc',
      title: 'VPCs',
      icon: 'deployment-unit',
      permission: [ 'listVPCs' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'securitygroups',
      title: 'Security Groups',
      icon: 'fire',
      permission: [ 'listSecurityGroups' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'vpngateway',
      title: 'VPN Gateways',
      icon: 'lock',
      permission: [ 'listVpnCustomerGateways' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    }
  ]
}
