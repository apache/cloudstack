export default {
  name: 'storage',
  title: 'Storage',
  icon: 'database',
  children: [
    {
      name: 'volume',
      title: 'Volumes',
      icon: 'hdd',
      permission: [ 'listVolumesMetrics', 'listVolumes' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'type', 'vmname', 'size', 'physicalsize', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskiopstotal', 'storage', 'account', 'zonename'],
      hidden: ['storage', 'utilization'],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus',
          label: 'Create Volume',
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
    {
      name: 'snapshot',
      title: 'Snapshots',
      icon: 'build',
      permission: [ 'listSnapshots' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'volumename', 'intervaltype', 'created', 'account']
    },
    {
      name: 'vmsnapshot',
      title: 'VM Snapshots',
      icon: 'camera',
      permission: [ 'listVMSnapshot' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'type', 'current', 'parent', 'created', 'account']
    }
  ]
}
