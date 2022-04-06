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
        @finish="handleSubmit"
        layout="vertical"
       >

        <a-form-item name="ispublic" ref="ispublic" :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-model:checked="form.ispublic" v-focus="true" />
        </a-form-item>

        <a-form-item name="domainid" ref="domainid" :label="$t('label.domainid')" v-if="!form.ispublic">
          <a-select
            v-focus="!form.ispublic"
            mode="multiple"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="zoneid" ref="zoneid" :label="$t('label.zoneid')">
          <a-select
            v-focus="form.ispublic"
            id="zone-selection"
            mode="multiple"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
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
import { isAdmin } from '@/role'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'UpdateOfferingAccess',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon
  },
  data () {
    return {
      formOffering: {},
      selectedDomains: [],
      selectedZones: [],
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      loading: false
    }
  },
  beforeCreate () {
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
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        domainid: [{ type: 'array', required: true, message: this.$t('message.error.select') }],
        zoneid: [{
          type: 'array',
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
    fetchData () {
      this.fetchOfferingData()
      this.fetchDomainData()
      this.fetchZoneData()
    },
    isAdmin () {
      return isAdmin()
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
      params.showicon = true
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
      params.showicon = true
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
        this.form.ispublic = false
        offeringDomainIds = offeringDomainIds.indexOf(',') !== -1 ? offeringDomainIds.split(',') : [offeringDomainIds]
        for (var i = 0; i < offeringDomainIds.length; i++) {
          for (var j = 0; j < this.domains.length; j++) {
            if (offeringDomainIds[i] === this.domains[j].id) {
              this.selectedDomains.push(j)
            }
          }
        }
      } else {
        if (isAdmin()) {
          this.form.ispublic = true
        }
      }
      if (!this.form.ispublic) {
        this.form.domainid = this.selectedDomains
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
      this.form.zoneid = this.selectedZones
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

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
</style>
