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
        layout="vertical"
        @finish="handleSubmit">

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

        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
        </a-form-item>

        <a-form-item v-if="isAdmin" name="domainid" ref="domainid">
          <template #label>
            <tooltip-label :title="$t('label.domain')" :tooltip="apiParams.domainid?.description" />
          </template>
          <a-select
            v-model:value="form.domainid"
            :placeholder="apiParams.domainid?.description"
            :loading="fetchingDomains"
            allowClear
            showSearch
            optionFilterProp="label"
            @change="handleDomainChange">
            <a-select-option
              v-for="domain in domains"
              :key="domain.id"
              :value="domain.id"
              :label="domain.path || domain.name">
              {{ domain.path || domain.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item v-if="isAdmin" name="account" ref="account">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account?.description" />
          </template>
          <a-select
            v-model:value="form.account"
            :placeholder="apiParams.account?.description"
            :loading="fetchingAccounts"
            :disabled="!form.domainid"
            allowClear
            showSearch
            optionFilterProp="label">
            <a-select-option
              v-for="account in accounts"
              :key="account.id"
              :value="account.name"
              :label="account.name">
              {{ account.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="existing" ref="existing">
          <template #label>
            <tooltip-label :title="$t('label.existing')" :tooltip="apiParams.existing?.description" />
          </template>
          <a-switch v-model:checked="form.existing" />
        </a-form-item>

        <div class="action-button">
          <a-button @click="closeAction">
            {{ $t('label.cancel') }}
          </a-button>
          <a-button
            type="primary"
            :loading="loading"
            htmlType="submit">
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
import { isAdmin } from '@/role'

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
        description: '',
        existing: false,
        domainid: undefined,
        account: undefined
      },
      rules: {},
      fetchingServers: false,
      dnsServers: [],
      fetchingDomains: false,
      domains: [],
      fetchingAccounts: false,
      accounts: []
    }
  },
  computed: {
    isAdmin () {
      return isAdmin()
    }
  },
  created () {
    this.apiParams = this.$getApiParams('createDnsZone') || {}
    this.rules = {
      name: [{ required: true, message: this.$t('message.error.required.input') }],
      dnsserverid: [{ required: true, message: this.$t('message.error.required.input') }]
    }
    this.fetchDnsServers()
    if (this.isAdmin) {
      this.fetchDomains()
    }
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
          description: this.form.description?.trim(),
          existing: this.form.existing
        }
        if (this.isAdmin && this.form.domainid && this.form.account) {
          params.domainid = this.form.domainid
          params.account = this.form.account
        }

        const response = await postAPI('createDnsZone', params)
        const jobId = response.creatednszoneresponse.jobid
        if (!jobId) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: 'Failed to get jobid for CreateDnsZone',
            duration: 0
          })
          this.loading = false
          return
        }
        this.$pollJob({
          jobId: jobId,
          title: this.$t('label.dns.create.zone'),
          description: params.name,
          successMethod: () => {
            this.$notification.success({
              message: this.$t('label.dns.create.zone'),
              description: `${this.$t('message.success.create.dns.zone')} ${params.name}`
            })
          },
          loadingMessage: `${this.$t('label.dns.create.zone')} ${params.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result')
        })
        this.closeAction()
      } catch (error) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: error?.response?.headers['x-description'] || error.message,
          duration: 0
        })
        this.loading = false
      }
    },
    closeAction () {
      this.$emit('close-action')
    },
    async fetchDomains () {
      this.fetchingDomains = true
      try {
        const response = await getAPI('listDomains', { listall: true, details: 'min' })
        this.domains = response?.listdomainsresponse?.domain || []
      } catch (error) {
        this.$notifyError(error)
      } finally {
        this.fetchingDomains = false
      }
    },
    async handleDomainChange (domainid) {
      this.form.account = undefined
      this.accounts = []
      if (!domainid) {
        return
      }
      this.fetchingAccounts = true
      try {
        const response = await getAPI('listAccounts', { domainid: domainid, state: 'enabled', listall: true })
        this.accounts = response?.listaccountsresponse?.account || []
      } catch (error) {
        this.$notifyError(error)
      } finally {
        this.fetchingAccounts = false
      }
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
