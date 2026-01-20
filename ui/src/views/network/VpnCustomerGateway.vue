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
    <a-form
      class="form-layout"
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          :placeholder="apiParams.name.description"
          v-focus="true" />
      </a-form-item>
      <a-form-item ref="gateway" name="gateway">
        <template #label>
          <tooltip-label :title="$t('label.gateway')" :tooltip="apiParams.gateway.description"/>
        </template>
        <a-input
          v-model:value="form.gateway"
          :placeholder="apiParams.gateway.description" />
      </a-form-item>
      <a-form-item ref="cidrlist" name="cidrlist">
        <template #label>
          <tooltip-label :title="$t('label.cidrlist')" :tooltip="apiParams.cidrlist.description"/>
        </template>
        <a-input
          v-model:value="form.cidrlist"
          :placeholder="apiParams.cidrlist.description" />
      </a-form-item>
      <a-form-item ref="ipsecpsk" name="ipsecpsk">
        <template #label>
          <tooltip-label :title="$t('label.ipsecpsk')" :tooltip="apiParams.ipsecpsk.description"/>
        </template>
        <a-input
          v-model:value="form.ipsecpsk"
          :placeholder="apiParams.ipsecpsk.description" />
      </a-form-item>
      <a-form-item ref="ikeEncryption" name="ikeEncryption" :label="$t('label.ikeencryption')">
        <a-select
          v-model:value="form.ikeEncryption"
          showSearch
          optionFilterProp="value"
          :loading="loadingParameters"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="algo" v-for="(algo, idx) in allowedEncryptionAlgos" :key="idx">
            {{ algo }}
            <a-tooltip v-if="isObsolete('encryption', algo)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
              <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            </a-tooltip>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('encryption', form.ikeEncryption)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.ikeEncryption }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('encryption', form.ikeEncryption)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.ikeEncryption }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="ikeHash" name="ikeHash" :label="$t('label.ikehash')">
        <a-select
          v-model:value="form.ikeHash"
          showSearch
          optionFilterProp="value"
          :loading="loadingParameters"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="h" v-for="(h, idx) in allowedHashingAlgos" :key="idx">
            {{ h }}
            <a-tooltip v-if="isObsolete('hash', h)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
              <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            </a-tooltip>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('hash', form.ikeHash)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.ikeHash }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('hash', form.ikeHash)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.ikeHash }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="ikeversion" name="ikeversion">
        <template #label>
          <tooltip-label :title="$t('label.ikeversion')" :tooltip="apiParams.ikeversion.description"/>
        </template>
        <a-select
          v-model:value="form.ikeversion"
          showSearch
          optionFilterProp="value"
          :loading="loadingParameters"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="vers" v-for="(vers, idx) in allowedIkeVersions" :key="idx">
            {{ vers }}
            <a-tooltip v-if="isObsolete('ikeversion', vers)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
              <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            </a-tooltip>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('ikeversion', form.ikeversion)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.ikeversion }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('ikeversion', form.ikeversion)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.ikeversion }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="ikeDh" name="ikeDh" :label="$t('label.ikedh')">
        <a-select
          v-model:value="form.ikeDh"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            :value="group + '(' + DHGroups[group] + ')'"
            v-for="(group, idx) in allowedDhGroupKeys"
            :key="idx"
            :label="group + '(' + DHGroups[group] + ')'">
            <div v-if="group !== ''">
              {{ group+"("+DHGroups[group]+")" }}
              <a-tooltip v-if="isObsolete('dh', group)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
                <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
              </a-tooltip>
            </div>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('dh', form.ikeDh)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.ikeDh }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('dh', form.ikeDh)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.ikeDh }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="espEncryption" name="espEncryption" :label="$t('label.espencryption')">
        <a-select
          v-model:value="form.espEncryption"
          showSearch
          optionFilterProp="value"
          :loading="loadingParameters"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="algo" v-for="(algo, idx) in allowedEncryptionAlgos" :key="idx">
            {{ algo }}
            <a-tooltip v-if="isObsolete('encryption', algo)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
              <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            </a-tooltip>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('encryption', form.espEncryption)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.espEncryption }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('encryption', form.espEncryption)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.espEncryption }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="espHash" name="espHash" :label="$t('label.esphash')">
        <a-select
          v-model:value="form.espHash"
          showSearch
          optionFilterProp="value"
          :loading="loadingParameters"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="h" v-for="(h, idx) in allowedHashingAlgos" :key="idx">
            {{ h }}
            <a-tooltip v-if="isObsolete('hash', h)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
              <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            </a-tooltip>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('hash', form.espHash)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.espHash }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('hash', form.espHash)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.espHash }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="perfectForwardSecrecy" name="perfectForwardSecrecy" :label="$t('label.perfectforwardsecrecy')">
        <a-select
         v-model:value="form.perfectForwardSecrecy"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            :value="group + '(' + DHGroups[group] + ')'"
            v-for="(group, idx) in allowedDhGroupKeys"
            :key="idx"
            :label="group === '' ? DHGroups[group] : group + '(' + DHGroups[group] + ')'">
            <div v-if="group === ''">
              {{ DHGroups[group] }}
            </div>
            <div v-else>
              {{ group+"("+DHGroups[group]+")" }}
              <a-tooltip v-if="isObsolete('dh', group)" :title="$t('message.vpn.customer.gateway.obsolete.parameter.tooltip')">
                <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
              </a-tooltip>
            </div>
          </a-select-option>
        </a-select>
        <template #extra v-if="isExcluded('dh', form.perfectForwardSecrecy)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@error-color'] }" />
            {{ form.perfectForwardSecrecy }} {{ $t('message.vpn.customer.gateway.excluded.parameter') }}
          </span>
        </template>
        <template #extra v-else-if="isObsolete('dh', form.perfectForwardSecrecy)">
          <span>
            <warning-outlined :style="{ color: $config.theme['@warning-color'] }" />
            {{ form.perfectForwardSecrecy }} {{ $t('message.vpn.customer.gateway.obsolete.parameter') }}
          </span>
        </template>
      </a-form-item>
      <a-form-item ref="ikelifetime" name="ikelifetime">
        <template #label>
          <tooltip-label :title="$t('label.ikelifetime')" :tooltip="apiParams.ikelifetime.description"/>
        </template>
        <a-input
          v-model:value="form.ikelifetime"
          :placeholder="apiParams.ikelifetime.description"/>
      </a-form-item>
      <a-form-item ref="esplifetime" name="esplifetime">
        <template #label>
          <tooltip-label :title="$t('label.esplifetime')" :tooltip="apiParams.esplifetime.description"/>
        </template>
        <a-input
          v-model:value="form.esplifetime"
          :placeholder="apiParams.esplifetime.description"/>
      </a-form-item>
      <a-form-item ref="dpd" name="dpd">
        <template #label>
          <tooltip-label :title="$t('label.dpd')" :tooltip="apiParams.dpd.description"/>
        </template>
        <a-switch v-model:checked="form.dpd"/>
      </a-form-item>
      <a-form-item ref="splitconnections" name="splitconnections" v-if="form.ikeversion !== 'ikev1'">
        <template #label>
          <tooltip-label :title="$t('label.splitconnections')" :tooltip="apiParams.splitconnections.description"/>
        </template>
        <a-switch v-model:checked="form.splitconnections"/>
      </a-form-item>
      <a-form-item ref="forceencap" name="forceencap">
        <template #label>
          <tooltip-label :title="$t('label.forceencap')" :tooltip="apiParams.forceencap.description"/>
        </template>
        <a-switch v-model:checked="form.forceencap"/>
      </a-form-item>
      <div class="action-button">
        <a-button @click="$emit('cancel')">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button type="primary" @click="handleSubmit" html-type="submit">
          {{ $t('label.ok') }}
        </a-button>
      </div>
    </a-form>
  </div>
