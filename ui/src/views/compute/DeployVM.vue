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
        <a-card :bordered="true" :title="this.$t('newInstance')">
          <a-form
            :form="form"
            @submit="handleSubmit"
            layout="vertical"
          >
            <a-steps direction="vertical" size="small">
              <a-step :title="this.$t('details')" status="process">
                <template slot="description">
                  <div style="margin-top: 15px">
                    <a-form-item :label="this.$t('name')">
                      <a-input
                        v-decorator="['name']"
                      />
                    </a-form-item>
                    <a-form-item :label="this.$t('zoneid')">
                      <a-select
                        v-decorator="['zoneid', {
                          rules: [{ required: true, message: 'Please select option' }]
                        }]"
                        :options="zoneSelectOptions"
                        @change="onSelectZoneId"
                        :loading="loading.zones"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('podId')">
                      <a-select
                        v-decorator="['podid']"
                        :options="podSelectOptions"
                        :loading="loading.pods"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('clusterid')">
                      <a-select
                        v-decorator="['clusterid']"
                        :options="clusterSelectOptions"
                        :loading="loading.clusters"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('hostId')">
                      <a-select
                        v-decorator="['hostid']"
                        :options="hostSelectOptions"
                        :loading="loading.hosts"
                      ></a-select>
                    </a-form-item>
                    <a-form-item :label="this.$t('group')">
                      <a-select
                        v-decorator="['group']"
                        :options="groupsSelectOptions"
                        :loading="loading.groups"
                      ></a-select>
                    </a-form-item>
                    <a-form-item :label="this.$t('keyboard')">
                      <a-select
                        v-decorator="['keyboard']"
                        :options="keyboardSelectOptions"
                      ></a-select>
                    </a-form-item>
                    <a-form-item :label="this.$t('userdata')">
                      <a-textarea
                        v-decorator="['userdata']">
                      </a-textarea>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="this.$t('templateIso')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected" style="margin-top: 15px">
                    <a-card
                      :tabList="tabList"
                      :activeTabKey="tabKey"
                      @tabChange="key => onTabChange(key, 'tabKey')">
                      <p v-if="tabKey === 'templateid'">
                        <template-iso-selection
                          input-decorator="templateid"
                          :items="options.templates"
                          :selected="tabKey"
                          :loading="loading.templates"
                          :preFillContent="dataPreFill"
                          @update-template-iso="updateFieldValue"
                        ></template-iso-selection>
                        <disk-size-selection
                          input-decorator="rootdisksize"
                          :preFillContent="dataPreFill"
                          @update-disk-size="updateFieldValue"/>
                      </p>
                      <p v-else>
                        <template-iso-selection
                          input-decorator="isoid"
                          :items="options.isos"
                          :selected="tabKey"
                          :loading="loading.isos"
                          :preFillContent="dataPreFill"
                          @update-template-iso="updateFieldValue"
                        ></template-iso-selection>
                        <a-form-item :label="this.$t('hypervisor')">
                          <a-select
                            v-decorator="['hypervisor', {
                              initialValue: hypervisorSelectOptions && hypervisorSelectOptions.length > 0
                                ? hypervisorSelectOptions[0].value
                                : null,
                              rules: [{ required: true, message: 'Please select option' }]
                            }]"
                            :options="hypervisorSelectOptions"
                            @change="value => this.hypervisor = value"
                          >
                          </a-select>
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
                :title="this.$t('serviceOfferingId')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <compute-offering-selection
                      :compute-items="options.serviceOfferings"
                      :value="serviceOffering ? serviceOffering.id : ''"
                      :loading="loading.serviceOfferings"
                      :preFillContent="dataPreFill"
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
                      :minCpu="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.mincpunumber*1 : 1"
                      :maxCpu="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.maxcpunumber*1 : Number.MAX_SAFE_INTEGER"
                      :minMemory="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.minmemory*1 : 1"
                      :maxMemory="'serviceofferingdetails' in serviceOffering ? serviceOffering.serviceofferingdetails.maxmemory*1 : Number.MAX_SAFE_INTEGER"
                      @update-compute-cpunumber="updateFieldValue"
                      @update-compute-cpuspeed="updateFieldValue"
                      @update-compute-memory="updateFieldValue" />
                    <span v-if="serviceOffering && serviceOffering.iscustomized">
                      <a-form-item class="form-item-hidden" >
                        <a-input v-decorator="['cpunumber']"/>
                      </a-form-item>
                      <a-form-item
                        class="form-item-hidden"
                        v-if="serviceOffering && !(serviceOffering.cpuspeed > 0)">
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
                :title="this.$t('diskofferingid')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <disk-offering-selection
                      :items="options.diskOfferings"
                      :value="diskOffering ? diskOffering.id : ''"
                      :loading="loading.diskOfferings"
                      :preFillContent="dataPreFill"
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
                :title="this.$t('Affinity Groups')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <affinity-group-selection
                      :items="options.affinityGroups"
                      :value="affinityGroupIds"
                      :loading="loading.affinityGroups"
                      :preFillContent="dataPreFill"
                      @select-affinity-group-item="($event) => updateAffinityGroups($event)"
                      @handle-search-filter="($event) => handleSearchFilter('affinityGroups', $event)"
                    ></affinity-group-selection>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="this.$t('networks')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <network-selection
                      :items="options.networks"
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
                </template>
              </a-step>
              <a-step
                :title="this.$t('sshKeyPairs')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <ssh-key-pair-selection
                      :items="options.sshKeyPairs"
                      :value="sshKeyPair ? sshKeyPair.name : ''"
                      :loading="loading.sshKeyPairs"
                      :preFillContent="dataPreFill"
                      @select-ssh-key-pair-item="($event) => updateSshKeyPairs($event)"
                      @handle-search-filter="($event) => handleSearchFilter('sshKeyPairs', $event)"
                    />
                  </div>
                </template>
              </a-step>
            </a-steps>
            <div class="card-footer">
              <!-- ToDo extract as component -->
              <a-button @click="() => this.$router.back()" :loading="loading.deploy">
                {{ this.$t('cancel') }}
              </a-button>
              <a-button type="primary" @click="handleSubmit" :loading="loading.deploy">
                <a-icon type="rocket" />
                {{ this.$t('Launch VM') }}
              </a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7" v-if="!isMobile()">
        <a-affix :offsetTop="75">
          <info-card class="vm-info-card" :resource="vm" :title="this.$t('yourInstance')">
            <!-- ToDo: Refactor this, maybe move everything to the info-card component -->
            <div slot="details" v-if="diskSize" style="margin-bottom: 12px;">
              <a-icon type="hdd"></a-icon>
              <span style="margin-left: 10px">{{ diskSize }}</span>
            </div>
            <div slot="details" v-if="networks">
              <div v-for="network in networks" :key="network.id" style="margin-bottom: 12px;">
                <a-icon type="api"></a-icon>
                <span style="margin-left: 10px">{{ network.name }}</span>
              </div>
            </div>
          </info-card>
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

