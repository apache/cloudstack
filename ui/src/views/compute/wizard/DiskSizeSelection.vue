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
  <a-row :span="24" :style="{ marginTop: '20px' }">
    <a-col :span="isCustomizedDiskIOps || isCustomizedIOps ? 8 : 24" v-if="isCustomized">
      <a-form-item
        :label="inputDecorator === 'rootdisksize' ? $t('label.root.disk.size') : $t('label.disksize')"
        class="form-item">
        <span style="display: inline-flex">
          <a-input-number
            v-focus="true"
            v-model:value="inputValue"
            @change="($event) => updateDiskSize($event)"
          />
          <span style="padding-top: 6px; margin-left: 5px">GB</span>
        </span>
        <p v-if="error" style="color: red"> {{ $t(error) }} </p>
      </a-form-item>
    </a-col>
    <a-col :span="8" v-if="isCustomizedDiskIOps || isCustomizedIOps">
      <a-form-item :label="$t('label.diskiopsmin')">
        <a-input-number v-model:value="minIOps" @change="updateDiskIOps" />
        <p v-if="errorMinIOps" style="color: red"> {{ $t(errorMinIOps) }} </p>
      </a-form-item>
    </a-col>
    <a-col :span="8" v-if="isCustomizedDiskIOps || isCustomizedIOps">
      <a-form-item :label="$t('label.diskiopsmax')">
        <a-input-number v-model:value="maxIOps" @change="updateDiskIOps" />
        <p v-if="errorMaxIOps" style="color: red"> {{ $t(errorMaxIOps) }} </p>
      </a-form-item>
    </a-col>
  </a-row>
</template>

<script>
export default {
  name: 'DiskSizeSelection',
  props: {
    inputDecorator: {
      type: String,
      default: ''
    },
    preFillContent: {
      type: Object,
      default: () => {}
    },
    minDiskSize: {
      type: Number,
      default: 0
    },
    diskSelected: {
      type: Object,
      default: () => {}
    },
    rootDiskSelected: {
      type: Object,
      default: () => {}
    },
    isCustomized: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    minDiskSize (newItem) {
      if (newItem && newItem > 0) {
        this.inputValue = newItem
        this.updateDiskSize(newItem)
      }
    }
  },
  computed: {
    isCustomizedDiskIOps () {
      return this.diskSelected?.iscustomizediops || false
    },
    isCustomizedIOps () {
      return this.rootDiskSelected?.iscustomizediops || false
    }
  },
  data () {
    return {
      inputValue: 0,
      error: false,
      minIOps: null,
      maxIOps: null,
      errorMinIOps: false,
      errorMaxIOps: false
    }
  },
  mounted () {
    this.fillValue()
  },
  methods: {
    fillValue () {
      this.inputValue = this.minDiskSize
      if (this.inputDecorator === 'rootdisksize') {
        this.inputValue = this.preFillContent?.rootdisksize ? this.preFillContent.rootdisksize : this.minDiskSize
      } else if (this.inputDecorator === 'size') {
        this.inputValue = this.preFillContent?.size ? this.preFillContent.size : this.minDiskSize
      }
      this.$emit('update-disk-size', this.inputDecorator, this.inputValue)
    },
    updateDiskSize (value) {
      if (value < this.minDiskSize) {
        this.inputValue = this.minDiskSize
        this.error = `${this.$t('message.error.limit.value')} ` + this.minDiskSize + ' GB'
        return
      }
      this.error = false
      this.$emit('update-disk-size', this.inputDecorator, value)
    },
    updateDiskIOps () {
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
      this.$emit('update-iops-value', 'diskIOpsMin', this.minIOps)
      this.$emit('update-iops-value', 'diskIOpsMax', this.maxIOps)
      this.$emit('update-root-disk-iops-value', 'minIops', this.minIOps)
      this.$emit('update-root-disk-iops-value', 'maxIops', this.maxIOps)
      this.$emit('handler-error', false)
    }
  }
}
</script>

<style scoped lang="less">
  .form-item {
    margin: 0 5px;
  }
</style>
