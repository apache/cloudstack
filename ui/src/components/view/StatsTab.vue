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
  <div class="container">
    <a-modal
      v-model:visible="showFilterStatsModal"
      :title="$t('label.select.period')"
      :maskClosable="false"
      :footer="null">
      <date-time-filter
        :startDateProp="startDate"
        :endDateProp="endDate"
        :allDataMessage="$t('message.alert.show.all.stats.data')"
        @closeAction="closeAction"
        @onSubmit="handleSubmit"/>
    </a-modal>
    <a-modal
      v-model:visible="showResourceInfoModal"
      :title="resourceInfoModalTitle"
      :footer="null">
      <resource-stats-info :resourceType="resourceTypeToShowInfo" :key="resourceTypeToShowInfo"/>
    </a-modal>
    <div class="chart-row">
      <a-space direction="vertical">
        <div>
          <a-radio-group
            v-model:value="durationSelectorValue"
            buttonStyle="solid"
            @change="handleDurationChange">
            <a-radio-button value="">
              {{ $t('1 hour') }}
            </a-radio-button>
            <a-radio-button value="6hours" v-if="statsRetentionTime >= 60">
              {{ $t('label.duration.6hours') }}
            </a-radio-button>
            <a-radio-button value="12hours" v-if="statsRetentionTime >= 6 * 60">
              {{ $t('label.duration.12hours') }}
            </a-radio-button>
            <a-radio-button value="day" v-if="statsRetentionTime >= 12 * 60">
              {{ $t('label.duration.24hours') }}
            </a-radio-button>
            <a-radio-button value="week" v-if="statsRetentionTime >= 24 * 60">
              {{ $t('label.duration.7days') }}
            </a-radio-button>
            <a-radio-button value="custom">
              {{ $t('label.duration.custom') }}
            </a-radio-button>
          </a-radio-group>
          <InfoCircleOutlined class="info-icon" :title="$t('label.see.more.info.shown.charts')" @click="onClickShowResourceInfoModal('CHART')"/>
        </div>
        <div class="ant-tag" v-if="durationSelectorValue==='custom'">
          <a-button @click="openFilter()">
            <FilterOutlined/>
          </a-button>
          <span v-html="formatedPeriod"></span>
        </div>
      </a-space>
    </div>
    <div v-if="loaded">
      <div v-if="chartLabels.length > 0">
        <a-row class="chart-row" v-if="resourceIsVirtualMachine">
          <a-col>
            <strong>CPU</strong>
            <InfoCircleOutlined class="info-icon" :title="$t('label.see.more.info.cpu.usage')" @click="onClickShowResourceInfoModal('CPU')"/>
            <resource-stats-line-chart
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.cpu"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="10"
              :yAxisMeasurementUnit="'%'"
            />
          </a-col>
        </a-row>
        <a-row class="chart-row" v-if="resourceIsVirtualMachine">
          <a-col>
            <strong>{{ $t('label.memory') }}</strong>
            <InfoCircleOutlined class="info-icon" :title="$t('label.see.more.info.memory.usage')" @click="onClickShowResourceInfoModal('MEM')"/>
            <a-select class="chart-type-select" v-model:value="selectedMemoryChartType">
              <a-select-option v-for="(type, typeIndex) in memoryChartTypes" :key="typeIndex">
                {{ type }}
              </a-select-option>
            </a-select>
            <a-select v-model:value="selectedMemoryUsageType">
              <a-select-option v-for="(type, typeIndex) in memoryUsageTypes" :key="typeIndex">
                {{ type }}
              </a-select-option>
            </a-select>
            <a-select v-model:value="selectedMemoryUnitOfMeasurement" v-if="selectedMemoryChartType === 0">
              <a-select-option v-for="unit in memoryUnitsOfMeasurement" :key="unit">
                {{ unit }}
              </a-select-option>
            </a-select>
            <resource-stats-line-chart
              v-if="selectedMemoryChartType === 0 && selectedMemoryUsageType === 0 && selectedMemoryUnitOfMeasurement === 'MB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.memory.rawData.used.inMB"
              :yAxisInitialMax="10"
              :yAxisIncrementValue="100"
              :yAxisMeasurementUnit="' MB'"
            />
            <resource-stats-line-chart
              v-if="selectedMemoryChartType === 0 && selectedMemoryUsageType === 0 && selectedMemoryUnitOfMeasurement === 'GB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.memory.rawData.used.inGB"
              :yAxisInitialMax="1"
              :yAxisIncrementValue="1"
              :yAxisMeasurementUnit="' GB'"
            />
            <resource-stats-line-chart
              v-if="selectedMemoryChartType === 0 && selectedMemoryUsageType === 1 && selectedMemoryUnitOfMeasurement === 'MB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.memory.rawData.free.inMB"
              :yAxisInitialMax="10"
              :yAxisIncrementValue="100"
              :yAxisMeasurementUnit="' MB'"
            />
            <resource-stats-line-chart
              v-if="selectedMemoryChartType === 0 && selectedMemoryUsageType === 1 && selectedMemoryUnitOfMeasurement === 'GB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.memory.rawData.free.inGB"
              :yAxisInitialMax="1"
              :yAxisIncrementValue="1"
              :yAxisMeasurementUnit="' GB'"
            />
            <resource-stats-line-chart
              v-if="selectedMemoryChartType === 1 && selectedMemoryUsageType === 0"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.memory.percentage.used"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="10"
              :yAxisMeasurementUnit="'%'"
            />
            <resource-stats-line-chart
              v-if="selectedMemoryChartType === 1 && selectedMemoryUsageType === 1"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.memory.percentage.free"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="10"
              :yAxisMeasurementUnit="'%'"
            />
          </a-col>
        </a-row>
        <a-row class="chart-row" v-if="diskStatsAvailable">
          <a-col>
            <strong>{{ $t('label.disk') }}</strong>
            <InfoCircleOutlined class="info-icon" :title="$t('label.see.more.info.disk.usage')" @click="onClickShowResourceInfoModal('DISK')"/>
            <div class="chart-row-inner">
              {{ $t('label.iops') }}
            </div>
            <resource-stats-line-chart
              v-if="selectedDiskChartType === 0"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.disk.iops"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="100"
              :yAxisMeasurementUnit="' IOPS'"
            />
            <div class="chart-row-inner">
              {{ $t('label.read.and.write') }}
              <a-select
                v-model:value="selectedDiskUnitOfMeasurement">
                <a-select-option v-for="unit in diskUnitsOfMeasurement" :key="unit">
                  {{ unit }}
                </a-select-option>
              </a-select>
            </div>
            <resource-stats-line-chart
              v-if="selectedDiskUnitOfMeasurement === 'KiB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.disk.readAndWrite.inKiB"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="100"
              :yAxisMeasurementUnit="' KiB'"
            />
            <resource-stats-line-chart
              v-if="selectedDiskUnitOfMeasurement === 'MiB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.disk.readAndWrite.inMiB"
              :yAxisInitialMax="10"
              :yAxisIncrementValue="10"
              :yAxisMeasurementUnit="' MiB'"
            />
            <resource-stats-line-chart
              v-if="selectedDiskUnitOfMeasurement === 'GiB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.disk.readAndWrite.inGiB"
              :yAxisInitialMax="1"
              :yAxisIncrementValue="1"
              :yAxisMeasurementUnit="' GiB'"
            />
          </a-col>
        </a-row>
        <a-row class="chart-row" v-if="resourceIsVirtualMachine">
          <a-col>
            <strong>{{ $t('label.network') }}</strong>
            <InfoCircleOutlined class="info-icon" :title="$t('label.see.more.info.network.usage')" @click="onClickShowResourceInfoModal('NET')"/>
            <a-select v-model:value="selectedNetworkUnitOfMeasurement">
              <a-select-option v-for="unit in networkUnitsOfMeasurement" :key="unit">
                {{ unit }}
              </a-select-option>
            </a-select>
            <resource-stats-line-chart
              v-if="selectedNetworkUnitOfMeasurement === 'KiB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.network.inKiB"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="100"
              :yAxisMeasurementUnit="' KiB'"
            />
            <resource-stats-line-chart
              v-if="selectedNetworkUnitOfMeasurement === 'MiB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.network.inMiB"
              :yAxisInitialMax="100"
              :yAxisIncrementValue="100"
              :yAxisMeasurementUnit="' MiB'"
            />
            <resource-stats-line-chart
              v-if="selectedNetworkUnitOfMeasurement === 'GiB'"
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.network.inGiB"
              :yAxisInitialMax="1"
              :yAxisIncrementValue="1"
              :yAxisMeasurementUnit="' GiB'"
            />
          </a-col>
        </a-row>
      </div>
      <div v-else>
        <a-alert :message="$t('message.no.data.to.show.for.period')" banner />
      </div>
    </div>
    <a-spin v-else></a-spin>
  </div>
