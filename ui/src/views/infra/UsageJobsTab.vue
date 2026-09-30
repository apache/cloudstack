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
    <a-row :gutter="12" style="margin-bottom: 10px">
      <a-col>
        <a-select
          v-model:value="durationHours"
          style="min-width: 180px"
          @change="fetchData">
          <a-select-option v-for="option in durationOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-col>
      <a-col>
        <a-button :loading="tabLoading" @click="fetchData">
          <template #icon><reload-outlined /></template>
          {{ $t('label.refresh') }}
        </a-button>
      </a-col>
    </a-row>
    <a-table
      class="table"
      size="small"
      :loading="tabLoading"
      :columns="columns"
      :dataSource="jobs"
      :rowKey="(item, index) => item.usageserver + '-' + item.startdate + '-' + index"
      :scroll="{ x: 'max-content' }"
      :pagination="false">
      <template #jobtype="{ text }">
        {{ jobTypeLabel(text) }}
      </template>
      <template #scheduled="{ text }">
        {{ text === 1 || text === true ? $t('label.yes') : $t('label.no') }}
      </template>
      <template #success="{ text }">
        <a-tag v-if="text === true || text === 1" color="green">{{ $t('label.success') }}</a-tag>
        <a-tag v-else-if="text === false || text === 0" color="red">{{ $t('label.failed') }}</a-tag>
        <span v-else>-</span>
      </template>
    </a-table>
    <a-pagination
      class="row-element"
      style="margin-top: 10px"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['20', '50', '100']"
      @change="changePage"
      @showSizeChange="changePage"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>
</template>

<script>
import { getAPI } from '@/api'

// Mirrors com.cloud.usage.UsageJobVO's job types.
const JOB_TYPE_RECURRING = 0
const JOB_TYPE_SINGLE = 1

export default {
  name: 'UsageJobsTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      jobs: [],
      tabLoading: false,
      page: 1,
      pageSize: 20,
      totalCount: 0,
      durationHours: 24,
      durationOptions: [
        { value: 1, label: this.$t('label.last.hour') },
        { value: 24, label: this.$t('label.last.24.hours') },
        { value: 24 * 7, label: this.$t('label.last.7.days') },
        { value: 0, label: this.$t('label.all') }
      ],
      columns: [
        {
          title: this.$t('label.usage.server'),
          dataIndex: 'usageserver'
        },
        {
          title: this.$t('label.job.type'),
          dataIndex: 'jobtype',
          slots: { customRender: 'jobtype' }
        },
        {
          title: this.$t('label.scheduled'),
          dataIndex: 'scheduled',
          slots: { customRender: 'scheduled' }
        },
        {
          title: this.$t('label.start.date'),
          dataIndex: 'startdate'
        },
        {
          title: this.$t('label.end.date'),
          dataIndex: 'enddate'
        },
        {
          title: this.$t('label.execution.time'),
          dataIndex: 'executiontime'
        },
        {
          title: this.$t('label.status'),
          dataIndex: 'success',
          slots: { customRender: 'success' }
        },
        {
          title: this.$t('label.heartbeat'),
          dataIndex: 'heartbeat'
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: function () {
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      const params = {
        page: this.page,
        pagesize: this.pageSize
      }
      if (this.durationHours > 0) {
        params.duration = this.durationHours
      }
      this.tabLoading = true
      getAPI('listUsageJobs', params).then(json => {
        this.totalCount = json?.listusagejobsresponse?.count || 0
        this.jobs = json?.listusagejobsresponse?.usagejob || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    jobTypeLabel (jobType) {
      if (jobType === JOB_TYPE_RECURRING) {
        return this.$t('label.recurring')
      }
      if (jobType === JOB_TYPE_SINGLE) {
        return this.$t('label.single')
      }
      return jobType
    }
  }
}
</script>
