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
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <h3>
              <cloud-outlined /> {{ $t('label.compute') }}
            </h3>
          </div>
        </template>
        <a-row>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { projectid: showProject ? '' : '-1', state: 'running' } }">
              <a-statistic :title="$t('label.running') + ' ' + $t('label.instances')" :value="instances.running">
                <template #prefix>
                  <status class="status" text="Running"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { projectid: showProject ? '' : '-1', state: 'stopped' } }">
              <a-statistic :title="$t('label.stopped') + ' ' + $t('label.instances')" :value="instances.stopped">
                <template #prefix>
                  <status class="status" text="Stopped"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
        </a-row>
        <a-divider style="margin: 8px 0px; border-width: 0px;"/>
        <div
          v-for="usageType in ['vm', 'cpu', 'memory', 'project']"
          :key="usageType">
          <div v-if="usageType + 'total' in entity">
            <div>
              <strong>
                {{ $t(getLabel(usageType)) }}
              </strong>
              <span style="float: right">
              {{ getValue(usageType, entity[usageType + 'total']) }} {{ $t('label.used') }}
              </span>
            </div>
            <a-progress
            status="active"
            :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
            :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
            stroke-color="#52c41a"
            size="small"
            style="width:94%; float: left"
            />
            <br/>
            <div style="text-align: center">
              {{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : getValue(usageType, entity[usageType + 'available']) }} {{ $t('label.available') }} |
              {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : getValue(usageType, entity[usageType + 'limit']) }} {{ $t('label.limit') }}
            </div>
          </div>
        </div>
        <div v-if="showProject">
          <a-divider style="margin: 12px 0px; border-width: 0px;"/>
          <router-link :to="{ path: '/project/' + project.id }">
            <a-button type="primary">
              {{ $t('label.view') }} {{  $t('label.project') }}
            </a-button>
          </router-link>
          &nbsp;
          <router-link :to="{ path: '/project/' + project.id, query: { tab: 'limits.configure' } }">
            <a-button v-if="['Admin'].includes($store.getters.userInfo.roletype)">
              {{ $t('label.configure') }} {{ $t('label.project') }} {{ $t('label.limits') }}
            </a-button>
          </router-link>
        </div>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <h3><hdd-outlined /> {{ $t('label.storage') }}</h3>
          </div>
        </template>
        <div
          v-for="usageType in ['volume', 'snapshot', 'template', 'primarystorage', 'secondarystorage']"
          :key="usageType">
          <div>
            <div>
              <strong>
                {{ $t(getLabel(usageType)) }}
              </strong>
              <span style="float: right">
              {{ getValue(usageType, entity[usageType + 'total']) }} {{ $t('label.used') }}
              </span>
            </div>
            <a-progress
            status="active"
            :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
            :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
            stroke-color="#52c41a"
            size="small"
            style="width:94%; float: left"
            />
            <br/>
            <div style="text-align: center">
              {{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : getValue(usageType, entity[usageType + 'available']) }} {{ $t('label.available') }} |
              {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : getValue(usageType, entity[usageType + 'limit']) }} {{ $t('label.limit') }}
            </div>
          </div>
        </div>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <h3><apartment-outlined /> {{ $t('label.network') }}</h3>
          </div>
        </template>
        <div
          v-for="usageType in ['ip', 'network', 'vpc']"
          :key="usageType">
          <div>
            <div>
              <strong>
                {{ $t(getLabel(usageType)) }}
              </strong>
              <span style="float: right">
              {{ getValue(usageType, entity[usageType + 'total']) }} {{ $t('label.used') }}
              </span>
            </div>
            <a-progress
            status="active"
            :percent="parseFloat(getPercentUsed(entity[usageType + 'total'], entity[usageType + 'limit']))"
            :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''"
            stroke-color="#52c41a"
            size="small"
            style="width:94%; float: left"
            />
            <br/>
            <div style="text-align: center">
              {{ entity[usageType + 'available'] === '-1' ? $t('label.unlimited') : getValue(usageType, entity[usageType + 'available']) }} {{ $t('label.available') }} |
              {{ entity[usageType + 'limit'] === '-1' ? $t('label.unlimited') : getValue(usageType, entity[usageType + 'limit']) }} {{ $t('label.limit') }}
            </div>
          </div>
        </div>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
      <chart-card :loading="loading" class="dashboard-card dashboard-event">
        <template #title>
          <div class="center">
            <h3><schedule-outlined /> {{ $t('label.events') }}</h3>
          </div>
        </template>
        <br/>
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
import Status from '@/components/widgets/Status'

export default {
  name: 'UsageDashboard',
  components: {
    ChartCard,
    UsageDashboardChart,
    ResourceLabel,
    Status
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
      project: {},
      account: {},
      events: [],
      instances: {
        running: 0,
        stopped: 0
      }
    }
  },
  computed: {
    entity: function () {
      if (this.showProject) {
        return this.project
      }
      return this.account
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
        if (newData.id) {
          this.fetchData()
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
      if (store.getters.project.id) {
        this.listProject()
      } else {
        this.listAccount()
      }
      this.listInstances()
      this.listEvents()
    },
    listAccount () {
      this.loading = true
      api('listAccounts', { id: this.$store.getters.userInfo.accountid }).then(json => {
        this.loading = false
        if (json && json.listaccountsresponse && json.listaccountsresponse.account) {
          this.account = json.listaccountsresponse.account[0]
        }
      })
    },
    listProject () {
      this.loading = true
      api('listProjects', { id: store.getters.project.id }).then(json => {
        this.loading = false
        if (json && json.listprojectsresponse && json.listprojectsresponse.project) {
          this.project = json.listprojectsresponse.project[0]
        }
      })
    },
    listInstances (zone) {
      this.loading = true
      api('listVirtualMachines', { listall: true, details: 'min', state: 'running' }).then(json => {
        this.loading = false
        this.instances.running = json?.listvirtualmachinesresponse?.count
      })
      api('listVirtualMachines', { listall: true, details: 'min', state: 'stopped' }).then(json => {
        this.loading = false
        this.instances.stopped = json?.listvirtualmachinesresponse?.count
      })
    },
    listEvents () {
      const params = {
        page: 1,
        pagesize: 5,
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
    getLabel (usageType) {
      switch (usageType) {
        case 'vm':
          return 'label.instances'
        case 'cpu':
          return 'label.cpunumber'
        case 'memory':
          return 'label.memory'
        case 'primarystorage':
          return 'label.primary.storage'
        case 'secondarystorage':
          return 'label.secondary.storage'
        case 'ip':
          return 'label.public.ips'
      }
      return 'label.' + usageType + 's'
    },
    getValue (usageType, value) {
      switch (usageType) {
        case 'memory':
          return parseFloat(value / 1024.0).toFixed(2) + ' GiB'
        case 'primarystorage':
          return parseFloat(value).toFixed(2) + ' GiB'
        case 'secondarystorage':
          return parseFloat(value).toFixed(2) + ' GiB'
      }
      return value
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
    min-height: 450px;
  }

  .dashboard-event {
    width: 100%;
    overflow-x:hidden;
    overflow-y: scroll;
    max-height: 450px;
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
