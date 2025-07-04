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
      <a-card class="ant-form-text" style="text-align: justify; margin: 10px 0; padding: 15px;" v-html="$t('message.desc.register.template')" />
      <a-table
        :columns="columns"
        :dataSource="predefinedTemplates"
        :rowSelection="rowSelection"
        :loading="loading"
        :scroll="{ y: 450 }"
        size="middle"
        :rowKey="record => record.id"
        :pagination="false"
        class="form-content"
      >
      <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <os-logo :osName="record.name" size="xl" />
            {{ record.name }}
          </template>
        </template>
      </a-table>
      <div class="form-action">
        <a-button class="button-back" @click="handleDone">{{ selectedRowKeys.length > 0 ? $t('label.done') : $t('label.skip') }}</a-button>
        <a-button class="button-next" type="primary" @click="handleSubmit" ref="submit">{{ $t('label.register.template') }}</a-button>
      </div>

      <a-modal
      :visible="showAlert"
      :footer="null"
      style="top: 20px;"
      centered
      width="auto"
      @cancel="showAlert = false"
      >
      <template #title>
        {{ $t('label.warning') }}
      </template>
      <a-alert type="warning">
        <template #message>
          <span v-html="$t('message.warn.select.template')" />
        </template>
      </a-alert>
      <a-divider style="margin-top: 0;"></a-divider>
    </a-modal>
    </div>
  </template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import OsLogo from '@/components/widgets/OsLogo'

export default {
  name: 'ZoneWizardRegisterTemplate',
  components: {
    OsLogo
  },
  props: {
    zoneid: {
      type: String,
      required: false
    },
    arch: {
      type: String,
      required: false
    },
    zoneSuperType: {
      type: String,
      required: false
    }
  },
  data: () => ({
    columns: null,
    loading: false,
    predefinedTemplates: [],
    rowKey: 0,
    selectedRowKeys: [],
    defaultOsTypeId: null,
    deployedTemplates: {},
    showAlert: false
  }),
  created () {
    this.initForm()
  },
  mounted () {
    this.fetchPredefinedTemplates()
  },
  computed: {
    rowSelection () {
      return {
        selectedRowKeys: this.selectedRowKeys || [],
        onChange: this.onSelectRow,
        getCheckboxProps: (record) => {
          return {
            disabled: record.disabled
          }
        }
      }
    }
  },
  methods: {
    async initForm () {
      this.columns = [
        {
          title: 'Name',
          dataIndex: 'name',
          key: 'name',
          width: 240,
          sorter: (a, b) => { return genericCompare(a.name || '', b.name || '') }
        },
        {
          title: 'Arch',
          dataIndex: 'arch',
          key: 'arch',
          width: 80,
          sorter: (a, b) => { return genericCompare(a.arch || '', b.arch || '') }
        },
        {
          title: 'URL',
          dataIndex: 'url',
          key: 'url'
        }
      ]
      this.defaultOsTypeId = await this.fetchOsTypeId('Other Linux (64-bit)')
    },
    handleDone () {
      this.$emit('refresh-data')
    },
    async handleSubmit () {
      await this.stepRegisterTemplates()
    },
    onSelectRow (value) {
      this.selectedRowKeys = value
    },
    async registerTemplate (templateData) {
      const params = {
        displaytext: templateData.name + ' ' + templateData.arch,
        format: this.getImageFormat(templateData.url),
        hypervisor: 'KVM',
        name: templateData.name,
        arch: templateData.arch,
        url: templateData.url,
        ostypeid: await this.fetchOsTypeId(templateData.name),
        zoneid: this.zoneid
      }
      if (this.zoneSuperType === 'Edge') {
        params.directdownload = true
      }
      return new Promise((resolve, reject) => {
        api('registerTemplate', params).then(json => {
          const result = json.registertemplateresponse.template[0]
          resolve(result)
        }).catch(error => {
          const message = error.response.headers['x-description']
          reject(message)
        })
      })
    },
    async stepRegisterTemplates () {
      const templatesToRegister = this.predefinedTemplates.filter(template => this.selectedRowKeys.includes(template.id) && this.deployedTemplates[template.id] !== true)
      if (templatesToRegister.length === 0) {
        this.showAlert = true
        return
      }
      const registrationResults = []
      for (const templateData of templatesToRegister) {
        const promise = this.registerTemplate(templateData)
          .then(() => ({
            id: templateData.id,
            status: 'success',
            name: templateData.name
          }))
          .catch(() => ({
            id: templateData.id,
            status: 'error',
            name: templateData.name
          }))
        registrationResults.push(promise)
      }
      const results = await Promise.all(registrationResults)
      const successful = results.filter(r => r.status === 'success')
      const failed = results.filter(r => r.status === 'error')

      if (successful.length > 0) {
        this.$notification.success({
          message: this.$t('label.register.template'),
          description: 'Succesfully registered templates: ' + successful.map(r => r.name).join(', ')
        })

        successful.forEach(r => {
          this.deployedTemplates[r.id] = true
          this.predefinedTemplates.find(t => t.id === r.id).disabled = true
        })
      }
      if (failed.length > 0) {
        this.$notification.error({
          message: this.$t('label.register.template'),
          description: 'Failed registering templates: ' + failed.map(r => r.name).join(', ')
        })

        failed.forEach(r => {
          this.predefinedTemplates.find(t => t.id === r.id).disabled = true
          this.selectedRowKeys = this.selectedRowKeys.filter(id => id !== r.id)
        })
      }
    },
    async fetchOsTypeId (osName) {
      let osTypeId = this.defaultOsTypeId
      this.loading = true
      try {
        const json = await api('listOsTypes', { keyword: osName, filter: 'name,id' })
        if (json && json.listostypesresponse && json.listostypesresponse.ostype && json.listostypesresponse.ostype.length > 0) {
          osTypeId = json.listostypesresponse.ostype[0].id
        }
      } catch (error) {
        console.error('Error fetching OS types:', error)
      } finally {
        this.loading = false
      }
      return osTypeId
    },
    getImageFormat (url) {
      const fileExtension = url.split('.').pop()
      var format = fileExtension
      switch (fileExtension) {
        case 'img':
          format = 'RAW'
          break
        case 'qcow2':
          format = 'qcow2'
          break
        default:
          format = 'RAW'
      }
      return format
    },
    async fetchPredefinedTemplates () {
      this.loading = true
      try {
        const response = await fetch('./cloud-image-templates.json')
        if (!response.ok) {
          throw new Error(`Error fetching predefined templates, status_code: ${response.status}`)
        }
        const templates = await response.json()
        this.predefinedTemplates = this.arch
          ? templates.filter(template => template.arch === this.arch)
          : templates

        // Replace 'https' with 'http' in all URLs for EdgeZone
        if (this.zoneSuperType === 'Edge') {
          this.predefinedTemplates.forEach(template => {
            if (template.url.startsWith('https://')) {
              template.url = template.url.replace('https://', 'http://')
            }
          })
        }
      } catch (error) {
        console.error('Error fetching predefined templates:', error)
        this.predefinedTemplates = []
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .form-content {
    border: 1px dashed #e9e9e9;
    border-radius: 6px;
    background-color: #fafafa;
    min-height: 440px;
    text-align: center;
    vertical-align: center;
    padding: 8px;
    padding-top: 16px;
    margin-top: 8px;
    overflow-y: auto;
  }
</style>
