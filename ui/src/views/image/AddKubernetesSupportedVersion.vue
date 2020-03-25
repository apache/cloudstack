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
        <a-form-item :label="$t('semanticversion')">
          <a-input
            v-decorator="['semanticversion', {
              rules: [{ required: true, message: 'Please enter Kubernetes semantic version' }]
            }]"
            :placeholder="apiParams.semanticversion.description"/>
        </a-form-item>
        <a-form-item :label="$t('name')">
          <a-input
            v-decorator="['name', {
              rules: [{ message: 'Please enter name' }]
            }]"
            :placeholder="$t('name')"/>
        </a-form-item>
        <a-form-item :label="$t('zoneid')">
          <a-select
            id="zone-selection"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback('All Zones cannot be combined with any other zone')
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
        <a-form-item :label="$t('url')">
          <a-input
            v-decorator="['url', {
              rules: [{ required: true, message: 'Please enter binaries ISO URL' }]
            }]"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item :label="$t('checksum')">
          <a-input
            v-decorator="['checksum', {
              rules: [{ required: false, message: 'Please enter input' }]
            }]"
            :placeholder="apiParams.checksum.description" />
        </a-form-item>
        <a-form-item :label="$t('mincpunumber')">
          <a-input
            v-decorator="['mincpunumber', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="apiParams.mincpunumber.description"/>
        </a-form-item>
        <a-form-item :label="$t('minmemory')">
          <a-input
            v-decorator="['minmemory', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="apiParams.minmemory.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
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
  },
  mounted () {
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
          this.$message.success('Successfully added Kubernetes version: ' + values.semanticversion)
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.$emit('refresh-data')
          this.loading = false
          this.closeAction()
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
