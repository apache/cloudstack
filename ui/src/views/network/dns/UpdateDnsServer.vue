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
      url: [{ required: true, message: this.$t('message.error.required.input') }]
    }
    this.form.name = this.resource.name || ''
    this.form.url = this.resource.url || ''
    this.form.port = this.resource.port || 53
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
        await postAPI('updateDnsServer', { id: this.resource.id, ...this.form })

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
