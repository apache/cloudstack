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
  <div class="migrate-volume-container" v-ctrl-enter="submitMigrateVolume">
    <div class="modal-form">
      <a-alert class="top-spaced" type="warning">
        <span slot="message" v-html="$t('message.migrate.volume')" />
      </a-alert>
      <a-input-search
        class="top-spaced"
        :placeholder="$t('label.search')"
        v-model="searchQuery"
        style="margin-bottom: 10px;"
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
        <div slot="disksizetotal" slot-scope="record">
          <span v-if="record.disksizetotal">{{ record.disksizetotal | byteToGigabyte }} GB</span>
        </div>
        <div slot="disksizeused" slot-scope="record">
          <span v-if="record.disksizeused">{{ record.disksizeused | byteToGigabyte }} GB</span>
        </div>
        <div slot="disksizefree" slot-scope="record">
          <span v-if="record.disksizetotal && record.disksizeused">{{ (record.disksizetotal * 1 - record.disksizeused * 1) | byteToGigabyte }} GB</span>
        </div>
        <template slot="select" slot-scope="record">
          <a-radio
            @click="selectedStoragePool = record"
            :checked="record.id === selectedStoragePool.id"></a-radio>
        </template>
      </a-table>
      <a-pagination
        class="top-spaced"
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
      <div class="top-spaced" v-if="storagePools.length > 0">
        <template v-if="this.resource.virtualmachineid">
          <p class="modal-form__label" @click="replaceDiskOffering = !replaceDiskOffering" style="cursor:pointer;">
            {{ $t('label.usenewdiskoffering') }}
          </p>
          <a-checkbox v-model="replaceDiskOffering" />

          <template v-if="replaceDiskOffering">
            <p class="modal-form__label">{{ $t('label.newdiskoffering') }}</p>
            <a-select
              v-model="selectedDiskOffering"
              style="width: 100%;"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="(diskOffering, index) in diskOfferings" :value="diskOffering.id" :key="index">
                {{ diskOffering.displaytext }}
              </a-select-option>
            </a-select>
          </template>
        </template>
      </div>
    </div>

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('label.cancel') }}
      </a-button>
      <a-button type="primary" ref="submit" @click="submitMigrateVolume">
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
      loading: false,
      storagePools: [],
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      selectedStoragePool: null,
      diskOfferings: [],
      replaceDiskOffering: false,
      selectedDiskOffering: null,
      isSubmitted: false,
      columns: [
        {
          title: this.$t('label.storageid'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.suitability'),
          scopedSlots: { customRender: 'suitability' }
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
          title: this.$t('label.disksizetotal'),
          scopedSlots: { customRender: 'disksizetotal' }
        },
        {
          title: this.$t('label.disksizeused'),
          scopedSlots: { customRender: 'disksizeused' }
        },
        {
          title: this.$t('label.disksizefree'),
          scopedSlots: { customRender: 'disksizefree' }
        },
        {
          title: this.$t('label.select'),
          scopedSlots: { customRender: 'select' }
        }
      ]
    }
  },
  created () {
    this.fetchStoragePools()
    this.resource.virtualmachineid && this.fetchDiskOfferings()
  },
  methods: {
    fetchStoragePools () {
      api('findStoragePoolsForMigration', {
        id: this.resource.id,
        keyword: this.searchQuery,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.storagePools = response.findstoragepoolsformigrationresponse.storagepool || []
        if (Array.isArray(this.storagePools) && this.storagePools.length) {
          this.selectedStoragePool = this.storagePools[0].id || ''
        }
        this.totalCount = response.findstoragepoolsformigrationresponse.count
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
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
    closeModal () {
      this.$parent.$parent.close()
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
    submitMigrateVolume () {
      if (this.isSubmitted) return
      if (this.storagePools.length === 0) {
        this.closeModal()
        return
      }
      this.isSubmitted = true
      api('migrateVolume', {
        livemigrate: this.resource.vmstate === 'Running',
        storageid: this.selectedStoragePool,
        volumeid: this.resource.id,
        newdiskofferingid: this.replaceDiskOffering ? this.selectedDiskOffering : null
      }).then(response => {
        this.$pollJob({
          jobId: response.migratevolumeresponse.jobid,
          successMessage: this.$t('message.success.migrate.volume'),
          errorMessage: this.$t('message.migrate.volume.failed'),
          errorMethod: () => {
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.migrate.volume.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.isSubmitted = false
          }
        })
        this.closeModal()
      }).catch(error => {
        this.$notifyError(error)
        this.isSubmitted = false
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
  .form-layout {
    width: 80vw;

    @media (min-width: 800px) {
      width: 600px;
    }
  }

  .top-spaced {
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
