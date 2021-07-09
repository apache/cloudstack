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
        <a-card :bordered="true" :title="this.$t('label.newinstance')">
          <a-form
            :form="form"
            @submit="handleSubmit"
            layout="vertical"
          >
            <a-steps direction="vertical" size="small">
              <a-step :title="this.$t('label.select.deployment.infrastructure')" status="process">
                <template slot="description">
                  <div style="margin-top: 15px">
                    <span>{{ $t('message.select.a.zone') }}</span><br/>
                    <a-form-item :label="this.$t('label.zoneid')">
                      <a-select
                        v-decorator="['zoneid', {
                          rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
                        }]"
                        showSearch
                        optionFilterProp="children"
                        :filterOption="filterOption"
                        :options="zoneSelectOptions"
                        @change="onSelectZoneId"
                        :loading="loading.zones"
                        autoFocus
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('label.podid')">
                      <a-select
                        v-decorator="['podid']"
                        showSearch
                        optionFilterProp="children"
                        :filterOption="filterOption"
                        :options="podSelectOptions"
                        :loading="loading.pods"
                        @change="onSelectPodId"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('label.clusterid')">
                      <a-select
                        v-decorator="['clusterid']"
                        showSearch
                        optionFilterProp="children"
                        :filterOption="filterOption"
                        :options="clusterSelectOptions"
                        :loading="loading.clusters"
                        @change="onSelectClusterId"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('label.hostid')">
                      <a-select
                        v-decorator="['hostid']"
                        showSearch
                        optionFilterProp="children"
                        :filterOption="filterOption"
                        :options="hostSelectOptions"
                        :loading="loading.hosts"
                      ></a-select>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="this.$t('label.templateiso')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected" style="margin-top: 15px">
                    <a-card
                      :tabList="tabList"
                      :activeTabKey="tabKey"
                      @tabChange="key => onTabChange(key, 'tabKey')">
                      <p v-if="tabKey === 'templateid'">
                        {{ $t('message.template.desc') }}
                        <template-iso-selection
                          input-decorator="templateid"
                          :items="options.templates"
                          :selected="tabKey"
                          :loading="loading.templates"
                          :preFillContent="dataPreFill"
                          @handle-search-filter="($event) => fetchAllTemplates($event)"
                          @update-template-iso="updateFieldValue" />
                        <span>
                          {{ $t('label.override.rootdisk.size') }}
                          <a-switch
                            :checked="showRootDiskSizeChanger && rootDiskSizeFixed > 0"
                            :disabled="rootDiskSizeFixed > 0 || template.deployasis"
                            @change="val => { this.showRootDiskSizeChanger = val }"
                            style="margin-left: 10px;"/>
                          <div v-if="template.deployasis">  {{ this.$t('message.deployasis') }} </div>
                        </span>
                        <disk-size-selection
                          v-show="showRootDiskSizeChanger"
                          input-decorator="rootdisksize"
                          :preFillContent="dataPreFill"
                          :minDiskSize="dataPreFill.minrootdisksize"
                          @update-disk-size="updateFieldValue"
                          style="margin-top: 10px;"/>
                      </p>
                      <p v-else>
                        {{ $t('message.iso.desc') }}
                        <template-iso-selection
                          input-decorator="isoid"
                          :items="options.isos"
                          :selected="tabKey"
                          :loading="loading.isos"
                          :preFillContent="dataPreFill"
                          @handle-search-filter="($event) => fetchAllIsos($event)"
                          @update-template-iso="updateFieldValue" />
                        <a-form-item :label="this.$t('label.hypervisor')">
                          <a-select
                            v-decorator="['hypervisor', {
                              initialValue: hypervisorSelectOptions && hypervisorSelectOptions.length > 0
                                ? hypervisorSelectOptions[0].value
                                : null,
                              rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
                            }]"
                            :options="hypervisorSelectOptions"
                            @change="value => this.hypervisor = value" />
                        </a-form-item>
                      </p>
                    </a-card>
                    <a-form-item class="form-item-hidden">
                      <a-input v-decorator="['templateid']"/>
                    </a-form-item>
                    <a-form-item class="form-item-hidden">
                      <a-input v-decorator="['isoid']"/>
                    </a-form-item>
                    <a-form-item class="form-item-hidden">
                      <a-input v-decorator="['rootdisksize']"/>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="this.$t('label.serviceofferingid')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <a-form-item v-if="zoneSelected && templateConfigurationExists">
                      <span slot="label">
                        {{ $t('label.configuration') }}
                        <a-tooltip :title="$t('message.ovf.configurations')">
                          <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                        </a-tooltip>
                      </span>
                      <a-select
                        showSearch
                        optionFilterProp="children"
                        v-decorator="[
                          'templateConfiguration'
                        ]"
                        defaultActiveFirstOption
                        :placeholder="$t('label.configuration')"
                        :filterOption="(input, option) => {
                          return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        @change="onSelectTemplateConfigurationId"
                      >
                        <a-select-option v-for="opt in templateConfigurations" :key="opt.id">
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
                      v-if="serviceOffering && serviceOffering.iscustomized"
                      cpunumber-input-decorator="cpunumber"
                      cpuspeed-input-decorator="cpuspeed"
                      memory-input-decorator="memory"
                      :preFillContent="dataPreFill"
                      :computeOfferingId="instanceConfig.computeofferingid"
                      :isConstrained="'serviceofferingdetails' in serviceOffering"
                      :minCpu="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.mincpunumber*1 : 0"
                      :maxCpu="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.maxcpunumber*1 : Number.MAX_SAFE_INTEGER"
                      :minMemory="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.minmemory*1 : 0"
                      :maxMemory="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.maxmemory*1 : Number.MAX_SAFE_INTEGER"
                      @update-compute-cpunumber="updateFieldValue"
                      @update-compute-cpuspeed="updateFieldValue"
                      @update-compute-memory="updateFieldValue" />
                    <span v-if="serviceOffering && serviceOffering.iscustomized">
                      <a-form-item class="form-item-hidden">
                        <a-input v-decorator="['cpunumber']"/>
                      </a-form-item>
                      <a-form-item
                        class="form-item-hidden"
                        v-if="(serviceOffering && !(serviceOffering.cpuspeed > 0))">
                        <a-input v-decorator="['cpuspeed']"/>
                      </a-form-item>
                      <a-form-item class="form-item-hidden">
                        <a-input v-decorator="['memory']"/>
                      </a-form-item>
                    </span>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.data.disk')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="!template.deployasis && template.childtemplates && template.childtemplates.length > 0" >
                <template slot="description">
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
                :title="tabKey == 'templateid' ? $t('label.data.disk') : $t('label.disk.size')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <disk-offering-selection
                      :items="options.diskOfferings"
                      :row-count="rowCount.diskOfferings"
                      :zoneId="zoneId"
                      :value="diskOffering ? diskOffering.id : ''"
                      :loading="loading.diskOfferings"
                      :preFillContent="dataPreFill"
                      :isIsoSelected="tabKey==='isoid'"
                      @select-disk-offering-item="($event) => updateDiskOffering($event)"
                      @handle-search-filter="($event) => handleSearchFilter('diskOfferings', $event)"
                    ></disk-offering-selection>
                    <disk-size-selection
                      v-if="diskOffering && diskOffering.iscustomized"
                      input-decorator="size"
                      :preFillContent="dataPreFill"
                      @update-disk-size="updateFieldValue" />
                    <a-form-item class="form-item-hidden">
                      <a-input v-decorator="['size']"/>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="this.$t('label.networks')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="zone && zone.networktype !== 'Basic'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <div v-if="vm.templateid && templateNics && templateNics.length > 0">
                      <a-form-item
                        v-for="(nic, nicIndex) in templateNics"
                        :key="nicIndex"
                        :v-bind="nic.name" >
                        <span slot="label">
                          {{ nic.elementName + ' - ' + nic.name }}
                          <a-tooltip :title="nic.networkDescription">
                            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                          </a-tooltip>
                        </span>
                        <a-select
                          showSearch
                          optionFilterProp="children"
                          v-decorator="[
                            'networkMap.nic-' + nic.InstanceID.toString(),
                            { initialValue: options.networks && options.networks.length > 0 ? options.networks[Math.min(nicIndex, options.networks.length - 1)].id : null }
                          ]"
                          :placeholder="nic.networkDescription"
                          :filterOption="(input, option) => {
                            return option.componentOptions.children[0].children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                          }"
                        >
                          <a-select-option v-for="opt in options.networks" :key="opt.id">
                            <span v-if="opt.type!=='L2'">
                              {{ opt.name || opt.description }} ({{ `${$t('label.cidr')}: ${opt.cidr}` }})
                            </span>
                            <span v-else>{{ opt.name || opt.description }}</span>
                          </a-select-option>
                        </a-select>
                      </a-form-item>
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
                <template slot="description">
                  <security-group-selection
                    :zoneId="zoneId"
                    :value="securitygroupids"
                    :loading="loading.networks"
                    :preFillContent="dataPreFill"
                    @select-security-group-item="($event) => updateSecurityGroups($event)"></security-group-selection>
                </template>
              </a-step>
              <a-step
                :title="this.$t('label.sshkeypairs')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <ssh-key-pair-selection
                      :items="options.sshKeyPairs"
                      :row-count="rowCount.sshKeyPairs"
                      :zoneId="zoneId"
                      :value="sshKeyPair ? sshKeyPair.name : ''"
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
                <template slot="description">
                  <div v-for="(props, category) in templateProperties" :key="category">
                    <a-alert :message="'Category: ' + category + ' (' + props.length + ' properties)'" type="info" />
                    <div style="margin-left: 15px; margin-top: 10px">
                      <a-form-item
                        v-for="(property, propertyIndex) in props"
                        :key="propertyIndex"
                        :v-bind="property.key" >
                        <span slot="label" style="text-transform: capitalize">
                          {{ property.label }}
                          <a-tooltip :title="property.description">
                            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                          </a-tooltip>
                        </span>

                        <span v-if="property.type && property.type==='boolean'">
                          <a-switch
                            v-decorator="['properties.' + escapePropertyKey(property.key), { initialValue: property.value==='TRUE'?true:false}]"
                            :defaultChecked="property.value==='TRUE'?true:false"
                            :placeholder="property.description"
                          />
                        </span>
                        <span v-else-if="property.type && (property.type==='int' || property.type==='real')">
                          <a-input-number
                            v-decorator="['properties.'+ escapePropertyKey(property.key) ]"
                            :defaultValue="property.value"
                            :placeholder="property.description"
                            :min="getPropertyQualifiers(property.qualifiers, 'number-select').min"
                            :max="getPropertyQualifiers(property.qualifiers, 'number-select').max" />
                        </span>
                        <span v-else-if="property.type && property.type==='string' && property.qualifiers && property.qualifiers.startsWith('ValueMap')">
                          <a-select
                            showSearch
                            optionFilterProp="children"
                            v-decorator="['properties.' + escapePropertyKey(property.key), { initialValue: property.value && property.value.length>0 ? property.value: getPropertyQualifiers(property.qualifiers, 'select')[0] }]"
                            :placeholder="property.description"
                            :filterOption="(input, option) => {
                              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                            }"
                          >
                            <a-select-option v-for="opt in getPropertyQualifiers(property.qualifiers, 'select')" :key="opt">
                              {{ opt }}
                            </a-select-option>
                          </a-select>
                        </span>
                        <span v-else-if="property.type && property.type==='string' && property.password">
                          <a-input-password
                            v-decorator="['properties.' + escapePropertyKey(property.key), {
                              rules: [
                                {
                                  initialValue: property.value
                                },
                                {
                                  validator: (rule, value, callback) => {
                                    if (!property.qualifiers) {
                                      callback()
                                    }
                                    var minlength = getPropertyQualifiers(property.qualifiers, 'number-select').min
                                    var maxlength = getPropertyQualifiers(property.qualifiers, 'number-select').max
                                    var errorMessage = ''
                                    var isPasswordInvalidLength = function () {
                                      return false
                                    }
                                    if (minlength) {
                                      errorMessage = $t('message.validate.minlength').replace('{0}', minlength)
                                      isPasswordInvalidLength = function () {
                                        return !value || value.length < minlength
                                      }
                                    }
                                    if (maxlength !== Number.MAX_SAFE_INTEGER) {
                                      if (minlength) {
                                        errorMessage = $t('message.validate.range.length').replace('{0}', minlength).replace('{1}', maxlength)
                                        isPasswordInvalidLength = function () {
                                          return !value || (maxlength < value.length || value.length < minlength)
                                        }
                                      } else {
                                        errorMessage = $t('message.validate.maxlength').replace('{0}', maxlength)
                                        isPasswordInvalidLength = function () {
                                          return !value || value.length > maxlength
                                        }
                                      }
                                    }
                                    if (isPasswordInvalidLength()) {
                                      callback(errorMessage)
                                    }
                                    callback()
                                  }
                                }
                              ]
                            }]"
                            :placeholder="property.description" />
                        </span>
                        <span v-else>
                          <a-input
                            v-decorator="['properties.' + escapePropertyKey(property.key), { initialValue: property.value }]"
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
                <template slot="description" v-if="zoneSelected">
                  <span>
                    {{ $t('label.isadvanced') }}
                    <a-switch @change="val => { this.showDetails = val }" :checked="this.showDetails" style="margin-left: 10px"/>
                  </span>
                  <div style="margin-top: 15px" v-show="this.showDetails">
                    <div
                      v-if="vm.templateid && ['KVM', 'VMware'].includes(hypervisor) && !template.deployasis">
                      <a-form-item :label="$t('label.boottype')">
                        <a-select
                          :autoFocus="vm.templateid && ['KVM', 'VMware'].includes(hypervisor) && !template.deployasis"
                          v-decorator="['boottype']"
                          @change="fetchBootModes"
                        >
                          <a-select-option v-for="bootType in options.bootTypes" :key="bootType.id">
                            {{ bootType.description }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                      <a-form-item :label="$t('label.bootmode')">
                        <a-select
                          v-decorator="['bootmode']">
                          <a-select-option v-for="bootMode in options.bootModes" :key="bootMode.id">
                            {{ bootMode.description }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </div>
                    <a-form-item
                      :label="this.$t('label.bootintosetup')"
                      v-if="zoneSelected && ((tabKey === 'isoid' && hypervisor === 'VMware') || (tabKey === 'templateid' && template && template.hypervisor === 'VMware'))" >
                      <a-switch
                        v-decorator="['bootintosetup']">
                      </a-switch>
                    </a-form-item>
                    <a-form-item>
                      <span slot="label">
                        {{ $t('label.dynamicscalingenabled') }}
                        <a-tooltip :title="$t('label.dynamicscalingenabled.tooltip')">
                          <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                        </a-tooltip>
                      </span>
                      <a-form-item>
                        <a-switch
                          v-decorator="['dynamicscalingenabled']"
                          :checked="isDynamicallyScalable() && dynamicscalingenabled"
                          :disabled="!isDynamicallyScalable()"
                          @change="val => { dynamicscalingenabled = val }"/>
                      </a-form-item>
                    </a-form-item>
                    <a-form-item :label="$t('label.userdata')">
                      <a-textarea
                        v-decorator="['userdata']">
                      </a-textarea>
                    </a-form-item>
                    <a-form-item :label="this.$t('label.affinity.groups')">
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
                  </div>
                </template>
              </a-step>
              <a-step
                :title="this.$t('label.details')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description" v-if="zoneSelected">
                  <div style="margin-top: 15px">
                    {{ $t('message.vm.review.launch') }}
                    <a-form-item :label="$t('label.name.optional')">
                      <a-input
                        v-decorator="['name']"
                      />
                    </a-form-item>
                    <a-form-item :label="$t('label.group.optional')">
                      <a-auto-complete
                        v-decorator="['group']"
                        :filterOption="filterOption"
                        :dataSource="options.instanceGroups" />
                    </a-form-item>
                    <a-form-item :label="$t('label.keyboard')">
                      <a-select
                        v-decorator="['keyboard']"
                        :options="keyboardSelectOptions"
                      ></a-select>
                    </a-form-item>
                    <a-form-item :label="$t('label.action.start.instance')">
                      <a-switch v-decorator="['startvm', { initialValue: true }]" :checked="this.startvm" @change="checked => { this.startvm = checked }" />
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.license.agreements')"
                :status="zoneSelected ? 'process' : 'wait'"
                v-if="vm.templateid && templateLicenses && templateLicenses.length > 0">
                <template slot="description">
                  <div style="margin-top: 10px">
                    {{ $t('message.read.accept.license.agreements') }}
                    <a-form-item>
                      <div
                        style="margin-top: 10px"
                        v-for="(license, licenseIndex) in templateLicenses"
                        :key="licenseIndex"
                        :v-bind="license.id">
                        <span slot="label" style="text-transform: capitalize">
                          {{ 'Agreement ' + (licenseIndex+1) + ': ' + license.name }}
                        </span>
                        <a-textarea
                          :value="license.text"
                          :auto-size="{ minRows: 3, maxRows: 8 }"
                          readOnly />
                      </div>
                      <a-checkbox
                        style="margin-top: 10px"
                        v-decorator="['licensesaccepted', {
                          rules: [
                            {
                              validator: (rule, value, callback) => {
                                if (!value) {
                                  callback($t('message.license.agreements.not.accepted'))
                                }
                                callback()
                              }
                            }
                          ]
                        }]">
                        {{ $t('label.i.accept.all.license.agreements') }}
                      </a-checkbox>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
            </a-steps>
            <div class="card-footer">
              <a-form-item>
                <a-switch
                  class="form-item-hidden"
                  v-decorator="['stayonpage']"
                ></a-switch>
              </a-form-item>
              <!-- ToDo extract as component -->
              <a-button @click="() => this.$router.back()" :disabled="loading.deploy">
                {{ this.$t('label.cancel') }}
              </a-button>
              <a-dropdown-button style="margin-left: 10px" type="primary" @click="handleSubmit" :loading="loading.deploy">
                <a-icon type="rocket" />
                {{ this.$t('label.launch.vm') }}
                <a-icon slot="icon" type="down" />
                <a-menu type="primary" slot="overlay" @click="handleSubmitAndStay" theme="dark">
                  <a-menu-item type="primary" key="1">
                    <a-icon type="rocket" />
                    {{ $t('label.launch.vm.and.stay') }}
                  </a-menu-item>
                </a-menu>
              </a-dropdown-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7" v-if="!isMobile()">
        <a-affix :offsetTop="75">
          <info-card class="vm-info-card" :resource="vm" :title="this.$t('label.yourinstance')" />
        </a-affix>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import Vue from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import { mixin, mixinDevice } from '@/utils/mixin.js'
