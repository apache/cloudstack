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
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="file" ref="file">
          <template #label>
            <tooltip-label :title="$t('label.rules.file')" :tooltip="$t('label.rules.file.to.import')"/>
          </template>
          <a-upload-dragger
            :multiple="false"
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
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="true" />
        </a-form-item>

        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description" />
        </a-form-item>

        <a-form-item name="type" ref="type">
          <template #label>
            <tooltip-label :title="$t('label.type')" :tooltip="apiParams.type.description"/>
          </template>
          <a-select
            v-model:value="form.type"
            :placeholder="apiParams.type.description"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="role in defaultRoles" :key="role">
              {{ role }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="forced" ref="forced">
          <template #label>
            <tooltip-label :title="$t('label.forced')" :tooltip="apiParams.forced.description"/>
          </template>
          <a-switch v-model:checked="form.forced" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ImportRole',
  components: {
    TooltipLabel
  },
  data () {
    return {
      fileList: [],
      defaultRoles: ['Admin', 'DomainAdmin', 'ResourceAdmin', 'User'],
      rulesCsv: '',
      loading: false,
      csvFileType: ['.csv', 'text/csv', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel']
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('importRole')
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
        ],
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        type: [{ required: true, message: this.$t('message.error.select') }]
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
    beforeUpload (file) {
      if (!this.csvFileType.includes(file.type)) {
        return false
      }

      this.fileList = [file]
      this.form.file = file
      return false
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

        if (this.fileList.length !== 1) {
          return
        }

        var rules = this.rulesCsvToJson(this.rulesCsv)
        rules.forEach(function (values, index) {
          for (const key in values) {
            params['rules[' + index + '].' + key] = values[key]
          }
        })

        this.importRole(params)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    importRole (params) {
      this.loading = true
      api('importRole', {}, 'POST', params).then(json => {
        const role = json.importroleresponse.role
        if (role) {
          this.$emit('refresh-data')
          this.$notification.success({
            message: 'Import Role',
            description: 'Sucessfully imported role ' + params.name
          })
        }
        this.closeAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    rulesCsvToJson (rulesCsv) {
      const columnDelimiter = ','
      const lineDelimiter = '\n'
      var lines = rulesCsv.split(lineDelimiter)
      var result = []
      if (lines.length === 0) {
        return result
      }
      var headers = lines[0].split(columnDelimiter)
      lines = lines.slice(1) // Remove header

      lines.map((line, indexLine) => {
        if (line.trim() === '') return // Empty line
        var obj = {}
        var currentline = line.trim().split(columnDelimiter)

        headers.map((header, indexHeader) => {
          if (indexHeader === 2 && currentline.length > 3) {
            if (currentline[indexHeader].startsWith('"')) {
              obj[header.trim()] = currentline[indexHeader].substr(1)
            } else {
              obj[header.trim()] = currentline[indexHeader]
            }

            for (let i = 3; i < currentline.length - 1; i++) {
              obj[header.trim()] += columnDelimiter + currentline[i]
            }

            var lastColumn = currentline[currentline.length - 1]
            if (lastColumn.endsWith('"')) {
              obj[header.trim()] += columnDelimiter + lastColumn.substr(0, lastColumn.length - 1)
            } else {
              obj[header.trim()] += columnDelimiter + lastColumn
            }
          } else {
            obj[header.trim()] = currentline[indexHeader]
          }
        })
        result.push(obj)
      })
      return result
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
          console.log(reason)
          return Promise.reject(rule.message)
        }
      }
    },
    readCsvFile (file) {
      return new Promise((resolve, reject) => {
        if (window.FileReader) {
          var reader = new FileReader()
          reader.onload = (event) => {
            this.rulesCsv = event.target.result
            var lines = this.rulesCsv.split('\n')
            var headers = lines[0].split(',')
            if (headers.length !== 3) {
              resolve(false)
            } else if (!(headers[0].trim() === 'rule' && headers[1].trim() === 'permission' && headers[2].trim() === 'description')) {
              resolve(false)
            } else {
              resolve(true)
            }
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
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }
</style>
