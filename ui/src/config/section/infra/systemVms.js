export default {
  name: 'systemvm',
  title: 'System VMs',
  icon: 'thunderbolt',
  permission: [ 'listSystemVms' ],
  columns: [ 'name', 'state', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'hostname', 'zonename' ],
  details: [ 'name', 'id', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'linklocalip', 'gateway', 'hostname', 'zonename', 'created', 'activeviewersessions' ],
  actions: [
    {
      api: 'startSystemVm',
      icon: 'startvm',
      label: 'label.action.start.systemvm',
      dataView: true,
      hidden: (record) => { return record.state !== 'Stopped' },
      args: [
        'id'
      ]
    },
    {
      api: 'stopSystemVm',
      icon: 'stopvm',
      label: 'label.action.stop.systemvm',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'id'
      ]
    },
    {
      api: 'rebootSystemVm',
      icon: 'rebootvm',
      label: 'label.action.reboot.systemvm',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'id'
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
      api: 'migrateSystemVm',
      icon: 'migratevm',
      label: 'label.action.migrate.systemvm',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' },
      args: [
        'hostid', 'virtualmachineid'
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
      api: 'destroySystemVm',
      icon: 'delete',
      label: 'label.action.destroy.systemvm',
      dataView: true,
      hidden: (record) => { return record.state !== 'Running' && record.state !== 'Error' && record.state !== 'Stopped' },
      args: [
        'id'
      ]
    }
  ]
}
