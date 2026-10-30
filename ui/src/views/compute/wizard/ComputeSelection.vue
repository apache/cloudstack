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
  <a-card v-if="isCustomized">
    <a-col>
      <a-row>
        <a-col :md="colContraned" :lg="colContraned" v-if="isCustomized">
          <a-form-item
            :label="$t('label.cpunumber')"
            :validate-status="errors.cpu.status"
            :help="errors.cpu.message">
            <a-row :gutter="12">
              <a-col :md="10" :lg="10" v-show="isConstrained && maxCpu && !isNaN(maxCpu)">
                <a-slider
                  :min="minCpu"
                  :max="maxCpu"
                  v-model:value="cpuNumberInputValue"
                  @change="($event) => updateComputeCpuNumber($event)"
                />
              </a-col>
              <a-col :md="4" :lg="4">
                <a-input-number
                  v-focus="isConstrained"
                  v-model:value="cpuNumberInputValue"
                  @change="($event) => updateComputeCpuNumber($event)"
                />
              </a-col>
            </a-row>
          </a-form-item>
        </a-col>
        <a-col :md="8" :lg="8" v-show="!isConstrained" v-if="isCustomized">
          <a-form-item
            :label="$t('label.cpuspeed')"
            :validate-status="errors.cpuspeed.status"
            :help="errors.cpuspeed.message">
            <a-input-number
              v-focus="!isConstrained"
              v-model:value="cpuSpeedInputValue"
              @change="($event) => updateComputeCpuSpeed($event)"
            />
          </a-form-item>
        </a-col>
        <a-col :md="colContraned" :lg="colContraned" v-if="isCustomized">
          <a-form-item
            :label="$t('label.memory')"
            :validate-status="errors.memory.status"
            :help="errors.memory.message">
            <a-row :gutter="12">
              <a-col :md="14" :lg="14" v-show="isConstrained && maxMemory && !isNaN(maxMemory)">
                <a-slider
                  :min="minMemoryInUnit"
                  :max="maxMemoryInUnit"
                  :step="memoryStep"
                  v-model:value="memoryDisplayValue"
                  @change="($event) => updateComputeMemory($event)"
                />
              </a-col>
              <a-col :md="isConstrained ? 10 : 8" :lg="isConstrained ? 10 : 8">
                <a-input-group compact style="display: flex">
                  <a-tooltip :title="memoryTooltipText" placement="topLeft" style="flex: 1; display: flex">
                    <a-input-number
                      style="width: 100%"
                      :min="isConstrained ? minMemoryInUnit : 0"
                      :max="isConstrained ? maxMemoryInUnit : undefined"
                      :step="memoryStep"
                      :precision="memoryUnit === 'GiB' ? 2 : 0"
                      v-model:value="memoryDisplayValue"
                      @change="($event) => updateComputeMemory($event)"
                    />
                  </a-tooltip>
                  <a-select
                    v-model:value="memoryUnit"
                    style="width: 75px"
                    @change="handleUnitChange">
                    <a-select-option value="GiB">GiB</a-select-option>
                    <a-select-option value="MiB">MiB</a-select-option>
                  </a-select>
                </a-input-group>
              </a-col>
            </a-row>
          </a-form-item>
        </a-col>
        <a-col :md="8" v-if="isCustomizedIOps">
          <a-form-item :label="$t('label.miniops')">
            <a-input-number v-model:value="minIOps" @change="updateIOpsValue" />
            <p v-if="errorMinIOps" style="color: red"> {{ $t(errorMinIOps) }} </p>
          </a-form-item>
        </a-col>
        <a-col :md="8" v-if="isCustomizedIOps">
          <a-form-item :label="$t('label.maxiops')">
            <a-input-number v-model:value="maxIOps" @change="updateIOpsValue" />
            <p v-if="errorMaxIOps" style="color: red"> {{ $t(errorMaxIOps) }} </p>
          </a-form-item>
        </a-col>
      </a-row>
    </a-col>
  </a-card>
</template>

