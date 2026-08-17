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
    :apiParams="apiParams"
    @submit="handleSubmit"
    @cancel="closeModal"
  />
</template>

<script>
import { postAPI } from '@/api'
import VpnCustomerGateway from './VpnCustomerGateway.vue'

export default {
  name: 'CreateVpnCustomerGateway',
  components: {
    VpnCustomerGateway
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVpnCustomerGateway')
  },
  methods: {
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit ({ payload }) {
      postAPI('createVpnCustomerGateway', payload).then(response => {
        this.$pollJob({
          jobId: response.createvpncustomergatewayresponse.jobid,
          title: this.$t('message.add.vpn.customer.gateway'),
          description: payload.name,
          successMessage: this.$t('message.success.add.vpn.customer.gateway'),
          successMethod: () => {
            this.closeModal()
          },
          errorMessage: this.$t('message.create.vpn.customer.gateway.failed'),
          errorMethod: () => {
            this.closeModal()
          },
          loadingMessage: this.$t('message.add.vpn.customer.gateway.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.closeModal()
          }
        })
        this.closeModal()
      }).catch(error => {
        this.$refs.vpnCustomerGatewayForm.resetSubmission()
        const errResponse = error.response?.data?.createvpncustomergatewayresponse
        const errText = errResponse?.errortext || this.$t('message.create.vpn.customer.gateway.failed')
        this.$message.error(errText)
      })
    }
  }
}
</script>
