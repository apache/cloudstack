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
  <a-spin :spinning="componentLoading">
    <a-button
      :disabled="!('createIpv4SubnetForZone' in $store.getters.apis)"
      type="dashed"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpv4SubnetModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.ipv4.subnet') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="ipv4SubnetColumns"
      :dataSource="ipv4Subnets"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'subnet'">
          {{ record.subnet }}
        </template>
        <template v-if="column.key === 'domain'">
          {{ record.domain }}
        </template>
        <template v-if="column.key === 'account'">
          {{ record.account }}
        </template>
        <template v-if="column.key === 'actions'">
          <div
            class="actions"
            style="text-align: right" >
            <router-link :to="{ name: 'ipv4subnets', query: { parentid: record.id }}" target="_blank">
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.view') + ' ' + $t('label.ipv4.subnets')"
                icon="environment-outlined"/>
            </router-link>
            <tooltip-button
              v-if="!record.domain && record.subnet"
              tooltipPlacement="bottom"
              :tooltip="$t('label.add.account')"
              icon="user-add-outlined"
              @onClick="() => handleOpenAddAccountModal(record)"
              :disabled="!('dedicateIpv4GuestSubnetForZone' in $store.getters.apis)" />
            <tooltip-button
              v-if="record.domain"
              tooltipPlacement="bottom"
              :tooltip="$t('label.release.account')"
              icon="user-delete-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleRemoveAccount(record.id)"
              :disabled="!('releaseIpv4GuestSubnetForZone' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.update.ipv4.subnet')"
              icon="edit-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleUpdateIpv4SubnetModal(record)"
              :disabled="!('updateIpv4GuestSubnetForZone' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.remove.ipv4.subnet')"
              icon="delete-outlined"
              type="primary"
              :danger="true"
              @onClick="handleDeleteIpv4Subnet(record.id)"
              :disabled="!('deleteIpv4GuestSubnetForZone' in $store.getters.apis)" />
          </div>
        </template>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="ipv4SubnetPage"
      :pageSize="ipv4SubnetPageSize"
      :total="ipv4SubnetsTotal"
      :showTotal="total => `${$t('label.total')} ${ipv4SubnetsTotal} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changeIpv4SubnetPage"
      @showSizeChange="changeIpv4SubnetPageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :visible="accountModal"
      v-if="selectedItem"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="accountModal = false">
      <div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}</div>
          <div>{{ selectedItem.account }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.domain') }}</div>
          <div>{{ selectedItem.domain }}</div>
        </div>
      </div>

      <div :span="24" class="action-button">
        <a-button @click="accountModal = false">{{ $t('label.close') }}</a-button>
      </div>
    </a-modal>

    <a-modal
      v-if="addAccountModal"
      :zIndex="1001"
      :closable="true"
      :maskClosable="false"
      :visible="addAccountModal"
      :title="$t('label.add.account')"
      :footer="null"
      @cancel="addAccountModal = false">
      <a-spin :spinning="domainsLoading" v-ctrl-enter="handleAddAccount">
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}:</div>
          <a-input v-model:value="addAccount.account" v-focus="true"></a-input>
        </div>
        <div>
          <div class="list__label">{{ $t('label.domain') }}:</div>
          <a-select
            v-model:value="addAccount.domain"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="domain in domains"
              :key="domain.id"
              :value="domain.id"
              :label="domain.path || domain.name || domain.description">{{ domain.path || domain.name || domain.description }}
            </a-select-option>
          </a-select>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="addAccountModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddAccount">{{ $t('label.ok') }}</a-button>
        </div>
      </a-spin>
    </a-modal>

    <a-modal
      v-if="addIpv4SubnetModal"
      :visible="addIpv4SubnetModal"
      :title="$t('label.add.ipv4.subnet')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="addIpv4SubnetModal = false">
      <a-form
        :ref="ipv4SubnetFormRef"
        :model="ipv4SubnetForm"
        :rules="ipv4SubnetRules"
        @finish="handleAddIpv4Subnet"
        v-ctrl-enter="handleAddIpv4Subnet"
        layout="vertical"
        class="form"
      >
        <a-form-item name="subnet" ref="subnet" :label="$t('label.subnet')" class="form__item">
          <a-input v-focus="true" v-model:value="ipv4SubnetForm.subnet" />
        </a-form-item>
        <div class="form__item">
          <div style="color: black;">{{ $t('label.set.reservation') }}</div>
          <a-switch v-model:checked="showAccountFields" @change="handleShowAccountFields" />
        </div>
        <div v-if="showAccountFields" style="margin-top: 20px;">
          <div v-html="$t('label.ipv4.subnet.set.reservation.desc')"></div><br>
          <a-spin :spinning="domainsLoading">
            <a-form-item name="account" ref="account" :label="$t('label.account')" class="form__item">
              <a-input v-model:value="ipv4SubnetForm.account"></a-input>
            </a-form-item>
            <a-form-item name="domain" ref="domain" :label="$t('label.domain')" class="form__item">
              <a-select
                v-model:value="ipv4SubnetForm.domain"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="domain in domains"
                  :key="domain.id"
                  :value="domain.id"
                  :label="domain.path || domain.name || domain.description">{{ domain.path || domain.name || domain.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-spin>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="addIpv4SubnetModal = false; showAccountFields = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddIpv4Subnet">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      :visible="updateIpv4SubnetModal"
      :title="$t('label.update.ip.range')"
      v-if="selectedItem"
      :maskClosable="false"
      :footer="null"
      @cancel="updateIpv4SubnetModal = false">
      <a-form
        :ref="updateIpv4SubnetRef"
        :model="formUpdateIpv4Subnet"
        :rules="updateIpv4SubnetRules"
        @finish="handleAddIpv4Subnet"
        v-ctrl-enter="handleAddIpv4Subnet"
        layout="vertical"
        class="form"
      >
        <div>
          <a-form-item name="subnet" ref="subnet" :label="$t('label.subnet')" class="form__item">
            <a-input v-focus="true" v-model:value="formUpdateIpv4Subnet.subnet"></a-input>
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="updateIpv4SubnetModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleUpdateIpv4Subnet">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
    <br>
    <br>

    <a-button
      :disabled="!('createGuestNetworkIpv6Prefix' in $store.getters.apis)"
      type="dashed"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpv6PrefixForm()">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.ip.v6.prefix') }}
    </a-button>
    <a-table
      style="overflow-y: auto"
      size="small"
      :columns="ipv6Columns"
      :dataSource="ipv6Prefixes"
      :rowKey="record => record.id + record.prefix"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'allocated'">
          {{ record.usedsubnets + '/' + record.totalsubnets }}
        </template>
        <template v-if="column.key === 'actions'">
          <div class="actions">
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.delete.ip.v6.prefix')"
              type="primary"
              icon="delete-outlined"
              :danger="true"
              @click="handleDeleteIpv6Prefix(record)"
              :disabled="!('deleteGuestNetworkIpv6Prefix' in $store.getters.apis)" />
          </div>
        </template>
      </template>
    </a-table>
    <br>
    <br>

    <a-button
      :disabled="!('createNetwork' in $store.getters.apis)"
      type="dashed"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenShowCreateForm">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.guest.network') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          <resource-icon v-if="record.icon" :image="record.icon.base64image" size="1x" style="margin-right: 5px"/>
          <apartment-outlined v-else style="margin-right: 5px"/>
          <router-link :to="{ path: '/guestnetwork/' + record.id }">
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
      :total="total"
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
      v-if="showCreateForm"
      :visible="showCreateForm"
      :title="$t('label.add.guest.network')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="showCreateForm = false"
      centered
      width="auto">
      <CreateNetwork :resource="{ zoneid: resource.zoneid }" @close-action="closeAction"/>
    </a-modal>

    <a-modal
      :visible="addIpv6PrefixModal"
      :title="$t('Add IPv6 Prefix')"
      :maskClosable="false"
      :footer="null"
      @cancel="handleCloseAddIpv6PrefixForm()"
      centered
      v-ctrl-enter="handleAddIpv6Prefix">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @submit="handleAddIpv6Prefix"
        layout="vertical"
        class="form"
      >
        <a-form-item name="prefix" ref="prefix" :label="$t('label.prefix')" class="form__item">
          <a-input v-model:value="form.prefix" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="handleCloseAddIpv6PrefixForm()">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddIpv6Prefix">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import CreateNetwork from '@/views/network/CreateNetwork'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'IpRangesTabGuest',
  components: {
    CreateNetwork,
    ResourceIcon,
    TooltipButton
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
  data () {
    return {
      componentLoading: false,
      items: [],
      total: 0,
      showCreateForm: false,
      page: 1,
      pageSize: 10,
      selectedItem: null,
      ipv4Subnets: [],
      showAccountFields: false,
      accountModal: false,
      addAccountModal: false,
      addAccount: {
        account: null,
        domain: null
      },
      domains: [],
      domainsLoading: false,
      addIpv4SubnetModal: false,
      updateIpv4SubnetModal: false,
      ipv4SubnetPage: 1,
      ipv4SubnetPageSize: 10,
      ipv4SubnetColumns: [
        {
          title: this.$t('label.subnet'),
          dataIndex: 'subnet'
        },
        {
          title: this.$t('label.domain'),
          dataIndex: 'domain'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          key: 'actions',
          title: this.$t('label.actions'),
          width: '20%'
        }
      ],
      columns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlan'
        },
        {
          title: this.$t('label.broadcasturi'),
          dataIndex: 'broadcasturi'
        },
        {
          title: this.$t('label.cidr'),
          dataIndex: 'cidr'
        },
        {
          title: this.$t('label.ip6cidr'),
          dataIndex: 'ip6cidr'
        }
      ],
      ipv6Prefixes: [],
      ipv6Columns: [
        {
          title: this.$t('label.prefix'),
          dataIndex: 'prefix'
        },
        {
          key: 'allocated',
          title: this.$t('label.allocated')
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ],
      addIpv6PrefixModal: false
    }
  },
  beforeCreate () {
    this.form = null
    this.formRef = null
    this.rules = null
    this.ipv4SubnetForm = null
    this.ipv4SubnetFormRef = null
    this.ipv4SubnetRules = null
  },
  created () {
    this.initAddIpv4SubnetForm()
    this.initUpdateIpv4SubnetForm()
    this.fetchData()
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        prefix: [{ required: true, message: this.$t('label.required') }]
      })
    },
    initAddIpv4SubnetForm () {
      this.ipv4SubnetFormRef = ref()
      this.ipv4SubnetForm = reactive({
      })
      this.ipv4SubnetRules = reactive({
        subnet: [{ required: true, message: this.$t('label.required') }]
      })
    },
    initUpdateIpv4SubnetForm () {
      this.updateIpv4SubnetRef = ref()
      this.formUpdateIpv4Subnet = reactive({})
      this.updateIpv4SubnetRules = reactive({
        subnet: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchDomains () {
      this.domainsLoading = true
      api('listDomains', {
        details: 'min',
        listAll: true
      }).then(response => {
        this.domains = response.listdomainsresponse.domain ? response.listdomainsresponse.domain : []
        if (this.domains.length > 0) {
          this.addAccount.domain = this.domains[0].id
          this.ipv4SubnetForm.domain = this.domains[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    fetchData () {
      api('listNetworks', {
        zoneid: this.resource.zoneid,
        physicalnetworkid: this.resource.id,
        showicon: true,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.items = response?.listnetworksresponse?.network || []
        this.total = response?.listnetworksresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
      this.fetchZoneIpv4Subnet()
      this.fetchIpv6PrefixData()
    },
    fetchZoneIpv4Subnet () {
      this.componentLoading = true
      api('listIpv4GuestSubnetsForZone', {
        zoneid: this.resource.zoneid,
        showicon: true,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.ipv4Subnets = response?.listipv4guestsubnetsforzoneresponse?.zoneipv4subnet || []
        this.ipv4SubnetsTotal = response?.listipv4guestsubnetsforzoneresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    fetchIpv6PrefixData () {
      api('listGuestNetworkIpv6Prefixes', {
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.ipv6Prefixes = response?.listguestnetworkipv6prefixesresponse?.guestnetworkipv6prefix || []
        this.total = response?.listguestnetworkipv6prefixesresponse?.count || 0
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      }).finally(() => {
      })
    },
    handleOpenShowCreateForm () {
      this.showCreateForm = true
    },
    closeAction () {
      this.showCreateForm = false
    },
    handleOpenAddIpv6PrefixForm () {
      this.initForm()
      this.addIpv6PrefixModal = true
    },
    handleCloseAddIpv6PrefixForm () {
      this.formRef.value.resetFields()
      this.addIpv6PrefixModal = false
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
    },
    handleAddIpv6Prefix (e) {
      if (this.componentLoading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        this.componentLoading = true
        this.addIpv6PrefixModal = false
        var params = {
          zoneid: this.resource.zoneid,
          prefix: values.prefix
        }
        api('createGuestNetworkIpv6Prefix', params).then(response => {
          this.$pollJob({
            jobId: response.createguestnetworkipv6prefixresponse.jobid,
            title: this.$t('label.add.ip.v6.prefix'),
            description: values.prefix,
            successMessage: this.$t('message.success.add.ip.v6.prefix'),
            successMethod: () => {
              this.componentLoading = false
              this.fetchData()
            },
            errorMessage: this.$t('message.add.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchData()
            },
            loadingMessage: this.$t('message.add.ip.v6.prefix.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchData()
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleDeleteIpv6Prefix (prefix) {
      this.componentLoading = true
      api('deleteGuestNetworkIpv6Prefix', { id: prefix.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteguestnetworkipv6prefixresponse.jobid,
          title: this.$t('label.delete.ip.v6.prefix'),
          description: prefix.prefix,
          successMessage: this.$t('message.ip.v6.prefix.deleted') + ' ' + prefix.prefix,
          successMethod: () => {
            this.componentLoading = false
            this.fetchIpv6PrefixData()
          },
          errorMessage: this.$t('message.delete.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchIpv6PrefixData()
          },
          loadingMessage: this.$t('message.delete.ip.v6.prefix.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchIpv6PrefixData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
        this.fetchIpv6PrefixData()
      })
    },
    handleAddAccount () {
      if (this.domainsLoading) return
      this.domainsLoading = true

      if (this.addIpv4SubnetModal === true) {
        this.addAccountModal = false
        return
      }

      var params = {
        id: this.selectedItem.id,
        zoneid: this.selectedItem.zoneid,
        domainid: this.addAccount.domain
      }

      if (this.addAccount.account) {
        params.account = this.addAccount.account
      }

      api('dedicateIpv4GuestSubnetForZone', params).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addAccountModal = false
        this.domainsLoading = false
        this.fetchData()
      })
    },
    handleRemoveAccount (id) {
      this.componentLoading = true
      api('releaseIpv4GuestSubnetForZone', { id }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchData()
      })
    },
    handleOpenAddAccountModal (item) {
      if (!this.addIpv4SubnetModal) {
        this.selectedItem = item
      }
      this.addAccountModal = true
      this.fetchDomains()
    },
    handleShowAccountFields () {
      if (this.showAccountFields) {
        this.fetchDomains()
      }
    },
    handleOpenAddIpv4SubnetModal () {
      this.initAddIpv4SubnetForm()
      this.addIpv4SubnetModal = true
    },
    handleUpdateIpv4SubnetModal (item) {
      this.initUpdateIpv4SubnetForm()
      this.selectedItem = item
      this.updateIpv4SubnetModal = true
      this.formUpdateIpv4Subnet.subnet = this.selectedItem?.subnet || ''
    },
    handleDeleteIpv4Subnet (id) {
      this.componentLoading = true
      api('deleteIpv4GuestSubnetForZone', { id }).then(() => {
        this.$notification.success({
          message: this.$t('message.success.delete.ipv4.subnet')
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
        this.fetchData()
      })
    },
    handleAddIpv4Subnet (e) {
      if (this.componentLoading) return
      this.ipv4SubnetFormRef.value.validate().then(() => {
        const values = toRaw(this.ipv4SubnetForm)
        this.componentLoading = true
        this.addIpv4SubnetModal = false
        this.showAccountFields = false
        var params = {
          zoneId: this.resource.zoneid,
          subnet: values.subnet,
          domainid: values.domain,
          account: values.account
        }
        api('createIpv4SubnetForZone', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.add.ipv4.subnet')
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.createipv4subnetforzoneresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
      }).catch(error => {
        this.ipv4SubnetFormRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleUpdateIpv4Subnet (e) {
      if (this.componentLoading) return
      this.updateIpv4SubnetRef.value.validate().then(() => {
        const values = toRaw(this.formUpdateIpv4Subnet)

        this.componentLoading = true
        this.updateIpv4SubnetModal = false
        var params = {
          id: this.selectedItem.id,
          subnet: values.subnet
        }
        api('updateIpv4GuestSubnetForZone', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.update.ipv4.subnet')
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.updatevlanIpv4Subnetresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
      })
    },
    changeIpv4SubnetPage (page, pageSize) {
      this.ipv4SubnetPage = page
      this.ipv4SubnetPageSize = pageSize
      this.fetchData()
    },
    changeIpv4SubnetPageSize (currentPage, pageSize) {
      this.ipv4SubnetPage = currentPage
      this.ipv4SubnetPageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style lang="scss" scoped>
  .pagination {
    margin-top: 20px;
  }
  .form {
    .actions {
      display: flex;
      justify-content: flex-end;

      button {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }

    }
  }
  .pagination {
    margin-top: 20px;
  }
  .ant-select {
    width: 100%;
  }

</style>
