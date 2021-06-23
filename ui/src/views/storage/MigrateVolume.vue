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
      <div v-if="storagePools.length > 0">
        <a-alert type="warning">
          <span slot="message" v-html="$t('message.migrate.volume')" />
        </a-alert>
        <p class="modal-form__label">{{ $t('label.storagepool') }}</p>
        <a-select
          v-model="selectedStoragePool"
          style="width: 100%;"
          :autoFocus="storagePools.length > 0"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="(storagePool, index) in storagePools" :value="storagePool.id" :key="index">
            {{ storagePool.name }} <span v-if="resource.virtualmachineid">{{ storagePool.suitableformigration ? `(${$t('label.suitable')})` : `(${$t('label.not.suitable')})` }}</span>
          </a-select-option>
        </a-select>
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
          title: this.$t('label.podid'),
          dataIndex: 'podname'
        },
        {
          title: this.$t('label.clusterid'),
          dataIndex: 'clustername'
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
      selectedDiskOffering: null,
      isSubmitted: false
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
    submitMigrateVolume () {
      if (this.isSubmitted) return
      if (this.storagePools.length === 0) {
        this.closeModal()
        return
      }
      this.isSubmitted = true
      api('migrateVolume', {
        livemigrate: this.resource.vmstate === 'Running',
        storageid: this.selectedPool.id,
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
  .migrate-volume-container {
    width: 85vw;

    @media (min-width: 800px) {
      width: 750px;
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
