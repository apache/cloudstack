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
          :defaultActiveKey="tabs[0].name"
          @change="onTabChange" >
          <a-tab-pane
            v-for="tab in tabs"
            :tab="$t(tab.name)"
            :key="tab.name"
            v-if="showHideTab(tab)">
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

export default {
  name: 'ResourceView',
  components: {
    InfoCard,
    ResourceLayout
  },
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
    }
  },
  data () {
    return {
      activeTab: '',
      networkService: null,
      vpnEnabled: false
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
      if (newItem.id === oldItem.id) return

      if (this.resource.associatednetworkid) {
        api('listNetworks', { id: this.resource.associatednetworkid }).then(response => {
          this.networkService = response.listnetworksresponse.network[0]
        })
      }

      if (this.resource.id && this.resource.ipaddress) {
        api('listRemoteAccessVpns', {
          publicipid: this.resource.id,
          listAll: true
        }).then(response => {
          this.vpnEnabled = response.listremoteaccessvpnsresponse.remoteaccessvpn && response.listremoteaccessvpnsresponse.remoteaccessvpn.length > 0
        })
      }
    }
  },
  methods: {
    onTabChange (key) {
      this.activeTab = key
    },
    showHideTab (tab) {
      if ('networkServiceFilter' in tab) {
        if (this.resource.virtualmachineid && tab.name !== 'Firewall') return false
        return this.networkService && this.networkService.service &&
          tab.networkServiceFilter(this.networkService.service)
      } else if ('show' in tab) {
        return tab.show(this.resource, this.$route, this.$store.getters.userInfo)
      } else {
        return true
      }
    }
  }
}
</script>

<style lang="less" scoped>
</style>
