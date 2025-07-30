
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-table
    :loading="loading"
    :columns="summaryColumns"
    :dataSource="summaryData"
    :pagination="false"
    :rowKey="record => record.key"
    :childrenColumnName="'children'"
    :defaultExpandAllRows="true"
    :expandedRowKeys="expandedRowKeys"
    @expand="onExpand"
    size="small"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        <span
          :style="{ paddingLeft: record.isProfile ? '20px' : '0px', fontWeight: record.isProfile ? 'normal' : 'bold' }"
        >
          <router-link
            v-if="record.isProfile && record.vgpuprofileid"
            :to="{ path: '/vgpuprofile/' + record.vgpuprofileid }"
            :title="record.name"
            class="text-ellipsis"
          >
            {{ record.name }}
          </router-link>
          <router-link
            v-else-if="!record.isProfile && record.gpucardid"
            :to="{ path: '/gpucard/' + record.gpucardid }"
            :title="record.name"
            class="text-ellipsis"
          >
            {{ record.name }}
          </router-link>
          <span
            v-else
            :title="record.name"
            class="text-ellipsis"
          >{{ record.name }}</span>
        </span>
      </template>
    </template>
  </a-table>
</template>

<script>
import { getAPI } from '@/api'

export default {
  name: 'GPUSummaryTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      required: true,
      default: 'Host'
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      summaryColumns: [],
      summaryData: [],
      expandedRowKeys: []
    }
  },
  created () {
    this.updateSummaryColumns()
    this.fetchSummaryData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchSummaryData()
      }
    }
  },
  methods: {
    fetchSummaryData () {
      if (!this.resource.id) {
        return
      }
      // Reset expanded keys when fetching new data
      this.expandedRowKeys = []

      const params = {}
      if (this.resourceType === 'Host') {
        params.hostid = this.resource.id
      } else if (this.resourceType === 'VirtualMachine') {
        params.virtualmachineid = this.resource.id
      }

      getAPI('listGpuDevices', params).then(json => {
        const devices = json?.listgpudevicesresponse?.gpudevice || []
        this.summaryData = this.buildSummaryData(devices)
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    onExpand (expanded, record) {
      if (expanded) {
        if (!this.expandedRowKeys.includes(record.key)) {
          this.expandedRowKeys.push(record.key)
        }
      } else {
        this.expandedRowKeys = this.expandedRowKeys.filter(key => key !== record.key)
      }
    },
    buildSummaryData (devices) {
      // Group devices by GPU card
      const cardGroups = {}

      devices.forEach(device => {
        const cardKey = device.gpucardname || 'Unknown GPU Card'
        if (!cardGroups[cardKey]) {
          cardGroups[cardKey] = {
            gpucardname: cardKey,
            gpucardid: device.gpucardid,
            profiles: {},
            devices: []
          }
        }
        cardGroups[cardKey].devices.push(device)

        // Group by vGPU profile within each card
        if (device.vgpuprofilename) {
          const profileKey = device.vgpuprofilename
          if (!cardGroups[cardKey].profiles[profileKey]) {
            cardGroups[cardKey].profiles[profileKey] = {
              vgpuprofilename: profileKey,
              vgpuprofileid: device.vgpuprofileid,
              devices: []
            }
          }
          cardGroups[cardKey].profiles[profileKey].devices.push(device)
        }
      })

      // Build summary tree structure and collect expanded keys
      const summaryTree = []
      const expandedKeys = []

      Object.values(cardGroups).forEach(cardGroup => {
        const profileCount = Object.keys(cardGroup.profiles).length

        // Filter devices for card summary calculation
        // Exclude passthrough profile devices from aggregates if there are multiple profiles
        let cardDevicesForSummary = cardGroup.devices
        if (profileCount > 1) {
          cardDevicesForSummary = cardGroup.devices.filter(device => !device.vgpuprofilename || device.vgpuprofilename.toLowerCase() !== 'passthrough'
          )
        }

        const cardSummary = this.calculateSummary(cardDevicesForSummary)
        const cardKey = `card-${cardGroup.gpucardname}`

        const cardNode = {
          key: cardKey,
          name: cardGroup.gpucardname,
          gpucardid: cardGroup.gpucardid,
          isProfile: false,
          ...cardSummary
        }

        // If there's more than 1 profile, show profiles as children
        if (profileCount > 1) {
          // Add this card to expanded keys since it has children
          expandedKeys.push(cardKey)

          cardNode.children = Object.values(cardGroup.profiles)
            .filter(profile => profile.vgpuprofilename.toLowerCase() !== 'passthrough')
            .map(profile => {
              const profileSummary = this.calculateSummary(profile.devices)
              return {
                key: `profile-${profile.vgpuprofilename}-${cardGroup.gpucardname}`,
                name: profile.vgpuprofilename,
                vgpuprofileid: profile.vgpuprofileid,
                isProfile: true,
                ...profileSummary
              }
            })
        }

        summaryTree.push(cardNode)
      })

      // Set expanded row keys for all cards with children
      this.expandedRowKeys = expandedKeys

      return summaryTree
    },
    calculateSummary (devices) {
      const summary = {
        total: 0,
        allocated: 0,
        available: 0,
        uniqueVMs: new Set()
      }

      devices.forEach(device => {
        summary.total++

        if (device.virtualmachineid) {
          summary.allocated++
          summary.uniqueVMs.add(device.virtualmachineid)
        } else if (device.managedstate !== 'Unmanaged' && device.state !== 'Error') {
          summary.available++
        }
      })

      return {
        total: summary.total,
        allocated: summary.allocated,
        available: summary.available,
        uniqueVMs: summary.uniqueVMs.size
      }
    },
    updateSummaryColumns () {
      this.summaryColumns = [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name',
          sorter: (a, b) => { return (a.name || '').localeCompare(b.name || '') }
        },
        {
          key: 'total',
          title: this.$t('label.total'),
          dataIndex: 'total',
          align: 'center',
          sorter: (a, b) => { return a.total - b.total }
        }]
      if (this.$store.getters.userInfo.roletype === 'Admin') {
        this.summaryColumns.push(
          {
            key: 'allocated',
            title: this.$t('label.allocated'),
            dataIndex: 'allocated',
            align: 'center',
            sorter: (a, b) => { return a.allocated - b.allocated }
          },
          {
            key: 'available',
            title: this.$t('label.available'),
            dataIndex: 'available',
            align: 'center',
            sorter: (a, b) => { return a.available - b.available }
          }
        )
      }
      if (this.resourceType === 'Host') {
        this.summaryColumns.push({
          key: 'uniqueVMs',
          title: this.$t('label.vms'),
          dataIndex: 'uniqueVMs',
          align: 'center',
          sorter: (a, b) => { return a.uniqueVMs - b.uniqueVMs }
        })
      }
    },
    refresh () {
      this.fetchSummaryData()
    }
  }
}
</script>

<style scoped>
/* Text truncation for long names */
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  display: inline-block;
}
</style>
