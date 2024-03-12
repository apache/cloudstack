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
    :dataSource="routers"
    :rowKey="item => item.id"
    :pagination="false"
    :loading="fetchLoading"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.key === 'name'">
        <router-link :to="{ path: '/router/' + record.id }" >{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'status'">
        <status class="status" :text="record.state" displayText />
      </template>
      <template v-if="column.key === 'requiresupgrade'">
        {{ record.requiresupgrade ? $t('label.yes') : $t('label.no') }}
      </template>
      <template v-if="column.key === 'isredundantrouter'">
        {{ record.isredundantrouter ? record.redundantstate : record.isredundantrouter }}
      </template>
      <template v-if="column.key === 'hostname'">
        <router-link :to="{ path: '/host/' + record.hostid }" >{{ record.hostname || record.hostid }}</router-link>
      </template>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'RoutersTab',
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
      routers: [],
      columns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          key: 'status',
          title: this.$t('label.status'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.ip'),
          dataIndex: 'publicip'
        },
        {
          title: this.$t('label.version'),
          dataIndex: 'version'
        },
        {
          key: 'requiresupgrade',
          title: this.$t('label.requiresupgrade'),
          dataIndex: 'requiresupgrade'
        },
        {
          key: 'isredundantrouter',
          title: this.$t('label.isredundantrouter'),
          dataIndex: 'isredundantrouter'
        },
        {
          key: 'hostname',
          title: this.$t('label.hostname'),
          dataIndex: 'hostname'
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      var params = {
        listAll: true
      }
      if (this.$route.fullPath.startsWith('/vpc')) {
        params.vpcid = this.resource.id
      } else {
        params.networkid = this.resource.id
      }
      this.fetchLoading = true
      api('listRouters', params).then(json => {
        this.routers = json.listroutersresponse.router || []
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
