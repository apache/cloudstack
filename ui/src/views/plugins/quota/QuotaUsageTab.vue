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
    <filter-quota-data-by-period-view @fetchData="fetchData"/>

    <div v-if="dataSource.length > 0">
      <hr class="m-20-0" />
      <div class="chart-row">
        <a-space direction="vertical">
          <div>
            <a-radio-group
              v-model:value="graphType"
              buttonStyle="solid">
              <a-radio-button value="bar_chart">
                {{ $t('label.total') }}
              </a-radio-button>
              <a-radio-button value="line_chart">
                {{ $t('label.quota.statement.history') }}
              </a-radio-button>
              <a-radio-button value="incremental_chart">
                {{ $t('label.quota.statement.cumulative.history') }}
              </a-radio-button>
            </a-radio-group>
          </div>
        </a-space>
      </div>
      <div style="font-size: 18px">
        <strong> {{ $t('label.quota.usage.types.summary') }} </strong>
      </div>
      <export-to-csv-button :action="exportDataToCsv" />
      <bar-chart v-if="graphType === 'bar_chart'" :chart-options="getBarChartOptions()" :chart-data="getUsageTypeBarChartData()"/>
      <resource-stats-line-chart
        v-else
        :chart-labels="usageLineChartLabels"
        :chart-data="getEntryForCurrentGraphType(this.usageLineChartData)"
        :yAxisIncrementValue="getYAxisIncrement(getEntryForCurrentGraphType(this.YAxisMax.usageTypes))"
        :yAxisMeasurementUnit="''"
      />
      <a-table
        size="small"
        :loading="loading"
        :columns="columns"
        :dataSource="dataSource.filter(row => row.quota > 0)"
        :rowKey="record => record.name"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'name'">
            <a @click="handleSelectedTypeChange(`${record.type}-${record.name}`)">{{ $t(text) }}</a>
          </template>
          <template v-if="column.dataIndex === 'unit'">
            {{ $t(text) }}
          </template>
          <template v-if="column.dataIndex === 'quota'">
            <a-tooltip placement="right">
            <template #title>
              {{ text }}
            </template>
            <span class="dotted-underline">{{ parseFloat(text).toFixed(2) }}</span>
          </a-tooltip>
          </template>
        </template>
        <template #footer >
          <div style="text-align: right;">
            {{ $t('label.currency') }}: <b>{{ currency }}</b><br/>
            {{ $t('label.quota.total.consumption') }}:
            <a-tooltip placement="bottom">
              <template #title>
                {{ totalQuota }}
              </template>
              <b class="dotted-underline">{{ parseFloat(totalQuota).toFixed(2) }}</b>
            </a-tooltip>
          </div>
        </template>
      </a-table>

      <hr class="m-20-0" id="resource-by-type" />
      <strong>
        <tooltip-label style="font-size: 18px" :title="$t('label.quota.usage.resources.by.type')" :tooltip="$t('message.quota.usage.resource.warn')"/>
      </strong>
      <a-select
        v-model:value="selectedType"
        class="w-100"
        style="margin: 5px 0 10px 0px"
        show-search
        @change="handleSelectedTypeChange">
        <a-select-option
          v-for="quotaType of getQuotaTypesFiltered()"
          :value="`${quotaType.id}-${quotaType.type}`"
          :key="quotaType.id">
          {{ $t(quotaType.type) }}
        </a-select-option>
      </a-select>
      <export-to-csv-button v-if="dataSourceResource.length > 0" :action="exportResourcesToCsv" :label="`label.export.resources.csv`" />
      <bar-chart v-if="dataSourceResource.length > 0 && graphType === 'bar_chart'" :chart-options="getBarChartOptions()" :chart-data="getResourceBarChartData()"/>
      <resource-stats-line-chart
        v-else-if="dataSourceResource.length > 0 && graphType !== 'bar_chart'"
        :chart-labels="resourceLineChartLabels"
        :chart-data="getEntryForCurrentGraphType(this.resourceLineChartData)"
        :yAxisIncrementValue="getYAxisIncrement(getEntryForCurrentGraphType(this.YAxisMax.resources))"
        :yAxisMeasurementUnit="''"
      />
      <a-table
        size="small"
        :loading="loadingResources"
        :columns="resourceColumns"
        :dataSource="dataSourceResource"
        :rowKey="(record) => record.displayname"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #title v-if="dataSourceResource.length > 0">
          <div>{{ $t('label.currency') }}: <b>{{ currency }}</b></div>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'displayname'">
            <span v-if="!text">
              -
            </span>
            <span v-if="text === '<untraceable>' || !record.resourceid">
              {{ text }}
            </span>
            <a v-else @click="handleSelectedResourceChange(record.resourceid)">
              {{ text }}
            </a>
          </template>
          <template v-if="column.dataIndex === 'quotaconsumed'">
            <a-tooltip placement="right">
            <template #title>
              {{ text }}
            </template>
            <span class="dotted-underline">{{ parseFloat(text).toFixed(2) }}</span>
          </a-tooltip>
          </template>
        </template>
      </a-table>

      <hr class="m-20-0" id="details-by-resource" />
      <strong>
        <tooltip-label style="font-size: 18px" :title="$t('label.quota.usage.details.by.resource')"/>
      </strong>
      <a-select
        v-model:value="selectedResource"
        class="w-100"
        style="margin: 5px 0 10px 0px"
        show-search
        @change="handleSelectedResourceChange"
        :disabled="getResources().length == 0">
        <a-select-option
          v-for="item of getResources()"
          :value="item.id"
          :key="item.id">
          {{ $t(item.name) }}
        </a-select-option>
      </a-select>
      <export-to-csv-button v-if="dataSourceTariffs.length > 0" :action="exportResourceDetailsToCsv" :label="`label.export.details.csv`" />
      <bar-chart v-if="dataSourceTariffs.length > 0 && graphType === 'bar_chart'" :chart-options="getBarChartOptions()" :chart-data="getTariffsBarChartData()"/>
      <resource-stats-line-chart
        v-else-if="dataSourceTariffs.length > 0 && graphType !== 'bar_chart'"
        :chart-labels="tariffLineChartLabels"
        :chart-data="getEntryForCurrentGraphType(this.tariffLineChartData)"
        :yAxisIncrementValue="getYAxisIncrement(getEntryForCurrentGraphType(this.YAxisMax.tariffs))"
        :yAxisMeasurementUnit="''"
      />
      <a-table
        size="small"
        :loading="loadingTariffs"
        :columns="resourceDetailsColumns"
        :dataSource="dataSourceTariffs"
        :rowKey="record => record.tariffname + '-' + record.startdate"
        :pagination="false"
        :scroll="{ y: '55vh' }">
          <template #title v-if="dataSourceTariffs.length > 0">
            <div>{{ $t('label.currency') }}: <b>{{ currency }}</b></div>
          </template>
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'tariffname'">
              <a v-if="'quotaTariffList' in $store.getters.apis" :href="`#/quotatariff/${record.tariffid}`" target="_blank">
                {{ text }}
              </a>
              <span v-else>
                {{ text }}
              </span>
            </template>
            <template v-if="column.dataIndex === 'enddate'">
              {{ $toLocaleDate(text) }}
            </template>
            <template v-if="column.dataIndex === 'startdate'">
              {{ $toLocaleDate(text) }}
            </template>
            <template v-if="column.dataIndex === 'quotaconsumed'">
              <a-tooltip placement="right">
              <template #title>
                {{ text }}
              </template>
              <span class="dotted-underline">{{ parseFloat(text).toFixed(2) }}</span>
            </a-tooltip>
            </template>
          </template>
      </a-table>
    </div>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import BarChart from '@/components/view/charts/BarChart.vue'
