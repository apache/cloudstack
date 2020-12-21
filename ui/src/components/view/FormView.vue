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
    :title="$t(currentAction.label)"
    :visible="showForm"
    :closable="true"
    :confirmLoading="currentAction.loading"
    :okText="$t('label.ok')"
    :cancelText="$t('label.cancel')"
    style="top: 20px;"
    @ok="handleSubmit"
    @cancel="close"
    centered
  >
    <a-spin :spinning="currentAction.loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical" >
        <a-form-item
          v-for="(field, fieldIndex) in currentAction.params"
          :key="fieldIndex"
          :label="$t(field.name)"
          :v-bind="field.name"
          v-if="field.name !== 'id'"
        >
          <span v-if="field.type==='boolean'">
            <a-switch
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: `${this.$t('message.error.required.input')}` }]
              }]"
              :placeholder="field.description"
            />
          </span>
          <span v-else-if="field.type==='uuid' || field.name==='account'">
            <a-select
              :loading="field.loading"
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: `${this.$t('message.error.select')}` }]
              }]"
              :placeholder="field.description"

            >
              <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </span>
          <span v-else-if="field.type==='long'">
            <a-input-number
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: `${this.$t('message.validate.number')}` }]
              }]"
              :placeholder="field.description"
            />
          </span>
          <span v-else-if="field.name==='password'">
            <a-input-password
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: `${this.$t('message.error.required.input')}` }]
              }]"
              :placeholder="field.description"
            />
          </span>
          <span v-else>
            <a-input
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: `${this.$t('message.error.required.input')}` }]
              }]"
              :placeholder="field.description"
            />
          </span>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>

import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'FormView',
  components: {
    ChartCard
  },
  props: {
    currentAction: {
      type: Object,
      required: true
    },
    showForm: {
      type: Boolean,
      default: false
    },
    handleSubmit: {
      type: Function,
      default: () => {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    close () {
      this.currentAction.loading = false
      this.showForm = false
    }
  }
}
</script>

<style scoped>
</style>
