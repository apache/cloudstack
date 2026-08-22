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
        {{ scheduleToEdit === null ? $t('label.header.backup.schedule') : null }}
      </label>
      <div class="form" v-ctrl-enter="handleSubmit">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit">
          <a-row :gutter="12">
            <a-col :md="24" :lg="24" v-if="scheduleToEdit === null">
              <a-form-item :label="$t('label.intervaltype')" ref="intervaltype" name="intervaltype">
                <a-radio-group
                  v-model:value="form.intervaltype"
                  button-style="solid"
                  @change="handleChangeIntervalType">
                  <a-radio-button value="hourly" :disabled="isIntervalDisabled('hourly')">
                    {{ $t('label.hourly') }}
                  </a-radio-button>
                  <a-radio-button value="daily" :disabled="isIntervalDisabled('daily')">
                    {{ $t('label.daily') }}
                  </a-radio-button>
                  <a-radio-button value="weekly" :disabled="isIntervalDisabled('weekly')">
                    {{ $t('label.weekly') }}
                  </a-radio-button>
                  <a-radio-button value="monthly" :disabled="isIntervalDisabled('monthly')">
                    {{ $t('label.monthly') }}
                  </a-radio-button>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='hourly'">
              <a-form-item :label="$t('label.time')" ref="time" name="time">
                <a-input-number
                  style="width: 100%"
                  :disabled="scheduleToEdit === null && isIntervalDisabled(form.intervaltype)"
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
                  :disabled="scheduleToEdit === null && isIntervalDisabled(form.intervaltype)"
                  v-model:value="form.timeSelect"
                  style="width: 100%;" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='weekly'">
              <a-form-item :label="$t('label.day.of.week')" ref="day-of-week" name="day-of-week">
                <a-select
                  v-model:value="form['day-of-week']"
                  showSearch
                  :disabled="scheduleToEdit === null && isIntervalDisabled(form.intervaltype)"
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
                  :disabled="scheduleToEdit === null && isIntervalDisabled(form.intervaltype)"
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
            <a-col :md="24" :lg="12">
              <a-form-item :label="$t('label.keep')" name="maxbackups" ref="maxbackups">
                <a-tooltip
                  placement="right"
                  :title="$t('label.maxbackups.to.retain')">
                  <a-input-number
                    style="width: 100%"
                    v-model:value="form.maxbackups"
                    :min="0" />
                </a-tooltip>
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
            <a-col :md="24" :lg="12">
              <a-form-item v-if="isQuiesceVmSupported" name="quiescevm" ref="quiescevm">
                <a-switch v-model:checked="form.quiescevm"/>
                <template #label>
                  <tooltip-label :title="$t('label.quiescevm')" :tooltip="apiParams.quiescevm.description"/>
                </template>
              </a-form-item>
              <a-form-item name="isolated" ref="isolated">
                <template #label>
                  <tooltip-label
                    :title="$t('label.isolated')"
                    :tooltip="apiParams.isolated?.description"
                  />
                </template>
                <a-switch v-model:checked="form.isolated" />
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
import { getAPI, postAPI } from '@/api'
import { timeZone } from '@/utils/timezone'
import { mixinForm } from '@/utils/mixin'
import debounce from 'lodash/debounce'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import dayjs from 'dayjs'

