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
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit">
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.name"
          :placeholder="$t('placeholder.quota.tariff.name')"
          :max-length="65535"/>
      </a-form-item>
      <a-form-item ref="description" name="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-textarea
          v-model:value="form.description"
          :placeholder="$t('placeholder.quota.tariff.description')"
          :max-length="65535" />
      </a-form-item>
      <a-form-item ref="usageType" name="usageType">
        <template #label>
          <tooltip-label :title="$t('label.quota.type.name')" :tooltip="apiParams.usagetype.description"/>
        </template>
        <a-select
          v-model:value="form.usageType"
          show-search
          :placeholder="$t('placeholder.quota.tariff.usagetype')">
          <a-select-option v-for="quotaType of getQuotaTypes()" :value="`${quotaType.id}-${quotaType.type}`" :key="quotaType.id">
            {{ $t(quotaType.type) }}
          </a-select-option>
        </a-select>
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
          <tooltip-label :title="$t('label.quota.tariff.position')" :tooltip="apiParams.position.description" />
        </template>
        <a-input-number
          class="full-width-input"
          v-model:value="form.position"
          :placeholder="$t('placeholder.quota.tariff.position')" />
      </a-form-item>
      <a-form-item ref="startDate" name="startDate">
        <template #label>
          <tooltip-label :title="$t('label.start.date')" :tooltip="apiParams.startdate.description"/>
        </template>
        <a-date-picker
          class="full-width-input"
          v-model:value="form.startDate"
          :disabled-date="disabledStartDate"
          :placeholder="$t('placeholder.quota.tariff.startdate')"
          show-time
        />
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
import { ref, reactive, toRaw } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { getQuotaTypes } from '@/utils/quota'
import { dayjs, parseDayJsObject } from '@/utils/date'
import { mixinForm } from '@/utils/mixin'

export default {
  name: 'CreateQuotaTariff',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      dayjs
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('quotaTariffCreate')
  },
  created () {
    this.initForm()
  },
  inject: ['parentFetchData'],
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        value: 0,
        processingPeriod: 'BY_ENTRY'
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.action.quota.tariff.create.error.namerequired') }],
        usageType: [{ required: true, message: this.$t('message.action.quota.tariff.create.error.usagetyperequired') }],
        value: [{ required: true, message: this.$t('message.action.quota.tariff.create.error.valuerequired') }]
      })
      this.processingPeriod = 'BY_ENTRY'
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        values.usageType = values.usageType.split('-')[0]

        if (values.startDate) {
          values.startDate = parseDayJsObject({ value: values.startDate })
        }

        if (values.endDate) {
          values.endDate = parseDayJsObject({ value: values.endDate })
        }

        this.loading = true
        api('quotaTariffCreate', values).then(response => {
          this.$message.success(this.$t('message.quota.tariff.create.success', { quotaTariff: values.name }))
          this.parentFetchData()
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    getQuotaTypes () {
      return getQuotaTypes()
    },
    disabledStartDate (current) {
      return current < dayjs().startOf('day')
    },
    disabledEndDate (current) {
      return current < (this.form.startDate || dayjs().startOf('day'))
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>
