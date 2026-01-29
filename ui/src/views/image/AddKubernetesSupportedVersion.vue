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
      <loading-outlined />
      {{ $t('message.upload.file.processing') }}
      <a-progress :percent="uploadPercentage" />
    </span>
    <a-spin :spinning="loading" v-else>
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item ref="semanticversion" name="semanticversion">
          <template #label>
            <tooltip-label :title="$t('label.semanticversion')" :tooltip="apiParams.semanticversion.description"/>
          </template>
          <a-input
            v-model:value="form.semanticversion"
            :placeholder="apiParams.semanticversion.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item ref="name" name="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item ref="zoneid" name="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            id="zone-selection"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description"
            @change="handleZoneChange">
            <a-select-option v-for="opt in this.zones" :key="opt.id" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="url" name="url" v-if="currentForm === 'Create'">
          <template #label>
            <tooltip-label :title="$t('label.url')" :tooltip="apiParams.url.description"/>
          </template>
          <a-input
            v-model:value="form.url"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item ref="file" name="file" :label="$t('label.templatefileupload')" v-else>
          <a-upload-dragger
            :multiple="false"
            :fileList="fileList"
            @remove="handleRemove"
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
        <a-form-item ref="checksum" name="checksum">
          <template #label>
            <tooltip-label :title="$t('label.checksum')" :tooltip="apiParams.checksum.description"/>
          </template>
          <a-input
            v-model:value="form.checksum"
            :placeholder="apiParams.checksum.description" />
        </a-form-item>
        <a-form-item ref="mincpunumber" name="mincpunumber">
          <template #label>
            <tooltip-label :title="$t('label.mincpunumber')" :tooltip="apiParams.mincpunumber.description"/>
          </template>
          <a-input
            v-model:value="form.mincpunumber"
            :placeholder="apiParams.mincpunumber.description"/>
        </a-form-item>
        <a-form-item ref="minmemory" name="minmemory">
          <template #label>
            <tooltip-label :title="$t('label.minmemory')" :tooltip="apiParams.minmemory.description"/>
          </template>
          <a-input
            v-model:value="form.minmemory"
            :placeholder="apiParams.minmemory.description"/>
        </a-form-item>
        <a-form-item ref="directdownload" name="directdownload" v-if="currentForm !== 'Upload'">
          <template #label>
            <tooltip-label :title="$t('label.directdownload')" :tooltip="apiParams.directdownload.description"/>
          </template>
          <a-switch
            :disabled="directDownloadDisabled"
            v-model:checked="form.directdownload"
            :placeholder="apiParams.directdownload.description"/>
        </a-form-item>
        <a-form-item
          name="arch"
          ref="arch">
          <template #label>
            <tooltip-label :title="$t('label.arch')" :tooltip="apiParams.arch.description"/>
          </template>
          <a-select
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-model:value="form.arch"
            :placeholder="apiParams.arch.description">
            <a-select-option v-for="opt in architectureTypes.opts" :key="opt.id">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
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
import { getAPI, postAPI } from '@/api'
import { axios } from '../../utils/request'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddKubernetesSupportedVersion',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  props: {
    action: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      fileList: [],
      zones: [],
      zoneLoading: false,
      loading: false,
      selectedZone: {},
      uploadParams: null,
      directDownloadDisabled: false,
      lastNonEdgeDirectDownloadUserSelection: false,
      architectureTypes: {},
      uploadPercentage: 0,
      currentForm: ['plus-outlined', 'PlusOutlined'].includes(this.action.currentAction.icon) ? 'Create' : 'Upload'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('addKubernetesSupportedVersion')
  },
  created () {
    if (this.$store.getters.userInfo.roletype === 'Admin' && this.currentForm === 'Create') {
      this.zones = [
        {
          id: null,
          name: this.$t('label.all.zone')
        }
      ]
    }
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        mincpunumber: 2,
        minmemory: 2048,
        directdownload: false
      })
      this.rules = reactive({
        semanticversion: [{ required: true, message: this.$t('message.error.kuberversion') }],
        name: [{ required: true, message: this.$t('message.error.name') }],
        zoneid: [{
          required: this.currentForm === 'Upload',
          message: this.$t('message.error.select')
        }],
        url: [{ required: true, message: this.$t('message.error.binaries.iso.url') }],
        mincpunumber: [
          { required: true, message: this.$t('message.please.enter.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value <= 0)) {
                return Promise.reject(this.$t('message.validate.number'))
              }
              return Promise.resolve()
            }
          }
        ],
        minmemory: [
          { required: true, message: this.$t('message.please.enter.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value <= 0)) {
                return Promise.reject(this.$t('message.validate.number'))
              }
              Promise.resolve()
            }
          }
        ]
      })
    },
    fetchData () {
      this.architectureTypes.opts = this.$fetchCpuArchitectureTypes()
      this.fetchZoneData()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true
      this.zoneLoading = true
      getAPI('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = (this.zones[0].id ? this.zones[0].id : '')
          this.selectedZone = this.zones[0]
        }
      })
    },
    handleZoneChange (zoneIdx) {
      const zone = this.zones[zoneIdx]
      if (this.selectedZone.type === zone.type) {
        return
      }
      var lastZoneType = this.selectedZone?.type || ''
      if (lastZoneType !== 'Edge') {
        this.nonEdgeDirectDownloadUserSelection = this.form.directdownload
      }
      this.selectedZone = zone
      if (zone.type && zone.type === 'Edge') {
        this.form.directdownload = true
        this.directDownloadDisabled = true
        return
      }
      this.directDownloadDisabled = false
      this.form.directdownload = this.nonEdgeDirectDownloadUserSelection
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {}
        const customCheckParams = ['mincpunumber', 'minmemory']
        for (const key in values) {
          if (!customCheckParams.includes(key) && values[key]) {
            params[key] = values[key]
          }
        }
        if (this.isValidValueForKey(values, 'mincpunumber') && values.mincpunumber > 0) {
          params.mincpunumber = values.mincpunumber
        }
        if (this.isValidValueForKey(values, 'minmemory') && values.minmemory > 0) {
          params.minmemory = values.minmemory
        }

        if (this.currentForm === 'Create') {
          postAPI('addKubernetesSupportedVersion', params).then(json => {
            this.$message.success(`${this.$t('message.success.add.kuberversion')}: ${values.semanticversion}`)
            this.$emit('refresh-data')
            this.closeAction()
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
          })
        } else {
          if (this.fileList.length !== 1) {
            return
          }
          params.format = 'ISO'
          this.loading = true
          postAPI('getUploadParamsForKubernetesSupportedVersion', params).then(json => {
            this.uploadParams = (json.getuploadparamsforkubernetessupportedversionresponse && json.getuploadparamsforkubernetessupportedversionresponse.getuploadparams) ? json.getuploadparamsforkubernetessupportedversionresponse.getuploadparams : ''
            const response = this.handleUpload()
            if (response === 'upload successful') {
              this.$notification.success({
                message: this.$t('message.success.upload'),
                description: this.$t('message.success.add.kuberversion.from.local')
              })
            }
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
            this.$emit('refresh-data')
          })
        }
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleRemove (file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
      this.form.file = undefined
    },
    beforeUpload (file) {
      this.fileList = [file]
      this.form.file = file
      return false
    },
    handleUpload () {
      const { fileList } = this
      if (this.fileList.length > 1) {
        this.$notification.error({
          message: this.$t('message.upload.iso.failed'),
          description: this.$t('message.error.upload.iso.description'),
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
            'content-type': 'multipart/form-data',
            'x-signature': this.uploadParams.signature,
            'x-expires': this.uploadParams.expires,
            'x-metadata': this.uploadParams.metadata
          },
          onUploadProgress: (progressEvent) => {
            this.uploadPercentage = Number(parseFloat(100 * progressEvent.loaded / progressEvent.total).toFixed(1))
          },
          timeout: 86400000
        }).then((json) => {
        this.$notification.success({
          message: this.$t('message.success.upload'),
          description: this.$t('message.success.upload.description')
        })
        this.closeAction()
        this.$emit('refresh-data')
      }).catch(e => {
        this.$notification.error({
          message: this.$t('message.upload.failed'),
          description: `${this.$t('message.upload.iso.failed.description')} -  ${e}`,
          duration: 0
        })
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