export default {
  name: 'FormSchedule',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    dataSource: {
      type: Array,
      required: true
    },
    resource: {
      type: Object,
      required: true
    },
    submitFn: {
      type: Function,
      default: null
    },
    scheduleToEdit: {
      type: Object,
      required: false,
      default: null
    }
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)

    return {
      dayOfWeek: [],
      dayOfMonth: [],
      timeZoneMap: [],
      fetching: false,
      backupProvider: null,
      backupOffering: null,
      maxSchedType: {
        HOURLY: 1,
        DAILY: 1,
        WEEKLY: 1,
        MONTHLY: 1
      },
      actionLoading: false,
      listDayOfWeek: ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    }
  },
  beforeCreate () {
    this.apiToCall = this.scheduleToEdit !== null ? 'updateBackupSchedule' : 'createBackupSchedule'
    this.apiParams = this.$getApiParams(this.apiToCall)
  },
  created () {
    this.initForm()
    this.fetchTimeZone()
    this.fetchBackupOffering()
  },
  mounted () {
    if (this.form.intervaltype && this.isIntervalDisabled(this.form.intervaltype)) {
      const nextAvailable = this.getNextAvailableIntervalType()
      if (nextAvailable) {
        this.form.intervaltype = nextAvailable
        this.handleChangeIntervalType()
      }
    }
    this.populateFormWithScheduleData()
  },
  watch: {
    dataSource: {
      handler () {
        if (this.form.intervaltype && this.getNextAvailableIntervalType && this.isIntervalDisabled(this.form.intervaltype)) {
          const nextAvailable = this.getNextAvailableIntervalType()
          if (nextAvailable) {
            this.form.intervaltype = nextAvailable
            this.handleChangeIntervalType()
          }
        }
      },
      deep: true
    },
    'form.intervaltype' (newVal) {
      if (newVal && this.getNextAvailableIntervalType && this.isIntervalDisabled(newVal)) {
        const nextAvailable = this.getNextAvailableIntervalType()
        if (nextAvailable) {
          this.form.intervaltype = nextAvailable
          this.handleChangeIntervalType()
        }
      }
    },
    scheduleToEdit () {
      this.populateFormWithScheduleData()
      this.handleChangeIntervalType()
    }
  },
  inject: ['refreshSchedule', 'closeSchedule'],
  computed: {
    isQuiesceVmSupported () {
      return this.$isBackupProviderSupportsQuiesceVm(this.backupProvider)
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        intervaltype: 'hourly',
        isolated: false
      })
      this.rules = reactive({
        time: [{ type: 'number', required: true, message: this.$t('message.error.required.input') }],
        timeSelect: [{ type: 'object', required: true, message: this.$t('message.error.time') }],
        'day-of-week': [{ type: 'number', required: true, message: `${this.$t('message.error.select')}` }],
        'day-of-month': [{ required: true, message: `${this.$t('message.error.select')}` }],
        timezone: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    getMaxSchedules () {
      for (const [key, value] of Object.entries(this.maxSchedType)) {
        let typeLimit = value
        if (this.backupOffering.backupofferingdetails) {
          typeLimit = this.backupOffering.backupofferingdetails[key.toUpperCase()] ?? 1
        }
        this.maxSchedType[key] = typeLimit
      }
    },
    fetchBackupOffering () {
      getAPI('listBackupOfferings', { id: this.resource.backupofferingid }).then(json => {
        if (json.listbackupofferingsresponse && json.listbackupofferingsresponse.backupoffering) {
          this.backupOffering = json.listbackupofferingsresponse.backupoffering[0]
          this.backupProvider = this.backupOffering.provider
          this.getMaxSchedules()
        }
      }).catch(error => {
        this.$notifyError(error)
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
    populateFormWithScheduleData () {
      if (!this.scheduleToEdit) return
      const { quiescevm, isolated, schedule, timezone, intervaltype, maxbackups } = this.scheduleToEdit
      const lowerIntervalType = intervaltype.toLowerCase()

      this.form.intervaltype = lowerIntervalType
      this.form.quiescevm = quiescevm ?? false
      this.form.isolated = isolated ?? false
      this.form.timezone = timezone
      this.form.maxbackups = maxbackups !== 0 ? maxbackups : null

      if (lowerIntervalType === 'hourly') {
        this.form.time = Number(schedule)
        return
      }

      if (lowerIntervalType === 'daily') {
        this.form.timeSelect = dayjs(schedule, 'mm:HH')
        return
      }

      const [minute, hour, day] = schedule.split(':')
      this.form.timeSelect = dayjs(`${minute}:${hour}`, 'mm:HH')

      if (lowerIntervalType === 'weekly') {
        this.form['day-of-week'] = day - 1
      }

      if (lowerIntervalType === 'monthly') {
        this.form['day-of-month'] = day
      }
    },
    handleChangeIntervalType () {
      if (this.form.intervaltype === 'weekly') {
        this.fetchDayOfWeek()
      } else if (this.form.intervaltype === 'monthly') {
        this.fetchDayOfMonth()
      }
    },
    getNextAvailableIntervalType () {
      const intervalTypes = ['hourly', 'daily', 'weekly', 'monthly']

      for (let i = 0; i <= intervalTypes.length; i++) {
        const nextIntervalType = intervalTypes[i]

        if (!this.isIntervalDisabled(nextIntervalType)) {
          return nextIntervalType
        }
      }
      return null
    },
    isIntervalDisabled (intervalType) {
      intervalType = intervalType.toUpperCase()
      if (this.dataSource?.length === 0 && this.maxSchedType[intervalType] !== 0) {
        return false
      }
      if (this.maxSchedType[intervalType] < 0) {
        return false
      }
      const dataSource = this.dataSource.filter(item => item.intervaltype === intervalType)
      return dataSource && dataSource.length >= this.maxSchedType[intervalType]
    },
    handleSubmit (e) {
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        console.log(formRaw)
        const values = this.handleRemoveFields(formRaw)
        const params = {}
        params.virtualmachineid = this.resource.id
        params.intervaltype = this.scheduleToEdit?.intervaltype.toUpperCase() ?? values.intervaltype.toUpperCase()
        params.maxbackups = values.maxbackups
        params.timezone = values.timezone
        if (values.quiescevm) {
          params.quiescevm = values.quiescevm
        }
        params.isolated = values.isolated
        switch (params.intervaltype) {
          case 'HOURLY':
            params.schedule = values.time
            break
          case 'DAILY':
            params.schedule = values.timeSelect.format('mm:HH')
            break
          case 'WEEKLY':
            params.schedule = [values.timeSelect.format('mm:HH'), (values['day-of-week'] + 1)].join(':')
            break
          case 'MONTHLY':
            params.schedule = [values.timeSelect.format('mm:HH'), values['day-of-month']].join(':')
            break
        }
        if (this.submitFn) {
          this.submitFn(params)
          this.resetForm()
          return
        }
        if (this.scheduleToEdit !== null) {
          params.id = this.scheduleToEdit.id
        }
        this.actionLoading = true
        postAPI(this.apiToCall, params).then(json => {
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
      }).finally(() => {
        if (this.scheduleToEdit !== null) {
          this.closeAction()
        }
      })
    },
    resetForm () {
      this.formRef.value.resetFields()
      this.form.intervaltype = 'hourly'
      this.tags = []
    },
    closeAction () {
      this.closeSchedule()
      this.resetForm()
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
