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
          <a-switch v-decorator="['ispersisitent', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.specifyvlan')">
          <a-switch v-decorator="['specifyvlan', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.vpc')">
          <a-switch v-decorator="['forvpc', {initialValue: this.isVpc}]" :defaultChecked="this.isVpc" @change="val => { this.isVpc = val }" />
        </a-form-item>
        <a-form-item :label="$t('userdatal2')">
          <a-switch v-decorator="['userdatal2', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('load.balancer.type')" v-if="this.isVpc && this.lbServiceChecked">
          <a-radio-group
            v-decorator="['lbType', {
              initialValue: 'publicLb'
            }]"
            buttonStyle="solid">
            <a-radio-button value="publicLb">
              {{ $t('Public LB') }}
            </a-radio-button>
            <a-radio-button value="internalLb">
              {{ $t('Internal LB') }}
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
        <a-form-item :label="$t('label.supported.services')">
          <div class="supported-services-container" scroll-to="last-child">
            <a-list itemLayout="horizontal" :dataSource="this.supportedServices">
              <a-list-item slot="renderItem" slot-scope="item">
                <CheckBoxSelectPair
                  v-decorator="['service.'+item.name, {}]"
                  :resourceKey="item.name"
                  :resourceTitle="item.description"
                  :resourceOptions="item.provider"
                  @handle-check-change="handleSupportedServiceChange"/>
              </a-list-item>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item :label="$t('serviceofferingid')" v-if="this.serviceOfferingVisible">
          <a-select
            v-decorator="['serviceofferingid', {
              rules: [
                {
                  required: true,
                  message: 'Please select option'
                }
              ],
              initialValue: 0
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="this.$t('serviceofferingid')">
            <a-select-option v-for="(opt, optIndex) in this.serviceOfferings" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('service.SourceNat.redundant.router.capability')" v-if="this.sourceNatServiceChecked">
          <a-switch v-decorator="['redundantroutercapability', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('service.SourceNat.sourceNatType')" v-if="this.sourceNatServiceChecked">
          <a-radio-group
            v-decorator="['sourcenattype', {
              initialValue: 'peraccount'
            }]"
            buttonStyle="solid">
            <a-radio-button value="peraccount">
              {{ $t('label.peraccount') }}
            </a-radio-button>
            <a-radio-button value="perzone">
              {{ $t('label.perzone') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('service.Lb.elasticLb')" v-if="this.lbServiceChecked">
          <a-switch v-decorator="['elasticlb', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('service.Lb.inlineMode')" v-if="this.lbServiceChecked">
          <a-radio-group
            v-decorator="['inlinemode', {
              initialValue: 'false'
            }]"
            buttonStyle="solid">
            <a-radio-button value="false">
              {{ $t('side by side') }}
            </a-radio-button>
            <a-radio-button value="true">
              {{ $t('inline') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('service.Lb.netscaler.service.packages')" v-if="this.lbServiceChecked">
          <a-select
            v-decorator="['netscalerservicepackages', {
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
            :loading="registeredServicePackageLoading"
            :placeholder="this.$t('netscaler.service.packages')">
            <a-select-option v-for="(opt, optIndex) in this.registeredServicePackages" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('service.Lb.netscaler.service.packages.description')" v-if="this.lbServiceChecked">
          <a-input
            v-decorator="['netscalerservicepackagesdescription', {}]"
            :placeholder="this.$t('netscaler.service.packages.description')"/>
        </a-form-item>
        <a-form-item :label="$t('service.Lb.lbIsolation')" v-if="this.lbServiceChecked">
          <a-radio-group
            v-decorator="['isolation', {
              initialValue: 'dedicated'
            }]"
            buttonStyle="solid">
            <a-radio-button value="dedicated">
              {{ $t('Dedicated') }}
            </a-radio-button>
            <a-radio-button value="shared">
              {{ $t('Shared') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('service.StaticNat.elasticIp')" v-if="this.staticNatServiceChecked">
          <a-switch v-decorator="['elasticip', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('service.StaticNat.associatePublicIP')" v-if="this.staticNatServiceChecked">
          <a-switch v-decorator="['associatepublicip', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('service.Connectivity.supportsstrechedl2subnet')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['supportsstrechedl2subnet', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('service.Connectivity.supportspublicaccess')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['supportspublicaccess', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('label.conservemode')">
          <a-switch v-decorator="['isconservemode', {initialValue: true}]" :defaultChecked="true" />
        </a-form-item>
        <a-form-item :label="$t('label.tags')">
          <a-input
            v-decorator="['tags', {}]"
            :placeholder="this.$t('label.networktags')"/>
        </a-form-item>
        <a-form-item :label="$t('availability')" v-if="this.availabilityVisible">
          <a-radio-group
            v-decorator="['availability', {
              initialValue: 'optional'
            }]"
            buttonStyle="solid">
            <a-radio-button value="optional">
              {{ $t('optional') }}
            </a-radio-button>
            <a-radio-button value="required">
              {{ $t('required') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('egressdefaultpolicy')" v-if="this.egressDefaultPolicyVisible">
          <a-radio-group
            v-decorator="['egressdefaultpolicy', {
              initialValue: 'allow'
            }]"
            buttonStyle="solid">
            <a-radio-button value="allow">
              {{ $t('Allow') }}
            </a-radio-button>
            <a-radio-button value="deny">
              {{ $t('Deny') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: this.isPublic}]" :defaultChecked="this.isPublic" @change="val => { this.isPublic = val }" />
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
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'

