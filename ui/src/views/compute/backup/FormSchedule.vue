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
      <div class="form">
        <a-form
          :form="form"
          layout="vertical"
          @submit="handleSubmit">
          <a-row :gutter="12">
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.intervaltype')">
                <a-radio-group
                  v-decorator="['intervaltype', {
                    initialValue: intervalType
                  }]"
                  buttonStyle="solid"
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
            <a-col :md="24" :lg="12" v-if="intervalType==='hourly'">
              <a-form-item :label="$t('label.time')">
                <a-tooltip
                  placement="right"
                  :title="$t('label.minute.past.hour')">
                  <a-input-number
                    style="width: 100%"
                    v-decorator="['time', {
                      rules: [{required: true, message: $t('message.error.required.input')}]
                    }]"
                    :min="1"
                    :max="59"
                    autoFocus />
                </a-tooltip>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="['daily', 'weekly', 'monthly'].includes(intervalType)">
              <a-form-item
                class="custom-time-select"
                :label="$t('label.time')">
                <a-time-picker
                  use12Hours
                  format="h:mm A"
                  v-decorator="['timeSelect', {
                    rules: [{
                      type: 'object',
                      required: true,
                      message: $t('message.error.time')
                    }]
                  }]" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="intervalType==='weekly'">
              <a-form-item :label="$t('label.day.of.week')">
                <a-select
                  v-decorator="['day-of-week', {
                    rules: [{
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }]
                  }]" >
                  <a-select-option v-for="(opt, optIndex) in dayOfWeek" :key="optIndex">
                    {{ opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="intervalType==='monthly'">
              <a-form-item :label="$t('label.day.of.month')">
                <a-select
                  v-decorator="['day-of-month', {
                    rules: [{
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }]
                  }]">
                  <a-select-option v-for="opt in dayOfMonth" :key="opt.name">
                    {{ opt.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.timezone')">
                <a-select
                  showSearch
                  v-decorator="['timezone', {
                    rules: [{
                      required: true,
                      messamessage: `${this.$t('message.error.select')}`
                    }]
                  }]"
                  :loading="fetching">
                  <a-select-option v-for="opt in timeZoneMap" :key="opt.id">
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
              {{ this.$t('label.cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              type="primary"
              @click="handleSubmit">
              {{ this.$t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'

export default {
  name: 'FormSchedule',
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
      intervalValue: 0,
      intervalType: 'hourly',
      listDayOfWeek: ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.fetchTimeZone()
  },
  inject: ['refreshSchedule', 'closeSchedule'],
  methods: {
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
      this.intervalType = e.target.value
      this.resetForm()

      switch (this.intervalType) {
        case 'hourly':
          this.intervalValue = 'HOURLY'
          break
        case 'daily':
          this.intervalValue = 'DAILY'
          break
        case 'weekly':
          this.intervalValue = 'WEEKLY'
          this.fetchDayOfWeek()
          break
        case 'monthly':
          this.intervalValue = 'MONTHLY'
          this.fetchDayOfMonth()
          break
      }
    },
    handleSubmit (e) {
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }
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
      })
    },
    resetForm () {
      this.form.setFieldsValue({
        time: undefined,
        timezone: undefined,
        timeSelect: undefined,
        'day-of-week': undefined,
        'day-of-month': undefined
      })
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

  /deep/.custom-time-select .ant-time-picker {
    width: 100%;
  }

  /deep/.ant-divider-horizontal {
    margin-top: 0;
  }
}

.form {
  margin: 10px 0;
}

.action-button {
  margin-top: 20px;
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
