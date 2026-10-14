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
    <a-row style="margin-bottom: 10px; display: flex; align-items: center; flex-wrap: wrap; gap: 10px">
      <a-col style="display: flex; align-items: center; flex-wrap: wrap; gap: 10px">
        <a-button :loading="tabLoading" size="small" @click="fetchData">
          <template #icon><reload-outlined /></template>
          {{ $t('label.refresh') }}
        </a-button>
        <a-tooltip placement="right">
          <template #title>{{ $t('label.filterby') }}</template>
          <a-select
            v-model:value="statusFilter"
            :placeholder="$t('label.filterby')"
            style="min-width: 120px"
            size="small"
            @change="onFilterChange">
            <template #suffixIcon><filter-outlined class="ant-select-suffix" /></template>
            <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value" :label="option.label">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-tooltip>
        <a-tooltip v-if="statusFilter !== 'pending'" placement="right">
          <template #title>{{ $t('label.period') }}</template>
          <a-select
            v-model:value="durationHours"
            style="min-width: 140px"
            size="small"
            @change="onFilterChange">
            <template #suffixIcon><filter-outlined class="ant-select-suffix" /></template>
            <a-select-option v-for="option in durationOptions" :key="option.value" :value="option.value" :label="option.label">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-tooltip>
      </a-col>
      <a-col v-if="!resourceType" style="margin-left: auto">
        <search-view
          :searchFilters="searchFilters"
          :searchParams="searchParams"
          apiName="listAsyncJobs"
          @search="onSearch" />
      </a-col>
    </a-row>
    <a-table
      class="table"
      size="small"
      :loading="tabLoading"
      :columns="columns"
      :dataSource="jobs"
      :rowKey="item => item.jobid"
      :scroll="{ x: 'max-content' }"
      :pagination="false" >
      <template #customFilterDropdown>
        <div style="padding: 8px" class="filter-dropdown">
          <a-menu>
            <a-menu-item v-for="column in allColumns" :key="column.key" @click="updateSelectedColumns(column.key)">
              <a-checkbox :id="column.key" :checked="selectedColumns.includes(column.key)" />
              {{ column.title }}
            </a-menu-item>
          </a-menu>
        </div>
      </template>
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
      <template #resource="{ record }">
        <resource-label
          v-if="record.jobinstanceid"
          :resourceType="record.jobinstancetype"
          :resourceId="record.jobinstanceid"
          :resourceName="record.jobinstancename || record.jobinstanceid" />
        <span v-else>-</span>
      </template>
      <template #account="{ text, record }">
        <router-link :to="{ path: '/account/' + record.accountid }">{{ text }}</router-link>
      </template>
      <template #domainpath="{ text, record }">
        <router-link :to="{ path: '/domain/' + record.domainid, query: { tab: 'details' } }">{{ text }}</router-link>
      </template>
      <template #created="{ text }">
        {{ text ? $toLocaleDate(text) : '-' }}
      </template>
      <template #completed="{ text }">
        {{ text ? $toLocaleDate(text) : '-' }}
      </template>
      <template #jobresult="{ text }">
        <a-popover v-if="resultText(text) !== '-'" placement="topLeft" overlayClassName="job-result-popover">
          <template #content>
            <div class="job-result-full">
              <a
                href="javascript:;"
                class="job-result-copy"
                v-clipboard:copy="prettyResult(text)"
                @click="$message.success($t('label.copied.clipboard'))">
                <copy-outlined /> {{ $t('label.copy') }}
              </a>
              <pre>{{ prettyResult(text) }}</pre>
            </div>
          </template>
          <span class="job-result">{{ resultText(text) }}</span>
        </a-popover>
        <span v-else>-</span>
      </template>
      <template #actions="{ record }">
        <router-link v-if="canListEvents" :to="{ path: '/event', query: { jobid: record.jobid } }" style="margin-right: 8px">
          {{ $t('label.events') }}
        </router-link>
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
import SearchView from '@/components/view/SearchView'
import ResourceLabel from '@/components/widgets/ResourceLabel'
import { genericCompare } from '@/utils/sort.js'

// Mirrors org.apache.cloudstack.jobs.JobInfo.Status ordinals.
const STATUS_IN_PROGRESS = 0
const STATUS_SUCCEEDED = 1
const STATUS_FAILED = 2
const STATUS_CANCELLED = 3

