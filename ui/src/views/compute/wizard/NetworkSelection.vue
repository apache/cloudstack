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
    <a-input-search
      style="width: 25vw; float: right; margin-bottom: 10px; z-index: 8"
      :placeholder="$t('label.search')"
      v-model:value="filter"
      @search="handleSearch" />
    <a-button type="primary" @click="onCreateNetworkClick" style="float: right; margin-right: 5px; z-index: 8" v-if="showCreateButton">
      {{ $t('label.create.network') }}
    </a-button>
    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="networkItems"
      :rowKey="record => record.id"
      :pagination="false"
      :rowSelection="rowSelection"
      :scroll="{ y: 225 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <resource-icon
            v-if="record.icon"
            :image="record.icon.base64image"
            size="1x"
            style="margin-right: 5px"/>
          <apartment-outlined v-else style="margin-right: 5px" />
          {{ record.name }}
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <a-list
          :key="record.id"
          :dataSource="getDetails(record)"
          size="small"
        >
          <template #renderItem="{ item }">
            <a-list-item :key="item.id">
              <a-list-item-meta
                :description="item.description"
              >
                <template #title>{{ item.title }}</template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </template>
    </a-table>

    <div style="display: block; text-align: right;">
      <a-pagination
        size="small"
        :current="options.page"
        :pageSize="options.pageSize"
        :total="rowCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100', '200']"
        @change="onChangePage"
        @showSizeChange="onChangePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>

    <a-modal
      :visible="showCreateForm"
      :title="$t('label.add.network')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="showCreateForm = false"
      centered
      width="auto">
      <create-network
        :resource="{ zoneid: zoneId }"
        @refresh-data="handleSearch"
        @close-action="showCreateForm = false"
      />
    </a-modal>
  </div>
</template>

