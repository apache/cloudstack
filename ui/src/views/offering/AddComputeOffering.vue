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
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            autoFocus
            v-model:value="form.name"
            :placeholder="$t('label.name')"/>
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            {{ $t('label.displaytext') }}
            <a-tooltip :title="apiParams.displaytext.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="$t('label.displaytext')"/>
        </a-form-item>
        <a-form-item name="systemvmtype" ref="systemvmtype" v-if="isSystem">
          <template #label>
            {{ $t('label.systemvmtype') }}
            <a-tooltip :title="apiParams.systemvmtype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.systemvmtype"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="$t('label.systemvmtype')">
            <a-select-option key="domainrouter">{{ $t('label.domain.router') }}</a-select-option>
            <a-select-option key="consoleproxy">{{ $t('label.console.proxy') }}</a-select-option>
            <a-select-option key="secondarystoragevm">{{ $t('label.secondary.storage.vm') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="storagetype" ref="storagetype">
          <template #label>
            {{ $t('label.storagetype') }}
            <a-tooltip :title="apiParams.storagetype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-radio-group
            v-model:value="form.storagetype"
            buttonStyle="solid">
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
            <a-radio-button value="local">
              {{ $t('label.local') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="provisioningtype" ref="provisioningtype">
          <template #label>
            {{ $t('label.provisioningtype') }}
            <a-tooltip :title="apiParams.provisioningtype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-radio-group
            v-model:value="form.provisioningtype"
            buttonStyle="solid">
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
        <a-form-item name="cachemode" ref="cachemode">
          <template #label>
            {{ $t('label.cachemode') }}
            <a-tooltip :title="apiParams.cachemode.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-radio-group
            v-model:value="form.cachemode"
            buttonStyle="solid">
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
        <a-form-item name="offeringtype" ref="offeringtype" :label="$t('label.offeringtype')" v-show="!isSystem">
          <a-radio-group
            v-model:value="form.offeringtype"
            buttonStyle="solid">
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
        <a-form-item name="cpunumber" ref="cpunumber" v-if="form.offeringtype === 'fixed'">
          <template #label>
            {{ $t('label.cpunumber') }}
            <a-tooltip :title="apiParams.cpunumber.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.cpunumber"
            :placeholder="$t('label.cpunumber')"/>
        </a-form-item>
        <a-form-item name="cpuspeed" ref="cpuspeed" v-if="form.offeringtype !== 'customunconstrained'">
          <template #label>
            {{ $t('label.cpuspeed') }}
            <a-tooltip :title="apiParams.cpuspeed.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.cpuspeed"
            :placeholder="$t('label.cpuspeed')"/>
        </a-form-item>
        <a-form-item name="mincpunumber" ref="mincpunumber" v-if="form.offeringtype === 'customconstrained'">
          <template #label>
            {{ $t('label.mincpunumber') }}
            <a-tooltip :title="apiParams.mincpunumber.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.mincpunumber"
            :placeholder="$t('label.mincpunumber')"/>
        </a-form-item>
        <a-form-item name="maxcpunumber" ref="maxcpunumber" v-if="form.offeringtype === 'customconstrained'">
          <template #label>
            {{ $t('label.maxcpunumber') }}
            <a-tooltip :title="apiParams.maxcpunumber.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.maxcpunumber"
            :placeholder="$t('label.maxcpunumber')"/>
        </a-form-item>
        <a-form-item name="memory" ref="memory" v-if="form.offeringtype === 'fixed'">
          <template #label>
            {{ $t('label.memory.mb') }}
            <a-tooltip :title="apiParams.memory.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.memory"
            :placeholder="$t('label.memory')"/>
        </a-form-item>
        <a-form-item name="minmemory" ref="minmemory" v-if="form.offeringtype === 'customconstrained'">
          <template #label>
            {{ $t('label.minmemory') }}
            <a-tooltip :title="apiParams.minmemory.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.minmemory"
            :placeholder="$t('label.minmemory')"/>
        </a-form-item>
        <a-form-item name="maxmemory" ref="maxmemory" v-if="form.offeringtype === 'customconstrained'">
          <template #label>
            {{ $t('label.maxmemory') }}
            <a-tooltip :title="apiParams.maxmemory.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.maxmemory"
            :placeholder="$t('label.maxmemory')"/>
        </a-form-item>
        <a-form-item name="networkrate" ref="networkrate">
          <template #label>
            {{ $t('label.networkrate') }}
            <a-tooltip :title="apiParams.networkrate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.networkrate"
            :placeholder="$t('label.networkrate')"/>
        </a-form-item>
        <a-form-item name="rootdisksize" ref="rootdisksize" v-if="apiParams.rootdisksize">
          <template #label>
            {{ $t('label.root.disk.size') }}
            <a-tooltip :title="apiParams.rootdisksize.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.rootdisksize"
            :placeholder="$t('label.root.disk.size')"/>
        </a-form-item>
        <a-form-item name="qostype" ref="qostype" :label="$t('label.qostype')">
          <a-radio-group
            v-model:value="form.qostype"
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
        <a-form-item name="diskbytesreadrate" ref="diskbytesreadrate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskbytesreadrate') }}
            <a-tooltip :title="apiParams.bytesreadrate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskbytesreadrate"
            :placeholder="$t('label.diskbytesreadrate')"/>
        </a-form-item>
        <a-form-item name="diskbyteswriterate" ref="diskbyteswriterate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskbyteswriterate') }}
            <a-tooltip :title="apiParams.byteswriterate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskbyteswriterate"
            :placeholder="$t('label.diskbyteswriterate')"/>
        </a-form-item>
        <a-form-item name="diskiopsreadrate" ref="diskiopsreadrate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskiopsreadrate') }}
            <a-tooltip :title="apiParams.iopsreadrate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopsreadrate"
            :placeholder="$t('label.diskiopsreadrate')"/>
        </a-form-item>
        <a-form-item name="diskiopswriterate" ref="diskiopswriterate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskiopswriterate') }}
            <a-tooltip :title="apiParams.iopswriterate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopswriterate"
            :placeholder="$t('label.diskiopswriterate')"/>
        </a-form-item>
        <a-form-item name="iscustomizeddiskiops" ref="iscustomizeddiskiops" v-if="!isSystem && form.qostype === 'storage'">
          <template #label>
            {{ $t('label.iscustomizeddiskiops') }}
            <a-tooltip :title="apiParams.customizediops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.iscustomizeddiskiops" />
        </a-form-item>
        <a-form-item name="diskiopsmin" ref="diskiopsmin" v-if="form.qostype === 'storage' && !form.iscustomizeddiskiops">
          <template #label>
            {{ $t('label.diskiopsmin') }}
            <a-tooltip :title="apiParams.miniops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopsmin"
            :placeholder="$t('label.diskiopsmin')"/>
        </a-form-item>
        <a-form-item name="diskiopsmax" ref="diskiopsmax" v-if="form.qostype === 'storage' && !form.iscustomizeddiskiops">
          <template #label>
            {{ $t('label.diskiopsmax') }}
            <a-tooltip :title="apiParams.maxiops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopsmax"
            :placeholder="$t('label.diskiopsmax')"/>
        </a-form-item>
        <a-form-item name="hypervisorsnapshotreserve" ref="hypervisorsnapshotreserve" v-if="!isSystem && form.qostype === 'storage'">
          <template #label>
            {{ $t('label.hypervisorsnapshotreserve') }}
            <a-tooltip :title="apiParams.hypervisorsnapshotreserve.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.hypervisorsnapshotreserve"
            :placeholder="$t('label.hypervisorsnapshotreserve')"/>
        </a-form-item>
        <a-form-item name="offerha" ref="offerha">
          <template #label>
            {{ $t('label.offerha') }}
            <a-tooltip :title="apiParams.offerha.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.offerha" />
        </a-form-item>
        <a-form-item name="hosttags" ref="hosttags" v-if="isAdmin()">
          <template #label>
            {{ $t('label.hosttags') }}
            <a-tooltip :title="apiParams.hosttags.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.hosttags"
            :placeholder="$t('label.hosttags')"/>
        </a-form-item>
        <a-form-item name="storagetags" ref="storagetags" v-if="isAdmin()">
          <template #label>
            {{ $t('label.storagetags') }}
            <a-tooltip :title="apiParams.tags.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            mode="tags"
            v-model:value="form.storagetags"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="storageTagLoading"
            :placeholder="$t('label.storagetags')"
            v-if="isAdmin()">
            <a-select-option v-for="opt in storageTags" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="limitcpuuse" ref="limitcpuuse">
          <template #label>
            {{ $t('label.limitcpuuse') }}
            <a-tooltip :title="apiParams.limitcpuuse.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.limitcpuuse" />
        </a-form-item>
        <a-form-item name="isvolatile" ref="isvolatile" v-if="!isSystem">
          <template #label>
            {{ $t('label.isvolatile') }}
            <a-tooltip :title="apiParams.isvolatile.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.isvolatile" />
        </a-form-item>
        <a-form-item name="deploymentplanner" ref="deploymentplanner" v-if="!isSystem && isAdmin()">
          <template #label>
            {{ $t('label.deploymentplanner') }}
            <a-tooltip :title="apiParams.deploymentplanner.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.deploymentplanner"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="deploymentPlannerLoading"
            :placeholder="$t('label.deploymentplanner')"
            @change="val => { handleDeploymentPlannerChange(val) }">
            <a-select-option v-for="(opt) in deploymentPlanners" :key="opt.name">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="plannermode" ref="plannermode" :label="$t('label.plannermode')" v-if="plannerModeVisible">
          <a-radio-group
            v-model:value="form.plannermode"
            buttonStyle="solid">
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
        <a-form-item name="pcidevice" ref="pcidevice" :label="$t('label.gpu')" v-if="!isSystem">
          <a-radio-group
            v-model:value="form.pcidevice"
            buttonStyle="solid"
            @change="selected => { handleGpuChange(selected.target.value) }">
            <a-radio-button v-for="(opt, optIndex) in gpuTypes" :key="optIndex" :value="opt.value">
              {{ opt.title }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="vgputype" ref="vgputype" :label="$t('label.vgputype')" v-if="vGpuVisible">
          <a-select
            v-model:value="form.vgputype"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="$t('label.vgputype')">
            <a-select-option v-for="(opt, optIndex) in vGpuTypes" :key="optIndex">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="ispublic" ref="ispublic" :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-model:checked="form.ispublic" />
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="!form.ispublic">
          <template #label>
            {{ $t('label.domain') }}
            <a-tooltip :title="apiParams.domainid.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            mode="multiple"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="$t('label.domainid')">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid" v-if="!isSystem">
          <template #label>
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="zone-selection"
            mode="multiple"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @select="val => fetchvSphereStoragePolicies(val)"
            :loading="zoneLoading"
            :placeholder="$t('label.zoneid')">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="storagepolicy" ref="storagepolicy" v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
          <template #label>
            {{ $t('label.vmware.storage.policy') }}
            <a-tooltip :title="apiParams.storagetype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.storagepolicy"
            :placeholder="apiParams.storagepolicy.description">
            <a-select-option v-for="policy in storagePolicies" :key="policy.id">
              {{ policy.name || policy.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'AddServiceOffering',
  data () {
    return {
      isSystem: false,
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      selectedDeployementPlanner: null,
      storagePolicies: null,
      storageTags: [],
      storageTagLoading: false,
      deploymentPlanners: [],
      deploymentPlannerLoading: false,
      plannerModeVisible: false,
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
      loading: false
    }
  },
  beforeCreate () {
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
    this.isPublic = this.isAdmin()
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        systemvmtype: 'domainrouter',
        storagetype: 'shared',
        provisioningtype: 'thin',
        cachemode: 'none',
        offeringtype: 'fixed',
        ispublic: this.isPublic
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        displaytext: [{ required: true, message: this.$t('message.error.required.input') }],
        cpunumber: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        cpuspeed: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        mincpunumber: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        maxcpunumber: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        memory: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        minmemory: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        maxmemory: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        networkrate: [{ type: 'number', validator: this.validateNumber }],
        rootdisksize: [{ type: 'number', validator: this.validateNumber }],
        diskbytesreadrate: [{ type: 'number', validator: this.validateNumber }],
        diskbyteswriterate: [{ type: 'number', validator: this.validateNumber }],
        diskiopsreadrate: [{ type: 'number', validator: this.validateNumber }],
        diskiopswriterate: [{ type: 'number', validator: this.validateNumber }],
        diskiopsmin: [{ type: 'number', validator: this.validateNumber }],
        diskiopsmax: [{ type: 'number', validator: this.validateNumber }],
        hypervisorsnapshotreserve: [{ type: 'number', validator: this.validateNumber }],
        domainid: [{ type: 'array', required: true, message: this.$t('message.error.select') }],
        zoneid: [{
          type: 'array',
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
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
        this.form.deploymentplanner = this.deploymentPlanners.length > 0 ? this.deploymentPlanners[0].name : ''
      }).finally(() => {
        this.deploymentPlannerLoading = false
      })
    },
    fetchvSphereStoragePolicies (zoneIndex) {
      if (zoneIndex === 0 || this.form.zoneid.length > 1) {
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
    handleDeploymentPlannerChange (planner) {
      this.selectedDeployementPlanner = planner
      this.plannerModeVisible = false
      if (this.selectedDeployementPlanner === 'ImplicitDedicationPlanner') {
        this.plannerModeVisible = this.isAdmin()
      }
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
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        var params = {
          issystem: this.isSystem,
          name: values.name,
          displaytext: values.displaytext,
          storagetype: values.storagetype,
          provisioningtype: values.provisioningtype,
          cachemode: values.cachemode,
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
          this.vGpuTypes !== null && this.vGpuTypes !== undefined &&
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
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.error.number'))
      }
      return Promise.resolve()
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
