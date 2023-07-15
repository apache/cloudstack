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
      <a-form-item name="zoneid" ref="zoneid">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        </template>
        <a-select
          allowClear
          v-model:value="form.zoneid"
          :loading="zones.loading"
          @change="onChangeZone"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="zone in zones.opts" :key="zone.name" :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px"/>
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="externalid" ref="externalid">
        <template #label>
          <tooltip-label :title="$t('label.externalid')" :tooltip="apiParams.externalid.description"/>
        </template>
        <a-select
          allowClear
          v-model:value="form.externalid"
          :loading="externals.loading"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in externals.opts" :key="opt.id" :label="opt.name">
            {{ opt.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="allowuserdrivenbackups" ref="allowuserdrivenbackups">
        <template #label>
          <tooltip-label :title="$t('label.allowuserdrivenbackups')" :tooltip="apiParams.allowuserdrivenbackups.description"/>
        </template>
        <a-switch v-model:checked="form.allowuserdrivenbackups"/>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ImportBackupOffering',
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
      }
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
        allowuserdrivenbackups: true
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        description: [{ required: true, message: this.$t('message.error.required.input') }],
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        externalid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchZone()
    },
    fetchZone () {
      this.zones.loading = true
      api('listZones', { available: true, showicon: true }).then(json => {
        this.zones.opts = json.listzonesresponse.zone || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.zones.loading = false
      })
    },
    fetchExternal (zoneId) {
      if (!zoneId) {
        this.externals.opts = []
        return
      }
      this.externals.loading = true
      api('listBackupProviderOfferings', { zoneid: zoneId }).then(json => {
        this.externals.opts = json.listbackupproviderofferingsresponse.backupoffering || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.externals.loading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (key === 'zoneid') {
            params[key] = this.zones.opts.filter(zone => zone.name === input)[0].id || null
          } else {
            params[key] = input
          }
        }
        params.allowuserdrivenbackups = values.allowuserdrivenbackups
        this.loading = true
        const title = this.$t('label.import.offering')
        api('importBackupOffering', params).then(json => {
          const jobId = json.importbackupofferingresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description: values.name,
              successMethod: result => {
                this.closeAction()
                this.loading = false
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')} ${this.$t('label.for')} ${params.name}`,
              catchMessage: this.$t('error.fetching.async.job.result'),
              catchMethod: () => {
                this.loading = false
              }
            })
          }
        }).catch(error => {
          this.$notifyError(error)
          this.loading = false
        })
      })
    },
    onChangeZone (value) {
      if (!value) {
        this.externals.opts = []
        return
      }
      const zoneId = this.zones.opts.filter(zone => zone.name === value)[0].id || null
      this.fetchExternal(zoneId)
    },
    closeAction () {
      this.$emit('close-action')
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
