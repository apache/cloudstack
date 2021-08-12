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
  <div class="form-layout">
    <span v-if="uploadPercentage > 0">
      <loading-outlined />
      {{ $t('message.upload.file.processing') }}
      <a-progress :percent="uploadPercentage" />
    </span>
    <a-spin :spinning="loading" v-else>
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('label.templatefileupload')" ref="file" name="file">
          <a-upload-dragger
            :multiple="false"
            :fileList="fileList"
            :remove="handleRemove"
            :beforeUpload="beforeUpload"
            v-model:value="form.file">
            <p class="ant-upload-drag-icon">
              <cloud-upload-outlined />
            </p>
            <p class="ant-upload-text" v-if="fileList.length === 0">
              {{ $t('label.volume.volumefileupload.description') }}
            </p>
          </a-upload-dragger>
        </a-form-item>
        <a-form-item ref="name" name="name">
          <template #label>
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <info-circle-outlined />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="$t('label.volumename')"
            autoFocus />
        </a-form-item>
        <a-form-item ref="zoneId" name="zoneId">
          <template #label>
            {{ $t('label.zone') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <info-circle-outlined />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.zoneId">
            <a-select-option :value="zone.id" v-for="zone in zones" :key="zone.id">
              {{ zone.name || zone.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="format" name="format">
          <template #label>
            {{ $t('label.format') }}
            <a-tooltip :title="apiParams.format.description">
              <info-circle-outlined />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.format">
            <a-select-option v-for="format in formats" :key="format">
              {{ format }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="checksum" name="checksum">
          <template #label>
            {{ $t('label.volumechecksum') }}
            <a-tooltip :title="apiParams.checksum.description">
              <info-circle-outlined />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.checksum"
            :placeholder="$t('label.volumechecksum.description')"
          />
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" html-type="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { axios } from '../../utils/request'

export default {
  name: 'UploadLocalVolume',
  data () {
    return {
      fileList: [],
      zones: [],
      formats: ['RAW', 'VHD', 'VHDX', 'OVA', 'QCOW2'],
      uploadParams: null,
      loading: false,
      uploadPercentage: 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('getUploadParamsForVolume')
  },
  created () {
    this.initForm()
    this.listZones()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        file: undefined,
        name: undefined,
        zoneId: undefined,
        format: 'RAW',
        checksum: undefined
      })
      this.rules = reactive({
        file: [{ required: true, message: this.$t('message.error.required.input') }],
        name: [{ required: true, message: this.$t('message.error.volume.name') }],
        zoneId: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    listZones () {
      api('listZones').then(json => {
        if (json && json.listzonesresponse && json.listzonesresponse.zone) {
          this.zones = json.listzonesresponse.zone
          if (this.zones.length > 0) {
            this.form.zoneId = this.zones[0].id
          }
        }
      })
    },
    handleRemove (file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
    },
    beforeUpload (file) {
      this.fileList = [...this.fileList, file]
      return false
    },
    handleSubmit () {
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
        api('getUploadParamsForVolume', params).then(json => {
          this.uploadParams = (json.postuploadvolumeresponse && json.postuploadvolumeresponse.getuploadparams) ? json.postuploadvolumeresponse.getuploadparams : ''
          const { fileList } = this
          if (this.fileList.length > 1) {
            this.$notification.error({
              message: this.$t('message.upload.volume.failed'),
              description: this.$t('message.upload.file.limit'),
              duration: 0
            })
          }
          const formData = new FormData()
          fileList.forEach(file => {
            formData.append('files[]', file)
          })
          this.loading = true
          this.uploadPercentage = 0
          axios.post(this.uploadParams.postURL,
            formData,
            {
              headers: {
                'Content-Type': 'multipart/form-data',
                'X-signature': this.uploadParams.signature,
                'X-expires': this.uploadParams.expires,
                'X-metadata': this.uploadParams.metadata
              },
              onUploadProgress: (progressEvent) => {
                this.uploadPercentage = Number(parseFloat(100 * progressEvent.loaded / progressEvent.total).toFixed(1))
              },
              timeout: 86400000
            }).then((json) => {
            this.$notification.success({
              message: this.$t('message.success.upload'),
              description: this.$t('message.success.upload.volume.description')
            })
            this.closeAction()
          }).catch(e => {
            this.$notification.error({
              message: this.$t('message.upload.failed'),
              description: `${this.$t('message.upload.iso.failed.description')} -  ${e}`,
              duration: 0
            })
          }).finally(() => {
            this.loading = false
          })
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
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
