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
  <div class="list" :loading="loading">
    <div class="list__header">
      <div class="list__header__col" v-if="tiersSelect">
        <a-select @change="handleTierSelect" v-model="vpcTiers" placeholder="Select a tier">
          <a-select-option v-for="network in networksList" :key="network.id" :value="network.id">
            {{ network.name }}
          </a-select-option>
        </a-select>
      </div>

      <div class="list__header__col list__header__col--full">
        <a-input-search
          placeholder="Search"
          v-model="searchQuery"
          @search="fetchData" />
      </div>
    </div>

    <a-table
      size="small"
      :loading="loading"
      :columns="columns"
      :dataSource="vmsList"
      :pagination="false"
      :rowKey="record => record.id || record.account">
      <template slot="name" slot-scope="record">
        <div>
          {{ record.name }}
        </div>
        <a-select
          v-if="nicsList.length && selectedVm && selectedVm === record.id"
          class="nic-select"
          :defaultValue="selectedNic.ipaddress">
          <a-select-option
            @click="selectedNic = item"
            v-for="item in nicsList"
            :key="item.id">
            {{ item.ipaddress }}
          </a-select-option>
        </a-select>
      </template>
      <template slot="state" slot-scope="text">
        <status :text="text ? text : ''" displayText />
      </template>
      <template slot="radio" slot-scope="text">
        <a-radio
          class="list__radio"
          :value="text"
          :checked="selectedVm && selectedVm === text"
          @change="fetchNics"></a-radio>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="vmsList.length"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger/>

    <div class="list__footer">
      <a-button @click="handleClose">{{ $t('cancel') }}</a-button>
      <a-button @click="handleSubmit" type="primary" :disabled="!selectedVm || !selectedNic">{{ $t('OK') }}</a-button>
    </div>

  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    Status
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      vmsList: [],
      selectedVm: null,
      nicsList: [],
      searchQuery: null,
      selectedNic: null,
      columns: [
        {
          title: this.$t('name'),
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('instancename'),
          dataIndex: 'instancename'
        },
        {
          title: this.$t('displayname'),
          dataIndex: 'displayname'
        },
        {
          title: this.$t('account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('zonenamelabel'),
          dataIndex: 'zonename'
        },
        {
          title: this.$t('state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: 'Select',
          dataIndex: 'id',
          scopedSlots: { customRender: 'radio' }
        }
      ],
      tiersSelect: false,
      networksList: [],
      vpcTiers: [],
      selectedVpcTier: null,
      page: 1,
      pageSize: 10
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      if (this.resource.vpcid) {
        this.handleTiers()
        if (this.selectedVpcTier) this.fetchDataTiers(this.selectedVpcTier)
        return
      }

      api('listVirtualMachines', {
        page: this.page,
        pageSize: this.pageSize,
        listAll: true,
        networkid: this.resource.associatednetworkid,
        account: this.resource.account,
        domainid: this.resource.domainid,
        keyword: this.searchQuery
      }).then(response => {
        this.vmsList = response.listvirtualmachinesresponse.virtualmachine
        this.loading = false
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
        this.loading = false
      })
    },
    fetchDataTiers (e) {
      this.loading = true

      api('listVirtualMachines', {
        page: this.page,
        pageSize: this.pageSize,
        listAll: true,
        networkid: e,
        account: this.resource.account,
        domainid: this.resource.domainid,
        vpcid: this.resource.vpcid,
        keyword: this.searchQuery
      }).then(response => {
        this.vmsList = response.listvirtualmachinesresponse.virtualmachine
        this.loading = false
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
        this.loading = false
      })
    },
    fetchNics (e) {
      this.selectedVm = e.target.value
      this.loading = true
      this.nicsList = []
      api('listNics', {
        virtualmachineid: this.selectedVm,
        networkid: this.resource.associatednetworkid
      }).then(response => {
        this.nicsList = response.listnicsresponse.nic

        let secondaryIps = this.nicsList.map(item => item.secondaryip)

        if (secondaryIps[0]) {
          secondaryIps = secondaryIps[0]
          this.nicsList = [...this.nicsList, ...secondaryIps]
        }

        this.selectedNic = this.nicsList[0]
        this.loading = false
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
        this.loading = false
      })
    },
    fetchNetworks () {
      this.loading = true

      api('listNetworks', {
        vpcid: this.resource.vpcid,
        domainid: this.resource.domainid,
        account: this.resource.account,
        supportedservices: 'StaticNat'
      }).then(response => {
        this.networksList = response.listnetworksresponse.network
        this.loading = false
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
        this.loading = false
      })
    },
    handleSubmit () {
      this.loading = true
      api('enableStaticNat', {
        ipaddressid: this.resource.id,
        virtualmachineid: this.selectedVm,
        vmguestip: this.selectedNic.ipaddress,
        networkid: this.selectedVpcTier
      }).then(() => {
        this.parentFetchData()
        this.loading = false
        this.handleClose()
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description'],
          duration: 0
        })
        this.loading = false
        this.handleClose()
      })
    },
    handleClose () {
      this.$parent.$parent.close()
    },
    handleTiers () {
      this.tiersSelect = true
      this.fetchNetworks()
    },
    handleTierSelect (tier) {
      this.selectedVpcTier = tier
      this.fetchDataTiers(tier)
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

<style scoped lang="scss">

  .list {
    max-height: 95vh;
    width: 95vw;
    overflow-y: scroll;
    margin: -24px;

    @media (min-width: 1000px) {
      max-height: 70vh;
      width: 60vw;
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

  .nic-select {
    margin-top: 10px;
    margin-right: auto;
    min-width: 150px;
  }

  .pagination {
    margin-top: 20px;
    padding-right: 20px;
    padding-left: 20px;
  }

  .table {
    margin-top: 20px;
    overflow-y: auto;
  }
</style>
