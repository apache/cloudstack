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
    <span v-if="uploadPercentage > 0">
      <a-icon type="loading" />
      {{ $t('message.upload.file.processing') }}
      <a-progress :percent="uploadPercentage" />
    </span>
    <a-spin :spinning="loading" v-else>
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('label.templatefileupload')">
          <a-upload-dragger
            :multiple="false"
            :fileList="fileList"
            :remove="handleRemove"
            :beforeUpload="beforeUpload"
            v-decorator="['file', {
              rules: [{ required: true, message: `${this.$t('message.error.required.input')}`}]
            }]">
            <p class="ant-upload-drag-icon">
              <a-icon type="cloud-upload" />
            </p>
            <p class="ant-upload-text" v-if="fileList.length === 0">
              {{ $t('label.volume.volumefileupload.description') }}
            </p>
          </a-upload-dragger>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.volume.name') }]
            }]"
            :placeholder="$t('label.volumename')"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          <a-select
            v-decorator="['zoneId', {
              initialValue: zoneSelected,
              rules: [
                {
                  required: true,
                  message: `${this.$t('message.error.select')}`
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option :value="zone.id" v-for="zone in zones" :key="zone.id" :label="zone.name || zone.description">
              <span>
                <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="global" style="margin-right: 5px"/>
                {{ zone.name || zone.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.format')" :tooltip="apiParams.format.description"/>
          <a-select
            v-decorator="['format', {
              initialValue: formats[0],
              rules: [
                {
                  required: false,
                  message: `${this.$t('message.error.select')}`
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="format in formats" :key="format">
              {{ format }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.volumechecksum')" :tooltip="apiParams.checksum.description"/>
          <a-input
            v-decorator="['checksum']"
            :placeholder="$t('label.volumechecksum.description')"
          />
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" ref="submit" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import { axios } from '../../utils/request'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'UploadLocalVolume',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      fileList: [],
      zones: [],
      formats: ['RAW', 'VHD', 'VHDX', 'OVA', 'QCOW2'],
      zoneSelected: '',
      uploadParams: null,
      loading: false,
      uploadPercentage: 0
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('getUploadParamsForVolume')
  },
  created () {
    this.listZones()
  },
  methods: {
    listZones () {
      api('listZones', { showicon: true }).then(json => {
        if (json && json.listzonesresponse && json.listzonesresponse.zone) {
          this.zones = json.listzonesresponse.zone
          if (this.zones.length > 0) {
            this.zoneSelected = this.zones[0].id
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
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
        this.loading = true
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
</style>
