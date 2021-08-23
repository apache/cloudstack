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
    <tungsten-network-action
      :actions="actions"
      :page="page"
      :pageSize="pageSize" />
    <tungsten-network-table
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :searchQuery="searchQuery"
      :page="page"
      :page-size="pageSize"
      :item-count="itemCount"
      :actions="tableAction"/>
    <a-modal
      :closable="true"
      :maskClosable="false"
      style="top: 20px;"
      :visible="showAddModal"
      :confirm-loading="fetchLoading"
      :footer="null"
      centered
      width="auto"
      @cancel="closeAction">
      <span slot="title">
        {{ $t('label.add.tungsten.fabric.route') }}
      </span>
      <a-form :form="form" layout="vertical" class="form-layout">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.tungstennetworkroutetablename.description" />
          <a-input
            :autoFocus="true"
            v-decorator="['tungstennetworkroutetablename', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.tungstennetworkroutetablename.description" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import TungstenNetworkAction from '@/views/network/tungsten/TungstenNetworkAction'
import TungstenNetworkTable from '@/views/network/tungsten/TungstenNetworkTable'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'TungstenNetworkRouteTable',
  components: {
    TungstenNetworkAction,
    TungstenNetworkTable,
    TooltipLabel
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
      page: 1,
      pageSize: 20,
      itemCount: 0,
      searchQuery: '',
      actions: [{
        api: 'createTungstenFabricNetworkRouteTable',
        icon: 'plus',
        label: 'label.add.tungsten.fabric.route',
        dataView: false,
        listView: true
      }],
      dataSource: [],
      columns: [{
        dataIndex: 'name',
        title: this.$t('label.name'),
        scopedSlots: { customRender: 'name' }
      }, {
        dataIndex: 'network',
        title: this.$t('label.network'),
        scopedSlots: { customRender: 'network' }
      }],
      showAddModal: false,
      tableAction: [{
        api: 'removeTungstenFabricNetworkRouteTable',
        icon: 'delete',
        label: 'label.remove.network.route.table',
        dataView: true
      }]
    }
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createTungstenFabricNetworkRouteTable')
  },
  created () {
    this.fetchData()
  },
  provide: function () {
    return {
      onFetchData: this.fetchData,
      onExecAction: this.execAction
    }
  },
  methods: {
    fetchData (args = {}) {
      this.itemCount = 0
      this.dataSource = []
      if (this.resource === {} || !this.resource.zoneid) {
        return false
      }
      if (Object.keys(args).length > 0) {
        this.page = args.page || 1
        this.pageSize = args.pageSize || 20
        this.searchQuery = args.searchQuery || ''
      }
      this.fetchLoading = true
      const params = {}
      params.zoneid = this.resource.zoneid
      params.listAll = true
      params.page = this.page
      params.pagesize = this.pageSize
      params.query = this.searchQuery
      api('listTungstenFabricNetworkRouteTable', params).then(json => {
        this.itemCount = json?.listtungstenfabricnetworkroutetableresponse?.count || 0
        this.dataSource = json?.listtungstenfabricnetworkroutetableresponse?.routetable || []
      }).finally(() => { this.fetchLoading = false })
    },
    execAction (action, record) {
      if (action.api === 'createTungstenFabricNetworkRouteTable') {
        this.showAddModal = true
        return
      }

      const self = this
      const title = `${this.$t('label.deleteconfirm')} ${this.$t('label.network.route.table')}`

      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.removeTungstenFabricNetworkRouteTable(record)
        }
      })
    },
    closeAction () {
      this.form.resetFields()
      this.showAddModal = false
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((error, values) => {
        if (error) {
          return false
        }

        this.fetchLoading = true
        const params = {}
        params.zoneid = this.resource.zoneid
        params.tungstennetworkroutetablename = values.tungstennetworkroutetablename
        api('createTungstenFabricNetworkRouteTable', params).then(json => {
          this.$pollJob({
            jobId: json.createtungstenfabricnetworkroutetableresponse.jobid,
            title: this.$t('label.add.tungsten.fabric.route'),
            description: values.tungstennetworkroutetablename,
            successMessage: `${this.$t('label.success')}`,
            successMethod: () => {
              this.fetchData()
            },
            loadingMessage: `${this.$t('label.add.tungsten.fabric.route')} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            action: {
              isFetchData: false
            }
          })
        }).finally(() => {
          this.fetchLoading = false
          this.closeAction()
        })
      })
    },
    removeTungstenFabricNetworkRouteTable (record) {
      this.fetchLoading = true
      api('removeTungstenFabricNetworkRouteTable', {
        zoneid: this.resource.zoneid,
        tungstennetworkroutetableuuid: record.uuid
      }).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricnetworkroutetableresponse.jobid,
          title: this.$t('label.remove.network.route.table'),
          description: record.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.remove.network.route.table')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: {
            isFetchData: false
          }
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;

  @media (min-width: 600px) {
    width: 450px;
  }
}
</style>
