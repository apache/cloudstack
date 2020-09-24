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
      style="width: 25vw;float: right;margin-bottom: 10px; z-index: 8"
      :placeholder="$t('label.search')"
      v-model="filter"
      @search="handleSearch" />
    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="tableSource"
      :pagination="false"
      :rowSelection="rowSelection"
      :customRow="onClickRow"
      size="middle"
      :scroll="{ y: 225 }"
    >
      <span slot="diskSizeTitle"><a-icon type="hdd" /> {{ $t('label.disksize') }}</span>
      <span slot="iopsTitle"><a-icon type="rocket" /> {{ $t('label.minmaxiops') }}</span>
      <template slot="diskSize" slot-scope="text, record">
        <div v-if="record.isCustomized">{{ $t('label.iscustomized') }}</div>
        <div v-else-if="record.diskSize">{{ record.diskSize }} GB</div>
        <div v-else>-</div>
      </template>
      <template slot="iops" slot-scope="text, record">
        <span v-if="record.miniops && record.maxiops">{{ record.miniops }} - {{ record.maxiops }}</span>
        <span v-else-if="record.miniops && !record.maxiops">{{ record.miniops }}</span>
        <span v-else-if="!record.miniops && record.maxiops">{{ record.maxiops }}</span>
        <span v-else>-</span>
      </template>
    </a-table>

    <div style="display: block; text-align: right;">
      <a-pagination
        size="small"
        :current="options.page"
        :pageSize="options.pageSize"
        :total="rowCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100', '200']"
        @change="onChangePage"
        @showSizeChange="onChangePageSize"
        showSizeChanger>
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DiskOfferingSelection',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    rowCount: {
      type: Number,
      default: () => 0
    },
    value: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    preFillContent: {
      type: Object,
      default: () => {}
    },
    zoneId: {
      type: String,
      default: () => ''
    },
    isIsoSelected: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      filter: '',
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.diskoffering'),
          width: '40%'
        },
        {
          dataIndex: 'disksize',
          slots: { title: 'diskSizeTitle' },
          width: '30%',
          scopedSlots: { customRender: 'diskSize' }
        },
        {
          dataIndex: 'iops',
          slots: { title: 'iopsTitle' },
          width: '30%',
          scopedSlots: { customRender: 'iops' }
        }
      ],
      selectedRowKeys: ['0'],
      dataItems: [],
      oldZoneId: null,
      options: {
        page: 1,
        pageSize: 10,
        keyword: null
      }
    }
  },
  created () {
    this.initDataItem()
    if (this.items) {
      this.dataItems = this.dataItems.concat(this.items)
    }
  },
  computed: {
    tableSource () {
      return this.dataItems.map((item) => {
        return {
          key: item.id,
          name: item.name,
          diskSize: item.disksize,
          miniops: item.miniops,
          maxiops: item.maxiops,
          isCustomized: item.iscustomized
        }
      })
    },
    rowSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectRow
      }
    }
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && newValue !== oldValue) {
        this.selectedRowKeys = [newValue]
      }
    },
    items (newData, oldData) {
      this.initDataItem()
      this.dataItems = this.dataItems.concat(newData)
    },
    loading () {
      if (!this.loading) {
        if (this.preFillContent.diskofferingid) {
          this.selectedRowKeys = [this.preFillContent.diskofferingid]
          this.$emit('select-disk-offering-item', this.preFillContent.diskofferingid)
        } else {
          if (this.oldZoneId === this.zoneId) {
            return
          }
          this.oldZoneId = this.zoneId
          this.selectedRowKeys = ['0']
          this.$emit('select-disk-offering-item', '0')
        }
      }
    },
    isIsoSelected () {
      if (this.isIsoSelected) {
        this.dataItems = this.dataItems.filter(item => item.id !== '0')
      } else {
        this.dataItems.unshift({
          id: '0',
          name: this.$t('label.noselect'),
          diskSize: undefined,
          miniops: undefined,
          maxiops: undefined,
          isCustomized: undefined
        })
      }
    }
  },
  methods: {
    initDataItem () {
      this.dataItems = []
      if (this.options.page === 1 && !this.isIsoSelected) {
        this.dataItems.push({
          id: '0',
          name: this.$t('label.noselect'),
          diskSize: undefined,
          miniops: undefined,
          maxiops: undefined,
          isCustomized: undefined
        })
      }
    },
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-disk-offering-item', value[0])
    },
    handleSearch (value) {
      this.filter = value
      this.options.page = 1
      this.options.pageSize = 10
      this.options.keyword = this.filter
      this.$emit('handle-search-filter', this.options)
    },
    onChangePage (page, pageSize) {
      this.options.page = page
      this.options.pageSize = pageSize
      this.$emit('handle-search-filter', this.options)
    },
    onChangePageSize (page, pageSize) {
      this.options.page = page
      this.options.pageSize = pageSize
      this.$emit('handle-search-filter', this.options)
    },
    onClickRow (record) {
      return {
        on: {
          click: () => {
            this.selectedRowKeys = [record.key]
            this.$emit('select-disk-offering-item', record.key)
          }
        }
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }

  /deep/.ant-table-tbody > tr > td {
    cursor: pointer;
  }
</style>
