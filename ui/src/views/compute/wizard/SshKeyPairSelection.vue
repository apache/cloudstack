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
      :dataSource="items"
      :rowSelection="rowSelection"
      :rowKey="item => item.name"
      :pagination="false"
      size="middle"
      :scroll="{ y: 225 }">
      <template #headerCell="{ column }">
        <template v-if="column.key === 'account'"><user-outlined /> {{ $t('label.account') }}</template>
        <template v-if="column.key === 'domain'"><block-outlined /> {{ $t('label.domain') }}</template>
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
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SshKeyPairSelection',
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
      type: Array,
      default: () => []
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
    }
  },
  data () {
    return {
      filter: '',
      columns: [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.sshkeypairs'),
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
      selectedRowKeys: [],
      oldZoneId: null,
      options: {
        page: 1,
        pageSize: 10,
        keyword: null
      }
    }
  },
  computed: {
    rowSelection () {
      return {
        type: 'checkbox',
        onChange: (selectedRowKeys, selectedRows) => {
          this.$emit('select-ssh-key-pair-item', selectedRows)
        }
      }
    }
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && newValue !== oldValue) {
        this.selectedRowKeys = newValue
      }
    },
    loading () {
      if (!this.loading) {
        if (this.preFillContent.keypairs) {
          this.selectedRowKeys = this.preFillContent.keypairs
          this.$emit('select-ssh-key-pair-item', this.selectedRowKeys)
        } else {
          if (this.oldZoneId === this.zoneId) {
            return
          }
          this.oldZoneId = this.zoneId
          this.selectedRowKeys = []
          this.$emit('select-ssh-key-pair-item', this.selectedRowKeys)
        }
      }
    }
  },
  methods: {
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
