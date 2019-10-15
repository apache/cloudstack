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
      details: ['username', 'id', 'description', 'state', 'level', 'type', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'archiveEvents',
          icon: 'book',
          label: 'Archive Event',
          dataView: true,
          args: ['ids']
        },
        {
          api: 'deleteEvents',
          icon: 'delete',
          label: 'Delete Event',
          dataView: true,
          args: ['ids']
        }
      ]
    },
    {
      name: 'alert',
      title: 'Alerts',
      icon: 'flag',
      permission: [ 'listAlerts' ],
      columns: ['name', 'description', 'type', 'sent'],
      details: ['name', 'id', 'type', 'sent', 'description'],
      actions: [
        {
          api: 'archiveAlerts',
          icon: 'book',
          label: 'Archive Alert',
          dataView: true,
          args: ['ids']
        },
        {
          api: 'deleteAlerts',
          icon: 'delete',
          label: 'Delete Alert',
          dataView: true,
          args: ['ids']
        }
      ]
    }
  ]
}
