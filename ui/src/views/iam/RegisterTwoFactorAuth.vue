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
  <div style="width:500px;height=500px">
  <h3> {{ $t('label.select.2fa.provider') }} </h3>
  <a-form
    :rules="rules"
    layout="vertical">
    <div class="form-layout" v-ctrl-enter="submitPin">
      <a-select
        v-model:value="selectedProvider"
        optionFilterProp="label"
        :filterOption="(input, option) => {
          return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }"
        style="width: 100%"
        @change="val => { handleSelectChange(val) }">
        <a-select-option
          v-for="(opt) in providers"
          :key="opt.name"
          :disabled="opt.enabled === false">
            {{ opt.name }}
        </a-select-option>
      </a-select>
    </div>
    <div v-if="show2FAdetails">
      <div v-if="selectedProvider === 'google'">
        <br />
        <div> {{ $t('message.two.fa.auth.register.account') }} </div>
        <vue-qrious
          class="center-align"
          :value="googleUrl"
          @change="onDataUrlChange"
        />
      </div>
      <div v-else-if="selectedProvider === 'staticpin'">
        <div> <a @click="setup2FAProvider"> {{ $t('message.two.fa.static.pin.part2') }}</a></div>
      </div>
      <div v-else-if="selectedProvider !== null && selectedProvider !== 'staticpin'">
        <div> {{ $t('message.two.fa.static.pin.part1') }} <a @click="setup2FAProvider"> {{ $t('message.two.fa.static.pin.part2') }}</a></div>
      </div>
      <div v-if="selectedProvider">
        <br />
        <h3> {{ $t('label.enter.code') }} </h3>
        <a-form @finish="submitPin" v-ctrl-enter="submitPin" class="container">
          <a-input v-model:value="code" />
          <div :span="24">
            <a-button ref="submit" type="primary" @click="submitPin">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </div>

      <a-modal
        v-if="showPin"
        :visible="showPin"
        :title="$t('label.two.factor.secret')"
        :closable="true"
        :footer="null"
        @cancel="onCloseModal"
        centered
        width="450px">
        <div> {{ pin }} </div>
      </a-modal>
    </div>
  </a-form>
  </div>
</template>
<script>

import { api } from '@/api'
import VueQrious from 'vue-qrious'
export default {
  name: 'RegisterTwoFactorAuth',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    VueQrious
  },
  data () {
    return {
      googleUrl: '',
      dataUrl: '',
      pin: '',
      code: '',
      showPin: false,
      show2FAdetails: false,
      providers: [],
      selectedProvider: null
    }
  },
  mounted () {
    this.list2FAProviders()
  },
  methods: {
    onDataUrlChange (dataUrl) {
      this.dataUrl = dataUrl
    },
    handleSelectChange (val) {
      this.selectedProvider = val
      this.setup2FAProvider()
    },
    setup2FAProvider () {
      api('setupUserTwoFactorAuthentication', { provider: this.selectedProvider }).then(response => {
        console.log(response)
        this.pin = response.setupusertwofactorauthenticationresponse.setup2fa.secretcode
        if (this.selectedProvider === 'google') {
          this.username = response.setupusertwofactorauthenticationresponse.setup2fa.username
          this.googleUrl = 'otpauth://totp/CloudStack:' + this.username + '?secret=' + this.pin + '&issuer=CloudStack'
        }
        this.showPin = true
        this.show2FAdetails = true
      }).catch(error => {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
        })
      })
    },
    list2FAProviders () {
      api('listUserTwoFactorAuthenticatorProviders', {}).then(response => {
        this.providers = response.listusertwofactorauthenticatorprovidersresponse.providers || []
      })
    },
    submitPin () {
      api('validateUserTwoFactorAuthenticationCode', { '2facode': this.code }).then(response => {
        this.$message.success({
          content: `${this.$t('label.action.enable.two.factor.authentication')}`,
          duration: 2
        })
        this.$emit('refresh-data')
      }).catch(error => {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
        })
      })
      this.closeAction()
    },
    closeAction () {
      this.$emit('close-action')
    },
    onCloseModal () {
      this.showPin = false
    }
  }
}
</script>

<style scoped>
  .center-align {
    display: block;
    margin-left: auto;
    margin-right: auto;
  }
  .container {
    display: flex;
  }
</style>
