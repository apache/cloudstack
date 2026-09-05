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
  <div>
    <a-modal
      v-model:visible="showFilterQuotaDataByPeriodModal"
      :title="$t('label.quota.select.period')"
      :maskClosable="false"
      :footer="null">
      <a-form
        class="form-layout"
        :model="form"
        @finish="handleSubmit"
        v-ctrl-enter="handleSubmit">
        <a-form-item ref="dates" name="dates" style="width: 100%">
          <a-range-picker
            class="w-100"
            v-model:value="form.dates"
            show-time />
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeFilterQuotaDataByPeriodModal">{{ this.$t('label.cancel') }}</a-button>
          <a-button ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <div class="chart-row">
      <a-space direction="vertical">
        <div>
          <a-radio-group
            v-model:value="periodSelected"
            buttonStyle="solid"
            @change="handlePeriodChange">
            <a-radio-button value="day">
              {{ $t('label.quota.period.today') }}
            </a-radio-button>
            <a-radio-button value="week">
              {{ $t('label.quota.period.this.week') }}
            </a-radio-button>
            <a-radio-button value="month">
              {{ $t('label.quota.period.this.month') }}
            </a-radio-button>
            <a-radio-button value="lastmonth">
              {{ $t('label.quota.period.last.month') }}
            </a-radio-button>
            <a-radio-button value="year">
              {{ $t('label.quota.period.this.year') }}
            </a-radio-button>
            <a-radio-button value="lastyear">
              {{ $t('label.quota.period.last.year') }}
            </a-radio-button>
            <a-radio-button value="custom" @click="openFilterQuotaDataByPeriodModal">
              {{ $t('label.quota.period.custom') }}
            </a-radio-button>
          </a-radio-group>
        </div>
      </a-space>
      <div class="mt-10">
        <filter-outlined style="margin-right: 5px"/>
        <span v-html="getPeriodToString()" />
      </div>
    </div>
  </div>
</template>

<script>

import { dayjs, parseDayJsObject, toLocaleDate } from '@/utils/date'
import { reactive, toRaw } from 'vue'

export default {
  name: 'FilterQuotaDataByPeriodView',
  data () {
    return {
      periodSelected: 'month',
      showFilterQuotaDataByPeriodModal: false,
      startDate: undefined,
      endDate: undefined
    }
  },
  created () {
    this.initForm()
    this.handlePeriodChange()
  },
  methods: {
    initForm () {
      this.form = reactive({ dates: [this.startDate, this.endDate] })
    },
    handlePeriodChange () {
      let end = dayjs()
      let start
      switch (this.periodSelected) {
        case 'day':
        case 'week':
        case 'month':
        case 'year':
          start = dayjs().startOf(this.periodSelected)
          break
        case 'lastmonth': {
          const lastMonth = dayjs().subtract(1, 'months')
          start = dayjs(lastMonth).startOf('month')
          end = dayjs(lastMonth).endOf('month')
          break
        }
        case 'lastyear': {
          const lastYear = dayjs().subtract(1, 'years')
          start = dayjs(lastYear).startOf('year')
          end = dayjs(lastYear).endOf('year')
          break
        }
        default:
          return
      }
      this.triggerFetchData([start, end])
    },
    openFilterQuotaDataByPeriodModal () {
      this.showFilterQuotaDataByPeriodModal = true
    },
    closeFilterQuotaDataByPeriodModal () {
      this.showFilterQuotaDataByPeriodModal = false
    },
    getPeriodToString () {
      return this.$t('label.quota.filter.period', {
        startDate: toLocaleDate({ date: parseDayJsObject({ value: this.startDate, keepMoment: true, format: false }) }),
        endDate: toLocaleDate({ date: parseDayJsObject({ value: this.endDate, keepMoment: true, format: false }) })
      })
    },
    handleSubmit () {
      const formRaw = toRaw(this.form)
      this.triggerFetchData(formRaw.dates)
    },
    triggerFetchData (values) {
      this.startDate = values[0]
      this.endDate = values[1]
      this.initForm()
      this.closeFilterQuotaDataByPeriodModal()
      this.$emit('fetchData', this.startDate, this.endDate)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
</style>
