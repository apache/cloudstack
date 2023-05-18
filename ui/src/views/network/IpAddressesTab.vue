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
    <a-spin :spinning="fetchLoading">
      <a-button
        :disabled="!('associateIpAddress' in $store.getters.apis) || resource.type === 'Shared'"
        type="dashed"
        style="width: 100%; margin-bottom: 15px"
        @click="onShowAcquireIp">
        <template #icon><plus-outlined /></template>
        {{ $t('label.acquire.new.ip') }}
      </a-button>
      <a-button
        v-if="(('disassociateIpAddress' in $store.getters.apis) && this.selectedRowKeys.length > 0)"
        type="primary"
        danger
        style="width: 100%; margin-bottom: 15px"
        @click="bulkActionConfirmation()">
        <template #icon><delete-outlined /></template>
        {{ $t('label.action.bulk.release.public.ip.address') }}
      </a-button>
      <div v-if="$route.path.startsWith('/vpc')">
        {{ $t('label.select.tier') + ':' }}
        <a-select
          v-focus="true"
          style="width: 40%; margin-left: 15px;margin-bottom: 15px"
          :loading="fetchLoading"
          defaultActiveFirstOption
          v-model:value="vpcTier"
          @change="handleTierSelect"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option key="all" value="">
            {{ $t('label.view.all') }}
          </a-select-option>
          <a-select-option v-for="network in networksList" :key="network.id" :value="network.id" :label="network.name">
            {{ network.name }}
          </a-select-option>
        </a-select>
      </div>
      <a-table
        size="small"
        style="overflow-y: auto"
        :columns="columns"
        :dataSource="ips"
        :rowKey="item => item.id"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        :pagination="false" >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'ipaddress'">
            <router-link v-if="record.forvirtualnetwork === true" :to="{ path: '/publicip/' + record.id }" >{{ text }} </router-link>
            <div v-else>{{ text }}</div>
            <a-tag v-if="record.issourcenat === true">source-nat</a-tag>
          </template>

          <template v-if="column.key === 'state'">
            <status :text="record.state" displayText />
          </template>

          <template v-if="column.key === 'virtualmachineid'">
            <desktop-outlined v-if="record.virtualmachineid" />
            <router-link :to="{ path: '/vm/' + record.virtualmachineid }" > {{ record.virtualmachinename || record.virtualmachineid }} </router-link>
          </template>

          <template v-if="column.key === 'associatednetworkname'">
            <router-link v-if="record.forvirtualnetwork === true" :to="{ path: '/guestnetwork/' + record.associatednetworkid }" > {{ record.associatednetworkname || record.associatednetworkid }} </router-link>
            <div v-else>{{ record.networkname }}</div>
          </template>

          <template v-if="column.key === 'actions'">
            <tooltip-button
              v-if="record.issourcenat !== true && record.forvirtualnetwork === true"
              :tooltip="$t('label.action.release.ip')"
              type="primary"
              :danger="true"
              icon="delete-outlined"
              :disabled="!('disassociateIpAddress' in $store.getters.apis)"
              @onClick="releaseIpAddress(record)" />
          </template>
        </template>
      </a-table>
      <a-divider/>
      <a-pagination
        class="row-element pagination"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="totalIps"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>
    <a-modal
      v-if="showAcquireIp"
      :visible="showAcquireIp"
      :title="$t('label.acquire.new.ip')"
      :closable="true"
      :footer="null"
      @cancel="onCloseModal"
      centered
      width="450px">
      <a-spin :spinning="acquireLoading" v-ctrl-enter="acquireIpAddress">
        <a-alert :message="$t('message.action.acquire.ip')" type="warning" />
        <a-form layout="vertical" style="margin-top: 10px">
          <a-form-item :label="$t('label.ipaddress')">
            <a-select
              v-focus="true"
              style="width: 100%;"
              v-model:value="acquireIp"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="ip in listPublicIpAddress"
                :key="ip.ipaddress"
                :label="ip.ipaddress + '(' + ip.state + ')'">{{ ip.ipaddress }} ({{ ip.state }})</a-select-option>
            </a-select>
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button @click="onCloseModal">{{ $t('label.cancel') }}</a-button>
            <a-button ref="submit" type="primary" @click="acquireIpAddress">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>
    <bulk-action-view
      v-if="showConfirmationAction || showGroupActionModal"
      :showConfirmationAction="showConfirmationAction"
      :showGroupActionModal="showGroupActionModal"
      :items="ips"
      :selectedRowKeys="selectedRowKeys"
      :selectedItems="selectedItems"
      :columns="columns"
      :selectedColumns="selectedColumns"
      action="disassociateIpAddress"
      :loading="loading"
      :message="message"
      @group-action="releaseIpAddresses"
      @handle-cancel="handleCancel"
      @close-modal="closeModal" />
  </div>
