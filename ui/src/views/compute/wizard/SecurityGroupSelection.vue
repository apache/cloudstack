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
  <div style="margin-top: 10px;">
    <label>{{ $t('message.select.security.groups') }}</label>
    <a-input-search
      style="width: 25vw; float: right; margin-bottom: 10px; z-index: 8;"
      :placeholder="$t('label.search')"
      v-model="filter"
      @search="handleSearch" />
    <a-table
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
      :rowSelection="rowSelection"
      size="middle"
      :scroll="{ y: 225 }"
    ></a-table>

    <div style="display: block; text-align: right; margin-top: 30px">
      <a-pagination
        size="small"
        :current="page"
        :pageSize="pageSize"
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
import { api } from '@/api'
import _ from 'lodash'

export default {
  name: 'SecurityGroupSelection',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    zoneId: {
      type: String,
      default: () => ''
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      filter: '',
      fetchLoading: false,
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.security.groups'),
          width: '40%'
        },
        {
          dataIndex: 'description',
          title: this.$t('label.description'),
          width: '60%'
        }
      ],
      items: [],
      selectedRowKeys: [],
      page: 1,
      pageSize: 10,
      keyword: null,
      rowCount: 0
    }
  },
  computed: {
    rowSelection () {
      return {
        type: 'checkbox',
        selectedRowKeys: this.selectedRowKeys,
        onChange: (rows) => {
          this.$emit('select-security-group-item', rows)
        }
      }
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && !_.isEqual(newValue, oldValue)) {
        this.selectedRowKeys = newValue
      }
    },
    loading () {
      if (!this.loading) {
        if (this.preFillContent.securitygroupids) {
          this.selectedRowKeys = this.preFillContent.securitygroupids
          this.$emit('select-security-group-item', this.preFillContent.securitygroupids)
        } else {
          if (this.oldZoneId === this.zoneId) {
            return
          }
          this.oldZoneId = this.zoneId
          this.selectedRowKeys = []
          this.$emit('select-security-group-item', null)
        }
      }
    }
  },
  methods: {
    fetchData () {
      const params = {
        domainid: this.$store.getters.userInfo.domainid,
        account: this.$store.getters.userInfo.account,
        page: this.page,
        pageSize: this.pageSize
      }

      if (this.keyword) {
        params.keyword = this.keyword
      }

      this.items = []
      this.fetchLoading = true

      api('listSecurityGroups', params).then(json => {
        const items = json.listsecuritygroupsresponse.securitygroup || []
        this.rowCount = json.listsecuritygroupsresponse.count || 0
        if (items && items.length > 0) {
          for (let i = 0; i < items.length; i++) {
            this.items.push(items[i])
          }
          this.items.sort((a, b) => {
            if (a.name < b.name) return -1
            if (a.name > b.name) return 1
            return 0
          })
        }
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    handleSearch (value) {
      this.filter = value
      this.page = 1
      this.pageSize = 10
      this.keyword = this.filter
      this.fetchData()
    },
    onChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    onChangePageSize (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
