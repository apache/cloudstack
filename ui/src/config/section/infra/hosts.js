export default {
  name: 'host',
  title: 'Hosts',
  icon: 'desktop',
  permission: [ 'listHosts', 'listHostsMetrics' ],
  params: { 'type': 'routing' },
  columns: [ 'name', 'state', 'resourcestate', 'ipaddress', 'hypervisor', 'hypervisorversion', 'clustername', 'zonename' ],
  actions: [
    {
      api: 'addHost',
      icon: 'plus',
      label: 'label.add.host',
      listView: true,
      args: [
        'zoneid', 'podid', 'clusterid', 'hypervisor', 'hosttags', 'username', 'password', 'url'
      ]
    },
    {
      api: 'updateHost',
      icon: 'edit',
      label: 'label.edit',
      dataView: true,
      args: [
        'id', 'hosttags', 'annotation'
      ]
    },
    {
      api: 'dedicateHost',
      icon: 'dedicate',
      label: 'label.dedicate.host',
      dataView: true,
      hidden: (record) => { return record.domainid === null },
      args: [
        'hostId', 'domainId', 'account'
      ]
    },
    {
      api: 'releaseDedicatedHost',
      icon: 'release',
      label: 'label.release.dedicated.host',
      dataView: true,
      hidden: (record) => { return record.domainid !== null },
      args: [
        'hostid'
      ]
    },
    {
      api: 'provisionCertificate',
      icon: 'kvmcertificate',
      label: 'label.action.secure.host',
      dataView: true,
      hidden: (record) => { return record.hypervisor === 'KVM' },
      args: [
        'hostid'
      ]
    },
    {
      api: 'prepareHostForMaintenance',
      icon: 'enablemaintenance',
      label: 'label.action.enable.maintenance.mode',
      dataView: true,
      hidden: (record) => { return record.resourcestate === 'Enabled' },
      args: [
        'id'
      ]
    },
    {
      api: 'cancelHostMaintenance',
      icon: 'cancelmaintenance',
      label: 'label.action.cancel.maintenance.mode',
      dataView: true,
      hidden: (record) => { return record.resourcestate === 'Maintenance' || record.resourcestate === 'ErrorInMaintenance' || record.resourcestate === 'PrepareForMaintenance' },
      args: [
        'id'
      ]
    },
    {
      api: 'reconnectHost',
      icon: 'reconnect',
      label: 'label.action.force.reconnect',
      dataView: true,
      hidden: (record) => { return record.state !== 'Disconnected' },
      args: [
        'id'
      ]
    },
    {
      api: 'updateHost',
      icon: 'enable',
      label: 'label.enable.host',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Enabled' },
      args: [ 'id' ],
      defaultArgs: { allocationstate: 'Disabled' }
    },
    {
      api: 'updateHost',
      icon: 'disable',
      label: 'label.disable.host',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Disabled' },
      args: [ 'id' ],
      defaultArgs: { allocationstate: 'Enabled' }
    },
    {
      api: 'configureHAForHost',
      icon: 'configureha',
      label: 'label.ha.configure',
      dataView: true,
      args: [
        'provider', 'tags', 'hostid'
      ]
    },
    {
      api: 'enableHAForHost',
      icon: 'enableha',
      label: 'label.ha.enable',
      dataView: true,
      hidden: (record) => { return !(record.hostha !== null && record.hostha.haenable) },
      args: [
        'hostid'
      ]
    },
    {
      api: 'disableHAForHost',
      icon: 'disableha',
      label: 'label.ha.disable',
      dataView: true,
      hidden: (record) => { return record.hostha !== null && record.hostha.haenable },
      args: [
        'hostid'
      ]
    },
    {
      api: 'configureOutOfBandManagement',
      icon: 'plus',
      label: 'label.outofbandmanagement.configure',
      dataView: true,
      args: [
        'address', 'port', 'username', 'password', 'driver', 'tags', 'hostid'
      ]
    },
    {
      api: 'enableOutOfBandManagementForHost',
      icon: 'enableband',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      hidden: (record) => { return record.outofbandmanagement === null || !record.outofbandmanagement.enabled },
      args: [
        'hostid'
      ]
    },
    {
      api: 'disableOutOfBandManagementForHost',
      icon: 'disableband',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      hidden: (record) => { return !(record.outofbandmanagement !== null && record.outofbandmanagement.enabled) },
      args: [
        'hostid'
      ]
    },
    {
      api: 'issueOutOfBandManagementPowerAction',
      icon: 'issuepowerband',
      label: 'label.outofbandmanagement.action.issue',
      dataView: true,
      hidden: (record) => { return !(record.outofbandmanagement !== null && record.outofbandmanagement.enabled) },
      args: [
        'action', 'tags', 'hostid'
      ]
    },
    {
      api: 'changeOutOfBandManagementPassword',
      icon: 'changebandpassword',
      label: 'label.outofbandmanagement.changepassword',
      dataView: true,
      hidden: (record) => { return !(record.outofbandmanagement !== null && record.outofbandmanagement.enabled) },
      args: [
        'password', 'reenterpassword', 'tags', 'hostid'
      ]
    },
    {
      api: 'deleteHost',
      icon: 'delete',
      label: 'label.action.delete.host',
      dataView: true,
      hidden: (record) => { return !(record.resourcestate === 'Maintenance' || record.resourcestate !== 'Disabled' || record.state === 'Down' || record.state === 'Alert' || record.state === 'Disconnected') },
      args: [
        'id', 'forced'
      ]
    }
  ]
}
