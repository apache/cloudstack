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
    <a-tabs :animated="false" tabPosition="left">
      <a-tab-pane tab="Private Gateways" key="pgw" v-if="'listPrivateGateways' in $store.getters.apis">
        <a-button type="dashed" icon="plus" style="width: 100%">Add Private Gateway</a-button>
      </a-tab-pane>
      <a-tab-pane tab="Public IP Addresses" key="ip" v-if="'listPublicIpAddresses' in $store.getters.apis">
        <a-button type="dashed" icon="plus" style="width: 100%">Acquire New IP</a-button>
      </a-tab-pane>
      <a-tab-pane tab="S2S VPN Gateway" key="vpngw" v-if="'listVpnGateways' in $store.getters.apis">
        <a-button type="dashed" icon="plus" style="width: 100%">Create Site-to-Site VPN Gateway</a-button>
      </a-tab-pane>
      <a-tab-pane tab="S2S VPN Connection" key="vpnc" v-if="'listVpnConnections' in $store.getters.apis">
        <a-button type="dashed" icon="plus" style="width: 100%">Create Site-to-Site VPN Connection</a-button>
      </a-tab-pane>
      <a-tab-pane tab="Network ACL Lists" key="acl" v-if="'listNetworkACLLists' in $store.getters.apis">
        <a-button type="dashed" icon="plus" style="width: 100%">Add Network ACL List</a-button>
      </a-tab-pane>
      <a-tab-pane tab="Virtual Routers" key="vr" v-if="'listRouters' in $store.getters.apis">
        {{ routers }}
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'VpcRouterTab',
  components: {
    Status
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
      routers: [],
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
      api('listRouters', { vpcid: this.resource.id, listAll: true }).then(json => {
        this.routers = json.listroutersresponse.router
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>

</style>
