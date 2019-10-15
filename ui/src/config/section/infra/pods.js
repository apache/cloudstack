export default {
  name: 'pod',
  title: 'Pods',
  icon: 'appstore',
  permission: [ 'listPods' ],
  columns: [ 'name', 'allocationstate', 'gateway', 'netmask', 'zonename' ],
  details: [ 'name', 'id', 'allocationstate', 'netmask', 'gateway', 'zonename' ],
  actions: [
    {
      api: 'createPod',
      icon: 'plus',
      label: 'label.add.pod',
      listView: true,
      args: ['zoneid', 'name', 'gateway', 'netmask', 'startip', 'endip']
    },
    {
      api: 'updatePod',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: ['id', 'name', 'netmask', 'gateway']
    },
    {
      api: 'dedicatePod',
      icon: 'user-add',
      label: 'label.dedicate.pod',
      dataView: true,
      args: ['podid', 'domainid', 'account'],
      show: (record) => { return !record.domainid }
    },
    {
      api: 'releaseDedicatedPod',
      icon: 'user-delete',
      label: 'label.release.dedicated.pod',
      dataView: true,
      args: ['podid'],
      show: (record) => { return record.domainid }
    },
    {
      api: 'updatePod',
      icon: 'play-circle',
      label: 'label.action.enable.pod',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'updatePod',
      icon: 'pause-circle',
      label: 'label.action.disable.pod',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.allocationstate === 'Enabled' },
      defaultArgs: { allocationstate: 'Disabled' }
    },
    {
      api: 'deletePod',
      icon: 'delete',
      label: 'label.action.delete.pod',
      dataView: true,
      args: ['id']
    }
  ]
}
