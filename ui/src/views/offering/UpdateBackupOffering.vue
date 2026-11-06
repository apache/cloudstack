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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-form
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
    >
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.name"/>
      </a-form-item>
      <a-form-item name="description" ref="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-input v-model:value="form.description"/>
      </a-form-item>
      <a-form-item name="allowuserdrivenbackups" ref="allowuserdrivenbackups">
        <template #label>
          <tooltip-label :title="$t('label.allowuserdrivenbackups')" :tooltip="apiParams.allowuserdrivenbackups.description"/>
        </template>
        <a-switch v-model:checked="form.allowuserdrivenbackups"/>
      </a-form-item>
      <a-form-item name="maxschedules" ref="maxschedules">
        <template #label>
          <tooltip-label :title="$t('label.maxschedules')" :tooltip="apiParams.maxschedules.description"/>
        </template>
        <div style="display: flex ">
          <div v-for="(max,schedule) in maxSchedule" :key="schedule">
            <a-tooltip :title="schedule + ': ' + max ">
              <a-tag style="margin:2px" >
                {{ (schedule + ': ' + max) }}
                <edit-outlined class="traffic-type-action" @click="editSchedule(schedule)"/>
              </a-tag>
            </a-tooltip>
          </div>
        </div>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
    <a-modal
      :title="$t('label.edit.max.schedules')"
      v-model:visible="showEditMaxSchedules"
      :closable="true"
      :maskClosable="false"
      centered
      :footer="null">
      <a-form
        :ref="formRef"
        :model="form"
        layout="vertical"
      >
        <a-form-item
          name="maxSchedule"
          ref="maxSchedule"
          v-bind="formItemLayout"
          style="margin-top:16px;"
          :label="$t('label.maxschedules') + ' of ' + this.scheduleInEdit + ' type'">
          <a-input v-model:value="form.maxSchedule" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="cancelEditSchedule">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="updateMaxSchedule(scheduleInEdit)">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ImportBackupOffering',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    TooltipLabel,
    ResourceIcon
  },
  data () {
    return {
      loading: false,
      zones: {
        loading: false,
        opts: []
      },
      externals: {
        loading: false,
        opts: []
      },
      maxSchedule: {
        HOURLY: 1,
        DAILY: 1,
        WEEKLY: 1,
        MONTHLY: 1
      },
      showEditMaxSchedules: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('importBackupOffering')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: '',
        description: '',
        allowuserdrivenbackups: true,
        maxSchedule: 1
      })
    },
    fetchData () {
      const params = { id: this.resource.id }
      getAPI('listBackupOfferings', params).then(json => {
        const backupOffering = json.listbackupofferingsresponse.backupoffering[0]
        this.form.name = backupOffering.name
        this.form.description = backupOffering.description
        this.form.allowuserdrivenbackups = backupOffering.allowuserdrivenbackups
        this.maxSchedule.HOURLY = backupOffering.backupofferingdetails?.HOURLY ?? 1
        this.maxSchedule.DAILY = backupOffering.backupofferingdetails?.DAILY ?? 1
        this.maxSchedule.WEEKLY = backupOffering.backupofferingdetails?.WEEKLY ?? 1
        this.maxSchedule.MONTHLY = backupOffering.backupofferingdetails?.MONTHLY ?? 1
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        for (const key in values) {
          if (key === 'maxSchedule') {
            continue
          }
          const input = values[key]
          if (key === 'zoneid') {
            params[key] = this.zones.opts.filter(zone => zone.name === input)[0].id || null
          } else {
            params[key] = input
          }
        }
        params.allowuserdrivenbackups = values.allowuserdrivenbackups
        Object.keys(this.maxSchedule).forEach(key => {
          params['maxschedules[0].' + key] = this.maxSchedule[key]
        })
        params.id = this.resource.id
        this.loading = true
        postAPI('updateBackupOffering', params).then(
          this.$emit('refresh-data')
        ).catch(error => {
          this.$notifyError(error)
          this.loading = false
        })
        this.loading = false
        this.closeAction()
        this.$emit('refresh-data')
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    editSchedule (maxSchedule) {
      this.scheduleInEdit = maxSchedule
      this.form.maxSchedule = this.maxSchedule[maxSchedule]
      this.showEditMaxSchedules = true
    },
    updateMaxSchedule (scheduleInEdit) {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.showEditMaxSchedules = false
        this.maxSchedule[scheduleInEdit] = values.maxSchedule
        this.scheduleInEdit = null
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    cancelEditSchedule () {
      this.showEditMaxSchedules = false
      this.scheduleInEdit = null
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 30vw;

  @media (min-width: 500px) {
    width: 450px;
  }
}
</style>
