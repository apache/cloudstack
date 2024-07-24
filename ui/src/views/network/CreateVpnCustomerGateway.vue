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
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="algo" v-for="(algo, idx) in encryptionAlgo" :key="idx">
            {{ algo }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="ikeHash" name="ikeHash" :label="$t('label.ikehash')">
        <a-select
          v-model:value="form.ikeHash"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="h" v-for="(h, idx) in hash" :key="idx">
            {{ h }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="ikeversion" name="ikeversion">
        <template #label>
          <tooltip-label :title="$t('label.ikeversion')" :tooltip="apiParams.ikeversion.description"/>
        </template>
        <a-select
          v-model:value="form.ikeversion"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="vers" v-for="(vers, idx) in ikeVersions" :key="idx">
            {{ vers }}
          </a-select-option>
        </a-select>
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
            :value="DHGroups[group]"
            v-for="(group, idx) in Object.keys(DHGroups)"
            :key="idx"
            :label="group + '(' + DHGroups[group] + ')'">
            <div v-if="group !== ''">
              {{ group+"("+DHGroups[group]+")" }}
            </div>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="espEncryption" name="espEncryption" :label="$t('label.espencryption')">
        <a-select
          v-model:value="form.espEncryption"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="algo" v-for="(algo, idx) in encryptionAlgo" :key="idx">
            {{ algo }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="espHash" name="espHash" :label="$t('label.esphash')">
        <a-select
          v-model:value="form.espHash"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="h" v-for="(h, idx) in hash" :key="idx">
            {{ h }}
          </a-select-option>
        </a-select>
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
            :value="DHGroups[group]"
            v-for="(group, idx) in Object.keys(DHGroups)"
            :key="idx"
            :label="group === '' ? DHGroups[group] : group + '(' + DHGroups[group] + ')'">
            <div v-if="group === ''">
              {{ DHGroups[group] }}
            </div>
            <div v-else>
              {{ group+"("+DHGroups[group]+")" }}
            </div>
          </a-select-option>
        </a-select>
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
        <a-button @click="closeModal">
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
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVpnCustomerGateway',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
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
        '': 'None',
        'Group 2': 'modp1024',
        'Group 5': 'modp1536',
        'Group 14': 'modp2048',
        'Group 15': 'modp3072',
        'Group 16': 'modp4096',
        'Group 17': 'modp6144',
        'Group 18': 'modp8192'
      },
      ikeDhGroupInitialValue: 'Group 5(modp1536)',
      isSubmitted: false,
      ikeversion: 'ike'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVpnCustomerGateway')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        ikeEncryption: 'aes128',
        ikeHash: 'sha1',
        ikeversion: 'ike',
        ikeDh: 'Group 5(modp1536)',
        espEncryption: 'aes128',
        espHash: 'sha1',
        perfectForwardSecrecy: 'None',
        ikelifetime: '86400',
        esplifetime: '3600',
        dpd: false,
        splitconnections: false,
        forceencap: false
      })
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
    handleSubmit (e) {
      e.preventDefault()
      if (this.isSubmitted) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        let ikepolicy = values.ikeEncryption + '-' + values.ikeHash + ';'
        ikepolicy += (values.ikeDh !== this.ikeDhGroupInitialValue) ? values.ikeDh : (values.ikeDh.split('(')[1]).split(')')[0]
        let esppolicy = values.espEncryption + '-' + values.espHash
        if (values.perfectForwardSecrecy !== 'None') {
          esppolicy += ';' + (values.perfectForwardSecrecy)
        }
        api('createVpnCustomerGateway', {
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
        }).then(response => {
          this.$pollJob({
            jobId: response.createvpncustomergatewayresponse.jobid,
            title: this.$t('message.add.vpn.customer.gateway'),
            description: values.name,
            successMessage: this.$t('message.success.add.vpn.customer.gateway'),
            successMethod: () => {
              this.closeModal()
              this.isSubmitted = false
            },
            errorMessage: `${this.$t('message.create.vpn.customer.gateway.failed')} ` + response,
            errorMethod: () => {
              this.closeModal()
              this.isSubmitted = false
            },
            loadingMessage: this.$t('message.add.vpn.customer.gateway.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.closeModal()
              this.isSubmitted = false
            }
          })
          this.closeModal()
          this.formRef.value.resetFields()
        }).catch(error => {
          console.error(error)
          this.$message.error(this.$t('message.add.vpn.customer.gateway.failed'))
          this.isSubmitted = false
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
