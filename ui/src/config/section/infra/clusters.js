export default {
  name: 'cluster',
  title: 'Clusters',
  icon: 'cluster',
  permission: [ 'listClustersMetrics', 'listClusters' ],
  columns: [ 'name', 'state', 'clustertype', 'hypervisortype', 'hosts', 'cpuused', 'cpumaxdeviation', 'cpuallocated', 'cputotal', 'memoryused', 'memorymaxdeviation', 'memoryallocated', 'memorytotal', 'podname', 'zonename' ],
  details: [ 'name', 'id', 'allocationstate', 'clustertype', 'hypervisortype', 'podname', 'zonename' ],
  actions: [
    {
      api: 'addCluster',
      icon: 'plus',
      label: 'label.add.cluster',
      listView: true,
      args: ['zoneid', 'hypervisor', 'podid', 'clustername']
    },
    {
      api: 'updateCluster',
      icon: 'pause-circle',
      label: 'label.action.enable.cluster',
      dataView: true,
      args: ['id'],
      defaultArgs: { allocationstate: 'Disabled' },
      show: (record) => { return record.allocationstate === 'Enabled' }
    },
    {
      api: 'updateCluster',
      icon: 'play-circle',
      label: 'label.action.disable.cluster',
      dataView: true,
      args: [ 'id' ],
      defaultArgs: { allocationstate: 'Enabled' },
      show: (record) => { return record.allocationstate === 'Disabled' }
    },
    {
      api: 'dedicateCluster',
      icon: 'user-add',
      label: 'label.dedicate.cluster',
      dataView: true,
      args: ['clusterid', 'domainid', 'account'],
      show: (record) => { return !record.domainid }
    },
    {
      api: 'releaseDedicatedCluster',
      icon: 'user-delete',
      label: 'label.release.dedicated.cluster',
      dataView: true,
      args: ['clusterid'],
      show: (record) => { return record.domainid }
    },
    {
      api: 'updateCluster',
      icon: 'plus-square',
      label: 'Manage Cluster',
      dataView: true,
      args: ['id'],
      defaultArgs: { managedstate: 'Managed' },
      show: (record) => { return record.clustertype === 'CloudManaged' && ['PrepareUnmanaged', 'Unmanaged'].includes(record.state) }
    },
    {
      api: 'updateCluster',
      icon: 'minus-square',
      label: 'Unmanage Cluster',
      dataView: true,
      args: ['id'],
      defaultArgs: { managedstate: 'Unmanaged' },
      show: (record) => { return record.clustertype === 'CloudManaged' && record.state === 'Enabled' }
    },
    {
      api: 'enableOutOfBandManagementForCluster',
      icon: 'plus-circle',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      args: ['clusterid'],
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.outOfBandManagementEnabled ||
          record.resourcedetails.outOfBandManagementEnabled === 'false'
      }
    },
    {
      api: 'disableOutOfBandManagementForCluster',
      icon: 'minus-circle',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      args: ['clusterid'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled &&
          record.resourcedetails.outOfBandManagementEnabled === 'true'
      }
    },
    {
      api: 'enableHAForCluster',
      icon: 'eye',
      label: 'label.ha.enable',
      dataView: true,
      args: ['clusterid'],
      show: (record) => {
        return !record.resourcedetails || !record.resourcedetails.resourceHAEnabled ||
          record.resourcedetails.resourceHAEnabled === 'false'
      }
    },
    {
      api: 'disableHAForCluster',
      icon: 'eye-invisible',
      label: 'label.ha.disable',
      dataView: true,
      args: ['clusterid'],
      show: (record) => {
        return record.resourcedetails && record.resourcedetails.resourceHAEnabled &&
          record.resourcedetails.resourceHAEnabled === 'true'
      }
    },
    {
      api: 'deleteCluster',
      icon: 'delete',
      label: 'label.action.delete.cluster',
      dataView: true,
      args: ['id']
    }
  ]
}