</template>
<script>

import { getAPI } from '@/api'
import { ref, reactive, toRaw } from 'vue'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'VpnCustomerGateway',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  props: {
    initialValues: {
      type: Object,
      default: () => ({})
    },
    apiParams: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      encryptionAlgo: [
        'aes128',
        'aes192',
        'aes256',
        '3des'
      ],
      hash: [
        'sha1',
        'sha256',
        'sha384',
        'sha512',
        'md5'
      ],
      ikeVersions: [
        'ike',
        'ikev1',
        'ikev2'
      ],
      DHGroups: {
        'Group 2': 'modp1024',
        'Group 5': 'modp1536',
        'Group 14': 'modp2048',
        'Group 15': 'modp3072',
        'Group 16': 'modp4096',
        'Group 17': 'modp6144',
        'Group 18': 'modp8192'
      },
      ikeDhGroupInitialKey: 'Group 5',
      isSubmitted: false,
      ikeversion: 'ike',
      allowedEncryptionAlgos: [],
      allowedHashingAlgos: [],
      allowedIkeVersions: [],
      allowedDhGroupKeys: [],
      allowedDhGroupValues: [],
      obsoleteEncryptionAlgos: [],
      obsoleteHashingAlgos: [],
      obsoleteIkeVersions: [],
      obsoleteDhGroups: [],
      loadingParameters: false
    }
  },
  created () {
    this.initForm()
    this.fetchVpnCustomerGatewayParameters()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      const defaults = {
        name: '',
        gateway: '',
        cidrlist: '',
        ipsecpsk: '',
        ikeEncryption: '',
        ikeHash: '',
        ikeversion: '',
        ikeDh: '',
        espEncryption: '',
        espHash: '',
        perfectForwardSecrecy: 'None',
        ikelifetime: '86400',
        esplifetime: '3600',
        dpd: false,
        splitconnections: false,
        forceencap: false
      }
      this.form = reactive({
        ...defaults,
        ...this.initialValues
      })
      if (this.initialValues.ikeDh) {
        const ikeDhKey = Object.keys(this.DHGroups).find(key => this.DHGroups[key] === this.initialValues.ikeDh)
        this.form.ikeDh = ikeDhKey + '(' + this.initialValues.ikeDh + ')'
      }
      if (this.initialValues.espDh) {
        const espDhKey = Object.keys(this.DHGroups).find(key => this.DHGroups[key] === this.initialValues.espDh)
        this.form.perfectForwardSecrecy = espDhKey + '(' + this.initialValues.espDh + ')'
      }
      this.rules = reactive({
        name: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        cidrlist: [{ required: true, message: this.$t('label.required') }],
        ipsecpsk: [{ required: true, message: this.$t('label.required') }]
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    resetSubmission () {
      this.isSubmitted = false
    },
    buildPolicies (values) {
      let ikepolicy = values.ikeEncryption + '-' + values.ikeHash + ';'
      ikepolicy += (values.ikeDh.split('(')[1]).split(')')[0]

      let esppolicy = values.espEncryption + '-' + values.espHash
      if (values.perfectForwardSecrecy !== 'None') {
        esppolicy += ';' + (values.perfectForwardSecrecy.split('(')[1]).split(')')[0]
      }

      return { ikepolicy, esppolicy }
    },
    isObsolete (type, value) {
      switch (type) {
        case 'encryption':
          return this.obsoleteEncryptionAlgos.some(v => v.toLowerCase() === value)
        case 'hash':
          return this.obsoleteHashingAlgos.some(v => v.toLowerCase() === value)
        case 'ikeversion':
          return this.obsoleteIkeVersions.some(v => v.toLowerCase() === value)
        case 'dh': {
          const key = value.split('(')[0]
          return this.obsoleteDhGroups.some(v => v.toLowerCase() === this.DHGroups[key])
        }
        default:
          return false
      }
    },
    isExcluded (type, value) {
      switch (type) {
        case 'encryption':
          return !this.allowedEncryptionAlgos.some(v => v.toLowerCase() === value)
        case 'hash':
          return !this.allowedHashingAlgos.some(v => v.toLowerCase() === value)
        case 'ikeversion':
          return !this.allowedIkeVersions.some(v => v.toLowerCase() === value)
        case 'dh': {
          if (value === '' || value === 'None') return false
          const key = value.split('(')[0]
          return !this.allowedDhGroupValues.some(v => v.toLowerCase() === this.DHGroups[key])
        }
        default:
          return false
      }
    },
    async fetchVpnCustomerGatewayParameters () {
      const getParam = (obj, key) => {
        const val = obj?.[key.toLowerCase()]
        return typeof val === 'string'
          ? val.split(',').map(s => s.trim()).filter(Boolean)
          : []
      }
      const getAllowed = (baseList, excludedList) => {
        const excluded = new Set(excludedList.map(v => v.toLowerCase()))
        return baseList.filter(item => !excluded.has(item.toLowerCase()))
      }

      this.loadingParameters = true

      const response = await getAPI('listCapabilities', { domainid: this.initialValues.domainid })
      const capability = response.listcapabilitiesresponse?.capability || {}
      const parameters = capability.vpncustomergatewayparameters || {}

      const excludedEnc = getParam(parameters, 'excludedEncryptionAlgorithms')
      const excludedHash = getParam(parameters, 'excludedHashingAlgorithms')
      const excludedIke = getParam(parameters, 'excludedIkeVersions')
      const excludedDh = getParam(parameters, 'excludedDhGroups')

      this.allowedEncryptionAlgos = getAllowed(this.encryptionAlgo, excludedEnc)
      this.allowedHashingAlgos = getAllowed(this.hash, excludedHash)
      this.allowedIkeVersions = getAllowed(this.ikeVersions, excludedIke)

      const dhValues = Object.values(this.DHGroups)
      this.allowedDhGroupValues = getAllowed(dhValues, excludedDh)
      this.allowedDhGroupKeys = Object.entries(this.DHGroups)
        .filter(([key, value]) => this.allowedDhGroupValues.includes(value))
        .map(([key]) => key)

      this.form.ikeEncryption = this.form.ikeEncryption || this.allowedEncryptionAlgos[0]
      this.form.ikeHash = this.form.ikeHash || this.allowedHashingAlgos[0]
      this.form.ikeversion = this.form.ikeversion || this.allowedIkeVersions[0]
      this.form.espEncryption = this.form.espEncryption || this.allowedEncryptionAlgos[0]
      this.form.espHash = this.form.espHash || this.allowedHashingAlgos[0]
      if (!this.initialValues.ikeDh) {
        if (this.allowedDhGroupKeys.includes(this.ikeDhGroupInitialKey)) {
          this.form.ikeDh = this.ikeDhGroupInitialKey + '(' + this.DHGroups[this.ikeDhGroupInitialKey] + ')'
        } else {
          this.form.ikeDh = this.allowedDhGroupKeys[0] + '(' + this.DHGroups[this.allowedDhGroupKeys[0]] + ')'
        }
      }

      this.obsoleteEncryptionAlgos = getParam(parameters, 'obsoleteEncryptionAlgorithms')
      this.obsoleteHashingAlgos = getParam(parameters, 'obsoleteHashingAlgorithms')
      this.obsoleteIkeVersions = getParam(parameters, 'obsoleteIkeVersions')
      this.obsoleteDhGroups = getParam(parameters, 'obsoleteDhGroups')

      this.loadingParameters = false
    },
    handleSubmit (e) {
      if (e && e.preventDefault) {
        e.preventDefault()
      }
      if (this.isSubmitted) return

      this.formRef.value.validate().then(() => {
        this.isSubmitted = true
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const { ikepolicy, esppolicy } = this.buildPolicies(values)

        const payload = {
          name: values.name,
          gateway: values.gateway,
          cidrlist: values.cidrlist,
          ipsecpsk: values.ipsecpsk,
          ikelifetime: values.ikelifetime,
          esplifetime: values.esplifetime,
          dpd: values.dpd,
          forceencap: values.forceencap,
          ikepolicy: ikepolicy,
          esppolicy: esppolicy,
          splitconnections: values.splitconnections,
          ikeversion: values.ikeversion
        }
        this.$emit('submit', {
          payload,
          rawValues: values
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.form-layout {
  width: 500px;
}
</style>
