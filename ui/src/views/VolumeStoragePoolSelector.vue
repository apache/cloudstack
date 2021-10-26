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
  <div class="migrate-volume-container" v-ctrl-enter="submitForm">
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
        <div slot="name" slot-scope="record">
          {{ record.name }}
          <a-tooltip v-if="record.name === $t('label.auto.assign')" :title="$t('message.volume.pool.auto.assign')" placement="top">
            <a-icon type="info-circle" class="tooltip-icon" />
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
            :checked="selectedStoragePool != null && record.id === selectedStoragePool.id"></a-radio>
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

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('label.cancel') }}
      </a-button>
      <a-button type="primary" ref="submit" @click="submitForm">
        {{ $t('label.ok') }}
      </a-button>
    </div>

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
      this.columns.splice(1, 0,
        { title: this.$t('label.suitability'), scopedSlots: { customRender: 'suitability' } }
      )
    }
    this.preselectStoragePool()
    this.fetchStoragePools()
  },
  watch: {
    isOpen (newValue) {
      if (newValue) {
        this.resetForm()
        this.preselectStoragePool()
        this.fetchStoragePools()
      }
    }
  },
  methods: {
    fetchStoragePools () {
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
          this.addAutoAssignOption()
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
          this.addAutoAssignOption()
        })
      }
    },
    addAutoAssignOption () {
      if (this.autoAssignAllowed && this.page === 1) {
        this.storagePools.unshift({ id: -1, name: this.$t('label.auto.assign'), clustername: '', podname: '' })
      }
    },
    closeModal () {
      this.$emit('close-action')
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
    resetForm () {
      this.storagePools = []
      this.searchQuery = ''
      this.totalCount = 0
      this.page = 1
      this.pageSize = 10
      this.selectedStoragePool = null
    },
    submitForm () {
      this.$emit('select', this.resource.id, this.selectedStoragePool)
      this.closeModal()
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
