export default {
  name: 'router',
  title: 'Virtual Routers',
  icon: 'fork',
  permission: [ 'listRouters' ],
  columns: [ 'name', 'state', 'publicip', 'guestnetworkname', 'vpcname', 'redundantstate', 'version', 'hostname', 'account', 'zonename', 'requiresupgrade' ],
  details: [ 'name', 'id', 'version', 'requiresupgrade', 'guestnetworkname', 'vpcname', 'publicip', 'guestipaddress', 'linklocalip', 'serviceofferingname', 'networkdomain', 'isredundantrouter', 'redundantstate', 'hostname', 'account', 'zonename', 'created' ],
  actions: [
    {
      api: 'startRouter',
      icon: 'caret-right',
      label: 'label.action.start.router',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.state === 'Stopped' }
    },
    {
      api: 'stopRouter',
      icon: 'stop',
      label: 'label.action.stop.router',
      dataView: true,
      args: ['id', 'forced'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'rebootRouter',
      icon: 'sync',
      label: 'label.action.reboot.router',
      dataView: true,
      args: ['id'],
      hidden: (record) => { return record.state === 'Running' }
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt',
      label: 'label.change.service.offering',
      dataView: true,
      args: ['id', 'serviceofferingid'],
      show: (record) => { return record.state === 'Stopped' || record.hypervisor === 'VMWare' }
    },
    {
      api: 'upgradeRouterTemplate',
      icon: 'fullscreen',
      label: 'label.upgrade.router.newer.template',
      dataView: true,
      groupAction: true,
      args: ['id'],
      show: (record) => { return record.requiresupgrade }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag',
      label: 'label.action.migrate.router',
      dataView: true,
      args: ['virtualmachineid', 'hostid'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'runDiagnostics',
      icon: 'reconciliation',
      label: 'label.action.run.diagnostics',
      dataView: true,
      args: ['targetid', 'type', 'ipaddress', 'params'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'destroyRouter',
      icon: 'delete',
      label: 'label.destroy.router',
      dataView: true,
      args: ['id'],
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) }
    }
  ]
}
