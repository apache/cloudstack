// section -> list apis and actions

export const apiConfig = {

  // Instance
  'vm': {
    icon: 'cloud',
    path: 'vm',
    label: 'Instances',
    listApi: 'listVirtualMachinesMetrics',
    column: ['name', 'instancename', 'state', 'nic[].ipaddress', 'zonename', 'account', 'domain',
      'cpunumber', 'cpuused', 'cputotal', 'memoryintfreekbs', 'memorytotal', 'networkread', 'networkwrite', 'diskkbsread', 'diskkbswrite', 'diskiopstotal'
    ],
    hidden: ['zonename', 'account', 'domain'],
    actions: [
      {
        api: 'deployVirtualMachine',
        icon: 'plus',
        label: 'Deploy VM',
        type: 'main',
        params: ['name', 'zoneid', 'diskofferingid']
      }
    ]
  },

  // Storage Group
  'storage': {
    icon: 'database',
    path: 'storage',
    label: 'Storage',
    redirect: 'volume',
    children: [
      'volume',
      'snapshot',
      'vmsnapshot'
    ]
  },

  // Volume
  'volume': {
    icon: 'hdd',
    path: 'volume',
    label: 'Volumes',
    parent: 'storage',
    listApi: 'listVolumesMetrics',
    column: ['name', 'state', 'type', 'vmname', 'size', 'physicalsize', 'utilization', 'storage', 'hypervisor', 'account', 'domain', 'zonename'],
    hidden: ['storage', 'utilization'],
    actions: [
      {
        api: 'createVolume',
        icon: 'plus',
        label: 'Create Volumes',
        type: 'main',
        params: ['name', 'zoneid', 'diskofferingid']
      }, {
        api: 'uploadVolume',
        icon: 'cloud-upload',
        label: 'Upload Volume From URL',
        type: 'main',
        params: ['url', 'name', 'zoneid', 'format', 'diskofferingid', 'checksum']
      }, {
        api: 'getUploadParamsForVolume',
        icon: 'upload',
        label: 'Upload Local Volume',
        params: ['@file', 'name', 'zoneid', 'format', 'checksum']
      }, {
        api: 'resizeVolume',
        icon: 'fullscreen',
        label: 'Resize Volume',
        type: 'main',
        params: ['id', 'virtualmachineid']
      }, {
        api: 'attachVolume',
        icon: 'paper-clip',
        label: 'Attach Volume',
        params: ['id', 'virtualmachineid']
      }, {
        api: 'detachVolume',
        icon: 'link',
        label: 'Detach Volume',
        params: ['id', 'virtualmachineid']
      }, {
        api: 'extractVolume',
        icon: 'cloud-download',
        label: 'Download Volume',
        params: ['id', 'zoneid', 'mode'],
        paramOptions: {
          'mode': {
            'value': 'HTTP_DOWNLOAD'
          }
        }
      }, {
        api: 'migrateVolume',
        icon: 'drag',
        label: 'Migrate Volume',
        params: ['volumeid', 'storageid', 'livemigrate']
      }, {
        api: 'deleteVolume',
        icon: 'delete',
        label: 'Delete Volume',
        params: ['id']
      }
    ]
  },

  // Snapshot Tab
  'snapshot': {
    icon: 'build',
    path: 'snapshot',
    label: 'Snapshots',
    parent: 'storage',
    listApi: 'listSnapshots',
    column: ['volumename', 'name', 'state', 'intervaltype', 'created', 'account', 'domain'],
    hidden: [],
    actions: [
    ]
  },

  // VM Snapshot Tab
  'vmsnapshot': {
    icon: 'camera',
    path: 'vmsnapshot',
    label: 'VM Snapshots',
    parent: 'storage',
    listApi: 'listVMSnapshot',
    column: ['name', 'state', 'type', 'current', 'parent', 'created', 'account', 'domain'],
    hidden: ['storage'],
    actions: [
    ]
  },

  // Guest Network
  'guestnetwork': {
    icon: 'wifi',
    listApi: 'listNetworks',
    column: [],
    hidden: [],
    actions: [
      {
        api: 'deleteNetwork',
        icon: 'delete',
        label: 'Delete Network',
        params: ['id']
      }
    ]
  }

}
