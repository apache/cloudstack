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
  <a-spin :spinning="fetchLoading">
    <a-tabs :tabPosition="device === 'mobile' ? 'top' : 'left'" :animated="false">
      <a-tab-pane v-for="(item, index) in traffictypes" :tab="item.traffictype" :key="index">
        <div
          v-for="(type, idx) in ['kvmnetworklabel', 'vmwarenetworklabel', 'xennetworklabel', 'hypervnetworklabel', 'ovm3networklabel']"
          :key="idx"
          style="margin-bottom: 10px;">
          <div><strong>{{ $t(type) }}</strong></div>
          <div>{{ item[type] || 'Use default gateway' }}</div>
        </div>
        <div v-if="item.traffictype === 'Public'">
          <div style="margin-bottom: 10px;">
            <div><strong>{{ $t('traffictype') }}</strong></div>
            <div>{{ publicNetwork.traffictype }}</div>
          </div>
          <div style="margin-bottom: 10px;">
            <div><strong>{{ $t('broadcastdomaintype') }}</strong></div>
            <div>{{ publicNetwork.broadcastdomaintype }}</div>
          </div>
          <a-divider />
          <IpRangesTabPublic :resource="resource" :loading="loading" :network="publicNetwork" />
        </div>
        <div v-if="item.traffictype === 'Management'">
          <a-divider />
          <IpRangesTabManagement :resource="resource" :loading="loading" />
        </div>
        <div v-if="item.traffictype === 'Storage'">
          <a-divider />
          <IpRangesTabStorage :resource="resource" />
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import IpRangesTabPublic from './IpRangesTabPublic'
import IpRangesTabManagement from './IpRangesTabManagement'
import IpRangesTabStorage from './IpRangesTabStorage'

export default {
  name: 'TrafficTypesTab',
  components: {
    IpRangesTabPublic,
    IpRangesTabManagement,
    IpRangesTabStorage
  },
  mixins: [mixinDevice],
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
      traffictypes: [],
      publicNetwork: {},
      fetchLoading: false
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchLoading = true
      api('listTrafficTypes', { physicalnetworkid: this.resource.id }).then(json => {
        this.traffictypes = json.listtraffictypesresponse.traffictype
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })

      this.fetchLoading = true
      api('listNetworks', {
        listAll: true,
        trafficType: 'Public',
        isSystem: true,
        zoneId: this.resource.zoneid
      }).then(json => {
        this.publicNetwork = json.listnetworksresponse.network[0] || {}
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>

</style>
