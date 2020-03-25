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
  <a-form-item :label="this.$t('diskSize')">
    <a-row>
      <a-col :span="10">
        <a-slider
          :min="0"
          :max="1024"
          v-model="inputValue"
          @change="($event) => updateDickSize($event)"
        />
      </a-col>
      <a-col :span="4">
        <a-input-number
          v-model="inputValue"
          :formatter="value => `${value} GB`"
          :parser="value => value.replace(' GB', '')"
        />
      </a-col>
    </a-row>
  </a-form-item>
</template>

<script>
export default {
  name: 'DiskSizeSelection',
  props: {
    inputDecorator: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      inputValue: 0
    }
  },
  methods: {
    updateDickSize (value) {
      this.$emit('update-disk-size', this.inputDecorator, value)
    }
  }
}
</script>

<style scoped>

</style>
