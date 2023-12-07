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
      v-model:value="searchQuery"
      style="margin-bottom: 10px;"
      @search="fetchImageStores"
      v-focus="true" />
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="imageStores"
      :pagination="false"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          {{ record.name }}
        </template>
        <template v-if="column.key === 'disksizetotal'">
          <span v-if="record.disksizetotal">{{ $bytesToHumanReadableSize(record.disksizetotal) }}</span>
        </template>
        <template v-if="column.key === 'disksizeused'">
          <span v-if="record.disksizeused">{{ $bytesToHumanReadableSize(record.disksizeused) }}</span>
        </template>
        <template v-if="column.key === 'disksizefree'">
          <span v-if="record.disksizetotal && record.disksizeused">{{ $bytesToHumanReadableSize(record.disksizetotal * 1 - record.disksizeused * 1) }}</span>
        </template>
        <template v-if="column.key === 'select' && record.id !== srcImageStoreId">
          <a-tooltip placement="top" :title="record.readonly ? $t('message.secondary.storage.invalid.state') : ''">
            <a-radio
              :disabled="record.id === srcImageStoreId"
              @click="updateSelection(record)"
              :checked="selectedImageStore != null && record.id === selectedImageStore.id">
            </a-radio>
          </a-tooltip>
        </template>
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
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ImageStoreSelector',
  props: {
    zoneid: {
      type: String,
      required: true
    },
    srcImageStoreId: {
      type: String,
      required: false
    }
  },
  data () {
    return {
      loading: false,
      imageStores: [],
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      selectedImageStore: null,
      columns: [
        {
          key: 'name',
          title: this.$t('label.storageid')
        },
        {
          key: 'disksizetotal',
          title: this.$t('label.disksizetotal')
        },
        {
          key: 'disksizeused',
          title: this.$t('label.disksizeused')
        },
        {
          key: 'disksizefree',
          title: this.$t('label.disksizefree')
        },
        {
          key: 'select',
          title: this.$t('label.select')
        }
      ]
    }
  },
  created () {
    this.fetchImageStores()
  },
  watch: {
    searchQuery (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.page = 1
      }
    }
  },
  methods: {
    fetchImageStores () {
      this.loading = true
      var params = {
        zoneid: this.zoneid,
        keyword: this.searchQuery,
        page: this.page,
        pagesize: this.pageSize
      }
      api('listImageStores', params).then(response => {
        this.imageStores = response.listimagestoresresponse.imagestore || []
        this.totalCount = response.listimagestoresresponse.count
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.handleImageStoresFetchComplete()
      })
    },
    handleImageStoresFetchComplete () {
      this.$emit('imageStoresUpdated', this.imageStores)
      this.loading = false
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchImageStores()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchImageStores()
    },
    clearView () {
      this.imageStores = []
      this.searchQuery = ''
      this.totalCount = 0
      this.page = 1
      this.pageSize = 10
    },
    reset () {
      this.clearView()
      this.fetchImageStores()
    },
    updateSelection (imageStore) {
      this.selectedImageStore = imageStore
      this.$emit('select', this.selectedImageStore)
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
