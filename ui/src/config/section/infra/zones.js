export default {
  name: 'zone',
  title: 'Zones',
  icon: 'global',
  permission: [ 'listZones', 'listZonesMetrics' ],
  columns: [ 'name', 'allocationstate', 'networktype', 'guestcidraddress' ],
  details: [ 'name', 'id', 'allocationstate', 'networktype', 'guestcidraddress', 'localstorageenabled', 'securitygroupsenabled', 'dns1', 'dns2', 'internaldns1', 'internaldns2' ],
  actions: [
    {
      api: 'createZone',
      icon: 'plus',
      label: 'Add Zone',
      listView: true,
      popup: true,
      component: () => import('@/views/infra/ZoneWizard.vue')
    },
    {
      api: 'updateZone',
      icon: 'enable',
      label: 'label.action.disable.zone',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Enabled' },
      args: [ 'id' ],
      defaultArgs: { allocationstate: 'Disabled' }
    },
    {
      api: 'updateZone',
      icon: 'disable',
      label: 'label.action.enable.zone',
      dataView: true,
      hidden: (record) => { return record.allocationstate === 'Disabled' },
      args: [ 'id' ],
      defaultArgs: { allocationstate: 'Enabled' }
    },
    {
      api: 'dedicateZone',
      icon: 'dedicate',
      label: 'label.dedicate.zone',
      dataView: true,
      hidden: (record) => { return record.domainid !== null },
      args: [
        'zoneId', 'domainid', 'account'
      ]
    },
    {
      api: 'releaseDedicatedZone',
      icon: 'release',
      label: 'label.release.dedicated.zone',
      dataView: true,
      hidden: (record) => { return record.domainid !== null },
      args: [
        'zoneid'
      ]
    },
    {
      api: 'deleteZone',
      icon: 'delete',
      label: 'label.action.delete.zone',
      dataView: true,
      args: [
        'id'
      ]
    },
    {
      api: 'updateZone',
      icon: 'disable',
      label: 'label.edit',
      dataView: true,
      hidden: (record) => { return record.networktype === 'Advanced' },
      args: [
        'id', 'name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'guestcidraddress', 'internaldns1', 'internaldns2', 'domain', 'localstorageenabled'
      ]
    },
    {
      api: 'updateZone',
      icon: 'disable',
      label: 'label.edit',
      dataView: true,
      hidden: (record) => { return record.networktype === 'Basic' },
      args: [
        'id', 'name', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns1', 'internaldns2', 'domain', 'localstorageenabled'
      ]
    },
    {
      api: 'disableOutOfBandManagementForZone',
      icon: 'disableband',
      label: 'label.outofbandmanagement.disable',
      dataView: true,
      hidden: (record) => { return !(record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled && record.resourcedetails.outOfBandManagementEnabled === 'false') },
      args: [
        'zoneid'
      ]
    },
    {
      api: 'enableOutOfBandManagementForZone',
      icon: 'enableband',
      label: 'label.outofbandmanagement.enable',
      dataView: true,
      hidden: (record) => { return record.resourcedetails && record.resourcedetails.outOfBandManagementEnabled && record.resourcedetails.outOfBandManagementEnabled === 'false' },
      args: [
        'zoneid'
      ]
    },
    {
      api: 'disableHAForZone',
      icon: 'disableha',
      label: 'label.ha.disable',
      dataView: true,
      hidden: (record) => { return !(record.resourcedetails && record.resourcedetails.resourceHAEnabled && record.resourcedetails.resourceHAEnabled === 'false') },
      args: [
        'zoneid'
      ]
    },
    {
      api: 'enableHAForZone',
      icon: 'enableha',
      label: 'label.ha.enable',
      dataView: true,
      hidden: (record) => { return record.resourcedetails && record.resourcedetails.resourceHAEnabled && record.resourcedetails.resourceHAEnabled === 'false' },
      args: [
        'zoneid'
      ]
    },
    {
      api: 'addVmwareDc',
      icon: 'addvmwdc',
      label: 'label.add.vmware.datacenter',
      dataView: true,
      hidden: (record) => { return record.vmwaredcId === null },
      args: [
        'zoneid', 'name', 'vcenter', 'username', 'password'
      ]
    },
    {
      api: 'updateVmwareDc',
      icon: 'addvmwdc',
      label: 'label.update.vmware.datacenter',
      dataView: true,
      hidden: (record) => { return record.vmwaredcId !== null },
      args: [
        'zoneid', 'name', 'vcenter', 'username', 'password'
      ]
    },
    {
      api: 'removeVmwareDc',
      icon: 'addvmwdc',
      label: 'label.remove.vmware.datacenter',
      dataView: true,
      hidden: (record) => { return record.vmwaredcId !== null },
      args: [
        'zoneid'
      ]
    }
  ]
}
