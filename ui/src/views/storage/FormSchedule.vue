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
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <a-alert type="warning">
        <template #message>
          <div v-html="$t('label.header.volume.snapshot')"></div>
        </template>
      </a-alert>
      <div class="form">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
         >
          <a-row :gutter="12">
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.intervaltype')" name="intervaltype" ref="intervaltype">
                <a-radio-group
                  v-model:value="form.intervaltype"
                  buttonStyle="solid"
                  @change="handleChangeIntervalType">
                  <a-radio-button value="hourly" :disabled="handleVisibleInterval(0)">
                    {{ $t('label.hourly') }}
                  </a-radio-button>
                  <a-radio-button value="daily" :disabled="handleVisibleInterval(1)">
                    {{ $t('label.daily') }}
                  </a-radio-button>
                  <a-radio-button value="weekly" :disabled="handleVisibleInterval(2)">
                    {{ $t('label.weekly') }}
                  </a-radio-button>
                  <a-radio-button value="monthly" :disabled="handleVisibleInterval(3)">
                    {{ $t('label.monthly') }}
                  </a-radio-button>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='hourly'">
              <a-form-item :label="$t('label.time')" name="time" ref="time">
                <a-tooltip
                  placement="right"
                  :title="$t('label.minute.past.hour')">
                  <a-input-number
                    style="width: 100%"
                    v-model:value="form.time"
                    :min="1"
                    :max="59"
                    v-focus="true" />
                </a-tooltip>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="['daily', 'weekly', 'monthly'].includes(form.intervaltype)">
              <a-form-item
                class="custom-time-select"
                :label="$t('label.time')"
                name="timeSelect"
                ref="timeSelect">
                <a-time-picker
                  use12Hours
                  format="h:mm A"
                  v-model:value="form.timeSelect"
                  style="width: 100%;" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12" v-if="form.intervaltype==='weekly'">
              <a-form-item :label="$t('label.day.of.week')" name="day-of-week" ref="day-of-week">
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
                  optionFilterProp="value"
                  :filterOption="(input, option) => {
                    return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="opt in dayOfMonth" :key="opt.name">
                    {{ opt.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="12">
              <a-form-item :label="$t('label.keep')" name="maxsnaps" ref="maxsnaps">
                <a-tooltip
                  placement="right"
                  :title="$t('label.snapshots')">
                  <a-input-number
                    style="width: 100%"
                    v-model:value="form.maxsnaps"
                    :min="1" />
                </a-tooltip>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.timezone')" ref="timezone" name="timezone">
                <a-select
                  v-model:value="form.timezone"
                  :loading="fetching"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="opt in timeZoneMap" :key="opt.id" :label="opt.name || opt.description">
                    {{ opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-divider/>
          <div class="tagsTitle">{{ $t('label.tags') }}</div>
          <div>
            <div v-for="(tag, index) in tags" :key="index">
              <a-tag :key="index" :closable="'deleteTags' in $store.getters.apis" @close="() => handleDeleteTag(tag)">
                {{ tag.key }} = {{ tag.value }}
              </a-tag>
            </div>
            <div v-if="inputVisible">
              <a-input-group
                type="text"
                size="small"
                @blur="handleInputConfirm"
                @keyup.enter="handleInputConfirm"
                compact>
                <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 100px; text-align: center" :placeholder="$t('label.key')" />
                <a-input
                  class="tag-disabled-input"
                  style=" width: 30px; border-left: 0; pointer-events: none; text-align: center"
                  placeholder="="
                  disabled />
                <a-input :value="inputValue" @change="handleValueChange" style="width: 100px; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
                <tooltip-button :tooltip="$t('label.ok')" icon="check-outlined" size="small" @onClick="handleInputConfirm" />
                <tooltip-button :tooltip="$t('label.cancel')" icon="close-outlined" size="small" @onClick="inputVisible=false" />
              </a-input-group>
            </div>
            <a-tag v-else @click="showInput" class="btn-add-tag" style="borderStyle: dashed;">
              <plus-outlined /> {{ $t('label.new.tag') }}
            </a-tag>
          </div>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ $t('label.cancel') }}
            </a-button>
            <a-button
              v-if="handleShowButton()"
              :loading="actionLoading"
              type="primary"
              ref="submit"
              @click="handleSubmit">
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
import TooltipButton from '@/components/widgets/TooltipButton'
import { timeZone } from '@/utils/timezone'
import { mixinForm } from '@/utils/mixin'
import debounce from 'lodash/debounce'

export default {
  name: 'FormSchedule',
  mixins: [mixinForm],
  components: {
    TooltipButton
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
    }
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)

    return {
      actionLoading: false,
      volumeId: '',
      inputKey: '',
      inputVisible: '',
      inputValue: '',
      intervalType: 'hourly',
      intervalValue: 0,
      tags: [],
      dayOfWeek: [],
      dayOfMonth: [],
      timeZoneMap: [],
      fetching: false,
      listDayOfWeek: ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    }
  },
  created () {
    this.initForm()
    this.volumeId = this.resource.id
    this.fetchTimeZone()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        intervaltype: 'hourly',
        time: undefined,
        timeSelect: undefined,
        'day-of-week': undefined,
        'day-of-month': undefined,
        maxsnaps: undefined,
        timezone: undefined
      })
      this.rules = reactive({
        time: [{ type: 'number', required: true, message: this.$t('message.error.required.input') }],
        timeSelect: [{ type: 'object', required: true, message: this.$t('message.error.time') }],
        'day-of-week': [{ type: 'number', required: true, message: `${this.$t('message.error.select')}` }],
        'day-of-month': [{ required: true, message: `${this.$t('message.error.select')}` }],
        maxsnaps: [{ required: true, message: this.$t('message.error.required.input') }],
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
    handleChangeIntervalType () {
      switch (this.form.intervaltype) {
        case 'hourly':
          this.intervalValue = 0
          break
        case 'daily':
          this.intervalValue = 1
          break
        case 'weekly':
          this.intervalValue = 2
          this.fetchDayOfWeek()
          break
        case 'monthly':
          this.intervalValue = 3
          this.fetchDayOfMonth()
          break
      }
    },
    handleVisibleInterval (intervalType) {
      if (this.dataSource.length === 0) {
        return false
      }
      const dataSource = this.dataSource.filter(item => item.intervaltype === intervalType)
      if (dataSource && dataSource.length > 0) {
        return true
      }
      return false
    },
    handleShowButton () {
      if (this.dataSource.length === 0) {
        return true
      }
      const dataSource = this.dataSource.filter(item => item.intervaltype === this.intervalValue)
      if (dataSource && dataSource.length > 0) {
        return false
      }
      return true
    },
    handleKeyChange (e) {
      this.inputKey = e.target.value
    },
    handleValueChange (e) {
      this.inputValue = e.target.value
    },
    handleInputConfirm () {
      this.tags.push({
        key: this.inputKey,
        value: this.inputValue
      })
      this.inputVisible = false
      this.inputKey = ''
      this.inputValue = ''
    },
    handleDeleteTag (tag) {
    },
    handleSubmit (e) {
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        let params = {}
        params.volumeid = this.volumeId
        params.intervaltype = values.intervaltype
        params.timezone = values.timezone
        params.maxsnaps = values.maxsnaps
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
        for (let i = 0; i < this.tags.length; i++) {
          const formattedTagData = {}
          const tag = this.tags[i]
          formattedTagData['tags[' + i + '].key'] = tag.key
          formattedTagData['tags[' + i + '].value'] = tag.value
          params = Object.assign({}, params, formattedTagData)
        }
        this.actionLoading = true
        api('createSnapshotPolicy', params).then(json => {
          this.$emit('refresh')
          this.$notification.success({
            message: this.$t('label.action.recurring.snapshot'),
            description: this.$t('message.success.recurring.snapshot')
          })
          this.resetForm()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      })
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    resetForm () {
      this.form.time = undefined
      this.form.timezone = undefined
      this.form.timeSelect = undefined
      this.form.maxsnaps = undefined
      this.form['day-of-week'] = undefined
      this.form['day-of-month'] = undefined
      this.tags = []
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
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

.tagsTitle {
  font-weight: 500;
  margin-bottom: 12px;
}
</style>
