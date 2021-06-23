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
  <div class="migrate-volume-container">
    <a-alert type="warning">
      <span slot="message" v-html="$t('message.migrate.volume')" />
    </a-alert>
    <a-input-search
      :placeholder="$t('label.search')"
      v-model="searchQuery"
      style="margin-top: 10px; margin-bottom: 10px;"
      @search="fetchStoragePools"
      autoFocus />
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="storagePools"
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
      <div slot="disksizeallocated" slot-scope="record">
        {{ record.disksizeallocated | byteToGigabyte }} GB
      </div>
      <div slot="disksizetotal" slot-scope="record">
        {{ record.disksizetotal | byteToGigabyte }} GB
      </div>
      <template slot="select" slot-scope="record">
        <a-radio
          @click="selectedPool = record"
          :checked="record.id === selectedPool.id"></a-radio>
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
    <template v-if="this.resource.virtualmachineid">
      <p class="modal-form__label" @click="replaceDiskOffering = !replaceDiskOffering" style="cursor:pointer;">
        {{ $t('label.usenewdiskoffering') }}
      </p>
      <a-checkbox v-model="replaceDiskOffering" />

      <template v-if="replaceDiskOffering">
        <p class="modal-form__label">{{ $t('label.newdiskoffering') }}</p>
        <a-select v-model="selectedDiskOffering" style="width: 100%;">
          <a-select-option v-for="(diskOffering, index) in diskOfferings" :value="diskOffering.id" :key="index">
            {{ diskOffering.displaytext }}
          </a-select-option>
        </a-select>
      </template>
    </template>
    <div style="margin-top: 20px; display: flex; justify-content:flex-end;">
      <a-button type="primary" :disabled="!selectedPool.id" @click="submitForm">
        {{ $t('label.ok') }}
      </a-button>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'MigrateVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: true,
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      storagePools: [],
      selectedPool: {},
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.clusterid'),
          dataIndex: 'clustername'
        },
        {
          title: this.$t('label.podid'),
          dataIndex: 'podname'
        },
        {
          title: this.$t('label.disksizeallocated'),
          scopedSlots: { customRender: 'disksizeallocated' }
        },
        {
          title: this.$t('label.disksizetotal'),
          scopedSlots: { customRender: 'disksizetotal' }
        },
        {
          title: this.$t('label.select'),
          scopedSlots: { customRender: 'select' }
        }
      ],
      diskOfferings: [],
      replaceDiskOffering: false,
      selectedDiskOffering: null
    }
  },
  created () {
    if (this.resource.virtualmachineid) {
      this.columns.splice(1, 0,
        {
          title: this.$t('label.suitability'),
          scopedSlots: { customRender: 'suitability' }
        }
      )
    }
    this.fetchStoragePools()
    this.resource.virtualmachineid && this.fetchDiskOfferings()
  },
  methods: {
    fetchStoragePools () {
      if (this.resource.virtualmachineid) {
        this.loading = true
        api('findStoragePoolsForMigration', {
          id: this.resource.id,
          keyword: this.searchQuery,
          page: this.page,
          pagesize: this.pageSize
        }).then(response => {
          this.storagePools = response.findstoragepoolsformigrationresponse.storagepool || []
          if (Array.isArray(this.storagePools) && this.storagePools.length) {
            this.selectedPool = this.storagePools[0].id || ''
          }
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        }).finally(() => {
          this.loading = false
        })
      } else {
        api('listStoragePools', {
          zoneid: this.resource.zoneid
        }).then(response => {
          this.storagePools = response.liststoragepoolsresponse.storagepool || []
          this.storagePools = this.storagePools.filter(pool => { return pool.id !== this.resource.storageid })
          if (Array.isArray(this.storagePools) && this.storagePools.length) {
            this.selectedPool = this.storagePools[0].id || ''
          }
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        }).finally(() => {
          this.loading = false
        })
      }
    },
    fetchDiskOfferings () {
      api('listDiskOfferings', {
        listall: true
      }).then(response => {
        this.diskOfferings = response.listdiskofferingsresponse.diskoffering
        this.selectedDiskOffering = this.diskOfferings[0].id
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchStoragePools()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchStoragePools()
    },
    closeModal () {
      this.$parent.$parent.close()
    },
    submitForm () {
      if (this.storagePools.length === 0) {
        this.closeModal()
        return
      }
      api('migrateVolume', {
        livemigrate: this.resource.vmstate === 'Running',
        storageid: this.selectedPool.id,
        volumeid: this.resource.id,
        newdiskofferingid: this.replaceDiskOffering ? this.selectedDiskOffering : null
      }).then(response => {
        this.$pollJob({
          jobId: response.migratevolumeresponse.jobid,
          successMessage: this.$t('message.success.migrate.volume'),
          successMethod: () => {
            this.parentFetchData()
          },
          errorMessage: this.$t('message.migrate.volume.failed'),
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.migrate.volume.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
          }
        })
        this.closeModal()
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      })
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
  .migrate-volume-container {
    width: 85vw;

    @media (min-width: 760px) {
      width: 500px;
    }
  }

  .pagination {
    margin-top: 20px;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;

    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }

  .modal-form {
    margin-top: -20px;

    &__label {
      margin-top: 10px;
      margin-bottom: 5px;
    }

  }
</style>
