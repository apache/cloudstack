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
    <a-form
      :form="form"
      layout="vertical"
      @submit="handleSubmit">
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.ip')">
            <a-input
              autoFocus
              v-decorator="['ip', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.username')">
            <a-input
              v-decorator="['username', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.password')">
            <a-input-password
              v-decorator="['password', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.numretries')">
            <a-input-number
              style="width: 100%"
              v-decorator="['numretries', { initialValue: 2 }]" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.transportzoneuuid')">
            <a-input
              v-decorator="['transportzoneuuid']" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.l3gatewayserviceuuid')">
            <a-input
              v-decorator="['l3gatewayserviceuuid']" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item :label="$t('label.l2gatewayserviceuuid')">
            <a-input
              v-decorator="['l2gatewayserviceuuid']" />
          </a-form-item>
        </a-col>
      </a-row>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'AddNiciraNvpDevice',
  props: {
    resource: {
      type: Object,
      default: () => {}
    },
    action: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      loading: false,
      nsp: {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    if (this.resource && Object.keys(this.resource).length > 0) {
      this.nsp = this.resource
    }
  },
  inject: ['provideCloseAction', 'provideReload', 'provideCloseAction', 'parentPollActionCompletion'],
  methods: {
    onCloseAction () {
      this.provideCloseAction()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFieldsAndScroll(async (err, values) => {
        if (err) {
          return
        }
        const params = {}
        params.physicalnetworkid = this.resource.physicalnetworkid
        params.hostname = values.ip
        params.username = values.username
        params.password = values.password
        params.transportzoneuuid = values.transportzoneuuid ? values.transportzoneuuid : null
        params.l2gatewayserviceuuid = values.l2gatewayserviceuuid ? values.l2gatewayserviceuuid : null
        params.l3gatewayserviceuuid = values.l3gatewayserviceuuid ? values.l3gatewayserviceuuid : null

        this.loading = true

        try {
          if (!this.nsp.id) {
            const addParams = {}
            addParams.name = this.resource.name
            addParams.physicalnetworkid = this.resource.physicalnetworkid
            const networkServiceProvider = await this.addNetworkServiceProvider(addParams)
            this.nsp = { ...this.resource, ...networkServiceProvider }
          }
          params.id = this.nsp.id
          const jobId = await this.addNiciraNvpDevice(params)
          this.parentPollActionCompletion(jobId, this.action, this.$t(this.nsp.name))
          this.provideCloseAction()
          this.loading = false
        } catch (error) {
          this.loading = false
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }
      })
    },
    addNetworkServiceProvider (args) {
      return new Promise((resolve, reject) => {
        api('addNetworkServiceProvider', args).then(async json => {
          this.$pollJob({
            jobId: json.addnetworkserviceproviderresponse.jobid,
            successMethod: (result) => {
              resolve(result.jobresult.networkserviceprovider)
            },
            errorMethod: (result) => {
              reject(result.jobresult.errortext)
            },
            catchMessage: this.$t('error.fetching.async.job.result'),
            action: {
              isFetchData: false
            }
          })
        }).catch(error => {
          reject(error)
        })
      })
    },
    addNiciraNvpDevice (args) {
      return new Promise((resolve, reject) => {
        api('addNiciraNvpDevice', args).then(json => {
          const jobId = json.addniciranvpdeviceresponse.jobid || null
          resolve(jobId)
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>
