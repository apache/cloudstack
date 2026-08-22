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
  <a-spin :spinning="loading">
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <div class="form">
        <a-alert type="info">
          <template #message>
            <span v-html="$t('message.create.l3.network')" />
          </template>
        </a-alert>
        <br/>
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
         >
          <a-form-item name="name" ref="name">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
            </template>
            <a-input
              v-model:value="form.name"
              :placeholder="apiParams.name.description"
              v-focus="true"/>
          </a-form-item>
          <a-form-item name="displaytext" ref="displaytext">
            <template #label>
              <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
            </template>
            <a-input
              v-model:value="form.displaytext"
              :placeholder="apiParams.displaytext.description"/>
          </a-form-item>
          <a-form-item name="zoneid" ref="zoneid">
            <template #label>
              <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
            </template>
            <a-select
              v-model:value="form.zoneid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              :placeholder="apiParams.zoneid.description"
              @change="val => { handleZoneChange(zones[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <ownership-selection v-if="isAdminOrDomainAdmin()" @fetch-owner="fetchOwnerOptions"/>
          <a-form-item name="networkofferingid" ref="networkofferingid">
            <template #label>
              <tooltip-label :title="$t('label.networkofferingid')" :tooltip="apiParams.networkofferingid.description"/>
            </template>
            <a-select
              v-model:value="form.networkofferingid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networkOfferingLoading"
              :placeholder="apiParams.networkofferingid.description"
              @change="val => { handleNetworkOfferingChange(networkOfferings[val]) }">
              <a-select-option v-for="(opt, optIndex) in networkOfferings" :key="optIndex" :label="opt.displaytext || opt.name || opt.description">
                {{ opt.displaytext || opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="gateway" ref="gateway">
                <template #label>
                  <tooltip-label :title="$t('label.ip4gateway')" :tooltip="apiParams.gateway.description"/>
                </template>
                <a-input
                  v-model:value="form.gateway"
                  :placeholder="apiParams.gateway.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="netmask" ref="netmask">
                <template #label>
                  <tooltip-label :title="$t('label.netmask')" :tooltip="apiParams.netmask.description"/>
                </template>
                <a-input
                  v-model:value="form.netmask"
                  :placeholder="apiParams.netmask.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="startip" ref="startip">
                <template #label>
                  <tooltip-label :title="$t('label.startipv4')" :tooltip="apiParams.startip.description"/>
                </template>
                <a-input
                  v-model:value="form.startip"
                  :placeholder="apiParams.startip.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="endip" ref="endip">
                <template #label>
                  <tooltip-label :title="$t('label.endipv4')" :tooltip="apiParams.endip.description"/>
                </template>
                <a-input
                  v-model:value="form.endip"
                  :placeholder="apiParams.endip.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="ip6gateway" ref="ip6gateway">
                <template #label>
                  <tooltip-label :title="$t('label.ip6gateway')" :tooltip="apiParams.ip6gateway.description"/>
                </template>
                <a-input
                  v-model:value="form.ip6gateway"
                  :placeholder="apiParams.ip6gateway.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="ip6cidr" ref="ip6cidr">
                <template #label>
                  <tooltip-label :title="$t('label.ip6cidr')" :tooltip="apiParams.ip6cidr.description"/>
                </template>
                <a-input
                  v-model:value="form.ip6cidr"
                  :placeholder="apiParams.ip6cidr.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="startipv6" ref="startipv6">
                <template #label>
                  <tooltip-label :title="$t('label.startipv6')" :tooltip="apiParams.startipv6.description"/>
                </template>
                <a-input
                  v-model:value="form.startipv6"
                  :placeholder="apiParams.startipv6.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="endipv6" ref="endipv6">
                <template #label>
                  <tooltip-label :title="$t('label.endipv6')" :tooltip="apiParams.endipv6.description"/>
                </template>
                <a-input
                  v-model:value="form.endipv6"
                  :placeholder="apiParams.endipv6.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="dns1" ref="dns1">
                <template #label>
                  <tooltip-label :title="$t('label.dns1')" :tooltip="apiParams.dns1 ? apiParams.dns1.description : null"/>
                </template>
                <a-input
                  v-model:value="form.dns1"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="dns2" ref="dns2">
                <template #label>
                  <tooltip-label :title="$t('label.dns2')" :tooltip="apiParams.dns2 ? apiParams.dns2.description : null"/>
                </template>
                <a-input
                  v-model:value="form.dns2"/>
              </a-form-item>
            </a-col>
          </a-row>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ $t('label.cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              type="primary"
              htmlType="submit"
              @click="handleSubmit">
              {{ $t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { isAdminOrDomainAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection.vue'

export default {
  name: 'CreateL3NetworkForm',
  mixins: [mixinForm],
  components: {
    OwnershipSelection,
    TooltipLabel,
    ResourceIcon
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    resource: {
      type: Object,
      default: () => { return {} }
    }
  },
  data () {
    return {
      actionLoading: false,
      owner: {},
      zones: [],
      zoneLoading: false,
      selectedZone: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      selectedNetworkOffering: {}
    }
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.fetchData()
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createNetwork')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    isAdminOrDomainAdmin,
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        networkofferingid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchZoneData()
    },
    fetchOwnerOptions (owner) {
      this.owner = owner
      this.fetchNetworkOfferingData()
    },
    fetchZoneData () {
      this.zoneLoading = true
      getAPI('listZones', { showicon: true }).then(json => {
        this.zones = (json.listzonesresponse.zone || []).filter(zone => zone.networktype === 'Advanced')
        this.zoneLoading = false
        if (this.zones.length > 0) {
          this.form.zoneid = 0
          this.handleZoneChange(this.zones[0])
        }
      })
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      this.fetchNetworkOfferingData()
    },
    fetchNetworkOfferingData () {
      if (this.isObjectEmpty(this.selectedZone)) {
        return
      }
      this.networkOfferingLoading = true
      const params = {
        zoneid: this.selectedZone.id,
        guestiptype: 'L3',
        state: 'Enabled'
      }
      if (this.owner.projectid) {
        this.form.account = null
        this.form.domainid = null
      }
      getAPI('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering || []
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.networkOfferings.length > 0) {
          this.form.networkofferingid = 0
          this.handleNetworkOfferingChange(this.networkOfferings[0])
        }
      })
    },
    handleNetworkOfferingChange (networkOffering) {
      this.selectedNetworkOffering = networkOffering
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        this.actionLoading = true
        const params = {
          name: values.name,
          displaytext: values.displaytext || values.name,
          zoneid: this.selectedZone.id,
          networkofferingid: this.selectedNetworkOffering.id
        }
        for (const key of ['gateway', 'netmask', 'startip', 'endip', 'ip6gateway', 'ip6cidr', 'startipv6', 'endipv6', 'dns1', 'dns2']) {
          if (values[key]) {
            params[key] = values[key]
          }
        }
        if (this.owner.account) {
          params.account = this.owner.account
          params.domainid = this.owner.domainid
        } else if (this.owner.projectid) {
          params.domainid = this.owner.domainid
          params.projectid = this.owner.projectid
        }
        postAPI('createNetwork', params).then(json => {
          this.$notification.success({
            message: this.$t('label.network'),
            description: this.$t('message.success.create.l3.network')
          })
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="scss" scoped>
.form-layout {
  .form {
    margin: 10px 0;
  }
}

.form-layout {
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }
}
</style>
