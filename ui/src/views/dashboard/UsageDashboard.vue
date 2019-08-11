<template>
  <div class="page-header-index-wide">
    <a-row :gutter="12">
      <a-col
        :xl="16"
        :style="{ marginTop: '24px' }">
        <a-row :gutter="12">
          <a-col
            :xl="12"
            v-for="stat in stats"
            :key="stat.type"
            :style="{ marginBottom: '12px' }">
            <chart-card :loading="loading">
              <router-link :to="{ name: stat.path }">
                <div style="text-align: center">
                  <h4>{{ stat.name }}</h4>
                  <h1>{{ stat.count == undefined ? 0 : stat.count }}</h1>
                </div>
              </router-link>
            </chart-card>
          </a-col>
        </a-row>
      </a-col>
      <a-col
        :xl="8"
        :style="{ marginTop: '24px' }">
        <chart-card>
          <div style="text-align: center">
            <a-button><router-link :to="{ name: 'event' }">View Events</router-link></a-button>
          </div>
          <template slot="footer">
            <div :style="{ paddingTop: '12px', paddingLeft: '3px', whiteSpace: 'normal' }">
              <a-timeline pending="...">
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
  </div>
</template>

<script>
import { api } from '@/api'

import ChartCard from '@/components/chart/ChartCard'
import ACol from 'ant-design-vue/es/grid/Col'

export default {
  name: 'UsageDashboard',
  components: {
    ACol,
    ChartCard
  },
  data () {
    return {
      loading: false,
      events: [],
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
        this.stats.push({ name: 'Running VMs', count: count, path: 'vm' })
      })
      api('listVirtualMachines', { state: 'Stopped', listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.push({ name: 'Stopped VMs', count: count, path: 'vm' })
      })
      api('listVirtualMachines', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvirtualmachinesresponse) {
          count = json.listvirtualmachinesresponse.count
        }
        this.stats.push({ name: 'Total VMs', count: count, path: 'vm' })
      })
      api('listVolumes', { listall: true }).then(json => {
        var count = 0
        if (json && json.listvolumesresponse) {
          count = json.listvolumesresponse.count
        }
        this.stats.push({ name: 'Total Volumes', count: count, path: 'volume' })
      })
      api('listNetworks', { listall: true }).then(json => {
        var count = 0
        if (json && json.listnetworksresponse) {
          count = json.listnetworksresponse.count
        }
        this.stats.push({ name: 'Total Networks', count: count, path: 'guestnetwork' })
      })
      api('listPublicIpAddresses', { listall: true }).then(json => {
        var count = 0
        if (json && json.listpublicipaddressesresponse) {
          count = json.listpublicipaddressesresponse.count
        }
        this.stats.push({ name: 'Public IP Addresses', count: count, path: 'publicip' })
      })
      this.listEvents()
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
