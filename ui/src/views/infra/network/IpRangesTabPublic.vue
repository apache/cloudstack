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
      type="dashed"
      icon="plus"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpRangeModal">
      {{ $t('label.add.ip.range') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template slot="account" slot-scope="record">
        <a-button @click="() => handleOpenAccountModal(record)">{{ `[${record.domain}] ${record.account}` }}</a-button>
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <a-popover v-if="record.account === 'system'" placement="bottom">
            <template slot="content">{{ $t('label.add.account') }}</template>
            <a-button
              icon="user-add"
              shape="round"
              type="primary"
              @click="() => handleOpenAddAccountModal(record)"></a-button>
          </a-popover>
          <a-popover
            v-else
            placement="bottom">
            <template slot="content">{{ $t('label.release.account') }}</template>
            <a-button
              icon="user-delete"
              shape="round"
              type="danger"
              @click="() => handleRemoveAccount(record.id)"></a-button>
          </a-popover>
          <a-popover placement="bottom">
            <template slot="content">{{ $t('label.remove.ip.range') }}</template>
            <a-button icon="delete" shape="round" type="danger" @click="handleDeleteIpRange(record.id)"></a-button>
          </a-popover>
        </div>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      style="overflow-y: auto"
      :current="page"
      :pageSize="pageSize"
      :total="items.length"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger/>

    <a-modal v-model="accountModal" v-if="selectedItem" @ok="accountModal = false">
      <div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('account') }}</div>
          <div>{{ selectedItem.account }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('domain') }}</div>
          <div>{{ selectedItem.domain }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('System VMs') }}</div>
          <div>{{ selectedItem.forsystemvms }}</div>
        </div>
      </div>
    </a-modal>

    <a-modal :zIndex="1001" v-model="addAccountModal" :title="$t('label.add.account')" @ok="handleAddAccount">
      <a-spin :spinning="domainsLoading">
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('account') }}:</div>
          <a-input v-model="addAccount.account"></a-input>
        </div>
        <div>
          <div class="list__label">{{ $t('domain') }}:</div>
          <a-select v-model="addAccount.domain">
            <a-select-option
              v-for="domain in domains"
              :key="domain.id"
              :value="domain.id">{{ domain.name }}
            </a-select-option>
          </a-select>
        </div>
      </a-spin>
    </a-modal>

    <a-modal v-model="addIpRangeModal" :title="$t('label.add.ip.range')" @ok="handleAddIpRange">
      <a-form
        :form="form"
        @submit="handleAddIpRange"
        layout="vertical"
        class="form"
      >
        <a-form-item :label="$t('gateway')" class="form__item">
          <a-input
            v-decorator="['gateway', { rules: [{ required: true, message: 'Required' }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('netmask')" class="form__item">
          <a-input
            v-decorator="['netmask', { rules: [{ required: true, message: 'Required' }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('vlan')" class="form__item">
          <a-input
            v-decorator="['vlan']">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('startip')" class="form__item">
          <a-input
            v-decorator="['startip', { rules: [{ required: true, message: 'Required' }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('endip')" class="form__item">
          <a-input
            v-decorator="['endip', { rules: [{ required: true, message: 'Required' }] }]">
          </a-input>
        </a-form-item>
        <div class="form__item">
          <div style="color: black;">{{ $t('label.set.reservation') }}</div>
          <a-switch @change="handleShowAccountFields"></a-switch>
        </div>
        <div v-if="showAccountFields" style="margin-top: 20px;">
          <p>(optional) Please specify an account to be associated with this IP range.</p>
          <p>System VMs: Enable dedication of public IP range for SSVM and CPVM, account field disabled. Reservation strictness defined on 'system.vm.public.ip.reservation.mode.strictness'.</p>
          <a-form-item :label="$t('System VMs')" class="form__item">
            <a-switch v-decorator="['forsystemvms']"></a-switch>
          </a-form-item>
          <a-spin :spinning="domainsLoading">
            <a-form-item :label="$t('account')" class="form__item">
              <a-input v-decorator="['account']"></a-input>
            </a-form-item>
            <a-form-item :label="$t('domain')" class="form__item">
              <a-select v-decorator="['domain']">
                <a-select-option
                  v-for="domain in domains"
                  :key="domain.id"
                  :value="domain.id">{{ domain.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-spin>
        </div>
      </a-form>
    </a-modal>

  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'IpRangesTabPublic',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    network: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      componentLoading: false,
      items: [],
      selectedItem: null,
      accountModal: false,
      addAccountModal: false,
      addAccount: {
        account: null,
        domain: null
      },
      domains: [],
      domainsLoading: false,
      addIpRangeModal: false,
      showAccountFields: false,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('netmask'),
          dataIndex: 'netmask'
        },
        {
          title: this.$t('vlan'),
          dataIndex: 'vlan'
        },
        {
          title: this.$t('startip'),
          dataIndex: 'startip'
        },
        {
          title: this.$t('endip'),
          dataIndex: 'endip'
        },
        {
          title: this.$t('account'),
          scopedSlots: { customRender: 'account' }
        },
        {
          title: this.$t('action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    network (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.componentLoading = true
      api('listVlanIpRanges', {
        networkid: this.network.id,
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.items = response.listvlaniprangesresponse.vlaniprange ? response.listvlaniprangesresponse.vlaniprange : []
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.listvlaniprangesresponse
            ? error.response.data.listvlaniprangesresponse.errortext : error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.componentLoading = false
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
          this.form.setFieldsValue({ domain: this.domains[0].id })
        }
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.listdomains
            ? error.response.data.listdomains.errortext : error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    handleAddAccount () {
      this.domainsLoading = true

      if (this.addIpRangeModal === true) {
        this.addAccountModal = false
        return
      }

      api('dedicatePublicIpRange', {
        id: this.selectedItem.id,
        zoneid: this.selectedItem.zoneid,
        domainid: this.addAccount.domain,
        account: this.addAccount.account
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.dedicatepubliciprangeresponse
            ? error.response.data.dedicatepubliciprangeresponse.errortext : error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.addAccountModal = false
        this.domainsLoading = false
        this.fetchData()
      })
    },
    handleRemoveAccount (id) {
      this.componentLoading = true
      api('releasePublicIpRange', { id }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.releasepubliciprangeresponse
            ? error.response.data.releasepubliciprangeresponse.errortext : error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.fetchData()
      })
    },
    handleOpenAccountModal (item) {
      this.selectedItem = item
      this.accountModal = true
    },
    handleOpenAddAccountModal (item) {
      if (!this.addIpRangeModal) {
        this.selectedItem = item
      }
      this.addAccountModal = true
      this.fetchDomains()
    },
    handleShowAccountFields () {
      if (this.showAccountFields === false) {
        this.showAccountFields = true
        this.fetchDomains()
        return
      }
      this.showAccountFields = false
    },
    handleOpenAddIpRangeModal () {
      this.addIpRangeModal = true
    },
    handleDeleteIpRange (id) {
      this.componentLoading = true
      api('deleteVlanIpRange', { id }).then(() => {
        this.$notification.success({
          message: 'Removed IP Range'
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.deletevlaniprangeresponse
            ? error.response.data.deletevlaniprangeresponse.errortext : error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.componentLoading = false
        this.fetchData()
      })
    },
    handleAddIpRange (e) {
      this.form.validateFields((error, values) => {
        if (error) return

        this.componentLoading = true
        this.addIpRangeModal = false
        api('createVlanIpRange', {
          zoneId: this.resource.zoneid,
          vlan: values.vlan,
          gateway: values.gateway,
          netmask: values.netmask,
          startip: values.startip,
          endip: values.endip,
          forsystemvms: values.forsystemvms,
          account: values.forsystemvms ? null : values.account,
          domainid: values.forsystemvms ? null : values.domain,
          forvirtualnetwork: true
        }).then(() => {
          this.$notification.success({
            message: 'Successfully added IP Range'
          })
        }).catch(error => {
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.createvlaniprangeresponse
              ? error.response.data.createvlaniprangeresponse.errortext : error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
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
    &__item {
      display: flex;
    }

    &__data {
      display: flex;
      flex-wrap: wrap;
    }

    &__col {
      flex-basis: calc((100% / 3) - 20px);
      margin-right: 20px;
      margin-bottom: 10px;
    }

    &__label {
      font-weight: bold;
    }
  }

  .ant-list-item {
    padding-top: 0;
    padding-bottom: 0;

    &:not(:first-child) {
      padding-top: 20px;
    }

    &:not(:last-child) {
      padding-bottom: 20px;
    }
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }

  .ant-select {
    width: 100%;
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
</style>
