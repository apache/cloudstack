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
  <global-layout>
    <multi-tab v-if="$store.getters.multiTab"></multi-tab>
    <transition name="page-transition">
      <route-view />
    </transition>
  </global-layout>
</template>

<script>
import RouteView from '@/layouts/RouteView'
import MultiTab from '@/components/multitab'
import GlobalLayout from '@/components/page/GlobalLayout'

export default {
  name: 'BasicLayout',
  components: {
    RouteView,
    MultiTab,
    GlobalLayout
  },
  data () {
    return {}
  }
}
</script>

<style lang="less">
  @import url('../style/index.less');

  /*
 * The following styles are auto-applied to elements with
 * transition="page-transition" when their visibility is toggled
 * by Vue.js.
 *
 * You can easily play with the page transition by editing
 * these styles.
 */

  .page-transition-enter {
    opacity: 0;
  }

  .page-transition-leave-active {
    opacity: 0;
  }

  .page-transition-enter .page-transition-container,
  .page-transition-leave-active .page-transition-container {
    -webkit-transform: scale(1.1);
    transform: scale(1.1);
  }
</style>
