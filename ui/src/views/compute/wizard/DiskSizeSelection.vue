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
  <a-form-item
    :label="inputDecorator === 'rootdisksize' ? $t('label.root.disk.size') : $t('label.disksize')"
    class="form-item">
    <a-row :gutter="12">
      <a-col :md="4" :lg="4">
        <span style="display: inline-flex">
          <a-input-number
            autoFocus
            v-model="inputValue"
            @change="($event) => updateDiskSize($event)"
          />
          <span style="padding-top: 6px; margin-left: 5px">GB</span>
        </span>
      </a-col>
    </a-row>
    <p v-if="error" style="color: red"> {{ $t(error) }} </p>
  </a-form-item>
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
  data () {
    return {
      inputValue: 0,
      error: false
    }
  },
  mounted () {
    this.fillValue()
  },
  methods: {
    fillValue () {
      this.inputValue = this.minDiskSize
      if (this.inputDecorator === 'rootdisksize') {
        this.inputValue = this.preFillContent.rootdisksize ? this.preFillContent.rootdisksize : this.minDiskSize
      } else if (this.inputDecorator === 'size') {
        this.inputValue = this.preFillContent.size ? this.preFillContent.size : this.minDiskSize
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
    }
  }
}
</script>

<style scoped lang="less">
  .form-item {
    margin: 0 5px;
  }
</style>
