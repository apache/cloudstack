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
        style="width: 100%; display: table-cell"
        :placeholder="$t('label.search')"
        v-model="searchQuery"
        allowClear
        @search="onSearch" />
    </span>

    <span
      v-else
      class="filter-group">
      <a-input-search
        allowClear
        class="input-search"
        :placeholder="$t('label.search')"
        v-model="searchQuery"
        @search="onSearch">
        <a-popover
          placement="bottomRight"
          slot="addonBefore"
          trigger="click"
          v-model="visibleFilter">
          <template slot="content" v-if="visibleFilter">
            <a-form
              style="min-width: 170px"
              :form="form"
              layout="vertical"
              @submit="handleSubmit">
              <a-form-item
                v-for="(field, index) in fields"
                :key="index"
                :label="field.name==='keyword' ? $t('label.name') : $t('label.' + field.name)">
                <a-select
                  allowClear
                  v-if="field.type==='list'"
                  v-decorator="[field.name, {
                    initialValue: fieldValues[field.name] || null
                  }]"
                  :loading="field.loading">
                  <a-select-option
                    v-for="(opt, idx) in field.opts"
                    :key="idx"
                    :value="opt.id">{{ $t(opt.name) }}</a-select-option>
                </a-select>
                <a-input
                  v-else-if="field.type==='input'"
                  v-decorator="[field.name, {
                    initialValue: fieldValues[field.name] || null
                  }]" />
                <div v-else-if="field.type==='tag'">
                  <div>
                    <a-input-group
                      type="text"
                      size="small"
                      compact>
                      <a-input ref="input" :value="inputKey" @change="e => inputKey = e.target.value" style="width: 50px; text-align: center" :placeholder="$t('label.key')" />
                      <a-input style=" width: 20px; border-left: 0; pointer-events: none; backgroundColor: #fff" placeholder="=" disabled />
                      <a-input :value="inputValue" @change="handleValueChange" style="width: 50px; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
                      <tooltip-button :tooltip="$t('label.clear')" icon="close" size="small" @click="inputKey = inputValue = ''" />
                    </a-input-group>
                  </div>
                </div>
              </a-form-item>
              <div class="filter-group-button">
                <a-button
                  class="filter-group-button-clear"
                  type="default"
                  size="small"
                  icon="stop"
                  @click="onClear">{{ $t('label.reset') }}</a-button>
                <a-button
                  class="filter-group-button-search"
                  type="primary"
                  size="small"
                  icon="search"
                  html-type="submit"
                  @click="handleSubmit">{{ $t('label.search') }}</a-button>
              </div>
            </a-form>
          </template>
          <a-button
            class="filter-button"
            size="small"
            @click="() => { searchQuery = null }">
            <a-icon type="filter" :theme="isFiltered ? 'twoTone' : 'outlined'" />
          </a-button>
        </a-popover>
      </a-input-search>
    </span>
  </span>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'SearchView',
  components: {
    TooltipButton
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
  inject: ['parentSearch', 'parentChangeFilter'],
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
  beforeCreate () {
    this.form = this.$form.createForm(this)
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
        if (['zoneid', 'domainid', 'state', 'level', 'clusterid', 'podid'].includes(item)) {
          type = 'list'
        } else if (item === 'tags') {
          type = 'tag'
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

      Promise.all(promises).then(response => {
        if (zoneIndex > -1) {
          const zones = response.filter(item => item.type === 'zoneid')
          if (zones && zones.length > 0) {
            this.fields[zoneIndex].opts = zones[0].data
          }
        }
        if (domainIndex > -1) {
          const domain = response.filter(item => item.type === 'domainid')
          if (domain && domain.length > 0) {
            this.fields[domainIndex].opts = domain[0].data
          }
        }
        if (podIndex > -1) {
          const pod = response.filter(item => item.type === 'podid')
          if (pod && pod.length > 0) {
            this.fields[podIndex].opts = pod[0].data
          }
        }
        if (clusterIndex > -1) {
          const cluster = response.filter(item => item.type === 'clusterid')
          if (cluster && cluster.length > 0) {
            this.fields[clusterIndex].opts = cluster[0].data
          }
        }
        this.$forceUpdate()
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
        this.fillFormFieldValues()
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
      this.inputKey = this.fieldValues['tags[0].key'] || null
      this.inputValue = this.fieldValues['tags[0].value'] || null
    },
    fetchZones () {
      return new Promise((resolve, reject) => {
        api('listZones', { listAll: true }).then(json => {
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
        api('listDomains', { listAll: true }).then(json => {
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
        api('listPods', { listAll: true }).then(json => {
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
        api('listClusters', { listAll: true }).then(json => {
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
      this.parentSearch({ searchQuery: this.searchQuery })
    },
    onClear () {
      this.searchFilters.map(item => {
        const field = {}
        field[item] = undefined
        this.form.setFieldsValue(field)
      })
      this.isFiltered = false
      this.inputKey = null
      this.inputValue = null
      this.searchQuery = null
      this.paramsFilter = {}
      this.parentSearch(this.paramsFilter)
    },
    handleSubmit (e) {
      e.preventDefault()
      this.paramsFilter = {}
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
        this.parentSearch(this.paramsFilter)
      })
    },
    handleKeyChange (e) {
      this.inputKey = e.target.value
    },
    handleValueChange (e) {
      this.inputValue = e.target.value
    },
    changeFilter (filter) {
      this.parentChangeFilter(filter)
    }
  }
}
</script>

<style scoped lang="less">
.input-search {
  margin-left: 10px;
}

.filter-group {
  /deep/.ant-input-group-addon {
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

  /deep/.ant-input-group {
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
