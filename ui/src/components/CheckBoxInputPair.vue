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
  <div v-if="visible" style="width: 100%">
    <a-col>
      <a-row :md="24" :lg="layout === 'horizontal' ? 12 : 24">
        <a-checkbox
          v-model:value="fields[checkBoxDecorator]"
          :checked="checked"
          @change="handleCheckChange">
          {{ checkBoxLabel }}
        </a-checkbox>
      </a-row>
      <a-row :md="24" :lg="layout === 'horizontal' ? 12 : 24">
        <a-form-item
          :label="inputLabel"
          v-if="reversed !== checked">
          <a-input
            v-model:value="fields[inputDecorator]"
            @change="val => handleInputChangeTimed(val)" />
        </a-form-item>
      </a-row>
    </a-col>
  </div>
</template>

<script>

export default {
  name: 'CheckBoxInputPair',
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
    defaultInputValue: {
      type: String,
      default: ''
    },
    inputLabel: {
      type: String,
      default: ''
    },
    inputDecorator: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: true
    },
    reversed: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      checked: false,
      inputValue: '',
      inputUpdateTimer: null,
      fields: {}
    }
  },
  created () {
    this.checked = this.defaultCheckBoxValue
    this.fields[this.checkBoxDecorator] = this.checked
    this.fields[this.inputDecorator] = this.defaultInputValue
  },
  methods: {
    handleCheckChange (e) {
      this.checked = e.target.checked
      this.$emit('handle-checkinputpair-change', this.resourceKey, this.checked, this.inputValue)
    },
    handleInputChange (e) {
      this.inputValue = e.target.value
      this.$emit('handle-checkinputpair-change', this.resourceKey, this.checked, this.inputValue)
    },
    handleInputChangeTimed (e) {
      clearTimeout(this.inputUpdateTimer)
      this.inputUpdateTimer = setTimeout(() => {
        this.handleInputChange(e)
      }, 500)
    }
  }
}
</script>
