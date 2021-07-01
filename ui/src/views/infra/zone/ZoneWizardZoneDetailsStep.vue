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
      style="text-align: justify; margin: 10px 0; padding: 24px;"
      v-html="$t(description)">
    </a-card>
    <a-form class="form-content" :form="form" @submit="handleSubmit">
      <a-form-item
        :label="$t('label.name')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-decorator="['name', {
            rules: [{
              required: true,
              message: $t('message.error.zone.name'),
              initialValue: name
            }]
          }]"
          autoFocus
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.ipv4.dns1')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-decorator="['ipv4Dns1', {
            rules: [
              {
                required: true,
                message: $t('message.error.ipv4.dns1'),
                initialValue: ipv4Dns1
              },
              {
                validator: checkIpFormat,
                ipV4: true,
                message: $t('message.error.ipv4.address')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.ipv4.dns2')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-decorator="['ipv4Dns2', {
            rules: [
              {
                message: $t('message.error.ipv4.dns2'),
                initialValue: ipv4Dns2
              },
              {
                validator: checkIpFormat,
                ipV4: true,
                message: $t('message.error.ipv4.address')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.ipv6.dns1')"
        v-bind="formItemLayout"
        v-if="isAdvancedZone"
        has-feedback>
        <a-input
          v-decorator="['ipv6Dns1', {
            rules: [
              {
                message: $t('message.error.ipv6.dns1'),
                initialValue: ipv6Dns1
              },
              {
                validator: checkIpFormat,
                ipV6: true,
                message: $t('message.error.ipv6.address')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.ipv6.dns2')"
        v-bind="formItemLayout"
        v-if="isAdvancedZone"
        has-feedback>
        <a-input
          v-decorator="['ipv6Dns2', {
            rules: [
              {
                message: $t('message.error.ipv6.dns2'),
                initialValue: ipv6Dns2
              },
              {
                validator: checkIpFormat,
                ipV6: true,
                message: $t('message.error.ipv6.address')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.ip6cidr')"
        v-bind="formItemLayout"
        v-if="isAdvancedZone && securityGroupsEnabled"
        has-feedback>
        <a-input
          v-decorator="['ipv6Cidr', {
            rules: [
              {
                message: $t('message.error.ipv6.cidr'),
                initialValue: ipv6Cidr
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.ip6gateway')"
        v-bind="formItemLayout"
        v-if="isAdvancedZone && securityGroupsEnabled"
        has-feedback>
        <a-input
          v-decorator="['ip6gateway', {
            rules: [
              {
                message: $t('message.error.ipv6.gateway'),
                initialValue: ip6gateway
              },
              {
                validator: checkIpFormat,
                ipV6: true,
                message: $t('message.error.ipv6.gateway.format')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.internal.dns.1')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-decorator="['internalDns1', {
            rules: [
              {
                required: true,
                message: $t('message.error.internal.dns1'),
                initialValue: internalDns1
              },
              {
                validator: checkIpFormat,
                ipV4: true,
                message: $t('message.error.ipv4.address')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.internal.dns.2')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-decorator="['internalDns2', {
            rules: [
              {
                message: $t('message.error.internal.dns2'),
                initialValue: internalDns2
              },
              {
                validator: checkIpFormat,
                ipV4: true,
                message: $t('message.error.ipv4.address')
              }
            ]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.hypervisor')"
        v-bind="formItemLayout"
        has-feedback>
        <a-select
          :loading="hypervisors === null"
          showSearch
          v-decorator="['hypervisor',{
            rules: [{
              required: true,
              message: $t('message.error.hypervisor.type'),
              initialValue: currentHypervisor
            }]
          }]"
          :placeholder="$t('message.error.hypervisor.type')"
        >
          <a-select-option v-for="hypervisor in hypervisors" :key="hypervisor.name">
            {{ hypervisor.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.network.offering')"
        v-bind="formItemLayout"
        v-if="!isAdvancedZone || securityGroupsEnabled"
        has-feedback>
        <a-select
          :loading="availableNetworkOfferings === null"
          v-decorator="['networkOfferingId', {
            rules: [{
              message: $t('message.error.network.offering'),
              initialValue: currentNetworkOfferingId
            }]
          }]"
          :placeholder="$t('message.error.network.offering')"
        >
          <a-select-option
            v-for="networkOffering in availableNetworkOfferings"
            :key="networkOffering.id">
            {{ networkOffering.displaytext || networkOffering.name || networkOffering.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.network.domain')"
        v-bind="formItemLayout"
        has-feedback>
        <a-input
          v-decorator="['networkDomain', {
            rules: [{
              message: $t('message.error.network.domain'),
              intialValue: networkDomain
            }]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.guest.cidr')"
        v-bind="formItemLayout"
        v-if="isAdvancedZone && !securityGroupsEnabled"
        has-feedback>
        <a-input
          v-decorator="['guestcidraddress', {
            rules: [{
              intialValue: guestcidraddress
            }]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.dedicated')"
        v-bind="formItemLayout">
        <a-switch
          v-decorator="['isDedicated', { valuePropName: 'checked' }]"
          :value="isDedicated"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.domains')"
        v-bind="formItemLayout"
        has-feedback
        v-if="isDedicated">
        <a-select
          :loading="domains === null"
          v-decorator="['domainId', {
            rules: [{
              initialValue: domain
            }]
          }]"
          :placeholder="$t('message.error.select.domain.to.dedicate')"
        >
          <a-select-option v-for="dom in domains" :key="dom.id">
            {{ dom.path }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.account')"
        v-bind="formItemLayout"
        v-if="isDedicated">
        <a-input
          v-decorator="['account', {
            rules: [{
              intialValue: guestcidraddress
            }]
          }]"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.local.storage.enabled')"
        v-bind="formItemLayout">
        <a-switch
          v-decorator="['localstorageenabled', { valuePropName: 'checked' }]"
          :value="localstorageenabled"
        />
      </a-form-item>
      <a-form-item
        :label="$t('label.local.storage.enabled.system.vms')"
        v-bind="formItemLayout">
        <a-switch
          v-decorator="['localstorageenabledforsystemvm', { valuePropName: 'checked' }]"
          :value="localstorageenabledforsystemvm"
        />
      </a-form-item>
    </a-form>
    <div class="form-action">
      <a-button
        @click="handleBack"
        class="button-back"
        v-if="!isFixError">
        {{ $t('label.previous') }}
      </a-button>
      <a-button type="primary" @click="handleSubmit" class="button-next">
        {{ $t('label.next') }}
      </a-button>
    </div>
  </div>
</template>

<script>

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
    description: 'message.desc.zone',
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
    ipV6Regex: /^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))$/i
  }),
  created () {
    this.hypervisors = this.prefillContent.hypervisors ? this.prefillContent.hypervisors : null
    this.networkOfferings = this.prefillContent.networkOfferings ? this.prefillContent.networkOfferings : null
    this.form = this.$form.createForm(this, {
      onFieldsChange: (_, changedFields) => {
        if (changedFields.networkOfferingId && this.prefillContent.networkOfferingSelected) {
          if (this.prefillContent.networkOfferingSelected.id !== changedFields.networkOfferingId.value) {
            changedFields.physicalNetworks = []
          }
        }
        if (this.networkOfferings && changedFields.networkOfferingId) {
          changedFields.networkOfferings = this.networkOfferings
          changedFields.networkOfferingSelected = this.networkOfferings[changedFields.networkOfferingId.value]
        }
        if (this.hypervisors && changedFields.hypervisor) {
          changedFields.hypervisors = this.hypervisors
          this.availableNetworkOfferings = this.getAvailableNetworkOfferings(changedFields.hypervisor)
        }
        if (this.domains && changedFields.domain) {
          changedFields.domains = this.domains
        }
        this.$emit('fieldsChanged', changedFields)
      }
    })

    this.fetchData()
  },
  mounted () {
    this.form.setFieldsValue({
      name: this.name,
      ipv4Dns1: this.ipv4Dns1,
      ipv4Dns2: this.ipv4Dns2,
      ipv6Dns1: this.ipv6Dns1,
      ipv6Dns2: this.ipv6Dns2,
      internalDns1: this.internalDns1,
      internalDns2: this.internalDns2,
      hypervisor: this.currentHypervisor,
      networkOfferingId: this.currentNetworkOfferingId,
      networkDomain: this.networkDomain,
      guestcidraddress: this.isAdvancedZone && !this.securityGroupsEnabled ? this.guestcidraddress : null,
      isDedicated: this.isDedicated,
      domain: this.domain,
      account: this.account,
      localstorageenabled: this.localstorageenabled,
      localstorageenabledforsystemvm: this.localstorageenabledforsystemvm
    })
  },
  computed: {
    isAdvancedZone () {
      return this.zoneType === 'Advanced'
    },
    zoneType () {
      return this.prefillContent.zoneType ? this.prefillContent.zoneType.value : null
    },
    securityGroupsEnabled () {
      return this.isAdvancedZone && (this.prefillContent.securityGroupsEnabled ? this.prefillContent.securityGroupsEnabled.value : false)
    },
    name () {
      return this.prefillContent.name ? this.prefillContent.name.value : null
    },
    ipv4Dns1 () {
      return this.prefillContent.ipv4Dns1 ? this.prefillContent.ipv4Dns1.value : null
    },
    ipv4Dns2 () {
      return this.prefillContent.ipv4Dns2 ? this.prefillContent.ipv4Dns2.value : null
    },
    ipv6Dns1 () {
      return this.prefillContent.ipv6Dns1 ? this.prefillContent.ipv6Dns1.value : null
    },
    ipv6Dns2 () {
      return this.prefillContent.ipv6Dns2 ? this.prefillContent.ipv6Dns2.value : null
    },
    internalDns1 () {
      return this.prefillContent.internalDns1 ? this.prefillContent.internalDns1.value : null
    },
    internalDns2 () {
      return this.prefillContent.internalDns2 ? this.prefillContent.internalDns2.value : null
    },
    ipv6Cidr () {
      return this.prefillContent.ipv6Cidr ? this.prefillContent.ipv6Cidr.value : null
    },
    ip6gateway () {
      return this.prefillContent.ip6gateway ? this.prefillContent.ip6gateway.value : null
    },
    currentHypervisor () {
      if (this.prefillContent.hypervisor) {
        return this.prefillContent.hypervisor.value
      } else if (this.hypervisors && this.hypervisors.length > 0) {
        return this.hypervisors[0]
      }
      return null
    },
    currentNetworkOfferingId () {
      const lastNetworkOfferingId = this.prefillContent.networkOfferingSelected ? this.prefillContent.networkOfferingSelected.id : null
      if (this.networkOfferings) {
        if (lastNetworkOfferingId !== null && this.networkOfferings[lastNetworkOfferingId]) {
          return lastNetworkOfferingId
        }
        return this.availableNetworkOfferings[0].id
      }
      return null
    },
    networkDomain () {
      return this.prefillContent.networkDomain ? this.prefillContent.networkDomain.value : null
    },
    guestcidraddress () {
      return this.prefillContent.guestcidraddress ? this.prefillContent.guestcidraddress.value : '10.1.1.0/24'
    },
    isDedicated () {
      return this.prefillContent.isDedicated ? this.prefillContent.isDedicated.value : false
    },
    domain () {
      const lastDomainId = this.prefillContent.domainId ? this.prefillContent.domainId.value : null
      if (this.domains !== null && lastDomainId !== null && this.domains[lastDomainId]) {
        return lastDomainId
      }
      return null
    },
    account () {
      return this.prefillContent.account ? this.prefillContent.account.value : null
    },
    localstorageenabled () {
      return this.prefillContent.localstorageenabled ? this.prefillContent.localstorageenabled.value : false
    },
    localstorageenabledforsystemvm () {
      return this.prefillContent.localstorageenabledforsystemvm ? this.prefillContent.localstorageenabledforsystemvm.value : false
    }
  },
  methods: {
    fetchData () {
      const cForm = this.form
      api('listHypervisors', { listAll: true }).then(json => {
        this.hypervisors = json.listhypervisorsresponse.hypervisor
        if ('listSimulatorHAStateTransitions' in this.$store.getters.apis) {
          this.hypervisors.push({ name: 'Simulator' })
        }
        cForm.setFieldsValue({
          hypervisor: this.currentHypervisor
        })
      })

      if (!this.isAdvancedZone || this.securityGroupsEnabled) {
        api('listNetworkOfferings', { state: 'Enabled', guestiptype: 'Shared' }).then(json => {
          this.networkOfferings = {}
          json.listnetworkofferingsresponse.networkoffering.forEach(offering => {
            this.setupNetworkOfferingAdditionalFlags(offering)
            this.networkOfferings[offering.id] = offering
          })
          this.availableNetworkOfferings = this.getAvailableNetworkOfferings(this.currentHypervisor)
          cForm.setFieldsValue({
            networkOfferingId: this.currentNetworkOfferingId
          })
        })
      }

      api('listDomains', { listAll: true }).then(json => {
        this.domains = {}
        json.listdomainsresponse.domain.forEach(dom => {
          this.domains[dom.id] = dom
        })
        cForm.setFieldsValue({
          domain: this.domain
        })
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        if (this.isFixError) {
          this.$emit('submitLaunchZone')
          return
        }

        this.$emit('nextPressed')
      })
    },
    handleBack (e) {
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
    checkIpFormat (rule, value, callback) {
      if (!value || value === '') {
        callback()
      } else if (rule.ipV4 && !this.ipV4Regex.test(value)) {
        callback(rule.message)
      } else if (rule.ipV6 && !this.ipV6Regex.test(value)) {
        callback(rule.message)
      } else {
        callback()
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

    /deep/.has-error {
      .ant-form-explain {
        text-align: left;
      }
    }

    /deep/.ant-form-item-control {
      text-align: left;
    }
  }
</style>
