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
    <div class="form-layout">
      <div class="form">
        <a-form
          :form="form"
          @submit="handleSubmit"
          layout="vertical">
          <a-form-item :label="$t('label.podid')" v-if="pods && pods.length > 0">
            <a-select
              autoFocus
              v-decorator="['podid', {
                initialValue: this.pods && this.pods.length > 0 ? this.pods[0].id : '',
                rules: [{ required: true, message: `${$t('label.required')}` }]
              }]"
            >
              <a-select-option v-for="pod in pods" :key="pod.id" :value="pod.id">{{ pod.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <span slot="label">
              {{ $t('label.gateway') }}
              <a-tooltip :title="apiParams.gateway.description">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>
            <a-input
              autoFocus
              v-decorator="['gateway', {
                rules: [{ required: true, message: $t('message.error.gateway') }]
              }]"
              :placeholder="apiParams.gateway.description"/>
          </a-form-item>
          <a-form-item>
            <span slot="label">
              {{ $t('label.netmask') }}
              <a-tooltip :title="apiParams.netmask.description">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>
            <a-input
              v-decorator="['netmask', {
                rules: [{ required: true, message: $t('message.error.netmask') }]
              }]"
              :placeholder="apiParams.netmask.description"/>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" lg="12">
              <a-form-item>
                <span slot="label">
                  {{ $t('label.startipv4') }}
                  <a-tooltip :title="apiParams.startip.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-input
                  v-decorator="['startip', {
                    rules: [
                      { required: true, message: $t('message.error.startip') },
                      {
                        validator: checkIpFormat,
                        ipV4: true,
                        message: $t('message.error.ipv4.address')
                      }
                    ]
                  }]"
                  :placeholder="apiParams.startip.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item>
                <span slot="label">
                  {{ $t('label.endipv4') }}
                  <a-tooltip :title="apiParams.endip.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-input
                  v-decorator="['endip', {
                    rules: [
                      { required: true, message: $t('message.error.endip') },
                      {
                        validator: checkIpFormat,
                        ipV4: true,
                        message: $t('message.error.ipv4.address')
                      }
                    ]
                  }]"
                  :placeholder="apiParams.endip.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item>
            <span slot="label">
              {{ $t('label.ip6cidr') }}
              <a-tooltip :title="apiParams.ip6cidr.description">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>
            <a-input
              v-decorator="['ip6cidr']"
              :placeholder="apiParams.ip6cidr.description"/>
          </a-form-item>
          <a-form-item>
            <span slot="label">
              {{ $t('label.ip6gateway') }}
              <a-tooltip :title="apiParams.ip6gateway.description">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>
            <a-input
              v-decorator="['ip6gateway']"
              :placeholder="apiParams.ip6gateway.description"/>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item>
                <span slot="label">
                  {{ $t('label.startipv6') }}
                  <a-tooltip :title="apiParams.startipv6.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-input
                  v-decorator="['startipv6', {
                    rules: [
                      {
                        validator: checkIpFormat,
                        ipV6: true,
                        message: $t('message.error.ipv6.address')
                      }
                    ]
                  }]"
                  :placeholder="apiParams.startipv6.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item>
                <span slot="label">
                  {{ $t('label.endipv6') }}
                  <a-tooltip :title="apiParams.endipv6.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-input
                  v-decorator="['endipv6', {
                    rules: [
                      {
                        validator: checkIpFormat,
                        ipV6: true,
                        message: $t('message.error.ipv6.address')
                      }
                    ]
                  }]"
                  :placeholder="apiParams.endip.description"/>
              </a-form-item>
            </a-col>
          </a-row>
          <div :span="24" class="action-button">
            <a-button
              :loading="loading"
              @click="closeAction">
              {{ this.$t('label.cancel') }}
            </a-button>
            <a-button
              :loading="loading"
              type="primary"
              @click="handleSubmit">
              {{ this.$t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'CreateVlanIpRange',
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
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.createVlanIpRange || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
    this.fetchData()
  },
  methods: {
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
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()

      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

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
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    checkIpFormat (rule, value, callback) {
      if (!value || value === '') {
        callback()
      } else if (rule.ipV4 && !this.ipV4Regex.test(value)) {
        callback(rule.message)
      } else if (rule.ipV6 && !this.ipV6Regex.test(value)) {
        callback(rule.message)
      } else {
        callback()
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

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
