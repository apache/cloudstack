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
    <a-collapse v-model="activeKey">
      <a-collapse-panel :header="'ISO: ' + vm.isoname" v-if="vm.isoid" key="1">
        <a-list
          itemLayout="horizontal">
          <a-list-item>
            <a-list-item-meta :description="vm.isoid">
              <a slot="title" href="">
                <router-link :to="{ path: '/iso/' + vm.isoid }">{{ vm.isoname }}</router-link>
              </a> ({{ vm.isoname }})
              <a-avatar slot="avatar">
                <a-icon type="usb" />
              </a-avatar>
            </a-list-item-meta>
          </a-list-item>
        </a-list>
      </a-collapse-panel>

      <a-collapse-panel :header="'Disks: ' + volumes.length" key="2">
        <a-list
          size="small"
          itemLayout="horizontal"
          :dataSource="volumes"
        >
          <a-list-item slot="renderItem" slot-scope="item">
            <a-list-item-meta>
              <div slot="title">
                <router-link :to="{ path: '/volume/' + item.id }">{{ item.name }}</router-link> ({{ item.type }}) <br/>
                <status :text="item.state" displayText /><br/>
              </div>
              <div slot="description">
                <a-icon type="barcode"/> {{ item.id }}
              </div>
              <a-avatar slot="avatar">
                <a-icon type="hdd" />
              </a-avatar>
            </a-list-item-meta>
            <p>
              Size: {{ (item.size / (1024 * 1024 * 1024.0)).toFixed(4) }} GB<br/>
              Physical Size: {{ (item.physicalsize / (1024 * 1024 * 1024.0)).toFixed(4) }} GB<br/>
              Provisioning: {{ item.provisioningtype }}<br/>
              Storage Pool: {{ item.storage }} ({{ item.storagetype }})<br/>
            </p>
          </a-list-item>
        </a-list>

      </a-collapse-panel>
      <a-collapse-panel :header="'Network Adapter(s): ' + (vm && vm.nic ? vm.nic.length : 0)" key="3" >
        <a-list
          size="small"
          itemLayout="horizontal"
          :dataSource="vm.nic"
        >
          <a-list-item slot="renderItem" slot-scope="item">
            <a-list-item-meta>
              <div slot="title">
                <span v-show="item.isdefault">(Default) </span>
                <router-link :to="{ path: '/guestnetwork/' + item.networkid }">{{ item.networkname }} </router-link><br/>
                Mac Address: {{ item.macaddress }}<br/>
                <span v-if="item.ipaddress">Address: {{ item.ipaddress }} <br/></span>
                Netmask: {{ item.netmask }}<br/>
                Gateway: {{ item.gateway }}<br/>
              </div>
              <div slot="description">
                <a-icon type="barcode"/> {{ item.id }}
              </div>
              <a-avatar slot="avatar">
                <a-icon type="wifi" />
              </a-avatar>
            </a-list-item-meta>
            <p>
              Type: {{ item.type }}<br/>
              Broadcast URI: {{ item.broadcasturi }}<br/>
              Isolation URI: {{ item.isolationuri }}<br/>
            </p>
          </a-list-item>
        </a-list>
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>

<script>

import { api } from '@/api'
import ResourceLayout from '@/layouts/ResourceLayout'
import Status from '@/components/widgets/Status'

export default {
  name: 'InstanceHardware',
  components: {
    ResourceLayout,
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
      vm: {},
      volumes: [],
      totalStorage: 0,
      activeKey: ['1', '2', '3']
    }
  },
  created () {
    this.vm = this.resource
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.vm = newItem
      if (newItem.id === oldItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.volumes = []
      if (!this.vm || !this.vm.id) {
        return
      }
      api('listVolumes', { listall: true, virtualmachineid: this.vm.id }).then(json => {
        this.volumes = json.listvolumesresponse.volume
        if (this.volumes) {
          this.volumes.sort((a, b) => { return a.deviceid - b.deviceid })
        }
        this.$set(this.resource, 'volumes', this.volumes)
      })
    }
  }
}
</script>

<style lang="less" scoped>
.page-header-wrapper-grid-content-main {
  width: 100%;
  height: 100%;
  min-height: 100%;
  transition: 0.3s;
  .vm-detail {
    .svg-inline--fa {
      margin-left: -1px;
      margin-right: 8px;
    }
    span {
      margin-left: 10px;
    }
    margin-bottom: 8px;
  }
}
</style>
