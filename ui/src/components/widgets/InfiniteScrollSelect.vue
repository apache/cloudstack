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
<!--
  InfiniteScrollSelect.vue

  A reusable select component that supports:
  - Infinite scrolling with paginated API
  - Dynamic search filtering. Needs minimum
  - Deduplicated option loading
  - Auto-fetching of preselected value if not present in the initial result

  Usage Example:

  <infinite-scroll-select
    v-model:value="form.account"
    api="listAccounts"
    :apiParams="accountsApiParams"
    resourceType="account"
    optionValueKey="name"
    optionLabelKey="name"
    @change-option-value="handleAccountNameChange" />

  Props:
  - api (String, required): API command name (e.g., 'listAccounts')
  - apiParams (Object, optional): Additional parameters passed to the API
  - resourceType (String, required): The key in the API response containing the resource array (e.g., 'account')
  - optionValueKey (String, optional): Property to use as the value for options (e.g., 'name'). Default is 'id'
  - optionLabelKey (String, optional): Property to use as the label for options (e.g., 'name'). Default is 'name'
  - defaultOption (Object, optional): Preselected object to include initially
  - allowClear (Boolean, optional): Whether to allow clearing the selection. Default is false
  - showIcon (Boolean, optional): Whether to show icon for the options. Default is true
  - defaultIcon (String, optional): Icon to be shown when there is no resource icon for the option. Default is 'cloud-outlined'
  - autoSelectFirstOption (Boolean, optional): Whether to automatically select the first option when options are loaded. Default is false
  - selectFirstOption (Boolean, optional): Whether to automatically select the first option when options are loaded. Default is false

  Events:
  - @change-option-value (Function): Emits the selected option value(s) when value(s) changes. Do not use @change as it will give warnings and may not work
  - @change-option (Function): Emits the selected option object when value changes. Works only when mode is not multiple

  Features:
  - Debounced remote filtering
  - Custom dropdown footer/header (e.g., clear search button)
  - Handles preselection and fetches missing option automatically
-->
<template>
  <a-select
    :filter-option="false"
    :loading="loading"
    show-search
    :allowClear="allowClear"
    placeholder="Select"
    @search="onSearchTimed"
    @popupScroll="onScroll"
    @change="onChange"
  >
    <template #dropdownRender="{ menuNode: menu }">
      <v-nodes :vnodes="menu" />
      <div v-if="!!searchQuery">
        <a-divider style="margin: 4px 0" />
        <div class="select-list-footer">
          <span>{{ formattedSearchFooterMessage }}</span>
          <close-outlined
            @mousedown="e => e.preventDefault()"
            @click="onSearch()" />
        </div>
      </div>
    </template>
    <a-select-option v-for="option in selectableOptions" :key="option.id" :value="option[optionValueKey]">
      <span>
        <span v-if="showIcon && option.showicon !== false && option.id !== null && option.id !== undefined">
          <resource-icon v-if="option.icon && option.icon.base64image" :image="option.icon.base64image" size="1x" style="margin-right: 5px"/>
          <render-icon v-else :icon="defaultIcon" style="margin-right: 5px" />
        </span>
        <span>{{ optionLabelFn ? optionLabelFn(option) : option[optionLabelKey] }}</span>
      </span>
    </a-select-option>
  </a-select>
</template>

