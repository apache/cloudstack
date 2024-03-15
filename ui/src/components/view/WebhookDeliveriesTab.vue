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
      v-if="('deleteWebhookDelivery' in $store.getters.apis)"
      type="danger"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="clearDeliveriesConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ $t('label.action.clear.webhook.deliveries') }}
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
  name: 'WebhookDeliveriesTab',
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
      columnKeys: ['payload', 'eventtype', 'success', 'response', 'duration'],
      selectedColumnKeys: [],
      columns: [],
      cols: [],
      dispatches: [],
      actions: [
        {
          api: 'executeWebhookDelivery',
          icon: 'retweet-outlined',
          label: 'label.redeliver',
          message: 'message.redeliver.webhook.delivery',
          dataView: true,
          popup: true
        }
      ],
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
        this.fetchDeliveries()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchDeliveries()
    },
    fetchDeliveries () {
      this.dispatches = []
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        webhookid: this.resource.id,
        listall: true
      }
      this.tabLoading = true
      api('listWebhookDeliveries', params).then(json => {
        this.dispatches = []
        this.totalCount = json?.listwebhookdeliveriesresponse?.count || 0
        this.dispatches = json?.listwebhookdeliveriesresponse?.webhookdelivery || []
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
    clearDeliveriesConfirmation () {
      const self = this
      this.$confirm({
        title: this.$t('label.action.clear.webhook.deliveries'),
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.clearDeliveries()
        }
      })
    },
    clearDeliveries () {
      const params = {
        webhookid: this.resource.id
      }
      this.tabLoading = true
      api('deleteWebhookDelivery', params).then(json => {
        this.$message.success(this.$t('message.success.clear.webhook.deliveries'))
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    redeliverDeliveryConfirmation (item) {
      const self = this
      this.$confirm({
        title: this.$t('label.redeliver') + ' ' + item.eventtype,
        okText: this.$t('label.ok'),
        okType: 'primary',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.redeliverDelivery(item)
        }
      })
    },
    redeliverDelivery (item) {
      const params = {
        id: item.id
      }
      this.tabLoading = true
      api('executeWebhookDelivery', params).then(json => {
        this.$message.success(this.$t('message.success.redeliver.webhook.delivery'))
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    execAction (action) {
      console.log('-------------------', action)
      if (action.api === 'executeWebhookDelivery') {
        this.redeliverDeliveryConfirmation(action.resource)
      }
    }
  }
}
</script>
