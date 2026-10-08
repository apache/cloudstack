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
  <div style="margin-top: 10px;" v-if="this.vnf">
    <label>{{ $t('message.configure.network.ip.and.mac') }}</label>
  </div>
  <div style="margin-top: 10px;" v-else>
    <label>{{ $t('message.configure.network.select.default.network') }}</label>
  </div>
  <a-form
    :ref="formRef"
    :model="form"
    :rules="rules">
    <a-table
      :columns="columns"
      :dataSource="visibleDataItems"
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
          <div v-if="trunkGroups[record.id] && trunkGroups[record.id].length > 0" style="margin-top: 4px">
            <a-tag v-for="assocId in trunkGroups[record.id]" :key="assocId" closable @close.prevent.stop="removeFromGroup(record.id, assocId)">
              {{ networkNameById(assocId) }}
            </a-tag>
          </div>
        </template>
        <template v-if="column.key === 'associate'">
          <span @click.stop>
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.network.add.associated')"
              icon="plus-outlined"
              size="small"
              type="primary"
              @onClick="openAssociateModal(record)" />
          </span>
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
    <div v-if="preFillContent.allowIpAddressesFetch" style="margin-bottom: 2rem;">
      <a-button :type="'primary'" @click="handleFetchIpAddresses">
        {{ $t('label.fetch.from.backup') }}
      </a-button>
      <a-button style="margin-left: 8px;" @click="handleClearIpAddresses">
        {{ $t('label.clear') }}
      </a-button>
    </div>
  </a-form>

  <a-modal
    :visible="showAssociateModal"
    :title="$t('label.associate.network')"
    :maskClosable="false"
    :closable="true"
    :footer="null"
    @cancel="closeAssociateModal">
    {{ $t('message.associate.network.desc') }}
    <a-alert
      type="warning"
      show-icon
      style="margin-top: 10px; margin-bottom: 10px"
      :message="$t('message.associate.network.loop.warning')" />
    <a-select
      mode="multiple"
      style="width: 100%"
      :loading="associateModalLoading"
      :placeholder="$t('label.networks')"
      v-model:value="associateSelection"
      optionFilterProp="label">
      <a-select-option
        v-for="network in associateModalNetworks"
        :key="network.id"
        :value="network.id"
        :label="network.name">
        {{ network.name }}
      </a-select-option>
    </a-select>
    <div style="margin-top: 20px; text-align: right;">
      <a-button @click="closeAssociateModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" style="margin-left: 8px" @click="confirmAssociate">{{ $t('label.ok') }}</a-button>
    </div>
  </a-modal>
</template>

