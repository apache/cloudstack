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
      class="form-layout"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="submitForm"
      v-ctrl-enter="submitForm">
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          v-focus="true" />
      </a-form-item>
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
          button-style="solid">
          <a-radio-button value="START">
            {{ $t('label.start') }}
          </a-radio-button>
          <a-radio-button value="STOP">
            {{ $t('label.stop') }}
          </a-radio-button>
          <a-radio-button value="REBOOT">
            {{ $t('label.reboot') }}
          </a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item name="schedule" ref="schedule">
        <template #label>
          <tooltip-label :title="$t('label.schedule')" :tooltip="apiParams.schedule.description"/>
        </template>
        <cron-light
          v-model="form.schedule"
          @error="error=$event"/>
        <br/>
        <label>{{ $t('label.cron') }}: {{form.schedule}}</label>
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
      <a-form-item name="enabled" ref="enabled">
        <template #label>
          <tooltip-label :title="$t('label.enable')" :tooltip="apiParams.enabled.description"/>
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
import { genericCompare } from '@/utils/sort.js'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'
import moment from 'moment'

export default {
  name: 'Schedules',
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
      columnKeys: ['enabled', 'name', 'description', 'schedule', 'timezone', 'vmScheduleActions'],
      selectedColumnKeys: [],
      columns: [],
      schedules: [],
      timeZoneMap: [],
      actions: [],
      page: 1,
      pageSize: 20,
      totalCount: 0,
      showModal: false,
      isSubmitted: false,
      error: ''
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateVMSchedule')
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
    this.updateSelectedColumns('description')
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
        timezone: 'UTC'
      })
      this.rules = reactive({
        name: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        schedule: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        action: [{ type: 'string', required: true, message: this.$t('message.error.required.input') }],
        timezone: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    createVMSchedule (schedule) {
      this.resetForm()
      this.showAddModal()
    },
    removeVMSchedule (schedule) {
      api('deleteVMSchedule', {
        id: schedule.id
      }).then(() => {
        const message = `${this.$t('label.removing')} ${schedule.name}`
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
      Object.assign(this.form, schedule)
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
          name: values.name,
          description: values.description,
          schedule: values.schedule,
          timezone: values.timezone,
          action: values.action,
          virtualmachineid: this.virtualmachine.id,
          startDate: moment(new Date()).add(1, 'm').format(),
          enabled: values.enabled
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
          this.resetForm()
          this.fetchData()
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
          this.isSubmitted = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.isSubmitted = false
      })
    },
    resetForm () {
      if (this.formRef.value !== null && this.formRef.value !== undefined) {
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
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        this.columns.push({
          key: columnKey,
          title: this.$t('label.' + String(columnKey).toLowerCase()),
          dataIndex: columnKey,
          sorter: function (a, b) { console.log(this); return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;

  @media (min-width: 600px) {
    width: 450px;
  }

  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
}
</style>
