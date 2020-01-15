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
        <a-form-item :label="$t('url')">
          <a-input
            v-decorator="['url', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('iso.url.description')" />
        </a-form-item>

        <a-form-item :label="$t('name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('iso.name.description')" />
        </a-form-item>

        <a-form-item :label="$t('displaytext')">
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('iso.displaytext.description')" />
        </a-form-item>

        <a-form-item :label="$t('directdownload')">
          <a-switch v-decorator="['directdownload']" />
        </a-form-item>

        <a-form-item :label="$t('zoneid')">
          <a-select
            v-decorator="['zoneid', {
              rules: [
                {
                  required: true,
                  message: 'Please select option'
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="$t('iso.zoneid.description')">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('bootable')">
          <a-switch v-decorator="['bootable']" />
        </a-form-item>

        <a-form-item :label="$t('ostypeid')">
          <a-select
            v-decorator="['ostypeid', {
              rules: [{ required: true, message: 'Please select option' }]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="osTypeLoading"
            :placeholder="$t('iso.ostypeid.description')">
            <a-select-option v-for="(opt, optIndex) in osTypes" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('isextractable')">
          <a-switch v-decorator="['isextractable']" />
        </a-form-item>

        <a-form-item :label="$t('ispublic')">
          <a-switch v-decorator="['ispublic']" />
        </a-form-item>

        <a-form-item :label="$t('isfeatured')">
          <a-switch v-decorator="['isfeatured']" />
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
  name: 'RegisterIso',
  data () {
    return {
      zones: [],
      osTypes: [],
      zoneLoading: false,
      osTypeLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.zones = [
      {
        id: '-1',
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
      this.fetchOsType()
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
      })
    },
    fetchOsType () {
      const params = {}
      params.listAll = true

      this.osTypeLoading = true

      api('listOsTypes', params).then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.osTypes = this.osTypes.concat(listOsTypes)
      }).finally(() => {
        this.osTypeLoading = false
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
          if (input === undefined) {
            continue
          }
          switch (key) {
            case 'zoneid':
              params[key] = this.zones[input].id
              break
            case 'ostypeid':
              params[key] = this.osTypes[input].id
              break
            default:
              params[key] = input
              break
          }
        }

        this.loading = true
        api('registerIso', params).then(json => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: 'Register ISO',
            description: 'Sucessfully registered ISO ' + params.name
          })
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
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
    width: 500px;
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