export default {
  name: 'AsyncJobsTab',
  components: {
    SearchView,
    ResourceLabel
  },
  props: {
    resource: {
      type: Object,
      default: null
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
      searchFilters: ['domainid', 'account', 'resourcetype'],
      searchParams: {},
      selectedColumns: [],
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
    allColumns () {
      const column = (key, title, slot) => {
        const col = { key, title, dataIndex: key, sorter: (a, b) => genericCompare(a[key] || '', b[key] || '') }
        if (slot) {
          col.slots = { customRender: slot }
        }
        return col
      }
      const columns = [
        column('cmd', this.$t('label.command'), 'cmd'),
        column('jobstatus', this.$t('label.status'), 'jobstatus')
      ]
      if (!this.resourceType) {
        columns.push(column('jobinstancetype', this.$t('label.resourcetype'), 'jobinstance'))
        columns.push(column('jobinstancename', this.$t('label.resource'), 'resource'))
      }
      columns.push(
        column('account', this.$t('label.account'), 'account'),
        column('domainpath', this.$t('label.domain'), 'domainpath'))
      if (!this.scopedToManagementServer) {
        columns.push(column('managementservername', this.$t('label.managementservername')))
      }
      columns.push(
        column('created', this.$t('label.created'), 'created'),
        column('completed', this.$t('label.completed'), 'completed'),
        column('jobresult', this.$t('label.jobresult'), 'jobresult'))
      if (this.showActions || this.canListEvents) {
        columns.push({
          key: 'actions',
          title: this.$t('label.actions'),
          dataIndex: 'actions',
          width: 160,
          slots: { customRender: 'actions' }
        })
      }
      return columns
    },
    columns () {
      const columns = this.allColumns.filter(column => this.selectedColumns.includes(column.key))
      columns.push({
        key: 'filtercolumn',
        dataIndex: 'filtercolumn',
        title: '',
        customFilterDropdown: true,
        width: columns.length === 0 ? 'auto' : 5
      })
      return columns
    },
    columnsStoreKey () {
      return 'asyncjobs:' + (this.resourceType || (this.scopedToManagementServer ? 'managementserver' : 'all'))
    },
    showActions () {
      return isAdmin() && 'cancelAsyncJob' in this.$store.getters.apis
    },
    canListEvents () {
      return 'listEvents' in this.$store.getters.apis
    },
    scopedToManagementServer () {
      return !this.resourceType && !!(this.resource && this.resource.id)
    }
  },
  created () {
    this.loadSelectedColumns()
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
      if (this.resourceType && !(this.resource && this.resource.id)) {
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
      } else if (this.scopedToManagementServer) {
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
      if (!this.resourceType) {
        for (const key of ['domainid', 'account', 'resourcetype']) {
          if (this.searchParams[key]) {
            params[key] = this.searchParams[key]
          }
        }
        if (this.searchParams.searchQuery) {
          params.keyword = this.searchParams.searchQuery
        }
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
    loadSelectedColumns () {
      const userId = this.$store.getters.userInfo.id
      const saved = this.$store.getters.customColumns?.[userId]?.[this.columnsStoreKey]
      const keys = this.allColumns.map(column => column.key)
      this.selectedColumns = saved ? keys.filter(key => saved.includes(key)) : keys
    },
    updateSelectedColumns (key) {
      if (this.selectedColumns.includes(key)) {
        this.selectedColumns = this.selectedColumns.filter(x => x !== key)
      } else {
        this.selectedColumns = [...this.selectedColumns, key]
      }
      const userId = this.$store.getters.userInfo.id
      const customColumns = this.$store.getters.customColumns || {}
      customColumns[userId] = customColumns[userId] || {}
      customColumns[userId][this.columnsStoreKey] = this.selectedColumns
      this.$store.dispatch('SetCustomColumns', customColumns)
    },
    resultText (text) {
      if (!text) {
        return '-'
      }
      if (typeof text === 'object') {
        return text.errortext || JSON.stringify(text)
      }
      return String(text)
    },
    prettyResult (text) {
      if (text && typeof text === 'object' && !text.errortext) {
        return JSON.stringify(text, null, 2)
      }
      return this.resultText(text)
    },
    onFilterChange () {
      this.page = 1
      this.fetchData()
    },
    onSearch (opts) {
      this.searchParams = opts && Object.keys(opts).length > 0 ? opts : {}
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
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
  cursor: default;
}
</style>

<style lang="less">
.job-result-popover .ant-popover-inner-content {
  padding: 8px 12px;
}
.job-result-full {
  max-width: 640px;
  max-height: 420px;
  overflow: auto;

  .job-result-copy {
    display: inline-block;
    margin-bottom: 6px;
  }

  pre {
    margin: 0;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