<script>
import _ from 'lodash'
import { api } from '@/api'
import { isAdmin } from '@/role'
import store from '@/store'
import CreateNetwork from '@/views/network/CreateNetwork'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'NetworkSelection',
  components: {
    CreateNetwork,
    ResourceIcon
  },
  props: {
    items: {
      type: Array,
      default: () => []
    },
    rowCount: {
      type: Number,
      default: () => 0
    },
    value: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    zoneId: {
      type: String,
      default: () => ''
    },
    autoscale: {
      type: Boolean,
      default: () => false
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      filter: '',
      selectedRowKeys: [],
      vpcs: [],
      filteredInfo: null,
      networkOffering: {
        loading: false,
        opts: []
      },
      showCreateButton: false,
      showCreateForm: false,
      oldZoneId: null,
      options: {
        page: 1,
        pageSize: 10,
        keyword: null
      },
      networksBeforeCreate: null
    }
  },
  computed: {
    columns () {
      let vpcFilter = []
      if (this.vpcs) {
        vpcFilter = this.vpcs.map((vpc) => {
          return {
            text: vpc.displaytext,
            value: vpc.id
          }
        })
      }
      const vpcCol = {
        key: 'vpcName',
        dataIndex: 'vpcName',
        title: this.$t('label.vpc'),
        width: '30%'
      }
      if (vpcFilter.length > 0) {
        vpcCol.filters = vpcFilter
        vpcCol.filteredValue = _.get(this.filteredInfo, 'id')
        vpcCol.onFilter = (value, record) => {
          return record.vpcid === value
        }
      }
      return [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.networks'),
          width: '40%'
        },
        {
          key: 'type',
          dataIndex: 'type',
          title: this.$t('label.guestiptype'),
          width: '15%'
        },
        vpcCol,
        {
          key: 'supportsvmautoscaling',
          dataIndex: 'supportsvmautoscaling',
          title: this.$t('label.supportsvmautoscaling'),
          width: '25%'
        }
      ]
    },
    rowSelection () {
      if (this.autoscale) {
        return {
          type: 'radio',
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectRow
        }
      } else {
        return {
          type: 'checkbox',
          selectedRowKeys: this.selectedRowKeys,
          onChange: (rows) => {
            this.$emit('select-network-item', rows)
          }
        }
      }
    },
    networkItems () {
      return this.items.map((network) => {
        const vpc = _.find(this.vpcs, { id: network.vpcid })
        return {
          ...network,
          ...{
            supportsvmautoscaling: network.supportsvmautoscaling ? 'Yes' : 'No',
            vpcName: _.get(vpc, 'displaytext')
          }
        }
      })
    }
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && !_.isEqual(newValue, oldValue)) {
        this.selectedRowKeys = newValue
      }
    },
    loading () {
      api('listZones', { id: this.zoneId }).then(json => {
        const zoneResponse = json.listzonesresponse.zone || []
        this.showCreateButton = false
        if (zoneResponse && zoneResponse.length > 0 && (!zoneResponse[0].securitygroupsenabled || (isAdmin() && zoneResponse[0].networktype === 'Advanced'))) {
          this.showCreateButton = true
        }
      })
      if (!this.loading) {
        if (this.preFillContent.networkids) {
          this.selectedRowKeys = this.preFillContent.networkids
          this.$emit('select-network-item', this.preFillContent.networkids)
        } else {
          if (this.items && this.items.length > 0) {
            if (this.oldZoneId === this.zoneId) {
              return
            }
            this.oldZoneId = this.zoneId
            this.selectedRowKeys = [this.items[0].id]
            this.$emit('select-network-item', this.selectedRowKeys)
          } else {
            this.selectedRowKeys = []
            this.$emit('select-network-item', [])
          }
        }
      }
    },
    items: {
      deep: true,
      handler () {
        if (this.items && this.items.length > 0 &&
          this.networksBeforeCreate) {
          var user = this.$store.getters.userInfo
          for (var network of this.items) {
            if (user.account !== network.account ||
              user.domainid !== network.domainid ||
              (new Date()).getTime() - Date.parse(network.created) > 30000) {
              continue
            }
            var networkFoundInNewList = false
            for (var oldNetwork of this.networksBeforeCreate) {
              if (oldNetwork.id === network.id) {
                networkFoundInNewList = true
                break
              }
            }
            if (!networkFoundInNewList) {
              this.selectedRowKeys.push(network.id)
              this.$emit('select-network-item', this.selectedRowKeys)
              break
            }
          }
          this.networksBeforeCreate = null
        }
      }
    }
  },
  created () {
    this.vpcs = []
    const projectId = store?.getters?.project?.id || null
    var params = {}
    if (projectId) {
      params.projectid = projectId
    }
    api('listVPCs', {
      params
    }).then((response) => {
      this.vpcs = _.get(response, 'listvpcsresponse.vpc')
    })
  },
  inject: ['vmFetchNetworks'],
  methods: {
    fetchVPCs () {
      const projectId = store?.getters?.project?.id || null
      if (!projectId) {
        return false
      }
      api('listVPCs', {
        projectid: store.getters.project.id
      }).then((response) => {
        this.vpcs = _.get(response, 'listvpcsresponse.vpc')
      })
    },
    getDetails (network) {
      const detail = [
        {
          title: this.$t('label.description'),
          description: network.displaytext
        },
        {
          title: this.$t('label.networkofferingid'),
          description: network.networkofferingdisplaytext
        }
      ]
      if (network.type !== 'L2') {
        detail.push({
          title: this.$t('label.cidr'),
          description: network.cidr
        })
      }
      return detail
    },
    handleSearch (value) {
      this.filter = value
      this.options.page = 1
      this.options.pageSize = 10
      this.options.keyword = this.filter
      this.$emit('handle-search-filter', this.options)
    },
    onChangePage (page, pageSize) {
      this.options.page = page
      this.options.pageSize = pageSize
      this.$emit('handle-search-filter', this.options)
    },
    onChangePageSize (page, pageSize) {
      this.options.page = page
      this.options.pageSize = pageSize
      this.$emit('handle-search-filter', this.options)
    },
    onSelectRow (value) {
      this.selectedRowKeys = value
      this.$emit('select-network-item', value[0])
    },
    listNetworkOfferings () {
      return new Promise((resolve, reject) => {
        const args = {}
        args.forvpc = false
        args.zoneid = this.zoneId
        args.guestiptype = 'Isolated'
        args.supportedServices = 'SourceNat'
        args.specifyvlan = false
        args.state = 'Enabled'

        api('listNetworkOfferings', args).then(json => {
          const listNetworkOfferings = json.listnetworkofferingsresponse.networkoffering || []
          resolve(listNetworkOfferings)
        }).catch(error => {
          resolve(error)
        })
      })
    },
    onCreateNetworkClick () {
      this.networksBeforeCreate = this.items
      this.showCreateForm = true
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
