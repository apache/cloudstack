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
    <a-form class="form-layout" :form="form" layout="vertical">
      <a-form-item :label="$t('label.name')">
        <a-input
          v-decorator="[
            'name',
            {
              rules: [{ required: true, message: `${$t('label.required')}` }],
            }
          ]"
          :placeholder="$t('label.vpncustomergatewayname')" />
      </a-form-item>
      <a-form-item :label="$t('label.gateway')">
        <a-input
          v-decorator="[
            'gateway',
            {
              rules: [{ required: true, message: `${$t('label.required')}`}],
            }
          ]"
          :placeholder="$t('label.vpncustomergateway')" />
      </a-form-item>
      <a-form-item :label="$t('label.cidrlist')">
        <a-input
          v-decorator="[
            'cidrlist',
            {
              rules: [{ required: true, message: `${$t('label.required')}`}],
            }
          ]"
          :placeholder="$t('label.vpncustomergateway.cidrlist')" />
      </a-form-item>
      <a-form-item
        :label="$t('label.ipsecpsk')">
        <a-input
          v-decorator="[
            'ipsecpsk',
            {
              rules: [{ required: true, message: `${$t('label.required')}`}],
            }
          ]"
          :placeholder="$t('label.vpncustomergateway.secretkey')" />
      </a-form-item>
      <a-form-item
        :label="$t('label.ikeencryption')">
        <a-select
          v-decorator="[
            'ikeEncryption',
            {
              initialValue: 'aes128',
            },
          ]">
          <a-select-option :value="algo" v-for="(algo, idx) in encryptionAlgo" :key="idx">
            {{ algo }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.ikehash')">
        <a-select
          v-decorator="[
            'ikeHash',
            {
              initialValue: 'sha1',
            },
          ]">
          <a-select-option :value="h" v-for="(h, idx) in hash" :key="idx">
            {{ h }}
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
          ]">
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
          ]">
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
          ]">
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
          ]">
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
      <a-form-item
        :label="$t('label.ikelifetime')">
        <a-input
          v-decorator="[
            'ikelifetime',
            {
              initialValue: '86400',
            },
          ]"
          :placeholder="$t('label.vpncustomergateway.ikelifetime')"/>
      </a-form-item>
      <a-form-item
        :label="$t('label.esplifetime')">
        <a-input
          v-decorator="[
            'esplifetime',
            {
              initialValue: '3600',
            },
          ]"
          :placeholder="$t('label.vpncustomergateway.esplifetime')"/>
      </a-form-item>
      <a-form-item :label="$t('label.dpd')">
        <a-switch
          v-decorator="[
            'dpd',
            {
              initialValue: 'false',
            },
          ]"/>
      </a-form-item>
      <a-form-item :label="$t('label.forceencap')">
        <a-switch
          v-decorator="[
            'forceencap',
            {
              initialValue: 'false',
            },
          ]"/>
      </a-form-item>
      <div class="actions">
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
export default {
  name: 'CreateVpnCustomerGateway',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
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
      ikeDhGroupInitialValue: 'Group 5(modp1536)'
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    closeModal () {
      this.$parent.$parent.close()
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
          esppolicy: esppolicy
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('message.add.vpn.customer.gateway'),
            jobid: response.createvpncustomergatewayresponse.jobid,
            description: values.name,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.createvpncustomergatewayresponse.jobid,
            successMessage: this.$t('message.success.add.vpn.customer.gateway'),
            successMethod: () => {
              this.closeModal()
              this.parentFetchData()
            },
            errorMessage: `${this.$t('message.create.vpn.customer.gateway.failed')} ` + response,
            errorMethod: () => {
              this.closeModal()
              this.parentFetchData()
            },
            loadingMessage: this.$t('message.add.vpn.customer.gateway.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.closeModal()
            }
          })
          this.closeModal()
        }).catch(error => {
          console.error(error)
          this.$message.error(this.$t('message.success.add.vpn.customer.gateway'))
        }).finally(() => {
          this.form.resetFields()
          this.closeModal()
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

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
</style>
