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
  <a-tooltip arrowPointAtCenter placement="bottomRight">
    <template slot="title" v-if="tooltip">
      {{ tooltip }}
    </template>
    <a-button
      shape="circle"
      size="default"
      :type="type"
      :disabled="disabled"
      :icon="icon"
      :class="buttonClass"
      @click="handleClicked()" >
      <a-icon
        v-if="iconType && iconTwoToneColor"
        :type="iconType"
        theme="twoTone"
        :twoToneColor="iconTwoToneColor" />
    </a-button>
  </a-tooltip>
</template>

<script>

export default {
  name: 'TooltipButton',
  props: {
    tooltip: {
      type: String,
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    },
    type: {
      type: String,
      default: null
    },
    icon: {
      type: String,
      default: null
    },
    iconType: {
      type: String,
      default: null
    },
    iconTwoToneColor: {
      type: String,
      default: null
    },
    buttonClass: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
    }
  },
  methods: {
    handleClicked () {
      this.$emit('click')
    }
  }
}
</script>

<style scoped lang="scss">
</style>
