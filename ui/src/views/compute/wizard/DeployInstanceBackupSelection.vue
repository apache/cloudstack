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

<template>
  <div>
    <div>{{ $t('message.backup.provision.instance') }}</div>
    <infinite-scroll-select
      style="margin-top: 10px; width: 100%;"
      v-model:value="localBackupOfferingId"
      placeholder="Select backup offering"
      api="listBackupOfferings"
      :apiParams="listBackupOfferingApiParams"
      resourceType="backupoffering"
      defaultIcon="cloud-upload-outlined"
      :defaultOption="backupOfferingDefaultOption"
      @change-option="handleChangeBackupOffering" />

      <div v-if="backupOfferingId && 'createBackupSchedule' in $store.getters.apis" style="margin-top: 15px">
        <a-form-item :label="$t('label.schedule')">
          <a-button
            type="dashed"
            style="width: 100%"
            @click="onShowAddBackupSchedule">
            <template #icon><plus-outlined /></template>
            {{ $t('label.add.backup.schedule') }}
          </a-button>
        </a-form-item>
        <backup-schedule
          style="margin-top: 10px;"
          :dataSource="backupSchedules"
          :deleteFn="handleDeleteBackupSchedule" />
      </div>

    <a-modal
      style="min-width: 400px;"
      :visible="showAddBackupSchedule"
      :title="$t('label.add.backup.schedule')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeModals">
      <form-schedule
        :resource="addFormResource"
        :submitFn="handleAddBackupSchedule" />
    </a-modal>
  </div>
</template>

<script>
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect'
import BackupSchedule from '@views/compute/backup/BackupSchedule'
import FormSchedule from '@views/compute/backup/FormSchedule'

export default {
  name: 'DeployInstanceBackupSelection',
  components: {
    InfiniteScrollSelect,
    BackupSchedule,
    FormSchedule
  },
  props: {
    zoneId: {
      type: String,
      default: null
    },
    backupOfferingId: {
      type: String,
      default: null
    },
    backupSchedules: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      backupOffering: null,
      showAddBackupSchedule: false,
      localBackupOfferingId: this.backupOfferingId
    }
  },
  provide () {
    return {
      refreshSchedule: null,
      closeSchedule: this.closeModals
    }
  },
  emits: ['change-backup-offering', 'add-backup-schedule', 'delete-backup-schedule', 'update:backupOfferingId'],
  computed: {
    listBackupOfferingApiParams () {
      return {
        zoneid: this.zoneId
      }
    },
    backupOfferingDefaultOption () {
      return { id: null, name: this.$t('label.noselect'), showicon: false }
    },
    addFormResource () {
      return {
        id: 'NEW',
        backupofferingid: this.backupOfferingId,
        backupoffering: this.backupOffering
      }
    }
  },
  watch: {
    localBackupOfferingId (val) {
      if (val !== this.backupOfferingId) {
        this.$emit('update:backupOfferingId', val)
      }
    },
    backupOfferingId (val) {
      if (val !== this.localBackupOfferingId) {
        this.localBackupOfferingId = val
      }
    }
  },
  methods: {
    handleChangeBackupOffering (offering) {
      this.$emit('change-backup-offering', offering)
      this.backupOffering = offering
    },
    onShowAddBackupSchedule () {
      this.showAddBackupSchedule = true
    },
    handleAddBackupSchedule (schedule) {
      schedule.id = 'SCH_' + new Date().getTime()
      schedule.intervaltype = schedule.intervaltype?.toUpperCase()
      this.$emit('add-backup-schedule', schedule)
      this.closeModals()
    },
    handleDeleteBackupSchedule (schedule) {
      this.$emit('delete-backup-schedule', schedule)
    },
    closeModals () {
      this.showAddBackupSchedule = false
    }
  }
}
</script>
