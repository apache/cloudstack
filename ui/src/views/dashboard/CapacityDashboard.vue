<template>
  <div class="page-header-index-wide">
    <a-row :gutter="24" :style="{ marginTop: '24px' }">
      <a-col :sm="24" :md="6" :xl="18" :style="{ marginBottom: '24px' }">
        <a-col :sm="24" :md="6" :xl="20" :style="{ marginBottom: '24px' }">
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
        </a-col>
        <a-col :sm="24" :md="6" :xl="4" :style="{ marginBottom: '24px' }">
          <a-button
            type="primary"
            @click="listCapacity(zoneSelected, true)">Fetch Latest Statistics</a-button>
        </a-col>

        <a-col
          :sm="12"
          :md="12"
          :xl="6"
          :style="{ marginBottom: '28px' }"
          v-for="stat in stats"
          :key="stat.type">
          <chart-card :loading="loading" style="padding-top: 40px">
            <div style="text-align: center">
              <h4>{{ stat.name }}</h4>
              <a-progress type="dashboard" :percent="parseFloat(stat.percentused, 10)" :width="100" />
            </div>
            <template slot="footer"><span>{{ stat.capacityused }} / {{ stat.capacitytotal }}</span></template>
          </chart-card>
        </a-col>
      </a-col>
      <a-col :xl="6">
        <chart-card style="margin-bottom: 24px">
          <div style="text-align: center">
            <h4>Alerts</h4>
            <p>Some event data here...</p>
          </div>
        </chart-card>
        <chart-card style="margin-bottom: 24px">
          <div style="text-align: center">
            <h4>Host Alerts</h4>
            <p>Some event data here...</p>
          </div>
        </chart-card>
        <chart-card style="margin-bottom: 24px">
          <div style="text-align: center">
            <h4>Events</h4>
            <p>Some event data here...</p>
          </div>
        </chart-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'

import ChartCard from '@/components/chart/ChartCard'
import ACol from 'ant-design-vue/es/grid/Col'
import ATooltip from 'ant-design-vue/es/tooltip/Tooltip'
import MiniArea from '@/components/chart/MiniArea'
import MiniBar from '@/components/chart/MiniBar'
import MiniProgress from '@/components/chart/MiniProgress'
import Bar from '@/components/chart/Bar'
import Trend from '@/components/Trend'

export default {
  name: 'CapacityDashboard',
  components: {
    ATooltip,
    ACol,
    ChartCard,
    MiniArea,
    MiniBar,
    MiniProgress,
    Bar,
    Trend
  },
  data () {
    return {
      loading: true,
      zones: [],
      zoneSelected: {},
      stats: []
    }
  },
  mounted () {
    this.listZones()
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'dashboard') {
        this.listZones()
      }
    }
  },
  created () {
    setTimeout(() => {
      // to do after initial timeout
    }, 1000)
  },
  methods: {
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
</style>
