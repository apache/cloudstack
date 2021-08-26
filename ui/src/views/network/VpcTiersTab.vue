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
            <template #expandIcon="{ props }">
              <caret-right-outlined :rotate="props.isActive ? 90 : 0" />
            </template>
            <a-collapse-panel :header="$t('label.instances')" key="vm" :style="customStyle">
              <a-button
                type="dashed"
                style="margin-bottom: 15px; width: 100%"
                :disabled="!('deployVirtualMachine' in $store.getters.apis)"
                @click="$router.push({ path: '/action/deployVirtualMachine?networkid=' + network.id + '&zoneid=' + network.zoneid })">
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
                <template #name="{ item }">
                  <router-link
                    :to="{ path: '/vm/'+item.id}">{{ item.name }}
                  </router-link>
                </template>
                <template #state="{ item }">
                  <status :text="item.state" displayText></status>
                </template>
                <template #ip="{ item }">
                  <div v-for="nic in item.nic" :key="nic.id">
                    {{ nic.networkid === network.id ? nic.ipaddress : '' }}
                  </div>
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
            <a-collapse-panel :header="$t('label.internal.lb')" key="ilb" :style="customStyle" :disabled="!showIlb(network)" >
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
                <template #name="{ item }">
                  <router-link
                    :to="{ path: '/ilb/'+item.id}">{{ item.name }}
                  </router-link>
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
      @ok="handleAddNetworkSubmit">
      <a-spin :spinning="modalLoading">
        <a-form
          @submit.prevent="handleAddNetworkSubmit"
          :ref="createNetworkRef"
          :model="createNetworkForm"
          :rules="createNetworkRules">
          <a-form-item ref="name" name="name" :colon="false">
            <span slot="label">
              {{ $t('label.name') }}
              <a-tooltip placement="right" :title="$t('label.create.tier.name.description')">
                <a-icon type="info-circle" />
              </a-tooltip>
            </span>
            <a-input
              :placeholder="$t('label.create.tier.name.description')"
              v-model:value="createNetworkForm.name"
              autoFocus></a-input>
          </a-form-item>
          <a-form-item ref="networkOffering" name="networkOffering" :colon="false">
            <span slot="label">
              {{ $t('label.networkofferingid') }}
              <a-tooltip placement="right" :title="$t('label.create.tier.networkofferingid.description')">
                <a-icon type="info-circle" />
              </a-tooltip>
            </span>
            <a-select v-model:value="createNetworkForm.networkOffering">
              <a-select-option v-for="item in networkOfferings" :key="item.id" :value="item.id">
                {{ item.displaytext || item.name || item.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item ref="gateway" name="gateway" :colon="false">
            <span slot="label">
              {{ $t('label.gateway') }}
              <a-tooltip placement="right" :title="$t('label.create.tier.gateway.description')">
                <a-icon type="info-circle" />
              </a-tooltip>
            </span>
            <a-input
              :placeholder="$t('label.create.tier.gateway.description')"
              v-model:value="createNetworkForm.gateway"></a-input>
          </a-form-item>
          <a-form-item ref="netmask" name="netmask" :colon="false">
            <span slot="label">
              {{ $t('label.netmask') }}
              <a-tooltip placement="right" :title="$t('label.create.tier.netmask.description')">
                <a-icon type="info-circle" />
              </a-tooltip>
            </span>
            <a-input
              :placeholder="$t('label.create.tier.netmask.description')"
              v-model:value="createNetworkForm.netmask"></a-input>
          </a-form-item>
          <a-form-item ref="externalId" name="externalId" :colon="false">
            <span slot="label">
              {{ $t('label.externalid') }}
              <a-tooltip placement="right" :title="$t('label.create.tier.externalid.description')">
                <a-icon type="info-circle" />
              </a-tooltip>
            </span>
            <a-input
              :placeholder=" $t('label.create.tier.externalid.description')"
              v-model:value="createNetworkForm.externalId"/>
          </a-form-item>
          <a-form-item ref="acl" name="acl" :colon="false">
            <span slot="label">
              {{ $t('label.aclid') }}
              <a-tooltip placement="right" :title="$t('label.create.tier.aclid.description')">
                <a-icon type="info-circle" />
              </a-tooltip>
            </span>
            <a-select
              :placeholder="$t('label.create.tier.aclid.description')"
              v-model:value="createNetworkForm.acl"
              @change="val => { handleNetworkAclChange(val) }">
              <a-select-option v-for="item in networkAclList" :key="item.id" :value="item.id">
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
        </a-form>
      </a-spin>
    </a-modal>

    <a-modal
      :visible="showAddInternalLB"
      :title="$t('label.add.internal.lb')"
      :maskClosable="false"
      @ok="handleAddInternalLBSubmit">
      <a-spin :spinning="modalLoading">
        <a-form
          @submit.prevent="handleAddInternalLBSubmit"
          :ref="internalLbRef"
          :form="internalLbForm"
          :rules="internalLbRules">
          <a-form-item ref="name" name="name" :label="$t('label.name')">
            <a-input
              autoFocus
              :placeholder="$t('label.internallb.name.description')"
              v-model:value="internalLbForm.name"/>
          </a-form-item>
          <a-form-item ref="description" name="description" :label="$t('label.description')">
            <a-input
              :placeholder="$t('label.internallb.description')"
              v-model:value="internalLbForm.description"/>
          </a-form-item>
          <a-form-item ref="sourceIP" name="sourceIP" :label="$t('label.sourceipaddress')">
            <a-input
              :placeholder="$t('label.internallb.sourceip.description')"
              v-model:value="internalLbForm.sourceIP"/>
          </a-form-item>
          <a-form-item ref="sourcePort" name="sourcePort" :label="$t('label.sourceport')">
            <a-input v-model:value="internalLbForm.sourcePort"/>
          </a-form-item>
          <a-form-item ref="instancePort" name="instancePort" :label="$t('label.instanceport')">
            <a-input v-model:value="internalLbForm.instancePort"/>
          </a-form-item>
          <a-form-item ref="algorithm" name="algorithm" :label="$t('label.algorithm')">
            <a-select v-model:value="internalLbForm.algorithm">
              <a-select-option v-for="(key, idx) in Object.keys(algorithms)" :key="idx" :value="algorithms[key]">
                {{ key }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'VpcTiersTab',
  components: {
    Status
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
      algorithms: {
        Source: 'source',
        'Round-robin': 'roundrobin',
        'Least connections': 'leastconn'
      },
      internalLbCols: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          slots: { customRender: 'name' }
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
      LBPublicIPCols: [
        {
          title: this.$t('label.ip'),
          dataIndex: 'ipaddress',
          slots: { customRender: 'ipaddress' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.networkid'),
          dataIndex: 'associatedNetworkName'
        },
        {
          title: this.$t('label.vm'),
          dataIndex: 'virtualmachinename'
        }
      ],
      StaticNatCols: [
        {
          title: this.$t('label.ips'),
          dataIndex: 'ipaddress',
          slots: { customRender: 'ipaddress' }
        },
        {
          title: this.$t('label.zoneid'),
          dataIndex: 'zonename'
        },
        {
          title: this.$t('label.networkid'),
          dataIndex: 'associatedNetworkName'
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state'
        }
      ],
      vmCols: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          slots: { customRender: 'name' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          slots: { customRender: 'state' }
        },
        {
          title: this.$t('label.ip'),
          slots: { customRender: 'ip' }
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
          vpc: ['VpcVirtualRouter', 'Netscaler']
        }
      },
      publicLBExists: false
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
    initForm () {
      this.createNetworkRef = ref()
      this.internalLbRef = ref()
      this.createNetworkForm = reactive({})
      this.internalLbForm = reactive({ algorithm: 'Source' })
      this.createNetworkRules = reactive({
        name: [{ required: true, message: this.$t('label.required') }],
        networkOffering: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        acl: [{ required: true, message: this.$t('label.required') }]
      })
      this.internalLbRules = reactive({
        name: [{ required: true, message: this.$t('message.error.internallb.name') }],
        sourcePort: [{ required: true, message: this.$t('message.error.internallb.source.port') }],
        instancePort: [{ required: true, message: this.$t('message.error.internallb.instance.port') }],
        algorithm: [{ required: true, message: this.$t('label.required') }]
      })
    },
    showIlb (network) {
      return network.service.filter(s => (s.name === 'Lb') && (s.capability.filter(c => c.name === 'LbSchemes' && c.value === 'Internal').length > 0)).length > 0 || false
    },
    fetchData () {
      this.networks = this.resource.network
      if (!this.networks || this.networks.length === 0) {
        return
      }
      for (const network of this.networks) {
        this.fetchLoadBalancers(network.id)
        this.fetchVMs(network.id)
      }
      this.publicLBNetworkExists()
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
        this.createNetworkForm.networkOffering = this.networkOfferings[0].id
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
        pagesize: this.pageSize
      }).then(json => {
        this.internalLB[id] = json.listloadbalancersresponse.loadbalancer || []
        this.itemCounts.internalLB[id] = json.listloadbalancersresponse.count || 0
        this.$forceUpdate()
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
        this.$forceUpdate()
      }).finally(() => {
        this.fetchLoading = false
      })
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
      this.fetchNetworkAclList()
      this.fetchNetworkOfferings()
      this.showCreateNetworkModal = true
    },
    handleAddInternalLB (id) {
      this.showAddInternalLB = true
      this.networkid = id
    },
    handleAddNetworkSubmit () {
      this.fetchLoading = true

      this.createNetworkRef.value.validate().then(() => {
        const values = toRaw(this.form)

        this.showCreateNetworkModal = false
        api('createNetwork', {
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
        }).then(() => {
          this.$notification.success({
            message: this.$t('message.success.add.vpc.network')
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.parentFetchData()
          this.fetchData()
          this.fetchLoading = false
        })
      }).catch(() => { this.fetchLoading = false })
    },
    handleAddInternalLBSubmit () {
      this.fetchLoading = true
      this.modalLoading = true
      this.internalLbRef.value.validate().then(() => {
        const values = toRaw(this.form)

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
            },
            errorMessage: `${this.$t('message.create.internallb.failed')} ` + response,
            errorMethod: () => {
              this.fetchData()
            },
            loadingMessage: this.$t('message.create.internallb.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
            }
          })
        }).catch(error => {
          console.error(error)
          this.$message.error(this.$t('message.create.internallb.failed'))
        }).finally(() => {
          this.modalLoading = false
          this.fetchLoading = false
          this.showAddInternalLB = false
          this.fetchData()
        })
      }).catch(() => { this.fetchLoading = true })
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
