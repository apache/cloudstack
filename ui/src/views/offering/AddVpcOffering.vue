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
  <div class="form-layout" @keyup.ctrl.enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical"
       >
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-focus="true"
            v-model:value="form.name"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item name="internetprotocol" ref="internetprotocol">
          <template #label>
            <tooltip-label :title="$t('label.internetprotocol')" :tooltip="apiParams.internetprotocol.description"/>
          </template>
          <span v-if="!ipv6NetworkOfferingEnabled || internetProtocolValue!=='ipv4'">
            <a-alert type="warning">
              <template #message>
                <span v-html="ipv6NetworkOfferingEnabled ? $t('message.offering.internet.protocol.warning') : $t('message.offering.ipv6.warning')" />
              </template>
            </a-alert>
            <br/>
          </span>
          <a-radio-group
            v-model:value="form.internetprotocol"
            :disabled="!ipv6NetworkOfferingEnabled"
            buttonStyle="solid"
            @change="e => { internetProtocolValue = e.target.value }" >
            <a-radio-button value="ipv4">
              {{ $t('label.ip.v4') }}
            </a-radio-button>
            <a-radio-button value="dualstack">
              {{ $t('label.ip.v4.v6') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item name="fornsx" ref="fornsx">
              <template #label>
                <tooltip-label :title="$t('label.nsx')" :tooltip="apiParams.fornsx.description"/>
              </template>
              <a-switch v-model:checked="form.fornsx" @change="val => { handleForNsxChange(val) }" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12" v-if="forNsx">
            <a-form-item name="nsxsupportlb" ref="nsxsupportlb">
              <template #label>
                <tooltip-label :title="$t('label.nsx.supports.lb')" :tooltip="apiParams.nsxsupportlb.description"/>
              </template>
              <a-switch v-model:checked="form.nsxsupportlb" @change="val => { handleNsxLbService(val) }" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="routingMode === 'dynamic' && forNsx">
          <a-col :md="12" :lg="12">
            <a-form-item name="specifyasnumber" ref="specifyasnumber">
              <template #label>
                <tooltip-label :title="$t('label.specifyasnumber')"/>
              </template>
              <a-switch v-model:checked="form.specifyasnumber" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="networkmode" ref="networkmode">
          <template #label>
            <tooltip-label :title="$t('label.networkmode')" :tooltip="apiParams.networkmode.description"/>
          </template>
          <a-select
            optionFilterProp="label"
            v-model:value="form.networkmode"
            @change="val => { handleForNetworkModeChange(val) }"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.networkmode.description">
            <a-select-option v-for="(opt) in networkmodes" :key="opt.name" :label="opt.name">
              {{ opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="routingmode" ref="routingmode" v-if="networkmode === 'ROUTED' || internetProtocolValue === 'ipv6' || internetProtocolValue === 'dualstack'">
          <template #label>
            <tooltip-label :title="$t('label.routingmode')" :tooltip="apiParams.routingmode.description"/>
          </template>
          <a-radio-group
            v-model:value="form.routingmode"
            buttonStyle="solid"
            @change="selected => { routingMode = selected.target.value }">
            <a-radio-button value="static">
              {{ $t('label.static') }}
            </a-radio-button>
            <a-radio-button value="dynamic">
              {{ $t('label.dynamic') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <template #label>
            <tooltip-label :title="$t('label.supportedservices')" :tooltip="apiParams.supportedservices.description"/>
          </template>
          <div class="supported-services-container" scroll-to="last-child">
            <a-list itemLayout="horizontal" :dataSource="supportedServices">
              <template #renderItem="{ item }">
                <a-list-item>
                  <CheckBoxSelectPair
                    :resourceKey="item.name"
                    :checkBoxLabel="item.description"
                    :forNsx="forNsx"
                    :defaultCheckBoxValue="forNsx"
                    :selectOptions="item.provider"
                    @handle-checkselectpair-change="handleSupportedServiceChange"/>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item name="regionlevelvpc" ref="regionlevelvpc" :label="$t('label.service.connectivity.regionlevelvpccapabilitycheckbox')" v-if="connectivityServiceChecked">
          <a-switch v-model:checked="form.regionlevelvpc" />
        </a-form-item>
        <a-form-item name="distributedrouter" ref="distributedrouter" :label="$t('label.service.connectivity.distributedroutercapabilitycheckbox')" v-if="connectivityServiceChecked">
          <a-switch v-model:checked="form.distributedrouter" />
        </a-form-item>
        <a-form-item name="redundantrouter" ref="redundantrouter" :label="$t('label.redundantrouter')" v-if="sourceNatServiceChecked">
          <a-switch v-model:checked="form.redundantrouter" />
        </a-form-item>
        <a-form-item name="serviceofferingid" ref="serviceofferingid">
          <a-alert v-if="!isVpcVirtualRouterForAtLeastOneService" type="warning" style="margin-bottom: 10px">
            <template #message>
              <span v-html="$t('message.vr.alert.upon.network.offering.creation.others')" />
            </template>
          </a-alert>
          <template #label>
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
          </template>
          <a-select
            showSearch
            optionFilterProp="label"
            v-model:value="form.serviceofferingid"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="apiParams.serviceofferingid.description">
            <a-select-option v-for="(opt) in serviceOfferings" :key="opt.id" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
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
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
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
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="enable" ref="enable" v-if="apiParams.enable">
          <template #label>
            <tooltip-label :title="$t('label.enable.vpc.offering')" :tooltip="apiParams.enable.description"/>
          </template>
          <a-switch v-model:checked="form.enable" />
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddVpcOffering',
  mixins: [mixinForm],
  components: {
    CheckBoxSelectPair,
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      selectedDomains: [],
      selectedZones: [],
      isConserveMode: true,
      internetProtocolValue: 'ipv4',
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      forNsx: false,
      loading: false,
      supportedServices: [],
      supportedServiceLoading: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      isVpcVirtualRouterForAtLeastOneService: false,
      connectivityServiceChecked: false,
      sourceNatServiceChecked: false,
      selectedServiceProviderMap: {},
      ipv6NetworkOfferingEnabled: false,
      routingMode: 'static',
      networkmode: '',
      networkmodes: [
        {
          id: 0,
          name: 'NATTED'
        },
        {
          id: 1,
          name: 'ROUTED'
        }
      ],
      VPCVR: {
        name: 'VPCVirtualRouter',
        description: 'VPCVirtualRouter',
        enabled: true
      },
      NSX: {
        name: 'Nsx',
        description: 'Nsx',
        enabled: true
      },
      nsxSupportedServicesMap: {}
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVPCOffering')
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
      this.form = reactive({
        regionlevelvpc: true,
        distributedrouter: true,
        ispublic: true,
        internetprotocol: this.internetProtocolValue,
        nsxsupportlb: true,
        routingmode: 'static'
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
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
      this.fetchDomainData()
      this.fetchZoneData()
      this.fetchSupportedServiceData()
      this.fetchIpv6NetworkOfferingConfiguration()
    },
    isAdmin () {
      return isAdmin()
    },
    fetchIpv6NetworkOfferingConfiguration () {
      this.ipv6NetworkOfferingEnabled = false
      var params = { name: 'ipv6.offering.enabled' }
      api('listConfigurations', params).then(json => {
        var value = json?.listconfigurationsresponse?.configuration?.[0].value || null
        this.ipv6NetworkOfferingEnabled = value === 'true'
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
      })
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    fetchSupportedServiceData () {
      var services = []
      if (this.forNsx) {
        services.push({
          name: 'Dhcp',
          enabled: true,
          provider: [
            { name: 'VpcVirtualRouter' }
          ]
        })
        services.push({
          name: 'Dns',
          enabled: true,
          provider: [{ name: 'VpcVirtualRouter' }]
        })
        services.push({
          name: 'Lb',
          enabled: true,
          provider: [{ name: 'Nsx' }]
        })
        services.push({
          name: 'StaticNat',
          enabled: true,
          provider: [{ name: 'Nsx' }]
        })
        services.push({
          name: 'SourceNat',
          enabled: true,
          provider: [{ name: 'Nsx' }]
        })
        services.push({
          name: 'NetworkACL',
          enabled: true,
          provider: [{ name: 'Nsx' }]
        })
        services.push({
          name: 'PortForwarding',
          enabled: true,
          provider: [{ name: 'Nsx' }]
        })
        services.push({
          name: 'UserData',
          enabled: true,
          provider: [{ name: 'VpcVirtualRouter' }]
        })
      } else {
        services.push({
          name: 'Dhcp',
          provider: [
            { name: 'VpcVirtualRouter' }
          ]
        })
        services.push({
          name: 'Dns',
          provider: [{ name: 'VpcVirtualRouter' }]
        })
        services.push({
          name: 'Lb',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'InternalLbVm' }
          ]
        })
        services.push({
          name: 'Gateway',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'BigSwitchBcf' }
          ]
        })
        services.push({
          name: 'StaticNat',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'BigSwitchBcf' }
          ]
        })
        services.push({
          name: 'SourceNat',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'BigSwitchBcf' }
          ]
        })
        services.push({
          name: 'NetworkACL',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'BigSwitchBcf' }
          ]
        })
        services.push({
          name: 'PortForwarding',
          provider: [{ name: 'VpcVirtualRouter' }]
        })
        services.push({
          name: 'UserData',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'ConfigDrive' }
          ]
        })
        services.push({
          name: 'Vpn',
          provider: [
            { name: 'VpcVirtualRouter' },
            { name: 'BigSwitchBcf' }
          ]
        })
        services.push({
          name: 'Connectivity',
          provider: [
            { name: 'BigSwitchBcf' },
            { name: 'NiciraNvp' },
            { name: 'Ovs' },
            { name: 'JuniperContrailVpcRouter' }
          ]
        })
      }
      this.supportedServices = []
      if (this.networkmode === 'ROUTED') {
        services = services.filter(service => {
          return !['SourceNat', 'StaticNat', 'Lb', 'PortForwarding', 'Vpn'].includes(service.name)
        })
      }
      for (var i in services) {
        services[i].description = services[i].name
      }
      var self = this
      setTimeout(() => {
        self.supportedServices = services
        self.supportedServiceLoading = false
      }, 50)
    },
    async handleForNsxChange (forNsx) {
      this.forNsx = forNsx
      if (forNsx) {
        this.form.nsxsupportlb = true
        this.handleNsxLbService(true)
      }
      this.fetchSupportedServiceData()
    },
    handleNsxLbService (supportLb) {
      if (!supportLb) {
        this.supportedServices = this.supportedServices.filter(svc => svc.name !== 'Lb')
      }
      if (supportLb) {
        this.supportedServices.push({
          name: 'Lb',
          enabled: true,
          provider: [{ name: 'Nsx' }]
        })
      }
    },
    handleSupportedServiceChange (service, checked, provider) {
      if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      }
      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      }
      if (checked && provider != null & provider !== undefined) {
        this.selectedServiceProviderMap[service] = provider
      } else {
        delete this.selectedServiceProviderMap[service]
      }
      this.isVpcVirtualRouterForAtLeastOneService = false
      const providers = Object.values(this.selectedServiceProviderMap)
      const self = this
      providers.forEach(function (prvdr, idx) {
        if (prvdr === 'VpcVirtualRouter') {
          self.isVpcVirtualRouterForAtLeastOneService = true
        }
      })
      if (this.isVpcVirtualRouterForAtLeastOneService && this.serviceOfferings.length === 0) {
        this.fetchServiceOfferingData()
      }
    },
    handleForNetworkModeChange (networkMode) {
      this.networkmode = networkMode
      this.fetchSupportedServiceData()
    },
    fetchServiceOfferingData () {
      const params = {}
      params.issystem = true
      params.systemvmtype = 'domainrouter'
      this.serviceOfferingLoading = true
      api('listServiceOfferings', params).then(json => {
        const listServiceOfferings = json.listserviceofferingsresponse.serviceoffering
        this.serviceOfferings = this.serviceOfferings.concat(listServiceOfferings)
      }).finally(() => {
        this.serviceOfferingLoading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
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
        if (values.internetprotocol) {
          params.internetprotocol = values.internetprotocol
        }
        if (values.fornsx === true) {
          params.fornsx = true
          params.nsxsupportlb = values.nsxsupportlb
        }
        params.networkmode = values.networkmode
        if (!values.forVpc) {
          params.specifyasnumber = values.specifyasnumber
        }
        params.routingmode = values.routingmode
        if (this.selectedServiceProviderMap != null) {
          var supportedServices = Object.keys(this.selectedServiceProviderMap)
          params.supportedservices = []
          if (!this.forNsx) {
            params.supportedservices = supportedServices.join(',')
          }
          for (var k in supportedServices) {
            params['serviceProviderList[' + k + '].service'] = supportedServices[k]
            params['serviceProviderList[' + k + '].provider'] = this.selectedServiceProviderMap[supportedServices[k]]
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
          } else if (values.redundantrouter === true) {
            params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Gateway'
            params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RedundantRouter'
            params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
            serviceCapabilityIndex++
          }
          if (values.serviceofferingid && this.isVpcVirtualRouterForAtLeastOneService) {
            params.serviceofferingid = values.serviceofferingid
          }
        } else {
          params.supportedservices = []
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
</style>
