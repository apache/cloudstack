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
  <span class="filter-group">
    <a-row type="flex">
      <a-col flex="200px" v-if="!filtersDisabled">
        <a-select
          mode="multiple"
          class="filter-select"
          :placeholder="$t('label.filterby')"
          v-model:value="filterValues"
          @change="handleFilterChange"
          showSearch
          allowClear
          :showArrow="true"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }">
          <template #suffixIcon><filter-outlined class="ant-select-suffix" /></template>
          <a-select-option
            v-for="filter in filters"
            :key="filter"
            :label="$t('label.' + filter)">
            {{ $t('label.' + filter) }}
          </a-select-option>
        </a-select>
      </a-col>
      <a-col flex="auto">
        <a-input-search
          class="filter-search"
          :placeholder="$t('label.search')"
          v-model:value="searchedText"
          allowClear
          @search="handleTextSearch" />
      </a-col>
    </a-row>
  </span>
</template>

<script>

export default {
  name: 'OsBasedImageSelectionSearchView',
  props: {
    filtersDisabled: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      filters: [
        'public',
        'featured'
      ],
      filterValues: undefined,
      searchedText: null,
      paramsFilter: {}
    }
  },
  created () {
  },
  methods: {
    handleTextSearch (value) {
      this.searchedText = value
      if (value) {
        this.paramsFilter.keyword = value
      } else {
        delete this.paramsFilter.keyword
      }
      const params = this.paramsFilter
      if (this.filtersDisabled) {
        delete this.paramsFilter.public
        delete this.paramsFilter.featured
      }
      this.$emit('search', params)
    },
    handleFilterChange () {
      this.paramsFilter = {}
      if (Array.isArray(this.filterValues)) {
        this.filterValues.forEach(e => {
          this.paramsFilter[e] = true
        })
      }
      if (this.searchedText) {
        this.paramsFilter.keyword = this.searchedText
      }
      this.$emit('search', this.paramsFilter)
    }
  }
}
</script>

<style lang="less" scoped>
.filter-group .ant-select,
.filter-group .ant-input-search {
  margin-right: 12px;
}

.filter-select {
  width: 200px;
}
</style>