<script>
import { callAPI } from '@/api/index'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'InfiniteScrollSelect',
  components: {
    ResourceIcon,
    VNodes: (_, { attrs }) => {
      return attrs.vnodes
    }
  },
  props: {
    api: {
      type: String,
      required: true
    },
    apiParams: {
      type: Object,
      required: null
    },
    resourceType: {
      type: String,
      required: true
    },
    optionValueKey: {
      type: String,
      default: 'id'
    },
    optionLabelKey: {
      type: String,
      default: 'name'
    },
    optionLabelFn: {
      type: Function,
      default: null
    },
    defaultOption: {
      type: Object,
      default: null
    },
    allowClear: {
      type: Boolean,
      default: false
    },
    showIcon: {
      type: Boolean,
      default: true
    },
    defaultIcon: {
      type: String,
      default: 'cloud-outlined'
    },
    pageSize: {
      type: Number,
      default: null
    },
    autoSelectFirstOption: {
      type: Boolean,
      default: false
    },
    selectFirstOption: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      options: [],
      page: 1,
      totalCount: null,
      loading: false,
      searchQuery: '',
      searchTimer: null,
      scrollHandlerAttached: false,
      preselectedOptionValue: null,
      successiveFetches: 0,
      canSelectFirstOption: false,
      hasAutoSelectedFirst: false
    }
  },
  created () {
    this.addDefaultOptionIfNeeded()
  },
  mounted () {
    this.preselectedOptionValue = this.$attrs.value
    this.fetchItems()
  },
  computed: {
    maxSuccessiveFetches () {
      return 10
    },
    computedPageSize () {
      return this.pageSize || this.$store.getters.defaultListViewPageSize
    },
    formattedSearchFooterMessage () {
      return `${this.$t('label.showing.results.for').replace('%x', this.searchQuery)}`
    },
    selectableOptions () {
      const currentValue = this.$attrs.value
      // Only filter out null/empty options when the current value is also null/undefined/empty
      // This prevents such options from being selected and allows the placeholder to show instead
      if (currentValue === null || currentValue === undefined || currentValue === '') {
        return this.options.filter(option => {
          const optionValue = option[this.optionValueKey]
          return optionValue !== null && optionValue !== undefined && optionValue !== ''
        })
      }
      // When a valid value is selected, show all options
      return this.options
    },
    apiOptionsCount () {
      if (this.defaultOption) {
        const defaultOptionValue = this.defaultOption[this.optionValueKey]
        return this.options.filter(option => option[this.optionValueKey] !== defaultOptionValue).length
      }
      return this.options.length
    },
    preselectedMatchValue () {
      // Extract the first value from preselectedOptionValue if it's an array, otherwise return the value itself
      if (!this.preselectedOptionValue) return null
      return Array.isArray(this.preselectedOptionValue) ? this.preselectedOptionValue[0] : this.preselectedOptionValue
    },
    preselectedMatch () {
      // Find the matching option for the preselected value
      if (!this.preselectedMatchValue) return null
      return this.options.find(entry => entry[this.optionValueKey] === this.preselectedMatchValue) || null
    }
  },
  watch: {
    apiParams () {
      this.onSearch()
    }
  },
  emits: ['change-option-value', 'change-option'],
  methods: {
    async fetchItems () {
      if (this.successiveFetches === 0 && this.loading) return
      this.loading = true
      const params = {
        page: this.page,
        pagesize: this.computedPageSize
      }
      if (this.searchQuery && this.searchQuery.length > 0) {
        params.keyword = this.searchQuery
      }
      if (this.apiParams) {
        Object.assign(params, this.apiParams)
      }
      if (this.showIcon) {
        params.showicon = true
      }
      callAPI(this.api, params).then(json => {
        const response = json[this.api.toLowerCase() + 'response'] || {}
        if (this.totalCount === null) {
          this.totalCount = response.count || 0
        }
        const newOpts = response[this.resourceType] || []
        const existingOptions = new Set(this.options.map(o => o[this.optionValueKey]))
        newOpts.forEach(opt => {
          if (!existingOptions.has(opt[this.optionValueKey])) {
            this.options.push(opt)
          }
        })
        this.page++
        this.checkAndFetchPreselectedOption()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.canSelectFirstOption = true
        if (this.successiveFetches === 0) {
          this.loading = false
          this.autoSelectFirstOptionIfNeeded()
        }
      })
    },
    checkAndFetchPreselectedOption () {
      if (!this.preselectedOptionValue ||
        (Array.isArray(this.preselectedOptionValue) && this.preselectedOptionValue.length === 0) ||
        this.successiveFetches >= this.maxSuccessiveFetches) {
        this.resetPreselectedOptionValue()
        if (!this.canSelectFirstOption && this.autoSelectFirstOption && this.options.length > 0) {
          this.$nextTick(() => {
            this.preselectedOptionValue = this.options[0][this.optionValueKey]
            this.onChange(this.preselectedOptionValue)
          })
        }
        return
      }
      if (!this.preselectedMatch) {
        this.successiveFetches++
        // Exclude defaultOption from count when comparing with totalCount
        if (this.apiOptionsCount < this.totalCount) {
          this.fetchItems()
        } else {
          this.resetPreselectedOptionValue()
        }
        return
      }
      if (Array.isArray(this.preselectedOptionValue) && this.preselectedOptionValue.length > 1) {
        this.preselectedOptionValue = this.preselectedOptionValue.filter(o => o !== this.preselectedMatchValue)
      } else {
        this.resetPreselectedOptionValue()
      }
    },
    addDefaultOptionIfNeeded () {
      if (this.defaultOption) {
        this.canSelectFirstOption = true
        this.options.push(this.defaultOption)
      }
    },
    resetPreselectedOptionValue () {
      this.preselectedOptionValue = null
      this.successiveFetches = 0
    },
    autoSelectFirstOptionIfNeeded () {
      if (!this.selectFirstOption || this.hasAutoSelectedFirst) {
        return
      }
      // Don't auto-select if there's a preselected value being fetched
      if (this.preselectedOptionValue) {
        return
      }
      const currentValue = this.$attrs.value
      if (currentValue !== undefined && currentValue !== null && currentValue !== '') {
        return
      }
      if (this.options.length === 0) {
        return
      }
      if (this.searchQuery && this.searchQuery.length > 0) {
        return
      }
      // Only auto-select after initial load is complete (no more successive fetches)
      if (this.successiveFetches > 0) {
        return
      }
      const firstOption = this.options[0]
      if (firstOption) {
        const firstValue = firstOption[this.optionValueKey]
        this.hasAutoSelectedFirst = true
        this.$emit('change-option-value', firstValue)
        this.$emit('change-option', firstOption)
      }
    },
    onSearchTimed (value) {
      clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.onSearch(value)
      }, 500)
    },
    onSearch (value) {
      this.searchQuery = value
      this.page = 1
      this.totalCount = null
      this.options = []
      if (!this.searchQuery) {
        this.addDefaultOptionIfNeeded()
      }
      this.fetchItems()
    },
    onScroll (e) {
      const nearBottom = e.target.scrollTop + e.target.clientHeight >= e.target.scrollHeight - 10
      // Exclude defaultOption from count when comparing with totalCount
      const hasMore = this.apiOptionsCount < this.totalCount
      if (nearBottom && hasMore && !this.loading) {
        this.fetchItems()
      }
    },
    onChange (value) {
      this.resetPreselectedOptionValue()
      this.$emit('change-option-value', value)
      if (Array.isArray(value)) {
        return
      }
      if (value === undefined || value == null) {
        this.$emit('change-option', undefined)
        return
      }
      const match = this.options.find(entry => entry[this.optionValueKey] === value)
      if (match) {
        this.$emit('change-option', match)
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .select-list-footer {
    margin: 4px 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
</style>
