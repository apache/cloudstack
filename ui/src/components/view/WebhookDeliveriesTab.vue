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
    <a-modal
      v-model:visible="showTimeFilterModal"
      :title="$t('label.select.period')"
      :maskClosable="false"
      :footer="null">
      <date-time-filter
        :startDateProp="startDate"
        :endDateProp="endDate"
        :showAllDataOption="false"
        @closeAction="closeTimeFilterAction"
        @onSubmit="handleSubmitDateTimeFilter"/>
    </a-modal>
    <div class="filter-row">
      <a-row>
        <a-col :xs="24" :md="12">
          <a-space direction="vertical">
            <div>
              <a-radio-group
                v-model:value="durationSelectorValue"
                buttonStyle="solid"
                @change="handleTimeFilterChange">
                <a-radio-button value="day">
                  {{ $t('label.duration.24hours') }}
                </a-radio-button>
                <a-radio-button value="all">
                  {{ $t('All') }}
                </a-radio-button>
                <a-radio-button value="custom">
                  {{ $t('label.duration.custom') }}
                </a-radio-button>
              </a-radio-group>
              <InfoCircleOutlined class="info-icon" :title="$t('message.webhook.deliveries.time.filter')"/>
            </div>
            <div class="ant-tag" v-if="durationSelectorValue==='custom'">
              <a-button @click="openTimeFilter()">
                <FilterOutlined/>
              </a-button>
              <span v-html="formatedPeriod"></span>
            </div>
          </a-space>
        </a-col>
        <a-col :xs="24" :md="12" style="text-align: right; padding-right: 10px;">
          <span>
            <search-view
              :searchFilters="searchFilters"
              :searchParams="searchParams"
              :apiName="'listWebhookDeliveries'"
              @search="searchDelivieries"
            />
          </span>
        </a-col>
      </a-row>
    </div>
    <a-button
      v-if="('deleteWebhookDelivery' in $store.getters.apis) && ((selectedRowKeys && selectedRowKeys.length > 0) || (durationSelectorValue === 'all' && searchParamsIsEmpty))"
      type="danger"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="clearOrDeleteDeliveriesConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ (selectedRowKeys && selectedRowKeys.length > 0) ? $t('label.action.delete.webhook.deliveries') : $t('label.action.clear.webhook.deliveries') }}
    </a-button>
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="deliveries"
      :actions="actions"
      :columnKeys="columnKeys"
      :explicitlyAllowRowSelection="true"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData"
      @selection-change="updateSelectedRows"/>
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
import moment from 'moment'
import DateTimeFilter from './DateTimeFilter'
import SearchView from '@/components/view/SearchView'
import ListView from '@/components/view/ListView'

