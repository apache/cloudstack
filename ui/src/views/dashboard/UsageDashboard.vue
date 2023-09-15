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
  <a-row class="capacity-dashboard" :gutter="[12,12]">
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <template #title>
          <div class="center">
            <h3><cloud-outlined /> {{ $t('label.compute') }}</h3>
          </div>
        </template>
        <div>
          <div
            v-for="usageType in ['vm', 'cpu', 'memory']"
            :key="usageType">
            <div>
              <div>
                <strong>
                  {{ $t('label.' + usageType + 'limit') }}
                </strong>
                {{ entity[usageType + 'total'] }} {{ $t('label.used') }}
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
              :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : entity[usageType + 'available'] }} {{ $t('label.available') }} |
                {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : entity[usageType + 'limit'] }} {{ $t('label.limit') }}
              </div>
            </div>
          </div>
        </div>
      </chart-card>
      <chart-card :loading="loading" style="margin-top: 12px">
        <template #title>
          <div class="center">
            <h3>
              <cloud-server-outlined />
              {{ $t('label.instances') }}
              <span style="float: right">
                <router-link :to="{ path: '/vm', query: { projectid: '-1' } }">
                  {{ instances.total }}
                </router-link>
              </span>
            </h3>
          </div>
        </template>
        <a-row>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { projectid: '-1', state: 'running' } }">
              <a-statistic :title="$t('label.running')" :value="instances.running">
                <template #prefix>
                  <status class="status" text="Running"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { projectid: '-1', state: 'stopped' } }">
              <a-statistic :title="$t('label.stopped')" :value="instances.stopped">
                <template #prefix>
                  <status class="status" text="Stopped"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
        </a-row>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <template #title>
          <div class="center">
            <h3><hdd-outlined /> {{ $t('label.storage') }}</h3>
          </div>
        </template>
        <div>
          <div
            v-for="usageType in ['volume', 'snapshot', 'template', 'primarystorage', 'secondarystorage']"
            :key="usageType">
            <div>
              <div>
                <strong>
                  {{ $t('label.' + usageType + 'limit') }}
                </strong>
                ({{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : entity[usageType + 'available'] }} {{ $t('label.available') }})
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
              :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ entity[usageType + 'total'] }} {{ $t('label.used') }} | {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : entity[usageType + 'limit'] }} {{ $t('label.limit') }}
              </div>
            </div>
          </div>
        </div>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <template #title>
          <div class="center">
            <h3><apartment-outlined /> {{ $t('label.network') }}</h3>
          </div>
        </template>
        <div>
          <div
            v-for="usageType in ['ip', 'network', 'vpc']"
            :key="usageType">
            <div>
              <div>
                <strong>
                  {{ $t('label.' + usageType + 'limit') }}
                </strong>
                ({{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : entity[usageType + 'available'] }} {{ $t('label.available') }})
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
              :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ entity[usageType + 'total'] }} {{ $t('label.used') }} | {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : entity[usageType + 'limit'] }} {{ $t('label.limit') }}
              </div>
            </div>
          </div>
        </div>
      </chart-card>
      <chart-card :loading="loading" style="margin-top: 12px" v-if="!project.id">
        <template #title>
          <div class="center">
            <h3><project-outlined /> {{ $t('label.projects') }}</h3>
          </div>
        </template>
        <div>
          <div
            v-for="usageType in ['project']"
            :key="usageType">
            <div>
              <div>
                <strong>
                  {{ $t('label.' + usageType + 'limit') }}
                </strong>
                ({{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : entity[usageType + 'available'] }} {{ $t('label.available') }})
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
              :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ entity[usageType + 'total'] }} {{ $t('label.used') }} | {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : entity[usageType + 'limit'] }} {{ $t('label.limit') }}
              </div>
            </div>
          </div>
        </div>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card dashboard-event">
      <a-card :loading="loading" :bordered="false" class="dashboard-event">
        <div class="center" style="margin-top: -8px">
          <h3>
            <schedule-outlined />
            {{ $t('label.events') }}
          </h3>
        </div>
        <a-timeline>
          <a-timeline-item
            v-for="event in events"
            :key="event.id"
            :color="getEventColour(event)">
            <span :style="{ color: '#999' }"><small>{{ $toLocaleDate(event.created) }}</small></span>&nbsp;
            <span :style="{ color: '#666' }"><small><router-link :to="{ path: '/event/' + event.id }">{{ event.type }}</router-link></small></span><br/>
            <span>
              <resource-label :resourceType="event.resourcetype" :resourceId="event.resourceid" :resourceName="event.resourcename" />
            </span>
            <span :style="{ color: '#aaa' }">({{ event.username }}) {{ event.description }}</span>
          </a-timeline-item>
        </a-timeline>
        <router-link :to="{ path: '/event' }">
          <a-button>
            {{ $t('label.view') }} {{ $t('label.events') }}
          </a-button>
        </router-link>
      </a-card>
    </a-col>
  </a-row>
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
      project: {},
      entity: {},
      instances: {
        total: 0,
        running: 0,
        stopped: 0
      }
    }
  },
  created () {
    this.project = store.getters.project
    this.listAccount()
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
        if (!this.project.id) {
          this.listAccount()
        } else {
          this.entity = this.project
        }
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
      api('listVirtualMachines', { state: 'Running', listall: true, retrieveonlyresourcecount: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-runningvms-bg'] || '#dfe9cc'
        this.stats.splice(0, 1, { name: this.$t('label.running.vms'), count: count, icon: 'desktop-outlined', bgcolor: tileColor, path: '/vm', query: { state: 'running', filter: 'running' } })
      })
      api('listVirtualMachines', { state: 'Stopped', listall: true, retrieveonlyresourcecount: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-stoppedvms-bg'] || '#edcbce'
        this.stats.splice(1, 1, { name: this.$t('label.stopped.vms'), count: count, icon: 'poweroff-outlined', bgcolor: tileColor, path: '/vm', query: { state: 'stopped', filter: 'stopped' } })
      })
      api('listVirtualMachines', { listall: true, retrieveonlyresourcecount: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalvms-bg'] || '#ffffff'
        this.stats.splice(2, 1, { name: this.$t('label.total.vms'), count: count, icon: 'number-outlined', bgcolor: tileColor, path: '/vm' })
      })
      api('listVolumes', { listall: true, retrieveonlyresourcecount: true }).then(json => {
        var count = 0
        if (json && json.listvolumesresponse) {
          count = json.listvolumesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalvolumes-bg'] || '#ffffff'
        this.stats.splice(3, 1, { name: this.$t('label.total.volume'), count: count, icon: 'database-outlined', bgcolor: tileColor, path: '/volume' })
      })
      api('listNetworks', { listall: true, retrieveonlyresourcecount: true }).then(json => {
        var count = 0
        if (json && json.listnetworksresponse) {
          count = json.listnetworksresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalnetworks-bg'] || '#ffffff'
        this.stats.splice(4, 1, { name: this.$t('label.total.network'), count: count, icon: 'apartment-outlined', bgcolor: tileColor, path: '/guestnetwork' })
      })
      api('listPublicIpAddresses', { listall: true, retrieveonlyresourcecount: true }).then(json => {
        var count = 0
        if (json && json.listpublicipaddressesresponse) {
          count = json.listpublicipaddressesresponse.count
        }
        var tileColor = this.$config.theme['@dashboard-tile-totalips-bg'] || '#ffffff'
        this.stats.splice(5, 1, { name: this.$t('label.public.ip.addresses'), count: count, icon: 'environment-outlined', bgcolor: tileColor, path: '/publicip' })
      })
      this.listEvents()
    },
    listAccount () {
      this.loading = true
      api('listAccounts', { id: this.$store.getters.userInfo.accountid }).then(json => {
        this.loading = false
        if (json && json.listaccountsresponse && json.listaccountsresponse.account) {
          this.entity = json.listaccountsresponse.account[0]
        }
      })
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
    getPercentUsed (total, limit) {
      return (limit === 'Unlimited') ? 0 : (total / limit) * 100
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

  .dashboard-card {
    width: 100%;
  }

  .dashboard-event {
    width: 100%;
    overflow-x:hidden;
    overflow-y: scroll;
    max-height: 345px;
  }

  .center {
    display: block;
    text-align: center;
  }

  @media (max-width: 1200px) {
    .ant-col-xl-8 {
      width: 100%;
    }
  }
</style>
