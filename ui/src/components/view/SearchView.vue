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
                    :loading="field.loading">
                    <a-select-option
                      v-for="(opt, idx) in field.opts"
                      :key="idx"
                      :value="opt.id"
                      :label="$t(opt.path || opt.name)">
                      <div>
                        <span v-if="(field.name.startsWith('zone'))">
                          <span v-if="opt.icon">
                            <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                          </span>
                          <global-outlined v-else style="margin-right: 5px" />
                        </span>
                        <span v-if="(field.name.startsWith('domain'))">
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
import TooltipButton from '@/components/widgets/TooltipButton'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'SearchView',
  components: {
    TooltipButton,
    ResourceIcon
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
      isFiltered: false
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
      this.isFiltered = false
      this.searchFilters.some(item => {
        if (this.searchParams[item]) {
          this.isFiltered = true
          return true
        }
      })
    }
  },
  mounted () {
    this.searchQuery = ''
    if (this.$route && this.$route.query && 'q' in this.$route.query) {
      this.searchQuery = this.$route.query.q
    }
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
    async initFormFieldData () {
      const arrayField = []
      this.fields = []
      this.searchFilters.forEach(item => {
        let type = 'input'

        if (item === 'domainid' && !('listDomains' in this.$store.getters.apis)) {
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
        if (['zoneid', 'domainid', 'state', 'level', 'clusterid', 'podid', 'groupid', 'entitytype', 'type'].includes(item)) {
          type = 'list'
        } else if (item === 'tags') {
          type = 'tag'
        } else if (item === 'resourcetype') {
          type = 'autocomplete'
        }

        this.fields.push({
          type: type,
          name: item,
          opts: [],
          loading: false
        })
        arrayField.push(item)
      })

      const promises = []
      let zoneIndex = -1
      let domainIndex = -1
      let podIndex = -1
      let clusterIndex = -1
      let groupIndex = -1

      if (arrayField.includes('type')) {
        if (this.$route.path === '/guestnetwork' || this.$route.path.includes('/guestnetwork/')) {
          const typeIndex = this.fields.findIndex(item => item.name === 'type')
          this.fields[typeIndex].loading = true
          this.fields[typeIndex].opts = this.fetchGuestNetworkTypes()
          this.fields[typeIndex].loading = false
        }
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

      if (arrayField.includes('zoneid')) {
        zoneIndex = this.fields.findIndex(item => item.name === 'zoneid')
        this.fields[zoneIndex].loading = true
        promises.push(await this.fetchZones())
      }

      if (arrayField.includes('domainid')) {
        domainIndex = this.fields.findIndex(item => item.name === 'domainid')
        this.fields[domainIndex].loading = true
        promises.push(await this.fetchDomains())
      }

      if (arrayField.includes('podid')) {
        podIndex = this.fields.findIndex(item => item.name === 'podid')
        this.fields[podIndex].loading = true
        promises.push(await this.fetchPods())
      }

      if (arrayField.includes('clusterid')) {
        clusterIndex = this.fields.findIndex(item => item.name === 'clusterid')
        this.fields[clusterIndex].loading = true
        promises.push(await this.fetchClusters())
      }

      if (arrayField.includes('groupid')) {
        groupIndex = this.fields.findIndex(item => item.name === 'groupid')
        this.fields[groupIndex].loading = true
        promises.push(await this.fetchInstanceGroups())
      }

      if (arrayField.includes('entitytype')) {
        const entityTypeIndex = this.fields.findIndex(item => item.name === 'entitytype')
        this.fields[entityTypeIndex].loading = true
        this.fields[entityTypeIndex].opts = this.fetchEntityType()
        this.fields[entityTypeIndex].loading = false
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
          { value: 'Volume' }
        ]
        this.fields[resourceTypeIndex].loading = false
      }

      Promise.all(promises).then(response => {
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
      }).finally(() => {
        if (zoneIndex > -1) {
          this.fields[zoneIndex].loading = false
        }
        if (domainIndex > -1) {
          this.fields[domainIndex].loading = false
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
        this.fillFormFieldValues()
      })
    },
    sortArray (data, key = 'name') {
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
    fetchZones () {
      return new Promise((resolve, reject) => {
        api('listZones', { showicon: true }).then(json => {
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
    fetchDomains () {
      return new Promise((resolve, reject) => {
        api('listDomains', { listAll: true, showicon: true }).then(json => {
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
    fetchPods () {
      return new Promise((resolve, reject) => {
        api('listPods').then(json => {
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
    fetchClusters () {
      return new Promise((resolve, reject) => {
        api('listClusters').then(json => {
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
    fetchInstanceGroups () {
      return new Promise((resolve, reject) => {
        api('listInstanceGroups', { listAll: true }).then(json => {
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
    fetchState () {
      const state = []
      if (this.apiName.indexOf('listVolumes') > -1) {
        state.push({
          id: 'Allocated',
          name: 'label.allocated'
        })
        state.push({
          id: 'Ready',
          name: 'label.isready'
        })
        state.push({
          id: 'Destroy',
          name: 'label.destroy'
        })
        state.push({
          id: 'Expunging',
          name: 'label.expunging'
        })
        state.push({
          id: 'Expunged',
          name: 'label.expunged'
        })
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

  :deep(.ant-input-group) {
    .ant-input-affix-wrapper {
      width: calc(100% - 10px);
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
