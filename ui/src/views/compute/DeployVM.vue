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
                        :options="zoneSelectOptions"
                        @change="onSelectZoneId"
                        :loading="loading.zones"
                      ></a-select>
                    </a-form-item>
                    <a-form-item
                      v-if="!isNormalAndDomainUser"
                      :label="this.$t('label.podid')">
                      <a-select
                        v-decorator="['podid']"
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
                          @update-template-iso="updateFieldValue" />
                        <span>
                          {{ $t('label.override.rootdisk.size') }}
                          <a-switch @change="val => { this.showRootDiskSizeChanger = val }" style="margin-left: 10px;"/>
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
                    <compute-offering-selection
                      :compute-items="options.serviceOfferings"
                      :row-count="rowCount.serviceOfferings"
                      :zoneId="zoneId"
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
                :title="this.$t('label.diskofferingid')"
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
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description">
                  <div v-if="zoneSelected">
                    <network-selection
                      v-if="!networkId"
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
                v-if="vm.templateid && template.properties && template.properties.length > 0">
                <template slot="description">
                  <div>
                    <a-form-item
                      v-for="(property, propertyIndex) in template.properties"
                      :key="propertyIndex"
                      :v-bind="property.key" >
                      <span slot="label">
                        {{ property.label }}
                        <a-tooltip :title="property.description">
                          <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                        </a-tooltip>
                      </span>

                      <span v-if="property.type && property.type==='boolean'">
                        <a-switch
                          v-decorator="['properties.' + property.key, { initialValue: property.value==='TRUE'?true:false}]"
                          :defaultChecked="property.value==='TRUE'?true:false"
                          :placeholder="property.description"
                        />
                      </span>
                      <span v-else-if="property.type && (property.type==='int' || property.type==='real')">
                        <a-input-number
                          v-decorator="['properties.'+property.key]"
                          :defaultValue="property.value"
                          :placeholder="property.description"
                          :min="property.qualifiers && property.qualifiers.includes('MinValue') && property.qualifiers.includes('MaxValue')?property.qualifiers.split(',')[0].replace('MinValue(','').slice(0, -1):0"
                          :max="property.qualifiers && property.qualifiers.includes('MinValue') && property.qualifiers.includes('MaxValue')?property.qualifiers.split(',')[1].replace('MaxValue(','').slice(0, -1):property.type==='real'?1:Number.MAX_SAFE_INTEGER" />
                      </span>
                      <span v-else-if="property.type && property.type==='string' && property.qualifiers && property.qualifiers.startsWith('ValueMap')">
                        <a-select
                          showSearch
                          optionFilterProp="children"
                          v-decorator="['properties.' + property.key, { initialValue: property.value }]"
                          :placeholder="property.description"
                          :filterOption="(input, option) => {
                            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                          }"
                        >
                          <a-select-option :v-if="property.value===''" key="">{{ }}</a-select-option>
                          <a-select-option v-for="opt in property.qualifiers.replace('ValueMap','').substr(1).slice(0, -1).split(',')" :key="removeQuotes(opt)">
                            {{ removeQuotes(opt) }}
                          </a-select-option>
                        </a-select>
                      </span>
                      <span v-else-if="property.type && property.type==='string' && property.password">
                        <a-input-password
                          v-decorator="['properties.' + property.key, { initialValue: property.value }]"
                          :placeholder="property.description" />
                      </span>
                      <span v-else>
                        <a-input
                          v-decorator="['properties.' + property.key, { initialValue: property.value }]"
                          :placeholder="property.description" />
                      </span>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
              <a-step
                :title="$t('label.advanced.mode')"
                :status="zoneSelected ? 'process' : 'wait'">
                <template slot="description" v-if="zoneSelected">
                  <span>
                    {{ $t('label.isadvanced') }}
                    <a-switch @change="val => { this.showDetails = val }" style="margin-left: 10px"/>
                  </span>
                  <div style="margin-top: 15px" v-show="this.showDetails">
                    <div
                      v-if="vm.templateid && ['KVM', 'VMware'].includes(hypervisor)">
                      <a-form-item :label="$t('label.vm.boottype')">
                        <a-select
                          v-decorator="['boottype']"
                          @change="fetchBootModes"
                        >
                          <a-select-option v-for="bootType in options.bootTypes" :key="bootType.id">
                            {{ bootType.description }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                      <a-form-item :label="$t('label.vm.bootmode')">
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
                      <a-input v-decorator="['group']" />
                    </a-form-item>
                    <a-form-item :label="$t('label.keyboard')">
                      <a-select
                        v-decorator="['keyboard']"
                        :options="keyboardSelectOptions"
                      ></a-select>
                    </a-form-item>
                  </div>
                </template>
              </a-step>
            </a-steps>
            <div class="card-footer">
              <!-- ToDo extract as component -->
              <a-button @click="() => this.$router.back()" :loading="loading.deploy">
                {{ this.$t('label.cancel') }}
              </a-button>
              <a-button type="primary" @click="handleSubmit" :loading="loading.deploy">
                <a-icon type="rocket" />
                {{ this.$t('label.launch.vm') }}
              </a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7" v-if="!isMobile()">
        <a-affix :offsetTop="75">
          <info-card class="vm-info-card" :resource="vm" :title="this.$t('label.yourinstance')">
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
import ComputeOfferingSelection from '@views/compute/wizard/ComputeOfferingSelection'
import ComputeSelection from '@views/compute/wizard/ComputeSelection'
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
      podId: null,
      clusterId: null,
      zoneSelected: false,
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
        bootModes: []
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
        SSH_KEY_PAIR: 6,
        ENABLE_SETUP: 7
      },
      initDataConfig: {},
      defaultNetwork: '',
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
      showRootDiskSizeChanger: false
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
    networkId () {
      return this.$route.query.networkid || null
    },
    networkName () {
      return this.$route.query.name || null
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

      if (instanceConfig.hypervisor) {
        var hypervisorItem = _.find(this.options.hypervisors, (option) => option.name === instanceConfig.hypervisor)
        this.hypervisor = hypervisorItem ? hypervisorItem.name : null
      }

      this.serviceOffering = _.find(this.options.serviceOfferings, (option) => option.id === instanceConfig.computeofferingid)
      this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.diskofferingid)
      this.zone = _.find(this.options.zones, (option) => option.id === instanceConfig.zoneid)
      this.affinityGroups = _.filter(this.options.affinityGroups, (option) => _.includes(instanceConfig.affinitygroupids, option.id))
      if (!this.networkId) {
        this.networks = _.filter(this.options.networks, (option) => _.includes(instanceConfig.networkids, option.id))
      }
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
        Object.keys(fields).forEach(field => {
          if (field in this.vm) {
            this.vm[field] = this.instanceConfig[field]
          }
        })
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
    removeQuotes (value) {
      return value.replace(/"/g, '')
    },
    fillValue (field) {
      this.form.getFieldDecorator([field], { initialValue: this.dataPreFill[field] })
    },
    fetchData () {
      if (this.networkId) {
        this.updateNetworks([this.networkId])
        this.updateDefaultNetworks(this.networkId)
        this.networks = [{
          id: this.networkId,
          name: this.networkName
        }]
      }

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
      Vue.nextTick().then(() => {
        ['name', 'keyboard', 'boottype', 'bootmode', 'userdata'].forEach(this.fillValue)
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
        const templates = this.options.templates.filter(x => x.id === value)
        if (templates.length > 0) {
          var size = templates[0].size / (1024 * 1024 * 1024) || 0 // bytes to GB
          this.dataPreFill.minrootdisksize = Math.ceil(size)
        }
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
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    handleSubmit (e) {
      console.log('wizard submit')
      e.preventDefault()
      this.form.validateFields(async (err, values) => {
        if (err) {
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

        this.loading.deploy = true

        let networkIds = []

        const deployVmData = {}
        // step 1 : select zone
        deployVmData.zoneid = values.zoneid
        deployVmData.podid = values.podid
        deployVmData.clusterid = values.clusterid
        deployVmData.hostid = values.hostid
        deployVmData.group = values.group
        deployVmData.keyboard = values.keyboard
        deployVmData.boottype = values.boottype
        deployVmData.bootmode = values.bootmode
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
        const arrNetwork = []
        networkIds = values.networkids
        if (networkIds.length > 0) {
          for (let i = 0; i < networkIds.length; i++) {
            if (networkIds[i] === this.defaultNetwork) {
              const ipToNetwork = {
                networkid: this.defaultNetwork
              }
              arrNetwork.unshift(ipToNetwork)
            } else {
              const ipToNetwork = {
                networkid: networkIds[i]
              }
              arrNetwork.push(ipToNetwork)
            }
          }
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
        // step 7: select ssh key pair
        deployVmData.keypair = values.keypair
        deployVmData.name = values.name
        deployVmData.displayname = values.name
        // step 8: enter setup
        if ('properties' in values) {
          const keys = Object.keys(values.properties)
          for (var i = 0; i < keys.length; ++i) {
            deployVmData['properties[' + i + '].key'] = keys[i]
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
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
            this.$store.dispatch('AddAsyncJob', {
              title: title,
              jobid: jobId,
              description: description,
              status: 'progress'
            })
          }
          this.$router.back()
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
    fetchAllTemplates (filterKeys) {
      const promises = []
      this.options.templates = []
      this.loading.templates = true
      this.templateFilter.forEach((filter) => {
        if (filterKeys && !filterKeys.includes(filter)) {
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
          return true
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
