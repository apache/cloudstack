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
  <a-spin :spinning="fetchLoading">
    <a-button
      type="dashed"
      style="width: 100%;margin-bottom: 20px;"
      :disabled="!('createNetwork' in $store.getters.apis)"
      @click="handleOpenModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.new.tier') }}
    </a-button>
    <a-list class="list">
      <a-list-item v-for="(network, idx) in networks" :key="idx" class="list__item">
        <div class="list__item-outer-container">
          <div class="list__item-container">
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.name') }}
              </div>
              <div>
                <router-link :to="{ path: '/guestnetwork/' + network.id }">{{ network.name }} </router-link>
                <a-tag v-if="network.broadcasturi">{{ network.broadcasturi }}</a-tag>
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">{{ $t('label.state') }}</div>
              <div><status :text="network.state" displayText></status></div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.cidr') }}
              </div>
              <div>{{ network.cidr }}</div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.aclid') }}
              </div>
              <div>
                <router-link :to="{ path: '/acllist/' + network.aclid }">
                  {{ network.aclname }}
                </router-link>
              </div>
            </div>
          </div>
          <a-collapse :bordered="false" style="margin-left: -18px">
            <template #expandIcon="props">
              <caret-right-outlined :rotate="props.isActive ? 90 : 0" />
            </template>
            <a-collapse-panel :header="$t('label.instances')" key="vm" :style="customStyle">
              <a-button
                type="dashed"
                style="margin-bottom: 15px; width: 100%"
                :disabled="!('deployVirtualMachine' in $store.getters.apis)"
                @click="$router.push({ path: '/action/deployVirtualMachine', query: { networkid: network.id, zoneid: network.zoneid } })">
                <template #icon><plus-outlined /></template>
                {{ $t('label.vm.add') }}
              </a-button>
              <a-table
                class="table"
                size="small"
                :columns="vmCols"
                :dataSource="vms[network.id]"
                :rowKey="item => item.id"
                :pagination="false"
                :loading="fetchLoading">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'">
                    <router-link :to="{ path: '/vm/' + record.id}">{{ record.name }}
                    </router-link>
                  </template>
                  <template v-if="column.key === 'state'">
                    <status :text="record.state" displayText></status>
                  </template>
                  <template v-if="column.key === 'ip'">
                    <div v-for="nic in record.nic" :key="nic.id">
                      {{ nic.networkid === network.id ? nic.ipaddress : '' }}
                    </div>
                  </template>
                </template>
              </a-table>
              <a-divider/>
              <a-pagination
                class="row-element pagination"
                size="small"
                :current="page"
                :pageSize="pageSize"
                :total="itemCounts.vms[network.id]"
                :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
                :pageSizeOptions="['10', '20', '40', '80', '100']"
                @change="changePage"
                @showSizeChange="changePageSize"
                showSizeChanger>
                <template #buildOptionText="props">
                  <span>{{ props.value }} / {{ $t('label.page') }}</span>
                </template>
              </a-pagination>
            </a-collapse-panel>
            <a-collapse-panel :header="$t('label.internal.lb')" key="ilb" :style="customStyle" :collapsible="!showIlb(network) ? 'disabled' : null" >
              <a-button
                type="dashed"
                style="margin-bottom: 15px; width: 100%"
                :disabled="!('createLoadBalancer' in $store.getters.apis)"
                @click="handleAddInternalLB(network.id)">
                <template #icon><plus-outlined /></template>
                {{ $t('label.add.internal.lb') }}
              </a-button>
              <a-table
                class="table"
                size="small"
                :columns="internalLbCols"
                :dataSource="internalLB[network.id]"
                :rowKey="item => item.id"
                :pagination="false"
                :loading="fetchLoading">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'">
                    <router-link :to="{ path: '/ilb/'+ record.id}">{{ record.name }}
                    </router-link>
                  </template>
                </template>
              </a-table>
              <a-divider/>
              <a-pagination
                class="row-element pagination"
                size="small"
                :current="page"
                :pageSize="pageSize"
                :total="itemCounts.internalLB[network.id]"
                :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
                :pageSizeOptions="['10', '20', '40', '80', '100']"
                @change="changePage"
                @showSizeChange="changePageSize"
                showSizeChanger>
                <template #buildOptionText="props">
                  <span>{{ props.value }} / {{ $t('label.page') }}</span>
                </template>
              </a-pagination>
            </a-collapse-panel>
          </a-collapse>
        </div>
      </a-list-item>
    </a-list>

    <a-modal
      :visible="showCreateNetworkModal"
      :title="$t('label.add.new.tier')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="showCreateNetworkModal = false">
      <a-spin :spinning="modalLoading" v-ctrl-enter="handleAddNetworkSubmit">
        <a-form
          layout="vertical"
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleAddNetworkSubmit"
         >
          <a-form-item ref="name" name="name" :colon="false">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="$t('label.create.tier.name.description')"/>
            </template>
            <a-input
              :placeholder="$t('label.create.tier.name.description')"
              v-model:value="form.name"
              v-focus="true"></a-input>
          </a-form-item>
          <a-form-item ref="networkOffering" name="networkOffering" :colon="false">
            <template #label>
              <tooltip-label :title="$t('label.networkofferingid')" :tooltip="$t('label.create.tier.networkofferingid.description')"/>
            </template>
            <a-select
              v-model:value="form.networkOffering"
              @change="val => { this.handleNetworkOfferingChange(val) }"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="item in networkOfferings" :key="item.id" :value="item.id" :label="item.displaytext || item.name || item.description">
                {{ item.displaytext || item.name || item.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            v-if="setMTU"
            ref="privatemtu"
            name="privatemtu">
            <template #label>
              <tooltip-label :title="$t('label.privatemtu')" :tooltip="$t('label.privatemtu')"/>
            </template>
            <a-input-number
              style="width: 100%;"
              v-model:value="form.privatemtu"
              :placeholder="$t('label.privatemtu')"
              @change="updateMtu()"/>
              <div style="color: red" v-if="errorPrivateMtu" v-html="errorPrivateMtu.replace('%x', privateMtuMax)"></div>
          </a-form-item>
          <a-form-item v-if="!isObjectEmpty(selectedNetworkOffering) && selectedNetworkOffering.specifyvlan">
            <template #label>
              <tooltip-label :title="$t('label.vlan')" :tooltip="$t('label.vlan')"/>
            </template>
            <a-input
              v-model:value="form.vlan"
              :placeholder="$t('label.vlan')"/>
          </a-form-item>
          <a-form-item ref="gateway" name="gateway" :colon="false">
            <template #label>
              <tooltip-label :title="$t('label.gateway')" :tooltip="$t('label.create.tier.gateway.description')"/>
            </template>
            <a-input
              :placeholder="$t('label.create.tier.gateway.description')"
              v-model:value="form.gateway"></a-input>
          </a-form-item>
          <a-form-item ref="netmask" name="netmask" :colon="false">
            <template #label>
              <tooltip-label :title="$t('label.netmask')" :tooltip="$t('label.create.tier.netmask.description')"/>
            </template>
            <a-input
              :placeholder="$t('label.create.tier.netmask.description')"
              v-model:value="form.netmask"></a-input>
          </a-form-item>
          <a-form-item ref="externalId" name="externalId" :colon="false">
            <template #label>
              <tooltip-label :title="$t('label.externalid')" :tooltip="$t('label.create.tier.externalid.description')"/>
            </template>
            <a-input
              :placeholder=" $t('label.create.tier.externalid.description')"
              v-model:value="form.externalId"/>
          </a-form-item>
          <a-form-item ref="acl" name="acl" :colon="false">
            <template #label>
              <tooltip-label :title="$t('label.aclid')" :tooltip="$t('label.create.tier.aclid.description')"/>
            </template>
            <a-select
              :placeholder="$t('label.create.tier.aclid.description')"
              v-model:value="form.acl"
              @change="val => { handleNetworkAclChange(val) }"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="item in networkAclList" :key="item.id" :value="item.id" :label="`${item.name}(${item.description})`">
                <strong>{{ item.name }}</strong> ({{ item.description }})
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-alert v-if="selectedNetworkAcl.name==='default_allow'" type="warning" show-icon>
            <template #message><div v-html="$t('message.network.acl.default.allow')"/></template>
          </a-alert>
          <a-alert v-else-if="selectedNetworkAcl.name==='default_deny'" type="warning" show-icon>
            <template #message><div v-html="$t('message.network.acl.default.deny')"/></template>
          </a-alert>
          <div :span="24" class="action-button">
            <a-button @click="showCreateNetworkModal = false">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="handleAddNetworkSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>

    <a-modal
      :visible="showAddInternalLB"
      :title="$t('label.add.internal.lb')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="showAddInternalLB = false">
      <a-spin :spinning="modalLoading" v-ctrl-enter="handleAddInternalLBSubmit">
        <a-form
          layout="vertical"
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleAddInternalLBSubmit"
         >
          <a-form-item ref="name" name="name" :label="$t('label.name')">
            <a-input
              v-focus="true"
              :placeholder="$t('label.internallb.name.description')"
              v-model:value="form.name"/>
          </a-form-item>
          <a-form-item ref="description" name="description" :label="$t('label.description')">
            <a-input
              :placeholder="$t('label.internallb.description')"
              v-model:value="form.description"/>
          </a-form-item>
          <a-form-item ref="sourceIP" name="sourceIP" :label="$t('label.sourceipaddress')">
            <a-input
              :placeholder="$t('label.internallb.sourceip.description')"
              v-model:value="form.sourceIP"/>
          </a-form-item>
          <a-form-item ref="sourcePort" name="sourcePort" :label="$t('label.sourceport')">
            <a-input v-model:value="form.sourcePort"/>
          </a-form-item>
          <a-form-item ref="instancePort" name="instancePort" :label="$t('label.instanceport')">
            <a-input v-model:value="form.instancePort"/>
          </a-form-item>
          <a-form-item ref="algorithm" name="algorithm" :label="$t('label.algorithm')">
            <a-select
              v-model:value="form.algorithm"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="(key, idx) in Object.keys(algorithms)" :key="idx" :value="algorithms[key]" :label="key">
                {{ key }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button @click="showAddInternalLB = false">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="handleAddInternalLBSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import Status from '@/components/widgets/Status'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'VpcTiersTab',
  mixins: [mixinForm],
  components: {
    Status,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      networks: [],
      networkid: '',
      fetchLoading: false,
      showCreateNetworkModal: false,
      showAddInternalLB: false,
      networkOfferings: [],
      networkAclList: [],
      selectedNetworkAcl: {},
      modalLoading: false,
      internalLB: {},
      LBPublicIPs: {},
      staticNats: {},
      vms: {},
      selectedNetworkOffering: {},
      privateMtuMax: 1500,
      errorPrivateMtu: '',
      algorithms: {
        Source: 'source',
        'Round-robin': 'roundrobin',
        'Least connections': 'leastconn'
      },
      internalLbCols: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.sourceipaddress'),
          dataIndex: 'sourceipaddress'
        },
        {
          title: this.$t('label.algorithm'),
          dataIndex: 'algorithm'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        }
      ],
      vmCols: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          key: 'ip',
          title: this.$t('label.ip')
        }
      ],
      customStyle: 'margin-bottom: 0; border: none',
      page: 1,
      pageSize: 10,
      itemCounts: {
        internalLB: {},
        publicIps: {},
        snats: {},
        vms: {}
      },
      lbProviderMap: {
        publicLb: {
          vpc: ['Netscaler']
        }
      },
      publicLBExists: false,
      setMTU: false
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    showIlb (network) {
      return network.service.filter(s => (s.name === 'Lb') && (s.capability.filter(c => c.name === 'LbSchemes' && c.value === 'Internal').length > 0)).length > 0 || false
    },
    updateMtu () {
      if (this.form.privatemtu > this.privateMtuMax) {
        this.errorPrivateMtu = `${this.$t('message.error.mtu.private.max.exceed')}`
        this.form.privatemtu = this.privateMtuMax
      } else {
        this.errorPrivateMtu = ''
      }
    },
    fetchData () {
      this.networks = this.resource.network
      this.fetchMtuForZone()
      if (!this.networks || this.networks.length === 0) {
        return
      }
      for (const network of this.networks) {
        this.fetchLoadBalancers(network.id)
        this.fetchVMs(network.id)
      }
      this.publicLBNetworkExists()
    },
    fetchMtuForZone () {
      api('listZones', {
        id: this.resource.zoneid
      }).then(json => {
        this.setMTU = json?.listzonesresponse?.zone?.[0]?.allowuserspecifyvrmtu || false
        this.privateMtuMax = json?.listzonesresponse?.zone?.[0]?.routerprivateinterfacemaxmtu || 1500
      })
    },
    fetchNetworkAclList () {
      this.fetchLoading = true
      this.modalLoading = true
      api('listNetworkACLLists', { vpcid: this.resource.id }).then(json => {
        this.networkAclList = json.listnetworkacllistsresponse.networkacllist || []
        this.handleNetworkAclChange(null)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
        this.modalLoading = false
      })
    },
    getNetworkOffering (networkId) {
      return new Promise((resolve, reject) => {
        api('listNetworkOfferings', {
          id: networkId
        }).then(json => {
          var networkOffering = json.listnetworkofferingsresponse.networkoffering[0]
          resolve(networkOffering)
        }).catch(e => {
          reject(e)
        })
      })
    },
    publicLBNetworkExists () {
      api('listNetworks', {
        vpcid: this.resource.id,
        supportedservices: 'LB'
      }).then(async json => {
        var lbNetworks = json.listnetworksresponse.network || []
        if (lbNetworks.length > 0) {
          this.publicLBExists = true
          for (var idx = 0; idx < lbNetworks.length; idx++) {
            const lbNetworkOffering = await this.getNetworkOffering(lbNetworks[idx].networkofferingid)
            const index = lbNetworkOffering.service.map(svc => { return svc.name }).indexOf('Lb')
            if (index !== -1 &&
              this.lbProviderMap.publicLb.vpc.indexOf(lbNetworkOffering.service.map(svc => { return svc.provider[0].name })[index]) !== -1) {
              this.publicLBExists = true
              break
            }
          }
        }
      })
    },
    fetchNetworkOfferings () {
      this.fetchLoading = true
      this.modalLoading = true
      api('listNetworkOfferings', {
        forvpc: true,
        guestiptype: 'Isolated',
        supportedServices: 'SourceNat',
        state: 'Enabled'
      }).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering || []
        var filteredOfferings = []
        if (this.publicLBExists) {
          for (var index in this.networkOfferings) {
            const offering = this.networkOfferings[index]
            const idx = offering.service.map(svc => { return svc.name }).indexOf('Lb')
            if (idx === -1 || this.lbProviderMap.publicLb.vpc.indexOf(offering.service.map(svc => { return svc.provider[0].name })[idx]) === -1) {
              filteredOfferings.push(offering)
            }
          }
          this.networkOfferings = filteredOfferings
        }
        this.form.networkOffering = this.networkOfferings[0].id
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
        this.modalLoading = false
      })
    },
    fetchLoadBalancers (id) {
      this.fetchLoading = true
      api('listLoadBalancers', {
        networkid: id,
        page: this.page,
        pagesize: this.pageSize,
        listAll: true
      }).then(json => {
        this.internalLB[id] = json.listloadbalancersresponse.loadbalancer || []
        this.itemCounts.internalLB[id] = json.listloadbalancersresponse.count || 0
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchVMs (id) {
      this.fetchLoading = true
      api('listVirtualMachines', {
        listAll: true,
        vpcid: this.resource.id,
        networkid: id,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.vms[id] = json.listvirtualmachinesresponse.virtualmachine || []
        this.itemCounts.vms[id] = json.listvirtualmachinesresponse.count || 0
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    handleNetworkOfferingChange (networkOfferingId) {
      this.selectedNetworkOffering = this.networkOfferings.filter(offering => offering.id === networkOfferingId)[0]
    },
    handleNetworkAclChange (aclId) {
      if (aclId) {
        this.selectedNetworkAcl = this.networkAclList.filter(acl => acl.id === aclId)[0]
      } else {
        this.selectedNetworkAcl = {}
      }
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleOpenModal () {
      this.initForm()
      this.fetchNetworkAclList()
      this.fetchNetworkOfferings()
      this.showCreateNetworkModal = true
      this.rules = {
        name: [{ required: true, message: this.$t('label.required') }],
        networkOffering: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        acl: [{ required: true, message: this.$t('label.required') }],
        vlan: [{ required: true, message: this.$t('message.please.enter.value') }]
      }
    },
    handleAddInternalLB (id) {
      this.initForm()
      this.showAddInternalLB = true
      this.networkid = id
      this.form.algorithm = 'Source'
      this.rules = {
        name: [{ required: true, message: this.$t('message.error.internallb.name') }],
        sourcePort: [{ required: true, message: this.$t('message.error.internallb.source.port') }],
        instancePort: [{ required: true, message: this.$t('message.error.internallb.instance.port') }],
        algorithm: [{ required: true, message: this.$t('label.required') }]
      }
    },
    handleAddNetworkSubmit () {
      if (this.modalLoading) return
      this.fetchLoading = true
      this.modalLoading = true

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        this.showCreateNetworkModal = false
        var params = {
          vpcid: this.resource.id,
          domainid: this.resource.domainid,
          account: this.resource.account,
          networkOfferingId: values.networkOffering,
          name: values.name,
          displayText: values.name,
          gateway: values.gateway,
          netmask: values.netmask,
          zoneId: this.resource.zoneid,
          externalid: values.externalId,
          aclid: values.acl
        }

        if (values.vlan) {
          params.vlan = values.vlan
        }

        if (values.privatemtu) {
          params.privatemtu = values.privatemtu
        }

        api('createNetwork', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.add.vpc.network')
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.parentFetchData()
          this.fetchData()
          this.fetchLoading = false
          this.modalLoading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.fetchLoading = false
        this.modalLoading = false
      })
    },
    handleAddInternalLBSubmit () {
      if (this.modalLoading) return
      this.fetchLoading = true
      this.modalLoading = true
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        api('createLoadBalancer', {
          name: values.name,
          sourceport: values.sourcePort,
          instanceport: values.instancePort,
          algorithm: values.algorithm,
          networkid: this.networkid,
          sourceipaddressnetworkid: this.networkid,
          scheme: 'Internal'
        }).then(response => {
          this.$pollJob({
            jobId: response.createloadbalancerresponse.jobid,
            title: this.$t('message.create.internallb'),
            description: values.name,
            successMessage: this.$t('message.success.create.internallb'),
            successMethod: () => {
              this.fetchData()
              this.modalLoading = false
            },
            errorMessage: `${this.$t('message.create.internallb.failed')} ` + response,
            errorMethod: () => {
              this.fetchData()
              this.modalLoading = false
            },
            loadingMessage: this.$t('message.create.internallb.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
              this.modalLoading = false
            }
          })
        }).catch(error => {
          console.error(error)
          this.$message.error(this.$t('message.create.internallb.failed'))
          this.modalLoading = false
        }).finally(() => {
          this.modalLoading = false
          this.fetchLoading = false
          this.showAddInternalLB = false
          this.fetchData()
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.fetchLoading = true
        this.modalLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style lang="scss" scoped>
.list {

  &__label {
    font-weight: bold;
  }

  &__col {
    flex: 1;

    @media (min-width: 480px) {
      &:not(:last-child) {
        margin-right: 20px;
      }
    }
  }

  &__item {
    margin-right: -8px;
    align-items: flex-start;

    &-outer-container {
      width: 100%;
    }

    &-container {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 480px) {
        flex-direction: row;
        margin-bottom: 10px;
      }
    }
  }
}
</style>
