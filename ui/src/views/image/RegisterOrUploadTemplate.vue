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
        <div v-if="currentForm === 'Create'">
          <a-row :gutter="12">
            <a-form-item :label="$t('label.url')">
              <a-input
                :autoFocus="currentForm === 'Create'"
                v-decorator="['url', {
                  rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
                }]"
                :placeholder="apiParams.url.description" />
            </a-form-item>
          </a-row>
        </div>
        <div v-if="currentForm === 'Upload'">
          <a-form-item :label="$t('label.templatefileupload')">
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
        </div>
        <a-row :gutter="12">
          <a-form-item :label="$t('label.name')">
            <a-input
              v-decorator="['name', {
                rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
              }]"
              :placeholder="apiParams.name.description"
              :autoFocus="currentForm !== 'Create'"/>
          </a-form-item>
        </a-row>
        <a-row :gutter="12">
          <a-form-item :label="$t('label.displaytext')">
            <a-input
              v-decorator="['displaytext', {
                rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
              }]"
              :placeholder="apiParams.displaytext.description" />
          </a-form-item>
        </a-row>
        <div v-if="currentForm === 'Create'">
          <a-row :gutter="12">
            <a-col :md="24" :lg="24">
              <a-form-item
                :label="$t('label.zone')"
                :validate-status="zoneError"
                :help="zoneErrorMessage">
                <a-select
                  v-decorator="['zoneids', {
                    rules: [
                      {
                        required: true,
                        message: `${this.$t('message.error.select')}`,
                        type: 'array'
                      }
                    ]
                  }]"
                  :loading="zones.loading"
                  mode="multiple"
                  optionFilterProp="children"
                  :filterOption="(input, option) => {
                    return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  :placeholder="apiParams.zoneids.description"
                  @change="handlerSelectZone">
                  <a-select-option v-for="opt in zones.opts" :key="opt.id">
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
                :label="$t('label.zoneid')"
                :validate-status="zoneError"
                :help="zoneErrorMessage">
                <a-select
                  v-decorator="['zoneid', {
                    initialValue: this.zoneSelected,
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
                  @change="handlerSelectZone"
                  :placeholder="apiParams.zoneid.description"
                  :loading="zones.loading">
                  <a-select-option :value="zone.id" v-for="zone in zones.opts" :key="zone.id">
                    {{ zone.name || zone.description }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item :label="$t('label.hypervisor')">
              <a-select
                v-decorator="['hypervisor', {
                  rules: [
                    {
                      required: true,
                      message: `${this.$t('message.error.select')}`
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
            <a-form-item :label="$t('label.format')">
              <a-select
                v-decorator="['format', {
                  rules: [
                    {
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :placeholder="apiParams.format.description"
                @change="val => { selectedFormat = val }">
                <a-select-option v-for="opt in format.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="allowed && hyperKVMShow && currentForm !== 'Upload'">
          <a-col :md="24" :lg="12">
            <a-form-item :label="$t('label.directdownload')">
              <a-switch v-decorator="['directdownload']" @change="handleChangeDirect" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12" v-if="allowDirectDownload">
            <a-form-item :label="$t('label.checksum')">
              <a-input
                v-decorator="['checksum', {
                  rules: [{ required: false, message: `${this.$t('message.error.required.input')}` }]
                }]"
                :placeholder="apiParams.checksum.description" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="allowed && hyperXenServerShow">
          <a-form-item v-if="hyperXenServerShow" :label="$t('label.xenservertoolsversion61plus')">
            <a-switch
              v-decorator="['xenserverToolsVersion61plus',{
                initialValue: xenServerProvider
              }]"
              :default-checked="xenServerProvider" />
          </a-form-item>
        </a-row>

        <a-form-item :label="$t('label.deployasis')" v-if="selectedFormat === 'OVA'">
          <a-switch
            v-decorator="['deployasis', {
              initialValue: false,
            }]"
            :checked="deployasis"
            @change="val => deployasis = val"/>
        </a-form-item>

        <a-row :gutter="12" v-if="hyperKVMShow || hyperVMWShow">
          <a-col :md="24" :lg="24" v-if="hyperKVMShow || (hyperVMWShow && !deployasis)">
            <a-form-item :label="$t('label.rootdiskcontrollertype')">
              <a-select
                v-decorator="['rootDiskControllerType', {
                  initialValue: rootDisk.opts.length > 0 ? 'osdefault' : '',
                  rules: [
                    {
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :loading="rootDisk.loading"
                :placeholder="$t('label.rootdiskcontrollertype')">
                <a-select-option v-for="opt in rootDisk.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24">
            <a-form-item v-if="hyperVMWShow && !deployasis" :label="$t('label.keyboardtype')">
              <a-select
                v-decorator="['keyboardType', {
                  rules: [
                    {
                      required: false,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :placeholder="$t('label.keyboard')">
                <a-select-option v-for="opt in keyboardType.opts" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="!hyperVMWShow || (hyperVMWShow && !deployasis)">
          <a-col :md="24" :lg="24">
            <a-form-item :label="$t('label.ostypeid')">
              <a-select
                showSearch
                optionFilterProp="children"
                :filterOption="(input, option) => {
                  return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                v-decorator="['ostypeid', {
                  initialValue: defaultOsId,
                  rules: [
                    {
                      required: true,
                      message: `${this.$t('message.error.select')}`
                    }
                  ]
                }]"
                :loading="osTypes.loading"
                :placeholder="apiParams.ostypeid.description">
                <a-select-option v-for="opt in osTypes.opts" :key="opt.id">
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
                      {{ $t('label.isextractable') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox value="passwordenabled">
                      {{ $t('label.passwordenabled') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
                <a-row>
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
                </a-row>
                <a-row>
                  <a-col :span="12">
                    <a-checkbox value="isfeatured">
                      {{ $t('label.isfeatured') }}
                    </a-checkbox>
                  </a-col>
                  <a-col :span="12">
                    <a-checkbox
                      value="ispublic"
                      v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.userpublictemplateenabled" >
                      {{ $t('label.ispublic') }}
                    </a-checkbox>
                  </a-col>
                </a-row>
                <a-row>
                  <a-col :span="12" v-if="$store.getters.userInfo.roletype === 'Admin'">
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
      defaultOsId: null,
      xenServerProvider: false,
      hyperKVMShow: false,
      hyperXenServerShow: false,
      hyperVMWShow: false,
      selectedFormat: '',
      deployasis: false,
      zoneError: '',
      zoneErrorMessage: '',
      loading: false,
      rootAdmin: 'Admin',
      allowed: false,
      allowDirectDownload: false,
      uploadParams: null,
      currentForm: this.action.currentAction.icon === 'plus' ? 'Create' : 'Upload'
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
    this.fetchData()
  },
  computed: {
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
      params.listAll = true

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
        this.defaultOsId = this.osTypes.opts[1].id
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

      if (value.includes(this.$t('label.all.zone'))) {
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
      this.deployasis = false
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
        if (!('requireshvm' in params)) { // handled as default true by API
          params.requireshvm = false
        }
        if (this.currentForm === 'Create') {
          this.loading = true
          api('registerTemplate', params).then(json => {
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
        this.zoneErrorMessage = this.$t('message.error.zone.combined')
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
