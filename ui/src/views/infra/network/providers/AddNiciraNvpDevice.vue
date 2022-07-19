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
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      @finish="handleSubmit"
     >
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="ip" ref="ip" :label="$t('label.ip')">
            <a-input
              :placeholder="apiParams.hostname.description"
              v-focus="true"
              v-model:value="form.ip" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="username" ref="username" :label="$t('label.username')">
            <a-input
              :placeholder="apiParams.username.description"
              v-model:value="form.username" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="password" ref="password" :label="$t('label.password')">
            <a-input-password
              :placeholder="apiParams.password.description"
              v-model:value="form.password" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="numretries" ref="numretries" :label="$t('label.numretries')">
            <a-input-number
              style="width: 100%"
              v-model:value="form.numretries" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="transportzoneuuid" ref="transportzoneuuid" :label="$t('label.transportzoneuuid')">
            <a-input
              :placeholder="apiParams.transportzoneuuid.description"
              v-model:value="form.transportzoneuuid" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="l3gatewayserviceuuid" ref="l3gatewayserviceuuid" :label="$t('label.l3gatewayserviceuuid')">
            <a-input
              :placeholder="apiParams.l3gatewayserviceuuid.description"
              v-model:value="form.l3gatewayserviceuuid" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="l2gatewayserviceuuid" ref="l2gatewayserviceuuid" :label="$t('label.l2gatewayserviceuuid')">
            <a-input
              :placeholder="apiParams.l2gatewayserviceuuid.description"
              v-model:value="form.l2gatewayserviceuuid" />
          </a-form-item>
        </a-col>
      </a-row>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
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
      apiParams: {},
      loading: false,
      nsp: {}
    }
  },
  created () {
    this.initForm()
    this.apiParams = this.$getApiParams('addNiciraNvpDevice')
  },
  mounted () {
    if (this.resource && Object.keys(this.resource).length > 0) {
      this.nsp = this.resource
    }
  },
  inject: ['provideCloseAction', 'provideReload', 'provideCloseAction', 'parentPollActionCompletion'],
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        numretries: 2
      })
      this.rules = reactive({
        ip: [{ required: true, message: this.$t('message.error.required.input') }],
        username: [{ required: true, message: this.$t('message.error.required.input') }],
        password: [{ required: true, message: this.$t('message.error.required.input') }],
        numretries: [{ type: 'number' }]
      })
    },
    onCloseAction () {
      this.provideCloseAction()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const values = toRaw(this.form)
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
