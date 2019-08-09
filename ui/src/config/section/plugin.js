export default {
  name: 'plugin',
  title: 'Plugins',
  icon: 'heat-map',
  permission: [ 'quotaSummary', 'cloudianSsoLogin' ],
  children: [
    {
      name: 'quota',
      title: 'Quota',
      icon: 'pie-chart',
      permission: [ 'quotaSummary', 'quotaIsEnabled' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'cloudian',
      title: 'Cloudian Storage',
      icon: 'cloud-download',
      permission: [ 'cloudianSsoLogin', 'cloudianIsEnabled' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    }
  ]
}
