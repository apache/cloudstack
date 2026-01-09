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
    <a-spin :spinning="loading">
      <a-alert
        v-if="resource"
        type="info"
        style="margin-bottom: 16px">
        <template #message>
          <div style="display: block; width: 100%;">
            <div style="display: block; margin-bottom: 8px;">
              <strong>{{ $t('message.clone.offering.from') }}: {{ resource.name }}</strong>
            </div>
            <div style="display: block; font-size: 12px;">
              {{ $t('message.clone.offering.edit.hint') }}
            </div>
          </div>
        </template>
      </a-alert>
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
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid ? apiParams.zoneid.description : ''"/>
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
            <tooltip-label :title="$t('label.externalid')" :tooltip="apiParams.externalid ? apiParams.externalid.description : ''"/>
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
            <tooltip-label :title="$t('label.allowuserdrivenbackups')" :tooltip="apiParams.allowuserdrivenbackups ? apiParams.allowuserdrivenbackups.description : ''"/>
          </template>
          <a-switch v-model:checked="form.allowuserdrivenbackups"/>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button :loading="loading" @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { GlobalOutlined } from '@ant-design/icons-vue'

export default {
  name: 'CloneBackupOffering',
  components: {
    TooltipLabel,
    ResourceIcon,
    GlobalOutlined
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
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
    this.apiParams = this.$getApiParams('cloneBackupOffering')
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
        description: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      this.fetchZone()
      this.$nextTick(() => {
        this.populateFormFromResource()
      })
    },
    fetchZone () {
      this.zones.loading = true
      getAPI('listZones', { available: true, showicon: true }).then(json => {
        this.zones.opts = json.listzonesresponse.zone || []
        this.$nextTick(() => {
          this.populateFormFromResource()
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.zones.loading = false
      })
    },
    fetchExternal (zoneId) {
      if (!zoneId) {
        this.externals.opts = []
        return
      }
      this.externals.loading = true
      getAPI('listBackupProviderOfferings', { zoneid: zoneId }).then(json => {
        this.externals.opts = json.listbackupproviderofferingsresponse.backupoffering || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.externals.loading = false
      })
    },
    populateFormFromResource () {
      if (!this.resource) return

      const r = this.resource

      this.form.name = r.name + ' - Clone'
      this.form.description = r.description || r.name

      if (r.allowuserdrivenbackups !== undefined) {
        this.form.allowuserdrivenbackups = r.allowuserdrivenbackups
      }

      if (r.zoneid && this.zones.opts.length > 0) {
        const zone = this.zones.opts.find(z => z.id === r.zoneid)
        if (zone) {
          this.form.zoneid = zone.name
          this.fetchExternal(zone.id)
        }
      }

      if (r.externalid) {
        this.$nextTick(() => {
          this.form.externalid = r.externalid
        })
      }
    },
    handleSubmit (e) {
      if (e) {
        e.preventDefault()
      }
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {
          sourceofferingid: this.resource.id
        }

        if (values.name) {
          params.name = values.name
        }

        if (values.description) {
          params.description = values.description
        }

        if (values.zoneid) {
          const zone = this.zones.opts.find(z => z.name === values.zoneid)
          if (zone) {
            params.zoneid = zone.id
          }
        }

        if (values.externalid) {
          params.externalid = values.externalid
        }

        if (values.allowuserdrivenbackups !== undefined) {
          params.allowuserdrivenbackups = values.allowuserdrivenbackups
        }

        this.loading = true
        const title = this.$t('message.success.clone.backup.offering')

        postAPI('cloneBackupOffering', params).then(json => {
          const jobId = json.clonebackupofferingresponse?.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description: values.name,
              successMethod: result => {
                this.$message.success(`${title}: ${values.name}`)
                this.$emit('refresh-data')
                this.closeAction()
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')} ${this.$t('label.for')} ${params.name}`,
              catchMessage: this.$t('error.fetching.async.job.result'),
              catchMethod: () => {
                this.closeAction()
              }
            })
          } else {
            this.$message.success(`${title}: ${values.name}`)
            this.$emit('refresh-data')
            this.closeAction()
          }
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    onChangeZone (value) {
      if (!value) {
        this.externals.opts = []
        this.form.externalid = null
        return
      }
      const zone = this.zones.opts.find(z => z.name === value)
      if (zone) {
        this.fetchExternal(zone.id)
      }
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

