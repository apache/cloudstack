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
      placeholder="Search"
      v-model="searchQuery"
      style="margin-bottom: 10px;"
      @search="fetchData" />
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
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger/>

    <div style="margin-top: 20px; display: flex; justify-content:flex-end;">
      <a-button type="primary" :disabled="!selectedHost.id" @click="submitForm">
        {{ $t('ok') }}
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
          title: this.$t('name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('Suitability'),
          scopedSlots: { customRender: 'suitability' }
        },
        {
          title: this.$t('cpuused'),
          dataIndex: 'cpuused'
        },
        {
          title: this.$t('memused'),
          scopedSlots: { customRender: 'memused' }
        },
        {
          title: this.$t('select'),
          scopedSlots: { customRender: 'select' }
        }
      ]
    }
  },
  mounted () {
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
        this.hosts = response.findhostsformigrationresponse.host
        this.totalCount = response.findhostsformigrationresponse.count
        if (this.totalCount > 0) {
          this.totalCount -= 1
        }
      }).catch(error => {
        this.$message.error('Failed to load hosts: ' + error)
      }).finally(() => {
        this.loading = false
      })
    },
    submitForm () {
      this.loading = true
      api(this.selectedHost.requiresStorageMotion ? 'migrateVirtualMachineWithVolume' : 'migrateVirtualMachine', {
        hostid: this.selectedHost.id,
        virtualmachineid: this.resource.id
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `Migrating ${this.resource.name}`,
          jobid: response.migratevirtualmachineresponse.jobid,
          description: this.resource.name,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.migratevirtualmachineresponse.jobid,
          successMessage: `Migration completed successfully for ${this.resource.name}`,
          successMethod: () => {
            this.$parent.$parent.close()
          },
          errorMessage: 'Migration failed',
          errorMethod: () => {
            this.$parent.$parent.close()
          },
          loadingMessage: `Migration in progress for ${this.resource.name}`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.$parent.$parent.close()
          }
        })
        this.$parent.$parent.close()
      }).catch(error => {
        console.error(error)
        this.$message.error(`Failed to migrate VM to host ${this.selectedHost.name}`)
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
    width: 85vw;
    @media (min-width: 800px) {
      width: 750px;
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
