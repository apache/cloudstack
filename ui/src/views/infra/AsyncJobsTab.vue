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
          v-model:value="statusFilter"
          style="min-width: 160px"
          @change="onFilterChange">
          <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-col>
      <a-col v-if="statusFilter !== 'pending'">
        <a-select
          v-model:value="durationHours"
          style="min-width: 160px"
          @change="onFilterChange">
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
      :rowKey="item => item.jobid"
      :pagination="false" >
      <template #cmd="{ text }">
        {{ text ? text.split('.').pop() : '' }}
      </template>
      <template #jobstatus="{ text }">
        <a-tag :color="statusColor(text)">{{ statusLabel(text) }}</a-tag>
      </template>
      <template #jobinstance="{ record }">
        <router-link v-if="resourcePath(record)" :to="{ path: resourcePath(record) }">
          {{ record.jobinstancetype }}
        </router-link>
        <span v-else>{{ record.jobinstancetype || '-' }}</span>
      </template>
      <template #account="{ text, record }">
        <router-link :to="{ path: '/account/' + record.accountid }">{{ text }}</router-link>
      </template>
      <template #domainpath="{ text, record }">
        <router-link :to="{ path: '/domain/' + record.domainid, query: { tab: 'details' } }">{{ text }}</router-link>
      </template>
      <template #jobresult="{ text }">
        <a-tooltip v-if="text" placement="topLeft" :title="text">
          <span class="job-result">{{ text }}</span>
        </a-tooltip>
        <span v-else>-</span>
      </template>
      <template #actions="{ record }">
        <a-popconfirm
          v-if="canCancel(record)"
          :title="$t('message.confirm.cancel.job')"
          :ok-text="$t('label.ok')"
          :cancel-text="$t('label.cancel')"
          placement="topRight"
          @confirm="cancelJob(record)">
          <a-button
            type="primary"
            danger
            size="small"
            :loading="cancelling === record.jobid">
            {{ $t('label.cancel.job') }}
          </a-button>
        </a-popconfirm>
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
import { getAPI, postAPI } from '@/api'
import { isAdmin } from '@/role'

// Mirrors org.apache.cloudstack.jobs.JobInfo.Status ordinals.
const STATUS_IN_PROGRESS = 0
const STATUS_SUCCEEDED = 1
const STATUS_FAILED = 2
const STATUS_CANCELLED = 3

