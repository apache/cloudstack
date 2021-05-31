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
  <div class="form">
    <a-input-search
      :placeholder="$t('label.search')"
      v-model="searchQuery"
      style="margin-bottom: 10px;"
      @search="fetchData"
      autoFocus />
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="hosts"
      :pagination="false"
      :rowKey="record => record.id">
      <div slot="suitability" slot-scope="record">
        <a-icon
          class="host-item__suitability-icon"
          type="check-circle"
          theme="twoTone"
          twoToneColor="#52c41a"
          v-if="record.suitableformigration" />
        <a-icon
          class="host-item__suitability-icon"
          type="close-circle"
          theme="twoTone"
          twoToneColor="#f5222d"
          v-else />
      </div>
      <div slot="memused" slot-scope="record">
        {{ record.memoryused | byteToGigabyte }} GB
      </div>
      <div slot="memoryallocatedpercentage" slot-scope="record">
        {{ record.memoryallocatedpercentage }}
      </div>
      <div slot="cluster" slot-scope="record">
        {{ record.clustername }}
      </div>
      <div slot="pod" slot-scope="record">
        {{ record.podname }}
      </div>
      <div slot="requiresstoragemigration" slot-scope="record">
        {{ record.requiresStorageMotion ? $t('label.yes') : $t('label.no') }}
      </div>
      <template slot="select" slot-scope="record">
        <a-radio
          class="host-item__radio"
          @click="selectedHost = record"
          :checked="record.id === selectedHost.id"
          :disabled="!record.suitableformigration"></a-radio>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <div style="margin-top: 20px; display: flex; justify-content:flex-end;">
      <a-button type="primary" :disabled="!selectedHost.id" @click="submitForm">
        {{ $t('label.ok') }}
      </a-button>
    </div>
  </div>

</template>

<script>
import { api } from '@/api'

export default {
  name: 'VMMigrateWizard',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: true,
      hosts: [],
      selectedHost: {},
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.suitability'),
          scopedSlots: { customRender: 'suitability' }
        },
        {
          title: this.$t('label.cpuused'),
          dataIndex: 'cpuused'
        },
        {
          title: this.$t('label.memoryallocated'),
          scopedSlots: { customRender: 'memoryallocatedpercentage' }
        },
        {
          title: this.$t('label.memused'),
          scopedSlots: { customRender: 'memused' }
        },
        {
          title: this.$t('label.cluster'),
          scopedSlots: { customRender: 'cluster' }
        },
        {
          title: this.$t('label.pod'),
          scopedSlots: { customRender: 'pod' }
        },
        {
          title: this.$t('label.storage.migration.required'),
          scopedSlots: { customRender: 'requiresstoragemigration' }
        },
        {
          title: this.$t('label.select'),
          scopedSlots: { customRender: 'select' }
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('findHostsForMigration', {
        virtualmachineid: this.resource.id,
        keyword: this.searchQuery,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.hosts = response.findhostsformigrationresponse.host || []
        this.hosts.sort((a, b) => {
          return b.suitableformigration - a.suitableformigration
        })
        this.totalCount = response.findhostsformigrationresponse.count
      }).catch(error => {
        this.$message.error(`${this.$t('message.load.host.failed')}: ${error}`)
      }).finally(() => {
        this.loading = false
      })
    },
    submitForm () {
      this.loading = true
      var isUserVm = true
      if (this.$route.meta.name !== 'vm') {
        isUserVm = false
      }
      var migrateApi = isUserVm
        ? this.selectedHost.requiresStorageMotion ? 'migrateVirtualMachineWithVolume' : 'migrateVirtualMachine'
        : 'migrateSystemVm'
      api(migrateApi, {
        hostid: this.selectedHost.id,
        virtualmachineid: this.resource.id
      }).then(response => {
        var migrateResponse = isUserVm
          ? this.selectedHost.requiresStorageMotion ? response.migratevirtualmachinewithvolumeresponse : response.migratevirtualmachineresponse
          : response.migratesystemvmresponse
        this.$store.dispatch('AddAsyncJob', {
          title: `${this.$t('label.migrating')} ${this.resource.name}`,
          jobid: migrateResponse.jobid,
          description: this.resource.name,
          status: 'progress'
        })
        this.$pollJob({
          jobId: migrateResponse.jobid,
          successMessage: `${this.$t('message.success.migrating')} ${this.resource.name}`,
          successMethod: () => {
            this.$emit('close-action')
          },
          errorMessage: this.$t('message.migrating.failed'),
          errorMethod: () => {
            this.$emit('close-action')
          },
          loadingMessage: `${this.$t('message.migrating.processing')} ${this.resource.name}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.$emit('close-action')
          }
        })
        this.$emit('close-action')
      }).catch(error => {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
          duration: 0
        })
      }).finally(() => {
        this.loading = false
      })
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  },
  filters: {
    byteToGigabyte: value => {
      return (value / Math.pow(10, 9)).toFixed(2)
    }
  }
}
</script>

<style scoped lang="scss">

  .form {
    width: 95vw;
    @media (min-width: 900px) {
      width: 850px;
    }
  }

  .host-item {
    padding-right: 20px;
    padding-bottom: 0;
    padding-left: 20px;

    &--selected {
      background-color: #e6f7ff;
    }

    &__row {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 760px) {
        flex-direction: row;
      }
    }

    &__value {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      flex: 1;
      margin-bottom: 10px;

      &--small {

        @media (min-width: 760px) {
          flex: none;
          margin-right: 40px;
          margin-left: 40px;
        }
      }
    }

    &__title {
      font-weight: bold;
    }

    &__suitability-icon {
      margin-top: 5px;
    }

    &__radio {
      display: flex;
      align-items: center;
    }

  }

  .pagination {
    margin-top: 20px;
  }
</style>
