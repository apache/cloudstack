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
  <a-row>
    {{ $t('message.drs.plan.description') + ' ' + algorithm }}
  </a-row>
  <a-row>
  <a-col :span="12">
    <a-slider v-model:value="iterations" :min="0.01" :max="1" :step="0.01" />
  </a-col>
  <a-col :span="4">
    <a-input-number
            v-model:value="iterations"
            :min="0.01"
            :max="1"
            :step="0.01"
            style="margin-left: 16px"
          />
  </a-col>
  <a-col :span="6">
    <a-popconfirm
          title="Are you sure you want to execute this plan?"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.cancel')"
          @confirm="generateDrsPlan()"
          >
    <a-button
        type="dashed"
        @click="showAddModal"
        :loading="loading"
        :disabled="!('generateClusterDrsPlan' in $store.getters.apis)">
        {{ $t('label.cluster.drs.generate') }}
    </a-button>
  </a-popconfirm>
  </a-col>
  </a-row>
  <a-table
    size="medium"
    :columns="drsPlanColumns"
    :dataSource="drsPlans"
    :rowKey="item => item.id"
    :pagination="true"
  >
    <template #expandedRowRender="{ record }">
      <a-table
        size="small"
        :columns="migrationColumns"
        :dataSource="record.migrations"
        :rowKey="(record, index) => index"
        :pagination="true">
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'vm'">
            <router-link :to="{ path: '/vm/' + record.vm }">
              {{ record.vm.displayname }}
            </router-link>
          </template>
          <template v-else-if="column.key === 'sourcehost'">
            <router-link :to="{ path: '/host/' + record.sourcehost }">
              {{ record.sourcehost.name }}
            </router-link>
          </template>
          <template v-else-if="column.key === 'destinationhost'">
            <router-link :to="{ path: '/host/' + record.destinationhost }">
              {{ record.destinationhost.name }}
            </router-link>
          </template>
          <template v-else>
            {{ text }}
          </template>
        </template>
      </a-table>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.key === 'actions' && record.type === 'MANUAL' && record.status === 'UNDER_REVIEW'">
        <a-popconfirm
          title="Are you sure you want to execute this plan?"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.cancel')"
          @confirm="executeDrsPlan(record)"
          >
          <a href="#">{{ $t('label.execute') }}</a>
        </a-popconfirm>
      </template>
      <template v-else-if="column.key === 'created'">
        {{ $toLocaleDate(text) }}
      </template>
      <template v-else-if="column.key !== 'actions'">
        {{ text }}
      </template>
    </template>
  </a-table>
</template>

<script>

import { reactive } from 'vue'
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'NicsTable',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon,
    TooltipButton,
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      drsPlanColumns: [
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.status'),
          dataIndex: 'status'
        },
        {
          key: 'created',
          title: this.$t('label.created'),
          dataIndex: 'created'
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ],
      migrationColumns: [
        {
          key: 'vm',
          title: this.$t('label.vm'),
          dataIndex: 'vm'
        },
        {
          key: 'sourcehost',
          title: this.$t('label.source.host'),
          dataIndex: 'sourcehost'
        },
        {
          key: 'destinationhost',
          title: this.$t('label.destination.host'),
          dataIndex: 'created'
        },
        {
          key: 'jobstatus',
          title: this.$t('label.destination.host'),
          dataIndex: 'jobstatus'
        }
      ],
      loading: false,
      drsPlans: [],
      algorithm: '',
      iterations: 0
    }
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem, oldItem) {
        if (newItem && (!oldItem || (newItem.id !== oldItem.id))) {
          this.fetchDRSPlans()
        }
      }
    }
  },
  created () {
    // this.algorithm = reactive('')
    // this.iterations = reactive(0)
    this.fetchDRSPlans()
    this.fetchDrsConfig()
  },
  methods: {
    fetchDRSPlans () {
      if (!this.resource || !this.resource.id) return
      api('listClusterDrsPlan', { page: 1, pageSize: 500, clusterid: this.resource.id }).then(json => {
        this.drsPlans = json.listclusterdrsplanresponse.drsPlan
      })
    },
    executeDrsPlan (record) {
      api('executeClusterDrsPlan', { id: record.id }).then(json => {
        this.$message.success(this.$t('message.drs.plan.executed'))
        this.fetchDRSPlans()
      })
    },
    generateDrsPlan () {
      api('generateClusterDrsPlan', { id: this.resource.id, iterations: this.iterations }).then(json => {
        this.$message.success(this.$t('message.drs.plan.executed'))
        this.fetchDRSPlans()
      })
    },
    fetchDrsConfig () {
      this.loading = true
      api('listConfigurations', { clusterid: this.resource.id, name: 'drs.algorithm' }).then(json => {
        this.algorithm = reactive(json.listconfigurationsresponse.configuration[0].value)
        api('listConfigurations', { clusterid: this.resource.id, name: 'drs.iterations' }).then(json => {
          this.iterations = reactive(json.listconfigurationsresponse.configuration[0].value)
          this.loading = false
        })
      })
    }
  }
}
</script>
