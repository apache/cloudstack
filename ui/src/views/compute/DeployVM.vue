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
  <div>
    <a-row :gutter="12">
      <a-col :md="24" :lg="17">
        <a-card :bordered="true" :title="$t('label.newinstance')">
          <a-form
            v-ctrl-enter="handleSubmit"
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleSubmit"
            layout="vertical"
          >
            <a-steps direction="vertical" size="small">
              <a-step :title="$t('label.select.deployment.infrastructure')" status="process">
                <template #description>
                  <div style="margin-top: 15px">
                    <span>{{ $t('message.select.a.zone') }}</span><br/>
                    <a-form-item :label="$t('label.zoneid')" name="zoneid" ref="zoneid">
                      <div v-if="zones.length <= 8">
                        <a-row type="flex" :gutter="5" justify="start">
                          <div v-for="(zoneItem, idx) in zones" :key="idx">
                            <a-radio-group
                              :key="idx"
                              v-model:value="form.zoneid"
                              @change="onSelectZoneId(zoneItem.id)">
                              <a-col :span="8">
                                <a-card style="width:200px;" :hoverable="false">
                                  <a-radio :value="zoneItem.id" />
                                  <div :style="{fontSize: '36px', marginLeft: '60px', marginTop: '-30px', marginBottom: '10px'}">
                                      <resource-icon
                                        v-if="zoneItem && zoneItem.icon && zoneItem.icon.base64image"
                                        :image="zoneItem.icon.base64image"
                                        size="36" />
                                      <global-outlined v-else/>
                                    </div>
                                  <a-card-meta title="" :description="zoneItem.name" style="text-align:center; paddingTop: 10px;" />
                                </a-card>
                              </a-col>
                            </a-radio-group>
                          </div>
                        </a-row>
                      </div>
                      <a-select
                        v-else
                        v-model:value="form.zoneid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        @change="onSelectZoneId"
                        :loading="loading.zones"
                        v-focus="true"
                      >
                        <a-select-option v-for="zone1 in zones" :key="zone1.id" :label="zone1.name">
                          <span>
                            <resource-icon v-if="zone1.icon && zone1.icon.base64image" :image="zone1.icon.base64image" size="1x" style="margin-right: 5px"/>
                            <global-outlined v-else style="margin-right: 5px" />
                            {{ zone1.name }}
                          </span>
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="$t('label.podid')"
                      name="podid"
                      ref="podid">
                      <a-select
                        v-model:value="form.podid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="filterOption"
                        :options="podSelectOptions"
                        :loading="loading.pods"
                        @change="onSelectPodId"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="$t('label.clusterid')"
                      name="clusterid"
                      ref="clusterid">
                      <a-select
                        v-model:value="form.clusterid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="filterOption"
                        :options="clusterSelectOptions"
                        :loading="loading.clusters"
                        @change="onSelectClusterId"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="$t('label.hostid')"
                      name="hostid"
                      ref="hostid">
                      <a-select
                        v-model:value="form.hostid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="filterOption"
                        :options="hostSelectOptions"
                        :loading="loading.hosts"
                        @change="onSelectHostId"
                      ></a-select>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.templateiso')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <div v-if="zoneSelected" style="margin-top: 15px">
                    <a-card
                      :tabList="tabList"
                      :activeTabKey="tabKey"
                      @tabChange="key => onTabChange(key, 'tabKey')">
                      <div v-if="tabKey === 'templateid'">
                        {{ $t('message.template.desc') }}
                        <template-iso-selection
                          input-decorator="templateid"
                          :items="options.templates"
                          :selected="tabKey"
                          :loading="loading.templates"
                          :preFillContent="dataPreFill"
                          :key="templateKey"
                          @handle-search-filter="($event) => fetchAllTemplates($event)"
                          @update-template-iso="updateFieldValue" />
                         <div>
                          {{ $t('label.override.rootdisk.size') }}
                          <a-switch
                            v-model:checked="form.rootdisksizeitem"
                            :disabled="rootDiskSizeFixed > 0 || template.deployasis || showOverrideDiskOfferingOption"
                            @change="val => { showRootDiskSizeChanger = val }"
                            style="margin-left: 10px;"/>
                          <div v-if="template.deployasis">  {{ $t('message.deployasis') }} </div>
                        </div>
                        <disk-size-selection
                          v-if="showRootDiskSizeChanger"
                          input-decorator="rootdisksize"
                          :preFillContent="dataPreFill"
                          :isCustomized="true"
                          :minDiskSize="dataPreFill.minrootdisksize"
                          @update-disk-size="updateFieldValue"
                          style="margin-top: 10px;"/>
                      </div>
                      <div v-else>
                        {{ $t('message.iso.desc') }}
                        <template-iso-selection
                          input-decorator="isoid"
                          :items="options.isos"
                          :selected="tabKey"
                          :loading="loading.isos"
                          :preFillContent="dataPreFill"
                          @handle-search-filter="($event) => fetchAllIsos($event)"
                          @update-template-iso="updateFieldValue" />
                        <a-form-item :label="$t('label.hypervisor')">
                          <a-select
                            v-model:value="form.hypervisor"
                            :preFillContent="dataPreFill"
                            :options="hypervisorSelectOptions"
                            @change="value => hypervisor = value"
                            showSearch
                            optionFilterProp="label"
                            :filterOption="filterOption" />
                        </a-form-item>
                      </div>
                    </a-card>
                    <a-form-item class="form-item-hidden">
                      <a-input v-model:value="form.templateid" />
                    </a-form-item>
                    <a-form-item class="form-item-hidden">
                      <a-input v-model:value="form.isoid" />
                    </a-form-item>
                    <a-form-item class="form-item-hidden">
                      <a-input v-model:value="form.rootdisksize" />
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.serviceofferingid')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <div v-if="zoneSelected">
                    <a-form-item v-if="zoneSelected && templateConfigurationExists" name="templateConfiguration" ref="templateConfiguration">
                      <template #label>
                        <tooltip-label :title="$t('label.configuration')" :tooltip="$t('message.ovf.configurations')"/>
                      </template>
                      <a-select
                        showSearch
                        optionFilterProp="label"
                        v-model:value="form.templateConfiguration"
                        defaultActiveFirstOption
                        :placeholder="$t('message.ovf.configurations')"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        @change="onSelectTemplateConfigurationId"
                      >
                        <a-select-option v-for="opt in templateConfigurations" :key="opt.id" :label="opt.name || opt.description">
                          {{ opt.name || opt.description }}
                        </a-select-option>
                      </a-select>
                      <span v-if="selectedTemplateConfiguration && selectedTemplateConfiguration.description">{{ selectedTemplateConfiguration.description }}</span>
                    </a-form-item>
                    <compute-offering-selection
                      :compute-items="options.serviceOfferings"
                      :selected-template="template ? template : {}"
                      :row-count="rowCount.serviceOfferings"
                      :zoneId="zoneId"
                      :value="serviceOffering ? serviceOffering.id : ''"
                      :loading="loading.serviceOfferings"
                      :preFillContent="dataPreFill"
                      :minimum-cpunumber="templateConfigurationExists && selectedTemplateConfiguration && selectedTemplateConfiguration.cpunumber ? selectedTemplateConfiguration.cpunumber : 0"
                      :minimum-cpuspeed="templateConfigurationExists && selectedTemplateConfiguration && selectedTemplateConfiguration.cpuspeed ? selectedTemplateConfiguration.cpuspeed : 0"
                      :minimum-memory="templateConfigurationExists && selectedTemplateConfiguration && selectedTemplateConfiguration.memory ? selectedTemplateConfiguration.memory : 0"
                      @select-compute-item="($event) => updateComputeOffering($event)"
                      @handle-search-filter="($event) => handleSearchFilter('serviceOfferings', $event)"
                    ></compute-offering-selection>
                    <compute-selection
                      v-if="serviceOffering && (serviceOffering.iscustomized || serviceOffering.iscustomizediops)"
                      cpuNumberInputDecorator="cpunumber"
                      cpuSpeedInputDecorator="cpuspeed"
                      memoryInputDecorator="memory"
                      :preFillContent="dataPreFill"
                      :computeOfferingId="instanceConfig.computeofferingid"
                      :isConstrained="isOfferingConstrained(serviceOffering)"
                      :minCpu="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.mincpunumber*1 : 0"
                      :maxCpu="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.maxcpunumber*1 : Number.MAX_SAFE_INTEGER"
                      :minMemory="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.minmemory*1 : 0"
                      :maxMemory="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.maxmemory*1 : Number.MAX_SAFE_INTEGER"
                      :isCustomized="serviceOffering.iscustomized"
                      :isCustomizedIOps="'iscustomizediops' in serviceOffering && serviceOffering.iscustomizediops"
                      @handler-error="handlerError"
                      @update-iops-value="updateIOPSValue"
                      @update-compute-cpunumber="updateFieldValue"
                      @update-compute-cpuspeed="updateFieldValue"
                      @update-compute-memory="updateFieldValue" />
                    <span v-if="serviceOffering && serviceOffering.iscustomized">
                      <a-form-item name="cpunumber" ref="cpunumber" class="form-item-hidden">
                        <a-input v-model:value="form.cpunumber"/>
                      </a-form-item>
                      <a-form-item
                        class="form-item-hidden"
                        v-if="(serviceOffering && !(serviceOffering.cpuspeed > 0))"
                        name="cpuspeed"
                        ref="cpuspeed">
                        <a-input v-model:value="form.cpuspeed"/>
                      </a-form-item>
                      <a-form-item class="form-item-hidden" name="memory" ref="memory">
                        <a-input v-model:value="form.memory"/>
                      </a-form-item>
                    </span>
                    <span v-if="tabKey!=='isoid'">
                      {{ $t('label.override.root.diskoffering') }}
                      <a-switch
                        v-model:checked="showOverrideDiskOfferingOption"
                        :checked="serviceOffering && !serviceOffering.diskofferingstrictness && showOverrideDiskOfferingOption"
                        :disabled="(serviceOffering && serviceOffering.diskofferingstrictness)"
                        @change="val => { updateOverrideRootDiskShowParam(val) }"
                        style="margin-left: 10px;"/>
                    </span>
                    <span v-if="tabKey!=='isoid' && serviceOffering && !serviceOffering.diskofferingstrictness">
                      <a-step
                        :status="zoneSelected ? 'process' : 'wait'"
                        v-if="!template.deployasis && template.childtemplates && template.childtemplates.length > 0" >
                        <template #description>
                          <div v-if="zoneSelected">
                            <multi-disk-selection
                              :items="template.childtemplates"
                              :diskOfferings="options.diskOfferings"
                              :zoneId="zoneId"
                              @select-multi-disk-offering="updateMultiDiskOffering($event)" />
                          </div>
                        </template>
                      </a-step>
                      <a-step
                        v-else
                        :status="zoneSelected ? 'process' : 'wait'">
                        <template #description>
                          <div v-if="zoneSelected">
                            <disk-offering-selection
                              v-if="showOverrideDiskOfferingOption"
                              :items="options.diskOfferings"
                              :row-count="rowCount.diskOfferings"
                              :zoneId="zoneId"
                              :value="overrideDiskOffering ? overrideDiskOffering.id : ''"
                              :loading="loading.diskOfferings"
                              :preFillContent="dataPreFill"
                              :isIsoSelected="tabKey==='isoid'"
                              :isRootDiskOffering="true"
                              @on-selected-root-disk-size="onSelectRootDiskSize"
                              @select-disk-offering-item="($event) => updateOverrideDiskOffering($event)"
                              @handle-search-filter="($event) => handleSearchFilter('diskOfferings', $event)"
                            ></disk-offering-selection>
                            <disk-size-selection
                              v-if="overrideDiskOffering && (overrideDiskOffering.iscustomized || overrideDiskOffering.iscustomizediops)"
                              input-decorator="rootdisksize"
                              :preFillContent="dataPreFill"
                              :minDiskSize="dataPreFill.minrootdisksize"
                              :rootDiskSelected="overrideDiskOffering"
                              :isCustomized="overrideDiskOffering.iscustomized"
                              @handler-error="handlerError"
                              @update-disk-size="updateFieldValue"
                              @update-root-disk-iops-value="updateIOPSValue"/>
                            <a-form-item class="form-item-hidden">
                              <a-input v-model:value="form.rootdisksize"/>
                            </a-form-item>
                          </div>
                        </template>
                      </a-step>
                    </span>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.data.disk')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="!template.deployasis && template.childtemplates && template.childtemplates.length > 0" >
                <template #description>
                  <div v-if="zoneSelected">
                    <multi-disk-selection
                      :items="template.childtemplates"
                      :diskOfferings="options.diskOfferings"
                      :zoneId="zoneId"
                      @select-multi-disk-offering="updateMultiDiskOffering($event)" />
                  </div>
                </template>
              </a-step>
              <a-step
                v-else
                :title="tabKey === 'templateid' ? $t('label.data.disk') : $t('label.disk.size')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <div v-if="zoneSelected">
                    <disk-offering-selection
                      :items="options.diskOfferings"
                      :row-count="rowCount.diskOfferings"
                      :zoneId="zoneId"
                      :value="diskOffering ? diskOffering.id : ''"
                      :loading="loading.diskOfferings"
                      :preFillContent="dataPreFill"
                      :isIsoSelected="tabKey==='isoid'"
                      @on-selected-disk-size="onSelectDiskSize"
                      @select-disk-offering-item="($event) => updateDiskOffering($event)"
                      @handle-search-filter="($event) => handleSearchFilter('diskOfferings', $event)"
                    ></disk-offering-selection>
                    <disk-size-selection
                      v-if="diskOffering && (diskOffering.iscustomized || diskOffering.iscustomizediops)"
                      input-decorator="size"
                      :preFillContent="dataPreFill"
                      :diskSelected="diskSelected"
                      :isCustomized="diskOffering.iscustomized"
                      @handler-error="handlerError"
                      @update-disk-size="updateFieldValue"
                      @update-iops-value="updateIOPSValue"/>
                    <a-form-item class="form-item-hidden">
                      <a-input v-model:value="form.size"/>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.networks')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="zone && zone.networktype !== 'Basic'">
                <template #description>
                  <div v-if="zoneSelected">
                    <div v-if="vm.templateid && templateNics && templateNics.length > 0">
                      <instance-nics-network-select-list-view
                        :nics="templateNics"
                        :zoneid="selectedZone"
                        @select="handleNicsNetworkSelection" />
                    </div>
                    <div v-show="!(vm.templateid && templateNics && templateNics.length > 0)" >
                      <network-selection
                        :items="options.networks"
                        :row-count="rowCount.networks"
                        :value="networkOfferingIds"
                        :loading="loading.networks"
                        :zoneId="zoneId"
                        :preFillContent="dataPreFill"
                        @select-network-item="($event) => updateNetworks($event)"
                        @handle-search-filter="($event) => handleSearchFilter('networks', $event)"
                      ></network-selection>
                      <network-configuration
                        v-if="networks.length > 0"
                        :items="networks"
                        :preFillContent="dataPreFill"
                        @update-network-config="($event) => updateNetworkConfig($event)"
                        @handler-error="($event) => hasError = $event"
                        @select-default-network-item="($event) => updateDefaultNetworks($event)"
                      ></network-configuration>
                    </div>
                  </div>
                </template>
              </a-step>
              <a-step
                v-if="showSecurityGroupSection"
                :title="$t('label.security.groups')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <security-group-selection
                    :zoneId="zoneId"
                    :value="securitygroupids"
                    :loading="loading.networks"
                    :preFillContent="dataPreFill"
                    @select-security-group-item="($event) => updateSecurityGroups($event)"></security-group-selection>
                </template>
              </a-step>
              <a-step
                v-if="isUserAllowedToListSshKeys"
                :title="$t('label.sshkeypairs')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <div v-if="zoneSelected">
                    <ssh-key-pair-selection
                      :items="options.sshKeyPairs"
                      :row-count="rowCount.sshKeyPairs"
                      :zoneId="zoneId"
                      :value="sshKeyPairs"
                      :loading="loading.sshKeyPairs"
                      :preFillContent="dataPreFill"
                      @select-ssh-key-pair-item="($event) => updateSshKeyPairs($event)"
                      @handle-search-filter="($event) => handleSearchFilter('sshKeyPairs', $event)"
                    />
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.ovf.properties')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="vm.templateid && templateProperties && Object.keys(templateProperties).length > 0">
                <template #description>
                  <div v-for="(props, category) in templateProperties" :key="category">
                    <a-alert :message="'Category: ' + category + ' (' + props.length + ' properties)'" type="info" />
                    <div style="margin-left: 15px; margin-top: 10px">
                      <a-form-item
                        v-for="(property, propertyIndex) in props"
                        :key="propertyIndex"
                        :v-bind="property.key"
                        :name="'properties.' + escapePropertyKey(property.key)"
                        :ref="'properties.' + escapePropertyKey(property.key)">
                        <tooltip-label style="text-transform: capitalize" :title="property.label" :tooltip="property.description"/>

                        <span v-if="property.type && property.type==='boolean'">
                          <a-switch
                            v-model:checked="form['properties.' + escapePropertyKey(property.key)]"
                            :placeholder="property.description"
                          />
                        </span>
                        <span v-else-if="property.type && (property.type==='int' || property.type==='real')">
                          <a-input-number
                            v-model:value="form['properties.'+ escapePropertyKey(property.key)]"
                            :placeholder="property.description"
                            :min="getPropertyQualifiers(property.qualifiers, 'number-select').min"
                            :max="getPropertyQualifiers(property.qualifiers, 'number-select').max" />
                        </span>
                        <span v-else-if="property.type && property.type==='string' && property.qualifiers && property.qualifiers.startsWith('ValueMap')">
                          <a-select
                            showSearch
                            optionFilterProp="value"
                            v-model:value="form['properties.' + escapePropertyKey(property.key)]"
                            :placeholder="property.description"
                            :filterOption="(input, option) => {
                              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                            }"
                          >
                            <a-select-option v-for="opt in getPropertyQualifiers(property.qualifiers, 'select')" :key="opt">
                              {{ opt }}
                            </a-select-option>
                          </a-select>
                        </span>
                        <span v-else-if="property.type && property.type==='string' && property.password">
                          <a-input-password
                            v-model:value="form['properties.' + escapePropertyKey(property.key)]"
                            :placeholder="property.description" />
                        </span>
                        <span v-else>
                          <a-input
                            v-model:value="form['properties.' + escapePropertyKey(property.key)]"
                            :placeholder="property.description" />
                        </span>
                      </a-form-item>
                    </div>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.advanced.mode')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description v-if="zoneSelected">
                  <span>
                    {{ $t('label.isadvanced') }}
                    <a-switch v-model:checked="showDetails" style="margin-left: 10px"/>
                  </span>
                  <div style="margin-top: 15px" v-show="showDetails">
                    <div
                      v-if="vm.templateid && ['KVM', 'VMware', 'XenServer'].includes(hypervisor) && !template.deployasis">
                      <a-form-item :label="$t('label.boottype')" name="boottype" ref="boottype">
                        <a-select
                          v-model:value="form.boottype"
                          @change="onBootTypeChange"
                          showSearch
                          optionFilterProp="label"
                          :filterOption="filterOption">
                          <a-select-option v-for="bootType in options.bootTypes" :key="bootType.id" :label="bootType.description">
                            {{ bootType.description }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                      <a-form-item :label="$t('label.bootmode')" name="bootmode" ref="bootmode">
                        <a-select
                          v-model:value="form.bootmode"
                          showSearch
                          optionFilterProp="label"
                          :filterOption="filterOption">
                          <a-select-option v-for="bootMode in options.bootModes" :key="bootMode.id" :label="bootMode.description">
                            {{ bootMode.description }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </div>
                    <a-form-item
                      :label="$t('label.bootintosetup')"
                      v-if="zoneSelected && ((tabKey === 'isoid' && hypervisor === 'VMware') || (tabKey === 'templateid' && template && template.hypervisor === 'VMware'))"
                      name="bootintosetup"
                      ref="bootintosetup">
                      <a-switch v-model:checked="form.bootintosetup" />
                    </a-form-item>
                    <a-form-item name="dynamicscalingenabled" ref="dynamicscalingenabled">
                      <template #label>
                        <tooltip-label :title="$t('label.dynamicscalingenabled')" :tooltip="$t('label.dynamicscalingenabled.tooltip')"/>
                      </template>
                      <a-form-item name="dynamicscalingenabled" ref="dynamicscalingenabled">
                        <a-switch
                          v-model:checked="form.dynamicscalingenabled"
                          :checked="isDynamicallyScalable() && dynamicscalingenabled"
                          :disabled="!isDynamicallyScalable()"
                          @change="val => { dynamicscalingenabled = val }"/>
                      </a-form-item>
                    </a-form-item>
                    <a-form-item :label="$t('label.userdata')">
                      <a-card>
                        <div v-if="this.template && this.template.userdataid">
                          <a-text type="primary">
                              Userdata "{{ $t(this.template.userdataname) }}" is linked with template "{{ $t(this.template.name) }}" with override policy "{{ $t(this.template.userdatapolicy) }}"
                          </a-text><br/><br/>
                          <div v-if="templateUserDataParams.length > 0 && !doUserdataOverride">
                            <a-text type="primary" v-if="this.template && this.template.userdataid && templateUserDataParams.length > 0">
                                Enter the values for the variables in userdata
                            </a-text>
                            <a-input-group>
                              <a-table
                                size="small"
                                style="overflow-y: auto"
                                :columns="userDataParamCols"
                                :dataSource="templateUserDataParams"
                                :pagination="false"
                                :rowKey="record => record.key">
                                <template #bodyCell="{ column, record }">
                                  <template v-if="column.key === 'value'">
                                    <a-input v-model:value="templateUserDataValues[record.key]" />
                                  </template>
                                </template>
                              </a-table>
                            </a-input-group>
                          </div>
                        </div>
                        <div v-if="this.iso && this.iso.userdataid">
                          <a-text type="primary">
                              Userdata "{{ $t(this.iso.userdataname) }}" is linked with ISO "{{ $t(this.iso.name) }}" with override policy "{{ $t(this.iso.userdatapolicy) }}"
                          </a-text><br/><br/>
                          <div v-if="templateUserDataParams.length > 0 && !doUserdataOverride">
                            <a-text type="primary" v-if="this.iso && this.iso.userdataid && templateUserDataParams.length > 0">
                                Enter the values for the variables in userdata
                            </a-text>
                            <a-input-group>
                              <a-table
                                size="small"
                                style="overflow-y: auto"
                                :columns="userDataParamCols"
                                :dataSource="templateUserDataParams"
                                :pagination="false"
                                :rowKey="record => record.key">
                                <template #bodyCell="{ column, record }">
                                  <template v-if="column.key === 'value'">
                                    <a-input v-model:value="templateUserDataValues[record.key]" />
                                  </template>
                                </template>
                              </a-table>
                            </a-input-group>
                          </div>
                        </div><br/><br/>
                        <div v-if="userdataDefaultOverridePolicy === 'ALLOWOVERRIDE' || userdataDefaultOverridePolicy === 'APPEND' || !userdataDefaultOverridePolicy">
                          <span v-if="userdataDefaultOverridePolicy === 'ALLOWOVERRIDE'" >
                            {{ $t('label.userdata.do.override') }}
                            <a-switch v-model:checked="doUserdataOverride" style="margin-left: 10px"/>
                          </span>
                          <span v-if="userdataDefaultOverridePolicy === 'APPEND'">
                            {{ $t('label.userdata.do.append') }}
                            <a-switch v-model:checked="doUserdataAppend" style="margin-left: 10px"/>
                          </span>
                          <a-step
                            :status="zoneSelected ? 'process' : 'wait'">
                            <template #description>
                              <div v-if="doUserdataOverride || doUserdataAppend || !userdataDefaultOverridePolicy" style="margin-top: 15px">
                                <a-card
                                  :tabList="userdataTabList"
                                  :activeTabKey="userdataTabKey"
                                  @tabChange="key => onUserdataTabChange(key, 'userdataTabKey')">
                                  <div v-if="userdataTabKey === 'userdataregistered'">
                                    <a-step
                                      v-if="isUserAllowedToListUserDatas"
                                      :status="zoneSelected ? 'process' : 'wait'">
                                      <template #description>
                                        <div v-if="zoneSelected">
                                          <user-data-selection
                                            :items="options.userDatas"
                                            :row-count="rowCount.userDatas"
                                            :zoneId="zoneId"
                                            :disabled="template.userdatapolicy === 'DENYOVERRIDE'"
                                            :loading="loading.userDatas"
                                            :preFillContent="dataPreFill"
                                            @select-user-data-item="($event) => updateUserData($event)"
                                            @handle-search-filter="($event) => handleSearchFilter('userData', $event)"
                                          />
                                          <div v-if="userDataParams.length > 0">
                                            <a-input-group>
                                              <a-table
                                                size="small"
                                                style="overflow-y: auto"
                                                :columns="userDataParamCols"
                                                :dataSource="userDataParams"
                                                :pagination="false"
                                                :rowKey="record => record.key">
                                                <template #bodyCell="{ column, record }">
                                                  <template v-if="column.key === 'value'">
                                                    <a-input v-model:value="userDataValues[record.key]" />
                                                  </template>
                                                </template>
                                              </a-table>
                                            </a-input-group>
                                          </div>
                                        </div>
                                      </template>
                                    </a-step>
                                  </div>
                                  <div v-else>
                                    <a-form-item name="userdata" ref="userdata" >
                                      <a-textarea
                                        placeholder="Userdata"
                                        v-model:value="form.userdata">
                                      </a-textarea>
                                    </a-form-item>
                                  </div>
                                </a-card>
                              </div>
                            </template>
                          </a-step>
                        </div>
                      </a-card>
                    </a-form-item>
                    <a-form-item :label="$t('label.affinity.groups')">
                      <affinity-group-selection
                        :items="options.affinityGroups"
                        :row-count="rowCount.affinityGroups"
                        :zoneId="zoneId"
                        :value="affinityGroupIds"
                        :loading="loading.affinityGroups"
                        :preFillContent="dataPreFill"
                        @select-affinity-group-item="($event) => updateAffinityGroups($event)"
                        @handle-search-filter="($event) => handleSearchFilter('affinityGroups', $event)"/>
                    </a-form-item>
                    <a-form-item name="iothreadsenabled" ref="iothreadsenabled" v-if="vm.templateid && ['KVM'].includes(hypervisor)">
                      <template #label>
                        <tooltip-label :title="$t('label.iothreadsenabled')" :tooltip="$t('label.iothreadsenabled.tooltip')"/>
                      </template>
                      <a-form-item name="iothreadsenabled" ref="iothreadsenabled">
                        <a-switch
                          v-model:checked="form.iothreadsenabled"
                          :checked="iothreadsenabled"
                          @change="val => { iothreadsenabled = val }"/>
                      </a-form-item>
                    </a-form-item>
                    <a-form-item name="iodriverpolicy" ref="iodriverpolicy" v-if="vm.templateid && ['KVM'].includes(hypervisor)">
                      <template #label>
                        <tooltip-label :title="$t('label.iodriverpolicy')" :tooltip="$t('label.iodriverpolicy.tooltip')"/>
                      </template>
                      <a-select
                        v-model:value="form.iodriverpolicy"
                        optionFilterProp="label"
                        :filterOption="filterOption">
                        <a-select-option v-for="iodriverpolicy in options.ioPolicyTypes" :key="iodriverpolicy.id" :label="iodriverpolicy.description">
                          {{ iodriverpolicy.description }}
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.details')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description v-if="zoneSelected">
                  <div style="margin-top: 15px">
                    {{ $t('message.vm.review.launch') }}
                    <a-form-item :label="$t('label.name.optional')" name="name" ref="name">
                      <a-input v-model:value="form.name" />
                    </a-form-item>
                    <a-form-item :label="$t('label.group.optional')" name="group" ref="group">
                      <a-auto-complete
                        v-model:value="form.group"
                        :filterOption="filterOption"
                        :options="options.instanceGroups" />
                    </a-form-item>
                    <a-form-item :label="$t('label.keyboard')" name="keyboard" ref="keyboard">
                      <a-select
                        v-model:value="form.keyboard"
                        :options="keyboardSelectOptions"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="filterOption"
                      ></a-select>
                    </a-form-item>
                    <a-form-item :label="$t('label.action.start.instance')" name="startvm" ref="startvm">
                      <a-switch v-model:checked="form.startvm" />
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.license.agreements')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="vm.templateid && templateLicenses && templateLicenses.length > 0">
                <template #description>
                  <div style="margin-top: 10px">
                    {{ $t('message.read.accept.license.agreements') }}
                    <a-form-item
                      style="margin-top: 10px"
                      v-for="(license, licenseIndex) in templateLicenses"
                      :key="licenseIndex"
                      :v-bind="license.id">
                      <template #label>
                        <tooltip-label style="text-transform: capitalize" :title="$t('label.agreement' + ' ' + (licenseIndex+1) + ': ' + license.name)"/>
                      </template>
                      <a-textarea
                        v-model:value="license.text"
                        :auto-size="{ minRows: 3, maxRows: 8 }"
                        readOnly />
                      <a-checkbox
                        style="margin-top: 10px"
                        v-model:checked="form.licensesaccepted">
                        {{ $t('label.i.accept.all.license.agreements') }}
                      </a-checkbox>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
            </a-steps>
            <div class="card-footer">
              <a-form-item name="stayonpage" ref="stayonpage">
                <a-switch
                  class="form-item-hidden"
                  v-model:checked="form.stayonpage" />
              </a-form-item>
              <!-- ToDo extract as component -->
              <a-button @click="() => $router.back()" :disabled="loading.deploy">
                {{ $t('label.cancel') }}
              </a-button>
              <a-dropdown-button style="margin-left: 10px" type="primary" ref="submit" @click="handleSubmit" :loading="loading.deploy">
                <rocket-outlined />
                {{ $t('label.launch.vm') }}
                <template #icon><down-outlined /></template>
                <template #overlay>
                  <a-menu type="primary" @click="handleSubmitAndStay" theme="dark" class="btn-stay-on-page">
                    <a-menu-item type="primary" key="1">
                      <rocket-outlined />
                      {{ $t('label.launch.vm.and.stay') }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7" v-if="!isMobile()">
        <a-affix :offsetTop="75" class="vm-info-card">
          <info-card :resource="vm" :title="$t('label.yourinstance')" @change-resource="(data) => resource = data" />
        </a-affix>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { ref, reactive, toRaw, nextTick } from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import { mixin, mixinDevice } from '@/utils/mixin.js'
import store from '@/store'
import eventBus from '@/config/eventBus'

import InfoCard from '@/components/view/InfoCard'
import ResourceIcon from '@/components/view/ResourceIcon'
import ComputeOfferingSelection from '@views/compute/wizard/ComputeOfferingSelection'
import ComputeSelection from '@views/compute/wizard/ComputeSelection'
import DiskOfferingSelection from '@views/compute/wizard/DiskOfferingSelection'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'
import MultiDiskSelection from '@views/compute/wizard/MultiDiskSelection'
import TemplateIsoSelection from '@views/compute/wizard/TemplateIsoSelection'
import AffinityGroupSelection from '@views/compute/wizard/AffinityGroupSelection'
import NetworkSelection from '@views/compute/wizard/NetworkSelection'
import NetworkConfiguration from '@views/compute/wizard/NetworkConfiguration'
import SshKeyPairSelection from '@views/compute/wizard/SshKeyPairSelection'
import UserDataSelection from '@views/compute/wizard/UserDataSelection'
import SecurityGroupSelection from '@views/compute/wizard/SecurityGroupSelection'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import InstanceNicsNetworkSelectListView from '@/components/view/InstanceNicsNetworkSelectListView.vue'
import { sanitizeReverse } from '@/utils/util'

export default {
  name: 'Wizard',
  components: {
    SshKeyPairSelection,
    UserDataSelection,
    NetworkConfiguration,
    NetworkSelection,
    AffinityGroupSelection,
    TemplateIsoSelection,
    DiskSizeSelection,
    MultiDiskSelection,
    DiskOfferingSelection,
    InfoCard,
    ComputeOfferingSelection,
    ComputeSelection,
    SecurityGroupSelection,
    ResourceIcon,
    TooltipLabel,
    InstanceNicsNetworkSelectListView
  },
  props: {
    visible: {
      type: Boolean
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  mixins: [mixin, mixinDevice],
  data () {
    return {
      zoneId: '',
      podId: null,
      clusterId: null,
      zoneSelected: false,
      dynamicscalingenabled: true,
      templateKey: 0,
      showRegisteredUserdata: true,
      doUserdataOverride: false,
      doUserdataAppend: false,
      userdataDefaultOverridePolicy: 'ALLOWOVERRIDE',
      vm: {
        name: null,
        zoneid: null,
        zonename: null,
        hypervisor: null,
        templateid: null,
        templatename: null,
        keyboard: null,
        keypairs: [],
        group: null,
        affinitygroupids: [],
        affinitygroup: [],
        serviceofferingid: null,
        serviceofferingname: null,
        ostypeid: null,
        ostypename: null,
        rootdisksize: null,
        disksize: null
      },
      options: {
        templates: {},
        isos: {},
        hypervisors: [],
        serviceOfferings: [],
        diskOfferings: [],
        zones: [],
        affinityGroups: [],
        networks: [],
        sshKeyPairs: [],
        UserDatas: [],
        pods: [],
        clusters: [],
        hosts: [],
        groups: [],
        keyboards: [],
        bootTypes: [],
        bootModes: [],
        ioPolicyTypes: [],
        dynamicScalingVmConfig: false
      },
      rowCount: {},
      loading: {
        deploy: false,
        templates: false,
        isos: false,
        hypervisors: false,
        serviceOfferings: false,
        diskOfferings: false,
        affinityGroups: false,
        networks: false,
        sshKeyPairs: false,
        userDatas: false,
        zones: false,
        pods: false,
        clusters: false,
        hosts: false,
        groups: false
      },
      instanceConfig: {},
      template: {},
      defaultBootType: '',
      defaultBootMode: '',
      templateConfigurations: [],
      templateNics: [],
      templateLicenses: [],
      templateProperties: {},
      selectedTemplateConfiguration: {},
      iso: {},
      hypervisor: '',
      serviceOffering: {},
      diskOffering: {},
      affinityGroups: [],
      networks: [],
      networksAdd: [],
      zone: {},
      sshKeyPairs: [],
      sshKeyPair: {},
      userData: {},
      userDataParams: [],
      userDataParamCols: [
        {
          title: this.$t('label.key'),
          dataIndex: 'key'
        },
        {
          title: this.$t('label.value'),
          dataIndex: 'value',
          key: 'value'
        }
      ],
      userDataValues: {},
      templateUserDataCols: [
        {
          title: this.$t('label.userdata'),
          dataIndex: 'userdata'
        },
        {
          title: this.$t('label.userdatapolicy'),
          dataIndex: 'userdataoverridepolicy'
        }
      ],
      templateUserDataParams: [],
      templateUserDataValues: {},
      overrideDiskOffering: {},
      templateFilter: [
        'featured',
        'community',
        'selfexecutable',
        'sharedexecutable'
      ],
      isoFilter: [
        'featured',
        'community',
        'selfexecutable',
        'sharedexecutable'
      ],
      initDataConfig: {},
      defaultnetworkid: '',
      networkConfig: [],
      dataNetworkCreated: [],
      tabKey: 'templateid',
      userdataTabKey: 'userdataregistered',
      dataPreFill: {},
      showDetails: false,
      showRootDiskSizeChanger: false,
      showOverrideDiskOfferingOption: false,
      securitygroupids: [],
      rootDiskSizeFixed: 0,
      hasError: false,
      error: false,
      diskSelected: {},
      rootDiskSelected: {},
      diskIOpsMin: 0,
      diskIOpsMax: 0,
      minIops: 0,
      maxIops: 0,
      zones: [],
      selectedZone: '',
      formModel: {},
      nicToNetworkSelection: []
    }
  },
  computed: {
    rootDiskSize () {
      return this.showRootDiskSizeChanger && this.rootDiskSizeFixed > 0
    },
    isNormalAndDomainUser () {
      return ['DomainAdmin', 'User'].includes(this.$store.getters.userInfo.roletype)
    },
    diskSize () {
      const rootDiskSize = _.get(this.instanceConfig, 'rootdisksize', 0)
      const customDiskSize = _.get(this.instanceConfig, 'size', 0)
      const diskOfferingDiskSize = _.get(this.diskOffering, 'disksize', 0)
      const dataDiskSize = diskOfferingDiskSize > 0 ? diskOfferingDiskSize : customDiskSize
      const size = []
      if (rootDiskSize > 0) {
        size.push(`${rootDiskSize} GB (Root)`)
      }
      if (dataDiskSize > 0) {
        size.push(`${dataDiskSize} GB (Data)`)
      }
      return size.join(' | ')
    },
    affinityGroupIds () {
      return _.map(this.affinityGroups, 'id')
    },
    params () {
      return {
        serviceOfferings: {
          list: 'listServiceOfferings',
          options: {
            zoneid: _.get(this.zone, 'id'),
            issystem: false,
            page: 1,
            pageSize: 10,
            keyword: undefined
          }
        },
        diskOfferings: {
          list: 'listDiskOfferings',
          options: {
            zoneid: _.get(this.zone, 'id'),
            page: 1,
            pageSize: 10,
            keyword: undefined
          }
        },
        zones: {
          list: 'listZones',
          isLoad: true,
          field: 'zoneid'
        },
        hypervisors: {
          list: 'listHypervisors',
          options: {
            zoneid: _.get(this.zone, 'id')
          },
          field: 'hypervisor'
        },
        affinityGroups: {
          list: 'listAffinityGroups',
          options: {
            page: 1,
            pageSize: 10,
            keyword: undefined,
            listall: false
          }
        },
        sshKeyPairs: {
          list: 'listSSHKeyPairs',
          options: {
            page: 1,
            pageSize: 10,
            keyword: undefined,
            listall: false
          }
        },
        userDatas: {
          list: 'listUserData',
          options: {
            page: 1,
            pageSize: 10,
            keyword: undefined,
            listall: false
          }
        },
        networks: {
          list: 'listNetworks',
          options: {
            zoneid: _.get(this.zone, 'id'),
            canusefordeploy: true,
            projectid: store.getters.project ? store.getters.project.id : null,
            domainid: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.domainid,
            account: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.account,
            page: 1,
            pageSize: 10,
            keyword: undefined,
            showIcon: true
          }
        },
        pods: {
          list: 'listPods',
          isLoad: !this.isNormalAndDomainUser,
          options: {
            zoneid: _.get(this.zone, 'id')
          },
          field: 'podid'
        },
        clusters: {
          list: 'listClusters',
          isLoad: !this.isNormalAndDomainUser,
          options: {
            zoneid: _.get(this.zone, 'id'),
            podid: this.podId
          },
          field: 'clusterid'
        },
        hosts: {
          list: 'listHosts',
          isLoad: !this.isNormalAndDomainUser,
          options: {
            zoneid: _.get(this.zone, 'id'),
            podid: this.podId,
            clusterid: this.clusterId,
            state: 'Up',
            type: 'Routing'
          },
          field: 'hostid'
        },
        dynamicScalingVmConfig: {
          list: 'listConfigurations',
          options: {
            zoneid: _.get(this.zone, 'id'),
            name: 'enable.dynamic.scale.vm'
          }
        }
      }
    },
    networkOfferingIds () {
      return _.map(this.networks, 'id')
    },
    zoneSelectOptions () {
      return this.options.zones.map((zone) => {
        return {
          label: zone.name,
          value: zone.id
        }
      })
    },
    hypervisorSelectOptions () {
      return this.options.hypervisors.map((hypervisor) => {
        return {
          label: hypervisor.name,
          value: hypervisor.name
        }
      })
    },
    podSelectOptions () {
      const options = this.options.pods.map((pod) => {
        return {
          label: pod.name,
          value: pod.id
        }
      })
      options.unshift({
        label: this.$t('label.default'),
        value: null
      })
      return options
    },
    clusterSelectOptions () {
      const options = this.options.clusters.map((cluster) => {
        return {
          label: cluster.name,
          value: cluster.id
        }
      })
      options.unshift({
        label: this.$t('label.default'),
        value: null
      })
      return options
    },
    hostSelectOptions () {
      const options = this.options.hosts.map((host) => {
        return {
          label: host.name,
          value: host.id
        }
      })
      options.unshift({
        label: this.$t('label.default'),
        value: null
      })
      return options
    },
    keyboardSelectOptions () {
      const keyboardOpts = this.$config.keyboardOptions || {}
      return Object.keys(keyboardOpts).map((keyboard) => {
        return {
          label: this.$t(keyboardOpts[keyboard]),
          value: keyboard
        }
      })
    },
    templateConfigurationExists () {
      return this.vm.templateid && this.templateConfigurations && this.templateConfigurations.length > 0
    },
    templateId () {
      return this.$route.query.templateid || null
    },
    isoId () {
      return this.$route.query.isoid || null
    },
    networkId () {
      return this.$route.query.networkid || null
    },
    tabList () {
      let tabList = []
      if (this.templateId) {
        tabList = [{
          key: 'templateid',
          tab: this.$t('label.templates')
        }]
      } else if (this.isoId) {
        tabList = [{
          key: 'isoid',
          tab: this.$t('label.isos')
        }]
      } else {
        tabList = [{
          key: 'templateid',
          tab: this.$t('label.templates')
        },
        {
          key: 'isoid',
          tab: this.$t('label.isos')
        }]
      }

      return tabList
    },
    userdataTabList () {
      let tabList = []
      tabList = [{
        key: 'userdataregistered',
        tab: this.$t('label.userdata.registered')
      },
      {
        key: 'userdatatext',
        tab: this.$t('label.userdata.text')
      }]

      return tabList
    },
    showSecurityGroupSection () {
      return (this.networks.length > 0 && this.zone.securitygroupsenabled) || (this.zone && this.zone.networktype === 'Basic')
    },
    isUserAllowedToListSshKeys () {
      return Boolean('listSSHKeyPairs' in this.$store.getters.apis)
    },
    isUserAllowedToListUserDatas () {
      return Boolean('listUserData' in this.$store.getters.apis)
    },
    dynamicScalingVmConfigValue () {
      return this.options.dynamicScalingVmConfig?.[0]?.value === 'true'
    },
    isCustomizedDiskIOPS () {
      return this.diskSelected?.iscustomizediops || false
    },
    isCustomizedIOPS () {
      return this.rootDiskSelected?.iscustomizediops || this.serviceOffering?.iscustomizediops || false
    }
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'deployVirtualMachine') {
        this.resetData()
      }
    },
    formModel: {
      deep: true,
      handler (instanceConfig) {
        this.instanceConfig = toRaw(instanceConfig)
        Object.keys(instanceConfig).forEach(field => {
          this.vm[field] = this.instanceConfig[field]
        })
        this.template = ''
        for (const key in this.options.templates) {
          var template = _.find(_.get(this.options.templates[key], 'template', []), (option) => option.id === instanceConfig.templateid)
          if (template) {
            this.template = template
            break
          }
        }

        this.iso = ''
        for (const key in this.options.isos) {
          var iso = _.find(_.get(this.options.isos[key], 'iso', []), (option) => option.id === instanceConfig.isoid)
          if (iso) {
            this.iso = iso
            break
          }
        }

        if (instanceConfig.hypervisor) {
          var hypervisorItem = _.find(this.options.hypervisors, (option) => option.name === instanceConfig.hypervisor)
          this.hypervisor = hypervisorItem ? hypervisorItem.name : null
        }

        this.serviceOffering = _.find(this.options.serviceOfferings, (option) => option.id === instanceConfig.computeofferingid)
        if (this.serviceOffering?.diskofferingid) {
          if (iso) {
            this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === this.serviceOffering.diskofferingid)
          } else {
            instanceConfig.overridediskofferingid = this.serviceOffering.diskofferingid
          }
        }
        if (!iso && this.diskSelected) {
          this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.diskofferingid)
        }
        if (this.rootDiskSelected?.id) {
          instanceConfig.overridediskofferingid = this.rootDiskSelected.id
        }
        if (instanceConfig.overridediskofferingid) {
          this.overrideDiskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.overridediskofferingid)
        } else {
          this.overrideDiskOffering = null
        }

        if (!iso && this.diskSelected) {
          this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.diskofferingid)
        }
        if (this.rootDiskSelected?.id) {
          instanceConfig.overridediskofferingid = this.rootDiskSelected.id
        }
        if (instanceConfig.overridediskofferingid) {
          this.overrideDiskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.overridediskofferingid)
        } else {
          this.overrideDiskOffering = null
        }
        this.zone = _.find(this.options.zones, (option) => option.id === instanceConfig.zoneid)
        this.affinityGroups = _.filter(this.options.affinityGroups, (option) => _.includes(instanceConfig.affinitygroupids, option.id))
        this.networks = this.getSelectedNetworksWithExistingConfig(_.filter(this.options.networks, (option) => _.includes(instanceConfig.networkids, option.id)))

        this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.diskofferingid)
        this.sshKeyPair = _.find(this.options.sshKeyPairs, (option) => option.name === instanceConfig.keypair)

        if (this.zone) {
          this.vm.zoneid = this.zone.id
          this.vm.zonename = this.zone.name
        }

        const pod = _.find(this.options.pods, (option) => option.id === instanceConfig.podid)
        if (pod) {
          this.vm.podid = pod.id
          this.vm.podname = pod.name
        }

        const cluster = _.find(this.options.clusters, (option) => option.id === instanceConfig.clusterid)
        if (cluster) {
          this.vm.clusterid = cluster.id
          this.vm.clustername = cluster.name
        }

        const host = _.find(this.options.hosts, (option) => option.id === instanceConfig.hostid)
        if (host) {
          this.vm.hostid = host.id
          this.vm.hostname = host.name
        }

        if (this.serviceOffering?.rootdisksize) {
          this.vm.disksizetotalgb = this.serviceOffering.rootdisksize
        } else if (this.diskSize) {
          this.vm.disksizetotalgb = this.diskSize
        } else {
          this.vm.disksizetotalgb = null
        }

        if (this.diskSize) {
          this.vm.disksizetotalgb = this.diskSize
        }

        if (this.networks) {
          this.vm.networks = this.networks
          this.vm.defaultnetworkid = this.defaultnetworkid
        }

        if (this.template) {
          this.vm.templateid = this.template.id
          this.vm.templatename = this.template.displaytext
          this.vm.ostypeid = this.template.ostypeid
          this.vm.ostypename = this.template.ostypename
        }

        if (this.iso) {
          this.vm.isoid = this.iso.id
          this.vm.templateid = this.iso.id
          this.vm.templatename = this.iso.displaytext
          this.vm.ostypeid = this.iso.ostypeid
          this.vm.ostypename = this.iso.ostypename
          if (this.hypervisor) {
            this.vm.hypervisor = this.hypervisor
          }
        }

        if (this.serviceOffering) {
          this.vm.serviceofferingid = this.serviceOffering.id
          this.vm.serviceofferingname = this.serviceOffering.displaytext
          if (this.serviceOffering.cpunumber) {
            this.vm.cpunumber = this.serviceOffering.cpunumber
          }
          if (this.serviceOffering.cpuspeed) {
            this.vm.cpuspeed = this.serviceOffering.cpuspeed
          }
          if (this.serviceOffering.memory) {
            this.vm.memory = this.serviceOffering.memory
          }
        }

        if (!this.template.deployasis && this.template.childtemplates && this.template.childtemplates.length > 0) {
          this.vm.diskofferingid = ''
          this.vm.diskofferingname = ''
          this.vm.diskofferingsize = ''
        } else if (this.diskOffering) {
          this.vm.diskofferingid = this.diskOffering.id
          this.vm.diskofferingname = this.diskOffering.displaytext
          this.vm.diskofferingsize = this.diskOffering.disksize
        }

        if (this.affinityGroups) {
          this.vm.affinitygroup = this.affinityGroups
        }

        if (this.sshKeyPairs && this.sshKeyPairs.length > 0) {
          this.vm.keypairs = this.sshKeyPairs
        }
      }
    }
  },
  serviceOffering (oldValue, newValue) {
    if (oldValue && newValue && oldValue.id !== newValue.id) {
      this.dynamicscalingenabled = this.isDynamicallyScalable()
    }
  },
  template (oldValue, newValue) {
    if (oldValue && newValue && oldValue.id !== newValue.id) {
      this.dynamicscalingenabled = this.isDynamicallyScalable()
      this.doUserdataOverride = false
      this.doUserdataAppend = false
    }
  },
  created () {
    this.initForm()
    this.dataPreFill = this.preFillContent && Object.keys(this.preFillContent).length > 0 ? this.preFillContent : {}
    this.fetchData()
  },
  provide () {
    return {
      vmFetchTemplates: this.fetchAllTemplates,
      vmFetchIsos: this.fetchAllIsos,
      vmFetchNetworks: this.fetchNetwork
    }
  },
  methods: {
    updateTemplateKey () {
      this.templateKey += 1
    },
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        zoneid: [{ required: true, message: `${this.$t('message.error.select')}` }],
        hypervisor: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })

      if (this.zoneSelected) {
        this.form.startvm = true
      }

      if (this.zone && this.zone.networktype !== 'Basic') {
        if (this.zoneSelected && this.vm.templateid && this.templateNics && this.templateNics.length > 0) {
          this.templateNics.forEach((nic, nicIndex) => {
            this.form['networkMap.nic-' + nic.InstanceID.toString()] = this.options.networks && this.options.networks.length > 0
              ? this.options.networks[Math.min(nicIndex, this.options.networks.length - 1)].id
              : null
          })
        }

        if (this.vm.templateid && this.templateProperties && Object.keys(this.templateProperties).length > 0) {
          this.templateProperties.forEach((props, category) => {
            props.forEach((property, propertyIndex) => {
              if (property.type && property.type === 'boolean') {
                this.form['properties.' + this.escapePropertyKey(property.key)] = property.value === 'TRUE'
              } else if (property.type && (property.type === 'int' || property.type === 'real')) {
                this.form['properties.' + this.escapePropertyKey(property.key)] = property.value
              } else if (property.type && property.type === 'string' && property.qualifiers && property.qualifiers.startsWith('ValueMap')) {
                this.form['properties.' + this.escapePropertyKey(property.key)] = property.value && property.value.length > 0
                  ? property.value
                  : this.getPropertyQualifiers(property.qualifiers, 'select')[0]
              } else if (property.type && property.type === 'string' && property.password) {
                this.form['properties.' + this.escapePropertyKey(property.key)] = property.value
                this.rules['properties.' + this.escapePropertyKey(property.key)] = [{
                  validator: async (rule, value) => {
                    if (!property.qualifiers) {
                      return Promise.resolve()
                    }
                    var minlength = this.getPropertyQualifiers(property.qualifiers, 'number-select').min
                    var maxlength = this.getPropertyQualifiers(property.qualifiers, 'number-select').max
                    var errorMessage = ''
                    var isPasswordInvalidLength = function () {
                      return false
                    }
                    if (minlength) {
                      errorMessage = this.$t('message.validate.minlength').replace('{0}', minlength)
                      isPasswordInvalidLength = function () {
                        return !value || value.length < minlength
                      }
                    }
                    if (maxlength !== Number.MAX_SAFE_INTEGER) {
                      if (minlength) {
                        errorMessage = this.$t('message.validate.range.length').replace('{0}', minlength).replace('{1}', maxlength)
                        isPasswordInvalidLength = function () {
                          return !value || (maxlength < value.length || value.length < minlength)
                        }
                      } else {
                        errorMessage = this.$t('message.validate.maxlength').replace('{0}', maxlength)
                        isPasswordInvalidLength = function () {
                          return !value || value.length > maxlength
                        }
                      }
                    }
                    if (isPasswordInvalidLength()) {
                      return Promise.reject(errorMessage)
                    }
                    return Promise.resolve()
                  }
                }]
              } else {
                this.form['properties.' + this.escapePropertyKey(property.key)] = property.value
              }
            })
          })
        }

        if (this.vm.templateid && this.templateLicenses && this.templateLicenses.length > 0) {
          this.rules.licensesaccepted = [{
            validator: async (rule, value) => {
              if (!value) {
                return Promise.reject(this.$t('message.license.agreements.not.accepted'))
              }
              return Promise.resolve()
            }
          }]
        }
      }
    },
    getPropertyQualifiers (qualifiers, type) {
      var result = ''
      switch (type) {
        case 'select':
          result = []
          if (qualifiers && qualifiers.includes('ValueMap')) {
            result = qualifiers.replace('ValueMap', '').substr(1).slice(0, -1).split(',')
            for (var i = 0; i < result.length; i++) {
              result[i] = result[i].replace(/"/g, '')
            }
          }
          break
        case 'number-select':
          var min = 0
          var max = Number.MAX_SAFE_INTEGER
          if (qualifiers) {
            var match = qualifiers.match(/MinLen\((\d+)\)/)
            if (match) {
              min = parseInt(match[1])
            }
            match = qualifiers.match(/MaxLen\((\d+)\)/)
            if (match) {
              max = parseInt(match[1])
            }
          }
          result = { min: min, max: max }
          break
        default:
      }
      return result
    },
    fillValue (field) {
      this.form[field] = this.dataPreFill[field]
    },
    fetchZoneByQuery () {
      return new Promise(resolve => {
        let zones = []
        let apiName = ''
        const params = {}
        if (this.templateId) {
          apiName = 'listTemplates'
          params.listall = true
          params.templatefilter = this.isNormalAndDomainUser ? 'executable' : 'all'
          params.id = this.templateId
        } else if (this.isoId) {
          params.listall = true
          params.isofilter = this.isNormalAndDomainUser ? 'executable' : 'all'
          params.id = this.isoId
          apiName = 'listIsos'
        } else if (this.networkId) {
          params.listall = true
          params.id = this.networkId
          apiName = 'listNetworks'
        }
        if (!apiName) return resolve(zones)

        api(apiName, params).then(json => {
          let objectName
          const responseName = [apiName.toLowerCase(), 'response'].join('')
          for (const key in json[responseName]) {
            if (key === 'count') {
              continue
            }
            objectName = key
            break
          }
          const data = json?.[responseName]?.[objectName] || []
          zones = data.map(item => item.zoneid)
          return resolve(zones)
        }).catch(() => {
          return resolve(zones)
        })
      })
    },
    async fetchData () {
      const zones = await this.fetchZoneByQuery()
      if (zones && zones.length === 1) {
        this.selectedZone = zones[0]
        this.dataPreFill.zoneid = zones[0]
      }
      if (this.dataPreFill.zoneid) {
        this.fetchDataByZone(this.dataPreFill.zoneid)
      } else {
        this.fetchZones(null, zones)
        _.each(this.params, (param, name) => {
          if (param.isLoad) {
            this.fetchOptions(param, name)
          }
        })
      }
      this.fetchBootTypes()
      this.fetchBootModes()
      this.fetchInstaceGroups()
      this.fetchIoPolicyTypes()
      nextTick().then(() => {
        ['name', 'keyboard', 'boottype', 'bootmode', 'userdata', 'iothreadsenabled', 'iodriverpolicy'].forEach(this.fillValue)
        this.form.boottype = this.defaultBootType ? this.defaultBootType : this.options.bootTypes && this.options.bootTypes.length > 0 ? this.options.bootTypes[0].id : undefined
        this.form.bootmode = this.defaultBootMode ? this.defaultBootMode : this.options.bootModes && this.options.bootModes.length > 0 ? this.options.bootModes[0].id : undefined
        this.instanceConfig = toRaw(this.form)
      })
    },
    isDynamicallyScalable () {
      return this.serviceOffering && this.serviceOffering.dynamicscalingenabled && this.template && this.template.isdynamicallyscalable && this.dynamicScalingVmConfigValue
    },
    isOfferingConstrained (serviceOffering) {
      return 'serviceofferingdetails' in serviceOffering && 'mincpunumber' in serviceOffering.serviceofferingdetails &&
        'maxmemory' in serviceOffering.serviceofferingdetails && 'maxcpunumber' in serviceOffering.serviceofferingdetails &&
        'minmemory' in serviceOffering.serviceofferingdetails
    },
    updateOverrideRootDiskShowParam (val) {
      if (val) {
        this.showRootDiskSizeChanger = false
      } else {
        this.rootDiskSelected = null
      }
      this.showOverrideDiskOfferingOption = val
    },
    async fetchDataByZone (zoneId) {
      this.fillValue('zoneid')
      this.options.zones = await this.fetchZones(zoneId)
      this.onSelectZoneId(zoneId)
    },
    fetchBootTypes () {
      this.options.bootTypes = [
        { id: 'BIOS', description: 'BIOS' },
        { id: 'UEFI', description: 'UEFI' }
      ]
    },
    fetchBootModes (bootType) {
      const bootModes = [
        { id: 'LEGACY', description: 'LEGACY' }
      ]
      if (bootType === 'UEFI') {
        bootModes.unshift(
          { id: 'SECURE', description: 'SECURE' }
        )
      }
      this.options.bootModes = bootModes
    },
    fetchIoPolicyTypes () {
      this.options.ioPolicyTypes = [
        { id: 'native', description: 'native' },
        { id: 'threads', description: 'threads' },
        { id: 'io_uring', description: 'io_uring' },
        { id: 'storage_specific', description: 'storage_specific' }
      ]
    },
    fetchInstaceGroups () {
      this.options.instanceGroups = []
      api('listInstanceGroups', {
        account: this.$store.getters.userInfo.account,
        domainid: this.$store.getters.userInfo.domainid,
        listall: true
      }).then(response => {
        const groups = response.listinstancegroupsresponse.instancegroup || []
        groups.forEach(x => {
          this.options.instanceGroups.push({ label: x.name, value: x.name })
        })
      })
    },
    fetchNetwork () {
      const param = this.params.networks
      this.fetchOptions(param, 'networks')
    },
    resetData () {
      this.vm = {
        name: null,
        zoneid: null,
        zonename: null,
        hypervisor: null,
        templateid: null,
        templatename: null,
        keyboard: null,
        keypair: null,
        group: null,
        affinitygroupids: [],
        affinitygroup: [],
        serviceofferingid: null,
        serviceofferingname: null,
        ostypeid: null,
        ostypename: null,
        rootdisksize: null,
        disksize: null
      }
      this.zoneSelected = false
      this.formRef.value.resetFields()
      this.fetchData()
    },
    updateFieldValue (name, value) {
      if (name === 'templateid') {
        this.tabKey = 'templateid'
        this.form.templateid = value
        this.form.isoid = null
        this.resetFromTemplateConfiguration()
        let template = ''
        for (const key in this.options.templates) {
          var t = _.find(_.get(this.options.templates[key], 'template', []), (option) => option.id === value)
          if (t) {
            this.template = t
            this.templateConfigurations = []
            this.selectedTemplateConfiguration = {}
            this.templateNics = []
            this.templateLicenses = []
            this.templateProperties = {}
            this.updateTemplateParameters()
            template = t
            break
          }
        }
        if (template) {
          var size = template.size / (1024 * 1024 * 1024) || 0 // bytes to GB
          this.dataPreFill.minrootdisksize = Math.ceil(size)
          this.defaultBootType = this.template?.details?.UEFI ? 'UEFI' : ''
          this.fetchBootModes(this.defaultBootType)
          this.defaultBootMode = this.template?.details?.UEFI
          this.updateTemplateLinkedUserData(this.template.userdataid)
          this.userdataDefaultOverridePolicy = this.template.userdatapolicy
        }
      } else if (name === 'isoid') {
        this.templateConfigurations = []
        this.selectedTemplateConfiguration = {}
        this.templateNics = []
        this.templateLicenses = []
        this.templateProperties = {}
        this.tabKey = 'isoid'
        this.resetFromTemplateConfiguration()
        this.form.isoid = value
        this.form.templateid = null
        this.updateTemplateLinkedUserData(this.iso.userdataid)
        this.userdataDefaultOverridePolicy = this.iso.userdatapolicy
      } else if (['cpuspeed', 'cpunumber', 'memory'].includes(name)) {
        this.vm[name] = value
        this.form[name] = value
      } else {
        this.form[name] = value
      }
    },
    updateComputeOffering (id) {
      this.form.computeofferingid = id
      setTimeout(() => {
        this.updateTemplateConfigurationOfferingDetails(id)
      }, 500)
    },
    updateDiskOffering (id) {
      if (id === '0') {
        this.form.diskofferingid = undefined
        return
      }
      this.form.diskofferingid = id
    },
    updateOverrideDiskOffering (id) {
      if (id === '0') {
        this.form.overridediskofferingid = undefined
        return
      }
      this.form.overridediskofferingid = id
    },
    updateMultiDiskOffering (value) {
      this.form.multidiskoffering = value
    },
    updateAffinityGroups (ids) {
      this.form.affinitygroupids = ids
    },
    updateNetworks (ids) {
      this.form.networkids = ids
    },
    updateDefaultNetworks (id) {
      this.defaultnetworkid = id
      this.form.defaultnetworkid = id
    },
    updateNetworkConfig (networks) {
      this.networkConfig = networks
    },
    updateSshKeyPairs (names) {
      this.form.keypairs = names
      this.sshKeyPairs = names.map((sshKeyPair) => { return sshKeyPair.name })
    },
    updateUserData (id) {
      if (id === '0') {
        this.form.userdataid = undefined
        return
      }
      this.form.userdataid = id
      this.userDataParams = []
      api('listUserData', { id: id }).then(json => {
        const resp = json?.listuserdataresponse?.userdata || []
        if (resp) {
          var params = resp[0].params
          if (params) {
            var dataParams = params.split(',')
          }
          var that = this
          dataParams.forEach(function (val, index) {
            that.userDataParams.push({
              id: index,
              key: val
            })
          })
        }
      })
    },
    updateTemplateLinkedUserData (id) {
      if (id === '0') {
        return
      }
      this.templateUserDataParams = []

      api('listUserData', { id: id }).then(json => {
        const resp = json.listuserdataresponse.userdata || []
        if (resp.length > 0) {
          var params = resp[0].params
          if (params) {
            var dataParams = params.split(',')
          }
          var that = this
          that.templateUserDataParams = []
          if (dataParams) {
            dataParams.forEach(function (val, index) {
              that.templateUserDataParams.push({
                id: index,
                key: val
              })
            })
          }
        }
      })
    },
    escapePropertyKey (key) {
      return key.split('.').join('\\002E')
    },
    updateSecurityGroups (securitygroupids) {
      this.securitygroupids = securitygroupids || []
    },
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    handleSubmitAndStay (e) {
      this.form.stayonpage = true
      this.handleSubmit(e.domEvent)
    },
    handleSubmit (e) {
      console.log('wizard submit')
      e.preventDefault()
      if (this.loading.deploy) return
      this.formRef.value.validate().then(async () => {
        const values = toRaw(this.form)

        if (!values.templateid && !values.isoid) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.template.iso')
          })
          return
        } else if (values.isoid && (!values.diskofferingid || values.diskofferingid === '0')) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.step.3.continue')
          })
          return
        }
        if (!values.computeofferingid) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.step.2.continue')
          })
          return
        }
        if (this.error) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('error.form.message')
          })
          return
        }

        this.loading.deploy = true

        let networkIds = []

        let deployVmData = {}
        // step 1 : select zone
        deployVmData.zoneid = values.zoneid
        deployVmData.podid = values.podid
        deployVmData.clusterid = values.clusterid
        deployVmData.hostid = values.hostid
        deployVmData.keyboard = values.keyboard
        if (!this.template?.deployasis) {
          deployVmData.boottype = values.boottype
          deployVmData.bootmode = values.bootmode
        }
        deployVmData.dynamicscalingenabled = values.dynamicscalingenabled
        deployVmData.iothreadsenabled = values.iothreadsenabled
        deployVmData.iodriverpolicy = values.iodriverpolicy
        if (values.userdata && values.userdata.length > 0) {
          deployVmData.userdata = encodeURIComponent(btoa(sanitizeReverse(values.userdata)))
        }
        // step 2: select template/iso
        if (this.tabKey === 'templateid') {
          deployVmData.templateid = values.templateid
          values.hypervisor = null
        } else {
          deployVmData.templateid = values.isoid
        }

        if (this.showRootDiskSizeChanger && values.rootdisksize && values.rootdisksize > 0) {
          deployVmData.rootdisksize = values.rootdisksize
        } else if (this.rootDiskSizeFixed > 0 && !this.template.deployasis) {
          deployVmData.rootdisksize = this.rootDiskSizeFixed
        }

        if (values.hypervisor && values.hypervisor.length > 0) {
          deployVmData.hypervisor = values.hypervisor
        }

        deployVmData.startvm = values.startvm

        // step 3: select service offering
        deployVmData.serviceofferingid = values.computeofferingid
        if (this.serviceOffering && this.serviceOffering.iscustomized) {
          if (values.cpunumber) {
            deployVmData['details[0].cpuNumber'] = values.cpunumber
          }
          if (values.cpuspeed) {
            deployVmData['details[0].cpuSpeed'] = values.cpuspeed
          }
          if (values.memory) {
            deployVmData['details[0].memory'] = values.memory
          }
        }
        if (this.selectedTemplateConfiguration) {
          deployVmData['details[0].configurationId'] = this.selectedTemplateConfiguration.id
        }
        if (!this.serviceOffering.diskofferingstrictness && values.overridediskofferingid) {
          deployVmData.overridediskofferingid = values.overridediskofferingid
          if (values.rootdisksize && values.rootdisksize > 0) {
            deployVmData.rootdisksize = values.rootdisksize
          }
        }
        if (this.isCustomizedIOPS) {
          deployVmData['details[0].minIops'] = this.minIops
          deployVmData['details[0].maxIops'] = this.maxIops
        }
        // step 4: select disk offering
        if (!this.template.deployasis && this.template.childtemplates && this.template.childtemplates.length > 0) {
          if (values.multidiskoffering) {
            let i = 0
            Object.entries(values.multidiskoffering).forEach(([disk, offering]) => {
              const diskKey = `datadiskofferinglist[${i}].datadisktemplateid`
              const offeringKey = `datadiskofferinglist[${i}].diskofferingid`
              deployVmData[diskKey] = disk
              deployVmData[offeringKey] = offering
              i++
            })
          }
        } else {
          deployVmData.diskofferingid = values.diskofferingid
          if (values.size) {
            deployVmData.size = values.size
          }
        }
        if (this.isCustomizedDiskIOPS) {
          deployVmData['details[0].minIopsDo'] = this.diskIOpsMin
          deployVmData['details[0].maxIopsDo'] = this.diskIOpsMax
        }
        // step 5: select an affinity group
        deployVmData.affinitygroupids = (values.affinitygroupids || []).join(',')
        // step 6: select network
        if (this.zone.networktype !== 'Basic') {
          if (this.nicToNetworkSelection && this.nicToNetworkSelection.length > 0) {
            for (var j in this.nicToNetworkSelection) {
              var nicNetwork = this.nicToNetworkSelection[j]
              deployVmData['nicnetworklist[' + j + '].nic'] = nicNetwork.nic
              deployVmData['nicnetworklist[' + j + '].network'] = nicNetwork.network
            }
          } else {
            const arrNetwork = []
            networkIds = values.networkids
            if (networkIds.length > 0) {
              for (let i = 0; i < networkIds.length; i++) {
                if (networkIds[i] === this.defaultnetworkid) {
                  const ipToNetwork = {
                    networkid: this.defaultnetworkid
                  }
                  arrNetwork.unshift(ipToNetwork)
                } else {
                  const ipToNetwork = {
                    networkid: networkIds[i]
                  }
                  arrNetwork.push(ipToNetwork)
                }
              }
            } else {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.step.4.continue')
              })
              this.loading.deploy = false
              return
            }
            for (let j = 0; j < arrNetwork.length; j++) {
              deployVmData['iptonetworklist[' + j + '].networkid'] = arrNetwork[j].networkid
              if (this.networkConfig.length > 0) {
                const networkConfig = this.networkConfig.filter((item) => item.key === arrNetwork[j].networkid)
                if (networkConfig && networkConfig.length > 0) {
                  deployVmData['iptonetworklist[' + j + '].ip'] = networkConfig[0].ipAddress ? networkConfig[0].ipAddress : undefined
                  deployVmData['iptonetworklist[' + j + '].mac'] = networkConfig[0].macAddress ? networkConfig[0].macAddress : undefined
                }
              }
            }
          }
        }
        if (this.securitygroupids.length > 0) {
          deployVmData.securitygroupids = this.securitygroupids.join(',')
        }
        // step 7: select ssh key pair
        deployVmData.keypairs = this.sshKeyPairs.join(',')
        deployVmData.userdataid = values.userdataid

        if (values.name) {
          deployVmData.name = values.name
          deployVmData.displayname = values.name
        }
        if (values.group) {
          deployVmData.group = values.group
        }
        // step 8: enter setup
        if ('properties' in values) {
          const keys = Object.keys(values.properties)
          for (var i = 0; i < keys.length; ++i) {
            const propKey = keys[i].split('\\002E').join('.')
            deployVmData['properties[' + i + '].key'] = propKey
            deployVmData['properties[' + i + '].value'] = values.properties[keys[i]]
          }
        }
        if ('bootintosetup' in values) {
          deployVmData.bootintosetup = values.bootintosetup
        }

        const title = this.$t('label.launch.vm')
        const description = values.name || ''
        const password = this.$t('label.password')

        deployVmData = Object.fromEntries(
          Object.entries(deployVmData).filter(([key, value]) => value !== undefined))

        var idx = 0
        if (this.templateUserDataValues) {
          for (const [key, value] of Object.entries(this.templateUserDataValues)) {
            deployVmData['userdatadetails[' + idx + '].' + `${key}`] = value
            idx++
          }
        }
        if (this.userDataValues) {
          for (const [key, value] of Object.entries(this.userDataValues)) {
            deployVmData['userdatadetails[' + idx + '].' + `${key}`] = value
            idx++
          }
        }

        const httpMethod = deployVmData.userdata ? 'POST' : 'GET'
        const args = httpMethod === 'POST' ? {} : deployVmData
        const data = httpMethod === 'POST' ? deployVmData : {}

        api('deployVirtualMachine', args, httpMethod, data).then(response => {
          const jobId = response.deployvirtualmachineresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description,
              successMethod: result => {
                const vm = result.jobresult.virtualmachine
                const name = vm.displayname || vm.name || vm.id
                if (vm.password) {
                  this.$notification.success({
                    message: password + ` ${this.$t('label.for')} ` + name,
                    description: vm.password,
                    duration: 0
                  })
                }
                eventBus.emit('vm-refresh-data')
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')}`,
              catchMessage: this.$t('error.fetching.async.job.result'),
              action: {
                isFetchData: false
              }
            })
          }
          // Sending a refresh in case it hasn't picked up the new VM
          new Promise(resolve => setTimeout(resolve, 3000)).then(() => {
            eventBus.emit('vm-refresh-data')
          })
          if (!values.stayonpage) {
            this.$router.back()
          }
        }).catch(error => {
          this.$notifyError(error)
          this.loading.deploy = false
        }).finally(() => {
          this.form.stayonpage = false
          this.loading.deploy = false
        })
      }).catch(err => {
        this.formRef.value.scrollToField(err.errorFields[0].name)
        if (err) {
          if (err.licensesaccepted) {
            this.$notification.error({
              message: this.$t('message.license.agreements.not.accepted'),
              description: this.$t('message.step.license.agreements.continue')
            })
            return
          }

          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('error.form.message')
          })
        }
      })
    },
    fetchZones (zoneId, listZoneAllow) {
      this.zones = []
      return new Promise((resolve) => {
        this.loading.zones = true
        const param = this.params.zones
        const args = { showicon: true }
        if (zoneId) args.id = zoneId
        api(param.list, args).then(json => {
          const zoneResponse = json.listzonesresponse.zone || []
          if (listZoneAllow && listZoneAllow.length > 0) {
            zoneResponse.map(zone => {
              if (listZoneAllow.includes(zone.id)) {
                this.zones.push(zone)
              }
            })
          } else {
            this.zones = zoneResponse
          }

          resolve(this.zones)
        }).catch(function (error) {
          console.log(error.stack)
        }).finally(() => {
          this.loading.zones = false
        })
      })
    },
    fetchOptions (param, name, exclude) {
      if (exclude && exclude.length > 0) {
        if (exclude.includes(name)) {
          return
        }
      }
      this.loading[name] = true
      param.loading = true
      param.opts = []
      const options = param.options || {}
      if (!('listall' in options) && !['zones', 'pods', 'clusters', 'hosts', 'dynamicScalingVmConfig', 'hypervisors'].includes(name)) {
        options.listall = true
      }
      api(param.list, options).then((response) => {
        param.loading = false
        _.map(response, (responseItem, responseKey) => {
          if (Object.keys(responseItem).length === 0) {
            this.rowCount[name] = 0
            this.options[name] = []
            return
          }
          if (!responseKey.includes('response')) {
            return
          }
          _.map(responseItem, (response, key) => {
            if (key === 'count') {
              this.rowCount[name] = response
              return
            }
            param.opts = response
            this.options[name] = response

            if (name === 'hypervisors') {
              const hypervisorFromResponse = response[0] && response[0].name ? response[0].name : null
              this.dataPreFill.hypervisor = hypervisorFromResponse
              this.form.hypervisor = hypervisorFromResponse
            }

            if (param.field) {
              this.fillValue(param.field)
            }
          })

          if (name === 'zones') {
            let zoneid = ''
            if (this.$route.query.zoneid) {
              zoneid = this.$route.query.zoneid
            } else if (this.options.zones.length === 1) {
              zoneid = this.options.zones[0].id
            }
            if (zoneid) {
              this.form.zoneid = zoneid
              this.onSelectZoneId(zoneid)
            }
          }
        })
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      }).finally(() => {
        this.loading[name] = false
      })
    },
    fetchTemplates (templateFilter, params) {
      const args = Object.assign({}, params)
      if (args.keyword || args.category !== templateFilter) {
        args.page = 1
        args.pageSize = args.pageSize || 10
      }
      args.zoneid = _.get(this.zone, 'id')
      args.templatefilter = templateFilter
      args.details = 'all'
      args.showicon = 'true'
      args.id = this.templateId

      return new Promise((resolve, reject) => {
        api('listTemplates', args).then((response) => {
          resolve(response)
        }).catch((reason) => {
          // ToDo: Handle errors
          reject(reason)
        })
      })
    },
    fetchIsos (isoFilter, params) {
      const args = Object.assign({}, params)
      if (args.keyword || args.category !== isoFilter) {
        args.page = 1
        args.pageSize = args.pageSize || 10
      }
      args.zoneid = _.get(this.zone, 'id')
      args.isoFilter = isoFilter
      args.bootable = true
      args.showicon = 'true'
      args.id = this.isoId

      return new Promise((resolve, reject) => {
        api('listIsos', args).then((response) => {
          resolve(response)
        }).catch((reason) => {
          // ToDo: Handle errors
          reject(reason)
        })
      })
    },
    fetchAllTemplates (params) {
      const promises = []
      const templates = {}
      this.loading.templates = true
      this.templateFilter.forEach((filter) => {
        templates[filter] = { count: 0, template: [] }
        promises.push(this.fetchTemplates(filter, params))
      })
      this.options.templates = templates
      Promise.all(promises).then((response) => {
        response.forEach((resItem, idx) => {
          templates[this.templateFilter[idx]] = _.isEmpty(resItem.listtemplatesresponse) ? { count: 0, template: [] } : resItem.listtemplatesresponse
          this.options.templates = { ...templates }
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.templates = false
      })
    },
    fetchAllIsos (params) {
      const promises = []
      const isos = {}
      this.loading.isos = true
      this.isoFilter.forEach((filter) => {
        isos[filter] = { count: 0, iso: [] }
        promises.push(this.fetchIsos(filter, params))
      })
      this.options.isos = isos
      Promise.all(promises).then((response) => {
        response.forEach((resItem, idx) => {
          isos[this.isoFilter[idx]] = _.isEmpty(resItem.listisosresponse) ? { count: 0, iso: [] } : resItem.listisosresponse
          this.options.isos = { ...isos }
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.isos = false
      })
    },
    filterOption (input, option) {
      return option.label.toUpperCase().indexOf(input.toUpperCase()) >= 0
    },
    onSelectZoneId (value) {
      this.dataPreFill = {}
      this.zoneId = value
      this.podId = null
      this.clusterId = null
      this.zone = _.find(this.options.zones, (option) => option.id === value)
      this.zoneSelected = true
      this.form.startvm = true
      this.selectedZone = this.zoneId
      this.form.zoneid = this.zoneId
      this.form.clusterid = undefined
      this.form.podid = undefined
      this.form.hostid = undefined
      this.form.templateid = undefined
      this.form.isoid = undefined
      this.tabKey = 'templateid'
      if (this.isoId) {
        this.tabKey = 'isoid'
      }
      _.each(this.params, (param, name) => {
        if (this.networkId && name === 'networks') {
          param.options = {
            id: this.networkId
          }
        }
        if (!('isLoad' in param) || param.isLoad) {
          this.fetchOptions(param, name, ['zones'])
        }
      })
      if (this.tabKey === 'templateid') {
        this.fetchAllTemplates()
      } else {
        this.fetchAllIsos()
      }
      this.updateTemplateKey()
      this.formModel = toRaw(this.form)
    },
    onSelectPodId (value) {
      this.podId = value
      if (this.podId === null) {
        this.form.podid = undefined
      }

      this.fetchOptions(this.params.clusters, 'clusters')
      this.fetchOptions(this.params.hosts, 'hosts')
    },
    onSelectClusterId (value) {
      this.clusterId = value
      if (this.clusterId === null) {
        this.form.clusterid = undefined
      }

      this.fetchOptions(this.params.hosts, 'hosts')
    },
    onSelectHostId (value) {
      this.hostId = value
      if (this.hostId === null) {
        this.form.hostid = undefined
      }
    },
    handleSearchFilter (name, options) {
      this.params[name].options = { ...this.params[name].options, ...options }
      this.fetchOptions(this.params[name], name)
    },
    onTabChange (key, type) {
      this[type] = key
      if (key === 'isoid') {
        this.fetchAllIsos()
      }
    },
    onUserdataTabChange (key, type) {
      this[type] = key
      this.userDataParams = []
    },
    fetchTemplateNics (template) {
      var nics = []
      this.nicToNetworkSelection = []
      if (template && template.deployasisdetails && Object.keys(template.deployasisdetails).length > 0) {
        var keys = Object.keys(template.deployasisdetails)
        keys = keys.filter(key => key.startsWith('network-'))
        for (var key of keys) {
          var propertyMap = JSON.parse(template.deployasisdetails[key])
          nics.push(propertyMap)
        }
        nics.sort(function (a, b) {
          return a.InstanceID - b.InstanceID
        })
        if (this.options.networks && this.options.networks.length > 0) {
          for (var i = 0; i < nics.length; ++i) {
            var nic = nics[i]
            nic.id = nic.InstanceID
            var network = this.options.networks[Math.min(i, this.options.networks.length - 1)]
            nic.selectednetworkid = network.id
            nic.selectednetworkname = network.name
            this.nicToNetworkSelection.push({ nic: nic.id, network: network.id })
          }
        }
      }
      return nics
    },
    groupBy (array, key) {
      const result = {}
      array.forEach(item => {
        if (!result[item[key]]) {
          result[item[key]] = []
        }
        result[item[key]].push(item)
      })
      return result
    },
    fetchTemplateProperties (template) {
      var properties = []
      if (template && template.deployasisdetails && Object.keys(template.deployasisdetails).length > 0) {
        var keys = Object.keys(template.deployasisdetails)
        keys = keys.filter(key => key.startsWith('property-'))
        for (var key of keys) {
          var propertyMap = JSON.parse(template.deployasisdetails[key])
          properties.push(propertyMap)
        }
        properties.sort(function (a, b) {
          return a.index - b.index
        })
      }
      return this.groupBy(properties, 'category')
    },
    fetchTemplateConfigurations (template) {
      var configurations = []
      if (template && template.deployasisdetails && Object.keys(template.deployasisdetails).length > 0) {
        var keys = Object.keys(template.deployasisdetails)
        keys = keys.filter(key => key.startsWith('configuration-'))
        for (var key of keys) {
          var configuration = JSON.parse(template.deployasisdetails[key])
          configuration.name = configuration.label
          configuration.displaytext = configuration.label
          configuration.iscustomized = true
          configuration.cpunumber = 0
          configuration.cpuspeed = 0
          configuration.memory = 0
          for (var harwareItem of configuration.hardwareItems) {
            if (harwareItem.resourceType === 'Processor') {
              configuration.cpunumber = harwareItem.virtualQuantity
              configuration.cpuspeed = harwareItem.reservation
            } else if (harwareItem.resourceType === 'Memory') {
              configuration.memory = harwareItem.virtualQuantity
            }
          }
          configurations.push(configuration)
        }
        configurations.sort(function (a, b) {
          return a.index - b.index
        })
      }
      return configurations
    },
    fetchTemplateLicenses (template) {
      var licenses = []
      if (template && template.deployasisdetails && Object.keys(template.deployasisdetails).length > 0) {
        var keys = Object.keys(template.deployasisdetails)
        const prefix = /eula-\d-/
        keys = keys.filter(key => key.startsWith('eula-')).sort()
        for (var key of keys) {
          var license = {
            id: this.escapePropertyKey(key.replace(' ', '-')),
            name: key.replace(prefix, ''),
            text: template.deployasisdetails[key]
          }
          licenses.push(license)
        }
      }
      return licenses
    },
    deleteFrom (options, values) {
      for (const value of values) {
        delete options[value]
      }
    },
    resetFromTemplateConfiguration () {
      this.deleteFrom(this.params.serviceOfferings.options, ['cpuspeed', 'cpunumber', 'memory'])
      this.deleteFrom(this.dataPreFill, ['cpuspeed', 'cpunumber', 'memory'])
      this.handleSearchFilter('serviceOfferings', {
        page: 1,
        pageSize: 10
      })
    },
    handleTemplateConfiguration () {
      if (!this.selectedTemplateConfiguration) {
        return
      }
      const params = {
        cpunumber: this.selectedTemplateConfiguration.cpunumber,
        cpuspeed: this.selectedTemplateConfiguration.cpuspeed,
        memory: this.selectedTemplateConfiguration.memory,
        page: 1,
        pageSize: 10
      }
      this.dataPreFill.cpunumber = params.cpunumber
      this.dataPreFill.cpuspeed = params.cpuspeed
      this.dataPreFill.memory = params.memory
      this.handleSearchFilter('serviceOfferings', params)
    },
    updateTemplateParameters () {
      if (this.template) {
        this.templateNics = this.fetchTemplateNics(this.template)
        this.templateConfigurations = this.fetchTemplateConfigurations(this.template)
        this.templateLicenses = this.fetchTemplateLicenses(this.template)
        this.templateProperties = this.fetchTemplateProperties(this.template)
        this.selectedTemplateConfiguration = {}
        setTimeout(() => {
          if (this.templateConfigurationExists) {
            this.selectedTemplateConfiguration = this.templateConfigurations[0]
            this.handleTemplateConfiguration()
            if ('templateConfiguration' in this.form.fieldsStore.fieldsMeta) {
              this.updateFieldValue('templateConfiguration', this.selectedTemplateConfiguration.id)
            }
            this.updateComputeOffering(null) // reset as existing selection may be incompatible
          }
        }, 500)
      }
    },
    onSelectTemplateConfigurationId (value) {
      this.selectedTemplateConfiguration = _.find(this.templateConfigurations, (option) => option.id === value)
      this.handleTemplateConfiguration()
      this.updateComputeOffering(null)
    },
    updateTemplateConfigurationOfferingDetails (offeringId) {
      this.rootDiskSizeFixed = 0
      var offering = this.serviceOffering
      if (!offering || offering.id !== offeringId) {
        offering = _.find(this.options.serviceOfferings, (option) => option.id === offeringId)
      }
      if (offering && offering.iscustomized && this.templateConfigurationExists && this.selectedTemplateConfiguration) {
        if ('cpunumber' in this.form.fieldsStore.fieldsMeta) {
          this.updateFieldValue('cpunumber', this.selectedTemplateConfiguration.cpunumber)
        }
        if ((offering.cpuspeed == null || offering.cpuspeed === undefined) && 'cpuspeed' in this.form.fieldsStore.fieldsMeta) {
          this.updateFieldValue('cpuspeed', this.selectedTemplateConfiguration.cpuspeed)
        }
        if ('memory' in this.form.fieldsStore.fieldsMeta) {
          this.updateFieldValue('memory', this.selectedTemplateConfiguration.memory)
        }
      }
      if (offering && offering.rootdisksize > 0) {
        this.rootDiskSizeFixed = offering.rootdisksize
        this.showRootDiskSizeChanger = false
      }
      this.form.rootdisksizeitem = this.showRootDiskSizeChanger && this.rootDiskSizeFixed > 0
      this.formModel = toRaw(this.form)
    },
    handlerError (error) {
      this.error = error
    },
    onSelectDiskSize (rowSelected) {
      this.diskSelected = rowSelected
    },
    onSelectRootDiskSize (rowSelected) {
      this.rootDiskSelected = rowSelected
    },
    updateIOPSValue (input, value) {
      this[input] = value
    },
    onBootTypeChange (value) {
      this.fetchBootModes(value)
      this.defaultBootMode = this.options.bootModes?.[0]?.id || undefined
      this.updateFieldValue('bootmode', this.defaultBootMode)
    },
    handleNicsNetworkSelection (nicToNetworkSelection) {
      this.nicToNetworkSelection = nicToNetworkSelection
    },
    getSelectedNetworksWithExistingConfig (networks) {
      for (var i in this.networks) {
        var n = this.networks[i]
        for (var c of this.networkConfig) {
          if (n.id === c.key) {
            n = { ...n, ...c }
            networks[i] = n
            break
          }
        }
      }
      return networks
    }
  }
}
</script>

<style lang="less" scoped>
  .card-footer {
    text-align: right;
    margin-top: 2rem;

    button + button {
      margin-left: 8px;
    }
  }

  .ant-list-item-meta-avatar {
    font-size: 1rem;
  }

  .ant-collapse {
    margin: 2rem 0;
  }
</style>

<style lang="less">
  @import url('../../style/index');

  .ant-table-selection-column {
    // Fix for the table header if the row selection use radio buttons instead of checkboxes
    > div:empty {
      width: 16px;
    }
  }

  .ant-collapse-borderless > .ant-collapse-item {
    border: 1px solid @border-color-split;
    border-radius: @border-radius-base !important;
    margin: 0 0 1.2rem;
  }

  .vm-info-card {
    .ant-card-body {
      min-height: 250px;
      max-height: calc(100vh - 150px);
      overflow-y: auto;
      scroll-behavior: smooth;
    }

    .resource-detail-item__label {
      font-weight: normal;
    }

    .resource-detail-item__details, .resource-detail-item {
      a {
        color: rgba(0, 0, 0, 0.65);
        cursor: default;
        pointer-events: none;
      }
    }
  }

  .form-item-hidden {
    display: none;
  }

  .btn-stay-on-page {
    &.ant-dropdown-menu-dark {
      .ant-dropdown-menu-item:hover {
        background: transparent !important;
      }
    }
  }
</style>
