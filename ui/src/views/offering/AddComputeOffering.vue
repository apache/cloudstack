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
        <a-form-item :label="$t('description')">
          <a-input
            v-decorator="['description', {
              rules: [{ required: true, message: 'Please enter description' }]
            }]"
            :placeholder="this.$t('Description')"/>
        </a-form-item>
        <a-form-item :label="$t('label.storagetype')">
          <a-radio-group
            v-decorator="['storagetype', {
              initialValue: this.storageType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleStorageTypeChange(selected.target.value) }">
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
            <a-radio-button value="local">
              {{ $t('label.local') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.provisioningtype')">
          <a-radio-group
            v-decorator="['provisioningtype', {
              initialValue: this.provisioningType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleProvisioningTypeChange(selected.target.value) }">
            <a-radio-button value="thin">
              {{ $t('label.thin') }}
            </a-radio-button>
            <a-radio-button value="sparse">
              {{ $t('label.sparse') }}
            </a-radio-button>
            <a-radio-button value="fat">
              {{ $t('label.fat') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.computeofferingtype')">
          <a-radio-group
            v-decorator="['computeofferingtype', {
              initialValue: this.computeOfferingType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleComputeOfferingTypeChange(selected.target.value) }">
            <a-radio-button value="fixed">
              {{ $t('label.fixed') }}
            </a-radio-button>
            <a-radio-button value="customconstrained">
              {{ $t('label.customconstrained') }}
            </a-radio-button>
            <a-radio-button value="customunconstrained">
              {{ $t('label.customunconstrained') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.cpucores')" v-if="this.computeOfferingType === 'fixed'">
          <a-input
            v-decorator="['cpucores', {
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
            :placeholder="this.$t('label.cpucores')"/>
        </a-form-item>
        <a-form-item :label="$t('label.cpuspeed')" v-if="this.computeOfferingType !== 'customunconstrained'">
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
            :placeholder="this.$t('label.cpuspeed')"/>
        </a-form-item>
        <a-form-item :label="$t('label.mincpucores')" v-if="this.computeOfferingType === 'customconstrained'">
          <a-input
            v-decorator="['mincpucores', {
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
            :placeholder="this.$t('label.mincpucores')"/>
        </a-form-item>
        <a-form-item :label="$t('label.maxcpucores')" v-if="this.computeOfferingType === 'customconstrained'">
          <a-input
            v-decorator="['maxcpucores', {
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
            :placeholder="this.$t('label.maxcpucores')"/>
        </a-form-item>
        <a-form-item :label="$t('label.memory.mb')" v-if="this.computeOfferingType === 'fixed'">
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
            :placeholder="this.$t('label.memory.mb')"/>
        </a-form-item>
        <a-form-item :label="$t('label.minmemory.mb')" v-if="this.computeOfferingType === 'customconstrained'">
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
            :placeholder="this.$t('label.minmemory.mb')"/>
        </a-form-item>
        <a-form-item :label="$t('label.maxmemory.mb')" v-if="this.computeOfferingType === 'customconstrained'">
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
            :placeholder="this.$t('label.maxmemory.mb')"/>
        </a-form-item>
        <a-form-item :label="$t('label.qostype')">
          <a-radio-group
            v-decorator="['qostype', {
              initialValue: this.qosType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleQosTypeChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('label.none') }}
            </a-radio-button>
            <a-radio-button value="hypervisor">
              {{ $t('label.hypervisor') }}
            </a-radio-button>
            <a-radio-button value="storage">
              {{ $t('label.storage') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.disk.bytes.read.rate')" v-if="this.qosType === 'hypervisor'">
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
            :placeholder="this.$t('label.disk.bytes.read.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.bytes.write.rate')" v-if="this.qosType === 'hypervisor'">
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
            :placeholder="this.$t('label.disk.bytes.write.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.read.rate')" v-if="this.qosType === 'hypervisor'">
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
            :placeholder="this.$t('label.disk.iops.read.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.write.rate')" v-if="this.qosType === 'hypervisor'">
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
            :placeholder="this.$t('label.disk.iops.write.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.custom.disk.iops')" v-if="this.qosType === 'storage'">
          <a-switch v-decorator="['iscustomizeddiskiops']" :checked="this.isCustomizedDiskIops" @change="val => { this.isCustomizedDiskIops = val }" />
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.min')" v-if="!this.isCustomizedDiskIops">
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
            :placeholder="this.$t('label.disk.iops.min')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.max')" v-if="!this.isCustomizedDiskIops">
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
            :placeholder="this.$t('label.disk.iops.max')"/>
        </a-form-item>
        <a-form-item :label="$t('label.hypervisor.snapshot.reserve')" v-if="this.qosType === 'storage'">
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
            :placeholder="this.$t('label.hypervisor.snapshot.reserve')"/>
        </a-form-item>
        <a-form-item :label="$t('label.offerha')">
          <a-switch v-decorator="['isofferha']" />
        </a-form-item>
        <a-form-item :label="$t('label.hosttags')" v-if="this.isAdmin()">
          <a-input
            v-decorator="['hosttags', {}]"
            :placeholder="this.$t('label.hosttags')"/>
        </a-form-item>
        <a-form-item :label="$t('label.storage.tags')" v-if="this.isAdmin()">
          <a-select
            mode="tags"
            v-decorator="['storagetags', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="storageTagLoading"
            :placeholder="this.$t('label.storage.tags')"
            v-if="this.isAdmin()">
            <a-select-option v-for="(opt) in this.storageTags" :key="opt.name">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.cpucap')">
          <a-switch v-decorator="['iscpucap']" />
        </a-form-item>
        <a-form-item :label="$t('label.volatile')">
          <a-switch v-decorator="['isvolatile']" />
        </a-form-item>
        <a-form-item :label="$t('label.deploymentplanner')" v-if="this.isAdmin()">
          <a-select
            v-decorator="['deploymentplanner', {
              rules: [
                {
                  required: true,
                  message: 'Please select option'
                }
              ],
              initialValue: 0
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="deploymentPlannerLoading"
            :placeholder="this.$t('label.deploymentplanner')"
            @change="val => { this.handleDeploymentPlannerChange(this.deploymentPlanners[val]) }">
            <a-select-option v-for="(opt, optIndex) in this.deploymentPlanners" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.plannermode')" v-if="this.plannerModeVisible">
          <a-radio-group
            v-decorator="['plannermode', {
              initialValue: this.plannerMode
            }]"
            buttonStyle="solid"
            @change="selected => { this.handlePlannerModeChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('label.none') }}
            </a-radio-button>
            <a-radio-button value="strict">
              {{ $t('label.strict') }}
            </a-radio-button>
            <a-radio-button value="preferred">
              {{ $t('label.preferred') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.gpu')">
          <a-radio-group
            v-decorator="['gpu', {
              initialValue: this.selectedGpu
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleGpuChange(selected.target.value) }">
            <a-radio-button v-for="(opt, optIndex) in this.gpuTypes" :key="optIndex" :value="opt.value">
              {{ opt.title }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.vgputype')" v-if="this.vGpuVisible">
          <a-select
            v-decorator="['vgputype', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="this.$t('label.vgputype')">
            <a-select-option v-for="(opt, optIndex) in this.vGpuTypes" :key="optIndex">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic']" :checked="this.isPublic" @change="val => { this.isPublic = val }" />
        </a-form-item>
        <a-form-item :label="$t('domainid')" v-if="!this.isPublic">
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
            :placeholder="this.$t('label.domain')">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('zoneid')">
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
            :placeholder="this.$t('label.zone')">
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
      storageType: 'shared',
      provisioningType: 'thin',
      computeOfferingType: 'fixed',
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
        id: 'all',
        name: this.$t('label.all.zone')
      }
    ]
  },
  mounted () {
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
      this.computeOfferingType = val
    },
    handleQosTypeChange (val) {
      this.qosType = val
    },
    handleDeploymentPlannerChange (planner) {
      this.selectedDeployementPlanner = planner
      this.plannerModeVisible = false
      if (this.selectedDeployementPlanner.name === 'ImplicitDedicationPlanner') {
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
        var params = {}
        console.log(params)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="scss">
  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
