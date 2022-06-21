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
    <label>{{ $t('message.select.load.balancer.rule') }}</label>
    <a-input-search
      style="width: 25vw; float: right; margin-bottom: 10px; z-index: 8;"
      :placeholder="$t('label.search')"
      v-model:value="filter"
      @search="handleSearch" />
    <a-table
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
      :rowSelection="rowSelection"
      size="middle"
      :scroll="{ y: 225 }">
      <template #publicip><environment-outlined /> {{ $t('label.publicip') }}</template>
      <template #publicport>{{ $t('label.publicport') }}</template>
      <template #privateport>{{ $t('label.privateport') }}</template>
    </a-table>

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
        <template #buildOptionText="props">
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
  name: 'LoadBalancerSelection',
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
    networkId: {
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
          title: this.$t('label.name'),
          width: '40%'
        },
        {
          title: this.$t('label.publicip'),
          dataIndex: 'publicip'
        },
        {
          title: this.$t('label.publicport'),
          dataIndex: 'publicport'
        },
        {
          title: this.$t('label.privateport'),
          dataIndex: 'privateport'
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
        type: 'radio',
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectRow
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
        if (this.preFillContent.lbruleid) {
          this.selectedRowKeys = this.preFillContent.lbruleid
          this.$emit('select-load-balancer-item', this.preFillContent.lbruleid)
        } else {
          if (this.items && this.items.length > 0) {
            if (this.oldZoneId === this.zoneId) {
              return
            }
            this.oldZoneId = this.zoneId
            this.selectedRowKeys = this.items[0].id
            this.$emit('select-load-balancer-item', this.selectedRowKeys)
          } else {
            this.selectedRowKeys = []
            this.$emit('select-load-balancer-item', '0')
          }
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

      if (this.networkId) {
        params.networkId = this.networkId
      }

      this.items = []
      this.fetchLoading = true

      api('listLoadBalancerRules', params).then(json => {
        const items = json.listloadbalancerrulesresponse.loadbalancerrule || []
        this.rowCount = json.listloadbalancerrulesresponse.count || 0
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
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-load-balancer-item', value[0])
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
