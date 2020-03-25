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
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="{showSizeChanger: true}"
      :rowSelection="rowSelection"
      size="middle"
      :scroll="{ y: 225 }"
    >
    </a-table>
  </div>
</template>

<script>
import _ from 'lodash'

export default {
  name: 'AffinityGroupSelection',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    value: {
      type: Array,
      default: () => []
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
          title: this.$t('Affinity Groups'),
          width: '40%'
        },
        {
          dataIndex: 'description',
          title: this.$t('description'),
          width: '60%'
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
    rowSelection () {
      return {
        type: 'checkbox',
        selectedRowKeys: this.selectedRowKeys,
        onChange: (rows) => {
          this.$emit('select-affinity-group-item', rows)
        }
      }
    }
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && !_.isEqual(newValue, oldValue)) {
        this.selectedRowKeys = newValue
      }
    }
  },
  methods: {
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
