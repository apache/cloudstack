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
      style="text-align: justify; margin: 10px 0; padding: 24px;"
      v-html="$t(description)">
    </a-card>
    <a-form
      class="form-content"
      :ref="formRef"
      :model="form"
      :rules="rules"
     >
      <a-form-item
        name="name"
        ref="name"
        :label="$t('label.name')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-model:value="form.name"
          v-focus="true"
        />
      </a-form-item>
      <div v-if="!this.isEdgeZone">
        <a-form-item
          name="ipv4Dns1"
          ref="ipv4Dns1"
          :label="$t('label.ipv4.dns1')"
          v-bind="formItemLayout"
          has-feedback>
          <a-input v-model:value="form.ipv4Dns1" />
        </a-form-item>
        <a-form-item
          name="ipv4Dns2"
          ref="ipv4Dns2"
          :label="$t('label.ipv4.dns2')"
          v-bind="formItemLayout"
          has-feedback>
          <a-input v-model:value="form.ipv4Dns2" />
        </a-form-item>
        <a-form-item
          name="ipv6Dns1"
          ref="ipv6Dns1"
          :label="$t('label.ipv6.dns1')"
          v-bind="formItemLayout"
          v-if="isAdvancedZone"
          has-feedback>
          <a-input v-model:value="form.ipv6Dns1" />
        </a-form-item>
        <a-form-item
          name="ipv6Dns2"
          ref="ipv6Dns2"
          :label="$t('label.ipv6.dns2')"
          v-bind="formItemLayout"
          v-if="isAdvancedZone"
          has-feedback>
          <a-input v-model:value="form.ipv6Dns2" />
        </a-form-item>
        <a-form-item
          name="ipv6Cidr"
          ref="ipv6Cidr"
          :label="$t('label.ip6cidr')"
          v-bind="formItemLayout"
          v-if="isAdvancedZone && securityGroupsEnabled"
          has-feedback>
          <a-input v-model:value="form.ipv6Cidr" />
        </a-form-item>
        <a-form-item
          name="ip6gateway"
          ref="ip6gateway"
          :label="$t('label.ip6gateway')"
          v-bind="formItemLayout"
          v-if="isAdvancedZone && securityGroupsEnabled"
          has-feedback>
          <a-input v-model:value="form.ip6gateway" />
        </a-form-item>
        <a-form-item
          name="internalDns1"
          ref="internalDns1"
          :label="$t('label.internal.dns.1')"
          v-bind="formItemLayout"
          has-feedback>
          <a-input v-model:value="form.internalDns1" />
        </a-form-item>
        <a-form-item
          name="internalDns2"
          ref="internalDns2"
          :label="$t('label.internal.dns.2')"
          v-bind="formItemLayout"
          has-feedback>
          <a-input v-model:value="form.internalDns2" />
        </a-form-item>
      </div>
      <a-form-item
        name="hypervisor"
        ref="hypervisor"
        :label="$t('label.hypervisor')"
        v-bind="formItemLayout"
        has-feedback>
        <a-select
          v-model:value="form.hypervisor"
          :placeholder="$t('message.error.hypervisor.type')"
          :loading="hypervisors === null"
          :disabled="this.isEdgeZone"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="hypervisor in hypervisors" :key="hypervisor.name">
            {{ hypervisor.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div v-if="!this.isEdgeZone">
        <a-form-item
          name="networkOfferingId"
          ref="networkOfferingId"
          :label="$t('label.network.offering')"
          v-bind="formItemLayout"
          v-if="!isAdvancedZone || securityGroupsEnabled"
          has-feedback>
          <a-select
            :loading="availableNetworkOfferings === null"
            v-model:value="form.networkOfferingId"
            :placeholder="$t('message.error.network.offering')"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="networkOffering in availableNetworkOfferings"
              :key="networkOffering.id"
              :label="networkOffering.displaytext || networkOffering.name || networkOffering.description">
              {{ networkOffering.displaytext || networkOffering.name || networkOffering.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="networkDomain"
          ref="networkDomain"
          :label="$t('label.network.domain')"
          v-bind="formItemLayout"
          has-feedback>
          <a-input v-model:value="form.networkDomain" />
        </a-form-item>
        <a-form-item
          name="guestcidraddress"
          ref="guestcidraddress"
          :label="$t('label.guest.cidr')"
          v-bind="formItemLayout"
          v-if="isAdvancedZone && !securityGroupsEnabled"
          has-feedback>
          <a-input v-model:value="form.guestcidraddress" />
        </a-form-item>
      </div>
      <a-form-item
        name="isDedicated"
        ref="isDedicated"
        :label="$t('label.dedicated')"
        v-bind="formItemLayout">
        <a-switch v-model:checked="form.isDedicated" />
      </a-form-item>
      <a-form-item
        name="domainId"
        ref="domainId"
        :label="$t('label.domains')"
        v-bind="formItemLayout"
        has-feedback
        v-if="isDedicated">
        <a-select
          :loading="domains === null"
          v-model:value="form.domainId"
          :placeholder="$t('message.error.select.domain.to.dedicate')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="dom in domains" :key="dom.id" :label="dom.path">
            {{ dom.path }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-alert style="margin-top: 5px" type="warning" v-if="this.isEdgeZone">
        <template #message>
          <span v-html="$t('message.zone.edge.local.storage')" />
        </template>
      </a-alert>
      <div v-else>
        <a-form-item
          name="account"
          ref="account"
          :label="$t('label.account')"
          v-bind="formItemLayout"
          v-if="isDedicated">
          <a-input v-model:value="form.account" />
        </a-form-item>
        <a-form-item
          name="localstorageenabled"
          ref="localstorageenabled"
          :label="$t('label.local.storage.enabled')"
          v-bind="formItemLayout">
          <a-switch v-model:checked="form.localstorageenabled"/>
        </a-form-item>
        <a-form-item
          name="localstorageenabledforsystemvm"
          ref="localstorageenabledforsystemvm"
          :label="$t('label.local.storage.enabled.system.vms')"
          v-bind="formItemLayout">
          <a-switch v-model:checked="form.localstorageenabledforsystemvm" />
        </a-form-item>
      </div>
    </a-form>
    <div class="form-action">
      <a-button
        @click="handleBack"
        class="button-back"
        v-if="!isFixError">
        {{ $t('label.previous') }}
      </a-button>
      <a-button ref="submit" type="primary" @click="handleSubmit" class="button-next">
        {{ $t('label.next') }}
      </a-button>
    </div>
  </div>
</template>

<script>

import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
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
  data: () => ({
    formItemLayout: {
      labelCol: { span: 8 },
      wrapperCol: { span: 12 }
    },
    hypervisors: null,
    networkOfferings: null,
    domains: null,
    baremetalProviders: ['BaremetalDhcpProvider', 'BaremetalPxeProvider', 'BaremetalUserdataProvider'],
    selectedBaremetalProviders: [],
    availableNetworkOfferings: null,
    ipV4Regex: /^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$/i,
    ipV6Regex: /^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))$/i,
    formModel: {}
  }),
  created () {
    this.hypervisors = this.prefillContent?.hypervisors || null
    this.networkOfferings = this.prefillContent?.networkOfferings || null

    this.initForm()
    this.fetchData()
  },
  mounted () {
    this.form.name = this.name
    this.form.ipv4Dns1 = this.ipv4Dns1
    this.form.ipv4Dns2 = this.ipv4Dns2
    this.form.ipv6Dns1 = this.ipv6Dns1
    this.form.ipv6Dns2 = this.ipv6Dns2
    this.form.internalDns1 = this.internalDns1
    this.form.internalDns2 = this.internalDns2
    this.form.hypervisor = this.currentHypervisor
    this.form.networkOfferingId = this.currentNetworkOfferingId
    this.form.networkDomain = this.networkDomain
    this.form.guestcidraddress = this.isAdvancedZone && !this.securityGroupsEnabled ? this.guestcidraddress : null
    this.form.isDedicated = this.isDedicated
    this.form.domain = this.domain
    this.form.account = this.account
    this.form.localstorageenabled = this.localstorageenabled
    this.form.localstorageenabledforsystemvm = this.localstorageenabledforsystemvm

    this.formModel = toRaw(this.form)
  },
  watch: {
    formModel: {
      deep: true,
      handler (changedFields) {
        const fieldsChanged = toRaw(changedFields)
        if (fieldsChanged.networkOfferingId && this.prefillContent.networkOfferingSelected) {
          if (this.prefillContent.networkOfferingSelected.id !== fieldsChanged.networkOfferingId) {
            fieldsChanged.physicalNetworks = []
          }
        }
        if (this.networkOfferings && fieldsChanged.networkOfferingId) {
          fieldsChanged.networkOfferings = this.networkOfferings
          fieldsChanged.networkOfferingSelected = this.networkOfferings[fieldsChanged.networkOfferingId]
        }
        if (this.hypervisors && fieldsChanged.hypervisor) {
          fieldsChanged.hypervisors = this.hypervisors
          this.availableNetworkOfferings = this.getAvailableNetworkOfferings(fieldsChanged.hypervisor)
        }
        if (this.domains && fieldsChanged.domain) {
          fieldsChanged.domains = this.domains
        }
        this.$emit('fieldsChanged', fieldsChanged)
      }
    }
  },
  computed: {
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
    description () {
      return this.isEdgeZone ? 'message.desc.zone.edge' : 'message.desc.zone'
    },
    name () {
      return this.prefillContent?.name || null
    },
    ipv4Dns1 () {
      if (this.isEdgeZone) {
        return '8.8.8.8'
      }
      return this.prefillContent?.ipv4Dns1 || null
    },
    ipv4Dns2 () {
      return this.prefillContent?.ipv4Dns2 || null
    },
    ipv6Dns1 () {
      return this.prefillContent?.ipv6Dns1 || null
    },
    ipv6Dns2 () {
      return this.prefillContent?.ipv6Dns2 || null
    },
    internalDns1 () {
      if (this.isEdgeZone) {
        return '8.8.8.8'
      }
      return this.prefillContent?.internalDns1 || null
    },
    internalDns2 () {
      return this.prefillContent?.internalDns2 || null
    },
    ipv6Cidr () {
      return this.prefillContent?.ipv6Cidr || null
    },
    ip6gateway () {
      return this.prefillContent?.ip6gateway || null
    },
    currentHypervisor () {
      if (this.isEdgeZone) {
        return 'KVM'
      }
      if (this.prefillContent.hypervisor) {
        return this.prefillContent.hypervisor
      } else if (this.hypervisors && this.hypervisors.length > 0) {
        return this.hypervisors[0].name
      }
      return null
    },
    currentNetworkOfferingId () {
      const lastNetworkOfferingId = this.prefillContent?.networkOfferingSelected?.id || null
      if (this.networkOfferings) {
        if (lastNetworkOfferingId !== null && this.networkOfferings[lastNetworkOfferingId]) {
          return lastNetworkOfferingId
        }
        return this.availableNetworkOfferings[0].id
      }
      return null
    },
    networkDomain () {
      return this.prefillContent?.networkDomain || null
    },
    guestcidraddress () {
      return this.prefillContent?.guestcidraddress || '10.1.1.0/24'
    },
    isDedicated () {
      return this.prefillContent?.isDedicated || false
    },
    domain () {
      const lastDomainId = this.prefillContent?.domainId || null
      if (this.domains !== null && lastDomainId !== null && this.domains[lastDomainId]) {
        return lastDomainId
      }
      return null
    },
    account () {
      return this.prefillContent?.account || null
    },
    localstorageenabled () {
      if (this.isEdgeZone) {
        return true
      }
      return this.prefillContent?.localstorageenabled || false
    },
    localstorageenabledforsystemvm () {
      if (this.isEdgeZone) {
        return true
      }
      return this.prefillContent?.localstorageenabledforsystemvm || false
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.zone.name') }],
        ipv4Dns1: [
          { required: true, message: this.$t('message.error.ipv4.dns1') },
          { ipV4: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv4.address') }
        ],
        ipv4Dns2: [{ ipV4: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv4.address') }],
        ipv6Dns1: [{ ipV6: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv6.address') }],
        ipv6Dns2: [{ ipV6: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv6.address') }],
        ip6gateway: [{ ipV6: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv6.gateway.format') }],
        internalDns1: [
          { required: true, message: this.$t('message.error.internal.dns1') },
          { ipV4: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv4.address') }
        ],
        internalDns2: [{ ipV4: true, validator: this.checkIpFormat, message: this.$t('message.error.ipv4.address') }],
        hypervisor: [{ required: true, message: this.$t('message.error.hypervisor.type') }]
      })
    },
    fetchData () {
      api('listHypervisors').then(json => {
        this.hypervisors = json.listhypervisorsresponse.hypervisor
        if ('listSimulatorHAStateTransitions' in this.$store.getters.apis) {
          this.hypervisors.push({ name: 'Simulator' })
        }
        this.form.hypervisor = this.currentHypervisor
        this.formModel = toRaw(this.form)
      })

      if (!this.isAdvancedZone || this.securityGroupsEnabled) {
        api('listNetworkOfferings', { state: 'Enabled', guestiptype: 'Shared' }).then(json => {
          this.networkOfferings = {}
          json.listnetworkofferingsresponse.networkoffering.forEach(offering => {
            this.setupNetworkOfferingAdditionalFlags(offering)
            this.networkOfferings[offering.id] = offering
          })
          this.availableNetworkOfferings = this.getAvailableNetworkOfferings(this.currentHypervisor)
          this.form.networkOfferingId = this.currentNetworkOfferingId
          this.formModel = toRaw(this.form)
        })
      }

      api('listDomains', { listAll: true }).then(json => {
        this.domains = {}
        json.listdomainsresponse.domain.forEach(dom => {
          this.domains[dom.id] = dom
        })
        this.form.domain = this.domain
        this.formModel = toRaw(this.form)
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        if (this.isFixError) {
          this.$emit('submitLaunchZone')
          return
        }

        this.$emit('nextPressed')
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleBack () {
      this.$emit('backPressed')
    },
    setupNetworkOfferingAdditionalFlags (nOffering) {
      nOffering.havingNetscaler = false
      nOffering.havingSG = false
      nOffering.havingEIP = false
      nOffering.havingELB = false
      nOffering.selectedBaremetalProviders = []

      nOffering.service.forEach(service => {
        service.provider.forEach(provider => {
          if (provider.name === 'Netscaler') {
            nOffering.havingNetscaler = true
          } else if (this.baremetalProviders.includes(provider.name)) {
            this.selectedBaremetalProviders.push(this.name)
            nOffering.selectedBaremetalProviders = this.selectedBaremetalProviders
          }
        })

        if (service.name === 'SecurityGroup') {
          nOffering.havingSG = true
        } else if (service.name === 'StaticNat') {
          service.capability.forEach(capability => {
            if (capability.name === 'ElasticIp' && capability.value === 'true') {
              nOffering.havingEIP = true
            }
          })
        } else if (service.name === 'Lb') {
          service.capability.forEach(capability => {
            if (capability.name === 'ElasticLb' && capability.value === 'true') {
              nOffering.havingELB = true
            }
          })
        }
      })
    },
    getAvailableNetworkOfferings (hypervisor) {
      if (this.networkOfferings) {
        return Object.values(this.networkOfferings).filter(nOffering => {
          if ((hypervisor === 'VMware' ||
            (this.isAdvancedZone && this.securityGroupsEnabled)) &&
            (nOffering.havingEIP && nOffering.havingELB)) {
            return false
          }

          if (this.isAdvancedZone && this.securityGroupsEnabled && !nOffering.havingSG) {
            return false
          }

          return true
        })
      }
      return null
    },
    async checkIpFormat (rule, value) {
      if (!value || value === '') {
        return Promise.resolve()
      } else if (rule.ipV4 && !this.ipV4Regex.test(value)) {
        return Promise.reject(rule.message)
      } else if (rule.ipV6 && !this.ipV6Regex.test(value)) {
        return Promise.reject(rule.message)
      } else {
        return Promise.resolve()
      }
    }
  }
}
</script>
<style scoped lang="less">
  .form-content {
    border: 1px dashed #e9e9e9;
    border-radius: 6px;
    background-color: #fafafa;
    min-height: 200px;
    text-align: center;
    vertical-align: center;
    padding: 8px;
    padding-top: 16px;
    margin-top: 8px;
    max-height: 40vh;
    overflow-y: auto;

    :deep(.has-error) {
      .ant-form-explain {
        text-align: left;
      }
    }

    :deep(.ant-form-item-control) {
      text-align: left;
    }
  }
</style>