export default {
  name: 'AsyncJobsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      jobs: [],
      tabLoading: false,
      cancelling: null,
      page: 1,
      pageSize: 20,
      totalCount: 0,
      statusFilter: 'pending',
      durationHours: 24,
      statusOptions: [
        { value: 'pending', label: this.$t('label.pending') },
        { value: String(STATUS_SUCCEEDED), label: this.$t('label.success') },
        { value: String(STATUS_FAILED), label: this.$t('label.failed') },
        { value: String(STATUS_CANCELLED), label: this.$t('label.cancelled') },
        { value: 'all', label: this.$t('label.all') }
      ],
      durationOptions: [
        { value: 1, label: this.$t('label.last.hour') },
        { value: 24, label: this.$t('label.last.24.hours') },
        { value: 24 * 7, label: this.$t('label.last.7.days') },
        { value: 0, label: this.$t('label.all') }
      ]
    }
  },
  computed: {
    columns () {
      const columns = [
        {
          title: this.$t('label.command'),
          dataIndex: 'cmd',
          slots: { customRender: 'cmd' }
        },
        {
          title: this.$t('label.status'),
          dataIndex: 'jobstatus',
          slots: { customRender: 'jobstatus' }
        }
      ]
      if (!this.resourceType) {
        columns.push({
          title: this.$t('label.resourcetype'),
          dataIndex: 'jobinstancetype',
          slots: { customRender: 'jobinstance' }
        })
      }
      columns.push(
        {
          title: this.$t('label.account'),
          dataIndex: 'account',
          slots: { customRender: 'account' }
        },
        {
          title: this.$t('label.domain'),
          dataIndex: 'domainpath',
          slots: { customRender: 'domainpath' }
        })
      if (this.resourceType) {
        columns.push({
          title: this.$t('label.managementservername'),
          dataIndex: 'managementservername'
        })
      }
      columns.push(
        {
          title: this.$t('label.created'),
          dataIndex: 'created'
        },
        {
          title: this.$t('label.completed'),
          dataIndex: 'completed'
        },
        {
          title: this.$t('label.jobresult'),
          dataIndex: 'jobresult',
          slots: { customRender: 'jobresult' }
        })
      if (this.showActions) {
        columns.push({
          title: this.$t('label.actions'),
          dataIndex: 'actions',
          width: 120,
          slots: { customRender: 'actions' }
        })
      }
      return columns
    },
    showActions () {
      return isAdmin() && 'cancelAsyncJob' in this.$store.getters.apis
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: function () {
      this.page = 1
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      if (!this.resource || !this.resource.id) {
        this.jobs = []
        return
      }
      const params = {
        listall: true,
        isrecursive: true,
        page: this.page,
        pagesize: this.pageSize
      }
      if (this.resourceType) {
        params.resourcetype = this.resourceType
        params.resourceid = this.resource.id
      } else {
        params.managementserverid = this.resource.id
      }
      // pending is the API default; a period only makes sense once completed jobs are included
      if (this.statusFilter === 'all') {
        params.jobstatus = [STATUS_IN_PROGRESS, STATUS_SUCCEEDED, STATUS_FAILED, STATUS_CANCELLED].join(',')
      } else if (this.statusFilter !== 'pending') {
        params.jobstatus = this.statusFilter
      }
      if (this.statusFilter !== 'pending' && this.durationHours > 0) {
        params.duration = this.durationHours
      }
      this.tabLoading = true
      getAPI('listAsyncJobs', params).then(json => {
        this.totalCount = json?.listasyncjobsresponse?.count || 0
        this.jobs = json?.listasyncjobsresponse?.asyncjobs || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    onFilterChange () {
      this.page = 1
      this.fetchData()
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    canCancel (record) {
      return this.showActions && record.jobstatus === STATUS_IN_PROGRESS
    },
    cancelJob (record) {
      this.cancelling = record.jobid
      postAPI('cancelAsyncJob', { jobid: record.jobid }).then(() => {
        this.$message.success(this.$t('message.cancel.job.success'))
      }).catch(error => {
        // a refusal (the operation cannot be stopped) is expected; show the server's reason
        this.$notifyError(error)
      }).finally(() => {
        this.cancelling = null
        this.fetchData()
      })
    },
    statusLabel (status) {
      switch (status) {
        case STATUS_IN_PROGRESS: return this.$t('label.pending')
        case STATUS_SUCCEEDED: return this.$t('label.success')
        case STATUS_FAILED: return this.$t('label.failed')
        case STATUS_CANCELLED: return this.$t('label.cancelled')
        default: return status
      }
    },
    statusColor (status) {
      switch (status) {
        case STATUS_IN_PROGRESS: return 'blue'
        case STATUS_SUCCEEDED: return 'green'
        case STATUS_FAILED: return 'red'
        case STATUS_CANCELLED: return 'orange'
        default: return 'default'
      }
    },
    resourcePath (record) {
      if (!record.jobinstanceid) {
        return null
      }
      switch (record.jobinstancetype) {
        case 'VirtualMachine': return '/vm/' + record.jobinstanceid
        case 'Volume': return '/volume/' + record.jobinstanceid
        case 'Snapshot': return '/snapshot/' + record.jobinstanceid
        case 'VmSnapshot': return '/vmsnapshot/' + record.jobinstanceid
        case 'Template': return '/template/' + record.jobinstanceid
        case 'Iso': return '/iso/' + record.jobinstanceid
        case 'Network': return '/guestnetwork/' + record.jobinstanceid
        case 'Host': return '/host/' + record.jobinstanceid
        default: return null
      }
    }
  }
}
</script>

<style scoped lang="less">
.job-result {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>
