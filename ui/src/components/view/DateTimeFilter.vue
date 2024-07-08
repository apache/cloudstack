// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-form
    class="form-layout"
    :ref="formRef"
    :model="form"
    @finish="handleSubmit"
    layout="vertical">
    <div v-show="showAllDataOption && (!(!showStartDate || !showEndDate) || (!showStartDate && !showEndDate))">
      <a-form-item :label="$t('label.all.available.data')" ref="allData" name="allData">
        <a-switch v-model:checked="allDataIsChecked" @change="onToggleAllData"/>
      </a-form-item>
      <div v-show="showAllDataAlert">
        <a-alert :message="allDataMessage" banner />
      </div>
    </div>
    <div v-show="showStartDate">
      <a-form-item :label="$t('label.only.start.date.and.time')" ref="allData" name="allData">
        <a-switch v-model:checked="onlyStartDateIsChecked" @change="onToggleStartDate"/>
      </a-form-item>
      <a-form-item :label="$t('label.start.date.and.time')" ref="startDate" name="startDate" :rules="[{ required: showStartDate, message: `${this.$t('message.error.start.date.and.time')}` }]">
        <a-date-picker
          v-model:value="form.startDate"
          show-time
          :placeholder="$t('message.select.start.date.and.time')"/>
      </a-form-item>
    </div>
    <div v-show="showEndDate">
      <a-form-item :label="$t('label.only.end.date.and.time')">
        <a-switch v-model:checked="onlyEndDateIsChecked"  @change="onToggleEndDate"/>
      </a-form-item>
      <a-form-item :label="$t('label.end.date.and.time')" ref="endDate" name="endDate" :rules="[{ required: showEndDate, message: `${this.$t('message.error.end.date.and.time')}` }]">
        <a-date-picker
          v-model:value="form.endDate"
          show-time
          :placeholder="$t('message.select.end.date.and.time')"/>
      </a-form-item>
    </div>
  </a-form>
  <div :span="24" class="action-button">
    <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
    <a-button type="primary" @click="handleSubmit">{{ submitButtonLabel }}</a-button>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import moment from 'moment'

export default {
  name: 'DateTimeFilter',
  emits: ['closeAction', 'onSubmit'],
  props: {
    startDateProp: {
      type: [Date, String, Number],
      required: false
    },
    endDateProp: {
      type: [Date, String, Number],
      required: false
    },
    showAllDataOption: {
      type: Boolean,
      default: true
    },
    allDataMessage: {
      type: String,
      value: ''
    }
  },
  computed: {
    startDate () {
      if (this.startDateProp) {
        return moment(this.startDateProp)
      }
      return null
    },
    endDate () {
      if (this.endDateProp) {
        return moment(this.endDateProp)
      }
      return null
    }
  },
  data () {
    return {
      allDataIsChecked: false,
      onlyStartDateIsChecked: false,
      onlyEndDateIsChecked: false,
      showAllDataAlert: false,
      showAllData: true,
      showStartDate: true,
      showEndDate: true,
      submitButtonLabel: this.$t('label.ok')
    }
  },
  updated () {
    this.form.startDate = this.startDate
    this.form.endDate = this.endDate
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        startDate: this.startDate,
        endDate: this.endDate
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.formRef.value.validate().then(() => {
        this.submitButtonLabel = this.$t('label.refresh')
        const values = toRaw(this.form)
        this.$emit('onSubmit', values)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('closeAction')
    },
    onToggleAllData () {
      this.showAllDataAlert = !this.showAllDataAlert
      if (this.showAllDataAlert) {
        this.showStartDate = false
        this.showEndDate = false
        this.form.startDate = null
        this.form.endDate = null
      } else {
        this.showStartDate = true
        this.showEndDate = true
      }
      this.resetSubmitButton()
    },
    onToggleStartDate () {
      this.showEndDate = !this.showEndDate
      if (this.showEndDate === false) {
        this.form.endDate = null
      }
      this.resetSubmitButton()
    },
    onToggleEndDate () {
      this.showStartDate = !this.showStartDate
      if (this.showStartDate === false) {
        this.form.startDate = null
      }
      this.resetSubmitButton()
    },
    resetSubmitButton () {
      this.submitButtonLabel = this.$t('label.ok')
    }
  }
}
</script>
