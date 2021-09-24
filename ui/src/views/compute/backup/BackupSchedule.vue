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
  <div class="list-schedule">
    <a-table
      size="small"
      :columns="columns"
      :dataSource="dataSchedules"
      :rowKey="record => record.virtualmachineid"
      :pagination="false"
      :loading="loading">
      <template #icon="{ text, record }" :name="text">
        <label class="interval-icon">
          <span v-if="record.intervaltype==='HOURLY'">
            <clock-circle-outlined />
          </span>
          <span class="custom-icon icon-daily" v-else-if="record.intervaltype==='DAILY'">
            <calendar-outlined />
          </span>
          <span class="custom-icon icon-weekly" v-else-if="record.intervaltype==='WEEKLY'">
            <calendar-outlined />
          </span>
          <span class="custom-icon icon-monthly" v-else-if="record.intervaltype==='MONTHLY'">
            <calendar-outlined />
          </span>
        </label>
      </template>
      <template #time="{ text, record }" :name="text">
        <label class="interval-content">
          <span v-if="record.intervaltype==='HOURLY'">{{ record.schedule + ' ' + $t('label.min.past.hour') }}</span>
          <span v-else>{{ record.schedule.split(':')[1] + ':' + record.schedule.split(':')[0] }}</span>
        </label>
      </template>
      <template #interval="{ text, record }" :name="text">
        <span v-if="record.intervaltype==='WEEKLY'">
          {{ `${$t('label.every')} ${$t(listDayOfWeek[record.schedule.split(':')[2] - 1])}` }}
        </span>
        <span v-else-if="record.intervaltype==='MONTHLY'">
          {{ `${$t('label.day')} ${record.schedule.split(':')[2]} ${$t('label.of.month')}` }}
        </span>
      </template>
      <template #timezone="{ text, record }" :name="text">
        <label>{{ getTimeZone(record.timezone) }}</label>
      </template>
      <template #action="{ text, record }" class="account-button-action" :name="text">
        <tooltip-button
          tooltipPlacement="top"
          :tooltip="$t('label.delete')"
          type="primary"
          :danger="true"
          icon="close-outlined"
          size="small"
          :loading="actionLoading"
          @onClick="handleClickDelete(record)"/>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import { timeZoneName } from '@/utils/timezone'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'BackupSchedule',
  components: {
    TooltipButton
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    dataSource: {
      type: Object,
      required: true
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      actionLoading: false,
      dataSchedules: [],
      listDayOfWeek: ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    }
  },
  computed: {
    columns () {
      return [
        {
          title: '',
          dataIndex: 'icon',
          width: 30,
          slots: { customRender: 'icon' }
        },
        {
          title: this.$t('label.time'),
          dataIndex: 'schedule',
          slots: { customRender: 'time' }
        },
        {
          title: '',
          dataIndex: 'interval',
          slots: { customRender: 'interval' }
        },
        {
          title: this.$t('label.timezone'),
          dataIndex: 'timezone',
          slots: { customRender: 'timezone' }
        },
        {
          title: this.$t('label.action'),
          dataIndex: 'action',
          width: 80,
          slots: { customRender: 'action' }
        }
      ]
    }
  },
  mounted () {
    this.dataSchedules = []
    if (this.dataSource && Object.keys(this.dataSource).length > 0) {
      this.dataSchedules.push(this.dataSource)
    }
  },
  inject: ['refreshSchedule'],
  watch: {
    dataSource: {
      deep: true,
      handler (newData) {
        this.dataSchedules = []
        if (newData && Object.keys(newData).length > 0) {
          this.dataSchedules.push(newData)
        }
      }
    }
  },
  methods: {
    handleClickDelete (record) {
      const params = {}
      params.virtualmachineid = record.virtualmachineid
      this.actionLoading = true
      api('deleteBackupSchedule', params).then(json => {
        if (json.deletebackupscheduleresponse.success) {
          this.$notification.success({
            message: this.$t('label.scheduled.backups'),
            description: this.$t('message.success.delete.backup.schedule')
          })
        }
        this.refreshSchedule()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.actionLoading = false
      })
    },
    getTimeZone (timeZone) {
      return timeZoneName(timeZone)
    }
  }
}
</script>

<style scoped lang="less">
.interval-icon {
  span {
    position: relative;
    font-size: 18px;
  }

  .custom-icon:before {
    font-size: 8px;
    position: absolute;
    top: 8px;
    left: 3.5px;
    color: #000;
    font-weight: 700;
    line-height: 1.7;
  }

  .icon-daily:before {
    content: "01";
    left: 5px;
    top: 7px;
    line-height: 1.9;
  }

  .icon-weekly:before {
    content: "1-7";
    left: 3px;
    line-height: 1.7;
  }

  .icon-monthly:before {
    content: "***";
  }
}

:deep(.ant-btn) > .anticon {
  line-height: 1.8;
}
</style>
