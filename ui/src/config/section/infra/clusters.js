export default {
  name: 'cluster',
  title: 'Clusters',
  icon: 'cluster',
  permission: [ 'listClusters', 'listClustersMetrics' ],
  columns: [ 'name', 'allocationstate', 'clustertype', 'hypervisortype', 'podname', 'zonename' ],
  actions: [
    {
      api: 'addCluster',
      icon: 'plus',
      label: 'label.add.cluster',
      listView: true,
      args: [
        'zoneId', 'hypervisor', 'clustertype', 'podId', 'clustername'
      ]
    },
    {
      api: 'updateCluster',
      icon: 'enable',
      label: 'label.action.enable.cluster',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Disabled' },
      args: [
        'id'
      ],
      defaultArgs: { allocationstate: 'Enabled' }
    },
    {
      api: 'updateCluster',
      icon: 'disable',
      label: 'label.action.disable.cluster',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Enabled' },
      args: [
        'id'
      ],
      defaultArgs: { allocationstate: 'Disabled' }
    },
    {
      api: 'dedicateCluster',
      icon: 'dedicate',
      label: 'label.dedicate.cluster',
      dataView: true,
      hidden: (record) => { return record.domainid === null },
      args: [
        'clusterId', 'domainId', 'account'
      ]
    },
    {
      api: 'releaseDedicatedCluster',
      icon: 'release',
      label: 'label.release.dedicated.cluster',
      dataView: true,
      hidden: (record) => { return record.domainid !== null },
      args: [
        'clusterid'
      ]
    },
    {
      api: 'updateCluster',
      icon: 'managed',
      label: 'label.add.zone',
      dataView: true,
      hidden: (record) => { return record.state !== 'Enabled' && record.state !== 'Disabled' },
      args: [
        'id'
      ],
      defaultArgs: { managedstate: 'Managed' }
    },
    {
      api: 'updateCluster',
      icon: 'unmanaged',
      label: 'label.add.zone',
      dataView: true,
      hidden: (record) => { return record.state === 'Enabled' || record.state === 'Disabled' },
      args: [
        'id'
      ],
      defaultArgs: { managedstate: 'Unmanaged' }
    },
    {
      api: 'disableOutOfBandManagementForCluster',
      icon: 'disableband',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      hidden: (record) => { return !(record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled && record.resourcedetails.outOfBandManagementEnabled === 'false') },
      args: [
        'clusterid'
      ]
    },
    {
      api: 'enableOutOfBandManagementForCluster',
      icon: 'enableband',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      hidden: (record) => { return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled && record.resourcedetails.outOfBandManagementEnabled === 'false' },
      args: [
        'clusterid'
      ]
    },
    {
      api: 'disableHAForCluster',
      icon: 'disableha',
      label: 'label.ha.disable',
      dataView: true,
      hidden: (record) => { return !(record.resourcedetails && record.resourcedetails.resourceHAEnabled && record.resourcedetails.resourceHAEnabled === 'false') },
      args: [
        'clusterid'
      ]
    },
    {
      api: 'enableHAForCluster',
      icon: 'enableha',
      label: 'label.ha.enable',
      dataView: true,
      hidden: (record) => { return record.resourcedetails && record.resourcedetails.resourceHAEnabled && record.resourcedetails.resourceHAEnabled === 'false' },
      args: [
        'clusterid'
      ]
    },
    {
      api: 'deleteCluster',
      icon: 'delete',
      label: 'label.action.delete.cluster',
      dataView: true,
      args: [
        'id'
      ]
    }
  ]
}
