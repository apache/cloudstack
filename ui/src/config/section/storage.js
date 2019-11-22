// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

export default {
  name: 'storage',
  title: 'Storage',
  icon: 'database',
  children: [
    {
      name: 'volume',
      title: 'Volumes',
      icon: 'hdd',
      permission: ['listVolumesMetrics', 'listVolumes'],
      resourceType: 'Volume',
      columns: ['name', 'state', 'type', 'vmname', 'size', 'physicalsize', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskiopstotal', 'storage', 'account', 'zonename'],
      details: ['name', 'id', 'type', 'deviceid', 'sizegb', 'physicalsize', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'diskiopstotal', 'path'],
      related: [{
        name: 'snapshot',
        title: 'Snapshots',
        param: 'volumeid'
      }],
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
          icon: 'link',
          label: 'Upload Volume From URL',
          type: 'main',
          args: ['url', 'name', 'zoneid', 'format', 'diskofferingid', 'checksum'],
          listView: true
        }, {
          api: 'getUploadParamsForVolume',
          icon: 'cloud-upload',
          label: 'Upload Local Volume',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/UploadLocalVolume.vue')
        },
        {
          api: 'attachVolume',
          icon: 'paper-clip',
          label: 'Attach Volume',
          args: ['id', 'virtualmachineid'],
          dataView: true,
          show: (record) => { return !('virtualmachineid' in record) }
        },
        {
          api: 'detachVolume',
          icon: 'link',
          label: 'Detach Volume',
          args: ['id'],
          dataView: true,
          show: (record) => { return 'virtualmachineid' in record && record.virtualmachineid }
        },
        {
          api: 'createSnapshot',
          icon: 'camera',
          label: 'Take Snapshot',
          args: ['volumeid', 'name', 'asyncbackup', 'tags'],
          dataView: true,
          show: (record) => { return record.state === 'Ready' }
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'video-camera',
          label: 'Recurring Snapshots',
          args: ['volumeid', 'schedule', 'timezone', 'intervaltype', 'maxsnaps'],
          dataView: true,
          show: (record) => { return record.state === 'Ready' }
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
          api: 'migrateVolume',
          icon: 'drag',
          label: 'Migrate Volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record) => { return 'virtualmachineid' in record && record.virtualmachineid }
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download',
          label: 'Download Volume',
          args: ['id', 'zoneid', 'mode'],
          paramOptions: {
            mode: {
              value: 'HTTP_DOWNLOAD'
            }
          },
          dataView: true
        },
        {
          api: 'createTemplate',
          icon: 'picture',
          label: 'Create Template from Volume',
          args: ['volumeid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled', 'sshkeyenabled'],
          dataView: true,
          show: (record) => { return record.type === 'ROOT' }
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
      permission: ['listSnapshots'],
      resourceType: 'Snapshot',
      columns: ['name', 'state', 'volumename', 'intervaltype', 'created', 'account'],
      details: ['name', 'id', 'volumename', 'intervaltype', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus',
          label: 'Create volume',
          dataView: true,
          args: ['snapshotid', 'name']
        },
        {
          api: 'createTemplate',
          icon: 'picture',
          label: 'Create volume',
          dataView: true,
          args: ['snapshotid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled', 'sshkeyenabled']
        },
        {
          api: 'revertSnapshot',
          icon: 'sync',
          label: 'Revert Snapshot',
          dataView: true,
          args: ['id']
        },
        {
          api: 'deleteSnapshot',
          icon: 'delete',
          label: 'Delete Snapshot',
          dataView: true,
          args: ['id']
        }
      ]
    },
    {
      name: 'vmsnapshot',
      title: 'VM Snapshots',
      icon: 'camera',
      permission: ['listVMSnapshot'],
      resourceType: 'VMSnapshot',
      columns: ['name', 'state', 'type', 'current', 'parent', 'created', 'account'],
      details: ['name', 'id', 'displayname', 'description', 'type', 'current', 'parent', 'virtualmachineid', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'revertToVMSnapshot',
          icon: 'sync',
          label: 'Revert VM snapshot',
          dataView: true,
          args: ['vmsnapshotid']
        },
        {
          api: 'deleteVMSnapshot',
          icon: 'delete',
          label: 'Delete VM Snapshot',
          dataView: true,
          args: ['vmsnapshotid']
        }
      ]
    }
  ]
}
