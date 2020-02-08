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
      Do not close this form, file upload is in progress...
      <a-progress :percent="uploadPercentage" />
    </span>
    <a-spin :spinning="loading" v-else>
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item v-if="currentForm === 'Create'" :label="$t('url')">
          <a-input
            v-decorator="['url', {
              rules: [{ required: true, message: 'Please upload an ISO' }]
            }]"
            :placeholder="$t('iso.url.description')" />
        </a-form-item>
        <a-form-item v-if="currentForm === 'Upload'" :label="$t('templateFileUpload')">
          <a-upload-dragger
            :multiple="false"
            :fileList="fileList"
            :remove="handleRemove"
            :beforeUpload="beforeUpload"
            v-decorator="['file', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]">
            <p class="ant-upload-drag-icon">
              <a-icon type="cloud-upload" />
            </p>
            <p class="ant-upload-text" v-if="fileList.length === 0">
              Click or drag file to this area to upload
            </p>
          </a-upload-dragger>
        </a-form-item>
        <a-form-item :label="$t('name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('iso.name.description')" />
        </a-form-item>

        <a-form-item :label="$t('displaytext')">
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('iso.displaytext.description')" />
        </a-form-item>

        <a-form-item v-if="allowed && currentForm !== 'Upload'" :label="$t('directdownload')">
          <a-switch v-decorator="['directdownload']"/>
        </a-form-item>

        <a-form-item :label="$t('zoneid')">
          <a-select
            v-decorator="['zoneid', {
              initialValue: this.selectedZone,
              rules: [
                {
                  required: true,
                  message: 'Please select option'
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="$t('iso.zoneid.description')">
            <a-select-option :value="opt.id" v-for="opt in zones" :key="opt.id">
              <div v-if="currentForm === 'Upload'">
                <div v-if="opt.name !== $t('label.all.zone')">
                  {{ opt.name || opt.description }}
                </div>
              </div>
              <div v-else>
                {{ opt.name || opt.description }}
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('bootable')">
          <a-switch
            v-decorator="['bootable', {
              initialValue: true,
            }]"
            :checked="bootable"
            @change="val => bootable = val"/>
        </a-form-item>

        <a-form-item v-if="bootable" :label="$t('ostypeid')">
          <a-select
            v-decorator="['ostypeid', {
              initialValue: defaultOsType,
              rules: [{ required: true, message: 'Please select option' }]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="osTypeLoading"
            :placeholder="$t('iso.ostypeid.description')">
            <a-select-option :value="opt.description" v-for="(opt, optIndex) in osTypes" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('isextractable')">
          <a-switch
            v-decorator="['isextractable', {
              initialValue: false
            }]" />
        </a-form-item>

        <a-form-item :label="$t('ispublic')">
          <a-switch
            v-decorator="['ispublic', {
              initialValue: false
            }]" />
        </a-form-item>

        <a-form-item :label="$t('isfeatured')">
          <a-switch
            v-decorator="['isfeatured', {
              initialValue: false
            }]" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
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
      currentForm: this.action.currentAction.api === 'registerIso' ? 'Create' : 'Upload'
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.zones = [
      {
        id: '-1',
        name: this.$t('label.all.zone')
      }
    ]
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
        this.selectedZone = this.currentForm === 'Create' ? (this.zones[0].id ? this.zones[0].id : '') : ((this.zones[1].id) ? this.zones[1].id : '')
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
        this.defaultOsType = this.osTypes[0].description
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
          message: 'ISO Upload Failed',
          description: 'Only one ISO can be uploaded at a time',
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
          message: 'Upload Successful',
          description: 'This ISO file has been uploaded. Please check its status at Templates menu'
        })
        this.closeAction()
      }).catch(e => {
        this.$notification.error({
          message: 'Upload Failed',
          description: `Failed to upload ISO -  ${e}`,
          duration: 0
        })
        this.closeAction()
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
              var os = this.osTypes.filter(osType => osType.description === input)
              params[key] = os[0].id
              break
            default:
              params[key] = input
              break
          }
        }

        if (this.currentForm === 'Create') {
          this.loading = true
          api('registerIso', params).then(json => {
            this.$emit('refresh-data')
            this.$notification.success({
              message: 'Register ISO',
              description: 'Sucessfully registered ISO ' + params.name
            })
          }).catch(error => {
            this.$notification.error({
              message: 'Request Failed',
              description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
              duration: 0
            })
          }).finally(() => {
            this.loading = false
            this.closeAction()
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
                message: 'Upload Successful',
                description: 'This ISO file has been uploaded. Please check its status in the Images > ISOs menu'
              })
            }
          }).catch(error => {
            this.$notification.error({
              message: 'Request Failed',
              description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
              duration: 0
            })
          }).finally(() => {
            this.loading = false
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
