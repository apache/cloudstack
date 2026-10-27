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
  <vpn-customer-gateway
    ref="vpnCustomerGatewayForm"
    :initial-values="initialValues"
    :apiParams="apiParams"
    @submit="handleSubmit"
    @cancel="closeModal"
  />
</template>

<script>
import { postAPI } from '@/api'
import VpnCustomerGateway from './VpnCustomerGateway.vue'

export default {
  name: 'UpdateVpnCustomerGateway',
  components: {
    VpnCustomerGateway
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateVpnCustomerGateway')
  },
  computed: {
    initialValues () {
      const ikepolicy = this.parseIkePolicy(this.resource.ikepolicy)
      const esppolicy = this.parseEspPolicy(this.resource.esppolicy)
      return {
        id: this.resource.id,
        name: this.resource.name,
        domainid: this.resource.domainid,
        gateway: this.resource.gateway,
        cidrlist: this.resource.cidrlist,
        ipsecpsk: this.resource.ipsecpsk,
        ikeEncryption: ikepolicy.encryption,
        ikeHash: ikepolicy.hash,
        ikeDh: ikepolicy.dh,
        espEncryption: esppolicy.encryption,
        espHash: esppolicy.hash,
        espDh: esppolicy.dh,
        ikelifetime: this.resource.ikelifetime,
        esplifetime: this.resource.esplifetime,
        dpd: this.resource.dpd,
        splitconnections: this.resource.splitconnections,
        forceencap: this.resource.forceencap,
        ikeversion: this.resource.ikeversion
      }
    }
  },
  methods: {
    closeModal () {
      this.$emit('close-action')
    },
    parseIkePolicy (ikePolicy) {
      console.log('ikePolicy', ikePolicy)
      if (!ikePolicy) return { encryption: null, hash: null, dh: null }
      const parts = ikePolicy.split(';')
      const cipherHash = parts[0] || ''
      const dhGroup = parts[1] || null
      const cipherHashParts = cipherHash.split('-')
      const encryption = cipherHashParts[0] || null
      const hash = cipherHashParts[1] || null
      return { encryption, hash, dh: dhGroup }
    },
    parseEspPolicy (espPolicy) {
      if (!espPolicy) return { encryption: null, hash: null, dh: null }
      const parts = espPolicy.split(';')
      const cipherHash = parts[0] || ''
      const dhGroup = parts[1] || null
      const cipherHashParts = cipherHash.split('-')
      const encryption = cipherHashParts[0] || null
      const hash = cipherHashParts[1] || null
      return { encryption, hash, dh: dhGroup }
    },
    handleSubmit ({ payload }) {
      postAPI('updateVpnCustomerGateway', {
        id: this.resource.id,
        ...payload
      }).then(response => {
        this.$pollJob({
          jobId: response.updatevpncustomergatewayresponse.jobid,
          title: this.$t('message.update.vpn.customer.gateway'),
          description: payload.name,
          successMessage: this.$t('message.success.update.vpn.customer.gateway'),
          successMethod: () => {
            this.closeModal()
          },
          errorMessage: this.$t('message.update.vpn.customer.gateway.failed'),
          errorMethod: () => {
            this.closeModal()
          },
          loadingMessage: this.$t('message.update.vpn.customer.gateway.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.closeModal()
          }
        })
        this.closeModal()
      }).catch(error => {
        this.$refs.vpnCustomerGatewayForm.resetSubmission()
        const errResponse = error.response?.data?.createvpncustomergatewayresponse
        const errText = errResponse?.errortext || this.$t('message.update.vpn.customer.gateway.failed')
        this.$message.error(errText)
      })
    }
  }
}
</script>
