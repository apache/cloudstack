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
      <div slot="action">
        <action-button
          :style="{ float: device === 'mobile' ? 'left' : 'right' }"
          :loading="loading"
          :actions="actions"
          :selectedRowKeys="selectedRowKeys"
          :dataView="true"
          :resource="resource"
          @exec-action="(action) => execAction(action, action.groupAction && !dataView)" />
      </div>
      <div slot="resource">
        <resource-view
          v-if="isPublicIpAddress && 'id' in resource"
          :loading="loading"
          :resource="resource"
          :historyTab="activeTab"
          :tabs="tabs"
          @onTabChange="(tab) => { this.activeTab = tab }" />
      </div>
    </autogen-view>
  </div>
</template>

<script>
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
        component: () => import('@/components/view/DetailsTab.vue')
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
    resource () {
      if ('id' in this.resource) {
        this.fetchData()
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
      this.portFWRuleCount = await this.fetchPortFWRule()

      // disable load balancing rules only if port forwarding is enabled and
      // network belongs to VPC
      if (this.portFWRuleCount > 0 && this.resource.vpcid) {
        this.tabs = this.$route.meta.tabs.filter(tab => tab.name !== 'loadbalancing')
      } else {
        this.loadBalancerRuleCount = await this.fetchLoadBalancerRule()

        // for isolated networks, display both LB and PF
        // for VPC they are mutually exclusive
        if (this.loadBalancerRuleCount > 0) {
          this.tabs =
            this.resource.vpcid ? this.$route.meta.tabs.filter(tab => tab.name !== 'portforwarding') : this.$route.meta.tabs
          this.loading = false
        } else {
          this.tabs = this.$route.meta.tabs
        }
      }

      await this.fetchAction()
      this.loading = false
    },
    fetchAction () {
      this.actions = []
      if (this.$route.meta.actions) {
        this.actions = this.$route.meta.actions
      }

      if (this.portFWRuleCount > 0 || this.loadBalancerRuleCount > 0) {
        this.actions = this.actions.filter(action => action.api !== 'enableStaticNat')
      }
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
    },
    execAction (action, isGroupAction) {
      eventBus.$emit('exec-action', action, isGroupAction)
    }
  }
}
</script>
