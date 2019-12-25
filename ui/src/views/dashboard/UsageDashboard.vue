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
  <a-row class="usage-dashboard" :gutter="12">
    <a-col :xl="16">
      <a-row>
        <a-card>
          <a-tabs
            v-if="showProject"
            :animated="false"
            @change="onTabChange">
            <a-tab-pane
              v-for="tab in $route.meta.tabs"
              :tab="$t(tab.name)"
              :key="tab.name"
              v-if="'show' in tab ? tab.show(project, $route, $store.getters.userInfo) : true">
              <component
                :is="tab.component"
                :resource="project"
                :loading="loading"
                :bordered="false"
                :stats="stats" />
            </a-tab-pane>
          </a-tabs>
          <a-col
            v-else
            class="usage-dashboard-chart-tile"
            :xs="12"
            :md="8"
            v-for="stat in stats"
            :key="stat.type">
            <chart-card class="usage-dashboard-chart-card" :loading="loading">
              <router-link :to="{ name: stat.path }">
                <div class="usage-dashboard-chart-card-inner">
                  <h4>{{ stat.name }}</h4>
                  <h1>{{ stat.count == undefined ? 0 : stat.count }}</h1>
                </div>
              </router-link>
            </chart-card>
          </a-col>
        </a-card>
      </a-row>
    </a-col>
    <a-col
      :xl="8">
      <chart-card>
        <div class="usage-dashboard-chart-card-inner">
          <a-button><router-link :to="{ name: 'event' }">View Events</router-link></a-button>
        </div>
        <template slot="footer">
          <div class="usage-dashboard-chart-footer">
            <a-timeline>
              <a-timeline-item
                v-for="event in events"
                :key="event.id"
                :color="getEventColour(event)">
                <span :style="{ color: '#999' }"><small>{{ event.created }}</small></span><br/>
                <span :style="{ color: '#666' }"><small>{{ event.type }}</small></span><br/>
                <span :style="{ color: '#aaa' }">({{ event.username }}) {{ event.description }}</span>
              </a-timeline-item>
            </a-timeline>
          </div>
        </template>
      </chart-card>
    </a-col>
  </a-row>
</template>

<script>
import { api } from '@/api'
import store from '@/store'

import ChartCard from '@/components/widgets/ChartCard'
import UsageDashboardChart from '@/views/dashboard/UsageDashboardChart'

export default {
  name: 'UsageDashboard',
  components: {
    ChartCard,
    UsageDashboardChart
  },
  props: {
    resource: {
      type: Object,
      default () {
        return []
      }
    },
    showProject: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      loading: false,
      showAction: false,
      showAddAccount: false,
      events: [],
      stats: [],
      project: {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.project = store.getters.project
    this.fetchData()
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'dashboard') {
        this.fetchData()
      }
    },
    resource (newData, oldData) {
      this.project = newData
    }
  },
  methods: {
    fetchData () {
      this.stats = [{}, {}, {}, {}, {}, {}]
      api('listVirtualMachines', { state: 'Running', listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.splice(0, 1, { name: 'Running VMs', count: count, path: 'vm' })
      })
      api('listVirtualMachines', { state: 'Stopped', listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.splice(1, 1, { name: 'Stopped VMs', count: count, path: 'vm' })
      })
      api('listVirtualMachines', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.splice(2, 1, { name: 'Total VMs', count: count, path: 'vm' })
      })
      api('listVolumes', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvolumesresponse) {
          count = json.listvolumesresponse.count
        }
        this.stats.splice(3, 1, { name: 'Total Volumes', count: count, path: 'volume' })
      })
      api('listNetworks', { listall: true }).then(json => {
        var count = 0
        if (json && json.listnetworksresponse) {
          count = json.listnetworksresponse.count
        }
        this.stats.splice(4, 1, { name: 'Total Networks', count: count, path: 'guestnetwork' })
      })
      api('listPublicIpAddresses', { listall: true }).then(json => {
        var count = 0
        if (json && json.listpublicipaddressesresponse) {
          count = json.listpublicipaddressesresponse.count
        }
        this.stats.splice(5, 1, { name: 'Public IP Addresses', count: count, path: 'publicip' })
      })
      this.listEvents()
    },
    listEvents () {
      const params = {
        page: 1,
        pagesize: 6,
        listall: true
      }
      this.loading = true
      api('listEvents', params).then(json => {
        this.events = []
        this.loading = false
        if (json && json.listeventsresponse && json.listeventsresponse.event) {
          this.events = json.listeventsresponse.event
        }
      })
    },
    getEventColour (event) {
      if (event.level === 'ERROR') {
        return 'red'
      }
      if (event.state === 'Completed') {
        return 'green'
      }
      return 'blue'
    },
    onTabChange (key) {
      this.showAddAccount = false

      if (key !== 'Dashboard') {
        this.showAddAccount = true
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .usage-dashboard {

    &-chart-tile {
      margin-bottom: 12px;
    }

    &-chart-card {
      padding-top: 24px;
    }

    &-chart-card-inner {
      text-align: center;
    }

    &-footer {
       padding-top: 12px;
       padding-left: 3px;
       white-space: normal;
    }
  }
</style>
