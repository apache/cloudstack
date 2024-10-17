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
  <span :style="styleSearch">
    <span v-if="!searchFilters || searchFilters.length === 0" style="display: flex;">
      <a-input-search
        v-model:value="searchQuery"
        :placeholder="$t('label.search')"
        allowClear
        @search="onSearch"
      />
    </span>

    <span
      v-else
      class="filter-group">
      <a-input-search
        allowClear
        class="input-search"
        :placeholder="$t('label.search')"
        v-model:value="searchQuery"
        @search="onSearch">
        <template #addonBefore>
          <a-popover
            placement="bottomRight"
            trigger="click"
            v-model:visible="visibleFilter">
            <template #content v-if="visibleFilter">
              <a-form
                style="min-width: 170px"
                :ref="formRef"
                :model="form"
                :rules="rules"
                layout="vertical"
                @finish="handleSubmit"
                v-ctrl-enter="handleSubmit">
                <a-form-item
                  v-for="(field, index) in fields"
                  :ref="field.name"
                  :name="field.name"
                  :key="index"
                  :label="retrieveFieldLabel(field.name)">
                  <a-select
                    allowClear
                    v-if="field.type==='list'"
                    v-model:value="form[field.name]"
                    showSearch
                    :dropdownMatchSelectWidth="false"
                    optionFilterProp="label"
                    :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }"
                    :loading="field.loading"
                    @input="onchange($event, field.name)"
                    @change="onSelectFieldChange(field.name)">
                    <a-select-option
                      v-for="(opt, idx) in field.opts"
                      :key="idx"
                      :value="['account'].includes(field.name) ? opt.name : opt.id"
                      :label="$t((['storageid'].includes(field.name) || !opt.path) ? opt.name : opt.path)">
                      <div>
                        <span v-if="(field.name.startsWith('zone'))">
                          <span v-if="opt.icon">
                            <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                          </span>
                          <global-outlined v-else style="margin-right: 5px" />
                        </span>
                        <span v-if="(field.name.startsWith('domain') || field.name === 'account')">
                          <span v-if="opt.icon">
                            <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                          </span>
                          <block-outlined v-else style="margin-right: 5px" />
                        </span>
                        <span v-if="(field.name.startsWith('managementserver'))">
                          <status :text="opt.state" />
                        </span>
                        {{ $t((['storageid'].includes(field.name) || !opt.path) ? opt.name : opt.path) }}
                        <span v-if="(field.name.startsWith('associatednetwork'))">
                          <span v-if="opt.icon">
                            <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                          </span>
                          <block-outlined v-else style="margin-right: 5px" />
                        </span>
                        {{ $t(opt.path || opt.name) }}
                      </div>
                    </a-select-option>
                  </a-select>
                  <a-input
                    v-else-if="field.type==='input'"
                    v-model:value="form[field.name]" />
                  <div v-else-if="field.type==='tag'">
                    <a-input-group
                      type="text"
                      size="small"
                      compact>
                      <a-input ref="input" v-model:value="inputKey" style="width: 50px; text-align: center" :placeholder="$t('label.key')" />
                      <a-input
                        class="tag-disabled-input"
                        style=" width: 20px; border-left: 0; pointer-events: none; text-align: center"
                        placeholder="="
                        disabled />
                      <a-input v-model:value="inputValue" style="width: 50px; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
                      <tooltip-button :tooltip="$t('label.clear')" icon="close-outlined" size="small" @onClick="inputKey = inputValue = ''" />
                    </a-input-group>
                  </div>
                  <a-switch
                    v-else-if="field.type==='boolean'"
                    v-model:checked="form[field.name]"
                  />
                  <a-auto-complete
                    v-else-if="field.type==='autocomplete'"
                    v-model:value="form[field.name]"
                    :placeholder="$t('message.error.input.value')"
                    :options="field.opts"
                    :filterOption="(inputValue, option) => {
                      return option.value.toLowerCase().indexOf(inputValue.toLowerCase()) !== -1
                    }" />
                </a-form-item>
                <div class="filter-group-button">
                  <a-button
                    class="filter-group-button-clear"
                    type="default"
                    size="small"
                    @click="onClear">
                    <template #icon><stop-outlined /></template>
                    {{ $t('label.reset') }}
                  </a-button>
                  <a-button
                    class="filter-group-button-search"
                    type="primary"
                    size="small"
                    ref="submit"
                    html-type="submit">
                    <template #icon><search-outlined /></template>
                    {{ $t('label.search') }}
                  </a-button>
                </div>
              </a-form>
            </template>
            <a-button
              class="filter-button"
              size="small"
              @click="() => { searchQuery = null }">
              <filter-two-tone v-if="isFiltered" />
              <filter-outlined v-else />
            </a-button>
          </a-popover>
        </template>
      </a-input-search>
    </span>
  </span>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { isAdmin } from '@/role'
