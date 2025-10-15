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
          {{ $t('label.import.vm.tasks') }}
          <a-tooltip :title="$t('message.import.vm.tasks')">
            <info-circle-outlined />
          </a-tooltip>
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="this.$emit('fetch-import-vm-tasks')" >
            <template #icon><reload-outlined /></template>
          </a-button>
          <span style="float: right; width: 50%">
            <a-select
              :placeholder="$t('label.filterby')"
              :value="filter"
              style="min-width: 100px; margin-left: 10px; margin-bottom: 5px"
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
                  :label="$t('label.' + filter)"
                >
                  {{ $t('label.' + filter) }}
                </a-select-option>
              </a-select>
          </span>
        </template>
        <a-table
          :data-source="tasks"
          class="instances-card-table"
          size="middle"
          :pagination="false"
          :columns="columns">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'convertinstancehostid'">
                <router-link :to="{ path: '/host/' + record.convertinstancehostid }">{{ record.convertinstancehostname }}</router-link>
              </template>
              <template v-else-if="column.key === 'displayname'">
                <router-link v-if="record.virtualmachineid" :to="{ path: '/vm/' + record.virtualmachineid }">{{ record.displayname }}</router-link>
                <span v-else>{{ record.displayname }}</span>
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
  name: 'ImportVmTasks',
  components: {
  },
  props: {
    tasks: {
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
        key: 'displayname',
        title: 'VM Display Name',
        dataIndex: 'displayname'
      },
      {
        key: 'convertinstancehostid',
        title: 'Conversion Host',
        dataIndex: 'convertinstancehostid'
      },
      {
        key: 'step',
        title: 'Current Step',
        dataIndex: 'step'
      },
      {
        key: 'stepduration',
        title: 'Current Step Duration',
        dataIndex: 'stepduration'
      },
      {
        key: 'description',
        title: 'Description',
        dataIndex: 'description'
      },
      {
        key: 'duration',
        title: 'Total Duration',
        dataIndex: 'duration'
      },
      {
        key: 'sourcevmname',
        title: 'Source VM Name',
        dataIndex: 'sourcevmname'
      },
      {
        key: 'vcenter',
        title: 'vCenter',
        dataIndex: 'vcenter'
      },
      {
        key: 'datacentername',
        title: 'Datacenter Name',
        dataIndex: 'datacentername'
      }
    ]
    return {
      columns,
      filters: ['running', 'completed', 'failed'],
      filterValue: 'running'
    }
  },
  methods: {
    fetchData () {
      this.$emit('fetch-import-vm-tasks')
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
