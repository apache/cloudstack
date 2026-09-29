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

      <a-form-item name="condition" ref="condition">
        <template #label>{{ $t('label.condition') }}</template>
        <a-select v-model:value="form.condition">
          <a-select-option v-for="c in conditions" :key="c" :value="c">{{ conditionLabels[c] || c }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="threshold" ref="threshold">
        <template #label>{{ $t('label.threshold') }}</template>
        <a-input-number v-model:value="form.threshold" :min="0" style="width: 100%" />
      </a-form-item>

      <a-form-item name="severity" ref="severity">
        <template #label>{{ $t('label.severity') }}</template>
        <a-select v-model:value="form.severity">
          <a-select-option v-for="s in severities" :key="s" :value="s">{{ severityLabels[s] || s }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="message" ref="message">
        <template #label>{{ $t('label.message') }}</template>
        <a-input v-model:value="form.message" />
      </a-form-item>

      <a-form-item name="email" ref="email" v-if="isRootAdmin">
        <template #label>{{ $t('label.email') }}</template>
        <a-switch v-model:checked="form.email" />
      </a-form-item>

      <a-form-item name="resetinterval" ref="resetinterval">
        <template #label>{{ $t('label.resetinterval') }}</template>
        <a-input-number v-model:value="form.resetinterval" :min="0" style="width: 100%" />
      </a-form-item>

      <a-form-item name="webhookids" ref="webhookids" v-if="'listWebhooks' in $store.getters.apis">
        <template #label>{{ $t('label.webhooks') }}</template>
        <a-select
          v-model:value="form.webhookids"
          mode="multiple"
          :loading="webhooksLoading"
          optionFilterProp="label"
          :filterOption="(input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0">
          <a-select-option v-for="wh in webhooks" :key="wh.id" :value="wh.id" :label="wh.name">{{ wh.name }}</a-select-option>
        </a-select>
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button @click="() => { this.$emit('close-action') }">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" :loading="loading" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import { CONDITIONS, SEVERITIES, CONDITION_LABELS, SEVERITY_LABELS } from './resourceAlertOptions'

export default {
  name: 'EditResourceAlertRule',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      webhooks: [],
      webhooksLoading: false,
      form: {
        name: this.resource.name,
        condition: this.resource.condition,
        threshold: this.resource.threshold,
        severity: this.resource.severity,
        message: this.resource.message || '',
        email: !!this.resource.email,
        resetinterval: this.resource.resetinterval,
        webhookids: [...(this.resource.webhookids || [])]
      },
      rules: {
        name: [{ required: true, message: this.$t('label.required') }],
        condition: [{ required: true, message: this.$t('label.required') }],
        threshold: [{ required: true, message: this.$t('label.required') }],
        severity: [{ required: true, message: this.$t('label.required') }]
      },
      conditions: CONDITIONS,
      severities: SEVERITIES,
      conditionLabels: CONDITION_LABELS,
      severityLabels: SEVERITY_LABELS
    }
  },
  computed: {
    isRootAdmin () {
      return this.$store.getters.userInfo.roletype === 'Admin'
    }
  },
  created () {
    this.fetchWebhooks()
  },
  methods: {
    fetchWebhooks () {
      if (!('listWebhooks' in this.$store.getters.apis)) return
      // Only webhooks the rule owner can use are valid for the rule
      const owner = this.resource.projectid
        ? { projectid: this.resource.projectid }
        : { account: this.resource.account, domainid: this.resource.domainid }
      this.webhooksLoading = true
      getAPI('listWebhooks', { listall: true, ...owner }).then(json => {
        this.webhooks = json?.listwebhooksresponse?.webhook || []
      }).finally(() => {
        this.webhooksLoading = false
      })
    },
    webhooksChanged () {
      const before = [...(this.resource.webhookids || [])].sort().join(',')
      return before !== [...this.form.webhookids].sort().join(',')
    },
    handleSubmit () {
      this.$refs.formRef.validate().then(() => {
        const params = {
          id: this.resource.id,
          name: this.form.name,
          condition: this.form.condition,
          threshold: this.form.threshold,
          severity: this.form.severity,
          message: this.form.message
        }
        if (this.isRootAdmin) params.email = this.form.email
        if (this.form.resetinterval !== undefined && this.form.resetinterval !== null) params.resetinterval = this.form.resetinterval
        if (this.webhooksChanged()) {
          if (this.form.webhookids.length > 0) {
            params.webhookids = this.form.webhookids.join(',')
          } else {
            params.cleanupwebhooks = true
          }
        }
        this.loading = true
        postAPI('updateResourceAlertRule', params).then(() => {
          this.$message.success(this.$t('label.edit') + ' - ' + params.name)
          this.$emit('refresh-data')
          this.$emit('close-action')
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
