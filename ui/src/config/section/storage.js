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
      viewComponent: () => import('@/views/common/DetailView.vue'),
      columns: ['name', 'state', 'type', 'vmname', 'size', 'physicalsize', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskiopstotal', 'storage', 'account', 'zonename'],
      hidden: ['storage', 'utilization'],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus',
          label: 'Create Volume',
          type: 'main',
          args: ['name', 'zoneid', 'diskofferingid'],
          listView: true
        }, {
          api: 'uploadVolume',
          icon: 'cloud-upload',
          label: 'Upload Volume From URL',
          type: 'main',
          args: ['url', 'name', 'zoneid', 'format', 'diskofferingid', 'checksum'],
          listView: true
        }, {
          api: 'getUploadParamsForVolume',
          icon: 'upload',
          label: 'Upload Local Volume',
          args: ['@file', 'name', 'zoneid', 'format', 'checksum'],
          listView: true
        },
        {
          api: 'attachVolume',
          icon: 'paper-clip',
          label: 'Attach Volume',
          args: ['id', 'virtualmachineid'],
          dataView: true,
          hidden: (record) => { return record.virtualmachineid }
        },
        {
          api: 'detachVolume',
          icon: 'link',
          label: 'Detach Volume',
          args: ['id', 'virtualmachineid'],
          dataView: true,
          hidden: (record) => { return !record.virtualmachineid }
        },
        {
          api: 'migrateVolume',
          icon: 'drag',
          label: 'Migrate Volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true
        },
        {
          api: 'resizeVolume',
          icon: 'fullscreen',
          label: 'Resize Volume',
          type: 'main',
          args: ['id', 'virtualmachineid'],
          dataView: true
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download',
          label: 'Download Volume',
          args: ['id', 'zoneid', 'mode'],
          paramOptions: {
            'mode': {
              'value': 'HTTP_DOWNLOAD'
            }
          },
          dataView: true
        },
        {
          api: 'deleteVolume',
          icon: 'delete',
          label: 'Delete Volume',
          args: ['id'],
          dataView: true,
          groupAction: true
        }
      ]
    },
    {
      name: 'snapshot',
      title: 'Snapshots',
      icon: 'build',
      permission: [ 'listSnapshots' ],
      columns: ['name', 'state', 'volumename', 'intervaltype', 'created', 'account']
    },
    {
      name: 'vmsnapshot',
      title: 'VM Snapshots',
      icon: 'camera',
      permission: [ 'listVMSnapshot' ],
      columns: ['name', 'state', 'type', 'current', 'parent', 'created', 'account']
    }
  ]
}