export default {
  name: 'AddNetworkOffering',
  components: {
    CheckBoxSelectPair
  },
  data () {
    return {
      guestType: 'isolated',
      promiscuousMode: '',
      macAddressChanges: '',
      forgedTransmits: '',
      selectedDomains: [],
      selectedZones: [],
      isVpc: false,
      supportedServices: [],
      supportedServiceLoading: false,
      serviceOfferingVisible: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      sourceNatServiceChecked: false,
      lbServiceChecked: false,
      staticNatServiceChecked: false,
      connectivityServiceChecked: false,
      availbilityVisible: false,
      egressDefaultPolicyVisible: false,
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
            provider.enabled = provider.canenableindividualservice
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
          console.log(this.supportedServices[i])
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
    handleSupportedServiceChange (service, checked) {
      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      }
      if (service === 'Lb') {
        if (checked) {
          this.fetchRegisteredServicePackageData()
        }
        this.lbServiceChecked = checked
      }
      if (service === 'StaticNat') {
        this.staticNatServiceChecked = checked
      }
      if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        console.log(values)
        var params = {}

        var keys = Object.keys(values)
        var ignoredKeys = ['state', 'status', 'allocationstate', 'forvpc', 'specifyvlan', 'ispublic', 'domainid', 'zoneid', 'egressdefaultpolicy', 'promiscuousmode', 'macaddresschanges', 'forgedtransmits']
        for (var i in keys) {
          var key = keys[i]
          if (!ignoredKeys.includes(key) &&
            !this.isSupportedServiceObject(values[key]) &&
            (key === 'availability' && values.availability !== 'Optional')) {
            params[key] = values[key]
          }
        }

        if (values.guesttype === 'Shared') { // specifyVlan checkbox is disabled, so inputData won't include specifyVlan
          params.specifyvlan = values.specifyvlan
          params.specifyipranges = true
          delete params.isPersistent // if Persistent checkbox is unchecked, do not pass isPersistent parameter to API call since we need to keep API call's size as small as possible (p.s. isPersistent is defaulted as false at server-side)
        } else if (values.guesttype === 'Isolated') { // specifyVlan checkbox is shown
          if (values.specifyvlan === true) {
            params.specifyvlan = true
          }
          if (values.ispersistent === true) {
            params.ispersistent = true
          } else { // Isolated Network with Non-persistent network
            delete params.ispersistent
          }
        } else if (values.guesttype === 'L2') {
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
        if (values.guesttype === 'Shared' || values.guesttype === 'Isolated') {
          if (values.conservemode !== true) { // if ConserveMode checkbox is checked, do not pass conservemode parameter to API call since we need to keep API call's size as small as possible (p.s. conservemode is defaulted as true at server-side)
            params.conservemode = false
          }
        }
        var selectedServices = null
        if (values.guesttype !== 'L2') {
          values.label
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
        }

        if ('egressdefaultpolicy' in values && values.egressdefaultpolicy !== 'allow') {
          params.egressdefaultpolicy = false // do not pass egressdefaultpolicy unnecessarily to API call  since we need to keep API call's size as small as possible (p.s. egressdefaultpolicy is defaulted as true at server-side)
        }
        if ('promiscuousmode' in values) {
          params['details[0].promiscuousMode'] = values.promiscuousmode
        }
        if ('macaddresschanges' in values) {
          params['details[0].macaddresschanges'] = values.macaddresschanges
        }
        if ('forgedtransmits' in values) {
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
        values.traffictype = 'GUEST' // traffic type dropdown has been removed since it has only one option ('Guest'). Hardcode traffic type value here.

        console.log(params)
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
    width: 60vw;

    @media (min-width: 450px) {
      width: 40vw;
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
