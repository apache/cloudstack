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
    <a-tooltip
      arrowPointAtCenter
      placement="bottomRight">
      <template slot="title">
        {{ $t('addNewNetworks') }}
      </template>
    </a-tooltip>
    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="networkItems"
      :rowKey="record => record.id"
      :pagination="{showSizeChanger: true, size: 'small'}"
      :rowSelection="rowSelection"
      @change="handleTableChange"
      :scroll="{ y: 225 }"
    >
      <a-list
        slot="expandedRowRender"
        slot-scope="record"
        :key="record.id"
        :dataSource="getDetails(record)"
        size="small"
      >
        <a-list-item slot="renderItem" slot-scope="item" :key="item.id">
          <a-list-item-meta
            :description="item.description"
          >
            <template v-slot:title>{{ item.title }}</template>
          </a-list-item-meta>
        </a-list-item>
      </a-list>
    </a-table>
  </div>
</template>

<script>
import _ from 'lodash'
import { api } from '@/api'
import store from '@/store'

export default {
  name: 'NetworkSelection',
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
      vpcs: [],
      filteredInfo: null,
      networkOffering: {
        loading: false,
        opts: []
      }
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
    columns () {
      let vpcFilter = []
      if (this.vpcs) {
        vpcFilter = this.vpcs.map((vpc) => {
          return {
            text: vpc.displaytext,
            value: vpc.id
          }
        })
      }
      return [
        {
          dataIndex: 'name',
          title: this.$t('networks'),
          width: '40%'
        },
        {
          dataIndex: 'type',
          title: this.$t('guestIpType'),
          width: '30%'
        },
        {
          dataIndex: 'vpcName',
          title: this.$t('VPC'),
          width: '30%',
          filters: vpcFilter,
          filteredValue: _.get(this.filteredInfo, 'id'),
          onFilter: (value, record) => {
            return record.vpcid === value
          }
        }
      ]
    },
    rowSelection () {
      return {
        type: 'checkbox',
        selectedRowKeys: this.selectedRowKeys,
        onChange: (rows) => {
          this.$emit('select-network-item', rows)
        }
      }
    },
    networkItems () {
      return this.items.map((network) => {
        const vpc = _.find(this.vpcs, { id: network.vpcid })
        return {
          ...network,
          ...{
            vpcName: _.get(vpc, 'displaytext')
          }
        }
      })
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
        if (this.preFillContent.networkids) {
          this.selectedRowKeys = this.preFillContent.networkids
          this.$emit('select-network-item', this.preFillContent.networkids)
        } else {
          this.selectedRowKeys = []
          this.$emit('select-network-item', null)
        }
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    api('listVPCs', {
      projectid: store.getters.project.id
    }).then((response) => {
      this.vpcs = _.get(response, 'listvpcsresponse.vpc')
    })
  },
  inject: ['vmFetchNetworks'],
  methods: {
    getDetails (network) {
      return [
        {
          title: this.$t('description'),
          description: network.displaytext
        },
        {
          title: this.$t('networkOfferingId'),
          description: network.networkofferingdisplaytext
        }
      ]
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
    },
    listNetworkOfferings () {
      return new Promise((resolve, reject) => {
        const args = {}
        args.forvpc = false
        args.zoneid = this.zoneId
        args.guestiptype = 'Isolated'
        args.supportedServices = 'SourceNat'
        args.specifyvlan = false
        args.state = 'Enabled'

        api('listNetworkOfferings', args).then(json => {
          const listNetworkOfferings = json.listnetworkofferingsresponse.networkoffering || []
          resolve(listNetworkOfferings)
        }).catch(error => {
          resolve(error)
        })
      })
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
