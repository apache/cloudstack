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
    <a-col :span="24">
      <div class="capacity-dashboard-wrapper">
        <div class="capacity-dashboard-select">
          <a-select
            :placeholder="$t('label.select.a.zone')"
            v-model:value="zoneSelectedKey"
            @change="changeZone"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="(zone, index) in zones" :key="index" :label="zone.name">
              <span>
                <resource-icon v-if="zone.icon && zone.icon.base64image" :image="zone.icon.base64image" size="2x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ zone.name }}
              </span>
            </a-select-option>
          </a-select>
        </div>
        <div class="capacity-dashboard-button">
          <a-button
            shape="round"
            @click="() => { listCapacity(zoneSelected, true); listHosts(zoneSelected); listInstances(zoneSelected); listAlerts(); listEvents(); }">
            {{ $t('label.fetch.latest') }}
          </a-button>
        </div>
      </div>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <div>
          <div v-for="ctype in ['MEMORY', 'CPU', 'CPU_CORE', 'GPU']" :key="ctype" >
            <div>
              <div>
                <strong>{{ $t(ts[ctype]) }}</strong>
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(statsMap[ctype]?.percentused)"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ displayData(ctype, statsMap[ctype]?.capacityused) }} {{ $t('label.allocated') }} | {{ displayData(ctype, statsMap[ctype]?.capacitytotal) }} {{ $t('label.total') }}
              </div>
            </div>
          </div>
        </div>
        <template #title>
          <div class="center">
            <h3><cloud-outlined /> {{ $t('label.compute') }}</h3>
          </div>
          <a-divider style="margin: 12px 0;"/>
        </template>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <div>
          <div v-for="ctype in ['STORAGE', 'STORAGE_ALLOCATED', 'LOCAL_STORAGE', 'SECONDARY_STORAGE']" :key="ctype" >
            <div>
              <div>
                <strong>{{ $t(ts[ctype]) }}</strong>
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(statsMap[ctype]?.percentused)"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ displayData(ctype, statsMap[ctype]?.capacityused) }} {{ $t('label.allocated') }} | {{ displayData(ctype, statsMap[ctype]?.capacitytotal) }} {{ $t('label.total') }}
              </div>
            </div>
          </div>
        </div>
        <template #title>
          <div class="center">
            <h3><hdd-outlined /> {{ $t('label.storage') }}</h3>
          </div>
          <a-divider style="margin: 12px 0;"/>
        </template>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <div>
          <div v-for="ctype in ['VLAN', 'VIRTUAL_NETWORK_PUBLIC_IP', 'VIRTUAL_NETWORK_IPV6_SUBNET', 'DIRECT_ATTACHED_PUBLIC_IP', 'PRIVATE_IP']" :key="ctype" >
            <div v-if="statsMap[ctype]">
              <div>
                <strong>{{ $t(ts[ctype]) }}</strong>
              </div>
              <a-progress
              status="active"
              :percent="parseFloat(statsMap[ctype]?.percentused)"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ displayData(ctype, statsMap[ctype]?.capacityused) }} {{ $t('label.allocated') }} | {{ displayData(ctype, statsMap[ctype]?.capacitytotal) }} {{ $t('label.total') }}
              </div>
            </div>
          </div>
        </div>
        <template #title>
          <div class="center">
            <h3><apartment-outlined /> {{ $t('label.network') }}</h3>
          </div>
          <a-divider style="margin: 12px 0;"/>
        </template>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }" class="dashboard-card">
      <chart-card :loading="loading">
        <a-row>
          <a-col :span="12">
            <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id, state: 'up' } }">
              <a-statistic :title="$t('label.up')" :value="hosts.up">
                <template #prefix>
                  <status class="status" text="Up"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id, state: 'down' } }">
              <a-statistic :title="$t('label.down')" :value="hosts.alert">
                <template #prefix>
                  <status class="status" text="Down"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="12">
            <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id, state: 'alert' } }">
              <a-statistic :title="$t('label.alert')" :value="hosts.alert">
                <template #prefix>
                  <status class="status" text="Alert"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id, resourcestate: 'maintenance' } }">
              <a-statistic :title="$t('label.maintenance')" :value="hosts.maintenance">
                <template #prefix>
                  <status class="status" text="Maintenance"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
        </a-row>
        <div>
        </div>
        <template #title>
          <div class="center">
            <h3>
              <desktop-outlined />
              {{ $t('label.hosts') }}
              <span style="float: right">
                <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id } }">
                  {{ hosts.total }}
                </router-link>
              </span>
            </h3>
          </div>
        </template>
      </chart-card>
      <chart-card :loading="loading" style="margin-top: 12px">
        <a-row>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { zoneid: zoneSelected.id, projectid: '-1', state: 'running' } }">
              <a-statistic :title="$t('label.running')" :value="instances.running">
                <template #prefix>
                  <status class="status" text="Running"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { zoneid: zoneSelected.id, projectid: '-1', state: 'stopped' } }">
              <a-statistic :title="$t('label.stopped')" :value="instances.stopped">
                <template #prefix>
                  <status class="status" text="Stopped"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
        </a-row>
        <template #title>
          <div class="center">
            <h3>
              <cloud-server-outlined />
              {{ $t('label.instances') }}
              <span style="float: right">
                <router-link :to="{ path: '/vm', query: { zoneid: zoneSelected.id, projectid: '-1' } }">
                  {{ instances.total }}
                </router-link>
              </span>
            </h3>
          </div>
        </template>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 24 }" :xl="{ span: 16 }" :xxl="{ span: 12 }" class="dashboard-card dashboard-event">
      <a-card :loading="loading" :border="false" class="dashboard-event">
        <a-tabs v-model:activeKey="tabKey">
          <a-tab-pane key="alerts" :tab="$t('label.alerts')">
            <a-timeline>
              <a-timeline-item
                v-for="alert in alerts"
                :key="alert.id"
                color="red">
                <span :style="{ color: '#999' }"><small>{{ $toLocaleDate(alert.sent) }}</small></span>&nbsp;
                <span :style="{ color: '#666' }"><small><router-link :to="{ path: '/alert/' + alert.id }">{{ alert.name }}</router-link></small></span><br/>
                <span :style="{ color: '#aaa' }">{{ alert.description }}</span>
              </a-timeline-item>
            </a-timeline>
            <router-link :to="{ path: '/alert' }">
              <a-button>
                {{ $t('label.view') }} {{ $t('label.alerts') }}
              </a-button>
            </router-link>
          </a-tab-pane>
          <a-tab-pane key="event" :tab="$t('label.events')" force-render>
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
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
import { api } from '@/api'

