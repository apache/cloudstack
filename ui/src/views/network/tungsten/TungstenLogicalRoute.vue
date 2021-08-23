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
        {{ $t(currentAction.label) }}
      </span>
      <a-form :form="form" layout="vertical" class="form-layout">
        <a-form-item v-if="currentAction.api==='createTungstenFabricLogicalRouter'">
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description" />
          <a-input
            :autoFocus="true"
            v-decorator="['name', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.name.description" />
        </a-form-item>
        <a-form-item v-else-if="['addTungstenFabricNetworkToLogicalRouter', 'removeTungstenFabricNetworkFromLogicalRouter'].includes(currentAction.api)">
          <tooltip-label slot="label" :title="$t('label.networkuuid')" :tooltip="apiParams.networkuuid.description" />
          <a-select
            :loading="networkLoading"
            v-decorator="['networkuuid', {
              rules: [{ required: true, message: $t('message.error.select') }]
            }]">
            <a-select-option
              v-for="opt in logicalNetworks"
              :key="opt.uuid">{{ $t(opt.name) }}</a-select-option>
          </a-select>
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
  name: 'TungstenLogicalRoute',
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
        api: 'createTungstenFabricLogicalRouter',
        icon: 'plus',
        label: 'label.add.tungsten.logical.route',
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
      networkLoading: false,
      logicalNetworks: [],
      currentAction: {},
      showAddModal: false,
      tableAction: [
        {
          api: 'addTungstenFabricNetworkToLogicalRouter',
          icon: 'plus',
          label: 'label.add.logical.network',
          dataView: true
        }, {
          api: 'removeTungstenFabricNetworkFromLogicalRouter',
          icon: 'close',
          label: 'label.remove.logical.network',
          dataView: true
        }, {
          api: 'deleteTungstenFabricLogicalRouter',
          icon: 'delete',
          label: 'label.remove.logical.router',
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
      api('listTungstenFabricLogicalRouter', params).then(json => {
        this.itemCount = json?.listtungstenfabriclogicalrouterresponse?.count || 0
        this.dataSource = json?.listtungstenfabriclogicalrouterresponse?.logicalrouter || []
      }).finally(() => { this.fetchLoading = false })
    },
    fetchLogicalNetworks () {
      this.logicalNetworks = []
      this.networkLoading = true
      api('listTungstenFabricNetwork', { zoneid: this.resource.zoneid }).then(json => {
        this.logicalNetworks = json?.listtungstenfabricnetworkresponse?.network || []
      }).finally(() => {
        this.networkLoading = false
      })
    },
    execAction (action, record) {
      const self = this

      if ([
        'createTungstenFabricLogicalRouter',
        'addTungstenFabricNetworkToLogicalRouter',
        'removeTungstenFabricNetworkFromLogicalRouter'
      ].includes(action.api)) {
        this.currentAction = action
        this.apiParams = this.$getApiParams(this.currentAction.api)
        if (['addTungstenFabricNetworkToLogicalRouter', 'removeTungstenFabricNetworkFromLogicalRouter'].includes(this.currentAction.api)) {
          this.fetchLogicalNetworks()
          this.currentAction.resource = record
        }
        this.showAddModal = true
      } else if (action.api === 'deleteTungstenFabricLogicalRouter') {
        const title = `${this.$t('label.deleteconfirm')} ${this.$t('label.logical.router')}`

        this.$confirm({
          title: title,
          okText: this.$t('label.ok'),
          okType: 'danger',
          cancelText: this.$t('label.cancel'),
          onOk () {
            self.deleteTungstenFabricLogicalRouter(record)
          }
        })
      }
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

        if (this.currentAction.api === 'createTungstenFabricLogicalRouter') {
          this.createTungstenFabricLogicalRouter(values)
        } else if (this.currentAction.api === 'addTungstenFabricNetworkToLogicalRouter') {
          this.addTungstenFabricNetworkToLogicalRouter(values)
        } else {
          this.removeTungstenFabricNetworkFromLogicalRouter(values)
        }
      })
    },
    createTungstenFabricLogicalRouter (args) {
      this.fetchLoading = true
      const params = {}
      params.zoneid = this.resource.zoneid
      params.name = args.name
      api('createTungstenFabricLogicalRouter', params).then(json => {
        this.$pollJob({
          jobId: json.createtungstenfabriclogicalrouterresponse.jobid,
          title: this.$t('label.add.tungsten.logical.route'),
          description: args.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.add.tungsten.logical.route')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: {
            isFetchData: false
          }
        })
      }).finally(() => {
        this.fetchLoading = false
        this.closeAction()
      })
    },
    addTungstenFabricNetworkToLogicalRouter (args) {
      this.fetchLoading = true
      const params = {}
      params.zoneid = this.resource.zoneid
      params.networkuuid = args.networkuuid
      params.logicalrouteruuid = this.currentAction.resource.uuid
      api('addTungstenFabricNetworkToLogicalRouter', params).then(json => {
        this.$pollJob({
          jobId: json.addtungstenfabricnetworktologicalrouterresponse.jobid,
          title: this.$t('label.add.logical.network'),
          description: args.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.add.logical.network')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: {
            isFetchData: false
          }
        })
      }).finally(() => {
        this.fetchLoading = false
        this.closeAction()
      })
    },
    removeTungstenFabricNetworkFromLogicalRouter (args) {
      this.fetchLoading = true
      const params = {}
      params.zoneid = this.resource.zoneid
      params.networkuuid = args.networkuuid
      params.logicalrouteruuid = this.currentAction.resource.uuid
      api('removeTungstenFabricNetworkFromLogicalRouter', params).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricnetworkfromlogicalrouterresponse.jobid,
          title: this.$t('label.remove.logical.network'),
          description: args.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.remove.logical.network')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: {
            isFetchData: false
          }
        })
      }).finally(() => {
        this.fetchLoading = false
        this.closeAction()
      })
    },
    deleteTungstenFabricLogicalRouter (record) {
      this.fetchLoading = true
      api('deleteTungstenFabricLogicalRouter', {
        zoneid: this.resource.zoneid,
        logicalrouteruuid: record.uuid
      }).then(json => {
        this.$pollJob({
          jobId: json.deletetungstenfabriclogicalrouterresponse.jobid,
          title: this.$t('label.remove.logical.router'),
          description: record.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.remove.logical.router')} ${this.$t('label.in.progress')}`,
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