import TooltipButton from '@/components/widgets/TooltipButton'
import ResourceIcon from '@/components/view/ResourceIcon'
import Status from '@/components/widgets/Status'
import { i18n } from '@/locales'

export default {
  name: 'SearchView',
  components: {
    TooltipButton,
    ResourceIcon,
    Status
  },
  props: {
    searchFilters: {
      type: Array,
      default: () => []
    },
    apiName: {
      type: String,
      default: () => ''
    },
    searchParams: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      searchQuery: null,
      paramsFilter: {},
      visibleFilter: false,
      fields: [],
      inputKey: null,
      inputValue: null,
      fieldValues: {},
      isFiltered: false,
      alertTypes: []
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
  },
  watch: {
    visibleFilter (newValue, oldValue) {
      if (newValue) {
        this.initFormFieldData()
      }
    },
    '$route' (to, from) {
      this.searchQuery = ''
      if (to && to.query && 'q' in to.query) {
        this.searchQuery = to.query.q
      }
      this.updateIsFiltered()
    }
  },
  mounted () {
    this.searchQuery = ''
    if (this.$route && this.$route.query && 'q' in this.$route.query) {
      this.searchQuery = this.$route.query.q
    }
    this.updateIsFiltered()
  },
  computed: {
    styleSearch () {
      if (!this.searchFilters || this.searchFilters.length === 0) {
        return {
          width: '100%',
          display: 'table-cell'
        }
      }

      return {
        width: '100%',
        display: 'table-cell',
        lineHeight: '31px'
      }
    }
  },
  methods: {
    onchange: async function (event, fieldname) {
      this.fetchDynamicFieldData(fieldname, event.target.value)
    },
    onSelectFieldChange (fieldname) {
      if (fieldname === 'domainid') {
        this.fetchDynamicFieldData('account')
      }
    },
    onVisibleForm () {
      this.visibleFilter = !this.visibleFilter
      if (!this.visibleFilter) return
      this.initFormFieldData()
    },
    retrieveFieldLabel (fieldName) {
      if (fieldName === 'groupid') {
        fieldName = 'group'
      }
      if (fieldName === 'keyword') {
        if ('listAnnotations' in this.$store.getters.apis) {
          return this.$t('label.annotation')
        } else {
          return this.$t('label.name')
        }
      }
      return this.$t('label.' + fieldName)
    },
    initFields () {
      const arrayField = []
      this.fields = []
      this.searchFilters.forEach(item => {
        let type = 'input'

        if (item === 'domainid' && !('listDomains' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'account' && !('listAccounts' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'account' && !('addAccountToProject' in this.$store.getters.apis || 'createAccount' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'podid' && !('listPods' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'clusterid' && !('listClusters' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'groupid' && !('listInstanceGroups' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'associatednetworkid' && this.$route.meta.name === 'asnumbers') {
          item = 'networkid'
        }
        if (item === 'usagetype' && !('listUsageTypes' in this.$store.getters.apis)) {
          return true
        }
        if (item === 'isencrypted' && !('listVolumes' in this.$store.getters.apis)) {
          return true
        }
        if (['zoneid', 'domainid', 'imagestoreid', 'storageid', 'state', 'account', 'hypervisor', 'level',
          'clusterid', 'podid', 'groupid', 'entitytype', 'accounttype', 'systemvmtype', 'scope', 'provider',
          'type', 'scope', 'managementserverid', 'serviceofferingid', 'diskofferingid', 'networkid', 'usagetype', 'restartrequired'].includes(item)
        ) {
          type = 'list'
        } else if (item === 'tags') {
          type = 'tag'
        } else if (item === 'resourcetype') {
          type = 'autocomplete'
        } else if (item === 'isencrypted') {
          type = 'boolean'
        }

        this.fields.push({
          type: type,
          name: item,
          opts: [],
          loading: false
        })
        arrayField.push(item)
      })
      return arrayField
    },
    fetchStaticFieldData (arrayField) {
      if (arrayField.includes('type')) {
        if (this.$route.path === '/guestnetwork' || this.$route.path.includes('/guestnetwork/')) {
          const typeIndex = this.fields.findIndex(item => item.name === 'type')
          this.fields[typeIndex].loading = true
          this.fields[typeIndex].opts = this.fetchGuestNetworkTypes()
          this.fields[typeIndex].loading = false
        } else if (this.$route.path === '/role' || this.$route.path.includes('/role/')) {
          const typeIndex = this.fields.findIndex(item => item.name === 'type')
          this.fields[typeIndex].loading = true
          this.fields[typeIndex].opts = this.fetchRoleTypes()
          this.fields[typeIndex].loading = false
        }
      }

      if (arrayField.includes('scope')) {
        const scopeIndex = this.fields.findIndex(item => item.name === 'scope')
        this.fields[scopeIndex].loading = true
        this.fields[scopeIndex].opts = this.fetchScope()
        this.fields[scopeIndex].loading = false
      }

      if (arrayField.includes('state')) {
        const stateIndex = this.fields.findIndex(item => item.name === 'state')
        this.fields[stateIndex].loading = true
        this.fields[stateIndex].opts = this.fetchState()
        this.fields[stateIndex].loading = false
      }

      if (arrayField.includes('level')) {
        const levelIndex = this.fields.findIndex(item => item.name === 'level')
        this.fields[levelIndex].loading = true
        this.fields[levelIndex].opts = this.fetchLevel()
        this.fields[levelIndex].loading = false
      }

      if (arrayField.includes('entitytype')) {
        const entityTypeIndex = this.fields.findIndex(item => item.name === 'entitytype')
        this.fields[entityTypeIndex].loading = true
        this.fields[entityTypeIndex].opts = this.fetchEntityType()
        this.fields[entityTypeIndex].loading = false
      }

      if (arrayField.includes('accounttype')) {
        const accountTypeIndex = this.fields.findIndex(item => item.name === 'accounttype')
        this.fields[accountTypeIndex].loading = true
        this.fields[accountTypeIndex].opts = this.fetchAccountTypes()
        this.fields[accountTypeIndex].loading = false
      }

      if (arrayField.includes('systemvmtype')) {
        const systemVmTypeIndex = this.fields.findIndex(item => item.name === 'systemvmtype')
        this.fields[systemVmTypeIndex].loading = true
        this.fields[systemVmTypeIndex].opts = this.fetchSystemVmTypes()
        this.fields[systemVmTypeIndex].loading = false
      }

      if (arrayField.includes('scope')) {
        const scopeIndex = this.fields.findIndex(item => item.name === 'scope')
        this.fields[scopeIndex].loading = true
        this.fields[scopeIndex].opts = this.fetchStoragePoolScope()
        this.fields[scopeIndex].loading = false
      }

      if (arrayField.includes('provider')) {
        const providerIndex = this.fields.findIndex(item => item.name === 'provider')
        this.fields[providerIndex].loading = true
        this.fields[providerIndex].opts = this.fetchImageStoreProviders()
        this.fields[providerIndex].loading = false
      }

      if (arrayField.includes('restartrequired')) {
        const restartRequiredIndex = this.fields.findIndex(item => item.name === 'restartrequired')
        this.fields[restartRequiredIndex].loading = true
        this.fields[restartRequiredIndex].opts = [
          { id: 'true', name: 'label.yes' },
          { id: 'false', name: 'label.no' }
        ]
        this.fields[restartRequiredIndex].loading = false
      }

      if (arrayField.includes('resourcetype')) {
        const resourceTypeIndex = this.fields.findIndex(item => item.name === 'resourcetype')
        this.fields[resourceTypeIndex].loading = true
        this.fields[resourceTypeIndex].opts = [
          { value: 'Account' },
          { value: 'Domain' },
          { value: 'Iso' },
          { value: 'Network' },
          { value: 'Template' },
          { value: 'User' },
          { value: 'VirtualMachine' },
          { value: 'Volume' },
          { value: 'QuotaTariff' }
        ]
        this.fields[resourceTypeIndex].loading = false
      }
    },
    async fetchDynamicFieldData (arrayField, searchKeyword) {
      const promises = []
      let typeIndex = -1
      let zoneIndex = -1
      let domainIndex = -1
      let accountIndex = -1
      let hypervisorIndex = -1
      let imageStoreIndex = -1
      let storageIndex = -1
      let podIndex = -1
      let clusterIndex = -1
      let groupIndex = -1
      let managementServerIdIndex = -1
      let serviceOfferingIndex = -1
      let diskOfferingIndex = -1
      let networkIndex = -1
      let usageTypeIndex = -1
      let volumeIndex = -1

      if (arrayField.includes('type')) {
        if (this.$route.path === '/alert') {
          typeIndex = this.fields.findIndex(item => item.name === 'type')
          this.fields[typeIndex].loading = true
          promises.push(await this.fetchAlertTypes())
        } else if (this.$route.path === '/affinitygroup') {
          typeIndex = this.fields.findIndex(item => item.name === 'type')
          this.fields[typeIndex].loading = true
          promises.push(await this.fetchAffinityGroupTypes())
        }
      }

      if (arrayField.includes('zoneid')) {
        zoneIndex = this.fields.findIndex(item => item.name === 'zoneid')
        this.fields[zoneIndex].loading = true
        promises.push(await this.fetchZones(searchKeyword))
      }

      if (arrayField.includes('domainid')) {
        domainIndex = this.fields.findIndex(item => item.name === 'domainid')
        this.fields[domainIndex].loading = true
        promises.push(await this.fetchDomains(searchKeyword))
      }

      if (arrayField.includes('account')) {
        accountIndex = this.fields.findIndex(item => item.name === 'account')
        this.fields[accountIndex].loading = true
        promises.push(await this.fetchAccounts(searchKeyword))
      }

      if (arrayField.includes('hypervisor')) {
        hypervisorIndex = this.fields.findIndex(item => item.name === 'hypervisor')
        this.fields[hypervisorIndex].loading = true
        promises.push(await this.fetchHypervisors())
      }

      if (arrayField.includes('imagestoreid')) {
        imageStoreIndex = this.fields.findIndex(item => item.name === 'imagestoreid')
        this.fields[imageStoreIndex].loading = true
        promises.push(await this.fetchImageStores(searchKeyword))
      }

      if (arrayField.includes('storageid')) {
        storageIndex = this.fields.findIndex(item => item.name === 'storageid')
        this.fields[storageIndex].loading = true
        promises.push(await this.fetchStoragePools(searchKeyword))
      }

      if (arrayField.includes('podid')) {
        podIndex = this.fields.findIndex(item => item.name === 'podid')
        this.fields[podIndex].loading = true
        promises.push(await this.fetchPods(searchKeyword))
      }

      if (arrayField.includes('clusterid')) {
        clusterIndex = this.fields.findIndex(item => item.name === 'clusterid')
        this.fields[clusterIndex].loading = true
        promises.push(await this.fetchClusters(searchKeyword))
      }

      if (arrayField.includes('groupid')) {
        groupIndex = this.fields.findIndex(item => item.name === 'groupid')
        this.fields[groupIndex].loading = true
        promises.push(await this.fetchInstanceGroups(searchKeyword))
      }

      if (arrayField.includes('managementserverid')) {
        managementServerIdIndex = this.fields.findIndex(item => item.name === 'managementserverid')
        this.fields[managementServerIdIndex].loading = true
        promises.push(await this.fetchManagementServers(searchKeyword))
      }

      if (arrayField.includes('serviceofferingid')) {
        serviceOfferingIndex = this.fields.findIndex(item => item.name === 'serviceofferingid')
        this.fields[serviceOfferingIndex].loading = true
        promises.push(await this.fetchServiceOfferings(searchKeyword))
      }

      if (arrayField.includes('diskofferingid')) {
        diskOfferingIndex = this.fields.findIndex(item => item.name === 'diskofferingid')
        this.fields[diskOfferingIndex].loading = true
        promises.push(await this.fetchDiskOfferings(searchKeyword))
      }

      if (arrayField.includes('networkid')) {
        networkIndex = this.fields.findIndex(item => item.name === 'networkid')
        this.fields[networkIndex].loading = true
        promises.push(await this.fetchNetworks(searchKeyword))
      }

      if (arrayField.includes('usagetype')) {
        usageTypeIndex = this.fields.findIndex(item => item.name === 'usagetype')
        this.fields[usageTypeIndex].loading = true
        promises.push(await this.fetchUsageTypes())
      }

      if (arrayField.includes('isencrypted')) {
        volumeIndex = this.fields.findIndex(item => item.name === 'isencrypted')
        this.fields[volumeIndex].loading = true
        promises.push(await this.fetchVolumes(searchKeyword))
      }

      Promise.all(promises).then(response => {
        if (typeIndex > -1) {
          const types = response.filter(item => item.type === 'type')
          if (types && types.length > 0) {
            this.fields[typeIndex].opts = this.sortArray(types[0].data)
          }
        }
        if (zoneIndex > -1) {
          const zones = response.filter(item => item.type === 'zoneid')
          if (zones && zones.length > 0) {
            this.fields[zoneIndex].opts = this.sortArray(zones[0].data)
          }
        }
        if (domainIndex > -1) {
          const domain = response.filter(item => item.type === 'domainid')
          if (domain && domain.length > 0) {
            this.fields[domainIndex].opts = this.sortArray(domain[0].data, 'path')
          }
        }
        if (accountIndex > -1) {
          const account = response.filter(item => item.type === 'account')
          if (account && account.length > 0) {
            this.fields[accountIndex].opts = this.sortArray(account[0].data, 'name')
          }
        }
        if (hypervisorIndex > -1) {
          const hypervisor = response.filter(item => item.type === 'hypervisor')
          if (hypervisor && hypervisor.length > 0) {
            this.fields[hypervisorIndex].opts = this.sortArray(hypervisor[0].data, 'name')
          }
        }
        if (imageStoreIndex > -1) {
          const imageStore = response.filter(item => item.type === 'imagestoreid')
          if (imageStore && imageStore.length > 0) {
            this.fields[imageStoreIndex].opts = this.sortArray(imageStore[0].data, 'name')
          }
        }
        if (storageIndex > -1) {
          const storagePool = response.filter(item => item.type === 'storageid')
          if (storagePool && storagePool.length > 0) {
            this.fields[storageIndex].opts = this.sortArray(storagePool[0].data, 'name')
          }
        }
        if (podIndex > -1) {
          const pod = response.filter(item => item.type === 'podid')
          if (pod && pod.length > 0) {
            this.fields[podIndex].opts = this.sortArray(pod[0].data)
          }
        }
        if (clusterIndex > -1) {
          const cluster = response.filter(item => item.type === 'clusterid')
          if (cluster && cluster.length > 0) {
            this.fields[clusterIndex].opts = this.sortArray(cluster[0].data)
          }
        }
        if (groupIndex > -1) {
          const groups = response.filter(item => item.type === 'groupid')
          if (groups && groups.length > 0) {
            this.fields[groupIndex].opts = this.sortArray(groups[0].data)
          }
        }

        if (managementServerIdIndex > -1) {
          const managementServers = response.filter(item => item.type === 'managementserverid')
          if (managementServers && managementServers.length > 0) {
            this.fields[managementServerIdIndex].opts = this.sortArray(managementServers[0].data)
          }
        }

        if (serviceOfferingIndex > -1) {
          const serviceOfferings = response.filter(item => item.type === 'serviceofferingid')
          if (serviceOfferings && serviceOfferings.length > 0) {
            this.fields[serviceOfferingIndex].opts = this.sortArray(serviceOfferings[0].data)
          }
        }

        if (diskOfferingIndex > -1) {
          const diskOfferings = response.filter(item => item.type === 'diskofferingid')
          if (diskOfferings && diskOfferings.length > 0) {
            this.fields[diskOfferingIndex].opts = this.sortArray(diskOfferings[0].data)
          }
        }

        if (networkIndex > -1) {
          const networks = response.filter(item => item.type === 'networkid')
          if (networks && networks.length > 0) {
            this.fields[networkIndex].opts = this.sortArray(networks[0].data)
          }
        }

        if (usageTypeIndex > -1) {
          const usageTypes = response.filter(item => item.type === 'usagetype')
          if (usageTypes?.length > 0) {
            this.fields[usageTypeIndex].opts = this.sortArray(usageTypes[0].data)
          }
        }
      }).finally(() => {
        if (typeIndex > -1) {
          this.fields[typeIndex].loading = false
        }
        if (zoneIndex > -1) {
          this.fields[zoneIndex].loading = false
        }
        if (domainIndex > -1) {
          this.fields[domainIndex].loading = false
        }
        if (accountIndex > -1) {
          this.fields[accountIndex].loading = false
        }
        if (imageStoreIndex > -1) {
          this.fields[imageStoreIndex].loading = false
        }
        if (storageIndex > -1) {
          this.fields[storageIndex].loading = false
        }
        if (podIndex > -1) {
          this.fields[podIndex].loading = false
        }
        if (clusterIndex > -1) {
          this.fields[clusterIndex].loading = false
        }
        if (groupIndex > -1) {
          this.fields[groupIndex].loading = false
        }
        if (managementServerIdIndex > -1) {
          this.fields[managementServerIdIndex].loading = false
        }
        if (serviceOfferingIndex > -1) {
          this.fields[serviceOfferingIndex].loading = false
        }
        if (diskOfferingIndex > -1) {
          this.fields[diskOfferingIndex].loading = false
        }
        if (networkIndex > -1) {
          this.fields[networkIndex].loading = false
        }
        if (usageTypeIndex > -1) {
          this.fields[usageTypeIndex].loading = false
        }
        if (Array.isArray(arrayField)) {
          this.fillFormFieldValues()
        }
        if (networkIndex > -1) {
          this.fields[networkIndex].loading = false
        }
        this.fillFormFieldValues()
      })
    },
    initFormFieldData () {
      const arrayField = this.initFields()

      this.fetchStaticFieldData(arrayField)

      this.fetchDynamicFieldData(arrayField)
    },
    sortArray (data, key = 'name') {
      if (!data) {
        return []
      }
      return data.sort(function (a, b) {
        if (a[key] < b[key]) { return -1 }
        if (a[key] > b[key]) { return 1 }

        return 0
      })
    },
    fillFormFieldValues () {
      this.fieldValues = {}
      if (Object.keys(this.$route.query).length > 0) {
        this.fieldValues = this.$route.query
      }
      if (this.$route.meta.params) {
        Object.assign(this.fieldValues, this.$route.meta.params)
      }
      this.fields.forEach(field => {
        this.form[field.name] = this.fieldValues[field.name]
      })
      this.inputKey = this.fieldValues['tags[0].key'] || null
      this.inputValue = this.fieldValues['tags[0].value'] || null
    },
    fetchZones (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listZones', { showicon: true, keyword: searchKeyword }).then(json => {
          const zones = json.listzonesresponse.zone
          resolve({
            type: 'zoneid',
            data: zones
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchDomains (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listDomains', { listAll: true, details: 'min', showicon: true, keyword: searchKeyword }).then(json => {
          const domain = json.listdomainsresponse.domain
          resolve({
            type: 'domainid',
            data: domain
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchAccounts (searchKeyword) {
      return new Promise((resolve, reject) => {
        const params = { listAll: true, isrecursive: false, showicon: true, keyword: searchKeyword }
        if (this.form.domainid) {
          params.domainid = this.form.domainid
        }
        api('listAccounts', params).then(json => {
          var account = json.listaccountsresponse.account
          if (this.form.domainid) {
            account = account.filter(a => a.domainid === this.form.domainid)
          }
          resolve({
            type: 'account',
            data: account
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchHypervisors () {
      return new Promise((resolve, reject) => {
        api('listHypervisors').then(json => {
          const hypervisor = json.listhypervisorsresponse.hypervisor.map(a => { return { id: a.name, name: a.name } })
          resolve({
            type: 'hypervisor',
            data: hypervisor
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchImageStores (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listImageStores', { listAll: true, showicon: true, keyword: searchKeyword }).then(json => {
          const imageStore = json.listimagestoresresponse.imagestore
          resolve({
            type: 'imagestoreid',
            data: imageStore
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchStoragePools (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listStoragePools', { listAll: true, showicon: true, keyword: searchKeyword }).then(json => {
          const storagePool = json.liststoragepoolsresponse.storagepool
          resolve({
            type: 'storageid',
            data: storagePool
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchPods (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listPods', { keyword: searchKeyword }).then(json => {
          const pods = json.listpodsresponse.pod
          resolve({
            type: 'podid',
            data: pods
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchClusters (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listClusters', { keyword: searchKeyword }).then(json => {
          const clusters = json.listclustersresponse.cluster
          resolve({
            type: 'clusterid',
            data: clusters
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchInstanceGroups (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listInstanceGroups', { listAll: true, keyword: searchKeyword }).then(json => {
          const instancegroups = json.listinstancegroupsresponse.instancegroup
          resolve({
            type: 'groupid',
            data: instancegroups
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchServiceOfferings (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listServiceOfferings', { listAll: true, keyword: searchKeyword }).then(json => {
          const serviceOfferings = json.listserviceofferingsresponse.serviceoffering
          resolve({
            type: 'serviceofferingid',
            data: serviceOfferings
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchDiskOfferings (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listDiskOfferings', { listAll: true, keyword: searchKeyword }).then(json => {
          const diskOfferings = json.listdiskofferingsresponse.diskoffering
          resolve({
            type: 'diskofferingid',
            data: diskOfferings
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchNetworks (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listNetworks', { listAll: true, keyword: searchKeyword }).then(json => {
          const networks = json.listnetworksresponse.network
          resolve({
            type: 'networkid',
            data: networks
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchAlertTypes () {
      if (this.alertTypes.length > 0) {
        return new Promise((resolve, reject) => {
          resolve({
            type: 'type',
            data: this.alertTypes
          })
        })
      } else {
        return new Promise((resolve, reject) => {
          api('listAlertTypes').then(json => {
            const alerttypes = json.listalerttypesresponse.alerttype.map(a => { return { id: a.alerttypeid, name: a.name } })
            this.alertTypes = alerttypes
            resolve({
              type: 'type',
              data: alerttypes
            })
          }).catch(error => {
            reject(error.response.headers['x-description'])
          })
        })
      }
    },
    fetchAffinityGroupTypes () {
      if (this.alertTypes.length > 0) {
        return new Promise((resolve, reject) => {
          resolve({
            type: 'type',
            data: this.alertTypes
          })
        })
      } else {
        return new Promise((resolve, reject) => {
          api('listAffinityGroupTypes').then(json => {
            const alerttypes = json.listaffinitygrouptypesresponse.affinityGroupType.map(a => {
              let name = a.type
              if (a.type === 'host anti-affinity') {
                name = 'host anti-affinity (Strict)'
              } else if (a.type === 'host affinity') {
                name = 'host affinity (Strict)'
              } else if (a.type === 'non-strict host anti-affinity') {
                name = 'host anti-affinity (Non-Strict)'
              } else if (a.type === 'non-strict host affinity') {
                name = 'host affinity (Non-Strict)'
              }
              return { id: a.type, name: name }
            })
            this.alertTypes = alerttypes
            resolve({
              type: 'type',
              data: alerttypes
            })
          }).catch(error => {
            reject(error.response.headers['x-description'])
          })
        })
      }
    },
    fetchVolumes (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listvolumes', { listAll: true, isencrypted: searchKeyword }).then(json => {
          const volumes = json.listvolumesresponse.volume
          resolve({
            type: 'isencrypted',
            data: volumes
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchManagementServers (searchKeyword) {
      return new Promise((resolve, reject) => {
        api('listManagementServers', { listAll: true, keyword: searchKeyword }).then(json => {
          const managementservers = json.listmanagementserversresponse.managementserver
          resolve({
            type: 'managementserverid',
            data: managementservers
          })
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    fetchGuestNetworkTypes () {
      const types = []
      if (this.apiName.indexOf('listNetworks') > -1) {
        types.push({
          id: 'Isolated',
          name: 'label.isolated'
        })
        types.push({
          id: 'Shared',
          name: 'label.shared'
        })
        types.push({
          id: 'L2',
          name: 'label.l2'
        })
      }
      return types
    },
    fetchAccountTypes () {
      const types = []
      if (this.apiName.indexOf('listAccounts') > -1) {
        types.push({
          id: '1',
          name: 'Admin'
        })
        types.push({
          id: '2',
          name: 'DomainAdmin'
        })
        types.push({
          id: '3',
          name: 'User'
        })
      }
      return types
    },
    fetchSystemVmTypes () {
      const types = []
      if (this.apiName.indexOf('listSystemVms') > -1) {
        types.push({
          id: 'consoleproxy',
          name: 'label.console.proxy.vm'
        })
        types.push({
          id: 'secondarystoragevm',
          name: 'label.secondary.storage.vm'
        })
      }
      return types
    },
    fetchStoragePoolScope () {
      const types = []
      if (this.apiName.indexOf('listStoragePools') > -1) {
        types.push({
          id: 'HOST',
          name: 'label.hostname'
        })
        types.push({
          id: 'CLUSTER',
          name: 'label.cluster'
        })
        types.push({
          id: 'ZONE',
          name: 'label.zone'
        })
        types.push({
          id: 'REGION',
          name: 'label.region'
        })
        types.push({
          id: 'GLOBAL',
          name: 'label.global'
        })
      }
      return types
    },
    fetchImageStoreProviders () {
      const types = []
      if (this.apiName.indexOf('listImageStores') > -1) {
        types.push({
          id: 'NFS',
          name: 'NFS'
        })
        types.push({
          id: 'SMB/CIFS',
          name: 'SMB/CIFS'
        })
        types.push({
          id: 'S3',
          name: 'S3'
        })
        types.push({
          id: 'Swift',
          name: 'Swift'
        })
      }
      return types
    },
    fetchRoleTypes () {
      const types = []
      if (this.apiName.indexOf('listRoles') > -1) {
        types.push({
          id: 'Admin',
          name: 'Admin'
        })
        types.push({
          id: 'ResourceAdmin',
          name: 'ResourceAdmin'
        })
        types.push({
          id: 'DomainAdmin',
          name: 'DomainAdmin'
        })
        types.push({
          id: 'User',
          name: 'User'
        })
      }
      return types
    },
    fetchScope () {
      const scope = []
      if (this.apiName.indexOf('listWebhooks') > -1) {
        scope.push({
          id: 'Local',
          name: 'label.local'
        })
        scope.push({
          id: 'Domain',
          name: 'label.domain'
        })
        if (isAdmin()) {
          scope.push({
            id: 'Global',
            name: 'label.global'
          })
        }
      }
      return scope
    },
    fetchState () {
      var state = []
      if (this.apiName.includes('listVolumes')) {
        state = [
          {
            id: 'Allocated',
            name: 'label.allocated'
          },
          {
            id: 'Ready',
            name: 'label.isready'
          },
          {
            id: 'Destroy',
            name: 'label.destroy'
          },
          {
            id: 'Expunging',
            name: 'label.expunging'
          },
          {
            id: 'Expunged',
            name: 'label.expunged'
          },
          {
            id: 'Migrating',
            name: 'label.migrating'
          }
        ]
      } else if (this.apiName.includes('listKubernetesClusters')) {
        state = [
          {
            id: 'Created',
            name: 'label.created'
          },
          {
            id: 'Starting',
            name: 'label.starting'
          },
          {
            id: 'Running',
            name: 'label.running'
          },
          {
            id: 'Stopping',
            name: 'label.stopping'
          },
          {
            id: 'Stopped',
            name: 'label.stopped'
          },
          {
            id: 'Scaling',
            name: 'label.scaling'
          },
          {
            id: 'Upgrading',
            name: 'label.upgrading'
          },
          {
            id: 'Alert',
            name: 'label.alert'
          },
          {
            id: 'Recovering',
            name: 'label.recovering'
          },
          {
            id: 'Destroyed',
            name: 'label.destroyed'
          },
          {
            id: 'Destroying',
            name: 'label.destroying'
          },
          {
            id: 'Error',
            name: 'label.error'
          }
        ]
      } else if (this.apiName.indexOf('listWebhooks') > -1) {
        state = [
          {
            id: 'Enabled',
            name: 'label.enabled'
          },
          {
            id: 'Disabled',
            name: 'label.disabled'
          }
        ]
      }
      return state
    },
    fetchEntityType () {
      const entityType = []
      if (this.apiName.indexOf('listAnnotations') > -1) {
        const allowedTypes = {
          VM: 'Virtual Machine',
          HOST: 'Host',
          VOLUME: 'Volume',
          SNAPSHOT: 'Snapshot',
          VM_SNAPSHOT: 'VM Snapshot',
          INSTANCE_GROUP: 'Instance Group',
          NETWORK: 'Network',
          VPC: 'VPC',
          PUBLIC_IP_ADDRESS: 'Public IP Address',
          VPN_CUSTOMER_GATEWAY: 'VPC Customer Gateway',
          TEMPLATE: 'Template',
          ISO: 'ISO',
          SSH_KEYPAIR: 'SSH Key Pair',
          DOMAIN: 'Domain',
          SERVICE_OFFERING: 'Service Offfering',
          DISK_OFFERING: 'Disk Offering',
          NETWORK_OFFERING: 'Network Offering',
          POD: 'Pod',
          ZONE: 'Zone',
          CLUSTER: 'Cluster',
          PRIMARY_STORAGE: 'Primary Storage',
          SECONDARY_STORAGE: 'Secondary Storage',
          VR: 'Virtual Router',
          SYSTEM_VM: 'System VM',
          KUBERNETES_CLUSTER: 'Kubernetes Cluster'
        }
        for (var key in allowedTypes) {
          entityType.push({
            id: key,
            name: allowedTypes[key]
          })
        }
      }
      return entityType
    },
    fetchLevel () {
      const levels = []
      levels.push({
        id: 'INFO',
        name: 'label.info.upper'
      })
      levels.push({
        id: 'WARN',
        name: 'label.warn.upper'
      })
      levels.push({
        id: 'ERROR',
        name: 'label.error.upper'
      })
      return levels
    },
    fetchUsageTypes () {
      return new Promise((resolve, reject) => {
        api('listUsageTypes')
          .then(json => {
            const usageTypes = json.listusagetypesresponse.usagetype.map(entry => {
              return {
                id: entry.id,
                name: i18n.global.t(entry.name)
              }
            })

            resolve({
              type: 'usagetype',
              data: usageTypes
            })
          })
          .catch(error => {
            reject(error.response.headers['x-description'])
          })
      })
    },
    onSearch (value) {
      this.paramsFilter = {}
      this.searchQuery = value
      this.$emit('search', { searchQuery: this.searchQuery })
    },
    onClear () {
      this.formRef.value.resetFields()
      this.form = reactive({})
      this.isFiltered = false
      this.inputKey = null
      this.inputValue = null
      this.searchQuery = null
      this.paramsFilter = {}
      this.$emit('search', this.paramsFilter)
    },
    handleSubmit () {
      this.paramsFilter = {}
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.isFiltered = true
        for (const key in values) {
          const input = values[key]
          if (input === '' || input === null || input === undefined) {
            continue
          }
          this.paramsFilter[key] = input
        }
        if (this.searchFilters.includes('tags')) {
          if (this.inputKey) {
            this.paramsFilter['tags[0].key'] = this.inputKey
            this.paramsFilter['tags[0].value'] = this.inputValue
          }
        }
        this.$emit('search', this.paramsFilter)
      })
    },
    changeFilter (filter) {
      this.$emit('change-filter', filter)
    },
    updateIsFiltered () {
      this.isFiltered = this.searchFilters.some(item => {
        if (this.searchParams[item]) {
          return true
        }
      })
    }
  }
}
</script>

<style scoped lang="less">
.input-search {
  margin-left: 10px;
}

.filter-group {
  :deep(.ant-input-group-addon) {
    padding: 0 5px;
  }

  &-button {
    background: inherit;
    border: 0;
    padding: 0;
  }

  &-button {
    position: relative;
    display: block;
    min-height: 25px;

    &-clear {
      position: absolute;
      left: 0;
    }

    &-search {
      position: absolute;
      right: 0;
    }
  }
}

.filter-button {
  background: inherit;
  border: 0;
  padding: 0;
  position: relative;
  display: block;
  min-height: 25px;
  width: 20px;
}
</style>
