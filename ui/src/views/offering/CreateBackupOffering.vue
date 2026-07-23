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
      <a-form-item name="allowuserdrivenbackups" ref="allowuserdrivenbackups">
        <template #label>
          <tooltip-label :title="$t('label.allowuserdrivenbackups')" :tooltip="apiParams.allowuserdrivenbackups.description"/>
        </template>
        <a-switch v-model:checked="form.allowuserdrivenbackups"/>
      </a-form-item>
      <a-form-item name="ispublic" ref="ispublic" :label="$t('label.ispublic')" v-if="isAdmin()">
        <a-switch v-model:checked="form.ispublic" />
      </a-form-item>
      <a-form-item name="domainid" ref="domainid" v-if="!form.ispublic">
        <template #label>
          <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
        </template>
        <a-select
          mode="multiple"
          :getPopupContainer="(trigger) => trigger.parentNode"
          v-model:value="form.domainid"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
          :loading="domains.loading"
          :placeholder="apiParams.domainid.description">
          <a-select-option v-for="(opt, optIndex) in domains.opts" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="compress" ref="compress" :label="$t('label.compress')">
        <a-switch v-model:checked="form.compress" />
      </a-form-item>
      <a-form-item name="compressionlibrary" ref="compressionlibrary" v-if="form.compress">
        <template #label>
          <tooltip-label :title="$t('label.compressionlibrary')" :tooltip="apiParams.compressionlibrary.description"/>
        </template>
        <a-select
          v-model:value="form.compressionlibrary"
          :placeholder="apiParams.compressionlibrary.description">
          <a-select-option
            v-for="item in compressionlibraryopts"
            :key="item"
            :value="item">
            {{ item }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="validate" ref="validate" :label="$t('label.validate')">
        <a-switch v-model:checked="form.validate" />
      </a-form-item>
      <a-form-item name="validationsteps" ref="validationsteps" v-if="form.validate">
        <template #label>
          <tooltip-label :title="$t('label.validationsteps')" :tooltip="apiParams.validationsteps.description"/>
        </template>
        <a-select
          mode="tags"
          showSearch
          :options="validationstepsoptions"
          v-model:value="form.validationsteps"
          :placeholder="apiParams.validationsteps.description">
        </a-select>
      </a-form-item>
      <a-form-item name="allowextractfile" ref="allowextractfile" :label="$t('label.allowextractfile')">
        <a-switch v-model:checked="form.allowextractfile" />
      </a-form-item>
      <a-form-item name="allowquickrestore" ref="allowquickrestore" :label="$t('label.allowquickrestore')">
        <a-switch v-model:checked="form.allowquickrestore" />
      </a-form-item>
      <a-form-item ref="backupchainsize" name="backupchainsize">
        <template #label>
          <tooltip-label :title="$t('label.backupchainsize')" :tooltip="apiParams.backupchainsize.description"/>
        </template>
        <a-input
          v-model:value="form.backupchainsize"/>
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
import { getAPI, postAPI } from '@/api'
import { isAdmin } from '@/role'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateBackupOffering',
  components: {
    TooltipLabel,
    ResourceIcon
  },
  data () {
    return {
      loading: false,
      domains: {
        loading: false,
        opts: []
      },
      zones: {
        loading: false,
        opts: []
      },
      compressionlibraryopts: ['zlib', 'zstd'],
      validationstepsoptions: [
        { value: 'wait_for_boot', label: 'wait_for_boot' },
        { value: 'screenshot', label: 'screenshot' },
        { value: 'execute_command', label: 'execute_command' }
      ]
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createBackupOffering')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        allowuserdrivenbackups: true,
        ispublic: true,
        compress: false,
        validate: false,
        allowquickrestore: true,
        allowextractfile: true,
        backupchainsize: null,
        validationsteps: [],
        compressionlibrary: null
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        description: [{ required: true, message: this.$t('message.error.required.input') }],
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        domainid: [{ type: 'array', message: this.$t('message.error.select') }]
      })
    },
    isAdmin () {
      return isAdmin()
    },
    fetchData () {
      this.fetchZone()
      this.fetchDomainData()
    },
    fetchZone () {
      this.zones.loading = true
      getAPI('listZones', { available: true, showicon: true }).then(json => {
        this.zones.opts = json.listzonesresponse.zone || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.zones.loading = false
      })
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.details = 'min'
      this.domains.loading = true
      getAPI('listDomains', params).then(json => {
        this.domains.opts = json.listdomainsresponse.domain || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domains.loading = false
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
          } else if (input !== null && key !== 'validationsteps') {
            params[key] = input
          }
        }
        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains.opts[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        if (values.validationsteps.length !== 0) {
          params.validationsteps = values.validationsteps.join(',')
        }
        this.loading = true
        postAPI('createBackupOffering', params).then(json => {
          this.closeAction()
          this.loading = false
        }).catch(error => {
          this.$notifyError(error)
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
      this.$emit('refresh-data')
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
