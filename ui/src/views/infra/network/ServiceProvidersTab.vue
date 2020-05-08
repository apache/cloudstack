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
    <a-tabs :tabPosition="device === 'mobile' ? 'top' : 'left'" :animated="false">
      <a-tab-pane v-for="nsp in hardcodedNsps" :key="nsp">
        <span slot="tab">
          {{ nsp }}
          <status :text="nsp in nsps ? nsps[nsp].state : 'Disabled'" style="margin-bottom: 6px; margin-left: 6px" />
        </span>
        <a-list size="small" :dataSource="details">
          <a-list-item
            slot="renderItem"
            slot-scope="item">
            <div>
              <strong>{{ $t(item) }}</strong>
              <br />
              <div v-if="item === 'name'">
                <span v-if="nsp in nsps">
                  <router-link :to="{ path: '/nsp/' + nsps[nsp].id + '?name=' + nsps[nsp].name + '&physicalnetworkid=' + resource.id }">
                    {{ nsps[nsp].name }}
                  </router-link>
                </span>
                <span v-else>
                  {{ nsp }}
                </span>
              </div>
              <div v-else-if="item === 'state'">
                <status :text="nsp in nsps ? nsps[nsp].state : 'Disabled'" displayText />
              </div>
              <div v-else-if="item === 'id'">
                <span v-if="nsp in nsps"> {{ nsps[nsp].id }} </span>
              </div>
              <div v-else-if="item === 'servicelist'">
                <span v-if="nsp in nsps"> {{ nsps[nsp].servicelist.join(', ') }} </span>
              </div>
            </div>
          </a-list-item>
        </a-list>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import Status from '@/components/widgets/Status'

export default {
  name: 'ServiceProvidersTab',
  components: {
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
  data () {
    return {
      nsps: {},
      details: ['name', 'state', 'id', 'servicelist'],
      hardcodedNsps: [
        'BaremetalDhcpProvider',
        'BaremetalPxeProvider',
        'BigSwitchBcf',
        'BrocadeVcs',
        'CiscoVnmc',
        'ConfigDrive',
        'F5BigIp',
        'GloboDns',
        'InternalLbVm',
        'JuniperSRX',
        'Netscaler',
        'NiciraNvp',
        'Opendaylight',
        'Ovs',
        'PaloAlto',
        'SecurityGroupProvider',
        'VirtualRouter',
        'VpcVirtualRouter'
      ],
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
      this.fetchServiceProvider()
    },
    fetchServiceProvider (name) {
      this.fetchLoading = true
      api('listNetworkServiceProviders', { physicalnetworkid: this.resource.id, name: name }).then(json => {
        var sps = json.listnetworkserviceprovidersresponse.networkserviceprovider || []
        if (sps.length > 0) {
          for (const sp of sps) {
            this.nsps[sp.name] = sp
          }
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
</style>
