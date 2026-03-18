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
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical">

        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label
              :title="$t('label.name')"
              :tooltip="apiParams.name?.description" />
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name?.description"
            v-focus="true" />
        </a-form-item>

        <a-form-item name="url" ref="url">
          <template #label>
            <tooltip-label
              :title="$t('label.url')"
              :tooltip="apiParams.url?.description" />
          </template>
          <a-input
            v-model:value="form.url"
            :placeholder="apiParams.url?.description" />
        </a-form-item>

        <a-form-item name="port" ref="port">
          <template #label>
            <tooltip-label
              :title="$t('label.port')"
              :tooltip="apiParams.port?.description" />
          </template>
          <a-input-number
            v-model:value="form.port"
            :min="1"
            :max="65535"
            style="width: 100%" />
        </a-form-item>

        <a-form-item name="credentials">
          <template #label>
            <tooltip-label :title="$t('label.dns.credentials')" :tooltip="apiParams.credentials?.description" />
          </template>
          <a-input-password v-model:value="form.credentials" :placeholder="apiParams.credentials?.description" />
        </a-form-item>

        <a-form-item v-if="isAdminOrDomainAdmin()" name="publicdomainsuffix">
          <template #label>
            <tooltip-label :title="$t('label.dns.publicdomainsuffix')" :tooltip="apiParams.publicdomainsuffix?.description" />
          </template>
          <a-input v-model:value="form.publicdomainsuffix" :placeholder="apiParams.publicdomainsuffix?.description" />
        </a-form-item>

        <a-form-item name="nameservers" ref="nameservers">
          <template #label>
            <tooltip-label
              :title="$t('label.nameservers')"
              :tooltip="apiParams.nameservers?.description" />
          </template>

          <a-select
            v-model:value="form.nameservers"
            mode="tags"
            style="width: 100%"
            :token-separators="[',', ' ']"
            :placeholder="apiParams.nameservers?.description" />
        </a-form-item>

        <a-form-item v-if="isAdminOrDomainAdmin()" name="ispublic">
          <template #label>
            <tooltip-label :title="$t('label.ispublic')" :tooltip="apiParams.ispublic?.description" />
          </template>
          <a-switch v-model:checked="form.ispublic" />
        </a-form-item>

        <div class="action-button">
          <a-button @click="closeAction">
            {{ $t('label.cancel') }}
          </a-button>
          <a-button
            type="primary"
            :loading="loading"
            @click="handleSubmit">
            {{ $t('label.ok') }}
          </a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'UpdateDnsServer',
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
      loading: false,
      apiParams: {},
      form: {
        name: '',
        url: '',
        port: 53
      },
      rules: {}
    }
  },
  created () {
    this.apiParams = this.$getApiParams('updateDnsServer') || {}
    this.rules = {
      name: [{ required: true, message: this.$t('message.error.required.input') }],
      url: [
        { required: true, message: this.$t('message.error.required.input') },
        { validator: this.validateUrl }
      ],
      port: [
        { required: true, message: this.$t('message.error.required.input') },
        { validator: this.validatePort }
      ],
      publicdomainsuffix: [{ validator: this.validatePublicDomainSuffix }],
      state: [{ required: true, message: this.$t('message.error.required.input') }]
    }
    this.form.name = this.resource.name
    this.form.url = this.resource.url
    this.form.port = this.resource.port
    this.form.publicdomainsuffix = this.resource.publicdomainsuffix
    this.form.nameservers = this.resource.nameservers || []
    this.form.ispublic = this.resource.ispublic
  },
  methods: {
    async handleSubmit () {
      if (this.loading) return

      try {
        await this.$refs.formRef.validate()
      } catch (error) {
        if (error.errorFields && error.errorFields.length > 0) {
          this.$refs.formRef.scrollToField(error.errorFields[0].name)
        }
        return
      }

      this.loading = true

      try {
        const params = {
          id: this.resource.id,
          name: this.form.name.trim(),
          url: this.form.url?.trim().replace(/\/$/, ''),
          port: this.form.port,
          nameservers: this.form.nameservers?.map(ns => ns.toLowerCase().trim()).filter(Boolean),
          ispublic: this.form.ispublic,
          state: this.form.state
        }
        if (this.form.credentials) {
          params.credentials = this.form.credentials
        }
        if (this.form.ispublic) {
          params.publicdomainsuffix = this.form.publicdomainsuffix?.trim().toLowerCase()
        }
        await postAPI('updateDnsServer', params)
        this.$notification.success({
          message: this.$t('label.dns.update.server'),
          description: this.$t('message.success.update.dns.server')
        })

        this.$emit('refresh-data')
        this.closeAction()
      } catch (error) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: error?.response?.headers['x-description'] || error.message,
          duration: 0
        })
      } finally {
        this.loading = false
      }
    },
    closeAction () {
      this.$emit('close-action')
    },
    validateUrl (rule, value) {
      if (!value) return Promise.resolve()
      try {
        const parsed = new URL(value)
        if (!['http:', 'https:'].includes(parsed.protocol)) {
          return Promise.reject(new Error('URL must start with http:// or https://'))
        }
        if (parsed.port) {
          return Promise.reject(new Error('Do not include port in URL. Use the port field instead.'))
        }
      } catch (e) {
        return Promise.reject(new Error('Invalid URL format'))
      }
      return Promise.resolve()
    },
    validatePort (rule, value) {
      if (value === undefined || value === null) {
        return Promise.resolve()
      }

      if (value < 1 || value > 65535) {
        return Promise.reject(new Error('Port must be between 1 and 65535'))
      }

      return Promise.resolve()
    },
    validateNameservers (rule, value) {
      if (!value || !value.length) {
        return Promise.resolve()
      }

      const fqdnRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z]{2,})+$/

      for (const ns of value) {
        if (!fqdnRegex.test(ns)) {
          return Promise.reject(new Error('Invalid nameserver'))
        }
      }

      if (new Set(value).size !== value.length) {
        return Promise.reject(new Error('Nameservers must be unique'))
      }

      return Promise.resolve()
    },
    validatePublicDomainSuffix (rule, value) {
      if (!this.form.ispublic) {
        return Promise.resolve()
      }

      if (!value) {
        return Promise.reject(new Error(this.$t('message.error.required.publicdomainsuffix')))
      }

      const fqdnRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z]{2,})+$/

      if (!fqdnRegex.test(value)) {
        return Promise.reject(new Error('Invalid domain suffix'))
      }

      return Promise.resolve()
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    }
  }
}
</script>

<style lang="less" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 600px) {
    width: 450px;
  }
}

.action-button {
  text-align: right;
  margin-top: 20px;
}
.action-button button {
  margin-left: 8px;
}
</style>
