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
    <a-table
      size="small"
      :columns="columns"
      :dataSource="alerts"
      :rowKey="item => item.id"
      :loading="tabLoading"
      :pagination="{ pageSize: 20, showSizeChanger: true }">
      <template #bodyCell="{ column, text }">
        <template v-if="column.key === 'alerttimestamp'">
          {{ $toLocaleDate(text) }}
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

export default {
  name: 'ResourceAlertsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      alerts: [],
      tabLoading: false,
      columns: [
        { title: this.$t('label.metrictype'), dataIndex: 'metrictype', key: 'metrictype' },
        { title: this.$t('label.metricvalue'), dataIndex: 'metricvalue', key: 'metricvalue' },
        { title: this.$t('label.severity'), dataIndex: 'severity', key: 'severity' },
        { title: this.$t('label.alerttimestamp'), dataIndex: 'alerttimestamp', key: 'alerttimestamp' },
        { title: this.$t('label.message'), dataIndex: 'message', key: 'message' }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      if (!this.resource || !this.resource.id) return
      this.tabLoading = true
      getAPI('listResourceAlerts', { resourceid: this.resource.id, listall: true }).then(json => {
        this.alerts = json?.listresourcealertsresponse?.resourcealert || []
      }).finally(() => {
        this.tabLoading = false
      })
    },
    severityColor (severity) {
      const map = { CRITICAL: 'red', HIGH: 'orange', MEDIUM: 'gold', LOW: 'blue' }
      return map[severity] || 'default'
    }
  }
}
</script>
