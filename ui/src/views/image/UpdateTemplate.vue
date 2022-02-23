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
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.name.description"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.displaytext.description"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.passwordenabled')" :tooltip="apiParams.passwordenabled.description"/>
          <a-switch v-decorator="['passwordenabled', {}]" />
        </a-form-item>

        <a-row :gutter="12" v-if="['KVM', 'VMware'].includes(resource.hypervisor)">
          <a-col :md="24" :lg="resource.hypervisor === 'KVM' ? 24 : 12" v-if="resource.hypervisor === 'KVM' || (resource.hypervisor === 'VMware' && !resource.deployasis)">
            <a-form-item :label="$t('label.rootdiskcontrollertype')">
              <a-select
                v-decorator="['rootDiskController', {
                  rules: [
                    {
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :loading="rootDisk.loading"
                :placeholder="$t('label.rootdiskcontrollertype')">
                <a-select-option v-for="opt in rootDisk.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="resource.hypervisor === 'VMware' && !resource.deployasis">
            <a-form-item :label="$t('label.nicadaptertype')">
              <a-select
                v-decorator="['nicAdapter', {
                  rules: [
                    {
                      required: false,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :placeholder="$t('label.nicadaptertype')">
                <a-select-option v-for="opt in nicAdapterType.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="resource.hypervisor !== 'VMware' || (resource.hypervisor === 'VMware' && !resource.deployasis)">
          <a-col :md="24" :lg="24">
            <a-form-item v-if="resource.hypervisor === 'VMware' && !resource.deployasis" :label="$t('label.keyboardtype')">
              <a-select
                v-decorator="['keyboard']"
                :placeholder="$t('label.keyboard')">
                <a-select-option v-for="opt in keyboardType.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24">
            <a-form-item :label="$t('label.ostypeid')">
              <a-select
                showSearch
                optionFilterProp="children"
                :filterOption="(input, option) => {
                  return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                v-decorator="['ostypeid', {
                  rules: [
                    {
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :loading="osTypes.loading"
                :placeholder="apiParams.ostypeid.description">
                <a-select-option v-for="opt in osTypes.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.isdynamicallyscalable')" :tooltip="apiParams.isdynamicallyscalable.description"/>
          <a-switch v-decorator="['isdynamicallyscalable', {}]" />
        </a-form-item>
        <a-form-item v-if="isAdmin">
          <tooltip-label slot="label" :title="$t('label.templatetype')" :tooltip="apiParams.templatetype.description"/>
          <span v-if="selectedTemplateType ==='SYSTEM'">
            <a-alert type="warning">
              <span slot="message" v-html="$t('message.template.type.change.warning')" />
            </a-alert>
            <br/>
          </span>
          <a-select
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-decorator="['templatetype']"
            :placeholder="apiParams.templatetype.description"
            @change="val => { selectedTemplateType = val }">
            <a-select-option v-for="opt in templatetypes" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'UpdateTemplate',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      templatetypes: ['BUILTIN', 'USER', 'SYSTEM', 'ROUTING'],
      rootDisk: {},
      nicAdapterType: {},
      keyboardType: {},
      osTypes: {},
      loading: false,
      selectedTemplateType: ''
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('updateTemplate')
    this.isAdmin = ['Admin'].includes(this.$store.getters.userInfo.roletype)
  },
  created () {
    this.$set(this.rootDisk, 'loading', false)
    this.$set(this.rootDisk, 'opts', [])
    this.$set(this.nicAdapterType, 'loading', false)
    this.$set(this.nicAdapterType, 'opts', [])
    this.$set(this.keyboardType, 'loading', false)
    this.$set(this.keyboardType, 'opts', [])
    this.$set(this.osTypes, 'loading', false)
    this.$set(this.osTypes, 'opts', [])
    const resourceFields = ['name', 'displaytext', 'passwordenabled', 'ostypeid', 'isdynamicallyscalable']
    if (this.isAdmin) {
      resourceFields.push('templatetype')
    }
    for (var field of resourceFields) {
      var fieldValue = this.resource[field]
      if (fieldValue) {
        this.form.getFieldDecorator(field, { initialValue: fieldValue })
      }
    }
    const resourceDetailsFields = []
    if (this.resource.hypervisor === 'KVM') {
      resourceDetailsFields.push('rootDiskController')
    } else if (this.resource.hypervisor === 'VMware' && !this.resource.deployasis) {
      resourceDetailsFields.push(...['rootDiskController', 'nicAdapter', 'keyboard'])
    }
    for (var detailsField of resourceDetailsFields) {
      var detailValue = this.resource?.details?.[detailsField] || null
      if (detailValue) {
        this.form.getFieldDecorator(detailsField, { initialValue: detailValue })
      }
    }
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchOsTypes()
      this.fetchRootDiskControllerTypes(this.resource.hypervisor)
      this.fetchNicAdapterTypes()
      this.fetchKeyboardTypes()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null && obj[key] !== undefined && obj[key] !== ''
    },
    fetchOsTypes () {
      const params = {}
      params.listAll = true
      this.osTypes.opts = []
      this.osTypes.loading = true
      api('listOsTypes', params).then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.$set(this.osTypes, 'opts', listOsTypes)
      }).finally(() => {
        this.osTypes.loading = false
      })
    },
    fetchRootDiskControllerTypes (hyperVisor) {
      const controller = []
      this.rootDisk.opts = []

      if (hyperVisor === 'KVM') {
        controller.push({
          id: '',
          description: ''
        })
        controller.push({
          id: 'ide',
          description: 'ide'
        })
        controller.push({
          id: 'osdefault',
          description: 'osdefault'
        })
        controller.push({
          id: 'scsi',
          description: 'scsi'
        })
        controller.push({
          id: 'virtio',
          description: 'virtio'
        })
      } else if (hyperVisor === 'VMware') {
        controller.push({
          id: '',
          description: ''
        })
        controller.push({
          id: 'scsi',
          description: 'scsi'
        })
        controller.push({
          id: 'ide',
          description: 'ide'
        })
        controller.push({
          id: 'osdefault',
          description: 'osdefault'
        })
        controller.push({
          id: 'pvscsi',
          description: 'pvscsi'
        })
        controller.push({
          id: 'lsilogic',
          description: 'lsilogic'
        })
        controller.push({
          id: 'lsisas1068',
          description: 'lsilogicsas'
        })
        controller.push({
          id: 'buslogic',
          description: 'buslogic'
        })
      }

      this.$set(this.rootDisk, 'opts', controller)
    },
    fetchNicAdapterTypes () {
      const nicAdapterType = []
      nicAdapterType.push({
        id: '',
        description: ''
      })
      nicAdapterType.push({
        id: 'E1000',
        description: 'E1000'
      })
      nicAdapterType.push({
        id: 'PCNet32',
        description: 'PCNet32'
      })
      nicAdapterType.push({
        id: 'Vmxnet2',
        description: 'Vmxnet2'
      })
      nicAdapterType.push({
        id: 'Vmxnet3',
        description: 'Vmxnet3'
      })

      this.$set(this.nicAdapterType, 'opts', nicAdapterType)
    },
    fetchKeyboardTypes () {
      const keyboardType = []
      const keyboardOpts = this.$config.keyboardOptions || {}
      keyboardType.push({
        id: '',
        description: ''
      })

      Object.keys(keyboardOpts).forEach(keyboard => {
        keyboardType.push({
          id: keyboard,
          description: this.$t(keyboardOpts[keyboard])
        })
      })

      this.$set(this.keyboardType, 'opts', keyboardType)
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {
          id: this.resource.id
        }
        const detailsField = ['rootDiskController', 'nicAdapter', 'keyboard']
        for (const key in values) {
          if (!this.isValidValueForKey(values, key)) continue
          if (detailsField.includes(key)) {
            params['details[0].' + key] = values[key]
            continue
          }
          params[key] = values[key]
        }
        api('updateTemplate', params).then(json => {
          this.$message.success(`${this.$t('message.success.update.template')}: ${this.resource.name}`)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
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
    width: 60vw;

    @media (min-width: 500px) {
      width: 450px;
    }
  }
</style>
