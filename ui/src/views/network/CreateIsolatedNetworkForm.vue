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
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
         >
          <a-form-item ref="name" name="name">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
            </template>
            <a-input
             v-model:value="form.name"
              :placeholder="apiParams.name.description"
              v-focus="true"/>
          </a-form-item>
          <a-form-item ref="displaytext" name="displaytext">
            <template #label>
              <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
            </template>
            <a-input
             v-model:value="form.displaytext"
              :placeholder="apiParams.displaytext.description"/>
          </a-form-item>
          <a-form-item ref="zoneid" name="zoneid">
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
          <a-form-item
            ref="networkdomain"
            name="networkdomain"
            v-if="!isObjectEmpty(selectedNetworkOffering) && !selectedNetworkOffering.forvpc">
            <template #label>
              <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
            </template>
            <a-input
             v-model:value="form.networkdomain"
              :placeholder="apiParams.networkdomain.description"/>
          </a-form-item>
          <a-form-item ref="networkofferingid" name="networkofferingid">
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
          <a-form-item ref="asnumber" name="asnumber" v-if="isASNumberRequired()">
            <template #label>
              <tooltip-label :title="$t('label.asnumber')" :tooltip="apiParams.asnumber.description"/>
            </template>
            <a-select
             v-model:value="form.asnumber"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="asNumberLoading"
              :placeholder="apiParams.asnumber.description"
              @change="val => { handleASNumberChange(val) }">
              <a-select-option v-for="(opt, optIndex) in asNumbersZone" :key="optIndex" :label="opt.asnumber">
                {{ opt.asnumber }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-row :gutter="12" v-if="setMTU">
            <a-col :md="12" :lg="12">
              <a-form-item
                ref="publicmtu"
                name="publicmtu">
                <template #label>
                  <tooltip-label :title="$t('label.publicmtu')" :tooltip="apiParams.publicmtu.description"/>
                </template>
                <a-input-number
                style="width: 100%;"
                v-model:value="form.publicmtu"
                  :placeholder="apiParams.publicmtu.description"
                  @change="updateMtu(true)"/>
                <div style="color: red" v-if="errorPublicMtu" v-html="errorPublicMtu"></div>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item
                ref="privatemtu"
                name="privatemtu">
                <template #label>
                  <tooltip-label :title="$t('label.privatemtu')" :tooltip="apiParams.privatemtu.description"/>
                </template>
                <a-input-number
                style="width: 100%;"
                v-model:value="form.privatemtu"
                  :placeholder="apiParams.privatemtu.description"
                  @change="updateMtu(false)"/>
                <div style="color: red" v-if="errorPrivateMtu"  v-html="errorPrivateMtu"></div>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item
            ref="vlan"
            name="vlan"
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan">
            <template #label>
              <tooltip-label :title="$t('label.vlan')" :tooltip="apiParams.vlan.description"/>
            </template>
            <a-input
             v-model:value="form.vlan"
              :placeholder="apiParams.vlan.description"/>
          </a-form-item>
          <a-form-item
            ref="vpcid"
            name="vpcid"
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.forvpc">
            <template #label>
              <tooltip-label :title="$t('label.vpcid')" :tooltip="apiParams.vpcid.description"/>
            </template>
            <a-select
             v-model:value="form.vpcid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="vpcLoading"
              :placeholder="apiParams.vpcid.description"
              @change="val => { selectedVpc = vpcs[val] }">
              <a-select-option v-for="(opt, optIndex) in vpcs" :key="optIndex" :label="opt.name || opt.description">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            ref="externalid"
            name="externalid">
            <template #label>
              <tooltip-label :title="$t('label.externalid')" :tooltip="apiParams.externalid.description"/>
            </template>
            <a-input
             v-model:value="form.externalid"
              :placeholder="apiParams.externalid.description"/>
          </a-form-item>
          <a-form-item
            v-if="selectedNetworkOffering && (selectedNetworkOffering.networkmode !== 'ROUTED' || isAdmin())"
            ref="gateway"
            name="gateway">
            <template #label>
              <tooltip-label :title="$t('label.gateway')" :tooltip="apiParams.gateway.description"/>
            </template>
            <a-input
             v-model:value="form.gateway"
              :placeholder="apiParams.gateway.description"/>
          </a-form-item>
          <a-form-item
            v-if="selectedNetworkOffering && (selectedNetworkOffering.networkmode !== 'ROUTED' || isAdmin())"
            ref="netmask"
            name="netmask">
            <template #label>
              <tooltip-label :title="$t('label.netmask')" :tooltip="apiParams.netmask.description"/>
            </template>
            <a-input
             v-model:value="form.netmask"
              :placeholder="apiParams.netmask.description"/>
          </a-form-item>
          <a-form-item
            v-if="selectedNetworkOffering && selectedNetworkOffering.networkmode === 'ROUTED'"
            ref="cidrsize"
            name="cidrsize">
            <template #label>
              <tooltip-label :title="$t('label.cidrsize')" :tooltip="apiParams.cidrsize.description"/>
            </template>
            <a-input
              v-model:value="form.cidrsize"
              :placeholder="apiParams.cidrsize.description"/>
          </a-form-item>
          <a-form-item v-if="selectedNetworkOffering && selectedNetworkOffering.specifyipranges" name="startip" ref="startip">
            <template #label>
              <tooltip-label :title="$t('label.startipv4')" :tooltip="apiParams.startip.description"/>
            </template>
            <a-input
              v-model:value="form.startip"
              :placeholder="apiParams.startip.description"/>
          </a-form-item>
          <a-form-item v-if="selectedNetworkOffering && selectedNetworkOffering.specifyipranges" name="endip" ref="endip">
            <template #label>
              <tooltip-label :title="$t('label.endip')" :tooltip="apiParams.endip.description"/>
            </template>
            <a-input
              v-model:value="form.endip"
              :placeholder="apiParams.endip.description"/>
          </a-form-item>
          <div v-if="selectedNetworkOfferingSupportsDns">
            <a-row :gutter="12">
              <a-col :md="12" :lg="12">
                <a-form-item v-if="'dns1' in apiParams" name="dns1" ref="dns1">
                  <template #label>
                    <tooltip-label :title="$t('label.dns1')" :tooltip="apiParams.dns1.description"/>
                  </template>
                  <a-input
                    v-model:value="form.dns1"
                    :placeholder="apiParams.dns1.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item v-if="'dns2' in apiParams" name="dns2" ref="dns2">
                  <template #label>
                    <tooltip-label :title="$t('label.dns2')" :tooltip="apiParams.dns2.description"/>
                  </template>
                  <a-input
                    v-model:value="form.dns2"
                    :placeholder="apiParams.dns2.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12">
              <a-col :md="12" :lg="12">
                <a-form-item v-if="selectedNetworkOffering && selectedNetworkOffering.internetprotocol === 'DualStack' && 'ip6dns1' in apiParams" name="ip6dns1" ref="ip6dns1">
                  <template #label>
                    <tooltip-label :title="$t('label.ip6dns1')" :tooltip="apiParams.ip6dns1.description"/>
                  </template>
                  <a-input
                    v-model:value="form.ip6dns1"
                    :placeholder="apiParams.ip6dns1.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item v-if="selectedNetworkOffering && selectedNetworkOffering.internetprotocol === 'DualStack' && 'ip6dns2' in apiParams" name="ip6dns2" ref="ip6dns2">
                  <template #label>
                    <tooltip-label :title="$t('label.ip6dns2')" :tooltip="apiParams.ip6dns2.description"/>
                  </template>
                  <a-input
                    v-model:value="form.ip6dns2"
                    :placeholder="apiParams.ip6dns2.description"/>
                </a-form-item>
              </a-col>
            </a-row>
          </div>
          <a-form-item v-if="selectedNetworkOfferingSupportsSourceNat" name="sourcenatipaddress" ref="sourcenatipaddress">
            <template #label>
              <tooltip-label :title="$t('label.routerip')" :tooltip="apiParams.sourcenatipaddress?.description"/>
            </template>
            <a-input
              v-model:value="form.sourcenatipaddress"
              :placeholder="apiParams.sourcenatipaddress?.description"/>
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ $t('label.cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              ref="submit"
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
import { api } from '@/api'
import { isAdmin, isAdminOrDomainAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection.vue'

export default {
  name: 'CreateIsolatedNetworkForm',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    ResourceIcon,
    OwnershipSelection
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    vpc: {
      type: Object,
      default: null
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
      accountVisible: isAdminOrDomainAdmin(),
      accountLoading: false,
      zones: [],
      zoneLoading: false,
      selectedZone: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      selectedNetworkOffering: {},
      vpcs: [],
      vpcLoading: false,
      selectedVpc: {},
      privateMtuMax: 1500,
      publicMtuMax: 1500,
      minMTU: 68,
      errorPublicMtu: '',
      errorPrivateMtu: '',
      setMTU: false,
      asNumberLoading: false,
      selectedAsNumber: 0,
      asNumbersZone: []
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
  computed: {
    selectedNetworkOfferingSupportsDns () {
      if (this.selectedNetworkOffering) {
        const services = this.selectedNetworkOffering?.service || []
        const dnsServices = services.filter(service => service.name === 'Dns')
        return dnsServices && dnsServices.length === 1
      }
      return false
    },
    selectedNetworkOfferingSupportsSourceNat () {
      if (this.selectedNetworkOffering) {
        const services = this.selectedNetworkOffering?.service || []
        const sourcenatService = services.filter(service => service.name === 'SourceNat')
        return sourcenatService && sourcenatService.length === 1
      }
      return false
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        zoneid: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        networkofferingid: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        vpcid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchZoneData()
      this.allowSettingMTU()
    },
    allowSettingMTU () {
    },
    isAdmin () {
      return isAdmin()
    },
    isAdminOrDomainAdmin () {
      return isAdminOrDomainAdmin()
    },
    isASNumberRequired () {
      return !this.isObjectEmpty(this.selectedNetworkOffering) && this.selectedNetworkOffering.specifyasnumber && this.selectedNetworkOffering?.routingmode.toLowerCase() === 'dynamic'
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isValidTextValueForKey (obj, key) {
      return key in obj && obj[key] != null && String(obj[key]).length > 0
    },
    fetchZoneData () {
      this.zones = []
      const params = {}
      if (this.resource.zoneid && this.$route.name === 'deployVirtualMachine') {
        params.id = this.resource.zoneid
      }
      params.showicon = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        for (const i in json.listzonesresponse.zone) {
          const zone = json.listzonesresponse.zone[i]
          if (zone.networktype === 'Advanced' && zone.securitygroupsenabled !== true && zone.type !== 'Edge') {
            this.zones.push(zone)
          }
        }
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = 0
          this.handleZoneChange(this.zones[0])
        }
      })
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      this.setMTU = zone?.allowuserspecifyvrmtu || false
      this.privateMtuMax = zone?.routerprivateinterfacemaxmtu || 1500
      this.publicMtuMax = zone?.routerpublicinterfacemaxmtu || 1500
      this.updateVPCCheckAndFetchNetworkOfferingData()
      if (this.isASNumberRequired()) {
        this.fetchZoneASNumbers()
      }
    },
    fetchZoneASNumbers () {
      const params = {}
      this.asNumberLoading = true
      params.zoneid = this.selectedZone.id
      params.isallocated = false
      api('listASNumbers', params).then(json => {
        this.asNumbersZone = json.listasnumbersresponse.asnumber
        this.asNumberLoading = false
      })
    },
    handleASNumberChange (selectedIndex) {
      this.selectedAsNumber = this.asNumbersZone[selectedIndex].asnumber
      this.form.asnumber = this.selectedAsNumber
    },
    fetchOwnerOptions (OwnerOptions) {
      this.owner = {
        projectid: null,
        domainid: this.$store.getters.userInfo.domainid,
        account: this.$store.getters.userInfo.account
      }
      if (OwnerOptions.selectedAccountType === this.$t('label.account')) {
        if (!OwnerOptions.selectedAccount) {
          return
        }
        this.owner.account = OwnerOptions.selectedAccount
        this.owner.domainid = OwnerOptions.selectedDomain
        this.owner.projectid = null
      } else if (OwnerOptions.selectedAccountType === this.$t('label.project')) {
        if (!OwnerOptions.selectedProject) {
          return
        }
        this.owner.account = null
        this.owner.domainid = null
        this.owner.projectid = OwnerOptions.selectedProject
      }
      if (isAdminOrDomainAdmin()) {
        this.updateVPCCheckAndFetchNetworkOfferingData()
      }
    },
    updateVPCCheckAndFetchNetworkOfferingData () {
      if (this.vpc !== null) { // from VPC section
        this.fetchNetworkOfferingData(true)
      } else { // from guest network section
        var params = {}
        this.networkOfferingLoading = true
        if ('listVPCs' in this.$store.getters.apis) {
          api('listVPCs', params).then(json => {
            const listVPCs = json.listvpcsresponse.vpc
            var vpcAvailable = this.arrayHasItems(listVPCs)
            if (vpcAvailable === false) {
              this.fetchNetworkOfferingData(false)
            } else {
              this.fetchNetworkOfferingData()
            }
          })
        } else {
          this.fetchNetworkOfferingData(false)
        }
      }
    },
    fetchNetworkOfferingData (forVpc) {
      this.networkOfferingLoading = true
      var params = {
        zoneid: this.selectedZone.id,
        guestiptype: 'Isolated',
        state: 'Enabled'
      }
      if (isAdminOrDomainAdmin() && this.owner.domainid !== '-1') { // domain is visible only for admins
        params.domainid = this.owner.domainid
      }
      if (!isAdmin()) { // normal user is not aware of the VLANs in the system, so normal user is not allowed to create network with network offerings whose specifyvlan = true
        params.specifyvlan = false
      }
      if (forVpc !== null) {
        params.forvpc = forVpc
      }
      this.networkOfferings = []
      this.selectedNetworkOffering = {}
      api('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering
        if (this.selectedZone.isnsxenabled) {
          this.networkOfferings = this.networkOfferings.filter(offering => offering.fornsx)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.arrayHasItems(this.networkOfferings)) {
          this.form.networkofferingid = 0
          this.handleNetworkOfferingChange(this.networkOfferings[0])
        }
      })
    },
    handleNetworkOfferingChange (networkOffering) {
      this.selectedNetworkOffering = networkOffering
      if (networkOffering.forvpc) {
        this.fetchVpcData()
      }
      if (this.isASNumberRequired()) {
        this.fetchZoneASNumbers()
      }
    },
    fetchVpcData () {
      this.vpcLoading = true
      var params = {
        listAll: true,
        details: 'min'
      }
      if (this.vpc !== null) {
        params.id = this.vpc.id
      }
      api('listVPCs', params).then(json => {
        this.vpcs = json.listvpcsresponse.vpc
      }).finally(() => {
        this.vpcLoading = false
        if (this.arrayHasItems(this.vpcs)) {
          this.form.vpcid = 0
          this.selectedVpc = this.vpcs[0]
        }
      })
    },
    handleSubmit () {
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        this.actionLoading = true
        var params = {
          zoneId: this.selectedZone.id,
          name: values.name,
          displayText: values.displaytext,
          networkOfferingId: this.selectedNetworkOffering.id
        }
        var usefulFields = ['gateway', 'netmask', 'cidrsize', 'startip', 'startipv4', 'endip', 'endipv4', 'dns1', 'dns2', 'ip6dns1', 'ip6dns2', 'sourcenatipaddress', 'externalid', 'vpcid', 'vlan', 'networkdomain']
        for (var field of usefulFields) {
          if (this.isValidTextValueForKey(values, field)) {
            params[field] = values[field]
          }
        }
        if (this.isValidTextValueForKey(values, 'publicmtu')) {
          params.publicmtu = values.publicmtu
        }
        if (this.isValidTextValueForKey(values, 'privatemtu')) {
          params.privatemtu = values.privatemtu
        }
        if ('vpcid' in values) {
          params.vpcid = this.selectedVpc.id
        }

        if (this.owner.account) {
          params.account = this.owner.account
          params.domainid = this.owner.domainid
        } else if (this.owner.projectid) {
          params.domainid = this.owner.domainid
          params.projectid = this.owner.projectid
        }

        if ('asnumber' in values && this.isASNumberRequired()) {
          params.asnumber = values.asnumber
        }

        api('createNetwork', params).then(json => {
          this.$notification.success({
            message: 'Network',
            description: this.$t('message.success.create.isolated.network')
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
    updateMtu (isPublic) {
      if (isPublic) {
        if (this.form.publicmtu > this.publicMtuMax) {
          this.errorPublicMtu = this.$t('message.error.mtu.public.max.exceed')
          this.form.publicmtu = this.publicMtuMax
        } else if (this.form.publicmtu < this.minMTU) {
          this.errorPublicMtu = `${this.$t('message.error.mtu.below.min').replace('%x', this.minMTU)}`
          this.form.publicmtu = this.minMTU
        } else {
          this.errorPublicMtu = ''
        }
      } else {
        if (this.form.privatemtu > this.privateMtuMax) {
          this.errorPrivateMtu = this.$t('message.error.mtu.private.max.exceed')
          this.form.privatemtu = this.privateMtuMax
        } else if (this.form.privatemtu < this.minMTU) {
          this.errorPrivateMtu = `${this.$t('message.error.mtu.below.min').replace('%x', this.minMTU)}`
          this.form.privatemtu = this.minMTU
        } else {
          this.errorPrivateMtu = ''
        }
      }
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    resetForm () {
      this.formRef.value.resetFields()
      this.tags = []
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }

  .ant-tag {
    margin-bottom: 10px;
  }

  :deep(.custom-time-select) .ant-time-picker {
    width: 100%;
  }

  :deep(.ant-divider-horizontal) {
    margin-top: 0;
  }
}

.form {
  margin: 10px 0;
}

.tagsTitle {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}
</style>
