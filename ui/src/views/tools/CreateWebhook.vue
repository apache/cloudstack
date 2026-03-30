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
          <infinite-scroll-select
            id="domain-selection"
            v-model:value="form.domainid"
            api="listDomains"
            :apiParams="domainsApiParams"
            resourceType="domain"
            optionValueKey="id"
            optionLabelKey="path"
            defaultIcon="block-outlined"
            :defaultOption="{ id: null, path: ''}"
            allowClear="true"
            :placeholder="apiParams.domainid.description"
            @change-option-value="handleDomainChanged" />
        </a-form-item>
        <a-form-item name="account" ref="account" v-if="isAdminOrDomainAdmin && ['Local'].includes(form.scope) && form.domainid">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          </template>
          <infinite-scroll-select
            v-model:value="form.account"
            api="listAccounts"
            :apiParams="accountsApiParams"
            resourceType="account"
            optionValueKey="name"
            optionLabelKey="name"
            defaultIcon="team-outlined"
            :placeholder="apiParams.account.description" />
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
          :sslVerification="isPayloadUrlHttps && form.sslverification"
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
import { postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TestWebhookDeliveryView from '@/components/view/TestWebhookDeliveryView'
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect.vue'

export default {
  name: 'CreateWebhook',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    TestWebhookDeliveryView,
    InfiniteScrollSelect
  },
  props: {},
  data () {
    return {
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
    },
    domainsApiParams () {
      return {
        listAll: true,
        showicon: true,
        details: 'min'
      }
    },
    accountsApiParams () {
      if (!this.form.domainid) {
        return null
      }
      return {
        domainid: this.form.domainid
      }
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
          // values.account is the account name (optionValueKey="name")
          params.account = values.account
        }
        this.loading = true
        postAPI('createWebhook', params).then(json => {
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
      this.form.domainid = null
      this.form.account = null
    },
    handleDomainChanged (domainid) {
      this.form.account = null
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
