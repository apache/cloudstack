export default {
  name: 'offering',
  title: 'Offerings',
  icon: 'shopping',
  permission: [ 'listServiceOfferings' ],
  children: [
    {
      name: 'computeoffering',
      title: 'Compute Offerings',
      icon: 'cloud',
      permission: [ 'listServiceOfferings' ],
      columns: [ 'name', 'displaytext', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'offerha', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'created' ]
    },
    {
      name: 'diskoffering',
      title: 'Disk Offerings',
      icon: 'hdd',
      permission: [ 'listDiskOfferings' ],
      columns: [ 'name', 'displaytext', 'disksize', 'tags', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'disksize', 'provisioningtype', 'storagetype', 'iscustomized', 'tags', 'domain', 'zone', 'created' ]
    },
    {
      name: 'networkoffering',
      title: 'Network Offerings',
      icon: 'wifi',
      permission: [ 'listNetworkOfferings' ],
      columns: [ 'name', 'state', 'guestiptype', 'traffictype', 'networkrate', 'tags', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'guestiptype', 'traffictype', 'networkrate', 'ispersistent', 'egressdefaultpolicy', 'availability', 'conservemode', 'specifyvlan', 'specifyipranges', 'supportspublicaccess', 'supportsstrechedl2subnet', 'service', 'tags', 'domain', 'zone' ]
    },
    {
      name: 'vpcoffering',
      title: 'VPC Offerings',
      icon: 'deployment-unit',
      permission: [ 'listVPCOfferings' ],
      resourceType: 'VpcOffering',
      columns: [ 'name', 'state', 'displaytext', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'distributedvpcrouter', 'service', 'tags', 'domain', 'zone', 'created' ]
    },
    {
      name: 'systemoffering',
      title: 'System Offerings',
      icon: 'setting',
      permission: [ 'listInfrastructure' ],
      params: { 'issystem': 'true' },
      columns: [ 'name', 'systemvmtype', 'cpunumber', 'cpuspeed', 'memory', 'storagetype', 'tags' ],
      details: [ 'name', 'id', 'displaytext', 'systemvmtype', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'created' ]
    }
  ]
}
