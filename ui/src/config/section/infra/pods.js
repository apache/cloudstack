export default {
  name: 'pod',
  title: 'Pods',
  icon: 'appstore',
  permission: [ 'listPods' ],
  columns: [ 'name', 'allocationstate', 'gateway', 'netmask', 'zonename' ],
  actions: [
    {
      api: 'createPod',
      icon: 'plus',
      label: 'label.add.pod',
      listView: true,
      popup: true,
      args: [
        'zoneid', 'name', 'gateway', 'netmask', 'startip', 'endip'
      ]
    },
    {
      api: 'updatePod',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: [
        'id', 'name', 'netmask', 'gateway'
      ]
    },
    {
      api: 'dedicatePod',
      icon: 'dedicate',
      label: 'label.dedicate.pod',
      dataView: true,
      hidden: (record) => { return record.domainid !== null },
      args: [
        'podId', 'domainid', 'account'
      ]
    },
    {
      api: 'releaseDedicatedPod',
      icon: 'release',
      label: 'label.release.dedicated.pod',
      dataView: true,
      hidden: (record) => { return record.domainid === null },
      args: [
        'podid'
      ]
    },
    {
      api: 'updatePod',
      icon: 'enable',
      label: 'label.action.enable.pod',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Enabled' },
      args: [
        'id'
      ],
      defaultArgs: { allocationstate: 'Enabled' }
    },
    {
      api: 'updatePod',
      icon: 'disable',
      label: 'label.action.disable.pod',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Disabled' },
      args: [
        'id'
      ],
      defaultArgs: { allocationstate: 'Disabled' }
    },
    {
      api: 'deletePod',
      icon: 'delete',
      label: 'label.action.delete.pod',
      dataView: true,
      args: [
        'id'
      ]
    }
  ]
}
