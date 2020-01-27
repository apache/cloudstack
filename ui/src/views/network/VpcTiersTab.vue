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
    <div v-for="(network, index) in networks" :key="index">
      <router-link :to="{ path: '/guestnetwork/' + network.id }">{{ network.name }}</router-link>
      {{ network.cidr }}
      <router-link :to="{ path: '/vm/?vpcid=' + resource.id + '&networkid=' + network.id }">View VMs</router-link>
      <router-link :to="{ path: '/publicip/?isstaticnat=true' + '&associatednetworkid=' + network.id }">View SNATs</router-link>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'VpcTiersTab',
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
      networks: [],
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
      api('listNetworks', { vpcid: this.resource.id }).then(json => {
        this.networks = json.listnetworksresponse.network
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
