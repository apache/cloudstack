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
      docHelp: 'adminguide/storage.html#working-with-volumes',
      permission: ['listVolumesMetrics'],
      resourceType: 'Volume',
      columns: () => {
        const fields = ['name', 'state', 'type', 'vmname', 'sizegb']
        const metricsFields = ['diskkbsread', 'diskkbswrite', 'diskiopstotal']

        if (store.getters.userInfo.roletype === 'Admin') {
          metricsFields.push({
            physicalsize: (record) => {
              return record.physicalsize ? parseFloat(record.physicalsize / (1024.0 * 1024.0 * 1024.0)).toFixed(2) + 'GB' : ''
            }
          })
        }
        metricsFields.push('utilization')

        if (store.getters.metrics) {
          fields.push(...metricsFields)
        }

        if (store.getters.userInfo.roletype === 'Admin') {
          fields.push('account')
          fields.push('storage')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
        }
        fields.push('zonename')

        return fields
      },
      details: ['name', 'id', 'type', 'storagetype', 'diskofferingdisplaytext', 'deviceid', 'sizegb', 'physicalsize', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'diskiopstotal', 'miniops', 'maxiops', 'path'],
      related: [{
        name: 'snapshot',
        title: 'label.snapshots',
        param: 'volumeid'
      }],
      searchFilters: ['name', 'zoneid', 'domainid', 'account', 'state', 'tags'],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus',
          docHelp: 'adminguide/storage.html#creating-a-new-volume',
          label: 'label.action.create.volume',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/CreateVolume.vue')
        },
        {
          api: 'createVolume',
          icon: 'cloud-upload',
          docHelp: 'adminguide/storage.html#uploading-an-existing-volume-to-a-virtual-machine',
          label: 'label.upload.volume.from.local',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/UploadLocalVolume.vue')
        },
        {
          api: 'uploadVolume',
          icon: 'link',
          docHelp: 'adminguide/storage.html#uploading-an-existing-volume-to-a-virtual-machine',
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
          dataView: true,
          show: (record) => { return record.type !== 'ROOT' && ['Allocated', 'Ready', 'Uploaded'].includes(record.state) && !('virtualmachineid' in record) },
          popup: true,
          component: () => import('@/views/storage/AttachVolume.vue')
        },
        {
          api: 'detachVolume',
          icon: 'link',
          label: 'label.action.detach.disk',
          message: 'message.detach.disk',
          dataView: true,
          show: (record) => {
            return record.type !== 'ROOT' && record.virtualmachineid &&
              ['Running', 'Stopped', 'Destroyed'].includes(record.vmstate)
          }
        },
        {
          api: 'createSnapshot',
          icon: 'camera',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.take.snapshot',
          dataView: true,
          show: (record, store) => {
            return record.state === 'Ready' && (record.hypervisor !== 'KVM' ||
              record.hypervisor === 'KVM' && record.vmstate === 'Running' && store.features.kvmsnapshotenabled ||
              record.hypervisor === 'KVM' && record.vmstate !== 'Running')
          },
          popup: true,
          component: () => import('@/views/storage/TakeSnapshot.vue')
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'clock-circle',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.recurring.snapshot',
          dataView: true,
          show: (record, store) => {
            return record.state === 'Ready' && (record.hypervisor !== 'KVM' ||
              record.hypervisor === 'KVM' && record.vmstate === 'Running' && store.features.kvmsnapshotenabled ||
              record.hypervisor === 'KVM' && record.vmstate !== 'Running')
          },
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
          docHelp: 'adminguide/storage.html#resizing-volumes',
          label: 'label.action.resize.volume',
          dataView: true,
          popup: true,
          show: (record) => { return ['Allocated', 'Ready'].includes(record.state) },
          component: () => import('@/views/storage/ResizeVolume.vue')
        },
        {
          api: 'migrateVolume',
          icon: 'drag',
          docHelp: 'adminguide/storage.html#id2',
          label: 'label.migrate.volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record, store) => { return record.state === 'Ready' && ['Admin'].includes(store.userInfo.roletype) },
          popup: true,
          component: () => import('@/views/storage/MigrateVolume.vue')
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download',
          label: 'label.action.download.volume',
          message: 'message.download.volume.confirm',
          dataView: true,
          show: (record) => { return record.state === 'Ready' && (record.vmstate === 'Stopped' || !record.virtualmachineid) },
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
          show: (record) => {
            return !['Destroy', 'Destroyed', 'Expunging', 'Expunged', 'Migrating', 'Uploading', 'UploadError', 'Creating'].includes(record.state) &&
            ((record.type === 'ROOT' && record.vmstate === 'Stopped') ||
            (record.type !== 'ROOT' && !record.virtualmachineid && !['Allocated', 'Uploaded'].includes(record.state)))
          },
          args: ['volumeid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled'],
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
              ['Allocated', 'Uploaded'].includes(record.state) && record.type !== 'ROOT' && !record.virtualmachineid ||
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
            return (['Admin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume)
              ? ['expunge'] : []
          },
          show: (record, store) => {
            return !['Destroy', 'Destroyed', 'Expunging', 'Expunged', 'Migrating', 'Uploading', 'UploadError', 'Creating', 'Allocated', 'Uploaded'].includes(record.state) &&
              record.type !== 'ROOT' && !record.virtualmachineid
          }
        }
      ]
    },
    {
      name: 'snapshot',
      title: 'label.snapshots',
      icon: 'build',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listSnapshots'],
      resourceType: 'Snapshot',
      columns: () => {
        var fields = ['name', 'state', 'volumename', 'intervaltype', 'created']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('domain')
          fields.push('account')
        }
        return fields
      },
      details: ['name', 'id', 'volumename', 'intervaltype', 'account', 'domain', 'created'],
      searchFilters: ['name', 'domainid', 'account', 'tags'],
      actions: [
        {
          api: 'createTemplate',
          icon: 'picture',
          label: 'label.create.template',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' },
          args: ['snapshotid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled'],
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
          args: (record, store) => {
            var fields = ['snapshotid', 'name']
            if (record.volumetype === 'ROOT') {
              fields.push('diskofferingid')
            }
            return fields
          },
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
          show: (record) => { return record.state === 'BackedUp' && record.revertable }
        },
        {
          api: 'deleteSnapshot',
          icon: 'delete',
          label: 'label.action.delete.snapshot',
          message: 'message.action.delete.snapshot',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' }
        }
      ]
    },
    {
      name: 'vmsnapshot',
      title: 'label.vm.snapshots',
      icon: 'camera',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listVMSnapshot'],
      resourceType: 'VMSnapshot',
      columns: () => {
        const fields = ['displayname', 'state', 'name', 'type', 'current', 'parentName', 'created']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('domain')
          fields.push('account')
        }
        return fields
      },
      details: ['name', 'id', 'displayname', 'description', 'type', 'current', 'parentName', 'virtualmachineid', 'account', 'domain', 'created'],
      searchFilters: ['name', 'domainid', 'account', 'tags'],
      actions: [
        {
          api: 'createSnapshotFromVMSnapshot',
          icon: 'camera',
          label: 'label.action.create.snapshot.from.vmsnapshot',
          message: 'message.action.create.snapshot.from.vmsnapshot',
          dataView: true,
          popup: true,
          show: (record) => { return (record.state === 'Ready' && record.hypervisor === 'KVM') },
          component: () => import('@/views/storage/CreateSnapshotFromVMSnapshot.vue')
        },
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
          show: (record) => { return ['Ready', 'Expunging', 'Error'].includes(record.state) },
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
      columns: [{ name: (record) => { return record.virtualmachinename } }, 'virtualmachinename', 'status', 'type', 'created', 'account', 'zone'],
      details: ['virtualmachinename', 'id', 'type', 'externalid', 'size', 'virtualsize', 'volumes', 'backupofferingname', 'zone', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'restoreBackup',
          icon: 'sync',
          docHelp: 'adminguide/virtual_machines.html#restoring-vm-backups',
          label: 'label.backup.restore',
          message: 'message.backup.restore',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' }
        },
        {
          api: 'restoreVolumeFromBackupAndAttachToVM',
          icon: 'paper-clip',
          label: 'label.backup.attach.restore',
          message: 'message.backup.attach.restore',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' },
          popup: true,
          component: () => import('@/views/storage/RestoreAttachBackupVolume.vue')
        },
        {
          api: 'removeVirtualMachineFromBackupOffering',
          icon: 'scissor',
          label: 'label.backup.offering.remove',
          message: 'message.backup.offering.remove',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' },
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
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' }
        }
      ]
    }
  ]
}
