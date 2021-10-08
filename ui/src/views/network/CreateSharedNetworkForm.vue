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
  <a-spin :spinning="loading">
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <div class="form">
        <a-form
          :form="form"
          layout="vertical"
          @submit="handleSubmit">
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
            <a-input
              v-decorator="['name', {
                rules: [{ required: true, message: $t('message.error.name') }]
              }]"
              :placeholder="this.$t('label.name')"
              autoFocus />
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
            <a-input
              v-decorator="['displaytext', {
                rules: [{ required: true, message: $t('message.error.display.text') }]
              }]"
              :placeholder="this.$t('label.display.text')"/>
          </a-form-item>
          <a-form-item v-if="this.isObjectEmpty(this.zone)">
            <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
            <a-select
              v-decorator="['zoneid', {
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
                return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              :placeholder="this.$t('label.zoneid')"
              @change="val => { this.handleZoneChange(this.zones[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <a-icon v-else type="global" style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="this.isObjectEmpty(this.zone)">
            <tooltip-label slot="label" :title="$t('label.physicalnetworkid')" :tooltip="apiParams.physicalnetworkid.description"/>
            <a-select
              v-decorator="['physicalnetworkid', {}]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="formPhysicalNetworkLoading"
              :placeholder="this.$t('label.physicalnetworkid')"
              @change="val => { this.handlePhysicalNetworkChange(this.formPhysicalNetworks[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.formPhysicalNetworks" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.vlan')" :tooltip="apiParams.vlan.description"/>
            <a-input
              v-decorator="['vlanid', {
                rules: [{ required: true, message: $t('message.please.enter.value') }]
              }]"
              :placeholder="this.$t('label.vlanid')"/>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.bypassvlanoverlapcheck')" :tooltip="apiParams.bypassvlanoverlapcheck.description"/>
            <a-switch v-decorator="['bypassvlanoverlapcheck']" />
          </a-form-item>
          <a-form-item v-if="!this.isObjectEmpty(this.selectedNetworkOffering) && this.selectedNetworkOffering.specifyvlan">
            <tooltip-label slot="label" :title="$t('label.isolatedpvlantype')" :tooltip="apiParams.isolatedpvlantype.description"/>
            <a-radio-group
              v-decorator="['isolatedpvlantype', {
                initialValue: this.isolatePvlanType
              }]"
              buttonStyle="solid"
              @change="selected => { this.handleIsolatedPvlanTypeChange(selected.target.value) }">
              <a-radio-button value="none">
                {{ $t('label.none') }}
              </a-radio-button>
              <a-radio-button value="community">
                {{ $t('label.community') }}
              </a-radio-button>
              <a-radio-button value="isolated">
                {{ $t('label.secondary.isolated.vlan.type.isolated') }}
              </a-radio-button>
              <a-radio-button value="promiscuous">
                {{ $t('label.secondary.isolated.vlan.type.promiscuous') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-if="this.isolatePvlanType=='community' || this.isolatePvlanType=='isolated'">
            <tooltip-label slot="label" :title="$t('label.isolatedpvlanid')" :tooltip="apiParams.isolatedpvlan.description"/>
            <a-input
              v-decorator="['isolatedpvlan', {}]"
              :placeholder="this.$t('label.isolatedpvlanid')"/>
          </a-form-item>
          <a-form-item :label="$t('label.scope')">
            <a-radio-group
              v-decorator="['scope', {
                initialValue: this.scopeType
              }]"
              buttonStyle="solid"
              @change="selected => { this.handleScopeTypeChange(selected.target.value) }">
              <a-radio-button value="all">
                {{ $t('label.all') }}
              </a-radio-button>
              <a-radio-button value="domain" v-if="!this.parseBooleanValueForKey(this.selectedZone, 'securitygroupsenabled')">
                {{ $t('label.domain') }}
              </a-radio-button>
              <a-radio-button value="account" v-if="!this.parseBooleanValueForKey(this.selectedZone, 'securitygroupsenabled')">
                {{ $t('label.account') }}
              </a-radio-button>
              <a-radio-button value="project" v-if="!this.parseBooleanValueForKey(this.selectedZone, 'securitygroupsenabled')">
                {{ $t('label.project') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-if="this.scopeType !== 'all'">
            <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
            <a-select
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
                return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="domainLoading"
              :placeholder="this.$t('label.domainid')"
              @change="val => { this.handleDomainChange(this.domains[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <a-icon v-else-if="optIndex !== 0" type="block" style="margin-right: 5px" />
                  {{ opt.path || opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="this.scopeType === 'domain'">
            <tooltip-label slot="label" :title="$t('label.subdomainaccess')" :tooltip="apiParams.subdomainaccess.description"/>
            <a-switch v-decorator="['subdomainaccess']" />
          </a-form-item>
          <a-form-item v-if="this.scopeType === 'account'">
            <tooltip-label slot="label" :title="$t('label.account')" :tooltip="apiParams.account.description"/>
            <a-input
              v-decorator="['account', {}]"
              :placeholder="this.$t('label.account')"/>
          </a-form-item>
          <a-form-item v-if="this.scopeType === 'project'">
            <tooltip-label slot="label" :title="$t('label.projectid')" :tooltip="apiParams.projectid.description"/>
            <a-select
              v-decorator="['projectid', {
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
                return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="projectLoading"
              :placeholder="this.$t('label.projectid')"
              @change="val => { this.handleProjectChange(this.projects[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.projects" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <a-icon v-else-if="optIndex !== 0" type="project" style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.networkofferingid')" :tooltip="apiParams.networkofferingid.description"/>
            <a-select
              v-decorator="['networkofferingid', {
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
              :loading="networkOfferingLoading"
              :placeholder="this.$t('label.networkofferingid')"
              @change="val => { this.handleNetworkOfferingChange(this.networkOfferings[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.networkOfferings" :key="optIndex">
                {{ opt.displaytext || opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.ip4gateway')" :tooltip="apiParams.gateway.description"/>
                <a-input
                  v-decorator="['ip4gateway', {}]"
                  :placeholder="this.$t('label.ip4gateway')"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.netmask')" :tooltip="apiParams.netmask.description"/>
                <a-input
                  v-decorator="['netmask', {}]"
                  :placeholder="this.$t('label.netmask')"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.startipv4')" :tooltip="apiParams.startip.description"/>
                <a-input
                  v-decorator="['startipv4', {}]"
                  :placeholder="this.$t('label.startipv4')"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.endipv4')" :tooltip="apiParams.endip.description"/>
                <a-input
                  v-decorator="['endipv4', {}]"
                  :placeholder="this.$t('label.endipv4')"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item v-if="isVirtualRouterForAtLeastOneService">
            <tooltip-label slot="label" :title="$t('label.routerip')" :tooltip="apiParams.routerip.description"/>
            <a-input
              v-decorator="['routerip', {}]"
              :placeholder="this.$t('label.routerip')"/>
          </a-form-item>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.ip6gateway')" :tooltip="apiParams.ip6gateway.description"/>
                <a-input
                  v-decorator="['ip6gateway', {}]"
                  :placeholder="this.$t('label.ip6gateway')"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.ip6cidr')" :tooltip="apiParams.ip6cidr.description"/>
                <a-input
                  v-decorator="['ip6cidr', {}]"
                  :placeholder="this.$t('label.ip6cidr')"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.startipv6')" :tooltip="apiParams.startipv6.description"/>
                <a-input
                  v-decorator="['startipv6', {}]"
                  :placeholder="this.$t('label.startipv6')"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item>
                <tooltip-label slot="label" :title="$t('label.endipv6')" :tooltip="apiParams.endipv6.description"/>
                <a-input
                  v-decorator="['endipv6', {}]"
                  :placeholder="this.$t('label.endipv6')"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item v-if="isVirtualRouterForAtLeastOneService">
            <tooltip-label slot="label" :title="$t('label.routeripv6')" :tooltip="apiParams.routeripv6.description"/>
            <a-input
              v-decorator="['routeripv6', {}]"
              :placeholder="this.$t('label.routeripv6')"/>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
            <a-input
              v-decorator="['networkdomain', {}]"
              :placeholder="this.$t('label.networkdomain')"/>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.hideipaddressusage')" :tooltip="apiParams.hideipaddressusage.description"/>
            <a-switch v-decorator="['hideipaddressusage']" />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ this.$t('label.cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              type="primary"
              ref="submit"
              @click="handleSubmit">
              {{ this.$t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateGuestNetworkForm',
  components: {
    TooltipLabel,
    ResourceIcon
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    zone: {
      type: Object,
      default: null
    },
    physicalNetworks: {
      type: Array,
      default: null
    },
    resource: {
      type: Object,
      default: () => { return {} }
    }
  },
  data () {
    return {
      actionLoading: false,
      zones: [],
      zoneLoading: false,
      selectedZone: {},
      formPhysicalNetworks: [],
      formPhysicalNetworkLoading: false,
      formSelectedPhysicalNetwork: {},
      isolatePvlanType: 'none',
      scopeType: 'all',
      domains: [],
      domainLoading: false,
      selectedDomain: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      selectedNetworkOffering: {},
      projects: [],
      projectLoading: false,
      selectedProject: {},
      isVirtualRouterForAtLeastOneService: false,
      selectedServiceProviderMap: {}
    }
  },
  watch: {
    resource (newItem, oldItem) {
      this.fetchData()
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createNetwork')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (this.isObjectEmpty(this.zone)) {
        this.fetchZoneData()
      } else {
        this.fetchNetworkOfferingData()
      }
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    parseBooleanValueForKey (obj, key) {
      return this.isValidValueForKey(obj, key) && obj[key] === true
    },
    isValidTextValueForKey (obj, key) {
      return this.isValidValueForKey(obj, key) && obj[key].length > 0
    },
    fetchZoneData () {
      this.zones = []
      if (this.zone !== null) {
        this.zones.push(this.zone)
        if (this.arrayHasItems(this.zones)) {
          this.form.setFieldsValue({
            zoneid: 0
          })
          this.handleZoneChange(this.zones[0])
        }
      } else {
        const params = {}
        if (this.resource.zoneid && this.$route.name === 'deployVirtualMachine') {
          params.id = this.resource.zoneid
        }
        params.listAll = true
        params.showicon = true
        this.zoneLoading = true
        api('listZones', params).then(json => {
          for (const i in json.listzonesresponse.zone) {
            const zone = json.listzonesresponse.zone[i]
            if (zone.networktype === 'Advanced') {
              this.zones.push(zone)
            }
          }
        }).finally(() => {
          this.zoneLoading = false
          if (this.arrayHasItems(this.zones)) {
            this.form.setFieldsValue({
              zoneid: 0
            })
            this.handleZoneChange(this.zones[0])
          }
        })
      }
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      this.fetchPhysicalNetworkData()
    },
    fetchPhysicalNetworkData () {
      this.formSelectedPhysicalNetwork = {}
      this.formPhysicalNetworks = []
      if (this.physicalNetworks != null) {
        this.formPhysicalNetworks = this.physicalNetworks
        this.selectFirstPhysicalNetwork()
      } else {
        if (this.selectedZone === null || this.selectedZone === undefined) {
          return
        }
        const promises = []
        const params = {
          zoneid: this.selectedZone.id
        }
        this.formPhysicalNetworkLoading = true
        api('listPhysicalNetworks', params).then(json => {
          var networks = json.listphysicalnetworksresponse.physicalnetwork
          if (this.arrayHasItems(networks)) {
            for (const network of networks) {
              promises.push(this.addPhysicalNetworkForGuestTrafficType(network))
            }
          } else {
            this.formPhysicalNetworkLoading = false
          }
        }).finally(() => {
          if (this.arrayHasItems(promises)) {
            Promise.all(promises).catch(error => {
              this.$notifyError(error)
            }).finally(() => {
              this.formPhysicalNetworkLoading = false
              this.selectFirstPhysicalNetwork()
            })
          }
        })
      }
    },
    selectFirstPhysicalNetwork () {
      if (this.arrayHasItems(this.formPhysicalNetworks)) {
        this.form.setFieldsValue({
          physicalnetworkid: 0
        })
        this.handlePhysicalNetworkChange(this.formPhysicalNetworks[0])
      }
    },
    addPhysicalNetworkForGuestTrafficType (physicalNetwork) {
      const params = {}
      params.physicalnetworkid = physicalNetwork.id
      return new Promise((resolve, reject) => {
        api('listTrafficTypes', params).then(json => {
          var trafficTypes = json.listtraffictypesresponse.traffictype
          if (this.arrayHasItems(trafficTypes)) {
            for (const type of trafficTypes) {
              if (type.traffictype === 'Guest') {
                this.formPhysicalNetworks.push(physicalNetwork)
                break
              }
            }
          }
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },
    handlePhysicalNetworkChange (physicalNet) {
      this.formSelectedPhysicalNetwork = physicalNet
      this.fetchNetworkOfferingData()
    },
    handleIsolatedPvlanTypeChange (pvlan) {
      this.isolatePvlanType = pvlan
    },
    handleScopeTypeChange (scope) {
      this.scopeType = scope
      switch (scope) {
        case 'domain':
        {
          this.fetchDomainData()
          break
        }
        case 'project':
        {
          this.fetchDomainData()
          this.fetchProjectData()
          this.fetchNetworkOfferingData()
          break
        }
        default:
        {
          this.fetchNetworkOfferingData()
        }
      }
    },
    fetchNetworkOfferingData () {
      if (this.isObjectEmpty(this.selectedZone)) {
        return
      }
      this.networkOfferingLoading = true
      var params = {
        zoneid: this.selectedZone.id,
        state: 'Enabled'
      }
      if (!this.isObjectEmpty(this.formSelectedPhysicalNetwork) &&
        this.formSelectedPhysicalNetwork.tags &&
        this.formSelectedPhysicalNetwork.tags.length > 0) {
        params.tags = this.formSelectedPhysicalNetwork.tags
      }
      // Network tab in Guest Traffic Type in Infrastructure menu is only available when it's under Advanced zone.
      // zone dropdown in add guest network dialog includes only Advanced zones.
      if (this.scopeType === 'all' || this.scopeType === 'domain') {
        params.guestiptype = 'Shared'
        if (this.scopeType === 'domain') {
          params.domainid = this.selectedDomain.id
        }
      }
      this.handleNetworkOfferingChange(null)
      this.networkOfferings = []
      api('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering
        this.handleNetworkOfferingChange(this.networkOfferings[0])
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.arrayHasItems(this.networkOfferings)) {
          this.form.setFieldsValue({
            networkofferingid: 0
          })
        } else {
          this.form.setFieldsValue({
            networkofferingid: null
          })
        }
      })
    },
    handleNetworkOfferingChange (networkOffering) {
      this.selectedNetworkOffering = networkOffering
      if (networkOffering) {
        this.networkServiceProviderMap(this.selectedNetworkOffering.id)
      }
    },
    networkServiceProviderMap (id) {
      api('listNetworkOfferings', { id: id }).then(json => {
        var networkOffering = json.listnetworkofferingsresponse.networkoffering[0]
        const services = networkOffering.service
        this.selectedServiceProviderMap = {}
        for (const svc of services) {
          this.selectedServiceProviderMap[svc.name] = svc.provider[0].name
        }
        var providers = Object.values(this.selectedServiceProviderMap)
        this.isVirtualRouterForAtLeastOneService = false
        var self = this
        providers.forEach(function (prvdr, idx) {
          if (prvdr === 'VirtualRouter') {
            self.isVirtualRouterForAtLeastOneService = true
          }
        })
      })
    },
    fetchDomainData () {
      const params = {}
      if (!this.isObjectEmpty(this.selectedZone) && this.selectedZone.domainid != null) {
        params.id = this.selectedZone.id
        params.isrecursive = true
      } else {
        params.listall = true
      }
      params.showicon = true
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
        this.form.setFieldsValue({
          domainid: 0
        })
        this.handleDomainChange(this.domains[0])
      })
    },
    handleDomainChange (domain) {
      this.selectedDomain = domain
      if (!this.isObjectEmpty(domain)) {
        this.fetchNetworkOfferingData()
      }
    },
    fetchProjectData () {
      this.projects = []
      const params = {}
      params.listall = true
      params.showicon = true
      params.details = 'min'
      this.projectLoading = true
      api('listProjects', params).then(json => {
        const listProjects = json.listprojectsresponse.project
        this.projects = this.projects.concat(listProjects)
      }).finally(() => {
        this.projectLoading = false
        if (this.arrayHasItems(this.projects)) {
          this.form.setFieldsValue({
            projectid: 0
          })
          this.handleProjectChange(this.projects[0])
        }
      })
    },
    handleProjectChange (project) {
      this.selectedProject = project
    },
    handleSubmit (e) {
      if (this.actionLoading) return
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }
        if (
          (!this.isValidTextValueForKey(values, 'ip4gateway') && !this.isValidTextValueForKey(values, 'netmask') &&
            !this.isValidTextValueForKey(values, 'startipv4') && !this.isValidTextValueForKey(values, 'endipv4') &&
            !this.isValidTextValueForKey(values, 'ip6gateway') && !this.isValidTextValueForKey(values, 'ip6cidr') &&
            !this.isValidTextValueForKey(values, 'startipv6') && !this.isValidTextValueForKey(values, 'endipv6'))
        ) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.add.guest.network')
          })
          return
        }
        this.actionLoading = true
        var params = {
          zoneId: this.selectedZone.id,
          name: values.name,
          displayText: values.displaytext,
          networkOfferingId: this.selectedNetworkOffering.id
        }
        if (this.selectedNetworkOffering.guestiptype === 'Shared') {
          params.physicalnetworkid = this.formSelectedPhysicalNetwork.id
        }
        if (this.isValidTextValueForKey(values, 'vlanid')) {
          params.vlan = values.vlanid
        }
        if (this.isValidValueForKey(values, 'bypassvlanoverlapcheck')) {
          params.bypassvlanoverlapcheck = values.bypassvlanoverlapcheck
        }
        if (this.isValidValueForKey(values, 'isolatedpvlantype') && values.isolatedpvlantype !== 'none') {
          params.isolatedpvlantype = values.isolatedpvlantype
          if (this.isValidValueForKey(values, 'isolatedpvlan')) {
            params.isolatedpvlan = values.isolatedpvlan
          }
        }
        if (this.scopeType !== 'all') {
          params.domainid = this.selectedDomain.id
          params.acltype = this.scopeType
          if (this.scopeType === 'account') { // account-specific
            params.account = values.account
          } else if (this.scopeType === 'project') { // project-specific
            params.projectid = this.selectedProject.id
          } else { // domain-specific
            params.subdomainaccess = this.parseBooleanValueForKey(values, 'subdomainaccess')
          }
        } else { // zone-wide
          params.acltype = 'domain' // server-side will make it Root domain (i.e. domainid=1)
        }
        // IPv4 (begin)
        if (this.isValidTextValueForKey(values, 'ip4gateway')) {
          params.gateway = values.ip4gateway
        }
        if (this.isValidTextValueForKey(values, 'netmask')) {
          params.netmask = values.netmask
        }
        if (this.isValidTextValueForKey(values, 'startipv4')) {
          params.startip = values.startipv4
        }
        if (this.isValidTextValueForKey(values, 'endipv4')) {
          params.endip = values.endipv4
        }
        // IPv4 (end)

        // IPv6 (begin)
        if (this.isValidTextValueForKey(values, 'ip4gateway')) {
          params.ip6gateway = values.ip6gateway
        }
        if (this.isValidTextValueForKey(values, 'routerip')) {
          params.routerip = values.routerip
        }
        if (this.isValidTextValueForKey(values, 'ip6cidr')) {
          params.ip6cidr = values.ip6cidr
        }
        if (this.isValidTextValueForKey(values, 'startipv6')) {
          params.startipv6 = values.startipv6
        }
        if (this.isValidTextValueForKey(values, 'endipv6')) {
          params.endipv6 = values.endipv6
        }
        if (this.isValidTextValueForKey(values, 'routeripv6')) {
          params.routeripv6 = values.routeripv6
        }
        // IPv6 (end)

        if (this.isValidTextValueForKey(values, 'networkdomain')) {
          params.networkdomain = values.networkdomain
        }
        var hideipaddressusage = this.parseBooleanValueForKey(values, 'hideipaddressusage')
        if (hideipaddressusage) {
          params.hideipaddressusage = true
        }
        api('createNetwork', params).then(json => {
          this.$notification.success({
            message: this.$t('label.network'),
            description: this.$t('message.success.add.guest.network')
          })
          this.resetForm()
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      })
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    resetForm () {
      this.form.setFieldsValue({
      })
      this.tags = []
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
.form-layout {
  .ant-tag {
    margin-bottom: 10px;
  }

  /deep/.custom-time-select .ant-time-picker {
    width: 100%;
  }

  /deep/.ant-divider-horizontal {
    margin-top: 0;
  }
}

.form {
  margin: 10px 0;
}

.tagsTitle {
  font-weight: 500;
  margin-bottom: 12px;
}
</style>
