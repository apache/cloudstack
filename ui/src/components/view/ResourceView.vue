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
  <resource-layout>
    <div slot="left">
      <slot name="info-card">
        <info-card :resource="resource" :loading="loading" />
      </slot>
    </div>
    <a-spin :spinning="loading" slot="right">
      <a-card
        class="spin-content"
        :bordered="true"
        style="width:100%">
        <component
          v-if="tabs.length === 1"
          :is="tabs[0].component"
          :resource="resource"
          :loading="loading"
          :tab="tabs[0].name" />
        <a-tabs
          v-else
          style="width: 100%"
          :animated="false"
          :activeKey="activeTab || tabs[0].name"
          @change="onTabChange" >
          <a-tab-pane
            v-for="tab in tabs"
            :tab="$t('label.' + tab.name)"
            :key="tab.name"
            v-if="showTab(tab)">
            <component :is="tab.component" :resource="resource" :loading="loading" :tab="activeTab" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-spin>
  </resource-layout>
</template>

<script>
import DetailsTab from '@/components/view/DetailsTab'
import InfoCard from '@/components/view/InfoCard'
import ResourceLayout from '@/layouts/ResourceLayout'
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'

export default {
  name: 'ResourceView',
  components: {
    InfoCard,
    ResourceLayout
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
    },
    tabs: {
      type: Array,
      default: function () {
        return [{
          name: 'details',
          component: DetailsTab
        }]
      }
    },
    historyTab: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      activeTab: '',
      networkService: null,
      projectAccount: null
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
      if (newItem.id === oldItem.id) return

      if (this.resource.associatednetworkid) {
        api('listNetworks', { id: this.resource.associatednetworkid, listall: true }).then(response => {
          if (response && response.listnetworksresponse && response.listnetworksresponse.network) {
            this.networkService = response.listnetworksresponse.network[0]
          } else {
            this.networkService = {}
          }
        })
      }
    },
    $route: function (newItem, oldItem) {
      this.setActiveTab()
    }
  },
  mounted () {
    this.setActiveTab()
  },
  methods: {
    onTabChange (key) {
      this.activeTab = key
      const query = Object.assign({}, this.$route.query)
      query.tab = key
      history.replaceState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
      this.$emit('onTabChange', key)
    },
    showTab (tab) {
      if ('networkServiceFilter' in tab) {
        if (this.resource && this.resource.virtualmachineid && !this.resource.vpcid && tab.name !== 'firewall') {
          return false
        }
        if (this.resource && this.resource.virtualmachineid && this.resource.vpcid) {
          return false
        }
        // dont display any option for source NAT IP of VPC
        if (this.resource && this.resource.vpcid && !this.resource.issourcenat && tab.name !== 'firewall') {
          return true
        }
        // display LB and PF options for isolated networks if static nat is disabled
        if (this.resource && !this.resource.vpcid) {
          if (!this.resource.isstaticnat) {
            return true
          } else if (tab.name === 'firewall') {
            return true
          }
        }
        return this.networkService && this.networkService.service &&
          tab.networkServiceFilter(this.networkService.service)
      } else if ('show' in tab) {
        return tab.show(this.resource, this.$route, this.$store.getters.userInfo)
      } else {
        return true
      }
    },
    setActiveTab () {
      if (this.$route.query.tab) {
        this.activeTab = this.$route.query.tab
        return
      }
      if (!this.historyTab || !this.$route.meta.tabs || this.$route.meta.tabs.length === 0) {
        this.activeTab = this.tabs[0].name
        return
      }
      const tabIdx = this.$route.meta.tabs.findIndex(tab => tab.name === this.historyTab)
      if (tabIdx === -1) {
        this.activeTab = this.tabs[0].name
      } else {
        this.activeTab = this.historyTab
      }
    }
  }
}
</script>

<style lang="less" scoped>
</style>
