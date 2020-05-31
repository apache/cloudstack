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
  <div class="page-header-index-wide page-header-wrapper-grid-content-main">
    <a-row :gutter="12">
      <a-col :md="24" :lg="device === 'desktop' ? 7 : 8" style="margin-bottom: 12px">
        <slot name="left">
        </slot>
      </a-col>
      <a-col :md="24" :lg="device === 'desktop' ? 17 : 16">
        <slot name="right">
        </slot>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { mixinDevice } from '@/utils/mixin.js'

export default {
  name: 'ResourceLayout',
  mixins: [mixinDevice]
}
</script>

<style lang="less" scoped>
.page-header-wrapper-grid-content-main {
  width: 100%;
  height: 100%;
  min-height: 100%;
  transition: 0.3s;
}
</style>
