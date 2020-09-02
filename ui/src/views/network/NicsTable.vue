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
    <p slot="expandedRowRender" slot-scope="record">
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
    </p>
    <template slot="networkname" slot-scope="text, item">
      <a-icon type="apartment" />
      <router-link :to="{ path: '/guestnetwork/' + item.networkid }">
        {{ text }}
      </router-link>
      <a-tag v-if="item.isdefault">
        {{ $t('label.default') }}
      </a-tag>
    </template>
  </a-table>
</template>

<script>

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
  inject: ['parentFetchData'],
  data () {
    return {
      nicColumns: [
        {
          title: this.$t('label.networkname'),
          dataIndex: 'networkname',
          scopedSlots: { customRender: 'networkname' }
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
      ]
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
    }
  }
}
</script>
