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

import { shallowRef, defineAsyncComponent } from 'vue'
import store from '@/store'
import { isZoneCreated } from '@/utils/zone'

export default {
  name: 'storage',
  title: 'label.storage',
  icon: 'hdd-outlined',
  children: [
    {
      name: 'volume',
      title: 'label.volumes',
      icon: 'hdd-outlined',
      docHelp: 'adminguide/storage.html#working-with-volumes',
      permission: ['listVolumesMetrics'],
      resourceType: 'Volume',
      filters: () => {
        if (store.getters.userInfo.roletype === 'Admin') {
          return ['user', 'all']
        } else {
          return []
        }
      },
      columns: () => {
        const fields = ['name', 'state', 'size', 'type', 'vmname', 'vmstate']
        const metricsFields = ['diskkbsread', 'diskkbswrite', 'diskiopstotal']

        if (store.getters.userInfo.roletype === 'Admin') {
          metricsFields.push('physicalsize')
        }
        metricsFields.push('utilization')

        if (store.getters.metrics) {
          fields.push(...metricsFields)
        }
        if (store.getters.userInfo.roletype === 'Admin') {
          fields.push('storage')
          fields.push('account')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
        }
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push('zonename')

        return fields
      },
      details: ['name', 'id', 'type', 'storagetype', 'diskofferingdisplaytext', 'deviceid', 'sizegb', 'physicalsize', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'diskiopstotal', 'miniops', 'maxiops', 'path', 'deleteprotection'],
      related: [{
        name: 'snapshot',
        title: 'label.snapshots',
        param: 'volumeid'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'metrics',
          resourceType: 'Volume',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/StatsTab.vue'))),
          show: (record) => { return store.getters.features.instancesdisksstatsretentionenabled }
        },
        {
          name: 'events',
          resourceType: 'Volume',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      searchFilters: () => {
        var filters = ['name', 'zoneid', 'domainid', 'account', 'state', 'tags', 'serviceofferingid', 'diskofferingid', 'isencrypted']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          filters.push('storageid')
        }
        return filters
      },
      actions: [
        {
          api: 'createVolume',
          icon: 'plus-outlined',
          docHelp: 'adminguide/storage.html#creating-a-new-volume',
          label: 'label.action.create.volume',
          show: isZoneCreated,
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateVolume.vue')))
        },
        {
          api: 'createVolume',
          icon: 'cloud-upload-outlined',
          docHelp: 'adminguide/storage.html#uploading-an-existing-volume-to-a-virtual-machine',
          label: 'label.upload.volume.from.local',
          show: () => { return isZoneCreated() && 'getUploadParamsForVolume' in store.getters.apis },
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/UploadLocalVolume.vue')))
        },
        {
          api: 'uploadVolume',
          icon: 'link-outlined',
          docHelp: 'adminguide/storage.html#uploading-an-existing-volume-to-a-virtual-machine',
          label: 'label.upload.volume.from.url',
          show: isZoneCreated,
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/UploadVolume.vue')))
        },
        {
          api: 'attachVolume',
          icon: 'paper-clip-outlined',
          label: 'label.action.attach.disk',
          dataView: true,
          show: (record) => { return ['Allocated', 'Ready', 'Uploaded'].includes(record.state) && !('virtualmachineid' in record) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/AttachVolume.vue')))
        },
        {
          api: 'detachVolume',
          icon: 'link-outlined',
          label: 'label.action.detach.disk',
          message: 'message.detach.disk',
          dataView: true,
          show: (record) => { return record.virtualmachineid && ['Running', 'Stopped', 'Destroyed'].includes(record.vmstate) }
        },
        {
          api: 'updateVolume',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          args: ['name', 'deleteprotection'],
          mapping: {
            account: {
              value: (record) => { return record.account }
            },
            domainid: {
              value: (record) => { return record.domainid }
            }
          }
        },
        {
          api: 'createSnapshot',
          icon: 'camera-outlined',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.take.snapshot',
          dataView: true,
          show: (record, store) => {
            return record.state === 'Ready' &&
              (record.hypervisor !== 'KVM' ||
               ['Stopped', 'Destroyed'].includes(record.vmstate) ||
               store.features.kvmsnapshotenabled)
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/TakeSnapshot.vue')))
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'clock-circle-outlined',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.recurring.snapshot',
          dataView: true,
          show: (record, store) => {
            return record.state === 'Ready' &&
              (record.hypervisor !== 'KVM' ||
               (['Stopped', 'Destroyed'].includes(record.vmstate)) ||
               (store.features.kvmsnapshotenabled))
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/RecurringSnapshotVolume.vue'))),
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
          icon: 'fullscreen-outlined',
          docHelp: 'adminguide/storage.html#resizing-volumes',
          label: 'label.action.resize.volume',
          dataView: true,
          popup: true,
          show: (record) => { return ['Allocated', 'Ready'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/ResizeVolume.vue')))
        },
        {
          api: 'migrateVolume',
          permission: ['migrateVolume', 'findStoragePoolsForMigration', 'listStoragePools', 'listDiskOfferings'],
          icon: 'drag-outlined',
          docHelp: 'adminguide/storage.html#id2',
          label: 'label.migrate.volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record, store) => { return record.state === 'Ready' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/MigrateVolume.vue')))
        },
        {
          api: 'changeOfferingForVolume',
          icon: 'swap-outlined',
          docHelp: 'adminguide/storage.html#id2',
          label: 'label.change.offering.for.volume',
          args: ['id', 'diskofferingid', 'size', 'miniops', 'maxiops', 'automigrate'],
          dataView: true,
          show: (record, store) => { return ['Allocated', 'Ready'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/ChangeOfferingForVolume.vue')))
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download-outlined',
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
          icon: 'picture-outlined',
          label: 'label.action.create.template.from.volume',
          dataView: true,
          show: (record) => {
            return record.state === 'Ready' && (record.vmstate === 'Stopped' || !record.virtualmachineid)
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateTemplate.vue')))
        },
        {
          api: 'recoverVolume',
          icon: 'medicine-box-outlined',
          label: 'label.action.recover.volume',
          message: 'message.action.recover.volume',
          dataView: true,
          show: (record, store) => {
            return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy'
          }
        },
        {
          api: 'deleteVolume',
          icon: 'delete-outlined',
          label: 'label.action.delete.volume',
          message: 'message.action.delete.volume',
          dataView: true,
          show: (record, store) => {
            return ['Expunging', 'Expunged', 'UploadError'].includes(record.state) ||
                ['Allocated', 'Uploaded'].includes(record.state) && record.type !== 'ROOT' && !record.virtualmachineid ||
                ((['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy')
          },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'destroyVolume',
          icon: 'delete-outlined',
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
      icon: 'build-outlined',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listSnapshots'],
      resourceType: 'Snapshot',
      columns: () => {
        var fields = ['name', 'state', 'volumename', 'intervaltype', 'physicalsize', 'created']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
          if (store.getters.listAllProjects) {
            fields.push('project')
          }
          fields.push('domain')
        } else if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push('zonename')
        return fields
      },
      details: ['name', 'id', 'volumename', 'volumetype', 'snapshottype', 'intervaltype', 'physicalsize', 'virtualsize', 'chainsize', 'account', 'domain', 'created'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'zones',
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/SnapshotZones.vue')))
        },
        {
          name: 'events',
          resourceType: 'Snapshot',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      searchFilters: () => {
        var filters = ['name', 'domainid', 'account', 'tags', 'zoneid']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          filters.push('storageid')
          filters.push('imagestoreid')
        }
        return filters
      },
      actions: [
        {
          api: 'createTemplate',
          icon: 'picture-outlined',
          label: 'label.create.template',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateTemplate.vue')))
        },
        {
          api: 'createVolume',
          icon: 'hdd-outlined',
          label: 'label.action.create.volume',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateVolume.vue')))
        },
        {
          api: 'revertSnapshot',
          icon: 'sync-outlined',
          label: 'label.action.revert.snapshot',
          message: 'message.action.revert.snapshot',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' && record.revertable }
        },
        {
          api: 'extractSnapshot',
          icon: 'cloud-download-outlined',
          label: 'label.action.download.snapshot',
          message: 'message.action.download.snapshot',
          dataView: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
                ((record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
                  (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id))) &&
                    record.state === 'BackedUp'
          },
          args: ['zoneid'],
          mapping: {
            zoneid: {
              value: (record) => { return record.zoneid }
            }
          },
          response: (result) => { return `Please click <a href="${result.snapshot.url}" target="_blank">${result.snapshot.url}</a> to download.` }
        },
        {
          api: 'deleteSnapshot',
          icon: 'delete-outlined',
          label: 'label.action.delete.snapshot',
          message: 'message.action.delete.snapshot',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'snapshotpolicy',
      title: 'label.snapshotpolicies',
      icon: 'build-outlined',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listSnapshotPolicies'],
      resourceType: 'SnapshotPolicy',
      params: { listall: true },
      columns: () => {
        var fields = ['intervaltype', 'maxsnaps', 'schedule', 'timezone', 'volumename']
        return fields
      },
      searchFilters: ['volumeid'],
      actions: [
        {
          api: 'createSnapshotPolicy',
          icon: 'plus-outlined',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.create.recurring.snapshot',
          listView: true,
          show: () => { return 'createSnapshotPolicy' in store.getters.apis },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/RecurringSnapshotVolume.vue'))),
          mapping: {
            intervaltype: {
              options: ['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY']
            }
          }
        },
        {
          api: 'deleteSnapshotPolicies',
          icon: 'delete-outlined',
          label: 'label.delete.snapshot.policy',
          message: 'message.action.delete.snapshot.policy',
          dataView: true,
          show: (record) => true,
          args: ['id'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        }
      ]
    },
    {
      name: 'backup',
      title: 'label.backups',
      icon: 'cloud-upload-outlined',
      permission: ['listBackups'],
      params: { listvmdetails: 'true' },
      columns: ['name', 'status', 'size', 'virtualsize', 'virtualmachinename', 'backupofferingname', 'intervaltype', 'type', 'created', 'account', 'domain', 'zone'],
      details: ['name', 'description', 'virtualmachinename', 'id', 'intervaltype', 'type', 'externalid', 'size', 'virtualsize', 'volumes', 'backupofferingname', 'zone', 'account', 'domain', 'created'],
      searchFilters: () => {
        var filters = ['name', 'zoneid', 'domainid', 'account', 'backupofferingid']
        return filters
      },
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'instance.metadata',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/BackupMetadata.vue')))
        }
      ],
      actions: [
        {
          api: 'restoreBackup',
          icon: 'sync-outlined',
          docHelp: 'adminguide/virtual_machines.html#restoring-instance-backups',
          label: 'label.backup.restore',
          message: 'message.backup.restore',
          dataView: true,
          show: (record) => { return record.status === 'BackedUp' }
        },
        {
          api: 'restoreVolumeFromBackupAndAttachToVM',
          icon: 'paper-clip-outlined',
          label: 'label.backup.attach.restore',
          message: 'message.backup.attach.restore',
          dataView: true,
          show: (record) => { return record.status === 'BackedUp' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/RestoreAttachBackupVolume.vue')))
        },
        {
          api: 'createVMFromBackup',
          icon: 'caret-right-outlined',
          docHelp: 'adminguide/virtual_machines.html#creating-a-new-instance-from-backup',
          label: 'label.create.instance.from.backup',
          message: 'message.backup.restore',
          dataView: true,
          popup: true,
          show: (record) => { return record.status === 'BackedUp' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateVMFromBackup.vue')))
        },
        {
          api: 'removeVirtualMachineFromBackupOffering',
          icon: 'scissor-outlined',
          label: 'label.backup.offering.remove',
          message: 'message.backup.offering.remove',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' && record.vmbackupofferingremoved !== true },
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
          icon: 'delete-outlined',
          label: 'label.delete.backup',
          message: 'message.delete.backup',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' },
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) },
          args: ['forced']
        }
      ]
    },
    {
      name: 'backupschedule',
      title: 'label.backup.schedules',
      icon: 'build-outlined',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listBackupSchedule'],
      resourceType: 'backupSchedule',
      params: { listall: true },
      columns: () => {
        var fields = ['intervaltype', 'maxbackups', 'schedule', 'timezone', 'virtualmachinename']
        return fields
      },
      searchFilters: ['virtualmachineid'],
      actions: [
        {
          api: 'createBackupSchedule',
          icon: 'plus-outlined',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.create.backup.schedule',
          listView: true,
          show: () => { return 'createBackupSchedule' in store.getters.apis },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/backup/CreateBackupSchedule.vue'))),
          mapping: {
            intervaltype: {
              options: ['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY']
            }
          }
        },
        {
          api: 'deleteBackupSchedule',
          icon: 'delete-outlined',
          label: 'label.delete.backup.schedule',
          message: 'message.action.delete.backup.schedule',
          dataView: true,
          show: (record) => true,
          args: ['id'],
          mapping: {
            id: {
              value: (record) => record.id
            }
          }
        }
      ]
    },
    {
      name: 'buckets',
      title: 'label.buckets',
      icon: 'funnel-plot-outlined',
      permission: ['listBuckets'],
      columns: ['name', 'state', 'objectstore', 'size', 'account'],
      details: ['id', 'name', 'state', 'objectstore', 'size', 'url', 'accesskey', 'usersecretkey', 'account', 'domain', 'created', 'quota', 'encryption', 'versioning', 'objectlocking', 'policy'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'browser',
          resourceType: 'Bucket',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/ObjectStoreBrowser.vue'))),
          show: (record) => { return record.provider !== 'Simulator' }

        },
        {
          name: 'events',
          resourceType: 'Bucket',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        }
      ],
      actions: [
        {
          api: 'createBucket',
          icon: 'plus-outlined',
          docHelp: 'installguide/configuration.html#create-bucket',
          label: 'label.create.bucket',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateBucket.vue')))
        },
        {
          api: 'updateBucket',
          icon: 'edit-outlined',
          docHelp: 'adminguide/object_storage.html#update-bucket',
          label: 'label.bucket.update',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/UpdateBucket.vue'))),
          show: (record) => { return record.state !== 'Destroyed' }
        },
        {
          api: 'deleteBucket',
          icon: 'delete-outlined',
          label: 'label.bucket.delete',
          message: 'message.bucket.delete',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'sharedfs',
      title: 'label.shared.filesystems',
      icon: 'file-text-outlined',
      permission: ['listSharedFileSystems'],
      resourceType: 'SharedFS',
      columns: () => {
        const fields = ['name', 'state', 'sizegb']
        const metricsFields = ['diskkbsread', 'diskkbswrite', 'utilization', 'physicalsize']

        if (store.getters.metrics) {
          fields.push(...metricsFields)
        }
        if (store.getters.userInfo.roletype === 'Admin') {
          fields.push('storage')
          fields.push('account')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
        }
        if (store.getters.listAllProjects) {
          fields.push('project')
        }
        fields.push('zonename')

        return fields
      },
      details: ['id', 'name', 'description', 'state', 'filesystem', 'diskofferingdisplaytext', 'ipaddress', 'sizegb', 'provider', 'protocol', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'account', 'domain', 'created'],
      tabs: [{
        component: shallowRef(defineAsyncComponent(() => import('@/views/storage/SharedFSTab.vue')))
      }],
      searchFilters: () => {
        var filters = ['name', 'zoneid', 'domainid', 'account', 'networkid', 'serviceofferingid', 'diskofferingid']
        return filters
      },
      actions: [
        {
          api: 'createSharedFileSystem',
          icon: 'plus-outlined',
          docHelp: 'adminguide/storage.html#creating-a-new-file-share',
          label: 'label.create.sharedfs',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateSharedFS.vue')))
        },
        {
          api: 'updateSharedFileSystem',
          icon: 'edit-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.update.sharedfs',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/UpdateSharedFS.vue')))
        },
        {
          api: 'startSharedFileSystem',
          icon: 'caret-right-outlined',
          label: 'label.action.start.sharedfs',
          message: 'message.action.start.sharedfs',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          dataView: true,
          popup: true,
          groupAction: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'stopSharedFileSystem',
          icon: 'poweroff-outlined',
          label: 'label.action.stop.sharedfs',
          message: 'message.action.stop.sharedfs',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          dataView: true,
          popup: true,
          groupAction: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) },
          args: ['forced'],
          show: (record) => { return ['Ready'].includes(record.state) }
        },
        {
          api: 'restartSharedFileSystem',
          icon: 'reload-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.action.restart.sharedfs',
          message: 'message.action.restart.sharedfs',
          dataView: true,
          popup: true,
          args: ['cleanup'],
          show: (record) => { return ['Stopped', 'Ready', 'Detached'].includes(record.state) }
        },
        {
          api: 'changeSharedFileSystemDiskOffering',
          icon: 'swap-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.change.disk.offering',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/ChangeSharedFSDiskOffering.vue'))),
          show: (record) => { return ['Stopped', 'Ready'].includes(record.state) }
        },
        {
          api: 'changeSharedFileSystemServiceOffering',
          icon: 'arrows-alt-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.change.service.offering',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/ChangeSharedFSServiceOffering.vue'))),
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'destroySharedFileSystem',
          icon: 'delete-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.destroy.sharedfs',
          message: 'message.action.destroy.sharedfs',
          dataView: true,
          popup: true,
          groupAction: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, expunge: values.expunge, forced: values.forced } }) },
          args: ['expunge', 'forced'],
          show: (record) => { return !['Destroyed', 'Expunging', 'Error'].includes(record.state) }
        },
        {
          api: 'recoverSharedFileSystem',
          icon: 'medicine-box-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.recover.sharedfs',
          message: 'message.action.recover.sharedfs',
          dataView: true,
          show: (record) => { return record.state === 'Destroyed' }
        },
        {
          api: 'expungeSharedFileSystem',
          icon: 'delete-outlined',
          docHelp: 'adminguide/storage.html#lifecycle-operations',
          label: 'label.expunge.sharedfs',
          message: 'message.action.expunge.sharedfs',
          dataView: true,
          popup: true,
          show: (record) => { return ['Destroyed', 'Expunging', 'Error'].includes(record.state) }
        }
      ]
    }
  ]
}
