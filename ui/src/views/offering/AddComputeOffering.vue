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
        <a-form-item>
          <span slot="label">
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            autoFocus
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="this.$t('label.name')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.displaytext') }}
            <a-tooltip :title="apiParams.displaytext.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="this.$t('label.displaytext')"/>
        </a-form-item>
        <a-form-item v-if="this.isSystem">
          <span slot="label">
            {{ $t('label.systemvmtype') }}
            <a-tooltip :title="apiParams.systemvmtype.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            v-decorator="['systemvmtype', {
              initialValue: 'domainrouter'
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="this.$t('label.systemvmtype')">
            <a-select-option key="domainrouter">{{ $t('label.domain.router') }}</a-select-option>
            <a-select-option key="consoleproxy">{{ $t('label.console.proxy') }}</a-select-option>
            <a-select-option key="secondarystoragevm">{{ $t('label.secondary.storage.vm') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.storagetype') }}
            <a-tooltip :title="apiParams.storagetype.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
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
        <a-form-item>
          <span slot="label">
            {{ $t('label.provisioningtype') }}
            <a-tooltip :title="apiParams.provisioningtype.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-radio-group
            v-decorator="['provisioningtype', {
              initialValue: this.provisioningType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleProvisioningTypeChange(selected.target.value) }">
            <a-radio-button value="thin">
              {{ $t('label.provisioningtype.thin') }}
            </a-radio-button>
            <a-radio-button value="sparse">
              {{ $t('label.provisioningtype.sparse') }}
            </a-radio-button>
            <a-radio-button value="fat">
              {{ $t('label.provisioningtype.fat') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.cachemode') }}
            <a-tooltip :title="apiParams.cachemode.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-radio-group
            v-decorator="['cachemode', {
              initialValue: this.cacheMode
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleCacheModeChange(selected.target.value) }">
            <a-radio-button value="none">
              {{ $t('label.nodiskcache') }}
            </a-radio-button>
            <a-radio-button value="writeback">
              {{ $t('label.writeback') }}
            </a-radio-button>
            <a-radio-button value="writethrough">
              {{ $t('label.writethrough') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.offeringtype')" v-show="!this.isSystem">
          <a-radio-group
            v-decorator="['offeringtype', {
              initialValue: this.offeringType
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
        <a-form-item v-if="this.offeringType === 'fixed'">
          <span slot="label">
            {{ $t('label.cpunumber') }}
            <a-tooltip :title="apiParams.cpunumber.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['cpunumber', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.cpunumber')"/>
        </a-form-item>
        <a-form-item v-if="this.offeringType !== 'customunconstrained'">
          <span slot="label">
            {{ $t('label.cpuspeed') }}
            <a-tooltip :title="apiParams.cpuspeed.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['cpuspeed', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value < 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.cpuspeed')"/>
        </a-form-item>
        <a-form-item v-if="this.offeringType === 'customconstrained'">
          <span slot="label">
            {{ $t('label.mincpunumber') }}
            <a-tooltip :title="apiParams.mincpunumber.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['mincpunumber', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.mincpunumber')"/>
        </a-form-item>
        <a-form-item v-if="this.offeringType === 'customconstrained'">
          <span slot="label">
            {{ $t('label.maxcpunumber') }}
            <a-tooltip :title="apiParams.maxcpunumber.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['maxcpunumber', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.maxcpunumber')"/>
        </a-form-item>
        <a-form-item v-if="this.offeringType === 'fixed'">
          <span slot="label">
            {{ $t('label.memory.mb') }}
            <a-tooltip :title="apiParams.memory.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['memory', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.memory')"/>
        </a-form-item>
        <a-form-item v-if="this.offeringType === 'customconstrained'">
          <span slot="label">
            {{ $t('label.minmemory') }}
            <a-tooltip :title="apiParams.minmemory.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['minmemory', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.minmemory')"/>
        </a-form-item>
        <a-form-item v-if="this.offeringType === 'customconstrained'">
          <span slot="label">
            {{ $t('label.maxmemory') }}
            <a-tooltip :title="apiParams.maxmemory.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['maxmemory', {
              rules: [{ required: true, message: $t('message.error.required.input') },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback(this.$t('message.error.number'))
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="this.$t('label.maxmemory')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.networkrate') }}
            <a-tooltip :title="apiParams.networkrate.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['networkrate', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback(this.$t('message.error.number'))
                    }
                    callback()
                  }
                }
              ]
            }]"
            :placeholder="this.$t('label.networkrate')"/>
        </a-form-item>
        <a-form-item v-if="apiParams.rootdisksize">
          <span slot="label">
            {{ $t('label.root.disk.size') }}
            <a-tooltip :title="apiParams.rootdisksize.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['rootdisksize', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback(this.$t('message.error.number'))
                    }
                    callback()
                  }
                }
              ]
            }]"
            :placeholder="this.$t('label.root.disk.size')"/>
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
        <a-form-item v-if="this.qosType === 'hypervisor'">
          <span slot="label">
            {{ $t('label.diskbytesreadrate') }}
            <a-tooltip :title="apiParams.bytesreadrate.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['diskbytesreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.diskbytesreadrate')"/>
        </a-form-item>
        <a-form-item v-if="this.qosType === 'hypervisor'">
          <span slot="label">
            {{ $t('label.diskbyteswriterate') }}
            <a-tooltip :title="apiParams.byteswriterate.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['diskbyteswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.diskbyteswriterate')"/>
        </a-form-item>
        <a-form-item v-if="this.qosType === 'hypervisor'">
          <span slot="label">
            {{ $t('label.diskiopsreadrate') }}
            <a-tooltip :title="apiParams.iopsreadrate.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['diskiopsreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.diskiopsreadrate')"/>
        </a-form-item>
        <a-form-item v-if="this.qosType === 'hypervisor'">
          <span slot="label">
            {{ $t('label.diskiopswriterate') }}
            <a-tooltip :title="apiParams.iopswriterate.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['diskiopswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.diskiopswriterate')"/>
        </a-form-item>
        <a-form-item v-if="!this.isSystem && this.qosType === 'storage'">
          <span slot="label">
            {{ $t('label.iscustomizeddiskiops') }}
            <a-tooltip :title="apiParams.customizediops.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['iscustomizeddiskiops', {initialValue: this.isCustomizedDiskIops}]" :defaultChecked="this.isCustomizedDiskIops" @change="val => { this.isCustomizedDiskIops = val }" />
        </a-form-item>
        <a-form-item v-if="this.qosType === 'storage' && !this.isCustomizedDiskIops">
          <span slot="label">
            {{ $t('label.diskiopsmin') }}
            <a-tooltip :title="apiParams.miniops.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['diskiopsmin', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.diskiopsmin')"/>
        </a-form-item>
        <a-form-item v-if="this.qosType === 'storage' && !this.isCustomizedDiskIops">
          <span slot="label">
            {{ $t('label.diskiopsmax') }}
            <a-tooltip :title="apiParams.maxiops.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['diskiopsmax', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.diskiopsmax')"/>
        </a-form-item>
        <a-form-item v-if="!this.isSystem && this.qosType === 'storage'">
          <span slot="label">
            {{ $t('label.hypervisorsnapshotreserve') }}
            <a-tooltip :title="apiParams.hypervisorsnapshotreserve.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['hypervisorsnapshotreserve', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.hypervisorsnapshotreserve')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.offerha') }}
            <a-tooltip :title="apiParams.offerha.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['offerha', {initialValue: false}]" />
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.dynamicscalingenabled') }}
            <a-tooltip :title="apiParams.dynamicscalingenabled.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['dynamicscalingenabled', {initialValue: dynamicscalingenabled}]" :checked="dynamicscalingenabled" @change="val => { dynamicscalingenabled = val }"/>
        </a-form-item>
        <a-form-item v-if="this.isAdmin()">
          <span slot="label">
            {{ $t('label.hosttags') }}
            <a-tooltip :title="apiParams.hosttags.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['hosttags', {}]"
            :placeholder="this.$t('label.hosttags')"/>
        </a-form-item>
        <a-form-item v-if="this.isAdmin()">
          <span slot="label">
            {{ $t('label.storagetags') }}
            <a-tooltip :title="apiParams.tags.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            mode="tags"
            v-decorator="['storagetags', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="storageTagLoading"
            :placeholder="$t('label.storagetags')"
            v-if="this.isAdmin()">
            <a-select-option v-for="opt in storageTags" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.limitcpuuse') }}
            <a-tooltip :title="apiParams.limitcpuuse.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['limitcpuuse', {initialValue: false}]" />
        </a-form-item>
        <a-form-item v-if="!this.isSystem">
          <span slot="label">
            {{ $t('label.isvolatile') }}
            <a-tooltip :title="apiParams.isvolatile.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['isvolatile', {initialValue: false}]" />
        </a-form-item>
        <a-form-item v-if="!this.isSystem && this.isAdmin()">
          <span slot="label">
            {{ $t('label.deploymentplanner') }}
            <a-tooltip :title="apiParams.deploymentplanner.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
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
            :placeholder="this.$t('label.deploymentplanner')"
            @change="val => { this.handleDeploymentPlannerChange(val) }">
            <a-select-option v-for="(opt) in this.deploymentPlanners" :key="opt.name">
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
        <a-form-item :label="$t('label.gpu')" v-if="!this.isSystem">
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
        <a-form-item :label="$t('label.ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: this.isPublic}]" :checked="this.isPublic" @change="val => { this.isPublic = val }" />
        </a-form-item>
        <a-form-item v-if="!this.isPublic">
          <span slot="label">
            {{ $t('label.domain') }}
            <a-tooltip :title="apiParams.domainid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
              rules: [
                {
                  required: true,
                  message: $t('message.error.select')
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="this.$t('label.domainid')">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="!this.isSystem">
          <span slot="label">
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            id="zone-selection"
            mode="multiple"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback(this.$t('message.error.zone.combined'))
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
            @select="val => fetchvSphereStoragePolicies(val)"
            :loading="zoneLoading"
            :placeholder="this.$t('label.zoneid')">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
          <span slot="label">
            {{ $t('label.vmware.storage.policy') }}
            <a-tooltip :title="apiParams.storagetype.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            v-decorator="['storagepolicy']"
            :placeholder="apiParams.storagepolicy.description">
            <a-select-option v-for="policy in this.storagePolicies" :key="policy.id">
              {{ policy.name || policy.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
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
      cacheMode: 'none',
      offeringType: 'fixed',
      qosType: '',
      isCustomizedDiskIops: false,
      isPublic: true,
      selectedDomains: [],
      domains: [],
      domainLoading: false,
      selectedZones: [],
      selectedZoneIndex: [],
      zones: [],
      zoneLoading: false,
      selectedDeployementPlanner: null,
      storagePolicies: null,
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
          title: this.$t('label.none'),
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
      loading: false,
      dynamicscalingenabled: true
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this, {
      onValuesChange: (_, values) => {
        this.selectedZoneIndex = values.zoneid
      }
    })
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.createServiceOffering || {}
    apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    if (this.$route.meta.name === 'systemoffering') {
      this.isSystem = true
    }
    this.fetchData()
    this.isPublic = this.isAdmin()
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
      this.storageTags = []
      api('listStorageTags', params).then(json => {
        const tags = json.liststoragetagsresponse.storagetag || []
        for (const tag of tags) {
          if (!this.storageTags.includes(tag.name)) {
            this.storageTags.push(tag.name)
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
    fetchvSphereStoragePolicies (zoneIndex) {
      if (zoneIndex === 0 || this.selectedZoneIndex.length > 1) {
        this.storagePolicies = null
        return
      }
      const zoneid = this.zones[zoneIndex].id
      if ('importVsphereStoragePolicies' in this.$store.getters.apis) {
        this.storagePolicies = []
        api('listVsphereStoragePolicies', {
          zoneid: zoneid
        }).then(response => {
          this.storagePolicies = response.listvspherestoragepoliciesresponse.StoragePolicy || []
        })
      }
    },
    handleStorageTypeChange (val) {
      this.storageType = val
    },
    handleProvisioningTypeChange (val) {
      this.provisioningType = val
    },
    handleCacheModeChange (val) {
      this.cacheMode = val
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
          storagetype: values.storagetype,
          provisioningtype: values.provisioningtype,
          cachemode: values.cachemode,
          customized: values.offeringtype !== 'fixed',
          offerha: values.offerha === true,
          limitcpuuse: values.limitcpuuse === true,
          dynamicscalingenabled: values.dynamicscalingenabled
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

        if (values.networkrate != null && values.networkrate.length > 0) {
          params.networkrate = values.networkrate
        }
        if (values.rootdisksize != null && values.rootdisksize.length > 0) {
          params.rootdisksize = values.rootdisksize
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
        if ('systemvmtype' in values && values.systemvmtype !== undefined) {
          params.systemvmtype = values.systemvmtype
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
        if (values.storagepolicy) {
          params.storagepolicy = values.storagepolicy
        }
        api('createServiceOffering', params).then(json => {
          const message = this.isSystem
            ? `${this.$t('message.create.service.offering')}: `
            : `${this.$t('message.create.compute.offering')}: `
          this.$message.success(message + values.name)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
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
      width: 500px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
