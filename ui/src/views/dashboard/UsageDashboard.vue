<template>
  <div class="page-header-index-wide">
    <a-row :gutter="24">
      <a-col
        :sm="12"
        :md="12"
        :xl="6"
        :style="{ marginTop: '24px' }"
        v-for="stat in stats"
        :key="stat.type">
        <chart-card :loading="loading" style="padding-top: 24px">
          <div style="text-align: center">
            <h4>{{ stat.name }}</h4>
            <h1>{{ stat.count == undefined ? 0 : stat.count }}</h1>
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
  name: 'UsageDashboard',
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
      loading: false,
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
  methods: {
    fetchData () {
      this.stats = []
      api('listVirtualMachines', { state: 'Running', listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.push({ name: 'Running VMs', count: count })
      })
      api('listVirtualMachines', { state: 'Stopped', listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.push({ name: 'Stopped VMs', count: count })
      })
      api('listVirtualMachines', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listnetworksresponse.count
        }
        this.stats.push({ name: 'Total VMs', count: count })
      })
      api('listNetworks', { listall: true }).then(json => {
        var count = 0
        if (json && json.listnetworksresponse) {
          count = json.listnetworksresponse.count
        }
        this.stats.push({ name: 'Isolated Networks', count: count })
      })
      api('listPublicIpAddresses', { listall: true }).then(json => {
        var count = 0
        if (json && json.listpublicipaddressesresponse) {
          count = json.listpublicipaddressesresponse.count
        }
        this.stats.push({ name: 'Public IP Addresses', count: count })
      })
      api('listEvents', { listall: true }).then(json => {
        var count = 0
        if (json && json.listeventsresponse) {
          count = json.listeventsresponse.count
        }
        this.stats.push({ name: 'Events', count: count })
      })
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
