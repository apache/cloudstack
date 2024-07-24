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
              :placeholder="apiParams.url.description"
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
          <a-form-item name="networkdevicetype" ref="networkdevicetype" :label="$t('label.networkdevicetype')">
            <a-select
              :placeholder="apiParams.networkdevicetype.description"
              v-model:value="form.networkdevicetype"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="opt in networkDeviceType"
                :key="opt.id"
                :label="$t(opt.description)">{{ $t(opt.description) }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="publicinterface" ref="publicinterface" :label="$t('label.publicinterface')">
            <a-input v-model:value="form.publicinterface" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="privateinterface" ref="privateinterface" :label="$t('label.privateinterface')">
            <a-input v-model:value="form.privateinterface" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="usageinterface" ref="usageinterface" :label="$t('label.usageinterface')">
            <a-input v-model:value="form.usageinterface" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="12" :lg="12">
          <a-form-item name="numretries" ref="numretries" :label="$t('label.numretries')">
            <a-input-number
              style="width: 100%"
              v-model:value="form.numretries" />
          </a-form-item>
        </a-col>
        <a-col :md="12" :lg="12">
          <a-form-item name="timeout" ref="timeout" :label="$t('label.timeout')">
            <a-input-number
              style="width: 100%"
              v-model:value="form.timeout" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="12" :lg="12">
          <a-form-item name="publicnetwork" ref="publicnetwork" :label="$t('label.publicnetwork')">
            <a-input
              v-model:value="form.publicnetwork"
              :disabled="true" />
          </a-form-item>
        </a-col>
        <a-col :md="12" :lg="12">
          <a-form-item name="privatenetwork" ref="privatenetwork" :label="$t('label.privatenetwork')">
            <a-input
              v-model:value="form.privatenetwork"
              :disabled="true" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="pavr" ref="pavr" :label="$t('label.pavr')">
            <a-input v-model:value="form.pavr" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="12" :lg="12">
          <a-form-item name="patp" ref="patp" :label="$t('label.patp')">
            <a-input v-model:value="form.patp" />
          </a-form-item>
        </a-col>
        <a-col :md="12" :lg="12">
          <a-form-item name="palp" ref="palp" :label="$t('label.palp')">
            <a-input v-model:value="form.palp" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item name="capacity" ref="capacity" :label="$t('label.capacity')">
            <a-input v-model:value="form.capacity" />
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
  name: 'AddPaloAltoFirewall',
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
  computed: {
    networkDeviceType () {
      const items = []
      items.push({
        id: 'PaloAltoFirewall',
        description: 'label.palo.alto.firewall'
      })

      return items
    }
  },
  created () {
    this.initForm()
    this.apiParams = this.$getApiParams('addPaloAltoFirewall')
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
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        numretries: 2,
        timeout: 300,
        publicnetwork: 'untrusted',
        privatenetwork: 'untrusted'
      })
      this.rules = reactive({
        ip: [{ required: true, message: this.$t('message.error.required.input') }],
        username: [{ required: true, message: this.$t('message.error.required.input') }],
        password: [{ required: true, message: this.$t('message.error.required.input') }],
        networkdevicetype: [{ required: true, message: this.$t('message.error.select') }],
        numretries: [{ type: 'number' }],
        timeout: [{ type: 'number' }]
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const values = toRaw(this.form)
        const params = {}
        params.physicalnetworkid = this.resource.physicalnetworkid
        params.username = values.username
        params.password = values.password
        params.networkdevicetype = values.networkdevicetype

        const url = []
        const ip = values.ip
        url.push('https://' + ip)
        let isQuestionMarkAdded = false

        const publicInterface = values.publicinterface ? values.publicinterface : null
        if (publicInterface != null && publicInterface.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('publicinterface=' + publicInterface)
        }

        const privateInterface = values.privateinterface ? values.privateinterface : null
        if (privateInterface != null && privateInterface.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('privateinterface=' + publicInterface)
        }

        const usageInterface = values.usageinterface ? values.usageinterface : null
        if (usageInterface != null && usageInterface.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('usageinterface=' + usageInterface)
        }

        const numretries = values.numretries ? values.numretries : null
        if (numretries != null && numretries.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('numretries=' + numretries)
        }

        const timeout = values.timeout ? values.timeout : null
        if (timeout != null && timeout.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('timeout=' + timeout)
        }

        const publicNetwork = values.publicnetwork ? values.publicnetwork : null
        if (publicNetwork != null && publicNetwork.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('publicnetwork=' + publicNetwork)
        }

        const privateNetwork = values.privatenetwork ? values.privatenetwork : null
        if (privateNetwork != null && privateNetwork.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('privatenetwork=' + privateNetwork)
        }

        const capacity = values.capacity ? values.capacity : null
        if (capacity != null && capacity.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('lbdevicecapacity=' + capacity)
        }

        const dedicated = values.dedicated ? values.dedicated : false
        if (!isQuestionMarkAdded) {
          url.push('?')
          isQuestionMarkAdded = true
        } else {
          url.push('&')
        }
        url.push('lbdevicededicated=' + dedicated)

        const externalVirtualRouter = values.pavr ? values.pavr : null
        if (externalVirtualRouter != null && externalVirtualRouter.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('pavr=' + externalVirtualRouter)
        }

        const externalThreatProfile = values.patp ? values.patp : null
        if (externalThreatProfile != null && externalThreatProfile.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('patp=' + externalThreatProfile)
        }

        const externalLogProfile = values.palp ? values.palp : null
        if (externalLogProfile != null && externalLogProfile.length > 0) {
          if (!isQuestionMarkAdded) {
            url.push('?')
            isQuestionMarkAdded = true
          } else {
            url.push('&')
          }
          url.push('palp=' + externalLogProfile)
        }

        params.url = url.join('')

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
          const jobId = await this.addPaloAltoFirewall(params)
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
    addPaloAltoFirewall (args) {
      return new Promise((resolve, reject) => {
        api('addPaloAltoFirewall', args).then(json => {
          const jobId = json.addpaloaltofirewallresponse.jobid || null
          resolve(jobId)
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>
