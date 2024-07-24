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
      centered
      :visible="showAction"
      :closable="true"
      :maskClosable="false"
      :confirmLoading="action.loading"
      :footer="null"
      @cancel="parentCloseAction"
      style="top: 20px;"
    >
      <template #title>
        {{ $t(action.label) }}
      </template>
      <a-spin :spinning="actionLoading" v-ctrl-enter="handleSubmit">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleSubmit"
          layout="vertical">
          <a-alert type="warning" v-if="action.message">
            <template #message>{{ $t(action.message) }}</template>
          </a-alert>
          <template v-for="(field, fieldIndex) in action.paramFields" :key="fieldIndex">
            <a-form-item
              :ref="field.name"
              :name="field.name"
              :v-bind="field.name"
              v-if="!(action.mapping && field.name in action.mapping && action.mapping[field.name].value)"
            >
              <template #label>
                <tooltip-label :title="$t('label.' + field.name)" :tooltip="field.description"/>
              </template>

              <a-switch
                v-if="field.type==='boolean'"
                v-model:checked="form[field.name]"
                :placeholder="field.description"
              />
              <a-select
                v-else-if="action.mapping && field.name in action.mapping && action.mapping[field.name].options"
                :loading="field.loading"
                v-model:value="form[field.name]"
                :placeholder="field.description"
                v-focus="fieldIndex === firstIndex"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="(opt, optIndex) in action.mapping[field.name].options" :key="optIndex" :label="opt">
                  {{ opt }}
                </a-select-option>
              </a-select>
              <a-select
                v-else-if="field.type==='uuid' || field.name==='account'"
                showSearch
                optionFilterProp="label"
                v-model:value="form[field.name]"
                :loading="field.loading"
                :placeholder="field.description"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                v-focus="fieldIndex === firstIndex"
              >
                <a-select-option
                  v-for="(opt, optIndex) in field.opts"
                  :key="optIndex"
                  :label="opt.name || opt.description || opt.traffictype || opt.publicip">
                  {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                </a-select-option>
              </a-select>
              <a-select
                v-else-if="field.type==='list'"
                :loading="field.loading"
                mode="multiple"
                v-model:value="form[field.name]"
                :placeholder="field.description"
                v-focus="fieldIndex === firstIndex"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="(opt, optIndex) in field.opts"
                  :key="optIndex"
                  :label="opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description">
                  {{ opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description }}
                </a-select-option>
              </a-select>
              <a-input-number
                v-else-if="field.type==='long'"
                v-model:value="form[field.name]"
                :placeholder="field.description"
                v-focus="fieldIndex === firstIndex"
              />
              <a-input
                v-else
                v-model:value="form[field.name]"
                :placeholder="field.description"
                v-focus="fieldIndex === firstIndex" />
            </a-form-item>
          </template>

          <div :span="24" class="action-button">
            <a-button @click="parentCloseAction">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import { ref, reactive, toRaw } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'DomainActionForm',
  components: {
    TooltipLabel
  },
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
  data () {
    return {
      actionLoading: false
    }
  },
  created () {
    this.actionLoading = this.action.loading
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
    let isFirstIndexSet = false
    this.firstIndex = 0
    this.action.paramFields.forEach((field, fieldIndex) => {
      this.form[field.name] = undefined
      this.rules[field.name] = []
      this.setRules(field)
      if (!isFirstIndexSet && !(this.action.mapping && field.name in this.action.mapping && this.action.mapping[field.name].value)) {
        this.firstIndex = fieldIndex
        isFirstIndexSet = true
      }
    })
    if (this.action.dataView && ['edit-outlined', 'EditOutlined'].includes(this.action.icon)) {
      this.fillEditFormFieldValues()
    }
  },
  inject: ['parentCloseAction', 'parentFetchData', 'parentForceRerender'],
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      if (this.action.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.actionLoading = true
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
                  this.$pollJob({
                    jobId: json[obj][res],
                    title: this.$t(this.action.label),
                    description: this.resource.name,
                    successMethod: result => {
                      if (this.action.api === 'deleteDomain') {
                        this.parentFetchData()
                        this.parentForceRerender()
                      }
                      if (this.action.response) {
                        const description = this.action.response(result.jobresult)
                        if (description) {
                          this.$notification.info({
                            message: this.$t(this.action.label),
                            description: (<span domPropsInnerHTML={description}></span>),
                            duration: 0
                          })
                        }
                      }
                      this.parentFetchData()
                    },
                    loadingMessage: `${this.$t(this.action.label)} ${this.$t('label.in.progress')} ${this.$t('label.for')} ${this.resource.name}`,
                    catchMessage: this.$t('error.fetching.async.job.result'),
                    action: this.action
                  })
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
          this.actionLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    fillEditFormFieldValues () {
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
          this.form[field.name] = fieldValue
        }
      })
    },
    setRules (field) {
      let rule = {}

      switch (true) {
        case (field.type === 'boolean'):
          break
        case (this.action.mapping && field.name in this.action.mapping && this.action.mapping[field.name].options):
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          rule.trigger = 'change'
          this.rules[field.name].push(rule)
          break
        case (field.type === 'uuid' || field.name === 'account'):
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          rule.trigger = 'change'
          this.rules[field.name].push(rule)
          break
        case (field.type === 'list'):
          rule.type = 'array'
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          rule.trigger = 'change'
          this.rules[field.name].push(rule)
          break
        case (field.type === 'long'):
          rule.type = 'number'
          rule.required = field.required
          rule.message = this.$t('message.validate.number')
          this.rules[field.name].push(rule)
          break
        default:
          rule.required = field.required
          rule.message = this.$t('message.error.required.input')
          this.rules[field.name].push(rule)
          break
      }

      rule = {}
    }
  }
}
</script>

<style scoped>
</style>
