<template>
  <a-row :gutter="24" style="margin: 6px">
    <a-col
      :md="6"
      :style="{ marginBottom: '12px', marginTop: '12px' }"
      v-for="(section, index) in sections"
      v-if="routes[section]"
      :key="index">
      <chart-card :loading="loading">
        <div class="chart-card-inner">
          <h2>{{ $t(routes[section].title) }}</h2>
          <h1><a-icon :type="routes[section].icon" /> {{ stats[section] }}</h1>
        </div>
        <template slot="footer">
          <center>
            <router-link :to="{ name: section.substring(0, section.length - 1) }">
              <a-button style="margin-bottom: 3px">View {{ $t(routes[section].title) }}</a-button>
            </router-link>
          </center>
        </template>
      </chart-card>
    </a-col>
    <!-- move refresh and ssl cert setup somewhere more friendly -->
    <a-col
      :md="6"
      :style="{ marginBottom: '12px', marginTop: '12px' }">
      <a-card>
        <a-button
          style="margin-right: 20px"
          icon="safety-certificate">
          {{ $t('SSL Certificate') }}
        </a-button>
        <a-button
          @click="fetchData()"
          :loading="loading"
          type="primary"
          icon="reload">
          {{ $t('Refresh') }}
        </a-button>
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
import { api } from '@/api'
import router from '@/router'

import ChartCard from '@/components/chart/ChartCard'

export default {
  name: 'InfraSummary',
  components: {
    ChartCard
  },
  data () {
    return {
      loading: true,
      sections: ['zones', 'pods', 'clusters', 'hosts', 'storagepools', 'imagestores', 'systemvms', 'routers', 'cpusockets', 'managementservers'],
      routes: {},
      stats: {}
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.routes = {}
      for (const section of this.sections) {
        const node = router.resolve({ name: section.substring(0, section.length - 1) })
        this.routes[section] = {
          title: node.route.meta.title,
          icon: node.route.meta.icon
        }
      }
      this.listInfra()
    },
    listInfra () {
      this.loading = true
      api('listInfrastructure').then(json => {
        this.stats = []
        if (json && json.listinfrastructureresponse && json.listinfrastructureresponse.infrastructure) {
          this.stats = json.listinfrastructureresponse.infrastructure
        }
      }).finally(f => {
        this.loading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
.chart-card-inner {
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
}
</style>
