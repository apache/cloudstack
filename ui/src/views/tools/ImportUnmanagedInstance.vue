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
    <a-spin :spinning="loading" v-ctrl-enter="handleSubmit">
      <a-row :gutter="12">
        <a-col :md="24" :lg="7" v-if="!isDiskImport">
          <info-card
            class="vm-info-card"
            :isStatic="true"
            :resource="resource"
            :title="$t('label.unmanaged.instance')" />
        </a-col>
        <a-col :md="24" :lg="17">
          <a-card :bordered="true">
            <a-form
              :ref="formRef"
              :model="form"
              :rules="rules"
              @finish="handleSubmit"
              layout="vertical">
              <a-alert
                v-if="selectedVmwareVcenter && isVmRunning"
                type="warning"
                :showIcon="true"
                :message="$t('message.import.running.instance.warning')"
              />
              <a-form-item name="displayname" ref="displayname">
                <template #label>
                  <tooltip-label :title="$t('label.displayname')" :tooltip="apiParams.displayname.description"/>
                </template>
                <a-input
                  v-model:value="form.displayname"
                  :placeholder="apiParams.displayname.description"
                  ref="displayname"
                  v-focus="true" />
              </a-form-item>
              <a-form-item name="hostname" ref="hostname">
                <template #label>
                  <tooltip-label :title="$t('label.hostnamelabel')" :tooltip="apiParams.hostname.description"/>
                </template>
                <a-input
                  v-model:value="form.hostname"
                  :placeholder="apiParams.hostname.description" />
              </a-form-item>
              <a-form-item name="domainid" ref="domainid">
                <template #label>
                  <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
                </template>
                <a-select
                  v-model:value="form.domainid"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  :loading="optionsLoading.domains"
                  :placeholder="apiParams.domainid.description"
                  @change="val => { this.selectedDomainId = val }">
                  <a-select-option v-for="dom in domainSelectOptions" :key="dom.value" :label="dom.label">
                    <span>
                      <resource-icon v-if="dom.icon" :image="dom.icon" size="1x" style="margin-right: 5px"/>
                      <block-outlined v-else-if="dom.value !== null" style="margin-right: 5px" />
                      {{ dom.label }}
                    </span>
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item name="account" ref="account" v-if="selectedDomainId">
                <template #label>
                  <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
                </template>
                <a-input
                  v-model:value="form.account"
                  :placeholder="apiParams.account.description"/>
              </a-form-item>
              <a-form-item name="projectid" ref="projectid">
                <template #label>
                  <tooltip-label :title="$t('label.project')" :tooltip="apiParams.projectid.description"/>
                </template>
                <a-select
                  v-model:value="form.projectid"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  :loading="optionsLoading.projects"
                  :placeholder="apiParams.projectid.description">
                  <a-select-option v-for="proj in projectSelectOptions" :key="proj.value" :label="proj.label">
                    <span>
                      <resource-icon v-if="proj.icon" :image="proj.icon" size="1x" style="margin-right: 5px"/>
                      <project-outlined  v-else-if="proj.value !== null" style="margin-right: 5px" />
                      {{ proj.label }}
                    </span>
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item name="templateid" ref="templateid" v-if="cluster.hypervisortype === 'VMware' || (cluster.hypervisortype === 'KVM' && !selectedVmwareVcenter && !isDiskImport && !isExternalImport)">
                <template #label>
                  <tooltip-label :title="$t('label.templatename')" :tooltip="apiParams.templateid.description + '. ' + $t('message.template.import.vm.temporary')"/>
                </template>
                <a-radio-group
                  style="width:100%"
                  :value="templateType"
                  @change="changeTemplateType">
                  <a-row :gutter="12">
                    <a-col :md="24" :lg="12" v-if="this.cluster.hypervisortype === 'VMware'">
                      <a-radio value="auto">
                        {{ $t('label.template.temporary.import') }}
                      </a-radio>
                    </a-col>
                    <a-col :md="24" :lg="12">
                      <a-radio value="custom">
                        {{ $t('label.template.select.existing') }}
                      </a-radio>
                      <a-select
                        :disabled="templateType === 'auto'"
                        style="margin-top:10px"
                        v-model:value="form.templateid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        :loading="optionsLoading.templates"
                        :placeholder="apiParams.templateid.description">
                        <a-select-option v-for="temp in templateSelectOptions" :key="temp.value" :label="temp.label">
                          <span>
                            <resource-icon v-if="temp.icon" :image="temp.icon" size="1x" style="margin-right: 5px"/>
                            <os-logo v-else-if="temp.value !== null" :osId="temp.ostypeid" :osName="temp.ostypename" size="lg" style="margin-left: -1px" />
                            {{ temp.label }}
                          </span>
                        </a-select-option>
                      </a-select>
                    </a-col>
                  </a-row>
                </a-radio-group>
              </a-form-item>
              <a-form-item name="converthostid" ref="converthostid">
                <check-box-select-pair
                  layout="vertical"
                  v-if="cluster.hypervisortype === 'KVM' && selectedVmwareVcenter"
                  :resourceKey="cluster.id"
                  :selectOptions="kvmHostsForConversion"
                  :checkBoxLabel="$t('message.select.kvm.host.instance.conversion')"
                  :defaultCheckBoxValue="false"
                  :reversed="false"
                  @handle-checkselectpair-change="updateSelectedKvmHostForConversion"
                />
              </a-form-item>
              <a-form-item name="convertstorageoption" ref="convertstorageoption">
                <check-box-select-pair
                  layout="vertical"
                  style="margin-bottom: 5px"
                  v-if="cluster.hypervisortype === 'KVM' && selectedVmwareVcenter"
                  :resourceKey="cluster.id"
                  :selectOptions="storageOptionsForConversion"
                  :checkBoxLabel="$t('message.select.temporary.storage.instance.conversion')"
                  :defaultCheckBoxValue="false"
                  :reversed="false"
                  @handle-checkselectpair-change="updateSelectedStorageOptionForConversion"
                />
              </a-form-item>
              <a-form-item v-if="showStoragePoolsForConversion" name="convertstoragepool" ref="convertstoragepool" :label="$t('label.storagepool')">
                <a-select
                  v-model:value="form.convertstoragepoolid"
                  defaultActiveFirstOption
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  @change="val => { selectedStoragePoolForConversion = val }">
                  <a-select-option v-for="(pool) in storagePoolsForConversion" :key="pool.id" :label="pool.name">
                    {{ pool.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item name="forcemstoimportvmfiles" ref="forcemstoimportvmfiles" v-if="selectedVmwareVcenter">
                <template #label>
                  <tooltip-label :title="$t('label.force.ms.to.import.vm.files')" :tooltip="apiParams.forcemstoimportvmfiles.description"/>
                </template>
                <a-switch v-model:checked="form.forcemstoimportvmfiles" @change="val => { switches.forceMsToImportVmFiles = val }" />
              </a-form-item>
              <a-form-item name="serviceofferingid" ref="serviceofferingid">
                <template #label>
                  <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
                </template>
                <compute-offering-selection
                  :compute-items="computeOfferings"
                  :loading="computeOfferingLoading"
                  :rowCount="totalComputeOfferings"
                  :value="computeOffering ? computeOffering.id : ''"
                  :minimumCpunumber="isVmRunning ? resource.cpunumber : null"
                  :minimumCpuspeed="isVmRunning ? resource.cpuspeed : null"
                  :minimumMemory="isVmRunning ? resource.memory : null"
                  :allowAllOfferings="selectedVmwareVcenter ? true : false"
                  size="small"
                  @select-compute-item="($event) => updateComputeOffering($event)"
                  @handle-search-filter="($event) => fetchComputeOfferings($event)" />
                <compute-selection
                  class="row-element"
                  v-if="computeOffering && (computeOffering.iscustomized || computeOffering.iscustomizediops)"
                  :isCustomized="computeOffering.iscustomized"
                  :isCustomizedIOps="'iscustomizediops' in computeOffering && computeOffering.iscustomizediops"
                  :cpuNumberInputDecorator="cpuNumberKey"
                  :cpuSpeedInputDecorator="cpuSpeedKey"
                  :memoryInputDecorator="memoryKey"
                  :computeOfferingId="computeOffering.id"
                  :preFillContent="resource"
                  :isConstrained="'serviceofferingdetails' in computeOffering"
                  :minCpu="getMinCpu()"
                  :maxCpu="getMaxCpu()"
                  :minMemory="getMinMemory()"
                  :maxMemory="getMaxMemory()"
                  :cpuSpeed="getCPUSpeed()"
                  @update-iops-value="updateFieldValue"
                  @update-compute-cpunumber="updateFieldValue"
                  @update-compute-cpuspeed="updateCpuSpeed"
                  @update-compute-memory="updateFieldValue" />
              </a-form-item>
              <div v-if="resource.disk && resource.disk.length > 1">
                <a-form-item name="selection" ref="selection">
                  <template #label>
                    <tooltip-label :title="$t('label.disk.selection')" :tooltip="apiParams.datadiskofferinglist.description"/>
                  </template>
                </a-form-item>
                <a-form-item name="rootdiskid" ref="rootdiskid" :label="$t('label.select.root.disk')">
                  <a-select
                    v-model:value="form.rootdiskid"
                    defaultActiveFirstOption
                    showSearch
                    optionFilterProp="label"
                    :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }"
                    @change="onSelectRootDisk">
                    <a-select-option v-for="(opt, optIndex) in resource.disk" :key="optIndex" :label="opt.label || opt.id">
                      {{ opt.label || opt.id }}
                    </a-select-option>
                  </a-select>
                  <a-table
                    :columns="selectedRootDiskColumns"
                    :dataSource="selectedRootDiskSources"
                    :pagination="false">
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'name'">
                        <span>{{ record.displaytext || record.name }}</span>
                        <div v-if="record.meta">
                          <div v-for="meta in record.meta" :key="meta.key">
                            <a-tag style="margin-top: 5px" :key="meta.key">{{ meta.key + ': ' + meta.value }}</a-tag>
                          </div>
                        </div>
                      </template>
                    </template>
                  </a-table>
                </a-form-item>
                <multi-disk-selection
                  :items="dataDisks"
                  :zoneId="cluster.zoneid"
                  :selectionEnabled="false"
                  :customOfferingsAllowed="true"
                  :autoSelectCustomOffering="true"
                  :isKVMUnmanage="isKVMUnmanage"
                  :autoSelectLabel="$t('label.auto.assign.diskoffering.disk.size')"
                  @select-multi-disk-offering="updateMultiDiskOffering" />
              </div>
              <div v-if="resource.nic && resource.nic.length > 0">
                <a-form-item name="networkselection" ref="networkselection">
                  <template #label>
                    <tooltip-label :title="$t('label.network.selection')" :tooltip="apiParams.nicnetworklist.description"/>
                  </template>
                  <span>{{ $t('message.ip.address.changes.effect.after.vm.restart') }}</span>
                </a-form-item>
                <a-row v-if="selectedVmwareVcenter" :gutter="12" justify="end">
                  <a-col style="text-align: right">
                    <a-form-item name="forced" ref="forced">
                      <template #label>
                        <tooltip-label
                          :title="$t('label.allow.duplicate.macaddresses')"
                          :tooltip="apiParams.forced.description"/>
                      </template>
                      <a-switch v-model:checked="form.forced" @change="val => { switches.forced = val }" />
                    </a-form-item>
                  </a-col>
                </a-row>
                <multi-network-selection
                  :items="nics"
                  :zoneId="cluster.zoneid"
                  :domainid="form.domainid"
                  :account="form.account"
                  :selectionEnabled="false"
                  :filterUnimplementedNetworks="true"
                  :hypervisor="this.cluster.hypervisortype"
                  :filterMatchKey="isKVMUnmanage ? undefined : 'broadcasturi'"
                  @select-multi-network="updateMultiNetworkOffering" />
              </div>
              <a-row v-else style="margin: 12px 0" >
                <div v-if="!isExternalImport && !isDiskImport">
                  <a-alert type="warning">
                    <template #message>
                      <div v-html="$t('message.warn.importing.instance.without.nic')"></div>
                    </template>
                  </a-alert>
                </div>
              </a-row>
              <div v-if="isDiskImport">
                <a-form-item name="networkid" ref="networkid">
                  <template #label>
                    <tooltip-label :title="$t('label.network')"/>
                  </template>
                  <a-select
                    v-model:value="form.networkid"
                    showSearch
                    optionFilterProp="label"
                    :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }"
                    :loading="optionsLoading.networks">
                    <a-select-option v-for="network in networkSelectOptions" :key="network.value" :label="network.label">
                      <span>
                        {{ network.label }}
                      </span>
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </div>
              <a-row v-if="!selectedVmwareVcenter" :gutter="12">
                <a-col :md="24" :lg="12">
                  <a-form-item name="migrateallowed" ref="migrateallowed">
                    <template #label>
                      <tooltip-label :title="$t('label.migrate.allowed')" :tooltip="apiParams.migrateallowed.description"/>
                    </template>
                    <a-switch v-model:checked="form.migrateallowed" @change="val => { switches.migrateAllowed = val }" />
                  </a-form-item>
                </a-col>
                <a-col>
                  <a-form-item name="forced" ref="forced">
                    <template #label>
                      <tooltip-label
                        :title="$t('label.forced')"
                        :tooltip="apiParams.forced.description"/>
                    </template>
                    <a-switch v-model:checked="form.forced" @change="val => { switches.forced = val }" />
                  </a-form-item>
                </a-col>
              </a-row>
              <div :span="24" class="action-button">
                <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
                <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
              </div>
            </a-form>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import InfoCard from '@/components/view/InfoCard'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import ComputeOfferingSelection from '@views/compute/wizard/ComputeOfferingSelection'
