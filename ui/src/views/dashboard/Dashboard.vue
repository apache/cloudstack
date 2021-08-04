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
    <div v-if="showOnboarding">
      <onboarding-dashboard />
    </div>
    <div v-else-if="$store.getters.userInfo.roletype === 'Admin' && !project">
      <capacity-dashboard/>
    </div>
    <div v-else>
      <usage-dashboard :resource="$store.getters.project" :showProject="project" />
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import store from '@/store'
import CapacityDashboard from './CapacityDashboard'
import UsageDashboard from './UsageDashboard'
import OnboardingDashboard from './OnboardingDashboard'

export default {
  name: 'Dashboard',
  components: {
    CapacityDashboard,
    UsageDashboard,
    OnboardingDashboard
  },
  provide: function () {
    return {
      parentFetchData: this.fetchData
    }
  },
  data () {
    return {
      showCapacityDashboard: false,
      project: false,
      showOnboarding: false
    }
  },
  created () {
    this.fetchData()
  },
  mounted () {
    this.showCapacityDashboard = Object.prototype.hasOwnProperty.call(store.getters.apis, 'listCapacity')
    this.project = false
    if (store.getters.project && store.getters.project.id) {
      this.project = true
    }
    this.$store.watch(
      (state, getters) => getters.project,
      (newValue, oldValue) => {
        if (newValue && newValue.id) {
          this.project = true
        } else {
          this.project = false
        }
      }
    )
  },
  methods: {
    fetchData () {
      if (!['Admin'].includes(this.$store.getters.userInfo.roletype)) {
        return
      }
      api('listZones').then(json => {
        this.showOnboarding = json.listzonesresponse.count ? json.listzonesresponse.count === 0 : true
      })
    }
  }
}
</script>
