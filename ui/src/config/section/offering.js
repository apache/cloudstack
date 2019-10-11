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
      columns: [ 'name', 'displaytext', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone' ]
    },
    {
      name: 'diskoffering',
      title: 'Disk Offerings',
      icon: 'hdd',
      permission: [ 'listDiskOfferings' ],
      columns: [ 'name', 'displaytext', 'disksize', 'tags', 'domain', 'zone' ]
    },
    {
      name: 'networkoffering',
      title: 'Network Offerings',
      icon: 'wifi',
      permission: [ 'listNetworkOfferings' ],
      columns: [ 'name', 'state', 'guestiptype', 'traffictype', 'networkrate', 'tags', 'domain', 'zone' ]
    },
    {
      name: 'vpcoffering',
      title: 'VPC Offerings',
      icon: 'deployment-unit',
      permission: [ 'listVPCOfferings' ],
      resourceType: 'VpcOffering',
      columns: [ 'name', 'state', 'displaytext', 'domain', 'zone' ]
    },
    {
      name: 'systemoffering',
      title: 'System Offerings',
      icon: 'setting',
      permission: [ 'listServiceOfferings' ],
      params: { 'issystem': 'true' },
      columns: [ 'name', 'systemvmtype', 'cpunumber', 'cpuspeed', 'memory', 'storagetype', 'tags' ]
    }
  ]
}
