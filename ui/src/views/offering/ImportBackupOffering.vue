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
  <div class="form-layout">
    <a-form
      layout="vertical"
      :form="form"
      @submit="handleSubmit">
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        <a-input
          autoFocus
          v-decorator="['name', {
            rules: [{ required: true, message: $t('message.error.required.input') }]
          }]"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        <a-input
          v-decorator="['description', {
            rules: [{ required: true, message: $t('message.error.required.input') }]
          }]"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        <a-select
          showSearch
          allowClear
          v-decorator="['zoneid', {
            rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
          }]"
          :loading="zones.loading"
          @change="onChangeZone">
          <a-select-option v-for="zone in zones.opts" :key="zone.name">
            {{ zone.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.externalid')" :tooltip="apiParams.externalid.description"/>
        <a-select
          allowClear
          v-decorator="['externalid', {
            rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
          }] "
          :loading="externals.loading">
          <a-select-option v-for="opt in externals.opts" :key="opt.id">
            {{ opt.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.allowuserdrivenbackups')" :tooltip="apiParams.allowuserdrivenbackups.description"/>
        <a-switch
          v-decorator="['allowuserdrivenbackups']"
          :default-checked="true"/>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ImportBackupOffering',
  components: {
    TooltipLabel
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
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('importBackupOffering')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchZone()
    },
    fetchZone () {
      this.zones.loading = true
      api('listZones', { available: true }).then(json => {
        this.zones.opts = json.listzonesresponse.zone || []
        this.$forceUpdate()
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
        this.$forceUpdate()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.externals.loading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (key === 'zoneid') {
            params[key] = this.zones.opts.filter(zone => zone.name === input)[0].id || null
          } else {
            params[key] = input
          }
        }
        params.allowuserdrivenbackups = values.allowuserdrivenbackups ? values.allowuserdrivenbackups : true
        this.loading = true
        const title = this.$t('label.import.offering')
        api('importBackupOffering', params).then(json => {
          const jobId = json.importbackupofferingresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title: title,
              description: values.name,
              successMethod: result => {
                this.closeAction()
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')} ${this.$t('label.for')} ${params.name}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
          }
        }).catch(error => {
          this.$notifyError(error)
        }).finally(f => {
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

  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
}
</style>
