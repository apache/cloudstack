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
      v-model:value="filter"
      @search="handleSearch" />
    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="tableSource"
      :rowSelection="rowSelection"
      :customRow="onClickRow"
      :pagination="true"
      size="middle"
      :scroll="{ y: 225 }"
    >
      <template #headerCell="{ column }">
        <template v-if="column.key === 'account'"><user-outlined /> {{ $t('label.account') }}</template>
        <template v-if="column.key === 'domain'"><block-outlined /> {{ $t('label.domain') }}</template>
      </template>
    </a-table>
  </div>
</template>

<script>
export default {
  name: 'UserDataSelection',
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
    disabled: {
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
    }
  },
  data () {
    return {
      filter: '',
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.userdata'),
          width: '40%'
        },
        {
          key: 'account',
          dataIndex: 'account',
          width: '30%'
        },
        {
          key: 'domain',
          dataIndex: 'domain',
          width: '30%'
        }
      ],
      selectedRowKeys: [this.$t('label.noselect')],
      dataItems: [],
      oldZoneId: null,
      options: {
        page: 1,
        pageSize: 10,
        keyword: null
      }
    }
  },
  computed: {
    tableSource () {
      const dataItems = []

      if (this.options.page === 1) {
        dataItems.push({
          key: this.$t('label.noselect'),
          name: this.$t('label.noselect'),
          account: '-',
          domain: '-'
        })
      }

      this.items.map((item) => {
        dataItems.push({
          key: item.id,
          name: item.name,
          account: item.account,
          domain: item.domain
        })
      })

      return dataItems
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
    loading () {
      if (!this.loading) {
        if (this.preFillContent.userdataid) {
          this.selectedRowKeys = [this.preFillContent.userdataid]
          this.$emit('select-user-data-item', this.preFillContent.userdataid)
        } else {
          if (this.oldZoneId === this.zoneId) {
            return
          }
          this.oldZoneId = this.zoneId
          this.selectedRowKeys = [this.$t('label.noselect')]
          this.$emit('select-user-data-item', this.$t('label.noselect'))
        }
      }
    }
  },
  methods: {
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-user-data-item', value[0])
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
            this.$emit('select-user-data-item', record.key)
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

  :deep(.ant-table-tbody) > tr > td {
    cursor: pointer;
  }
</style>
