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
      details: ['name', 'id', 'type', 'storagetype', 'diskofferingdisplaytext', 'deviceid', 'sizegb', 'physicalsize', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'diskiopstotal', 'miniops', 'maxiops', 'path'],
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
          popup: true,
          component: () => import('@/views/storage/CreateVolume.vue')
        },
        {
          api: 'getUploadParamsForVolume',
          icon: 'cloud-upload',
          label: 'Upload Local Volume',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/UploadLocalVolume.vue')
        },
        {
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
        },
        {
          api: 'attachVolume',
          icon: 'paper-clip',
          label: 'Attach Volume',
          args: ['virtualmachineid'],
          dataView: true,
          show: (record) => { return record.type !== 'ROOT' && record.state !== 'Destroy' && !('virtualmachineid' in record) }
        },
        {
          api: 'detachVolume',
          icon: 'link',
          label: 'Detach Volume',
          dataView: true,
          show: (record) => { return record.type !== 'ROOT' && 'virtualmachineid' in record && record.virtualmachineid }
        },
        {
          api: 'createSnapshot',
          icon: 'camera',
          label: 'Take Snapshot',
          dataView: true,
          show: (record) => { return record.state === 'Ready' },
          popup: true,
          component: () => import('@/views/storage/TakeSnapshot.vue')
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'clock-circle',
          label: 'Recurring Snapshots',
          dataView: true,
          show: (record) => { return record.state === 'Ready' },
          popup: true,
          component: () => import('@/views/storage/RecurringSnapshotVolume.vue'),
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
          popup: true,
          show: (record) => { return record.state !== 'Destroy' },
          component: () => import('@/views/storage/ResizeVolume.vue')
        },
        {
          api: 'migrateVolume',
          icon: 'drag',
          label: 'Migrate Volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record, store) => { return record && record.state === 'Ready' && ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) },
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
          show: (record) => { return record && record.state === 'Ready' && (record.vmstate === 'Stopped' || record.virtualmachineid == null) && record.state !== 'Destroy' },
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
          show: (record) => { return (record.type === 'ROOT' && record.vmstate === 'Stopped') || (record.type !== 'ROOT' && !('virtualmachineid' in record) && !['Allocated', 'Uploaded', 'Destroy'].includes(record.state)) },
          args: ['volumeid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled', 'sshkeyenabled'],
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'recoverVolume',
          icon: 'medicine-box',
          label: 'Recover Volume',
          dataView: true,
          show: (record, store) => {
            return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy'
          }
        },
        {
          api: 'deleteVolume',
          icon: 'delete',
          label: 'Delete Volume',
          dataView: true,
          groupAction: true,
          show: (record, store) => {
            return ['Expunging', 'Expunged', 'UploadError'].includes(record.state) ||
              ((['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy')
          }
        },
        {
          api: 'destroyVolume',
          icon: 'delete',
          label: 'Destroy Volume',
          dataView: true,
          args: (record, store) => {
            return (!['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !store.features.allowuserexpungerecovervolumestore) ? [] : ['expunge']
          },
          show: (record, store) => {
            return (!['Creating'].includes(record.state) && record.type !== 'ROOT' && !('virtualmachineid' in record) && record.state !== 'Destroy')
          }
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
          api: 'createTemplate',
          icon: 'picture',
          label: 'Create Template',
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
          api: 'createVolume',
          icon: 'hdd',
          label: 'Create Volume',
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
          api: 'revertSnapshot',
          icon: 'sync',
          label: 'Revert Snapshot',
          dataView: true,
          show: (record) => { return record.revertable }
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
      columns: ['displayname', 'state', 'type', 'current', 'parentName', 'created', 'account'],
      details: ['name', 'id', 'displayname', 'description', 'type', 'current', 'parentName', 'virtualmachineid', 'account', 'domain', 'created'],
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
    },
    {
      name: 'backup',
      title: 'Backups',
      icon: 'cloud-upload',
      permission: ['listBackups'],
      columns: [{ name: (record) => { return record.virtualmachinename } }, 'status', 'type', 'created', 'account', 'zone'],
      details: ['virtualmachinename', 'id', 'type', 'externalid', 'size', 'virtualsize', 'volumes', 'backupofferingname', 'zone', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'restoreBackup',
          icon: 'sync',
          label: 'Restore Backup',
          dataView: true
        },
        {
          api: 'restoreVolumeFromBackupAndAttachToVM',
          icon: 'paper-clip',
          label: 'Restore Volume and Attach',
          dataView: true,
          popup: true,
          component: () => import('@/views/storage/RestoreAttachBackupVolume.vue')
        },
        {
          api: 'removeVirtualMachineFromBackupOffering',
          icon: 'scissor',
          label: 'Expunge Offering Assignment and Delete Backups',
          dataView: true,
          args: ['forced', 'virtualmachineid'],
          mapping: {
            forced: {
              value: (record) => { return true }
            },
            virtualmachineid: {
              value: (record) => { return record.virtualmachineid }
            }
          }
        },
        {
          api: 'deleteBackup',
          icon: 'delete',
          label: 'Delete Backup',
          dataView: true
        }
      ]
    }
  ]
}
