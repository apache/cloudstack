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
    <a-form class="form-layout" :form="form" layout="vertical" v-ctrl-enter="handleSubmit">
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        <a-input
          v-decorator="[
            'name',
            {
              rules: [{ required: true, message: `${$t('label.required')}` }],
            }
          ]"
          :placeholder="$t('label.vpncustomergatewayname')"
          autoFocus />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.gateway')" :tooltip="apiParams.gateway.description"/>
        <a-input
          v-decorator="[
            'gateway',
            {
              rules: [{ required: true, message: `${$t('label.required')}`}],
            }
          ]"
          :placeholder="$t('label.vpncustomergateway')" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.cidrlist')" :tooltip="apiParams.cidrlist.description"/>
        <a-input
          v-decorator="[
            'cidrlist',
            {
              rules: [{ required: true, message: `${$t('label.required')}`}],
            }
          ]"
          :placeholder="$t('label.vpncustomergateway.cidrlist')" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.ipsecpsk')" :tooltip="apiParams.ipsecpsk.description"/>
        <a-input
          v-decorator="[
            'ipsecpsk',
            {
              rules: [{ required: true, message: `${$t('label.required')}`}],
            }
          ]"
          :placeholder="$t('label.vpncustomergateway.secretkey')" />
      </a-form-item>
      <a-form-item :label="$t('label.ikeencryption')">
        <a-select
          v-decorator="[
            'ikeEncryption',
            {
              initialValue: 'aes128',
            },
          ]"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="algo" v-for="(algo, idx) in encryptionAlgo" :key="idx">
            {{ algo }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="$t('label.ikehash')">
        <a-select
          v-decorator="[
            'ikeHash',
            {
              initialValue: 'sha1',
            },
          ]"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="h" v-for="(h, idx) in hash" :key="idx">
            {{ h }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.ikeversion')" :tooltip="apiParams.ikeversion.description"/>
        <a-select
          v-decorator="[
            'ikeversion',
            {
              initialValue: 'ike',
            },
          ]"
          @change="val => { ikeversion = val }"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="vers" v-for="(vers, idx) in ikeVersions" :key="idx">
            {{ vers }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.ikedh')">
        <a-select
          v-decorator="[
            'ikeDh',
            {
              initialValue: 'Group 5(modp1536)',
            },
          ]"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="DHGroups[group]" v-for="(group, idx) in Object.keys(DHGroups)" :key="idx">
            <div v-if="group !== ''">
              {{ group+"("+DHGroups[group]+")" }}
            </div>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.espencryption')">
        <a-select
          v-decorator="[
            'espEncryption',
            {
              initialValue: 'aes128',
            },
          ]"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="algo" v-for="(algo, idx) in encryptionAlgo" :key="idx">
            {{ algo }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.esphash')">
        <a-select
          v-decorator="[
            'espHash',
            {
              initialValue: 'sha1',
            },
          ]"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="h" v-for="(h, idx) in hash" :key="idx">
            {{ h }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.perfectforwardsecrecy')">
        <a-select
          v-decorator="[
            'perfectForwardSecrecy',
            {
              initialValue: 'None',
            },
          ]"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option :value="DHGroups[group]" v-for="(group, idx) in Object.keys(DHGroups)" :key="idx">
            <div v-if="group === ''">
              {{ DHGroups[group] }}
            </div>
            <div v-else>
              {{ group+"("+DHGroups[group]+")" }}
            </div>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.ikelifetime')" :tooltip="apiParams.ikelifetime.description"/>
        <a-input
          v-decorator="[
            'ikelifetime',
            {
              initialValue: '86400',
            },
          ]"
          :placeholder="$t('label.vpncustomergateway.ikelifetime')"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.esplifetime')" :tooltip="apiParams.esplifetime.description"/>
        <a-input
          v-decorator="[
            'esplifetime',
            {
              initialValue: '3600',
            },
          ]"
          :placeholder="$t('label.vpncustomergateway.esplifetime')"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.dpd')" :tooltip="apiParams.dpd.description"/>
        <a-switch
          v-decorator="[
            'dpd',
            {
              initialValue: 'false',
            },
          ]"/>
      </a-form-item>
      <a-form-item v-if="ikeversion !== 'ikev1'">
        <tooltip-label slot="label" :title="$t('label.splitconnections')" :tooltip="apiParams.splitconnections.description"/>
        <a-switch
          v-decorator="[
            'splitconnections',
            {
              initialValue: 'false',
            },
          ]"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.forceencap')" :tooltip="apiParams.forceencap.description"/>
        <a-switch
          v-decorator="[
            'forceencap',
            {
              initialValue: 'false',
            },
          ]"/>
      </a-form-item>
      <div class="action-button">
        <a-button @click="closeModal">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button type="primary" @click="handleSubmit">
          {{ $t('label.ok') }}
        </a-button>
      </div>
    </a-form>
  </div>
</template>
<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVpnCustomerGateway',
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
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createVpnCustomerGateway')
  },
  methods: {
    closeModal () {
      this.$parent.$parent.close()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.isSubmitted) return
      const options = {
        scroll: {
          offsetTop: 10
        }
      }
      this.form.validateFieldsAndScroll(options, (err, values) => {
        if (err) {
          return
        }
        this.isSubmitted = true
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
          this.form.resetFields()
        }).catch(error => {
          console.error(error)
          this.$message.error(this.$t('message.success.add.vpn.customer.gateway'))
          this.isSubmitted = false
        })
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
