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
      <a-alert
        v-if="resource"
        type="info"
        style="margin-bottom: 16px">
        <template #message>
          <div style="display: block; width: 100%;">
            <div style="display: block; margin-bottom: 8px;">
              <strong>{{ $t('message.clone.offering.from') }}: {{ resource.name }}</strong>
            </div>
            <div style="display: block; font-size: 12px;">
              {{ $t('message.clone.offering.edit.hint') }}
            </div>
          </div>
        </template>
      </a-alert>
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
            <a-form-item name="provider" ref="provider">
              <template #label>
                <tooltip-label :title="$t('label.provider')" :tooltip="apiParams.provider.description"/>
              </template>
              <a-select
                v-model:value="form.provider"
                @change="val => handleProviderChange(val)"
                showSearch
                disabled
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                :placeholder="apiParams.provider.description" >
                <a-select-option key="" value="">{{ $t('label.none') }}</a-select-option>
                <a-select-option :value="'NSX'" :label="$t('label.nsx')"> {{ $t('label.nsx') }} </a-select-option>
                <a-select-option :value="'Netris'" :label="$t('label.netris')"> {{ $t('label.netris') }} </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12" v-if="form.provider === 'NSX'">
            <a-form-item name="nsxsupportlb" ref="nsxsupportlb">
              <template #label>
                <tooltip-label :title="$t('label.nsx.supports.lb')" :tooltip="apiParams.nsxsupportlb.description"/>
              </template>
              <a-switch v-model:checked="form.nsxsupportlb" @change="val => { handleNsxLbService(val) }" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="routingMode === 'dynamic' && form.provider === 'NSX'">
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
            :disabled="provider === 'NSX' || provider === 'Netris'"
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
            <tooltip-label :title="$t('label.supportedservices')" :tooltip="apiParams.supportedservices ? apiParams.supportedservices.description : ''"/>
          </template>
          <div class="supported-services-container" scroll-to="last-child">
            <a-spin v-if="!servicesReady" :spinning="true" />
            <a-list v-else itemLayout="horizontal" :dataSource="supportedServices">
              <template #renderItem="{ item }">
                <a-list-item>
                  <CheckBoxSelectPair
                    :key="`${item.name}-${item.selectedProvider || 'none'}`"
                    :resourceKey="item.name"
                    :checkBoxLabel="item.description"
                    :forExternalNetProvider="form.provider === 'NSX' || form.provider === 'Netris'"
                    :defaultCheckBoxValue="item.defaultChecked"
                    :defaultSelectValue="item.selectedProvider"
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
          <a-switch v-model:checked="form.ispublic" @change="val => { isPublic = val }" />
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="!isPublic">
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
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { BlockOutlined, GlobalOutlined } from '@ant-design/icons-vue'
import { buildVpcServiceCapabilityParams } from '@/composables/useServiceCapabilityParams'

