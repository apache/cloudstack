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
    <autogen-view @change-resource="changeResource">
      <template #action>
        <action-button
          :style="{ float: device === 'mobile' ? 'left' : 'right' }"
          :loading="loading"
          :actions="actions"
          :selectedRowKeys="selectedRowKeys"
          :dataView="true"
          :resource="resource"
          @exec-action="(action) => execAction(action, action.groupAction && !dataView)" />
      </template>
      <template #resource>
        <resource-view
          v-if="isPublicIpAddress && 'id' in resource"
          :loading="loading"
          :resource="resource"
          :historyTab="activeTab"
          :tabs="tabs"
          @onTabChange="(tab) => { activeTab = tab }" />
      </template>
    </autogen-view>
  </div>
</template>

<script>
import { shallowRef, defineAsyncComponent } from 'vue'
import { api } from '@api'
import { mixinDevice } from '@/utils/mixin.js'
import eventBus from '@/config/eventBus'
import AutogenView from '@/views/AutogenView.vue'
import ResourceView from '@/components/view/ResourceView'
import ActionButton from '@/components/view/ActionButton'

export default {
  name: 'PublicIpResource',
  components: {
    AutogenView,
    ResourceView,
    ActionButton
  },
  data () {
    return {
      loading: false,
      selectedRowKeys: [],
      actions: [],
      resource: {},
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      },
      {
        name: 'events',
        resourceType: 'IpAddress',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in this.$store.getters.apis }
      }],
      defaultTabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      },
      {
        name: 'events',
        resourceType: 'IpAddress',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in this.$store.getters.apis }
      },
      {
        name: 'comments',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
      }],
      activeTab: ''
    }
  },
  mixins: [mixinDevice],
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
    resource: {
      deep: true,
      handler () {
        if ('id' in this.resource) {
          this.fetchData()
        }
      }
    }
  },
  created () {
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
      await this.filterTabs()
      await this.fetchAction()
      this.loading = false
    },
    async filterTabs () {
      // Public IPs in Free state have nothing
      if (['Free', 'Reserved'].includes(this.resource.state)) {
        this.tabs = this.defaultTabs
        return
      }
      // VPC IPs with source nat have only VPN
      if (this.resource && this.resource.vpcid && this.resource.issourcenat) {
        this.tabs = this.defaultTabs.concat(this.$route.meta.tabs.filter(tab => tab.name === 'vpn'))
        return
      }
      // VPC IPs with vpnenabled have only VPN
      if (this.resource && this.resource.vpcid && this.resource.vpnenabled) {
        this.tabs = this.defaultTabs.concat(this.$route.meta.tabs.filter(tab => tab.name === 'vpn'))
        return
      }
      // VPC IPs with static nat have nothing
      if (this.resource && this.resource.vpcid && this.resource.isstaticnat) {
        return
      }
      if (this.resource && this.resource.vpcid) {
        // VPC IPs don't have firewall
        let tabs = this.$route.meta.tabs.filter(tab => tab.name !== 'firewall')

        const network = await this.fetchNetwork()
        if (network && network.networkofferingconservemode) {
          this.tabs = tabs
          return
        }

        this.portFWRuleCount = await this.fetchPortFWRule()
        this.loadBalancerRuleCount = await this.fetchLoadBalancerRule()

        // VPC IPs with PF only have PF
        if (this.portFWRuleCount > 0) {
          tabs = tabs.filter(tab => tab.name !== 'loadbalancing')
        }

        // VPC IPs with LB rules only have LB
        if (this.loadBalancerRuleCount > 0) {
          tabs = tabs.filter(tab => tab.name !== 'portforwarding')
        }
        this.tabs = tabs
        return
      }

      // Regular guest networks with Source Nat have everything
      if (this.resource && !this.resource.vpcid && this.resource.issourcenat) {
        this.tabs = this.$route.meta.tabs
        return
      }
      // Regular guest networks with Static Nat only have Firewall
      if (this.resource && !this.resource.vpcid && this.resource.isstaticnat) {
        this.tabs = this.defaultTabs.concat(this.$route.meta.tabs.filter(tab => tab.name === 'firewall'))
        return
      }

      // Regular guest networks have all tabs
      if (this.resource && !this.resource.vpcid) {
        this.tabs = this.$route.meta.tabs
      }
    },
    fetchAction () {
      this.actions = this.$route.meta.actions || []
    },
    fetchNetwork () {
      return new Promise((resolve, reject) => {
        api('listNetworks', {
          listAll: true,
          projectid: this.resource.projectid,
          id: this.resource.associatednetworkid
        }).then(json => {
          const network = json.listnetworksresponse?.network?.[0] || null
          resolve(network)
        }).catch(e => {
          reject(e)
        })
      })
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
      console.log(resource)
      this.resource = resource
    },
    toggleLoading () {
      this.loading = !this.loading
    },
    execAction (action, isGroupAction) {
      eventBus.emit('exec-action', { action, isGroupAction })
    }
  }
}
</script>