<script>
import { ref, reactive } from 'vue'
import { getAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import { isMultiNetworkNicEnabledForZone, fetchMultiNetworkNicEnabledForZone } from '@/utils/multiNetworkNic'
export default {
  name: 'NetworkConfiguration',
  components: {
    TooltipButton
  },
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
    vnf: {
      type: Boolean,
      default: () => false
    },
    preFillContent: {
      type: Object,
      default: () => {}
    },
    canTrunkNics: {
      type: Boolean,
      default: false
    },
    zoneId: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      networks: [],
      selectedRowKeys: [],
      dataItems: [],
      trunkGroups: {},
      showAssociateModal: false,
      associateTarget: null,
      associateSelection: [],
      associateModalNetworks: [],
      associateModalLoading: false,
      associatedNetworkNames: {},
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
    fetchMultiNetworkNicEnabledForZone(this.zoneId)
  },
  computed: {
    rowSelection () {
      if (this.vnf) {
        return null
      }
      return {
        type: 'radio',
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectRow
      }
    },
    showTrunkNicControls () {
      return this.canTrunkNics && isMultiNetworkNicEnabledForZone(this.zoneId)
    },
    columns () {
      const nameAndFieldWidth = this.showTrunkNicControls ? '25%' : '30%'
      const cols = [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.network'),
          width: nameAndFieldWidth
        },
        {
          key: 'ipAddress',
          dataIndex: 'ip',
          title: this.$t('label.ip'),
          width: nameAndFieldWidth
        },
        {
          key: 'macAddress',
          dataIndex: 'mac',
          title: this.$t('label.macaddress'),
          width: nameAndFieldWidth
        }
      ]
      if (this.showTrunkNicControls) {
        cols.push({
          key: 'associate',
          title: '',
          width: '25%'
        })
      }
      return cols
    },
    groupedAwayNetworkIds () {
      return Object.values(this.trunkGroups).flat()
    },
    visibleDataItems () {
      return this.dataItems.filter(item => !this.groupedAwayNetworkIds.includes(item.id))
    }
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && newValue !== oldValue) {
        this.selectedRowKeys = [newValue]
      }
    },
    zoneId (newValue) {
      fetchMultiNetworkNicEnabledForZone(newValue)
    },
    items: {
      deep: true,
      handler (newData) {
        if (newData && newData.length > 0) {
          this.dataItems = newData
          this.initForm()
          const validPrimaryIds = this.dataItems.map(item => item.id)
          const prunedGroups = {}
          let groupsChanged = false
          for (const [primaryId, assocIds] of Object.entries(this.trunkGroups)) {
            if (!validPrimaryIds.includes(primaryId)) {
              // the primary network this group was folded under is no longer selected - drop the whole group
              groupsChanged = true
              continue
            }
            // an associated network is independent of the selection list, except it can't also become its own
            // selected row - if the user just checked it separately, it needs its own nic, not an association
            const filtered = assocIds.filter(id => !validPrimaryIds.includes(id))
            if (filtered.length !== assocIds.length) {
              groupsChanged = true
            }
            if (filtered.length > 0) {
              prunedGroups[primaryId] = filtered
            }
          }
          if (groupsChanged) {
            this.trunkGroups = prunedGroups
            this.$emit('update-trunk-groups', this.trunkGroups)
          }
          const keyEx = this.visibleDataItems.filter((item) => this.selectedRowKeys.includes(item.id))
          if (!keyEx || keyEx.length === 0) {
            this.selectedRowKeys = this.visibleDataItems.length > 0 ? [this.visibleDataItems[0].id] : []
            this.$emit('select-default-network-item', this.selectedRowKeys[0])
          }
        }
      }
    }
  },
  emits: ['update-network-config', 'select-default-network-item', 'handler-error', 'update-trunk-groups'],
  methods: {
    networkNameById (id) {
      const network = this.items.find(item => item.id === id)
      if (network) {
        return network.name
      }
      return this.associatedNetworkNames[id] || id
    },
    openAssociateModal (record) {
      this.associateTarget = record
      this.associateSelection = []
      this.associateModalNetworks = []
      this.showAssociateModal = true
      this.associateModalLoading = true
      const alreadyUsedNetworkIds = [record.id, ...this.dataItems.map(item => item.id), ...this.groupedAwayNetworkIds]
      getAPI('listNetworks', {
        listAll: true,
        zoneid: this.zoneId
      }).then(response => {
        const allNetworks = response.listnetworksresponse.network || []
        this.associateModalNetworks = allNetworks.filter(network =>
          network.type !== 'L2' && !alreadyUsedNetworkIds.includes(network.id))
        const names = {}
        allNetworks.forEach(network => { names[network.id] = network.name })
        this.associatedNetworkNames = { ...this.associatedNetworkNames, ...names }
      }).finally(() => {
        this.associateModalLoading = false
      })
    },
    closeAssociateModal () {
      this.showAssociateModal = false
      this.associateTarget = null
      this.associateSelection = []
      this.associateModalNetworks = []
    },
    confirmAssociate () {
      if (!this.associateTarget || this.associateSelection.length === 0) {
        this.closeAssociateModal()
        return
      }
      const existing = this.trunkGroups[this.associateTarget.id] || []
      this.trunkGroups = {
        ...this.trunkGroups,
        [this.associateTarget.id]: [...existing, ...this.associateSelection]
      }
      if (!this.visibleDataItems.some(item => this.selectedRowKeys.includes(item.id))) {
        // the current default network just got folded into this group as an association; keep the group's primary as default
        this.selectedRowKeys = [this.associateTarget.id]
        this.$emit('select-default-network-item', this.associateTarget.id)
      }
      this.$emit('update-trunk-groups', this.trunkGroups)
      this.closeAssociateModal()
    },
    removeFromGroup (primaryId, assocId) {
      const existing = this.trunkGroups[primaryId] || []
      this.trunkGroups = {
        ...this.trunkGroups,
        [primaryId]: existing.filter(id => id !== assocId)
      }
      this.$emit('update-trunk-groups', this.trunkGroups)
    },
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})

      const form = {}
      const rules = {}

      let presetMacAddressIndex = 0

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
        } else if (this.preFillContent.macAddressArray && this.preFillContent.macAddressArray[presetMacAddressIndex]) {
          form[macAddressKey] = this.preFillContent.macAddressArray[presetMacAddressIndex]
          presetMacAddressIndex++
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
        this.updateNetworkDataWithoutValidation(name, key, value)
        this.$emit('update-network-config', this.networks)
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.$emit('handler-error', true)
      })
    },
    updateNetworkDataWithoutValidation (name, key, value) {
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
    handleFetchIpAddresses () {
      if (!this.preFillContent.networkids) {
        return
      }
      if (!this.preFillContent.ipAddresses && !this.preFillContent.macAddresses) {
        return
      }

      const networkIds = this.dataItems.map(item => item.id)
      this.dataItems.forEach(record => {
        const ipAddressKey = 'ipAddress' + record.id
        const macAddressKey = 'macAddress' + record.id
        this.form[ipAddressKey] = ''
        this.form[macAddressKey] = ''
      })

      networkIds.forEach((networkId) => {
        const backupIndex = this.preFillContent.networkids.findIndex(id => id === networkId)
        if (backupIndex !== -1) {
          if (this.preFillContent.ipAddresses && backupIndex < this.preFillContent.ipAddresses.length) {
            const ipAddress = this.preFillContent.ipAddresses[backupIndex]
            if (ipAddress) {
              const ipAddressKey = 'ipAddress' + networkId
              this.form[ipAddressKey] = ipAddress
              this.updateNetworkDataWithoutValidation('ipAddress', networkId, ipAddress)
            }
          }

          if (this.preFillContent.macAddresses && backupIndex < this.preFillContent.macAddresses.length) {
            const macAddress = this.preFillContent.macAddresses[backupIndex]
            if (macAddress) {
              const macAddressKey = 'macAddress' + networkId
              this.form[macAddressKey] = macAddress
              this.updateNetworkDataWithoutValidation('macAddress', networkId, macAddress)
            }
          }
        }
      })
    },
    handleClearIpAddresses () {
      this.dataItems.forEach(record => {
        const ipAddressKey = 'ipAddress' + record.id
        const macAddressKey = 'macAddress' + record.id
        this.form[ipAddressKey] = ''
        this.form[macAddressKey] = ''

        this.updateNetworkDataWithoutValidation('ipAddress', record.id, '')
        this.updateNetworkDataWithoutValidation('macAddress', record.id, '')
      })

      this.networks = []
      this.$emit('update-network-config', this.networks)
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
      } else if (rule.networkType === 'Isolated' && !this.isIp4InCidr(value, rule.cidr)) {
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
