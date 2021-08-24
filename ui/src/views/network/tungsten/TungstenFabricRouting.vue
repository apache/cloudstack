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
    <a-tabs tab-position="left">
      <a-tab-pane
        v-for="tab in tabs"
        :key="tab.name"
        :tab="$t('label.' + tab.name)">
        <component :is="tab.component" :resource="resource" :loading="loading" :tab="tab.name" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
export default {
  name: 'TungstenFabricRouting',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      tabs: [
        {
          name: 'network.route.table',
          component: () => import('@/views/network/tungsten/TungstenNetworkRouteTable.vue')
        },
        {
          name: 'interface.route.table',
          component: () => import('@/views/network/tungsten/TungstenInterfaceRouteTable.vue')
        },
        {
          name: 'logical.router',
          component: () => import('@/views/network/tungsten/TungstenLogicalRoute.vue')
        }
      ]
    }
  }
}
</script>

<style scoped>
</style>
