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
  <div class="page-header-index-wide">
    <div v-if="showCapacityDashboard && !project">
      <capacity-dashboard/>
    </div>
    <div v-else>
      <usage-dashboard/>
    </div>
  </div>
</template>

<script>
import store from '@/store'
import CapacityDashboard from './CapacityDashboard'
import UsageDashboard from './UsageDashboard'

export default {
  name: 'Dashboard',
  components: {
    CapacityDashboard,
    UsageDashboard
  },
  data () {
    return {
      showCapacityDashboard: false,
      project: false
    }
  },
  mounted () {
    this.showCapacityDashboard = Object.prototype.hasOwnProperty.call(store.getters.apis, 'listCapacity')
    this.project = store.getters.project !== undefined && store.getters.project.id !== undefined
    this.$store.watch(
      (state, getters) => getters.project,
      (newValue, oldValue) => {
        if (newValue === undefined || newValue.id === undefined) {
          this.project = false
        } else {
          this.project = true
        }
      }
    )
  }
}
</script>
