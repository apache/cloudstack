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
  <a-modal
    v-if="showAction"
    style="top: 20px;"
    centered
    :confirmLoading="loading"
    :title="$t('label.quota.configuration')"
    :closable="true"
    :maskClosable="false"
    :visible="showAction"
    :footer="null"
    @cancel="onClose"
  >
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      @finish="submitTariff"
      v-ctrl-enter="submitTariff"
     >
      <a-form-item name="value" ref="value" :label="$t('label.quota.value')">
        <a-input
          v-focus="true"
          v-model:value="form.value"></a-input>
      </a-form-item>
      <a-form-item name="startdate" ref="startdate" :label="$t('label.quota.tariff.effectivedate')">
        <a-date-picker
          :disabledDate="disabledDate"
          style="width: 100%"
          v-model:value="form.startdate"></a-date-picker>
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button @click="onClose">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="submitTariff">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-modal>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import moment from 'moment'

export default {
  name: 'EditTariffValueWizard',
  props: {
    showAction: {
      type: Boolean,
      default: () => false
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      pattern: 'YYYY-MM-DD'
    }
  },
  inject: ['parentFetchData'],
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        value: this.resource.tariffValue
      })
      this.rules = reactive({
        value: [{ required: true, message: this.$t('message.error.required.input') }],
        startdate: [{ type: 'object', required: true, message: this.$t('message.error.date') }]
      })
    },
    onClose () {
      this.$emit('edit-tariff-action', false)
    },
    submitTariff (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        const params = {}
        params.usageType = this.resource.usageType
        params.value = values.value
        params.startdate = values.startdate.format(this.pattern)

        this.loading = true

        api('quotaTariffUpdate', {}, 'POST', params).then(json => {
          const tariffResponse = json.quotatariffupdateresponse.quotatariff || {}
          if (Object.keys(tariffResponse).length > 0) {
            const effectiveDate = moment(tariffResponse.effectiveDate).format(this.pattern)
            const query = this.$route.query
            if (query.startdate !== effectiveDate) {
              this.$router.replace({ path: 'quotatariff', query: { startdate: effectiveDate } })
            }
            this.parentFetchData()
          }
          this.$message.success(`${this.$t('message.setting.updated')} ${this.resource.description}`)
          this.onClose()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    disabledDate (current) {
      return current && current < moment().endOf('day')
    }
  }
}
</script>

<style scoped>
</style>
