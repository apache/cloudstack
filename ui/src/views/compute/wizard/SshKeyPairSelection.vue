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
      <template v-slot:account><a-icon type="user" /> {{ $t('account') }}</template>
      <template v-slot:domain><a-icon type="block" /> {{ $t('domain') }}</template>
    </a-table>
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
    }
  },
  data () {
    return {
      filter: '',
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('sshKeyPairs'),
          width: '40%'
        },
        {
          dataIndex: 'account',
          slots: { title: 'account' },
          width: '30%'
        },
        {
          dataIndex: 'domain',
          slots: { title: 'domain' },
          width: '30%'
        }
      ],
      selectedRowKeys: [this.$t('noselect')],
      dataItems: []
    }
  },
  created () {
    this.initDataItem()
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
          key: item.name,
          name: item.name,
          account: item.account,
          domain: item.domain
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
        this.initDataItem()
        this.dataItems = this.dataItems.concat(newData)
      }
    },
    loading () {
      if (!this.loading) {
        if (this.preFillContent.keypair) {
          this.selectedRowKeys = [this.preFillContent.keypair]
          this.$emit('select-ssh-key-pair-item', this.preFillContent.keypair)
        } else {
          this.selectedRowKeys = [this.$t('noselect')]
          this.$emit('select-ssh-key-pair-item', this.$t('noselect'))
        }
      }
    }
  },
  methods: {
    initDataItem () {
      this.dataItems = []
      this.dataItems.push({
        name: this.$t('noselect'),
        account: '-',
        domain: '-'
      })
    },
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-ssh-key-pair-item', value[0])
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
