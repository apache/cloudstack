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
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: 'Please enter name' }]
            }]"
            :placeholder="this.$t('Name')"/>
        </a-form-item>
        <a-form-item :label="$t('displaytext')">
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: 'Please enter description' }]
            }]"
            :placeholder="this.$t('displaytext  ')"/>
        </a-form-item>
        <a-form-item :label="$t('systemvmtype')" v-if="this.isSystem">
          <a-select
            v-decorator="['systemvmtype', {
              initialValue: 'domainrouter'
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="this.$t('systemvmtype')">
            <a-select-option key="domainrouter">Domain Router</a-select-option>
            <a-select-option key="consoleproxy">Console Proxy</a-select-option>
            <a-select-option key="secondarystoragevm">Secondary Storage VM</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('storagetype')">
          <a-radio-group
            v-decorator="['storagetype', {
              initialValue: this.storageType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleStorageTypeChange(selected.target.value) }">
            <a-radio-button value="shared">
              {{ $t('shared') }}
            </a-radio-button>
            <a-radio-button value="local">
              {{ $t('local') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('provisioningtype')">
          <a-radio-group
            v-decorator="['provisioningtype', {
              initialValue: this.provisioningType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleProvisioningTypeChange(selected.target.value) }">
            <a-radio-button value="thin">
              {{ $t('thin') }}
            </a-radio-button>
            <a-radio-button value="sparse">
              {{ $t('sparse') }}
            </a-radio-button>
            <a-radio-button value="fat">
              {{ $t('fat') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('offeringtype')" v-show="!this.isSystem">
          <a-radio-group
            v-decorator="['offeringtype', {
              initialValue: this.offeringType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleComputeOfferingTypeChange(selected.target.value) }">
            <a-radio-button value="fixed">
              {{ $t('fixed') }}
            </a-radio-button>
            <a-radio-button value="customconstrained">
              {{ $t('customconstrained') }}
            </a-radio-button>
            <a-radio-button value="customunconstrained">
              {{ $t('customunconstrained') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('cpunumber')" v-if="this.offeringType === 'fixed'">
          <a-input
            v-decorator="['cpunumber', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('cpunumber')"/>
        </a-form-item>
        <a-form-item :label="$t('cpuspeed')" v-if="this.offeringType !== 'customunconstrained'">
          <a-input
            v-decorator="['cpuspeed', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('cpuspeed')"/>
        </a-form-item>
        <a-form-item :label="$t('mincpunumber')" v-if="this.offeringType === 'customconstrained'">
          <a-input
            v-decorator="['mincpunumber', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('mincpunumber')"/>
        </a-form-item>
        <a-form-item :label="$t('maxcpunumber')" v-if="this.offeringType === 'customconstrained'">
          <a-input
            v-decorator="['maxcpunumber', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('maxcpunumber')"/>
        </a-form-item>
        <a-form-item :label="$t('memory')" v-if="this.offeringType === 'fixed'">
          <a-input
            v-decorator="['memory', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('memory')"/>
        </a-form-item>
        <a-form-item :label="$t('minmemory')" v-if="this.offeringType === 'customconstrained'">
          <a-input
            v-decorator="['minmemory', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('minmemory')"/>
        </a-form-item>
        <a-form-item :label="$t('maxmemory')" v-if="this.offeringType === 'customconstrained'">
          <a-input
            v-decorator="['maxmemory', {
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('maxmemory')"/>
        </a-form-item>
        <a-form-item :label="$t('networkrate')">
          <a-input
            v-decorator="['networkrate', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback('Please enter a valid number')
                    }
                    callback()
                  }
                }
              ]
            }]"
            :placeholder="this.$t('networkrate')"/>
        </a-form-item>
        <a-form-item :label="$t('qostype')">
          <a-radio-group
            v-decorator="['qostype', {
              initialValue: this.qosType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleQosTypeChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('none') }}
            </a-radio-button>
            <a-radio-button value="hypervisor">
              {{ $t('hypervisor') }}
            </a-radio-button>
            <a-radio-button value="storage">
              {{ $t('storage') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('diskbytesreadrate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskbytesreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('diskbytesreadrate')"/>
        </a-form-item>
        <a-form-item :label="$t('diskbyteswriterate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskbyteswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('diskbyteswriterate')"/>
        </a-form-item>
        <a-form-item :label="$t('diskiopsreadrate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskiopsreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('diskiopsreadrate')"/>
        </a-form-item>
        <a-form-item :label="$t('diskiopswriterate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskiopswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('diskiopswriterate')"/>
        </a-form-item>
        <a-form-item :label="$t('iscustomizeddiskiops')" v-if="!this.isSystem && this.qosType === 'storage'">
          <a-switch v-decorator="['iscustomizeddiskiops', {initialValue: this.isCustomizedDiskIops}]" :defaultChecked="this.isCustomizedDiskIops" @change="val => { this.isCustomizedDiskIops = val }" />
        </a-form-item>
        <a-form-item :label="$t('diskiopsmin')" v-if="this.qosType === 'storage' && !this.isCustomizedDiskIops">
          <a-input
            v-decorator="['diskiopsmin', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('diskiopsmin')"/>
        </a-form-item>
        <a-form-item :label="$t('diskiopsmax')" v-if="this.qosType === 'storage' && !this.isCustomizedDiskIops">
          <a-input
            v-decorator="['diskiopsmax', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('diskiopsmax')"/>
        </a-form-item>
        <a-form-item :label="$t('hypervisorsnapshotreserve')" v-if="!this.isSystem && this.qosType === 'storage'">
          <a-input
            v-decorator="['hypervisorsnapshotreserve', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('hypervisorsnapshotreserve')"/>
        </a-form-item>
        <a-form-item :label="$t('offerha')">
          <a-switch v-decorator="['offerha', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('hosttags')" v-if="this.isAdmin()">
          <a-input
            v-decorator="['hosttags', {}]"
            :placeholder="this.$t('hosttags')"/>
        </a-form-item>
        <a-form-item :label="$t('storagetags')" v-if="this.isAdmin()">
          <a-select
            mode="tags"
            v-decorator="['storagetags', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="storageTagLoading"
            :placeholder="this.$t('tags')"
            v-if="this.isAdmin()">
            <a-select-option v-for="(opt) in this.storageTags" :key="opt.name">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('limitcpuuse')">
          <a-switch v-decorator="['limitcpuuse', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('isvolatile')" v-if="!this.isSystem">
          <a-switch v-decorator="['isvolatile', {initialValue: false}]" />
        </a-form-item>
        <a-form-item :label="$t('deploymentplanner')" v-if="!this.isSystem && this.isAdmin()">
          <a-select
            v-decorator="['deploymentplanner', {
              initialValue: this.deploymentPlanners.length > 0 ? this.deploymentPlanners[0].name : ''
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="deploymentPlannerLoading"
            :placeholder="this.$t('deploymentplanner')"
            @change="val => { this.handleDeploymentPlannerChange(val) }">
            <a-select-option v-for="(opt) in this.deploymentPlanners" :key="opt.name">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('plannermode')" v-if="this.plannerModeVisible">
          <a-radio-group
            v-decorator="['plannermode', {
              initialValue: this.plannerMode
            }]"
            buttonStyle="solid"
            @change="selected => { this.handlePlannerModeChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('none') }}
            </a-radio-button>
            <a-radio-button value="strict">
              {{ $t('strict') }}
            </a-radio-button>
            <a-radio-button value="preferred">
              {{ $t('preferred') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('gpu')" v-if="!this.isSystem">
          <a-radio-group
            v-decorator="['pcidevice', {
              initialValue: this.selectedGpu
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleGpuChange(selected.target.value) }">
            <a-radio-button v-for="(opt, optIndex) in this.gpuTypes" :key="optIndex" :value="opt.value">
              {{ opt.title }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('vgputype')" v-if="this.vGpuVisible">
          <a-select
            v-decorator="['vgputype', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="this.$t('vgputype')">
            <a-select-option v-for="(opt, optIndex) in this.vGpuTypes" :key="optIndex">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: this.isPublic}]" :checked="this.isPublic" @change="val => { this.isPublic = val }" />
        </a-form-item>
        <a-form-item :label="$t('domain')" v-if="!this.isPublic">
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
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
            :loading="domainLoading"
            :placeholder="this.$t('domainid')">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('zoneid')" v-if="!this.isSystem">
          <a-select
            id="zone-selection"
            mode="multiple"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback('All Zones cannot be combined with any other zone')
                    }
                    callback()
                  }
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="this.$t('zoneid')">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'AddServiceOffering',
  props: {
  },
  components: {
  },
  data () {
    return {
      isSystem: false,
      storageType: 'shared',
      provisioningType: 'thin',
      offeringType: 'fixed',
      qosType: '',
      isCustomizedDiskIops: false,
      isPublic: true,
      selectedDomains: [],
      domains: [],
      domainLoading: false,
      selectedZones: [],
      zones: [],
      zoneLoading: false,
      selectedDeployementPlanner: null,
      storageTags: [],
      storageTagLoading: false,
      deploymentPlanners: [],
      deploymentPlannerLoading: false,
      plannerModeVisible: false,
      plannerMode: '',
      selectedGpu: '',
      gpuTypes: [
        {
          value: '',
          title: 'None',
          vgpu: []
        },
        {
          value: 'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs',
          title: 'NVIDIA GRID K1',
          vgpu: ['', 'passthrough', 'GRID K100', 'GRID K120Q', 'GRID K140Q', 'GRID K160Q', 'GRID K180Q']
        },
        {
          value: 'Group of NVIDIA Corporation GK104GL [GRID K2] GPUs',
          title: 'NVIDIA GRID K2',
          vgpu: ['', 'passthrough', 'GRID K200', 'GRID K220Q', 'GRID K240Q', 'GRID K260Q', 'GRID K280Q']
        }
      ],
      vGpuVisible: false,
      vGpuTypes: [],
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
  },
  mounted () {
    if (this.$route.meta.name === 'systemoffering') {
      this.isSystem = true
    }
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
      if (this.isAdmin()) {
        this.fetchStorageTagData()
        this.fetchDeploymentPlannerData()
      }
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.details = 'min'
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchZoneData () {
      const params = {}
      params.listAll = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    fetchStorageTagData () {
      const params = {}
      params.listAll = true
      this.storageTagLoading = true
      api('listStorageTags', params).then(json => {
        const tags = json.liststoragetagsresponse.storagetag
        if (this.arrayHasItems(tags)) {
          for (var i in tags) {
            var tag = {}
            tag.id = tags[i].name
            tag.name = tags[i].name
            this.storageTags.push(tag)
          }
        }
      }).finally(() => {
        this.storageTagLoading = false
      })
    },
    fetchDeploymentPlannerData () {
      const params = {}
      params.listAll = true
      this.deploymentPlannerLoading = true
      api('listDeploymentPlanners', params).then(json => {
        const planners = json.listdeploymentplannersresponse.deploymentPlanner
        this.deploymentPlanners = this.deploymentPlanners.concat(planners)
        this.deploymentPlanners.unshift({ name: '' })
      }).finally(() => {
        this.deploymentPlannerLoading = false
      })
    },
    handleStorageTypeChange (val) {
      this.storageType = val
    },
    handleProvisioningTypeChange (val) {
      this.provisioningType = val
    },
    handleComputeOfferingTypeChange (val) {
      this.offeringType = val
    },
    handleQosTypeChange (val) {
      this.qosType = val
    },
    handleDeploymentPlannerChange (planner) {
      this.selectedDeployementPlanner = planner
      this.plannerModeVisible = false
      if (this.selectedDeployementPlanner === 'ImplicitDedicationPlanner') {
        this.plannerModeVisible = this.isAdmin()
      }
    },
    handlePlannerModeChange (val) {
      this.plannerMode = val
    },
    handleGpuChange (val) {
      this.vGpuTypes = []
      for (var i in this.gpuTypes) {
        if (this.gpuTypes[i].value === val) {
          this.vGpuTypes = this.gpuTypes[i].vgpu
          break
        }
      }
      this.vGpuVisible = true
      if (!this.arrayHasItems(this.vGpuTypes)) {
        this.vGpuVisible = false
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        var params = {
          issystem: this.isSystem,
          name: values.name,
          displaytext: values.displaytext,
          storagetype: values.storageType,
          provisioningtype: values.provisioningtype,
          customized: values.offeringtype !== 'fixed',
          offerha: values.offerha === true,
          limitcpuuse: values.limitcpuuse === true
        }

        // custom fields (begin)
        if (values.offeringtype === 'fixed') {
          params.cpunumber = values.cpunumber
          params.cpuspeed = values.cpuspeed
          params.memory = values.memory
        } else {
          if (values.cpuspeed != null &&
              values.mincpunumber != null &&
              values.maxcpunumber != null &&
              values.minmemory != null &&
              values.maxmemory != null) {
            params.cpuspeed = values.cpuspeed
            params.mincpunumber = values.mincpunumber
            params.maxcpunumber = values.maxcpunumber
            params.minmemory = values.minmemory
            params.maxmemory = values.maxmemory
          }
        }
        // custom fields (end)

        if (values.networkRate != null && values.networkRate.length > 0) {
          params.networkrate = values.networkrate
        }
        if (values.qostype === 'storage') {
          var customIops = values.iscustomizeddiskiops === true
          params.customizediops = customIops
          if (!customIops) {
            if (values.diskiopsmin != null && values.diskiopsmin.length > 0) {
              params.miniops = values.diskiopsmin
            }
            if (values.diskiopsmax != null && values.diskiopsmax.length > 0) {
              params.maxiops = values.diskiopsmax
            }
            if (values.hypervisorsnapshotreserve !== undefined &&
              values.hypervisorsnapshotreserve != null && values.hypervisorsnapshotreserve.length > 0) {
              params.hypervisorsnapshotreserve = values.hypervisorsnapshotreserve
            }
          }
        } else if (values.qostype === 'hypervisor') {
          if (values.diskbytesreadrate != null && values.diskbytesreadrate.length > 0) {
            params.bytesreadrate = values.diskbytesreadrate
          }
          if (values.diskbyteswriterate != null && values.diskbyteswriterate.length > 0) {
            params.byteswriterate = values.diskbyteswriterate
          }
          if (values.diskiopsreadrate != null && values.diskiopsreadrate.length > 0) {
            params.iopsreadrate = values.diskiopsreadrate
          }
          if (values.diskiopswriterate != null && values.diskiopswriterate.length > 0) {
            params.iopswriterate = values.diskiopswriterate
          }
        }
        if (values.storagetags != null && values.storagetags.length > 0) {
          var tags = values.storagetags.join(',')
          params.tags = tags
        }
        if (values.hosttags != null && values.hosttags.length > 0) {
          params.hosttags = values.hosttags
        }
        if ('deploymentplanner' in values &&
          values.deploymentplanner !== undefined &&
          values.deploymentplanner != null && values.deploymentplanner.length > 0) {
          params.deploymentplanner = values.deploymentplanner
        }
        if ('deploymentplanner' in values &&
          values.deploymentplanner !== undefined &&
          values.deploymentplanner === 'ImplicitDedicationPlanner' &&
          values.plannermode !== undefined &&
          values.plannermode !== '') {
          params['serviceofferingdetails[0].key'] = 'ImplicitDedicationMode'
          params['serviceofferingdetails[0].value'] = values.plannermode
        }
        if ('pcidevice' in values &&
          values.pcidevice !== undefined && values.pcidevice !== '') {
          params['serviceofferingdetails[1].key'] = 'pciDevice'
          params['serviceofferingdetails[1].value'] = values.pcidevice
        }
        if ('vgputype' in values &&
          this.vGpuTypes != null && this.vGpuTypes !== undefined &&
          values.vgputype > this.vGpuTypes.length) {
          params['serviceofferingdetails[2].key'] = 'vgpuType'
          params['serviceofferingdetails[2].value'] = this.vGpuTypes[values.vgputype]
        }
        if ('isvolatile' in values && values.isvolatile !== undefined) {
          params.isvolatile = values.isvolatile === true
        }
        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        var zoneIndexes = values.zoneid
        var zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          var zoneIds = []
          for (var j = 0; j < zoneIndexes.length; j++) {
            zoneIds = zoneIds.concat(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }
        api('createServiceOffering', params).then(json => {
          this.$notification.success({
            message: this.isSystem ? 'Service offering created' : 'Compute offering created',
            description: this.isSystem ? 'Service offering created' : 'Compute offering created'
          })
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.loading = false
          this.$emit('refresh-data')
          this.closeAction()
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="scss">
  .form-layout {
    width: 80vw;
    @media (min-width: 800px) {
      width: 400px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
