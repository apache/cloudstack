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
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <span slot="label">
            {{ $t('label.semanticversion') }}
            <a-tooltip :title="apiParams.semanticversion.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['semanticversion', {
              rules: [{ required: true, message: $t('message.error.kuberversion') }]
            }]"
            :placeholder="apiParams.semanticversion.description"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['name', {
              rules: [{ message: $t('message.error.name') }]
            }]"
            :placeholder="$t('label.name')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            id="zone-selection"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback(this.$t('message.error.zone.combined'))
                    }
                    callback()
                  }
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.url') }}
            <a-tooltip :title="apiParams.url.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['url', {
              rules: [{ required: true, message: $t('message.error.binaries.iso.url') }]
            }]"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.checksum') }}
            <a-tooltip :title="apiParams.checksum.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['checksum', {
              rules: [{ required: false, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.checksum.description" />
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.mincpunumber') }}
            <a-tooltip :title="apiParams.mincpunumber.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['mincpunumber', {
              rules: [{ required: true, message: $t('message.please.enter.value') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.validate.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="apiParams.mincpunumber.description"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.minmemory') }}
            <a-tooltip :title="apiParams.minmemory.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['minmemory', {
              rules: [{ required: true, message: $t('message.please.enter.value') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.validate.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="apiParams.minmemory.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
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
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.addKubernetesSupportedVersion || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.fetchData()
  },
  methods: {
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
          this.form.setFieldsValue({
            zoneid: 0
          })
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
