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
  <div v-decorator="['service.' + resourceTitle, {}]">
    <a-checkbox class="pair-checkbox" @change="handleCheckChange">
      {{ resourceTitle }}
    </a-checkbox>
    <a-form-item class="pair-select-container" :label="$t('label.provider')" v-if="this.checked">
      <a-select
        v-decorator="[resourceTitle + '.provider', {
          initialValue: resourceOptions[0].name
        }]"
        showSearch
        optionFilterProp="children"
        :filterOption="(input, option) => {
          return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }">
        <a-select-option v-for="(opt) in resourceOptions" :key="opt.name" :disabled="!opt.enabled">
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
    resourceTitle: {
      type: String,
      required: true
    },
    resourceOptions: {
      type: Array,
      required: true
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
    handleCheckChange (e) {
      this.checked = e.target.checked
      if (this.checked && this.arrayHasItems(this.resourceOptions)) {
        this.selectedOption = this.resourceOptions[0].name
      }
      this.$emit('handle-check-change', this.resourceKey, this.checked)
    }
  }
}
</script>

<style scoped lang="scss">
  .pair-checkbox {
    width: 20vw;
  }
  .pair-select-container {
    position: relative;
    float: right;
    margin-left: 5vw;
    margin-bottom: 0;
    width: 25vw
  }
</style>
