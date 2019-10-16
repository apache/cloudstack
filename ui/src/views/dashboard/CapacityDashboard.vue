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
            showSearch
            optionFilterProp="children"
            :defaultValue="zoneSelected.name"
            :value="zoneSelected.name"
            @change="changeZone">
            <a-select-option v-for="(zone, index) in zones" :key="index">
              {{ zone.name }}
            </a-select-option>
          </a-select>
        </div>
        <div class="capacity-dashboard-button">
          <a-tooltip placement="bottom">
            <template slot="title">
              Fetch Latest
            </template>
            <a-button
              type="primary"
              shape="circle"
              @click="listCapacity(zoneSelected, true)">
              <a-icon class="capacity-dashboard-button-icon" type="reload" />
            </a-button>
          </a-tooltip>
        </div>
        <div class="capacity-dashboard-button">
          <a-tooltip placement="bottom">
            <template slot="title">
              View Alerts
            </template>
            <a-button shape="circle">
              <router-link :to="{ name: 'alert' }">
                <a-icon class="capacity-dashboard-button-icon" type="flag" />
              </router-link>
            </a-button>
          </a-tooltip>
        </div>
        <div class="capacity-dashboard-button">
          <a-tooltip placement="bottom">
            <template slot="title">
              View Hosts in Alert State
            </template>
            <a-button type="danger" shape="circle">
              <router-link :to="{ name: 'host', query: {'state': 'Alert'} }">
                <a-badge dot>
                  <a-icon class="capacity-dashboard-button-icon" type="desktop" />
                </a-badge>
              </router-link>
            </a-button>
          </a-tooltip>
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
            <div class="capacity-dashboard-chart-card-inner">
              <h4>{{ $t(stat.name) }}</h4>
              <a-progress
                type="dashboard"
                :status="getStatus(parseFloat(stat.percentused))"
                :percent="parseFloat(stat.percentused)"
                :format="percent => `${parseFloat(stat.percentused, 10).toFixed(2)}%`"
                :width="100" />
            </div>
            <template slot="footer"><center>{{ displayData(stat.name, stat.capacityused) }} / {{ displayData(stat.name, stat.capacitytotal) }}</center></template>
          </chart-card>
        </a-col>
      </a-row>
    </a-col>

    <a-col :xl="6">
      <chart-card>
        <div style="text-align: center">
          <a-button><router-link :to="{ name: 'event' }">View Events</router-link></a-button>
        </div>
        <template slot="footer">
          <div class="capacity-dashboard-footer">
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

import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'CapacityDashboard',
  components: {
    ChartCard
  },
  data () {
    return {
      loading: true,
      events: [],
      zones: [],
      zoneSelected: {},
      stats: []
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'dashboard') {
        this.fetchData()
      }
    }
  },
  created () {
    setTimeout(() => {
      // to do after initial timeout
    }, 1000)
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
        case 'CAPACITY_TYPE_LOCAL_STORAGE':
          value = parseFloat(value / (1024 * 1024 * 1024.0), 10).toFixed(2) + ' GB'
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
      api('listZones').then(json => {
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
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
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
</style>
