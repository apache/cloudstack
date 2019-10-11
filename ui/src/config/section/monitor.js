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
      columns: ['username', 'description', 'state', 'level', 'type', 'account', 'domain', 'created'],
      details: ['username', 'id', 'description', 'state', 'level', 'type', 'account', 'domain', 'created']
    },
    {
      name: 'alert',
      title: 'Alerts',
      icon: 'flag',
      permission: [ 'listAlerts' ],
      columns: ['name', 'description', 'type', 'sent'],
      details: ['name', 'id', 'type', 'sent', 'description']
    }
  ]
}
