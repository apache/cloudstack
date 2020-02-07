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
        <a-form-item :label="$t('label.supported.services')">
          <div class="supported-services-container" scroll-to="last-child">
            <a-list itemLayout="horizontal" :dataSource="this.supportedServices">
              <a-list-item slot="renderItem" slot-scope="item">
                <CheckBoxSelectPair
                  :resourceKey="item.name"
                  :resourceTitle="item.description"
                  :resourceOptions="item.provider"
                  @handle-check-change="handleSupportedServiceChange"/>
              </a-list-item>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item :label="$t('serviceofferingid')">
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
          <a-switch v-decorator="['redundant.router.capability']" />
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
          <a-switch v-decorator="['elasticLb']" />
        </a-form-item>
        <a-form-item :label="$t('service.Lb.inlineMode')" v-if="this.lbServiceChecked">
          <a-radio-group
            v-decorator="['inlineMode', {
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
            v-decorator="['netscalerServicePackages', {
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
            v-decorator="['lbNetscalerServicePackagesDescription', {}]"
            :placeholder="this.$t('netscaler.service.packages.description')"/>
        </a-form-item>
        <a-form-item :label="$t('service.Lb.lbIsolation')" v-if="this.lbServiceChecked">
          <a-radio-group
            v-decorator="['lbIsolation', {
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
          <a-switch v-decorator="['staticnatelasticip']" />
        </a-form-item>
        <a-form-item :label="$t('service.StaticNat.associatePublicIP')" v-if="this.staticNatServiceChecked">
          <a-switch v-decorator="['staticnatassociatepublicip']" />
        </a-form-item>
        <a-form-item :label="$t('service.Connectivity.supportsstrechedl2subnet')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['supportsstrechedl2subnet']" />
        </a-form-item>
        <a-form-item :label="$t('service.Connectivity.supportspublicaccess')" v-if="this.connectivityServiceChecked">
          <a-switch v-decorator="['supportspublicaccess']" />
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
      supportedServices: [],
      supportedServiceLoading: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      sourceNatServiceChecked: false,
      lbServiceChecked: false,
      staticNatServiceChecked: false,
      connectivityServiceChecked: false,
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
      this.fetchSupportedServiceData()
      this.fetchServiceOfferingData()
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
          var serviceDisplayName = ''

          // Sanitize names
          switch (serviceName) {
            case 'Vpn':
              serviceDisplayName = this.$t('label.vpn')
              break
            case 'Dhcp':
              serviceDisplayName = this.$t('label.dhcp')
              break
            case 'Dns':
              serviceDisplayName = this.$t('label.dns')
              break
            case 'Lb':
              serviceDisplayName = this.$t('label.load.balancer')
              break
            case 'SourceNat':
              serviceDisplayName = this.$t('label.source.nat')
              break
            case 'StaticNat':
              serviceDisplayName = this.$t('label.static.nat')
              break
            case 'PortForwarding':
              serviceDisplayName = this.$t('label.port.forwarding')
              break
            case 'UserData':
              serviceDisplayName = this.$t('label.user.data')
              break
            case 'Connectivity':
              serviceDisplayName = this.$t('label.virtual.networking')
              break
            default:
              serviceDisplayName = serviceName
              break
          }
          var providers = []
          for (var j in this.supportedServices[i].provider) {
            var provider = this.supportedServices[i].provider[j]
            provider.description = provider.name
            provider.enabled = provider.canenableindividualservice
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
