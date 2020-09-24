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
  <div>
    <autogen-view @change-resource="changeResource"/>
    <resource-view
      v-if="isPublicIpAddress && 'id' in resource"
      :loading="loading"
      :resource="resource"
      :tabs="tabs" />
  </div>
</template>

<script>
import AutogenView from '@/views/AutogenView.vue'
import { api } from '@api'
import ResourceView from '@/components/view/ResourceView'

export default {
  name: 'PublicIpResource',
  components: {
    AutogenView,
    ResourceView
  },
  data () {
    return {
      loading: false,
      resource: {},
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }]
    }
  },
  provide: function () {
    return {
      parentFetchData: this.fetchData(),
      parentToggleLoading: this.toggleLoading
    }
  },
  computed: {
    isPublicIpAddress () {
      return this.$route.path.startsWith('/publicip') && this.$route.path.includes('/publicip/')
    }
  },
  watch: {
    resource () {
      if ('id' in this.resource) {
        this.fetchData()
      }
    }
  },
  mounted () {
    if ('id' in this.resource) {
      this.fetchData()
    }
  },
  methods: {
    async fetchData () {
      if (Object.keys(this.resource).length === 0) {
        return
      }
      this.loading = true
      this.portFWRuleCount = await this.fetchPortFWRule()
      if (this.portFWRuleCount > 0) {
        this.tabs = this.$route.meta.tabs.filter(tab => tab.name !== 'loadbalancing')
        this.loading = false
        return
      }
      this.loadBalancerRuleCount = await this.fetchLoadBalancerRule()
      if (this.loadBalancerRuleCount > 0) {
        this.tabs = this.$route.meta.tabs.filter(tab => tab.name !== 'portforwarding')
        this.loading = false
        return
      }

      this.tabs = this.$route.meta.tabs
      this.loading = false
    },
    fetchPortFWRule () {
      return new Promise((resolve, reject) => {
        api('listPortForwardingRules', {
          listAll: true,
          ipaddressid: this.resource.id,
          page: 1,
          pagesize: 1
        }).then(json => {
          const portFWRuleCount = json.listportforwardingrulesresponse.count || 0
          resolve(portFWRuleCount)
        }).catch(e => {
          reject(e)
        })
      })
    },
    fetchLoadBalancerRule () {
      return new Promise((resolve, reject) => {
        api('listLoadBalancerRules', {
          listAll: true,
          publicipid: this.resource.id,
          page: 1,
          pagesize: 1
        }).then(json => {
          const loadBalancerRuleCount = json.listloadbalancerrulesresponse.count || 0
          resolve(loadBalancerRuleCount)
        }).catch(e => {
          reject(e)
        })
      })
    },
    changeResource (resource) {
      this.resource = resource
    },
    toggleLoading () {
      this.loading = !this.loading
    }
  }
}
</script>
