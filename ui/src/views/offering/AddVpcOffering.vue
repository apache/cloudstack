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
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            autoFocus
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.name') }]
            }]"
            :placeholder="this.$t('label.name')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.displaytext') }}
            <a-tooltip :title="apiParams.displaytext.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: $t('message.error.description') }]
            }]"
            :placeholder="this.$t('label.displaytext')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.supportedservices') }}
            <a-tooltip :title="apiParams.supportedservices.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <div class="supported-services-container" scroll-to="last-child">
            <a-list itemLayout="horizontal" :dataSource="this.supportedServices">
              <a-list-item slot="renderItem" slot-scope="item">
                <CheckBoxSelectPair
                  v-decorator="['service.'+item.name, {}]"
                  :resourceKey="item.name"
                  :checkBoxLabel="item.description"
                  :checkBoxDecorator="'service.' + item.name"
                  :selectOptions="item.provider"
                  :selectDecorator="item.name + '.provider'"
                  @handle-checkpair-change="handleSupportedServiceChange"/>
              </a-list-item>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item :label="$t('label.service.connectivity.regionlevelvpccapabilitycheckbox')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['regionlevelvpc', {initialValue: true}]" defaultChecked />
        </a-form-item>
        <a-form-item :label="$t('label.service.connectivity.distributedroutercapabilitycheckbox')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['distributedrouter', {initialValue: true}]" defaultChecked />
        </a-form-item>
        <a-form-item :label="$t('label.redundantrouter')" v-if="this.sourceNatServiceChecked">
          <a-switch v-decorator="['redundantrouter', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.ispublic')" v-if="this.isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: this.isPublic}]" :defaultChecked="this.isPublic" @change="val => { this.isPublic = val }" />
        </a-form-item>
        <a-form-item v-if="!this.isPublic">
          <span slot="label">
            {{ $t('label.domainid') }}
            <a-tooltip :title="apiParams.domainid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
              rules: [
                {
                  required: true,
                  message: `${this.$t('message.error.select')}`
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
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
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
            :placeholder="this.$t('label.zone')">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="apiParams.enable">
          <span slot="label">
            {{ $t('label.enable.vpc.offering') }}
            <a-tooltip :title="apiParams.enable.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['enable', {initialValue: false}]" />
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'

export default {
  name: 'AddVpcOffering',
  components: {
    CheckBoxSelectPair
  },
  data () {
    return {
      selectedDomains: [],
      selectedZones: [],
      isConserveMode: true,
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      loading: false,
      supportedServices: [],
      supportedServiceLoading: false,
      connectivityServiceChecked: false,
      sourceNatServiceChecked: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.createVPCOffering || {}
    apiConfig.params.forEach(param => {
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
      this.fetchDomainData()
      this.fetchZoneData()
      this.fetchSupportedServiceData()
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    isSupportedServiceObject (obj) {
      return (obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object && 'provider' in obj)
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
    fetchSupportedServiceData () {
      this.supportedServices = []
      this.supportedServices.push({
        name: 'Dhcp',
        provider: [
          { name: 'VpcVirtualRouter' }
        ]
      })
      this.supportedServices.push({
        name: 'Dns',
        provider: [{ name: 'VpcVirtualRouter' }]
      })
      this.supportedServices.push({
        name: 'Lb',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'InternalLbVm' }
        ]
      })
      this.supportedServices.push({
        name: 'Gateway',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'BigSwitchBcf' }
        ]
      })
      this.supportedServices.push({
        name: 'StaticNat',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'BigSwitchBcf' }
        ]
      })
      this.supportedServices.push({
        name: 'SourceNat',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'BigSwitchBcf' }
        ]
      })
      this.supportedServices.push({
        name: 'NetworkACL',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'BigSwitchBcf' }
        ]
      })
      this.supportedServices.push({
        name: 'PortForwarding',
        provider: [{ name: 'VpcVirtualRouter' }]
      })
      this.supportedServices.push({
        name: 'UserData',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'ConfigDrive' }
        ]
      })
      this.supportedServices.push({
        name: 'Vpn',
        provider: [
          { name: 'VpcVirtualRouter' },
          { name: 'BigSwitchBcf' }
        ]
      })
      this.supportedServices.push({
        name: 'Connectivity',
        provider: [
          { name: 'BigSwitchBcf' },
          { name: 'NiciraNvp' },
          { name: 'Ovs' },
          { name: 'JuniperContrailVpcRouter' }
        ]
      })
      for (var i in this.supportedServices) {
        var serviceName = this.supportedServices[i].name
        var serviceDisplayName = serviceName
        // Sanitize names
        this.supportedServices[i].description = serviceDisplayName
      }
    },
    handleSupportedServiceChange (service, checked, provider) {
      if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      }
      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        var params = {}
        params.name = values.name
        params.displaytext = values.displaytext
        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        var zoneIndexes = values.zoneid
        var zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          var zoneIds = []
          for (var j = 0; j < zoneIndexes.length; j++) {
            zoneIds = zoneIds.concat(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }
        var selectedServices = null
        var keys = Object.keys(values)
        var self = this
        keys.forEach(function (key, keyIndex) {
          if (self.isSupportedServiceObject(values[key])) {
            if (selectedServices == null) {
              selectedServices = {}
            }
            selectedServices[key] = values[key]
          }
        })
        if (selectedServices != null) {
          var supportedServices = Object.keys(selectedServices)
          params.supportedservices = supportedServices.join(',')
          for (var k in supportedServices) {
            params['serviceProviderList[' + k + '].service'] = supportedServices[k]
            params['serviceProviderList[' + k + '].provider'] = selectedServices[supportedServices[k]].provider
          }
          var serviceCapabilityIndex = 0
          if (supportedServices.includes('Connectivity')) {
            if (values.regionlevelvpc === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RegionLevelVpc'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.distributedrouter === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'DistributedRouter'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
          }
          if (supportedServices.includes('SourceNat') && values.redundantrouter === true) {
            params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'SourceNat'
            params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RedundantRouter'
            params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
            serviceCapabilityIndex++
          }
        } else {
          params.supportedservices = ''
        }
        if (values.enable) {
          params.enable = values.enable
        }
        api('createVPCOffering', params).then(json => {
          this.$message.success(`${this.$t('message.create.vpc.offering')}: ` + values.name)
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

<style scoped lang="scss">
  .form-layout {
    width: 80vw;

    @media (min-width: 800px) {
      width: 500px;
    }
  }

  .supported-services-container {
    height: 250px;
    overflow: auto;
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
