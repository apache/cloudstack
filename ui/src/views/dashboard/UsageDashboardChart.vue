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
    <a-col
      class="usage-dashboard-chart-tile"
      :xs="12"
      :md="8"
      v-for="stat in stats"
      :key="stat.type">
      <a-card
        class="usage-dashboard-chart-card"
        :bordered="false"
        :loading="loading"
        :style="stat.bgcolor ? { 'background-color': stat.bgcolor } : {}">
        <router-link :to="{ path: stat.path }">
          <div
            class="usage-dashboard-chart-card-inner">
            <h3>{{ stat.name }}</h3>
            <h2>
              <a-icon :type="stat.icon" />
              {{ stat.count == undefined ? 0 : stat.count }}
            </h2>
          </div>
        </router-link>
      </a-card>
    </a-col>
  </div>
</template>

<script>
import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'UsageDashboardChart',
  components: {
    ChartCard
  },
  props: {
    stats: {
      type: Array,
      default () {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: false
    }
  }
}
</script>
