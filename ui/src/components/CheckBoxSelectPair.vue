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
  <div>
    <a-checkbox v-decorator="[checkBoxDecorator, {}]" class="pair-checkbox" @change="handleCheckChange">
      {{ checkBoxLabel }}
    </a-checkbox>
    <a-form-item class="pair-select-container" :label="selectLabel" v-if="this.checked">
      <a-select
        v-decorator="[selectDecorator, {
          initialValue: selectedOption ? selectedOption : this.getSelectInitialValue()
        }]"
        showSearch
        optionFilterProp="children"
        :filterOption="(input, option) => {
          return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }"
        @change="val => { this.handleSelectChange(val) }">
        <a-select-option v-for="(opt) in selectOptions" :key="opt.name" :disabled="opt.enabled === false">
          {{ opt.name || opt.description }}
        </a-select-option>
      </a-select>
    </a-form-item>
  </div>
</template>

<script>

export default {
  name: 'CheckBoxSelectPair',
  props: {
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
    }
  },
  data () {
    return {
      checked: false,
      selectedOption: ''
    }
  },
  methods: {
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    getSelectInitialValue () {
      const provider = this.selectOptions?.filter(x => x.enabled)?.[0]?.name || ''
      this.handleSelectChange(provider)
      return provider
    },
    handleCheckChange (e) {
      this.checked = e.target.checked
      this.$emit('handle-checkpair-change', this.resourceKey, this.checked, '')
    },
    handleSelectChange (val) {
      this.selectedOption = val
      this.$emit('handle-checkpair-change', this.resourceKey, this.checked, val)
    }
  }
}
</script>

<style scoped lang="scss">
  .pair-checkbox {
    width: 180px;
  }
  .pair-select-container {
    position: relative;
    float: right;
    margin-bottom: -5px;
    width: 20vw;
  }
</style>
