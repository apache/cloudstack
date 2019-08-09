export default {
  name: 'monitor',
  title: 'Monitor',
  icon: 'compass',
  permission: [ 'listEvents', 'listAlerts' ],
  children: [
    {
      name: 'event',
      title: 'Events',
      icon: 'schedule',
      permission: [ 'listEvents' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'alert',
      title: 'Alerts',
      icon: 'flag',
      permission: [ 'listAlerts' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    }
  ]
}
