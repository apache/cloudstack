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
      <div style="margin-bottom: 20px;">
        <a-alert type="warning">
          <template #message>
            <div v-html="$t('message.action.delete.dns.zone')"></div>
          </template>
        </a-alert>
      </div>

      <div style="margin-bottom: 20px; word-break: break-all;">
        Please type <strong>{{ resource.name }}</strong> to confirm.
      </div>

      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">

        <a-form-item name="name" ref="name">
          <a-input
            v-model:value="form.name"
            :placeholder="resource.name"
            v-focus="true" />
        </a-form-item>

        <a-form-item name="unmanage" ref="unmanage">
          <template #label>
            <tooltip-label :title="$t('label.dns.unmanage.zone')" :tooltip="apiParams.unmanage?.description" />
          </template>
          <a-switch v-model:checked="form.unmanage" />
        </a-form-item>

        <div class="action-button">
          <a-button @click="closeAction">
            {{ $t('label.cancel') }}
          </a-button>
          <a-button
            type="primary"
            danger
            :disabled="form.name !== resource.name"
            :loading="loading"
            htmlType="submit">
            {{ $t('label.delete') }}
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
  name: 'DeleteDnsZone',
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
        unmanage: false
      },
      rules: {
        name: [{ required: true, message: this.$t('message.error.required.input') }]
      }
    }
  },
  created () {
    this.apiParams = this.$getApiParams('deleteDnsZone') || {}
  },
  methods: {
    async handleSubmit () {
      if (this.loading || this.form.name !== this.resource.name) return

      this.loading = true

      try {
        const params = {
          id: this.resource.id,
          unmanage: this.form.unmanage
        }

        const response = await postAPI('deleteDnsZone', params)
        const jobId = response?.deletednszoneresponse?.jobid
        if (!jobId) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: 'Failed to get jobid for DeleteDnsZone',
            duration: 0
          })
          this.loading = false
          return
        }
        const onListPage = this.$route.path === '/dnszone'
        this.$pollJob({
          jobId: jobId,
          title: this.$t('label.dns.delete.zone'),
          description: this.resource.name,
          successMethod: () => {
            this.loading = false
            this.$notification.success({
              message: this.$t('label.dns.delete.zone'),
              description: `${this.$t('message.success.delete.dns.zone')} ${this.resource.name}`
            })
            if (!onListPage) {
              this.$router.push({ path: '/dnszone' })
            }
          },
          errorMethod: () => {
            this.loading = false
            this.$notification.error({
              message: this.$t('label.dns.delete.zone'),
              description: `${this.$t('message.error.delete.dns.zone')} ${this.resource.name}`
            })
          },
          loadingMessage: `${this.$t('label.dns.delete.zone')} ${this.resource.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: { isFetchData: onListPage }
        })
        this.closeAction()
      } catch (error) {
        this.loading = false
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: error?.response?.headers['x-description'] || error.message,
          duration: 0
        })
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
