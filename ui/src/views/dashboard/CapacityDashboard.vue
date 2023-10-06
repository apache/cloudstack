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
            @click="() => { updateData(zoneSelected); listAlerts(); listEvents(); }">
            <reload-outlined/>
            {{ $t('label.fetch.latest') }}
          </a-button>
        </div>
      </div>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 8 }">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <router-link :to="{ path: '/infrasummary' }" v-if="!zoneSelected.id">
              <h3>
                <bank-outlined />
                {{ $t('label.infrastructure') }}
              </h3>
            </router-link>
            <router-link :to="{ path: '/zone/' + zoneSelected.id }" v-else>
              <h3>
                <global-outlined />
                {{ $t('label.zone') }}
              </h3>
            </router-link>
          </div>
        </template>
        <a-divider style="margin: 0px 0px; border-width: 0px"/>
        <a-row :gutter="[12, 12]">
          <a-col :span="12">
            <router-link :to="{ path: '/pod', query: { zoneid: zoneSelected.id } }">
              <a-statistic
                :title="$t('label.pods')"
                :value="data.pods"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <appstore-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/cluster', query: { zoneid: zoneSelected.id } }">
              <a-statistic
                :title="$t('label.clusters')"
                :value="data.clusters"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <cluster-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id } }">
              <a-statistic
                :title="$t('label.hosts')"
                :value="data.totalHosts"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <database-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/host', query: { zoneid: zoneSelected.id, state: 'alert' } }">
              <a-statistic
                :title="$t('label.host.alerts')"
                :value="data.alertHosts"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <database-outlined/>
                  <status class="status" text="Alert" style="margin-left: -10px"/>
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/storagepool', query: { zoneid: zoneSelected.id } }">
              <a-statistic
                :title="$t('label.primary.storage')"
                :value="data.pools"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <hdd-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/systemvm', query: { zoneid: zoneSelected.id } }">
              <a-statistic
                :title="$t('label.system.vms')"
                :value="data.systemvms"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <thunderbolt-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/router', query: { zoneid: zoneSelected.id } }">
              <a-statistic
                :title="$t('label.virtual.routers')"
                :value="data.routers"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <fork-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
          <a-col :span="12">
            <router-link :to="{ path: '/vm', query: { zoneid: zoneSelected.id, projectid: '-1' } }">
              <a-statistic
                :title="$t('label.instances')"
                :value="data.instances"
                :value-style="{ color: $config.theme['@primary-color'] }">
                <template #prefix>
                  <cloud-server-outlined/>&nbsp;
                </template>
              </a-statistic>
            </router-link>
          </a-col>
        </a-row>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 8 }">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <h3><cloud-outlined /> {{ $t('label.compute') }}</h3>
          </div>
        </template>
        <div>
          <div v-for="ctype in ['MEMORY', 'CPU', 'CPU_CORE', 'GPU']" :key="ctype" >
            <div v-if="statsMap[ctype]">
              <div>
                <strong>{{ $t(ts[ctype]) }}</strong>
              </div>
              <a-progress
              status="active"
              :percent="statsMap[ctype]?.capacitytotal > 0 ? parseFloat(100.0 * statsMap[ctype]?.capacityused / statsMap[ctype]?.capacitytotal).toFixed(2) : 0"
              :format="p => statsMap[ctype]?.capacitytotal > 0 ? parseFloat(100.0 * statsMap[ctype]?.capacityused / statsMap[ctype]?.capacitytotal).toFixed(2) + '%' : '0%'"
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
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 8 }">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <h3><hdd-outlined /> {{ $t('label.storage') }}</h3>
          </div>
        </template>
        <div>
          <div v-for="ctype in ['STORAGE', 'STORAGE_ALLOCATED', 'LOCAL_STORAGE', 'SECONDARY_STORAGE']" :key="ctype" >
            <div v-if="statsMap[ctype]">
              <div>
                <strong>{{ $t(ts[ctype]) }}</strong>
              </div>
              <a-progress
              status="active"
              :percent="statsMap[ctype]?.capacitytotal > 0 ? parseFloat(100.0 * statsMap[ctype]?.capacityused / statsMap[ctype]?.capacitytotal).toFixed(2) : 0"
              :format="p => statsMap[ctype]?.capacitytotal > 0 ? parseFloat(100.0 * statsMap[ctype]?.capacityused / statsMap[ctype]?.capacitytotal).toFixed(2) + '%' : '0%'"
              stroke-color="#52c41a"
              size="small"
              style="width:95%; float: left"
              />
              <br/>
              <div style="text-align: center">
                {{ displayData(ctype, statsMap[ctype]?.capacityused) }} <span v-if="ctype !== 'STORAGE'">{{ $t('label.allocated') }}</span><span v-else>{{ $t('label.used') }}</span> | {{ displayData(ctype, statsMap[ctype]?.capacitytotal) }} {{ $t('label.total') }}
              </div>
            </div>
          </div>
        </div>
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 8 }">
      <chart-card :loading="loading" class="dashboard-card">
        <template #title>
          <div class="center">
            <h3><apartment-outlined /> {{ $t('label.network') }}</h3>
          </div>
        </template>
        <div>
          <div v-for="ctype in ['VLAN', 'VIRTUAL_NETWORK_PUBLIC_IP', 'VIRTUAL_NETWORK_IPV6_SUBNET', 'DIRECT_ATTACHED_PUBLIC_IP', 'PRIVATE_IP']" :key="ctype" >
            <div v-if="statsMap[ctype]">
              <div>
                <strong>{{ $t(ts[ctype]) }}</strong>
              </div>
              <a-progress
              status="active"
              :percent="statsMap[ctype]?.capacitytotal > 0 ? parseFloat(100.0 * statsMap[ctype]?.capacityused / statsMap[ctype]?.capacitytotal).toFixed(2) : 0"
              :format="p => statsMap[ctype]?.capacitytotal > 0 ? parseFloat(100.0 * statsMap[ctype]?.capacityused / statsMap[ctype]?.capacitytotal).toFixed(2) + '%' : '0%'"
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
      </chart-card>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 8 }">
      <router-link :to="{ path: '/alert' }">
      <a-card :loading="loading" :bordered="false" class="dashboard-card dashboard-event">
        <div class="center" style="margin-top: -8px">
          <h3>
            <flag-outlined />
            {{ $t('label.alerts') }}
          </h3>
        </div>
        <a-divider style="margin: 6px 0px; border-width: 0px"/>
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
      </a-card>
      </router-link>
    </a-col>
    <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 8 }">
      <router-link :to="{ path: '/event' }">
      <a-card :loading="loading" :bordered="false" class="dashboard-card dashboard-event">
        <div class="center" style="margin-top: -8px">
          <h3>
            <schedule-outlined />
            {{ $t('label.events') }}
          </h3>
        </div>
        <a-divider style="margin: 6px 0px; border-width: 0px"/>
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
      </router-link>
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
      statsMap: {},
      data: {
        pods: 0,
        clusters: 0,
        totalHosts: 0,
        alertHosts: 0,
        pools: 0,
        instances: 0,
        systemvms: 0,
        routers: 0
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
        STORAGE: 'label.primary.storage.used',
        STORAGE_ALLOCATED: 'label.primary.storage.allocated',
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
      if (!value) {
        value = 0
      }
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
            value = parseFloat(value / 1024.0).toFixed(2) + ' TiB'
          } else {
            value = value + ' GiB'
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
    listCapacity (zone, latest = false, additive = false) {
      this.loading = true
      api('listCapacity', { zoneid: zone.id, fetchlatest: latest }).then(json => {
        this.loading = false
        let stats = []
        if (json && json.listcapacityresponse && json.listcapacityresponse.capacity) {
          stats = json.listcapacityresponse.capacity
        }
        for (const stat of stats) {
          if (additive) {
            for (const [key, value] of Object.entries(stat)) {
              if (stat.name in this.statsMap) {
                if (key in this.statsMap[stat.name]) {
                  this.statsMap[stat.name][key] += value
                } else {
                  this.statsMap[stat.name][key] = value
                }
              } else {
                this.statsMap[stat.name] = { key: value }
              }
            }
          } else {
            this.statsMap[stat.name] = stat
          }
        }
      })
    },
    updateData (zone) {
      if (!zone.id) {
        this.statsMap = {}
        for (const zone of this.zones.slice(1)) {
          this.listCapacity(zone, true, true)
        }
      } else {
        this.statsMap = {}
        this.listCapacity(this.zoneSelected, true)
      }

      this.data = {
        pods: 0,
        clusters: 0,
        totalHosts: 0,
        alertHosts: 0,
        pools: 0,
        instances: 0,
        systemvms: 0,
        routers: 0
      }
      this.loading = true
      api('listPods', { zoneid: zone.id }).then(json => {
        this.loading = false
        this.data.pods = json?.listpodsresponse?.count
        if (!this.data.pods) {
          this.data.pods = 0
        }
      })
      api('listClusters', { zoneid: zone.id }).then(json => {
        this.loading = false
        this.data.clusters = json?.listclustersresponse?.count
        if (!this.data.clusters) {
          this.data.clusters = 0
        }
      })
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing', page: 1, pagesize: 1 }).then(json => {
        this.loading = false
        this.data.totalHosts = json?.listhostsresponse?.count
        if (!this.data.totalHosts) {
          this.data.totalHosts = 0
        }
      })
      api('listHosts', { zoneid: zone.id, listall: true, details: 'min', type: 'routing', state: 'alert', page: 1, pagesize: 1 }).then(json => {
        this.loading = false
        this.data.alertHosts = json?.listhostsresponse?.count
        if (!this.data.alertHosts) {
          this.data.alertHosts = 0
        }
      })
      api('listStoragePools', { zoneid: zone.id }).then(json => {
        this.loading = false
        this.data.pools = json?.liststoragepoolsresponse?.count
        if (!this.data.pools) {
          this.data.pools = 0
        }
      })
      api('listSystemVms', { zoneid: zone.id }).then(json => {
        this.loading = false
        this.data.systemvms = json?.listsystemvmsresponse?.count
        if (!this.data.systemvms) {
          this.data.systemvms = 0
        }
      })
      api('listRouters', { zoneid: zone.id, listall: true }).then(json => {
        this.loading = false
        this.data.routers = json?.listroutersresponse?.count
        if (!this.data.routers) {
          this.data.routers = 0
        }
      })
      api('listVirtualMachines', { zoneid: zone.id, listall: true, projectid: '-1', details: 'min', page: 1, pagesize: 1 }).then(json => {
        this.loading = false
        this.data.instances = json?.listvirtualmachinesresponse?.count
        if (!this.data.instances) {
          this.data.instances = 0
        }
      })
    },
    listAlerts () {
      const params = {
        page: 1,
        pagesize: 8,
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
        pagesize: 8,
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
            this.zones.splice(0, 0, { name: this.$t('label.all.zone') })
            this.zoneSelected = this.zones[0]
            this.updateData(this.zones[0])
          }
        }
      })
    },
    changeZone (index) {
      this.zoneSelected = this.zones[index]
      this.updateData(this.zoneSelected)
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
    padding-left: 8px;
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
  min-height: 370px;
}

.dashboard-event {
  width: 100%;
  overflow-x:hidden;
  overflow-y: auto;
  max-height: 370px;
}

.center {
  display: block;
  text-align: center;
}

</style>
