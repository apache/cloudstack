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
    <a-tabs
      :activeKey="currentTab"
      :tabPosition="device === 'mobile' ? 'top' : 'left'"
      :animated="false"
      @change="handleChangeTab">
      <a-tab-pane :tab="$t('label.details')" key="details">
        <DetailsTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.networks')" key="tier">
        <VpcTiersTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.public.ips')" key="ip" v-if="'listPublicIpAddresses' in $store.getters.apis">
        <IpAddressesTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.network.acl.lists')" key="acl" v-if="'listNetworkACLLists' in $store.getters.apis">
        <a-button
          type="dashed"
          style="width: 100%"
          :disabled="!('createNetworkACLList' in $store.getters.apis)"
          @click="() => handleOpenModals('networkAcl')">
          <template #icon><plus-circle-outlined /></template>
          {{ $t('label.add.network.acl.list') }}
        </a-button>
        <a-table
          class="table"
          size="small"
          :columns="networkAclsColumns"
          :dataSource="networkAcls"
          :rowKey="item => item.id"
          :pagination="false"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'name'">
              <router-link :to="{ path: '/acllist/' + record.id }">
                {{ text }}
              </router-link>
            </template>
          </template>
        </a-table>
        <a-pagination
          class="row-element pagination"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="itemCounts.networkAcls"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
        <a-modal
          :visible="modals.networkAcl"
          :title="$t('label.add.acl.list')"
          :footer="null"
          :maskClosable="false"
          :closable="true"
          @cancel="modals.networkAcl = false">
          <a-form
            layout="vertical"
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleNetworkAclFormSubmit"
            v-ctrl-enter="handleNetworkAclFormSubmit"
           >
            <a-form-item :label="$t('label.add.list.name')" ref="name" name="name">
              <a-input
                v-model:value="form.name"
                v-focus="true"></a-input>
            </a-form-item>
            <a-form-item :label="$t('label.description')"  ref="description" name="description">
              <a-input v-model:value="form.description" />
            </a-form-item>

            <div :span="24" class="action-button">
              <a-button @click="modals.networkAcl = false">{{ $t('label.cancel') }}</a-button>
              <a-button type="primary" @click="handleNetworkAclFormSubmit">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-modal>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.private.gateway')" key="pgw" v-if="'listPrivateGateways' in $store.getters.apis">
        <a-button
          type="dashed"
          style="width: 100%"
          :disabled="!('createPrivateGateway' in $store.getters.apis)"
          @click="() => handleOpenModals('privateGateways')">
          <template #icon><plus-circle-outlined /></template>
          {{ $t('label.add.private.gateway') }}
        </a-button>
        <a-table
          class="table"
          size="small"
          :columns="privateGatewaysColumns"
          :dataSource="privateGateways"
          :rowKey="item => item.id"
          :pagination="false"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'ipaddress'">
              <router-link :to="{ path: '/privategw/' + record.id }">{{ text }}</router-link>
            </template>
            <template v-if="column.key === 'state'">
              <status :text="record.state" displayText></status>
            </template>
          </template>
        </a-table>
        <a-pagination
          class="row-element pagination"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="itemCounts.privateGateways"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>

        <a-modal
          :visible="modals.gateway"
          :title="$t('label.add.new.gateway')"
          :maskClosable="false"
          :closable="true"
          :footer="null"
          @cancel="modals.gateway = false">
          <a-spin :spinning="modals.gatewayLoading" v-ctrl-enter="handleGatewayFormSubmit">
            <p>{{ $t('message.add.new.gateway.to.vpc') }}</p>
            <a-form
              layout="vertical"
              @finish="handleGatewayFormSubmit"
              :ref="formRef"
              :model="form"
              :rules="rules"
             >
              <a-form-item :label="$t('label.physicalnetworkid')" ref="physicalnetwork" name="physicalnetwork" v-if="this.isAdmin()">
                <a-select
                  v-model:value="form.physicalnetwork"
                  v-focus="true"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="item in physicalnetworks" :key="item.id" :value="item.id" :label="item.name">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item :label="$t('label.vlan')" ref="vlan" name="vlan" v-if="this.isAdmin()">
                <a-input
                  :placeholder="placeholders.vlan"
                  v-model:value="form.vlan"
                ></a-input>
              </a-form-item>
              <a-form-item
                ref="bypassvlanoverlapcheck"
                name="bypassvlanoverlapcheck"
                :label="$t('label.bypassvlanoverlapcheck')"
                v-if="this.isAdmin()">
                <a-checkbox
                  v-model:checked="form.bypassvlanoverlapcheck"
                ></a-checkbox>
              </a-form-item>
              <a-form-item :label="$t('label.associatednetwork')" ref="associatednetworkid" name="associatednetworkid">
                <a-select
                  v-model:value="form.associatednetworkid"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="(opt, optIndex) in this.associatedNetworks" :key="optIndex" :label="opt.name || opt.description" :value="opt.id">
                    <span>
                      <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                      <user-outlined style="margin-right: 5px" />
                      {{ opt.name || opt.description }}
                    </span>
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item :label="$t('label.publicip')" ref="ipaddress" name="ipaddress">
                <a-input
                  :placeholder="placeholders.ipaddress"
                  v-model:value="form.ipaddress"
                ></a-input>
              </a-form-item>
              <a-form-item :label="$t('label.gateway')" ref="gateway" name="gateway">
                <a-input
                  :placeholder="placeholders.gateway"
                  v-model:value="form.gateway"
                ></a-input>
              </a-form-item>
              <a-form-item :label="$t('label.netmask')" ref="netmask" name="netmask">
                <a-input
                  :placeholder="placeholders.netmask"
                  v-model:value="form.netmask"
                ></a-input>
              </a-form-item>
              <a-form-item :label="$t('label.sourcenat')" ref="nat" name="nat">
                <a-checkbox v-model:checked="form.nat"></a-checkbox>
              </a-form-item>
              <a-form-item :label="$t('label.aclid')" ref="acl" name="acl">
                <a-select
                  v-model:value="form.acl"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="item in networkAcls" :key="item.id" :value="item.id" :label="`${item.name} ${item.description}`">
                    <span><strong>{{ item.name }}</strong> ({{ item.description }})</span>
                  </a-select-option>
                </a-select>
              </a-form-item>

              <div :span="24" class="action-button">
                <a-button @click="modals.gateway = false">{{ $t('label.cancel') }}</a-button>
                <a-button type="primary" @click="handleGatewayFormSubmit">{{ $t('label.ok') }}</a-button>
              </div>
            </a-form>
          </a-spin>
        </a-modal>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.vpn.gateway')" key="vpngw" v-if="'listVpnGateways' in $store.getters.apis">
        <a-button
          v-if="vpnGateways.length === 0"
          type="dashed"
          style="width: 100%"
          :disabled="!('createVpnGateway' in $store.getters.apis)"
          @click="handleCreateVpnGateway">
          <template #icon><plus-circle-outlined /></template>
          {{ $t('label.create.site.vpn.gateway') }}
        </a-button>
        <a-list class="list">
          <a-list-item v-for="item in vpnGateways" :key="item.id">
            <div class="list__item">
              <div class="list__col">
                <div class="list__label">{{ $t('label.ip') }}</div>
                <div>
                  <router-link :to="{ path: '/s2svpn/' + item.id }">
                    {{ item.publicip }}
                  </router-link>
                </div>
              </div>
            </div>
          </a-list-item>
        </a-list>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.vpn.connection')" key="vpnc" v-if="'listVpnConnections' in $store.getters.apis">
        <a-button
          type="dashed"
          style="width: 100%"
          :disabled="!('createVpnConnection' in $store.getters.apis)"
          @click="handleOpenModals('vpnConnection')">
          <template #icon><plus-circle-outlined /></template>
          {{ $t('label.create.site.vpn.connection') }}
        </a-button>
        <a-table
          class="table"
          size="small"
          :columns="vpnConnectionsColumns"
          :dataSource="vpnConnections"
          :pagination="false"
          :rowKey="record => record.id">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'publicip'">
              <router-link :to="{ path: '/s2svpnconn/' + record.id }">
                {{ text }}
              </router-link>
            </template>
            <template v-if="column.key === 'state'">
              <status :text="text ? text : ''" displayText />
            </template>
          </template>
        </a-table>
        <a-pagination
          class="row-element pagination"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="itemCounts.vpnConnections"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
        <a-modal
          :visible="modals.vpnConnection"
          :title="$t('label.create.vpn.connection')"
          :maskClosable="false"
          :closable="true"
          :footer="null"
          @cancel="modals.vpnConnection = false">
          <a-spin :spinning="modals.vpnConnectionLoading" v-ctrl-enter="handleVpnConnectionFormSubmit">
            <a-form
              layout="vertical"
              @finish="handleVpnConnectionFormSubmit"
              :ref="formRef"
              :model="form"
              :rules="rules"
             >
              <a-form-item :label="$t('label.vpncustomergatewayid')" ref="vpncustomergateway" name="vpncustomergateway">
                <a-select
                  v-model:value="form.vpncustomergateway"
                  v-focus="true"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                  <a-select-option v-for="item in vpncustomergateways" :key="item.id" :value="item.id" :label="item.name">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item :label="$t('label.passive')" ref="passive" name="passive">
                <a-checkbox v-model:checked="form.passive"></a-checkbox>
              </a-form-item>

              <div :span="24" class="action-button">
                <a-button @click="modals.vpnConnection = false">{{ $t('label.cancel') }}</a-button>
                <a-button type="primary" htmlType="submit" @click="handleVpnConnectionFormSubmit">{{ $t('label.ok') }}</a-button>
              </div>
            </a-form>
          </a-spin>
        </a-modal>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.virtual.routers')" key="vr" v-if="$store.getters.userInfo.roletype === 'Admin'">
        <RoutersTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.events')" key="events" v-if="'listEvents' in $store.getters.apis">
        <events-tab :resource="resource" resourceType="Vpc" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.annotations')" key="comments" v-if="'listAnnotations' in $store.getters.apis">
        <AnnotationsTab
          :resource="resource"
          :items="annotations">
        </AnnotationsTab>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import DetailsTab from '@/components/view/DetailsTab'
