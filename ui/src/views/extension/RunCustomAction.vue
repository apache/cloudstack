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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      :loading="loading"
      layout="vertical"
      @finish="handleSubmit">
      <a-form-item name="customactionid" ref="customactionid">
        <template #label>
          <tooltip-label :title="$t('label.action')" :tooltip="apiParams.customactionid.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.customactionid"
          :placeholder="apiParams.customactionid.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          @change="onCustomActionChange" >
          <a-select-option v-for="opt in customActions" :key="opt.id" :label="opt.name || opt.id">
            {{ opt.name || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-card v-if="form.customactionid && (!!currentDescription || !!currentParameters && currentParameters.length > 0)" style="margin-bottom: 10px;">
        <div v-if="!!currentDescription">
          {{ currentDescription }}
          <a-divider v-if="!!currentParameters && currentParameters.length > 0"/>
        </div>
        <div v-for="(field, fieldIndex) in currentParameters" :key="fieldIndex">
          <a-form-item :name="field.name" :ref="field.name">
            <template #label>
              <tooltip-label :title="field.name" :tooltip="field.name"/>
            </template>
            <a-switch
              v-if="field.type === 'BOOLEAN'"
              v-model:checked="form[field.name]"
              :placeholder="field.name"
              v-focus="fieldIndex === 0"
            />
            <a-date-picker
              v-else-if="field.type === 'DATE'"
              show-time
              v-model:value="form[field.name]"
              :placeholder="field.name"
              v-focus="fieldIndex === 0"
            />
            <a-select
              v-else-if="field.valueoptions && field.valueoptions.length > 0"
              v-model:value="form[field.name]"
              :placeholder="field.name">
              <a-select-option v-for="t in field.valueoptions" :key="t" :value="t">{{ t }}</a-select-option>
            </a-select>
            <a-input-number
              v-else-if="['NUMBER'].includes(field.type)"
              :precision="['DECIMAL'].includes(field.validationformat) ? 2 : 0"
              v-focus="fieldIndex === 0"
              v-model:value="form[field.name]"
              :placeholder="field.name" />
            <a-input-password
              v-else-if="['STRING'].includes(field.type) && ['PASSWORD'].includes(field.validationformat)"
              v-focus="fieldIndex === 0"
              v-model:value="form[field.name]"
              :placeholder="field.name" />
            <a-input
              v-else
              v-focus="fieldIndex === 0"
              v-model:value="form[field.name]"
              :placeholder="field.name" />
          </a-form-item>
        </div>
      </a-card>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw, h } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'

export default {
  name: 'RunCustomAction',
  components: {
    TooltipLabel,
    DetailsInput
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      customActions: [],
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('runCustomAction')
  },
  created () {
    this.initForm()
    this.fetchCustomActions()
  },
  computed: {
    datePattern () {
      return 'YYYY-MM-DDTHH:mm:ssZ'
    },
    resourceType () {
      const metaResourceType = this.$route.meta.resourceType
      if (metaResourceType && !['UserVm', 'DomainRouter', 'SystemVm'].includes(metaResourceType)) {
        return metaResourceType
      }
      return 'VirtualMachine'
    },
    currentAction () {
      if (!this.customActions || this.customActions.length === 0 || !this.form.customactionid) {
        return []
      }
      return this.customActions.find(i => i.id === this.form.customactionid)
    },
    currentDescription () {
      return this.currentAction?.description || ''
    },
    currentParameters () {
      return this.currentAction?.parameters || []
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        customactionid: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    fetchCustomActions () {
      this.loading = true
      this.customActions = []
      const params = {
        resourcetype: this.resourceType,
        resourceid: this.resource.id,
        enabled: true
      }
      getAPI('listCustomActions', params).then(json => {
        this.customActions = json?.listcustomactionsresponse?.extensioncustomaction || []
      }).finally(() => {
        this.loading = false
      })
    },
    onCustomActionChange () {
      Object.keys(this.rules).forEach(key => {
        if (key !== 'customactionid') {
          delete this.rules[key]
        }
      })
      Object.keys(this.form).forEach(key => {
        if (key !== 'customactionid') {
          delete this.form[key]
        }
      })
      if (this.currentParameters && this.currentParameters.length > 0) {
        this.currentParameters.forEach(field => {
          const required = !!field.required
          const fieldRules = []
          if (field.type === 'BOOLEAN') {
            this.form[field.name] = false
          }
          // Required rule
          if (required) {
            fieldRules.push({
              required: true,
              message: `${this.$t('message.error.input.value', { field: field.name })}`
            })
          }
          if (field.type === 'STRING' && field.validationformat) {
            let validator
            switch (field.validationformat) {
              case 'EMAIL':
                validator = {
                  type: 'email',
                  message: this.$t('message.error.input.invalidemail')
                }
                break
              case 'URL':
                validator = {
                  type: 'url',
                  message: this.$t('message.error.input.invalidurl')
                }
                break
              case 'UUID':
                validator = {
                  pattern: /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
                  message: this.$t('message.error.input.invaliduuid')
                }
                break
            }
            if (validator) {
              fieldRules.push(validator)
            }
          }
          if (fieldRules.length > 0) {
            this.rules[field.name] = fieldRules
          }
        })
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          resourcetype: this.resourceType,
          resourceid: this.resource.id,
          customactionid: values.customactionid
        }
        var keys = Object.keys(values)
        keys = keys.filter(k => k !== 'customactionid')
        for (const key of keys) {
          var value = values[key]
          if (value !== undefined && value != null &&
              (typeof value !== 'string' || (typeof value === 'string' && value.trim().length > 0))) {
            const fieldDef = this.currentParameters.find(f => f.name === key)
            if (fieldDef?.type === 'DATE') {
              value = value.format(this.datePattern)
            }
            params['parameters[0].' + key] = value
          }
        }
        postAPI('runCustomAction', params).then(response => {
          this.$pollJob({
            jobId: response.runcustomactionresponse.jobid,
            title: this.currentAction.name || this.$t('label.run.custom.action'),
            description: this.currentAction.description || this.currentAction.name,
            showSuccessMessage: false,
            successMethod: (result) => {
              this.$emit('refresh-data')
              const actionResponse = result.jobresult?.customactionresult || {}
              const success = actionResponse.success || false
              const message = actionResponse?.result?.message || (success ? 'Success' : 'Failed')
              const details = actionResponse?.result?.details
              let printExtensionMessage = false
              let extensionMessage = null
              try {
                const parsedDetails = JSON.parse(details)
                printExtensionMessage = String(parsedDetails?.printmessage).toLowerCase() === 'true'
                extensionMessage = parsedDetails?.message
              } catch (_) {}
              const modalType = success ? this.$success : this.$error
              const contentElements = [h('p', `${message}`)]
              if (extensionMessage && !Array.isArray(extensionMessage) && typeof extensionMessage === 'object' && Object.keys(extensionMessage).length > 0) {
                extensionMessage = [extensionMessage]
              }
              if (Array.isArray(extensionMessage) && extensionMessage.length > 0 && printExtensionMessage) {
                contentElements.push(
                  h('div', {
                    style: {
                      marginTop: '1em',
                      maxHeight: '50vh',
                      maxWidth: '100%',
                      overflow: 'auto',
                      backgroundColor: '#f6f6f6',
                      border: '1px solid #ddd',
                      borderRadius: '4px',
                      display: 'block'
                    }
                  }, [
                    h('table', {
                      style: {
                        width: '100%',
                        minWidth: 'max-content',
                        borderCollapse: 'collapse',
                        whiteSpace: 'pre-wrap'
                      }
                    }, [
                      h('thead', [
                        h('tr', Object.keys(extensionMessage[0]).map(key =>
                          h('th', {
                            style: {
                              padding: '8px',
                              border: '1px solid #ddd',
                              textAlign: 'left',
                              fontWeight: 'bold',
                              backgroundColor: '#fafafa'
                            }
                          }, key)
                        ))
                      ]),
                      h('tbody', extensionMessage.map(row =>
                        h('tr', Object.values(row).map(value =>
                          h('td', {
                            style: {
                              padding: '8px',
                              border: '1px solid #ddd',
                              fontFamily: 'monospace'
                            }
                          }, String(value))
                        ))
                      ))
                    ])
                  ])
                )
              } else if (printExtensionMessage === 'true') {
                contentElements.push(
                  h('div', {
                    style: {
                      marginTop: '1em',
                      maxHeight: '50vh',
                      padding: '10px',
                      overflowY: 'auto',
                      backgroundColor: '#f6f6f6',
                      border: '1px solid #ddd',
                      borderRadius: '4px',
                      fontFamily: 'monospace',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word'
                    }
                  }, String(extensionMessage))
                )
              }

              modalType({
                title: this.currentAction.name || this.$t('label.run.custom.action'),
                content: h('div', contentElements),
                width: '700px',
                okText: this.$t('label.ok')
              })
            },
            errorMessage: this.currentAction.name || this.$t('label.run.custom.action'),
            loadingMessage: this.$t('message.running.custom.action'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;
  @media (min-width: 600px) {
    width: 550px;
  }
}
</style>
