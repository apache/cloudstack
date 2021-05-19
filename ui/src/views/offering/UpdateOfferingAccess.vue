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

        <a-form-item :label="$t('label.ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic', { initialValue: this.offeringIsPublic }]" :checked="this.offeringIsPublic" @change="val => { this.offeringIsPublic = val }" />
        </a-form-item>

        <a-form-item :label="$t('label.domainid')" v-if="!this.offeringIsPublic">
          <a-select
            :autoFocus="!this.offeringIsPublic"
            mode="multiple"
            v-decorator="['domainid', {
              rules: [
                {
                  required: true,
                  message: `${this.$t('message.error.select')}`
                }
              ],
              initialValue: this.selectedDomains
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="this.apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('label.zoneid')">
          <a-select
            :autoFocus="this.offeringIsPublic"
            id="zone-selection"
            mode="multiple"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback($t('message.error.zone.combined'))
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
            :placeholder="this.apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
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
  name: 'UpdateOfferingAccess',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      formOffering: {},
      selectedDomains: [],
      selectedZones: [],
      offeringIsPublic: false,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    switch (this.$route.meta.name) {
      case 'computeoffering':
        this.offeringType = 'ServiceOffering'
        break
      case 'diskoffering':
        this.offeringType = 'DiskOffering'
        break
      case 'networkoffering':
        this.offeringType = 'NetworkOffering'
        break
      case 'vpcoffering':
        this.offeringType = 'VPCOffering'
        break
      default:
        this.offeringType = this.$route.meta.name
    }
    this.apiParams = {}
    this.apiParamsConfig = this.$store.getters.apis['update' + this.offeringType] || {}
    this.apiParamsConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.zones = [
      {
        id: 'all',
        name: this.$t('label.all.zone')
      }
    ]
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchOfferingData()
      this.fetchDomainData()
      this.fetchZoneData()
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    fetchOfferingData () {
      this.loading = true
      const params = {}
      params.id = this.resource.id
      params.isrecursive = true
      var apiName = 'list' + this.offeringType + 's'
      api(apiName, params).then(json => {
        const offerings = json[apiName.toLowerCase() + 'response'][this.offeringType.toLowerCase()]
        this.formOffering = offerings[0]
      }).finally(() => {
        this.updateDomainSelection()
        this.updateZoneSelection()
        this.loading = false
      })
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
        this.updateDomainSelection()
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
        this.updateZoneSelection()
      })
    },
    updateDomainSelection () {
      if (!this.formOffering || Object.keys(this.formOffering).length === 0) return
      var offeringDomainIds = this.formOffering.domainid
      this.selectedDomains = []
      if (offeringDomainIds) {
        this.offeringIsPublic = false
        offeringDomainIds = offeringDomainIds.indexOf(',') !== -1 ? offeringDomainIds.split(',') : [offeringDomainIds]
        for (var i = 0; i < offeringDomainIds.length; i++) {
          for (var j = 0; j < this.domains.length; j++) {
            if (offeringDomainIds[i] === this.domains[j].id) {
              this.selectedDomains.push(j)
            }
          }
        }
      } else {
        if (this.isAdmin()) {
          this.offeringIsPublic = true
        }
      }
      if ('domainid' in this.form.fieldsStore.fieldsMeta) {
        this.form.setFieldsValue({
          domainid: this.selectedDomains
        })
      }
    },
    updateZoneSelection () {
      if (!this.formOffering || Object.keys(this.formOffering).length === 0) return
      var offeringZoneIds = this.formOffering.zoneid
      this.selectedZones = []
      if (offeringZoneIds) {
        offeringZoneIds = offeringZoneIds.indexOf(',') !== -1 ? offeringZoneIds.split(',') : [offeringZoneIds]
        for (var i = 0; i < offeringZoneIds.length; i++) {
          for (var j = 0; j < this.zones.length; j++) {
            if (offeringZoneIds[i] === this.zones[j].id) {
              this.selectedZones.push(j)
            }
          }
        }
      }
      this.form.setFieldsValue({
        zoneid: this.selectedZones
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        const params = {}
        params.id = this.resource.id
        var ispublic = values.ispublic
        if (ispublic === true) {
          params.domainid = 'public'
        } else {
          var domainIndexes = values.domainid
          var domainId = 'public'
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          params.domainid = domainId
        }
        var zoneIndexes = values.zoneid
        var zoneId = 'all'
        if (zoneIndexes && zoneIndexes.length > 0) {
          var zoneIds = []
          for (var j = 0; j < zoneIndexes.length; j++) {
            zoneIds = zoneIds.concat(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        params.zoneid = zoneId

        this.loading = true
        api('update' + this.offeringType, params).then(json => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.action.update.offering.access'),
            description: this.$t('label.action.update.offering.access')
          })
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

<style scoped lang="scss">
  .form-layout {
    width: 80vw;

    @media (min-width: 600px) {
      width: 25vw;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
