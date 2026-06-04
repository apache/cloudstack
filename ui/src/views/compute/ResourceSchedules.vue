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
      :disabled="!('createResourceSchedule' in $store.getters.apis)"
    >
      <template #icon><plus-outlined /></template> {{ $t('label.schedule.add') }}
    </a-button>
    <list-view
      :loading="tabLoading"
      :columns="columns"
      :items="schedules"
      :columnKeys="columnKeys"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData"
    >
      <template #scheduleActions="{ record }">
        <tooltip-button
          :tooltip="$t('label.edit')"
          :disabled="!('updateResourceSchedule' in $store.getters.apis)"
          icon="edit-outlined"
          @onClick="updateSchedule(record)"
        />
        <a-popconfirm
          :title="$t('label.delete') + ' ' + $t('label.schedule') + '?'"
          :okText="$t('label.yes')"
          :cancelText="$t('label.no')"
          @confirm="removeSchedule(record)"
        >
          <tooltip-button
            :tooltip="$t('label.remove')"
            :disabled="!('deleteResourceSchedule' in $store.getters.apis)"
            icon="delete-outlined"
            :danger="true"
            type="primary"
          />
        </a-popconfirm>
      </template>
    </list-view>
    <a-pagination
      class="row-element"
      style="margin-top: 10px"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1 + ((page - 1) * pageSize))}-${Math.min(page * pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="pageSizeOptions"
      @change="changePage"
      @showSizeChange="changePage"
      showSizeChanger
      showQuickJumper
    >
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
    @cancel="closeModal"
    @ok="submitForm"
  >
    <a-form
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="submitForm"
      v-ctrl-enter="submitForm"
    >
      <a-form-item
        name="description"
        ref="description"
        :wrapperCol="{ span: 24 }"
      >
        <template #label>
          <tooltip-label
            :title="$t('label.description')"
            :tooltip="apiParams.description.description"
          />
        </template>
        <a-input
          v-model:value="form.description"
          v-focus="true"
        />
      </a-form-item>
      <a-form-item
        v-if="actions.length > 1"
        name="action"
        ref="action"
        :wrapperCol="{ span: 24 }"
      >
        <template #label>
          <tooltip-label
            :title="$t('label.action')"
            :tooltip="apiParams.action.description"
          />
        </template>
        <a-radio-group
          v-model:value="form.action"
          button-style="solid"
          :disabled="isEdit"
        >
          <a-radio-button
            v-for="action in actions"
            :key="action.id"
            :value="action.value"
          >
            {{ $t(action.label) }}
          </a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-row
        v-if="resourceType === 'AutoScaleVmGroup'"
        justify="space-between"
      >
        <a-col :span="11">
          <a-form-item
            name="minMembers"
            ref="minMembers"
          >
            <template #label>
              <tooltip-label :title="$t('label.minmembers')" />
            </template>
            <a-input-number
              v-model:value="form.minMembers"
              :min="1"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="11">
          <a-form-item
            name="maxMembers"
            ref="maxMembers"
          >
            <template #label>
              <tooltip-label :title="$t('label.maxmembers')" />
            </template>
            <a-input-number
              v-model:value="form.maxMembers"
              :min="1"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item
        name="timezone"
        ref="timezone"
        :wrapperCol="{ span: 24 }"
      >
        <template #label>
          <tooltip-label
            :title="$t('label.timezone')"
            :tooltip="apiParams.timezone.description"
          />
        </template>
        <a-select
          showSearch
          v-model:value="form.timezone"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="fetching"
        >
          <a-select-option
            v-for="opt in timeZoneMap"
            :key="opt.id"
            :label="opt.name || opt.description"
          >
            {{ opt.name || opt.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-row justify="space-between">
        <a-col>
          <a-form-item
            name="startDate"
            ref="startDate"
          >
            <template #label>
              <tooltip-label
                :title="$t('label.start.date.and.time')"
                :tooltip="apiParams.startdate.description"
              />
            </template>
            <a-date-picker
              v-model:value="form.startDate"
              show-time
              :locale="this.$i18n.locale"
              :placeholder="$t('message.select.start.date.and.time')"
            />
          </a-form-item>
        </a-col>
        <a-col>
          <a-form-item
            name="endDate"
            ref="endDate"
          >
            <template #label>
              <tooltip-label
                :title="$t('label.end.date.and.time')"
                :tooltip="apiParams.enddate.description"
              />
            </template>
            <a-date-picker
              v-model:value="form.endDate"
              show-time
              :locale="this.$i18n.locale"
              :placeholder="$t('message.select.end.date.and.time')"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item
        name="schedule"
        ref="schedule"
        :wrapperCol="{ span: 24 }"
      >
        <template #label>
          <tooltip-label
            :title="$t('label.schedule')"
            :tooltip="apiParams.schedule.description"
          />
        </template>
        <a-row
          style="margin-bottom: 15px; text-align: center;"
          justify="space-around"
          align="middle"
        >
          <cron-ant
            v-if="!form.useCronFormat"
            v-model="form.schedule"
            :periods="periods"
            :button-props="{ type: 'primary', size: 'small', disabled: form.useCronFormat }"
            @error="error = $event"
          />
          <label
            v-if="form.useCronFormat">
            {{ generateHumanReadableSchedule(form.schedule) }}
          </label>
        </a-row>
        <a-row
          justify="space-between"
          align="middle"
        >
          <a-col>
            <label>{{ $t('label.cron.mode') }}</label>
          </a-col>
          <a-col>
            <a-switch v-model:checked="form.useCronFormat">
            </a-switch>
          </a-col>
          <a-col :span="12">
            <a-input
              :addonBefore="$t('label.cron')"
              v-model:value="form.schedule"
              :disabled="!form.useCronFormat"
              v-focus="true"
            />
          </a-col>
        </a-row>
      </a-form-item>
      <a-form-item
        name="enabled"
        ref="enabled"
        :wrapperCol="{ span: 24}"
      >
        <template #label>
          <tooltip-label
            :title="$t('label.enabled')"
            :tooltip="apiParams.enabled.description"
          />
        </template>
        <a-switch v-model:checked="form.enabled" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>

import { reactive, ref, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import ListView from '@/components/view/ListView'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { mixinForm } from '@/utils/mixin'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'
import cronstrue from 'cronstrue/i18n'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(utc)
dayjs.extend(timezone)

export default {
  name: 'ResourceSchedules',
  mixins: [mixinForm],
  components: {
    Status,
    ListView,
    TooltipButton,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)
    return {
      tabLoading: false,
      columnKeys: ['action', 'enabled', 'description', 'schedule', 'timezone', 'startdate', 'enddate', 'created', 'scheduleActions'],
      selectedColumnKeys: [],
      columns: [],
      schedules: [],
      timeZoneMap: [],
      resourceActionsMap: {
        VirtualMachine: [
          { value: 'START', label: 'label.start' },
          { value: 'STOP', label: 'label.stop' },
          { value: 'REBOOT', label: 'label.reboot' },
          { value: 'FORCE_STOP', label: 'label.force.stop' },
          { value: 'FORCE_REBOOT', label: 'label.force.reboot' }
        ],
        AutoScaleVmGroup: [
          { value: 'UPDATE', label: 'label.update.members' }
        ]
      },
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
    this.apiParams = this.$getApiParams('createResourceSchedule')
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
    },
    actions () {
      return this.resourceActionsMap[this.resourceType] || []
    }
  },
  created () {
    this.selectedColumnKeys = this.columnKeys
    if (this.resourceType === 'AutoScaleVmGroup') {
      this.columnKeys = ['enabled', 'description', 'schedule', 'minmembers',
        'maxmembers', 'timezone', 'startdate', 'enddate', 'created',
        'scheduleActions']
      this.selectedColumnKeys = [...this.columnKeys]
    }
    this.updateColumns()
    this.pageSize = this.pageSizeOptions[0] * 1
    this.initForm()
    this.fetchData()
    this.fetchTimeZone()
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.fetchSchedules()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        action: this.actions.length > 0 ? this.actions[0].value : null,
        schedule: '* * * * *',
        description: '',
        timezone: 'UTC',
        minMembers: null,
        maxMembers: null,
        startDate: '',
        endDate: '',
        enabled: true,
        useCronFormat: false
      })
      this.rules = reactive({
        schedule: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        action: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        minMembers: [{ required: this.resourceType === 'AutoScaleVmGroup', message: this.$t('message.error.required.input') }],
        maxMembers: [{ required: this.resourceType === 'AutoScaleVmGroup', message: this.$t('message.error.required.input') }],
        timezone: [{ required: true, message: `${this.$t('message.error.select')}` }],
        startDate: [{ required: false, message: `${this.$t('message.error.select')}` }],
        endDate: [{ required: false, message: `${this.$t('message.error.select')}` }]
      })
    },
    createSchedule (schedule) {
      this.resetForm()
      this.showAddModal()
    },
    removeSchedule (schedule) {
      postAPI('deleteResourceSchedule', {
        id: schedule.id,
        resourceid: this.resource.id,
        resourcetype: this.resourceType
      }).then(() => {
        if (this.totalCount - 1 === this.pageSize * (this.page - 1)) {
          this.page = this.page - 1 > 0 ? this.page - 1 : 1
        }
        const message = `${this.$t('label.removing')} ${schedule.description}`
        this.$message.success(message)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.remove.resource.schedule'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.remove.resource.schedule')
        })
      }).finally(() => {
        this.fetchData()
      })
    },
    updateSchedule (schedule) {
      this.resetForm()
      this.isEdit = true
      Object.assign(this.form, schedule)
      this.form.minMembers = schedule?.details?.minmembers ? Number(schedule.details.minmembers) : null
      this.form.maxMembers = schedule?.details?.maxmembers ? Number(schedule.details.maxmembers) : null
      // Some weird issue when we directly pass in the moment with tz object
      this.form.startDate = dayjs(schedule.startdate).tz(schedule.timezone)
      this.form.endDate = schedule.enddate ? dayjs(dayjs(schedule.enddate).tz(schedule.timezone)) : null
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
        const selectedAction = values.action || (this.actions.length === 1 ? this.actions[0].value : null)
        var params = {
          description: values.description,
          schedule: values.schedule,
          timezone: values.timezone,
          action: selectedAction,
          resourceid: this.resource.id,
          resourcetype: this.resourceType,
          enabled: values.enabled,
          startdate: (values.startDate) ? values.startDate.format(this.pattern) : null,
          enddate: (values.endDate) ? values.endDate.format(this.pattern) : null
        }
        if (this.resourceType === 'AutoScaleVmGroup') {
          params['details[0].minmembers'] = values.minMembers
          params['details[1].maxmembers'] = values.maxMembers
        }
        let command = null
        if (this.form.id === null || this.form.id === undefined) {
          command = 'createResourceSchedule'
        } else {
          params.id = this.form.id
          command = 'updateResourceSchedule'
        }

        postAPI(command, params).then(response => {
          this.$notification.success({
            message: this.$t('label.schedule'),
            description: this.$t('message.success.config.resource.schedule')
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
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        resourceid: this.resource.id,
        resourcetype: this.resourceType,
        listall: true
      }
      this.tabLoading = true
      getAPI('listResourceSchedule', params).then(json => {
        this.schedules = []
        this.totalCount = json?.listresourcescheduleresponse?.count || 0
        const rawSchedules = json?.listresourcescheduleresponse?.resourceschedule || []
        this.schedules = rawSchedules.map(s => ({
          ...s,
          minmembers: s.details?.minmembers,
          maxmembers: s.details?.maxmembers
        }))
      }).catch(error => {
        console.error(error)
        this.$notifyError(error)
      }).finally(() => {
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
      return cronstrue.toString(schedule, { locale: this.$i18n.locale, throwExceptionOnParseError: false, verbose: true })
    },
    updateColumns () {
      this.columns = []
      const columnTitleMap = {
        enabled: this.$t('label.state'),
        startdate: this.$t('label.start.date.and.time'),
        enddate: this.$t('label.end.date.and.time')
      }
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        this.columns.push({
          key: columnKey,
          title: columnTitleMap[columnKey] || this.$t('label.' + String(columnKey).toLowerCase()),
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
