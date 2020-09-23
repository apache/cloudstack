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
  <div class="logo">
    <img
      v-if="$config.logo"
      :style="{
        width: $config.theme['@logo-width'],
        height: $config.theme['@logo-height']
      }"
      :src="$config.logo"
      class="logo-image" />
  </div>
</template>

<script>

export default {
  name: 'Logo',
  components: {
  },
  props: {
    title: {
      type: String,
      default: 'CloudStack',
      required: false
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false
    }
  }
}
</script>

<style type="less" scoped>
.logo {
  height: 64px;
  position: relative;
  line-height: 64px;
  -webkit-transition: all .3s;
  transition: all .3s;
  overflow: hidden;
}

.sider.light .logo {
  box-shadow: 1px 1px 0px 0px #e8e8e8;
}

.logo-image {
  display: inline-block;
  vertical-align: middle;
}

</style>