import ChartCard from '@/components/widgets/ChartCard'
import ResourceIcon from '@/components/view/ResourceIcon'
import ResourceLabel from '@/components/widgets/ResourceLabel'
import Status from '@/components/widgets/Status'

export default {
  name: 'CapacityDashboard',
  components: {
    ChartCard,
    ResourceIcon,
    ResourceLabel,
    Status
  },
  data () {
    return {
      loading: true,
      tabKey: 'alerts',
      alerts: [],
      events: [],
      zones: [],
      zoneSelected: {},
      stats: [],
      statsMap: {},
      hosts: {
        total: 0,
        up: 0,
        alert: 0,
        down: 0,
        maintenance: 0
      },
      instances: {
        total: 0,
        running: 0,
        stopped: 0
      },
      ts: {
        CPU: 'label.cpu',
        CPU_CORE: 'label.cpunumber',
        DIRECT_ATTACHED_PUBLIC_IP: 'label.direct.ips',
        GPU: 'label.gpu',
        LOCAL_STORAGE: 'label.local.storage',
        MEMORY: 'label.memory',
        PRIVATE_IP: 'label.management.ips',
        SECONDARY_STORAGE: 'label.secondary.storage',
        STORAGE: 'label.storage',
        STORAGE_ALLOCATED: 'label.primary.storage',
        VIRTUAL_NETWORK_PUBLIC_IP: 'label.public.ips',
        VLAN: 'label.vlan',
        VIRTUAL_NETWORK_IPV6_SUBNET: 'label.ipv6.subnets'
      }
    }
  },
  computed: {
    zoneSelectedKey () {
      if (this.zones.length === 0) {
        return this.zoneSelected.name
      }
      const zoneIndex = this.zones.findIndex(zone => zone.id === this.zoneSelected.id)
      return zoneIndex
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'dashboard') {
        this.fetchData()
      }
    }
  },
  methods: {
    getStatus (value) {
      if (value > 85) {
        return 'exception'
      }
      if (value > 75) {
        return 'active'
      }
      return 'normal'
    },
    displayData (dataType, value) {
      switch (dataType) {
        case 'CPU':
          value = parseFloat(value / 1000.0, 10).toFixed(2) + ' GHz'
          break
        case 'MEMORY':
        case 'STORAGE':
        case 'STORAGE_ALLOCATED':
        case 'SECONDARY_STORAGE':
        case 'LOCAL_STORAGE':
          value = parseFloat(value / (1024 * 1024 * 1024.0), 10).toFixed(2)
          if (value >= 1024.0) {
            value = parseFloat(value / 1024.0).toFixed(2) + ' TB'
          } else {
            value = value + ' GB'
          }
          break
      }
      return value
    },
    fetchData () {
      this.listZones()
      this.listAlerts()
      this.listEvents()
    },
    listCapacity (zone, latest = false) {
      const params = {
        zoneid: zone.id,
        fetchlatest: latest
      }
      this.loading = true
      api('listCapacity', params).then(json => {
        this.stats = []
        this.loading = false
        if (json && json.listcapacityresponse && json.listcapacityresponse.capacity) {
          this.stats = json.listcapacityresponse.capacity
        }
        for (const stat of this.stats) {
          this.statsMap[stat.name] = stat
        }
      })
    },
    listHosts (zone) {
      this.loading = true
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing' }).then(json => {
        this.loading = false
        this.hosts.total = json?.listhostsresponse?.count
      })
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing', state: 'up' }).then(json => {
        this.loading = false
        this.hosts.up = json?.listhostsresponse?.count
      })
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing', state: 'alert' }).then(json => {
        this.loading = false
        this.hosts.alert = json?.listhostsresponse?.count
      })
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing', state: 'down' }).then(json => {
        this.loading = false
        this.hosts.down = json?.listhostsresponse?.count
      })
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing', resourcestate: 'maintenance' }).then(json => {
        this.loading = false
        this.hosts.maintenance = json?.listhostsresponse?.count
      })
    },
    listInstances (zone) {
      this.loading = true
      api('listVirtualMachines', { zoneid: zone.id, listall: true, projectid: '-1', details: 'min' }).then(json => {
        this.loading = false
        this.instances.total = json?.listvirtualmachinesresponse?.count
      })
      api('listVirtualMachines', { zoneid: zone.id, listall: true, projectid: '-1', details: 'min', state: 'running' }).then(json => {
        this.loading = false
        this.instances.running = json?.listvirtualmachinesresponse?.count
      })
      api('listVirtualMachines', { zoneid: zone.id, listall: true, projectid: '-1', details: 'min', state: 'stopped' }).then(json => {
        this.loading = false
        this.instances.stopped = json?.listvirtualmachinesresponse?.count
      })
    },
    listAlerts () {
      const params = {
        page: 1,
        pagesize: 5,
        listall: true
      }
      this.loading = true
      api('listAlerts', params).then(json => {
        this.alerts = []
        this.loading = false
        if (json && json.listalertsresponse && json.listalertsresponse.alert) {
          this.alerts = json.listalertsresponse.alert
        }
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
    getEventColour (event) {
      if (event.level === 'ERROR') {
        return 'red'
      }
      if (event.state === 'Completed') {
        return 'green'
      }
      return 'blue'
    },
    listZones () {
      api('listZones', { showicon: true }).then(json => {
        if (json && json.listzonesresponse && json.listzonesresponse.zone) {
          this.zones = json.listzonesresponse.zone
          if (this.zones.length > 0) {
            this.zoneSelected = this.zones[0]
            this.listCapacity(this.zones[0])
            this.listHosts(this.zones[0])
            this.listInstances(this.zones[0])
          }
        }
      })
    },
    changeZone (index) {
      this.zoneSelected = this.zones[index]
      this.listCapacity(this.zoneSelected)
      this.listHosts(this.zoneSelected)
      this.listInstances(this.zoneSelected)
    },
    filterZone (input, option) {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }
  }
}
</script>

<style lang="less" scoped>
.capacity-dashboard {
  &-wrapper {
    display: flex;
  }

  &-chart-card-inner {
     text-align: center;
     white-space: nowrap;
     overflow: hidden;
  }

  &-select {
    width: 100%; // for flexbox causes

    .ant-select {
      width: 100%; // to fill flex item width
    }
  }

  &-button-wrapper {
    margin-left: 12px;
  }

  &-button {
    width: auto;
    padding-left: 12px;
  }

  &-button-icon {
    font-size: 16px;
    padding: 2px;
  }

  &-title {
    padding-top: 12px;
    padding-left: 3px;
    white-space: normal;
  }
}

.dashboard-card {
  width: 100%;
  .ant-card {
  }
}

.dashboard-event {
  width: 100%;
  overflow-x:hidden;
  overflow-y: scroll;
  max-height: 362px;
}

.center {
  display: block;
  text-align: center;
}

@media (max-width: 1200) {
}

</style>
