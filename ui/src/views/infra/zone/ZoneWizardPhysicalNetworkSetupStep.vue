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
  <div v-ctrl-enter="handleSubmit">
    <a-card
      class="ant-form-text"
      style="text-align: justify; margin: 10px 0; padding: 20px;"
      v-html="zoneType !== null ? $t(zoneDescription[zoneType]) : $t('message.error.select.zone.type')">
    </a-card>
    <a-table
      bordered
      :scroll="{ x: 500 }"
      :dataSource="physicalNetworks"
      :columns="columns"
      :pagination="false"
      style="margin-bottom: 24px; width: 100%">
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.key === 'name'">
          <a-input
            :disabled="tungstenNetworkIndex > -1 && tungstenNetworkIndex !== index"
            :value="text"
            @change="e => onCellChange(record.key, 'name', e.target.value)"
            v-focus="true">
            <template #suffix>
              <a-tooltip
                v-if="tungstenNetworkIndex > -1 && tungstenNetworkIndex !== index"
                :title="$t('message.no.support.tungsten.fabric')">
                <warning-outlined style="color: #f5222d" />
              </a-tooltip>
            </template>
          </a-input>
        </template>
        <template v-if="column.key === 'isolationMethod'">
          <a-select
            :disabled="tungstenNetworkIndex > -1 && tungstenNetworkIndex !== index"
            style="width: 100%"
            :defaultValue="text"
            @change="value => onCellChange(record.key, 'isolationMethod', value)"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="VLAN"> VLAN </a-select-option>
            <a-select-option value="VXLAN"> VXLAN </a-select-option>
            <a-select-option value="GRE"> GRE </a-select-option>
            <a-select-option value="STT"> STT </a-select-option>
            <a-select-option value="BCF_SEGMENT"> BCF_SEGMENT </a-select-option>
            <a-select-option value="ODL"> ODL </a-select-option>
            <a-select-option value="L3VPN"> L3VPN </a-select-option>
            <a-select-option value="VSP"> VSP </a-select-option>
            <a-select-option value="VCS"> VCS </a-select-option>
            <a-select-option value="TF"> TF </a-select-option>

            <template #suffixIcon>
              <a-tooltip
                v-if="tungstenNetworkIndex > -1 && tungstenNetworkIndex !== index"
                :title="$t('message.no.support.tungsten.fabric')">
                <warning-outlined style="color: #f5222d" />
              </a-tooltip>
            </template>
          </a-select>
        </template>
        <template v-if="column.key === 'traffics'">
          <div v-for="traffic in record.traffics" :key="traffic.type">
            <a-tag
              :color="trafficColors[traffic.type]"
              style="margin:2px"
            >
              {{ traffic.type.toUpperCase() }}
              <edit-outlined class="traffic-type-action" @click="editTraffic(record.key, traffic, $event)"/>
              <delete-outlined class="traffic-type-action" @click="deleteTraffic(record.key, traffic, $event)"/>
            </a-tag>
          </div>
          <div v-if="isShowAddTraffic(record.traffics, index)">
            <div class="traffic-select-item" v-if="addingTrafficForKey === record.key">
              <a-select
                :defaultValue="trafficLabelSelected"
                @change="val => { trafficLabelSelected = val }"
                style="min-width: 120px;"
                showSearch
                optionFilterProp="value"
                :filterOption="(input, option) => {
                  return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="(traffic, index) in availableTrafficToAdd"
                  :value="traffic"
                  :key="index"
                  :disabled="isDisabledTraffic(record.traffics, traffic)"
                >
                  {{ traffic.toUpperCase() }}
                </a-select-option>
              </a-select>
              <tooltip-button
                :tooltip="$t('label.add')"
                buttonClass="icon-button"
                icon="plus-outlined"
                size="small"
                @onClick="trafficAdded" />
              <tooltip-button
                :tooltip="$t('label.cancel')"
                buttonClass="icon-button"
                type="primary"
                :danger="true"
                icon="close-outlined"
                size="small"
                @onClick="() => { addingTrafficForKey = null }" />
            </div>
            <a-tag
              key="addingTraffic"
              style="margin:2px;"
              v-else
            >
              <a @click="addingTraffic(record.key, record.traffics)">
                <plus-outlined />
                {{ $t('label.add.traffic') }}
              </a>
            </a-tag>
          </div>
        </template>
        <template v-if="column.key === 'actions'">
          <tooltip-button
            :tooltip="$t('label.delete')"
            v-if="tungstenNetworkIndex === -1 ? index > 0 : tungstenNetworkIndex !== index"
            type="primary"
            :danger="true"
            icon="delete-outlined"
            @onClick="onDelete(record)" />
        </template>
      </template>
      <template #footer v-if="isAdvancedZone">
        <a-button
          :disabled="tungstenNetworkIndex > -1"
          @click="handleAddPhysicalNetwork">
          {{ $t('label.add.physical.network') }}
        </a-button>
      </template>
    </a-table>
    <div class="form-action">
      <a-button
        v-if="!isFixError"
        class="button-right"
        @click="handleBack">
        {{ $t('label.previous') }}
      </a-button>
      <a-button
        class="button-next"
        type="primary"
        ref="submit"
        @click="handleSubmit">
        {{ $t('label.next') }}
      </a-button>
    </div>
    <a-modal
      v-model:visible="showError"
      :title="`${$t('label.error')}!`"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showError = false }"
      centered
    >
      <div v-ctrl-enter="() => showError = false">
        <span>{{ $t('message.required.traffic.type') }}</span>
        <div :span="24" class="action-button">
          <a-button @click="showError = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="showError = false">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
    <a-modal
      :title="$t('label.edit.traffic.type')"
      v-model:visible="showEditTraffic"
      :closable="true"
      :maskClosable="false"
      centered
      :footer="null">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        v-ctrl-enter:[trafficInEdit]="updateTrafficLabel"
       >
        <span class="ant-form-text"> {{ $t('message.edit.traffic.type') }} </span>
        <a-form-item
          v-if="hypervisor !== 'VMware'"
          name="trafficLabel"
          ref="trafficLabel"
          v-bind="formItemLayout"
          style="margin-top:16px;"
          :label="$t('label.traffic.label')">
          <a-input v-model:value="form.trafficLabel" />
        </a-form-item>
        <span v-else>
          <a-form-item :label="$t('label.vswitch.name')" name="vSwitchName" ref="vSwitchName">
            <a-input v-model:value="form.vSwitchName" />
          </a-form-item>
          <a-form-item :label="$t('label.vlanid')" name="vlanId" ref="vlanId">
            <a-input v-model:value="form.vlanId" />
          </a-form-item>
          <a-form-item v-if="isAdvancedZone" :label="$t('label.vswitch.type')" name="vSwitchType" ref="vSwitchType">
            <a-select
              v-model:value="form.vSwitchType"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                value="nexusdvs"
                :label="$t('label.vswitch.type.nexusdvs')">{{ $t('label.vswitch.type.nexusdvs') }}</a-select-option>
              <a-select-option
                value="vmwaresvs"
                :label="$t('label.vswitch.type.vmwaresvs')">{{ $t('label.vswitch.type.vmwaresvs') }}</a-select-option>
              <a-select-option
                value="vmwaredvs"
                :label="$t('label.vswitch.type.vmwaredvs')">{{ $t('label.vswitch.type.vmwaredvs') }}</a-select-option>
            </a-select>
          </a-form-item>
        </span>

        <div :span="24" class="action-button">
          <a-button @click="cancelEditTraffic">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="updateTrafficLabel(trafficInEdit)">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  components: {
    TooltipButton
  },
  props: {
    prefillContent: {
      type: Object,
      default: function () {
        return {}
      }
    },
    isFixError: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      formItemLayout: {
        labelCol: { span: 10 },
        wrapperCol: { span: 12 }
      },
      physicalNetworks: [],
      count: 0,
      zoneDescription: {
        Basic: 'message.setup.physical.network.during.zone.creation.basic',
        Advanced: 'message.setup.physical.network.during.zone.creation'
      },
      hasUnusedPhysicalNetwork: false,
      trafficColors: {
        public: 'orange',
        guest: 'green',
        management: 'blue',
        storage: 'red'
      },
      showEditTraffic: false,
      trafficInEdit: null,
      availableTrafficToAdd: ['storage'],
      addingTrafficForKey: '-1',
      trafficLabelSelected: null,
      showError: false,
      defaultTrafficOptions: [],
      isChangeHyperv: false
    }
  },
  computed: {
    columns () {
      const columns = []
      columns.push({
        key: 'name',
        title: this.$t('label.network.name'),
        dataIndex: 'name',
        width: 175
      })
      columns.push({
        key: 'isolationMethod',
        title: this.$t('label.isolation.method'),
        dataIndex: 'isolationMethod',
        width: 150
      })
      columns.push({
        key: 'traffics',
        title: this.$t('label.traffic.types'),
        dataIndex: 'traffics',
        width: 250
      })
      if (this.isAdvancedZone) {
        columns.push({
          key: 'actions',
          title: '',
          dataIndex: 'actions',
          width: 70
        })
      }

      return columns
    },
    isAdvancedZone () {
      return this.zoneType === 'Advanced'
    },
    zoneType () {
      return this.prefillContent?.zoneType || null
    },
    securityGroupsEnabled () {
      return this.isAdvancedZone && (this.prefillContent?.securityGroupsEnabled || false)
    },
    isEdgeZone () {
      return this.prefillContent?.zoneSuperType === 'Edge' || false
    },
    networkOfferingSelected () {
      return this.prefillContent.networkOfferingSelected
    },
    needsPublicTraffic () {
      if (!this.isAdvancedZone) { // Basic zone
        return (this.networkOfferingSelected && (this.networkOfferingSelected.havingEIP || this.networkOfferingSelected.havingELB))
      } else {
        return !this.securityGroupsEnabled && !this.isEdgeZone
      }
    },
    needsManagementTraffic () {
      return !this.isEdgeZone
    },
    requiredTrafficTypes () {
      const traffics = ['guest']
      if (this.needsManagementTraffic) {
        traffics.push('management')
      }
      if (this.needsPublicTraffic) {
        traffics.push('public')
      }
      return traffics
    },
    tungstenNetworkIndex () {
      const tungstenNetworkIndex = this.physicalNetworks.findIndex(network => network.isolationMethod === 'TF')
      return tungstenNetworkIndex
    },
    hypervisor () {
      return this.prefillContent.hypervisor || null
    }
  },
  created () {
    this.initForm()
    this.defaultTrafficOptions = ['management', 'guest', 'storage']
    if (this.isAdvancedZone || this.needsPublicTraffic) {
      this.defaultTrafficOptions.push('public')
    }
    this.physicalNetworks = this.prefillContent.physicalNetworks
    this.hasUnusedPhysicalNetwork = this.getHasUnusedPhysicalNetwork()
    const requiredTrafficTypes = this.requiredTrafficTypes
    if (this.physicalNetworks && this.physicalNetworks.length > 0) {
      this.count = this.physicalNetworks.length
      requiredTrafficTypes.forEach(type => {
        let foundType = false
        this.physicalNetworks.forEach((net, idx) => {
          for (const index in net.traffics) {
            if (this.hypervisor === 'VMware') {
              delete this.physicalNetworks[idx].traffics[index].label
            } else {
              this.physicalNetworks[idx].traffics[index].label = ''
            }
            const traffic = net.traffics[index]
            if (traffic.type === 'storage') {
              const idx = this.availableTrafficToAdd.indexOf(traffic.type)
              if (idx > -1) this.availableTrafficToAdd.splice(idx, 1)
            }
            if (traffic.type === type) {
              foundType = true
            }
          }
        })
        if (!foundType) this.availableTrafficToAdd.push(type)
      })
    } else {
      const traffics = requiredTrafficTypes.map(item => {
        return { type: item, label: '' }
      })
      this.count = 1
      this.physicalNetworks = [{ key: this.randomKeyTraffic(this.count), name: 'Physical Network 1', isolationMethod: 'VLAN', traffics: traffics }]
    }
    if (this.isAdvancedZone) {
      this.availableTrafficToAdd.push('guest')
    }
    this.emitPhysicalNetworks()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        trafficLabel: [{ required: true, message: this.$t('message.error.traffic.label') }]
      })
    },
    onCellChange (key, dataIndex, value) {
      const physicalNetworks = [...this.physicalNetworks]
      const target = physicalNetworks.find(item => item.key === key)
      if (target) {
        target[dataIndex] = value
        this.physicalNetworks = physicalNetworks
      }
      this.emitPhysicalNetworks()
    },
    onDelete (record) {
      record.traffics.forEach(traffic => {
        if (!this.availableTrafficToAdd.includes(traffic.type)) {
          this.availableTrafficToAdd.push(traffic.type)
        }
      })
      const physicalNetworks = [...this.physicalNetworks]
      this.physicalNetworks = physicalNetworks.filter(item => item.key !== record.key)
      this.hasUnusedPhysicalNetwork = this.getHasUnusedPhysicalNetwork()
      this.emitPhysicalNetworks()
    },
    handleAddPhysicalNetwork () {
      const { count, physicalNetworks } = this
      const newData = {
        key: this.randomKeyTraffic(count + 1),
        name: `Physical Network ${count + 1}`,
        isolationMethod: 'VLAN',
        traffics: []
      }
      this.physicalNetworks = [...physicalNetworks, newData]
      this.count = count + 1
      this.hasUnusedPhysicalNetwork = this.getHasUnusedPhysicalNetwork()
    },
    isValidSetup () {
      let physicalNetworks = this.physicalNetworks
      if (this.tungstenNetworkIndex > -1) {
        physicalNetworks = [this.physicalNetworks[this.tungstenNetworkIndex]]
      }
      const shouldHaveLabels = physicalNetworks.length > 1
      let isValid = true
      this.requiredTrafficTypes.forEach(type => {
        if (!isValid) return false
        let foundType = false
        physicalNetworks.forEach(net => {
          net.traffics.forEach(traffic => {
            if (!isValid) return false
            if (traffic.type === type) {
              foundType = true
            }
            if (this.hypervisor !== 'VMware') {
              if (shouldHaveLabels && (!traffic.label || traffic.label.length === 0)) {
                isValid = false
              }
            } else {
              if (shouldHaveLabels && (!traffic.vSwitchName || traffic.vSwitchName.length === 0)) {
                isValid = false
              }
            }
          })
        })
        if (!foundType || !isValid) {
          isValid = false
        }
      })
      return isValid
    },
    handleSubmit (e) {
      if (this.isValidSetup()) {
        if (this.isFixError) {
          this.$emit('submitLaunchZone')
          return
        }
        this.$emit('nextPressed', this.physicalNetworks)
      } else {
        this.showError = true
      }
    },
    handleBack (e) {
      this.$emit('backPressed')
    },
    addingTraffic (key, traffics) {
      this.addingTrafficForKey = key
      this.availableTrafficToAdd.forEach(type => {
        const trafficEx = traffics.filter(traffic => traffic.type === type)
        if (!trafficEx || trafficEx.length === 0) {
          this.trafficLabelSelected = type
          return false
        }
      })
    },
    trafficAdded (trafficType) {
      const trafficKey = this.physicalNetworks.findIndex(network => network.key === this.addingTrafficForKey)
      this.physicalNetworks[trafficKey].traffics.push({
        type: this.trafficLabelSelected.toLowerCase(),
        label: ''
      })
      if (!this.isAdvancedZone || this.trafficLabelSelected !== 'guest') {
        this.availableTrafficToAdd = this.availableTrafficToAdd.filter(item => item !== this.trafficLabelSelected)
      }
      this.addingTrafficForKey = null
      this.trafficLabelSelected = null
      this.emitPhysicalNetworks()
    },
    editTraffic (key, traffic, $event) {
      this.trafficInEdit = {
        key: key,
        traffic: traffic
      }
      this.showEditTraffic = true
      const fields = {}
      if (this.hypervisor === 'VMware') {
        delete this.trafficInEdit.traffic.label
        fields.vSwitchName = this.trafficInEdit?.traffic?.vSwitchName || null
        fields.vlanId = this.trafficInEdit?.traffic?.vlanId || null
        if (traffic.type === 'guest') {
          fields.vSwitchName = this.trafficInEdit?.traffic?.vSwitchName || 'vSwitch0'
        }
        fields.vSwitchType = this.trafficInEdit?.traffic?.vSwitchType || 'vmwaresvs'
      } else {
        delete this.trafficInEdit.traffic.vSwitchName
        delete this.trafficInEdit.traffic.vlanId
        delete this.trafficInEdit.traffic.vSwitchType
        fields.trafficLabel = null
        fields.trafficLabel = this.trafficInEdit?.traffic?.label || null
      }

      Object.keys(fields).forEach(key => {
        this.form[key] = fields[key]
      })
    },
    deleteTraffic (key, traffic, $event) {
      const trafficKey = this.physicalNetworks.findIndex(network => network.key === key)
      this.physicalNetworks[trafficKey].traffics = this.physicalNetworks[trafficKey].traffics.filter(tr => {
        return tr.type !== traffic.type
      })
      if (!this.isAdvancedZone || traffic.type !== 'guest') {
        this.availableTrafficToAdd.push(traffic.type)
      }
      this.hasUnusedPhysicalNetwork = this.getHasUnusedPhysicalNetwork()
      this.emitPhysicalNetworks()
    },
    updateTrafficLabel (trafficInEdit) {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.showEditTraffic = false
        if (this.hypervisor === 'VMware') {
          trafficInEdit.traffic.vSwitchName = values.vSwitchName
          trafficInEdit.traffic.vlanId = values.vlanId
          if (this.isAdvancedZone) {
            trafficInEdit.traffic.vSwitchType = values.vSwitchType
          }
        } else {
          trafficInEdit.traffic.label = values.trafficLabel
        }
        this.trafficInEdit = null
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
      this.emitPhysicalNetworks()
    },
    cancelEditTraffic () {
      this.showEditTraffic = false
      this.trafficInEdit = null
    },
    getHasUnusedPhysicalNetwork () {
      let hasUnused = false
      if (this.physicalNetworks && this.physicalNetworks.length > 0) {
        this.physicalNetworks.forEach(item => {
          if (!item.traffics || item.traffics.length === 0) {
            hasUnused = true
          }
        })
      }
      return hasUnused
    },
    emitPhysicalNetworks () {
      if (this.physicalNetworks) {
        this.$emit('fieldsChanged', { physicalNetworks: this.physicalNetworks })
      }
    },
    isDisabledTraffic (traffics, traffic) {
      const trafficEx = traffics.filter(item => item.type === traffic)
      if (trafficEx && trafficEx.length > 0) {
        return true
      }

      return false
    },
    isShowAddTraffic (traffics, index) {
      if (this.tungstenNetworkIndex > -1 && this.tungstenNetworkIndex !== index) {
        return false
      }
      if (!this.availableTrafficToAdd || this.availableTrafficToAdd.length === 0) {
        return false
      }

      if (traffics.length === this.defaultTrafficOptions.length) {
        return false
      }

      if (this.isAdvancedZone && this.availableTrafficToAdd.length === 1) {
        const guestEx = traffics.filter(traffic => traffic.type === 'guest')
        if (guestEx && guestEx.length > 0) {
          return false
        }
      }

      return true
    },
    randomKeyTraffic (key) {
      const now = new Date()
      const random = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
      return [key, random, now.getTime()].join('')
    }
  }
}
</script>

<style scoped lang="less">
  .form-action {
    margin-top: 16px;
  }

  .traffic-type-action {
    margin-left: 2px;
    margin-right: 2px;
    padding-left: 1px;
    padding-right: 1px;
  }

  .physical-network-support {
    margin: 10px 0;
  }

  .traffic-select-item {
    :deep(.icon-button) {
      margin: 0 0 0 5px;
    }
  }

  .disabled-traffic {
    position: relative;

    &::before {
      content: ' ';
      position: absolute;
      width: 100%;
      height: 100%;
      top: 0;
      left: 0;
      z-index: 100;
      cursor: not-allowed;
    }
  }
</style>
