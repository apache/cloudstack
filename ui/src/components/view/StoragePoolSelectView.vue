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
      <span slot="suitabilityCustomTitle">
        {{ $t('label.suitability') }}
        <a-tooltip :title="$t('message.volume.state.primary.storage.suitability')" placement="top">
          <a-icon type="info-circle" class="table-tooltip-icon" />
        </a-tooltip>
      </span>
      <div slot="name" slot-scope="record">
        {{ record.name }}
        <a-tooltip v-if="record.name === $t('label.auto.assign')" :title="$t('message.migrate.volume.pool.auto.assign')" placement="top">
          <a-icon type="info-circle" class="table-tooltip-icon" />
        </a-tooltip>
      </div>
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
        <span v-if="record.disksizetotal">{{ $bytesToHumanReadableSize(record.disksizetotal) }}</span>
      </div>
      <div slot="disksizeused" slot-scope="record">
        <span v-if="record.disksizeused">{{ $bytesToHumanReadableSize(record.disksizeused) }}</span>
      </div>
      <div slot="disksizefree" slot-scope="record">
        <span v-if="record.disksizetotal && record.disksizeused">{{ $bytesToHumanReadableSize(record.disksizetotal * 1 - record.disksizeused * 1) }}</span>
      </div>
      <template slot="select" slot-scope="record">
        <a-tooltip placement="top" :title="record.state !== 'Up' ? $t('message.primary.storage.invalid.state') : ''">
          <a-radio
            :disabled="record.id !== -1 && record.state !== 'Up'"
            @click="updateSelection(record)"
            :checked="selectedStoragePool != null && record.id === selectedStoragePool.id">
          </a-radio>
        </a-tooltip>
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
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'VolumeStoragePoolSelector',
  props: {
    resource: {
      type: Object,
      required: true
    },
    clusterId: {
      type: String,
      required: false,
      default: null
    },
    suitabilityEnabled: {
      type: Boolean,
      required: false,
      default: false
    },
    autoAssignAllowed: {
      type: Boolean,
      required: false,
      default: false
    },
    isOpen: {
      type: Boolean,
      required: false
    }
  },
  data () {
    return {
      loading: false,
      storagePools: [],
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      selectedStoragePool: null,
      columns: [
        {
          title: this.$t('label.storageid'),
          scopedSlots: { customRender: 'name' }
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
    if (this.suitabilityEnabled) {
      this.columns.splice(1, 0, { slots: { title: 'suitabilityCustomTitle' }, scopedSlots: { customRender: 'suitability' } }
      )
    }
    this.preselectStoragePool()
    this.fetchStoragePools()
  },
  watch: {
    searchQuery (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.page = 1
      }
    }
  },
  methods: {
    fetchStoragePools () {
      this.loading = true
      if (this.suitabilityEnabled) {
        api('findStoragePoolsForMigration', {
          id: this.resource.id,
          keyword: this.searchQuery,
          page: this.page,
          pagesize: this.pageSize
        }).then(response => {
          this.storagePools = response.findstoragepoolsformigrationresponse.storagepool || []
          this.totalCount = response.findstoragepoolsformigrationresponse.count
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.handleStoragePoolsFetchComplete()
        })
      } else {
        var params = {
          zoneid: this.resource.zoneid,
          keyword: this.searchQuery,
          page: this.page,
          pagesize: this.pageSize
        }
        if (this.clusterId) {
          params.clusterid = this.clusterId
        }
        api('listStoragePools', params).then(response => {
          this.storagePools = response.liststoragepoolsresponse.storagepool || []
          this.totalCount = response.liststoragepoolsresponse.count
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.handleStoragePoolsFetchComplete()
        })
      }
    },
    handleStoragePoolsFetchComplete () {
      this.$emit('storagePoolsUpdated', this.storagePools)
      this.addAutoAssignOption()
      this.loading = false
    },
    addAutoAssignOption () {
      if (this.autoAssignAllowed && this.page === 1) {
        this.storagePools.unshift({ id: -1, name: this.$t('label.auto.assign'), clustername: '', podname: '' })
      }
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
    preselectStoragePool () {
      if (this.resource && 'selectedstorageid' in this.resource) {
        this.selectedStoragePool = { id: this.resource.selectedstorageid }
      }
    },
    clearView () {
      this.storagePools = []
      this.searchQuery = ''
      this.totalCount = 0
      this.page = 1
      this.pageSize = 10
      this.selectedStoragePool = null
    },
    reset () {
      this.clearView()
      this.preselectStoragePool()
      this.fetchStoragePools()
    },
    updateSelection (storagePool) {
      this.selectedStoragePool = storagePool
      this.$emit('select', this.selectedStoragePool)
    }
  }
}
</script>

<style scoped lang="scss">
  .top-spaced {
    margin-top: 20px;
  }
  .table-tooltip-icon {
    color: rgba(0,0,0,.45);
  }
</style>
