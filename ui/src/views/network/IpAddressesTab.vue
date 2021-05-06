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
        icon="plus"
        style="width: 100%; margin-bottom: 15px"
        @click="onShowAcquireIp">
        {{ $t('label.acquire.new.ip') }}
      </a-button>
      <div v-if="$route.path.startsWith('/vpc')">
        Select Tier:
        <a-select
          autoFocus
          style="width: 40%; margin-left: 15px;margin-bottom: 15px"
          :loading="fetchLoading"
          defaultActiveFirstOption
          :value="vpcTier"
          @change="handleTierSelect"
        >
          <a-select-option key="all" value="">
            {{ $t('label.view.all') }}
          </a-select-option>
          <a-select-option v-for="network in networksList" :key="network.id" :value="network.id">
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
        :pagination="false" >
        <template slot="ipaddress" slot-scope="text, record">
          <router-link v-if="record.forvirtualnetwork === true" :to="{ path: '/publicip/' + record.id }" >{{ text }} </router-link>
          <div v-else>{{ text }}</div>
          <a-tag v-if="record.issourcenat === true">source-nat</a-tag>
        </template>

        <template slot="state" slot-scope="text, record">
          <status :text="record.state" displayText />
        </template>

        <template slot="virtualmachineid" slot-scope="text, record">
          <a-icon type="desktop" v-if="record.virtualmachineid" />
          <router-link :to="{ path: '/vm/' + record.virtualmachineid }" > {{ record.virtualmachinename || record.virtualmachineid }} </router-link>
        </template>

        <template slot="associatednetworkname" slot-scope="text, record">
          <router-link v-if="record.forvirtualnetwork === true" :to="{ path: '/guestnetwork/' + record.associatednetworkid }" > {{ record.associatednetworkname || record.associatednetworkid }} </router-link>
          <div v-else>{{ record.networkname }}</div>
        </template>

        <template slot="action" slot-scope="text, record">
          <tooltip-button
            v-if="record.issourcenat !== true && record.forvirtualnetwork === true"
            :tooltip="$t('label.action.release.ip')"
            type="danger"
            icon="delete"
            :disabled="!('disassociateIpAddress' in $store.getters.apis)"
            @click="releaseIpAddress(record)" />
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
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>
    <a-modal
      v-if="showAcquireIp"
      :visible="showAcquireIp"
      :title="$t('label.acquire.new.ip')"
      :closable="true"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @cancel="onCloseModal"
      @ok="acquireIpAddress"
      centered
      width="450px">
      <a-spin :spinning="acquireLoading">
        <a-alert :message="$t('message.action.acquire.ip')" type="warning" />
        <a-form-item :label="$t('label.ipaddress')">
          <a-select
            autoFocus
            style="width: 100%;"
            showSearch
            v-model="acquireIp">
            <a-select-option
              v-for="ip in listPublicIpAddress"
              :key="ip.ipaddress">{{ ip.ipaddress }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'IpAddressesTab',
  components: {
    Status,
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
      columns: [
        {
          title: this.$t('label.ipaddress'),
          dataIndex: 'ipaddress',
          scopedSlots: { customRender: 'ipaddress' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: this.$t('label.vm'),
          dataIndex: 'virtualmachineid',
          scopedSlots: { customRender: 'virtualmachineid' }
        },
        {
          title: this.$t('label.network'),
          dataIndex: 'associatednetworkname',
          scopedSlots: { customRender: 'associatednetworkname' }
        },
        {
          title: '',
          scopedSlots: { customRender: 'action' }
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
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
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
    acquireIpAddress () {
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
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.acquireLoading = false
      })
    },
    releaseIpAddress (ip) {
      this.fetchLoading = true
      api('disassociateIpAddress', {
        id: ip.id
      }).then(response => {
        this.$pollJob({
          jobId: response.disassociateipaddressresponse.jobid,
          successMessage: this.$t('message.success.release.ip'),
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.release.ip.failed'),
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.releasing.ip')} ${this.$t('label.for')} ${this.resource.name} ${this.$t('label.is.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result')
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
          if (item.state === 'Free') {
            this.listPublicIpAddress.push({
              ipaddress: item.ipaddress
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