import Status from '@/components/widgets/Status'
import IpAddressesTab from './IpAddressesTab'
import RoutersTab from './RoutersTab'
import VpcTiersTab from './VpcTiersTab'
import EventsTab from '@/components/view/EventsTab'
import AnnotationsTab from '@/components/view/AnnotationsTab'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'VpcTab',
  components: {
    DetailsTab,
    Status,
    IpAddressesTab,
    RoutersTab,
    VpcTiersTab,
    EventsTab,
    AnnotationsTab,
    ResourceIcon
  },
  mixins: [mixinDevice],
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
  data () {
    return {
      fetchLoading: false,
      privateGateways: [],
      associatedNetworks: [],
      vpnGateways: [],
      vpnConnections: [],
      networkAcls: [],
      modals: {
        gateway: false,
        gatewayLoading: false,
        vpnConnection: false,
        vpnConnectionLoading: false,
        networkAcl: false
      },
      placeholders: {
        vlan: null,
        ipaddress: null,
        gateway: null,
        netmask: null
      },
      physicalnetworks: [],
      vpncustomergateways: [],
      privateGatewaysColumns: [
        {
          key: 'ipaddress',
          title: this.$t('label.ip'),
          dataIndex: 'ipaddress'
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask'
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlan'
        }
      ],
      vpnConnectionsColumns: [
        {
          key: 'publicip',
          title: this.$t('label.ip'),
          dataIndex: 'publicip'
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('label.ipsecpsk'),
          dataIndex: 'ipsecpsk'
        }
      ],
      networkAclsColumns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.description'),
          dataIndex: 'description'
        }
      ],
      itemCounts: {
        privateGateways: 0,
        vpnConnections: 0,
        networkAcls: 0
      },
      page: 1,
      pageSize: 10,
      currentTab: 'details',
      annotations: []
    }
  },
  watch: {
    loading (newData) {
      if (!newData && this.resource.id) {
        this.handleFetchData()
      }
    },
    '$route.fullPath': function () {
      this.setCurrentTab()
    }
  },
  created () {
    this.initForm()
    this.setCurrentTab()
    this.handleFetchData()
    const self = this
    window.addEventListener('popstate', function () {
      self.setCurrentTab()
    })
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    setCurrentTab () {
      this.currentTab = this.$route?.query?.tab || 'details'
    },
    handleChangeTab (e) {
      this.currentTab = e
      this.page = 1
      this.pageSize = 10
      this.handleFetchData()
      const query = Object.assign({}, this.$route.query)
      query.tab = e
      history.pushState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
    },
    handleFetchData () {
      switch (this.currentTab) {
        case 'pgw':
          this.fetchPrivateGateways()
          break
        case 'vpngw':
          this.fetchVpnGateways()
          break
        case 'vpnc':
          this.fetchVpnConnections()
          break
        case 'acl':
          this.fetchAclList()
          break
        case 'comments':
          this.fetchComments()
          break
      }
    },
    fetchComments () {
      this.fetchLoading = true
      api('listAnnotations', { entityid: this.resource.id, entitytype: 'VPC', annotationfilter: 'all' }).then(json => {
        if (json.listannotationsresponse && json.listannotationsresponse.annotation) {
          this.annotations = json.listannotationsresponse.annotation
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchPrivateGateways () {
      this.fetchLoading = true
      api('listPrivateGateways', {
        vpcid: this.resource.id,
        listAll: true,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.privateGateways = json.listprivategatewaysresponse.privategateway
        this.itemCounts.privateGateways = json.listprivategatewaysresponse.count
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
      this.associatedNetworks = []
      api('listNetworks', {
        domainid: this.resource.domainid,
        account: this.resource.account,
        listAll: true,
        networkfilter: 'Account'
      }).then(json => {
        var networks = json.listnetworksresponse.network || []
        for (const network of networks) {
          if (network.type === 'Isolated' || network.type === 'L2') {
            this.associatedNetworks.push(network)
          }
        }
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    fetchVpnGateways () {
      this.fetchLoading = true
      api('listVpnGateways', {
        vpcid: this.resource.id,
        listAll: true
      }).then(json => {
        this.vpnGateways = json?.listvpngatewaysresponse?.vpngateway || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchVpnConnections () {
      this.fetchLoading = true
      api('listVpnConnections', {
        vpcid: this.resource.id,
        listAll: true,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.vpnConnections = json.listvpnconnectionsresponse.vpnconnection
        this.itemCounts.vpnConnections = json.listvpnconnectionsresponse.count
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchAclList () {
      this.fetchLoading = true
      api('listNetworkACLLists', {
        vpcid: this.resource.id,
        listAll: true,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.networkAcls = json.listnetworkacllistsresponse.networkacllist
        this.itemCounts.networkAcls = json.listnetworkacllistsresponse.count
        if (this.modals.gateway === true) {
          this.form.acl = this.networkAcls[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchPhysicalNetworks () {
      this.modals.gatewayLoading = true
      if (!this.isAdmin()) {
        this.modals.gatewayLoading = false
        return
      }
      api('listPhysicalNetworks', { zoneid: this.resource.zoneid }).then(json => {
        this.physicalnetworks = json.listphysicalnetworksresponse.physicalnetwork
        if (this.modals.gateway === true) {
          this.form.physicalnetwork = this.physicalnetworks[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.modals.gatewayLoading = false
      })
    },
    fetchVpnCustomerGateways () {
      this.modals.vpnConnectionLoading = true
      api('listVpnCustomerGateways', { listAll: true }).then(json => {
        this.vpncustomergateways = json.listvpncustomergatewaysresponse.vpncustomergateway || []
        if (this.modals.vpnConnection === true) {
          this.form.vpncustomergateway = this.vpncustomergateways[0]?.id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.modals.vpnConnectionLoading = false
      })
    },
    handleOpenModals (e) {
      this.initForm()

      switch (e) {
        case 'privateGateways':
          if (this.isAdmin()) {
            this.rules = {
              ipaddress: [{ required: true, message: this.$t('label.required') }],
              gateway: [{ required: true, message: this.$t('label.required') }],
              netmask: [{ required: true, message: this.$t('label.required') }]
            }
          } else {
            this.rules = {
              ipaddress: [{ required: true, message: this.$t('label.required') }],
              gateway: [{ required: true, message: this.$t('label.required') }],
              netmask: [{ required: true, message: this.$t('label.required') }],
              associatednetworkid: [{ required: true, message: this.$t('label.required') }]
            }
          }
          this.modals.gateway = true
          this.fetchAclList()
          this.fetchPhysicalNetworks()
          break
        case 'vpnConnection':
          this.modals.vpnConnection = true
          this.fetchVpnCustomerGateways()
          this.fetchVpnGateways()
          break
        case 'networkAcl':
          this.rules = {
            name: [{ required: true, message: this.$t('label.required') }],
            description: [{ required: true, message: this.$t('label.required') }]
          }
          this.modals.networkAcl = true
          break
      }
    },
    handleGatewayFormSubmit () {
      if (this.modals.gatewayLoading) return
      this.modals.gatewayLoading = true

      this.formRef.value.validate().then(() => {
        const data = toRaw(this.form)

        const params = {
          sourcenatsupported: data.nat,
          physicalnetworkid: data.physicalnetwork,
          vpcid: this.resource.id,
          ipaddress: data.ipaddress,
          gateway: data.gateway,
          netmask: data.netmask,
          aclid: data.acl
        }
        if (data.bypassvlanoverlapcheck) {
          params.bypassvlanoverlapcheck = data.bypassvlanoverlapcheck
        }
        if (data.vlan && String(data.vlan).length > 0) {
          params.vlan = data.vlan
        }
        if (data.associatednetworkid) {
          params.associatednetworkid = data.associatednetworkid
        }

        api('createPrivateGateway', params).then(response => {
          this.$pollJob({
            jobId: response.createprivategatewayresponse.jobid,
            title: this.$t('message.success.add.private.gateway'),
            description: this.resource.id,
            successMethod: () => {
              this.modals.gateway = false
              this.handleFetchData()
            },
            errorMessage: this.$t('message.add.private.gateway.failed'),
            errorMethod: () => {
              this.modals.gateway = false
              this.handleFetchData()
            },
            loadingMessage: this.$t('message.add.private.gateway.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.modals.gateway = false
              this.handleFetchData()
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.modals.gatewayLoading = false
          this.modals.gateway = false
          this.handleFetchData()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.modals.gatewayLoading = false
      })
    },
    handleVpnConnectionFormSubmit () {
      if (this.fetchLoading) return
      this.fetchLoading = true
      this.modals.vpnConnection = false

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        api('createVpnConnection', {
          s2svpngatewayid: this.vpnGateways[0].id,
          s2scustomergatewayid: values.vpncustomergateway,
          passive: values.passive ? values.passive : false
        }).then(response => {
          this.$pollJob({
            jobId: response.createvpnconnectionresponse.jobid,
            title: this.$t('label.vpn.connection'),
            description: this.vpnGateways[0].id,
            successMethod: () => {
              this.fetchVpnConnections()
              this.fetchLoading = false
            },
            errorMessage: this.$t('message.add.vpn.connection.failed'),
            errorMethod: () => {
              this.fetchVpnConnections()
              this.fetchLoading = false
            },
            loadingMessage: this.$t('message.add.vpn.connection.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchVpnConnections()
              this.fetchLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.fetchVpnConnections()
          this.fetchLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.fetchLoading = false
      })
    },
    handleNetworkAclFormSubmit () {
      if (this.fetchLoading) return
      this.fetchLoading = true

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        api('createNetworkACLList', {
          name: values.name,
          description: values.description,
          vpcid: this.resource.id
        }).then(response => {
          this.$pollJob({
            jobId: response.createnetworkacllistresponse.jobid,
            title: this.$t('message.success.add.network.acl'),
            description: values.name || values.description,
            successMethod: () => {
              this.fetchLoading = false
            },
            errorMessage: this.$t('message.add.network.acl.failed'),
            errorMethod: () => {
              this.fetchLoading = false
            },
            loadingMessage: this.$t('message.add.network.acl.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.modals.networkAcl = false
          this.fetchLoading = false
          this.fetchAclList()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
        this.fetchLoading = false
      })
    },
    handleCreateVpnGateway () {
      this.fetchLoading = true
      api('createVpnGateway', {
        vpcid: this.resource.id
      }).then(response => {
        this.$pollJob({
          jobId: response.createvpngatewayresponse.jobid,
          title: this.$t('message.success.add.vpn.gateway'),
          description: this.resource.id,
          successMethod: () => {
            this.fetchLoading = false
          },
          errorMessage: this.$t('message.add.vpn.gateway.failed'),
          errorMethod: () => {
            this.fetchLoading = false
          },
          loadingMessage: this.$t('message.add.vpn.gateway.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
        this.handleFetchData()
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.handleFetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.handleFetchData()
    }
  }
}
</script>

<style lang="scss" scoped>
.list {

  &__item,
  &__row {
    display: flex;
    flex-wrap: wrap;
    width: 100%;
  }

  &__item {
    margin-bottom: -20px;
  }

  &__col {
    flex: 1;
    margin-right: 20px;
    margin-bottom: 20px;
  }

  &__label {
    font-weight: bold;
  }

}

.pagination {
  margin-top: 20px;
}

.table {
  margin-top: 20px;
  overflow-y: auto;
}
</style>
