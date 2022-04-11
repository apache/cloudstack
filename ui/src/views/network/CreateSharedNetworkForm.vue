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
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
         >
          <a-form-item :label="$t('label.name')" name="name" ref="name">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
            </template>
            <a-input
              v-model:value="form.name"
              :placeholder="apiParams.name.description"
              v-focus="true" />
          </a-form-item>
          <a-form-item name="displaytext" ref="displaytext">
            <template #label>
              <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
            </template>
            <a-input
              v-model:value="form.displaytext"
              :placeholder="apiParams.displaytext.description"/>
          </a-form-item>
          <a-form-item v-if="isObjectEmpty(zone)" name="zoneid" ref="zoneid">
            <template #label>
              <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
            </template>
            <a-select
              v-model:value="form.zoneid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              :placeholder="apiParams.zoneid.description"
              @change="val => { handleZoneChange(zones[val]) }">
              <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="isObjectEmpty(zone) && isAdmin()" name="physicalnetworkid" ref="physicalnetworkid">
            <template #label>
              <tooltip-label :title="$t('label.physicalnetworkid')" :tooltip="apiParams.physicalnetworkid.description"/>
            </template>
            <a-select
              v-model:value="form.physicalnetworkid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="formPhysicalNetworkLoading"
              :placeholder="apiParams.physicalnetworkid.description"
              @change="val => { handlePhysicalNetworkChange(formPhysicalNetworks[val]) }">
              <a-select-option v-for="(opt, optIndex) in formPhysicalNetworks" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="!this.isObjectEmpty(this.selectedNetworkOffering) && this.selectedNetworkOffering.specifyvlan && isAdmin()" name="vlanid" ref="vlanid">
            <template #label>
              <tooltip-label :title="$t('label.vlan')" :tooltip="apiParams.vlan.description"/>
            </template>
            <a-input
              v-model:value="form.vlanid"
              :placeholder="apiParams.vlan.description"/>
          </a-form-item>
          <a-form-item name="bypassvlanoverlapcheck" ref="bypassvlanoverlapcheck" v-if="isAdmin()">
            <template #label>
              <tooltip-label :title="$t('label.bypassvlanoverlapcheck')" :tooltip="apiParams.bypassvlanoverlapcheck.description"/>
            </template>
            <a-switch v-model:checked="form.bypassvlanoverlapcheck" />
          </a-form-item>
          <a-form-item
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan && isAdmin()"
            name="isolatedpvlantype"
            ref="isolatedpvlantype">
            <template #label>
              <tooltip-label :title="$t('label.isolatedpvlantype')" :tooltip="apiParams.isolatedpvlantype.description"/>
            </template>
            <a-radio-group
              v-model:value="form.isolatedpvlantype"
              buttonStyle="solid">
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
          <a-form-item v-if="form.isolatedpvlantype=='community' || form.isolatedpvlantype=='isolated'" name="isolatedpvlan" ref="isolatedpvlan">
            <template #label>
              <tooltip-label :title="$t('label.isolatedpvlanid')" :tooltip="apiParams.isolatedpvlan.description"/>
            </template>
            <a-input
              v-model:value="form.isolatedpvlan"
              :placeholder="apiParams.isolatedpvlan.description"/>
          </a-form-item>
          <a-form-item :label="$t('label.scope')" name="scope" ref="scope">
            <a-radio-group
              v-model:value="form.scope"
              buttonStyle="solid"
              @change="selected => { handleScopeTypeChange(selected.target.value) }">
              <a-radio-button value="all" v-if="isAdmin()">
                {{ $t('label.all') }}
              </a-radio-button>
              <a-radio-button value="domain" v-if="!parseBooleanValueForKey(selectedZone, 'securitygroupsenabled') && isAdminOrDomainAdmin()">
                {{ $t('label.domain') }}
              </a-radio-button>
              <a-radio-button value="account" v-if="!parseBooleanValueForKey(selectedZone, 'securitygroupsenabled')">
                {{ $t('label.account') }}
              </a-radio-button>
              <a-radio-button value="project" v-if="!parseBooleanValueForKey(selectedZone, 'securitygroupsenabled')">
                {{ $t('label.project') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-if="scopeType !== 'all' && isAdminOrDomainAdmin()" name="domainid" ref="domainid">
            <template #label>
              <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
            </template>
            <a-select
              v-model:value="form.domainid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="domainLoading"
              :placeholder="apiParams.domainid.description"
              @change="val => { handleDomainChange(domains[val]) }">
              <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex">
                <span>
                  <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <block-outlined v-else style="margin-right: 5px" />
                  {{ opt.path || opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="scopeType === 'domain' && isAdminOrDomainAdmin()" name="subdomainaccess" ref="subdomainaccess">
            <template #label>
              <tooltip-label :title="$t('label.subdomainaccess')" :tooltip="apiParams.subdomainaccess.description"/>
            </template>
            <a-switch v-model:checked="form.subdomainaccess" />
          </a-form-item>
          <a-form-item v-if="scopeType === 'account' && isAdminOrDomainAdmin()" name="account" ref="account">
            <template #label>
              <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
            </template>
            <a-select
              v-model:value="form.account"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="accountLoading"
              :placeholder="apiParams.account.description"
              @change="val => { handleAccountChange(accounts[val]) }">
              <a-select-option v-for="(opt, optIndex) in accounts" :key="optIndex">
                <span>
                  <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <user-outlined v-else style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="scopeType === 'project'" name="projectid" ref="projectid">
            <template #label>
              <tooltip-label :title="$t('label.project')" :tooltip="apiParams.projectid.description"/>
            </template>
            <a-select
              v-model:value="form.projectid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="projectLoading"
              :placeholder="apiParams.projectid.description"
              @change="val => { handleProjectChange(projects[val]) }">
              <a-select-option v-for="(opt, optIndex) in projects" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="networkofferingid" ref="networkofferingid">
            <template #label>
              <tooltip-label :title="$t('label.networkofferingid')" :tooltip="apiParams.networkofferingid.description"/>
            </template>
            <a-select
              v-model:value="form.networkofferingid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networkOfferingLoading"
              :placeholder="apiParams.networkofferingid.description"
              @change="val => { handleNetworkOfferingChange(networkOfferings[val]) }">
              <a-select-option v-for="(opt, optIndex) in networkOfferings" :key="optIndex">
                {{ opt.displaytext || opt.name || opt.description }}
              </a-select-option>
            </a-select>
            <a-alert type="warning" :loading="networkOfferingLoading" v-if="networkOfferingWarning">
              <template #message>{{ $t('message.shared.network.offering.warning') }}</template>
            </a-alert>
          </a-form-item>
          <a-form-item v-if="!isObjectEmpty(selectedNetworkOffering) && !selectedNetworkOffering.specifyvlan" name="associatednetworkid" ref="associatednetworkid">
            <template #label>
              <tooltip-label :title="$t('label.associatednetwork')" :tooltip="apiParams.associatednetworkid.description"/>
            </template>
            <a-select
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networkLoading"
              :placeholder="this.$t('label.associatednetwork')"
              @change="val => { this.handleNetworkChange(this.networks[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.networks" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <apartment-outlined v-else style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="ip4gateway" ref="ip4gateway">
            <template #label>
              <tooltip-label :title="$t('label.ip4gateway')" :tooltip="apiParams.netmask.description"/>
            </template>
            <a-input
              v-model:value="form.ip4gateway"
              :placeholder="apiParams.netmask.description"/>
          </a-form-item>
          <a-form-item name="ip4netmask" ref="ip4netmask">
            <template #label>
              <tooltip-label :title="$t('label.netmask')" :tooltip="apiParams.netmask.description"/>
            </template>
            <a-input
              v-model:value="form.netmask"
              :placeholder="apiParams.netmask.description"/>
          </a-form-item>
          <a-form-item name="startipv4" ref="startipv4">
            <template #label>
              <tooltip-label :title="$t('label.startipv4')" :tooltip="apiParams.startip.description"/>
            </template>
            <a-input
              v-model:value="form.startipv4"
              :placeholder="apiParams.startip.description"/>
          </a-form-item>
          <a-form-item name="endipv4" ref="endipv4">
            <template #label>
              <tooltip-label :title="$t('label.endipv4')" :tooltip="apiParams.endip.description"/>
            </template>
            <a-input
              v-model:value="form.endipv4"
              :placeholder="apiParams.endip.description"/>
          </a-form-item>
          <a-form-item name="ip6gateway" ref="ip6gateway">
            <template #label>
              <tooltip-label :title="$t('label.ip6gateway')" :tooltip="apiParams.ip6gateway.description"/>
            </template>
            <a-input
              v-model:value="form.ip6gateway"
              :placeholder="apiParams.ip6gateway.description"/>
          </a-form-item>
          <a-form-item name="ip6cidr" ref="ip6cidr">
            <template #label>
              <tooltip-label :title="$t('label.ip6cidr')" :tooltip="apiParams.ip6cidr.description"/>
            </template>
            <a-input
              v-model:value="form.ip6cidr"
              :placeholder="apiParams.ip6cidr.description"/>
          </a-form-item>
          <a-form-item name="startipv6" ref="startipv6">
            <template #label>
              <tooltip-label :title="$t('label.startipv6')" :tooltip="apiParams.startipv6.description"/>
            </template>
            <a-input
              v-model:value="form.startipv6"
              :placeholder="apiParams.startipv6.description"/>
          </a-form-item>
          <a-form-item name="endipv6" ref="endipv6">
            <template #label>
              <tooltip-label :title="$t('label.endipv6')" :tooltip="apiParams.endipv6.description"/>
            </template>
            <a-input
              v-model:value="form.endipv6"
              :placeholder="apiParams.endipv6.description"/>
          </a-form-item>
          <a-form-item v-if="isVirtualRouterForAtLeastOneService" name="routeripv6" ref="routeripv6">
            <template #label>
              <tooltip-label :title="$t('label.routeripv6')" :tooltip="apiParams.routeripv6.description"/>
            </template>
            <a-input
              v-model:value="form.routeripv6"
              :placeholder="apiParams.routeripv6.description"/>
          </a-form-item>
          <a-form-item name="networkdomain" ref="networkdomain">
            <template #label>
              <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
            </template>
            <a-input
              v-model:value="form.networkdomain"
              :placeholder="apiParams.networkdomain.description"/>
          </a-form-item>
          <a-form-item name="hideipaddressusage" ref="hideipaddressusage">
            <template #label>
              <tooltip-label :title="$t('label.hideipaddressusage')" :tooltip="apiParams.hideipaddressusage.description"/>
            </template>
            <a-switch v-model:checked="form.hideipaddressusage" />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ $t('label.cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              type="primary"
              ref="submit"
              @click="handleSubmit">
              {{ $t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { isAdmin, isAdminOrDomainAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateGuestNetworkForm',
  mixins: [mixinForm],
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
      scopeType: 'all',
      domains: [],
      domainLoading: false,
      selectedDomain: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      networkOfferingWarning: false,
      selectedNetworkOffering: {},
      networks: [],
      networkLoading: false,
      selectedNetwork: {},
      accounts: [],
      accountLoading: false,
      selectedAccount: {},
      projects: [],
      projectLoading: false,
      selectedProject: {},
      isVirtualRouterForAtLeastOneService: false,
      selectedServiceProviderMap: {}
    }
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.fetchData()
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createNetwork')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      if (isAdmin()) {
        this.scopeType = 'all'
      } else if (isAdminOrDomainAdmin()) {
        this.scopeType = 'domain'
      } else {
        this.scopeType = 'account'
      }
      this.form = reactive({
        scope: this.scopeType,
        isolatedpvlantype: 'none'
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        displaytext: [{ required: true, message: this.$t('message.error.display.text') }],
        zoneid: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        vlanid: [{ required: true, message: this.$t('message.please.enter.value') }],
        networkofferingid: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        domainid: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        account: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        projectid: [{ type: 'number', required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      if (this.isObjectEmpty(this.zone)) {
        this.fetchZoneData()
      } else {
        this.fetchNetworkOfferingData()
      }
      if (this.scopeType !== 'all') {
        this.handleScopeTypeChange(this.scopeType)
      }
    },
    isAdmin () {
      return isAdmin()
    },
    isAdminOrDomainAdmin () {
      return isAdminOrDomainAdmin()
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
      return this.isValidValueForKey(obj, key) && String(obj[key]).length > 0
    },
    fetchZoneData () {
      this.zones = []
      if (this.zone !== null) {
        this.zones.push(this.zone)
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = 0
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
            this.form.zoneid = 0
            this.handleZoneChange(this.zones[0])
          }
        })
      }
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      if (isAdmin()) {
        this.fetchPhysicalNetworkData()
      } else {
        this.fetchNetworkOfferingData()
      }
    },
    fetchPhysicalNetworkData () {
      this.formSelectedPhysicalNetwork = {}
      this.formPhysicalNetworks = []
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
    },
    selectFirstPhysicalNetwork () {
      if (this.arrayHasItems(this.formPhysicalNetworks)) {
        this.form.physicalnetworkid = 0
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
          if (isAdminOrDomainAdmin()) {
            this.fetchDomainData()
          } else {
            this.fetchProjectData()
          }
          break
        }
        case 'account':
        {
          if (isAdminOrDomainAdmin()) {
            this.fetchDomainData()
          } else {
            this.fetchNetworkOfferingData()
          }
          break
        }
        default:
        {
          if (isAdminOrDomainAdmin()) {
            this.fetchDomainData()
          } else {
            this.fetchNetworkOfferingData()
          }
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
      if (!isAdmin()) {
        params.specifyvlan = false
      }
      if (!this.isObjectEmpty(this.formSelectedPhysicalNetwork) &&
        this.formSelectedPhysicalNetwork.tags &&
        this.formSelectedPhysicalNetwork.tags.length > 0) {
        params.tags = this.formSelectedPhysicalNetwork.tags
      }
      // Network tab in Guest Traffic Type in Infrastructure menu is only available when it's under Advanced zone.
      // zone dropdown in add guest network dialog includes only Advanced zones.
      params.guestiptype = 'Shared'
      if (this.scopeType === 'domain') {
        params.domainid = this.selectedDomain.id
      }
      this.handleNetworkOfferingChange(null)
      this.networkOfferings = []
      api('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.arrayHasItems(this.networkOfferings)) {
          this.form.networkofferingid = 0
          this.handleNetworkOfferingChange(this.networkOfferings[0])
          this.networkOfferingWarning = false
        } else {
          this.form.networkofferingid = null
          this.networkOfferingWarning = true
        }
      })
    },
    handleNetworkOfferingChange (networkOffering) {
      this.selectedNetworkOffering = networkOffering
      if (networkOffering) {
        this.networkServiceProviderMap(this.selectedNetworkOffering.id)
        if (!networkOffering.specifyvlan) {
          this.fetchNetworkData()
        }
      }
    },
    fetchNetworkData () {
      if (this.isObjectEmpty(this.selectedZone)) {
        return
      }
      if (this.isObjectEmpty(this.selectedNetworkOffering) || this.selectedNetworkOffering.specifyvlan) {
        return
      }
      if (this.isObjectEmpty(this.selectedProject) && this.scopeType === 'project') {
        return
      }
      this.networkLoading = true
      var params = {
        zoneid: this.selectedZone.id,
        networkfilter: 'Account'
      }
      if (this.formSelectedPhysicalNetwork) {
        params.physicalnetworkid = this.formSelectedPhysicalNetwork.id
      }
      switch (this.scopeType) {
        case 'domain':
        {
          params.domainid = this.selectedDomain.id
          params.ignoreproject = true
          break
        }
        case 'project':
        {
          params.domainid = this.selectedProject.domainid
          params.projectid = this.selectedProject.id
          break
        }
        case 'account':
        {
          params.domainid = this.selectedAccount.domainid
          params.account = this.selectedAccount.name
          params.ignoreproject = true
          break
        }
        default:
        {
        }
      }
      this.handleNetworkChange(null)
      this.networks = []
      api('listNetworks', params).then(json => {
        var networks = json.listnetworksresponse.network || []
        for (const network of networks) {
          if (network.type === 'Isolated' || network.type === 'L2') {
            this.networks.push(network)
          }
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.networkLoading = false
      })
    },
    handleNetworkChange (selectedNetwork) {
      this.selectedNetwork = selectedNetwork
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
      this.networkOfferingWarning = false
      const params = {}
      if (!this.isObjectEmpty(this.selectedZone) && this.selectedZone.domainid != null) {
        params.id = this.selectedZone.domainid
        params.isrecursive = true
      } else {
        params.listall = true
      }
      params.showicon = true
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain || []
        this.domains = listDomains
      }).finally(() => {
        this.domainLoading = false
        if (this.arrayHasItems(this.domains) && this.scopeType !== 'all') {
          this.form.domainid = 0
          this.handleDomainChange(this.domains[0])
        }
      })
    },
    handleDomainChange (domain) {
      this.selectedDomain = domain
      if (!this.isObjectEmpty(domain)) {
        if (this.scopeType === 'domain') {
          this.fetchNetworkOfferingData()
        } else if (this.scopeType === 'account') {
          this.fetchAccountData()
        } else if (this.scopeType === 'project') {
          this.fetchProjectData()
        }
      }
    },
    fetchAccountData () {
      this.networkOfferingWarning = false
      this.accounts = []
      const params = {}
      params.showicon = true
      params.details = 'min'
      if (this.selectedDomain) {
        params.domainid = this.selectedDomain.id
      }
      this.accountLoading = true
      this.handleAccountChange(null)
      api('listAccounts', params).then(json => {
        this.accounts = json.listaccountsresponse.account || []
      }).finally(() => {
        this.accountLoading = false
        if (this.arrayHasItems(this.accounts) && this.scopeType === 'account') {
          this.form.account = 0
          this.handleAccountChange(this.accounts[0])
        } else {
          this.form.account = null
        }
      })
    },
    handleAccountChange (account) {
      this.selectedAccount = account
      if (!this.isObjectEmpty(account)) {
        this.fetchNetworkOfferingData()
      }
    },
    fetchProjectData () {
      this.networkOfferingWarning = false
      this.projects = []
      const params = {}
      params.listall = true
      params.showicon = true
      params.details = 'min'
      if (this.selectedDomain) {
        params.domainid = this.selectedDomain.id
      }
      this.projectLoading = true
      this.handleProjectChange(null)
      api('listProjects', params).then(json => {
        this.projects = json.listprojectsresponse.project || []
      }).finally(() => {
        this.projectLoading = false
        if (this.arrayHasItems(this.projects) && this.scopeType === 'project') {
          this.form.projectid = 0
          this.handleProjectChange(this.projects[0])
        } else {
          this.form.projectid = null
        }
      })
    },
    handleProjectChange (project) {
      this.selectedProject = project
      if (!this.isObjectEmpty(project)) {
        this.fetchNetworkOfferingData()
      }
    },
    handleSubmit (e) {
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
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
        if (this.selectedNetwork) {
          params.associatednetworkid = this.selectedNetwork.id
        }
        if (this.scopeType !== 'all') {
          params.domainid = this.selectedDomain.id
          params.acltype = this.scopeType
          if (this.scopeType === 'account') { // account-specific
            params.domainid = this.selectedAccount.domainid
            params.account = this.selectedAccount.name
          } else if (this.scopeType === 'project') { // project-specific
            params.acltype = 'account'
            params.domainid = this.selectedProject.domainid
            params.projectid = this.selectedProject.id
          } else { // domain-specific
            params.subdomainaccess = this.parseBooleanValueForKey(values, 'subdomainaccess')
          }
        } else if (isAdminOrDomainAdmin()) { // zone-wide
          params.acltype = 'domain' // server-side will make it Root domain (i.e. domainid=1)
        } else {
          params.acltype = 'account' // acl type is "account" for regular users
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    resetForm () {
      this.formRef.value.resetFields()
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
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }

  .ant-tag {
    margin-bottom: 10px;
  }

  :deep(.custom-time-select) .ant-time-picker {
    width: 100%;
  }

  :deep(.ant-divider-horizontal) {
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
