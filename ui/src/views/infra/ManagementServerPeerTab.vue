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
    class="table"
    size="small"
    :columns="columns"
    :dataSource="managementservers"
    :rowKey="item => item.id"
    :pagination="false" >
    <template #peername="{ text, record }">
      <router-link :to="{ path: '/managementserver/' + record.peerid }">{{ text }}</router-link>
    </template>
    <template #peerserviceip="{ text, record }">
      <router-link :to="{ path: '/managementserver/' + record.peerid }">{{ text }}</router-link>
    </template>
    <template #lastupdated="{ text }">
      {{ $toLocaleDate(text) }}
    </template>
  </a-table>
</template>

<script>
import { getAPI } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'ManagementServerPeerTab',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      managementservers: [],
      columns: [
        {
          title: this.$t('label.peername'),
          dataIndex: 'peername',
          slots: { customRender: 'peername' }
        },
        {
          title: this.$t('label.peermsid'),
          dataIndex: 'peermsid'
        },
        {
          title: this.$t('label.peerrunid'),
          dataIndex: 'peerrunid'
        },
        {
          title: this.$t('label.peerserviceip'),
          dataIndex: 'peerserviceip',
          slots: { customRender: 'peerserviceip' }
        },
        {
          title: this.$t('label.peerserviceport'),
          dataIndex: 'peerserviceport'
        },
        {
          title: this.$t('label.state.reported'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.peerstate.lastupdated'),
          dataIndex: 'lastupdated',
          slots: { customRender: 'lastupdated' }
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem) {
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.managementservers = []
      getAPI('listManagementServers', {
        peers: true,
        id: this.resource.id
      }).then(json => {
        this.managementservers = json.listmanagementserversresponse.managementserver?.[0]?.peers || []
      })
    }
  }
}
</script>
