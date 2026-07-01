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
      <export-to-csv-button :action="exportDataToCsv" />
      <bar-chart :chart-options="getBalancesChartOptions()" :chart-data="getBalancesChartData()"/>
      <a-table
        size="small"
        :loading="loading"
        :columns="columns"
        :dataSource="dataSource"
        :rowKey="record => record.date"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #title>
          {{ $t('label.currency') }}: <b>{{ currency }}</b>
        </template>
        <template #date="{ text }">
          {{ text }}
        </template>
        <template #lastBalanceHour="{ text }">
          {{ text }}
        </template>
        <template #balance="{ text }">
          {{ parseFloat(text).toFixed(2) }}
        </template>
      </a-table>
    </div>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import BarChart from '@/components/view/charts/BarChart.vue'
import * as dateUtils from '@/utils/date'
import * as exportUtils from '@/utils/export'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import ExportToCsvButton from '@/components/view/buttons/ExportToCsvButton.vue'
import * as chartUtils from '@/utils/chart'

export default {
  name: 'QuotaBalance',
  components: {
    FilterQuotaDataByPeriodView,
    BarChart,
    ExportToCsvButton
  },
  data () {
    return {
      loading: false,
      currency: '',
      dataSource: [],
      startDate: undefined,
      endDate: undefined
    }
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.date'),
          dataIndex: 'date',
          width: 'calc(100% / 3)',
          slots: { customRender: 'date' },
          sorter: (a, b) => a.lastBalance.localeCompare(b.lastBalance),
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.quota.last.balance'),
          dataIndex: 'lastBalanceHour',
          width: 'calc(100% / 3)',
          slots: { customRender: 'lastBalanceHour' },
          sorter: (a, b) => a.lastBalance.localeCompare(b.lastBalance),
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.balance'),
          dataIndex: 'balance',
          width: 'calc(100% / 3)',
          slots: { customRender: 'balance' },
          sorter: (a, b) => a.balance - b.balance
        }
      ]
    }
  },
  methods: {
    async fetchData (startDate, endDate) {
      if (this.loading) return

      this.startDate = dateUtils.parseDayJsObject({ value: startDate })
      this.endDate = dateUtils.parseDayJsObject({ value: endDate })
      this.dataSource = []
      this.loading = true

      try {
        const data = await this.getQuotaBalance() || {}
        this.currency = data.currency
        this.dataSource = this.getLastBalanceOfEachDate(data.balances)
      } finally {
        this.loading = false
      }
    },
    async getQuotaBalance () {
      const params = {
        accountid: this.$route.params?.id,
        startdate: this.startDate,
        enddate: this.endDate
      }

      return await getAPI('quotaBalance', params)
        .then(json => json.quotabalanceresponse.balance)
        .catch(error => { error && this.$notification.info({ message: this.$t('message.request.no.data') }) })
    },
    getLastBalanceOfEachDate (data) {
      if (!data) return []

      return data.reduce((reduced, currentItem) => {
        const lastBalance = dateUtils.parseDayJsObject({ value: currentItem.date, keepMoment: false })
        const balance = currentItem.balance
        const date = dateUtils.toLocaleDate({ date: lastBalance, dateOnly: true })
        const lastBalanceHour = dateUtils.toLocaleDate({ date: lastBalance, hourOnly: true })
        const found = reduced.find(item => item.date === date)

        if (!found) {
          reduced.push({ lastBalance, balance, date, lastBalanceHour })
        } else {
          if (found.lastBalance < lastBalance) {
            found.balance = balance
            found.lastBalance = lastBalance
            found.date = date
            found.lastBalanceHour = lastBalanceHour
          }
        }
        return reduced
      }, [])
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        headers: ['date', 'balance'],
        keys: ['lastBalance', 'balance'],
        fileName: `quota-balances-of-user-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    getBalancesChartData () {
      const datasets = []

      datasets.push({
        label: this.$t('label.balance'),
        data: this.dataSource.map(row => row.balance),
        ...chartUtils.getChartColorObject()
      })

      return {
        labels: this.dataSource.map(row => row.date),
        datasets
      }
    },
    getBalancesChartOptions () {
      return {
        scales: {
          xAxis: {
            type: 'time',
            time: {
              unit: chartUtils.getUnitToTimeCartesianAxis('day', this.dataSource.length),
              displayFormats: chartUtils.defaultDisplayFormats
            }
          }
        },
        plugins: {
          tooltip: {
            callbacks: {
              title: (tooltipItem) => dateUtils.dayjs(tooltipItem[0].label).format(chartUtils.defaultDisplayFormats.day),
              label: (tooltipItem) => parseFloat(tooltipItem.raw).toFixed(2)
            }
          }
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
</style>
