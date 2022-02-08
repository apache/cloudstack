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
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            autoFocus
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item v-if="isSystem">
          <tooltip-label slot="label" :title="$t('label.systemvmtype')" :tooltip="apiParams.systemvmtype.description"/>
          <a-select
            v-decorator="['systemvmtype', {
              initialValue: 'domainrouter'
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.systemvmtype.description">
            <a-select-option key="domainrouter">{{ $t('label.domain.router') }}</a-select-option>
            <a-select-option key="consoleproxy">{{ $t('label.console.proxy') }}</a-select-option>
            <a-select-option key="secondarystoragevm">{{ $t('label.secondary.storage.vm') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.offeringtype')" v-show="!isSystem">
          <a-radio-group
            v-decorator="['offeringtype', {
              initialValue: offeringType
            }]"
            buttonStyle="solid"
            @change="selected => { handleComputeOfferingTypeChange(selected.target.value) }">
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
        <a-row :gutter="12">
          <a-col :md="8" :lg="8" v-if="offeringType === 'fixed'">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.cpunumber')" :tooltip="apiParams.cpunumber.description"/>
              <a-input
                v-decorator="['cpunumber', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, naturalNumberRule]
                }]"
                :placeholder="apiParams.cpunumber.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="8" :lg="8" v-if="offeringType !== 'customunconstrained'">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.cpuspeed')" :tooltip="apiParams.cpuspeed.description"/>
              <a-input
                v-decorator="['cpuspeed', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, wholeNumberRule]
                }]"
                :placeholder="apiParams.cpuspeed.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="8" :lg="8" v-if="offeringType === 'fixed'">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.memory.mb')" :tooltip="apiParams.memory.description"/>
              <a-input
                v-decorator="['memory', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, naturalNumberRule]
                }]"
                :placeholder="apiParams.memory.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="offeringType === 'customconstrained'">
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.mincpunumber')" :tooltip="apiParams.mincpunumber.description"/>
              <a-input
                v-decorator="['mincpunumber', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, naturalNumberRule]
                }]"
                :placeholder="apiParams.mincpunumber.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.maxcpunumber')" :tooltip="apiParams.maxcpunumber.description"/>
              <a-input
                v-decorator="['maxcpunumber', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, naturalNumberRule]
                }]"
                :placeholder="apiParams.maxcpunumber.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="offeringType === 'customconstrained'">
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.minmemory')" :tooltip="apiParams.minmemory.description"/>
              <a-input
                v-decorator="['minmemory', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, naturalNumberRule]
                }]"
                :placeholder="apiParams.minmemory.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.maxmemory')" :tooltip="apiParams.maxmemory.description"/>
              <a-input
                v-decorator="['maxmemory', {
                  rules: [{ required: true, message: $t('message.error.required.input') }, naturalNumberRule]
                }]"
                :placeholder="apiParams.maxmemory.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item v-if="isAdmin()">
              <tooltip-label slot="label" :title="$t('label.hosttags')" :tooltip="apiParams.hosttags.description"/>
              <a-input
                v-decorator="['hosttags', {}]"
                :placeholder="apiParams.hosttags.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.networkrate')" :tooltip="apiParams.networkrate.description"/>
              <a-input
                v-decorator="['networkrate', { rules: [naturalNumberRule] }]"
                :placeholder="apiParams.networkrate.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.offerha')" :tooltip="apiParams.offerha.description"/>
              <a-switch v-decorator="['offerha', {initialValue: false}]" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.dynamicscalingenabled')" :tooltip="apiParams.dynamicscalingenabled.description"/>
              <a-switch v-decorator="['dynamicscalingenabled', {initialValue: dynamicscalingenabled}]" :checked="dynamicscalingenabled" @change="val => { dynamicscalingenabled = val }"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.limitcpuuse')" :tooltip="apiParams.limitcpuuse.description"/>
              <a-switch v-decorator="['limitcpuuse', {initialValue: false}]" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item v-if="!isSystem">
              <tooltip-label slot="label" :title="$t('label.isvolatile')" :tooltip="apiParams.isvolatile.description"/>
              <a-switch v-decorator="['isvolatile', {initialValue: false}]" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="!isSystem && isAdmin()">
          <tooltip-label slot="label" :title="$t('label.deploymentplanner')" :tooltip="apiParams.deploymentplanner.description"/>
          <a-select
            v-decorator="['deploymentplanner', {
              initialValue: deploymentPlanners.length > 0 ? deploymentPlanners[0].name : ''
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="deploymentPlannerLoading"
            :placeholder="apiParams.deploymentplanner.description"
            @change="val => { handleDeploymentPlannerChange(val) }">
            <a-select-option v-for="(opt) in deploymentPlanners" :key="opt.name">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.plannermode')" v-if="plannerModeVisible">
          <a-radio-group
            v-decorator="['plannermode', {
              initialValue: plannerMode
            }]"
            buttonStyle="solid"
            @change="selected => { handlePlannerModeChange(selected.target.value) }">
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
        <a-form-item :label="$t('label.gpu')" v-if="!isSystem">
          <a-radio-group
            v-decorator="['pcidevice', {
              initialValue: selectedGpu
            }]"
            buttonStyle="solid"
            @change="selected => { handleGpuChange(selected.target.value) }">
            <a-radio-button v-for="(opt, optIndex) in gpuTypes" :key="optIndex" :value="opt.value">
              {{ opt.title }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.vgputype')" v-if="vGpuVisible">
          <a-select
            v-decorator="['vgputype', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="$t('label.vgputype')">
            <a-select-option v-for="(opt, optIndex) in vGpuTypes" :key="optIndex">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: isPublic}]" :checked="isPublic" @change="val => { isPublic = val }" />
        </a-form-item>
        <a-form-item v-if="!isPublic">
          <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
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
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="block" style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="!isSystem">
          <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          <a-select
            id="zone-selection"
            mode="multiple"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback($t('message.error.zone.combined'))
                    }
                    callback()
                  }
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @select="val => fetchvSphereStoragePolicies(val)"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="global" style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
          <tooltip-label slot="label" :title="$t('label.vmware.storage.policy')" :tooltip="apiParams.storagepolicy.description"/>
          <a-select
            v-decorator="['storagepolicy']"
            :placeholder="apiParams.storagepolicy.description"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="policy in storagePolicies" :key="policy.id">
              {{ policy.name || policy.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.computeonly.offering') }}
          </span>
          <a-switch v-decorator="['computeonly', {initialValue: computeonly}]" :checked="computeonly" @change="val => { computeonly = val }"/>
        </a-form-item>
        <a-card>
          <span v-if="computeonly">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.storagetype')" :tooltip="apiParams.storagetype.description"/>
              <a-radio-group
                v-decorator="['storagetype', {
                  initialValue: storageType
                }]"
                buttonStyle="solid"
                @change="selected => { handleStorageTypeChange(selected.target.value) }">
                <a-radio-button value="shared">
                  {{ $t('label.shared') }}
                </a-radio-button>
                <a-radio-button value="local">
                  {{ $t('label.local') }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.provisioningtype')" :tooltip="apiParams.provisioningtype.description"/>
              <a-radio-group
                v-decorator="['provisioningtype', {
                  initialValue: provisioningType
                }]"
                buttonStyle="solid"
                @change="selected => { handleProvisioningTypeChange(selected.target.value) }">
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
              <tooltip-label slot="label" :title="$t('label.cachemode')" :tooltip="apiParams.cachemode.description"/>
              <a-radio-group
                v-decorator="['cachemode', {
                  initialValue: cacheMode
                }]"
                buttonStyle="solid"
                @change="selected => { handleCacheModeChange(selected.target.value) }">
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
            <a-form-item :label="$t('label.qostype')">
              <a-radio-group
                v-decorator="['qostype', {
                  initialValue: qosType
                }]"
                buttonStyle="solid"
                @change="selected => { handleQosTypeChange(selected.target.value) }">
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
            <a-row :gutter="12" v-if="qosType === 'hypervisor'">
              <a-col :md="12" :lg="12">
                <a-form-item>
                  <tooltip-label slot="label" :title="$t('label.diskbytesreadrate')" :tooltip="apiParams.bytesreadrate.description"/>
                  <a-input
                    v-decorator="['diskbytesreadrate', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.bytesreadrate.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item>
                  <tooltip-label slot="label" :title="$t('label.diskbyteswriterate')" :tooltip="apiParams.byteswriterate.description"/>
                  <a-input
                    v-decorator="['diskbyteswriterate', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.byteswriterate.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12" v-if="qosType === 'hypervisor'">
              <a-col :md="12" :lg="12">
                <a-form-item>
                  <tooltip-label slot="label" :title="$t('label.diskiopsreadrate')" :tooltip="apiParams.iopsreadrate.description"/>
                  <a-input
                    v-decorator="['diskiopsreadrate', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.iopsreadrate.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item>
                  <tooltip-label slot="label" :title="$t('label.diskiopswriterate')" :tooltip="apiParams.iopswriterate.description"/>
                  <a-input
                    v-decorator="['diskiopswriterate', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.iopswriterate.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="!isSystem && qosType === 'storage'">
              <tooltip-label slot="label" :title="$t('label.iscustomizeddiskiops')" :tooltip="apiParams.customizediops.description"/>
              <a-switch v-decorator="['iscustomizeddiskiops', {initialValue: isCustomizedDiskIops}]" :defaultChecked="isCustomizedDiskIops" @change="val => { isCustomizedDiskIops = val }" />
            </a-form-item>
            <a-row :gutter="12" v-if="qosType === 'storage' && !isCustomizedDiskIops">
              <a-col :md="12" :lg="12">
                <a-form-item>
                  <tooltip-label slot="label" :title="$t('label.diskiopsmin')" :tooltip="apiParams.miniops.description"/>
                  <a-input
                    v-decorator="['diskiopsmin', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.miniops.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item>
                  <tooltip-label slot="label" :title="$t('label.diskiopsmax')" :tooltip="apiParams.maxiops.description"/>
                  <a-input
                    v-decorator="['diskiopsmax', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.maxiops.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="!isSystem && qosType === 'storage'">
              <tooltip-label slot="label" :title="$t('label.hypervisorsnapshotreserve')" :tooltip="apiParams.hypervisorsnapshotreserve.description"/>
              <a-input
                v-decorator="['hypervisorsnapshotreserve', { rules: [naturalNumberRule] }]"
                :placeholder="apiParams.hypervisorsnapshotreserve.description"/>
            </a-form-item>
            <a-row :gutter="12">
              <a-col :md="12" :lg="12">
                <a-form-item v-if="apiParams.rootdisksize">
                  <tooltip-label slot="label" :title="$t('label.root.disk.size')" :tooltip="apiParams.rootdisksize.description"/>
                  <a-input
                    v-decorator="['rootdisksize', { rules: [naturalNumberRule] }]"
                    :placeholder="apiParams.rootdisksize.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item v-if="isAdmin()">
                  <tooltip-label slot="label" :title="$t('label.storagetags')" :tooltip="apiParams.tags.description"/>
                  <a-select
                    mode="tags"
                    v-decorator="['storagetags', {}]"
                    showSearch
                    optionFilterProp="children"
                    :filterOption="(input, option) => {
                      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }"
                    :loading="storageTagLoading"
                    :placeholder="apiParams.tags.description"
                    v-if="isAdmin()">
                    <a-select-option v-for="opt in storageTags" :key="opt">
                      {{ opt }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
          </span>
          <span v-if="!computeonly">
            <a-form-item>
              <a-button type="primary" @click="addDiskOffering()"> {{ $t('label.add.disk.offering') }} </a-button>
              <a-modal
                :visible="showDiskOfferingModal"
                :title="$t('label.add.disk.offering')"
                :footer="null"
                centered
                :closable="true"
                @cancel="closeDiskOfferingModal"
                width="auto">
                <add-disk-offering @close-action="closeDiskOfferingModal()" @publish-disk-offering-id="($event) => updateSelectedDiskOffering($event)"/>
              </a-modal>
              <br /><br />
              <a-form-item :label="$t('label.disk.offerings')">
                <a-select
                  v-decorator="['diskofferingid', {
                    initialValue: selectedDiskOfferingId,
                    rules: [{ required: true, message: `${this.$t('message.error.select')}` }]}]"
                  :loading="loading"
                  :placeholder="$t('label.diskoffering')">
                  <a-select-option
                    v-for="(offering, index) in diskOfferings"
                    :value="offering.id"
                    :key="index">
                    {{ offering.displaytext || offering.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-form-item>
          </span>
          <a-form-item>
            <span slot="label">
              {{ $t('label.diskofferingstrictness') }}
              <a-tooltip :title="apiParams.diskofferingstrictness.description">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>
            <a-switch v-decorator="['diskofferingstrictness', {initialValue: diskofferingstrictness}]" :checked="diskofferingstrictness" @change="val => { diskofferingstrictness = val }"/>
          </a-form-item>
        </a-card>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import AddDiskOffering from '@/views/offering/AddDiskOffering'
import { isAdmin } from '@/role'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddServiceOffering',
  components: {
    AddDiskOffering,
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      isSystem: false,
      naturalNumberRule: {
        validator: (rule, value, callback) => {
          if (value && (isNaN(value) || value <= 0)) {
            callback(this.$t('message.error.number'))
          }
          callback()
        }
      },
      wholeNumberRule: {
        validator: (rule, value, callback) => {
          if (value && (isNaN(value) || value < 0)) {
            callback(this.$t('message.error.number'))
          }
          callback()
        }
      },
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
      showDiskOfferingModal: false,
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
      dynamicscalingenabled: true,
      diskofferingstrictness: false,
      computeonly: true,
      diskOfferingLoading: false,
      diskOfferings: [],
      selectedDiskOfferingId: ''
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this, {
      onValuesChange: (_, values) => {
        this.selectedZoneIndex = values.zoneid
      }
    })
    this.apiParams = this.$getApiParams('createServiceOffering')
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
    this.isPublic = isAdmin()
  },
  methods: {
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
      if (isAdmin()) {
        this.fetchStorageTagData()
        this.fetchDeploymentPlannerData()
      }
      this.fetchDiskOfferings()
    },
    addDiskOffering () {
      this.showDiskOfferingModal = true
    },
    fetchDiskOfferings () {
      this.diskOfferingLoading = true
      api('listDiskOfferings', {
        listall: true
      }).then(json => {
        this.diskOfferings = json.listdiskofferingsresponse.diskoffering || []
        if (this.selectedDiskOfferingId === '') {
          this.selectedDiskOfferingId = this.diskOfferings[0].id || ''
        }
      }).finally(() => {
        this.diskOfferingLoading = false
      })
    },
    updateSelectedDiskOffering (id) {
      if (id) {
        this.selectedDiskOfferingId = id
      }
    },
    closeDiskOfferingModal () {
      this.fetchDiskOfferings()
      this.showDiskOfferingModal = false
    },
    isAdmin () {
      return isAdmin()
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
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
      params.showicon = true
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
        this.plannerModeVisible = isAdmin()
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
      if (this.loading) return
      this.form.validateFieldsAndScroll((err, values) => {
        if (err) {
          return
        }
        this.loading = true
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
          dynamicscalingenabled: values.dynamicscalingenabled,
          diskofferingstrictness: values.diskofferingstrictness
        }
        if (values.diskofferingid) {
          params.diskofferingid = values.diskofferingid
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
      width: 700px;
    }
  }
</style>
