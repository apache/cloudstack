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
              buttonStyle="solid"
              @change="handlePeriodChange">
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
      <bar-chart v-if="graphType === 'bar_chart'" :chart-options="getUsageTypeChartOptions()" :chart-data="getUsageTypeBarChartData()"/>
      <resource-stats-line-chart
        v-else
        :chart-labels="usageLineChartLabels"
        :chart-data="getGraphType(this.usageTypeChartData)"
        :yAxisIncrementValue="getYaxisIncrement(this.getGraphType(this.yAxisMax.usageType))"
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
        <template #nameRedirect="props">
          <a @click="handleSelectedTypeChange(`${props.record.type}-${props.record.name}`)">{{ $t(props.text) }}</a>
        </template>
        <template #unit="{ text }">
          {{ $t(text) }}
        </template>
        <template #quota="{ text }">
          <a-tooltip placement="right">
            <template #title>
              {{ text }}
            </template>
            <span class="dotted-underline">{{ parseFloat(text).toFixed(2) }}</span>
          </a-tooltip>
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
        v-model="selectedType"
        @change="handleSelectedTypeChange">
        <a-select-option
          v-for="quotaType of getQuotaTypesFiltered()"
          :value="`${quotaType.id}-${quotaType.type}`"
          :key="quotaType.id">
          {{ $t(quotaType.type) }}
        </a-select-option>
      </a-select>
      <export-to-csv-button v-if="dataSourceResource.length > 0" :action="exportResourcesToCsv" :label="`label.export.resources.csv`" />
      <bar-chart v-if="dataSourceResource.length > 0 && graphType === 'bar_chart'" :chart-options="getUsageTypeChartOptions()" :chart-data="getResourceByUsageTypeBarChartData()"/>
      <resource-stats-line-chart
        v-else-if="dataSourceResource.length > 0 && graphType !== 'bar_chart'"
        :chart-labels="resourceLineChartLabels"
        :chart-data="getGraphType(this.resourceByTypeChartData)"
        :yAxisIncrementValue="getYaxisIncrement(this.getGraphType(this.yAxisMax.resourceByType))"
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
        <template #displayName="props">
          <span v-if="!props.text">
            -
          </span>
          <span v-if="!props.text === '<untraceable>' || !props.record.resourceid">
            {{ props.text }}
          </span>
          <a v-else @click="handleSelectedResourceChange(props.record.resourceid)">
            {{ props.text }}
          </a>
        </template>
        <template #quota="{ text }">
          <a-tooltip placement="right">
            <template #title>
              {{ text }}
            </template>
            <span class="dotted-underline">{{ parseFloat(text).toFixed(2) }}</span>
          </a-tooltip>
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
        v-model="selectedResource"
        @change="handleSelectedResourceChange"
        :disabled="getResources().length == 0">
        <a-select-option
          v-for="item of getResources()"
          :value="item.id"
          :key="item.id">
          {{ $t(item.name) }}
        </a-select-option>
      </a-select>
      <export-to-csv-button v-if="dataSourceResourceDetails.length > 0" :action="exportResourceDetailsToCsv" :label="`label.export.details.csv`" />
      <bar-chart v-if="dataSourceResourceDetails.length > 0 && graphType === 'bar_chart'" :chart-options="getUsageTypeChartOptions()" :chart-data="getResourceDetailsBarChartData()"/>
      <resource-stats-line-chart
        v-else-if="dataSourceResourceDetails.length > 0 && graphType !== 'bar_chart'"
        :chart-labels="tariffLineChartLabels"
        :chart-data="getGraphType(this.usageResourceDetailsChartData)"
        :yAxisIncrementValue="getYaxisIncrement(this.getGraphType(this.yAxisMax.resourceDetails))"
        :yAxisMeasurementUnit="''"
      />
      <a-table
        size="small"
        :loading="loadingResourceDetails"
        :columns="resourceDetailsColumns"
        :dataSource="dataSourceResourceDetails"
        :rowKey="record => record.tariffname + '-' + record.startDate"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #title v-if="dataSourceResourceDetails.length > 0">
          <div>{{ $t('label.currency') }}: <b>{{ currency }}</b></div>
        </template>
        <template #tariffName="props">
          <a v-if="'quotaTariffList' in $store.getters.apis" :href="`#/quotatariff/${props.record.tariffid}`" target="_blank">
            {{ props.text }}
          </a>
          <span v-else>
            {{ props.text }}
          </span>
        </template>
        <template #endDate="{ text }">
          {{ $toLocaleDate(text) }}
        </template>
        <template #startDate="{ text }">
          {{ $toLocaleDate(text) }}
        </template>
        <template #quota="{ text }">
          <a-tooltip placement="right">
            <template #title>
              {{ text }}
            </template>
            <span class="dotted-underline">{{ parseFloat(text).toFixed(2) }}</span>
          </a-tooltip>
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
import * as exportUtils from '@/utils/export'
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
      loadingResourceDetails: false,
      dataSourceResourceDetails: [],
      startDate: undefined,
      endDate: undefined,
      graphType: 'bar_chart',
      usageLineChartLabels: [],
      resourceLineChartLabels: [],
      tariffLineChartLabels: [],
      usageTypeChartData: {},
      resourceByTypeChartData: {},
      usageResourceDetailsChartData: {},
      yAxisMax: {}
    }
  },
  watch: {
    graphType (newGraphType) {
      if (newGraphType === 'bar_chart') {
        return
      }
      this.fetchDataForUsageTypeLineGraph()
      if (!this.selectedType) {
        return
      }
      this.fetchDataForResourceByTypeLineGraph()
      if (!this.selectedResource) {
        return
      }
      this.fetchDataForResourceDetailsLineGraph()
    }
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.quota.type.name'),
          dataIndex: 'name',
          width: 'calc(100% / 3)',
          slots: { customRender: 'nameRedirect' },
          sorter: (a, b) => a.name.localeCompare(b.name)
        },
        {
          title: this.$t('label.quota.type.unit'),
          dataIndex: 'unit',
          width: 'calc(100% / 3)',
          slots: { customRender: 'unit' },
          sorter: (a, b) => a.unit.localeCompare(b.unit)
        },
        {
          title: this.$t('label.quota.consumed'),
          dataIndex: 'quota',
          width: 'calc(100% / 3)',
          slots: { customRender: 'quota' },
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
          slots: { customRender: 'displayName' },
          sorter: (a, b) => a.displayname.localeCompare(b.displayname),
          defaultSortOrder: 'ascend'
        },
        {
          title: this.$t('label.quota.consumed'),
          dataIndex: 'quotaconsumed',
          width: '50%',
          slots: { customRender: 'quota' },
          sorter: (a, b) => a.quotaconsumed - b.quotaconsumed
        }
      ]
    },
    resourceDetailsColumns () {
      return [
        {
          title: this.$t('label.quota.tariff'),
          dataIndex: 'tariffname',
          slots: { customRender: 'tariffName' },
          sorter: (a, b) => a.tariffname.localeCompare(b.tariffname)
        },
        {
          title: this.$t('label.start.date'),
          dataIndex: 'startDate',
          slots: { customRender: 'startDate' },
          sorter: (a, b) => a.startDate.localeCompare(b.startDate),
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.end.date'),
          dataIndex: 'endDate',
          slots: { customRender: 'endDate' },
          sorter: (a, b) => a.endDate.localeCompare(b.endDate)
        },
        {
          title: this.$t('label.quota.consumed'),
          dataIndex: 'quotaconsumed',
          slots: { customRender: 'quota' },
          sorter: (a, b) => a.quotaused - b.quotaused
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
      this.dataSourceResourceDetails = []
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
        this.currency = quotaStatement.currency
        this.totalQuota = quotaStatement.totalquota
        if (this.graphType !== 'bar_chart') {
          this.fetchDataForUsageTypeLineGraph()
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
          startDate: this.startDate,
          endDate: this.endDate,
          showResources: true,
          type: this.selectedType.split('-')[0]
        })

        this.dataSourceResource = quotaStatement.quotausage[0].resources
        this.dataSourceResource = this.dataSourceResource.filter(row => row.quotaconsumed !== 0)
        if (this.graphType !== 'bar_chart') {
          this.fetchDataForResourceByTypeLineGraph()
        }
      } finally {
        this.loadingResources = false
      }
    },
    async fetchResourceDetailsData () {
      if (this.selectedResource === '' || this.loadingResourceDetails) return

      this.dataSourceResourceDetails = []
      this.loadingResourceDetails = true

      try {
        this.dataSourceResourceDetails = await getAPI('quotaResourceStatement', {
          startDate: this.startDate,
          endDate: this.endDate, // TODO: remover
          usageType: this.selectedType.split('-')[0],
          id: this.selectedResource,
          accountId: this.$route.params?.id
        }).then(json => json.quotaresourcestatementresponse?.quotaresourcestatement?.items || [])
          .catch(error => { error && this.$notification.info({ message: this.$t('message.request.no.data') }) })

        this.dataSourceResourceDetails = this.dataSourceResourceDetails.map(detail => ({
          ...detail,
          startDate: dateUtils.parseDayJsObject({ value: detail.startdate }),
          endDate: dateUtils.parseDayJsObject({ value: detail.enddate })
        })).filter(row => row.quotaconsumed !== 0)
        if (this.graphType !== 'bar_chart') {
          this.fetchDataForResourceDetailsLineGraph()
        }
      } finally {
        this.loadingResourceDetails = false
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
        .catch(error => {
          if (error) {
            this.$notification.info({ message: this.$t('message.request.no.data') })
          }
        })
    },
    getUsageTypeChartOptions () {
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
    getResourceByUsageTypeBarChartData () {
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
    getResourceDetailsBarChartData () {
      const tariffs = {}
      for (const row of this.dataSourceResourceDetails) {
        if (tariffs[row.tariffname] === undefined) {
          tariffs[row.tariffname] = row.quotaconsumed
        } else {
          tariffs[row.tariffname] += row.quotaconsumed
        }
      }

      const datasets = []
      for (const key in tariffs) {
        datasets.push({
          label: key,
          data: [tariffs[key]],
          ...this.getColor({ tariffname: key })
        })
      }

      return { labels: [this.$t('label.quota.tariff')], datasets }
    },
    setUsageTypeChartData () {
      this.usageLineChartLabels = this.getLineChartLabelsForData(this.dataSource)
      this.usageTypeChartData = this.getChartData(this.dataSource, this.usageLineChartLabels)
    },
    setResourceByTypeChartData () {
      this.resourceLineChartLabels = this.getLineChartLabelsForData(this.dataSourceResource)
      this.resourceByTypeChartData = this.getChartData(this.dataSourceResource, this.resourceLineChartLabels)
    },
    setResourceDetailsChartData () {
      const transformedData = {}

      this.dataSourceResourceDetails.sort((a, b) => new Date(a.enddate) - new Date(b.enddate))

      this.dataSourceResourceDetails.forEach((obj) => {
        if (!(obj.tariffname in transformedData)) {
          transformedData[obj.tariffname] = { tariffname: obj.tariffname, history: [] }
        }
        transformedData[obj.tariffname].history.push(obj)
      })

      const filteredDataSource = Object.values(transformedData)

      this.tariffLineChartLabels = this.getLineChartLabelsForData(filteredDataSource)
      this.usageResourceDetailsChartData = this.getChartData(filteredDataSource, this.tariffLineChartLabels)
    },
    getAdjustedDate (date) {
      return this.$toLocalDate(date)
    },
    getLineChartLabelsForData (data) {
      const lineChartLabels = [this.getAdjustedDate(this.startDate)]
      for (const row of data) {
        let lastIsZero = true
        for (let i = 0; i < row.history.length; i++) {
          const item = row.history[i]
          if (item.quotaconsumed === 0) {
            if (lastIsZero) {
              continue
            }
            lastIsZero = true
          }
          if (lastIsZero) {
            this.pushDateToLabelsIfNotPresent(lineChartLabels, this.getAdjustedDate(item.startdate))
          }
          this.pushDateToLabelsIfNotPresent(lineChartLabels, this.getAdjustedDate(item.enddate))
          lastIsZero = false
        }
      }
      this.pushDateToLabelsIfNotPresent(lineChartLabels, this.getAdjustedDate(this.endDate))
      lineChartLabels.sort((a, b) => new Date(a) - new Date(b))
      return lineChartLabels
    },
    pushDateToLabelsIfNotPresent (lineChartLabels, date) {
      const hasDate = lineChartLabels.some(d => {
        const diff = Math.abs(new Date(date) - new Date(d).getTime())
        return diff < 5 * 1000
      })
      if (!hasDate) {
        lineChartLabels.push(date)
        return true
      }
      return false
    },
    setYAxisMax () {
      this.yAxisMax.usageType = {}
      this.yAxisMax.usageType.incremental = Math.round(Math.max(...this.dataSource.map(obj => obj.quotaconsumed)) * 1.2)
      const max = []
      for (const row of this.dataSource) {
        max.push(Math.max(...Object.values(row.history.map(h => h.quotaconsumed))))
      }
      this.yAxisMax.usageType.history = Math.max(...max)
    },
    setYAxisInitialMaxResourceByType () {
      this.yAxisMax.resourceByType = {}

      this.yAxisMax.resourceByType.incremental = Math.max(...this.dataSourceResource.map(obj => obj.quotaconsumed))
      const max = []
      for (const row of this.dataSourceResource) {
        max.push(Math.max(...Object.values(row.history.map(h => h.quotaconsumed))))
      }
      this.yAxisMax.resourceByType.history = Math.round(Math.max(...max) * 1.2)
    },
    setYAxisInitialMaxResourceDetails () {
      this.yAxisMax.resourceDetails = {}
      const historyMax = []
      const incrementalMax = []
      for (const row in this.usageResourceDetailsChartData.history) {
        historyMax.push(Math.max(...this.usageResourceDetailsChartData.history[row].data.map(obj => obj.stat)))
      }
      for (const row in this.usageResourceDetailsChartData.incremental) {
        incrementalMax.push(Math.max(...this.usageResourceDetailsChartData.incremental[row].data.map(obj => obj.stat)))
      }
      this.yAxisMax.resourceDetails.history = (Math.max(...historyMax))
      this.yAxisMax.resourceDetails.incremental = (Math.max(...incrementalMax))
    },
    getGraphType (data) {
      if (this.graphType === 'line_chart') {
        return data.history
      }
      return data.incremental
    },
    fetchDataForUsageTypeLineGraph () {
      this.setUsageTypeChartData()
      this.setYAxisMax()
    },
    fetchDataForResourceByTypeLineGraph () {
      this.setResourceByTypeChartData()
      this.setYAxisInitialMaxResourceByType()
    },
    fetchDataForResourceDetailsLineGraph () {
      this.setResourceDetailsChartData()
      this.setYAxisInitialMaxResourceDetails()
    },
    getChartData (data, lineChartLabels) {
      const chartData = {
        history: [],
        incremental: []
      }

      for (const row of data) {
        const name = this.$t(this.getName(row))
        const historyData = { label: name, data: [], fill: false, ...this.getColor(row) }
        const incrementalData = { label: name, data: [], fill: false, ...this.getColor(row) }

        let i = 0
        let accumulatedQuota = 0

        for (const label of lineChartLabels) {
          let labelQuota = 0
          while (true) {
            if (i >= row.history.length) {
              break
            }
            const item = row.history[i]
            if (this.getAdjustedDate(item.enddate) > label) {
              break
            }
            labelQuota += item.quotaconsumed
            i++
          }
          accumulatedQuota += labelQuota
          historyData.data.push({ stat: labelQuota })
          incrementalData.data.push({ stat: accumulatedQuota })
        }

        chartData.history.push(historyData)
        chartData.incremental.push(incrementalData)
      }

      return chartData
    },
    getColor (row) {
      const quotaType = getQuotaTypeByName(row.name)
      if (quotaType) {
        return getChartColorObject(quotaType.chartColor)
      }
      console.log(row)
      return getChartColorObject(this.textToDeterministicColor(this.getName(row)))
    },
    textToDeterministicColor (text) {
      console.log(text)
      let hash = 0

      for (let i = text.length - 1; i >= 0; i--) {
        hash = ((hash << 5) - hash) + text.charCodeAt(i)
        hash = hash & hash
      }

      // Use multiple parts of the hash for better color variance
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
        return this.$t(row.name)
      }
      return row.displayname || row.tariffname
    },
    getYaxisIncrement (max) {
      if (max < 1) {
        return 1
      }
      return Math.pow(10, Math.floor(Math.log10(max)))
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        keys: ['type', 'name', 'unit', 'quota'],
        fileName: `quota-usage-of-account-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    exportResourcesToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSourceResource.map(row => ({ ...row, name: row.displayname })),
        keys: ['resourceid', 'name', 'quotaconsumed'],
        fileName: `quota-usage-of-resources-of-type-${this.selectedType}-of-account-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    exportResourceDetailsToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSourceResourceDetails.map(row => ({
          ...row,
          startdate: dateUtils.parseDayJsObject({ value: row.startdate, keepMoment: false }),
          enddate: dateUtils.parseDayJsObject({ value: row.enddate, keepMoment: false })
        })),
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
      this.dataSourceResourceDetails = []
      document.getElementById('resource-by-type').scrollIntoView({ behavior: 'smooth' })
      await this.fetchResourceData()
    },
    async handleSelectedResourceChange (value) {
      if (!value) return

      this.selectedResource = value
      document.getElementById('details-by-resource').scrollIntoView({ behavior: 'smooth' })
      await this.fetchResourceDetailsData()
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
</style>
