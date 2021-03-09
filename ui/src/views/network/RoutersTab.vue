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
    <template slot="name" slot-scope="text,item">
      <router-link :to="{ path: '/router/' + item.id }" >{{ text }}</router-link>
    </template>
    <template slot="status" slot-scope="text, item">
      <status class="status" :text="item.state" displayText />
    </template>
    <template slot="requiresupgrade" slot-scope="text, item">
      {{ item.requiresupgrade ? $t('label.yes') : $t('label.no') }}
    </template>
    <template slot="isredundantrouter" slot-scope="text, record">
      {{ record.isredundantrouter ? record.redundantstate : record.isredundantrouter }}
    </template>
    <template slot="hostname" slot-scope="text, record">
      <router-link :to="{ path: '/host/' + record.hostid }" >{{ record.hostname || record.hostid }}</router-link>
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
          title: this.$t('label.name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('label.status'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'status' }
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
          title: this.$t('label.requiresupgrade'),
          dataIndex: 'requiresupgrade',
          scopedSlots: { customRender: 'requiresupgrade' }
        },
        {
          title: this.$t('label.isredundantrouter'),
          dataIndex: 'isredundantrouter',
          scopedSlots: { customRender: 'isredundantrouter' }
        },
        {
          title: this.$t('label.hostname'),
          dataIndex: 'hostname',
          scopedSlots: { customRender: 'hostname' }
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
