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
    :okText="$t('label.ok')"
    :cancelText="$t('label.cancel')"
    @ok="submitTariff"
    @cancel="onClose"
  >
    <a-form
      :form="form"
      layout="vertical"
      @submit="submitTariff">
      <a-form-item :label="$t('label.quota.value')">
        <a-input
          autoFocus
          v-decorator="['value', {
            rules: [{
              required: true,
              message: `${$t('message.error.required.input')}`
            }]
          }]"></a-input>
      </a-form-item>
      <a-form-item :label="$t('label.quota.tariff.effectivedate')">
        <a-date-picker
          :disabledDate="disabledDate"
          style="width: 100%"
          v-decorator="['startdate', {
            rules: [{
              type: 'object',
              required: true,
              message: `${$t('message.error.date')}`
            }]
          }]"></a-date-picker>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>
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
  inject: ['parentEditTariffAction', 'parentFetchData'],
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.form.getFieldDecorator('value', {
      initialValue: this.resource.tariffValue
    })
  },
  methods: {
    onClose () {
      this.parentEditTariffAction(false)
    },
    submitTariff (e) {
      e.preventDefault()
      this.form.validateFields((error, values) => {
        if (error) return

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

          this.onClose()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.loading = false
        })
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
