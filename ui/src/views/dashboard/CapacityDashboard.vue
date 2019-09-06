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
              <h4>{{ stat.name }}</h4>
              <a-progress type="dashboard" :percent="Number(parseFloat(stat.percentused, 10).toFixed(2))" :width="100" />
            </div>
            <template slot="footer"><span>{{ stat.capacityused }} / {{ stat.capacitytotal }}</span></template>
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

import ChartCard from '@/components/chart/ChartCard'
import ACol from 'ant-design-vue/es/grid/Col'

export default {
  name: 'CapacityDashboard',
  components: {
    ACol,
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
