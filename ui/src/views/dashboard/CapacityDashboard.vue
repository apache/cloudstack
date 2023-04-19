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
  <a-row class="capacity-dashboard" :gutter="12">
    <a-col :xl="18">
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
                <resource-icon v-if="zone.icon && zone.icon.base64image" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ zone.name }}
              </span>
            </a-select-option>
          </a-select>
        </div>
        <div class="capacity-dashboard-button">
          <a-button
            shape="round"
            @click="() => { listCapacity(zoneSelected, true); listEvents() }">
            {{ $t('label.fetch.latest') }}
          </a-button>
        </div>
      </div>
      <a-row :gutter="12">
        <a-col
          :xs="12"
          :sm="8"
          :md="6"
          :style="{ marginBottom: '12px' }"
          v-for="stat in stats"
          :key="stat.type">
          <chart-card :loading="loading">
            <router-link :to="{ path: '/zone/' + zoneSelected.id }">
              <div class="capacity-dashboard-chart-card-inner">
                <h3>{{ $t(ts[stat.name]) }}</h3>
                <a-progress
                  type="dashboard"
                  :status="getStatus(parseFloat(stat.percentused))"
                  :percent="parseFloat(stat.percentused)"
                  :format="percent => `${parseFloat(stat.percentused).toFixed(2)}%`"
                  :strokeColor="getStrokeColour(parseFloat(stat.percentused))"
                  :width="100" />
              </div>
            </router-link>
            <template #footer>
              <div class="center">{{ displayData(stat.name, stat.capacityused) }} / {{ displayData(stat.name, stat.capacitytotal) }}</div>
            </template>
          </chart-card>
        </a-col>
      </a-row>
    </a-col>

    <a-col :xl="6" class="dashboard-event">
      <chart-card :loading="loading">
        <div style="text-align: center">
          <a-tooltip placement="bottom" class="capacity-dashboard-button-wrapper">
            <template #title>
              {{ $t('label.view') + ' ' + $t('label.host.alerts') }}
            </template>
            <a-button type="primary" danger shape="circle">
              <router-link :to="{ name: 'host', query: {'state': 'Alert'} }">
                <desktop-outlined class="capacity-dashboard-button-icon" />
              </router-link>
            </a-button>
          </a-tooltip>
          <a-tooltip placement="bottom" class="capacity-dashboard-button-wrapper">
            <template #title>
              {{ $t('label.view') + ' ' + $t('label.alerts') }}
            </template>
            <a-button shape="circle">
              <router-link :to="{ name: 'alert' }">
                <flag-outlined class="capacity-dashboard-button-icon" />
              </router-link>
            </a-button>
          </a-tooltip>
          <a-tooltip placement="bottom" class="capacity-dashboard-button-wrapper">
            <template #title>
              {{ $t('label.view') + ' ' + $t('label.events') }}
            </template>
            <a-button shape="circle">
              <router-link :to="{ name: 'event' }">
                <schedule-outlined class="capacity-dashboard-button-icon" />
              </router-link>
            </a-button>
          </a-tooltip>
        </div>
        <template #footer>
          <div class="capacity-dashboard-footer">
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

import ChartCard from '@/components/widgets/ChartCard'
import ResourceIcon from '@/components/view/ResourceIcon'
import ResourceLabel from '@/components/widgets/ResourceLabel'

export default {
  name: 'CapacityDashboard',
  components: {
    ChartCard,
    ResourceIcon,
    ResourceLabel
  },
  data () {
    return {
      loading: true,
      events: [],
      zones: [],
      zoneSelected: {},
      stats: [],
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
    getStrokeColour (value) {
      if (value >= 80) {
        return this.$config.theme['@graph-exception-color'] || 'red'
      }
      return this.$config.theme['@graph-normal-color'] || 'primary'
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
          }
        }
      })
    },
    changeZone (index) {
      this.zoneSelected = this.zones[index]
      this.listCapacity(this.zoneSelected)
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
    margin-bottom: 12px;
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

  &-footer {
    padding-top: 12px;
    padding-left: 3px;
    white-space: normal;
  }
}

.center {
  display: block;
  text-align: center;
}

@media (max-width: 1200px) {
  .dashboard-event {
    width: 100%;
  }
}
</style>
