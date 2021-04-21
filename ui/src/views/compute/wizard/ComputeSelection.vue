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
  <a-card>
    <a-col>
      <a-row>
        <a-col :md="colContraned" :lg="colContraned">
          <a-form-item
            :label="$t('label.cpunumber')"
            :validate-status="errors.cpu.status"
            :help="errors.cpu.message">
            <a-row :gutter="12">
              <a-col :md="10" :lg="10" v-show="isConstrained">
                <a-slider
                  :min="minCpu"
                  :max="maxCpu"
                  v-model="cpuNumberInputValue"
                  @change="($event) => updateComputeCpuNumber($event)"
                />
              </a-col>
              <a-col :md="4" :lg="4">
                <a-input-number
                  :autoFocus="isConstrained"
                  v-model="cpuNumberInputValue"
                  @change="($event) => updateComputeCpuNumber($event)"
                />
              </a-col>
            </a-row>
          </a-form-item>
        </a-col>
        <a-col :md="8" :lg="8" v-show="!isConstrained">
          <a-form-item
            :label="$t('label.cpuspeed')"
            :validate-status="errors.cpuspeed.status"
            :help="errors.cpuspeed.message">
            <a-input-number
              :autoFocus="!isConstrained"
              v-model="cpuSpeedInputValue"
              @change="($event) => updateComputeCpuSpeed($event)"
            />
          </a-form-item>
        </a-col>
        <a-col :md="colContraned" :lg="colContraned">
          <a-form-item
            :label="$t('label.memory.mb')"
            :validate-status="errors.memory.status"
            :help="errors.memory.message">
            <a-row :gutter="12">
              <a-col :md="10" :lg="10" v-show="isConstrained">
                <a-slider
                  :min="minMemory"
                  :max="maxMemory"
                  v-model="memoryInputValue"
                  @change="($event) => updateComputeMemory($event)"
                />
              </a-col>
              <a-col :md="4" :lg="4">
                <a-input-number
                  v-model="memoryInputValue"
                  @change="($event) => updateComputeMemory($event)"
                />
              </a-col>
            </a-row>
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
    cpunumberInputDecorator: {
      type: String,
      default: ''
    },
    cpuspeedInputDecorator: {
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
      }
    }
  },
  computed: {
    colContraned () {
      return this.isConstrained ? 12 : 8
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
    this.fillValue()
  },
  methods: {
    fillValue () {
      this.cpuNumberInputValue = this.minCpu
      this.memoryInputValue = this.minMemory

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
      this.$emit('update-compute-cpunumber', this.cpunumberInputDecorator, value)
    },
    updateComputeCpuSpeed (value) {
      this.$emit('update-compute-cpuspeed', this.cpuspeedInputDecorator, value)
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
    }
  }
}
</script>
