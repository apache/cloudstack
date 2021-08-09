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
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item ref="semanticversion" name="semanticversion">
          <template #label>
            {{ $t('label.semanticversion') }}
            <a-tooltip :title="apiParams.semanticversion.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.semanticversion"
            :placeholder="apiParams.semanticversion.description"
            autoFocus />
        </a-form-item>
        <a-form-item ref="name" name="name">
          <template #label>
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="$t('label.name')"/>
        </a-form-item>
        <a-form-item ref="zoneid" name="zoneid">
          <template #label>
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="zone-selection"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="url" name="url">
          <template #label>
            {{ $t('label.url') }}
            <a-tooltip :title="apiParams.url.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.url"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item ref="checksum" name="checksum">
          <template #label>
            {{ $t('label.checksum') }}
            <a-tooltip :title="apiParams.checksum.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.checksum"
            :placeholder="apiParams.checksum.description" />
        </a-form-item>
        <a-form-item ref="mincpunumber" name="mincpunumber">
          <template #label>
            {{ $t('label.mincpunumber') }}
            <a-tooltip :title="apiParams.mincpunumber.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.mincpunumber"
            :placeholder="apiParams.mincpunumber.description"/>
        </a-form-item>
        <a-form-item ref="minmemory" name="minmemory">
          <template #label>
            {{ $t('label.minmemory') }}
            <a-tooltip :title="apiParams.minmemory.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.minmemory"
            :placeholder="apiParams.minmemory.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" html-type="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'AddKubernetesSupportedVersion',
  props: {},
  data () {
    return {
      zones: [],
      zoneLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('addKubernetesSupportedVersion')
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        semanticversion: [{ required: true, message: this.$t('message.error.kuberversion') }],
        zoneid: [{
          type: 'number',
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }],
        url: [{ required: true, message: this.$t('message.error.binaries.iso.url') }],
        mincpunumber: [
          { required: true, message: this.$t('message.please.enter.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value <= 0)) {
                return Promise.reject(this.$t('message.validate.number'))
              }
              return Promise.resolve()
            }
          }
        ],
        minmemory: [
          { required: true, message: this.$t('message.please.enter.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value <= 0)) {
                return Promise.reject(this.$t('message.validate.number'))
              }
              Promise.resolve()
            }
          }
        ]
      })
    },
    fetchData () {
      this.fetchZoneData()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchZoneData () {
      const params = {}
      params.listAll = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = 0
        }
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          semanticversion: values.semanticversion,
          url: values.url
        }
        if (this.isValidValueForKey(values, 'name')) {
          params.name = values.name
        }
        if (this.isValidValueForKey(values, 'checksum')) {
          params.checksum = values.checksum
        }
        if (this.isValidValueForKey(values, 'zoneid')) {
          params.zoneid = this.zones[values.zoneid].id
        }
        if (this.isValidValueForKey(values, 'mincpunumber') && values.mincpunumber > 0) {
          params.mincpunumber = values.mincpunumber
        }
        if (this.isValidValueForKey(values, 'minmemory') && values.minmemory > 0) {
          params.minmemory = values.minmemory
        }
        api('addKubernetesSupportedVersion', params).then(json => {
          this.$message.success(`${this.$t('message.success.add.kuberversion')}: ${values.semanticversion}`)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
