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
        <a-card :bordered="true" :title="$t('label.new.autoscale.vmgroup')">
          <a-form
            v-ctrl-enter="handleSubmit"
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleSubmit"
            layout="vertical"
          >
            <a-steps direction="vertical" size="small">
              <a-step :title="$t('label.select.a.zone')" status="process">
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
                                      <global-outlined v-else />
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
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.template')"
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
                    </a-card>
                    <a-form-item class="form-item-hidden">
                      <a-input v-model:value="form.templateid" />
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
                      :autoscale="true"
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
                      :computeOfferingId="vmgroupConfig.computeofferingid"
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
                    <span>
                      {{ $t('label.override.root.diskoffering') }}
                      <a-switch
                        v-model:checked="showOverrideDiskOfferingOption"
                        :checked="serviceOffering && !serviceOffering.diskofferingstrictness && showOverrideDiskOfferingOption"
                        :disabled="(serviceOffering && serviceOffering.diskofferingstrictness)"
                        @change="val => { updateOverrideRootDiskShowParam(val) }"
                        style="margin-left: 10px;"/>
                    </span>
                    <span v-if="serviceOffering && !serviceOffering.diskofferingstrictness">
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
                    <span>
                      {{ $t('label.data.disk') }}
                      <a-switch
                        v-model:checked="showDiskOfferingOption"
                        @change="updateDiskOffering(0)"
                        style="margin-left: 10px;"/>
                    </span>
                    <disk-offering-selection
                      v-if="showDiskOfferingOption"
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
                :title="$t('label.select.network')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="zone && zone.networktype !== 'Basic'">
                <template #description>
                  <label>{{ $t('message.autoscale.vm.networks') }}</label>
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
                        :autoscale="true"
                        :preFillContent="dataPreFill"
                        @handler-error="($event) => hasError = $event"
                        @select-default-network-item="($event) => updateDefaultNetworks($event)"
                      ></network-configuration>
                    </div>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.loadbalancing')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="networks.length > 0">
                <template #description>
                  <load-balancer-selection
                    :items="options.loadbalancers"
                    :zoneId="zoneId"
                    :loading="loading.networks"
                    :preFillContent="dataPreFill"
                    @select-load-balancer-item="($event) => updateLoadBalancers($event)"
                    @handle-search-filter="($event) => handleSearchFilter('loadbalancers', $event)"
                  ></load-balancer-selection>
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
                            v-model:cheked="form['properties.' + escapePropertyKey(property.key)]"
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
                :title="$t('label.scaleup.policies')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <label>{{ $t('message.scaleup.policies') }}</label>
                  <a-divider/>
                  <div class="form">
                    <strong>{{ $t('label.scaleup.policy') }} &nbsp;&nbsp;&nbsp;</strong>
                    <a-select
                      style="width: 320px"
                      v-model:value="selectedScaleUpPolicyId"
                      @change="fetchScaleUpConditions()"
                      :placeholder="$t('label.scaleup.policy')"
                      showSearch
                      optionFilterProp="label"
                      :filterOption="(input, option) => {
                        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                      }" >
                      <a-select-option
                        v-for="policy in this.scaleUpPolicies"
                        :key="policy.id"
                        :label="policy.name">
                        {{ policy.name }}
                      </a-select-option>
                    </a-select>
                    <a-button style="margin-left: 10px" ref="submit" type="primary" @click="addNewScaleUpPolicy">
                      <template #icon><plus-outlined /></template>
                      {{ $t('label.add.policy') }}
                    </a-button>
                    <a-button style="margin-left: 10px" ref="submit" type="primary" @click="removeScaleUpPolicy" :danger="true" >
                      <template #icon><delete-outlined /></template>
                      {{ $t('label.remove.policy') }}
                    </a-button>
                  </div>
                  <div class="form">
                    <a-form-item>
                      <template #label>
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.name')" :tooltip="createAutoScalePolicyApiParams.name.description"/>
                      </template>
                      <a-input v-model:value="selectedScaleUpPolicy.name"></a-input>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </a-form-item>
                    <a-form-item name="scaleupduration" ref="scaleupduration">
                      <template #label>
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.duration')" :tooltip="createAutoScalePolicyApiParams.duration.description"/>
                      </template>
                      <a-input v-model:value="selectedScaleUpPolicy.scaleupduration" type="number"></a-input>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </a-form-item>
                    <a-form-item name="scaleupquiettime" ref="scaleupquiettime">
                      <template #label>
                        <tooltip-label :title="$t('label.quiettime')" :tooltip="createAutoScalePolicyApiParams.quiettime.description"/>
                      </template>
                      <a-input v-model:value="selectedScaleUpPolicy.scaleupquiettime" type="number"></a-input>
                    </a-form-item>
                  </div>
                  <a-divider/>
                  <div class="form">
                    <div class="form__item" ref="newScaleUpConditionCounterId">
                      <div class="form__label">
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.counter')" :tooltip="$t('label.counter.name')"/>
                      </div>
                      <a-select
                        style="width: 100%"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        v-focus="true"
                        v-model:value="newScaleUpCondition.counterid">
                        <a-select-option
                          v-for="(counter, index) in countersList"
                          :value="counter.id"
                          :key="index"
                          :label="counter.name">
                          {{ counter.name }}
                        </a-select-option>
                      </a-select>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </div>
                    <div class="form__item" ref="newScaleUpConditionRelationalOperator">
                      <div class="form__label">
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.relationaloperator')" :tooltip="createConditionApiParams.relationaloperator.description"/>
                      </div>
                      <a-select
                        v-model:value="newScaleUpCondition.relationaloperator"
                        style="width: 100%;"
                        optionFilterProp="value"
                        :filterOption="(input, option) => {
                          return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }" >
                        <a-select-option value="GT">{{ getOperator('GT') }}</a-select-option>
                      </a-select>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </div>
                    <div class="form__item" ref="newScaleUpConditionThreshold">
                      <div class="form__label">
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.threshold')" :tooltip="$t('label.threshold.description')"/>
                      </div>
                      <a-input v-model:value="newScaleUpCondition.threshold" type="number"></a-input>
                      <span class="error-text">{{ $t('label.invalid.number') }}</span>
                    </div>
                    <div class="form__item">
                      <div class="form__label">{{ $t('label.action') }}</div>
                      <a-button ref="submit" type="primary" @click="addScaleUpCondition">
                        <template #icon><plus-outlined /></template>
                        {{ $t('label.add.condition') }}
                      </a-button>
                    </div>
                  </div>
                  <a-divider/>
                  <div>
                    <a-table
                      size="small"
                      style="overflow-y: auto"
                      :loading="false"
                      :columns="scaleUpColumns"
                      :dataSource="scaleUpConditions"
                      :pagination="false"
                      :rowKey="record => record.counterid">
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'relationaloperator'">
                          {{ getOperator(record.relationaloperator) }}
                        </template>
                        <template v-if="column.key === 'actions'">
                          <a-button ref="submit" type="primary" :danger="true" @click="deleteScaleUpCondition(record.counterid)">
                            <template #icon><delete-outlined /></template>
                            {{ $t('label.delete') }}
                          </a-button>
                        </template>
                      </template>
                    </a-table>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.scaledown.policies')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description>
                  <label>{{ $t('message.scaledown.policies') }}</label>
                  <a-divider/>
                  <div class="form">
                    <strong>{{ $t('label.scaledown.policy') }} &nbsp;&nbsp;&nbsp;</strong>
                    <a-select
                      style="width: 320px"
                      v-model:value="selectedScaleDownPolicyId"
                      @change="fetchScaleDownConditions()"
                      :placeholder="$t('label.scaledown.policy')"
                      showSearch
                      optionFilterProp="label"
                      :filterOption="(input, option) => {
                        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                      }" >
                      <a-select-option
                        v-for="policy in this.scaleDownPolicies"
                        :key="policy.id"
                        :label="policy.name">
                        {{ policy.name }}
                      </a-select-option>
                    </a-select>
                    <a-button style="margin-left: 10px" ref="submit" type="primary" @click="addNewScaleDownPolicy">
                      <template #icon><plus-outlined /></template>
                      {{ $t('label.add.policy') }}
                    </a-button>
                    <a-button style="margin-left: 10px" ref="submit" type="primary" @click="removeScaleDownPolicy" :danger="true" >
                      <template #icon><delete-outlined /></template>
                      {{ $t('label.remove.policy') }}
                    </a-button>
                  </div>
                  <div class="form">
                    <a-form-item>
                      <template #label>
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.name')" :tooltip="createAutoScalePolicyApiParams.name.description"/>
                      </template>
                      <a-input v-model:value="selectedScaleDownPolicy.name"></a-input>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </a-form-item>
                    <a-form-item name="scaledownduration" ref="scaledownduration">
                      <template #label>
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.duration')" :tooltip="createAutoScalePolicyApiParams.duration.description"/>
                      </template>
                      <a-input v-model:value="selectedScaleDownPolicy.scaledownduration" type="number"></a-input>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </a-form-item>
                    <a-form-item name="scaledownquiettime" ref="scaledownquiettime">
                      <template #label>
                        <tooltip-label :title="$t('label.quiettime')" :tooltip="createAutoScalePolicyApiParams.quiettime.description"/>
                      </template>
                      <a-input v-model:value="selectedScaleDownPolicy.scaledownquiettime" type="number"></a-input>
                    </a-form-item>
                  </div>
                  <a-divider/>
                  <div class="form">
                    <div class="form__item" ref="newScaleDownConditionCounterId">
                      <div class="form__label">
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.counter')" :tooltip="$t('label.counter.name')"/>
                      </div>
                      <a-select
                        style="width: 100%"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        v-focus="true"
                        v-model:value="newScaleDownCondition.counterid">
                        <a-select-option
                          v-for="(counter, index) in countersList"
                          :value="counter.id"
                          :key="index"
                          :label="counter.name">
                          {{ counter.name }}
                        </a-select-option>
                      </a-select>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </div>
                    <div class="form__item" ref="newScaleDownConditionRelationalOperator">
                      <div class="form__label">
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.relationaloperator')" :tooltip="createConditionApiParams.relationaloperator.description"/>
                      </div>
                      <a-select
                        v-model:value="newScaleDownCondition.relationaloperator"
                        style="width: 100%;"
                        optionFilterProp="value"
                        :filterOption="(input, option) => {
                          return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }" >
                        <a-select-option value="LT">{{ getOperator('LT') }}</a-select-option>
                      </a-select>
                      <span class="error-text">{{ $t('label.required') }}</span>
                    </div>
                    <div class="form__item" ref="newScaleDownConditionThreshold">
                      <div class="form__label">
                        <span class="form__required">*</span>
                        <tooltip-label :title="$t('label.threshold')" :tooltip="$t('label.threshold.description')"/>
                      </div>
                      <a-input v-model:value="newScaleDownCondition.threshold" type="number"></a-input>
                      <span class="error-text">{{ $t('label.invalid.number') }}</span>
                    </div>
                    <div class="form__item">
                      <div class="form__label">{{ $t('label.action') }}</div>
                      <a-button ref="submit" type="primary" @click="addScaleDownCondition">
                        <template #icon><plus-outlined /></template>
                        {{ $t('label.add.condition') }}
                      </a-button>
                    </div>
                  </div>
                  <a-divider/>
                  <div style="display: block">
                    <a-table
                      size="small"
                      style="overflow-y: auto"
                      :loading="false"
                      :columns="scaleDownColumns"
                      :dataSource="scaleDownConditions"
                      :pagination="false"
                      :rowKey="record => record.counterid">
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'relationaloperator'">
                          {{ getOperator(record.relationaloperator) }}
                        </template>
                        <template v-if="column.key === 'actions'">
                          <a-button ref="submit" type="primary" :danger="true" @click="deleteScaleDownCondition(record.counterid)">
                            <template #icon><delete-outlined /></template>
                            {{ $t('label.delete') }}
                          </a-button>
                        </template>
                      </template>
                    </a-table>
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
                    <a-form-item :label="$t('label.sshkeypairs')">
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
                    <a-form-item name="userdata" ref="userdata">
                      <template #label>
                        <tooltip-label :title="$t('label.userdata')" :tooltip="createAutoScaleVmProfileApiParams.userdata.description"/>
                      </template>
                      <a-textarea
                        v-model:value="form.userdata">
                      </a-textarea>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.details')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template #description v-if="zoneSelected">
                  <div style="margin-top: 15px">
                    <a-form-item name="name" ref="name">
                      <template #label>
                        <tooltip-label :title="$t('label.name')" :tooltip="createAutoScaleVmGroupApiParams.name.description"/>
                      </template>
                      <a-input v-model:value="form.name"></a-input>
                    </a-form-item>
                    <a-form-item name="autoscaleuserid" ref="autoscaleuserid" v-if="this.selectedLbProdiver === 'Netscaler'">
                      <template #label>
                        <tooltip-label :title="$t('label.user')" :tooltip="createAutoScaleVmProfileApiParams.autoscaleuserid.description"/>
                      </template>
                      <a-select
                        style="width: 100%"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        v-focus="true"
                        v-model:value="form.autoscaleuserid">
                        <a-select-option
                          v-for="(user, index) in usersList"
                          :value="user.id"
                          :key="index"
                          :label="user.username">
                          {{ user.username }}
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                    <a-form-item name="expungevmgraceperiod" ref="expungevmgraceperiod">
                      <template #label>
                        <tooltip-label :title="$t('label.expungevmgraceperiod')" :tooltip="createAutoScaleVmProfileApiParams.expungevmgraceperiod.description"/>
                      </template>
                      <a-input v-model:value="form.expungevmgraceperiod" type="number"></a-input>
                    </a-form-item>
                    <a-form-item name="maxmembers" ref="maxmembers">
                      <template #label>
                        <tooltip-label :title="$t('label.maxmembers')" :tooltip="createAutoScaleVmGroupApiParams.maxmembers.description"/>
                      </template>
                      <a-input v-model:value="form.maxmembers" type="number"></a-input>
                    </a-form-item>
                    <a-form-item name="minmembers" ref="minmembers">
                      <template #label>
                        <tooltip-label :title="$t('label.minmembers')" :tooltip="createAutoScaleVmGroupApiParams.minmembers.description"/>
                      </template>
                      <a-input v-model:value="form.minmembers" type="number"></a-input>
                    </a-form-item>
                    <a-form-item name="interval" ref="interval">
                      <template #label>
                        <tooltip-label :title="$t('label.interval')" :tooltip="createAutoScaleVmGroupApiParams.interval.description"/>
                      </template>
                      <a-input v-model:value="form.interval" type="number"></a-input>
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
              <!-- ToDo extract as component -->
              <a-button @click="() => $router.back()" :disabled="loading.deploy">
                {{ $t('label.cancel') }}
              </a-button>
              <a-button style="margin-left: 10px" type="primary" ref="submit" @click="handleSubmit" :loading="loading.deploy">
                {{ $t('label.create') }}
              </a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7" v-if="!isMobile()">
        <a-affix :offsetTop="75" class="vm-info-card">
          <info-card :resource="vm" :title="$t('label.your.autoscale.vmgroup')" @change-resource="(data) => resource = data" />
        </a-affix>
      </a-col>
    </a-row>
  </div>
  <div>
    <a-modal
      :title="$t('message.creating.autoscale.vmgroup')"
      :visible="processStatusModalVisible"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="false"
      :footer="null"
      @cancel="processStatusModalVisible = false">

      <div class="form">
        <a-card class="ant-form-text card-launch-description">
          {{ $t('message.please.wait.while.autoscale.vmgroup.is.being.created') }}
        </a-card>
      </div>
      <div class="form">
        <a-card
          id="launch-content"
          class="ant-form-text card-launch-content">
          <a-steps
            size="small"
            direction="vertical"
            :current="currentStep"
          >
            <a-step
              v-for="(step, index) in steps"
              :key="index"
              :title="$t(step.title) + (step.detail ? ' (' + step.detail + ')' : '')"
              :status="step.status">
              <template #icon>
                <LoadingOutlined v-if="step.status===status.PROCESS" />
                <CloseCircleOutlined v-else-if="step.status===status.FAILED" />
              </template>
              <template #description>
                <a-card
                  class="step-error"
                  v-if="step.status===status.FAILED"
                >
                  <div><strong>{{ $t('label.error.something.went.wrong.please.correct.the.following') }}:</strong></div>
                  <div>{{ messageError }}</div>
                </a-card>
              </template>
            </a-step>
          </a-steps>
        </a-card>
      </div>
      <div class="form-action" v-if="processStatus">
        <a-button
          type="primary"
          @click="closeModal"
        >{{ $t('label.close') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
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
import LoadBalancerSelection from '@views/compute/wizard/LoadBalancerSelection'
import SshKeyPairSelection from '@views/compute/wizard/SshKeyPairSelection'
import SecurityGroupSelection from '@views/compute/wizard/SecurityGroupSelection'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import InstanceNicsNetworkSelectListView from '@/components/view/InstanceNicsNetworkSelectListView.vue'

const STATUS_PROCESS = 'process'
const STATUS_FINISH = 'finish'
const STATUS_FAILED = 'error'

export default {
  name: 'Wizard',
  components: {
    SshKeyPairSelection,
    NetworkConfiguration,
    NetworkSelection,
    LoadBalancerSelection,
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
      steps: [],
      currentStep: 0,
      processStatus: null,
      messageError: '',
      processStatusModalVisible: false,
      status: {
        PROCESS: STATUS_PROCESS,
        FAILED: STATUS_FAILED,
        FINISH: STATUS_FINISH
      },
      naturalNumberRule: {
        type: 'number',
        validator: this.validateNumber
      },
      zoneId: '',
      zoneSelected: false,
      dynamicscalingenabled: true,
      templateKey: 0,
      vm: {
        name: null,
        zoneid: null,
        zonename: null,
        templateid: null,
        templatename: null,
        serviceofferingid: null,
        serviceofferingname: null,
        ostypeid: null,
        ostypename: null,
        keypairs: [],
        affinitygroupids: [],
        affinitygroup: [],
        rootdisksize: null,
        disksize: null
      },
      options: {
        templates: {},
        serviceOfferings: [],
        diskOfferings: [],
        zones: [],
        affinityGroups: [],
        networks: [],
        sshKeyPairs: [],
        loadbalancers: []
      },
      rowCount: {},
      loading: {
        deploy: false,
        templates: false,
        serviceOfferings: false,
        diskOfferings: false,
        affinityGroups: false,
        networks: false,
        sshKeyPairs: false,
        loadbalancers: false,
        zones: false
      },
      vmgroupConfig: {},
      template: {},
      templateConfigurations: [],
      templateNics: [],
      templateLicenses: [],
      templateProperties: {},
      selectedTemplateConfiguration: {},
      serviceOffering: {},
      diskOffering: {},
      affinityGroups: [],
      networks: [],
      networksAdd: [],
      selectedLbId: null,
      selectedLbProdiver: null,
      countersList: [],
      scaleUpPolicies: [],
      scaleUpCounter: 0,
      selectedScaleUpPolicy: null,
      selectedScaleUpPolicyId: null,
      scaleUpConditions: [],
      newScaleUpCondition: {
        counterid: null,
        relationaloperator: 'GT',
        threshold: null
      },
      scaleUpColumns: [
        {
          title: this.$t('label.counter'),
          dataIndex: 'countername'
        },
        {
          title: this.$t('label.relationaloperator'),
          key: 'relationaloperator'
        },
        {
          title: this.$t('label.threshold'),
          dataIndex: 'threshold'
        },
        {
          title: this.$t('label.actions'),
          key: 'actions'
        }
      ],
      scaleDownPolicies: [],
      scaleDownCounter: 0,
      selectedScaleDownPolicy: null,
      selectedScaleDownPolicyId: null,
      scaleDownConditions: [],
      newScaleDownCondition: {
        counterid: null,
        relationaloperator: 'LT',
        threshold: null
      },
      scaleDownColumns: [
        {
          title: this.$t('label.counter'),
          dataIndex: 'countername'
        },
        {
          title: this.$t('label.relationaloperator'),
          key: 'relationaloperator'
        },
        {
          title: this.$t('label.threshold'),
          dataIndex: 'threshold'
        },
        {
          title: this.$t('label.actions'),
          key: 'actions'
        }
      ],
      usersList: [],
      zone: {},
      sshKeyPairs: [],
      sshKeyPair: {},
      overrideDiskOffering: {},
      templateFilter: [
        'featured',
        'community',
        'selfexecutable',
        'sharedexecutable'
      ],
      initDataConfig: {},
      defaultNetworkId: '',
      dataNetworkCreated: [],
      tabKey: 'templateid',
      dataPreFill: {},
      showDetails: false,
      showRootDiskSizeChanger: false,
      showOverrideDiskOfferingOption: false,
      showDiskOfferingOption: false,
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
      const rootDiskSize = _.get(this.vmgroupConfig, 'rootdisksize', 0)
      const customDiskSize = _.get(this.vmgroupConfig, 'size', 0)
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
          networktype: 'Advanced',
          isLoad: true,
          field: 'zoneid'
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
        loadbalancers: {
          list: 'listLoadBalancerRules',
          options: {
            zoneid: _.get(this.zone, 'id'),
            networkid: this.defaultNetworkId,
            id: this.lbRuleId,
            projectid: store.getters.project ? store.getters.project.id : null,
            domainid: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.domainid,
            account: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.account,
            page: 1,
            pageSize: 10,
            keyword: undefined,
            showIcon: true
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
    templateConfigurationExists () {
      return this.vm.templateid && this.templateConfigurations && this.templateConfigurations.length > 0
    },
    templateId () {
      return this.$route.query.templateid || null
    },
    networkId () {
      return this.$route.query.networkid || null
    },
    lbRuleId () {
      return this.$route.query.lbruleid || null
    },
    tabList () {
      const tabList = [{
        key: 'templateid',
        tab: this.$t('label.templates')
      }]
      return tabList
    },
    showSecurityGroupSection () {
      return (this.networks.length > 0 && this.zone.securitygroupsenabled) || (this.zone && this.zone.networktype === 'Basic')
    },
    isUserAllowedToListSshKeys () {
      return Boolean('listSSHKeyPairs' in this.$store.getters.apis)
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
      if (to.name === 'createAutoScaleVmGroup') {
        this.resetData()
      }
    },
    formModel: {
      deep: true,
      handler (vmgroupConfig) {
        this.vmgroupConfig = toRaw(vmgroupConfig)
        Object.keys(vmgroupConfig).forEach(field => {
          this.vm[field] = this.vmgroupConfig[field]
        })
        this.template = ''
        for (const key in this.options.templates) {
          var template = _.find(_.get(this.options.templates[key], 'template', []), (option) => option.id === vmgroupConfig.templateid)
          if (template) {
            this.template = template
            break
          }
        }

        this.serviceOffering = _.find(this.options.serviceOfferings, (option) => option.id === vmgroupConfig.computeofferingid)
        if (this.serviceOffering?.diskofferingid) {
          vmgroupConfig.overridediskofferingid = this.serviceOffering.diskofferingid
        }
        if (this.diskSelected) {
          this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === vmgroupConfig.diskofferingid)
        }
        if (this.rootDiskSelected?.id) {
          vmgroupConfig.overridediskofferingid = this.rootDiskSelected.id
        }
        if (vmgroupConfig.overridediskofferingid) {
          this.overrideDiskOffering = _.find(this.options.diskOfferings, (option) => option.id === vmgroupConfig.overridediskofferingid)
        } else {
          this.overrideDiskOffering = null
        }

        this.zone = _.find(this.options.zones, (option) => option.id === vmgroupConfig.zoneid)
        this.networks = _.filter(this.options.networks, (option) => _.includes(vmgroupConfig.networkids, option.id))
        this.affinityGroups = _.filter(this.options.affinityGroups, (option) => _.includes(vmgroupConfig.affinitygroupids, option.id))
        this.sshKeyPair = _.find(this.options.sshKeyPairs, (option) => option.name === vmgroupConfig.keypair)

        if (this.zone) {
          this.vm.zoneid = this.zone.id
          this.vm.zonename = this.zone.name
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
        }

        if (this.selectedLbId) {
          this.loadbalancer = _.find(this.options.loadbalancers, (option) => option.id === this.selectedLbId)
          this.vm.loadbalancer = this.loadbalancer
        } else {
          this.vm.loadbalancer = null
        }

        if (this.template) {
          this.vm.templateid = this.template.id
          this.vm.templatename = this.template.displaytext
          this.vm.ostypeid = this.template.ostypeid
          this.vm.ostypename = this.template.ostypename
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
    }
  },
  beforeCreate () {
    this.createConditionApi = this.$store.getters.apis.createCondition || {}
    this.createConditionApiParams = {}
    this.createConditionApi.params.forEach(param => {
      this.createConditionApiParams[param.name] = param
    })
    this.createAutoScalePolicyApi = this.$store.getters.apis.createAutoScalePolicy || {}
    this.createAutoScalePolicyApiParams = {}
    this.createAutoScalePolicyApi.params.forEach(param => {
      this.createAutoScalePolicyApiParams[param.name] = param
    })
    this.createAutoScaleVmGroupApi = this.$store.getters.apis.createAutoScaleVmGroup || {}
    this.createAutoScaleVmGroupApiParams = {}
    this.createAutoScaleVmGroupApi.params.forEach(param => {
      this.createAutoScaleVmGroupApiParams[param.name] = param
    })
    this.createAutoScaleVmProfileApi = this.$store.getters.apis.createAutoScaleVmProfile || {}
    this.createAutoScaleVmProfileApiParams = {}
    this.createAutoScaleVmProfileApi.params.forEach(param => {
      this.createAutoScaleVmProfileApiParams[param.name] = param
    })
  },
  created () {
    this.initForm()
    this.dataPreFill = this.preFillContent && Object.keys(this.preFillContent).length > 0 ? this.preFillContent : {}
    this.fetchData()
  },
  provide () {
    return {
      vmFetchTemplates: this.fetchAllTemplates,
      vmFetchNetworks: this.fetchNetwork,
      vmFetchLoadBalancers: this.fetchLoadBalancer
    }
  },
  methods: {
    addStep (title, step) {
      this.steps.push({
        index: this.currentStep,
        title,
        step,
        detail: '',
        status: STATUS_PROCESS
      })
      this.setStepStatus(STATUS_PROCESS)
    },
    addStepDetail (title, step, detail) {
      this.steps.push({
        index: this.currentStep,
        title,
        step,
        detail: detail,
        status: STATUS_PROCESS
      })
      this.setStepStatus(STATUS_PROCESS)
    },
    setStepStatus (status) {
      const index = this.steps.findIndex(step => step.index === this.currentStep)
      this.steps[index].status = status
    },
    updateTemplateKey () {
      this.templateKey += 1
    },
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        zoneid: [
          { required: true, message: `${this.$t('message.error.select')}` }
        ],
        scaleupduration: [
          { required: false, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        scaleupquiettime: [
          { required: false, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        scaledownduration: [
          { required: false, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        scaledownquiettime: [
          { required: false, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        name: [
          { required: true, message: this.$t('message.error.required.input') }
        ],
        autoscaleuserid: [
          { required: true, message: `${this.$t('message.error.select')}` }
        ],
        expungevmgraceperiod: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        maxmembers: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        minmembers: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        interval: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ]
      })

      this.addNewScaleUpPolicy()
      this.addNewScaleDownPolicy()

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
        } else if (this.networkId) {
          params.listall = true
          params.id = this.networkId
          apiName = 'listNetworks'
        } else if (this.lbRuleId) {
          params.listall = true
          params.id = this.lbRuleId
          apiName = 'listLoadBalancerRules'
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
      this.fetchUserData()
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
    fetchNetwork () {
      const param = this.params.networks
      this.fetchOptions(param, 'networks')
    },
    fetchLoadBalancer () {
      this.selectedLbId = null
      const param = this.params.loadbalancers
      this.fetchOptions(param, 'loadbalancers')
    },
    fetchCountersList () {
      api('listNetworks', {
        listAll: true,
        id: this.defaultNetworkId
      }).then(response => {
        const services = response.listnetworksresponse?.network?.[0]?.service
        const index = services.map(svc => { return svc.name }).indexOf('Lb')
        if (index === -1) {
          this.selectedLbProdiver = null
          this.countersList = []
          return
        }
        this.selectedLbProdiver = services[index].provider[0].name
        api('listCounters', {
          listAll: true,
          provider: this.selectedLbProdiver
        }).then(response => {
          this.countersList = response.counterresponse?.counter || []
        })
      })
    },
    fetchUserData () {
      api('listUsers', {
        domainid: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.domainid,
        account: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.account
      }).then(json => {
        this.usersList = json.listusersresponse?.user || []
      })
    },
    addNewScaleUpPolicy () {
      const newScaleUpPolicy = {
        id: this.scaleUpCounter,
        name: 'ScaleUpPolicy-' + this.scaleUpCounter,
        scaleupduration: null,
        scaleupquiettime: 300,
        conditions: []
      }
      this.scaleUpPolicies.push(newScaleUpPolicy)
      this.selectedScaleUpPolicyId = this.scaleUpCounter
      this.scaleUpCounter++
      this.fetchScaleUpConditions()
    },
    removeScaleUpPolicy () {
      if (this.scaleUpPolicies.length === 1) {
        return
      }
      this.scaleUpPolicies = this.scaleUpPolicies.filter(policy => policy.id !== this.selectedScaleUpPolicyId)
      this.selectedScaleUpPolicyId = this.scaleUpPolicies[this.scaleUpPolicies.length - 1].id
      this.fetchScaleUpConditions()
    },
    fetchScaleUpConditions () {
      this.selectedScaleUpPolicy = this.scaleUpPolicies.filter(policy => policy.id === this.selectedScaleUpPolicyId)[0]
      this.scaleUpConditions = this.selectedScaleUpPolicy.conditions
    },
    addNewScaleDownPolicy () {
      const newScaleDownPolicy = {
        id: this.scaleDownCounter,
        name: 'ScaleDownPolicy-' + this.scaleDownCounter,
        scaledownduration: null,
        scaledownquiettime: 300,
        conditions: []
      }
      this.scaleDownPolicies.push(newScaleDownPolicy)
      this.selectedScaleDownPolicyId = this.scaleDownCounter
      this.scaleDownCounter++
      this.fetchScaleDownConditions()
    },
    removeScaleDownPolicy () {
      if (this.scaleDownPolicies.length === 1) {
        return
      }
      this.scaleDownPolicies = this.scaleDownPolicies.filter(policy => policy.id !== this.selectedScaleDownPolicyId)
      this.selectedScaleDownPolicyId = this.scaleDownPolicies[this.scaleDownPolicies.length - 1].id
      this.fetchScaleDownConditions()
    },
    fetchScaleDownConditions () {
      this.selectedScaleDownPolicy = this.scaleDownPolicies.filter(policy => policy.id === this.selectedScaleDownPolicyId)[0]
      this.scaleDownConditions = this.selectedScaleDownPolicy.conditions
    },
    resetData () {
      this.vm = {
        name: null,
        zoneid: null,
        zonename: null,
        templateid: null,
        templatename: null,
        keypair: null,
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
        }
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
    updateNetworks (ids) {
      this.form.networkids = ids
    },
    updateDefaultNetworks (id) {
      this.defaultNetworkId = id
      this.fetchLoadBalancer()
      this.fetchCountersList()
    },
    escapePropertyKey (key) {
      return key.split('.').join('\\002E')
    },
    updateLoadBalancers (id) {
      if (id === '0') {
        this.form.loadbalancerid = undefined
        this.selectedLbId = null
      } else {
        this.form.loadbalancerid = id
        this.selectedLbId = id
      }
    },
    updateSecurityGroups (securitygroupids) {
      this.securitygroupids = securitygroupids || []
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.error.number'))
      }
      return Promise.resolve()
    },
    isNumber (value) {
      if (value && (isNaN(value) || value < 0)) {
        return false
      }
      return true
    },
    getOperator (val) {
      if (val === 'GT' || val === 'gt') return this.$t('label.operator.greater')
      if (val === 'GE' || val === 'ge') return this.$t('label.operator.greater.or.equal')
      if (val === 'LT' || val === 'lt') return this.$t('label.operator.less')
      if (val === 'LE' || val === 'le') return this.$t('label.operator.less.or.equal')
      if (val === 'EQ' || val === 'eq') return this.$t('label.operator.equal')
      return val
    },
    addScaleUpCondition () {
      if (!this.newScaleUpCondition.counterid) {
        this.$refs.newScaleUpConditionCounterId.classList.add('error')
      } else {
        this.$refs.newScaleUpConditionCounterId.classList.remove('error')
      }

      if (!this.newScaleUpCondition.relationaloperator) {
        this.$refs.newScaleUpConditionRelationalOperator.classList.add('error')
      } else {
        this.$refs.newScaleUpConditionRelationalOperator.classList.remove('error')
      }

      if (!this.newScaleUpCondition.threshold || !this.isNumber(this.newScaleUpCondition.threshold)) {
        this.$refs.newScaleUpConditionThreshold.classList.add('error')
      } else {
        this.$refs.newScaleUpConditionThreshold.classList.remove('error')
      }

      if (!this.newScaleUpCondition.counterid || !this.newScaleUpCondition.relationaloperator || !this.newScaleUpCondition.threshold) {
        return
      }
      const countername = this.countersList.filter(counter => counter.id === this.newScaleUpCondition.counterid).map(counter => { return counter.name }).join(',')
      this.fetchScaleUpConditions()
      this.scaleUpConditions = this.scaleUpConditions.filter(condition => condition.counterid !== this.newScaleUpCondition.counterid)
      this.scaleUpConditions.push({
        counterid: this.newScaleUpCondition.counterid,
        countername: countername,
        relationaloperator: this.newScaleUpCondition.relationaloperator,
        threshold: this.newScaleUpCondition.threshold
      })
      this.selectedScaleUpPolicy.conditions = this.scaleUpConditions

      this.newScaleUpCondition = {
        counterid: null,
        relationaloperator: 'GT',
        threshold: null
      }
    },
    deleteScaleUpCondition (counterId) {
      this.scaleUpConditions = this.scaleUpConditions.filter(condition => condition.counterid !== counterId)
      this.selectedScaleUpPolicy.conditions = this.scaleUpConditions
    },
    addScaleDownCondition () {
      if (!this.newScaleDownCondition.counterid) {
        this.$refs.newScaleDownConditionCounterId.classList.add('error')
      } else {
        this.$refs.newScaleDownConditionCounterId.classList.remove('error')
      }

      if (!this.newScaleDownCondition.relationaloperator) {
        this.$refs.newScaleDownConditionRelationalOperator.classList.add('error')
      } else {
        this.$refs.newScaleDownConditionRelationalOperator.classList.remove('error')
      }

      if (!this.newScaleDownCondition.threshold || !this.isNumber(this.newScaleDownCondition.threshold)) {
        this.$refs.newScaleDownConditionThreshold.classList.add('error')
      } else {
        this.$refs.newScaleDownConditionThreshold.classList.remove('error')
      }

      if (!this.newScaleDownCondition.counterid || !this.newScaleDownCondition.relationaloperator || !this.newScaleDownCondition.threshold) {
        return
      }
      const countername = this.countersList.filter(counter => counter.id === this.newScaleDownCondition.counterid).map(counter => { return counter.name }).join(',')
      this.fetchScaleDownConditions()
      this.scaleDownConditions = this.scaleDownConditions.filter(condition => condition.counterid !== this.newScaleDownCondition.counterid)
      this.scaleDownConditions.push({
        counterid: this.newScaleDownCondition.counterid,
        countername: countername,
        relationaloperator: this.newScaleDownCondition.relationaloperator,
        threshold: this.newScaleDownCondition.threshold
      })
      this.selectedScaleDownPolicy.conditions = this.scaleDownConditions

      this.newScaleDownCondition = {
        counterid: null,
        relationaloperator: 'LT',
        threshold: null
      }
    },
    deleteScaleDownCondition (counterId) {
      this.scaleDownConditions = this.scaleDownConditions.filter(condition => condition.counterid !== counterId)
      this.selectedScaleDownPolicy.conditions = this.scaleDownConditions
    },
    updateSshKeyPairs (names) {
      this.form.keypairs = names
      this.sshKeyPairs = names.map((sshKeyPair) => { return sshKeyPair.name })
    },
    updateAffinityGroups (ids) {
      this.form.affinitygroupids = ids
    },
    async pollJob (jobId) {
      return new Promise(resolve => {
        const asyncJobInterval = setInterval(() => {
          api('queryAsyncJobResult', { jobId }).then(async json => {
            const result = json.queryasyncjobresultresponse
            if (result.jobstatus === 0) {
              return
            }

            clearInterval(asyncJobInterval)
            resolve(result)
          })
        }, 1000)
      })
    },
    createVmProfile (createVmGroupData) {
      this.addStep('message.creating.autoscale.vmprofile', 'createVmProfile')

      return new Promise((resolve, reject) => {
        const params = {
          expungevmgraceperiod: createVmGroupData.expungevmgraceperiod,
          serviceofferingid: createVmGroupData.serviceofferingid,
          templateid: createVmGroupData.templateid,
          userdata: createVmGroupData.userdata,
          zoneid: createVmGroupData.zoneid
        }
        if (createVmGroupData.autoscaleuserid) {
          params.autoscaleuserid = createVmGroupData.autoscaleuserid
        }
        var i = 0
        if (createVmGroupData.snmpcommunity) {
          params['counterparam[' + i + '].name'] = 'snmpcommunity'
          params['counterparam[' + i + '].value'] = createVmGroupData.snmpcommunity
          i++
        }
        if (createVmGroupData.snmpport) {
          params['counterparam[' + i + '].name'] = 'snmpport'
          params['counterparam[' + i + '].value'] = createVmGroupData.snmpport
          i++
        }
        var j = 0
        if (createVmGroupData.rootdisksize) {
          params['otherdeployparams[' + j + '].name'] = 'rootdisksize'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.rootdisksize
          j++
        }
        if (createVmGroupData.overridediskofferingid) {
          params['otherdeployparams[' + j + '].name'] = 'overridediskofferingid'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.overridediskofferingid
          j++
        }
        if (createVmGroupData.diskofferingid) {
          params['otherdeployparams[' + j + '].name'] = 'diskofferingid'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.diskofferingid
          j++
        }
        if (createVmGroupData.size) {
          params['otherdeployparams[' + j + '].name'] = 'disksize'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.size
          j++
        }
        if (createVmGroupData.securitygroupids) {
          params['otherdeployparams[' + j + '].name'] = 'securitygroupids'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.securitygroupids
          j++
        }
        if (createVmGroupData.keypairs) {
          params['otherdeployparams[' + j + '].name'] = 'keypairs'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.keypairs
          j++
        }
        if (createVmGroupData.affinitygroupids) {
          params['otherdeployparams[' + j + '].name'] = 'affinitygroupids'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.affinitygroupids
          j++
        }
        if (createVmGroupData.networkids) {
          params['otherdeployparams[' + j + '].name'] = 'networkids'
          params['otherdeployparams[' + j + '].value'] = createVmGroupData.networkids
          j++
        }

        const httpMethod = createVmGroupData.userdata ? 'POST' : 'GET'
        const args = httpMethod === 'POST' ? {} : params
        const data = httpMethod === 'POST' ? params : {}

        api('createAutoScaleVmProfile', args, httpMethod, data).then(async json => {
          const jobId = json.autoscalevmprofileresponse.jobid
          if (jobId) {
            const result = await this.pollJob(jobId)
            if (result.jobstatus === 2) {
              this.messageError = result.jobresult.errortext
              this.processStatus = STATUS_FAILED
              this.setStepStatus(STATUS_FAILED)
              return
            }
            resolve(result.jobresult.autoscalevmprofile)
          }
        }).catch(error => {
          this.messageError = error.response.headers['x-description']
          this.processStatus = STATUS_FAILED
          this.setStepStatus(STATUS_FAILED)
        })
      })
    },
    createCondition (counterid, relationaloperator, threshold) {
      return new Promise((resolve, reject) => {
        const params = {
          counterid: counterid,
          relationaloperator: relationaloperator,
          threshold: threshold
        }
        api('createCondition', params).then(async json => {
          const jobId = json.conditionresponse.jobid
          if (jobId) {
            const result = await this.pollJob(jobId)
            if (result.jobstatus === 2) {
              this.messageError = result.jobresult.errortext
              this.processStatus = STATUS_FAILED
              this.setStepStatus(STATUS_FAILED)
              return
            }
            resolve(result.jobresult.condition)
          }
        }).catch(error => {
          this.messageError = error.response.headers['x-description']
          this.processStatus = STATUS_FAILED
          this.setStepStatus(STATUS_FAILED)
        })
      })
    },
    createScalePolicy (action, name, conditionIds, duration, quiettime) {
      return new Promise((resolve, reject) => {
        const params = {
          name: name,
          action: action,
          duration: duration,
          quiettime: quiettime,
          conditionids: conditionIds
        }
        api('createAutoScalePolicy', params).then(async json => {
          const jobId = json.autoscalepolicyresponse.jobid
          if (jobId) {
            const result = await this.pollJob(jobId)
            if (result.jobstatus === 2) {
              this.messageError = result.jobresult.errortext
              this.processStatus = STATUS_FAILED
              this.setStepStatus(STATUS_FAILED)
              return
            }
            resolve(result.jobresult.autoscalepolicy)
          }
        }).catch(error => {
          this.messageError = error.response.headers['x-description']
          this.processStatus = STATUS_FAILED
          this.setStepStatus(STATUS_FAILED)
        })
      })
    },
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    async handleSubmit (e) {
      console.log('wizard submit')
      e.preventDefault()
      if (this.loading.deploy) return
      this.formRef.value.validate().then(async () => {
        const values = toRaw(this.form)

        if (!values.templateid) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.template.iso')
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

        if (!this.defaultNetworkId) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.select.network')
          })
          return
        }

        const defaultNetwork = this.networks.filter(network => network.id === this.defaultNetworkId)[0]
        if (defaultNetwork.supportsvmautoscaling !== true) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.select.network.supports.vm.autoscaling')
          })
          return
        }

        if (!values.loadbalancerid) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.select.load.balancer')
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

        const regex = /^([a-zA-Z0-9-]){0,255}$/
        if (!values.name || values.name.length === 0 || !regex.test(values.name)) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.invalid.autoscale.vmgroup.name')
          })
          return
        }

        if (this.scaleUpPolicies.length === 0) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.scaleup.policy.continue')
          })
          return
        }
        for (const policy of this.scaleUpPolicies) {
          if (!policy.name || policy.name.length === 0) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.scaleup.policy.name.continue')
            })
            return
          }
          if (!policy.scaleupduration || parseInt(policy.scaleupduration) <= 0) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.scaleup.policy.duration.continue')
            })
            return
          }
          if (policy.conditions.length === 0) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.scaleup.policy.continue')
            })
            return
          }
          if (parseInt(policy.scaleupduration) < parseInt(values.interval)) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.error.duration.less.than.interval')
            })
            return
          }
        }

        if (this.scaleDownPolicies.length === 0) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.scaledown.policy.continue')
          })
          return
        }
        for (const policy of this.scaleDownPolicies) {
          if (!policy.name || policy.name.length === 0) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.scaledown.policy.name.continue')
            })
            return
          }
          if (!policy.scaledownduration || parseInt(policy.scaledownduration) <= 0) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.scaledown.policy.duration.continue')
            })
            return
          }
          if (policy.conditions.length === 0) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.scaledown.policy.continue')
            })
            return
          }
          if (parseInt(policy.scaledownduration) < parseInt(values.interval)) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.error.duration.less.than.interval')
            })
            return
          }
        }

        if (parseInt(values.maxmembers) < parseInt(values.minmembers)) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.error.max.members.less.than.min.members')
          })
          return
        }

        this.loading.deploy = true

        let networkIds = []

        let createVmGroupData = {}
        // step 1 : select zone
        createVmGroupData.zoneid = values.zoneid
        // step 2: select template/iso
        createVmGroupData.templateid = values.templateid

        if (this.showRootDiskSizeChanger && values.rootdisksize && values.rootdisksize > 0) {
          createVmGroupData.rootdisksize = values.rootdisksize
        } else if (this.rootDiskSizeFixed > 0 && !this.template.deployasis) {
          createVmGroupData.rootdisksize = this.rootDiskSizeFixed
        }

        // step 3: select service offering
        createVmGroupData.serviceofferingid = values.computeofferingid
        if (this.serviceOffering && this.serviceOffering.iscustomized) {
          if (values.cpunumber) {
            createVmGroupData['details[0].cpuNumber'] = values.cpunumber
          }
          if (values.cpuspeed) {
            createVmGroupData['details[0].cpuSpeed'] = values.cpuspeed
          }
          if (values.memory) {
            createVmGroupData['details[0].memory'] = values.memory
          }
        }
        if (this.selectedTemplateConfiguration) {
          createVmGroupData['details[0].configurationId'] = this.selectedTemplateConfiguration.id
        }
        if (!this.serviceOffering.diskofferingstrictness && values.overridediskofferingid) {
          createVmGroupData.overridediskofferingid = values.overridediskofferingid
          if (values.rootdisksize && values.rootdisksize > 0) {
            createVmGroupData.rootdisksize = values.rootdisksize
          }
        }
        if (this.isCustomizedIOPS) {
          createVmGroupData['details[0].minIops'] = this.minIops
          createVmGroupData['details[0].maxIops'] = this.maxIops
        }
        // step 4: select disk offering
        if (!this.template.deployasis && this.template.childtemplates && this.template.childtemplates.length > 0) {
          if (values.multidiskoffering) {
            let i = 0
            Object.entries(values.multidiskoffering).forEach(([disk, offering]) => {
              const diskKey = `datadiskofferinglist[${i}].datadisktemplateid`
              const offeringKey = `datadiskofferinglist[${i}].diskofferingid`
              createVmGroupData[diskKey] = disk
              createVmGroupData[offeringKey] = offering
              i++
            })
          }
        } else {
          createVmGroupData.diskofferingid = values.diskofferingid
          if (values.size) {
            createVmGroupData.size = values.size
          }
        }
        if (this.isCustomizedDiskIOPS) {
          createVmGroupData['details[0].minIopsDo'] = this.diskIOpsMin
          createVmGroupData['details[0].maxIopsDo'] = this.diskIOpsMax
        }

        // step 6: select network
        if (this.zone.networktype !== 'Basic') {
          if (this.nicToNetworkSelection && this.nicToNetworkSelection.length > 0) {
            for (var j in this.nicToNetworkSelection) {
              var nicNetwork = this.nicToNetworkSelection[j]
              createVmGroupData['nicnetworklist[' + j + '].nic'] = nicNetwork.nic
              createVmGroupData['nicnetworklist[' + j + '].network'] = nicNetwork.network
            }
          } else {
            networkIds = values.networkids
            if (networkIds.length > 0) {
              createVmGroupData.networkids = values.networkids.join(',')
            } else {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.step.4.continue')
              })
              this.loading.deploy = false
              return
            }
          }
        }

        if (this.securitygroupids.length > 0) {
          createVmGroupData.securitygroupids = this.securitygroupids.join(',')
        }

        // advanced settings
        createVmGroupData.keypairs = this.sshKeyPairs.join(',')
        createVmGroupData.affinitygroupids = (values.affinitygroupids || []).join(',')
        if (values.userdata && values.userdata.length > 0) {
          createVmGroupData.userdata = encodeURIComponent(btoa(this.sanitizeReverse(values.userdata)))
        }

        // vm profile details
        createVmGroupData.autoscaleuserid = values.autoscaleuserid
        createVmGroupData.expungevmgraceperiod = values.expungevmgraceperiod

        createVmGroupData = Object.fromEntries(
          Object.entries(createVmGroupData).filter(([key, value]) => value !== undefined))

        this.processStatusModalVisible = true
        this.processStatus = null

        // create autoscale vm profile
        const vmprofile = await this.createVmProfile(createVmGroupData)

        // create scaleup conditions and policy
        const scaleUpPolicyIds = []
        for (const policy of this.scaleUpPolicies) {
          this.setStepStatus(STATUS_FINISH)
          this.currentStep++
          this.addStepDetail('message.creating.autoscale.scaleup.conditions', 'createScaleUpConditions', policy.name)
          var scaleUpConditionIds = []
          for (const condition of policy.conditions) {
            const newCondition = await this.createCondition(condition.counterid, condition.relationaloperator, condition.threshold)
            scaleUpConditionIds.push(newCondition.id)
          }
          this.setStepStatus(STATUS_FINISH)
          this.currentStep++
          this.addStepDetail('message.creating.autoscale.scaleup.policy', 'createScaleUpPolicy', policy.name)
          const scaleUpPolicy = await this.createScalePolicy('ScaleUp', policy.name, scaleUpConditionIds.join(','), policy.scaleupduration, policy.scaleupquiettime)
          scaleUpPolicyIds.push(scaleUpPolicy.id)
        }

        // create scaledown conditions and policy
        const scaleDownPolicyIds = []
        for (const policy of this.scaleDownPolicies) {
          this.setStepStatus(STATUS_FINISH)
          this.currentStep++
          this.addStepDetail('message.creating.autoscale.scaledown.conditions', 'createScaleDownConditions', policy.name)
          var scaleDownConditionIds = []
          for (const condition of policy.conditions) {
            const newCondition = await this.createCondition(condition.counterid, condition.relationaloperator, condition.threshold)
            scaleDownConditionIds.push(newCondition.id)
          }
          this.setStepStatus(STATUS_FINISH)
          this.currentStep++
          this.addStepDetail('message.creating.autoscale.scaledown.policy', 'createScaleDownPolicy', policy.name)
          const scaleDownPolicy = await this.createScalePolicy('ScaleDown', policy.name, scaleDownConditionIds.join(','), policy.scaledownduration, policy.scaledownquiettime)
          scaleDownPolicyIds.push(scaleDownPolicy.id)
        }

        this.setStepStatus(STATUS_FINISH)
        this.currentStep++
        this.addStep('message.creating.autoscale.vmgroup', 'createVmGroup')

        // create autoscale vmgroup
        const params = {
          vmprofileid: vmprofile.id,
          scaleuppolicyids: scaleUpPolicyIds.join(','),
          scaledownpolicyids: scaleDownPolicyIds.join(','),
          lbruleid: values.loadbalancerid,
          name: values.name,
          maxmembers: values.maxmembers,
          minmembers: values.minmembers,
          interval: values.interval
        }
        api('createAutoScaleVmGroup', params).then(async response => {
          const jobId = response.autoscalevmgroupresponse.jobid
          const result = await this.pollJob(jobId)
          if (result.jobstatus === 2) {
            this.messageError = result.jobresult.errortext
            this.processStatus = STATUS_FAILED
            this.setStepStatus(STATUS_FAILED)
            return
          } else {
            this.setStepStatus(STATUS_FINISH)
            const vmgroup = result.jobresult.autoscalevmgroup
            this.$notification.success({
              message: this.$t('label.new.autoscale.vmgroup'),
              description: vmgroup.name,
              duration: 0
            })
            eventBus.emit('vm-refresh-data')
          }
          // Back to previous page
          this.processStatusModalVisible = false
          this.$router.back()
        }).catch(error => {
          this.messageError = error.response.headers['x-description']
          this.processStatus = STATUS_FAILED
          this.setStepStatus(STATUS_FAILED)
          this.loading.deploy = false
        }).finally(() => {
          this.loading.deploy = false
        })
      }).catch(err => {
        if (err) {
          if (err.errorFields) {
            this.formRef.value.scrollToField(err.errorFields[0].name)
          }
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
        const args = { listall: true, showicon: true }
        if (zoneId) args.id = zoneId
        api(param.list, args).then(json => {
          const zoneResponse = (json.listzonesresponse.zone || []).filter(item => item.securitygroupsenabled === false)
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
      if (!('listall' in options)) {
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

      return new Promise((resolve, reject) => {
        api('listTemplates', args).then((response) => {
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
    filterOption (input, option) {
      return option.label.toUpperCase().indexOf(input.toUpperCase()) >= 0
    },
    onSelectZoneId (value) {
      this.dataPreFill = {}
      this.zoneId = value
      this.zone = _.find(this.options.zones, (option) => option.id === value)
      this.zoneSelected = true
      this.selectedZone = this.zoneId
      this.form.zoneid = this.zoneId
      this.form.templateid = undefined
      this.tabKey = 'templateid'
      _.each(this.params, (param, name) => {
        if (this.networkId && name === 'networks') {
          param.options = {
            id: this.networkId
          }
        }
        if (this.lbRuleId && name === 'loadbalancers') {
          param.options = {
            id: this.lbRuleId
          }
        }
        if (!('isLoad' in param) || param.isLoad) {
          this.fetchOptions(param, name, ['zones'])
        }
      })
      if (this.tabKey === 'templateid') {
        this.fetchAllTemplates()
      }
      this.updateTemplateKey()
      this.formModel = toRaw(this.form)
    },
    handleSearchFilter (name, options) {
      this.params[name].options = { ...this.params[name].options, ...options }
      this.fetchOptions(this.params[name], name)
    },
    sanitizeReverse (value) {
      const reversedValue = value
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')

      return reversedValue
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
    closeModal () {
      this.loading.deploy = false
      this.processStatusModalVisible = false
      this.currentStep = 0
      this.steps = []
      this.processStatus = null
      this.messageError = ''
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
    handleNicsNetworkSelection (nicToNetworkSelection) {
      this.nicToNetworkSelection = nicToNetworkSelection
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

<style lang="less" scoped>
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

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;
    align-items: flex-start;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__item {
      display: flex;
      flex-direction: column;
      flex: 1;
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        margin-bottom: 0;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

    }

    &__label {
      font-weight: bold;
    }

    &__required {
      margin-right: 5px;
      color: red;
    }

    .error-text {
      display: none;
      color: red;
      font-size: 0.8rem;
    }

    .error {

      input {
        border-color: red;
      }
      .error-text {
        display: block;
      }

    }

  }
</style>
