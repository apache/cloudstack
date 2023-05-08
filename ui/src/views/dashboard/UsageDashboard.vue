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
    <a-col :xl="16" style="padding-left: 0; padding-right: 0;">
      <a-row>
        <a-card style="width: 100%">
          <a-tabs
            v-if="showProject"
            :animated="false"
            @change="onTabChange">
            <template v-for="tab in $route.meta.tabs" :key="tab.name">
              <a-tab-pane
                v-if="'show' in tab ? tab.show(project, $route, $store.getters.userInfo) : true"
                :tab="$t('label.' + tab.name)"
                :key="tab.name">
                <keep-alive>
                  <component
                    :is="tab.component"
                    :resource="project"
                    :loading="loading"
                    :bordered="false"
                    :stats="stats" />
                </keep-alive>
              </a-tab-pane>
            </template>
          </a-tabs>
          <a-row :gutter="24" v-else>
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
                :style="stat.bgcolor ? { 'background': stat.bgcolor } : {}">
                <router-link v-if="stat.path" :to="{ path: stat.path, query: stat.query }">
                  <div
                    class="usage-dashboard-chart-card-inner">
                    <h3>{{ stat.name }}</h3>
                    <h2>
                      <render-icon :icon="stat.icon" />
                      {{ stat.count == undefined ? 0 : stat.count }}
                    </h2>
                  </div>
                </router-link>
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-row>
    </a-col>
    <a-col :xl="8">
      <chart-card :loading="loading" >
        <div class="usage-dashboard-chart-card-inner">
          <a-button>
            <router-link :to="{ name: 'event' }">
              {{ $t('label.view') + ' ' + $t('label.events') }}
            </router-link>
          </a-button>
        </div>
        <template #footer>
          <div class="usage-dashboard-chart-footer">
            <a-timeline>
              <a-timeline-item
                v-for="event in events"
                :key="event.id"
                :color="getEventColour(event)">
                <span :style="{ color: '#999' }"><small>{{ $toLocaleDate(event.created) }}</small></span><br/>
                <span :style="{ color: '#666' }"><small><router-link :to="{ path: '/event/' + event.id }">{{ event.type }}</router-link></small></span><br/>
                <resource-label :resourceType="event.resourcetype" :resourceId="event.resourceid" :resourceName="event.resourcename" />
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
import ResourceLabel from '@/components/widgets/ResourceLabel'

export default {
  name: 'UsageDashboard',
  components: {
    ChartCard,
    UsageDashboardChart,
    ResourceLabel
  },
  props: {
    resource: {
      type: Object,
      default () {
        return {}
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
  created () {
    this.project = store.getters.project
    this.fetchData()
    this.$store.watch(
      (state, getters) => getters.project,
      (newValue, oldValue) => {
        if (newValue && newValue.id && (!oldValue || newValue.id !== oldValue.id)) {
          this.fetchData()
        } else if (store.getters.userInfo.roletype !== 'Admin') {
          this.fetchData()
        }
      }
    )
  },
  watch: {
    '$route' (to) {
      if (to.name === 'dashboard') {
        this.fetchData()
      }
    },
    resource: {
      deep: true,
      handler (newData, oldData) {
        this.project = newData
      }
    },
    '$i18n.global.locale' (to, from) {
      if (to !== from) {
        this.fetchData()
      }
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
        var tileColor = this.$config.theme['@dashboard-tile-runningvms-bg'] || '#dfe9cc'
        this.stats.splice(0, 1, { name: this.$t('label.running.vms'), count: count, icon: 'desktop-outlined', bgcolor: tileColor, path: '/vm', query: { state: 'running', filter: 'running' } })
      })
      api('listVirtualMachines', { state: 'Stopped', listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-stoppedvms-bg'] || '#edcbce'
        this.stats.splice(1, 1, { name: this.$t('label.stopped.vms'), count: count, icon: 'poweroff-outlined', bgcolor: tileColor, path: '/vm', query: { state: 'stopped', filter: 'stopped' } })
      })
      api('listVirtualMachines', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalvms-bg'] || '#ffffff'
        this.stats.splice(2, 1, { name: this.$t('label.total.vms'), count: count, icon: 'number-outlined', bgcolor: tileColor, path: '/vm' })
      })
      api('listVolumes', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvolumesresponse) {
          count = json.listvolumesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalvolumes-bg'] || '#ffffff'
        this.stats.splice(3, 1, { name: this.$t('label.total.volume'), count: count, icon: 'database-outlined', bgcolor: tileColor, path: '/volume' })
      })
      api('listNetworks', { listall: true }).then(json => {
        var count = 0
        if (json && json.listnetworksresponse) {
          count = json.listnetworksresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalnetworks-bg'] || '#ffffff'
        this.stats.splice(4, 1, { name: this.$t('label.total.network'), count: count, icon: 'apartment-outlined', bgcolor: tileColor, path: '/guestnetwork' })
      })
      api('listPublicIpAddresses', { listall: true }).then(json => {
        var count = 0
        if (json && json.listpublicipaddressesresponse) {
          count = json.listpublicipaddressesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalips-bg'] || '#ffffff'
        this.stats.splice(5, 1, { name: this.$t('label.public.ip.addresses'), count: count, icon: 'environment-outlined', bgcolor: tileColor, path: '/publicip' })
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
  :deep(.usage-dashboard) {

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

  @media (max-width: 1200px) {
    .ant-col-xl-8 {
      width: 100%;
    }
  }
</style>
