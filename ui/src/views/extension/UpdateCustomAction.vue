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
      :loading="loading"
      layout="vertical"
      @finish="handleSubmit">
      <a-form-item name="description" ref="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-input
          v-model:value="form.description"
          :placeholder="apiParams.description.description"
          v-focus="true" />
      </a-form-item>
      <a-form-item ref="allowedroletypes" name="allowedroletypes">
        <template #label>
          <tooltip-label :title="$t('label.allowedroletypes')" :tooltip="apiParams.allowedroletypes.description"/>
        </template>
        <a-select
          showSearch
          mode="multiple"
          v-model:value="form.allowedroletypes"
          :placeholder="apiParams.allowedroletypes.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in roleTypes" :key="opt.id" :label="opt.description || opt.id">
            {{ opt.description || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="parameters" ref="parameters">
        <template #label>
          <tooltip-label :title="$t('label.parameters')" :tooltip="apiParams.parameters.description"/>
        </template>
        <div style="margin-bottom: 10px">{{ $t('message.add.custom.action.parameters') }}</div>
        <parameters-input
          v-model:value="form.parameters" />
      </a-form-item>
      <a-form-item name="successmessage" ref="successmessage">
        <template #label>
          <tooltip-label :title="$t('label.successmessage')" :tooltip="apiParams.successmessage.description"/>
        </template>
        <a-input
          v-model:value="form.successmessage"
          :placeholder="apiParams.successmessage.description" />
      </a-form-item>
      <a-form-item name="errormessage" ref="errormessage">
        <template #label>
          <tooltip-label :title="$t('label.errormessage')" :tooltip="apiParams.errormessage.description"/>
        </template>
        <a-input
          v-model:value="form.errormessage"
          :placeholder="apiParams.errormessage.description" />
      </a-form-item>
      <a-form-item name="details" ref="details">
        <template #label>
          <tooltip-label :title="$t('label.configuration.details')" :tooltip="apiParams.details.description"/>
        </template>
        <div style="margin-bottom: 10px">{{ $t('message.add.extension.custom.action.details') }}</div>
        <details-input
          v-model:value="form.details" />
      </a-form-item>
      <a-form-item name="timeout" ref="timeout">
        <template #label>
          <tooltip-label :title="$t('label.timeout')" :tooltip="apiParams.timeout.description"/>
        </template>
        <a-input-number
          v-model:value="form.timeout"
          :placeholder="apiParams.timeout.description"
          :min="1" />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import ParametersInput from '@/views/extension/ParametersInput'
import DetailsInput from '@/components/widgets/DetailsInput'

export default {
  name: 'AddCustomAction',
  components: {
    TooltipLabel,
    ParametersInput,
    DetailsInput
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      roleTypes: [],
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateCustomAction')
    this.extensionsApiParams = {
      listall: true
    }
  },
  created () {
    this.initForm()
    this.roleTypes = this.$fetchCustomActionRoleTypes()
  },
  methods: {
    fixParamatersOptions (params) {
      if (!params) {
        return
      }
      for (var param of params) {
        if (!param.options || param.options.length === 0) {
          continue
        }
        param.options = param.options.join(',')
      }
      return params
    },
    initForm () {
      this.formRef = ref()
      const formData = {
        parameters: this.fixParamatersOptions(this.resource.parameters)
      }
      const keys = ['description', 'allowedroletypes', 'successmessage', 'errormessage', 'details', 'timeout']
      for (const key of keys) {
        formData[key] = this.resource[key]
      }
      this.form = reactive(formData)
      this.rules = reactive({})
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          id: this.resource.id
        }
        const keys = ['description', 'allowedroletypes', 'successmessage', 'errormessage', 'timeout']
        for (const key of keys) {
          if (values[key] !== undefined || values[key] !== null) {
            params[key] = Array.isArray(values[key]) ? values[key].join(',') : values[key]
          }
        }
        if (values.parameters && values.parameters.length > 0) {
          values.parameters.forEach((param, index) => {
            Object.keys(param).forEach(key => {
              const val = param[key]
              params['parameters[' + index + '].' + key] = Array.isArray(val) ? val.join(',') : val
            })
          })
        } else {
          params.cleanupparameters = true
        }
        if (values.details && Object.keys(values.details).length > 0) {
          Object.entries(values.details).forEach(([key, value]) => {
            params['details[0].' + key] = value
          })
        } else {
          params.cleanupdetails = true
        }
        postAPI('updateCustomAction', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.update.custom.action'),
            description: this.$t('message.success.update.custom.action')
          })
          this.closeAction()
          this.parentFetchData()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 100vw;
  @media (min-width: 1000px) {
    width: 950px;
  }
}
</style>
