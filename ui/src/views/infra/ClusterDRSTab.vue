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
    {{ $t('message.drs.plan.description') }}
  </a-row>
  <a-row>
    <strong>{{ $t('label.algorithm') }}:</strong>&nbsp;{{ algorithm }}
  </a-row>
  <br/>
  <a-row>
    <a-col>
      <a-input-number
          v-model:value="maxMigrations"
          :addonBefore="$t('label.max.migrations')"
          :min="1"
          :step="1"
        />
        &nbsp;&nbsp;
    </a-col>
    <a-col>
      <a-button
        type="primary"
        @click="generateDrsPlan"
        :loading="loading"
        :disabled="!('generateClusterDrsPlan' in $store.getters.apis)">
        {{ $t('label.drs.generate.plan') }}
      </a-button>
    </a-col>
  </a-row>
  <br/>
  <a-table
    size="small"
    :columns="drsPlanColumns"
    :dataSource="drsPlans"
    :rowKey="item => item.id"
    :pagination="{hideOnSinglePage: true, showSizeChanger: true}"
  >
    <template #expandedRowRender="{ record }">
      <a-table
        size="small"
        :columns="migrationColumns"
        :dataSource="record.migrations"
        :rowKey="(record, index) => index"
        :pagination="{hideOnSinglePage: true, showSizeChanger: true}">
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'vm'">
            <router-link :to="{ path: '/vm/' + record.virtualmachineid }">
              <desktop-outlined/> {{ record.virtualmachinename }}
            </router-link>
          </template>
          <template v-else-if="column.key === 'sourcehost'">
            <router-link :to="{ path: '/host/' + record.sourcehostid }">
              <cluster-outlined/> {{ record.sourcehostname }}
            </router-link>
          </template>
          <template v-else-if="column.key === 'destinationhost'">
            <router-link :to="{ path: '/host/' + record.destinationhostid }">
              <cluster-outlined/> {{ record.destinationhostname }}
            </router-link>
          </template>
          <template v-else>
            {{ text }}
          </template>
        </template>
      </a-table>
      <br/>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="column.key === 'successfulMigrations'">
        {{  text.migrations.filter(m => m.jobstatus === 'SUCCEEDED').length }} / {{  text.migrations.length }}
        <!-- {{  text.migrations }} -->
      </template>
      <template v-else-if="column.key === 'created'">
        {{ $toLocaleDate(text) }}
      </template>
      <template v-else-if="column.key === 'eventid'" >
        <router-link :to="{ path: '/event', query: { startid: text} }" target="_blank">
          <schedule-outlined /> {{ $t('label.events') }}
        </router-link>
      </template>
      <template v-else>
        {{ text }}
      </template>
    </template>
  </a-table>

  <a-modal
    width="50%"
    :visible="showModal"
    :title="$t('label.drs.plan')"
    :maskClosable="false"
    :closable="true"
    :okButtonProps="{ style: { display: generatedMigrations.length === 0 ? 'none' : null } }"
    :okText="$t('label.execute')"
    :cancelText="$t('label.cancel')"
    @ok="executeDrsPlan"
    @cancel="closeModal">
    <a-table
      v-if="generatedMigrations.length > 0"
      size="small"
      :columns="generatedPlanMigrationColumns"
      :dataSource="generatedMigrations"
      :rowKey="(record, index) => index"
      :pagination="{ showTotal: (total, range) => [range[0], '-', range[1], $t('label.of'), total, $t('label.items')].join(' ') }" >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'vm'">
          <router-link :to="{ path: '/vm/' + record.virtualmachineid }">
            <desktop-outlined/> {{ record.virtualmachinename }}
          </router-link>
        </template>
        <template v-else-if="column.key === 'sourcehost'">
          <router-link :to="{ path: '/host/' + record.sourcehostid }">
            <cluster-outlined/> {{ record.sourcehostname }}
          </router-link>
        </template>
        <template v-else-if="column.key === 'destinationhost'">
          <router-link :to="{ path: '/host/' + record.destinationhostid }">
            <cluster-outlined/> {{ record.destinationhostname }}
          </router-link>
        </template>
        <template v-else>
          {{ text }}
        </template>
      </template>
    </a-table>
    <a-p v-else>
      {{ $t('label.drs.no.plan.generated') }}
    </a-p>

  </a-modal>

</template>

<script>

import { reactive } from 'vue'
import { api } from '@/api'

export default {
  name: 'ClusterDrsTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    const generatedPlanMigrationColumns = [
      {
        key: 'vm',
        title: this.$t('label.vm'),
        dataIndex: 'vm',
        ellipsis: true
      },
      {
        key: 'sourcehost',
        title: this.$t('label.sourcehost'),
        dataIndex: 'sourcehost',
        ellipsis: true
      },
      {
        key: 'destinationhost',
        title: this.$t('label.desthost'),
        dataIndex: 'created',
        ellipsis: true
      }
    ]
    return {
      drsPlanColumns: [
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.success.migrations'),
          key: 'successfulMigrations'
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
          key: 'eventid',
          title: this.$t('label.events'),
          dataIndex: 'eventid'
        }
      ],
      generatedPlanMigrationColumns: generatedPlanMigrationColumns,
      migrationColumns: generatedPlanMigrationColumns.concat([
        {
          key: 'jobstatus',
          title: this.$t('label.status'),
          dataIndex: 'jobstatus'
        }
      ]),
      loading: false,
      drsPlans: [],
      algorithm: '',
      maxMigrations: 0,
      generatedMigrations: reactive([]),
      showModal: false
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
    executeDrsPlan () {
      if (this.generatedMigrations.length === 0) return

      var params = { id: this.resource.id }

      for (var i = 0; i < this.generatedMigrations.length; i++) {
        const mapping = this.generatedMigrations[i]
        params['migrateto[' + i + '].vm'] = mapping.virtualmachineid
        params['migrateto[' + i + '].host'] = mapping.destinationhostid
      }

      api('executeClusterDrsPlan', params).then(json => {
        this.$message.success(this.$t('message.drs.plan.executed'))
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.drs.plan.execution.failed'))
      }).finally(() => {
        this.fetchDRSPlans()
        this.closeModal()
      })
    },
    generateDrsPlan () {
      this.loading = true
      api('generateClusterDrsPlan', { id: this.resource.id, migrations: this.maxMigrations }).then(json => {
        this.generatedMigrations = json?.generateclusterdrsplanresponse?.generateclusterdrsplanresponse?.migrations || []
        this.loading = false
        this.showModal = true
      })
    },
    fetchDrsConfig () {
      this.loading = true
      api('listConfigurations', { clusterid: this.resource.id, name: 'drs.algorithm' }).then(json => {
        this.algorithm = json.listconfigurationsresponse.configuration[0].value
        api('listConfigurations', { clusterid: this.resource.id, name: 'drs.max.migrations' }).then(json => {
          this.maxMigrations = json.listconfigurationsresponse.configuration[0].value
          this.loading = false
        }).catch((err) => {
          console.error(err)
          this.loading = false
        })
      }).catch((err) => {
        console.error(err)
        this.loading = false
      })
    },
    closeModal () {
      this.showModal = false
      this.generatedMigrations = reactive([])
    }
  }
}
</script>
