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
  <div
    class="form-layout"
    @keyup.ctrl.enter="handleSubmit">
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
        v-ctrl-enter="handleSubmit"
        layout="vertical">
        <a-form-item
          v-if="currentForm === 'Create'"
          ref="url"
          name="url"
          :label="$t('label.url')">
          <a-input
            v-focus="currentForm === 'Create'"
            v-model:value="form.url"
            :placeholder="apiParams.url.description" />
        </a-form-item>
        <a-form-item
          v-if="currentForm === 'Upload'"
          ref="file"
          name="file"
          :label="$t('label.templatefileupload')">
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
        <a-form-item ref="name" name="name" :label="$t('label.name')">
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="currentForm !== 'Create'" />
        </a-form-item>
        <a-form-item ref="displaytext" name="displaytext" :label="$t('label.displaytext')">
          <a-input
            v-model:value="form.displaytext"
            :placeholder="apiParams.displaytext.description" />
        </a-form-item>

        <a-form-item ref="directdownload" name="directdownload" v-if="allowed && currentForm !== 'Upload'" :label="$t('label.directdownload')">
          <a-switch v-model:checked="form.directdownload"/>
        </a-form-item>

        <a-form-item ref="zoneid" name="zoneid" :label="$t('label.zoneid')">
          <a-select
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option :value="opt.id" v-for="opt in zones" :key="opt.id" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item ref="bootable" name="bootable" :label="$t('label.bootable')">
          <a-switch v-model:checked="form.bootable" />
        </a-form-item>

        <a-form-item ref="ostypeid" name="ostypeid" v-if="form.bootable" :label="$t('label.ostypeid')">
          <a-select
            v-model:value="form.ostypeid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="osTypeLoading"
            :placeholder="apiParams.ostypeid.description">
            <a-select-option :value="opt.id" v-for="(opt, optIndex) in osTypes" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item
              name="userdataid"
              ref="userdataid"
              :label="$t('label.userdata')">
              <a-select
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                v-model:value="userdataid"
                :placeholder="linkUserDataParams.userdataid.description"
                :loading="userdata.loading">
                <a-select-option v-for="opt in userdata.opts" :key="opt.id" :label="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item ref="userdatapolicy" name="userdatapolicy">
              <template #label>
                <tooltip-label :title="$t('label.userdatapolicy')" :tooltip="$t('label.userdatapolicy.tooltip')"/>
              </template>
              <a-select
                showSearch
                v-model:value="userdatapolicy"
                :placeholder="linkUserDataParams.userdatapolicy.description"
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="opt in userdatapolicylist.opts" :key="opt.id" :label="opt.id || opt.description">
                  {{ opt.id || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item ref="isextractable" name="isextractable" :label="$t('label.isextractable')">
          <a-switch v-model:checked="form.isextractable" />
        </a-form-item>

        <a-form-item
          ref="ispublic"
          name="ispublic"
          :label="$t('label.ispublic')"
          v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.userpublictemplateenabled" >
          <a-switch v-model:checked="form.ispublic" />
        </a-form-item>

        <a-form-item ref="isfeatured" name="isfeatured" :label="$t('label.isfeatured')" v-if="$store.getters.userInfo.roletype === 'Admin'">
          <a-switch v-model:checked="form.isfeatured" />
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
import store from '@/store'
import { axios } from '../../utils/request'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'RegisterIso',
  mixins: [mixinForm],
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
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      fileList: [],
      zones: [],
      osTypes: [],
      zoneLoading: false,
      osTypeLoading: false,
      userdata: {},
      userdataid: null,
      userdatapolicy: null,
      userdatapolicylist: {},
      loading: false,
      allowed: false,
      uploadParams: null,
      uploadPercentage: 0,
      currentForm: ['plus-outlined', 'PlusOutlined'].includes(this.action.currentAction.icon) ? 'Create' : 'Upload'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('registerIso')
    this.linkUserDataParams = this.$getApiParams('linkUserDataToTemplate')
  },
  created () {
    this.initForm()
    this.zones = []
    if (this.$store.getters.userInfo.roletype === 'Admin' && this.currentForm === 'Create') {
      this.zones = [
        {
          id: '-1',
          name: this.$t('label.all.zone')
        }
      ]
    }
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        bootable: true,
        isextractable: false,
        ispublic: false
      })
      this.rules = reactive({
        url: [{ required: true, message: this.$t('label.upload.iso.from.local') }],
        file: [{ required: true, message: this.$t('message.error.required.input') }],
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        ostypeid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchZoneData()
      this.fetchOsType()
      this.fetchUserData()
      this.fetchUserdataPolicy()
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true

      this.zoneLoading = true
      if (store.getters.userInfo.roletype === this.rootAdmin) {
        this.allowed = true
      }
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
          this.zones = this.zones.filter(zone => zone.type !== 'Edge')
        }
      }).finally(() => {
        this.zoneLoading = false
        this.form.zoneid = (this.zones[0].id ? this.zones[0].id : '')
      })
    },
    fetchOsType () {
      this.osTypeLoading = true

      api('listOsTypes').then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.osTypes = this.osTypes.concat(listOsTypes)
      }).finally(() => {
        this.osTypeLoading = false
        this.form.ostypeid = this.osTypes[0].id
      })
    },
    fetchUserData () {
      const params = {}
      params.listAll = true

      this.userdata.opts = []
      this.userdata.loading = true

      api('listUserData', params).then(json => {
        const listUserdata = json.listuserdataresponse.userdata
        this.userdata.opts = listUserdata
      }).finally(() => {
        this.userdata.loading = false
      })
    },
    fetchUserdataPolicy () {
      const userdataPolicy = []
      userdataPolicy.push({
        id: 'allowoverride',
        description: 'allowoverride'
      })
      userdataPolicy.push({
        id: 'append',
        description: 'append'
      })
      userdataPolicy.push({
        id: 'denyoverride',
        description: 'denyoverride'
      })
      this.userdatapolicylist.opts = userdataPolicy
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
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
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
            if (this.userdataid !== null) {
              this.linkUserdataToTemplate(this.userdataid, json.registerisoresponse.iso[0].id, this.userdatapolicy)
            }
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
            if (this.userdataid !== null) {
              this.linkUserdataToTemplate(this.userdataid, json.postuploadisoresponse.iso[0].id)
            }
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    linkUserdataToTemplate (userdataid, templateid, userdatapolicy) {
      this.loading = true
      const params = {}
      params.userdataid = userdataid
      params.templateid = templateid
      if (userdatapolicy) {
        params.userdatapolicy = userdatapolicy
      }
      api('linkUserDataToTemplate', params).then(json => {
        this.closeAction()
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
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }
</style>
