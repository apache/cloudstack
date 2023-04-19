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
    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="tableSource"
      :rowKey="record => record.id"
      :pagination="false"
      :rowSelection="rowSelection"
      :scroll="{ y: 225 }" >

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span>{{ record.displaytext || record.name }}</span>
          <div v-if="record.meta">
            <div v-for="meta in record.meta" :key="meta.key">
              <a-tag style="margin-top: 5px" :key="meta.key">{{ meta.key + ': ' + meta.value }}</a-tag>
            </div>
          </div>
        </template>
        <template v-if="column.key === 'network'">
          <a-select
            v-if="validNetworks[record.id] && validNetworks[record.id].length > 0"
            :defaultValue="validNetworks[record.id][0].id"
            @change="val => handleNetworkChange(record, val)"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="network in validNetworks[record.id]"
              :key="network.id"
              :label="network.displaytext + (network.broadcasturi ? ' (' + network.broadcasturi + ')' : '')">
              {{ network.displaytext + (network.broadcasturi ? ' (' + network.broadcasturi + ')' : '') }}
            </a-select-option>
          </a-select>
          <span v-else>
            {{ $t('label.no.matching.network') }}
          </span>
        </template>
        <template v-if="column.key === 'ipaddress'">
          <check-box-input-pair
            layout="vertical"
            :resourceKey="record.id"
            :checkBoxLabel="$t('label.auto.assign.random.ip')"
            :defaultCheckBoxValue="true"
            :reversed="true"
            :visible="(indexNum > 0 && ipAddressesEnabled[record.id])"
            @handle-checkinputpair-change="setIpAddress" />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import _ from 'lodash'
import CheckBoxInputPair from '@/components/CheckBoxInputPair'

export default {
  name: 'MultiDiskSelection',
  components: {
    CheckBoxInputPair
  },
  props: {
    items: {
      type: Array,
      default: () => []
    },
    zoneId: {
      type: String,
      default: () => ''
    },
    selectionEnabled: {
      type: Boolean,
      default: true
    },
    filterUnimplementedNetworks: {
      type: Boolean,
      default: false
    },
    filterMatchKey: {
      type: String,
      default: null
    }
  },
  data () {
    return {
      columns: [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.nic')
        },
        {
          key: 'network',
          dataIndex: 'network',
          title: this.$t('label.network')
        },
        {
          key: 'ipaddress',
          dataIndex: 'ipaddress',
          title: this.$t('label.ipaddress')
        }
      ],
      loading: false,
      selectedRowKeys: [],
      networks: [],
      validNetworks: {},
      values: {},
      ipAddressesEnabled: {},
      ipAddresses: {},
      indexNum: 1,
      sendValuesTimer: null
    }
  },
  computed: {
    tableSource () {
      return this.items.map(item => {
        var nic = { ...item, disabled: this.validNetworks[item.id] && this.validNetworks[item.id].length === 0 }
        nic.name = item.displaytext || item.name
        return nic
      })
    },
    rowSelection () {
      if (this.selectionEnabled === true) {
        return {
          type: 'checkbox',
          selectedRowKeys: this.selectedRowKeys,
          getCheckboxProps: record => ({
            props: {
              disabled: record.disabled
            }
          }),
          onChange: (rows) => {
            this.selectedRowKeys = rows
            this.sendValues()
          }
        }
      }
      return null
    }
  },
  watch: {
    items: {
      deep: true,
      handler () {
        this.selectedRowKeys = []
        this.fetchNetworks()
      }
    },
    zoneId () {
      this.fetchNetworks()
    }
  },
  created () {
    this.fetchNetworks()
  },
  methods: {
    fetchNetworks () {
      this.networks = []
      if (!this.zoneId || this.zoneId.length === 0) {
        return
      }
      this.loading = true
      api('listNetworks', {
        zoneid: this.zoneId,
        listall: true
      }).then(response => {
        this.networks = response.listnetworksresponse.network || []
        this.orderNetworks()
      }).finally(() => {
        this.loading = false
      })
    },
    orderNetworks () {
      this.loading = true
      this.validNetworks = {}
      for (const item of this.items) {
        this.validNetworks[item.id] = this.networks
        if (this.filterUnimplementedNetworks) {
          this.validNetworks[item.id] = this.validNetworks[item.id].filter(x => (x.state === 'Implemented' || (x.state === 'Setup' && x.type === 'Shared')))
        }
        if (this.filterMatchKey) {
          this.validNetworks[item.id] = this.validNetworks[item.id].filter(x => x[this.filterMatchKey] === item[this.filterMatchKey])
        }
      }
      this.setDefaultValues()
      this.loading = false
    },
    setIpAddressEnabled (nic, network) {
      this.ipAddressesEnabled[nic.id] = network && network.type !== 'L2'
      this.indexNum = (this.indexNum % 2) + 1
    },
    setIpAddress (nicId, autoAssign, ipAddress) {
      this.ipAddresses[nicId] = autoAssign ? 'auto' : ipAddress
      this.sendValuesTimed()
    },
    setDefaultValues () {
      this.values = {}
      this.ipAddresses = {}
      for (const item of this.items) {
        var network = this.validNetworks[item.id]?.[0] || null
        this.values[item.id] = network ? network.id : ''
        this.ipAddresses[item.id] = (!network || network.type === 'L2') ? null : 'auto'
        this.setIpAddressEnabled(item, network)
      }
      this.sendValuesTimed()
    },
    handleNetworkChange (nic, networkId) {
      this.setIpAddressEnabled(nic, _.find(this.validNetworks[nic.id], (option) => option.id === networkId))
      this.sendValuesTimed()
    },
    sendValuesTimed () {
      clearTimeout(this.sendValuesTimer)
      this.sendValuesTimer = setTimeout(() => {
        this.sendValues(this.selectedScope)
      }, 500)
    },
    sendValues () {
      const data = {}
      if (this.selectionEnabled) {
        this.selectedRowKeys.map(x => {
          var d = { network: this.values[x] }
          if (this.ipAddresses[x]) {
            d.ipAddress = this.ipAddresses[x]
          }
          data[x] = d
        })
      } else {
        for (var x in this.values) {
          var d = { network: this.values[x] }
          if (this.ipAddresses[x] != null && this.ipAddresses[x] !== undefined) {
            d.ipAddress = this.ipAddresses[x]
          }
          data[x] = d
        }
      }
      this.$emit('select-multi-network', data)
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
