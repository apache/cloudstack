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
  <a-form
    ref="internalFormRef"
    :model="form"
    :rules="rules"
    @finish="onInternalSubmit"
    layout="vertical">
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
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
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
        <a-radio-button value="fixed">{{ $t('label.fixed') }}</a-radio-button>
        <a-radio-button value="customconstrained">{{ $t('label.customconstrained') }}</a-radio-button>
        <a-radio-button value="customunconstrained">{{ $t('label.customunconstrained') }}</a-radio-button>
      </a-radio-group>
    </a-form-item>

    <a-row :gutter="12">
      <a-col :md="8" :lg="8" v-if="offeringType === 'fixed'">
        <a-form-item name="cpunumber" ref="cpunumber">
          <template #label>
            <tooltip-label :title="$t('label.cpunumber')" :tooltip="apiParams.cpunumber.description"/>
          </template>
          <a-input v-model:value="form.cpunumber" :placeholder="apiParams.cpunumber.description"/>
        </a-form-item>
      </a-col>
      <a-col :md="8" :lg="8" v-if="offeringType !== 'customunconstrained'">
        <a-form-item name="cpuspeed" ref="cpuspeed">
          <template #label>
            <tooltip-label :title="$t('label.cpuspeed')" :tooltip="apiParams.cpuspeed.description"/>
          </template>
          <a-input v-model:value="form.cpuspeed" :placeholder="apiParams.cpuspeed.description"/>
        </a-form-item>
      </a-col>
      <a-col :md="8" :lg="8" v-if="offeringType === 'fixed'">
        <a-form-item name="memory" ref="memory">
          <template #label>
            <tooltip-label :title="$t('label.memory.mb')" :tooltip="apiParams.memory.description"/>
          </template>
          <a-input v-model:value="form.memory" :placeholder="apiParams.memory.description"/>
        </a-form-item>
      </a-col>
    </a-row>

    <!-- The rest of the form fields (storage, QoS, GPU, tags, etc.) were copied
         from AddComputeOffering.vue to ensure identical behavior. -->

    <a-row :gutter="12" v-if="offeringType === 'customconstrained'">
      <a-col :md="12" :lg="12">
        <a-form-item name="mincpunumber" ref="mincpunumber">
          <template #label>
            <tooltip-label :title="$t('label.mincpunumber')" :tooltip="apiParams.mincpunumber.description"/>
          </template>
          <a-input v-model:value="form.mincpunumber" :placeholder="apiParams.mincpunumber.description"/>
        </a-form-item>
      </a-col>
      <a-col :md="12" :lg="12">
        <a-form-item name="maxcpunumber" ref="maxcpunumber">
          <template #label>
            <tooltip-label :title="$t('label.maxcpunumber')" :tooltip="apiParams.maxcpunumber.description"/>
          </template>
          <a-input v-model:value="form.maxcpunumber" :placeholder="apiParams.maxcpunumber.description"/>
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="12" v-if="offeringType === 'customconstrained'">
      <a-col :md="12" :lg="12">
        <a-form-item name="minmemory" ref="minmemory">
          <template #label>
            <tooltip-label :title="$t('label.minmemory')" :tooltip="apiParams.minmemory.description"/>
          </template>
          <a-input v-model:value="form.minmemory" :placeholder="apiParams.minmemory.description"/>
        </a-form-item>
      </a-col>
      <a-col :md="12" :lg="12">
        <a-form-item name="maxmemory" ref="maxmemory">
          <template #label>
            <tooltip-label :title="$t('label.maxmemory')" :tooltip="apiParams.maxmemory.description"/>
          </template>
          <a-input v-model:value="form.maxmemory" :placeholder="apiParams.maxmemory.description"/>
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="12">
      <a-col :md="12" :lg="12">
        <a-form-item v-if="isAdmin() || isDomainAdminAllowedToInformTags" name="hosttags" ref="hosttags">
          <template #label>
            <tooltip-label :title="$t('label.hosttags')" :tooltip="apiParams.hosttags.description"/>
          </template>
          <a-input v-model:value="form.hosttags" :placeholder="apiParams.hosttags.description"/>
        </a-form-item>
      </a-col>
      <a-col :md="12" :lg="12">
        <a-form-item name="networkrate" ref="networkrate">
          <template #label>
            <tooltip-label :title="$t('label.networkrate')" :tooltip="apiParams.networkrate.description"/>
          </template>
          <a-input v-model:value="form.networkrate" :placeholder="apiParams.networkrate.description"/>
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
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
        :loading="deploymentPlannerLoading"
        :placeholder="apiParams.deploymentplanner.description"
        @change="val => { handleDeploymentPlannerChange(val) }">
        <a-select-option v-for="(opt) in deploymentPlanners" :key="opt.name" :label="opt.name || opt.description || ''">{{ opt.name || opt.description }}</a-select-option>
      </a-select>
    </a-form-item>

    <a-form-item name="plannermode" ref="plannermode" :label="$t('label.plannermode')" v-if="plannerModeVisible">
      <a-radio-group v-model:value="form.plannermode" buttonStyle="solid">
        <a-radio-button value="">{{ $t('label.none') }}</a-radio-button>
        <a-radio-button value="strict">{{ $t('label.strict') }}</a-radio-button>
        <a-radio-button value="Preferred">{{ $t('label.preferred') }}</a-radio-button>
      </a-radio-group>
    </a-form-item>

    <a-form-item name="gpucardid" ref="gpucardid" :label="$t('label.gpu.card')" v-if="!isSystem">
      <a-select
        v-model:value="form.gpucardid"
        showSearch
        optionFilterProp="label"
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
        :loading="gpuCardLoading"
        :placeholder="$t('label.gpu.card')"
        @change="handleGpuCardChange">
        <a-select-option v-for="(opt, optIndex) in gpuCards" :key="optIndex" :value="opt.id" :label="opt.name || opt.description || ''">{{ opt.description || opt.name || '' }}</a-select-option>
      </a-select>
    </a-form-item>

    <a-form-item name="vgpuprofile" ref="vgpuprofile" :label="$t('label.vgpu.profile')" v-if="!isSystem && form.gpucardid && vgpuProfiles.length > 0">
      <a-select
        v-model:value="form.vgpuprofile"
        showSearch
        optionFilterProp="label"
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
        :loading="vgpuProfileLoading"
        :placeholder="$t('label.vgpu.profile')">
        <a-select-option v-for="(vgpu, vgpuIndex) in vgpuProfiles" :key="vgpuIndex" :value="vgpu.id" :label="vgpu.vgpuprofile || ''">{{ vgpu.name }} {{ getVgpuProfileDetails(vgpu) }}</a-select-option>
      </a-select>
    </a-form-item>

    <a-row :gutter="12" v-if="!isSystem && form.gpucardid">
      <a-col :md="12" :lg="12">
        <a-form-item name="gpucount" ref="gpucount">
          <template #label>
            <tooltip-label :title="$t('label.gpu.count')" :tooltip="apiParams.gpucount.description"/>
          </template>
          <a-input v-model:value="form.gpucount" type="number" min="1" max="16" :placeholder="$t('label.gpu.count')"/>
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
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
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
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
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

    <a-form-item name="storagepolicy" ref="storagepolicy" v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
      <template #label>
        <tooltip-label :title="$t('label.vmware.storage.policy')" :tooltip="apiParams.storagepolicy.description"/>
      </template>
      <a-select
        :getPopupContainer="(trigger) => trigger.parentNode"
        v-model:value="form.storagepolicy"
        :placeholder="apiParams.storagepolicy.description"
        showSearch
        optionFilterProp="label"
        :filterOption="(input, option) => { return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0 }">
        <a-select-option v-for="policy in storagePolicies" :key="policy.id" :label="policy.name || policy.id || ''">{{ policy.name || policy.id }}</a-select-option>
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
          <a-input v-model:value="form.leaseduration" :placeholder="$t('label.instance.lease.placeholder')"/>
        </a-form-item>
      </a-col>
      <a-col :md="12" :lg="12">
        <a-form-item name="leaseexpiryaction" ref="leaseexpiryaction" v-if="form.leaseduration > 0">
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
          <a-radio-group v-model:value="form.storagetype" buttonStyle="solid" @change="selected => { handleStorageTypeChange(selected.target.value) }">
            <a-radio-button value="shared">{{ $t('label.shared') }}</a-radio-button>
            <a-radio-button value="local">{{ $t('label.local') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="provisioningtype" ref="provisioningtype">
          <template #label>
            <tooltip-label :title="$t('label.provisioningtype')" :tooltip="apiParams.provisioningtype.description"/>
          </template>
          <a-radio-group v-model:value="form.provisioningtype" buttonStyle="solid" @change="selected => { handleProvisioningTypeChange(selected.target.value) }">
            <a-radio-button value="thin">{{ $t('label.provisioningtype.thin') }}</a-radio-button>
            <a-radio-button value="sparse">{{ $t('label.provisioningtype.sparse') }}</a-radio-button>
            <a-radio-button value="fat">{{ $t('label.provisioningtype.fat') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="cachemode" ref="cachemode">
          <template #label>
            <tooltip-label :title="$t('label.cachemode')" :tooltip="apiParams.cachemode.description"/>
          </template>
          <a-radio-group v-model:value="form.cachemode" buttonStyle="solid" @change="selected => { handleCacheModeChange(selected.target.value) }">
            <a-radio-button value="none">{{ $t('label.nodiskcache') }}</a-radio-button>
            <a-radio-button value="writeback">{{ $t('label.writeback') }}</a-radio-button>
            <a-radio-button value="writethrough">{{ $t('label.writethrough') }}</a-radio-button>
            <a-radio-button value="hypervisor_default">{{ $t('label.hypervisor.default') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.qostype')" name="qostype" ref="qostype">
          <a-radio-group v-model:value="form.qostype" buttonStyle="solid" @change="selected => { handleQosTypeChange(selected.target.value) }">
            <a-radio-button value="">{{ $t('label.none') }}</a-radio-button>
            <a-radio-button value="hypervisor">{{ $t('label.hypervisor') }}</a-radio-button>
            <a-radio-button value="storage">{{ $t('label.storage') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-row :gutter="12" v-if="qosType === 'hypervisor'">
          <a-col :md="12" :lg="12">
            <a-form-item name="diskbytesreadrate" ref="diskbytesreadrate">
              <template #label>
                <tooltip-label :title="$t('label.diskbytesreadrate')" :tooltip="apiParams.bytesreadrate.description"/>
              </template>
              <a-input v-model:value="form.diskbytesreadrate" :placeholder="apiParams.bytesreadrate.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="diskbyteswriterate" ref="diskbyteswriterate">
              <template #label>
                <tooltip-label :title="$t('label.diskbyteswriterate')" :tooltip="apiParams.byteswriterate.description"/>
              </template>
              <a-input v-model:value="form.diskbyteswriterate" :placeholder="apiParams.byteswriterate.description"/>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="12" v-if="qosType === 'hypervisor'">
          <a-col :md="12" :lg="12">
            <a-form-item name="diskiopsreadrate" ref="diskiopsreadrate">
              <template #label>
                <tooltip-label :title="$t('label.diskiopsreadrate')" :tooltip="apiParams.iopsreadrate.description"/>
              </template>
              <a-input v-model:value="form.diskiopsreadrate" :placeholder="apiParams.iopsreadrate.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="diskiopswriterate" ref="diskiopswriterate">
              <template #label>
                <tooltip-label :title="$t('label.diskiopswriterate')" :tooltip="apiParams.iopswriterate.description"/>
              </template>
              <a-input v-model:value="form.diskiopswriterate" :placeholder="apiParams.iopswriterate.description"/>
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
              <a-input v-model:value="form.diskiopsmin" :placeholder="apiParams.miniops.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="diskiopsmax" ref="diskiopsmax">
              <template #label>
                <tooltip-label :title="$t('label.diskiopsmax')" :tooltip="apiParams.maxiops.description"/>
              </template>
              <a-input v-model:value="form.diskiopsmax" :placeholder="apiParams.maxiops.description"/>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item v-if="!isSystem && qosType === 'storage'" name="hypervisorsnapshotreserve" ref="hypervisorsnapshotreserve">
          <template #label>
            <tooltip-label :title="$t('label.hypervisorsnapshotreserve')" :tooltip="apiParams.hypervisorsnapshotreserve.description"/>
          </template>
          <a-input v-model:value="form.hypervisorsnapshotreserve" :placeholder="apiParams.hypervisorsnapshotreserve.description"/>
        </a-form-item>

        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item v-if="apiParams.rootdisksize" name="rootdisksize" ref="rootdisksize">
              <template #label>
                <tooltip-label :title="$t('label.root.disk.size')" :tooltip="apiParams.rootdisksize.description"/>
              </template>
              <a-input v-model:value="form.rootdisksize" :placeholder="apiParams.rootdisksize.description"/>
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
                :filterOption="(input, option) => { return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0 }"
                :loading="storageTagLoading"
                :placeholder="apiParams.tags.description"
                v-if="isAdmin() || isDomainAdminAllowedToInformTags">
                <a-select-option v-for="opt in storageTags" :key="opt">{{ opt }}</a-select-option>
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
            <a-select :getPopupContainer="(trigger) => trigger.parentNode" v-model:value="form.diskofferingid" :loading="loading" :placeholder="$t('label.diskoffering')">
              <a-select-option v-for="(offering, index) in diskOfferings" :value="offering.id" :key="index">{{ offering.displaytext || offering.name }}</a-select-option>
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
        <details-input v-model:value="form.externaldetails" />
      </a-card>
    </a-form-item>

    <slot name="form-actions"></slot>
  </a-form>
</template>

<script>
import { reactive, toRaw } from 'vue'
import { getAPI } from '@/api'
import AddDiskOffering from '@/views/offering/AddDiskOffering'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'
import store from '@/store'

export default {
  name: 'ComputeOfferingForm',
  mixins: [mixinForm],
  components: {
    AddDiskOffering,
    ResourceIcon,
    TooltipLabel,
    DetailsInput
  },
  props: {
    initialValues: {
      type: Object,
      default: () => ({})
    },
    apiParams: {
      type: Object,
      default: () => ({})
    },
    isSystem: {
      type: Boolean,
      default: false
    },
    isAdmin: {
      type: Function,
      default: () => false
    }
  },
  data () {
    return {
      internalFormRef: null,
      form: reactive(Object.assign({
        systemvmtype: 'domainrouter',
        offeringtype: 'fixed',
        ispublic: true,
        dynamicscalingenabled: true,
        plannermode: '',
        gpucardid: '',
        vgpuprofile: '',
        gpucount: '1',
        gpudisplay: false,
        computeonly: true,
        storagetype: 'shared',
        provisioningtype: 'thin',
        cachemode: 'none',
        qostype: '',
        iscustomizeddiskiops: false,
        diskofferingid: null,
        diskofferingstrictness: false,
        encryptdisk: false,
        leaseduration: undefined,
        leaseexpiryaction: undefined
      }, this.initialValues || {})),
      rules: reactive({}),
      // other UI state copied
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
      selectedDeploymentPlanner: null,
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
  created () {
    this.zones = [{ id: null, name: this.$t('label.all.zone') }]
    this.initForm()
    this.fetchData()
    this.isPublic = isAdmin()
    this.form.ispublic = this.isPublic
  },
  methods: {
    initForm () {
      this.formRef = this.$refs.internalFormRef
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }]
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
        this.gpuCards.unshift({ id: '', name: this.$t('label.none') })
      }).finally(() => {
        this.gpuCardLoading = false
      })
    },
    addDiskOffering () { this.showDiskOfferingModal = true },
    fetchDiskOfferings () {
      this.diskOfferingLoading = true
      getAPI('listDiskOfferings', { listall: true }).then(json => {
        this.diskOfferings = json.listdiskofferingsresponse.diskoffering || []
        if (this.selectedDiskOfferingId === '') {
          this.selectedDiskOfferingId = this.diskOfferings[0]?.id || ''
        }
      }).finally(() => { this.diskOfferingLoading = false })
    },
    updateSelectedDiskOffering (id) { if (id) this.selectedDiskOfferingId = id },
    closeDiskOfferingModal () { this.fetchDiskOfferings(); this.showDiskOfferingModal = false },
    isDomainAdmin () {
      return ['DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    getVgpuProfileDetails (vgpuProfile) {
      let output = '('
      if (vgpuProfile?.videoram) output += `${vgpuProfile.videoram} MB`
      if (vgpuProfile?.maxresolutionx && vgpuProfile?.maxresolutiony) {
        if (output !== '(') output += ', '
        output += `${vgpuProfile.maxresolutionx}x${vgpuProfile.maxresolutiony}`
      }
      output += ')'
      return output === '()' ? '' : output
    },
    checkIfDomainAdminIsAllowedToInformTag () {
      const params = { id: store.getters.userInfo.accountid }
      getAPI('isAccountAllowedToCreateOfferingsWithTags', params).then(json => {
        this.isDomainAdminAllowedToInformTags = json.isaccountallowedtocreateofferingswithtagsresponse.isallowed.isallowed
      })
    },
    arrayHasItems (array) { return array !== null && array !== undefined && Array.isArray(array) && array.length > 0 },
    fetchDomainData () {
      const params = { listAll: true, showicon: true, details: 'min' }
      this.domainLoading = true
      getAPI('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => { this.domainLoading = false })
    },
    fetchZoneData () {
      const params = { showicon: true }
      this.zoneLoading = true
      getAPI('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) this.zones = this.zones.concat(listZones)
      }).finally(() => { this.zoneLoading = false })
    },
    fetchStorageTagData () {
      this.storageTagLoading = true
      this.storageTags = []
      getAPI('listStorageTags').then(json => {
        const tags = json.liststoragetagsresponse.storagetag || []
        for (const tag of tags) if (!this.storageTags.includes(tag.name)) this.storageTags.push(tag.name)
      }).finally(() => { this.storageTagLoading = false })
    },
    fetchDeploymentPlannerData () {
      this.deploymentPlannerLoading = true
      getAPI('listDeploymentPlanners').then(json => {
        const planners = json.listdeploymentplannersresponse.deploymentPlanner
        this.deploymentPlanners = this.deploymentPlanners.concat(planners)
        this.deploymentPlanners.unshift({ name: '' })
        this.form.deploymentplanner = this.deploymentPlanners.length > 0 ? this.deploymentPlanners[0].name : ''
      }).finally(() => { this.deploymentPlannerLoading = false })
    },
    fetchvSphereStoragePolicies (zoneIndex) {
      if (zoneIndex === 0 || this.form.zoneid.length > 1) { this.storagePolicies = null; return }
      const zoneid = this.zones[zoneIndex].id
      if ('importVsphereStoragePolicies' in this.$store.getters.apis) {
        this.storagePolicies = []
        getAPI('listVsphereStoragePolicies', { zoneid }).then(response => { this.storagePolicies = response.listvspherestoragepoliciesresponse.StoragePolicy || [] })
      }
    },
    handleStorageTypeChange (val) { this.storageType = val },
    handleProvisioningTypeChange (val) { this.provisioningType = val },
    handleCacheModeChange (val) { this.cacheMode = val },
    handleComputeOfferingTypeChange (val) { this.offeringType = val },
    handleQosTypeChange (val) { this.qosType = val },
    handleDeploymentPlannerChange (planner) { this.selectedDeploymentPlanner = planner; this.plannerModeVisible = false; if (this.selectedDeploymentPlanner === 'ImplicitDedicationPlanner') this.plannerModeVisible = isAdmin() },
    handleGpuCardChange (cardId) { this.selectedGpuCard = cardId; this.form.vgpuprofile = ''; if (cardId && cardId !== '') this.fetchVgpuProfiles(cardId); else { this.vgpuProfiles = []; this.form.gpucount = '1' } },
    fetchVgpuProfiles (gpuCardId) { this.vgpuProfileLoading = true; this.vgpuProfiles = []; getAPI('listVgpuProfiles', { gpucardid: gpuCardId }).then(json => { this.vgpuProfiles = json.listvgpuprofilesresponse.vgpuprofile || []; this.form.vgpuprofile = this.vgpuProfiles.length > 0 ? this.vgpuProfiles[0].id : '' }).catch(() => { this.vgpuProfiles = [] }).finally(() => { this.vgpuProfileLoading = false }) },
    onExternalDetailsEnabledChange (val) { if (val || !this.form.externaldetails) return; this.form.externaldetails = undefined },
    onToggleLeaseData () { if (this.showLeaseOptions === false) { this.leaseduration = undefined; this.leaseexpiryaction = undefined } else { this.leaseduration = this.leaseduration !== undefined ? this.leaseduration : this.defaultLeaseDuration; this.leaseexpiryaction = this.leaseexpiryaction !== undefined ? this.leaseexpiryaction : this.defaultLeaseExpiryAction } this.form.leaseduration = this.leaseduration; this.form.leaseexpiryaction = this.leaseexpiryaction },

    validate () {
      return this.$refs.internalFormRef.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        return values
      })
    },
    onInternalSubmit (e) {
      // When internal form triggers submit, validate and emit
      this.validate().then(values => this.$emit('submit', values))
    }
  }
}
</script>

<style scoped>
</style>
