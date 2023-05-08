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
  <a-spin :spinning="loading">
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <div class="form">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
         >
          <a-form-item :label="$t('label.podid')" v-if="pods && pods.length > 0" name="podid" ref="podid">
            <template #label>
              <tooltip-label :title="$t('label.podid')" :tooltip="apiParams.podid.description"/>
            </template>
            <a-select
              v-focus="true"
              v-model:value="form.podid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="pod in pods" :key="pod.id" :value="pod.id" :label="pod.name">{{ pod.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="gateway" ref="gateway">
            <template #label>
              <tooltip-label :title="$t('label.gateway')" :tooltip="apiParams.gateway.description"/>
            </template>
            <a-input
              v-focus="true"
              v-model:value="form.gateway"
              :placeholder="apiParams.gateway.description"/>
          </a-form-item>
          <a-form-item name="netmask" ref="netmask">
            <template #label>
              <tooltip-label :title="$t('label.netmask')" :tooltip="apiParams.netmask.description"/>
            </template>
            <a-input
              v-model:value="form.netmask"
              :placeholder="apiParams.netmask.description"/>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="startip" ref="startip">
                <template #label>
                  <tooltip-label :title="$t('label.startipv4')" :tooltip="apiParams.startip.description"/>
                </template>
                <a-input
                  v-model:value="form.startip"
                  :placeholder="apiParams.startip.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="endip" ref="endip">
                <template #label>
                  <tooltip-label :title="$t('label.endipv4')" :tooltip="apiParams.endip.description"/>
                </template>
                <a-input
                  v-model:value="form.endip"
                  :placeholder="apiParams.endip.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item name="ip6cidr" ref="ip6cidr">
            <template #label>
              <tooltip-label :title="$t('label.ip6cidr')" :tooltip="apiParams.ip6cidr.description"/>
            </template>
            <a-input
              v-model:value="form.ip6cidr"
              :placeholder="apiParams.ip6cidr.description"/>
          </a-form-item>
          <a-form-item name="ip6gateway" ref="ip6gateway">
            <template #label>
              <tooltip-label :title="$t('label.ip6gateway')" :tooltip="apiParams.ip6gateway.description"/>
            </template>
            <a-input
              v-model:value="form.ip6gateway"
              :placeholder="apiParams.ip6gateway.description"/>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item name="startipv6" ref="startipv6">
                <template #label>
                  <tooltip-label :title="$t('label.startipv6')" :tooltip="apiParams.startipv6.description"/>
                </template>
                <a-input
                  v-model:value="form.startipv6"
                  :placeholder="apiParams.startipv6.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item name="endipv6" ref="endipv6">
                <template #label>
                  <tooltip-label :title="$t('label.endipv6')" :tooltip="apiParams.endipv6.description"/>
                </template>
                <a-input
                  v-model:value="form.endipv6"
                  :placeholder="apiParams.endip.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <div :span="24" class="action-button">
            <a-button
              :loading="loading"
              @click="closeAction">
              {{ $t('label.cancel') }}
            </a-button>
            <a-button
              :loading="loading"
              type="primary"
              ref="submit"
              @click="handleSubmit">
              {{ $t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVlanIpRange',
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
      zone: {},
      pods: [],
      ipV4Regex: /^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$/i,
      ipV6Regex: /^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))$/i
    }
  },
  created () {
    this.fetchData()
    this.initForm()
    this.apiParams = this.$getApiParams('createVlanIpRange')
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        podid: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('message.error.gateway') }],
        netmask: [{ required: true, message: this.$t('message.error.netmask') }],
        startip: [
          { required: true, message: this.$t('message.error.startip') },
          {
            validator: this.checkIpFormat,
            ipV4: true,
            message: this.$t('message.error.ipv4.address')
          }
        ],
        endip: [
          { required: true, message: this.$t('message.error.endip') },
          {
            validator: this.checkIpFormat,
            ipV4: true,
            message: this.$t('message.error.ipv4.address')
          }
        ],
        startipv6: [
          {
            validator: this.checkIpFormat,
            ipV6: true,
            message: this.$t('message.error.ipV6.address')
          }
        ],
        endipv6: [
          {
            validator: this.checkIpFormat,
            ipV6: true,
            message: this.$t('message.error.ipV6.address')
          }
        ]
      })
    },
    async fetchData () {
      await this.fetchZone()
      if (this.zone.networktype === 'Basic') {
        this.fetchPods()
      }
    },
    fetchZone () {
      return new Promise((resolve, reject) => {
        this.loading = true
        api('listZones', { id: this.resource.zoneid }).then(json => {
          this.zone = json.listzonesresponse.zone[0] || {}
          resolve(this.zone)
        }).catch(error => {
          this.$notifyError(error)
          reject(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    fetchPods () {
      this.loading = true
      api('listPods', {
        zoneid: this.resource.zoneid
      }).then(response => {
        this.pods = response.listpodsresponse.pod ? response.listpodsresponse.pod : []
        this.form.podid = this.pods && this.pods.length > 0 ? this.pods[0].id : ''
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()

      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.forVirtualNetwork = false
        params.networkid = this.resource.id
        if (values.podid) {
          params.podid = values.podid
        }
        params.gateway = values.gateway
        params.netmask = values.netmask
        params.startip = values.startip
        params.endip = values.endip
        params.ip6cidr = values.ip6cidr
        params.ip6gateway = values.ip6gateway
        params.startipv6 = values.startipv6
        params.endipv6 = values.endipv6

        this.loading = true

        api('createVlanIpRange', params)
          .then(() => {
            this.$notification.success({
              message: this.$t('message.success.add.iprange')
            })
            this.closeAction()
            this.$emit('refresh-data')
          }).catch(error => {
            this.$notification.error({
              message: `${this.$t('label.error')} ${error.response.status}`,
              description: error.response.data.createvlaniprangeresponse
                ? error.response.data.createvlaniprangeresponse.errortext : error.response.data.errorresponse.errortext,
              duration: 0
            })
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
    async checkIpFormat (rule, value) {
      if (!value || value === '') {
        return Promise.resolve()
      } else if (rule.ipV4 && !this.ipV4Regex.test(value)) {
        return Promise.reject(rule.message)
      } else if (rule.ipV6 && !this.ipV6Regex.test(value)) {
        return Promise.reject(rule.message)
      } else {
        return Promise.resolve()
      }
    }
  }
}
</script>

<style lang="less" scoped>
.form-layout {
  width: 60vw;

  @media (min-width: 500px) {
    width: 450px;
  }
}
</style>
