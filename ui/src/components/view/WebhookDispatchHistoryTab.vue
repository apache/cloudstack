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
    <a-button
      v-if="('deleteWebhookDispatchHistory' in $store.getters.apis)"
      type="danger"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="clearHistoryConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ $t('label.action.clear.webhook.history') }}
    </a-button>
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="dispatches"
      :actions="actions"
      :columnKeys="columnKeys"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData"/>
      <a-pagination
        class="row-element"
        style="margin-top: 10px"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="totalCount"
        :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="pageSizeOptions"
        @change="changePage"
        @showSizeChange="changePage"
        showSizeChanger
        showQuickJumper>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
</template>

<script>
import { api } from '@/api'
import { isAdmin } from '@/role'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'

export default {
  name: 'WebhookDispatchHistoryTab',
  components: {
    ListView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['eventtype', 'payload', 'success', 'response', 'duration'],
      selectedColumnKeys: [],
      columns: [],
      cols: [],
      dispatches: [],
      actions: [],
      page: 1,
      pageSize: 20,
      totalCount: 0
    }
  },
  computed: {
    pageSizeOptions () {
      var sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
  created () {
    if (isAdmin()) {
      this.columnKeys.splice(2, 0, 'managementservername')
    }
    this.selectedColumnKeys = this.columnKeys
    this.updateSelectedColumns('response')
    this.pageSize = this.pageSizeOptions[0] * 1
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchDispatches()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchDispatches()
    },
    fetchDispatches () {
      this.dispatches = []
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        webhookruleid: this.resource.id,
        listall: true
      }
      this.tabLoading = true
      api('listWebhookDispatchHistory', params).then(json => {
        this.dispatches = []
        this.totalCount = json?.listwebhookdispatchhistoryresponse?.count || 0
        this.dispatches = json?.listwebhookdispatchhistoryresponse?.webhookdispatch || []
        this.tabLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    updateSelectedColumns (key) {
      if (this.selectedColumnKeys.includes(key)) {
        this.selectedColumnKeys = this.selectedColumnKeys.filter(x => x !== key)
      } else {
        this.selectedColumnKeys.push(key)
      }
      this.updateColumns()
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        var title = this.$t('label.' + String(columnKey).toLowerCase())
        if (columnKey === 'eventtype') {
          title = this.$t('label.event')
        }
        this.columns.push({
          key: columnKey,
          title: title,
          dataIndex: columnKey,
          sorter: (a, b) => { return genericCompare(a[columnKey] || '', b[columnKey] || '') }
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    clearHistoryConfirmation () {
      const self = this
      this.$confirm({
        title: this.$t('label.action.clear.webhook.history'),
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.cleanHistory()
        }
      })
    },
    cleanHistory () {
      const params = {
        webhookruleid: this.resource.id
      }
      this.tabLoading = true
      api('deleteWebhookDispatchHistory', params).then(json => {
        this.$message.success(this.$t('message.success.clear.webhook.history'))
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    }
  }
}
</script>
