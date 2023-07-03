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
    <p v-html="$t('message.two.fa.setup.page')"></p>
    <h3> {{ $t('label.select.2fa.provider') }} </h3>
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical">
      <a-row :gutter="12">
        <a-col :md="24" :lg="20">
          <a-form-item v-ctrl-enter="submitPin" ref="selectedProvider" name="selectedProvider">
             <a-select
              v-model:value="form.selectedProvider"
              @change="val => { handleSelectChange(val) }">
              <a-select-option
                v-for="(opt) in providers"
                :key="opt"
                :value="opt">
                <div>
                  <span v-if="opt === 'totp'">
                    <google-outlined />
                    Google Authenticator
                  </span>
                  <span v-if="opt === 'othertotp'">
                    <field-time-outlined />
                    Other TOTP Authenticators
                  </span>
                  <span v-if="opt === 'staticpin'">
                    <lock-outlined />
                    Static PIN
                  </span>
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>

        <a-col :md="24" :lg="4">
          <a-form-item>
            <div v-if="selectedProvider">
              <a-button ref="submit" type="primary" :disabled="twoFAenabled" @click="setup2FAProvider">{{ $t('label.setup') }}</a-button>
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.accept.project.invitation')"
                icon="check-outlined"
                size="small"
                @onClick="setup2FAProvider()"/>
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.decline.invitation')"
                type="primary"
                :danger="true"
                icon="close-outlined"
                size="small"
                @onClick="setup2FAProvider()"/>
            </div>
          </a-form-item>
        </a-col>
      </a-row>

      <div v-if="twoFAenabled">
        <div v-if="form.selectedProvider !== 'staticpin'">
          <br />
          <p v-html="$t('message.two.fa.register.account')"></p>
          <vue-qrious
            class="center-align"
            :value="totpUrl"
            size="200"
            @change="onDataUrlChange"
          />
          <div style="text-align: center"> <a @click="showConfiguredPin"> {{ $t('message.two.fa.view.setup.key') }}</a></div>
        </div>
        <div v-if="form.selectedProvider === 'staticpin'">
          <br>
          <p v-html="$t('message.two.fa.staticpin')"></p>
          <br>
          <div> <a @click="showConfiguredPin"> {{ $t('message.two.fa.view.static.pin') }}</a></div>
        </div>
        <div v-if="form.selectedProvider">
          <br />
          <h3> {{ $t('label.enter.code') }} </h3>
          <a-row :gutter="12">
            <a-col :md="24" :lg="20">
              <a-form-item @finish="submitPin" v-ctrl-enter="submitPin" name="code" ref="code">
                <a-input-password
                  v-model:value="form.code"
                  placeholder="xxxxxx" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="4">
              <a-form-item>
                  <a-button ref="submit" type="primary" :disabled="verifybuttonstate" @click="submitPin">{{ $t('label.verify') }}</a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <a-modal
          v-if="showPin"
          :visible="showPin"
          :title="$t(form.selectedProvider === 'staticpin'? 'label.two.factor.authentication.static.pin' : 'label.two.factor.authentication.secret.key')"
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
import { ref, reactive, toRaw } from 'vue'
import store from '@/store'
import VueQrious from 'vue-qrious'
import eventBus from '@/config/eventBus'
export default {
  name: 'SetupTwoFaAtUserProfile',
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
      totpUrl: '',
      dataUrl: '',
      pin: '',
      showPin: false,
      twoFAenabled: false,
      twoFAverified: false,
      providers: [],
      selectedProvider: null,
      verifybuttonstate: false
    }
  },
  mounted () {
    this.list2FAProviders()
  },
  created () {
    this.initForm()
    eventBus.on('action-closing', (args) => {
      if (args.action.api === 'setupUserTwoFactorAuthentication' && this.twoFAenabled && !this.twoFAverified) {
        this.disable2FAProvider()
      }
    })
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        code: [{ required: true, message: this.$t('message.error.authentication.code') }]
      })
    },
    onDataUrlChange (dataUrl) {
      this.dataUrl = dataUrl
    },
    handleSelectChange (val) {
      if (this.twoFAenabled) {
        api('setupUserTwoFactorAuthentication', { enable: 'false' }).then(response => {
          this.pin = ''
          this.username = ''
          this.totpUrl = ''
          this.dataUrl = ''
          this.showPin = false
          this.twoFAenabled = false
          this.twoFAverified = false
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        })
      }
      this.selectedProvider = val
      this.twoFAenabled = false
    },
    setup2FAProvider () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.selectedProvider = values.selectedProvider
        var provider
        if (this.selectedProvider === 'othertotp') {
          provider = 'totp'
        } else {
          provider = this.selectedProvider
        }
        api('setupUserTwoFactorAuthentication', { provider: provider }).then(response => {
          this.pin = response.setupusertwofactorauthenticationresponse.setup2fa.secretcode
          if (this.selectedProvider === 'totp' || this.selectedProvider === 'othertotp') {
            this.username = response.setupusertwofactorauthenticationresponse.setup2fa.username

            var issuer = 'CloudStack'
            if (store.getters.twoFaIssuer !== '' && store.getters.twoFaIssuer !== undefined) {
              issuer = store.getters.twoFaIssuer
            }
            this.totpUrl = 'otpauth://totp/' + issuer + ':' + this.username + '?secret=' + this.pin + '&issuer=' + issuer

            this.showPin = false
          }
          if (this.selectedProvider === 'staticpin') {
            this.showPin = true
          }
          this.twoFAenabled = true
          this.twoFAverified = false
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        })
      })
    },
    disable2FAProvider () {
      api('setupUserTwoFactorAuthentication', { enable: false }).then(response => {
        this.showPin = false
        this.twoFAenabled = false
        this.twoFAverified = false
      }).catch(error => {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
        })
      })
    },
    list2FAProviders () {
      api('listUserTwoFactorAuthenticatorProviders', {}).then(response => {
        var providerlist = response.listusertwofactorauthenticatorprovidersresponse.providers || []
        var providernames = []
        for (const provider of providerlist) {
          providernames.push(provider.name)
          if (provider.name === 'totp') {
            providernames.push('othertotp')
          }
        }
        this.providers = providernames
      })
    },
    submitPin () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        if (values.code !== null) {
          this.verifybuttonstate = true
        }
        api('validateUserTwoFactorAuthenticationCode', { codefor2fa: values.code }).then(response => {
          this.$message.success({
            content: `${this.$t('label.action.enable.two.factor.authentication')}`,
            duration: 2
          })
          this.twoFAverified = true
          this.$emit('refresh-data')
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        })
        this.closeAction()
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    showConfiguredPin () {
      this.showPin = true
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
  .form-align {
    display: flex;
    flex-direction: row;
  }
  .container {
    display: flex;
  }
</style>
