<template>
  <div class="page-header-index-wide">
    <a-row :gutter="12" :style="{ marginTop: '24px' }">
      <a-col :xl="16">
        <div class="ant-pro-capacity-dashboard__wrapper">
          <div class="ant-pro-capacity-dashboard__select">
            <a-select
              showSearch
              optionFilterProp="children"
              :defaultValue="zoneSelected.name"
              :value="zoneSelected.name"
              @change="changeZone"
              style="width: 100%" >
              <a-select-option v-for="(zone, index) in zones" :key="index">
                {{ zone.name }}
              </a-select-option>
            </a-select>
          </div>
          <div class="ant-pro-capacity-dashboard__button">
            <a-button
              type="primary"
              @click="listCapacity(zoneSelected, true)">Fetch Latest</a-button>
          </div>
          <div class="ant-pro-capacity-dashboard__button">
            <a-button>
              <router-link :to="{ name: 'alert' }">
                <a-icon style="font-size: 16px; padding: 2px" type="flag" />
              </router-link>
            </a-button>
          </div>
          <div class="ant-pro-capacity-dashboard__button">
            <a-button type="danger">
              <router-link :to="{ name: 'host', query: {'state': 'Alert'} }">
                <a-badge dot>
                  <a-icon style="font-size: 16px" type="desktop" />
                </a-badge>
              </router-link>
            </a-button>
          </div>
        </div>
        <a-row :gutter="12">
          <a-col
            :xl="6"
            :style="{ marginBottom: '12px' }"
            v-for="stat in stats"
            :key="stat.type">
            <chart-card :loading="loading">
              <div style="text-align: center">
                <h4>{{ stat.name }}</h4>
                <a-progress type="dashboard" :percent="Number(parseFloat(stat.percentused, 10).toFixed(2))" :width="100" />
              </div>
              <template slot="footer"><span>{{ stat.capacityused }} / {{ stat.capacitytotal }}</span></template>
            </chart-card>
          </a-col>
        </a-row>
      </a-col>

      <a-col :xl="8">
        <chart-card>
          <div style="text-align: center">
            <a-button size="large"><router-link :to="{ name: 'event' }">View Events</router-link></a-button>
          </div>
          <template slot="footer">
            <div :style="{ paddingTop: '12px', paddingLeft: '3px', whiteSpace: 'normal' }">
              <a-timeline pending="...">
                <a-timeline-item
                  v-for="event in events"
                  :key="event.id"
                  :color="getEventColour(event)">
                  <span :style="{ color: '#999' }">{{ event.created }}</span><br/>
                  <span :style="{ color: '#666' }">{{ event.type }}</span><br/>
                  <span :style="{ color: '#aaa' }">({{ event.username }}) {{ event.description }}</span>
                </a-timeline-item>
              </a-timeline>
            </div>
          </template>
        </chart-card>
      </a-col>
    </a-row>
  </div>
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
  .extra-wrapper {
    line-height: 55px;
    //padding-right: 24px;

    .extra-item {
      display: inline-block;
      //margin-right: 24px;

      a {
        //margin-left: 24px;
      }
    }
  }

  .ant-pro-capacity-dashboard {
    &__wrapper {
      display: flex;
      margin-bottom: 12px;
    }

    &__select {
      width: 100%;
    }

    &__button {
      width: auto;
      padding-left: 12px;
    }
  }
</style>
