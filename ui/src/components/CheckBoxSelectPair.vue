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
      <a-col :md="24" :lg="layout === 'horizontal' ? 12 : 24">
        <a-checkbox
          v-model:checked="fields[checkBoxDecorator]"
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
            v-model:value="fields[selectDecorator]"
            :defaultValue="selectDecorator ? undefined : selectedOption ? selectedOption : getSelectInitialValue()"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @change="val => { this.handleSelectChange(val) }">
            <a-select-option
              v-for="(opt) in selectSource"
              :key="opt.id"
              :disabled="opt.enabled === false">
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
    checkBoxDecorator: {
      type: String,
      default: ''
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
    selectDecorator: {
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
      fields: {}
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
  created () {
    this.fields = {}
    this.fields[this.checkBoxDecorator] = false
    this.fields[this.selectDecorator] = this.selectedOption ? this.selectedOption : this.getSelectInitialValue()
    this.checked = this.defaultCheckBoxValue
  },
  methods: {
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    getSelectInitialValue () {
      const initialValue = this.selectSource?.filter(x => x.enabled !== false)?.[0]?.id || ''
      this.handleSelectChange(initialValue)
      return initialValue
    },
    handleCheckChange (e) {
      this.checked = e.target.checked
      this.$emit('handle-checkselectpair-change', this.resourceKey, this.checked, this.selectedOption)
    },
    handleSelectChange (val) {
      this.selectedOption = val
      this.$emit('handle-checkselectpair-change', this.resourceKey, this.checked, this.selectedOption)
    }
  }
}
</script>
