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
    <a-spin :spinning="fetchLoading">
      <a-button
        shape="round"
        style="float: right;margin-bottom: 10px; z-index: 8"
        @click="() => { showCreateForm = true }">
        <template #icon><plus-outlined /></template>
        {{ $t('label.add.ip.range') }}
      </a-button>
      <br />
      <br />

      <a-table
        size="small"
        style="overflow-y: auto; width: 100%;"
        :columns="columns"
        :dataSource="ipranges"
        :rowKey="item => item.id"
        :pagination="false" >

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-popconfirm
              :title="$t('message.confirm.remove.ip.range')"
              @confirm="removeIpRange(record.id)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')" >
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.action.delete.ip.range')"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </template>
        </template>

      </a-table>
      <a-divider/>
      <a-pagination
        class="row-element pagination"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="total"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>
    <a-modal
      v-if="showCreateForm"
      :visible="showCreateForm"
      :title="$t('label.add.ip.range')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showCreateForm = false }"
      centered
      width="auto">
      <CreateVlanIpRange
        :resource="resource"
        @refresh-data="fetchData"
        @close-action="showCreateForm = false" />
    </a-modal>
  </div>
</template>
<script>
import { api } from '@/api'
import CreateVlanIpRange from '@/views/network/CreateVlanIpRange'
import TooltipButton from '@/components/widgets/TooltipButton'
export default {
  name: 'GuestIpRanges',
  components: {
    CreateVlanIpRange,
    TooltipButton
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
      showCreateForm: false,
      total: 0,
      ipranges: [],
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.startipv4'),
          dataIndex: 'startip'
        },
        {
          title: this.$t('label.endipv4'),
          dataIndex: 'endip'
        },
        {
          title: this.$t('label.startipv6'),
          dataIndex: 'startipv6'
        },
        {
          title: this.$t('label.endipv6'),
          dataIndex: 'endipv6'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask'
        },
        {
          key: 'actions',
          title: ''
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
      const params = {
        zoneid: this.resource.zoneid,
        networkid: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }
      this.fetchLoading = true
      api('listVlanIpRanges', params).then(json => {
        this.total = json.listvlaniprangesresponse.count || 0
        this.ipranges = json.listvlaniprangesresponse.vlaniprange || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    removeIpRange (id) {
      api('deleteVlanIpRange', { id: id }).then(json => {
      }).finally(() => {
        this.fetchData()
      })
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
