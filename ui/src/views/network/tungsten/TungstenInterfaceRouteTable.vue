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
        {{ $t('label.add.tungsten.interface.route') }}
      </span>
      <a-form :form="form" layout="vertical" class="form-layout">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.tungsteninterfaceroutetablename.description" />
          <a-input
            :autoFocus="true"
            v-decorator="['tungsteninterfaceroutetablename', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.tungsteninterfaceroutetablename.description" />
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
  name: 'TungstenInterfaceRouteTable',
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
        api: 'createTungstenFabricInterfaceRouteTable',
        icon: 'plus',
        label: 'label.add.tungsten.interface.route',
        dataView: false,
        listView: true
      }],
      dataSource: [],
      columns: [{
        dataIndex: 'name',
        title: this.$t('label.name'),
        scopedSlots: { customRender: 'name' }
      }, {
        dataIndex: 'tungstenvms',
        title: this.$t('label.tungstenvms'),
        scopedSlots: { customRender: 'tungstenvms' }
      }],
      showAddModal: false,
      tableAction: [{
        api: 'removeTungstenFabricInterfaceRouteTable',
        icon: 'delete',
        label: 'label.remove.interface.route.table',
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
    this.apiParams = this.$getApiParams('createTungstenFabricInterfaceRouteTable')
  },
  created () {
    this.fetchData()
  },
  provide: function () {
    return {
      onFetchData: this.fetchData,
      onExecAction: this.execAction,
      onDeleteNetworkRouteTable: this.deleteNetworkRouteTable
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
      api('listTungstenFabricInterfaceRouteTable', params).then(json => {
        this.itemCount = json?.listtungstenfabricinterfaceroutetableresponse?.count || 0
        this.dataSource = json?.listtungstenfabricinterfaceroutetableresponse?.interfaceroutetable || []
      }).finally(() => { this.fetchLoading = false })
    },
    execAction (action, record) {
      if (action.api === 'createTungstenFabricInterfaceRouteTable') {
        this.showAddModal = true
        return
      }

      const self = this
      const title = `${this.$t('label.deleteconfirm')} ${this.$t('label.interface.route.table')}`

      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.removeTungstenFabricInterfaceRouteTable(record)
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
        params.tungsteninterfaceroutetablename = values.tungsteninterfaceroutetablename
        api('createTungstenFabricInterfaceRouteTable', params).then(json => {
          this.$pollJob({
            jobId: json.createtungstenfabricinterfaceroutetableresponse.jobid,
            title: this.$t('label.add.tungsten.interface.route'),
            description: values.tungsteninterfaceroutetablename,
            successMessage: `${this.$t('label.success')}`,
            successMethod: () => {
              this.fetchData()
            },
            loadingMessage: `${this.$t('label.add.tungsten.interface.route')} ${this.$t('label.in.progress')}`,
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
    removeTungstenFabricInterfaceRouteTable (record) {
      this.fetchLoading = true
      api('removeTungstenFabricInterfaceRouteTable', {
        zoneid: this.resource.zoneid,
        tungsteninterfaceroutetableuuid: record.uuid
      }).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricinterfaceroutetableresponse.jobid,
          title: this.$t('label.remove.interface.route.table'),
          description: record.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.remove.interface.route.table')} ${this.$t('label.in.progress')}`,
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
