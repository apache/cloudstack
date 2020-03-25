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
      placeholder="Search"
      v-model="filter"
      @search="handleSearch" />
    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="tableSource"
      :pagination="{showSizeChanger: true}"
      :rowSelection="rowSelection"
      size="middle"
      @change="handleTableChange"
      :scroll="{ y: 225 }"
    >
      <span slot="diskSizeTitle"><a-icon type="hdd" /> {{ $t('disksize') }}</span>
      <span slot="iopsTitle"><a-icon type="rocket" /> {{ $t('minMaxIops') }}</span>
      <template slot="diskSize" slot-scope="text, record">
        <div v-if="record.isCustomized">{{ $t('isCustomized') }}</div>
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
    value: {
      type: String,
      default: ''
    },
    loading: {
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
          title: this.$t('diskoffering'),
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
      dataItems: []
    }
  },
  created () {
    this.dataItems = []
    this.dataItems.push({
      id: '0',
      name: this.$t('noselect'),
      diskSize: undefined,
      miniops: undefined,
      maxiops: undefined,
      isCustomized: undefined
    })
  },
  computed: {
    options () {
      return {
        page: 1,
        pageSize: 10,
        keyword: ''
      }
    },
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
      if (newData && newData.length > 0) {
        this.dataItems = this.dataItems.concat(newData)
      }
    }
  },
  methods: {
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-disk-offering-item', value[0])
    },
    handleSearch (value) {
      this.filter = value
      this.options.keyword = this.filter
      this.$emit('handle-search-filter', this.options)
    },
    handleTableChange (pagination) {
      this.options.page = pagination.current
      this.options.pageSize = pagination.pageSize
      this.$emit('handle-search-filter', this.options)
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
