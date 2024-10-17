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
  <a-affix :offsetTop="this.$store.getters.shutdownTriggered ? 103 : 78">
    <a-card class="breadcrumb-card">
      <a-row>
        <a-col
          :span="device === 'mobile' ? 24 : 12"
          style="padding-left: 12px; margin-top: 10px"
        >
          <breadcrumb :resource="resource">
            <template #end>
              <a-tooltip placement="bottom">
                <template #title>{{ $t('label.refresh') }}</template>
                <a-button
                  style="margin-top: 4px"
                  :loading="serverMetricsLoading"
                  shape="round"
                  size="small"
                  @click="fetchData(); listUsageRecords()"
                >
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  {{ $t('label.refresh') }}
                </a-button>
              </a-tooltip>
            </template>
          </breadcrumb>
        </a-col>
        <a-col
          :span="device === 'mobile' ? 24 : 12"
          :style="device === 'mobile' ? { float: 'right', 'margin-top': '12px', 'margin-bottom': '-6px', display: 'table' } : { float: 'right', display: 'table', 'margin-top': '6px' }"
        >
          <a-row justify="end">
            <a-col>
              <tooltip-button
                type="primary"
                icon="hdd-outlined"
                :tooltip="$t('label.usage.records.generate')"
                @onClick="generateModal = true"
              />
            </a-col>&nbsp;&nbsp;
            <a-col>
              <tooltip-button
                type="danger"
                icon="delete-outlined"
                :tooltip="$t('label.usage.records.purge')"
                @onClick="() => purgeModal = true"
              />
            </a-col>
          </a-row>
        </a-col>
      </a-row>
    </a-card>
  </a-affix>
  <a-col>
    <a-card size="small" :loading="serverMetricsLoading">
      <a-row justify="space-around">
        <a-card-grid style="width: 30%; text-align: center; font-size: small;">
          <a-statistic
            :title="$t('label.server')"
            :value="serverStats.hostname"
            valueStyle="font-size: medium"
          >
            <template #prefix>
              <status :text="serverStats.state || ''" />
            </template>
          </a-statistic>
        </a-card-grid>
        <a-card-grid style="width: 35%; text-align: center; font-size: small;">
          <a-statistic
            :title="$t('label.lastheartbeat')"
            :value="$toLocaleDate(serverStats.lastheartbeat)"
            valueStyle="font-size: medium"
          />
          <a-card-meta :description="getTimeSince(serverStats.collectiontime)" />
        </a-card-grid>
        <a-card-grid style="width: 35%; text-align: center; font-size: small;">
          <a-statistic
            :title="$t('label.lastsuccessfuljob')"
            :value="$toLocaleDate(serverStats.lastsuccessfuljob)"
            valueStyle="font-size: medium"
          />
          <a-card-meta :description="getTimeSince(serverStats.lastsuccessfuljob)" />
        </a-card-grid>
      </a-row>
    </a-card>
  </a-col>
  <a-row justify="space-between">
    <a-col :span="24">
      <a-card>
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="inline"
          @finish="handleSearch"
        >
          <a-col :span="4">
            <a-row>
              <a-col :span="24">
                <a-form-item
                  ref="domain"
                  name="domain"
                >
                  <a-auto-complete
                    v-model:value="form.domain"
                    :options="domains"
                    :placeholder="$t('label.domain')"
                    :filter-option="filterOption"
                    style="width: 100%;"
                    @select="getAccounts"
                    :dropdownMatchSelectWidth="false"
                  />
                </a-form-item>
              </a-col>
            </a-row>&nbsp;
            <a-row>
              <a-col :span="24">
                <a-form-item
                  ref="isRecursive"
                  name="isRecursive"
                >
                  <a-checkbox v-model:checked="form.isRecursive">{{ $t('label.usage.records.fetch.child.domains')
                  }}</a-checkbox>
                </a-form-item>
              </a-col>
            </a-row>
          </a-col>
          <a-col :span="3">
            <a-form-item
              ref="account"
              name="account"
            >
            <a-auto-complete
                v-model:value="form.account"
                :options="accounts"
                :placeholder="$t('label.account')"
                :filter-option="filterOption"
                :disabled="form.isRecursive"
                :dropdownMatchSelectWidth="false"
                @select="selectAccount"
              />
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item
              ref="type"
              name="type"
            >
              <a-select
                v-model:value="form.type"
                :options="usageTypes"
                :placeholder="$t('label.usagetype')"
                :filterOption="filterOption"
                @select="selectUsageType"
              />
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item
              ref="id"
              name="id"
            >
              <a-input
                v-model:value="form.id"
                :placeholder="$t('label.resourceid')"
                :allowClear="true"
                @change="handleResourceIdChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item
              ref="dateRange"
              name="dateRange"
            >
              <a-range-picker
                :ranges="rangePresets"
                v-model:value="form.dateRange"
                :disabled-date="disabledDate"
              />
            </a-form-item>
          </a-col>
          <a-col>
            <a-row justify="space-between">
              <a-form-item>
                <a-button
                  type="primary"
                  html-type="submit"
                  @click="handleSearch"
                  :loading="loading"
                >
                  <search-outlined />
                  {{ $t('label.show.usage.records') }}
                </a-button>
              </a-form-item>
              <a-form-item>
                <a-button
                  type="primary"
                  @click="downloadRecords"
                  :loading="loading"
                >
                  <download-outlined />
                  {{ $t('label.download.csv') }}
                </a-button>
              </a-form-item>
              <a-form-item>
                <a-button @click="clearFilters">
                  {{ $t('label.clear') }}
                </a-button>
              </a-form-item>
            </a-row>
          </a-col>
        </a-form>
      </a-card>
    </a-col>
  </a-row>
  <a-row justify="space-around">
    <a-col :span="24">
      <list-view
        :loading="tableLoading"
        :columns="columns"
        :items="usageRecords"
        :columnKeys="columnKeys"
        :selectedColumns="selectedColumnKeys"
        ref="listview"
        @update-selected-columns="updateSelectedColumns"
        @view-usage-record="viewUsageRecord"
        @refresh="this.fetchData"
      />
      <a-pagination
        :current="page"
        :pageSize="pageSize"
        :total="totalUsageRecords"
        :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1 + ((page - 1) * pageSize))}-${Math.min(page * pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['20', '50', '100']"
        @change="handleTableChange"
        :showSizeChanger="true"
        :showQuickJumper="true"
      >
      </a-pagination>
    </a-col>
  </a-row>

  <a-modal
    :title="$t('label.usage.records.generate')"
    :cancelText="$t('label.cancel')"
    :closable="true"
    :maskClosable="true"
    :destroyOnClose="true"
    :visible="generateModal"
    @ok="generateUsageRecords"
    @cancel="generateModal = false"
  >
    <a-alert
      :message="$t('label.usage.records.generate.description')"
      type="info"
      show-icon
    >
    </a-alert>
    <br/>
    {{ $t('label.usage.records.generate.after') + $toLocaleDate(serverStats.lastsuccessfuljob) }}
  </a-modal>

  <a-modal
    :title="$t('label.usage.records.purge')"
    :visible="purgeModal"
    :okText="$t('label.usage.records.purge')"
    :okButtonProps="{ type: 'danger' }"
    :cancelText="$t('label.cancel')"
    :closable="true"
    :maskClosable="true"
    :destroyOnClose="true"
    @ok="purgeUsageRecords"
    @cancel="purgeModal = false"
  >
    <a-row>
      <a-alert
        :description="$t('label.usage.records.purge.alert')"
        type="error"
        show-icon
      />

    </a-row>
    <br />
    <a-row justify="space-between">
      <tooltip-label
        bold
        :title="$t('label.usage.records.purge.days')"
        :tooltip="$t('label.usage.records.purge.days.description')"
      />
      <a-input-number
        :min="0"
        v-model:value="purgeDays"
        style="width: 128px;"
      >
        <template #addonAfter>{{ $t('label.days') }}</template>
      </a-input-number>
    </a-row>
  </a-modal>
  <a-modal
    :title="$t('label.usage.records.downloading')"
    :visible="downloadModal"
    :closable="false"
    :maskClosable="false"
    :destroyOnClose="true"
    :footer="null"
  >
    <a-progress
      :percent="downloadPercent"
      :status="downloadStatus"
    />
    <a-spin size="small" /> {{ [$t('label.fetched'), downloadedRecords, $t('label.of'), downloadTotalRecords,
    $t('label.items')].join(' ') }}
  </a-modal>
  <a-modal
    :visible="viewModal"
    :cancelText="$t('label.close')"
    :closable="true"
    :maskClosable="true"
    :okButtonProps="{ style: { display: 'none' } }"
    :destroyOnClose="true"
    width="50%"
    @cancel="viewModal = false"
  >
    <pre style="text-align: start; white-space: break-spaces;">{{ JSON.stringify(recordView, null, 2) }}</pre>
  </a-modal>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import utc from 'dayjs/plugin/utc'
import { api } from '@/api'
import { toCsv } from '@/utils/util.js'
import { mixinForm } from '@/utils/mixin'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import ListView from '@/components/view/ListView'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TooltipButton from '@/components/widgets/TooltipButton'
import Status from '@/components/widgets/Status'

dayjs.extend(relativeTime)
dayjs.extend(utc)

export default {
  name: 'UsageRecords',
  mixins: [mixinForm],
  components: {
    Breadcrumb,
    ChartCard,
    ListView,
    Status,
    TooltipLabel,
    TooltipButton
  },
  props: {
    resource: {
      type: Object,
      default: function () {
        return {}
      }
    }
  },
  data () {
    var selectedColumnKeys = ['account', 'domain', 'usageType', 'usageid', 'startdate', 'enddate', 'rawusage', 'description']
    return {
      serverMetricsLoading: true,
      serverStats: {},
      loading: false,
      tableLoading: false,
      usageRecords: [],
      totalUsageRecords: 0,
      columnKeys: [...selectedColumnKeys,
        'zone', 'virtualmachinename', 'cpunumber', 'cpuspeed', 'memory', 'project', 'templateid', 'offeringid', 'size', 'type', 'vpcname'
      ],
      selectedColumnKeys: selectedColumnKeys,
      selectedColumns: [],
      columns: [],
      page: 1,
      pageSize: 20,
      usageTypes: [],
      domains: [],
      accounts: [],
      account: null,
      domain: null,
      usageType: null,
      usageTypeMap: {},
      usageRecordKeys: {},
      generateModal: false,
      downloadModal: false,
      viewModal: false,
      purgeModal: false,
      purgeDays: ref(365),
      downloadPercent: 0,
      downloadedRecords: 0,
      downloadTotalRecords: 0,
      downloadStatus: 'active',
      rangePresets: {},
      recordView: {}
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('listUsageRecords')
  },
  created () {
    this.rangePresets[this.$t('label.range.today')] = [dayjs(), dayjs()]
    this.rangePresets[this.$t('label.range.yesterday')] = [dayjs().add(-1, 'd'), dayjs().add(-1, 'd')]
    this.rangePresets[this.$t('label.range.last.1week')] = [dayjs().add(-1, 'w'), dayjs()]
    this.rangePresets[this.$t('label.range.last.2week')] = [dayjs().add(-2, 'w'), dayjs()]
    this.rangePresets[this.$t('label.range.last.1month')] = [dayjs().add(-1, 'M'), dayjs()]
    this.rangePresets[this.$t('label.range.last.3month')] = [dayjs().add(-90, 'M'), dayjs()]
    this.initForm()
    this.fetchData()
    this.updateColumns()
  },
  methods: {
    clearFilters () {
      this.formRef.value.resetFields()
      this.rules.type = {}
      this.domain = null
      this.account = null
      this.usageType = null
      this.page = 1
      this.pageSize = 20

      this.getAccounts()
    },
    disabledDate (current) {
      return current && current > dayjs().endOf('day')
    },
    filterOption (input, option) {
      return option.value.toUpperCase().indexOf(input.toUpperCase()) >= 0
    },
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        domain: null,
        account: null,
        type: null,
        id: null,
        dateRange: [],
        isRecursive: false
      })
      this.rules = reactive({
        dateRange: [{ type: 'array', required: true, message: this.$t('label.required') }],
        type: { type: 'string', required: false, message: this.$t('label.usage.records.usagetype.required') }
      })
    },
    fetchData () {
      this.listUsageServerMetrics()
      this.getUsageTypes()
      this.getAllUsageRecordColumns()
      this.getDomains()
      this.getAccounts()
      if (!this.$store.getters.customColumns[this.$store.getters.userInfo.id]) {
        this.$store.getters.customColumns[this.$store.getters.userInfo.id] = {}
        this.$store.getters.customColumns[this.$store.getters.userInfo.id][this.$route.path] = this.selectedColumnKeys
      } else {
        this.selectedColumnKeys = this.$store.getters.customColumns[this.$store.getters.userInfo.id][this.$route.path] || this.selectedColumnKeys
        this.updateSelectedColumns()
      }
      this.updateSelectedColumns()
    },
    viewUsageRecord (record) {
      this.viewModal = true
      this.recordView = record
    },
    handleResourceIdChange () {
      this.rules.type.required = this.form.id && this.form.id.trim()
    },
    handleTableChange (page, pageSize) {
      if (this.pageSize !== pageSize) {
        page = 1
      }
      if (this.page !== page || this.pageSize !== pageSize) {
        this.page = page
        this.pageSize = pageSize
        this.listUsageRecords()
        document.documentElement.scrollIntoView()
      }
    },
    listUsageServerMetrics () {
      this.serverMetricsLoading = true
      api('listUsageServerMetrics').then(json => {
        this.stats = []
        if (json && json.listusageservermetricsresponse && json.listusageservermetricsresponse.usageMetrics) {
          this.serverStats = json.listusageservermetricsresponse.usageMetrics
        }
      }).finally(f => {
        this.serverMetricsLoading = false
      })
    },
    handleSearch () {
      if (this.loading) return
      this.formRef.value.clearValidate()
      this.formRef.value.validate().then(() => {
        this.page = 1
        this.listUsageRecords()
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    selectAccount (value, option) {
      if (option && option.id) {
        this.account = option
      } else {
        this.account = null
        if (this.formRef?.value) {
          this.formRef.value.resetFields('account')
        }
      }
    },
    selectUsageType (value, option) {
      if (option && option.id) {
        this.usageType = option
      } else {
        this.usageType = null
        if (this.formRef?.value) {
          this.formRef.value.resetFields('type')
        }
      }
    },
    getDomains () {
      api('listDomains', { listAll: true }).then(json => {
        if (json && json.listdomainsresponse && json.listdomainsresponse.domain) {
          this.domains = [{ id: null, value: '' }, ...json.listdomainsresponse.domain.map(x => {
            return {
              id: x.id,
              value: x.path
            }
          })]
        }
      })
    },
    getAccounts (value, option) {
      var params = {
        listAll: true
      }
      if (option && option.id) {
        params.domainid = option.id
        this.domain = option
      } else {
        this.domain = null
        if (this.formRef?.value) {
          this.formRef.value.resetFields('domain')
        }
      }
      api('listAccounts', params).then(json => {
        if (json && json.listaccountsresponse && json.listaccountsresponse.account) {
          this.accounts = [{ id: null, value: '' }, ...json.listaccountsresponse.account.map(x => {
            return {
              id: x.id,
              value: x.name
            }
          })]
        }
      })
    },
    getParams (page, pageSize) {
      const formRaw = toRaw(this.form)
      const values = this.handleRemoveFields(formRaw)
      var params = {
        page: page || this.page,
        pagesize: pageSize || this.pageSize
      }
      if (values.dateRange) {
        if (this.$store.getters.usebrowsertimezone) {
          params.startdate = dayjs.utc(dayjs(values.dateRange[0]).startOf('day')).format('YYYY-MM-DD HH:mm:ss')
          params.enddate = dayjs.utc(dayjs(values.dateRange[0]).endOf('day')).format('YYYY-MM-DD HH:mm:ss')
        } else {
          params.startdate = dayjs(values.dateRange[0]).startOf('day').format('YYYY-MM-DD HH:mm:ss')
          params.enddate = dayjs(values.dateRange[1]).endOf('day').format('YYYY-MM-DD HH:mm:ss')
        }
      }
      if (values.domain) {
        params.domainid = this.domain.id
      }
      if (values.account) {
        params.accountid = this.account.id
      }
      if (values.type) {
        params.type = this.usageType.id
      }
      if (values.isRecursive) {
        params.isrecursive = true
      }
      if (values.id) {
        params.usageid = values.id
      }
      return params
    },
    listUsageRecords () {
      this.tableLoading = true
      this.loading = true
      var params = this.getParams()
      if (!(params.startdate && params.enddate)) {
        this.tableLoading = false
        this.loading = false
        return
      }
      api('listUsageRecords', params).then(json => {
        if (json && json.listusagerecordsresponse) {
          this.usageRecords = json?.listusagerecordsresponse?.usagerecord || []
          this.totalUsageRecords = json?.listusagerecordsresponse?.count || 0
          let count = 1
          for (var record of this.usageRecords) {
            // Set id to ensure a unique value of rowKey to avoid duplicates
            record.id = count++
          }
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.tableLoading = false
        this.loading = false
      })
    },
    getUsageTypes () {
      api('listUsageTypes').then(json => {
        if (json && json.listusagetypesresponse && json.listusagetypesresponse.usagetype) {
          this.usageTypes = [{ id: null, value: '' }, ...json.listusagetypesresponse.usagetype.map(x => {
            return {
              id: x.id,
              value: x.description
            }
          })]
          this.usageTypeMap = {}
          for (var usageType of this.usageTypes) {
            this.usageTypeMap[usageType.id] = usageType.value
          }
        }
      })
    },
    getTimeSince (date) {
      if (date === undefined || date === null) {
        return ''
      }
      return dayjs(date).fromNow()
    },
    updateSelectedColumns (key) {
      if (this.selectedColumnKeys.includes(key)) {
        this.selectedColumnKeys = this.selectedColumnKeys.filter(x => x !== key)
      } else {
        this.selectedColumnKeys.push(key)
      }
      this.updateColumns()
      if (!this.$store.getters.customColumns[this.$store.getters.userInfo.id]) {
        this.$store.getters.customColumns[this.$store.getters.userInfo.id] = {}
      }
      this.$store.getters.customColumns[this.$store.getters.userInfo.id][this.$route.path] = this.selectedColumnKeys
      this.$store.dispatch('SetCustomColumns', this.$store.getters.customColumns)
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        var title
        var dataIndex = columnKey
        var resizable = true
        switch (columnKey) {
          case 'templateid':
            title = this.$t('label.templatename')
            break
          case 'startdate':
            title = this.$t('label.start.date.and.time')
            break
          case 'enddate':
            title = this.$t('label.end.date.and.time')
            break
          case 'usageActions':
            title = this.$t('label.view')
            break
          case 'virtualmachinename':
            dataIndex = 'name'
            break
          default:
            title = this.$t('label.' + String(columnKey).toLowerCase())
        }
        this.columns.push({
          key: columnKey,
          title: title,
          dataIndex: dataIndex,
          resizable: resizable
        })
      }
      this.columns.push({
        key: 'usageActions',
        title: this.$t('label.view'),
        dataIndex: 'usageActions',
        resizable: false
      })
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    downloadRecords () {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        this.downloadModal = true
        this.downloadPercent = 0
        this.downloadStatus = 'active'
        this.loading = true
        var params = this.getParams(1, 0) // to get count
        api('listUsageRecords', params).then(json => {
          if (Object.getOwnPropertyNames(json.listusagerecordsresponse).length === 0 || json.listusagerecordsresponse.count === 0) {
            this.$notifyError({
              response: { data: null },
              message: this.$t('label.no.usage.records')
            })
            this.loading = false
            this.downloadStatus = 'exception'
            this.downloadModal = false
          } else {
            var totalRecords = json.listusagerecordsresponse.count
            this.downloadTotalRecords = totalRecords
            var pageSize = 500
            var totalPages = Math.ceil(totalRecords / pageSize)
            var records = []
            var promises = []
            for (var i = 1; i <= totalPages; i++) {
              var p = this.fetchUsageRecords({ ...params, page: i, pagesize: pageSize }).then(data => {
                records = records.concat(data)
                this.downloadPercent = Math.round((records.length / totalRecords) * 100)
                this.downloadedRecords += records.length
              })
              promises.push(p)
            }
            return Promise.allSettled(promises).then(() => {
              this.downloadPercent = 100
              this.downloadStatus = 'success'
              this.downloadCsv(records, 'usage-records.csv')
              this.loading = false
              this.downloadModal = false
            }).catch(error => {
              this.$notifyError(error)
              this.loading = false
              this.downloadStatus = 'exception'
              this.downloadModal = false
            })
          }
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    downloadCsv (records, filename) {
      var csv = toCsv({ keys: this.usageRecordKeys, data: records })
      const hiddenElement = document.createElement('a')
      hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(csv)
      hiddenElement.target = '_blank'
      hiddenElement.download = filename
      hiddenElement.click()
      hiddenElement.remove()
    },
    fetchUsageRecords (params) {
      return new Promise((resolve, reject) => {
        api('listUsageRecords', params).then(json => {
          return resolve(json.listusagerecordsresponse.usagerecord)
        }).catch(error => {
          return reject(error)
        })
      })
    },
    getAllUsageRecordColumns () {
      api('listApis', { name: 'listUsageRecords' }).then(json => {
        if (json && json.listapisresponse && json.listapisresponse.api) {
          var apiResponse = json.listapisresponse.api.filter(x => x.name === 'listUsageRecords')[0].response
          this.usageRecordKeys = []
          apiResponse.forEach(x => {
            if (x && x.name) {
              this.usageRecordKeys.push(x.name)
            }
          })
          this.usageRecordKeys.sort()
        }
      })
    },
    parseDates (date) {
      return this.$toLocaleDate(dayjs(date))
    },
    generateUsageRecords () {
      api('generateUsageRecords').then(json => {
        this.$message.success(this.$t('label.usage.records.generated'))
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.generateModal = false
      })
    },
    purgeUsageRecords () {
      var params = {
        interval: this.purgeDays
      }
      api('removeRawUsageRecords', params).then(json => {
        this.$message.success(this.$t('label.purge.usage.records.success'))
      }).catch(error => {
        this.$message.error(this.$t('label.purge.usage.records.error') + ': ' + error.message)
      }).finally(f => {
        this.purgeModal = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
.breadcrumb-card {
  margin-left: -24px;
  margin-right: -24px;
  margin-top: -16px;
  margin-bottom: 12px;
}
</style>
