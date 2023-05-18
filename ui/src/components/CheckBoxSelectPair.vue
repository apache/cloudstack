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
  <div style="width: 100%">
    <a-row :gutter="6">
      <a-col :md="24" :lg="layout === 'horizontal' ? 10 : 24">
        <a-checkbox
          :checked="checked"
          @change="handleCheckChange">
          {{ checkBoxLabel }}
        </a-checkbox>
      </a-col>
      <a-col :md="24" :lg="layout === 'horizontal' ? 12 : 24">
        <a-form-item
          v-if="reversed !== checked"
          :label="selectLabel">
          <a-select
            v-model:value="selectedOption"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @change="val => { handleSelectChange(val) }">
            <a-select-option
              v-for="(opt) in selectSource"
              :key="opt.id"
              :disabled="opt.enabled === false"
              :label="opt.displaytext || opt.name || opt.description">
              {{ opt.displaytext || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
  </div>
</template>

<script>

export default {
  name: 'CheckBoxSelectPair',
  props: {
    layout: {
      type: String,
      default: 'horizontal'
    },
    resourceKey: {
      type: String,
      required: true
    },
    checkBoxLabel: {
      type: String,
      required: true
    },
    defaultCheckBoxValue: {
      type: Boolean,
      default: false
    },
    selectOptions: {
      type: Array,
      required: true
    },
    selectLabel: {
      type: String,
      default: ''
    },
    reversed: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      checked: false,
      selectedOption: null,
      selectOptionsTimer: null
    }
  },
  created () {
    this.checked = this.defaultCheckBoxValue
  },
  watch: {
    selectOptions () {
      clearTimeout(this.selectOptionsTimer)
      this.selectOptionsTimer = setTimeout(() => {
        this.handleSelectOptionsUpdated()
      }, 50)
    }
  },
  computed: {
    selectSource () {
      return this.selectOptions.map(item => {
        var option = { ...item }
        if (!('id' in option)) {
          option.id = option.name
        }
        return option
      })
    }
  },
  methods: {
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    handleCheckChange (e) {
      this.checked = e.target.checked
      if (this.checked && !this.selectedOption) {
        this.selectedOption = this.selectSource?.filter(x => x.enabled !== false)?.[0]?.id || null
      }
      this.$emit('handle-checkselectpair-change', this.resourceKey, this.checked, this.selectedOption)
    },
    handleSelectChange (val) {
      this.selectedOption = val
      this.$emit('handle-checkselectpair-change', this.resourceKey, this.checked, this.selectedOption)
    },
    handleSelectOptionsUpdated () {
      if (!this.checked) return
      var enabledOptions = this.selectSource?.filter(x => x.enabled !== false) || []
      if (this.selectedOption && !enabledOptions.includes(this.selectedOption)) {
        this.handleSelectChange(enabledOptions[0]?.id || null)
      }
    }
  }
}
</script>

<style lang="less" scoped>
.ant-list-split .ant-list-item div {
  width: 100%;
}
</style>
