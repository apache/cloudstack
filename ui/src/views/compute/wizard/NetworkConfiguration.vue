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
  <a-form
    :ref="formRef"
    :model="form"
    :rules="rules">
    <a-table
      :columns="columns"
      :dataSource="dataItems"
      :pagination="false"
      :rowSelection="rowSelection"
      :customRow="onClickRow"
      :rowKey="record => record.id"
      size="middle"
      :scroll="{ y: 225 }">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          <div>{{ text }}</div>
          <small v-if="record.type!=='L2'">{{ $t('label.cidr') + ': ' + record.cidr }}</small>
        </template>
        <template  v-if="!this.autoscale">
          <template v-if="column.key === 'ipAddress'">
            <a-form-item
              style="display: block"
              v-if="record.type !== 'L2'"
              :name="'ipAddress' + record.id">
              <a-input
                style="width: 150px;"
                v-model:value="form['ipAddress' + record.id]"
                :placeholder="record.cidr"
                @change="($event) => updateNetworkData('ipAddress', record.id, $event.target.value)">
                <template #suffix>
                  <a-tooltip :title="getIpRangeDescription(record)">
                    <info-circle-outlined style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </template>
              </a-input>
            </a-form-item>
          </template>
          <template v-if="column.key === 'macAddress'">
            <a-form-item style="display: block" :name="'macAddress' + record.id">
              <a-input
                style="width: 150px;"
                :placeholder="$t('label.macaddress')"
                v-model:value="form[`macAddress` + record.id]"
                @change="($event) => updateNetworkData('macAddress', record.id, $event.target.value)">
                <template #suffix>
                  <a-tooltip :title="$t('label.macaddress.example')">
                    <info-circle-outlined style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </template>
              </a-input>
            </a-form-item>
          </template>
        </template>
      </template>
    </a-table>
  </a-form>
</template>

<script>
import { ref, reactive } from 'vue'
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
    autoscale: {
      type: Boolean,
      default: () => false
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
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.defaultnetwork'),
          width: '30%'
        },
        {
          key: 'ipAddress',
          dataIndex: 'ip',
          title: this.$t('label.ip'),
          width: '30%'
        },
        {
          key: 'macAddress',
          dataIndex: 'mac',
          title: this.$t('label.macaddress'),
          width: '30%'
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
    this.initForm()
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
    items: {
      deep: true,
      handler (newData) {
        if (newData && newData.length > 0) {
          this.dataItems = newData
          this.initForm()
          const keyEx = this.dataItems.filter((item) => this.selectedRowKeys.includes(item.id))
          if (!keyEx || keyEx.length === 0) {
            this.selectedRowKeys = [this.dataItems[0].id]
            this.$emit('select-default-network-item', this.dataItems[0].id)
          }
        }
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})

      const form = {}
      const rules = {}

      this.dataItems.forEach(record => {
        const ipAddressKey = 'ipAddress' + record.id
        const macAddressKey = 'macAddress' + record.id
        rules[ipAddressKey] = [{
          validator: this.validatorIpAddress,
          cidr: record.cidr,
          networkType: record.type
        }]
        if (record.ipAddress) {
          form[ipAddressKey] = record.ipAddress
        }
        rules[macAddressKey] = [{ validator: this.validatorMacAddress }]
        if (record.macAddress) {
          form[macAddressKey] = record.macAddress
        }
      })
      this.form = reactive(form)
      this.rules = reactive(rules)
    },
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-default-network-item', value[0])
    },
    updateNetworkData (name, key, value) {
      this.formRef.value.validate().then(() => {
        this.$emit('handler-error', false)
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
            this.networks[index][name] = value
          }
        })
        this.$emit('update-network-config', this.networks)
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.$emit('handler-error', true)
      })
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
    async validatorMacAddress (rule, value) {
      if (!value || value === '') {
        return Promise.resolve()
      } else if (!this.macRegex.test(value)) {
        return Promise.reject(this.$t('message.error.macaddress'))
      } else {
        return Promise.resolve()
      }
    },
    async validatorIpAddress (rule, value) {
      if (!value || value === '') {
        return Promise.resolve()
      } else if (!this.ipV4Regex.test(value)) {
        return Promise.reject(this.$t('message.error.ipv4.address'))
      } else if (rule.networkType !== 'L2' && !this.isIp4InCidr(value, rule.cidr)) {
        const rangeIps = this.calculateCidrRange(rule.cidr)
        const message = `${this.$t('message.error.ip.range')} ${this.$t('label.from')} ${rangeIps[0]} ${this.$t('label.to')} ${rangeIps[1]}`
        return Promise.reject(message)
      } else {
        return Promise.resolve()
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
        onClick: (event) => {
          if (event.target.tagName.toLowerCase() !== 'input') {
            this.selectedRowKeys = [record.id]
            this.$emit('select-default-network-item', record.id)
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

  :deep(.ant-table-tbody) > tr > td {
    cursor: pointer;
  }

  .ant-form .ant-form-item {
    margin-bottom: 0;
    padding-bottom: 0;
  }
</style>
