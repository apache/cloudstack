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
      :columns="columns"
      :dataSource="tableSource"
      :pagination="false"
      :rowSelection="rowSelection"
      :loading="loading"
      size="middle"
      :scroll="{ y: 225 }"
    >
      <span slot="cpuTitle"><a-icon type="appstore" /> {{ $t('label.cpu') }}</span>
      <span slot="ramTitle"><a-icon type="bulb" /> {{ $t('label.memory') }}</span>
    </a-table>

    <div style="display: block; text-align: right;">
      <a-pagination
        size="small"
        :current="options.page"
        :pageSize="options.pageSize"
        :total="rowCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100', '500']"
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
  name: 'ComputeOfferingSelection',
  props: {
    computeItems: {
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
    }
  },
  data () {
    return {
      filter: '',
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.serviceofferingid'),
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
    tableSource () {
      return this.computeItems.map((item) => {
        var cpuNumberValue = item.cpunumber + ''
        var cpuSpeedValue = (item.cpuspeed !== null && item.cpuspeed !== undefined && item.cpuspeed > 0) ? parseFloat(item.cpuspeed / 1000.0).toFixed(2) + '' : ''
        var ramValue = item.memory + ''
        if (item.iscustomized === true) {
          cpuNumberValue = ''
          ramValue = ''
          if ('serviceofferingdetails' in item &&
            'mincpunumber' in item.serviceofferingdetails &&
            'maxcpunumber' in item.serviceofferingdetails) {
            cpuNumberValue = item.serviceofferingdetails.mincpunumber + '-' + item.serviceofferingdetails.maxcpunumber
          }
          if ('serviceofferingdetails' in item &&
            'minmemory' in item.serviceofferingdetails &&
            'maxmemory' in item.serviceofferingdetails) {
            ramValue = item.serviceofferingdetails.minmemory + '-' + item.serviceofferingdetails.maxmemory
          }
        }
        return {
          key: item.id,
          name: item.name,
          cpu: cpuNumberValue.length > 0 ? `${cpuNumberValue} CPU x ${cpuSpeedValue} Ghz` : '',
          ram: ramValue.length > 0 ? `${ramValue} MB` : ''
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
    loading () {
      if (!this.loading) {
        if (!this.preFillContent) {
          return
        }
        if (this.preFillContent.computeofferingid) {
          this.selectedRowKeys = [this.preFillContent.computeofferingid]
          this.$emit('select-compute-item', this.preFillContent.computeofferingid)
        } else {
          if (this.oldZoneId === this.zoneId) {
            return
          }
          this.oldZoneId = this.zoneId
          if (this.computeItems && this.computeItems.length > 0) {
            this.selectedRowKeys = [this.computeItems[0].id]
            this.$emit('select-compute-item', this.computeItems[0].id)
          }
        }
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
