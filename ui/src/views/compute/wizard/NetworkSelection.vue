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
  <a-table
    :columns="columns"
    :dataSource="networkItems"
    :rowKey="record => record.id"
    :pagination="{showSizeChanger: true}"
    :rowSelection="rowSelection"
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
    }
  },
  data () {
    return {
      selectedRowKeys: [],
      vpcs: [],
      filteredInfo: null
    }
  },
  computed: {
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
    }
  },
  created () {
    api('listVPCs', {
      projectid: store.getters.project.id
    }).then((response) => {
      this.vpcs = _.get(response, 'listvpcsresponse.vpc')
    })
  },
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
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
