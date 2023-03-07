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
      :rowKey="record => record.id"
      :pagination="false"
      :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'icon'">
          <label class="interval-icon">
            <span v-if="record.intervaltype===0">
              <clock-circle-outlined />
            </span>
            <span class="custom-icon icon-daily" v-else-if="record.intervaltype===1">
              <calendar-outlined />
            </span>
            <span class="custom-icon icon-weekly" v-else-if="record.intervaltype===2">
              <calendar-outlined />
            </span>
            <span class="custom-icon icon-monthly" v-else-if="record.intervaltype===3">
              <calendar-outlined />
            </span>
          </label>
        </template>
        <template v-if="column.key === 'time'">
          <label class="interval-content">
            <span v-if="record.intervaltype===0">{{ record.schedule + $t('label.min.past.hour') }}</span>
            <span v-else>{{ record.schedule.split(':')[1] + ':' + record.schedule.split(':')[0] }}</span>
          </label>
        </template>
        <template v-if="column.key === 'interval'">
          <span v-if="record.intervaltype===2">
            {{ `${$t('label.every')} ${$t(listDayOfWeek[record.schedule.split(':')[2] - 1])}` }}
          </span>
          <span v-else-if="record.intervaltype===3">
            {{ `${$t('label.day')} ${record.schedule.split(':')[2]} ${$t('label.of.month')}` }}
          </span>
        </template>
        <template v-if="column.key === 'timezone'">
          <label>{{ getTimeZone(record.timezone) }}</label>
        </template>
        <template v-if="column.key === 'tags'">
          <a-tag v-for="(tag, index) in record.tags" :key="index">{{ tag.key + '=' + tag.value }}</a-tag>
        </template>
        <template v-if="column.key === 'actions'">
          <div class="account-button-action">
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.delete')"
              type="primary"
              :danger="true"
              icon="close-outlined"
              size="small"
              :loading="actionLoading"
              @onClick="handleClickDelete(record)" />
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import { timeZoneName } from '@/utils/timezone'

export default {
  name: 'ScheduledSnapshots',
  components: {
    TooltipButton
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    dataSource: {
      type: Array,
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
      columns: [],
      dataSchedules: [],
      listDayOfWeek: ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    }
  },
  created () {
    this.columns = [
      {
        key: 'icon',
        title: '',
        dataIndex: 'icon',
        width: 30
      },
      {
        key: 'time',
        title: this.$t('label.time'),
        dataIndex: 'schedule'
      },
      {
        key: 'interval',
        title: '',
        dataIndex: 'interval'
      },
      {
        key: 'timezone',
        title: this.$t('label.timezone'),
        dataIndex: 'timezone'
      },
      {
        key: 'keep',
        title: this.$t('label.keep'),
        dataIndex: 'maxsnaps'
      },
      {
        key: 'tags',
        title: this.$t('label.tags'),
        dataIndex: 'tags'
      },
      {
        key: 'actions',
        title: this.$t('label.actions'),
        dataIndex: 'actions',
        width: 50
      }
    ]
  },
  mounted () {
    this.dataSchedules = this.dataSource
  },
  watch: {
    dataSource: {
      deep: true,
      handler (newData) {
        this.dataSchedules = newData
      }
    }
  },
  methods: {
    handleClickDelete (record) {
      const params = {}
      params.id = record.id
      this.actionLoading = true
      api('deleteSnapshotPolicies', params).then(json => {
        if (json.deletesnapshotpoliciesresponse.success) {
          this.$notification.success({
            message: this.$t('label.delete.snapshot.policy'),
            description: this.$t('message.success.delete.snapshot.policy')
          })

          this.$emit('refresh')
        }
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

<style lang="less" scoped>
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
