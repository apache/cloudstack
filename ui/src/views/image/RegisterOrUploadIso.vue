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
      <a-icon type="loading" />
      {{ $t('message.upload.file.processing') }}
      <a-progress :percent="uploadPercentage" />
    </span>
    <a-spin :spinning="loading" v-else>
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item v-if="currentForm === 'Create'" :label="$t('label.url')">
          <a-input
            :autoFocus="currentForm === 'Create'"
            v-decorator="['url', {
              rules: [{ required: true, message: `${this.$t('label.upload.iso.from.local')}` }]
            }]"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item v-if="currentForm === 'Upload'" :label="$t('label.templatefileupload')">
          <a-upload-dragger
            :multiple="false"
            :fileList="fileList"
            :remove="handleRemove"
            :beforeUpload="beforeUpload"
            v-decorator="['file', {
              rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
            }]">
            <p class="ant-upload-drag-icon">
              <a-icon type="cloud-upload" />
            </p>
            <p class="ant-upload-text" v-if="fileList.length === 0">
              {{ $t('label.volume.volumefileupload.description') }}
            </p>
          </a-upload-dragger>
        </a-form-item>
        <a-form-item :label="$t('label.name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.name.description"
            :autoFocus="currentForm !== 'Create'" />
        </a-form-item>

        <a-form-item :label="$t('label.displaytext')">
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.displaytext.description" />
        </a-form-item>

        <a-form-item v-if="allowed && currentForm !== 'Upload'" :label="$t('label.directdownload')">
          <a-switch v-decorator="['directdownload']"/>
        </a-form-item>

        <a-form-item :label="$t('label.zoneid')">
          <a-select
            v-decorator="['zoneid', {
              initialValue: this.selectedZone,
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
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option :value="opt.id" v-for="opt in zones" :key="opt.id">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('label.bootable')">
          <a-switch
            v-decorator="['bootable', {
              initialValue: true,
            }]"
            :checked="bootable"
            @change="val => bootable = val"/>
        </a-form-item>

        <a-form-item v-if="bootable" :label="$t('label.ostypeid')">
          <a-select
            v-decorator="['ostypeid', {
              initialValue: defaultOsType,
              rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="osTypeLoading"
            :placeholder="apiParams.ostypeid.description">
            <a-select-option :value="opt.id" v-for="(opt, optIndex) in osTypes" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('label.isextractable')">
          <a-switch
            v-decorator="['isextractable', {
              initialValue: false
            }]" />
        </a-form-item>

        <a-form-item
          :label="$t('label.ispublic')"
          v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.userpublictemplateenabled" >
          <a-switch
            v-decorator="['ispublic', {
              initialValue: false
            }]" />
        </a-form-item>

        <a-form-item :label="$t('label.isfeatured')">
          <a-switch
            v-decorator="['isfeatured', {
              initialValue: false
            }]" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import store from '@/store'
import { axios } from '../../utils/request'

export default {
  name: 'RegisterIso',
  props: {
    resource: {
      type: Object,
      required: true
    },
    action: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      fileList: [],
      zones: [],
      osTypes: [],
      zoneLoading: false,
      osTypeLoading: false,
      defaultOsType: '',
      loading: false,
      allowed: false,
      bootable: true,
      selectedZone: '',
      uploadParams: null,
      uploadPercentage: 0,
      currentForm: this.action.currentAction.icon === 'plus' ? 'Create' : 'Upload'
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.registerIso || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.zones = []
    if (this.$store.getters.userInfo.roletype === 'Admin' && this.currentForm === 'Create') {
      this.zones = [
        {
          id: '-1',
          name: this.$t('label.all.zone')
        }
      ]
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchZoneData()
      this.fetchOsType()
    },
    fetchZoneData () {
      const params = {}
      params.listAll = true

      this.zoneLoading = true
      if (store.getters.userInfo.roletype === this.rootAdmin) {
        this.allowed = true
      }
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
        this.selectedZone = (this.zones[0].id ? this.zones[0].id : '')
      })
    },
    fetchOsType () {
      const params = {}
      params.listAll = true

      this.osTypeLoading = true

      api('listOsTypes', params).then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.osTypes = this.osTypes.concat(listOsTypes)
      }).finally(() => {
        this.osTypeLoading = false
        this.defaultOsType = this.osTypes[0].id
      })
    },
    handleRemove (file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
    },
    beforeUpload (file) {
      this.fileList = [file]
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
    },
    handleSubmit (e) {
      e.preventDefault()
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
          switch (key) {
            case 'zoneid':
              var zone = this.zones.filter(zone => zone.id === input)
              params[key] = zone[0].id
              break
            case 'ostypeid':
              params[key] = input
              break
            default:
              params[key] = input
              break
          }
        }

        if (this.currentForm === 'Create') {
          this.loading = true
          api('registerIso', params).then(json => {
            this.$notification.success({
              message: this.$t('label.action.register.iso'),
              description: `${this.$t('message.success.register.iso')} ${params.name}`
            })
            this.closeAction()
            this.$emit('refresh-data')
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
          api('getUploadParamsForIso', params).then(json => {
            this.uploadParams = (json.postuploadisoresponse && json.postuploadisoresponse.getuploadparams) ? json.postuploadisoresponse.getuploadparams : ''
            const response = this.handleUpload()
            if (response === 'upload successful') {
              this.$notification.success({
                message: this.$t('message.success.upload'),
                description: this.$t('message.success.upload.iso.description')
              })
            }
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
            this.$emit('refresh-data')
          })
        }
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
