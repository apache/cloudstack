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
      details: [ 'name', 'id', 'displaytext', 'offerha', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'created' ],
      actions: [{
        api: 'createServiceOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddComputeOffering.vue')
      }, {
        api: 'updateServiceOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteServiceOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    },
    {
      name: 'systemoffering',
      title: 'System Offerings',
      icon: 'setting',
      permission: [ 'listServiceOfferings', 'listInfrastructure' ],
      params: { 'issystem': 'true' },
      columns: [ 'name', 'systemvmtype', 'cpunumber', 'cpuspeed', 'memory', 'storagetype', 'tags' ],
      details: [ 'name', 'id', 'displaytext', 'systemvmtype', 'provisioningtype', 'storagetype', 'iscustomized', 'limitcpuuse', 'cpunumber', 'cpuspeed', 'memory', 'tags', 'domain', 'zone', 'created' ],
      actions: [{
        api: 'createServiceOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        params: { 'issystem': 'true' },
        popup: true,
        component: () => import('@/views/offering/AddSystemOffering.vue')
      }, {
        api: 'updateServiceOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        params: { 'issystem': 'true' },
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteServiceOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        params: { 'issystem': 'true' },
        args: ['id']
      }]
    },
    {
      name: 'diskoffering',
      title: 'Disk Offerings',
      icon: 'hdd',
      permission: [ 'listDiskOfferings' ],
      columns: [ 'name', 'displaytext', 'disksize', 'tags', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'disksize', 'provisioningtype', 'storagetype', 'iscustomized', 'tags', 'domain', 'zone', 'created' ],
      actions: [{
        api: 'createDiskOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddDiskOffering.vue')
      }, {
        api: 'updateDiskOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteDiskOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    },
    {
      name: 'networkoffering',
      title: 'Network Offerings',
      icon: 'wifi',
      permission: [ 'listNetworkOfferings' ],
      columns: [ 'name', 'state', 'guestiptype', 'traffictype', 'networkrate', 'tags', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'guestiptype', 'traffictype', 'networkrate', 'ispersistent', 'egressdefaultpolicy', 'availability', 'conservemode', 'specifyvlan', 'specifyipranges', 'supportspublicaccess', 'supportsstrechedl2subnet', 'service', 'tags', 'domain', 'zone' ],
      actions: [{
        api: 'createNetworkOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddNetworkOffering.vue')
      }, {
        api: 'updateNetworkOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['id', 'name', 'displaytext', 'availability']
      }, {
        api: 'deleteNetworkOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    },
    {
      name: 'vpcoffering',
      title: 'VPC Offerings',
      icon: 'deployment-unit',
      permission: [ 'listVPCOfferings' ],
      resourceType: 'VpcOffering',
      columns: [ 'name', 'state', 'displaytext', 'domain', 'zone' ],
      details: [ 'name', 'id', 'displaytext', 'distributedvpcrouter', 'service', 'tags', 'domain', 'zone', 'created' ],
      actions: [{
        api: 'createVPCOffering',
        icon: 'plus',
        label: 'Add Offering',
        listView: true,
        popup: true,
        component: () => import('@/views/offering/AddVpcOffering.vue')
      }, {
        api: 'updateVPCOffering',
        icon: 'edit',
        label: 'Edit Offering',
        dataView: true,
        args: ['id', 'name', 'displaytext']
      }, {
        api: 'deleteVPCOffering',
        icon: 'delete',
        label: 'Delete Offering',
        dataView: true,
        args: ['id']
      }]
    }
  ]
}
