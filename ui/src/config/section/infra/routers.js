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
      icon: 'startvm',
      label: 'label.action.start.router',
      dataView: true,
      hidden: (record) => { return record.state !== 'Stopped' },
      args: [
        'id'
      ]
    },
    {
      api: 'stopRouter',
      icon: 'stopvm',
      label: 'label.action.stop.router',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'id', 'forced'
      ]
    },
    {
      api: 'scaleSystemVm',
      icon: 'scalevm',
      label: 'label.change.service.offering',
      dataView: true,
      hidden: (record) => { return !(record.state === 'Stopped' || record.hypervisor === 'VMWare') },
      args: [
        'id', 'serviceofferingid'
      ]
    },
    {
      api: 'rebootRouter',
      icon: 'rebootvm',
      label: 'label.action.reboot.router',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'id'
      ]
    },
    {
      api: 'migrateSystemVm',
      icon: 'migratevm',
      label: 'label.action.migrate.router',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'hostid', 'virtualmachineid'
      ]
    },
    {
      api: 'upgradeRouterTemplate',
      icon: 'upgraderouter',
      label: 'label.upgrade.router.newer.template',
      dataView: true,
      groupAction: true,
      hidden: (record) => { return record.requiresupgrade },
      args: [
        'id'
      ]
    },
    {
      api: 'runDiagnostics',
      icon: 'diagnostics',
      label: 'label.action.run.diagnostics',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'targetid', 'ipaddress', 'type', 'params'
      ]
    },
    {
      api: 'destroyRouter',
      icon: 'delete',
      label: 'label.destroy.router',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' && record.state !== 'Stopped' },
      args: [
        'id'
      ]
    }
  ]
}
