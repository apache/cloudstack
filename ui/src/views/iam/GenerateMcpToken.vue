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
  <a-modal
    v-if="showModal"
    :visible="showModal"
    :closable="true"
    :maskClosable="false"
    :title="$t('label.action.create.mcp.token')"
    :okText="$t('label.ok')"
    :cancelText="$t('label.cancel')"
    :cancel-button-props="{ style: { display: token ? 'none' : 'inline-block' } }"
    :confirmLoading="loading"
    @cancel="closeModal"
    @ok="token ? closeModal() : handleSubmit()"
    centered>
    <div v-if="token">
      <a-alert
        style="margin-bottom: 10px"
        :message="$t('message.mcp.token.created.title')"
        :description="$t('message.mcp.token.created.body')"
        type="warning"
        show-icon />
      <a-input-group compact>
        <a-input :value="token" readonly style="width: calc(100% - 32px)" />
        <tooltip-button
          tooltipPlacement="top"
          :tooltip="$t('label.copy')"
          icon="CopyOutlined"
          @onClick="$message.success($t('label.copied.clipboard'))"
          :copyResource="token" />
      </a-input-group>
    </div>
    <a-form
      v-else
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      v-ctrl-enter="handleSubmit"
      @finish="handleSubmit">
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input v-focus="true" v-model:value="form.name" />
      </a-form-item>
      <a-form-item name="description" ref="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-input v-model:value="form.description" />
      </a-form-item>
      <a-form-item name="endDate" ref="endDate">
        <template #label>
          <tooltip-label :title="$t('label.end.date')" :tooltip="apiParams.enddate.description"/>
        </template>
        <a-date-picker v-model:value="form.endDate" :disabled-date="disabledEndDate" show-time />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { dayjs, parseDayJsObject } from '@/utils/date'

export default {
  name: 'GenerateMcpToken',
  components: {
    TooltipButton,
    TooltipLabel
  },
  props: {
    showModal: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      loading: false,
      // Kept only while the modal is open: the token cannot be retrieved again
      token: null
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createMcpToken')
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({
      name: [{ required: true, message: this.$t('message.error.required.input') }]
    })
  },
  methods: {
    handleSubmit (e) {
      if (e && typeof e.preventDefault === 'function') {
        e.preventDefault()
      }
      if (this.loading || this.token) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = { name: values.name }
        if (values.description) {
          params.description = values.description
        }
        if (values.endDate) {
          params.enddate = parseDayJsObject({ value: values.endDate })
        }
        this.loading = true
        postAPI('createMcpToken', params).then(response => {
          this.token = response.createmcptokenresponse.mcptoken.token
          this.$emit('fetch-data')
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(() => {})
    },
    closeModal () {
      this.token = null
      this.form.name = null
      this.form.description = null
      this.form.endDate = null
      this.$emit('close-modal')
    },
    disabledEndDate (current) {
      return current < dayjs().startOf('day')
    }
  }
}
</script>
