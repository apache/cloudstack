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
    <a-row :gutter="12">
      <a-col :md="24">
        <a-card class="breadcrumb-card">
          <a-row>
            <a-col :span="24" style="padding-left: 12px">
              <breadcrumb>
                <a-tooltip placement="bottom" slot="end">
                  <template slot="title">{{ $t('label.refresh') }}</template>
                  <a-button
                    style="margin-top: 4px"
                    :loading="loading"
                    shape="round"
                    size="small"
                    icon="reload"
                    @click="fetchDetails()"
                  >{{ $t('label.refresh') }}</a-button>
                </a-tooltip>
              </breadcrumb>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
    <a-divider type="vertical"/>
    <a-row>
      <a-col
        :md="24">
        <a-card>
          <a-alert type="info" :showIcon="true" :message="$t('label.desc.db.stats')"/>
          <div v-cloak class="resource-detail-item">
            <template v-for="metric of dbMetrics">
              <span style="margin-right:5px" :key="metric.name">
                <div> {{ metric.name }} </div>
                <div> {{ metric.value }} </div>
              </span>
            </template>
          </div>
        </a-card>
      </a-col>
    </a-row>
    <a-divider />
    <a-row>
      <a-col
        :md="24">
        <a-card>
          <a-alert type="info" :showIcon="true" :message="$t('label.desc.usage.stats')"/>
          <div v-cloak class="resource-detail-item">
            <template v-for="metric of usageMetrics">
              <span style="margin-right:5px" :key="metric.name">
                <div> {{ metric.name }} </div>
                <div> {{ metric.value }} </div>
              </span>
            </template>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import Breadcrumb from '@/components/widgets/Breadcrumb'

export default {
  name: 'Metrics',
  components: {
    Breadcrumb
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      dbMetrics: [],
      usageMetrics: [],
      columns: ['name', 'value']
    }
  },
  created () {
    this.fetchDetails()
  },
  watch: {
  },
  methods: {
    fetchDbMetrics () {
      var metrics
      api('listDbMetrics').then(json => {
        metrics = this.mapToArray(json.listdbmetricsresponse.dbMetrics)
        this.dbMetrics = metrics
      })
      return metrics
    },
    fetchUsageMetrics () {
      var metrics
      api('listUsageServerMetrics').then(json => {
        metrics = this.mapToArray(json.listusageservermetricsresponse.usageMetrics)
        this.usageMetrics = metrics
      })
      return metrics
    },
    mapToArray (map) {
      /* eslint-disable no-unused-vars */
      var array = []
      for (var key in map) {
        var metric = {}
        metric.name = key
        metric.value = map[key]
        array.push(metric)
      }
      /* eslint-enable no-unused-vars */
      return array
    },
    fetchDetails () {
      this.fetchDbMetrics()
      this.usageMetrics = this.fetchUsageMetrics()
    },
    fetchUsageListData () {
      this.columns = []
      this.columns.push({
        dataIndex: 'name',
        title: this.$t('label.name'),
        sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
      })

      this.columns.push({
        dataIndex: 'value',
        title: this.$t('label.value'),
        sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
      })
    }
  }
}
</script>

<style scoped lang="less">
  .metric-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }
</style>
