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
        <div v-if="currentForm === 'Create'">
          <a-row :gutter="12">
            <a-form-item :label="$t('url')">
              <a-input
                v-decorator="['url', {
                  rules: [{ required: true, message: 'Please enter input' }]
                }]"
                :placeholder="apiParams.url.description" />
            </a-form-item>
          </a-row>
        </div>
        <div v-if="currentForm === 'Upload'">
          <a-form-item :label="$t('templateFileUpload')">
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
        </div>
        <a-row :gutter="12">
          <a-form-item :label="$t('name')">
            <a-input
              v-decorator="['name', {
                rules: [{ required: true, message: 'Please upload a template ' }]
              }]"
              :placeholder="apiParams.name.description" />
          </a-form-item>
        </a-row>
        <a-row :gutter="12">
          <a-form-item :label="$t('displaytext')">
            <a-input
              v-decorator="['displaytext', {
                rules: [{ required: true, message: 'Please enter input' }]
              }]"
              :placeholder="apiParams.displaytext.description" />
          </a-form-item>
        </a-row>
        <div v-if="currentForm === 'Create'">
          <a-row :gutter="12">
            <a-col :md="24" :lg="24">
              <a-form-item
                :label="$t('zoneids')"
                :validate-status="zoneError"
                :help="zoneErrorMessage">
                <a-select
                  v-decorator="['zoneids', {
                    rules: [
                      {
                        required: false,
                        message: 'Please select option',
                        type: 'array'
                      }
                    ]
                  }]"
                  :loading="zones.loading"
                  mode="multiple"
                  :placeholder="apiParams.zoneids.description"
                  @change="handlerSelectZone">
                  <a-select-option v-for="opt in zones.opts" :key="opt.name || opt.description">
                    {{ opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
        <div v-else>
          <a-row :gutter="12">
            <a-col :md="24" :lg="24">
              <a-form-item
                :label="$t('zoneid')"
                :validate-status="zoneError"
                :help="zoneErrorMessage">
                <a-select
                  v-decorator="['zoneid', {
                    initialValue: this.zoneSelected
                  }]"
                  @change="handlerSelectZone"
                  :loading="zones.loading">
                  <a-select-option :value="zone.id" v-for="zone in zones.opts" :key="zone.id">
                    <div v-if="zone.name !== $t('label.all.zone')">
                      {{ zone.name || zone.description }}
                    </div>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item :label="$t('hypervisor')">
              <a-select
                v-decorator="['hypervisor', {
                  rules: [
                    {
                      required: true,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :loading="hyperVisor.loading"
                :placeholder="apiParams.hypervisor.description"
                @change="handlerSelectHyperVisor">
                <a-select-option v-for="(opt, optIndex) in hyperVisor.opts" :key="optIndex">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item :label="$t('format')">
              <a-select
                v-decorator="['format', {
                  rules: [
                    {
                      required: true,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :placeholder="apiParams.format.description">
                <a-select-option v-for="opt in format.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="allowed && hyperKVMShow && currentForm !== 'Upload'">
          <a-col :md="24" :lg="12">
            <a-form-item :label="$t('directdownload')">
              <a-switch v-decorator="['directdownload']" @change="handleChangeDirect" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="allowDirectDownload">
            <a-form-item :label="$t('checksum')">
              <a-input
                v-decorator="['checksum', {
                  rules: [{ required: false, message: 'Please enter input' }]
                }]"
                :placeholder="apiParams.checksum.description" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="allowed && hyperXenServerShow">
          <a-form-item v-if="hyperXenServerShow" :label="$t('xenserverToolsVersion61plus')">
            <a-switch
              v-decorator="['xenserverToolsVersion61plus',{
                initialValue: xenServerProvider
              }]"
              :default-checked="xenServerProvider" />
          </a-form-item>
        </a-row>
        <a-row :gutter="12" v-if="hyperKVMShow || hyperVMWShow">
          <a-col :md="24" :lg="24" v-if="hyperKVMShow">
            <a-form-item :label="$t('rootDiskControllerType')">
              <a-select
                v-decorator="['rootDiskControllerType', {
                  rules: [
                    {
                      required: true,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :loading="rootDisk.loading"
                :placeholder="$t('rootdiskcontroller')">
                <a-select-option v-for="opt in rootDisk.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="hyperVMWShow">
            <a-form-item :label="$t('rootDiskControllerType')">
              <a-select
                v-decorator="['rootDiskControllerType', {
                  rules: [
                    {
                      required: false,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :loading="rootDisk.loading"
                :placeholder="$t('rootdiskcontroller')">
                <a-select-option v-for="opt in rootDisk.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="hyperVMWShow">
            <a-form-item :label="$t('nicAdapterType')">
              <a-select
                v-decorator="['nicAdapterType', {
                  rules: [
                    {
                      required: false,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :placeholder="$t('nicadaptertype')">
                <a-select-option v-for="opt in nicAdapterType.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24">
            <a-form-item v-if="hyperVMWShow" :label="$t('keyboardType')">
              <a-select
                v-decorator="['keyboardType', {
                  rules: [
                    {
                      required: false,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :placeholder="$t('keyboard')">
                <a-select-option v-for="opt in keyboardType.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="24" :lg="24">
            <a-form-item :label="$t('ostypeid')">
              <a-select
                showSearch
                v-decorator="['ostypeid', {
                  initialValue: defaultOsType,
                  rules: [
                    {
                      required: true,
                      message: 'Please select option'
                    }
                  ]
                }]"
                :loading="osTypes.loading"
                :placeholder="apiParams.ostypeid.description">
                <a-select-option v-for="opt in osTypes.opts" :key="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="24" :lg="24">
            <a-form-item>
              <a-checkbox-group
                v-decorator="['groupenabled', { initialValue: ['requireshvm'] }]"
                style="width: 100%;"
              >
                <a-row>
                  <a-col :span="12">
                    <a-checkbox value="isextractable">
                      {{ $t('isextractable') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="passwordenabled">
                      {{ $t('passwordenabled') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
                <a-row>
                  <a-col :span="12">
                    <a-checkbox value="isdynamicallyscalable">
                      {{ $t('isdynamicallyscalable') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="sshkeyenabled">
                      {{ $t('sshkeyenabled') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
                <a-row>
                  <a-col :span="12">
                    <a-checkbox value="isrouting">
                      {{ $t('isrouting') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="ispublic">
                      {{ $t('ispublic') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
                <a-row>
                  <a-col :span="12">
                    <a-checkbox value="requireshvm">
                      {{ $t('requireshvm') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="isfeatured">
                      {{ $t('isfeatured') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </a-form-item>
          </a-col>
        </a-row>

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
  name: 'RegisterOrUploadTemplate',
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
      uploadPercentage: 0,
      uploading: false,
      fileList: [],
      zones: {},
      defaultZone: '',
      zoneSelected: '',
      hyperVisor: {},
      rootDisk: {},
      nicAdapterType: {},
      keyboardType: {},
      format: {},
      osTypes: {},
      defaultOsType: '',
      xenServerProvider: false,
      hyperKVMShow: false,
      hyperXenServerShow: false,
      hyperVMWShow: false,
      zoneError: '',
      zoneErrorMessage: '',
      loading: false,
      rootAdmin: 'Admin',
      allowed: false,
      allowDirectDownload: false,
      uploadParams: null,
      currentForm: this.action.currentAction.api === 'registerTemplate' ? 'Create' : 'Upload'
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.registerTemplate || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.$set(this.zones, 'loading', false)
    this.$set(this.zones, 'opts', [])
    this.$set(this.hyperVisor, 'loading', false)
    this.$set(this.hyperVisor, 'opts', [])
    this.$set(this.rootDisk, 'loading', false)
    this.$set(this.rootDisk, 'opts', [])
    this.$set(this.nicAdapterType, 'loading', false)
    this.$set(this.nicAdapterType, 'opts', [])
    this.$set(this.keyboardType, 'loading', false)
    this.$set(this.keyboardType, 'opts', [])
    this.$set(this.format, 'loading', false)
    this.$set(this.format, 'opts', [])
    this.$set(this.osTypes, 'loading', false)
    this.$set(this.osTypes, 'opts', [])
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchZone()
      this.fetchOsTypes()
      if (Object.prototype.hasOwnProperty.call(store.getters.apis, 'listConfigurations')) {
        this.fetchXenServerProvider()
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
    },
    beforeUpload (file) {
      this.fileList = [file]
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
          description: 'This template file has been uploaded. Please check its status at Templates menu'
        })
        this.closeAction()
      }).catch(e => {
        this.$notification.error({
          message: 'Upload Failed',
          description: `Failed to upload Template -  ${e}`,
          duration: 0
        })
        this.closeAction()
      })
    },
    fetchZone () {
      const params = {}
      let listZones = []
      params.listAll = true

      this.allowed = false

      if (store.getters.userInfo.roletype === this.rootAdmin) {
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

        this.$set(this.zones, 'opts', listZones)
      }).finally(() => {
        this.zoneSelected = (this.zones.opts && this.zones.opts[1]) ? this.zones.opts[1].id : ''
        this.zones.loading = false
        this.fetchHyperVisor({ zoneid: this.zoneSelected })
      })
    },
    fetchHyperVisor (params) {
      this.hyperVisor.loading = true
      let listhyperVisors = this.hyperVisor.opts

      api('listHypervisors', params).then(json => {
        const listResponse = json.listhypervisorsresponse.hypervisor
        if (listResponse) {
          listhyperVisors = listhyperVisors.concat(listResponse)
        }
        if (this.currentForm !== 'Upload') {
          listhyperVisors.push({
            name: 'Any'
          })
        }
        this.$set(this.hyperVisor, 'opts', listhyperVisors)
      }).finally(() => {
        this.hyperVisor.loading = false
      })
    },
    fetchOsTypes () {
      const params = {}
      params.listAll = true

      this.osTypes.opts = []
      this.osTypes.loading = true

      api('listOsTypes', params).then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.$set(this.osTypes, 'opts', listOsTypes)
        this.defaultOsType = this.osTypes.opts[1].description
      }).finally(() => {
        this.osTypes.loading = false
      })
    },
    fetchXenServerProvider () {
      const params = {}
      params.name = 'xenserver.pvdriver.version'

      this.xenServerProvider = true

      api('listConfigurations', params).then(json => {
        if (json.listconfigurationsresponse.configuration !== null && json.listconfigurationsresponse.configuration[0].value !== 'xenserver61') {
          this.xenServerProvider = false
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

      this.$set(this.rootDisk, 'opts', controller)
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

      this.$set(this.nicAdapterType, 'opts', nicAdapterType)
    },
    fetchKeyboardType () {
      const keyboardType = []
      keyboardType.push({
        id: '',
        description: ''
      })
      keyboardType.push({
        id: 'us',
        description: 'label.standard.us.keyboard'
      })
      keyboardType.push({
        id: 'uk',
        description: 'label.uk.keyboard'
      })
      keyboardType.push({
        id: 'fr',
        description: 'label.french.azerty.keyboard'
      })
      keyboardType.push({
        id: 'jp',
        description: 'label.japanese.keyboard'
      })
      keyboardType.push({
        id: 'sc',
        description: 'label.simplified.chinese.keyboard'
      })

      this.$set(this.keyboardType, 'opts', keyboardType)
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
      this.$set(this.format, 'opts', format)
    },
    handlerSelectZone (value) {
      if (!Array.isArray(value)) {
        value = [value]
      }
      this.validZone(value)
      this.hyperVisor.opts = []

      if (this.zoneError !== '') {
        return
      }

      this.resetSelect()

      const params = {}
      const allZoneExists = value.filter(zone => zone === this.$t('label.all.zone'))

      if (allZoneExists.length > 0) {
        params.listAll = true
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

      this.hyperXenServerShow = false
      this.hyperVMWShow = false
      this.hyperKVMShow = false
      this.allowDirectDownload = false

      this.resetSelect()
      this.fetchFormat(hyperVisor)
      this.fetchRootDisk(hyperVisor)
      this.fetchNicAdapterType()
      this.fetchKeyboardType()
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err || this.zoneError !== '') {
          return
        }
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
            const zonesSelected = []
            for (const index in input) {
              const name = input[index]
              const zone = this.zones.opts.filter(zone => zone.name === name)
              if (zone && zone[0]) {
                zonesSelected.push(zone[0].id)
              }
            }
            params[key] = zonesSelected.join(',')
          } else if (key === 'zoneid') {
            params[key] = values[key]
          } else if (key === 'ostypeid') {
            const osTypeSelected = this.osTypes.opts.filter(item => item.description === input)
            if (osTypeSelected && osTypeSelected[0]) {
              params[key] = osTypeSelected[0].id
            }
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
                formattedDetailData['details[0].rootDiskController'] = input
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
        if (this.currentForm === 'Create') {
          this.loading = true
          api('registerTemplate', params).then(json => {
            this.$emit('refresh-data')
            this.$notification.success({
              message: 'Register Template',
              description: 'Successfully registered template ' + params.name
            })
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
            this.closeAction()
          })
        } else {
          this.loading = true
          if (this.fileList.length > 1) {
            this.$notification.error({
              message: 'Template Upload Failed',
              description: 'Only one template can be uploaded at a time',
              duration: 0
            })
          }
          api('getUploadParamsForTemplate', params).then(json => {
            this.uploadParams = (json.postuploadtemplateresponse && json.postuploadtemplateresponse.getuploadparams) ? json.postuploadtemplateresponse.getuploadparams : ''
            this.handleUpload()
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
          })
        }
      })
    },
    handleChangeDirect (checked) {
      this.allowDirectDownload = checked
    },
    validZone (zones) {
      const allZoneExists = zones.filter(zone => zone === this.$t('label.all.zone'))

      this.zoneError = ''
      this.zoneErrorMessage = ''

      if (allZoneExists.length > 0 && zones.length > 1) {
        this.zoneError = 'error'
        this.zoneErrorMessage = this.$t('label.error.zone.combined')
      }
    },
    closeAction () {
      this.$emit('close-action')
    },
    resetSelect () {
      this.form.setFieldsValue({
        hypervisor: undefined,
        format: undefined,
        rootDiskControllerType: undefined,
        nicAdapterType: undefined,
        keyboardType: undefined
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

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
