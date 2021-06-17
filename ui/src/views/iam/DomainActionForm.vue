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
  <div>
    <a-modal
      :visible="showAction"
      :closable="true"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      style="top: 20px;"
      @ok="handleSubmit"
      @cancel="parentCloseAction"
      :confirmLoading="action.loading"
      centered
    >
      <span slot="title">
        {{ $t(action.label) }}
      </span>
      <a-spin :spinning="action.loading">
        <a-form
          :form="form"
          @submit="handleSubmit"
          layout="vertical" >
          <a-alert type="warning" v-if="action.message">
            <span slot="message" v-html="$t(action.message)" />
          </a-alert>
          <a-form-item
            v-for="(field, fieldIndex) in action.paramFields"
            :key="fieldIndex"
            :v-bind="field.name"
            v-if="!(action.mapping && field.name in action.mapping && action.mapping[field.name].value)"
          >
            <span slot="label">
              {{ $t('label.' + field.name) }}
              <a-tooltip :title="field.description">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>

            <span v-if="field.type==='boolean'">
              <a-switch
                v-decorator="[field.name, {
                  rules: [{ required: field.required, message: `${$t('message.error.required.input')}` }]
                }]"
                :placeholder="field.description"
              />
            </span>
            <span v-else-if="action.mapping && field.name in action.mapping && action.mapping[field.name].options">
              <a-select
                :loading="field.loading"
                v-decorator="[field.name, {
                  rules: [{ required: field.required, message: $t('message.error.select') }]
                }]"
                :placeholder="field.description"
                :autoFocus="fieldIndex === firstIndex"
              >
                <a-select-option v-for="(opt, optIndex) in action.mapping[field.name].options" :key="optIndex">
                  {{ opt }}
                </a-select-option>
              </a-select>
            </span>
            <span
              v-else-if="field.type==='uuid' || field.name==='account'">
              <a-select
                showSearch
                optionFilterProp="children"
                v-decorator="[field.name, {
                  rules: [{ required: field.required, message: $t('message.error.select') }]
                }]"
                :loading="field.loading"
                :placeholder="field.description"
                :filterOption="(input, option) => {
                  return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                :autoFocus="fieldIndex === firstIndex"
              >
                <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                  {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                </a-select-option>
              </a-select>
            </span>
            <span v-else-if="field.type==='list'">
              <a-select
                :loading="field.loading"
                mode="multiple"
                v-decorator="[field.name, {
                  rules: [{ required: field.required, message: $t('message.error.select') }]
                }]"
                :placeholder="field.description"
                :autoFocus="fieldIndex === firstIndex"
              >
                <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                  {{ opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </span>
            <span v-else-if="field.type==='long'">
              <a-input-number
                v-decorator="[field.name, {
                  rules: [{ required: field.required, message: `${$t('message.validate.number')}` }]
                }]"
                :placeholder="field.description"
                :autoFocus="fieldIndex === firstIndex"
              />
            </span>
            <span v-else>
              <a-input
                v-decorator="[field.name, {
                  rules: [{ required: field.required, message: $t('message.error.required.input') }]
                }]"
                :placeholder="field.description"
                :autoFocus="fieldIndex === firstIndex" />
            </span>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'DomainActionForm',
  props: {
    action: {
      type: Object,
      required: true
    },
    showAction: {
      type: Boolean,
      default: false
    },
    resource: {
      type: Object,
      default: () => {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.firstIndex = 0
    for (let fieldIndex = 0; fieldIndex < this.action.paramFields.length; fieldIndex++) {
      const field = this.action.paramFields[fieldIndex]
      if (!(this.action.mapping && field.name in this.action.mapping && this.action.mapping[field.name].value)) {
        this.firstIndex = fieldIndex
        break
      }
    }
    if (this.action.dataView && this.action.icon === 'edit') {
      this.fillEditFormFieldValues()
    }
  },
  inject: ['parentCloseAction', 'parentFetchData'],
  methods: {
    pollActionCompletion (jobId, action) {
      this.$pollJob({
        jobId,
        successMethod: result => {
          this.parentFetchData()
          if (action.response) {
            const description = action.response(result.jobresult)
            if (description) {
              this.$notification.info({
                message: this.$t(action.label),
                description: (<span domPropsInnerHTML={description}></span>),
                duration: 0
              })
            }
          }
        },
        errorMethod: () => this.parentFetchData(),
        loadingMessage: `${this.$t(action.label)} ${this.$t('label.in.progress')} ${this.$t('label.for')} ${this.resource.name}`,
        catchMessage: this.$t('error.fetching.async.job.result'),
        action
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.action.loading = true
        const params = {}
        if ('id' in this.resource && this.action.params.map(i => { return i.name }).includes('id')) {
          params.id = this.resource.id
        }
        for (const key in values) {
          const input = values[key]
          for (const param of this.action.params) {
            if (param.name === key) {
              if (input === undefined) {
                if (param.type === 'boolean') {
                  params[key] = false
                }
                break
              }
              if (this.action.mapping && key in this.action.mapping && this.action.mapping[key].options) {
                params[key] = this.action.mapping[key].options[input]
              } else if (param.type === 'uuid') {
                params[key] = param.opts[input].id
              } else if (param.type === 'list') {
                params[key] = input.map(e => { return param.opts[e].id }).reduce((str, name) => { return str + ',' + name })
              } else if (param.name === 'account') {
                params[key] = param.opts[input].name
              } else {
                params[key] = input
              }
              break
            }
          }
        }

        for (const key in this.action.defaultArgs) {
          if (!params[key]) {
            params[key] = this.action.defaultArgs[key]
          }
        }

        if (this.action.mapping) {
          for (const key in this.action.mapping) {
            if (!this.action.mapping[key].value) {
              continue
            }
            params[key] = this.action.mapping[key].value(this.resource, params)
          }
        }

        const resourceName = params.displayname || params.displaytext || params.name || this.resource.name
        let hasJobId = false
        api(this.action.api, params).then(json => {
          for (const obj in json) {
            if (obj.includes('response')) {
              for (const res in json[obj]) {
                if (res === 'jobid') {
                  this.$store.dispatch('AddAsyncJob', {
                    title: this.$t(this.action.label),
                    jobid: json[obj][res],
                    description: this.resource.name,
                    status: 'progress'
                  })
                  this.pollActionCompletion(json[obj][res], this.action)
                  hasJobId = true
                  break
                }
              }
              break
            }
          }
          if (!hasJobId) {
            var message = this.action.successMessage ? this.$t(this.action.successMessage) : this.$t(this.action.label) +
              (resourceName ? ' - ' + resourceName : '')
            var duration = 2
            if (this.action.additionalMessage) {
              message = message + ' - ' + this.$t(this.action.successMessage)
              duration = 5
            }
            this.$message.success({
              content: message,
              key: this.action.label + resourceName,
              duration: duration
            })
            this.parentFetchData()
          }
          this.parentCloseAction()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(f => {
          this.action.loading = false
        })
      })
    },
    fillEditFormFieldValues () {
      const form = this.form
      this.action.paramFields.map(field => {
        let fieldName = null
        if (field.type === 'uuid' ||
          field.type === 'list' ||
          field.name === 'account' ||
          (this.action.mapping && field.name in this.action.mapping)) {
          fieldName = field.name.replace('ids', 'name').replace('id', 'name')
        } else {
          fieldName = field.name
        }
        const fieldValue = this.resource[fieldName] ? this.resource[fieldName] : null
        if (fieldValue) {
          form.getFieldDecorator(field.name, { initialValue: fieldValue })
        }
      })
    }
  }
}
</script>

<style scoped>
</style>
