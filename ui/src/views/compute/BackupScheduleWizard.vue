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
  <div class="backup-layout">
    <a-tabs defaultActiveKey="1" :animated="false">
      <a-tab-pane :tab="$t('label.schedule')" key="1">
        <FormSchedule
          :loading="loading"
          :resource="resource"
          :dataSource="dataSource"
          @close-action="closeAction"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.scheduled.backups')" key="2">
        <BackupSchedule
          :loading="loading"
          :resource="resource"
          :dataSource="dataSource"
          @refresh="handleRefresh"
          @close-action="closeAction" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import FormSchedule from '@views/compute/backup/FormSchedule'
import BackupSchedule from '@views/compute/backup/BackupSchedule'

export default {
  name: 'BackupScheduleWizard',
  components: {
    FormSchedule,
    BackupSchedule
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      dataSource: []
    }
  },
  provide () {
    return {
      refreshSchedule: this.fetchData,
      closeSchedule: this.closeAction
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      const params = {}
      this.dataSource = []
      this.loading = true
      params.virtualmachineid = this.resource.id || this.resource.virtualmachineid

      if (!params.virtualmachineid) {
        console.error('No VM ID found in resource:', this.resource)
        this.loading = false
        return
      }

      getAPI('listBackupSchedule', params).then(json => {
        this.dataSource = json.listbackupscheduleresponse.backupschedule || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleRefresh () {
      this.fetchData()
      this.$emit('refresh')
    },
    closeAction () {
      this.$emit('refresh')
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .backup-layout {
    width: 80vw;
    @media (min-width: 800px) {
      width: 600px;
    }
  }
</style>
