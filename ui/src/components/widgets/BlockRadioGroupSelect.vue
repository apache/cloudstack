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
    <a-radio-group
      v-if="items.length <= maxBlocks"
      v-model:value="localValue"
      @change="handleChange()">
      <a-row type="flex" :gutter="[horizontalGutter, verticalGutter]" justify="start">
        <div v-for="item in items" :key="item.id">
          <a-col :span="6">
            <a-radio-button
              :value="item.id"
              style="border-width: 2px"
              :class="blockRadioButtonClass">
              <slot name="radio-option" :item="item"></slot>
            </a-radio-button>
          </a-col>
        </div>
      </a-row>
    </a-radio-group>
    <a-select
      v-else
      v-model:value="localValue"
      showSearch
      optionFilterProp="label"
      :filterOption="(input, option) => {
        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
      }"
      @change="handleChange()"
      :loading="loading"
      v-focus="true">
      <a-select-option v-for="item in items" :key="item.id" :label="item.name">
        <slot name="select-option" :item="item"></slot>
      </a-select-option>
    </a-select>
  </div>
</template>

<script>

export default {
  name: 'BlockRadioGroupSelect',
  props: {
    blockSize: {
      type: String,
      default: 'large'
    },
    selectedValue: {
      type: [String, Number],
      default: ''
    },
    horizontalGutter: {
      type: Number,
      default: 16
    },
    verticalGutter: {
      type: Number,
      default: 18
    },
    items: {
      type: Array,
      default: () => []
    },
    maxBlocks: {
      type: Number,
      default: 8
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      localValue: this.selectedValue
    }
  },
  watch: {
    selectedValue (newValue) {
      if (this.localValue === newValue) {
        return
      }
      this.localValue = newValue
    }
  },
  computed: {
    blockRadioButtonClass () {
      if (['square', 'medium', 'small'].includes(this.blockSize)) {
        return this.blockSize + '-block-radio-button'
      }
      return 'large-block-radio-button'
    }
  },
  emits: ['change'],
  methods: {
    handleChange () {
      this.$emit('change', this.localValue)
    }
  }
}
</script>

<style lang="less" scoped>
  .large-block-radio-button {
    width:100%;
    min-width: 345px;
    height: 60px;
    display: flex;
    padding-left: 20px;
    align-items: center;
  }
  .medium-block-radio-button {
    width:100%;
    min-width: 160px;
    height: 60px;
    display: flex;
    padding-left: 20px;
    align-items: center;
  }
  .small-block-radio-button {
    width:100%;
    min-width: 80px;
    height: 60px;
    display: flex;
    padding-left: 20px;
    align-items: center;
  }
  .square-block-radio-button {
    width: 88px;
    height: 88px;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
    padding: 0;
    box-sizing: border-box;
  }
</style>
