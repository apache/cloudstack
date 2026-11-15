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
    <label>{{ $t('message.vnf.select.networks') }}</label>
  </div>
  <a-form
    :ref="formRef"
    :model="form"
    :rules="rules">
    <a-table
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.deviceid"
      size="middle"
      :scroll="{ y: 225 }">
      <template #deviceid="{ text }">
        <div>{{ text }}</div>
      </template>
      <template #name="{ text }">
        <div>{{ text }}</div>
      </template>
      <template #required="{ record }">
        <span v-if="record.required">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </template>
      <template #management="{ record }">
        <span v-if="record.management">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </template>
      <template #description="{record}">
        <span> {{ record.description }} </span>
      </template>
      <template #network="{ record }">
        <a-form-item style="display: block" :name="'nic-' + record.deviceid">
          <a-select
            :disabled="templateNics && templateNics.length > 0"
            @change="updateNicNetworkValue($event, record.deviceid)"
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option key="" >{{ }}</a-select-option>
            <a-select-option v-for="network in availableNetworksForSelect" :key="network.id">
              {{ network.name }}
            </a-select-option>
          </a-select>
          <div v-if="trunkGroups[record.deviceid] && trunkGroups[record.deviceid].length > 0" style="margin-top: 4px">
            <a-tag v-for="assocId in trunkGroups[record.deviceid]" :key="assocId" closable @close.prevent.stop="removeFromGroup(record.deviceid, assocId)">
              {{ networkNameById(assocId) }}
            </a-tag>
          </div>
        </a-form-item>
      </template>
      <template #associate="{ record }">
        <tooltip-button
          v-if="!record.management && values[record.deviceid]"
          tooltipPlacement="top"
          :tooltip="$t('label.network.add.associated')"
          icon="plus-outlined"
          size="small"
          type="primary"
          @onClick="openAssociateModal(record)" />
      </template>
    </a-table>
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
  name: 'VnfNicsSelection',
  components: {
    TooltipButton
  },
  props: {
    items: {
      type: Array,
      default: () => []
    },
    templateNics: {
      type: Array,
      default: () => []
    },
    networks: {
      type: Array,
      default: () => []
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
      values: {},
      trunkGroups: {},
      showAssociateModal: false,
      associateTarget: null,
      associateSelection: [],
      associateModalNetworks: [],
      associateModalLoading: false,
      associatedNetworkNames: {}
    }
  },
  created () {
    this.initForm()
    fetchMultiNetworkNicEnabledForZone(this.zoneId)
  },
  computed: {
    showTrunkNicControls () {
      return this.canTrunkNics && isMultiNetworkNicEnabledForZone(this.zoneId)
    },
    groupedAwayNetworkIds () {
      return Object.values(this.trunkGroups).flat()
    },
    availableNetworksForSelect () {
      return this.networks.filter(network => !this.groupedAwayNetworkIds.includes(network.id))
    },
    columns () {
      const cols = [
        {
          dataIndex: 'deviceid',
          title: this.$t('label.deviceid'),
          width: '10%',
          slots: { customRender: 'deviceid' }
        },
        {
          dataIndex: 'name',
          title: this.$t('label.name'),
          width: '15%',
          slots: { customRender: 'name' }
        },
        {
          dataIndex: 'required',
          title: this.$t('label.required'),
          width: '10%',
          slots: { customRender: 'required' }
        },
        {
          dataIndex: 'management',
          title: this.$t('label.vnf.nic.management'),
          width: '15%',
          slots: { customRender: 'management' }
        },
        {
          dataIndex: 'description',
          title: this.$t('label.description'),
          width: this.showTrunkNicControls ? '25%' : '35%',
          slots: { customRender: 'description' }
        },
        {
          dataIndex: 'network',
          title: this.$t('label.network'),
          width: this.showTrunkNicControls ? '15%' : '25%',
          slots: { customRender: 'network' }
        }
      ]
      if (this.showTrunkNicControls) {
        cols.push({
          dataIndex: 'associate',
          title: '',
          width: '10%',
          slots: { customRender: 'associate' }
        })
      }
      return cols
    }
  },
  watch: {
    zoneId (newValue) {
      fetchMultiNetworkNicEnabledForZone(newValue)
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})

      const form = {}
      const rules = {}

      this.form = reactive(form)
      this.rules = reactive(rules)
    },
    networkNameById (id) {
      const network = this.networks.find(item => item.id === id)
      if (network) {
        return network.name
      }
      return this.associatedNetworkNames[id] || id
    },
    updateNicNetworkValue (value, deviceid) {
      this.values[deviceid] = this.networks.filter(network => network.id === value)?.[0] || null
      if (!value && this.trunkGroups[deviceid]) {
        // no primary network left for this nic, so any associated networks it had no longer make sense either
        const prunedGroups = { ...this.trunkGroups }
        delete prunedGroups[deviceid]
        this.trunkGroups = prunedGroups
        this.$emit('update-vnf-trunk-groups', this.trunkGroups)
      }
      this.$emit('update-vnf-nic-networks', this.values)
    },
    openAssociateModal (record) {
      this.associateTarget = record
      this.associateSelection = []
      this.associateModalNetworks = []
      this.showAssociateModal = true
      this.associateModalLoading = true
      const alreadyUsedDeviceNetworkIds = Object.values(this.values).filter(Boolean).map(network => network.id)
      const alreadyUsedNetworkIds = [...alreadyUsedDeviceNetworkIds, ...this.groupedAwayNetworkIds]
      getAPI('listNetworks', {
        listAll: true,
        zoneid: this.zoneId
      }).then(response => {
        const allNetworks = response.listnetworksresponse.network || []
        this.associateModalNetworks = allNetworks.filter(network => !alreadyUsedNetworkIds.includes(network.id))
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
      const deviceid = this.associateTarget.deviceid
      const existing = this.trunkGroups[deviceid] || []
      this.trunkGroups = {
        ...this.trunkGroups,
        [deviceid]: [...existing, ...this.associateSelection]
      }
      this.$emit('update-vnf-trunk-groups', this.trunkGroups)
      this.closeAssociateModal()
    },
    removeFromGroup (deviceid, assocId) {
      const existing = this.trunkGroups[deviceid] || []
      this.trunkGroups = {
        ...this.trunkGroups,
        [deviceid]: existing.filter(id => id !== assocId)
      }
      this.$emit('update-vnf-trunk-groups', this.trunkGroups)
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
