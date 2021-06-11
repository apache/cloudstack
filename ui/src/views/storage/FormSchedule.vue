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
      <a-alert type="warning">
        <span slot="message" v-html="$t('label.header.volume.snapshot')" />
      </a-alert>
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
            <a-col :md="24" :lg="12" v-if="intervalType==='hourly'">
              <a-form-item :label="$t('label.time')">
                <a-tooltip
                  placement="right"
                  :title="$t('label.minute.past.hour')">
                  <a-input-number
                    style="width: 100%"
                    v-decorator="['time', {
                      rules: [{required: true, message: `${this.$t('message.error.required.input')}`}]
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
            <a-col :md="24" :lg="12">
              <a-form-item :label="$t('label.keep')">
                <a-tooltip
                  placement="right"
                  :title="$t('label.snapshots')">
                  <a-input-number
                    style="width: 100%"
                    v-decorator="['maxsnaps', {
                      rules: [{ required: true, message: $t('message.error.required.input')}]
                    }]"
                    :min="1"
                    :max="8" />
                </a-tooltip>
              </a-form-item>
            </a-col>
            <a-col :md="24" :lg="24">
              <a-form-item :label="$t('label.timezone')">
                <a-select
                  showSearch
                  v-decorator="['timezone', {
                    rules: [{
                      required: true,
                      message: `${this.$t('message.error.select')}`
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
          <a-divider/>
          <div class="tagsTitle">{{ $t('label.tags') }}</div>
          <div>
            <template v-for="(tag, index) in tags">
              <a-tag :key="index" :closable="'deleteTags' in $store.getters.apis" :afterClose="() => handleDeleteTag(tag)">
                {{ tag.key }} = {{ tag.value }}
              </a-tag>
            </template>
            <div v-if="inputVisible">
              <a-input-group
                type="text"
                size="small"
                @blur="handleInputConfirm"
                @keyup.enter="handleInputConfirm"
                compact>
                <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 100px; text-align: center" :placeholder="$t('label.key')" />
                <a-input style=" width: 30px; border-left: 0; pointer-events: none; backgroundColor: #fff" placeholder="=" disabled />
                <a-input :value="inputValue" @change="handleValueChange" style="width: 100px; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
                <tooltip-button :tooltip="$t('label.ok')" icon="check" size="small" @click="handleInputConfirm" />
                <tooltip-button :tooltip="$t('label.cancel')" icon="close" size="small" @click="inputVisible=false" />
              </a-input-group>
            </div>
            <a-tag v-else @click="showInput" style="background: #fff; borderStyle: dashed;">
              <a-icon type="plus" /> {{ $t('label.new.tag') }}
            </a-tag>
          </div>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ this.$t('label.cancel') }}
            </a-button>
            <a-button
              v-if="handleShowButton()"
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
import TooltipButton from '@/components/view/TooltipButton'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'

export default {
  name: 'FormSchedule',
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
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.volumeId = this.resource.id
    this.fetchTimeZone()
  },
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

      switch (this.intervalType) {
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
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }

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
      this.form.setFieldsValue({
        time: undefined,
        timezone: undefined,
        timeSelect: undefined,
        maxsnaps: undefined,
        'day-of-week': undefined,
        'day-of-month': undefined
      })
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

.tagsTitle {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
