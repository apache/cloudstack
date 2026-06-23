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
  <a-spin :spinning="loading">
    <a-form
      class="form"
      layout="vertical"
      ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit">

      <a-form-item name="name" ref="name">
        <template #label>{{ $t('label.name') }}</template>
        <a-input v-focus="true" v-model:value="form.name" />
      </a-form-item>

      <a-form-item name="resourcetype" ref="resourcetype">
        <template #label>{{ $t('label.resourcetype') }}</template>
        <a-select v-model:value="form.resourcetype" @change="onResourceTypeChange">
          <a-select-option v-for="rt in resourceTypes" :key="rt" :value="rt">{{ rt }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="metric" ref="metric">
        <template #label>{{ $t('label.metric') }}</template>
        <a-select v-model:value="form.metric" :disabled="!form.resourcetype">
          <a-select-option v-for="m in availableMetrics" :key="m" :value="m">{{ m }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="condition" ref="condition">
        <template #label>{{ $t('label.condition') }}</template>
        <a-select v-model:value="form.condition">
          <a-select-option v-for="c in conditions" :key="c" :value="c">{{ c }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="threshold" ref="threshold">
        <template #label>{{ $t('label.threshold') }}</template>
        <a-input-number v-model:value="form.threshold" :min="0" style="width: 100%" />
      </a-form-item>

      <a-form-item name="severity" ref="severity">
        <template #label>{{ $t('label.severity') }}</template>
        <a-select v-model:value="form.severity">
          <a-select-option v-for="s in severities" :key="s" :value="s">{{ s }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="message" ref="message">
        <template #label>{{ $t('label.message') }}</template>
        <a-input v-model:value="form.message" />
      </a-form-item>

      <a-form-item name="email" ref="email">
        <template #label>{{ $t('label.email') }}</template>
        <a-switch v-model:checked="form.email" />
      </a-form-item>

      <a-form-item name="resetinterval" ref="resetinterval">
        <template #label>{{ $t('label.resetinterval') }}</template>
        <a-input-number v-model:value="form.resetinterval" :min="0" style="width: 100%" />
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button @click="() => { this.$emit('close-action') }">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" :loading="loading" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { postAPI } from '@/api'

const METRICS_BY_TYPE = {
  VirtualMachine: ['CPU_UTILIZATION', 'MEMORY_UTILIZATION', 'DISK_READ_IOPS', 'DISK_WRITE_IOPS', 'DISK_READ_KBPS', 'DISK_WRITE_KBPS', 'NETWORK_READ_KBPS', 'NETWORK_WRITE_KBPS'],
  Host: ['CPU_UTILIZATION', 'MEMORY_UTILIZATION'],
  Volume: ['DISK_READ_IOPS', 'DISK_WRITE_IOPS', 'DISK_READ_KBPS', 'DISK_WRITE_KBPS'],
  StoragePool: ['STORAGE_UTILIZATION']
}

export default {
  name: 'CreateResourceAlertRule',
  data () {
    return {
      loading: false,
      form: {
        name: '',
        resourcetype: undefined,
        metric: undefined,
        condition: undefined,
        threshold: undefined,
        severity: undefined,
        message: '',
        email: false,
        resetinterval: 600
      },
      rules: {
        name: [{ required: true, message: this.$t('label.required') }],
        resourcetype: [{ required: true, message: this.$t('label.required') }],
        metric: [{ required: true, message: this.$t('label.required') }],
        condition: [{ required: true, message: this.$t('label.required') }],
        threshold: [{ required: true, message: this.$t('label.required') }],
        severity: [{ required: true, message: this.$t('label.required') }]
      },
      resourceTypes: ['VirtualMachine', 'Host', 'Volume', 'StoragePool'],
      conditions: ['GT', 'GTE', 'LT', 'LTE', 'EQ'],
      severities: ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
    }
  },
  computed: {
    availableMetrics () {
      return METRICS_BY_TYPE[this.form.resourcetype] || []
    }
  },
  methods: {
    onResourceTypeChange () {
      this.form.metric = undefined
    },
    handleSubmit () {
      this.$refs.formRef.validate().then(() => {
        const params = {
          name: this.form.name,
          resourcetype: this.form.resourcetype,
          metric: this.form.metric,
          condition: this.form.condition,
          threshold: this.form.threshold,
          severity: this.form.severity,
          email: this.form.email
        }
        if (this.form.message) params.message = this.form.message
        if (this.form.resetinterval) params.resetinterval = this.form.resetinterval
        this.loading = true
        postAPI('createResourceAlertRule', params).then(() => {
          this.$emit('close-action')
          this.$store.dispatch('AddAsyncJob', { title: this.$t('label.create.resource.alert.rule') })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    }
  }
}
</script>

<style scoped>
.form {
  min-width: 450px;
}
</style>
