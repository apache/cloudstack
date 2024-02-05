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
  <a-row style="margin-bottom: 5px;" :gutter="5">
    <a-col :span="4">
      <a-card
        class="server-metrics"
        :loading="serverMetricsLoading"
      >
        <a-statistic
          :title="$t('label.server')"
          :value="serverStats.hostname"
        />
      </a-card>
    </a-col>
    <a-col :span="4">
      <a-card
        class="server-metrics"
        :loading="serverMetricsLoading"
      >
        <a-statistic
          :title="$t('label.status')"
          :value="serverStats.state"
          :valueStyle="{ color: serverStats.state === 'UP' ? 'green' : 'red' }"
        />
      </a-card>
    </a-col>
    <a-col :span="4">
      <a-card
        class="server-metrics"
        :loading="serverMetricsLoading"
      >
        <a-statistic
          :title="$t('label.lastheartbeat')"
          :value="$toLocaleDate(serverStats.lastheartbeat)"
        />
        <a-card-meta :description="getTimeSince(serverStats.collectiontime)" />
      </a-card>
    </a-col>
    <a-col :span="4">
      <a-card
        class="server-metrics"
        :loading="serverMetricsLoading"
      >
        <a-statistic
          :title="$t('label.lastsuccessfuljob')"
          :value="$toLocaleDate(serverStats.lastsuccessfuljob)"
        />
        <a-card-meta :description="getTimeSince(serverStats.lastsuccessfuljob)" />
      </a-card>
    </a-col>
    <a-col :span="4">
      <a-card
        class="server-metrics"
        :loading="serverMetricsLoading"
      >
        <a-statistic
          :title="$t('label.collectiontime')"
          :value="$toLocaleDate(serverStats.collectiontime)"
        />
        <a-card-meta :description="getTimeSince(serverStats.collectiontime)" />
      </a-card>
    </a-col>
    <a-col :span="4">
      <a-card class="server-metrics">
        <a-row justify="center">
          <a-button
            type="primary"
            @click="generateModal = true"
          >
            <hdd-outlined />
            {{ $t('label.usage.records.generate') }}
          </a-button>
        </a-row>
        <br />
        <a-row justify="center">
          <a-button
            type="danger"
            @click="() => purgeModal = true"
          >
            <delete-outlined />
            {{ $t('label.usage.records.purge') }}
          </a-button>
        </a-row>
      </a-card>
    </a-col>
  </a-row>
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
          <a-col :span="5">
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
                    :dropdownMatchSelectWidth="400"
                  />
                </a-form-item>
              </a-col>
            </a-row>
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
                :dropdownMatchSelectWidth="400"
                @select="(value, option) => account = option"
              />
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item
              ref="type"
              name="type"
            >
              <a-auto-complete
                v-model:value="form.type"
                :options="usageTypes"
                :placeholder="$t('label.usagetype')"
                :filter-option="filterOption"
                @select="(value, option) => usageType = option"
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
              />
            </a-form-item>
          </a-col>
          <a-col :span="3">
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
  <a-row>
    <a-col :span="24">
      <list-view
        :tabLoading="tabLoading"
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
    @ok="generateModal = false"
    @cancel="generateModal = false"
  >
    <a-alert
      :message="$t('label.usage.records.generate.description')"
      type="info"
      show-icon
    >
      <template #icon><smile-outlined /></template>
    </a-alert>
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
        type="warning"
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
import { api } from '@/api'
import { toCsv } from '@/utils/util.js'
import { mixinForm } from '@/utils/mixin'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import ListView from '@/components/view/ListView'
import TooltipLabel from '@/components/widgets/TooltipLabel'

dayjs.extend(relativeTime)

export default {
  name: 'UsageRecords',
  mixins: [mixinForm],
  components: {
    Breadcrumb,
    ChartCard,
    ListView,
    TooltipLabel
  },
  data () {
    var selectedColumnKeys = ['usageActions', 'account', 'domain', 'usageType', 'usageid', 'startdate', 'enddate', 'rawusage', 'description']
    return {
      serverMetricsLoading: true,
      serverStats: {},
      loading: false,
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
      purgeModal: false,
      downloadModal: false,
      viewModal: false,
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
    this.updateColumns()
    this.fetchData()
  },
  methods: {
    clearFilters () {
      this.formRef.value.resetFields()
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
      this.purgeDays = ref(1)
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        dateRange: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.listUsageServerMetrics()
      this.getUsageTypes()
      this.getAllUsageRecordColumns()
      this.getDomains()
      this.getAccounts()
    },
    viewUsageRecord (record) {
      this.viewModal = true
      this.recordView = record
    },
    handleTableChange (page, pageSize) {
      if (this.pageSize !== pageSize) {
        this.page = 1
      }
      if (this.page !== page || this.pageSize !== pageSize) {
        this.page = page
        this.pageSize = pageSize
        this.listUsageRecords()
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
      this.formRef.value.validate().then(() => {
        this.page = 1
        this.listUsageRecords()
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    getDomains () {
      api('listDomains', { listAll: true }).then(json => {
        if (json && json.listdomainsresponse && json.listdomainsresponse.domain) {
          this.domains = json.listdomainsresponse.domain.map(x => {
            return {
              id: x.id,
              value: x.path
            }
          })
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
      }
      api('listAccounts', params).then(json => {
        if (json && json.listaccountsresponse && json.listaccountsresponse.account) {
          this.accounts = json.listaccountsresponse.account.map(x => {
            return {
              id: x.id,
              value: x.name
            }
          })
        }
      })
    },
    getParams (page, pageSize) {
      const formRaw = toRaw(this.form)
      const values = this.handleRemoveFields(formRaw)
      var params = {
        startdate: dayjs(values.dateRange[0]).format('YYYY-MM-DD'),
        enddate: dayjs(values.dateRange[1]).format('YYYY-MM-DD'),
        page: page || this.page,
        pagesize: pageSize || this.pageSize
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
      this.loading = true
      var params = this.getParams()

      api('listUsageRecords', params).then(json => {
        if (json && json.listusagerecordsresponse && json.listusagerecordsresponse.usagerecord) {
          this.usageRecords = json.listusagerecordsresponse.usagerecord
          this.totalUsageRecords = json.listusagerecordsresponse.count
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.loading = false
      })
    },
    getUsageTypes () {
      api('listUsageTypes').then(json => {
        if (json && json.listusagetypesresponse && json.listusagetypesresponse.usagetype) {
          this.usageTypes = json.listusagetypesresponse.usagetype.map(x => {
            return {
              id: x.usagetypeid,
              value: x.description
            }
          })
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
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        var title
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
            title = ''
            break
          default:
            title = this.$t('label.' + String(columnKey).toLowerCase())
        }
        this.columns.push({
          key: columnKey,
          title: title,
          dataIndex: columnKey
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    downloadRecords () {
      this.downloadModal = true
      this.downloadPercent = 0
      this.downloadStatus = 'active'
      this.loading = true
      var params = this.getParams(1, 0) // to get count
      api('listUsageRecords', params).then(json => {
        if (json && json.listusagerecordsresponse && json.listusagerecordsresponse.count) {
          if (json.listusagerecordsresponse.count === 0) {
            this.$message.error(this.$t('label.no.usage.records'))
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
        }
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
.server-metrics {
  min-height: 100%;
  text-align: center;
}
</style>
