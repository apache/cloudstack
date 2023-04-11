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
        layout="vertical">
        <div v-if="currentForm === 'Create'">
          <a-form-item :label="$t('label.url')" name="url" ref="url">
            <a-input
              v-focus="currentForm === 'Create'"
              v-model:value="form.url"
              :placeholder="apiParams.url.description" />
          </a-form-item>
        </div>
        <div v-if="currentForm === 'Upload'">
          <a-form-item :label="$t('label.templatefileupload')" name="file" ref="file">
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
        </div>
        <a-form-item :label="$t('label.name')" ref="name" name="name">
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="currentForm !== 'Create'"/>
        </a-form-item>
        <a-form-item :label="$t('label.displaytext')" ref="displaytext" name="displaytext">
          <a-input
            v-model:value="form.displaytext"
            :placeholder="apiParams.displaytext.description" />
        </a-form-item>
        <div v-if="currentForm === 'Create'">
          <a-form-item
            :label="$t('label.zone')"
            name="zoneids"
            ref="zoneids">
            <a-select
              v-model:value="form.zoneids"
              :loading="zones.loading"
              mode="multiple"
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.zoneids.description"
              @change="handlerSelectZone">
              <a-select-option v-for="opt in zones.opts" :key="opt.id" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div v-else>
          <a-form-item
            :label="$t('label.zoneid')"
            ref="zoneid"
            name="zoneid">
            <a-select
              v-model:value="form.zoneid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="handlerSelectZone"
              :placeholder="apiParams.zoneid.description"
              :loading="zones.loading">
              <a-select-option :value="zone.id" v-for="zone in filteredZones" :key="zone.id" :label="zone.name || zone.description">
                <span>
                  <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ zone.name || zone.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item ref="hypervisor" name="hypervisor" :label="$t('label.hypervisor')">
              <a-select
                v-model:value="form.hypervisor"
                :loading="hyperVisor.loading"
                :placeholder="apiParams.hypervisor.description"
                @change="handlerSelectHyperVisor"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="(opt, optIndex) in hyperVisor.opts" :key="optIndex" :label="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item ref="format" name="format" :label="$t('label.format')">
              <a-select
                v-model:value="form.format"
                :placeholder="apiParams.format.description"
                @change="val => { selectedFormat = val }"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="opt in format.opts" :key="opt.id" :label="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="allowed && hyperKVMShow && currentForm !== 'Upload'">
          <a-col :md="24" :lg="12">
            <a-form-item ref="directdownload" name="directdownload" :label="$t('label.directdownload')">
              <a-switch v-model:checked="form.directdownload" @change="handleChangeDirect" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="allowDirectDownload">
            <a-form-item ref="checksum" name="checksum" :label="$t('label.checksum')">
              <a-input
                v-model:value="form.checksum"
                :placeholder="apiParams.checksum.description" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="allowed && hyperXenServerShow">
          <a-form-item ref="xenserverToolsVersion61plus" name="xenserverToolsVersion61plus" v-if="hyperXenServerShow" :label="$t('label.xenservertoolsversion61plus')">
            <a-switch v-model:checked="form.xenserverToolsVersion61plus" />
          </a-form-item>
        </a-row>

        <a-form-item ref="deployasis" name="deployasis" :label="$t('label.deployasis')" v-if="selectedFormat === 'OVA'">
          <a-switch
            v-model:checked="form.deployasis"
            :checked="deployasis"
            @change="val => deployasis = val"/>
        </a-form-item>

        <a-row :gutter="12" v-if="hyperKVMShow || hyperVMWShow">
          <a-col :md="24" :lg="hyperKVMShow ? 24 : 12" v-if="hyperKVMShow || (hyperVMWShow && !deployasis)">
            <a-form-item ref="rootDiskControllerType" name="rootDiskControllerType" :label="$t('label.rootdiskcontrollertype')">
              <a-select
                v-model:value="form.rootDiskControllerType"
                :loading="rootDisk.loading"
                :placeholder="$t('label.rootdiskcontrollertype')"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="opt in rootDisk.opts" :key="opt.id" :label="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="hyperVMWShow && !deployasis">
            <a-form-item :label="$t('label.nicadaptertype')" name="nicadaptertype" ref="nicadaptertype">
              <a-select
                v-model:value="form.nicAdapterType"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                :placeholder="$t('label.nicadaptertype')">
                <a-select-option v-for="opt in nicAdapterType.opts" :key="opt.id" :label="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          :label="$t('label.keyboardtype')"
          v-if="hyperVMWShow && !deployasis"
          name="keyboardType"
          ref="keyboardType">
          <a-select
            v-model:value="form.keyboardType"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="$t('label.keyboard')">
            <a-select-option v-for="opt in keyboardType.opts" :key="opt.id" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="ostypeid"
          ref="ostypeid"
          :label="$t('label.ostypeid')"
          v-if="!hyperVMWShow || (hyperVMWShow && !deployasis)">
          <a-select
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-model:value="form.ostypeid"
            :loading="osTypes.loading"
            :placeholder="apiParams.ostypeid.description">
            <a-select-option v-for="opt in osTypes.opts" :key="opt.id" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
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
        <a-row :gutter="12">
          <a-col :md="24" :lg="24">
            <a-form-item ref="groupenabled" name="groupenabled">
              <a-checkbox-group
                v-model:value="form.groupenabled"
                style="width: 100%;"
              >
                <a-row>
                  <a-col :span="12">
                    <a-checkbox value="isextractable">
                      {{ $t('label.isextractable') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="passwordenabled">
                      {{ $t('label.passwordenabled') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="isdynamicallyscalable">
                      {{ $t('label.isdynamicallyscalable') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="requireshvm">
                      {{ $t('label.requireshvm') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12" v-if="isAdminRole">
                    <a-checkbox value="isfeatured">
                      {{ $t('label.isfeatured') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12" v-if="isAdminRole || $store.getters.features.userpublictemplateenabled">
                    <a-checkbox value="ispublic">
                      {{ $t('label.ispublic') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12" v-if="isAdminRole">
                    <a-checkbox value="isrouting">
                      {{ $t('label.isrouting') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </a-form-item>
          </a-col>
        </a-row>

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
  name: 'RegisterOrUploadTemplate',
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
      uploadPercentage: 0,
      uploading: false,
      fileList: [],
      zones: {},
      defaultZone: '',
      hyperVisor: {},
      rootDisk: {},
      nicAdapterType: {},
      keyboardType: {},
      format: {},
      osTypes: {},
      defaultOsType: '',
      userdata: {},
      userdataid: null,
      userdatapolicy: null,
      userdatapolicylist: {},
      defaultOsId: null,
      hyperKVMShow: false,
      hyperXenServerShow: false,
      hyperVMWShow: false,
      selectedFormat: '',
      deployasis: false,
      zoneError: '',
      loading: false,
      rootAdmin: 'Admin',
      allowed: false,
      allowDirectDownload: false,
      uploadParams: null,
      currentForm: ['plus-outlined', 'PlusOutlined'].includes(this.action.currentAction.icon) ? 'Create' : 'Upload'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('registerTemplate')
    this.linkUserDataParams = this.$getApiParams('linkUserDataToTemplate')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  computed: {
    isAdminRole () {
      return this.$store.getters.userInfo.roletype === 'Admin'
    },
    filteredZones () {
      let zoneList = this.zones.opts
      if (zoneList && zoneList.length > 0 && this.currentForm === 'Upload') {
        zoneList = zoneList.filter(zone => zone.type !== 'Edge')
      }
      return zoneList
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        deployasis: false,
        groupenabled: ['requireshvm']
      })
      this.rules = reactive({
        url: [{ required: true, message: this.$t('message.error.required.input') }],
        file: [{ required: true, message: this.$t('message.error.required.input') }],
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        zoneids: [
          { type: 'array', required: true, message: this.$t('message.error.select') },
          {
            validator: this.validZone
          }
        ],
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        hypervisor: [{ type: 'number', required: true, message: this.$t('message.error.select') }],
        format: [{ required: true, message: this.$t('message.error.select') }],
        ostypeid: [{ required: true, message: this.$t('message.error.select') }],
        groupenabled: [{ type: 'array' }]
      })
    },
    fetchData () {
      this.fetchZone()
      this.fetchOsTypes()
      this.fetchUserData()
      this.fetchUserdataPolicy()
      if (Object.prototype.hasOwnProperty.call(store.getters.apis, 'listConfigurations')) {
        if (this.allowed && this.hyperXenServerShow) {
          this.fetchXenServerProvider()
        }
      }
    },
    handleFormChange (e) {
      this.currentForm = e.target.value
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
          description: this.$t('message.success.upload.template.description')
        })
        this.$emit('refresh-data')
        this.closeAction()
      }).catch(e => {
        this.$notification.error({
          message: this.$t('message.upload.failed'),
          description: `${this.$t('message.upload.template.failed.description')} -  ${e}`,
          duration: 0
        })
      })
    },
    fetchZone () {
      const params = {}
      let listZones = []
      params.showicon = true
      this.allowed = false

      if (store.getters.userInfo.roletype === this.rootAdmin && this.currentForm === 'Create') {
        this.allowed = true
        listZones.push({
          id: this.$t('label.all.zone'),
          name: this.$t('label.all.zone')
        })
      }

      this.zones.loading = true
      this.zones.opts = []

      api('listZones', params).then(json => {
        const listZonesResponse = json.listzonesresponse.zone
        listZones = listZones.concat(listZonesResponse)
        this.zones.opts = listZones
      }).finally(() => {
        this.form.zoneid = (this.filteredZones && this.filteredZones[1]) ? this.filteredZones[1].id : ''
        if (!this.form.zoneid) {
          this.form.zoneid = (this.filteredZones && this.filteredZones[0] && this.filteredZones[0].id !== this.$t('label.all.zone')) ? this.filteredZones[0].id : ''
        }
        this.zones.loading = false
        if (this.form.zoneid) {
          this.fetchHyperVisor({ zoneid: this.form.zoneid })
        }
      })
    },
    fetchHyperVisor (params) {
      this.hyperVisor.loading = true
      let listhyperVisors = this.hyperVisor.opts || []

      api('listHypervisors', params).then(json => {
        const listResponse = json.listhypervisorsresponse.hypervisor || []
        if (listResponse) {
          listhyperVisors = listhyperVisors.concat(listResponse)
        }
        if (this.currentForm !== 'Upload') {
          listhyperVisors.push({
            name: 'Simulator'
          })
        }
        this.hyperVisor.opts = listhyperVisors
      }).finally(() => {
        this.hyperVisor.loading = false
      })
    },
    fetchOsTypes () {
      this.osTypes.opts = []
      this.osTypes.loading = true

      api('listOsTypes').then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.osTypes.opts = listOsTypes
        this.defaultOsType = this.osTypes.opts[1].description
        this.defaultOsId = this.osTypes.opts[1].id
      }).finally(() => {
        this.osTypes.loading = false
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
    fetchXenServerProvider () {
      const params = {}
      params.name = 'xenserver.pvdriver.version'

      this.form.xenserverToolsVersion61plus = true

      api('listConfigurations', params).then(json => {
        if (json.listconfigurationsresponse.configuration !== null && json.listconfigurationsresponse.configuration[0].value !== 'xenserver61') {
          this.form.xenserverToolsVersion61plus = false
        }
      })
    },
    fetchRootDisk (hyperVisor) {
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

      this.rootDisk.opts = controller
    },
    fetchNicAdapterType () {
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

      this.nicAdapterType.opts = nicAdapterType
    },
    fetchKeyboardType () {
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

      this.keyboardType.opts = keyboardType
    },
    fetchFormat (hyperVisor) {
      const format = []

      switch (hyperVisor) {
        case 'Hyperv':
          format.push({
            id: 'VHD',
            description: 'VHD'
          })
          format.push({
            id: 'VHDX',
            description: 'VHDX'
          })
          break
        case 'KVM':
          this.hyperKVMShow = true
          format.push({
            id: 'QCOW2',
            description: 'QCOW2'
          })
          format.push({
            id: 'RAW',
            description: 'RAW'
          })
          format.push({
            id: 'VHD',
            description: 'VHD'
          })
          format.push({
            id: 'VMDK',
            description: 'VMDK'
          })
          break
        case 'XenServer':
          this.hyperXenServerShow = true
          format.push({
            id: 'VHD',
            description: 'VHD'
          })
          break
        case 'Simulator':
          format.push({
            id: 'VHD',
            description: 'VHD'
          })
          break
        case 'VMware':
          this.hyperVMWShow = true
          format.push({
            id: 'OVA',
            description: 'OVA'
          })
          break
        case 'BareMetal':
          format.push({
            id: 'BareMetal',
            description: 'BareMetal'
          })
          break
        case 'Ovm':
          format.push({
            id: 'RAW',
            description: 'RAW'
          })
          break
        case 'LXC':
          format.push({
            id: 'TAR',
            description: 'TAR'
          })
          break
        default:
          break
      }
      this.format.opts = format
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

    handlerSelectZone (value) {
      if (!Array.isArray(value)) {
        value = [value]
      }
      this.hyperVisor.opts = []

      const allZoneExists = value.filter(zone => zone === this.$t('label.all.zone'))
      if (allZoneExists.length > 0 && value.length > 1) {
        return
      }
      const arrSelectReset = ['hypervisor', 'format', 'rootDiskControllerType', 'nicAdapterType', 'keyboardType']
      this.resetSelect(arrSelectReset)

      const params = {}

      if (value.includes(this.$t('label.all.zone'))) {
        this.fetchHyperVisor(params)
        return
      }

      for (let i = 0; i < value.length; i++) {
        const zoneSelected = this.zones.opts.filter(zone => zone.id === value[i])

        if (zoneSelected.length > 0) {
          params.zoneid = zoneSelected[0].id
          this.fetchHyperVisor(params)
        }
      }
    },
    handlerSelectHyperVisor (value) {
      const hyperVisor = this.hyperVisor.opts[value].name
      const arrSelectReset = ['format', 'rootDiskControllerType', 'nicAdapterType', 'keyboardType']

      this.hyperXenServerShow = false
      this.hyperVMWShow = false
      this.hyperKVMShow = false
      this.deployasis = false
      this.allowDirectDownload = false
      this.selectedFormat = null
      this.form.deployasis = false
      this.form.directdownload = false
      this.form.xenserverToolsVersion61plus = false

      this.resetSelect(arrSelectReset)
      this.fetchFormat(hyperVisor)
      this.fetchRootDisk(hyperVisor)
      this.fetchNicAdapterType()
      this.fetchKeyboardType()

      this.form.rootDiskControllerType = this.rootDisk.opts.length > 0 ? 'osdefault' : ''
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        if (this.currentForm === 'Create') {
          delete this.form.zoneid
        } else {
          delete this.form.zoneids
        }
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        let params = {}
        for (const key in values) {
          const input = values[key]

          if (input === undefined) {
            continue
          }
          if (key === 'file') {
            continue
          }

          if (key === 'zoneids') {
            if (input.length === 1 && input[0] === this.$t('label.all.zone')) {
              params.zoneids = '-1'
              continue
            }
            params[key] = input.join()
          } else if (key === 'hypervisor') {
            params[key] = this.hyperVisor.opts[input].name
          } else if (key === 'groupenabled') {
            for (const index in input) {
              const name = input[index]
              params[name] = true
            }
          } else {
            const formattedDetailData = {}
            switch (key) {
              case 'rootDiskControllerType':
                if (input) {
                  formattedDetailData['details[0].rootDiskController'] = input
                }
                break
              case 'nicAdapterType':
                formattedDetailData['details[0].nicAdapter'] = input
                break
              case 'keyboardType':
                formattedDetailData['details[0].keyboard'] = input
                break
              case 'xenserverToolsVersion61plus':
                formattedDetailData['details[0].hypervisortoolsversion'] = input
                break
            }

            if (Object.keys(formattedDetailData).length > 0) {
              params = Object.assign({}, params, formattedDetailData)
            } else {
              params[key] = input
            }
          }
        }
        if (!('requireshvm' in params)) { // handled as default true by API
          params.requireshvm = false
        }
        if (this.currentForm === 'Create') {
          this.loading = true
          api('registerTemplate', params).then(json => {
            if (this.userdataid !== null) {
              this.linkUserdataToTemplate(this.userdataid, json.registertemplateresponse.template[0].id, this.userdatapolicy)
            }
            this.$notification.success({
              message: this.$t('label.register.template'),
              description: `${this.$t('message.success.register.template')} ${params.name}`
            })
            this.$emit('refresh-data')
            this.closeAction()
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
          })
        } else {
          this.loading = true
          if (this.fileList.length > 1) {
            this.$notification.error({
              message: this.$t('message.error.upload.template'),
              description: this.$t('message.error.upload.template.description'),
              duration: 0
            })
          }
          api('getUploadParamsForTemplate', params).then(json => {
            this.uploadParams = (json.postuploadtemplateresponse && json.postuploadtemplateresponse.getuploadparams) ? json.postuploadtemplateresponse.getuploadparams : ''
            this.handleUpload()
            if (this.userdataid !== null) {
              this.linkUserdataToTemplate(this.userdataid, json.postuploadtemplateresponse.template[0].id)
            }
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
          })
        }
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleChangeDirect (checked) {
      this.allowDirectDownload = checked
    },
    async validZone (rule, value) {
      if (!value || value.length === 0) {
        return Promise.resolve()
      }
      const allZoneExists = value.filter(zone => zone === this.$t('label.all.zone'))

      if (allZoneExists.length > 0 && value.length > 1) {
        return Promise.reject(this.$t('message.error.zone.combined'))
      }

      return Promise.resolve()
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
    },
    resetSelect (arrSelectReset) {
      arrSelectReset.forEach(name => {
        this.form[name] = undefined
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