</template>
<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import BulkActionView from '@/components/view/BulkActionView'
import eventBus from '@/config/eventBus'

export default {
  name: 'IpAddressesTab',
  components: {
    Status,
    TooltipButton,
    BulkActionView
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
      fetchLoading: false,
      ips: [],
      ipsTiers: [],
      networksList: [],
      defaultNetwork: '',
      vpcTier: '',
      page: 1,
      pageSize: 10,
      totalIps: 0,
      tiersSelect: false,
      selectedRowKeys: [],
      showGroupActionModal: false,
      selectedItems: [],
      selectedColumns: [],
      filterColumns: ['Actions'],
      showConfirmationAction: false,
      message: {
        title: this.$t('label.action.bulk.release.public.ip.address'),
        confirmMessage: this.$t('label.confirm.release.public.ip.addresses')
      },
      columns: [
        {
          key: 'ipaddress',
          title: this.$t('label.ipaddress'),
          dataIndex: 'ipaddress'
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          key: 'virtualmachineid',
          title: this.$t('label.vm'),
          dataIndex: 'virtualmachineid'
        },
        {
          key: 'associatednetworkname',
          title: this.$t('label.network'),
          dataIndex: 'associatednetworkname'
        },
        {
          key: 'actions',
          title: ''
        }
      ],
      showAcquireIp: false,
      acquireLoading: false,
      acquireIp: null,
      listPublicIpAddress: []
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  inject: ['parentFetchData'],
  methods: {
    fetchData () {
      const params = {
        listall: true,
        page: this.page,
        pagesize: this.pageSize
      }
      if (this.$route.path.startsWith('/vpc')) {
        this.networksList = this.resource.network
        params.vpcid = this.resource.id
        params.forvirtualnetwork = true
        if (this.vpcTier) {
          params.associatednetworkid = this.vpcTier
        }
      } else if (this.resource.type === 'Shared') {
        params.networkid = this.resource.id
        params.allocatedonly = false
        params.forvirtualnetwork = false
      } else {
        params.associatednetworkid = this.resource.id
      }
      this.fetchLoading = true
      api('listPublicIpAddresses', params).then(json => {
        this.totalIps = json.listpublicipaddressesresponse.count || 0
        this.ips = json.listpublicipaddressesresponse.publicipaddress || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchListPublicIpAddress () {
      return new Promise((resolve, reject) => {
        const params = {
          zoneid: this.resource.zoneid,
          domainid: this.resource.domainid,
          account: this.resource.account,
          forvirtualnetwork: true,
          allocatedonly: false
        }
        api('listPublicIpAddresses', params).then(json => {
          const listPublicIps = json.listpublicipaddressesresponse.publicipaddress || []
          resolve(listPublicIps)
        }).catch(reject)
      })
    },
    handleTierSelect (tier) {
      this.vpcTier = tier
      this.fetchData()
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
      this.selectedItems = (this.ips.filter(function (item) {
        return selection.indexOf(item.id) !== -1
      }))
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
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
    bulkActionConfirmation () {
      this.showConfirmationAction = true
      this.selectedColumns = this.columns.filter(column => {
        return !this.filterColumns.includes(column.title)
      })
      this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
    },
    acquireIpAddress () {
      if (this.acquireLoading) return
      const params = {}
      if (this.$route.path.startsWith('/vpc')) {
        params.vpcid = this.resource.id
        if (this.vpcTier) {
          params.networkid = this.vpcTier
        }
      } else {
        params.networkid = this.resource.id
      }
      params.ipaddress = this.acquireIp
      this.acquireLoading = true

      api('associateIpAddress', params).then(response => {
        this.$pollJob({
          jobId: response.associateipaddressresponse.jobid,
          successMessage: `${this.$t('message.success.acquire.ip')} ${this.$t('label.for')} ${this.resource.name}`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.acquire.ip.failed'),
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.acquiring.ip')} ${this.$t('label.for')} ${this.resource.name} ${this.$t('label.is.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result')
        })
        this.onCloseModal()
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.associateipaddressresponse.errortext || error.response.data.errorresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.acquireLoading = false
      })
    },
    handleCancel () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.selectedColumns = []
      this.selectedRowKeys = []
      this.parentFetchData()
    },
    releaseIpAddresses (e) {
      this.showConfirmationAction = false
      this.selectedColumns.splice(0, 0, {
        key: 'status',
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      if (this.selectedRowKeys.length > 0) {
        this.showGroupActionModal = true
      }
      for (const ip of this.selectedItems) {
        this.releaseIpAddress(ip)
      }
    },
    releaseIpAddress (ip) {
      this.fetchLoading = true
      api('disassociateIpAddress', {
        id: ip.id
      }).then(response => {
        const jobId = response.disassociateipaddressresponse.jobid
        eventBus.emit('update-job-details', { jobId, resourceId: null })
        this.$pollJob({
          title: this.$t('label.action.release.ip'),
          description: ip.id,
          jobId: jobId,
          successMessage: this.$t('message.success.release.ip'),
          successMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: ip.id, state: 'success' })
            }
            this.fetchData()
          },
          errorMessage: this.$t('message.release.ip.failed'),
          errorMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: ip.id, state: 'failed' })
            }
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.releasing.ip')} ${this.$t('label.for')} ${this.resource.name} ${this.$t('label.is.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          bulkAction: `${this.selectedItems.length > 0}` && this.showGroupActionModal
        })
      }).catch(error => {
        this.fetchLoading = false
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
      })
    },
    async onShowAcquireIp () {
      this.showAcquireIp = true
      this.acquireLoading = true
      this.listPublicIpAddress = []

      try {
        const listPublicIpAddress = await this.fetchListPublicIpAddress()
        listPublicIpAddress.forEach(item => {
          if (item.state === 'Free' || item.state === 'Reserved') {
            this.listPublicIpAddress.push({
              ipaddress: item.ipaddress,
              state: item.state
            })
          }
        })
        this.listPublicIpAddress.sort(function (a, b) {
          if (a.ipaddress < b.ipaddress) { return -1 }
          if (a.ipaddress > b.ipaddress) { return 1 }
          return 0
        })
        this.acquireIp = this.listPublicIpAddress && this.listPublicIpAddress.length > 0 ? this.listPublicIpAddress[0].ipaddress : null
        this.acquireLoading = false
      } catch (e) {
        this.acquireLoading = false
        this.$notifyError(e)
      }
    },
    onCloseModal () {
      this.showAcquireIp = false
    },
    closeModal () {
      this.showConfirmationAction = false
    }
  }
}
</script>

