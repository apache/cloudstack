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
      :loading="loading"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
      :rowSelection="rowSelection"
      size="middle"
      :scroll="{ y: 225 }">
      <template #headerCell="{ column }">
        <template v-if="column.key === 'publicip'"><environment-outlined /> {{ $t('label.publicip') }}</template>
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
import _ from 'lodash'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'LoadBalancerSelection',
  components: {
    ResourceIcon
  },
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
      selectedRowKeys: [],
      filteredInfo: null,
      oldZoneId: null,
      options: {
        page: 1,
        pageSize: 10,
        keyword: null
      }
    }
  },
  computed: {
    columns () {
      return [
        {
          dataIndex: 'name',
          title: this.$t('label.name'),
          width: '40%'
        },
        {
          key: 'publicip',
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
      ]
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
      if (newValue && !_.isEqual(newValue, oldValue)) {
        this.selectedRowKeys = newValue
      }
    },
    loading () {
      if (!this.loading) {
        if (this.preFillContent.loadbalancerid) {
          this.selectedRowKeys = this.preFillContent.loadbalancerid
          this.$emit('select-load-balancer-item', this.preFillContent.loadbalancerid)
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
    },
    items: {
      deep: true,
      handler () {
        if (this.items && this.items.length > 0) {
          this.selectedRowKeys = this.items[0].id
          this.$emit('select-load-balancer-item', this.selectedRowKeys)
        } else {
          this.selectedRowKeys = []
          this.$emit('select-load-balancer-item', '0')
        }
      }
    }
  },
  created () {
  },
  methods: {
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-load-balancer-item', value[0])
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
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
