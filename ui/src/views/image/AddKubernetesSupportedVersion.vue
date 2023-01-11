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
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item ref="semanticversion" name="semanticversion">
          <template #label>
            <tooltip-label :title="$t('label.semanticversion')" :tooltip="apiParams.semanticversion.description"/>
          </template>
          <a-input
            v-model:value="form.semanticversion"
            :placeholder="apiParams.semanticversion.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item ref="name" name="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item ref="zoneid" name="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            id="zone-selection"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="url" name="url">
          <template #label>
            <tooltip-label :title="$t('label.url')" :tooltip="apiParams.url.description"/>
          </template>
          <a-input
            v-model:value="form.url"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item ref="checksum" name="checksum">
          <template #label>
            <tooltip-label :title="$t('label.checksum')" :tooltip="apiParams.checksum.description"/>
          </template>
          <a-input
            v-model:value="form.checksum"
            :placeholder="apiParams.checksum.description" />
        </a-form-item>
        <a-form-item ref="mincpunumber" name="mincpunumber">
          <template #label>
            <tooltip-label :title="$t('label.mincpunumber')" :tooltip="apiParams.mincpunumber.description"/>
          </template>
          <a-input
            v-model:value="form.mincpunumber"
            :placeholder="apiParams.mincpunumber.description"/>
        </a-form-item>
        <a-form-item ref="minmemory" name="minmemory">
          <template #label>
            <tooltip-label :title="$t('label.minmemory')" :tooltip="apiParams.minmemory.description"/>
          </template>
          <a-input
            v-model:value="form.minmemory"
            :placeholder="apiParams.minmemory.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddKubernetesSupportedVersion',
  components: {
    ResourceIcon,
    TooltipLabel
  },
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
      params.showicon = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = 0
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
</style>
