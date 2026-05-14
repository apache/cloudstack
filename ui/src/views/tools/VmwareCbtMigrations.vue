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
  <a-row :gutter="12">
    <a-col :md="24" :lg="24">
      <a-card class="instances-card">
        <template #title>
          {{ $t('label.vmware.cbt.migrations') }}
          <a-tooltip :title="$t('message.vmware.cbt.migrations')">
            <info-circle-outlined />
          </a-tooltip>
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="this.$emit('fetch-vmware-cbt-migrations')" >
            <template #icon><reload-outlined /></template>
          </a-button>
          <span style="float: right; width: 50%">
            <a-select
              :placeholder="$t('label.filterby')"
              :value="filter"
              style="min-width: 140px; margin-left: 10px; margin-bottom: 5px"
              size=small
              @change="onFilterChange"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
            >
              <template #suffixIcon><filter-outlined class="ant-select-suffix" /></template>
              <a-select-option
                v-for="filter in filters"
                :key="filter"
                :label="getFilterLabel(filter)">
                {{ getFilterLabel(filter) }}
              </a-select-option>
            </a-select>
          </span>
        </template>
        <a-table
          :data-source="migrations"
          class="instances-card-table"
          size="middle"
          :pagination="false"
          :rowKey="record => record.id"
          :columns="columns">
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.key === 'displayname'">
              <router-link v-if="record.virtualmachineid" :to="{ path: '/vm/' + record.virtualmachineid }">{{ record.displayname }}</router-link>
              <span v-else>{{ record.displayname }}</span>
            </template>
            <template v-else-if="column.key === 'convertinstancehostid'">
              <router-link v-if="record.convertinstancehostid" :to="{ path: '/host/' + record.convertinstancehostid }">{{ record.convertinstancehostname }}</router-link>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'created'">
              <span>{{ $toLocaleDate(record.created) }}</span>
            </template>
            <template v-else-if="['lastchangedbytes', 'totalchangedbytes'].includes(column.key)">
              <span v-if="text">{{ $bytesToHumanReadableSize(text) }}</span>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'lastdirtyrate'">
              <span v-if="text">{{ $bytesToHumanReadableSize(text) }}/s</span>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm
                :title="$t('message.confirm.cancel.vmware.cbt.migration')"
                :okText="$t('label.ok')"
                :cancelText="$t('label.cancel')"
                @confirm="() => this.$emit('cancel-vmware-cbt-migration', record)">
                <a-button
                  v-if="canCancel(record)"
                  size="small"
                  :danger="true"
                  :disabled="!('cancelVmwareCbtMigration' in $store.getters.apis)">
                  {{ $t('label.cancel') }}
                </a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
        <div class="instances-card-footer">
          <a-pagination
            class="row-element"
            size="small"
            :current="page"
            :pageSize="pageSize"
            :total="total"
            :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
            @change="onPaginationChange"
            showQuickJumper>
            <template #buildOptionText="props">
              <span>{{ props.value }} / {{ $t('label.page') }}</span>
            </template>
          </a-pagination>
        </div>
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
export default {
  name: 'VmwareCbtMigrations',
  props: {
    migrations: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      required: false
    },
    filter: {
      type: String,
      required: false
    },
    total: {
      type: Number,
      required: true
    },
    page: {
      type: Number,
      required: true
    },
    pageSize: {
      type: Number,
      required: true
    }
  },
  data () {
    const columns = [
      {
        key: 'created',
        title: this.$t('label.created'),
        dataIndex: 'created'
      },
      {
        key: 'displayname',
        title: this.$t('label.displayname'),
        dataIndex: 'displayname'
      },
      {
        key: 'sourcevmname',
        title: this.$t('label.sourcevmname'),
        dataIndex: 'sourcevmname'
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'state'
      },
      {
        key: 'currentstep',
        title: this.$t('label.currentstep'),
        dataIndex: 'currentstep'
      },
      {
        key: 'completedcycles',
        title: this.$t('label.completedcycles'),
        dataIndex: 'completedcycles'
      },
      {
        key: 'quietcycles',
        title: this.$t('label.quietcycles'),
        dataIndex: 'quietcycles'
      },
      {
        key: 'lastchangedbytes',
        title: this.$t('label.lastchangedbytes'),
        dataIndex: 'lastchangedbytes'
      },
      {
        key: 'lastdirtyrate',
        title: this.$t('label.lastdirtyrate'),
        dataIndex: 'lastdirtyrate'
      },
      {
        key: 'totalchangedbytes',
        title: this.$t('label.totalchangedbytes'),
        dataIndex: 'totalchangedbytes'
      },
      {
        key: 'convertinstancehostid',
        title: this.$t('label.conversionhost'),
        dataIndex: 'convertinstancehostid'
      },
      {
        key: 'vcenter',
        title: this.$t('label.vcenter'),
        dataIndex: 'vcenter'
      },
      {
        key: 'datacentername',
        title: this.$t('label.vcenter.datacenter'),
        dataIndex: 'datacentername'
      },
      {
        key: 'sourcehost',
        title: this.$t('label.sourcehost'),
        dataIndex: 'sourcehost'
      },
      {
        key: 'sourcecluster',
        title: this.$t('label.sourcecluster'),
        dataIndex: 'sourcecluster'
      },
      {
        key: 'lasterror',
        title: this.$t('label.lasterror'),
        dataIndex: 'lasterror'
      },
      {
        key: 'action',
        title: this.$t('label.action'),
        dataIndex: 'action'
      }
    ]
    return {
      columns,
      filters: ['all', 'Created', 'InitialSync', 'Replicating', 'ReadyForCutover', 'CuttingOver', 'Completed', 'Failed', 'Cancelled']
    }
  },
  methods: {
    canCancel (record) {
      return !['Completed', 'Failed', 'Cancelled'].includes(record.state)
    },
    getFilterLabel (filter) {
      return filter === 'all' ? this.$t('label.all') : filter
    },
    onFilterChange (e) {
      this.$emit('change-filter', e)
    },
    onPaginationChange (page, size) {
      this.$emit('change-pagination', page, size)
    }
  }
}
</script>
