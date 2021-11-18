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
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            autoFocus
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.name') }]
            }]"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: $t('message.error.description') }]
            }]"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.networkrate')" :tooltip="apiParams.networkrate.description"/>
          <a-input
            v-decorator="['networkrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.validate.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.networkrate.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.guestiptype')" :tooltip="apiParams.guestiptype.description"/>
          <a-radio-group
            v-decorator="['guestiptype', {
              initialValue: guestType
            }]"
            buttonStyle="solid"
            @change="selected => { handleGuestTypeChange(selected.target.value) }">
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
        <a-row :gutter="12" v-if="guestType !== 'shared'">
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.ispersistent')" :tooltip="apiParams.ispersistent.description"/>
              <a-switch v-decorator="['ispersistent', {initialValue: false}]" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.specifyvlan')" :tooltip="apiParams.specifyvlan.description"/>
              <a-switch v-decorator="['specifyvlan', {initialValue: true}]" :defaultChecked="true" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="guestType === 'isolated'">
          <tooltip-label slot="label" :title="$t('label.vpc')" :tooltip="apiParams.forvpc.description"/>
          <a-switch v-decorator="['forvpc', {initialValue: forVpc}]" :defaultChecked="forVpc" @change="val => { handleForVpcChange(val) }" />
        </a-form-item>
        <a-form-item :label="$t('label.userdatal2')" v-if="guestType === 'l2'">
          <a-switch v-decorator="['userdatal2', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.lbtype')" v-if="forVpc && lbServiceChecked">
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
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.promiscuousmode')" :tooltip="$t('message.network.offering.promiscuous.mode')"/>
              <a-radio-group
                v-decorator="['promiscuousmode', {
                  initialValue: ''
                }]"
                buttonStyle="solid">
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
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.macaddresschanges')" :tooltip="$t('message.network.offering.mac.address.changes')"/>
              <a-radio-group
                v-decorator="['macaddresschanges', {
                  initialValue: ''
                }]"
                buttonStyle="solid">
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
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.forgedtransmits')" :tooltip="$t('message.network.offering.forged.transmits')"/>
              <a-radio-group
                v-decorator="['forgedtransmits', {
                  initialValue: ''
                }]"
                buttonStyle="solid">
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
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.maclearning')" :tooltip="$t('message.network.offering.mac.learning')"/>
              <span v-if="macLearningValue !== ''">
                <a-alert type="warning">
                  <span slot="message" v-html="$t('message.network.offering.mac.learning.warning')" />
                </a-alert>
                <br/>
              </span>
              <a-radio-group
                v-decorator="['maclearning', {
                  initialValue: macLearningValue
                }]"
                buttonStyle="solid"
                @change="e => { macLearningValue = e.target.value }">
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
          </a-col>
        </a-row>
        <a-form-item v-if="guestType !== 'l2'">
          <tooltip-label slot="label" :title="$t('label.supportedservices')" :tooltip="apiParams.supportedservices.description"/>
          <div class="supported-services-container" scroll-to="last-child">
            <a-list itemLayout="horizontal" :dataSource="supportedServices">
              <a-list-item slot="renderItem" slot-scope="item">
                <CheckBoxSelectPair
                  v-decorator="['service.'+item.name, {}]"
                  :resourceKey="item.name"
                  :checkBoxLabel="item.description"
                  :checkBoxDecorator="'service.' + item.name"
                  :selectOptions="item.provider"
                  :selectDecorator="item.name + '.provider'"
                  @handle-checkselectpair-change="handleSupportedServiceChange"/>
              </a-list-item>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item v-if="isVirtualRouterForAtLeastOneService">
          <tooltip-label slot="label" :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
          <a-select
            v-decorator="['serviceofferingid', {
              rules: [
                {
                  required: true,
                  message: $t('message.error.select')
                }
              ],
              initialValue: serviceOfferings.length > 0 ? serviceOfferings[0].id : ''
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="apiParams.serviceofferingid.description">
            <a-select-option v-for="(opt) in serviceOfferings" :key="opt.id">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.redundantrouter')" v-if="(guestType === 'shared' || guestType === 'isolated') && sourceNatServiceChecked && !isVpcVirtualRouterForAtLeastOneService">
          <a-switch v-decorator="['redundantroutercapability', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.sourcenattype')" v-if="(guestType === 'shared' || guestType === 'isolated') && sourceNatServiceChecked">
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
        <a-form-item :label="$t('label.service.lb.elasticlbcheckbox')" v-if="guestType == 'shared' && lbServiceChecked && lbServiceProvider === 'Netscaler'">
          <a-switch v-decorator="['elasticlb', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.inlinemodedropdown')" v-if="(guestType === 'shared' || guestType === 'isolated') && lbServiceChecked && firewallServiceChecked && lbServiceProvider === 'F5BigIp' && firewallServiceProvider === 'JuniperSRX'">
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
        <a-form-item :label="$t('label.service.lb.netscaler.servicepackages')" v-if="(guestType === 'shared' || guestType === 'isolated') && lbServiceChecked && lbServiceProvider === 'Netscaler'">
          <a-select
            v-decorator="['netscalerservicepackages', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="registeredServicePackageLoading"
            :placeholder="$t('label.service.lb.netscaler.servicepackages')">
            <a-select-option v-for="(opt, optIndex) in registeredServicePackages" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.service.lb.netscaler.servicepackages.description')" v-if="(guestType === 'shared' || guestType === 'isolated') && lbServiceChecked && lbServiceProvider === 'Netscaler'">
          <a-input
            v-decorator="['netscalerservicepackagesdescription', {}]"
            :placeholder="$t('label.service.lb.netscaler.servicepackages.description')"/>
        </a-form-item>
        <a-form-item :title="$t('label.service.lb.lbisolationdropdown')" v-show="false">
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
        <a-form-item :label="$t('label.service.staticnat.elasticipcheckbox')" v-if="guestType == 'shared' && staticNatServiceChecked && staticNatServiceProvider === 'Netscaler'">
          <a-switch v-decorator="['elasticip', {initialValue: isElasticIp}]" :defaultChecked="isElasticIp" @change="val => { isElasticIp = val }" />
        </a-form-item>
        <a-form-item :label="$t('label.service.staticnat.associatepublicip')" v-if="isElasticIp && staticNatServiceChecked && staticNatServiceProvider === 'Netscaler'">
          <a-switch v-decorator="['associatepublicip', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.supportsstrechedl2subnet')" v-if="connectivityServiceChecked">
          <a-switch v-decorator="['supportsstrechedl2subnet', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.supportspublicaccess')" v-show="false">
          <a-switch v-decorator="['supportspublicaccess', {initialValue: false}]" />
        </a-form-item>
        <a-form-item v-if="(guestType === 'shared' || guestType === 'isolated') && !isVpcVirtualRouterForAtLeastOneService">
          <tooltip-label slot="label" :title="$t('label.conservemode')" :tooltip="apiParams.conservemode.description"/>
          <a-switch v-decorator="['conservemode', {initialValue: true}]" :defaultChecked="true" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.tags')" :tooltip="apiParams.tags.description"/>
          <a-input
            v-decorator="['tags', {}]"
            :placeholder="apiParams.tags.description"/>
        </a-form-item>
        <a-form-item v-if="requiredNetworkOfferingExists && guestType === 'isolated' && sourceNatServiceChecked">
          <tooltip-label slot="label" :title="$t('label.availability')" :tooltip="apiParams.availability.description"/>
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
        <a-form-item v-if="firewallServiceChecked">
          <tooltip-label slot="label" :title="$t('label.egressdefaultpolicy')" :tooltip="apiParams.egressdefaultpolicy.description"/>
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
        <a-form-item :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: isPublic}]" :defaultChecked="isPublic" @change="val => { isPublic = val }" />
        </a-form-item>
        <a-form-item v-if="!isPublic">
          <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
              rules: [
                {
                  required: true,
                  message: $t('message.error.select')
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="block" style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
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
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="global" style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="apiParams.enable">
          <tooltip-label slot="label" :title="$t('label.enable.network.offering')" :tooltip="apiParams.enable.description"/>
          <a-switch v-decorator="['enable', {initialValue: false}]" />
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
import { api } from '@/api'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddNetworkOffering',
  components: {
    CheckBoxSelectPair,
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      hasAdvanceZone: false,
      requiredNetworkOfferingExists: false,
      guestType: 'isolated',
      selectedDomains: [],
      selectedZones: [],
      forVpc: false,
      macLearningValue: '',
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
    this.apiParams = this.$getApiParams('createNetworkOffering')
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
      params.listAll = true
      params.showicon = true
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
      if (this.loading) return
      const options = {
        scroll: {
          offsetTop: 10
        }
      }
      this.form.validateFieldsAndScroll(options, (err, values) => {
        if (err) {
          return
        }
        this.loading = true
        var params = {}

        var self = this
        var selectedServices = null
        var keys = Object.keys(values)
        const detailsKey = ['promiscuousmode', 'macaddresschanges', 'forgedtransmits', 'maclearning']
        const ignoredKeys = [...detailsKey, 'state', 'status', 'allocationstate', 'forvpc', 'specifyvlan', 'ispublic', 'domainid', 'zoneid', 'egressdefaultpolicy', 'isolation', 'supportspublicaccess']
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
        for (const key of detailsKey) {
          if (values[key]) {
            params['details[0].' + key] = values[key]
          }
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
      width: 600px;
    }
  }
  .supported-services-container {
    height: 250px;
    overflow: auto;
  }
</style>
