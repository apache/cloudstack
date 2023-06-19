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
    <a-button
      type="dashed"
      style="width: 100%; margin-bottom: 10px"
      @click="showAddModal"
      :loading="loading"
      :disabled="!('createVMSchedule' in $store.getters.apis)">
      <template #icon><plus-outlined/></template> {{ $t('label.schedule.add') }}
    </a-button>
    <list-view
      :loading="tabLoading"
      :columns="columns"
      :items="schedules"
      :columnKeys="columnKeys"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @update-vm-schedule="updateVMSchedule"
      @remove-vm-schedule="removeVMSchedule"
      @refresh="this.fetchData"/>
    <a-pagination
      class="row-element"
      style="margin-top: 10px"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="pageSizeOptions"
      @change="changePage"
      @showSizeChange="changePage"
      showSizeChanger
      showQuickJumper>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>

  <a-modal
    :visible="showModal"
    :title="$t('label.schedule')"
    :maskClosable="false"
    :closable="true"
    :footer="null"
    @cancel="closeModal">
    <a-form
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="submitForm"
      v-ctrl-enter="submitForm">
      <a-form-item name="description" ref="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-input
          v-model:value="form.description"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="action" ref="action">
        <template #label>
          <tooltip-label :title="$t('label.action')" :tooltip="apiParams.action.description"/>
        </template>
        <a-radio-group
          v-model:value="form.action"
          button-style="solid"
          :disabled="isEdit">
          <a-radio-button v-for="action in actions" :key="action.id" :value="action.value">
            {{ $t(action.label) }}
          </a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item name="schedule" ref="schedule">
        <template #label>
          <tooltip-label :title="$t('label.schedule')" :tooltip="apiParams.schedule.description"/>
        </template>
        <label>{{ $t('label.advanced.mode') }}</label>
        <a-switch
          v-model:checked="form.useCronFormat"
          >
        </a-switch>
        <br/>
        <span v-if="!form.useCronFormat">
        <cron-ant
          v-model="form.schedule"
          :periods="periods"
          :button-props="{ type: 'primary', size: 'small', disabled: form.useCronFormat }"
          @error="error=$event"/>
      </span>
      <span v-if="form.useCronFormat">
        <label>{{ generateHumanReadableSchedule(form.schedule) }}</label>
        <br/>
      </span>
        <a-input
          :addonBefore="$t('label.cron')"
          v-model:value="form.schedule"
          :disabled="!form.useCronFormat"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="timezone" ref="timezone">
        <template #label>
          <tooltip-label :title="$t('label.timezone')" :tooltip="apiParams.timezone.description"/>
        </template>
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
      <a-form-item name="startDate" ref="startDate">
        <template #label>
          <tooltip-label :title="$t('label.start.date.and.time')" :tooltip="apiParams.startdate.description"/>
        </template>
        <a-date-picker
          v-model:value="form.startDate"
          show-time
          :locale="this.$i18n.locale"
          :placeholder="$t('message.select.start.date.and.time')"/>
      </a-form-item>
      <a-form-item name="endDate" ref="endDate">
        <template #label>
          <tooltip-label :title="$t('label.end.date.and.time')" :tooltip="apiParams.enddate.description"/>
        </template>
        <a-date-picker
          v-model:value="form.endDate"
          show-time
          :locale="this.$i18n.locale"
          :placeholder="$t('message.select.end.date.and.time')"/>
      </a-form-item>
      <a-form-item name="enabled" ref="enabled">
        <template #label>
          <tooltip-label :title="$t('label.enabled')" :tooltip="apiParams.enabled.description"/>
        </template>
        <a-switch
          v-model:checked="form.enabled">
        </a-switch>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button
          :loading="loading"
          @click="closeModal">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button
          :loading="loading"
          ref="submit"
          type="primary"
          htmlType="submit">
          {{ $t('label.ok') }}
        </a-button>
      </div>
    </a-form>
  </a-modal>
</template>

<script>

import { reactive, ref, toRaw } from 'vue'
import { api } from '@/api'
import ListView from '@/components/view/ListView'
import Status from '@/components/widgets/Status'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { mixinForm } from '@/utils/mixin'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'
import cronstrue from 'cronstrue/i18n'
import moment from 'moment-timezone'