import ResourceStatsLineChart from '@/components/view/stats/ResourceStatsLineChart.vue'
import ExportToCsvButton from '@/components/view/buttons/ExportToCsvButton.vue'
import { getChartColorObject } from '@/utils/chart'
import { getQuotaTypeByName, getQuotaTypes } from '@/utils/quota'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { downloadDataAsCsv } from '@/utils/util.js'
import * as dateUtils from '@/utils/date'

export default {
  name: 'QuotaUsageTab',
  components: {
    FilterQuotaDataByPeriodView,
    BarChart,
    ExportToCsvButton,
    ResourceStatsLineChart,
    TooltipLabel
  },
  data () {
    return {
      dataSource: [],
      selectedType: '',
      loadingResources: false,
      dataSourceResource: [],
      selectedResource: '',
      loadingTariffs: false,
      dataSourceTariffs: [],
      startDate: undefined,
      endDate: undefined,
      graphType: 'bar_chart',
      usageLineChartLabels: [],
      resourceLineChartLabels: [],
      tariffLineChartLabels: [],
      usageLineChartData: {},
      resourceLineChartData: {},
      tariffLineChartData: {},
      YAxisMax: {}
    }
  },
  watch: {
    graphType (newGraphType) {
      if (newGraphType === 'bar_chart') {
        return
      }
      this.prepareDataForUsageTypeLineGraph()
      if (!this.selectedType) {
        return
      }
      this.prepareDataForResourceLineGraph()
      if (!this.selectedResource) {
        return
      }
      this.prepareDataForTariffLineGraph()
    }
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.quota.type.name'),
          dataIndex: 'name',
          width: 'calc(100% / 3)',
          sorter: (a, b) => a.name.localeCompare(b.name)
        },
        {
          title: this.$t('label.quota.type.unit'),
          dataIndex: 'unit',
          width: 'calc(100% / 3)',
          sorter: (a, b) => a.unit.localeCompare(b.unit)
        },
        {
          title: this.$t('label.quota.consumed'),
          dataIndex: 'quota',
          width: 'calc(100% / 3)',
          sorter: (a, b) => a.quota - b.quota,
          defaultSortOrder: 'descend'
        }
      ]
    },
    resourceColumns () {
      return [
        {
          title: this.$t('label.resource'),
          dataIndex: 'displayname',
          width: '50%',
          sorter: (a, b) => a.displayname.localeCompare(b.displayname),
          defaultSortOrder: 'ascend'
        },
        {
          title: this.$t('label.quota.consumed'),
          dataIndex: 'quotaconsumed',
          width: '50%',
          sorter: (a, b) => a.quotaconsumed - b.quotaconsumed
        }
      ]
    },
    resourceDetailsColumns () {
      return [
        {
          title: this.$t('label.quota.tariff'),
          dataIndex: 'tariffname',
          sorter: (a, b) => a.tariffname.localeCompare(b.tariffname)
        },
        {
          title: this.$t('label.start.date'),
          dataIndex: 'startdate',
          sorter: (a, b) => a.startdate.localeCompare(b.startdate),
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.end.date'),
          dataIndex: 'enddate',
          sorter: (a, b) => a.enddate.localeCompare(b.enddate)
        },
        {
          title: this.$t('label.quota.consumed'),
          dataIndex: 'quotaconsumed',
          sorter: (a, b) => a.quotaconsumed - b.quotaconsumed
        }
      ]
    }
  },
  methods: {
    async fetchData (startDate, endDate, keepMoment = true) {
      if (this.loading) return

      this.startDate = dateUtils.parseDayJsObject({ value: startDate, keepMoment: keepMoment })
      this.endDate = dateUtils.parseDayJsObject({ value: endDate, keepMoment: keepMoment })
      this.loading = true
      this.dataSource = []
      this.dataSourceResource = []
      this.dataSourceTariffs = []
      this.selectedResource = ''
      this.selectedType = ''

      try {
        const quotaStatement = await this.getQuotaStatement({
          startdate: this.startDate,
          enddate: this.endDate
        })

        if (!quotaStatement) {
          return
        }

        this.dataSource = quotaStatement.quotausage.filter(row => row.quota !== 0)
        if (this.dataSource.length === 0) {
          this.$notification.info({ message: this.$t('message.request.no.data') })
        }

        this.currency = quotaStatement.currency
        this.totalQuota = quotaStatement.totalquota
        if (this.graphType !== 'bar_chart') {
          this.prepareDataForUsageTypeLineGraph()
        }
      } finally {
        this.loading = false
      }
    },
    async fetchResourceData () {
      if (this.selectedType === '' || this.loadingResources) return

      this.dataSourceResource = []
      this.loadingResources = true

      try {
        const quotaStatement = await this.getQuotaStatement({
          startdate: this.startDate,
          enddate: this.endDate,
          showresources: true,
          type: this.selectedType.split('-')[0]
        })

        this.dataSourceResource = quotaStatement.quotausage[0].resources.filter(row => row.quotaconsumed !== 0)
        if (this.dataSourceResource.length === 0) {
          this.$notification.info({ message: this.$t('message.request.no.data') })
        }

        if (this.graphType !== 'bar_chart') {
          this.prepareDataForResourceLineGraph()
        }
      } finally {
        this.loadingResources = false
      }
    },
    async fetchTariffData () {
      if (this.selectedResource === '' || this.loadingTariffs) return

      this.dataSourceTariffs = []
      this.loadingTariffs = true

      try {
        const quotaResourceStatement = await getAPI('quotaResourceStatement', {
          startdate: this.startDate,
          enddate: this.endDate,
          usagetype: this.selectedType.split('-')[0],
          id: this.selectedResource,
          accountid: this.$route.params?.id,
          ignoreproject: true
        }).then(json => json.quotaresourcestatementresponse?.quotaresourcestatement?.items || [])

        this.dataSourceTariffs = quotaResourceStatement.map(quotaUsage => ({
          ...quotaUsage,
          startdate: dateUtils.parseDayJsObject({ value: quotaUsage.startdate, keepMoment: false }),
          enddate: dateUtils.parseDayJsObject({ value: quotaUsage.enddate, keepMoment: false })
        })).filter(row => row.quotaconsumed !== 0)
        if (this.dataSourceTariffs.length === 0) {
          this.$notification.info({ message: this.$t('message.request.no.data') })
        }

        if (this.graphType !== 'bar_chart') {
          this.prepareDataForTariffLineGraph()
        }
      } finally {
        this.loadingTariffs = false
      }
    },
    async getQuotaStatement (apiParams) {
      const params = {
        ignoreproject: true,
        accountid: this.$route.params?.id,
        ...apiParams
      }

      return await getAPI('quotaStatement', params)
        .then(json => json.quotastatementresponse.statement || {})
    },
    getBarChartOptions () {
      return { responsive: true }
    },
    getUsageTypeBarChartData () {
      const datasets = []
      for (const row of this.dataSource) {
        datasets.push({
          label: this.$t(row.name),
          data: [row.quota],
          ...this.getColor(row)
        })
      }
      return { labels: [this.$t('label.quota.type.name')], datasets }
    },
    getResourceBarChartData () {
      const datasets = []
      for (const row of this.dataSourceResource) {
        datasets.push({
          label: row.displayname,
          data: [row.quotaconsumed],
          ...this.getColor(row)
        })
      }
      return { labels: [this.$t('label.resource')], datasets }
    },
    getTariffsBarChartData () {
      const aggregatedTariffs = this.aggregateTariffQuotas()
      const datasets = []
      for (const key in aggregatedTariffs) {
        datasets.push({
          label: key,
          data: [aggregatedTariffs[key]],
          ...this.getColor({ tariffname: key })
        })
      }
      return { labels: [this.$t('label.quota.tariff')], datasets }
    },
    aggregateTariffQuotas () {
      const tariffs = {}
      for (const row of this.dataSourceTariffs) {
        const currentValue = tariffs[row.tariffname] ?? 0
        tariffs[row.tariffname] = currentValue + row.quotaconsumed
      }
      return tariffs
    },
    setUsageTypeLineChartData () {
      this.usageLineChartLabels = this.getLineChartLabelsForData(this.dataSource)
      this.usageLineChartData = this.prepareLineChartData(this.dataSource, this.usageLineChartLabels)
    },
    setResourceLineChartData () {
      this.resourceLineChartLabels = this.getLineChartLabelsForData(this.dataSourceResource)
      this.resourceLineChartData = this.prepareLineChartData(this.dataSourceResource, this.resourceLineChartLabels)
    },
    setTariffLineChartData () {
      this.dataSourceTariffs.sort((a, b) => new Date(a.enddate) - new Date(b.enddate))
      const usageGroupedByTariffName = this.groupUsageByTariffName()

      const transformedData = Object.values(usageGroupedByTariffName)
      this.tariffLineChartLabels = this.getLineChartLabelsForData(transformedData)
      this.tariffLineChartData = this.prepareLineChartData(transformedData, this.tariffLineChartLabels)
    },
    groupUsageByTariffName () {
      const groupedData = {}
      this.dataSourceTariffs.forEach((obj) => {
        if (!(obj.tariffname in groupedData)) {
          groupedData[obj.tariffname] = { tariffname: obj.tariffname, history: [] }
        }
        groupedData[obj.tariffname].history.push(obj)
      })
      return groupedData
    },
    getLineChartLabelForDate (date) {
      return this.$toLocalDate(date)
    },
    getLineChartLabelsForData (data) {
      const lineChartLabels = [this.getLineChartLabelForDate(this.startDate)]

      for (const row of data) {
        let isPreviousZero = true
        for (let i = 0; i < row.history.length; i++) {
          const item = row.history[i]
          const isCurrentZero = item.quotaconsumed === 0

          if (isCurrentZero && isPreviousZero) {
            continue
          }

          if (isPreviousZero) {
            // Previous was zero, but we current is not. Push our startdate to have an accurate curve
            this.pushDateToLabelsIfNotPresent(lineChartLabels, this.getLineChartLabelForDate(item.startdate))
          }

          this.pushDateToLabelsIfNotPresent(lineChartLabels, this.getLineChartLabelForDate(item.enddate))
          isPreviousZero = isCurrentZero
        }
      }

      lineChartLabels.sort((a, b) => new Date(a) - new Date(b))
      return lineChartLabels
    },
    pushDateToLabelsIfNotPresent (lineChartLabels, date) {
      const hasNearDate = lineChartLabels.some(d => {
        const diff = Math.abs(new Date(date) - new Date(d).getTime())
        return diff < 5 * 1000 * 60 // Do not push the label if there is already one within 5 minutes
      })
      if (!hasNearDate) {
        lineChartLabels.push(date)
        return true
      }
      return false
    },
    calculatePaddedMax (values) {
      return Math.ceil(Math.max(...values) * 1.2)
    },
    getChartDataMaxValues (chartData) {
      return Object.values(chartData).map(item => Math.max(...item.data.map(obj => obj.stat)))
    },
    setYAxisMaxForUsageTypes () {
      this.YAxisMax.usageTypes = {
        incremental: this.calculatePaddedMax(this.getChartDataMaxValues(this.usageLineChartData.incremental)),
        history: this.calculatePaddedMax(this.getChartDataMaxValues(this.usageLineChartData.history))
      }
    },
    setYAxisMaxForResources () {
      this.YAxisMax.resources = {
        incremental: this.calculatePaddedMax(this.getChartDataMaxValues(this.resourceLineChartData.incremental)),
        history: this.calculatePaddedMax(this.getChartDataMaxValues(this.resourceLineChartData.history))
      }
    },
    setYAxisMaxForTariffs () {
      this.YAxisMax.tariffs = {
        incremental: this.calculatePaddedMax(this.getChartDataMaxValues(this.tariffLineChartData.incremental)),
        history: this.calculatePaddedMax(this.getChartDataMaxValues(this.tariffLineChartData.history))
      }
    },
    getEntryForCurrentGraphType (data) {
      if (this.graphType === 'line_chart') {
        return data.history
      }
      return data.incremental
    },
    prepareDataForUsageTypeLineGraph () {
      this.setUsageTypeLineChartData()
      this.setYAxisMaxForUsageTypes()
    },
    prepareDataForResourceLineGraph () {
      this.setResourceLineChartData()
      this.setYAxisMaxForResources()
    },
    prepareDataForTariffLineGraph () {
      this.setTariffLineChartData()
      this.setYAxisMaxForTariffs()
    },
    prepareLineChartData (data, lineChartLabels) {
      const chartData = { history: [], incremental: [] }

      for (const row of data) {
        const datasetName = this.$t(this.getName(row))
        const color = this.getColor(row)

        const historyDataset = { label: datasetName, data: [], fill: false, ...color }
        const incrementalDataset = { label: datasetName, data: [], fill: false, ...color }

        let historyIndex = 0
        let accumulatedQuota = 0

        for (const label of lineChartLabels) {
          const periodQuota = this.calculateQuotaForPeriod(row.history, label, historyIndex)
          historyIndex = periodQuota.nextIndex

          accumulatedQuota += periodQuota.value
          historyDataset.data.push({ stat: periodQuota.value })
          incrementalDataset.data.push({ stat: accumulatedQuota })
        }

        chartData.history.push(historyDataset)
        chartData.incremental.push(incrementalDataset)
      }

      return chartData
    },
    calculateQuotaForPeriod (historyItems, label, startIndex) {
      let quota = 0
      let currentIndex = startIndex

      while (currentIndex < historyItems.length) {
        const item = historyItems[currentIndex]
        if (this.getLineChartLabelForDate(item.enddate) > label) {
          break
        }
        quota += item.quotaconsumed
        currentIndex++
      }

      return { value: quota, nextIndex: currentIndex }
    },
    getColor (row) {
      const quotaType = getQuotaTypeByName(row.name)
      if (quotaType?.chartColor) {
        return getChartColorObject(quotaType.chartColor)
      }
      return getChartColorObject(this.textToDeterministicColor(this.getName(row)))
    },
    textToDeterministicColor (text) {
      let hash = 0

      for (let i = text.length - 1; i >= 0; i--) {
        hash = ((hash << 5) - hash) + text.charCodeAt(i)
        hash = hash & hash
      }

      const hash1 = Math.abs(hash)
      const hash2 = Math.abs(hash * 73)
      const hash3 = Math.abs(hash * 97)

      let color = '#'
      color += (hash1 & 0xFF).toString(16).padStart(2, '0')
      color += (hash2 & 0xFF).toString(16).padStart(2, '0')
      color += (hash3 & 0xFF).toString(16).padStart(2, '0')

      return color
    },
    getName (row) {
      if (row.name) {
        // Translate the usage type's name
        return this.$t(row.name)
      }
      return row.displayname || row.tariffname
    },
    getYAxisIncrement (max) {
      if (max < 1) {
        return 1
      }
      return Math.pow(10, Math.floor(Math.log10(max)))
    },
    exportDataToCsv () {
      downloadDataAsCsv({
        data: this.dataSource,
        keys: ['type', 'name', 'unit', 'quota'],
        fileName: `quota-usage-of-account-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    exportResourcesToCsv () {
      downloadDataAsCsv({
        data: this.dataSourceResource.map(row => ({ ...row, name: row.displayname })),
        keys: ['resourceid', 'name', 'quotaconsumed'],
        fileName: `quota-usage-of-resources-of-type-${this.selectedType}-of-account-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    exportResourceDetailsToCsv () {
      downloadDataAsCsv({
        data: this.dataSourceTariffs,
        keys: ['tariffid', 'tariffname', 'startdate', 'enddate', 'quotaconsumed'],
        fileName: `detailed-quota-usage-of-resource-${this.selectedResource}-of-type-${this.selectedType}-of-account-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    getQuotaTypesFiltered () {
      const quotaTypesRetrieved = this.dataSource.map(item => item.name)
      return getQuotaTypes().filter((item) => quotaTypesRetrieved.includes(item.type))
    },
    getResources () {
      return this.dataSourceResource.filter(item => item.resourceid).map(item => ({ id: item.resourceid, name: item.displayname }))
    },
    async handleSelectedTypeChange (value) {
      this.selectedType = value
      this.selectedResource = ''
      this.dataSourceTariffs = []
      document.getElementById('resource-by-type').scrollIntoView({ behavior: 'smooth' })
      await this.fetchResourceData()
    },
    async handleSelectedResourceChange (value) {
      this.selectedResource = value
      document.getElementById('details-by-resource').scrollIntoView({ behavior: 'smooth' })
      await this.fetchTariffData()
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
</style>
