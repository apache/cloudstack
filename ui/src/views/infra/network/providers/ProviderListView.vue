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
    <strong>{{ $t(title) }}</strong>
    <a-table
      style="margin-top: 10px;"
      size="small"
      class="row-list-data"
      :loading="loading"
      :columns="listCols"
      :dataSource="dataSource"
      :rowKey="record => record.id || record.name || record.nvpdeviceid || record.resourceid"
      :pagination="false"
      :scroll="scrollable">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          <span v-if="record.role==='VIRTUAL_ROUTER'">
            <router-link :to="{ path: '/router' + '/' + record.id }" v-if="record.id">{{ text }}</router-link>
            <label v-else>{{ text }}</label>
          </span>
          <span v-else>{{ text }}</span>
        </template>
        <template v-if="column.key === 'hostname'">
          <span v-if="record.role==='VIRTUAL_ROUTER'">
            <router-link :to="{ path: '/host' + '/' + record.hostid }" v-if="record.hostid">{{ text }}</router-link>
            <label v-else>{{ text }}</label>
          </span>
          <span v-else>{{ text }}</span>
        </template>
        <template v-if="column.key === 'zonename'">
          <span v-if="record.role==='VIRTUAL_ROUTER'">
            <router-link :to="{ path: '/zone' + '/' + record.zoneid }" v-if="record.zoneid">{{ text }}</router-link>
            <label v-else>{{ text }}</label>
          </span>
          <span v-else>{{ text }}</span>
        </template>
        <template v-if="column.key === 'actions'">
          <a-tooltip placement="top">
            <template #title>
              <span v-if="resource.name==='BigSwitchBcf'">{{ $t('label.delete.bigswitchbcf') }}</span>
              <span v-else-if="resource.name==='BrocadeVcs'">{{ $t('label.delete.brocadevcs') }}</span>
              <span v-else-if="resource.name==='NiciraNvp'">{{ $t('label.delete.niciranvp') }}</span>
              <span v-else-if="resource.name==='F5BigIp'">{{ $t('label.delete.f5') }}</span>
              <span v-else-if="resource.name==='JuniperSRX'">{{ $t('label.delete.srx') }}</span>
              <span v-else-if="resource.name==='Netscaler'">{{ $t('label.delete.netscaler') }}</span>
              <span v-else-if="resource.name==='Opendaylight'">{{ $t('label.delete.opendaylight.device') }}</span>
              <span v-else-if="resource.name==='PaloAlto'">{{ $t('label.delete.pa') }}</span>
              <span v-else-if="resource.name==='CiscoVnmc' && title==='listCiscoVnmcResources'">
                {{ $t('label.delete.ciscovnmc.resource') }}
              </span>
              <span v-else-if="resource.name==='CiscoVnmc' && title==='listCiscoAsa1000vResources'">
                {{ $t('label.delete.ciscoasa1000v') }}
              </span>
            </template>
            <tooltip-button
              v-if="resource.name==='Ovs'"
              :tooltip="$t('label.configure')"
              icon="setting-outlined"
              size="small"
              :loading="actionLoading"
              @onClick="onConfigureOvs(record)"/>
            <tooltip-button
              v-else
              :tooltip="$t('label.delete')"
              type="primary"
              :danger="true"
              icon="close-outlined"
              size="small"
              :loading="actionLoading"
              @onClick="onDelete(record)"/>
          </a-tooltip>
        </template>
        <template v-if="column.key === 'lbdevicestate'">
          <status :text="text ? text : ''" displayText />
        </template>
        <template v-if="column.key === 'status'">
          <status :text="text ? text : ''" displayText />
        </template>
        <template v-if="column.key === 'state'">
          <status :text="text ? text : ''" displayText />
        </template>
      </template>
    </a-table>
    <a-pagination
      size="small"
      class="row-pagination"
      :current="page"
      :pageSize="pageSize"
      :total="itemCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger
      showQuickJumper>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'ProviderListView',
  components: { Status, TooltipButton },
  props: {
    title: {
      type: String,
      required: true
    },
    columns: {
      type: Array,
      required: true
    },
    dataSource: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    page: {
      type: Number,
      default: () => 1
    },
    pageSize: {
      type: Number,
      default: () => 10
    },
    itemCount: {
      type: Number,
      default: () => 0
    },
    resource: {
      type: Object,
      default: () => {}
    },
    action: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      actionLoading: false
    }
  },
  computed: {
    scrollable () {
      if (this.dataSource.length === 0) {
        return { y: '60vh', x: 'auto' }
      } else if (this.columns.length > 3) {
        return { y: '60vh', x: '50vw' }
      }

      return { y: '60vh' }
    },
    listCols () {
      const columns = []
      this.columns.forEach(col => {
        if (col.dataIndex === 'hostname' && this.resource.name === 'BigSwitchBcf') {
          col.title = this.$t('label.bigswitch.controller.address')
        }
        if (col.dataIndex === 'hostname' && this.resource.name === 'BrocadeVcs') {
          col.title = this.$t('label.brocade.vcs.address')
        }
        columns.push(col)
      })
      return columns
    }
  },
  inject: ['providerChangePage', 'provideReload'],
  methods: {
    changePage (page, pageSize) {
      this.providerChangePage(this.title, page, pageSize)
    },
    changePageSize (currentPage, pageSize) {
      this.providerChangePage(this.title, currentPage, pageSize)
    },
    onDelete (record) {
      let apiName
      let confirmation
      let label
      let name
      const params = {}
      switch (this.resource.name) {
        case 'BigSwitchBcf':
          label = 'label.delete.bigswitchbcf'
          name = record.hostname
          apiName = 'deleteBigSwitchBcfDevice'
          confirmation = 'message.confirm.delete.bigswitchbcf'
          params.bcfdeviceid = record.bcfdeviceid
          break
        case 'F5BigIp':
          label = 'label.delete.f5'
          name = record.ipaddress
          apiName = 'deleteF5LoadBalancer'
          confirmation = 'message.confirm.delete.f5'
          params.lbdeviceid = record.lbdeviceid
          break
        case 'NiciraNvp':
          label = 'label.delete.niciranvp'
          name = record.hostname
          apiName = 'deleteNiciraNvpDevice'
          confirmation = 'message.confirm.delete.niciranvp'
          params.nvpdeviceid = record.nvpdeviceid
          break
        case 'BrocadeVcs':
          label = 'label.delete.brocadevcs'
          name = record.hostname
          apiName = 'deleteBrocadeVcsDevice'
          confirmation = 'message.confirm.delete.brocadevcs'
          params.vcsdeviceid = record.vcsdeviceid
          break
        case 'Netscaler':
          label = 'label.delete.netscaler'
          name = record.ipaddress
          apiName = 'deleteNetscalerLoadBalancer'
          confirmation = 'message.confirm.delete.netscaler'
          params.lbdeviceid = record.lbdeviceid
          break
        case 'Opendaylight':
          label = 'label.delete.opendaylight.device'
          name = record.name
          apiName = 'deleteOpenDaylightController'
          confirmation = 'message.confirm.delete.Opendaylight'
          params.id = record.id
          break
        case 'PaloAlto':
          label = 'label.delete.PA'
          name = record.ipaddress
          apiName = 'deletePaloAltoFirewall'
          confirmation = 'message.confirm.delete.pa'
          params.fwdeviceid = record.fwdeviceid
          break
        case 'CiscoVnmc':
          if (this.title === 'listCiscoVnmcResources') {
            label = 'label.delete.ciscovnmc.resource'
            apiName = 'deleteCiscoVnmcResource'
            confirmation = 'message.confirm.delete.ciscovnmc.resource'
          } else {
            label = 'label.delete.ciscoasa1000v'
            apiName = 'deleteCiscoAsa1000vResource'
            confirmation = 'message.confirm.delete.ciscoasa1000v'
          }

          name = record.hostname
          params.resourceid = record.resourceid
          break
        default:
          break
      }

      this.$confirm({
        title: this.$t('label.confirmation'),
        content: this.$t(confirmation),
        onOk: async () => {
          if (apiName) {
            this.actionLoading = true
            try {
              const jobId = await this.executeDeleteRecord(apiName, params)
              if (jobId) {
                this.$pollJob({
                  jobId,
                  title: this.$t(label),
                  description: this.$t(name),
                  loadingMessage: `${this.$t(label)} - ${this.$t(name)}`,
                  catchMessage: this.$t('error.fetching.async.job.result')
                })
              } else {
                this.$success('Success')
                this.provideReload()
              }
              this.actionLoading = false
            } catch (error) {
              this.actionLoading = false
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
              })
            }
          }
        }
      })
    },
    onConfigureOvs (record) {
      const params = {}
      params.id = record.id
      params.enabled = true

      this.$confirm({
        title: this.$t('label.confirmation'),
        content: this.$t('message.confirm.configure.ovs'),
        onOk: async () => {
          this.actionLoading = true
          try {
            const jobId = await this.configureOvsElement(params)
            if (jobId) {
              this.$pollJob({
                jobId,
                title: this.$t('label.configure.ovs'),
                description: this.$t(record.id),
                loadingMessage: `${this.$t('label.configure.ovs')} - ${this.$t(record.id)}`,
                catchMessage: this.$t('error.fetching.async.job.result')
              })
            } else {
              this.$success('Success')
              this.provideReload()
            }
            this.actionLoading = false
          } catch (error) {
            this.actionLoading = false
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
            })
          }
        }
      })
    },
    executeDeleteRecord (apiName, args) {
      return new Promise((resolve, reject) => {
        let jobId = null
        api(apiName, args).then(json => {
          for (const obj in json) {
            if (obj.includes('response')) {
              for (const res in json[obj]) {
                if (res === 'jobid') {
                  jobId = json[obj][res]
                  break
                }
              }
              break
            }
          }

          resolve(jobId)
        }).catch(error => {
          reject(error)
        })
      })
    },
    configureOvsElement (args) {
      return new Promise((resolve, reject) => {
        api('configureOvsElement', args).then(json => {
          const jobId = json.configureovselementresponse.jobid
          resolve(jobId)
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>

<style scoped lang="less">
.row-pagination {
  margin-top: 10px;
  margin-bottom: 10px;
  text-align: right;
}
</style>
