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
          :form="form"
          layout="vertical"
          @submit="handleSubmit">
          <a-form-item :label="$t('name')">
            <a-input
              v-decorator="['name', {
                rules: [{ required: true, message: 'Please enter name' }]
              }]"
              :placeholder="this.$t('Name')"/>
          </a-form-item>
          <a-form-item :label="$t('displaytext')">
            <a-input
              v-decorator="['displaytext', {
                rules: [{ required: true, message: 'Please enter display text' }]
              }]"
              :placeholder="this.$t('Display text')"/>
          </a-form-item>
          <a-form-item :label="$t('zoneid')" v-if="this.isObjectEmpty(this.zone)">
            <a-select
              v-decorator="['zoneid', {
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
              :loading="zoneLoading"
              :placeholder="this.$t('zoneid')"
              @change="val => { this.handleZoneChanged(this.zones[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('physicalnetworkid')" v-if="this.isObjectEmpty(this.zone)">
            <a-select
              v-decorator="['physicalnetworkid', {}]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              :placeholder="this.$t('physicalnetworkid')"
              @change="val => { this.handleZoneChanged(this.formPhysicalNetworks[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.formPhysicalNetworks" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('vlan')">
            <a-input
              v-decorator="['vlanid', {
                rules: [{ required: true, message: 'Please enter value' }]
              }]"
              :placeholder="this.$t('vlanid')"/>
          </a-form-item>
          <a-form-item :label="$t('bypassvlanoverlapcheck')">
            <a-switch v-decorator="['bypassvlanoverlapcheck']" />
          </a-form-item>
          <a-form-item :label="$t('isolatedpvlantype')">
            <a-radio-group
              v-decorator="['isolatedpvlantype', {
                initialValue: this.isolatePvlanType
              }]"
              buttonStyle="solid"
              @change="selected => { this.handleIsolatedPvlanTypeChange(selected.target.value) }">
              <a-radio-button value="none">
                {{ $t('None') }}
              </a-radio-button>
              <a-radio-button value="community">
                {{ $t('Community') }}
              </a-radio-button>
              <a-radio-button value="isolated">
                {{ $t('Isolated') }}
              </a-radio-button>
              <a-radio-button value="promiscuous">
                {{ $t('Promiscuous') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$t('isolatedpvlan')" v-if="this.isolatePvlanType=='community' || this.isolatePvlanType=='isolated'">
            <a-input
              v-decorator="['isolatedpvlan', {}]"
              :placeholder="this.$t('isolatedpvlan')"/>
          </a-form-item>
          <a-form-item :label="$t('scope')">
            <a-radio-group
              v-decorator="['scope', {
                initialValue: this.scopeType
              }]"
              buttonStyle="solid"
              @change="selected => { this.handleScopeTypeChange(selected.target.value) }">
              <a-radio-button value="all">
                {{ $t('All') }}
              </a-radio-button>
              <a-radio-button value="domain" v-if="!this.parseBooleanValueForKey(this.selectedZone, 'securitygroupsenabled')">
                {{ $t('Domain') }}
              </a-radio-button>
              <a-radio-button value="account" v-if="!this.parseBooleanValueForKey(this.selectedZone, 'securitygroupsenabled')">
                {{ $t('Account') }}
              </a-radio-button>
              <a-radio-button value="project" v-if="!this.parseBooleanValueForKey(this.selectedZone, 'securitygroupsenabled')">
                {{ $t('Project') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$t('domain')" v-if="this.scopeType !== 'all'">
            <a-select
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
              :placeholder="this.$t('domainid')"
              @change="val => { this.handleDomainChange(this.domains[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('subdomainaccess')" v-if="this.scopeType === 'domain'">
            <a-switch v-decorator="['subdomainaccess']" />
          </a-form-item>
          <a-form-item :label="$t('account')" v-if="this.scopeType === 'account'">
            <a-input
              v-decorator="['account', {}]"
              :placeholder="this.$t('account')"/>
          </a-form-item>
          <a-form-item :label="$t('projectid')" v-if="this.scopeType === 'project'">
            <a-select
              v-decorator="['projectid', {
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
              :loading="projectLoading"
              :placeholder="this.$t('projectid')"
              @change="val => { this.handleProjectChange(this.projects[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.projects" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('networkofferingid')">
            <a-select
              v-decorator="['networkofferingid', {
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
              :loading="networkOfferingLoading"
              :placeholder="this.$t('networkofferingid')"
              @change="val => { this.handleNetworkOfferingChange(this.networkOfferings[val]) }">
              <a-select-option v-for="(opt, optIndex) in this.networkOfferings" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('ip4gateway')">
            <a-input
              v-decorator="['ip4gateway', {}]"
              :placeholder="this.$t('ip4gateway')"/>
          </a-form-item>
          <a-form-item :label="$t('ip4Netmask')">
            <a-input
              v-decorator="['netmask', {}]"
              :placeholder="this.$t('netmask')"/>
          </a-form-item>
          <a-form-item :label="$t('startipv4')">
            <a-input
              v-decorator="['startipv4', {}]"
              :placeholder="this.$t('startipv4')"/>
          </a-form-item>
          <a-form-item :label="$t('endipv4')">
            <a-input
              v-decorator="['endipv4', {}]"
              :placeholder="this.$t('endipv4')"/>
          </a-form-item>
          <a-form-item :label="$t('ip6gateway')">
            <a-input
              v-decorator="['ip6gateway', {}]"
              :placeholder="this.$t('ip6gateway')"/>
          </a-form-item>
          <a-form-item :label="$t('ip6cidr')">
            <a-input
              v-decorator="['ip6cidr', {}]"
              :placeholder="this.$t('ip6cidr')"/>
          </a-form-item>
          <a-form-item :label="$t('startipv6')">
            <a-input
              v-decorator="['startipv6', {}]"
              :placeholder="this.$t('startipv6')"/>
          </a-form-item>
          <a-form-item :label="$t('endipv6')">
            <a-input
              v-decorator="['endipv6', {}]"
              :placeholder="this.$t('endipv6')"/>
          </a-form-item>
          <a-form-item :label="$t('networkdomain')">
            <a-input
              v-decorator="['networkdomain', {}]"
              :placeholder="this.$t('networkdomain')"/>
          </a-form-item>
          <a-form-item :label="$t('hideipaddressusage')">
            <a-switch v-decorator="['hideipaddressusage']" />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button
              :loading="actionLoading"
              @click="closeAction">
              {{ this.$t('Cancel') }}
            </a-button>
            <a-button
              :loading="actionLoading"
              type="primary"
              @click="handleSubmit">
              {{ this.$t('OK') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
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
      selectedProject: {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
  },
  mounted () {
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
      if (this.physicalNetworks != null) {
        this.formPhysicalNetworks = this.physicalNetworks
        if (this.arrayHasItems(this.formPhysicalNetworks)) {
          this.form.setFieldsValue({
            physicalnetworkid: 0
          })
          this.handlePhysicalNetworkChange(this.formPhysicalNetworks[0])
        }
      } else {
        if (this.selectedZone === null || this.selectedZone === undefined) {
          return
        }
        const params = {}
        params.zoneid = this.selectedZone.id
        this.formPhysicalNetworksLoading = true
        api('listPhysicalNetworks', params).then(json => {
          this.formPhysicalNetworks = []
          var networks = json.listphysicalnetworksresponse.physicalnetwork
          if (this.arrayHasItems(networks)) {
            for (const i in networks) {
              this.addPhysicalNetworkForGuestTrafficType(networks[i], i * 1 === networks.length - 1)
            }
          } else {
            this.formPhysicalNetworkLoading = false
          }
        }).finally(() => {
        })
      }
    },
    addPhysicalNetworkForGuestTrafficType (physicalNetwork, isLastNetwork) {
      const params = {}
      params.physicalnetworkid = physicalNetwork.id
      api('listTrafficTypes', params).then(json => {
        var trafficTypes = json.listtraffictypesresponse.traffictype
        if (this.arrayHasItems(trafficTypes)) {
          for (const i in trafficTypes) {
            if (trafficTypes[i].traffictype === 'Guest') {
              this.formPhysicalNetworks.push(physicalNetwork)
              break
            }
          }
        } else {
          this.formPhysicalNetworkLoading = false
        }
      }).finally(() => {
        if (isLastNetwork) {
          this.form.setFieldsValue({
            physicalnetworkid: 0
          })
          this.handlePhysicalNetworkChange(this.formPhysicalNetworks[0])
        }
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
        !this.isObjectEmpty(this.formSelectedPhysicalNetwork.tags) &&
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
      api('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.arrayHasItems(this.networkOfferings)) {
          this.form.setFieldsValue({
            networkofferingid: 0
          })
          this.handleNetworkOfferingChange(this.networkOfferings[0])
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
            message: 'Request Failed',
            description: 'Either IPv4 fields or IPv6 fields need to be filled when adding a guest network'
          })
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
        if (this.isValidValueForKey(values, 'isolatedpvlantype')) {
          params.isolatedpvlantype = values.isolatedpvlantype
        }
        if (this.isValidValueForKey(values, 'isolatedpvlan')) {
          params.isolatedpvlan = values.isolatedpvlan
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
            message: 'Network',
            description: 'Successfully created guest network'
          })
          this.resetForm()
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.$emit('refresh-data')
          this.actionLoading = false
          this.closeAction()
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
