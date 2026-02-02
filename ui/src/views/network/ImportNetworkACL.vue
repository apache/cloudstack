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
  <div v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">

        <div class="info-section" style="margin-bottom: 24px; padding: 16px; background: #fafafa; border-radius: 4px;">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item :label="$t('label.acl.id')">
              <span style="font-family: monospace;">{{ resource.id }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('label.add.acl.name')">
              <strong>{{ resource.name }}</strong>
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <a-form-item name="file" ref="file">
          <template #label>
            <tooltip-label :title="$t('label.rules.file')" :tooltip="$t('label.rules.file.to.import')"/>
          </template>
          <a-upload-dragger
            :multiple=false
            :fileList="fileList"
            @remove="handleRemove"
            :beforeUpload="beforeUpload"
            @change="handleChange"
            v-model:value="form.file">
            <p class="ant-upload-drag-icon">
              <cloud-upload-outlined />
            </p>
            <p class="ant-upload-text" v-if="fileList.length === 0">
              {{ $t('label.rules.file.import.description') }}
            </p>
          </a-upload-dragger>
        </a-form-item>

        <a-form-item v-if="csvData.length > 0" :label="$t('label.csv.preview')">
          <div class="csv-preview">
            <a-table
                :columns="columns"
                :dataSource="csvData"
                :pagination="{ pageSize: 5 }"
                :scroll="{ x: true }"
                size="small">

                <template #action="{ record }">
                  <a-tag :color="record.action && record.action.toLowerCase() === 'allow' ? 'green' : 'red'">
                    {{ record.action ? record.action.toUpperCase() : 'N/A' }}
                  </a-tag>
                </template>

                <template #traffictype="{ record }">
                  <a-tag :color="record.traffictype && record.traffictype.toLowerCase() === 'ingress' ? 'blue' : 'orange'">
                    {{ record.traffictype ? record.traffictype.toUpperCase() : 'N/A' }}
                  </a-tag>
                </template>

            </a-table>
          </div>
        </a-form-item>

        <span>{{ $t('message.network.acl.import.note') }}</span><br/>

        <div :span="24" class="action-button">
          <a-button class="button-cancel" @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button
            class="button-submit"
            ref="submit"
            type="primary"
            :loading="loading"
            :disabled="csvData.length === 0"
            @click="handleSubmit">{{ $t('label.import') }}</a-button>
        </div>

      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ImportNetworkACL',
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
      loading: false,
      fileList: [],
      csvData: '',
      csvFileType: ['.csv', 'text/csv', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel'],
      columns: [
        {
          title: this.$t('label.protocol'),
          dataIndex: 'protocol',
          key: 'protocol',
          width: 100
        },
        {
          title: this.$t('label.action'),
          dataIndex: 'action',
          key: 'action',
          width: 100,
          slots: { customRender: 'action' }
        },
        {
          title: this.$t('label.cidr'),
          dataIndex: 'cidrlist',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('label.startport'),
          dataIndex: 'startport',
          width: 100
        },
        {
          title: this.$t('label.endport'),
          dataIndex: 'endport',
          width: 100
        },
        {
          title: this.$t('label.traffictype'),
          dataIndex: 'traffictype',
          key: 'traffictype',
          width: 120,
          slots: { customRender: 'traffictype' }
        },
        {
          title: this.$t('label.number'),
          dataIndex: 'number',
          width: 80
        },
        {
          title: this.$t('label.reason'),
          dataIndex: 'reason',
          ellipsis: true
        }
      ]
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('importNetworkACL')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        file: [
          { required: true, message: this.$t('message.error.required.input') },
          {
            validator: this.checkCsvRulesFile,
            message: this.$t('label.error.rules.file.import')
          }
        ]
      })
    },
    beforeUpload (file) {
      if (!this.csvFileType.includes(file.type)) {
        return false
      }
      this.fileList = [file]
      this.form.file = file
      return false // Stop from uploading automatically
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          if (key === 'file') {
            continue
          }
          params[key] = input
        }

        if (this.csvData.length === 0) {
          this.$message.error(this.$t('message.csv.no.data'))
          return
        }
        this.importNetworkACL()
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleRemove (file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
      this.form.file = undefined
    },
    handleChange (info) {
      if (info.file.status === 'error') {
        this.$notification.error({
          message: this.$t('label.error.file.upload'),
          description: this.$t('label.error.file.upload')
        })
      }
    },
    async checkCsvRulesFile (rule, value) {
      if (!value || value === '') {
        return Promise.resolve()
      } else {
        if (!this.csvFileType.includes(value.type)) {
          return Promise.reject(rule.message)
        }

        try {
          const validFile = await this.readCsvFile(value)
          if (!validFile) {
            return Promise.reject(rule.message)
          } else {
            return Promise.resolve()
          }
        } catch (reason) {
          return Promise.reject(rule.message)
        }
      }
    },
    readCsvFile (file) {
      return new Promise((resolve, reject) => {
        if (window.FileReader) {
          const reader = new FileReader()
          reader.onload = (event) => {
            const text = event.target.result
            const lines = text.split('\n').filter(line => line.trim() !== '')
            if (lines.length < 2) {
              this.$message.error(this.$t('message.csv.empty'))
              resolve(false)
            }
            const headers = this.parseCSVLine(lines[0])
            const requiredHeaders = ['protocol', 'cidrlist', 'traffictype']
            const missingHeaders = requiredHeaders.filter(h => !headers.includes(h.toLowerCase()))
            if (missingHeaders.length > 0) {
              this.$message.error(this.$t('message.csv.missing.headers') + ': ' + missingHeaders.join(', '))
              resolve(false)
            }
            // Parse data rows
            const data = []
            for (let i = 1; i < lines.length; i++) {
              const values = this.parseCSVLine(lines[i])
              if (values.length === headers.length) {
                const row = {}
                headers.forEach((header, index) => {
                  const value = values[index].trim()
                  if (value !== '' && value !== 'null') {
                    row[header.toLowerCase()] = value
                  }
                })
                data.push(row)
              }
            }

            this.csvData = data
            resolve(true)
          }

          reader.onerror = (event) => {
            if (event.target.error.name === 'NotReadableError') {
              reject(event.target.error)
            }
          }

          reader.readAsText(file)
        } else {
          reject(this.$t('label.error.file.read'))
        }
      })
    },
    parseCSVLine (line) {
      const result = []
      let current = ''
      let inQuotes = false

      for (let i = 0; i < line.length; i++) {
        const char = line[i]
        if (char === '"') {
          inQuotes = !inQuotes
        } else if (char === ',' && !inQuotes) {
          result.push(current)
          current = ''
        } else {
          current += char
        }
      }
      result.push(current)

      return result.map(v => v.trim())
    },
    closeAction () {
      this.$emit('close-action')
    },
    importNetworkACL () {
      this.loading = true
      const params = {
        aclid: this.resource.id
      }
      this.csvData.forEach(function (values, index) {
        for (const key in values) {
          params['rules[' + index + '].' + key] = values[key]
        }
      })
      postAPI('importNetworkACL', params).then(response => {
        this.$pollJob({
          jobId: response.importnetworkaclresponse.jobid,
          title: this.$t('message.success.add.network.acl'),
          successMethod: () => {
            this.loading = false
          },
          errorMessage: this.$t('message.add.network.acl.failed'),
          errorMethod: () => {
            this.loading = false
          },
          loadingMessage: this.$t('message.add.network.acl.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="less">
  .csv-preview {
  max-height: 400px;
  overflow: auto;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 10px;
}
</style>
