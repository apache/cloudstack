// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
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

        <a-form-item name="provider" ref="provider">
          <template #label>
            <tooltip-label
              :title="$t('label.provider')"
              :tooltip="apiParams.provider?.description" />
          </template>
          <a-select
            v-model:value="form.provider"
            :placeholder="apiParams.provider?.description || 'Select Provider'"
            :loading="fetchingProviders"
            showSearch>
            <a-select-option
              v-for="provider in providers"
              :key="provider.name || provider"
              :value="provider.name || provider">
              {{ provider.description || provider.name || provider }}
            </a-select-option>
          </a-select>
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

        <a-form-item name="credentials" ref="credentials">
          <template #label>
            <tooltip-label
              :title="$t('label.dns.credentials')"
              :tooltip="apiParams.credentials?.description" />
          </template>
          <a-input-password
            v-model:value="form.credentials"
            :placeholder="apiParams.credentials?.description || 'Enter API Key'" />
        </a-form-item>

        <a-form-item name="nameservers" ref="nameservers">
          <template #label>
            <tooltip-label
              :title="$t('label.nameservers')"
              :tooltip="apiParams.nameservers?.description" />
          </template>
          <a-input
            v-model:value="form.nameservers"
            :placeholder="apiParams.nameservers?.description" />
        </a-form-item>

        <a-form-item name="ispublic" ref="ispublic">
          <a-checkbox v-model:checked="form.ispublic">
            {{  "Public server" }}
          </a-checkbox>
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
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddDnsServer',
  components: {
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      apiParams: {},
      form: {
        name: '',
        url: '',
        provider: '',
        port: 8081,
        nameservers: '',
        ispublic: false
      },
      rules: {},
      fetchingProviders: false,
      providers: []
    }
  },
  created () {
    this.apiParams = this.$getApiParams('addDnsServer') || {}
    this.rules = {
      name: [{ required: true, message: this.$t('message.error.required.input') }],
      url: [{ required: true, message: this.$t('message.error.required.input') }],
      provider: [{ required: true, message: this.$t('message.error.required.input') }],
      credentials: [{ required: true, message: this.$t('message.error.required.input') }]
    }
    this.fetchProviders()
  },
  methods: {
    async handleSubmit () {
      if (this.loading) return

      // 1. Validate the form natively using Vue 3 $refs
      try {
        await this.$refs.formRef.validate()
      } catch (error) {
        // Scroll to the first field with an error
        if (error.errorFields && error.errorFields.length > 0) {
          this.$refs.formRef.scrollToField(error.errorFields[0].name)
        }
        return // Abort submission
      }

      this.loading = true

      // 2. Execute the API
      try {
        console.log('form data ', this.form)
        // Pass the form object directly
        await postAPI('addDnsServer', this.form)

        this.$notification.success({
          message: this.$t('label.add.dns.server'),
          description: this.$t('message.success.add.dns.server')
        })

        // Refresh the parent table and close modal
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
    async fetchProviders () {
      this.fetchingProviders = true
      try {
        const response = await getAPI('listDnsProviders')
        const listResponse = response?.listdnsprovidersresponse || {}
        this.providers = listResponse.dnsprovider || listResponse.provider || []
        if (this.providers.length > 0) {
          const defaultProvider = this.providers[0]
          this.form.provider = defaultProvider.name || defaultProvider
        }
      } catch (error) {
        console.error('Failed to fetch DNS providers', error)
        this.$message.warning('Could not load DNS providers.')
      } finally {
        this.fetchingProviders = false
      }
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
