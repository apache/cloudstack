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
      columns: ['description', 'state', 'level', 'type', 'account', 'domain', 'created']
    },
    {
      name: 'alert',
      title: 'Alerts',
      icon: 'flag',
      permission: [ 'listAlerts' ],
      columns: ['description', 'type', 'sent']
    }
  ]
}