import store from '@/store'
import eventBus from '@/config/eventBus'

import InfoCard from '@/components/view/InfoCard'
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
import SecurityGroupSelection from '@views/compute/wizard/SecurityGroupSelection'

export default {
  name: 'Wizard',
  components: {
    SshKeyPairSelection,
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
    SecurityGroupSelection
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
      startvm: true,
      dynamicscalingenabled: true,
      vm: {
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
      },
      options: {
        templates: [],
        isos: [],
        hypervisors: [],
        serviceOfferings: [],
        diskOfferings: [],
        zones: [],
        affinityGroups: [],
        networks: [],
        sshKeyPairs: [],
        pods: [],
        clusters: [],
        hosts: [],
        groups: [],
        keyboards: [],
        bootTypes: [],
        bootModes: [],
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
        zones: false,
        pods: false,
        clusters: false,
        hosts: false,
        groups: false
      },
      instanceConfig: {},
      template: {},
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
      sshKeyPair: {},
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
      tabList: [
        {
          key: 'templateid',
          tab: this.$t('label.templates')
        },
        {
          key: 'isoid',
          tab: this.$t('label.isos')
        }
      ],
      tabKey: 'templateid',
      dataPreFill: {},
      showDetails: false,
      showRootDiskSizeChanger: false,
      securitygroupids: [],
      rootDiskSizeFixed: 0
    }
  },
  computed: {
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
            keyword: undefined
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
        value: undefined
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
        value: undefined
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
        value: undefined
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
    networkId () {
      return this.$route.query.networkid || null
    },
    showSecurityGroupSection () {
      return (this.networks.length > 0 && this.zone.securitygroupsenabled) || (this.zone && this.zone.networktype === 'Basic')
    },
    dynamicScalingVmConfigValue () {
      return this.options.dynamicScalingVmConfig?.[0]?.value === 'true'
    }
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'deployVirtualMachine') {
        this.resetData()
      }
    },
    instanceConfig (instanceConfig) {
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
      this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.diskofferingid)
      this.zone = _.find(this.options.zones, (option) => option.id === instanceConfig.zoneid)
      this.affinityGroups = _.filter(this.options.affinityGroups, (option) => _.includes(instanceConfig.affinitygroupids, option.id))
      this.networks = _.filter(this.options.networks, (option) => _.includes(instanceConfig.networkids, option.id))
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
  created () {
    this.form = this.$form.createForm(this, {
      onValuesChange: (props, fields) => {
        if (fields.isoid) {
          this.form.setFieldsValue({
            templateid: null,
            rootdisksize: 0
          })
        } else if (fields.templateid) {
          this.form.setFieldsValue({ isoid: null })
        }
        this.instanceConfig = { ...this.form.getFieldsValue(), ...fields }
        Object.keys(fields).forEach(field => {
          if (field in this.vm) {
            this.vm[field] = this.instanceConfig[field]
          }
        })
      }
    })
    this.form.getFieldDecorator('computeofferingid', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('diskofferingid', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('multidiskoffering', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('affinitygroupids', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('networkids', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('defaultnetworkid', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('keypair', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('cpunumber', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('cpuSpeed', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('memory', { initialValue: undefined, preserve: true })

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
      this.form.getFieldDecorator([field], { initialValue: this.dataPreFill[field] })
    },
    fetchData () {
      if (this.dataPreFill.zoneid) {
        this.fetchDataByZone(this.dataPreFill.zoneid)
      } else {
        _.each(this.params, (param, name) => {
          if (param.isLoad) {
            this.fetchOptions(param, name)
          }
        })
      }

      this.fetchBootTypes()
      this.fetchBootModes()
      this.fetchInstaceGroups()
      Vue.nextTick().then(() => {
        ['name', 'keyboard', 'boottype', 'bootmode', 'userdata'].forEach(this.fillValue)
        this.instanceConfig = this.form.getFieldsValue() // ToDo: maybe initialize with some other defaults
      })
    },
    isDynamicallyScalable () {
      return this.serviceOffering && this.serviceOffering.dynamicscalingenabled && this.template && this.template.isdynamicallyscalable && this.dynamicScalingVmConfigValue
    },
    async fetchDataByZone (zoneId) {
      this.fillValue('zoneid')
      this.options.zones = await this.fetchZones()
      this.zoneId = zoneId
      this.zoneSelected = true
      this.tabKey = 'templateid'
      await _.each(this.params, (param, name) => {
        if (!('isLoad' in param) || param.isLoad) {
          this.fetchOptions(param, name, ['zones'])
        }
      })
      await this.fetchAllTemplates()
    },
    fetchBootTypes () {
      const bootTypes = []

      bootTypes.push({
        id: 'BIOS',
        description: 'BIOS'
      })
      bootTypes.push({
        id: 'UEFI',
        description: 'UEFI'
      })

      this.options.bootTypes = bootTypes
      this.$forceUpdate()
    },
    fetchBootModes (bootType) {
      const bootModes = []

      if (bootType === 'UEFI') {
        bootModes.push({
          id: 'LEGACY',
          description: 'LEGACY'
        })
        bootModes.push({
          id: 'SECURE',
          description: 'SECURE'
        })
      } else {
        bootModes.push({
          id: 'LEGACY',
          description: 'LEGACY'
        })
      }

      this.options.bootModes = bootModes
      this.$forceUpdate()
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
          this.options.instanceGroups.push(x.name)
        })
      })
    },
    fetchNetwork () {
      const param = this.params.networks
      this.fetchOptions(param, 'networks')
    },
    resetData () {
      this.vm = {}
      this.zoneSelected = false
      this.form.resetFields()
      this.fetchData()
    },
    updateFieldValue (name, value) {
      if (name === 'templateid') {
        this.tabKey = 'templateid'
        this.form.setFieldsValue({
          templateid: value,
          isoid: null
        })
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
      } else if (name === 'isoid') {
        this.templateConfigurations = []
        this.selectedTemplateConfiguration = {}
        this.templateNics = []
        this.templateLicenses = []
        this.templateProperties = {}
        this.tabKey = 'isoid'
        this.resetFromTemplateConfiguration()
        this.form.setFieldsValue({
          isoid: value,
          templateid: null
        })
      } else if (['cpuspeed', 'cpunumber', 'memory'].includes(name)) {
        this.vm[name] = value
        this.form.setFieldsValue({
          [name]: value
        })
      } else {
        this.form.setFieldsValue({
          [name]: value
        })
      }
    },
    updateComputeOffering (id) {
      this.form.setFieldsValue({
        computeofferingid: id
      })
      setTimeout(() => {
        this.updateTemplateConfigurationOfferingDetails(id)
      }, 500)
    },
    updateDiskOffering (id) {
      if (id === '0') {
        this.form.setFieldsValue({
          diskofferingid: undefined
        })
        return
      }
      this.form.setFieldsValue({
        diskofferingid: id
      })
    },
    updateMultiDiskOffering (value) {
      this.form.setFieldsValue({
        multidiskoffering: value
      })
    },
    updateAffinityGroups (ids) {
      this.form.setFieldsValue({
        affinitygroupids: ids
      })
    },
    updateNetworks (ids) {
      this.form.setFieldsValue({
        networkids: ids
      })
    },
    updateDefaultNetworks (id) {
      this.defaultnetworkid = id
      this.form.setFieldsValue({
        defaultnetworkid: id
      })
    },
    updateNetworkConfig (networks) {
      this.networkConfig = networks
    },
    updateSshKeyPairs (name) {
      if (name === this.$t('label.noselect')) {
        this.form.setFieldsValue({
          keypair: undefined
        })
        return
      }
      this.form.setFieldsValue({
        keypair: name
      })
    },
    escapePropertyKey (key) {
      return key.split('.').join('\\002E')
    },
    updateSecurityGroups (securitygroupids) {
      this.securitygroupids = securitygroupids
    },
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    handleSubmitAndStay (e) {
      this.form.setFieldsValue({
        stayonpage: true
      })
      this.handleSubmit(e.domEvent)
      this.form.setFieldsValue({
        stayonpage: false
      })
    },
    handleSubmit (e) {
      console.log('wizard submit')
      e.preventDefault()
      this.form.validateFields(async (err, values) => {
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
          return
        }

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

        this.loading.deploy = true

        let networkIds = []

        const deployVmData = {}
        // step 1 : select zone
        deployVmData.zoneid = values.zoneid
        deployVmData.podid = values.podid
        deployVmData.clusterid = values.clusterid
        deployVmData.hostid = values.hostid
        deployVmData.keyboard = values.keyboard
        deployVmData.boottype = values.boottype
        deployVmData.bootmode = values.bootmode
        deployVmData.dynamicscalingenabled = values.dynamicscalingenabled
        if (values.userdata && values.userdata.length > 0) {
          deployVmData.userdata = encodeURIComponent(btoa(this.sanitizeReverse(values.userdata)))
        }
        // step 2: select template/iso
        if (this.tabKey === 'templateid') {
          deployVmData.templateid = values.templateid
        } else {
          deployVmData.templateid = values.isoid
        }
        if (this.showRootDiskSizeChanger && values.rootdisksize && values.rootdisksize > 0) {
          deployVmData.rootdisksize = values.rootdisksize
        } else if (this.rootDiskSizeFixed > 0) {
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
        // step 5: select an affinity group
        deployVmData.affinitygroupids = (values.affinitygroupids || []).join(',')
        // step 6: select network
        if (this.zone.networktype !== 'Basic') {
          if ('networkMap' in values) {
            const keys = Object.keys(values.networkMap)
            for (var j = 0; j < keys.length; ++j) {
              if (values.networkMap[keys[j]] && values.networkMap[keys[j]].length > 0) {
                deployVmData['nicnetworklist[' + j + '].nic'] = keys[j].replace('nic-', '')
                deployVmData['nicnetworklist[' + j + '].network'] = values.networkMap[keys[j]]
              }
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
          if (this.securitygroupids.length > 0) {
            deployVmData.securitygroupids = this.securitygroupids.join(',')
          }
        }
        // step 7: select ssh key pair
        deployVmData.keypair = values.keypair
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

        api('deployVirtualMachine', deployVmData).then(response => {
          const jobId = response.deployvirtualmachineresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
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
                eventBus.$emit('vm-refresh-data')
              },
              errorMethod: () => {
                eventBus.$emit('vm-refresh-data')
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')}`,
              catchMessage: this.$t('error.fetching.async.job.result'),
              catchMethod: () => {
                eventBus.$emit('vm-refresh-data')
              }
            })
            this.$store.dispatch('AddAsyncJob', {
              title: title,
              jobid: jobId,
              description: description,
              status: 'progress'
            })
          }
          // Sending a refresh in case it hasn't picked up the new VM
          new Promise(resolve => setTimeout(resolve, 3000)).then(() => {
            eventBus.$emit('vm-refresh-data')
          })
          if (!values.stayonpage) {
            this.$router.back()
          }
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading.deploy = false
        })
      })
    },
    fetchZones () {
      return new Promise((resolve) => {
        this.loading.zones = true
        const param = this.params.zones
        api(param.list, { listall: true }).then(json => {
          const zones = json.listzonesresponse.zone || []
          resolve(zones)
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
            this.$forceUpdate()
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
              this.hypervisor = response[0] && response[0].name ? response[0].name : null
            }

            this.$forceUpdate()
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
              this.form.getFieldDecorator(['zoneid'], { initialValue: zoneid })
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
          this.$forceUpdate()
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
          this.$forceUpdate()
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.isos = false
      })
    },
    filterOption (input, option) {
      return (
        option.componentOptions.children[0].text.toUpperCase().indexOf(input.toUpperCase()) >= 0
      )
    },
    onSelectZoneId (value) {
      this.dataPreFill = {}
      this.zoneId = value
      this.podId = null
      this.clusterId = null
      this.zone = _.find(this.options.zones, (option) => option.id === value)
      this.zoneSelected = true
      this.form.setFieldsValue({
        clusterid: undefined,
        podid: undefined,
        hostid: undefined,
        templateid: undefined,
        isoid: undefined
      })
      this.tabKey = 'templateid'
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
      this.fetchAllTemplates()
    },
    onSelectPodId (value) {
      this.podId = value

      this.fetchOptions(this.params.clusters, 'clusters')
      this.fetchOptions(this.params.hosts, 'hosts')
    },
    onSelectClusterId (value) {
      this.clusterId = value

      this.fetchOptions(this.params.hosts, 'hosts')
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
    sanitizeReverse (value) {
      const reversedValue = value
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')

      return reversedValue
    },
    fetchTemplateNics (template) {
      var nics = []
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
</style>