import InfoCard from '@/components/view/InfoCard'
import ComputeOfferingSelection from './wizard/ComputeOfferingSelection'
import ComputeSelection from './wizard/ComputeSelection'
import DiskOfferingSelection from '@views/compute/wizard/DiskOfferingSelection'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'
import TemplateIsoSelection from '@views/compute/wizard/TemplateIsoSelection'
import AffinityGroupSelection from '@views/compute/wizard/AffinityGroupSelection'
import NetworkSelection from '@views/compute/wizard/NetworkSelection'
import NetworkConfiguration from '@views/compute/wizard/NetworkConfiguration'
import SshKeyPairSelection from '@views/compute/wizard/SshKeyPairSelection'

export default {
  name: 'Wizard',
  components: {
    SshKeyPairSelection,
    NetworkConfiguration,
    NetworkSelection,
    AffinityGroupSelection,
    TemplateIsoSelection,
    DiskSizeSelection,
    DiskOfferingSelection,
    InfoCard,
    ComputeOfferingSelection,
    ComputeSelection
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
      zoneSelected: false,
      vm: {},
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
        keyboards: []
      },
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
      steps: {
        BASIC: 0,
        TEMPLATE_ISO: 1,
        COMPUTE: 2,
        DISK_OFFERING: 3,
        AFFINITY_GROUP: 4,
        NETWORK: 5,
        SSH_KEY_PAIR: 6
      },
      initDataConfig: {},
      defaultNetwork: '',
      networkConfig: [],
      tabList: [
        {
          key: 'templateid',
          tab: this.$t('Templates')
        },
        {
          key: 'isoid',
          tab: this.$t('ISOs')
        }
      ],
      tabKey: 'templateid',
      dataPreFill: {}
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
            projectid: store.getters.project.id,
            domainid: store.getters.project.id ? null : store.getters.userInfo.domainid,
            account: store.getters.project.id ? null : store.getters.userInfo.account,
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
            zoneid: _.get(this.zone, 'id')
          },
          field: 'clusterid'
        },
        hosts: {
          list: 'listHosts',
          isLoad: !this.isNormalAndDomainUser,
          options: {
            zoneid: _.get(this.zone, 'id'),
            state: 'Up',
            type: 'Routing'
          },
          field: 'hostid'
        },
        groups: {
          list: 'listInstanceGroups',
          options: {
            listall: false
          },
          isLoad: true,
          field: 'group'
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
      return this.options.pods.map((pod) => {
        return {
          label: pod.name,
          value: pod.id
        }
      })
    },
    clusterSelectOptions () {
      return this.options.clusters.map((cluster) => {
        return {
          label: cluster.name,
          value: cluster.id
        }
      })
    },
    hostSelectOptions () {
      return this.options.hosts.map((host) => {
        return {
          label: host.name,
          value: host.id
        }
      })
    },
    keyboardSelectOptions () {
      return this.options.keyboards.map((keyboard) => {
        return {
          label: this.$t(keyboard.description),
          value: keyboard.id
        }
      })
    },
    groupsSelectOptions () {
      return this.options.groups.map((group) => {
        return {
          label: group.name,
          value: group.id
        }
      })
    }
  },
  watch: {
    '$route' (to, from) {
      if (to.name === 'deployVirtualMachine') {
        this.resetData()
      }
    },
    instanceConfig (instanceConfig) {
      this.template = _.find(this.options.templates, (option) => option.id === instanceConfig.templateid)
      this.iso = _.find(this.options.isos, (option) => option.id === instanceConfig.isoid)
      var hypervisorItem = _.find(this.options.hypervisors, (option) => option.name === instanceConfig.hypervisor)
      this.hypervisor = hypervisorItem ? hypervisorItem.name : null
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
        this.vm.cpunumber = this.serviceOffering.cpunumber
        this.vm.cpuspeed = this.serviceOffering.cpuspeed
        this.vm.memory = this.serviceOffering.memory
      }

      if (this.diskOffering) {
        this.vm.diskofferingid = this.diskOffering.id
        this.vm.diskofferingname = this.diskOffering.displaytext
        this.vm.diskofferingsize = this.diskOffering.disksize
      }

      if (this.affinityGroups) {
        this.vm.affinitygroup = this.affinityGroups
      }
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
        this.vm = Object.assign({}, this.instanceConfig)
      }
    })
    this.form.getFieldDecorator('computeofferingid', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('diskofferingid', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('affinitygroupids', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('networkids', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('keypair', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('cpunumber', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('cpuSpeed', { initialValue: undefined, preserve: true })
    this.form.getFieldDecorator('memory', { initialValue: undefined, preserve: true })
  },
  mounted () {
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

      this.fetchKeyboard()
      Vue.nextTick().then(() => {
        ['name', 'keyboard', 'userdata'].forEach(this.fillValue)
        this.instanceConfig = this.form.getFieldsValue() // ToDo: maybe initialize with some other defaults
      })
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
    fetchKeyboard () {
      const keyboardType = []
      keyboardType.push({
        id: '',
        description: ''
      })
      keyboardType.push({
        id: 'us',
        description: 'label.standard.us.keyboard'
      })
      keyboardType.push({
        id: 'uk',
        description: 'label.uk.keyboard'
      })
      keyboardType.push({
        id: 'fr',
        description: 'label.french.azerty.keyboard'
      })
      keyboardType.push({
        id: 'jp',
        description: 'label.japanese.keyboard'
      })
      keyboardType.push({
        id: 'sc',
        description: 'label.simplified.chinese.keyboard'
      })

      this.$set(this.options, 'keyboards', keyboardType)
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
      } else if (name === 'isoid') {
        this.tabKey = 'isoid'
        this.form.setFieldsValue({
          isoid: value,
          templateid: null
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
      this.defaultNetwork = id
    },
    updateNetworkConfig (networks) {
      this.networkConfig = networks
    },
    updateSshKeyPairs (name) {
      if (name === this.$t('noselect')) {
        this.form.setFieldsValue({
          keypair: undefined
        })
        return
      }
      this.form.setFieldsValue({
        keypair: name
      })
    },
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    handleSubmit (e) {
      console.log('wizard submit')
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const deployVmData = {}
        // step 1 : select zone
        deployVmData.zoneid = values.zoneid
        deployVmData.podid = values.podid
        deployVmData.clusterid = values.clusterid
        deployVmData.hostid = values.hostid
        deployVmData.group = values.group
        deployVmData.keyboard = values.keyboard
        if (values.userdata && values.userdata.length > 0) {
          deployVmData.userdata = encodeURIComponent(btoa(this.sanitizeReverse(values.userdata)))
        }
        // step 2: select template/iso
        if (this.tabKey === 'templateid') {
          deployVmData.templateid = values.templateid
        } else {
          deployVmData.templateid = values.isoid
        }
        if (values.rootdisksize && values.rootdisksize > 0) {
          deployVmData.rootdisksize = values.rootdisksize
        }
        if (values.hypervisor && values.hypervisor.length > 0) {
          deployVmData.hypervisor = values.hypervisor
        }
        // step 3: select service offering
        deployVmData.serviceofferingid = values.computeofferingid
        if (values.cpunumber || values.cpuspeed || values.memory) {
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
        // step 4: select disk offering
        deployVmData.diskofferingid = values.diskofferingid
        if (values.size) {
          deployVmData.size = values.size
        }
        // step 5: select an affinity group
        deployVmData.affinitygroupids = (values.affinitygroupids || []).join(',')
        // step 6: select network
        if (values.networkids && values.networkids.length > 0) {
          for (let i = 0; i < values.networkids.length; i++) {
            deployVmData['iptonetworklist[' + i + '].networkid'] = values.networkids[i]
            if (this.networkConfig.length > 0) {
              const networkConfig = this.networkConfig.filter((item) => item.key === values.networkids[i])
              if (networkConfig && networkConfig.length > 0) {
                deployVmData['iptonetworklist[' + i + '].ip'] = networkConfig[0].ipAddress ? networkConfig[0].ipAddress : undefined
                deployVmData['iptonetworklist[' + i + '].mac'] = networkConfig[0].macAddress ? networkConfig[0].macAddress : undefined
              }
            }
          }
        }
        // step 7: select ssh key pair
        deployVmData.keypair = values.keypair
        deployVmData.name = values.name
        deployVmData.displayname = values.name
        const title = this.$t('Launch Virtual Machine')
        const description = deployVmData.name ? deployVmData.name : values.zoneid
        this.loading.deploy = true
        api('deployVirtualMachine', deployVmData).then(response => {
          const jobId = response.deployvirtualmachineresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              successMethod: result => {
                let successDescription = ''
                if (result.jobresult.virtualmachine.name) {
                  successDescription = result.jobresult.virtualmachine.name
                } else {
                  successDescription = result.jobresult.virtualmachine.id
                }
                this.$store.dispatch('AddAsyncJob', {
                  title: title,
                  jobid: jobId,
                  description: successDescription,
                  status: 'progress'
                })
              },
              loadingMessage: `${title} in progress for ${description}`,
              catchMessage: 'Error encountered while fetching async job result'
            })
          }
          this.$router.back()
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
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
            this.options[name] = []
            this.$forceUpdate()
            return
          }
          if (!responseKey.includes('response')) {
            return
          }
          _.map(responseItem, (response, key) => {
            if (key === 'count') {
              return
            }
            param.opts = response
            this.options[name] = response
            this.$forceUpdate()
            if (param.field) {
              this.fillValue(param.field)
            }
          })
        })
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      }).finally(() => {
        this.loading[name] = false
      })
    },
    fetchTemplates (templateFilter) {
      return new Promise((resolve, reject) => {
        api('listTemplates', {
          zoneid: _.get(this.zone, 'id'),
          templatefilter: templateFilter
        }).then((response) => {
          resolve(response)
        }).catch((reason) => {
          // ToDo: Handle errors
          reject(reason)
        })
      })
    },
    fetchIsos (isoFilter) {
      return new Promise((resolve, reject) => {
        api('listIsos', {
          zoneid: _.get(this.zone, 'id'),
          isofilter: isoFilter,
          bootable: true
        }).then((response) => {
          resolve(response)
        }).catch((reason) => {
          // ToDo: Handle errors
          reject(reason)
        })
      })
    },
    fetchAllTemplates (filterKey) {
      const promises = []
      this.options.templates = []
      this.loading.templates = true
      this.templateFilter.forEach((filter) => {
        if (filterKey && filterKey !== filter) {
          return true
        }
        promises.push(this.fetchTemplates(filter))
      })
      Promise.all(promises).then(response => {
        response.forEach((resItem) => {
          const concatTemplates = _.concat(this.options.templates, _.get(resItem, 'listtemplatesresponse.template', []))
          this.options.templates = _.uniqWith(concatTemplates, _.isEqual)
          this.$forceUpdate()
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.templates = false
      })
    },
    fetchAllIsos (filterKey) {
      const promises = []
      this.options.isos = []
      this.loading.isos = true
      this.isoFilter.forEach((filter) => {
        if (filterKey && filterKey !== filter) {
          return true
        }
        promises.push(this.fetchIsos(filter))
      })
      Promise.all(promises).then(response => {
        response.forEach((resItem) => {
          const concatedIsos = _.concat(this.options.isos, _.get(resItem, 'listisosresponse.iso', []))
          this.options.isos = _.uniqWith(concatedIsos, _.isEqual)
          this.$forceUpdate()
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.isos = false
      })
    },
    onSelectZoneId (value) {
      this.dataPreFill = {}
      this.zoneId = value
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
        if (!('isLoad' in param) || param.isLoad) {
          this.fetchOptions(param, name, ['zones', 'groups'])
        }
      })
      this.fetchAllTemplates()
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
