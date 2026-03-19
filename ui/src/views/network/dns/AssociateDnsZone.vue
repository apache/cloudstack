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

        <a-form-item name="dnszoneid" ref="dnszoneid">
          <template #label>
            <tooltip-label
              :title="$t('label.dns.zone')"
              :tooltip="apiParams.dnszoneid?.description" />
          </template>
          <a-select
            v-model:value="form.dnszoneid"
            :placeholder="$t('label.dns.zone.select')"
            :loading="fetchingZones"
            showSearch
            optionFilterProp="label"
            v-focus="true">
            <a-select-option
              v-for="zone in dnsZones"
              :key="zone.id"
              :value="zone.id"
              :label="zone.name">
              {{ zone.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="subdomain" ref="subdomain">
          <template #label>
            <tooltip-label
              :title="$t('label.subdomain')"
              :tooltip="apiParams.subdomain?.description" />
          </template>
          <a-input
            v-model:value="form.subdomain"
            :placeholder="apiParams.subdomain?.description" />
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
  name: 'AssociateDnsZone',
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
        dnszoneid: undefined,
        subdomain: ''
      },
      rules: {},
      fetchingZones: false,
      dnsZones: []
    }
  },
  created () {
    this.apiParams = this.$getApiParams('associateDnsZoneToNetwork') || {}
    this.rules = {
      dnszoneid: [{ required: true, message: this.$t('message.error.required.input') }]
    }
    this.fetchDnsZones()
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
          dnszoneid: this.form.dnszoneid,
          networkid: this.resource.id
        }

        if (this.form.subdomain && this.form.subdomain.trim()) {
          params.subdomain = this.form.subdomain.trim()
        }

        await postAPI('associateDnsZoneToNetwork', params)
        this.$notification.success({
          message: this.$t('label.action.associate.dns.zone'),
          description: this.$t('message.success.associate.dns.zone')
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
    async fetchDnsZones () {
      this.fetchingZones = true
      try {
        const response = await getAPI('listDnsZones')
        const listResponse = response?.listdnszonesresponse || {}
        this.dnsZones = listResponse.dnszone || []
        if (this.dnsZones.length > 0) {
          this.form.dnszoneid = this.dnsZones[0].id
        }
      } catch (error) {
        console.error('Failed to fetch DNS zones', error)
        this.$message.warning(this.$t('message.error.fetch.dns.zones'))
      } finally {
        this.fetchingZones = false
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
