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
    style="overflow-y: auto"
    :columns="columns"
    :dataSource="networks"
    :rowKey="item => item.id"
    :pagination="false"
    :loading="fetchLoading"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.key === 'name'">
        <router-link v-if="record.issystem === false && !record.projectid" :to="{ path: '/guestnetwork/' + record.id }" >{{ text }}</router-link>
        <router-link v-else-if="record.issystem === false && record.projectid" :to="{ path: '/guestnetwork/' + record.id, query: { projectid: record.projectid}}" >{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'state'">
        <status :text="record.state" displayText></status>
      </template>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'GuestVlanNetworksTab',
  components: {
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
      fetchLoading: false,
      networks: [],
      columns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          key: 'state',
          title: this.$t('label.state')
        },
        {
          title: this.$t('label.broadcasturi'),
          dataIndex: 'broadcasturi'
        },
        {
          title: this.$t('label.domain'),
          dataIndex: 'domain'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.project'),
          dataIndex: 'project'
        },
        {
          title: this.$t('label.vpc'),
          dataIndex: 'vpc'
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      var params = {
        id: this.resource.id
      }
      this.fetchLoading = true
      api('listGuestVlans', params).then(json => {
        this.networks = json.listguestvlansresponse.guestvlan[0].network || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.status {
  margin-top: -5px;

  &--end {
    margin-left: 5px;
  }
}
</style>
