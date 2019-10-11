export default {
  name: 'storagepool',
  title: 'Primary Storage',
  icon: 'database',
  permission: [ 'listStoragePools', 'listStoragePoolsMetrics' ],
  columns: [ 'name', 'state', 'ipaddress', 'type', 'path', 'scope', 'clustername', 'zonename' ],
  details: [ 'name', 'id', 'ipaddress', 'type', 'scope', 'path', 'provider', 'hypervisor', 'overprovisionfactor', 'disksizetotal', 'disksizeallocated', 'disksizeused', 'clustername', 'podname', 'zonename', 'created' ],
  actions: [
    {
      api: 'createStoragePool',
      icon: 'plus',
      label: 'label.add.primary.storage',
      listView: true,
      args: [
        'scope', 'zoneid', 'podid', 'clusterid', 'name', 'provider', 'managed', 'capacityBytes', 'capacityIops', 'url', 'tags'
      ]
    },
    {
      api: 'updateStoragePool',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: [
        'id', 'tags', 'capacitybytes', 'capacityiops'
      ]
    },
    {
      api: 'enableStorageMaintenance',
      icon: 'enablemaintenance',
      label: 'label.action.enable.maintenance.mode',
      dataView: true,
      hidden: (record) => { return !(record.state === 'Up' || record.state === 'Connecting' || record.state === 'Down' || record.state === 'ErrorInMaintenance') },
      args: [
        'id'
      ]
    },
    {
      api: 'cancelStorageMaintenance',
      icon: 'cancelmaintenance',
      label: 'label.action.cancel.maintenance.mode',
      dataView: true,
      hidden: (record) => { return !(record.state === 'ErrorInMaintenance' || record.state === 'PrepareForMaintenance' || record.state === 'Maintenance') },
      args: [
        'id'
      ]
    },
    {
      api: 'deleteStoragePool',
      icon: 'delete',
      label: 'label.action.delete.primary.storage',
      dataView: true,
      hidden: (record) => { return !(record.state === 'Down' || record.state === 'Alert' || record.state === 'Maintenance' || record.state === 'Disconnected') },
      args: [
        'id', 'forced'
      ]
    }
  ]
}