</template>
<script>
import { api } from '@/api'
import moment from 'moment'
import 'chartjs-adapter-moment'
import DateTimeFilter from './DateTimeFilter'
import ResourceStatsInfo from './stats/ResourceStatsInfo'
import ResourceStatsLineChart from './stats/ResourceStatsLineChart'

export default {
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      default: 'VirtualMachine'
    }
  },
  components: {
    DateTimeFilter,
    ResourceStatsInfo,
    ResourceStatsLineChart
  },
  data () {
    return {
      durationSelectorValue: '',
      resourceTypeToShowInfo: null,
      showResourceInfoModal: false,
      resourceInfoModalTitle: null,
      loaded: false,
      showCpuInfo: false,
      showFilterStatsModal: false,
      endDate: this.getEndDate(),
      startDate: this.getStartDate(),
      formatedPeriod: null,
      selectedMemoryChartType: 0,
      selectedMemoryUsageType: 0,
      memoryChartTypes: [this.$t('label.raw.data'), this.$t('label.percentage')],
      memoryUsageTypes: [this.$t('label.used'), this.$t('label.free')],
      selectedMemoryUnitOfMeasurement: 'GB',
      memoryUnitsOfMeasurement: ['MB', 'GB'],
      selectedNetworkUnitOfMeasurement: 'MiB',
      networkUnitsOfMeasurement: ['KiB', 'MiB', 'GiB'],
      selectedDiskChartType: 0,
      diskChartTypes: ['IOPS', this.$t('label.read.and.write')],
      selectedDiskUnitOfMeasurement: 'KiB',
      diskUnitsOfMeasurement: ['KiB', 'MiB', 'GiB'],
      chartLabels: [],
      resourceUsageHistory: {
        cpu: [],
        memory: {
          percentage: {
            free: [],
            used: []
          },
          rawData: {
            free: {
              inMB: [],
              inGB: []
            },
            used: {
              inMB: [],
              inGB: []
            }
          }
        },
        network: {
          inKiB: [],
          inMiB: [],
          inGiB: []
        },
        disk: {
          iops: [],
          readAndWrite: {
            inKiB: [],
            inMiB: [],
            inGiB: []
          }
        }
      }
    }
  },
  mounted () {
    this.fetchData()
  },
  computed: {
    statsRetentionTime () {
      if (this.resourceType === 'Volume') {
        return this.$store.getters.features.instancesdisksstatsretentiontime
      }
      return this.$store.getters.features.instancesstatsretentiontime
    },
    resourceStatsApi () {
      switch (this.resourceType) {
        case 'SystemVm':
        case 'DomainRouter':
          return 'listSystemVmsUsageHistory'
        case 'Volume':
          return 'listVolumesUsageHistory'
      }
      return 'listVirtualMachinesUsageHistory'
    },
    resourceStatsApiResponseObject () {
      switch (this.resourceType) {
        case 'Volume':
          return this.resourceType.toLowerCase()
      }
      return 'virtualmachine'
    },
    resourceIsVirtualMachine () {
      return ['VirtualMachine', 'SystemVm', 'DomainRouter'].includes(this.resourceType)
    },
    diskStatsAvailable () {
      return ['VirtualMachine', 'SystemVm', 'DomainRouter', 'Volume'].includes(this.resourceType)
    }
  },
  watch: {
    resource: function (newItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    openFilter () {
      this.showFilterStatsModal = true
    },
    onClickShowResourceInfoModal (resource) {
      switch (resource) {
        case 'CHART':
          this.resourceInfoModalTitle = this.$t('label.chart.info')
          break
        case 'CPU':
          this.resourceInfoModalTitle = this.$t('label.cpu.usage.info')
          break
        case 'MEM':
          this.resourceInfoModalTitle = this.$t('label.memory.usage.info')
          break
        case 'NET':
          this.resourceInfoModalTitle = this.$t('label.network.usage.info')
          break
        case 'DISK':
          this.resourceInfoModalTitle = this.$t('label.disk.usage.info')
          break
      }
      this.resourceTypeToShowInfo = resource
      this.showResourceInfoModal = true
    },
    handleDurationChange () {
      var now = this.getEndDate()
      var start = new Date(now)
      switch (this.durationSelectorValue) {
        case '6hours':
          start.setHours(start.getHours() - 6)
          break
        case '12hours':
          start.setHours(start.getHours() - 12)
          break
        case 'day':
          start.setDate(start.getDate() - 1)
          break
        case 'week':
          start.setDate(start.getDate() - 7)
          break
        default:
          start.setHours(start.getHours() - 1)
      }
      this.handleSubmit({ startDate: start, endDate: now })
    },
    handleSubmit (values) {
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
      this.showFilterStatsModal = false
      this.fetchData()
    },
    closeAction () {
      this.showFilterStatsModal = false
    },
    getStartDate () {
      var now = new Date()
      now.setHours(now.getHours() - 1)
      return now
    },
    getEndDate () {
      return new Date()
    },
    fetchData () {
      this.loaded = false
      this.showResourceInfoModal = false
      this.formatPeriod()
      var params = { id: this.resource.id }
      if (this.startDate) {
        params.startDate = moment(this.startDate).format()
      }
      if (this.endDate) {
        params.endDate = moment(this.endDate).format()
      }
      api(this.resourceStatsApi, params).then(response => {
        this.handleStatsResponse(response)
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    formatPeriod () {
      var formatedStartDate = null
      var formatedEndDate = null
      if (this.startDate) {
        formatedStartDate = moment(this.startDate).format('MMM DD, YYYY') + ' at ' + moment(this.startDate).format('HH:mm:ss')
      }
      if (this.endDate) {
        formatedEndDate = moment(this.endDate).format('MMM DD, YYYY') + ' at ' + moment(this.endDate).format('HH:mm:ss')
      }
      if (formatedStartDate && formatedEndDate) {
        this.formatedPeriod = ' ' + this.$t('label.vm.stats.filter.period', { startDate: formatedStartDate, endDate: formatedEndDate })
      } else if (formatedStartDate && !formatedEndDate) {
        this.formatedPeriod = ' ' + this.$t('label.vm.stats.filter.starting', { startDate: formatedStartDate })
      } else if (!formatedStartDate && formatedEndDate) {
        this.formatedPeriod = ' ' + this.$t('label.vm.stats.filter.up.to', { endDate: formatedEndDate })
      } else {
        this.formatedPeriod = ' <b>' + this.$t('label.all.available.data') + '</b>'
      }
    },
    handleStatsResponse (responseData) {
      this.resetData()
      const vm = responseData[this.resourceStatsApi.toLowerCase() + 'response'][this.resourceStatsApiResponseObject]

      const chartPointRadius = this.getChartPointRadius(vm[0].stats.length)

      const blue = '#166ab7'
      const green = '#389357'
      const blueInRgba = 'rgba(24, 144, 255, 0.5)'
      const greenInRgba = 'rgba(59, 198, 133, 0.65)'
      const red = '#ff4d4f'
      const redInRgba = 'rgb(255, 77, 79, 0.65)'

      const cpuLine = { label: 'CPU', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const memFreeLinePercent = { label: this.$t('label.memory.free'), backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const memUsedLinePercent = { label: this.$t('label.memory.used'), backgroundColor: redInRgba, borderColor: red, data: [], pointRadius: chartPointRadius }
      const memAllocatedLineInMB = { label: this.$t('label.memoryallocated'), backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const memFreeLineInMB = { label: this.$t('label.memory.free'), backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const memUsedLineInMB = { label: this.$t('label.memory.used'), backgroundColor: redInRgba, borderColor: red, data: [], pointRadius: chartPointRadius }
      const memAllocatedLineInGB = { label: this.$t('label.memoryallocated'), backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const memFreeLineInGB = { label: this.$t('label.memory.free'), backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const memUsedLineInGB = { label: this.$t('label.memory.used'), backgroundColor: redInRgba, borderColor: red, data: [], pointRadius: chartPointRadius }
      const netDownloadLineInKiB = { label: 'Download', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const netUploadLineInKiB = { label: 'Upload', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const netDownloadLineInMiB = { label: 'Download', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const netUploadLineInMiB = { label: 'Upload', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const netDownloadLineInGiB = { label: 'Download', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const netUploadLineInGiB = { label: 'Upload', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const diskReadLineInKiB = { label: 'Read', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const diskWriteLineInKiB = { label: 'Write', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const diskReadLineInMiB = { label: 'Read', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const diskWriteLineInMiB = { label: 'Write', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const diskReadLineInGiB = { label: 'Read', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const diskWriteLineInGiB = { label: 'Write', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const diskIopsLine = { label: 'IOPS', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }

      for (const element of vm[0].stats) {
        var ts = this.$toLocalDate(element.timestamp)
        const currentLabel = ts.split('T')[0] + ' ' + ts.split('T')[1].split('-')[0]
        this.chartLabels.push(currentLabel)

        if (this.resourceIsVirtualMachine) {
          cpuLine.data.push({ timestamp: currentLabel, stat: element.cpuused.split('%')[0] })

          element.memoryusedkbs = element.memorykbs - element.memoryintfreekbs
          memFreeLinePercent.data.push({ timestamp: currentLabel, stat: this.calculateMemoryPercentage(false, element.memorykbs, element.memoryintfreekbs) })
          memUsedLinePercent.data.push({ timestamp: currentLabel, stat: this.calculateMemoryPercentage(true, element.memorykbs, element.memoryintfreekbs) })
          memAllocatedLineInMB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.memorykbs, 1) })
          memFreeLineInMB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.memoryintfreekbs, 1) })
          memUsedLineInMB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.memoryusedkbs, 1) })
          memAllocatedLineInGB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.memorykbs, 2) })
          memFreeLineInGB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.memoryintfreekbs, 2) })
          memUsedLineInGB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.memoryusedkbs, 2) })

          netDownloadLineInKiB.data.push({ timestamp: currentLabel, stat: element.networkkbsread })
          netUploadLineInKiB.data.push({ timestamp: currentLabel, stat: element.networkkbswrite })
          netDownloadLineInMiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.networkkbsread, 1) })
          netUploadLineInMiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.networkkbswrite, 1) })
          netDownloadLineInGiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.networkkbsread, 2) })
          netUploadLineInGiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.networkkbswrite, 2) })
        }

        if (this.diskStatsAvailable) {
          diskReadLineInKiB.data.push({ timestamp: currentLabel, stat: element.diskkbsread })
          diskWriteLineInKiB.data.push({ timestamp: currentLabel, stat: element.diskkbswrite })
          diskReadLineInMiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.diskkbsread, 1) })
          diskWriteLineInMiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.diskkbswrite, 1) })
          diskReadLineInGiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.diskkbsread, 2) })
          diskWriteLineInGiB.data.push({ timestamp: currentLabel, stat: this.convertByteBasedUnitOfMeasure(element.diskkbswrite, 2) })
          diskIopsLine.data.push({ timestamp: currentLabel, stat: element.diskiopstotal })
        }
      }

      if (this.resourceIsVirtualMachine) {
        this.resourceUsageHistory.cpu.push(cpuLine)

        this.resourceUsageHistory.memory.percentage.free.push(memFreeLinePercent)
        this.resourceUsageHistory.memory.percentage.used.push(memUsedLinePercent)
        this.resourceUsageHistory.memory.rawData.free.inMB.push(memFreeLineInMB)
        this.resourceUsageHistory.memory.rawData.free.inMB.push(memAllocatedLineInMB)
        this.resourceUsageHistory.memory.rawData.used.inMB.push(memUsedLineInMB)
        this.resourceUsageHistory.memory.rawData.used.inMB.push(memAllocatedLineInMB)
        this.resourceUsageHistory.memory.rawData.free.inGB.push(memFreeLineInGB)
        this.resourceUsageHistory.memory.rawData.free.inGB.push(memAllocatedLineInGB)
        this.resourceUsageHistory.memory.rawData.used.inGB.push(memUsedLineInGB)
        this.resourceUsageHistory.memory.rawData.used.inGB.push(memAllocatedLineInGB)

        this.resourceUsageHistory.network.inKiB.push(netDownloadLineInKiB)
        this.resourceUsageHistory.network.inKiB.push(netUploadLineInKiB)
        this.resourceUsageHistory.network.inMiB.push(netDownloadLineInMiB)
        this.resourceUsageHistory.network.inMiB.push(netUploadLineInMiB)
        this.resourceUsageHistory.network.inGiB.push(netDownloadLineInGiB)
        this.resourceUsageHistory.network.inGiB.push(netUploadLineInGiB)
      }

      if (this.diskStatsAvailable) {
        this.resourceUsageHistory.disk.readAndWrite.inKiB.push(diskReadLineInKiB)
        this.resourceUsageHistory.disk.readAndWrite.inKiB.push(diskWriteLineInKiB)
        this.resourceUsageHistory.disk.readAndWrite.inMiB.push(diskReadLineInMiB)
        this.resourceUsageHistory.disk.readAndWrite.inMiB.push(diskWriteLineInMiB)
        this.resourceUsageHistory.disk.readAndWrite.inGiB.push(diskReadLineInGiB)
        this.resourceUsageHistory.disk.readAndWrite.inGiB.push(diskWriteLineInGiB)
        this.resourceUsageHistory.disk.iops.push(diskIopsLine)
      }

      this.loaded = true
    },
    /**
     * Calculates the ideal chart points radius based on the number of data points and the screen width.
     * @param numberOfDataPoints the number of data points.
     * @returns the ideal chart points radius (which is the size of the points on the chart).
     */
    getChartPointRadius (numberOfDataPoints) {
      const maxSizeLimit = 3
      const minSizeLimit = 2
      const minSize = 0.1 // the smallest value that allows to render the point in the chart
      const result = (screen.width * 0.04) / numberOfDataPoints
      if (result > maxSizeLimit) {
        return maxSizeLimit
      } else if (result < minSizeLimit) {
        return minSize
      }
      return parseFloat(result).toFixed(2)
    },
    /**
     * Converts a value (Byte-based) from an unit to other one. For example: from Byte to KiB; from GiB to MiB; etc.
     * To use it consider the following sequence: Byte -> KiB -> MiB -> GiB ...
     * So, from Byte to MiB there are 2 steps, while from MiB to Byte there are -2 steps.
     * @param value the value to be converted.
     * @param step the number of steps between Byte-based units of measure.
     * @returns the converted value.
     */
    convertByteBasedUnitOfMeasure (value, step) {
      if (value === 0) {
        return 0.00
      }
      if (step === 0) {
        return value
      }
      if (step > 0) {
        return parseFloat(value / (Math.pow(1024, step))).toFixed(2)
      }
      return parseFloat(value * (Math.pow(1024, Math.abs(step)))).toFixed(2)
    },
    resetData () {
      this.chartLabels = []
      this.resourceUsageHistory.cpu = []
      this.resourceUsageHistory.memory.percentage.free = []
      this.resourceUsageHistory.memory.percentage.used = []
      this.resourceUsageHistory.memory.rawData.free.inMB = []
      this.resourceUsageHistory.memory.rawData.free.inGB = []
      this.resourceUsageHistory.memory.rawData.used.inMB = []
      this.resourceUsageHistory.memory.rawData.used.inGB = []
      this.resourceUsageHistory.network.inKiB = []
      this.resourceUsageHistory.network.inMiB = []
      this.resourceUsageHistory.network.inGiB = []
      this.resourceUsageHistory.disk.iops = []
      this.resourceUsageHistory.disk.readAndWrite.inKiB = []
      this.resourceUsageHistory.disk.readAndWrite.inMiB = []
      this.resourceUsageHistory.disk.readAndWrite.inGiB = []
    },
    /**
     * Calculates the memory percentage.
     * @param isUsed "true" if the memory used percentage should be returned, "false" if the free memory percentage should be returned.
     * @param memoryTotalInKB the memory total (in KB).
     * @param memoryFreeInKB the memory free (in KB).
     * @returns the percentage of used/free memory.
     */
    calculateMemoryPercentage (isUsed, memoryTotalInKB, memoryFreeInKB) {
      if (memoryTotalInKB == null || memoryFreeInKB == null) {
        return -1
      }
      if (isUsed) {
        return parseFloat(100.0 * (memoryTotalInKB - memoryFreeInKB) / memoryTotalInKB).toFixed(2)
      }
      return parseFloat(100.0 * memoryFreeInKB / memoryTotalInKB).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/components/view/StatsTab.scss';
</style>
