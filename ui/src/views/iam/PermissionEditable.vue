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
    v-model:value="value"
    @change="handleChange"
    v-focus="true"
    showSearch
    optionFilterProp="label"
    :filterOption="(input, option) => {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }" >
    <a-select-option value="allow" :label="$t('label.allow')">{{ $t('label.allow') }}</a-select-option>
    <a-select-option value="deny" :label="$t('label.deny')">{{ $t('label.deny') }}</a-select-option>
  </a-select>
</template>

<script>
export default {
  props: {
    defaultValue: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      value: ''
    }
  },
  watch: {
    defaultValue () {
      this.value = this.defaultValue
    }
  },
  created () {
    this.value = this.defaultValue
  },
  methods: {
    handleChange (e) {
      this.$emit('onChange', e)
    }
  }
}
</script>
