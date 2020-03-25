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
      :columns="columns"
      :dataSource="tableSource"
      :pagination="{showSizeChanger: true}"
      :rowSelection="rowSelection"
      :loading="loading"
      size="middle"
      @change="handleTableChange"
      :scroll="{ y: 225 }"
    >
      <span slot="cpuTitle"><a-icon type="appstore" /> {{ $t('cpu') }}</span>
      <span slot="ramTitle"><a-icon type="bulb" /> {{ $t('memory') }}</span>
    </a-table>
  </div>
</template>

<script>
export default {
  name: 'ComputeSelection',
  props: {
    computeItems: {
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
          title: this.$t('serviceOfferingId'),
          width: '40%'
        },
        {
          dataIndex: 'cpu',
          slots: { title: 'cpuTitle' },
          width: '30%'
        },
        {
          dataIndex: 'ram',
          slots: { title: 'ramTitle' },
          width: '30%'
        }
      ],
      selectedRowKeys: []
    }
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
      return this.computeItems.map((item) => {
        return {
          key: item.id,
          name: item.name,
          cpu: `${item.cpunumber} CPU x ${parseFloat(item.cpuspeed / 1000.0).toFixed(2)} Ghz`,
          ram: `${item.memory} MB`
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
    }
  },
  methods: {
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-compute-item', value[0])
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
