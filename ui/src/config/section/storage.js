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
          listView: true,
          args: ['name', 'zoneid', 'diskofferingid']
        }, {
          api: 'uploadVolume',
          icon: 'link',
          label: 'Upload Volume From URL',
          listView: true,
          args: ['url', 'name', 'zoneid', 'format', 'diskofferingid', 'checksum'],
          mapping: {
            format: {
              options: ['RAW', 'VHD', 'VHDX', 'OVA', 'QCOW2']
            }
          }
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
          args: ['virtualmachineid'],
          dataView: true,
          show: (record) => { return !('virtualmachineid' in record) }
        },
        {
          api: 'detachVolume',
          icon: 'link',
          label: 'Detach Volume',
          dataView: true,
          show: (record) => { return 'virtualmachineid' in record && record.virtualmachineid }
        },
        {
          api: 'createSnapshot',
          icon: 'camera',
          label: 'Take Snapshot',
          dataView: true,
          show: (record) => { return record.state === 'Ready' },
          args: ['volumeid', 'name', 'asyncbackup', 'tags'],
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'video-camera',
          label: 'Recurring Snapshots',
          dataView: true,
          show: (record) => { return record.state === 'Ready' },
          args: ['volumeid', 'intervaltype', 'schedule', 'maxsnaps', 'timezone'],
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            },
            intervaltype: {
              options: ['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY']
            }
          }
        },
        {
          api: 'resizeVolume',
          icon: 'fullscreen',
          label: 'Resize Volume',
          dataView: true,
          args: ['size']
        },
        {
          api: 'migrateVolume',
          icon: 'drag',
          label: 'Migrate Volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record) => { return record && record.state === 'Ready' },
          popup: true,
          component: () => import('@/views/storage/MigrateVolume.vue'),
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            },
            storageid: {
              api: 'listStoragePools'
            }
          }
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download',
          label: 'Download Volume',
          dataView: true,
          show: (record) => { return record && record.state === 'Ready' },
          args: ['zoneid', 'mode'],
          mapping: {
            zoneid: {
              value: (record) => { return record.zoneid }
            },
            mode: {
              value: (record) => { return 'HTTP_DOWNLOAD' }
            }
          },
          response: (result) => { return `Please click <a href="${result.volume.url}" target="_blank">${result.volume.url}</a> to download.` }
        },
        {
          api: 'createTemplate',
          icon: 'picture',
          label: 'Create Template from Volume',
          dataView: true,
          show: (record) => { return record.type === 'ROOT' },
          args: ['volumeid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled', 'sshkeyenabled'],
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteVolume',
          icon: 'delete',
          label: 'Delete Volume',
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
          show: (record) => { return record.state === 'BackedUp' },
          args: ['snapshotid', 'name'],
          mapping: {
            snapshotid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'createTemplate',
          icon: 'picture',
          label: 'Create volume',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' },
          args: ['snapshotid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled', 'sshkeyenabled'],
          mapping: {
            snapshotid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'revertSnapshot',
          icon: 'sync',
          label: 'Revert Snapshot',
          dataView: true
        },
        {
          api: 'deleteSnapshot',
          icon: 'delete',
          label: 'Delete Snapshot',
          dataView: true
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
          show: (record) => { return record.state === 'Ready' },
          args: ['vmsnapshotid'],
          mapping: {
            vmsnapshotid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteVMSnapshot',
          icon: 'delete',
          label: 'Delete VM Snapshot',
          dataView: true,
          args: ['vmsnapshotid'],
          mapping: {
            vmsnapshotid: {
              value: (record) => { return record.id }
            }
          }
        }
      ]
    }
  ]
}
