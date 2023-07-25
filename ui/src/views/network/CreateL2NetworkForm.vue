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
          <a-form-item v-if="isAdminOrDomainAdmin()" name="domainid" ref="domainid">
            <template #label>
              <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
            </template>
            <a-select
              v-model:value="form.domainid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="domainLoading"
              :placeholder="apiParams.domainid.description"
              @change="val => { handleDomainChange(domains[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <block-outlined v-else-if="optIndex !== 0" style="margin-right: 5px" />
                  {{ opt.path || opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="accountVisible" name="account" ref="account">
            <template #label>
              <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
            </template>
            <a-select
             v-model:value="form.account"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="accountLoading"
              :placeholder="apiParams.account.description"
              @change="val => { handleAccountChange(accounts[val]) }">
              <a-select-option v-for="(opt, optIndex) in accounts" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="networkofferingid" ref="networkofferingid">
            <template #label>
              <tooltip-label :title="$t('label.networkofferingid')" :tooltip="apiParams.networkofferingid.description"/>
            </template>
            <a-select
              v-model:value="form.networkofferingid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networkOfferingLoading"
              :placeholder="apiParams.networkofferingid.description"
              @change="val => { handleNetworkOfferingChange(networkOfferings[val]) }">
              <a-select-option v-for="(opt, optIndex) in networkOfferings" :key="optIndex">
                {{ opt.displaytext || opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan"
            name="vlanid"
            ref="vlanid">
            <template #label>
              <tooltip-label :title="$t('label.vlan')" :tooltip="apiParams.vlan ? apiParams.vlan.description : $t('label.vlanid')"/>
            </template>
            <a-input
              v-model:value="form.vlanid"
              :placeholder="apiParams.vlan ? apiParams.vlan.description : $t('label.vlanid')"/>
          </a-form-item>
          <a-form-item
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan"
            name="bypassvlanoverlapcheck"
            ref="bypassvlanoverlapcheck">
            <template #label>
              <tooltip-label :title="$t('label.bypassvlanoverlapcheck')" :tooltip="apiParams.bypassvlanoverlapcheck ? apiParams.bypassvlanoverlapcheck.description : null"/>
            </template>
            <a-switch v-model:checked="form.bypassvlanoverlapcheck" />
          </a-form-item>
          <a-form-item
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan"
            name="isolatedpvlantype"
            ref="isolatedpvlantype">
            <template #label>
              <tooltip-label :title="$t('label.isolatedpvlantype')" :tooltip="apiParams.isolatedpvlantype.description"/>
            </template>
            <a-radio-group
              v-model:value="form.isolatedpvlantype"
              buttonStyle="solid"
              @change="selected => { isolatePvlanType = selected.target.value }">
              <a-radio-button value="none">
                {{ $t('label.none') }}
              </a-radio-button>
              <a-radio-button value="community">
                {{ $t('label.community') }}
              </a-radio-button>
              <a-radio-button value="isolated">
                {{ $t('label.secondary.isolated.vlan.type.isolated') }}
              </a-radio-button>
              <a-radio-button value="promiscuous">
                {{ $t('label.secondary.isolated.vlan.type.promiscuous') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            v-if="['community', 'isolated'].includes(form.isolatedpvlantype)"
             name="isolatedpvlanid"
             ref="isolatedpvlanid">
            <template #label>
              <tooltip-label :title="$t('label.isolatedpvlanid')" :tooltip="apiParams.isolatedpvlan.description"/>
            </template>
            <a-input
              v-model:value="form.isolatedpvlan"
              :placeholder="apiParams.isolatedpvlan.description"/>
          </a-form-item>
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
import { api } from '@/api'
import { isAdmin, isAdminOrDomainAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateL2NetworkForm',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    ResourceIcon
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
      domains: [],
      domainLoading: false,
      selectedDomain: {},
      accountVisible: isAdminOrDomainAdmin(),
      accounts: [],
      accountLoading: false,
      selectedAccount: {},
      zones: [],
      zoneLoading: false,
      selectedZone: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      selectedNetworkOffering: {},
      isolatePvlanType: 'none'
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
    this.domains = [
      {
        id: '-1',
        name: ' '
      }
    ]
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        isolatedpvlantype: 'none'
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        networkofferingid: [{ required: true, message: this.$t('message.error.select') }],
        vlanid: [{ required: true, message: this.$t('message.please.enter.value') }]
      })
    },
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
    },
    isAdminOrDomainAdmin () {
      return isAdminOrDomainAdmin()
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    isValidTextValueForKey (obj, key) {
      return this.isValidValueForKey(obj, key) && String(obj[key]).length > 0
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
          if (zone.networktype === 'Advanced') {
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
      this.updateVPCCheckAndFetchNetworkOfferingData()
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
      params.details = 'min'
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
        this.form.domainid = 0
        this.handleDomainChange(this.domains[0])
      })
    },
    handleDomainChange (domain) {
      this.selectedDomain = domain
      this.accountVisible = domain.id !== '-1'
      if (isAdminOrDomainAdmin()) {
        this.updateVPCCheckAndFetchNetworkOfferingData()
        this.fetchAccounts()
      }
    },
    handleAccountChange (account) {
      this.selectedAccount = account
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
        guestiptype: 'L2',
        state: 'Enabled'
      }
      if (isAdminOrDomainAdmin() && this.selectedDomain.id !== '-1') { // domain is visible only for admins
        params.domainid = this.selectedDomain.id
      }
      if (!isAdmin()) { // normal user is not aware of the VLANs in the system, so normal user is not allowed to create network with network offerings whose specifyvlan = true
        params.specifyvlan = false
      }
      if (forVpc !== null) {
        params.forvpc = forVpc
      }
      api('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering
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
    },
    fetchAccounts () {
      this.accountLoading = true
      var params = {}
      if (isAdminOrDomainAdmin() && this.selectedDomain.id !== '-1') { // domain is visible only for admins
        params.domainid = this.selectedDomain.id
      }
      this.accounts = [
        {
          id: '-1',
          name: ' '
        }
      ]
      this.selectedAccount = {}
      api('listAccounts', params).then(json => {
        const listAccounts = json.listaccountsresponse.account || []
        this.accounts = this.accounts.concat(listAccounts)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.accountLoading = false
        if (this.arrayHasItems(this.accounts)) {
          this.form.account = null
        }
      })
    },
    handleSubmit (e) {
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
        if (this.isValidTextValueForKey(values, 'vlanid')) {
          params.vlan = values.vlanid
        }
        if (this.isValidValueForKey(values, 'bypassvlanoverlapcheck')) {
          params.bypassvlanoverlapcheck = values.bypassvlanoverlapcheck
        }
        if ('domainid' in values && values.domainid > 0) {
          params.domainid = this.selectedDomain.id
          if (this.isValidTextValueForKey(values, 'account') && this.selectedAccount.id !== '-1') {
            params.account = this.selectedAccount.name
          }
        }
        if (this.isValidValueForKey(values, 'isolatedpvlantype') && values.isolatedpvlantype !== 'none') {
          params.isolatedpvlantype = values.isolatedpvlantype
          if (this.isValidValueForKey(values, 'isolatedpvlan')) {
            params.isolatedpvlan = values.isolatedpvlan
          }
        }
        api('createNetwork', params).then(json => {
          this.$notification.success({
            message: 'Network',
            description: this.$t('message.success.create.l2.network')
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
