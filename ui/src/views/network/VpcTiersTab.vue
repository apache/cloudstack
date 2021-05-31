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
      icon="plus"
      style="width: 100%;margin-bottom: 20px;"
      :disabled="!('createNetwork' in $store.getters.apis)"
      @click="handleOpenModal">{{ $t('label.add.network') }}</a-button>
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
            <template v-slot:expandIcon="props">
              <a-icon type="caret-right" :rotate="props.isActive ? 90 : 0" />
            </template>
            <a-collapse-panel :header="$t('label.instances')" key="vm" :style="customStyle">
              <a-button
                icon="plus"
                type="dashed"
                style="margin-bottom: 15px; width: 100%"
                :disabled="!('deployVirtualMachine' in $store.getters.apis)"
                @click="$router.push({ path: '/action/deployVirtualMachine?networkid=' + network.id + '&zoneid=' + network.zoneid })">
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
                <template slot="name" slot-scope="text, item">
                  <router-link
                    :to="{ path: '/vm/'+item.id}">{{ item.name }}
                  </router-link>
                </template>
                <template slot="state" slot-scope="text, item">
                  <status :text="item.state" displayText></status>
                </template>
                <template slot="ip" slot-scope="text, item">
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
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }} / {{ $t('label.page') }}</span>
                </template>
              </a-pagination>
            </a-collapse-panel>
            <a-collapse-panel :header="$t('label.internal.lb')" key="ilb" :style="customStyle" :disabled="!showIlb(network)" >
              <a-button
                icon="plus"
                type="dashed"
                style="margin-bottom: 15px; width: 100%"
                :disabled="!('createLoadBalancer' in $store.getters.apis)"
                @click="handleAddInternalLB(network.id)">
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
                <template slot="name" slot-scope="text, item">
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
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }} / {{ $t('label.page') }}</span>
                </template>
              </a-pagination>
            </a-collapse-panel>
          </a-collapse>
        </div>
      </a-list-item>
    </a-list>

    <a-modal
      v-model="showCreateNetworkModal"
      :title="$t('label.add.new.tier')"
      :maskClosable="false"
      @ok="handleAddNetworkSubmit">
      <a-spin :spinning="modalLoading">
        <a-form @submit.prevent="handleAddNetworkSubmit" :form="form">
          <a-form-item :label="$t('label.name')">
            <a-input
              :placeholder="$t('label.unique.name.tier')"
              v-decorator="['name',{rules: [{ required: true, message: `${$t('label.required')}` }]}]"
              autoFocus></a-input>
          </a-form-item>
          <a-form-item :label="$t('label.networkofferingid')">
            <a-select
              v-decorator="['networkOffering',{rules: [{ required: true, message: `${$t('label.required')}` }]}]"
              @change="val => { this.handleNetworkOfferingChange(val) }">
              <a-select-option v-for="item in networkOfferings" :key="item.id" :value="item.id">
                {{ item.displaytext || item.name || item.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="!this.isObjectEmpty(this.selectedNetworkOffering) && this.selectedNetworkOffering.specifyvlan">
            <span slot="label">
              {{ $t('label.vlan') }}
              <a-tooltip :title="$t('label.vlan')">
                <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </span>
            <a-input
              v-decorator="['vlan', {
                rules: [{ required: true, message: $t('message.please.enter.value') }]
              }]"
              :placeholder="this.$t('label.vlan')"/>
          </a-form-item>
          <a-form-item :label="$t('label.gateway')">
            <a-input
              :placeholder="$t('label.create.network.gateway.description')"
              v-decorator="['gateway',{rules: [{ required: true, message: `${$t('label.required')}` }]}]"></a-input>
          </a-form-item>
          <a-form-item :label="$t('label.netmask')">
            <a-input
              :placeholder="$t('label.create.network.netmask.description')"
              v-decorator="['netmask',{rules: [{ required: true, message: `${$t('label.required')}` }]}]"></a-input>
          </a-form-item>
          <a-form-item :label="$t('label.externalid')">
            <a-input
              v-decorator="['externalId']"></a-input>
          </a-form-item>
          <a-form-item :label="$t('label.aclid')">
            <a-select
              v-decorator="['acl',{rules: [{ required: true, message: `${$t('label.required')}` }]}]"
              @change="val => { this.handleNetworkAclChange(val) }">
              <a-select-option v-for="item in networkAclList" :key="item.id" :value="item.id">
                <strong>{{ item.name }}</strong> ({{ item.description }})
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-alert v-if="this.selectedNetworkAcl.name==='default_allow'" type="warning" show-icon>
            <span slot="message" v-html="$t('message.network.acl.default.allow')" />
          </a-alert>
          <a-alert v-else-if="this.selectedNetworkAcl.name==='default_deny'" type="warning" show-icon>
            <span slot="message" v-html="$t('message.network.acl.default.deny')" />
          </a-alert>
        </a-form>
      </a-spin>
    </a-modal>

    <a-modal
      v-model="showAddInternalLB"
      :title="$t('label.add.internal.lb')"
      :maskClosable="false"
      @ok="handleAddInternalLBSubmit">
      <a-spin :spinning="modalLoading">
        <a-form @submit.prevent="handleAddInternalLBSubmit" :form="form">
          <a-form-item :label="$t('label.name')">
            <a-input
              autoFocus
              :placeholder="$t('label.internallb.name.description')"
              v-decorator="['name', { rules: [{ required: true, message: $t('message.error.internallb.name')}] }]"/>
          </a-form-item>
          <a-form-item :label="$t('label.description')">
            <a-input
              :placeholder="$t('label.internallb.description')"
              v-decorator="['description']"/>
          </a-form-item>
          <a-form-item :label="$t('label.sourceipaddress')">
            <a-input
              :placeholder="$t('label.internallb.sourceip.description')"
              v-decorator="['sourceIP']"/>
          </a-form-item>
          <a-form-item :label="$t('label.sourceport')">
            <a-input
              v-decorator="['sourcePort', { rules: [{ required: true, message: $t('message.error.internallb.source.port')}] }]"/>
          </a-form-item>
          <a-form-item :label="$t('label.instanceport')">
            <a-input
              v-decorator="['instancePort', { rules: [{ required: true, message: $t('message.error.internallb.instance.port')}] }]"/>
          </a-form-item>
          <a-form-item :label="$t('label.algorithm')">
            <a-select
              v-decorator="[
                'algorithm',
                {
                  initialValue: 'Source',
                  rules: [{ required: true, message: `${$t('label.required')}`}]
                }]">
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
      selectedNetworkOffering: {},
      algorithms: {
        Source: 'source',
        'Round-robin': 'roundrobin',
        'Least connections': 'leastconn'
      },
      internalLbCols: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' }
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
          scopedSlots: { customRender: 'ipaddress' }
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
          scopedSlots: { customRender: 'ipaddress' }
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
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: this.$t('label.ip'),
          scopedSlots: { customRender: 'ip' }
        }
      ],
      customStyle: 'margin-bottom: -10px; border-bottom-style: none',
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
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
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
        this.$nextTick(function () {
          this.form.setFieldsValue({
            networkOffering: this.networkOfferings[0].id
          })
        })
        this.selectedNetworkOffering = this.networkOfferings[0]
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
      this.form = this.$form.createForm(this)
      this.fetchNetworkAclList()
      this.fetchNetworkOfferings()
      this.showCreateNetworkModal = true
    },
    handleAddInternalLB (id) {
      this.form = this.$form.createForm(this)
      this.showAddInternalLB = true
      this.networkid = id
    },
    handleAddNetworkSubmit () {
      this.fetchLoading = true

      this.form.validateFields((errors, values) => {
        if (errors) {
          this.fetchLoading = false
          return
        }

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
        })
      })
    },
    handleAddInternalLBSubmit () {
      this.fetchLoading = true
      this.modalLoading = true
      this.form.validateFields((errors, values) => {
        if (errors) {
          this.fetchLoading = false
          return
        }
        api('createLoadBalancer', {
          name: values.name,
          sourceport: values.sourcePort,
          instanceport: values.instancePort,
          algorithm: values.algorithm,
          networkid: this.networkid,
          sourceipaddressnetworkid: this.networkid,
          scheme: 'Internal'
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('message.create.internallb'),
            jobid: response.createloadbalancerresponse.jobid,
            description: values.name,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.createloadbalancerresponse.jobid,
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
