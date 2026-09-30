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
    <a-select
      v-model:value="severity"
      style="width: 200px; margin-bottom: 12px"
      @change="onFilterChange">
      <a-select-option value="">{{ $t('label.severity') }}: {{ $t('label.all') }}</a-select-option>
      <a-select-option v-for="s in severities" :key="s" :value="s">{{ s }}</a-select-option>
    </a-select>
    <a-table
      size="small"
      :columns="columns"
      :dataSource="alerts"
      :rowKey="item => item.id"
      :loading="tabLoading"
      :pagination="pagination"
      @change="onTableChange">
      <template #bodyCell="{ column, text }">
        <template v-if="column.key === 'alerttimestamp'">
          {{ $toLocaleDate(text) }}
        </template>
        <template v-else-if="column.key === 'metricvalue'">
          {{ Number(text).toFixed(2) }}
        </template>
        <template v-else-if="column.key === 'metrictype'">
          {{ resourceAlertLabel('metrictype', text) }}
        </template>
        <template v-else-if="column.key === 'severity'">
          <a-tag :color="severityColor(text)">{{ text }}</a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import { resourceAlertLabel } from '@/views/resourcealert/resourceAlertOptions'

// Shows the alerts of one resource, or of one rule when no resourceType is given.
export default {
  name: 'ResourceAlertsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    const firstColumn = this.resourceType
      ? { title: this.$t('label.alertrulename'), dataIndex: 'alertrulename', key: 'alertrulename' }
      : { title: this.$t('label.resourcename'), dataIndex: 'resourcename', key: 'resourcename' }
    return {
      alerts: [],
      tabLoading: false,
      severity: '',
      severities: ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'],
      page: 1,
      pageSize: 20,
      total: 0,
      columns: [
        firstColumn,
        { title: this.$t('label.metrictype'), dataIndex: 'metrictype', key: 'metrictype' },
        { title: this.$t('label.metricvalue'), dataIndex: 'metricvalue', key: 'metricvalue' },
        { title: this.$t('label.severity'), dataIndex: 'severity', key: 'severity' },
        { title: this.$t('label.alerttimestamp'), dataIndex: 'alerttimestamp', key: 'alerttimestamp' },
        { title: this.$t('label.message'), dataIndex: 'message', key: 'message' }
      ]
    }
  },
  computed: {
    pagination () {
      return {
        current: this.page,
        pageSize: this.pageSize,
        total: this.total,
        showSizeChanger: true,
        showTotal: total => `${this.$t('label.total')} ${total} ${this.$t('label.items')}`
      }
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      handler (newItem, oldItem) {
        if (newItem?.id !== oldItem?.id) {
          this.page = 1
          this.severity = ''
        }
        this.fetchData()
      }
    }
  },
  methods: {
    resourceAlertLabel,
    fetchData () {
      if (!this.resource || !this.resource.id) return
      const params = { listall: true, page: this.page, pagesize: this.pageSize }
      if (this.resourceType) {
        params.resourcetype = this.resourceType
        params.resourceid = this.resource.id
      } else {
        params.alertruleid = this.resource.id
      }
      if (this.severity) {
        params.severity = this.severity
      }
      this.tabLoading = true
      getAPI('listResourceAlerts', params).then(json => {
        const response = json?.listresourcealertsresponse || {}
        this.alerts = response.resourcealert || []
        this.total = response.count || 0
      }).finally(() => {
        this.tabLoading = false
      })
    },
    onFilterChange () {
      this.page = 1
      this.fetchData()
    },
    onTableChange (pagination) {
      this.page = pagination.current
      this.pageSize = pagination.pageSize
      this.fetchData()
    },
    severityColor (severity) {
      const map = { CRITICAL: 'red', HIGH: 'orange', MEDIUM: 'gold', LOW: 'blue' }
      return map[severity] || 'default'
    }
  }
}
</script>
