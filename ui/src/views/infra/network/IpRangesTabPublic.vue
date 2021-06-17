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
      :disabled="!('createVlanIpRange' in this.$store.getters.apis)"
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
      <template slot="account" slot-scope="record" v-if="!basicGuestNetwork">
        <a-button @click="() => handleOpenAccountModal(record)">{{ `[${record.domain}] ${record.account === undefined ? '' : record.account}` }}</a-button>
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <tooltip-button
            v-if="record.account === 'system' && !basicGuestNetwork"
            tooltipPlacement="bottom"
            :tooltip="$t('label.add.account')"
            icon="user-add"
            @click="() => handleOpenAddAccountModal(record)"
            :disabled="!('dedicatePublicIpRange' in $store.getters.apis)" />
          <tooltip-button
            v-if="record.account !== 'system' && !basicGuestNetwork"
            tooltipPlacement="bottom"
            :tooltip="$t('label.release.account')"
            icon="user-delete"
            type="danger"
            @click="() => handleRemoveAccount(record.id)"
            :disabled="!('releasePublicIpRange' in $store.getters.apis)" />
          <tooltip-button
            tooltipPlacement="bottom"
            :tooltip="$t('label.remove.ip.range')"
            icon="delete"
            type="danger"
            @click="handleDeleteIpRange(record.id)"
            :disabled="!('deleteVlanIpRange' in $store.getters.apis)" />
        </div>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      style="overflow-y: auto"
      :current="page"
      :pageSize="pageSize"
      :total="total"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-model="accountModal"
      v-if="selectedItem"
      :maskClosable="false"
      @ok="accountModal = false">
      <div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}</div>
          <div>{{ selectedItem.account }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.domain') }}</div>
          <div>{{ selectedItem.domain }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.system.vms') }}</div>
          <div>{{ selectedItem.forsystemvms }}</div>
        </div>
      </div>
    </a-modal>

    <a-modal
      :zIndex="1001"
      :maskClosable="false"
      v-model="addAccountModal"
      :title="$t('label.add.account')"
      @ok="handleAddAccount">
      <a-spin :spinning="domainsLoading">
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}:</div>
          <a-input v-model="addAccount.account" autoFocus></a-input>
        </div>
        <div>
          <div class="list__label">{{ $t('label.domain') }}:</div>
          <a-select v-model="addAccount.domain">
            <a-select-option
              v-for="domain in domains"
              :key="domain.id"
              :value="domain.id">{{ domain.path || domain.name || domain.description }}
            </a-select-option>
          </a-select>
        </div>
      </a-spin>
    </a-modal>

    <a-modal
      v-model="addIpRangeModal"
      :title="$t('label.add.ip.range')"
      :maskClosable="false"
      @ok="handleAddIpRange">
      <a-form
        :form="form"
        @submit="handleAddIpRange"
        layout="vertical"
        class="form"
      >
        <a-form-item :label="$t('label.podid')" class="form__item" v-if="basicGuestNetwork">
          <a-select
            autoFocus
            v-decorator="['podid', {
              rules: [{ required: true, message: `${$t('label.required')}` }]
            }]"
          >
            <a-select-option v-for="pod in pods" :key="pod.id" :value="pod.id">{{ pod.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.gateway')" class="form__item">
          <a-input
            autoFocus
            v-decorator="['gateway', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.netmask')" class="form__item">
          <a-input
            v-decorator="['netmask', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.vlan')" class="form__item" v-if="!basicGuestNetwork">
          <a-input
            v-decorator="['vlan']">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.startip')" class="form__item">
          <a-input
            v-decorator="['startip', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.endip')" class="form__item">
          <a-input
            v-decorator="['endip', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <div class="form__item" v-if="!basicGuestNetwork">
          <div style="color: black;">{{ $t('label.set.reservation') }}</div>
          <a-switch @change="handleShowAccountFields"></a-switch>
        </div>
        <div v-if="showAccountFields && !basicGuestNetwork" style="margin-top: 20px;">
          <div v-html="$t('label.set.reservation.desc')"></div>
          <a-form-item :label="$t('label.system.vms')" class="form__item">
            <a-switch v-decorator="['forsystemvms']"></a-switch>
          </a-form-item>
          <a-spin :spinning="domainsLoading">
            <a-form-item :label="$t('label.account')" class="form__item">
              <a-input v-decorator="['account']"></a-input>
            </a-form-item>
            <a-form-item :label="$t('label.domain')" class="form__item">
              <a-select v-decorator="['domain']">
                <a-select-option
                  v-for="domain in domains"
                  :key="domain.id"
                  :value="domain.id">{{ domain.path || domain.name || domain.description }}
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
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'IpRangesTabPublic',
  components: {
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
    },
    network: {
      type: Object,
      required: true
    },
    basicGuestNetwork: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      componentLoading: false,
      items: [],
      total: 0,
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
      podsLoading: false,
      pods: [],
      page: 1,
      pageSize: 10,
      columns: [
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
        },
        {
          title: this.$t('label.startip'),
          dataIndex: 'startip'
        },
        {
          title: this.$t('label.endip'),
          dataIndex: 'endip'
        },
        {
          title: this.$t('label.action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    if (!this.basicGuestNetwork) {
      this.columns.splice(5, 0,
        {
          title: this.$t('label.account'),
          scopedSlots: { customRender: 'account' }
        }
      )
    } else {
      this.columns.unshift({
        title: this.$t('label.pod'),
        dataIndex: 'podname'
      })
    }
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
        this.total = response.listvlaniprangesresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
      this.fetchPods()
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
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    fetchPods () {
      this.podsLoading = true
      api('listPods', {
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.total = response.listpodsresponse.count || 0
        this.pods = response.listpodsresponse.pod ? response.listpodsresponse.pod : []
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      }).finally(() => {
        this.podsLoading = false
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
        this.$notifyError(error)
      }).finally(() => {
        this.addAccountModal = false
        this.domainsLoading = false
        this.fetchData()
      })
    },
    handleRemoveAccount (id) {
      this.componentLoading = true
      api('releasePublicIpRange', { id }).catch(error => {
        this.$notifyError(error)
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
        this.$notifyError(error)
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
        var params = {
          gateway: values.gateway,
          netmask: values.netmask,
          startip: values.startip,
          endip: values.endip
        }
        if (!this.basicGuestNetwork) {
          params.zoneId = this.resource.zoneid
          params.vlan = values.vlan
          params.forsystemvms = values.forsystemvms
          params.account = values.forsystemvms ? null : values.account
          params.domainid = values.forsystemvms ? null : values.domain
          params.forvirtualnetwork = true
        } else {
          params.forvirtualnetwork = false
          params.podid = values.podid
          params.networkid = this.network.id
        }
        api('createVlanIpRange', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.add.iprange')
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
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
