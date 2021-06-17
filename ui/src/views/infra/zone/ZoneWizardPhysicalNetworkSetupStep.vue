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
      <template slot="name" slot-scope="text, record">
        <a-input :value="text" @change="e => onCellChange(record.key, 'name', e.target.value)" autoFocus />
      </template>
      <template slot="isolationMethod" slot-scope="text, record">
        <a-select
          style="width: 100%"
          :defaultValue="text"
          @change="value => onCellChange(record.key, 'isolationMethod', value)"
        >
          <a-select-option value="VLAN"> VLAN </a-select-option>
          <a-select-option value="VXLAN"> VXLAN </a-select-option>
          <a-select-option value="GRE"> GRE </a-select-option>
          <a-select-option value="STT"> STT </a-select-option>
          <a-select-option value="BCF_SEGMENT"> BCF_SEGMENT </a-select-option>
          <a-select-option value="ODL"> ODL </a-select-option>
          <a-select-option value="L3VPN"> L3VPN </a-select-option>
          <a-select-option value="VSP"> VSP </a-select-option>
          <a-select-option value="VCS"> VCS </a-select-option>
        </a-select>
      </template>
      <template slot="traffics" slot-scope="traffics, record">
        <div v-for="traffic in traffics" :key="traffic.type">
          <a-tag
            :color="trafficColors[traffic.type]"
            style="margin:2px"
          >
            {{ traffic.type.toUpperCase() }}
            <a-icon type="edit" class="traffic-type-action" @click="editTraffic(record.key, traffic, $event)"/>
            <a-icon type="delete" class="traffic-type-action" @click="deleteTraffic(record.key, traffic, $event)"/>
          </a-tag>
        </div>
        <div v-if="isShowAddTraffic(record.traffics)">
          <div class="traffic-select-item" v-if="addingTrafficForKey === record.key">
            <a-select
              :defaultValue="trafficLabelSelected"
              @change="val => { trafficLabelSelected = val }"
              style="min-width: 120px;"
            >
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
              icon="plus"
              size="small"
              @click="trafficAdded" />
            <tooltip-button
              :tooltip="$t('label.cancel')"
              buttonClass="icon-button"
              type="danger"
              icon="close"
              size="small"
              @click="() => { addingTrafficForKey = null }" />
          </div>
          <a-tag
            key="addingTraffic"
            style="margin:2px;"
            v-else
            @click="addingTraffic(record.key, record.traffics)"
          >
            <a-icon type="plus" />
            {{ $t('label.add.traffic') }}
          </a-tag>
        </div>
      </template>
      <template slot="actions" slot-scope="text, record">
        <tooltip-button :tooltip="$t('label.delete')" v-if="physicalNetworks.indexOf(record) > 0" type="danger" icon="delete" @click="onDelete(record)" />
      </template>
      <template slot="footer" v-if="isAdvancedZone">
        <a-button
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
        @click="handleSubmit">
        {{ $t('label.next') }}
      </a-button>
    </div>
    <a-modal
      :visible="showError"
      :title="`${$t('label.error')}!`"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @ok="() => { showError = false }"
      @cancel="() => { showError = false }"
      centered
    >
      <span>{{ $t('message.required.traffic.type') }}</span>
    </a-modal>
    <a-modal
      :title="$t('label.edit.traffic.type')"
      :visible="showEditTraffic"
      :closable="true"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @ok="updateTrafficLabel(trafficInEdit)"
      @cancel="cancelEditTraffic"
      centered
    >
      <a-form :form="form">
        <span class="ant-form-text"> {{ $t('message.edit.traffic.type') }} </span>
        <a-form-item v-bind="formItemLayout" style="margin-top:16px;" :label="$t('label.traffic.label')">
          <a-input
            v-decorator="['trafficLabel', {
              rules: [{
                required: true,
                message: $t('message.error.traffic.label'),
              }]
            }]"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script>

import TooltipButton from '@/components/view/TooltipButton'

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
      defaultTrafficOptions: []
    }
  },
  computed: {
    columns () {
      const columns = []
      columns.push({
        title: this.$t('label.network.name'),
        dataIndex: 'name',
        width: 175,
        scopedSlots: { customRender: 'name' }
      })
      columns.push({
        title: this.$t('label.isolation.method'),
        dataIndex: 'isolationMethod',
        width: 150,
        scopedSlots: { customRender: 'isolationMethod' }
      })
      columns.push({
        title: this.$t('label.traffic.types'),
        key: 'traffics',
        dataIndex: 'traffics',
        width: 250,
        scopedSlots: { customRender: 'traffics' }
      })
      if (this.isAdvancedZone) {
        columns.push({
          title: '',
          dataIndex: 'actions',
          scopedSlots: { customRender: 'actions' },
          width: 70
        })
      }

      return columns
    },
    isAdvancedZone () {
      return this.zoneType === 'Advanced'
    },
    zoneType () {
      return this.prefillContent.zoneType ? this.prefillContent.zoneType.value : null
    },
    securityGroupsEnabled () {
      return this.isAdvancedZone && (this.prefillContent.securityGroupsEnabled ? this.prefillContent.securityGroupsEnabled.value : false)
    },
    networkOfferingSelected () {
      return this.prefillContent.networkOfferingSelected
    },
    needsPublicTraffic () {
      if (!this.isAdvancedZone) { // Basic zone
        return (this.networkOfferingSelected && (this.networkOfferingSelected.havingEIP || this.networkOfferingSelected.havingELB))
      } else {
        return !this.securityGroupsEnabled
      }
    },
    requiredTrafficTypes () {
      const traffics = ['management', 'guest']
      if (this.needsPublicTraffic) {
        traffics.push('public')
      }
      return traffics
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
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
        this.physicalNetworks.forEach(net => {
          for (const index in net.traffics) {
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
      const shouldHaveLabels = this.physicalNetworks.length > 1
      let isValid = true
      this.requiredTrafficTypes.forEach(type => {
        let foundType = false
        this.physicalNetworks.forEach(net => {
          net.traffics.forEach(traffic => {
            if (traffic.type === type) {
              foundType = true
            }
            if (shouldHaveLabels && (!traffic.label || traffic.label.length === 0)) {
              isValid = false
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
      this.form.setFieldsValue({
        trafficLabel: this.trafficInEdit !== null ? this.trafficInEdit.traffic.label : null
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
      this.form.validateFields((err, values) => {
        if (!err) {
          this.showEditTraffic = false
          trafficInEdit.traffic.label = values.trafficLabel
          this.trafficInEdit = null
        }
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
    isShowAddTraffic (traffics) {
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
    /deep/.icon-button {
      margin: 0 0 0 5px;
    }
  }
</style>