export default {
  name: 'InstanceSchedules',
  mixins: [mixinForm],
  components: {
    Status,
    ListView,
    TooltipLabel
  },
  props: {
    virtualmachine: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)
    return {
      tabLoading: false,
      columnKeys: ['action', 'enabled', 'description', 'schedule', 'timezone', 'startdate', 'enddate', 'created', 'vmScheduleActions'],
      selectedColumnKeys: [],
      columns: [],
      schedules: [],
      timeZoneMap: [],
      actions: [
        { value: 'START', label: 'label.start' },
        { value: 'STOP', label: 'label.stop' },
        { value: 'REBOOT', label: 'label.reboot' },
        { value: 'FORCE_STOP', label: 'label.force.stop' },
        { value: 'FORCE_REBOOT', label: 'label.force.reboot' }
      ],
      periods: [
        { id: 'year', value: ['month', 'day', 'dayOfWeek', 'hour', 'minute'] },
        { id: 'month', value: ['day', 'dayOfWeek', 'hour', 'minute'] },
        { id: 'week', value: ['dayOfWeek', 'hour', 'minute'] },
        { id: 'day', value: ['hour', 'minute'] }
      ],
      page: 1,
      pageSize: 20,
      totalCount: 0,
      showModal: false,
      isSubmitted: false,
      isEdit: false,
      error: '',
      pattern: 'YYYY-MM-DD HH:mm:ss'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVMSchedule')
  },
  computed: {
    pageSizeOptions () {
      var sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
  created () {
    this.selectedColumnKeys = this.columnKeys
    this.updateColumns()
    this.pageSize = this.pageSizeOptions[0] * 1
    this.initForm()
    this.fetchData()
    this.fetchTimeZone()
  },
  watch: {
    virtualmachine: {
      handler () {
        this.fetchSchedules()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        action: 'START',
        schedule: '* * * * *',
        description: '',
        timezone: 'UTC',
        startDate: '',
        endDate: '',
        enabled: true,
        useCronFormat: false
      })
      this.rules = reactive({
        schedule: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        action: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        timezone: [{ required: true, message: `${this.$t('message.error.select')}` }],
        startDate: [{ required: false, message: `${this.$t('message.error.select')}` }],
        endDate: [{ required: false, message: `${this.$t('message.error.select')}` }]
      })
    },
    createVMSchedule (schedule) {
      this.resetForm()
      this.showAddModal()
    },
    removeVMSchedule (schedule) {
      api('deleteVMSchedule', {
        id: schedule.id,
        virtualmachineid: this.virtualmachine.id
      }).then(() => {
        if (this.totalCount - 1 === this.pageSize * (this.page - 1)) {
          this.page = this.page - 1 > 0 ? this.page - 1 : 1
        }
        const message = `${this.$t('label.removing')} ${schedule.description}`
        this.$message.success(message)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.remove.vm.schedule'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.remove.vm.schedule')
        })
      }).finally(() => {
        this.fetchData()
      })
    },
    updateVMSchedule (schedule) {
      this.resetForm()
      this.isEdit = true
      Object.assign(this.form, schedule)
      // Some weird issue when we directly pass in the moment with tz object
      this.form.startDate = moment(moment(schedule.startdate).tz(schedule.timezone).format(this.pattern))
      this.form.endDate = schedule.enddate ? moment(moment(schedule.enddate).tz(schedule.timezone).format(this.pattern)) : ''
      this.showAddModal()
    },
    showAddModal () {
      this.showModal = true
    },
    submitForm () {
      if (this.isSubmitted) return
      this.isSubmitted = true
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        var params = {
          description: values.description,
          schedule: values.schedule,
          timezone: values.timezone,
          action: values.action,
          virtualmachineid: this.virtualmachine.id,
          enabled: values.enabled,
          startdate: (values.startDate) ? values.startDate.format(this.pattern) : null,
          enddate: (values.endDate) ? values.endDate.format(this.pattern) : null
        }
        let command = null
        if (this.form.id === null || this.form.id === undefined) {
          command = 'createVMSchedule'
        } else {
          params.id = this.form.id
          command = 'updateVMSchedule'
        }

        api(command, params).then(response => {
          this.$notification.success({
            message: this.$t('label.schedule'),
            description: this.$t('message.success.config.vm.schedule')
          })
          this.isSubmitted = false
          this.fetchData()
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
          this.isSubmitted = false
        })
      }).catch(error => {
        this.$notifyError(error)
        if (error.errorFields !== undefined) {
          this.formRef.value.scrollToField(error.errorFields[0].name)
        }
      }).finally(() => {
        this.isSubmitted = false
      })
    },
    resetForm () {
      this.isEdit = false
      if (this.formRef.value) {
        this.formRef.value.resetFields()
      }
    },
    fetchTimeZone (value) {
      this.timeZoneMap = []
      this.fetching = true

      timeZone(value).then(json => {
        this.timeZoneMap = json
        this.fetching = false
      })
    },
    closeModal () {
      this.resetForm()
      this.initForm()
      this.showModal = false
    },
    fetchData () {
      this.fetchSchedules()
    },
    fetchSchedules () {
      this.schedules = []
      if (!this.virtualmachine.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        virtualmachineid: this.virtualmachine.id,
        listall: true
      }
      this.tabLoading = true
      api('listVMSchedule', params).then(json => {
        this.schedules = []
        this.totalCount = json?.listvmscheduleresponse?.count || 0
        this.schedules = json?.listvmscheduleresponse?.vmschedule || []
        this.tabLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    updateSelectedColumns (key) {
      if (this.selectedColumnKeys.includes(key)) {
        this.selectedColumnKeys = this.selectedColumnKeys.filter(x => x !== key)
      } else {
        this.selectedColumnKeys.push(key)
      }
      this.updateColumns()
    },
    generateHumanReadableSchedule (schedule) {
      return cronstrue.toString(schedule, { locale: this.$i18n.locale, throwExceptionOnParseError: false })
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        this.columns.push({
          key: columnKey,
          // If columnKey is 'enabled', then title is 'state'
          // If columnKey is 'startdate', then the title is `start.date.and.time`
          // else title is columnKey
          title: columnKey === 'enabled'
            ? this.$t('label.state')
            : columnKey === 'startdate'
              ? this.$t('label.start.date.and.time')
              : columnKey === 'enddate'
                ? this.$t('label.end.date.and.time')
                : this.$t('label.' + String(columnKey).toLowerCase()),
          dataIndex: columnKey
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    }
  }
}
</script>
