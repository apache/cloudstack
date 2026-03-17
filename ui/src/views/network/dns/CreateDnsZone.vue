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
            :placeholder="apiParams.name?.description || 'e.g. example.com'"
            v-focus="true" />
        </a-form-item>

        <a-form-item name="dnsserverid" ref="dnsserverid">
          <template #label>
            <tooltip-label
              :title="$t('label.dns.server')"
              :tooltip="apiParams.dnsserverid?.description" />
          </template>
          <a-select
            v-model:value="form.dnsserverid"
            :placeholder="apiParams.dnsserverid?.description || 'Select DNS Server'"
            :loading="fetchingServers"
            showSearch
            optionFilterProp="label">
            <a-select-option
              v-for="server in dnsServers"
              :key="server.id"
              :value="server.id"
              :label="server.name">
              {{ server.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
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
  name: 'CreateDnsZone',
  components: {
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      apiParams: {},
      form: {
        name: '',
        dnsserverid: undefined,
        description: ''
      },
      rules: {},
      fetchingServers: false,
      dnsServers: []
    }
  },
  created () {
    this.apiParams = this.$getApiParams('createDnsZone') || {}
    this.rules = {
      name: [{ required: true, message: this.$t('message.error.required.input') }],
      dnsserverid: [{ required: true, message: this.$t('message.error.required.input') }]
    }
    this.fetchDnsServers()
  },
  methods: {
    async handleSubmit () {
      if (this.loading) return

      try {
        await this.$refs.formRef.validate()
      } catch (error) {
        const field = error?.errorFields?.[0]?.name
        if (field) {
          this.$refs.formRef.scrollToField(field)
        }
        return
      }

      this.loading = true

      try {
        const params = {
          name: this.form.name.trim(),
          dnsserverid: this.form.dnsserverid,
          description: this.form.description?.trim()
        }

        const response = await postAPI('createDnsZone', params)
        const jobId = response.creatednszoneresponse.jobid
        if (!jobId) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: 'Failed to get jobid for CreateDnsZone',
            duration: 0
          })
        }
        await this.$pollJob({
          jobId: jobId,
          title: this.$t('label.dns.create.zone'),
          description: this.$t('label.creating.dns.zone'),
          successMethod: () => {
            this.$notification.success({
              message: this.$t('label.dns.create.zone'),
              description: this.$t('message.success.create.dns.zone')
            })
          },
          loadingMessage: `${this.$t('label.dns.create.zone')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: { isFetchData: false }
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
    async fetchDnsServers () {
      this.fetchingServers = true
      try {
        const response = await getAPI('listDnsServers')
        const listResponse = response?.listdnsserversresponse || {}
        this.dnsServers = listResponse.dnsserver || []
        if (this.dnsServers.length > 0) {
          this.form.dnsserverid = this.dnsServers[0].id
        }
      } catch (error) {
        console.error('Failed to fetch DNS servers', error)
        this.$message.warning('Could not load DNS servers.')
      } finally {
        this.fetchingServers = false
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
