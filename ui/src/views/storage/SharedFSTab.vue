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
  <a-spin :spinning="loading">
    <a-tabs
      :activeKey="currentTab"
      :tabPosition="device === 'mobile' ? 'top' : 'left'"
      :animated="false"
      @change="handleChangeTab">
      <a-tab-pane :tab="$t('label.details')" key="details">
        <DetailsTab :resource="dataResource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.access')" key="access">
        <h3>{{ $t('label.mount.sharedfs') }}</h3>
        <div v-for="(nic, index) in resource.nic" :key="index" class="content">
          <a-card>
            <template #title>
              <router-link :to="{ path: '/guestnetwork/' + nic.networkid }">
                {{ nic.networkname }}
              </router-link>
            </template>
            <timeline>
              <a-timeline-item color="blue">
                <h3>mount -t nfs {{ nic.ipaddress }}:{{ resource.path }} [local_mount_path]</h3>
                <p class="info"><i>(Mount the NFS share. Additional mount options can be given as required.)</i></p>
              </a-timeline-item>
              <a-timeline-item color="blue">
                <h3>showmount -e {{ nic.ipaddress }}</h3>
                <p class="info"><i>(Check the status of the NFS server using the showmount command.)</i></p>
              </a-timeline-item>
            </timeline>
          </a-card>
        </div>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.networks')" key="nics" v-if="'listNics' in $store.getters.apis">
        <NicsTab :resource="vm"/>
      </a-tab-pane>
      <a-tab-pane v-if="$store.getters.features.instancesdisksstatsretentionenabled" :tab="$t('label.volume.metrics')" key="volumestats">
        <StatsTab :resource="volume" :resourceType="'Volume'"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.metrics')" key="vmstats">
        <StatsTab :resource="vm"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.events')" key="events" v-if="'listEvents' in $store.getters.apis">
        <events-tab :resource="resource" resourceType="SharedFS" :loading="loading" />
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>

import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import Status from '@/components/widgets/Status'
import DetailsTab from '@/components/view/DetailsTab'
import StatsTab from '@/components/view/StatsTab'
import EventsTab from '@/components/view/EventsTab'
import NicsTab from '@/views/network/NicsTab.vue'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'SharedFSTab',
  components: {
    DetailsTab,
    StatsTab,
    EventsTab,
    NicsTab,
    TooltipButton,
    Status
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
  inject: ['parentFetchData'],
  data () {
    return {
      vm: {},
      virtualmachines: [],
      currentTab: 'details',
      dataResource: {}
    }
  },
  created () {
    const self = this
    this.dataResource = this.resource
    this.fetchData()
    window.addEventListener('popstate', function () {
      self.setCurrentTab()
    })
  },
  watch: {
    resource: {
      deep: true,
      handler (newData, oldData) {
        if (newData !== oldData) {
          this.dataResource = newData
          this.fetchData()
        }
      }
    },
    '$route.fullPath': function () {
      this.setCurrentTab()
    }
  },
  mounted () {
    this.setCurrentTab()
  },
  methods: {
    setCurrentTab () {
      this.currentTab = this.$route.query.tab ? this.$route.query.tab : 'details'
    },
    handleChangeTab (e) {
      this.currentTab = e
      const query = Object.assign({}, this.$route.query)
      query.tab = e
      history.pushState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
    },
    fetchInstances () {
      if (!this.resource.virtualmachineid) {
        return
      }
      this.instanceLoading = true
      var params = {
        id: this.resource.virtualmachineid,
        listall: true
      }
      if (this.$store.getters.listAllProjects) {
        params.projectid = '-1'
      }
      api('listVirtualMachines', params).then(json => {
        this.virtualmachines = json.listvirtualmachinesresponse.virtualmachine || []
        this.vm = this.virtualmachines[0]
      })
      this.instanceLoading = false
    },
    fetchVolumes () {
      if (!this.resource.volumeid) {
        return
      }
      this.volumeLoading = true
      var params = {
        id: this.resource.volumeid,
        listsystemvms: 'true',
        listall: true
      }
      api('listVolumes', params).then(json => {
        this.volumes = json.listvolumesresponse.volume || []
        this.volume = this.volumes[0]
      })
      this.volumeLoading = false
    },
    fetchData () {
      this.fetchInstances()
      this.fetchVolumes()
    }
  }
}
</script>

<style lang="scss" scoped>
  .page-header-wrapper-grid-content-main {
    width: 100%;
    height: 100%;
    min-height: 100%;
    transition: 0.3s;
  }
  .info {
    font-size: 0.8rem;
  }
</style>
