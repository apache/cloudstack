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
        @click="() => { showAddForm = true }">
        <template #icon><plus-outlined /></template>
        {{ $t('label.dns.add.record') }}
      </a-button>
      <br />
      <br />

      <a-table
        size="small"
        style="overflow-y: auto; width: 100%;"
        :columns="columns"
        :dataSource="records"
        :rowKey="item => item.id"
        :pagination="false">

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'contents'">
            <a-tag v-for="item in record.contents" :key="item">{{ item }}</a-tag>
          </template>
          <template v-if="column.key === 'actions'">
            <a-popconfirm
              :title="$t('message.confirm.delete.dns.record')"
              @confirm="handleDeleteRecord(record)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')">
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.delete')"
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
      v-if="showAddForm"
      :visible="showAddForm"
      :title="$t('label.dns.add.record')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showAddForm = false }"
      centered
      width="auto">
      <AddDnsRecord
        :resource="resource"
        @refresh-data="fetchData"
        @close-action="showAddForm = false" />
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import AddDnsRecord from '@/views/network/dns/AddDnsRecord'

export default {
  name: 'DnsRecordsTab',
  components: {
    TooltipButton,
    AddDnsRecord
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
      showAddForm: false,
      total: 0,
      records: [],
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.contents'),
          dataIndex: 'contents'
        },
        {
          title: this.$t('label.ttl'),
          dataIndex: 'ttl'
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
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
        dnszoneid: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }
      this.fetchLoading = true
      getAPI('listDnsRecords', params).then(json => {
        const response = json.listdnsrecordsresponse || {}
        this.total = response.count || 0
        this.records = response.dnsrecord || []
      }).catch(error => {
        console.error('Failed to fetch DNS records', error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    handleDeleteRecord (record) {
      postAPI('deleteDnsRecord', { id: record.id }).then(() => {
        this.$notification.success({
          message: this.$t('message.success.delete.dns.record')
        })
      }).catch(error => {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: error?.response?.headers['x-description'] || error.message,
          duration: 0
        })
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
