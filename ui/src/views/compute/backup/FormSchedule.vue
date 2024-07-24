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
    <div class="form-layout">
      <label>
        {{ $t('label.header.backup.schedule') }}
      </label>
      <div class="form" v-ctrl-enter="handleSubmit">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit">
          <a-row :gutter="12">
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.intervaltype')" ref="intervaltype" name="intervaltype">
                <a-radio-group
                  v-model:value="form.intervaltype"
                  button-style="solid"
                  @change="handleChangeIntervalType">
                  <a-radio-button value="hourly">
                    {{ $t('label.hourly') }}
                  </a-radio-button>
                  <a-radio-button value="daily">
                    {{ $t('label.daily') }}
                  </a-radio-button>
                  <a-radio-button value="weekly">
                    {{ $t('label.weekly') }}
                  </a-radio-button>
                  <a-radio-button value="monthly">
                    {{ $t('label.monthly') }}
                  </a-radio-button>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='hourly'">
              <a-form-item :label="$t('label.time')" ref="time" name="time">
                <a-input-number
                  style="width: 100%"
                  v-model:value="form.time"
                  :placeholder="$t('label.minute.past.hour')"
                  :min="1"
                  :max="59"
                  v-focus="true" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="['daily', 'weekly', 'monthly'].includes(form.intervaltype)">
              <a-form-item
                class="custom-time-select"
                :label="$t('label.time')"
                ref="timeSelect"
                name="timeSelect">
                <a-time-picker
                  use12Hours
                  format="h:mm A"
                  v-model:value="form.timeSelect"
                  style="width: 100%;" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='weekly'">
              <a-form-item :label="$t('label.day.of.week')" ref="day-of-week" name="day-of-week">
                <a-select
                  v-model:value="form['day-of-week']"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="(opt, optIndex) in dayOfWeek" :key="optIndex" :label="opt.name || opt.description">
                    {{ opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='monthly'">
              <a-form-item :label="$t('label.day.of.month')" ref="day-of-month" name="day-of-month">
                <a-select
                  v-model:value="form['day-of-month']"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }">
                  <a-select-option v-for="opt in dayOfMonth" :key="opt.name" :label="opt.name || opt.description">
                    {{ opt.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.timezone')" ref="timezone" name="timezone">
                <a-select
                  showSearch
                  v-model:value="form.timezone"
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  :loading="fetching">
                  <a-select-option v-for="opt in timeZoneMap" :key="opt.id" :label="opt.name || opt.description">
                    {{ opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ $t('label.cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              ref="submit"
              type="primary"
              htmlType="submit">
              {{ $t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { timeZone } from '@/utils/timezone'
import { mixinForm } from '@/utils/mixin'
import debounce from 'lodash/debounce'

export default {
  name: 'FormSchedule',
  mixins: [mixinForm],
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    dataSource: {
      type: Object,
      required: true
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)

    return {
      dayOfWeek: [],
      dayOfMonth: [],
      timeZoneMap: [],
      fetching: false,
      actionLoading: false,
      listDayOfWeek: ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    }
  },
  created () {
    this.initForm()
    this.fetchTimeZone()
  },
  inject: ['refreshSchedule', 'closeSchedule'],
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        intervaltype: 'hourly'
      })
      this.rules = reactive({
        time: [{ type: 'number', required: true, message: this.$t('message.error.required.input') }],
        timeSelect: [{ type: 'object', required: true, message: this.$t('message.error.time') }],
        'day-of-week': [{ type: 'number', required: true, message: `${this.$t('message.error.select')}` }],
        'day-of-month': [{ required: true, message: `${this.$t('message.error.select')}` }],
        timezone: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    fetchTimeZone (value) {
      this.timeZoneMap = []
      this.fetching = true

      timeZone(value).then(json => {
        this.timeZoneMap = json
        this.fetching = false
      })
    },
    fetchDayOfWeek () {
      this.dayOfWeek = []

      for (const index in this.listDayOfWeek) {
        const dayName = this.listDayOfWeek[index]
        this.dayOfWeek.push({
          id: dayName,
          name: this.$t('label.' + dayName)
        })
      }
    },
    fetchDayOfMonth () {
      this.dayOfMonth = []
      const maxDayOfMonth = 28
      for (let num = 1; num <= maxDayOfMonth; num++) {
        this.dayOfMonth.push({
          id: num,
          name: num
        })
      }
    },
    handleChangeIntervalType (e) {
      switch (this.form.intervaltype) {
        case 'weekly':
          this.fetchDayOfWeek()
          break
        case 'monthly':
          this.intervalValue = 'MONTHLY'
          this.fetchDayOfMonth()
          break
        default:
          break
      }
    },
    handleSubmit (e) {
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {}
        params.virtualmachineid = this.resource.id
        params.intervaltype = values.intervaltype
        params.timezone = values.timezone
        switch (values.intervaltype) {
          case 'hourly':
            params.schedule = values.time
            break
          case 'daily':
            params.schedule = values.timeSelect.format('mm:HH')
            break
          case 'weekly':
            params.schedule = [values.timeSelect.format('mm:HH'), (values['day-of-week'] + 1)].join(':')
            break
          case 'monthly':
            params.schedule = [values.timeSelect.format('mm:HH'), values['day-of-month']].join(':')
            break
        }
        this.actionLoading = true
        api('createBackupSchedule', params).then(json => {
          this.$notification.success({
            message: this.$t('label.scheduled.backups'),
            description: this.$t('message.success.config.backup.schedule')
          })
          this.refreshSchedule()
          this.resetForm()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    resetForm () {
      this.formRef.value.resetFields()
      this.form.intervaltype = 'hourly'
      this.tags = []
    },
    closeAction () {
      this.closeSchedule()
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  .ant-tag {
    margin-bottom: 10px;
  }

  :deep(.custom-time-select) .ant-time-picker {
    width: 100%;
  }

  :deep(.ant-divider-horizontal) {
    margin-top: 0;
  }
}

.form {
  margin: 10px 0;
}
</style>
