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
    :dataSource="dataItems"
    :pagination="false"
    :rowSelection="rowSelection"
    :customRow="onClickRow"
    :rowKey="record => record.id"
    size="middle"
    :scroll="{ y: 225 }"
  >
    <template slot="name" slot-scope="text, record">
      <div>{{ text }}</div>
      <small v-if="record.type!=='L2'">{{ $t('label.cidr') + ': ' + record.cidr }}</small>
    </template>
    <template slot="ipAddress" slot-scope="text, record, index">
      <a-form-item v-if="record.type!=='L2' && index === 0">
        <a-input
          style="width: 150px;"
          v-decorator="['ipAddress' + record.id, {
            rules: [{
              validator: validatorIpAddress,
              cidr: record.cidr,
              networkType: record.type
            }]
          }]"
          :placeholder="record.cidr"
          @change="($event) => updateNetworkData('ipAddress', record.id, $event.target.value)">
          <a-tooltip v-if="record.type !== 'L2'" slot="suffix" :title="getIpRangeDescription(record)">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </a-input>
      </a-form-item>
    </template>
    <template slot="macAddress" slot-scope="text, record">
      <a-form-item>
        <a-input
          style="width: 150px;"
          :placeholder="$t('label.macaddress')"
          v-decorator="[`macAddress` + record.id, {
            rules: [{
              validator: validatorMacAddress
            }]
          }]"
          @change="($event) => updateNetworkData('macAddress', record.id, $event.target.value)">
          <a-tooltip slot="suffix" :title="$t('label.macaddress.example')">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </a-input>
      </a-form-item>
    </template>
  </a-table>
</template>

<script>
export default {
  name: 'NetworkConfiguration',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    value: {
      type: String,
      default: ''
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      networks: [],
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.defaultnetwork'),
          width: '30%',
          scopedSlots: { customRender: 'name' }
        },
        {
          dataIndex: 'ip',
          title: this.$t('label.ip'),
          width: '30%',
          scopedSlots: { customRender: 'ipAddress' }
        },
        {
          dataIndex: 'mac',
          title: this.$t('label.macaddress'),
          width: '30%',
          scopedSlots: { customRender: 'macAddress' }
        }
      ],
      selectedRowKeys: [],
      dataItems: [],
      macRegex: /^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$/i,
      ipV4Regex: /^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$/i
    }
  },
  beforeCreate () {
    this.dataItems = []
  },
  created () {
    this.dataItems = this.items
    if (this.dataItems.length > 0) {
      this.selectedRowKeys = [this.dataItems[0].id]
      this.$emit('select-default-network-item', this.dataItems[0].id)
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
  watch: {
    value (newValue, oldValue) {
      if (newValue && newValue !== oldValue) {
        this.selectedRowKeys = [newValue]
      }
    },
    items (newData, oldData) {
      if (newData && newData.length > 0) {
        this.dataItems = newData
        const keyEx = this.dataItems.filter((item) => this.selectedRowKeys.includes(item.id))
        if (!keyEx || keyEx.length === 0) {
          this.selectedRowKeys = [this.dataItems[0].id]
          this.$emit('select-default-network-item', this.dataItems[0].id)
        }
      }
    }
  },
  methods: {
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-default-network-item', value[0])
    },
    updateNetworkData (name, key, value) {
      const index = this.networks.findIndex(item => item.key === key)
      if (index === -1) {
        const networkItem = {}
        networkItem.key = key
        networkItem[name] = value
        this.networks.push(networkItem)
        this.$emit('update-network-config', this.networks)
        return
      }

      this.networks.filter((item, index) => {
        if (item.key === key) {
          this.$set(this.networks[index], name, value)
        }
      })
      this.$emit('update-network-config', this.networks)
    },
    removeItem (id) {
      this.dataItems = this.dataItems.filter(item => item.id !== id)
      if (this.selectedRowKeys.includes(id)) {
        if (this.dataItems && this.dataItems.length > 0) {
          this.selectedRowKeys = [this.dataItems[0].id]
          this.$emit('select-default-network-item', this.dataItems[0].id)
        }
      }
    },
    validatorMacAddress (rule, value, callback) {
      if (!value || value === '') {
        callback()
      } else if (!this.macRegex.test(value)) {
        callback(this.$t('message.error.macaddress'))
      } else {
        callback()
      }
    },
    validatorIpAddress (rule, value, callback) {
      if (!value || value === '') {
        callback()
      } else if (!this.ipV4Regex.test(value)) {
        callback(this.$t('message.error.ipv4.address'))
      } else if (rule.networkType !== 'L2' && !this.isIp4InCidr(value, rule.cidr)) {
        const rangeIps = this.calculateCidrRange(rule.cidr)
        const message = `${this.$t('message.error.ip.range')} ${this.$t('label.from')} ${rangeIps[0]} ${this.$t('label.to')} ${rangeIps[1]}`
        callback(message)
      } else {
        callback()
      }
    },
    getIpRangeDescription (network) {
      const rangeIps = this.calculateCidrRange(network.cidr)
      const rangeIpDescription = [`${this.$t('label.ip.range')}:`, rangeIps[0], '-', rangeIps[1]].join(' ')
      return rangeIpDescription
    },
    isIp4InCidr (ip, cidr) {
      const [range, bits = 32] = cidr.split('/')
      const mask = ~(2 ** (32 - bits) - 1)
      return (this.ip4ToInt(ip) & mask) === (this.ip4ToInt(range) & mask)
    },
    calculateCidrRange (cidr) {
      const [range, bits = 32] = cidr.split('/')
      const mask = ~(2 ** (32 - bits) - 1)
      return [this.intToIp4(this.ip4ToInt(range) & mask), this.intToIp4(this.ip4ToInt(range) | ~mask)]
    },
    ip4ToInt (ip) {
      return ip.split('.').reduce((int, oct) => (int << 8) + parseInt(oct, 10), 0) >>> 0
    },
    intToIp4 (int) {
      return [(int >>> 24) & 0xFF, (int >>> 16) & 0xFF, (int >>> 8) & 0xFF, int & 0xFF].join('.')
    },
    onClickRow (record, index) {
      return {
        on: {
          click: (event) => {
            if (event.target.tagName.toLowerCase() !== 'input') {
              this.selectedRowKeys = [record.id]
              this.$emit('select-default-network-item', record.id)
            }
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

  /deep/.ant-table-tbody > tr > td {
    cursor: pointer;
  }

  .ant-form .ant-form-item {
    margin-bottom: 0;
    padding-bottom: 0;
  }
</style>
