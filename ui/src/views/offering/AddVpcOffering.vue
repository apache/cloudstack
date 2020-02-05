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
        <a-form-item :label="$t('regionlevelvpc')" v-if="this.regionLevelVpcVisible">
          <a-switch v-decorator="['regionlevelvpc']" defaultChecked="true" />
        </a-form-item>
        <a-form-item :label="$t('distributedrouter')" v-if="this.distributedRouterVisible">
          <a-switch v-decorator="['distributedrouter']" defaultChecked="true" />
        </a-form-item>
        <a-form-item :label="$t('redundant.router.capability')" v-if="this.redundantRouterCapabilityVisible">
          <a-switch v-decorator="['redundant.router.capability']" />
        </a-form-item>
        <a-form-item :label="$t('ispublic')" v-if="this.isAdmin()">
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
      regionLevelVpcVisible: false,
      distributedRouterVisible: false,
      redundantRouterCapabilityVisible: false
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
    fetchSupportedServiceData () {
      // const params = {}
      // params.listAll = true
      // this.supportedServiceLoading = true
      // api('listSupportedNetworkServices', params).then(json => {
      //   const networkServiceObjs = json.listsupportednetworkservicesresponse.networkservice
      //   var fields = {}; var providerCanenableindividualserviceMap = {}; var providerServicesMap = {}; var providerDropdownsForciblyChangedTogether = {}
      //   for (i in networkServiceObjs) {
      //     var networkServiceObj = networkServiceObjs[i]
      //     var serviceName = networkServiceObj.name;
      //     var providerObjs = networkServiceObj.provider;
      //     var serviceDisplayName;

      //     // Sanitize names
      //     switch (serviceName) {
      //         case 'Vpn':
      //             serviceDisplayName = _l('label.vpn');
      //             break;
      //         case 'Dhcp':
      //             serviceDisplayName = _l('label.dhcp');
      //             break;
      //         case 'Dns':
      //             serviceDisplayName = _l('label.dns');
      //             break;
      //         case 'Lb':
      //             serviceDisplayName = _l('label.load.balancer');
      //             break;
      //         case 'SourceNat':
      //             serviceDisplayName = _l('label.source.nat');
      //             break;
      //         case 'StaticNat':
      //             serviceDisplayName = _l('label.static.nat');
      //             break;
      //         case 'PortForwarding':
      //             serviceDisplayName = _l('label.port.forwarding');
      //             break;
      //         case 'SecurityGroup':
      //             serviceDisplayName = _l('label.security.groups');
      //             break;
      //         case 'UserData':
      //             serviceDisplayName = _l('label.user.data');
      //             break;
      //         case 'Connectivity':
      //             serviceDisplayName = _l('label.virtual.networking');
      //             break;
      //         default:
      //             serviceDisplayName = serviceName;
      //             break;
      //     }

      //     var id = {
      //         isEnabled: 'service' + '.' + serviceName + '.' + 'isEnabled',
      //         capabilities: 'service' + '.' + serviceName + '.' + 'capabilities',
      //         provider: 'service' + '.' + serviceName + '.' + 'provider'
      //     };

      //     serviceCheckboxNames.push(id.isEnabled);

      //     fields[id.isEnabled] = {
      //         label: serviceDisplayName,
      //         isBoolean: true
      //     };
      //     serviceFields.push(id.isEnabled);

      //     if (providerObjs != null && providerObjs.length > 1) { //present provider dropdown when there are multiple providers for a service
      //         fields[id.provider] = {
      //             label: serviceDisplayName + ' Provider',
      //             isHidden: true,
      //             dependsOn: id.isEnabled,
      //             select: function(args) {
      //                 //Virtual Router needs to be the first choice in provider dropdown (Bug 12509)
      //                 var items = [];
      //                 for (j in providerObjs) {
      //                   var providerObj = providerObjs[j]
      //                     if (this.name == "VirtualRouter")
      //                         items.unshift({
      //                             id: this.name,
      //                             description: this.name
      //                         });
      //                     else
      //                         items.push({
      //                             id: this.name,
      //                             description: this.name
      //                         });

      //                     if (!(this.name in providerCanenableindividualserviceMap))
      //                         providerCanenableindividualserviceMap[this.name] = this.canenableindividualservice;

      //                     if (!(this.name in providerServicesMap))
      //                         providerServicesMap[this.name] = [serviceName];
      //                     else
      //                         providerServicesMap[this.name].push(serviceName);
      //                 }

      //                 args.response.success({
      //                     data: items
      //                 });

      //                 // Disable VPC virtual router by default
      //                 args.$select.find('option[value=VpcVirtualRouter]').attr('disabled', true);

      //                 args.$select.change(function() {
      //                     var $thisProviderDropdown = $(this);
      //                     var providerName = $(this).val();
      //                     var canenableindividualservice = providerCanenableindividualserviceMap[providerName];
      //                     if (canenableindividualservice == false) { //This provider can NOT enable individual service, therefore, force all services supported by this provider have this provider selected in provider dropdown
      //                         var serviceNames = providerServicesMap[providerName];
      //                         if (serviceNames != null && serviceNames.length > 1) {
      //                             providerDropdownsForciblyChangedTogether = {}; //reset
      //                             $(serviceNames).each(function() {
      //                                 var providerDropdownId = 'service' + '.' + this + '.' + 'provider';
      //                                 providerDropdownsForciblyChangedTogether[providerDropdownId] = 1;
      //                                 $("select[name='" + providerDropdownId + "']").val(providerName);
      //                             });
      //                         }
      //                     } else { //canenableindividualservice == true
      //                         if (this.name in providerDropdownsForciblyChangedTogether) { //if this provider dropdown is one of provider dropdowns forcibly changed together earlier, make other forcibly changed provider dropdowns restore default option (i.e. 1st option in dropdown)
      //                             for (var key in providerDropdownsForciblyChangedTogether) {
      //                                 if (key == this.name)
      //                                     continue; //skip to next item in for loop
      //                                 else
      //                                     $("select[name='" + key + "'] option:first").attr("selected", "selected");
      //                             }
      //                             providerDropdownsForciblyChangedTogether = {}; //reset
      //                         }
      //                     }
      //                 });
      //             }
      //         };
      //     } else if (providerObjs != null && providerObjs.length == 1) { //present hidden field when there is only one provider for a service
      //         fields[id.provider] = {
      //             label: serviceDisplayName + ' Provider',
      //             isHidden: true,
      //             defaultValue: providerObjs[0].name
      //         };
      //     }
      //   });
      //   this.supportedServices = this.supportedServices.concat(networkServiceObjs)
      // }).finally(() => {
      //   this.supportedServiceLoading = false
      // })
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
        var serviceDisplayName
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
          default:
            serviceDisplayName = serviceName
            break
        }
        this.supportedServices[i].description = serviceDisplayName
      }
    },
    handleSupportedServiceChange (service, checked) {
      if (service === 'Connectivity') {
        this.regionLevelVpcVisible = checked
        this.distributedRouterVisible = checked
      }
      if (service === 'SourceNat') {
        this.redundantRouterCapabilityVisible = checked
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
        params.description = values.description
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
          params.zoneid = zoneId
        }
        console.log(values, params)
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
