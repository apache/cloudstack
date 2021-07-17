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
  <div id="userLayout" :class="['user-layout', device]">
    <div class="user-layout-container">
      <div class="user-layout-header">
        <img
          v-if="$config.banner"
          :style="{
            width: $config.theme['@banner-width'],
            height: $config.theme['@banner-height']
          }"
          :src="$config.banner"
          class="user-layout-logo"
          alt="logo">
      </div>
      <route-view></route-view>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import RouteView from '@/layouts/RouteView'
import { mixinDevice } from '@/utils/mixin.js'
import { DARK_MODE } from '@/store/mutation-types'

export default {
  name: 'UserLayout',
  components: { RouteView },
  mixins: [mixinDevice],
  data () {
    return {}
  },
  watch: {
    '$store.getters.darkMode' (darkMode) {
      if (darkMode) {
        document.body.classList.add('dark-mode')
      } else {
        document.body.classList.remove('dark-mode')
      }
    }
  },
  mounted () {
    document.body.classList.add('userLayout')
    const darkMode = Vue.ls.get(DARK_MODE, false)
    if (this.$store.getters.darkMode || darkMode) {
      document.body.classList.add('dark-mode')
    }
  },
  beforeDestroy () {
    document.body.classList.remove('userLayout')
    document.body.classList.remove('dark-mode')
  }
}
</script>

<style lang="less" scoped>
.user-layout {
  height: 100%;

  &-container {
    padding: 3rem 0;
    width: 100%;

    @media (min-height:600px) {
      padding: 0;
      position: relative;
      top: 50%;
      transform: translateY(-50%);
      margin-top: -50px;
    }
  }

  &-logo {
    border-style: none;
    margin: 0 auto 2rem;
    display: block;

    .mobile & {
      max-width: 300px;
      margin-bottom: 1rem;
    }
  }
}
</style>
