// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <Line
    :chart-options="preparedOptions"
    :chart-data="preparedData"
    :width="chartWidth"
    :height="chartHeight"
  />
</template>

<script>
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, LineElement, CategoryScale, TimeScale, LinearScale, PointElement, Filler } from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, LineElement, CategoryScale, TimeScale, LinearScale, PointElement, Filler)

export default {
  name: 'ResourceStatsLineChart',
  components: { Line },
  props: {
    chartData: {
      type: Object,
      required: true
    },
    chartLabels: {
      type: Array,
      required: true
    },
    yAxisMeasurementUnit: {
      type: String,
      required: true
    },
    yAxisInitialMax: {
      type: Number,
      default: 1
    },
    yAxisIncrementValue: {
      type: Number,
      default: 1
    },
    chartWidth: {
      type: Number,
      default: 1024
    },
    chartHeight: {
      type: Number,
      default: 250
    }
  },
  computed: {
    preparedData () {
      if (this.chartData) {
        return this.prepareData(this.chartData)
      }
      return {}
    },
    preparedOptions () {
      if (this.chartData) {
        return this.getChartOptions(this.calculateMaxYAxisAndStepSize(this.chartData, this.yAxisInitialMax, this.yAxisIncrementValue), this.yAxisMeasurementUnit)
      }
      return {}
    }
  },
  methods: {
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
    calculateMaxYAxisAndStepSize (chartLines, initialMaxYAxis, incrementValue) {
      const numberOfLabelsOnYaxis = 4
      var highestValue = 0
      var maxYAxis = initialMaxYAxis
      for (const line of chartLines) {
        for (const d of line.data) {
          const currentValue = parseFloat(d.stat)
          if (currentValue > highestValue) {
            highestValue = currentValue
            while (highestValue > maxYAxis) {
              maxYAxis += incrementValue
              if (maxYAxis % incrementValue !== 0) {
                maxYAxis = Math.round(maxYAxis / incrementValue) * incrementValue
              }
            }
          }
        }
      }
      return { maxYAxes: maxYAxis, stepSize: maxYAxis / numberOfLabelsOnYaxis }
    },
    /**
     * Returns the chart options.
     * @param yAxesStepSize the step size for the Y axes.
     * @param yAxesUnitOfMeasurement the unit of measurement label used on the Y axes.
     * @returns the chart options.
     */
    getChartOptions (yAxesOptions, yAxesUnitOfMeasurement) {
      const dateTimes = this.convertStringArrayToDateArray(JSON.parse(JSON.stringify(this.chartLabels)))
      const averageDifference = this.averageDifferenceBetweenTimes(dateTimes)
      const xAxisStepSize = this.calculateStepSize(this.chartLabels.length, averageDifference)
      const startDate = new Date(dateTimes[0])
      const endDate = new Date(dateTimes[dateTimes.length - 1])
      const differentDay = startDate.getDate() !== endDate.getDate()
      const differentYear = startDate.getFullYear() !== endDate.getFullYear()
      var displayFormat = 'HH:mm'
      if (xAxisStepSize < 5 * 60) {
        displayFormat += ':ss'
      }
      if (differentDay) {
        displayFormat = 'MMM-DD ' + displayFormat
      }
      if (xAxisStepSize >= 24 * 60 * 60) {
        displayFormat = 'MMM-DD'
      }
      if (differentYear) {
        displayFormat = 'YYYY-' + displayFormat
      }
      var chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          yAxis: {
            min: 0,
            max: yAxesOptions.maxYAxes,
            reverse: false,
            ticks: {
              stepSize: yAxesOptions.stepSize,
              callback: function (label) {
                return label + yAxesUnitOfMeasurement
              }
            }
          },
          xAxis: {
            type: 'time',
            autoSkip: false,
            time: {
              parser: 'YYYY-MM-DD HH:mm:ss',
              unit: 'second',
              displayFormats: {
                second: displayFormat
              }
            }
          }
        }
      }
      chartOptions.scales.xAxis.time.stepSize = xAxisStepSize
      return chartOptions
    },
    convertStringArrayToDateArray (stringArray) {
      const dateArray = []
      for (const element of stringArray) {
        dateArray.push(new Date(element.replace(' ', 'T')))
      }
      return dateArray
    },
    averageDifferenceBetweenTimes (timeList) {
      const oneSecond = 1000 // 1 second represented as milliseconds
      const differences = []
      var previous = timeList.splice(0, 1)[0]
      for (const time of timeList) {
        differences.push((time - previous) / oneSecond) // push the difference in seconds
        previous = time
      }
      if (differences.length === 0) {
        return 1
      }
      const averageDifference = Math.trunc(differences.reduce((a, b) => a + b, 0) / differences.length)
      return averageDifference
    },
    calculateStepSize (numberOfDataPoints, differenceBetweenTimes) {
      const idealNumberOfLabels = 8
      const result = numberOfDataPoints / idealNumberOfLabels
      if (result > 1) {
        return result * differenceBetweenTimes
      }
      return differenceBetweenTimes
    },
    prepareData (chartData) {
      const datasetList = []
      for (const element of chartData) {
        datasetList.push(
          {
            backgroundColor: element.backgroundColor,
            borderColor: element.borderColor,
            borderWidth: 3,
            label: element.label,
            data: element.data.map(d => d.stat),
            hidden: this.hideLine(element.data.map(d => d.stat)),
            pointRadius: element.pointRadius,
            fill: 'origin'
          }
        )
      }
      return {
        labels: this.chartLabels,
        datasets: datasetList
      }
    },
    hideLine (data) {
      for (const d of data) {
        if (d < 0) {
          return true
        }
      }
      return false
    }
  }
}
</script>
