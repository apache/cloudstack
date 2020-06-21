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

import store from '@/store'

export default {
  name: 'storage',
  title: 'label.storage',
  icon: 'database',
  children: [
    {
      name: 'volume',
      title: 'label.volumes',
      icon: 'hdd',
      permission: ['listVolumesMetrics'],
      resourceType: 'Volume',
      columns: () => {
        const fields = ['name', 'state', 'type', 'sizegb', 'vmname', 'diskkbsread', 'diskkbswrite', 'diskiopstotal']

        if (store.getters.userInfo.roletype === 'Admin') {
          fields.push('account')
          fields.push('storage')
          fields.push('zonename')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
          fields.push('zonename')
        } else {
          fields.push('zonename')
        }

        return fields
      },
      details: ['name', 'id', 'type', 'storagetype', 'diskofferingdisplaytext', 'deviceid', 'sizegb', 'physicalsize', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'diskiopstotal', 'miniops', 'maxiops', 'path'],
      related: [{
        name: 'snapshot',
        title: 'label.snapshots',
        param: 'volumeid'
      }],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus',
          label: 'label.action.create.volume',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/CreateVolume.vue')
        },
        {
          api: 'createVolume',
          icon: 'cloud-upload',
          label: 'label.upload.volume.from.local',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/UploadLocalVolume.vue')
        },
        {
          api: 'uploadVolume',
          icon: 'link',
          label: 'label.upload.volume.from.url',
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
          label: 'label.action.attach.disk',
          message: 'message.confirm.attach.disk',
          args: ['virtualmachineid'],
          dataView: true,
          show: (record) => { return record.type !== 'ROOT' && record.state !== 'Destroy' && !('virtualmachineid' in record) }
        },
        {
          api: 'detachVolume',
          icon: 'link',
          label: 'label.action.detach.disk',
          message: 'message.detach.disk',
          dataView: true,
          show: (record) => { return record.type !== 'ROOT' && 'virtualmachineid' in record && record.virtualmachineid }
        },
        {
          api: 'createSnapshot',
          icon: 'camera',
          label: 'label.action.take.snapshot',
          dataView: true,
          show: (record) => { return record.state === 'Ready' },
          popup: true,
          component: () => import('@/views/storage/TakeSnapshot.vue')
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'clock-circle',
          label: 'label.action.recurring.snapshot',
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
          label: 'label.action.resize.volume',
          dataView: true,
          popup: true,
          show: (record) => { return record.state !== 'Destroy' },
          component: () => import('@/views/storage/ResizeVolume.vue')
        },
        {
          api: 'migrateVolume',
          icon: 'drag',
          label: 'label.migrate.volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record, store) => { return record && record.state === 'Ready' && ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) },
          popup: true,
          component: () => import('@/views/storage/MigrateVolume.vue')
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download',
          label: 'label.action.download.volume',
          message: 'message.download.volume.confirm',
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
          label: 'label.action.create.template.from.volume',
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
          label: 'label.action.recover.volume',
          message: 'message.action.recover.volume',
          dataView: true,
          show: (record, store) => {
            return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy'
          }
        },
        {
          api: 'deleteVolume',
          icon: 'delete',
          label: 'label.action.delete.volume',
          message: 'message.action.delete.volume',
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
          label: 'label.action.destroy.volume',
          message: 'message.action.destroy.volume',
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
      title: 'label.snapshots',
      icon: 'build',
      permission: ['listSnapshots'],
      resourceType: 'Snapshot',
      columns: ['name', 'state', 'volumename', 'intervaltype', 'created', 'account'],
      details: ['name', 'id', 'volumename', 'intervaltype', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'createTemplate',
          icon: 'picture',
          label: 'label.create.template',
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
          label: 'label.action.create.volume',
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
          label: 'label.action.revert.snapshot',
          message: 'message.action.revert.snapshot',
          dataView: true,
          show: (record) => { return record.revertable }
        },
        {
          api: 'deleteSnapshot',
          icon: 'delete',
          label: 'label.action.delete.snapshot',
          message: 'message.action.delete.snapshot',
          dataView: true
        }
      ]
    },
    {
      name: 'vmsnapshot',
      title: 'label.vm.snapshots',
      icon: 'camera',
      permission: ['listVMSnapshot'],
      resourceType: 'VMSnapshot',
      columns: ['displayname', 'state', 'type', 'current', 'parentName', 'created', 'account'],
      details: ['name', 'id', 'displayname', 'description', 'type', 'current', 'parentName', 'virtualmachineid', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'revertToVMSnapshot',
          icon: 'sync',
          label: 'label.action.vmsnapshot.revert',
          message: 'label.action.vmsnapshot.revert',
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
          label: 'label.action.vmsnapshot.delete',
          message: 'message.action.vmsnapshot.delete',
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
      title: 'label.backup',
      icon: 'cloud-upload',
      permission: ['listBackups'],
      columns: [{ name: (record) => { return record.virtualmachinename } }, 'status', 'type', 'created', 'account', 'zone'],
      details: ['virtualmachinename', 'id', 'type', 'externalid', 'size', 'virtualsize', 'volumes', 'backupofferingname', 'zone', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'restoreBackup',
          icon: 'sync',
          label: 'label.backup.restore',
          message: 'message.backup.restore',
          dataView: true
        },
        {
          api: 'restoreVolumeFromBackupAndAttachToVM',
          icon: 'paper-clip',
          label: 'label.backup.attach.restore',
          message: 'message.backup.attach.restore',
          dataView: true,
          popup: true,
          component: () => import('@/views/storage/RestoreAttachBackupVolume.vue')
        },
        {
          api: 'removeVirtualMachineFromBackupOffering',
          icon: 'scissor',
          label: 'label.backup.offering.remove',
          message: 'message.backup.offering.remove',
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
          label: 'label.delete.backup',
          message: 'message.delete.backup',
          dataView: true
        }
      ]
    }
  ]
}
