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
      <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
        <a-input-search
          v-model:value="searchText"
          :placeholder="$t('label.search')"
          style="width: 250px;"
          allow-clear />
        <a-button
          shape="round"
          @click="() => { showAddForm = true }">
          {{ $t('label.dns.create.record') }}
          <plus-outlined style="margin-left: 5px;" />
        </a-button>
      </div>

      <a-table
        size="small"
        style="overflow-y: auto; width: 100%;"
        :columns="columns"
        :dataSource="filteredRecords"
        :rowKey="item => `${item.name}-${item.type}`"
        :pagination="tablePagination">

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'contents'">
            <a-tag v-for="(item, idx) in record.contents" :key="idx">{{ item }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'ttl'">
            {{ record.ttl }}
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
    </a-spin>

    <a-modal
      v-if="showAddForm"
      :visible="showAddForm"
      :title="$t('label.dns.create.record')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showAddForm = false }"
      centered
      width="auto">
      <CreateDnsRecord
        :resource="resource"
        @close-action="handleCloseCreateForm" />
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import CreateDnsRecord from '@/views/network/dns/CreateDnsRecord'

export default {
  name: 'DnsRecordsTab',
  components: {
    TooltipButton,
    CreateDnsRecord
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
      deleteLoading: false,
      showAddForm: false,
      searchText: '',
      records: [],
      tablePagination: {
        defaultPageSize: 10,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '40', '80', '100'],
        showTotal: (total) => `${this.$t('label.total')} ${total} ${this.$t('label.items')}`
      },
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          sorter: (a, b) => a.name.localeCompare(b.name)
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type',
          sorter: (a, b) => a.type.localeCompare(b.type)
        },
        {
          title: this.$t('label.contents'),
          dataIndex: 'contents'
        },
        {
          title: this.$t('label.ttl') + ' (' + this.$t('label.seconds') + ')',
          dataIndex: 'ttl',
          sorter: (a, b) => a.ttl - b.ttl
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ]
    }
  },
  computed: {
    filteredRecords () {
      const q = this.searchText.trim().toLowerCase()
      if (!q) return this.records
      return this.records.filter(r =>
        r.name?.toLowerCase().includes(q) ||
        r.type?.toLowerCase().includes(q) ||
        r.contents?.some(c => c.toLowerCase().includes(q))
      )
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    'resource.id' (newId) {
      if (!newId) return
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      if (this.fetchLoading) return

      const params = {
        dnszoneid: this.resource.id
      }
      this.fetchLoading = true
      getAPI('listDnsRecords', params).then(json => {
        const response = json.listdnsrecordsresponse || {}
        this.records = response.dnsrecord || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    handleDeleteRecord (record) {
      if (this.deleteLoading) return
      this.deleteLoading = true

      const params = {
        dnszoneid: this.resource.id,
        name: record.name,
        type: record.type
      }

      postAPI('deleteDnsRecord', params).then(response => {
        const jobId = response?.deletednsrecordresponse?.jobid
        if (!jobId) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: 'Failed to get jobid for DeleteDnsRecord',
            duration: 0
          })
          this.deleteLoading = false
          return
        }
        this.$pollJob({
          jobId,
          title: this.$t('label.dns.delete.record'),
          description: record.name,
          successMethod: () => {
            this.$notification.success({
              message: this.$t('message.success.delete.dns.record'),
              description: `${this.$t('message.success.delete.dns.record')} ${record.name}`
            })
            this.records = this.records.filter(r => !(r.name === record.name && r.type === record.type))
            this.deleteLoading = false
          },
          errorMethod: () => {
            this.deleteLoading = false
          },
          loadingMessage: `${this.$t('label.dns.delete.record')} ${record.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: {
            isFetchData: false
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: error?.response?.headers['x-description'] || error.message,
          duration: 0
        })
        this.deleteLoading = false
      })
    },
    handleCloseCreateForm (payload) {
      this.showAddForm = false
      const jobId = payload?.jobId
      if (!jobId) return
      const recordName = payload?.recordName || ''
      this.$pollJob({
        jobId,
        title: this.$t('label.dns.create.record'),
        description: recordName,
        successMethod: () => {
          this.$notification.success({
            message: this.$t('label.dns.create.record'),
            description: `${this.$t('message.success.create.dns.record')} ${recordName}`
          })
          this.fetchData()
        },
        errorMethod: () => {
          this.fetchData()
        },
        loadingMessage: `${this.$t('label.dns.create.record')} ${recordName} ${this.$t('label.in.progress')}`,
        catchMessage: this.$t('error.fetching.async.job.result'),
        action: {
          isFetchData: false
        }
      })
    }
  }
}
</script>