export default {
  name: 'WebhookDeliveriesTab',
  components: {
    DateTimeFilter,
    SearchView,
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
      columnKeys: ['payload', 'eventtype', 'success', 'response', 'startdate', 'duration'],
      selectedColumnKeys: ['payload', 'eventtype', 'success', 'duration'],
      selectedRowKeys: [],
      columns: [],
      cols: [],
      deliveries: [],
      actions: [
        {
          api: 'executeWebhookDelivery',
          icon: 'retweet-outlined',
          label: 'label.redeliver',
          message: 'message.redeliver.webhook.delivery',
          dataView: true,
          popup: true
        },
        {
          api: 'deleteWebhookDelivery',
          icon: 'delete-outlined',
          label: 'label.delete.webhook.delivery',
          message: 'message.redeliver.webhook.delivery',
          dataView: true,
          popup: true
        }
      ],
      page: 1,
      pageSize: 20,
      totalCount: 0,
      durationSelectorValue: 'day',
      showTimeFilterModal: false,
      endDate: null,
      startDate: this.get24hrStartDate(),
      formatedPeriod: null,
      searchFilters: [
        'eventtype'
      ],
      searchParams: {}
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
    },
    searchParamsIsEmpty () {
      if (!this.searchParams) {
        return true
      }
      return Object.keys(this.searchParams).length === 0
    }
  },
  created () {
    const routeQuery = this.$route.query
    const usefulQueryParams = ['keyword', 'managementserverid', 'eventtype']
    usefulQueryParams.forEach(queryParam => {
      if (routeQuery[queryParam]) {
        this.searchParams[queryParam] = routeQuery[queryParam]
      }
    })
    if (isAdmin()) {
      this.columnKeys.splice(2, 0, 'managementservername')
      this.selectedColumnKeys.splice(2, 0, 'managementservername')
      this.searchFilters.push('managementserverid')
    }
    this.updateColumns()
    this.pageSize = this.pageSizeOptions[0] * 1
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      if ('listview' in this.$refs && this.$refs.listview) {
        this.$refs.listview.resetSelection()
      }
      this.fetchDeliveries()
    },
    fetchDeliveries () {
      this.deliveries = []
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        webhookid: this.resource.id,
        listall: true
      }
      if (this.startDate) {
        params.startDate = moment(this.startDate).format()
      }
      if (this.endDate) {
        params.endDate = moment(this.endDate).format()
      }
      if (this.searchParams?.searchQuery) {
        params.keyword = this.searchParams.searchQuery
      }
      if (this.searchParams?.managementserverid) {
        params.managementserverid = this.searchParams.managementserverid
      }
      if (this.searchParams?.eventtype) {
        params.eventtype = this.searchParams.eventtype
      }
      this.tabLoading = true
      api('listWebhookDeliveries', params).then(json => {
        this.deliveries = []
        this.totalCount = json?.listwebhookdeliveriesresponse?.count || 0
        this.deliveries = json?.listwebhookdeliveriesresponse?.webhookdelivery || []
        this.formatTimeFilterPeriod()
        this.tabLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchDeliveries()
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
        const key = columnKey
        if (!this.selectedColumnKeys.includes(key)) continue
        var title = this.$t('label.' + String(key).toLowerCase())
        if (key === 'eventtype') {
          title = this.$t('label.event')
        }
        this.columns.push({
          key: key,
          title: title,
          dataIndex: key,
          sorter: (a, b) => { return genericCompare(a[key] || '', b[key] || '') }
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    updateSelectedRows (keys) {
      this.selectedRowKeys = keys
    },
    clearOrDeleteDeliveriesConfirmation () {
      const self = this
      const title = (this.selectedRowKeys && this.selectedRowKeys.length > 0) ? this.$t('label.action.delete.webhook.deliveries') : this.$t('label.action.clear.webhook.deliveries')
      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          if (self.selectedRowKeys && self.selectedRowKeys.length > 0) {
            self.deletedSelectedDeliveries()
            return
          }
          self.clearDeliveries()
        }
      })
    },
    deletedSelectedDeliveries () {
      const promises = []
      this.selectedRowKeys.forEach(id => {
        const params = {
          id: id
        }
        promises.push(new Promise((resolve, reject) => {
          api('deleteWebhookDelivery', params).then(json => {
            return resolve(id)
          }).catch(error => {
            return reject(error)
          })
        }))
      })
      const msg = this.$t('label.action.delete.webhook.deliveries')
      this.$message.info({
        content: msg,
        duration: 3
      })
      this.tabLoading = true
      Promise.all(promises).finally(() => {
        this.tabLoading = false
        this.fetchData()
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
    deleteDeliveryConfirmation (item) {
      const self = this
      this.$confirm({
        title: this.$t('label.delete') + ' ' + item.eventtype,
        okText: this.$t('label.ok'),
        okType: 'primary',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.deleteDelivery(item)
        }
      })
    },
    deleteDelivery (item) {
      const params = {
        id: item.id
      }
      this.tabLoading = true
      api('deleteWebhookDelivery', params).then(json => {
        const message = `${this.$t('message.success.delete')} ${this.$t('label.webhook.delivery')}`
        this.$message.success(message)
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    execAction (action) {
      if (action.api === 'executeWebhookDelivery') {
        this.redeliverDeliveryConfirmation(action.resource)
      } else if (action.api === 'deleteWebhookDelivery') {
        this.deleteDeliveryConfirmation(action.resource)
      }
    },
    get24hrStartDate () {
      const start = new Date()
      start.setDate(start.getDate() - 1)
      return start
    },
    handleTimeFilterChange () {
      var start = this.startDate
      var end = this.endDate
      switch (this.durationSelectorValue) {
        case 'day':
          start = this.get24hrStartDate()
          end = null
          break
        case 'all':
          start = null
          end = null
          break
      }
      if (start !== this.startDate || end !== this.endDate) {
        this.startDate = start
        this.endDate = end
        this.fetchData()
      }
    },
    openTimeFilter () {
      this.showTimeFilterModal = true
    },
    formatTimeFilterPeriod () {
      var formatedStartDate = null
      var formatedEndDate = null
      if (this.startDate) {
        formatedStartDate = moment(this.startDate).format('MMM DD, YYYY') + ' at ' + moment(this.startDate).format('HH:mm:ss')
      }
      if (this.endDate) {
        formatedEndDate = moment(this.endDate).format('MMM DD, YYYY') + ' at ' + moment(this.endDate).format('HH:mm:ss')
      }
      if (formatedStartDate && formatedEndDate) {
        this.formatedPeriod = ' ' + this.$t('label.datetime.filter.period', { startDate: formatedStartDate, endDate: formatedEndDate })
      } else if (formatedStartDate && !formatedEndDate) {
        this.formatedPeriod = ' ' + this.$t('label.datetime.filter.starting', { startDate: formatedStartDate })
      } else if (!formatedStartDate && formatedEndDate) {
        this.formatedPeriod = ' ' + this.$t('label.datetime.filter.up.to', { endDate: formatedEndDate })
      } else {
        this.formatedPeriod = ' <b>' + this.$t('label.all.available.data') + '</b>'
      }
    },
    handleSubmitDateTimeFilter (values) {
      if (values.startDate) {
        this.startDate = new Date(values.startDate)
      } else {
        this.startDate = null
      }
      if (values.endDate) {
        this.endDate = new Date(values.endDate)
      } else {
        this.endDate = null
      }
      this.showTimeFilterModal = false
      this.fetchData()
    },
    closeTimeFilterAction () {
      this.showTimeFilterModal = false
    },
    searchDelivieries (params) {
      const query = Object.assign({}, this.$route.query)
      for (const key in this.searchParams) {
        delete query[key]
      }
      delete query.keyword
      delete query.q
      this.searchParams = params
      if ('searchQuery' in this.searchParams) {
        query.keyword = this.searchParams.searchQuery
      } else {
        Object.assign(query, this.searchParams)
      }
      this.fetchData()
      if (JSON.stringify(query) === JSON.stringify(this.$route.query)) {
        return
      }
      history.pushState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
    }
  }
}
</script>

<style lang="scss" scoped>
.ant-tag {
  padding: 0 7px 0 0;
}
.ant-select {
  margin-left: 10px;
}
.info-icon {
  margin: 0 10px 0 5px;
}
.filter-row {
  margin-bottom: 2.5%;
}
.filter-row-inner {
  margin-top: 3%;
}
</style>
