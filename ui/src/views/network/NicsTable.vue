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
  <a-table
    size="small"
    :columns="nicColumns"
    :dataSource="resource.nic"
    :rowKey="item => item.id"
    :pagination="false"
  >
    <template #expandedRowRender="{ record }">
      <slot name="actions" :nic="record" />
      <a-descriptions style="margin-top: 10px" layout="vertical" :column="1" :bordered="false" size="small">
        <a-descriptions-item :label="$t('label.id')">
          {{ record.id }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.networkid')" v-if="record.networkid">
          {{ record.networkid }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.type')" v-if="record.type">
          {{ record.type }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.traffictype')" v-if="record.traffictype">
          {{ record.traffictype }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.secondaryips')" v-if="record.secondaryip && record.secondaryip.length > 0 && record.type !== 'L2'">
          {{ record.secondaryip.map(x => x.ipaddress).join(', ') }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.ip6address')" v-if="record.ip6address">
          {{ record.ip6address }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.ip6gateway')" v-if="record.ip6gateway">
          {{ record.ip6gateway }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('label.ip6cidr')" v-if="record.ip6cidr">
          {{ record.ip6cidr }}
        </a-descriptions-item>
        <template v-if="['Admin', 'DomainAdmin'].includes($store.getters.userInfo.roletype)">
          <a-descriptions-item :label="$t('label.broadcasturi')" v-if="record.broadcasturi">
            {{ record.broadcasturi }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('label.isolationuri')" v-if="record.isolationuri">
            {{ record.isolationuri }}
          </a-descriptions-item>
        </template>
      </a-descriptions>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.key === 'networkname'">
        <resource-icon v-if="!networkIconLoading && networkicon[record.id]" :image="networkicon[record.id]" size="1x" style="margin-right: 5px"/>
        <apartment-outlined v-else style="margin-right: 5px" />
        <router-link :to="{ path: '/guestnetwork/' + record.networkid }">
          {{ text }}
        </router-link>
        <a-tag v-if="record.isdefault">
          {{ $t('label.default') }}
        </a-tag>
      </template>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'NicsTable',
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
  components: {
    ResourceIcon
  },
  inject: ['parentFetchData'],
  data () {
    return {
      nicColumns: [
        {
          key: 'deviceid',
          title: this.$t('label.deviceid'),
          dataIndex: 'deviceid'
        },
        {
          key: 'networkname',
          title: this.$t('label.networkname'),
          dataIndex: 'networkname'
        },
        {
          title: this.$t('label.macaddress'),
          dataIndex: 'macaddress'
        },
        {
          title: this.$t('label.ipaddress'),
          dataIndex: 'ipaddress'
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        }
      ],
      networkicon: {},
      networkIconLoading: false
    }
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem, oldItem) {
        if (newItem && (!oldItem || (newItem.id !== oldItem.id))) {
          this.fetchNetworks()
        }
      }
    }
  },
  created () {
    this.fetchNetworks()
  },
  methods: {
    fetchNetworks () {
      if (!this.resource || !this.resource.nic) return
      this.networkIconLoading = true
      this.networkicon = {}
      const promises = []
      this.resource.nic.forEach((item, index) => {
        promises.push(this.fetchNetworkIcon(item.id, item.networkid))
      })
      Promise.all(promises).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.networkIconLoading = false
      })
    },
    fetchNetworkIcon (id, networkid) {
      return new Promise((resolve, reject) => {
        this.networkicon[id] = null
        api('listNetworks', {
          id: networkid,
          showicon: true
        }).then(json => {
          const network = json.listnetworksresponse?.network || []
          if (network?.[0]?.icon) {
            this.networkicon[id] = network[0]?.icon?.base64image
            resolve(this.networkicon)
          } else {
            this.networkicon[id] = ''
            resolve(this.networkicon)
          }
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>