export default {
  name: 'CloneVpcOffering',
  mixins: [mixinForm],
  components: {
    CheckBoxSelectPair,
    ResourceIcon,
    TooltipLabel,
    BlockOutlined,
    GlobalOutlined
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
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
      provider: '',
      loading: false,
      supportedServices: [],
      supportedServiceLoading: false,
      servicesReady: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      isVpcVirtualRouterForAtLeastOneService: false,
      connectivityServiceChecked: false,
      sourceNatServiceChecked: false,
      selectedServiceProviderMap: {},
      serviceProviderMap: {},
      ipv6NetworkOfferingEnabled: false,
      routedNetworkEnabled: false,
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
      isPublic: true,
      VPCVR: {
        name: 'VpcVirtualRouter',
        description: 'VpcVirtualRouter',
        enabled: true
      },
      NSX: {
        name: 'Nsx',
        description: 'Nsx',
        enabled: true
      },
      Netris: {
        name: 'Netris',
        description: 'Netris',
        enabled: true
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('cloneVPCOffering')
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.isPublic = isAdmin()
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        regionlevelvpc: true,
        distributedrouter: true,
        ispublic: this.isPublic,
        internetprotocol: this.internetProtocolValue,
        nsxsupportlb: true,
        routingmode: 'static',
        domainid: [],
        zoneid: []
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
      this.fetchIpv6NetworkOfferingConfiguration()
      this.fetchRoutedNetworkConfiguration()
      this.fetchSupportedServiceData()
    },
    isAdmin () {
      return isAdmin()
    },
    fetchIpv6NetworkOfferingConfiguration () {
      this.ipv6NetworkOfferingEnabled = false
      const params = { name: 'ipv6.offering.enabled' }
      getAPI('listConfigurations', params).then(json => {
        const value = json?.listconfigurationsresponse?.configuration?.[0].value || null
        this.ipv6NetworkOfferingEnabled = value === 'true'
      })
    },
    fetchRoutedNetworkConfiguration () {
      this.routedNetworkEnabled = false
      const params = { name: 'routed.network.vpc.enabled' }
      getAPI('listConfigurations', params).then(json => {
        const value = json?.listconfigurationsresponse?.configuration?.[0].value || null
        this.routedNetworkEnabled = value === 'true'
        if (!this.routedNetworkEnabled) {
          this.networkmodes.pop()
        }
      })
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
      params.details = 'min'
      this.domainLoading = true
      getAPI('listDomains', params).then(json => {
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
      getAPI('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    fetchSupportedServiceData () {
      this.supportedServiceLoading = true
      getAPI('listSupportedNetworkServices', {}).then(json => {
        const networkServices = json.listsupportednetworkservicesresponse.networkservice || []

        let services = []
        if (this.provider === 'NSX') {
          services = this.buildNsxServices(networkServices)
        } else if (this.provider === 'Netris') {
          services = this.buildNetrisServices(networkServices)
        } else {
          services = this.buildDefaultServices(networkServices)
        }

        if (this.networkmode === 'ROUTED') {
          services = services.filter(service => {
            return !['SourceNat', 'StaticNat', 'Lb', 'PortForwarding', 'Vpn'].includes(service.name)
          })
          if (['NSX', 'Netris'].includes(this.provider)) {
            services.push({
              name: 'Gateway',
              description: 'Gateway',
              enabled: true,
              provider: [{ name: this.provider }]
            })
          }
        }

        for (const i in services) {
          services[i].description = services[i].name
        }

        this.supportedServices = services
        this.supportedServiceLoading = false

        this.$nextTick(() => {
          this.populateFormFromResource()
        })
      })
    },
    buildNsxServices (networkServices) {
      return [
        { name: 'Dhcp', enabled: true, provider: [{ name: 'VpcVirtualRouter' }] },
        { name: 'Dns', enabled: true, provider: [{ name: 'VpcVirtualRouter' }] },
        { name: 'Lb', enabled: true, provider: [{ name: 'Nsx' }] },
        { name: 'StaticNat', enabled: true, provider: [{ name: 'Nsx' }] },
        { name: 'SourceNat', enabled: true, provider: [{ name: 'Nsx' }] },
        { name: 'NetworkACL', enabled: true, provider: [{ name: 'Nsx' }] },
        { name: 'PortForwarding', enabled: true, provider: [{ name: 'Nsx' }] },
        { name: 'UserData', enabled: true, provider: [{ name: 'VpcVirtualRouter' }] }
      ]
    },
    buildNetrisServices (networkServices) {
      return [
        { name: 'Dhcp', enabled: true, provider: [{ name: 'VpcVirtualRouter' }] },
        { name: 'Dns', enabled: true, provider: [{ name: 'VpcVirtualRouter' }] },
        { name: 'Lb', enabled: true, provider: [{ name: 'Netris' }] },
        { name: 'StaticNat', enabled: true, provider: [{ name: 'Netris' }] },
        { name: 'SourceNat', enabled: true, provider: [{ name: 'Netris' }] },
        { name: 'NetworkACL', enabled: true, provider: [{ name: 'Netris' }] },
        { name: 'PortForwarding', enabled: true, provider: [{ name: 'Netris' }] },
        { name: 'UserData', enabled: true, provider: [{ name: 'VpcVirtualRouter' }] }
      ]
    },
    buildDefaultServices (networkServices) {
      return [
        { name: 'Dhcp', provider: [{ name: 'VpcVirtualRouter' }, { name: 'ConfigDrive' }] },
        { name: 'Dns', provider: [{ name: 'VpcVirtualRouter' }, { name: 'ConfigDrive' }] },
        { name: 'Lb', provider: [{ name: 'VpcVirtualRouter' }, { name: 'InternalLbVm' }] },
        { name: 'Gateway', provider: [{ name: 'VpcVirtualRouter' }, { name: 'BigSwitchBcf' }] },
        { name: 'StaticNat', provider: [{ name: 'VpcVirtualRouter' }, { name: 'BigSwitchBcf' }] },
        { name: 'SourceNat', provider: [{ name: 'VpcVirtualRouter' }, { name: 'BigSwitchBcf' }] },
        { name: 'NetworkACL', provider: [{ name: 'VpcVirtualRouter' }, { name: 'BigSwitchBcf' }] },
        { name: 'PortForwarding', provider: [{ name: 'VpcVirtualRouter' }] },
        { name: 'UserData', provider: [{ name: 'VpcVirtualRouter' }, { name: 'ConfigDrive' }] },
        { name: 'Vpn', provider: [{ name: 'VpcVirtualRouter' }, { name: 'BigSwitchBcf' }] },
        { name: 'Connectivity', provider: [{ name: 'BigSwitchBcf' }, { name: 'NiciraNvp' }, { name: 'Ovs' }, { name: 'JuniperContrailVpcRouter' }] }
      ]
    },
    populateFormFromResource () {
      if (!this.resource) return

      const r = this.resource
      this.form.name = r.name + ' - Clone'
      this.form.displaytext = r.displaytext || r.name

      if (r.internetprotocol) {
        this.form.internetprotocol = r.internetprotocol.toLowerCase()
        this.internetProtocolValue = r.internetprotocol.toLowerCase()
      }

      if (r.service && Array.isArray(r.service)) {
        const networkAclService = r.service.find(svc => svc.name === 'NetworkACL')
        if (networkAclService && networkAclService.provider && networkAclService.provider.length > 0) {
          const providerName = networkAclService.provider[0].name
          if (providerName === 'Nsx') {
            this.provider = 'NSX'
            this.form.provider = 'NSX'
          } else if (providerName === 'Netris') {
            this.provider = 'Netris'
            this.form.provider = 'Netris'
          }
        }
      }

      if (r.networkmode) {
        this.networkmode = r.networkmode
        this.form.networkmode = r.networkmode
      }

      if (r.routingmode) {
        this.routingMode = r.routingmode
        this.form.routingmode = r.routingmode
      }

      if (r.serviceofferingid) {
        this.form.serviceofferingid = r.serviceofferingid
      }

      if (r.service && Array.isArray(r.service)) {
        const sourceServiceMap = {}
        r.service.forEach(svc => {
          if (svc.provider && svc.provider.length > 0) {
            const providerName = svc.provider[0].name
            sourceServiceMap[svc.name] = providerName
          }
        })

        this.serviceProviderMap = sourceServiceMap

        const updatedServices = this.supportedServices.map(svc => {
          const serviceCopy = { ...svc, provider: [...svc.provider] }

          if (sourceServiceMap[serviceCopy.name]) {
            const providerName = sourceServiceMap[serviceCopy.name]
            const providerIndex = serviceCopy.provider.findIndex(p => p.name === providerName)

            if (providerIndex > 0) {
              const targetProvider = serviceCopy.provider[providerIndex]
              serviceCopy.provider.splice(providerIndex, 1)
              serviceCopy.provider.unshift(targetProvider)
            }

            serviceCopy.defaultChecked = true
            serviceCopy.selectedProvider = providerName
            this.selectedServiceProviderMap[serviceCopy.name] = providerName
          } else {
            serviceCopy.defaultChecked = false
            serviceCopy.selectedProvider = null
          }
          return serviceCopy
        })

        this.supportedServices = updatedServices

        this.connectivityServiceChecked = Boolean(this.selectedServiceProviderMap.Connectivity)
        this.sourceNatServiceChecked = Boolean(this.selectedServiceProviderMap.SourceNat)

        this.$nextTick(() => {
          this.servicesReady = true
          this.$nextTick(() => {
            this.checkVpcVirtualRouterForServices()
          })
        })
      }

      if (r.service && Array.isArray(r.service)) {
        const connectivityService = r.service.find(svc => svc.name === 'Connectivity')
        if (connectivityService && connectivityService.capability) {
          const regionLevelCapability = connectivityService.capability.find(cap => cap.name === 'RegionLevelVpc')
          if (regionLevelCapability) {
            this.form.regionlevelvpc = regionLevelCapability.value === 'true'
          }
          const distributedRouterCapability = connectivityService.capability.find(cap => cap.name === 'DistributedRouter')
          if (distributedRouterCapability) {
            this.form.distributedrouter = distributedRouterCapability.value === 'true'
          }
        }

        const sourceNatService = r.service.find(svc => svc.name === 'SourceNat')
        if (sourceNatService && sourceNatService.capability) {
          const redundantRouterCapability = sourceNatService.capability.find(cap => cap.name === 'RedundantRouter')
          if (redundantRouterCapability) {
            this.form.redundantrouter = redundantRouterCapability.value === 'true'
          }
        }

        const gatewayService = r.service.find(svc => svc.name === 'Gateway')
        if (gatewayService && gatewayService.capability) {
          const redundantRouterCapability = gatewayService.capability.find(cap => cap.name === 'RedundantRouter')
          if (redundantRouterCapability) {
            this.form.redundantrouter = redundantRouterCapability.value === 'true'
          }
        }
      }

      if (this.provider === 'NSX') {
        this.form.nsxsupportlb = Boolean(this.serviceProviderMap.Lb)
      }
    },
    async handleProviderChange (value) {
      this.provider = value
      if (this.provider === 'NSX') {
        this.form.nsxsupportlb = true
        this.handleNsxLbService(true)
      }
      this.fetchSupportedServiceData()
    },
    handleNsxLbService (supportLb) {
      if (!supportLb) {
        this.supportedServices = this.supportedServices.filter(svc => svc.name !== 'Lb')
        delete this.selectedServiceProviderMap.Lb
      } else {
        const lbExists = this.supportedServices.some(svc => svc.name === 'Lb')
        if (!lbExists) {
          this.supportedServices.push({
            name: 'Lb',
            description: 'Lb',
            enabled: true,
            provider: [{ name: 'Nsx' }]
          })
        }
      }
    },
    handleSupportedServiceChange (service, checked, provider) {
      if (checked) {
        const correctProvider = this.serviceProviderMap[service]
        if (correctProvider && provider !== correctProvider) {
          this.selectedServiceProviderMap[service] = correctProvider
        } else {
          this.selectedServiceProviderMap[service] = provider
        }
      } else {
        delete this.selectedServiceProviderMap[service]
      }

      if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      }
      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      }

      this.checkVpcVirtualRouterForServices()
    },
    checkVpcVirtualRouterForServices () {
      this.isVpcVirtualRouterForAtLeastOneService = false
      const providers = Object.values(this.selectedServiceProviderMap)
      for (const provider of providers) {
        if (provider === 'VpcVirtualRouter') {
          this.isVpcVirtualRouterForAtLeastOneService = true
          break
        }
      }
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
      getAPI('listServiceOfferings', params).then(json => {
        const listServiceOfferings = json.listserviceofferingsresponse.serviceoffering
        this.serviceOfferings = this.serviceOfferings.concat(listServiceOfferings)
      }).finally(() => {
        this.serviceOfferingLoading = false
      })
    },
    handleSubmit (e) {
      if (e) {
        e.preventDefault()
      }
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {
          sourceofferingid: this.resource.id,
          name: values.name
        }
        if (values.displaytext) {
          params.displaytext = values.displaytext
        }

        if (values.ispublic !== true) {
          const domainIndexes = values.domainid
          let domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            const domainIds = []
            for (let i = 0; i < domainIndexes.length; i++) {
              domainIds.push(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }

        const zoneIndexes = values.zoneid
        let zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          const zoneIds = []
          for (let j = 0; j < zoneIndexes.length; j++) {
            zoneIds.push(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }

        if (values.internetprotocol) {
          params.internetprotocol = values.internetprotocol
        }

        const forNsx = values.provider === 'NSX'
        if (forNsx) {
          params.provider = 'NSX'
          params.nsxsupportlb = values.nsxsupportlb
        }
        const forNetris = values.provider === 'Netris'
        if (forNetris) {
          params.provider = 'Netris'
        }

        if (values.networkmode) {
          params.networkmode = values.networkmode
        }

        if (values.specifyasnumber !== undefined) {
          params.specifyasnumber = values.specifyasnumber
        }

        if (values.routingmode) {
          params.routingmode = values.routingmode
        }

        if (this.selectedServiceProviderMap != null) {
          const supportedServices = Object.keys(this.selectedServiceProviderMap)
          if (!forNsx && !forNetris) {
            params.supportedservices = supportedServices.join(',')
          }
          for (const k in supportedServices) {
            params['serviceProviderList[' + k + '].service'] = supportedServices[k]
            params['serviceProviderList[' + k + '].provider'] = this.selectedServiceProviderMap[supportedServices[k]]
          }
          buildVpcServiceCapabilityParams(params, values, this.selectedServiceProviderMap, this.isVpcVirtualRouterForAtLeastOneService)
        } else {
          params.supportedservices = ''
        }

        if (values.enable !== undefined) {
          params.enable = values.enable
        }

        this.loading = true
        postAPI('cloneVPCOffering', params).then(json => {
          this.$message.success(`${this.$t('message.success.clone.vpc.offering')} ${values.name}`)
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
