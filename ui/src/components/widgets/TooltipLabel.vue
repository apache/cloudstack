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
  <span>
    {{ title }}
    <a-tooltip v-if="tooltip" :title="tooltip" :placement="tooltipPlacement">
      <info-circle-outlined class="tooltip-icon" />
    </a-tooltip>
  </span>
</template>

<script>

export default {
  name: 'TooltipLabel',
  props: {
    title: {
      type: String,
      default: ''
    },
    tooltip: {
      type: String,
      default: null
    },
    tooltipPlacement: {
      type: String,
      default: 'top'
    }
  }
}
</script>

<style scoped lang="scss">
  .tooltip-icon {
    color: rgba(0,0,0,.45);
  }
</style>
