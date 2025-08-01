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
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical"
       >
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-focus="true"
            v-model:value="form.name"
            :placeholder="$t('label.name')"/>
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="$t('label.displaytext')"/>
        </a-form-item>
        <a-form-item name="systemvmtype" ref="systemvmtype" v-if="isSystem">
          <template #label>
            <tooltip-label :title="$t('label.systemvmtype')" :tooltip="apiParams.systemvmtype.description"/>
          </template>
          <a-select
            :getPopupContainer="(trigger) => trigger.parentNode"
            v-model:value="form.systemvmtype"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.systemvmtype.description">
            <a-select-option key="domainrouter" :label="$t('label.domain.router')">{{ $t('label.domain.router') }}</a-select-option>
            <a-select-option key="consoleproxy" :label="$t('label.console.proxy')">{{ $t('label.console.proxy') }}</a-select-option>
            <a-select-option key="secondarystoragevm" :label="$t('label.secondary.storage.vm')">{{ $t('label.secondary.storage.vm') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="offeringtype" ref="offeringtype" :label="$t('label.offeringtype')" v-show="!isSystem">
          <a-radio-group
            v-model:value="form.offeringtype"
            @change="selected => { handleComputeOfferingTypeChange(selected.target.value) }"
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
        <a-row :gutter="12">
          <a-col :md="8" :lg="8" v-if="offeringType === 'fixed'">
            <a-form-item name="cpunumber" ref="cpunumber">
              <template #label>
                <tooltip-label :title="$t('label.cpunumber')" :tooltip="apiParams.cpunumber.description"/>
              </template>
              <a-input
                v-model:value="form.cpunumber"
                :placeholder="apiParams.cpunumber.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="8" :lg="8" v-if="offeringType !== 'customunconstrained'">
            <a-form-item name="cpuspeed" ref="cpuspeed">
              <template #label>
                <tooltip-label :title="$t('label.cpuspeed')" :tooltip="apiParams.cpuspeed.description"/>
              </template>
              <a-input
                v-model:value="form.cpuspeed"
                :placeholder="apiParams.cpuspeed.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="8" :lg="8" v-if="offeringType === 'fixed'">
            <a-form-item name="memory" ref="memory">
              <template #label>
                <tooltip-label :title="$t('label.memory.mb')" :tooltip="apiParams.memory.description"/>
              </template>
              <a-input
                v-model:value="form.memory"
                :placeholder="apiParams.memory.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="offeringType === 'customconstrained'">
          <a-col :md="12" :lg="12">
            <a-form-item name="mincpunumber" ref="mincpunumber">
              <template #label>
                <tooltip-label :title="$t('label.mincpunumber')" :tooltip="apiParams.mincpunumber.description"/>
              </template>
              <a-input
                v-model:value="form.mincpunumber"
                :placeholder="apiParams.mincpunumber.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="maxcpunumber" ref="maxcpunumber">
              <template #label>
                <tooltip-label :title="$t('label.maxcpunumber')" :tooltip="apiParams.maxcpunumber.description"/>
              </template>
              <a-input
                v-model:value="form.maxcpunumber"
                :placeholder="apiParams.maxcpunumber.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="offeringType === 'customconstrained'">
          <a-col :md="12" :lg="12">
            <a-form-item name="minmemory" ref="minmemory">
              <template #label>
                <tooltip-label :title="$t('label.minmemory')" :tooltip="apiParams.minmemory.description"/>
              </template>
              <a-input
                v-model:value="form.minmemory"
                :placeholder="apiParams.minmemory.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="maxmemory" ref="maxmemory">
              <template #label>
                <tooltip-label :title="$t('label.maxmemory')" :tooltip="apiParams.maxmemory.description"/>
              </template>
              <a-input
                v-model:value="form.maxmemory"
                :placeholder="apiParams.maxmemory.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item v-if="isAdmin() || isDomainAdminAllowedToInformTags" name="hosttags" ref="hosttags">
              <template #label>
                <tooltip-label :title="$t('label.hosttags')" :tooltip="apiParams.hosttags.description"/>
              </template>
              <a-input
                v-model:value="form.hosttags"
                :placeholder="apiParams.hosttags.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="networkrate" ref="networkrate">
              <template #label>
                <tooltip-label :title="$t('label.networkrate')" :tooltip="apiParams.networkrate.description"/>
              </template>
              <a-input
                v-model:value="form.networkrate"
                :placeholder="apiParams.networkrate.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item name="offerha" ref="offerha">
              <template #label>
                <tooltip-label :title="$t('label.offerha')" :tooltip="apiParams.offerha.description"/>
              </template>
              <a-switch v-model:checked="form.offerha" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="dynamicscalingenabled" ref="dynamicscalingenabled">
              <template #label>
                <tooltip-label :title="$t('label.dynamicscalingenabled')" :tooltip="apiParams.dynamicscalingenabled.description"/>
              </template>
              <a-switch v-model:checked="form.dynamicscalingenabled" @change="val => { dynamicscalingenabled = val }"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item name="limitcpuuse" ref="limitcpuuse">
              <template #label>
                <tooltip-label :title="$t('label.limitcpuuse')" :tooltip="apiParams.limitcpuuse.description"/>
              </template>
              <a-switch v-model:checked="form.limitcpuuse" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item v-if="!isSystem" name="isvolatile" ref="isvolatile">
              <template #label>
                <tooltip-label :title="$t('label.isvolatile')" :tooltip="apiParams.isvolatile.description"/>
              </template>
              <a-switch v-model:checked="form.isvolatile" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="deploymentplanner" ref="deploymentplanner" v-if="!isSystem && isAdmin()">
          <template #label>
            <tooltip-label :title="$t('label.deploymentplanner')" :tooltip="apiParams.deploymentplanner.description"/>
          </template>
          <a-select
            :getPopupContainer="(trigger) => trigger.parentNode"
            v-model:value="form.deploymentplanner"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="deploymentPlannerLoading"
            :placeholder="apiParams.deploymentplanner.description"
            @change="val => { handleDeploymentPlannerChange(val) }">
            <a-select-option v-for="(opt) in deploymentPlanners" :key="opt.name" :label="opt.name || opt.description || ''">
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
            <a-radio-button value="Preferred">
              {{ $t('label.preferred') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="gpucardid" ref="gpucardid" :label="$t('label.gpu.card')" v-if="!isSystem">
          <a-select
            v-model:value="form.gpucardid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="gpuCardLoading"
            :placeholder="$t('label.gpu.card')"
            @change="handleGpuCardChange">
            <a-select-option v-for="(opt, optIndex) in gpuCards" :key="optIndex" :value="opt.id" :label="opt.name || opt.description || ''">
              {{ opt.description || opt.name || '' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="vgpuprofile" ref="vgpuprofile" :label="$t('label.vgpu.profile')" v-if="!isSystem && form.gpucardid && vgpuProfiles.length > 0">
          <a-select
            v-model:value="form.vgpuprofile"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="vgpuProfileLoading"
            :placeholder="$t('label.vgpu.profile')">
            <a-select-option v-for="(vgpu, vgpuIndex) in vgpuProfiles" :key="vgpuIndex" :value="vgpu.id" :label="vgpu.vgpuprofile || ''">
              {{ vgpu.name }} {{ getVgpuProfileDetails(vgpu) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12" v-if="!isSystem && form.gpucardid">
          <a-col :md="12" :lg="12">
            <a-form-item name="gpucount" ref="gpucount">
              <template #label>
                <tooltip-label :title="$t('label.gpu.count')" :tooltip="apiParams.gpucount.description"/>
              </template>
              <a-input
                v-model:value="form.gpucount"
                type="number"
                min="1"
                max="16"
                :placeholder="$t('label.gpu.count')"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="gpudisplay" ref="gpudisplay">
              <template #label>
                <tooltip-label :title="$t('label.gpu.display')" :tooltip="apiParams.gpudisplay.description"/>
              </template>
              <a-switch v-model:checked="form.gpudisplay" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="ispublic" ref="ispublic" :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-model:checked="form.ispublic" />
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="!form.ispublic">
          <template #label>
            <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          </template>
          <a-select
            mode="multiple"
            :getPopupContainer="(trigger) => trigger.parentNode"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid" v-if="!isSystem">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            id="zone-selection"
            mode="multiple"
            :getPopupContainer="(trigger) => trigger.parentNode"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @select="val => fetchvSphereStoragePolicies(val)"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="storagepolicy"
          ref="storagepolicy"
          v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
          <template #label>
            <tooltip-label :title="$t('label.vmware.storage.policy')" :tooltip="apiParams.storagepolicy.description"/>
          </template>
          <a-select
            :getPopupContainer="(trigger) => trigger.parentNode"
            v-model:value="form.storagepolicy"
            :placeholder="apiParams.storagepolicy.description"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="policy in storagePolicies" :key="policy.id" :label="policy.name || policy.id || ''">
              {{ policy.name || policy.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="purgeresources" ref="purgeresources">
          <template #label>
            <tooltip-label :title="$t('label.purgeresources')" :tooltip="apiParams.purgeresources.description"/>
          </template>
          <a-switch v-model:checked="form.purgeresources"/>
        </a-form-item>
        <a-form-item name="showLeaseOptions" ref="showLeaseOptions" v-if="isLeaseFeatureEnabled">
          <template #label>
            <tooltip-label :title="$t('label.lease.enable')" :tooltip="$t('label.lease.enable.tooltip')" />
          </template>
          <a-switch v-model:checked="showLeaseOptions" @change="onToggleLeaseData"/>
        </a-form-item>
        <a-row :gutter="12" v-if="isLeaseFeatureEnabled && showLeaseOptions">
          <a-col :md="12" :lg="12">
            <a-form-item name="leaseduration" ref="leaseduration">
              <template #label>
                <tooltip-label :title="$t('label.leaseduration')"/>
              </template>
              <a-input
                v-model:value="form.leaseduration"
                :placeholder="$t('label.instance.lease.placeholder')"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="leaseexpiryaction" ref="leaseexpiryaction"  v-if="form.leaseduration > 0">
              <template #label>
                <tooltip-label :title="$t('label.leaseexpiryaction')" />
              </template>
              <a-select v-model:value="form.leaseexpiryaction" :defaultValue="expiryActions">
                <a-select-option v-for="action in expiryActions" :key="action" :label="action"/>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="computeonly" ref="computeonly">
          <template #label>
            <tooltip-label :title="$t('label.computeonly.offering')" :tooltip="$t('label.computeonly.offering.tooltip')"/>
          </template>
          <a-switch v-model:checked="form.computeonly" :checked="computeonly" @change="val => { computeonly = val }"/>
        </a-form-item>
        <a-card style="margin-bottom: 10px;">
          <span v-if="computeonly">
            <a-form-item name="storagetype" ref="storagetype">
              <template #label>
                <tooltip-label :title="$t('label.storagetype')" :tooltip="apiParams.storagetype.description"/>
              </template>
              <a-radio-group
                v-model:value="form.storagetype"
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
            <a-form-item name="provisioningtype" ref="provisioningtype">
              <template #label>
                <tooltip-label :title="$t('label.provisioningtype')" :tooltip="apiParams.provisioningtype.description"/>
              </template>
              <a-radio-group
                v-model:value="form.provisioningtype"
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
            <a-form-item name="cachemode" ref="cachemode">
              <template #label>
                <tooltip-label :title="$t('label.cachemode')" :tooltip="apiParams.cachemode.description"/>
              </template>
              <a-radio-group
                v-model:value="form.cachemode"
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
            <a-form-item :label="$t('label.qostype')" name="qostype" ref="qostype">
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
            <a-row :gutter="12" v-if="qosType === 'hypervisor'">
              <a-col :md="12" :lg="12">
                <a-form-item name="diskbytesreadrate" ref="diskbytesreadrate">
                  <template #label>
                    <tooltip-label :title="$t('label.diskbytesreadrate')" :tooltip="apiParams.bytesreadrate.description"/>
                  </template>
                  <a-input
                    v-model:value="form.diskbytesreadrate"
                    :placeholder="apiParams.bytesreadrate.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item name="diskbyteswriterate" ref="diskbyteswriterate">
                  <template #label>
                    <tooltip-label :title="$t('label.diskbyteswriterate')" :tooltip="apiParams.byteswriterate.description"/>
                  </template>
                  <a-input
                    v-model:value="form.diskbyteswriterate"
                    :placeholder="apiParams.byteswriterate.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12" v-if="qosType === 'hypervisor'">
              <a-col :md="12" :lg="12">
                <a-form-item name="diskiopsreadrate" ref="diskiopsreadrate">
                  <template #label>
                    <tooltip-label :title="$t('label.diskiopsreadrate')" :tooltip="apiParams.iopsreadrate.description"/>
                  </template>
                  <a-input
                    v-model:value="form.diskiopsreadrate"
                    :placeholder="apiParams.iopsreadrate.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item name="diskiopswriterate" ref="diskiopswriterate">
                  <template #label>
                    <tooltip-label :title="$t('label.diskiopswriterate')" :tooltip="apiParams.iopswriterate.description"/>
                  </template>
                  <a-input
                    v-model:value="form.diskiopswriterate"
                    :placeholder="apiParams.iopswriterate.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="!isSystem && qosType === 'storage'" name="iscustomizeddiskiops" ref="iscustomizeddiskiops">
              <template #label>
                <tooltip-label :title="$t('label.iscustomizeddiskiops')" :tooltip="apiParams.customizediops.description"/>
              </template>
              <a-switch v-model:checked="form.iscustomizeddiskiops" :checked="isCustomizedDiskIops" @change="val => { isCustomizedDiskIops = val }" />
            </a-form-item>
            <a-row :gutter="12" v-if="qosType === 'storage' && !isCustomizedDiskIops">
              <a-col :md="12" :lg="12">
                <a-form-item name="diskiopsmin" ref="diskiopsmin">
                  <template #label>
                    <tooltip-label :title="$t('label.diskiopsmin')" :tooltip="apiParams.miniops.description"/>
                  </template>
                  <a-input
                    v-model:value="form.diskiopsmin"
                    :placeholder="apiParams.miniops.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item name="diskiopsmax" ref="diskiopsmax">
                  <template #label>
                    <tooltip-label :title="$t('label.diskiopsmax')" :tooltip="apiParams.maxiops.description"/>
                  </template>
                  <a-input
                    v-model:value="form.diskiopsmax"
                    :placeholder="apiParams.maxiops.description"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="!isSystem && qosType === 'storage'" name="hypervisorsnapshotreserve" ref="hypervisorsnapshotreserve">
              <template #label>
                <tooltip-label :title="$t('label.hypervisorsnapshotreserve')" :tooltip="apiParams.hypervisorsnapshotreserve.description"/>
              </template>
              <a-input
                v-model:value="form.hypervisorsnapshotreserve"
                :placeholder="apiParams.hypervisorsnapshotreserve.description"/>
            </a-form-item>
            <a-row :gutter="12">
              <a-col :md="12" :lg="12">
                <a-form-item v-if="apiParams.rootdisksize" name="rootdisksize" ref="rootdisksize">
                  <template #label>
                    <tooltip-label :title="$t('label.root.disk.size')" :tooltip="apiParams.rootdisksize.description"/>
                  </template>
                  <a-input
                    v-model:value="form.rootdisksize"
                    :placeholder="apiParams.rootdisksize.description"/>
                </a-form-item>
              </a-col>
              <a-col :md="12" :lg="12">
                <a-form-item v-if="isAdmin() || isDomainAdminAllowedToInformTags" name="storagetags" ref="storagetags">
                  <template #label>
                    <tooltip-label :title="$t('label.storagetags')" :tooltip="apiParams.tags.description"/>
                  </template>
                  <a-select
                    mode="tags"
                    :getPopupContainer="(trigger) => trigger.parentNode"
                    v-model:value="form.storagetags"
                    showSearch
                    optionFilterProp="value"
                    :filterOption="(input, option) => {
                      return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }"
                    :loading="storageTagLoading"
                    :placeholder="apiParams.tags.description"
                    v-if="isAdmin() || isDomainAdminAllowedToInformTags">
                    <a-select-option v-for="opt in storageTags" :key="opt">
                      {{ opt }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item name="encryptdisk" ref="encryptdisk">
              <template #label>
                <tooltip-label :title="$t('label.encrypt')" :tooltip="apiParams.encryptroot.description" />
              </template>
              <a-switch v-model:checked="form.encryptdisk" :checked="encryptdisk" @change="val => { encryptdisk = val }" />
            </a-form-item>
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
              <a-form-item :label="$t('label.disk.offerings')" name="diskofferingid" ref="diskofferingid">
                <a-select
                  :getPopupContainer="(trigger) => trigger.parentNode"
                  v-model:value="form.diskofferingid"
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
          <a-form-item name="diskofferingstrictness" ref="diskofferingstrictness">
            <template #label>
              <tooltip-label :title="$t('label.diskofferingstrictness')" :tooltip="apiParams.diskofferingstrictness.description"/>
            </template>
            <a-switch v-model:checked="form.diskofferingstrictness" :checked="diskofferingstrictness" @change="val => { diskofferingstrictness = val }"/>
          </a-form-item>
        </a-card>
        <a-form-item name="externaldetails" ref="externaldetails">
          <template #label>
            <tooltip-label :title="$t('label.externaldetails')" :tooltip="apiParams.externaldetails.description"/>
          </template>
          <a-switch v-model:checked="externalDetailsEnabled" @change="onExternalDetailsEnabledChange"/>
          <a-card v-if="externalDetailsEnabled" style="margin-top: 10px">
            <div style="margin-bottom: 10px">{{ $t('message.add.orchestrator.resource.details') }}</div>
            <details-input
              v-model:value="form.externaldetails" />
          </a-card>
        </a-form-item>
      </a-form>
      <br/>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import AddDiskOffering from '@/views/offering/AddDiskOffering'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'
import store from '@/store'

export default {
  name: 'AddServiceOffering',
  mixins: [mixinForm],
  components: {
    AddDiskOffering,
    ResourceIcon,
    TooltipLabel,
    DetailsInput
  },
  data () {
    return {
      isSystem: false,
      naturalNumberRule: {
        type: 'number',
        validator: this.validateNumber
      },
      wholeNumberRule: {
        type: 'number',
        validator: async (rule, value) => {
          if (value && (isNaN(value) || value < 0)) {
            return Promise.reject(this.$t('message.error.number'))
          }
          return Promise.resolve()
        }
      },
      storageType: 'shared',
      provisioningType: 'thin',
      cacheMode: 'none',
      offeringType: 'fixed',
      isCustomizedDiskIops: false,
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
      plannerMode: '',
      selectedGpuCard: '',
      showDiskOfferingModal: false,
      gpuCardLoading: false,
      gpuCards: [],
      loading: false,
      dynamicscalingenabled: true,
      diskofferingstrictness: false,
      encryptdisk: false,
      computeonly: true,
      diskOfferingLoading: false,
      diskOfferings: [],
      selectedDiskOfferingId: '',
      qosType: '',
      isDomainAdminAllowedToInformTags: false,
      isLeaseFeatureEnabled: this.$store.getters.features.instanceleaseenabled,
      showLeaseOptions: false,
      expiryActions: ['STOP', 'DESTROY'],
      defaultLeaseDuration: 90,
      defaultLeaseExpiryAction: 'STOP',
      leaseduration: undefined,
      leaseexpiryaction: undefined,
      vgpuProfiles: [],
      vgpuProfileLoading: false,
      externalDetailsEnabled: false
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
    this.initForm()
    this.fetchData()
    this.isPublic = isAdmin()
    this.form.ispublic = this.isPublic
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        systemvmtype: 'domainrouter',
        offeringtype: this.offeringType,
        ispublic: this.isPublic,
        dynamicscalingenabled: true,
        plannermode: this.plannerMode,
        gpucardid: this.selectedGpuCard,
        vgpuprofile: '',
        gpucount: '1',
        gpudisplay: false,
        computeonly: this.computeonly,
        storagetype: this.storageType,
        provisioningtype: this.provisioningType,
        cachemode: this.cacheMode,
        qostype: this.qosType,
        iscustomizeddiskiops: this.isCustomizedDiskIops,
        diskofferingid: this.selectedDiskOfferingId,
        diskofferingstrictness: this.diskofferingstrictness,
        encryptdisk: this.encryptdisk,
        leaseduration: this.leaseduration,
        leaseexpiryaction: this.leaseexpiryaction
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        cpunumber: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        cpuspeed: [
          { required: true, message: this.$t('message.error.required.input') },
          this.wholeNumberRule
        ],
        mincpunumber: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        maxcpunumber: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        memory: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        minmemory: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        maxmemory: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        networkrate: [this.naturalNumberRule],
        rootdisksize: [this.naturalNumberRule],
        diskbytesreadrate: [this.naturalNumberRule],
        diskbyteswriterate: [this.naturalNumberRule],
        diskiopsreadrate: [this.naturalNumberRule],
        diskiopswriterate: [this.naturalNumberRule],
        diskiopsmin: [this.naturalNumberRule],
        diskiopsmax: [this.naturalNumberRule],
        hypervisorsnapshotreserve: [this.naturalNumberRule],
        domainid: [{ type: 'array', required: true, message: this.$t('message.error.select') }],
        diskofferingid: [{ required: true, message: this.$t('message.error.select') }],
        gpucount: [{
          type: 'number',
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value < 1)) {
              return Promise.reject(this.$t('message.error.number.minimum.one'))
            }
            return Promise.resolve()
          }
        }],
        zoneid: [{
          type: 'array',
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }],
        leaseduration: [this.naturalNumberRule]
      })
    },
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
      this.fetchGPUCards()
      if (isAdmin()) {
        this.fetchStorageTagData()
        this.fetchDeploymentPlannerData()
      } else if (this.isDomainAdmin()) {
        this.checkIfDomainAdminIsAllowedToInformTag()
        if (this.isDomainAdminAllowedToInformTags) {
          this.fetchStorageTagData()
        }
      }
      this.fetchDiskOfferings()
    },
    fetchGPUCards () {
      this.gpuCardLoading = true
      getAPI('listGpuCards', {
      }).then(json => {
        this.gpuCards = json.listgpucardsresponse.gpucard || []
        // Add a "None" option at the beginning
        this.gpuCards.unshift({
          id: '',
          name: this.$t('label.none')
        })
      }).finally(() => {
        this.gpuCardLoading = false
      })
    },
    addDiskOffering () {
      this.showDiskOfferingModal = true
    },
    fetchDiskOfferings () {
      this.diskOfferingLoading = true
      getAPI('listDiskOfferings', {
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
    isDomainAdmin () {
      return ['DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    getVgpuProfileDetails (vgpuProfile) {
      let output = '('
      if (vgpuProfile?.videoram) {
        output += `${vgpuProfile.videoram} MB`
      }
      if (vgpuProfile?.maxresolutionx && vgpuProfile?.maxresolutiony) {
        if (output !== '(') {
          output += ', '
        }
        output += `${vgpuProfile.maxresolutionx}x${vgpuProfile.maxresolutiony}`
      }
      output += ')'
      if (output === '()') {
        return ''
      }
      return output
    },
    checkIfDomainAdminIsAllowedToInformTag () {
      const params = { id: store.getters.userInfo.accountid }
      getAPI('isAccountAllowedToCreateOfferingsWithTags', params).then(json => {
        this.isDomainAdminAllowedToInformTags = json.isaccountallowedtocreateofferingswithtagsresponse.isallowed.isallowed
      })
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
      getAPI('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true
      this.zoneLoading = true
      getAPI('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    fetchStorageTagData () {
      this.storageTagLoading = true
      this.storageTags = []
      getAPI('listStorageTags').then(json => {
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
      this.deploymentPlannerLoading = true
      getAPI('listDeploymentPlanners').then(json => {
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
        getAPI('listVsphereStoragePolicies', {
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
    handleGpuCardChange (cardId) {
      this.selectedGpuCard = cardId
      this.form.vgpuprofile = ''
      if (cardId && cardId !== '') {
        this.fetchVgpuProfiles(cardId)
      } else {
        this.vgpuProfiles = []
        this.form.gpucount = '1'
      }
    },
    fetchVgpuProfiles (gpuCardId) {
      this.vgpuProfileLoading = true
      this.vgpuProfiles = []
      getAPI('listVgpuProfiles', {
        gpucardid: gpuCardId
      }).then(json => {
        this.vgpuProfiles = json.listvgpuprofilesresponse.vgpuprofile || []
        this.form.vgpuprofile = this.vgpuProfiles.length > 0 ? this.vgpuProfiles[0].id : ''
      }).catch(error => {
        console.error('Error fetching vGPU profiles:', error)
        this.vgpuProfiles = []
      }).finally(() => {
        this.vgpuProfileLoading = false
      })
    },
    onExternalDetailsEnabledChange (val) {
      if (val || !this.form.externaldetails) {
        return
      }
      this.form.externaldetails = undefined
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
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
          diskofferingstrictness: values.diskofferingstrictness,
          encryptroot: values.encryptdisk,
          purgeresources: values.purgeresources,
          leaseduration: values.leaseduration,
          leaseexpiryaction: values.leaseexpiryaction
        }

        if (values.diskofferingid) {
          params.diskofferingid = values.diskofferingid
        }

        // Add GPU parameters
        if (values.vgpuprofile) {
          params.vgpuprofileid = values.vgpuprofile
        }
        if (values.gpucount && values.gpucount > 0) {
          params.gpucount = values.gpucount
        }
        if (values.gpudisplay !== undefined) {
          params.gpudisplay = values.gpudisplay
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
        if ('isvolatile' in values && values.isvolatile !== undefined) {
          params.isvolatile = values.isvolatile === true
        }
        if ('systemvmtype' in values && values.systemvmtype !== undefined) {
          params.systemvmtype = values.systemvmtype
        }

        if ('leaseduration' in values && values.leaseduration !== undefined) {
          params.leaseduration = values.leaseduration
        }

        if ('leaseexpiryaction' in values && values.leaseexpiryaction !== undefined) {
          params.leaseexpiryaction = values.leaseexpiryaction
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
        if (values.externaldetails) {
          Object.entries(values.externaldetails).forEach(([key, value]) => {
            params['externaldetails[0].' + key] = value
          })
        }

        postAPI('createServiceOffering', params).then(json => {
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
    },
    onToggleLeaseData () {
      if (this.showLeaseOptions === false) {
        this.leaseduration = undefined
        this.leaseexpiryaction = undefined
      } else {
        this.leaseduration = this.leaseduration !== undefined ? this.leaseduration : this.defaultLeaseDuration
        this.leaseexpiryaction = this.leaseexpiryaction !== undefined ? this.leaseexpiryaction : this.defaultLeaseExpiryAction
      }
      this.form.leaseduration = this.leaseduration
      this.form.leaseexpiryaction = this.leaseexpiryaction
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