<script>
export default {
  name: 'ComputeSelection',
  props: {
    computeOfferingId: {
      type: String,
      default: () => ''
    },
    isConstrained: {
      type: Boolean,
      default: true
    },
    cpuSpeed: {
      type: Number,
      default: 0
    },
    initialCpuValue: {
      type: Number,
      default: 0
    },
    minCpu: {
      type: Number,
      default: 0
    },
    maxCpu: {
      type: Number,
      default: 2
    },
    initialMemoryValue: {
      type: Number,
      default: 0
    },
    minMemory: {
      type: Number,
      default: 0
    },
    maxMemory: {
      type: Number,
      default: 256
    },
    cpuNumberInputDecorator: {
      type: String,
      default: ''
    },
    cpuSpeedInputDecorator: {
      type: String,
      default: ''
    },
    memoryInputDecorator: {
      type: String,
      default: ''
    },
    preFillContent: {
      type: Object,
      default: () => {}
    },
    isCustomized: {
      type: Boolean,
      default: false
    },
    isCustomizedIOps: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      cpuNumberInputValue: 0,
      cpuSpeedInputValue: 0,
      memoryInputValue: 0,
      memoryDisplayValue: 0,
      memoryUnit: 'GiB',
      errors: {
        cpu: {
          status: '',
          message: ''
        },
        cpuspeed: {
          status: '',
          message: ''
        },
        memory: {
          status: '',
          message: ''
        }
      },
      minIOps: null,
      maxIOps: null,
      errorMinIOps: false,
      errorMaxIOps: false
    }
  },
  computed: {
    colContraned () {
      if (this.isConstrained && this.maxCpu && !isNaN(this.maxCpu)) {
        return 12
      }

      return 8
    },
    minMemoryInUnit () {
      if (!this.minMemory) return 0
      if (this.memoryUnit === 'GiB') {
        return Math.ceil(this.minMemory / 256) * 0.25
      }
      return this.minMemory
    },
    maxMemoryInUnit () {
      if (!this.maxMemory) return 0
      if (this.memoryUnit === 'GiB') {
        return Math.max(this.minMemoryInUnit, Math.floor(this.maxMemory / 256) * 0.25)
      }
      return this.maxMemory
    },
    memoryStep () {
      if (this.memoryUnit === 'GiB') {
        return 0.25
      }
      return 1
    },
    memoryTooltipText () {
      if (!this.memoryInputValue) return ''
      const exactGiB = this.memoryInputValue / 1024
      if (this.memoryUnit === 'GiB') {
        if (this.memoryDisplayValue === exactGiB) {
          return `${this.memoryDisplayValue} GiB = ${this.memoryInputValue} MiB`
        }
        return `Exact: ${exactGiB} GiB (${this.memoryInputValue} MiB)`
      }
      return `${this.memoryInputValue} MiB = ${exactGiB} GiB`
    }
  },
  watch: {
    computeOfferingId (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.fillValue()
      }
    }
  },
  mounted () {
    if (this.isCustomized) {
      this.fillValue()
    }
  },
  methods: {
    fillValue () {
      this.cpuNumberInputValue = this.initialCpuValue > 0 ? this.initialCpuValue : this.minCpu
      const rawMemory = (this.preFillContent && this.preFillContent.memory)
        ? this.preFillContent.memory
        : (this.initialMemoryValue > 0 ? this.initialMemoryValue : this.minMemory)

      // MiB is the authoritative internal state
      this.memoryInputValue = rawMemory

      // Default to GiB if rawMemory is a multiple of 256 MiB (exact 0.25 GiB increment)
      const isQuarterGiB = rawMemory >= 256 && (rawMemory % 256 === 0)
      this.memoryUnit = isQuarterGiB ? 'GiB' : 'MiB'
      this.memoryDisplayValue = isQuarterGiB ? (rawMemory / 1024) : rawMemory

      this.cpuSpeedInputValue = this.cpuSpeed

      if (!this.preFillContent) {
        this.updateComputeCpuNumber(this.cpuNumberInputValue)
        this.updateComputeCpuSpeed(this.cpuSpeedInputValue)
        this.$emit('update-compute-memory', this.memoryInputDecorator, this.memoryInputValue)
        return
      }
      if (this.preFillContent.cpunumber) {
        this.cpuNumberInputValue = this.preFillContent.cpunumber
      }
      if (this.preFillContent.cpuspeed) {
        this.cpuSpeedInputValue = this.preFillContent.cpuspeed
      }

      this.updateComputeCpuNumber(this.preFillContent.cpunumber || this.cpuNumberInputValue)
      this.updateComputeCpuSpeed(this.preFillContent.cpuspeed || this.cpuSpeedInputValue)
      // Emit exact prefilled MiB without any conversion on load
      this.$emit('update-compute-memory', this.memoryInputDecorator, this.memoryInputValue)
    },
    handleUnitChange (newUnit) {
      if (newUnit === 'MiB') {
        this.memoryDisplayValue = this.memoryInputValue
      } else {
        this.memoryDisplayValue = Math.round(this.memoryInputValue / 256) * 0.25
      }
      if (this.isConstrained) {
        if (this.memoryDisplayValue < this.minMemoryInUnit) {
          this.memoryDisplayValue = this.minMemoryInUnit
        } else if (this.memoryDisplayValue > this.maxMemoryInUnit) {
          this.memoryDisplayValue = this.maxMemoryInUnit
        }
      }
    },
    updateComputeCpuNumber (value) {
      if (!value) this.cpuNumberInputValue = 0
      if (!this.validateInput('cpu', value)) {
        return
      }
      this.$emit('update-compute-cpunumber', this.cpuNumberInputDecorator, value)
    },
    updateComputeCpuSpeed (value) {
      this.$emit('update-compute-cpuspeed', this.cpuSpeedInputDecorator, value)
    },
    updateComputeMemory (displayValue) {
      if (displayValue === null || displayValue === undefined) {
        this.memoryDisplayValue = 0
        displayValue = 0
      } else {
        this.memoryDisplayValue = displayValue
      }
      if (!this.validateInput('memory', displayValue)) {
        return
      }
      let memoryInMiB
      if (this.memoryUnit === 'GiB') {
        if (this.isConstrained && displayValue === this.minMemoryInUnit) {
          memoryInMiB = this.minMemory
        } else if (this.isConstrained && displayValue === this.maxMemoryInUnit) {
          memoryInMiB = this.maxMemory
        } else {
          memoryInMiB = Math.round(displayValue * 1024)
        }
      } else {
        memoryInMiB = Math.round(displayValue)
      }

      this.memoryInputValue = memoryInMiB
      this.$emit('update-compute-memory', this.memoryInputDecorator, memoryInMiB)
    },
    validateInput (input, value) {
      this.errors[input].status = ''
      this.errors[input].message = ''

      if (value === null || value === undefined || value.length === 0) {
        this.errors[input].status = 'error'
        this.errors[input].message = this.$t('message.error.required.input')
        return false
      }

      if (!this.isConstrained) {
        return true
      }

      let min
      let max

      switch (input) {
        case 'cpu':
          min = this.minCpu
          max = this.maxCpu
          break
        case 'memory':
          min = this.minMemoryInUnit
          max = this.maxMemoryInUnit
          break
      }

      if (!this.checkValidRange(value, min, max)) {
        this.errors[input].status = 'error'
        this.errors[input].message = `${this.$t('message.please.enter.value')} ${this.$t('label.from')} ${min} ${this.$t('label.to')} ${max})`
        return false
      }

      return true
    },
    checkValidRange (value, min, max) {
      const epsilon = 0.001
      if (value < (min - epsilon) || value > (max + epsilon)) {
        return false
      }

      return true
    },
    updateIOpsValue () {
      let flag = true
      this.errorMinIOps = false
      this.errorMaxIOps = false
      if (this.minIOps < 0) {
        this.errorMinIOps = `${this.$t('message.error.limit.value')} 0`
        flag = false
      }
      if (this.maxIOps < 0) {
        this.errorMaxIOps = `${this.$t('message.error.limit.value')} 0`
        flag = false
      }

      if (!flag) {
        this.$emit('handler-error', true)
        return
      }

      if (this.minIOps > this.maxIOps) {
        this.errorMinIOps = this.$t('message.error.valid.iops.range')
        this.errorMaxIOps = this.$t('message.error.valid.iops.range')
        this.$emit('handler-error', true)
        return
      }
      this.$emit('update-iops-value', 'minIops', this.minIOps)
      this.$emit('update-iops-value', 'maxIops', this.maxIOps)
      this.$emit('handler-error', false)
    }
  }
}
</script>
