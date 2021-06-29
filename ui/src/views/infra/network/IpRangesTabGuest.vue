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
  <a-spin :spinning="componentLoading">
    <a-button
      :disabled="!('createNetwork' in this.$store.getters.apis)"
      type="dashed"
      icon="plus"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenShowCreateForm">
      {{ $t('label.add.guest.network') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template slot="name" slot-scope="text, item">
        <router-link :to="{ path: '/guestnetwork/' + item.id }">
          {{ text }}
        </router-link>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      style="overflow-y: auto"
      :current="page"
      :pageSize="pageSize"
      :total="total"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-model="showCreateForm"
      :title="$t('label.add.guest.network')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      :cancelText="$t('label.cancel')"
      @cancel="closeAction"
      centered
      width="auto">
      <CreateNetwork :resource="{ zoneid: resource.zoneid }" @close-action="closeAction"/>
    </a-modal>

  </a-spin>
</template>

<script>
import { api } from '@/api'
import CreateNetwork from '@/views/network/CreateNetwork'

export default {
  name: 'IpRangesTabGuest',
  components: {
    CreateNetwork
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
      componentLoading: false,
      items: [],
      total: 0,
      showCreateForm: false,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlan'
        },
        {
          title: this.$t('label.broadcasturi'),
          dataIndex: 'broadcasturi'
        },
        {
          title: this.$t('label.cidr'),
          dataIndex: 'cidr'
        },
        {
          title: this.$t('label.ip6cidr'),
          dataIndex: 'ip6cidr'
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    network (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.componentLoading = true
      api('listNetworks', {
        zoneid: this.resource.zoneid,
        physicalnetworkid: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.items = response.listnetworksresponse.network ? response.listnetworksresponse.network : []
        this.total = response.listnetworksresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleOpenShowCreateForm () {
      this.showCreateForm = true
    },
    closeAction () {
      this.showCreateForm = false
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style lang="scss" scoped>
  .pagination {
    margin-top: 20px;
  }
</style>
