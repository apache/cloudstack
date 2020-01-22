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
        <a-form-item :label="$t('name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: 'Please enter name' }]
            }]"
            :placeholder="this.$t('Name')"/>
        </a-form-item>
        <a-form-item :label="$t('description')">
          <a-input
            v-decorator="['description', {
              rules: [{ required: true, message: 'Please enter description' }]
            }]"
            :placeholder="this.$t('Description')"/>
        </a-form-item>
        <a-form-item :label="$t('label.networkrate.mbps')">
          <a-input
            v-decorator="['disksize', {
              rules: [{ required: true, message: 'Please enter disk size' },
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
            :placeholder="this.$t('label.networkrate.mbps')"/>
        </a-form-item>
        <a-form-item :label="$t('label.guesttype')">
          <a-radio-group
            v-decorator="['guesttype', {
              initialValue: this.guestType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleGuestTypeChange(selected.target.value) }">
            <a-radio-button value="isolated">
              {{ $t('label.isolated') }}
            </a-radio-button>
            <a-radio-button value="l2">
              {{ $t('label.l2') }}
            </a-radio-button>
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.persisitent')">
          <a-switch v-decorator="['ispersisitent']" />
        </a-form-item>
        <a-form-item :label="$t('label.specifyvlan')">
          <a-switch v-decorator="['isspecifyvlan']" />
        </a-form-item>
        <a-form-item :label="$t('label.vpc')">
          <a-switch v-decorator="['isvpc']" />
        </a-form-item>
        <a-form-item :label="$t('label.promiscuousmode')">
          <a-radio-group
            v-decorator="['promiscuousmode', {
              initialValue: this.promiscuousMode
            }]"
            buttonStyle="solid"
            @change="selected => { this.handlePromiscuousModeChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('label.none') }}
            </a-radio-button>
            <a-radio-button value="accept">
              {{ $t('label.accept') }}
            </a-radio-button>
            <a-radio-button value="reject">
              {{ $t('label.reject') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.macaddresschanges')">
          <a-radio-group
            v-decorator="['macaddresschanges', {
              initialValue: this.macAddressChanges
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleMacAddressChangesChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('label.none') }}
            </a-radio-button>
            <a-radio-button value="accept">
              {{ $t('label.accept') }}
            </a-radio-button>
            <a-radio-button value="reject">
              {{ $t('label.reject') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.forgedtransmits')">
          <a-radio-group
            v-decorator="['forgedtransmits', {
              initialValue: this.forgedTransmits
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleForgedTransmitsChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('label.none') }}
            </a-radio-button>
            <a-radio-button value="accept">
              {{ $t('label.accept') }}
            </a-radio-button>
            <a-radio-button value="reject">
              {{ $t('label.reject') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.conservemode')">
          <a-switch v-decorator="['isconservemode']" :checked="this.isConserveMode" />
        </a-form-item>
        <a-form-item :label="$t('label.tags')">
          <a-input
            v-decorator="['tags', {}]"
            :placeholder="this.$t('label.networktags')"/>
        </a-form-item>
        <a-form-item :label="$t('ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic']" :checked="this.isPublic" @change="val => { this.isPublic = val }" />
        </a-form-item>
        <a-form-item :label="$t('domainid')" v-if="!this.isPublic">
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
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
            :loading="domainLoading"
            :placeholder="this.$t('label.domain')">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('zoneid')">
          <a-select
            id="zone-selection"
            mode="multiple"
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
            :placeholder="this.$t('label.zone')">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'AddNetworkOffering',
  components: {
  },
  data () {
    return {
      guestType: 'isolated',
      promiscuousMode: '',
      macAddressChanges: '',
      forgedTransmits: '',
      selectedDomains: [],
      selectedZones: [],
      isConserveMode: true,
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.zones = [
      {
        id: 'all',
        name: this.$t('label.all.zone')
      }
    ]
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.details = 'min'
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
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
    handleGuestTypeChange (val) {
      this.guestType = val
    },
    handlePromiscuousModeChange (val) {
      this.promiscuousMode = val
    },
    handleMacAddressChangesChange (val) {
      this.macAddressChanges = val
    },
    handleForgedTransmitsChange (val) {
      this.forgedTransmits = val
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        var params = {}
        console.log(params)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped>
</style>
