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
    <a-input-search
      v-if="filtersDisabled"
      class="input-search"
      :placeholder="$t('label.search')"
      v-model:value="searchQuery"
      allowClear
      @search="handleImageSearch" />
    <a-input-search
      v-else
      class="input-search"
      :placeholder="$t('label.search')"
      v-model:value="searchQuery"
      allowClear
      @search="handleImageSearch">
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
              <a-form-item ref="public" name="public" :key="0" :label="$t('label.ispublic')">
                <a-checkbox v-model:checked="form.public">
                  {{ $t('label.show.public.only') }}
                </a-checkbox>
              </a-form-item>
              <a-form-item ref="featured" name="featured" :key="0" :label="$t('label.isfeatured')">
                <a-checkbox v-model:checked="form.featured">
                  {{ $t('label.show.featured.only') }}
                </a-checkbox>
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
            size="small">
            <filter-two-tone v-if="isFiltered" />
            <filter-outlined v-else />
          </a-button>
        </a-popover>
      </template>
    </a-input-search>
  </span>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'

export default {
  name: 'OsBasedImageSelectionSearchView',
  components: {
  },
  props: {
    filtersDisabled: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      visibleFilter: false,
      searchQuery: null,
      isFiltered: false,
      paramsFilter: {}
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
  },
  methods: {
    handleImageSearch (value) {
      this.searchQuery = value
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
        if (this.searchQuery) {
          this.paramsFilter.keyword = this.searchQuery
        }
        this.$emit('search', this.paramsFilter)
      })
    },
    onClear () {
      this.formRef.value.resetFields()
      this.form = reactive({})
      this.isFiltered = false
      this.paramsFilter = {}
      if (this.searchQuery) {
        this.paramsFilter.keyword = this.searchQuery
      }
      this.$emit('search', this.paramsFilter)
    }
  }
}
</script>

<style lang="less" scoped>

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
