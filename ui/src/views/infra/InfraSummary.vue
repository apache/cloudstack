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
  <a-row :gutter="24">
    <a-col :md="18">
      <a-card>
      <breadcrumb />
      </a-card>
    </a-col>
    <a-col
      :md="6" >
      <a-card>
        <a-button
          style="margin-left: 10px; float: right"
          @click="fetchData()"
          icon="reload"
          :loading="loading"
          type="primary">
          {{ $t('Refresh') }}
        </a-button>
        <a-button
          style="margin-left: 10px; float: right"
          @click="sslFormVisible = true"
          icon="safety-certificate">
          {{ $t('SSL Certificate') }}
        </a-button>
        <a-modal
          :title="$t('SSL Certificate')"
          v-model="sslFormVisible"
          @ok="handle">
          <p>Some contents...</p>
          <p>Some contents...</p>
          <p>Some contents...</p>
        </a-modal>
      </a-card>
    </a-col>
    <a-col
      :md="6"
      :style="{ marginBottom: '12px', marginTop: '12px' }"
      v-for="(section, index) in sections"
      v-if="routes[section]"
      :key="index">
      <chart-card :loading="loading">
        <div class="chart-card-inner">
          <router-link :to="{ name: section.substring(0, section.length - 1) }">
            <h2>{{ $t(routes[section].title) }}</h2>
            <h1><a-icon :type="routes[section].icon" /> {{ stats[section] }}</h1>
          </router-link>
        </div>
      </chart-card>
    </a-col>
  </a-row>
</template>

<script>
import { api } from '@/api'
import router from '@/router'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'InfraSummary',
  components: {
    Breadcrumb,
    ChartCard
  },
  data () {
    return {
      loading: true,
      routes: {},
      sections: ['zones', 'pods', 'clusters', 'hosts', 'storagepools', 'imagestores', 'systemvms', 'routers', 'cpusockets', 'managementservers', 'alerts'],
      sslFormVisible: false,
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
    },
    handleSslForm (e) {
      console.log(e)
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
