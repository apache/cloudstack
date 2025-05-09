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
  <a-select
    :filter-option="false"
    :loading="loading"
    show-search
    placeholder="Select"
    @search="onSearch"
    @popupScroll="onScroll"
    @change="onChange"
  >
    <template #dropdownRender="{ menuNode: menu }">
      <v-nodes :vnodes="menu" />
      <div v-if="searchQuery">
        <a-divider style="margin: 4px 0" />
        <div class="search-footer">
          <span>{{ 'Showing results for "' + searchQuery + '"' }}</span>
          <close-outlined
            @mousedown="e => e.preventDefault()"
            @click="onSearch()" />
        </div>
      </div>
    </template>
    <a-select-option v-for="option in options" :key="option.id" :value="option.id">
      <span>
        <resource-icon v-if="option.icon && option.icon.base64image" :option="option.icon.base64image" size="1x" style="margin-right: 5px"/>
        <render-icon :icon="defaultIcon" style="margin-right: 5px" />
        <span>{{ option.name }}</span>
      </span>
    </a-select-option>
  </a-select>
</template>

<script>
import { api } from '@/api'
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
    resourceType: {
      type: String,
      required: true
    },
    defaultOption: {
      type: Object,
      default: null
    },
    defaultIcon: {
      type: String,
      default: 'cloud-outlined'
    }
  },
  data () {
    return {
      options: [],
      page: 1,
      pageSize: 50,
      totalCount: null,
      loading: false,
      searchQuery: '',
      scrollHandlerAttached: false
    }
  },
  created () {
    this.addDefaultOptionIfNeeded(true)
  },
  mounted () {
    this.fetchItems()
  },
  emits: ['change-option'],
  methods: {
    async fetchItems () {
      if (this.loading) return
      this.loading = true
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        keyword: this.searchQuery,
        listall: true
      }
      api(this.api, params).then(json => {
        const response = json[this.api.toLowerCase() + 'response'] || {}
        if (this.totalCount === null) {
          this.totalCount = response.count || 0
        }
        const newOpts = response[this.resourceType] || []
        this.options = this.options.concat(newOpts)
        this.page++
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    addDefaultOptionIfNeeded () {
      if (this.defaultOption) {
        this.options.push(this.defaultOption)
      }
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
      const hasMore = this.options.length < this.totalCount
      if (nearBottom && hasMore && !this.loading) {
        this.fetchItems()
      }
    },
    onChange (id) {
      const match = this.options.find(entry => entry.id === id)
      if (match) {
        this.$emit('change-option', match)
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .search-footer {
    margin: 4px 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
</style>
