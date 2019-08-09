export default {
  name: 'offering',
  title: 'Offerings',
  icon: 'shopping',
  permission: [ 'listServiceOfferings'],
  children: [
    {
      name: 'computeoffering',
      title: 'Compute Offerings',
      icon: 'cloud',
      permission: [ 'listServiceOfferings' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'diskoffering',
      title: 'Disk Offerings',
      icon: 'hdd',
      permission: [ 'listDiskOfferings' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'networkoffering',
      title: 'Network Offerings',
      icon: 'wifi',
      permission: [ 'listNetworkOfferings' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'vpcoffering',
      title: 'VPC Offerings',
      icon: 'deployment-unit',
      permission: [ 'listVPCOfferings' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'systemoffering',
      title: 'System Offerings',
      icon: 'setting',
      permission: [ 'listServiceOfferings' ],
      params: {'issystem': 'true'},
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    }
  ]
}
