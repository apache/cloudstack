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
  <a-list
    size="small"
    :dataSource="$route.meta.details">
    <a-list-item slot="renderItem" slot-scope="item" v-if="item in resource">
      <div>
        <strong>{{ item === 'service' ? $t('label.supportedservices') : $t('label.' + String(item).toLowerCase()) }}</strong>
        <br/>
        <div v-if="Array.isArray(resource[item]) && item === 'service'">
          <div v-for="(service, idx) in resource[item]" :key="idx">
            {{ service.name }} : {{ service.provider[0].name }}
          </div>
        </div>
        <div v-else-if="$route.meta.name === 'backup' && item === 'volumes'">
          <div v-for="(volume, idx) in JSON.parse(resource[item])" :key="idx">
            <router-link :to="{ path: '/volume/' + volume.uuid }">{{ volume.type }} - {{ volume.path }}</router-link> ({{ parseFloat(volume.size / (1024.0 * 1024.0 * 1024.0)).toFixed(1) }} GB)
          </div>
        </div>
        <div v-else-if="['name', 'type'].includes(item)">
          <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(resource[item])">{{ $t(resource[item].toLowerCase()) }}</span>
          <span v-else>{{ resource[item] }}</span>
        </div>
        <div v-else>
          {{ resource[item] }}
        </div>
      </div>
    </a-list-item>
    <HostInfo :resource="resource" v-if="$route.meta.name === 'host' && 'listHosts' in $store.getters.apis" />
    <DedicateData :resource="resource" v-if="dedicatedSectionActive" />
    <VmwareData :resource="resource" v-if="$route.meta.name === 'zone' && 'listVmwareDcs' in $store.getters.apis" />
  </a-list>
</template>

<script>
import DedicateData from './DedicateData'
import HostInfo from '@/views/infra/HostInfo'
import VmwareData from './VmwareData'

export default {
  name: 'DetailsTab',
  components: {
    DedicateData,
    HostInfo,
    VmwareData
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      dedicatedRoutes: ['zone', 'pod', 'cluster', 'host'],
      dedicatedSectionActive: false
    }
  },
  mounted () {
    this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
  },
  created () {
    this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
  },
  watch: {
    $route () {
      this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
    }
  }
}
</script>