<style lang="scss" scoped>
.list {
    max-height: 95vh;
    width: 95vw;
    overflow-y: scroll;
    margin: -24px;

    @media (min-width: 1000px) {
      max-height: 70vh;
      width: 900px;
    }

    &__header,
    &__footer {
      padding: 20px;
    }

    &__header {
      display: flex;

      .ant-select {
        min-width: 200px;
      }

      &__col {

        &:not(:last-child) {
          margin-right: 20px;
        }

        &--full {
          flex: 1;
        }

      }

    }

    &__footer {
      display: flex;
      justify-content: flex-end;

      button {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }
    }

    &__item {
      padding-right: 20px;
      padding-left: 20px;

      &--selected {
        background-color: #e6f7ff;
      }

    }

    &__title {
      font-weight: bold;
    }

    &__outer-container {
      width: 100%;
      display: flex;
      flex-direction: column;
    }

    &__container {
      display: flex;
      flex-direction: column;
      width: 100%;
      cursor: pointer;

      @media (min-width: 760px) {
        flex-direction: row;
        align-items: center;
      }

    }

    &__row {
      margin-bottom: 10px;

      @media (min-width: 760px) {
        margin-right: 20px;
        margin-bottom: 0;
      }
    }

    &__radio {
      display: flex;
      justify-content: flex-end;
    }

  }
</style>
