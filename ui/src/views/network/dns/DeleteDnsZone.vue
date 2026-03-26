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
        layout="vertical">

        <a-form-item name="name" ref="name">
          <a-input
            v-model:value="form.name"
            :placeholder="resource.name"
            v-focus="true" />
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
            @click="handleSubmit">
            {{ $t('label.delete') }}
          </a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { postAPI } from '@/api'

export default {
  name: 'DeleteDnsZone',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      form: {
        name: ''
      },
      rules: {
        name: [{ required: true, message: this.$t('message.error.required.input') }]
      }
    }
  },
  methods: {
    async handleSubmit () {
      if (this.loading || this.form.name !== this.resource.name) return

      this.loading = true

      try {
        const params = {
          id: this.resource.id
        }

        const response = await postAPI('deleteDnsZone', params)
        const jobId = response?.deletednszoneresponse?.jobid
        const isDetailView = this.$route.path !== '/dnszone'
        if (jobId) {
          this.$pollJob({
            jobId: jobId,
            title: this.$t('label.dns.delete.zone'),
            description: this.resource.name,
            successMethod: () => {
              this.$notification.success({
                message: this.$t('label.dns.delete.zone'),
                description: `Successfully deleted DNS zone ${this.resource.name}`
              })
            },
            loadingMessage: `${this.$t('label.dns.delete.zone')} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }
        if (isDetailView) {
          this.$router.push({ path: '/dnszone' })
        } else {
          this.$emit('refresh-data')
        }
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