import ComputeSelection from '@views/compute/wizard/ComputeSelection'
import MultiDiskSelection from '@views/compute/wizard/MultiDiskSelection'
import MultiNetworkSelection from '@views/compute/wizard/MultiNetworkSelection'
import OsLogo from '@/components/widgets/OsLogo'
import ResourceIcon from '@/components/view/ResourceIcon'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'

export default {
  name: 'ImportUnmanagedInstances',
  components: {
    InfoCard,
    TooltipLabel,
    ComputeOfferingSelection,
    ComputeSelection,
    MultiDiskSelection,
    MultiNetworkSelection,
    OsLogo,
    ResourceIcon,
    CheckBoxSelectPair
  },
  props: {
    cluster: {
      type: Object,
      required: true
    },
    host: {
      type: Object,
      required: true
    },
    pool: {
      type: Object,
      required: true
    },
    resource: {
      type: Object,
      required: true
    },
    isOpen: {
      type: Boolean,
      required: false
    },
    zoneid: {
      type: String,
      required: false
    },
    importsource: {
      type: String,
      required: false
    },
    hypervisor: {
      type: String,
      required: false
    },
    exthost: {
      type: String,
      required: false
    },
    username: {
      type: String,
      required: false
    },
    password: {
      type: String,
      required: false
    },
    tmppath: {
      type: String,
      required: false
    },
    diskpath: {
      type: String,
      required: false
    },
    selectedVmwareVcenter: {
      type: Array,
      required: false
    }
  },
  data () {
    return {
      options: {
        domains: [],
        projects: [],
        networks: [],
        templates: []
      },
      rowCount: {},
      optionsLoading: {
        domains: false,
        projects: false,
        networks: false,
        templates: false
      },
      domains: [],
      domainLoading: false,
      selectedDomainId: null,
      templates: [],
      templateLoading: false,
      templateType: this.defaultTemplateType(),
      totalComputeOfferings: 0,
      computeOfferings: [],
      computeOfferingLoading: false,
      computeOffering: {},
      selectedRootDiskIndex: 0,
      dataDisksOfferingsMapping: {},
      nicsNetworksMapping: {},
      cpuNumberKey: 'cpuNumber',
      cpuSpeedKey: 'cpuSpeed',
      memoryKey: 'memory',
      minIopsKey: 'minIops',
      maxIopsKey: 'maxIops',
      switches: {},
      loading: false,
      kvmHostsForConversion: [],
      selectedKvmHostForConversion: null,
      storageOptionsForConversion: [
        {
          id: 'secondary',
          name: 'Secondary Storage'
        }, {
          id: 'primary',
          name: 'Primary Storage'
        }
      ],
      storagePoolsForConversion: [],
      selectedStorageOptionForConversion: null,
      selectedStoragePoolForConversion: null,
      showStoragePoolsForConversion: false,
      selectedRootDiskColumns: [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.rootdisk')
        }
      ],
      selectedRootDiskSources: []
    }
  },
  beforeCreate () {
    this.apiConfig = this.$store.getters.apis.importUnmanagedInstance || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
    this.apiConfig = this.$store.getters.apis.importVm || {}
    this.apiConfig.params.forEach(param => {
      if (!(param.name in this.apiParams)) {
        this.apiParams[param.name] = param
      }
    })
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  computed: {
    params () {
      return {
        domains: {
          list: 'listDomains',
          isLoad: true,
          field: 'domainid',
          options: {
            details: 'min',
            showicon: true
          }
        },
        projects: {
          list: 'listProjects',
          isLoad: true,
          field: 'projectid',
          options: {
            details: 'min',
            showicon: true
          }
        },
        networks: {
          list: 'listNetworks',
          isLoad: true,
          field: 'networkid',
          options: {
            zoneid: this.zoneid,
            details: 'min'
          }
        },
        templates: {
          list: 'listTemplates',
          isLoad: true,
          options: {
            templatefilter: 'all',
            hypervisor: this.cluster.hypervisortype,
            showicon: true
          },
          field: 'templateid'
        }
      }
    },
    isVmRunning () {
      if (this.resource && this.resource.powerstate === 'PowerOn') {
        return true
      }
      return false
    },
    isDiskImport () {
      if (this.importsource === 'local' || this.importsource === 'shared') {
        return true
      }
      return false
    },
    isExternalImport () {
      if (this.importsource === 'external') {
        return true
      }
      return false
    },
    isKVMUnmanage () {
      return this.hypervisor && this.hypervisor === 'kvm' && (this.importsource === 'unmanaged' || this.importsource === 'external')
    },
    domainSelectOptions () {
      var domains = this.options.domains.map((domain) => {
        return {
          label: domain.path || domain.name,
          value: domain.id,
          icon: domain?.icon?.base64image || ''
        }
      })
      domains.unshift({
        label: '',
        value: null
      })
      return domains
    },
    projectSelectOptions () {
      var projects = this.options.projects.map((project) => {
        return {
          label: project.name,
          value: project.id,
          icon: project?.icon?.base64image || ''
        }
      })
      projects.unshift({
        label: '',
        value: null
      })
      return projects
    },
    networkSelectOptions () {
      var networks = this.options.networks.map((network) => {
        return {
          label: network.name + ' (' + network.displaytext + ')',
          value: network.id
        }
      })
      networks.unshift({
        label: '',
        value: null
      })
      return networks
    },
    templateSelectOptions () {
      return this.options.templates.map((template) => {
        return {
          label: template.name,
          value: template.id,
          icon: template?.icon?.base64image || '',
          ostypeid: template.ostypeid,
          ostypename: template.ostypename
        }
      })
    },
    dataDisks () {
      var disks = []
      if (this.resource.disk && this.resource.disk.length > 1) {
        for (var index = 0; index < this.resource.disk.length; ++index) {
          if (index !== this.selectedRootDiskIndex) {
            var disk = { ...this.resource.disk[index] }
            disk.size = disk.capacity / (1024 * 1024 * 1024)
            disk.name = disk.label
            disk.meta = this.getMeta(disk, { controller: 'controller', datastorename: 'datastore', position: 'position' })
            disks.push(disk)
          }
        }
      }
      return disks
    },
    nics () {
      var nics = []
      if (this.resource.nic && this.resource.nic.length > 0) {
        for (var nicEntry of this.resource.nic) {
          var nic = { ...nicEntry }
          nic.name = nic.name || nic.id
          nic.displaytext = nic.name
          if (this.isExternalImport && nic.vlanid === -1) {
            delete nic.vlanid
          }
          if (nic.vlanid) {
            nic.broadcasturi = 'vlan://' + nic.vlanid
            if (nic.isolatedpvlan) {
              nic.broadcasturi = 'pvlan://' + nic.vlanid + '-i' + nic.isolatedpvlan
            }
          }
          if (this.cluster.hypervisortype === 'VMware') {
            nic.meta = this.getMeta(nic, { macaddress: 'mac', vlanid: 'vlan', networkname: 'network' })
          } else {
            nic.meta = this.getMeta(nic, { macaddress: 'mac', vlanid: 'vlan' })
          }
          nics.push(nic)
        }
      }
      return nics
    }
  },
  watch: {
    isOpen (newValue) {
      if (newValue) {
        this.resetForm()
        this.$refs.displayname.focus()
        this.selectMatchingComputeOffering()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        rootdiskid: 0,
        migrateallowed: this.switches.migrateAllowed,
        forced: this.switches.forced,
        forcemstoimportvmfiles: this.switches.forceMsToImportVmFiles,
        domainid: null,
        account: null
      })
      this.rules = reactive({
        displayname: [{ required: true, message: this.$t('message.error.input.value') }],
        templateid: [{ required: this.templateType !== 'auto', message: this.$t('message.error.input.value') }],
        rootdiskid: [{ required: this.templateType !== 'auto', message: this.$t('message.error.input.value') }]
      })
    },
    fetchData () {
      _.each(this.params, (param, name) => {
        if (param.isLoad) {
          this.fetchOptions(param, name)
        }
      })
      this.fetchComputeOfferings({
        keyword: '',
        pageSize: 10,
        page: 1
      })
      this.fetchKvmHostsForConversion()
      if (this.resource?.disk?.length > 1) {
        this.updateSelectedRootDisk()
      }
    },
    getMeta (obj, metaKeys) {
      var meta = []
      for (var key in metaKeys) {
        if (key in obj) {
          meta.push({ key: metaKeys[key], value: obj[key] })
        }
      }
      return meta
    },
    getMinCpu () {
      if (this.isVmRunning) {
        return this.resource.cpunumber
      }
      return 'serviceofferingdetails' in this.computeOffering ? this.computeOffering.serviceofferingdetails.mincpunumber * 1 : 1
    },
    getMinMemory () {
      if (this.isVmRunning) {
        return this.resource.memory
      }
      return 'serviceofferingdetails' in this.computeOffering ? this.computeOffering.serviceofferingdetails.minmemory * 1 : 32
    },
    getMaxCpu () {
      if (this.isVmRunning) {
        return this.resource.cpunumber
      }
      return 'serviceofferingdetails' in this.computeOffering ? this.computeOffering.serviceofferingdetails.maxcpunumber * 1 : Number.MAX_SAFE_INTEGER
    },
    getMaxMemory () {
      if (this.isVmRunning) {
        return this.resource.memory
      }
      return 'serviceofferingdetails' in this.computeOffering ? this.computeOffering.serviceofferingdetails.maxmemory * 1 : Number.MAX_SAFE_INTEGER
    },
    getCPUSpeed () {
      if (!this.computeOffering) {
        return 0
      }
      if (this.computeOffering.cpuspeed) {
        return this.computeOffering.cpuspeed * 1
      }
      return this.resource.cpuspeed * 1 || 0
    },
    fetchOptions (param, name, exclude) {
      if (exclude && exclude.length > 0) {
        if (exclude.includes(name)) {
          return
        }
      }
      this.optionsLoading[name] = true
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
          })
        })
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      }).finally(() => {
        this.optionsLoading[name] = false
      })
    },
    fetchComputeOfferings (options) {
      this.computeOfferingLoading = true
      this.totalComputeOfferings = 0
      this.computeOfferings = []
      this.offeringsMap = []
      api('listServiceOfferings', {
        keyword: options.keyword,
        page: options.page,
        pageSize: options.pageSize,
        details: 'min',
        response: 'json'
      }).then(response => {
        this.totalComputeOfferings = response.listserviceofferingsresponse.count
        if (this.totalComputeOfferings === 0) {
          return
        }
        this.computeOfferings = response.listserviceofferingsresponse.serviceoffering
        this.computeOfferings.map(i => { this.offeringsMap[i.id] = i })
      }).finally(() => {
        this.computeOfferingLoading = false
        this.selectMatchingComputeOffering()
      })
    },
    updateCpuSpeed (name, value) {
      if (this.computeOffering.iscustomized) {
        if (this.computeOffering.serviceofferingdetails) {
          this.updateFieldValue(this.cpuSpeedKey, this.computeOffering.cpuspeed)
        } else {
          this.updateFieldValue(this.cpuSpeedKey, value)
        }
      }
    },
    updateFieldValue (name, value) {
      this.form[name] = value
    },
    updateComputeOffering (id) {
      this.updateFieldValue('computeofferingid', id)
      this.computeOffering = this.computeOfferings.filter(x => x.id === id)[0]
      if (this.computeOffering && !this.computeOffering.iscustomizediops) {
        this.updateFieldValue(this.minIopsKey, undefined)
        this.updateFieldValue(this.maxIopsKey, undefined)
      }
    },
    updateMultiDiskOffering (data) {
      this.dataDisksOfferingsMapping = data
    },
    updateMultiNetworkOffering (data) {
      this.nicsNetworksMapping = data
    },
    defaultTemplateType () {
      if (this.cluster.hypervisortype === 'VMware') {
        return 'auto'
      }
      return 'custom'
    },
    changeTemplateType (e) {
      this.templateType = e.target.value
      if (this.templateType === 'auto') {
        this.updateFieldValue('templateid', undefined)
      }
      this.rules = reactive({
        displayname: [{ required: true, message: this.$t('message.error.input.value') }],
        templateid: [{ required: this.templateType !== 'auto', message: this.$t('message.error.input.value') }],
        rootdiskid: [{ required: this.templateType !== 'auto', message: this.$t('message.error.input.value') }]
      })
    },
    selectMatchingComputeOffering () {
      var offerings = [...this.computeOfferings]
      offerings.sort(function (a, b) {
        return a.cpunumber - b.cpunumber
      })
      for (var offering of offerings) {
        var cpuNumberMatches = false
        var cpuSpeedMatches = false
        var memoryMatches = false
        if (!offering.iscustomized) {
          cpuNumberMatches = offering.cpunumber === this.resource.cpunumber
          cpuSpeedMatches = !this.resource.cpuspeed || offering.cpuspeed === this.resource.cpuspeed
          memoryMatches = offering.memory === this.resource.memory
        } else {
          cpuNumberMatches = cpuSpeedMatches = memoryMatches = true
          if (offering.serviceofferingdetails) {
            cpuNumberMatches = (this.resource.cpunumber >= offering.serviceofferingdetails.mincpunumber &&
              this.resource.cpunumber <= offering.serviceofferingdetails.maxcpunumber)
            memoryMatches = (this.resource.memory >= offering.serviceofferingdetails.minmemory &&
              this.resource.memory <= offering.serviceofferingdetails.maxmemory)
            cpuSpeedMatches = !this.resource.cpuspeed || offering.cpuspeed === this.resource.cpuspeed
          }
        }
        if (cpuNumberMatches && cpuSpeedMatches && memoryMatches) {
          setTimeout(() => {
            this.updateComputeOffering(offering.id)
          }, 250)
          break
        }
      }
    },
    fetchKvmHostsForConversion () {
      api('listHosts', {
        clusterid: this.cluster.id,
        hypervisor: this.cluster.hypervisortype,
        type: 'Routing',
        state: 'Up',
        resourcestate: 'Enabled'
      }).then(json => {
        this.kvmHostsForConversion = json.listhostsresponse.host || []
        this.kvmHostsForConversion.map(host => {
          if (host.instanceconversionsupported !== null && host.instanceconversionsupported !== undefined && host.instanceconversionsupported) {
            host.name = host.name + ' (' + this.$t('label.supported') + ')'
          }
        })
      })
    },
    fetchStoragePoolsForConversion () {
      if (this.selectedStorageOptionForConversion === 'primary') {
        api('listStoragePools', {
          zoneid: this.cluster.zoneid,
          status: 'Up'
        }).then(json => {
          this.storagePoolsForConversion = json.liststoragepoolsresponse.storagepool || []
        })
      } else if (this.selectedStorageOptionForConversion === 'local') {
        const kvmHost = this.kvmHostsForConversion.filter(x => x.id === this.selectedKvmHostForConversion)[0]
        api('listStoragePools', {
          scope: 'HOST',
          ipaddress: kvmHost.ipaddress,
          status: 'Up'
        }).then(json => {
          this.storagePoolsForConversion = json.liststoragepoolsresponse.storagepool || []
        })
      }
    },
    updateSelectedKvmHostForConversion (clusterid, checked, value) {
      if (checked) {
        this.selectedKvmHostForConversion = value
        const kvmHost = this.kvmHostsForConversion.filter(x => x.id === this.selectedKvmHostForConversion)[0]
        if (kvmHost.islocalstorageactive) {
          this.storageOptionsForConversion.push({
            id: 'local',
            name: 'Host Local Storage'
          })
        } else {
          this.resetStorageOptionsForConversion()
        }
      } else {
        this.selectedKvmHostForConversion = null
        this.resetStorageOptionsForConversion()
      }
    },
    updateSelectedStorageOptionForConversion (clusterid, checked, value) {
      if (checked) {
        this.selectedStorageOptionForConversion = value
        this.fetchStoragePoolsForConversion()
        this.showStoragePoolsForConversion = value !== 'secondary'
      } else {
        this.showStoragePoolsForConversion = false
        this.selectedStoragePoolForConversion = null
      }
    },
    resetStorageOptionsForConversion () {
      this.storageOptionsForConversion = [
        {
          id: 'secondary',
          name: 'Secondary Storage'
        }, {
          id: 'primary',
          name: 'Primary Storage'
        }
      ]
    },
    onSelectRootDisk (val) {
      this.selectedRootDiskIndex = val
      this.updateSelectedRootDisk()
    },
    updateSelectedRootDisk () {
      var rootDisk = this.resource.disk[this.selectedRootDiskIndex]
      rootDisk.size = rootDisk.capacity / (1024 * 1024 * 1024)
      rootDisk.name = `${rootDisk.label} (${rootDisk.size} GB)`
      rootDisk.meta = this.getMeta(rootDisk, { controller: 'controller', datastorename: 'datastore', position: 'position' })
      this.selectedRootDiskSources = [rootDisk]
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {
          name: this.resource.name,
          clusterid: this.cluster.id,
          displayname: values.displayname,
          zoneid: this.zoneid,
          importsource: this.importsource,
          hypervisor: this.hypervisor,
          host: this.exthost,
          hostname: values.hostname,
          username: this.username,
          password: this.password,
          hostid: this.host.id,
          storageid: this.pool.id,
          diskpath: this.diskpath,
          temppath: this.tmppath
        }
        var importapi = 'importUnmanagedInstance'
        if (this.isExternalImport || this.isDiskImport || this.selectedVmwareVcenter) {
          importapi = 'importVm'
          if (this.isDiskImport) {
            if (!values.networkid) {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.please.enter.valid.value') + ': ' + this.$t('label.network')
              })
              return
            }
            params.name = values.displayname
            params.networkid = values.networkid
          }
        }
        if (!this.computeOffering || !this.computeOffering.id) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.step.2.continue')
          })
          return
        }
        params.serviceofferingid = values.computeofferingid
        if (this.computeOffering.iscustomized) {
          var details = [this.cpuNumberKey, this.cpuSpeedKey, this.memoryKey]
          for (var detail of details) {
            if (!(values[detail] || this.computeOffering[detail])) {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.please.enter.valid.value') + ': ' + this.$t('label.' + detail.toLowerCase())
              })
              return
            }
            if (values[detail]) {
              params['details[0].' + detail] = values[detail]
            }
          }
        }
        if (this.computeOffering.iscustomizediops) {
          var iopsDetails = [this.minIopsKey, this.maxIopsKey]
          for (var iopsDetail of iopsDetails) {
            if (!values[iopsDetail] || values[iopsDetail] < 0) {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.please.enter.valid.value') + ': ' + this.$t('label.' + iopsDetail.toLowerCase())
              })
              return
            }
            params['details[0].' + iopsDetail] = values[iopsDetail]
          }
          if (values[this.minIopsKey] > values[this.maxIopsKey]) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('error.form.message')
            })
          }
        }
        if (this.isDiskImport) {
          var storageType = this.computeOffering.storagetype
          if (this.importsource !== storageType) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: 'Incompatible Storage. Import Source is: ' + this.importsource + '. Storage Type in service offering is: ' + storageType
            })
            return
          }
        }
        if (this.selectedVmwareVcenter) {
          if (this.selectedVmwareVcenter.existingvcenterid) {
            params.existingvcenterid = this.selectedVmwareVcenter.existingvcenterid
          } else {
            params.vcenter = this.selectedVmwareVcenter.vcenter
            params.datacentername = this.selectedVmwareVcenter.datacentername
            params.username = this.selectedVmwareVcenter.username
            params.password = this.selectedVmwareVcenter.password
          }
          params.hostip = this.resource.hostname
          params.clustername = this.resource.clustername
          if (this.selectedKvmHostForConversion) {
            params.convertinstancehostid = this.selectedKvmHostForConversion
          }
          if (this.selectedStoragePoolForConversion) {
            params.convertinstancepoolid = this.selectedStoragePoolForConversion
          }
          params.forcemstoimportvmfiles = values.forcemstoimportvmfiles
        }
        var keys = ['hostname', 'domainid', 'projectid', 'account', 'migrateallowed', 'forced', 'forcemstoimportvmfiles']
        if (this.templateType !== 'auto') {
          keys.push('templateid')
        }
        for (var key of keys) {
          if (values[key]) {
            params[key] = values[key]
          }
        }
        var diskOfferingIndex = 0
        for (var diskId in this.dataDisksOfferingsMapping) {
          if (!this.dataDisksOfferingsMapping[diskId]) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.select.disk.offering') + ': ' + diskId
            })
            return
          }
          params['datadiskofferinglist[' + diskOfferingIndex + '].disk'] = diskId
          params['datadiskofferinglist[' + diskOfferingIndex + '].diskOffering'] = this.dataDisksOfferingsMapping[diskId]
          diskOfferingIndex++
        }
        var nicNetworkIndex = 0
        var nicIpIndex = 0
        var networkcheck = new Set()
        for (var nicId in this.nicsNetworksMapping) {
          if (!this.nicsNetworksMapping[nicId].network) {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: this.$t('message.select.nic.network') + ': ' + nicId
            })
            return
          }
          params['nicnetworklist[' + nicNetworkIndex + '].nic'] = nicId
          params['nicnetworklist[' + nicNetworkIndex + '].network'] = this.nicsNetworksMapping[nicId].network
          var netId = this.nicsNetworksMapping[nicId].network
          if (!networkcheck.has(netId)) {
            networkcheck.add(netId)
          } else {
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: 'Same network cannot be assigned to multiple Nics'
            })
            return
          }
          nicNetworkIndex++
          if ('ipAddress' in this.nicsNetworksMapping[nicId]) {
            if (!this.nicsNetworksMapping[nicId].ipAddress) {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.enter.valid.nic.ip') + ': ' + nicId
              })
              return
            }
            params['nicipaddresslist[' + nicIpIndex + '].nic'] = nicId
            params['nicipaddresslist[' + nicIpIndex + '].ip4Address'] = this.nicsNetworksMapping[nicId].ipAddress
            nicIpIndex++
          }
        }
        this.updateLoading(true)
        const name = params.name
        return new Promise((resolve, reject) => {
          api(importapi, params).then(response => {
            var jobId
            if (this.isDiskImport || this.isExternalImport || this.selectedVmwareVcenter) {
              jobId = response.importvmresponse.jobid
            } else {
              jobId = response.importunmanagedinstanceresponse.jobid
            }
            let msgLoading = this.$t('label.import.instance') + ' ' + name + ' ' + this.$t('label.in.progress')
            if (this.selectedKvmHostForConversion) {
              const kvmHost = this.kvmHostsForConversion.filter(x => x.id === this.selectedKvmHostForConversion)[0]
              msgLoading += ' on host ' + kvmHost.name
            }
            this.$pollJob({
              jobId,
              title: this.$t('label.import.instance'),
              description: name,
              loadingMessage: msgLoading,
              catchMessage: this.$t('error.fetching.async.job.result'),
              successMessage: this.$t('message.success.import.instance') + ' ' + name,
              successMethod: result => {
                this.$emit('refresh-data')
                resolve(result)
              },
              errorMethod: (result) => {
                this.updateLoading(false)
                reject(result.jobresult.errortext)
              }
            })
          }).catch(error => {
            this.updateLoading(false)
            this.$notifyError(error)
          }).finally(() => {
            this.closeAction()
            this.updateLoading(false)
          })
        })
      }).catch(() => {
        this.$emit('loading-changed', false)
      })
    },
    updateLoading (value) {
      this.loading = value
      this.$emit('loading-changed', value)
    },
    resetForm () {
      var fields = ['displayname', 'hostname', 'domainid', 'account', 'projectid', 'computeofferingid']
      for (var field of fields) {
        this.updateFieldValue(field, undefined)
      }
      this.templateType = this.defaultTemplateType()
      this.updateComputeOffering(undefined)
      this.switches = {}
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

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

.form-layout {
  width: 120vw;

  @media (min-width: 1000px) {
    width: 550px;
  }
}

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
