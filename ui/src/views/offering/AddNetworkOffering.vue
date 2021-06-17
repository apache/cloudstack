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
            {{ $t('label.networkrate') }}
            <a-tooltip :title="apiParams.networkrate.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['networkrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.validate.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.networkrate')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.guestiptype') }}
            <a-tooltip :title="apiParams.guestiptype.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-radio-group
            v-decorator="['guestiptype', {
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
        <a-form-item v-if="this.guestType !== 'shared'">
          <span slot="label">
            {{ $t('label.ispersistent') }}
            <a-tooltip :title="apiParams.ispersistent.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['ispersistent', {initialValue: false}]" />
        </a-form-item>
        <a-form-item v-if="this.guestType !== 'shared'">
          <span slot="label">
            {{ $t('label.specifyvlan') }}
            <a-tooltip :title="apiParams.specifyvlan.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['specifyvlan', {initialValue: true}]" :defaultChecked="true" />
        </a-form-item>
        <a-form-item v-if="this.guestType === 'isolated'">
          <span slot="label">
            {{ $t('label.vpc') }}
            <a-tooltip :title="apiParams.forvpc.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['forvpc', {initialValue: this.forVpc}]" :defaultChecked="this.forVpc" @change="val => { this.handleForVpcChange(val) }" />
        </a-form-item>
        <a-form-item :label="$t('label.userdatal2')" v-if="this.guestType === 'l2'">
          <a-switch v-decorator="['userdatal2', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.lbtype')" v-if="this.forVpc && this.lbServiceChecked">
          <a-radio-group
            v-decorator="[' ', {
              initialValue: 'publicLb'
            }]"
            buttonStyle="solid">
            <a-radio-button value="publicLb">
              {{ $t('label.public.lb') }}
            </a-radio-button>
            <a-radio-button value="internalLb">
              {{ $t('label.internal.lb') }}
            </a-radio-button>
          </a-radio-group>
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
            <a-radio-button value="true">
              {{ $t('label.accept') }}
            </a-radio-button>
            <a-radio-button value="false">
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
            <a-radio-button value="true">
              {{ $t('label.accept') }}
            </a-radio-button>
            <a-radio-button value="false">
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
            <a-radio-button value="true">
              {{ $t('label.accept') }}
            </a-radio-button>
            <a-radio-button value="false">
              {{ $t('label.reject') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="this.guestType !== 'l2'">
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
        <a-form-item v-if="this.isVirtualRouterForAtLeastOneService">
          <span slot="label">
            {{ $t('label.serviceofferingid') }}
            <a-tooltip :title="apiParams.serviceofferingid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            v-decorator="['serviceofferingid', {
              rules: [
                {
                  required: true,
                  message: `${this.$t('message.error.select')}`
                }
              ],
              initialValue: this.serviceOfferings.length > 0 ? this.serviceOfferings[0].id : ''
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="this.$t('label.serviceofferingid')">
            <a-select-option v-for="(opt) in this.serviceOfferings" :key="opt.id">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.redundantrouter')" v-if="(this.guestType === 'shared' || this.guestType === 'isolated') && this.sourceNatServiceChecked && !this.isVpcVirtualRouterForAtLeastOneService">
          <a-switch v-decorator="['redundantroutercapability', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.sourcenattype')" v-if="(this.guestType === 'shared' || this.guestType === 'isolated') && this.sourceNatServiceChecked">
          <a-radio-group
            v-decorator="['sourcenattype', {
              initialValue: 'peraccount'
            }]"
            buttonStyle="solid">
            <a-radio-button value="peraccount">
              {{ $t('label.per.account') }}
            </a-radio-button>
            <a-radio-button value="perzone">
              {{ $t('label.per.zone') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.elasticlbcheckbox')" v-if="this.guestType == 'shared' && this.lbServiceChecked && this.lbServiceProvider === 'Netscaler'">
          <a-switch v-decorator="['elasticlb', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.inlinemodedropdown')" v-if="(this.guestType === 'shared' || this.guestType === 'isolated') && this.lbServiceChecked && this.firewallServiceChecked && this.lbServiceProvider === 'F5BigIp' && this.firewallServiceProvider === 'JuniperSRX'">
          <a-radio-group
            v-decorator="['inlinemode', {
              initialValue: 'false'
            }]"
            buttonStyle="solid">
            <a-radio-button value="false">
              {{ $t('side.by.side') }}
            </a-radio-button>
            <a-radio-button value="true">
              {{ $t('inline') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.netscaler.servicepackages')" v-if="(this.guestType === 'shared' || this.guestType === 'isolated') && this.lbServiceChecked && this.lbServiceProvider === 'Netscaler'">
          <a-select
            v-decorator="['netscalerservicepackages', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="registeredServicePackageLoading"
            :placeholder="this.$t('label.service.lb.netscaler.servicepackages')">
            <a-select-option v-for="(opt, optIndex) in this.registeredServicePackages" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.netscaler.servicepackages.description')" v-if="(this.guestType === 'shared' || this.guestType === 'isolated') && this.lbServiceChecked && this.lbServiceProvider === 'Netscaler'">
          <a-input
            v-decorator="['netscalerservicepackagesdescription', {}]"
            :placeholder="this.$t('label.service.lb.netscaler.servicepackages.description')"/>
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.lbisolationdropdown')" v-show="false">
          <a-radio-group
            v-decorator="['isolation', {
              initialValue: 'dedicated'
            }]"
            buttonStyle="solid">
            <a-radio-button value="dedicated">
              {{ $t('label.dedicated') }}
            </a-radio-button>
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.service.staticnat.elasticipcheckbox')" v-if="this.guestType == 'shared' && this.staticNatServiceChecked && this.staticNatServiceProvider === 'Netscaler'">
          <a-switch v-decorator="['elasticip', {initialValue: this.isElasticIp}]" :defaultChecked="this.isElasticIp" @change="val => { this.isElasticIp = val }" />
        </a-form-item>
        <a-form-item :label="$t('label.service.staticnat.associatepublicip')" v-if="this.isElasticIp && this.staticNatServiceChecked && this.staticNatServiceProvider === 'Netscaler'">
          <a-switch v-decorator="['associatepublicip', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.supportsstrechedl2subnet')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['supportsstrechedl2subnet', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.supportspublicaccess')" v-show="false">
          <a-switch v-decorator="['supportspublicaccess', {initialValue: false}]" />
        </a-form-item>
        <a-form-item v-if="(this.guestType === 'shared' || this.guestType === 'isolated') && !this.isVpcVirtualRouterForAtLeastOneService">
          <span slot="label">
            {{ $t('label.conservemode') }}
            <a-tooltip :title="apiParams.conservemode.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['conservemode', {initialValue: true}]" :defaultChecked="true" />
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.tags') }}
            <a-tooltip :title="apiParams.tags.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['tags', {}]"
            :placeholder="this.$t('label.tags')"/>
        </a-form-item>
        <a-form-item v-if="this.requiredNetworkOfferingExists && this.guestType === 'isolated' && this.sourceNatServiceChecked">
          <span slot="label">
            {{ $t('label.availability') }}
            <a-tooltip :title="apiParams.availability.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-radio-group
            v-decorator="['availability', {
              initialValue: 'optional'
            }]"
            buttonStyle="solid">
            <a-radio-button value="optional">
              {{ $t('label.optional') }}
            </a-radio-button>
            <a-radio-button value="required">
              {{ $t('label.required') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="this.firewallServiceChecked">
          <span slot="label">
            {{ $t('label.egressdefaultpolicy') }}
            <a-tooltip :title="apiParams.egressdefaultpolicy.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-radio-group
            v-decorator="['egressdefaultpolicy', {
              initialValue: 'allow'
            }]"
            buttonStyle="solid">
            <a-radio-button value="allow">
              {{ $t('label.allow') }}
            </a-radio-button>
            <a-radio-button value="deny">
              {{ $t('label.deny') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.ispublic')" v-show="this.isAdmin()">
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
            {{ $t('label.enable.network.offering') }}
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
  name: 'AddNetworkOffering',
  components: {
    CheckBoxSelectPair
  },
  data () {
    return {
      hasAdvanceZone: false,
      requiredNetworkOfferingExists: false,
      guestType: 'isolated',
      promiscuousMode: '',
      macAddressChanges: '',
      forgedTransmits: '',
      selectedDomains: [],
      selectedZones: [],
      forVpc: false,
      supportedServices: [],
      supportedServiceLoading: false,
      isVirtualRouterForAtLeastOneService: false,
      isVpcVirtualRouterForAtLeastOneService: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      sourceNatServiceChecked: false,
      lbServiceChecked: false,
      lbServiceProvider: '',
      isElasticIp: false,
      staticNatServiceChecked: false,
      staticNatServiceProvider: '',
      connectivityServiceChecked: false,
      firewallServiceChecked: false,
      firewallServiceProvider: '',
      selectedServiceProviderMap: {},
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
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.createNetworkOffering || {}
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
      this.fetchServiceOfferingData()
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
    fetchSupportedServiceData () {
      const params = {}
      params.listAll = true
      this.supportedServiceLoading = true
      this.supportedServices = []
      api('listSupportedNetworkServices', params).then(json => {
        this.supportedServices = json.listsupportednetworkservicesresponse.networkservice
        for (var i in this.supportedServices) {
          var networkServiceObj = this.supportedServices[i]
          var serviceName = networkServiceObj.name
          var serviceDisplayName = serviceName

          // Sanitize names
          // switch (serviceName) {
          //   case 'Vpn':
          //     serviceDisplayName = this.$t('label.vpn')
          //     break
          //   case 'Dhcp':
          //     serviceDisplayName = this.$t('label.dhcp')
          //     break
          //   case 'Dns':
          //     serviceDisplayName = this.$t('label.dns')
          //     break
          //   case 'Lb':
          //     serviceDisplayName = this.$t('label.load.balancer')
          //     break
          //   case 'SourceNat':
          //     serviceDisplayName = this.$t('label.source.nat')
          //     break
          //   case 'StaticNat':
          //     serviceDisplayName = this.$t('label.static.nat')
          //     break
          //   case 'PortForwarding':
          //     serviceDisplayName = this.$t('label.port.forwarding')
          //     break
          //   case 'UserData':
          //     serviceDisplayName = this.$t('label.user.data')
          //     break
          //   case 'Connectivity':
          //     serviceDisplayName = this.$t('label.virtual.networking')
          //     break
          //   default:
          //     serviceDisplayName = serviceName
          //     break
          // }
          var providers = []
          for (var j in this.supportedServices[i].provider) {
            var provider = this.supportedServices[i].provider[j]
            provider.description = provider.name
            provider.enabled = true
            if (provider.name === 'VpcVirtualRouter') {
              provider.enabled = false
            }
            if (provider.name === 'VirtualRouter') {
              providers.unshift(provider)
            } else {
              providers.push(provider)
            }
          }
          this.supportedServices[i].provider = providers
          this.supportedServices[i].description = serviceDisplayName
        }
      })
    },
    fetchServiceOfferingData () {
      const params = {}
      params.issystem = true
      params.systemvmtype = 'domainrouter'
      this.supportedServiceLoading = true
      api('listServiceOfferings', params).then(json => {
        const listServiceOfferings = json.listserviceofferingsresponse.serviceoffering
        this.serviceOfferings = this.serviceOfferings.concat(listServiceOfferings)
      }).finally(() => {
        this.supportedServiceLoading = false
      })
    },
    fetchRegisteredServicePackageData () {
      this.registeredServicePackageLoading = true
      this.registeredServicePackages = []
      api('listRegisteredServicePackages', {}).then(json => {
        var servicePackages = json.listregisteredservicepackage.registeredServicepackage
        if (servicePackages === undefined || servicePackages == null || !servicePackages) {
          servicePackages = json.listregisteredservicepackage
        }
        for (var i in servicePackages) {
          this.registeredServicePackages.push({
            id: servicePackages[i].id,
            description: servicePackages[i].name,
            desc: servicePackages[i].description
          })
        }
      }).finally(() => {
        this.registeredServicePackageLoading = false
      })
    },
    handleForVpcChange (forVpc) {
      var self = this
      this.forVpc = forVpc
      this.supportedServices.forEach(function (svc, index) {
        if (svc !== 'Connectivity') {
          var providers = svc.provider
          providers.forEach(function (provider, providerIndex) {
            if (self.forVpc) { // *** vpc ***
              if (provider.name === 'InternalLbVm' || provider.name === 'VpcVirtualRouter' || provider.name === 'Netscaler' || provider.name === 'BigSwitchBcf' || provider.name === 'ConfigDrive') {
                provider.enabled = true
              } else {
                provider.enabled = false
              }
            } else { // *** non-vpc ***
              if (provider.name === 'InternalLbVm' || provider.name === 'VpcVirtualRouter') {
                provider.enabled = false
              } else {
                provider.enabled = true
              }
            }
            providers[providerIndex] = provider
          })
          svc.provider = providers
          self.supportedServices[index] = svc
        }
      })
    },
    handleSupportedServiceChange (service, checked, provider) {
      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      } else if (service === 'Lb') {
        if (checked) {
          this.fetchRegisteredServicePackageData()
          if (provider != null & provider !== undefined) {
            this.lbServiceProvider = provider
          }
        } else {
          this.lbServiceProvider = ''
        }
        this.lbServiceChecked = checked
      } else if (service === 'StaticNat') {
        this.staticNatServiceChecked = checked
        if (checked && provider != null & provider !== undefined) {
          this.staticNatServiceProvider = provider
        } else {
          this.staticNatServiceProvider = ''
        }
      } else if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      } else if (service === 'Firewall') {
        this.firewallServiceChecked = checked
        if (checked && provider != null & provider !== undefined) {
          this.staticNatServiceProvider = provider
        } else {
          this.staticNatServiceProvider = ''
        }
      }
      if (checked && provider != null & provider !== undefined) {
        this.selectedServiceProviderMap[service] = provider
      } else {
        delete this.selectedServiceProviderMap[service]
      }
      var providers = Object.values(this.selectedServiceProviderMap)
      this.isVirtualRouterForAtLeastOneService = false
      this.isVpcVirtualRouterForAtLeastOneService = false
      var self = this
      providers.forEach(function (prvdr, idx) {
        if (prvdr === 'VirtualRouter') {
          self.isVirtualRouterForAtLeastOneService = true
          if (self.serviceOfferings.length === 0) {
            self.fetchServiceOfferingData()
          }
        }
        if (prvdr === 'VpcVirtualRouter') {
          self.isVpcVirtualRouterForAtLeastOneService = true
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        var params = {}

        var self = this
        var selectedServices = null
        var keys = Object.keys(values)
        var ignoredKeys = ['state', 'status', 'allocationstate', 'forvpc', 'specifyvlan', 'ispublic', 'domainid', 'zoneid', 'egressdefaultpolicy', 'promiscuousmode', 'macaddresschanges', 'forgedtransmits', 'isolation', 'supportspublicaccess']
        keys.forEach(function (key, keyIndex) {
          if (self.isSupportedServiceObject(values[key])) {
            if (selectedServices == null) {
              selectedServices = {}
            }
            selectedServices[key] = values[key]
          } else {
            if (!ignoredKeys.includes(key) &&
              values[key] != null && values[key] !== undefined &&
              !(key === 'availability' && values[key] === 'Optional')) {
              params[key] = values[key]
            }
          }
        })

        if (values.guestiptype === 'shared') { // specifyVlan checkbox is disabled, so inputData won't include specifyVlan
          params.specifyvlan = true
          params.specifyipranges = true
          delete params.ispersistent
        } else if (values.guestiptype === 'isolated') { // specifyVlan checkbox is shown
          if (values.specifyvlan === true) {
            params.specifyvlan = true
          }
          if (values.ispersistent) {
            params.ispersistent = true
          } else { // Isolated Network with Non-persistent network
            delete params.ispersistent
          }
        } else if (values.guestiptype === 'l2') {
          if (values.specifyvlan === true) {
            params.specifyvlan = true
          }
          if (values.userdatal2 === true) {
            params['serviceProviderList[0].service'] = 'UserData'
            params['serviceProviderList[0].provider'] = 'ConfigDrive'
            params.supportedservices = 'UserData'
          }
          // Conserve mode is irrelevant on L2 network offerings as there are no resources to conserve, do not pass it, true by default on server side
          delete params.conservemode
        }

        if (values.forvpc === true) {
          params.forvpc = true
        }
        if (values.guestiptype === 'shared' || values.guestiptype === 'isolated') {
          if (values.conservemode !== true) {
            params.conservemode = false
          }
        }
        if (selectedServices != null) {
          var supportedServices = Object.keys(selectedServices)
          params.supportedservices = supportedServices.join(',')
          for (var k in supportedServices) {
            params['serviceProviderList[' + k + '].service'] = supportedServices[k]
            params['serviceProviderList[' + k + '].provider'] = selectedServices[supportedServices[k]].provider
          }
          var serviceCapabilityIndex = 0
          if (supportedServices.includes('Connectivity')) {
            if (values.supportsstrechedl2subnet === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RegionLevelVpc'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.supportspublicaccess === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'DistributedRouter'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            delete params.supportsstrechedl2subnet
            delete params.supportspublicaccess
          }
          if (supportedServices.includes('SourceNat')) {
            if (values.redundantroutercapability === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'SourceNat'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RedundantRouter'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'SourceNat'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'SupportedSourceNatTypes'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.sourcenattype
            serviceCapabilityIndex++
            delete params.redundantroutercapability
            delete params.sourcenattype
          }
          if (supportedServices.includes('SourceNat')) {
            if (values.elasticip === true) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'ElasticIp'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.elasticip === true || values.associatepublicip === true) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'associatePublicIP'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.associatepublicip
              serviceCapabilityIndex++
            }
            delete params.elasticip
            delete params.associatepublicip
          }
          if (supportedServices.includes('Lb')) {
            if (values.elasticlb === true) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'ElasticLb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.inlinemode === true && ((selectedServices.Lb.provider === 'F5BigIp') || (selectedServices.Lb.provider === 'Netscaler'))) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'InlineMode'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.inlinemode
              serviceCapabilityIndex++
            }
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'SupportedLbIsolation'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.isolation
            serviceCapabilityIndex++
            if (selectedServices.Lb.provider === 'InternalLbVm') {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'lbSchemes'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = 'internal'
              serviceCapabilityIndex++
            }
            if ('netscalerservicepackages' in values &&
              this.registeredServicePackages.length > values.netscalerservicepackages &&
              'netscalerservicepackagesdescription' in values) {
              params['details[' + 0 + '].servicepackageuuid'] = this.registeredServicePackages[values.netscalerservicepackages].id
              params['details[' + 1 + '].servicepackagedescription'] = values.netscalerservicepackagesdescription
            }
          }
        } else {
          if (!('supportedservices' in params)) {
            params.supportedservices = ''
          }
        }

        if ('egressdefaultpolicy' in values && values.egressdefaultpolicy !== 'allow') {
          params.egressdefaultpolicy = false
        }
        if (values.promiscuousmode) {
          params['details[0].promiscuousMode'] = values.promiscuousmode
        }
        if (values.macaddresschanges) {
          params['details[0].macaddresschanges'] = values.macaddresschanges
        }
        if (values.forgedtransmits) {
          params['details[0].forgedtransmits'] = values.forgedtransmits
        }
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
        if (values.enable) {
          params.enable = values.enable
        }
        params.traffictype = 'GUEST' // traffic type dropdown has been removed since it has only one option ('Guest'). Hardcode traffic type value here.
        api('createNetworkOffering', params).then(json => {
          this.$message.success('Network offering created: ' + values.name)
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
