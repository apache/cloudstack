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
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit">
      <a-form-item ref="description" name="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-textarea
          v-model:value="form.description"
          :placeholder="$t('placeholder.quota.tariff.description')"
          :max-length="65535" />
      </a-form-item>
      <a-form-item ref="value" name="value">
        <template #label>
          <tooltip-label :title="$t('label.quota.tariff.value')" :tooltip="apiParams.value.description"/>
        </template>
        <a-input-number
          class="full-width-input"
          v-model:value="form.value"
          :placeholder="$t('placeholder.quota.tariff.value')" />
      </a-form-item>
      <a-form-item ref="position" name="position">
        <template #label>
         <tooltip-label :title="$t('label.quota.tariff.position')" :tooltip="apiParams.position.description"/>
       </template>
       <a-input-number
          class="full-width-input"
          v-model:value="form.position"
          :placeholder="$t('placeholder.quota.tariff.position')" />
      </a-form-item>
      <a-form-item ref="endDate" name="endDate">
        <template #label>
          <tooltip-label :title="$t('label.end.date')" :tooltip="apiParams.enddate.description"/>
        </template>
        <a-date-picker
          class="full-width-input"
          v-model:value="form.endDate"
          :disabled-date="disabledEndDate"
          :placeholder="$t('placeholder.quota.tariff.enddate')"
          show-time
        />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { dayjs, parseDateToDatePicker, parseDayJsObject } from '@/utils/date'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { ref, reactive, toRaw } from 'vue'
import store from '@/store'

export default {
  name: 'EditQuotaTariff',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    loading: false,
    dayjs
  }),
  inject: ['parentFetchData'],
  beforeCreate () {
    this.apiParams = this.$getApiParams('quotaTariffUpdate')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        description: this.resource.description,
        value: this.resource.tariffValue,
        position: this.resource.position,
        endDate: parseDateToDatePicker(this.resource.endDate)
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        const params = {
          name: this.resource.name
        }

        if (this.resource.description !== values.description) {
          params.description = values.description
        }

        if (values.value && this.resource.tariffValue !== values.value) {
          params.value = values.value
        }

        if (values.position && this.resource.position !== values.position) {
          params.position = values.position
        }

        if (values.endDate && !values.endDate.isSame(this.resource.endDate)) {
          params.enddate = parseDayJsObject({ value: values.endDate })
        }

        if (Object.keys(params).length === 1) {
          this.closeModal()
          return
        }

        this.loading = true

        api('quotaTariffUpdate', {}, 'POST', params).then(json => {
          const tariffResponse = json.quotatariffupdateresponse.quotatariff || {}
          if (tariffResponse.id && this.$route.params.id) {
            this.$router.push(`/quotatariff/${tariffResponse.id}`)
          } else if (Object.keys(tariffResponse).length > 0) {
            this.parentFetchData()
          }

          this.$message.success(this.$t('message.quota.tariff.update.success', { quotaTariff: this.resource.name }))
          this.closeModal()
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
    disabledEndDate (current) {
      const lowerEndDateLimit = dayjs(this.resource.effectiveDate)
      const startOfToday = dayjs().startOf('day')

      if (store.getters.usebrowsertimezone) {
        return current < startOfToday || current < lowerEndDateLimit.startOf('day')
      }
      return current < startOfToday || current < lowerEndDateLimit.utc(false).startOf('day')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>
