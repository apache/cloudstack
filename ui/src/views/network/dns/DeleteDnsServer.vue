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
            <div v-html="$t('message.action.delete.dns.server')"></div>
          </template>
        </a-alert>
      </div>

      <div style="margin-bottom: 20px; word-break: break-all;">
        Please type <strong>{{ resource.name }}</strong> to confirm.
      </div>

      <div v-if="dnsZones && dnsZones.length > 0" style="margin-bottom: 20px;">
        <a-alert type="error">
          <template #message>
            <div>
              <strong>This action will impact {{ this.totalDnsZones }} DNS Zone(s):</strong>
              <ul style="margin-top: 10px; margin-bottom: 0; padding-left: 20px;">
                <li v-for="zone in dnsZones.slice(0, this.maxResults)" :key="zone.id">{{ zone.name }}</li>
              </ul>
              <div v-if="this.totalDnsZones > this.maxResults" style="margin-top: 5px; font-style: italic;">
                ...and {{ this.totalDnsZones - this.maxResults }} more
              </div>
            </div>
          </template>
        </a-alert>
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
import { getAPI, postAPI } from '@/api'

export default {
  name: 'DeleteDnsServer',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      dnsZones: [],
      form: {
        name: ''
      },
      rules: {
        name: [{ required: true, message: this.$t('message.error.required.input') }]
      },
      maxResults: 5,
      totalDnsZones: 0
    }
  },
  created () {
    this.fetchDnsZones()
  },
  methods: {
    async fetchDnsZones () {
      this.loading = true
      try {
        const response = await getAPI('listDnsZones', { dnsserverid: this.resource.id, page: 1, pagesize: this.maxResults, filter: 'name' })
        this.dnsZones = response?.listdnszonesresponse?.dnszone || []
        this.totalDnsZones = response?.listdnszonesresponse?.count || 0
      } catch (error) {
        console.error('Failed to fetch DNS zones', error)
      } finally {
        this.loading = false
      }
    },
    async handleSubmit () {
      if (this.loading || this.form.name !== this.resource.name) return

      this.loading = true

      try {
        const params = {
          id: this.resource.id
        }

        const response = await postAPI('deleteDnsServer', params)
        const jobId = response?.deletednsserverresponse?.jobid
        if (jobId) {
          this.$pollJob({
            jobId: jobId,
            title: this.$t('label.dns.delete.server'),
            description: this.resource.name,
            successMethod: () => {
              this.$notification.success({
                message: this.$t('label.dns.delete.server'),
                description: `Successfully deleted DNS server ${this.resource.name}`
              })
            },
            loadingMessage: `${this.$t('label.dns.delete.server')} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }

        if (this.$route.path !== '/dnsserver') {
          this.$router.push({ path: '/dnsserver' })
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
