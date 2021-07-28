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
    <div class="form-layout">
      <div class="form">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical">
          <a-form-item :label="$t('label.name')" name="name" ref="name">
            <template #label>
              {{ $t('label.name') }}
              <a-tooltip :title="apiParams.name.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.name"
              :placeholder="$t('label.name')"
              autoFocus />
          </a-form-item>
          <a-form-item name="displaytext" ref="displaytext">
            <template #label>
              {{ $t('label.displaytext') }}
              <a-tooltip :title="apiParams.displaytext.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.displaytext"
              :placeholder="$t('label.display.text')"/>
          </a-form-item>
          <a-form-item v-if="isObjectEmpty(zone)" name="zoneid" ref="zoneid">
            <template #label>
              {{ $t('label.zoneid') }}
              <a-tooltip :title="apiParams.zoneid.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-select
              v-model:value="form.zoneid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              :placeholder="$t('label.zoneid')"
              @change="val => { handleZoneChange(zones[val]) }">
              <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="isObjectEmpty(zone)" name="physicalnetworkid" ref="physicalnetworkid">
            <template #label>
              {{ $t('label.physicalnetworkid') }}
              <a-tooltip :title="apiParams.physicalnetworkid.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-select
              v-model:value="form.physicalnetworkid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="formPhysicalNetworkLoading"
              :placeholder="$t('label.physicalnetworkid')"
              @change="val => { handlePhysicalNetworkChange(formPhysicalNetworks[val]) }">
              <a-select-option v-for="(opt, optIndex) in formPhysicalNetworks" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="vlanid" ref="vlanid">
            <template #label>
              {{ $t('label.vlan') }}
              <a-tooltip :title="apiParams.vlan.description" v-if="'vlan' in apiParams">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.vlanid"
              :placeholder="$t('label.vlanid')"/>
          </a-form-item>
          <a-form-item name="bypassvlanoverlapcheck" ref="bypassvlanoverlapcheck">
            <template #label>
              {{ $t('label.bypassvlanoverlapcheck') }}
              <a-tooltip :title="apiParams.bypassvlanoverlapcheck.description" v-if="'bypassvlanoverlapcheck' in apiParams">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-switch v-model:checked="form.bypassvlanoverlapcheck" />
          </a-form-item>
          <a-form-item
            v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan"
            name="isolatedpvlantype"
            ref="isolatedpvlantype">
            <template #label>
              {{ $t('label.isolatedpvlantype') }}
              <a-tooltip :title="apiParams.isolatedpvlantype.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
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
              {{ $t('label.isolatedpvlanid') }}
              <a-tooltip :title="apiParams.isolatedpvlan.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.isolatedpvlan"
              :placeholder="$t('label.isolatedpvlanid')"/>
          </a-form-item>
          <a-form-item :label="$t('label.scope')" name="scope" ref="scope">
            <a-radio-group
              v-model:value="form.scope"
              buttonStyle="solid"
              @change="selected => { handleScopeTypeChange(selected.target.value) }">
              <a-radio-button value="all">
                {{ $t('label.all') }}
              </a-radio-button>
              <a-radio-button value="domain" v-if="!parseBooleanValueForKey(selectedZone, 'securitygroupsenabled')">
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
          <a-form-item v-if="scopeType !== 'all'" name="domainid" ref="domainid">
            <template #label>
              {{ $t('label.domain') }}
              <a-tooltip :title="apiParams.domainid.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-select
              v-model:value="form.domainid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="domainLoading"
              :placeholder="$t('label.domainid')"
              @change="val => { handleDomainChange(domains[val]) }">
              <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex">
                {{ opt.path || opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="scopeType === 'domain'" name="subdomainaccess" ref="subdomainaccess">
            <template #label>
              {{ $t('label.subdomainaccess') }}
              <a-tooltip :title="apiParams.subdomainaccess.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-switch v-model:checked="form.subdomainaccess" />
          </a-form-item>
          <a-form-item v-if="scopeType === 'account'" name="account" ref="account">
            <template #label>
              {{ $t('label.account') }}
              <a-tooltip :title="apiParams.account.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.account"
              :placeholder="$t('label.account')"/>
          </a-form-item>
          <a-form-item v-if="scopeType === 'project'" name="projectid" ref="projectid">
            <template #label>
              {{ $t('label.projectid') }}
              <a-tooltip :title="apiParams.projectid.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-select
              v-model:value="form.projectid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="projectLoading"
              :placeholder="$t('label.projectid')"
              @change="val => { handleProjectChange(projects[val]) }">
              <a-select-option v-for="(opt, optIndex) in projects" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="networkofferingid" ref="networkofferingid">
            <template #label>
              {{ $t('label.networkofferingid') }}
              <a-tooltip :title="apiParams.networkofferingid.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-select
              v-model:value="form.networkofferingid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networkOfferingLoading"
              :placeholder="$t('label.networkofferingid')"
              @change="val => { handleNetworkOfferingChange(networkOfferings[val]) }">
              <a-select-option v-for="(opt, optIndex) in networkOfferings" :key="optIndex">
                {{ opt.displaytext || opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="ip4gateway" ref="ip4gateway">
            <template #label>
              {{ $t('label.ip4gateway') }}
              <a-tooltip :title="apiParams.gateway.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.ip4gateway"
              :placeholder="$t('label.ip4gateway')"/>
          </a-form-item>
          <a-form-item name="ip4netmask" ref="ip4netmask">
            <template #label>
              {{ $t('label.ip4netmask') }}
              <a-tooltip :title="apiParams.netmask.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.netmask"
              :placeholder="$t('label.netmask')"/>
          </a-form-item>
          <a-form-item name="startipv4" ref="startipv4">
            <template #label>
              {{ $t('label.startipv4') }}
              <a-tooltip :title="apiParams.startip.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.startipv4"
              :placeholder="$t('label.startipv4')"/>
          </a-form-item>
          <a-form-item name="endipv4" ref="endipv4">
            <template #label>
              {{ $t('label.endipv4') }}
              <a-tooltip :title="apiParams.endip.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.endipv4"
              :placeholder="$t('label.endipv4')"/>
          </a-form-item>
          <a-form-item name="ip6gateway" ref="ip6gateway">
            <template #label>
              {{ $t('label.ip6gateway') }}
              <a-tooltip :title="apiParams.ip6gateway.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.ip6gateway"
              :placeholder="$t('label.ip6gateway')"/>
          </a-form-item>
          <a-form-item name="ip6cidr" ref="ip6cidr">
            <template #label>
              {{ $t('label.ip6cidr') }}
              <a-tooltip :title="apiParams.ip6cidr.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.ip6cidr"
              :placeholder="$t('label.ip6cidr')"/>
          </a-form-item>
          <a-form-item name="startipv6" ref="startipv6">
            <template #label>
              {{ $t('label.startipv6') }}
              <a-tooltip :title="apiParams.startipv6.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.startipv6"
              :placeholder="$t('label.startipv6')"/>
          </a-form-item>
          <a-form-item name="endipv6" ref="endipv6">
            <template #label>
              {{ $t('label.endipv6') }}
              <a-tooltip :title="apiParams.endipv6.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.endipv6"
              :placeholder="$t('label.endipv6')"/>
          </a-form-item>
          <a-form-item name="networkdomain" ref="networkdomain">
            <template #label>
              {{ $t('label.networkdomain') }}
              <a-tooltip :title="apiParams.networkdomain.description">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="form.networkdomain"
              :placeholder="$t('label.networkdomain')"/>
          </a-form-item>
          <a-form-item name="hideipaddressusage" ref="hideipaddressusage">
            <template #label>
              {{ $t('label.hideipaddressusage') }}
              <a-tooltip :title="apiParams.hideipaddressusage.description" v-if="'hideipaddressusage' in apiParams">
                <info-circle-outlined style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
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
              htmlType="submit"
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

export default {
  name: 'CreateGuestNetworkForm',
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
      scopeType: 'all',
      domains: [],
      domainLoading: false,
      selectedDomain: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      selectedNetworkOffering: {},
      projects: [],
      projectLoading: false,
      selectedProject: {}
    }
  },
  watch: {
    resource (newItem, oldItem) {
      this.fetchData()
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
      this.form = reactive({})
      this.rules = reactive({})

      this.form.scope = 'all'
      this.form.isolatedpvlantype = 'none'

      this.rules.name = [{ required: true, message: this.$t('message.error.name') }]
      this.rules.displaytext = [{ required: true, message: this.$t('message.error.display.text') }]
      this.rules.zoneid = [{ type: 'number', required: true, message: this.$t('message.error.select') }]
      this.rules.vlanid = [{ required: true, message: this.$t('message.please.enter.value') }]
      this.rules.networkofferingid = [{ type: 'number', required: true, message: this.$t('message.error.select') }]
      if (this.scopeType !== 'all') {
        this.rules.domainid = [{ required: true, message: this.$t('message.error.select') }]
      } else if (this.scopeType === 'project') {
        this.rules.projectid = [{ required: true, message: this.$t('message.error.select') }]
      }
    },
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
          this.form.zoneid = 0
          this.handleZoneChange(this.zones[0])
        }
      } else {
        const params = {}
        if (this.resource.zoneid && this.$route.name === 'deployVirtualMachine') {
          params.id = this.resource.zoneid
        }
        params.listAll = true
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
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.arrayHasItems(this.networkOfferings)) {
          this.form.networkofferingid = 0
          this.handleNetworkOfferingChange(this.networkOfferings[0])
        } else {
          this.form.networkofferingid = null
        }
      })
    },
    handleNetworkOfferingChange (networkOffering) {
      this.selectedNetworkOffering = networkOffering
    },
    fetchDomainData () {
      const params = {}
      if (!this.isObjectEmpty(this.selectedZone) && this.selectedZone.domainid != null) {
        params.id = this.selectedZone.id
        params.isrecursive = true
      } else {
        params.listall = true
      }
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
        this.form.domainid = 0
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
      params.details = 'min'
      this.projectLoading = true
      api('listProjects', params).then(json => {
        const listProjects = json.listprojectsresponse.project
        this.projects = this.projects.concat(listProjects)
      }).finally(() => {
        this.projectLoading = false
        if (this.arrayHasItems(this.projects)) {
          this.form.projectid = 0
          this.handleProjectChange(this.projects[0])
        }
      })
    },
    handleProjectChange (project) {
      this.selectedProject = project
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
        if (this.isValidTextValueForKey(values, 'ip6cidr')) {
          params.ip6cidr = values.ip6cidr
        }
        if (this.isValidTextValueForKey(values, 'startipv6')) {
          params.startipv6 = values.startipv6
        }
        if (this.isValidTextValueForKey(values, 'endipv6')) {
          params.endipv6 = values.endipv6
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
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
