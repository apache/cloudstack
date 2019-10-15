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
      icon: 'caret-right',
      label: 'label.action.start.systemvm',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.state === 'Stopped' }
    },
    {
      api: 'stopSystemVm',
      icon: 'stop',
      label: 'label.action.stop.systemvm',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'rebootSystemVm',
      icon: 'sync',
      label: 'label.action.reboot.systemvm',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.state === 'Running' }
    },
    {
      api: 'scaleSystemVm',
      icon: 'arrows-alt',
      label: 'label.change.service.offering',
      dataView: true,
      args: ['id', 'serviceofferingid'],
      show: (record) => { return record.hypervisor === 'VMWare' || record.state === 'Stopped' }
    },
    {
      api: 'migrateSystemVm',
      icon: 'drag',
      label: 'label.action.migrate.systemvm',
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
      api: 'destroySystemVm',
      icon: 'delete',
      label: 'label.action.destroy.systemvm',
      dataView: true,
      args: ['id'],
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) }
    }
  ]
}
