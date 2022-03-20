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
            :label="$t('label.memory.mb')"
            :validate-status="errors.memory.status"
            :help="errors.memory.message">
            <a-row :gutter="12">
              <a-col :md="10" :lg="10" v-show="isConstrained && maxMemory && !isNaN(maxMemory)">
                <a-slider
                  :min="minMemory"
                  :max="maxMemory"
                  v-model:value="memoryInputValue"
                  @change="($event) => updateComputeMemory($event)"
                />
              </a-col>
              <a-col :md="4" :lg="4">
                <a-input-number
                  v-model:value="memoryInputValue"
                  @change="($event) => updateComputeMemory($event)"
                />
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
    minCpu: {
      type: Number,
      default: 0
    },
    maxCpu: {
      type: Number,
      default: 2
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
      this.cpuNumberInputValue = this.minCpu
      this.memoryInputValue = this.minMemory
      this.cpuSpeedInputValue = this.cpuSpeed

      if (!this.preFillContent) {
        this.updateComputeCpuNumber(this.cpuNumberInputValue)
        this.updateComputeCpuSpeed(this.cpuSpeedInputValue)
        this.updateComputeMemory(this.memoryInputValue)
        return
      }
      if (this.preFillContent.cpunumber) {
        this.cpuNumberInputValue = this.preFillContent.cpunumber
      }
      if (this.preFillContent.cpuspeed) {
        this.cpuSpeedInputValue = this.preFillContent.cpuspeed
      }
      if (this.preFillContent.memory) {
        this.memoryInputValue = this.preFillContent.memory
      }
      this.updateComputeCpuNumber(this.preFillContent.cpunumber || this.cpuNumberInputValue)
      this.updateComputeCpuSpeed(this.preFillContent.cpuspeed || this.cpuSpeedInputValue)
      this.updateComputeMemory(this.preFillContent.memory || this.memoryInputValue)
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
    updateComputeMemory (value) {
      if (!value) this.memoryInputValue = 0
      if (!this.validateInput('memory', value)) {
        return
      }
      this.$emit('update-compute-memory', this.memoryInputDecorator, value)
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
          min = this.minMemory
          max = this.maxMemory
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
      if (value < min || value > max) {
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
