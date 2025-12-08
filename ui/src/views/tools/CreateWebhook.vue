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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item name="scope" ref="scope" v-if="isAdminOrDomainAdmin">
          <template #label>
            <tooltip-label :title="$t('label.scope')" :tooltip="apiParams.scope.description"/>
          </template>
          <a-radio-group
            v-model:value="form.scope"
            buttonStyle="solid"
            @change="handleScopeChange">
            <a-radio-button value="Local">
              {{ $t('label.local') }}
            </a-radio-button>
            <a-radio-button value="Domain">
              {{ $t('label.domain') }}
            </a-radio-button>
            <a-radio-button value="Global" v-if="isAdmin">
              {{ $t('label.global') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="isAdminOrDomainAdmin && ['Domain', 'Local'].includes(form.scope)">
          <template #label :title="apiParams.domainid.description">
            {{ $t('label.domainid') }}
            <a-tooltip>
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="domain-selection"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description"
            @change="val => { handleDomainChanged(val) }">
            <a-select-option v-for="opt in domains" :key="opt.id" :label="opt.path || opt.name || opt.description || ''">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="account" ref="account" v-if="isAdminOrDomainAdmin && ['Local'].includes(form.scope) && form.domainid">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          </template>
          <a-select
            v-model:value="form.account"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="accountLoading"
            :placeholder="apiParams.account.description">
            <a-select-option v-for="opt in accounts" :key="opt.id" :label="opt.name">
              {{ opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="payloadurl" ref="payloadurl">
          <template #label>
            <tooltip-label :title="$t('label.payloadurl')" :tooltip="apiParams.payloadurl.description"/>
          </template>
          <a-input
            v-model:value="form.payloadurl"
            :placeholder="apiParams.payloadurl.description"
            @change="handleParamUpdate"/>
        </a-form-item>
        <a-form-item name="sslverification" ref="sslverification" v-if="isPayloadUrlHttps">
          <template #label>
            <tooltip-label :title="$t('label.sslverification')" :tooltip="apiParams.sslverification.description"/>
          </template>
          <a-alert
            v-if="!form.sslverification"
            class="ssl-alert"
            type="warning"
            :message="$t('message.disable.webhook.ssl.verification')" />
          <a-switch
            v-model:checked="form.sslverification"
            @change="handleParamUpdate" />
        </a-form-item>
        <a-form-item name="secretkey" ref="secretkey">
          <template #label>
            <tooltip-label :title="$t('label.secretkey')" :tooltip="apiParams.secretkey.description"/>
          </template>
          <a-input
            v-model:value="form.secretkey"
            :placeholder="apiParams.secretkey.description"
            @change="handleParamUpdate"/>
        </a-form-item>
        <test-webhook-delivery-view
          ref="dispatchview"
          :payloadUrl="form.payloadurl"
          :sslVerification="form.sslverification"
          :secretKey="form.secretkey"
          :showActions="!(!form.payloadurl)" />
        <a-form-item name="state" ref="state">
          <template #label>
            <tooltip-label :title="$t('label.enabled')" :tooltip="apiParams.state.description"/>
          </template>
          <a-switch v-model:checked="form.state" />
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TestWebhookDeliveryView from '@/components/view/TestWebhookDeliveryView'

export default {
  name: 'CreateWebhook',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    TestWebhookDeliveryView
  },
  props: {},
  data () {
    return {
      domains: [],
      domainLoading: false,
      accounts: [],
      accountLoading: false,
      loading: false,
      testDeliveryAllowed: false,
      testDeliveryLoading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createWebhook')
  },
  created () {
    this.initForm()
    if (['Domain', 'Local'].includes(this.form.scope)) {
      this.fetchDomainData()
    }
  },
  computed: {
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    isPayloadUrlHttps () {
      if (this.form.payloadurl) {
        return this.form.payloadurl.toLowerCase().startsWith('https://')
      }
      return false
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        scope: 'Local',
        state: true,
        sslverification: true
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.create.webhook.name') }],
        payloadurl: [{ required: true, message: this.$t('message.error.create.webhook.payloadurl') }]
      })
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    updateTestDeliveryLoading (value) {
      this.testDeliveryLoading = value
    },
    fetchDomainData () {
      this.domainLoading = true
      this.domains = [
        {
          id: null,
          name: ''
        }
      ]
      this.form.domainid = null
      this.form.account = null
      api('listDomains', {}).then(json => {
        const listdomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listdomains)
      }).finally(() => {
        this.domainLoading = false
        if (this.arrayHasItems(this.domains)) {
          this.form.domainid = null
        }
      })
    },
    fetchAccountData () {
      this.accounts = []
      this.form.account = null
      if (!this.form.domainid) {
        return
      }
      this.accountLoading = true
      var params = {
        domainid: this.form.domainid
      }
      api('listAccounts', params).then(json => {
        const listAccounts = json.listaccountsresponse.account || []
        this.accounts = listAccounts
      }).finally(() => {
        this.accountLoading = false
        if (this.arrayHasItems(this.accounts)) {
          this.form.account = this.accounts[0].id
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {
          name: values.name,
          description: values.description,
          payloadurl: values.payloadurl,
          state: values.state ? 'Enabled' : 'Disabled'
        }
        if (this.isValidValueForKey(values, 'scope')) {
          params.scope = values.scope
        }
        if (this.isValidValueForKey(values, 'sslverification')) {
          params.sslverification = values.sslverification
        }
        if (this.isValidValueForKey(values, 'secretkey')) {
          params.secretkey = values.secretkey
        }
        if (values.domainid) {
          params.domainid = values.domainid
        }
        if (values.scope === 'Local' && values.domainid && !values.account) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.create.webhook.local.account')
          })
          return
        }
        if (values.account) {
          const accountItem = _.find(this.accounts, (option) => option.id === values.account)
          if (accountItem) {
            params.account = accountItem.name
          }
        }
        this.loading = true
        api('createWebhook', params).then(json => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.create.webhook'),
            description: `${this.$t('message.success.create.webhook')} ${params.name}`
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleParamUpdate () {
      setTimeout(() => {
        this.$refs.dispatchview.timedTestWebhookDelivery()
      }, 1)
    },
    handleScopeChange (e) {
      if (['Domain', 'Local'].includes(this.form.scope)) {
        this.fetchDomainData()
      }
    },
    handleDomainChanged (domainid) {
      if (domainid) {
        this.fetchAccountData()
      }
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 650px;
    }
  }

  .ssl-alert {
    margin: 0 0 10px 0;
  }
</style>
