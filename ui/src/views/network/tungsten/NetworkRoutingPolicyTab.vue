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
    <a-button
      :disabled="!('addTungstenFabricRoutingPolicyToNetwork' in $store.getters.apis)"
      type="dashed"
      icon="plus"
      style="width: 100%; margin-bottom: 15px"
      @click="onShowAction">
      {{ $t('label.add.tungsten.routing.policy') }}
    </a-button>
    <a-table
      size="small"
      :columns="columns"
      :loading="loading || fetchLoading"
      :dataSource="dataSource"
      :rowKey="item => item.uuid"
      :pagination="false">
      <template slot="action" slot-scope="text, record">
        <a-popconfirm
          v-if="'removeTungstenFabricRoutingPolicyFromNetwork' in $store.getters.apis"
          placement="topRight"
          :title="$t('message.action.remove.routing.policy')"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.no')"
          :loading="deleteLoading"
          @confirm="deleteLogicalRouter(record)"
        >
          <tooltip-button
            :tooltip="$t('label.action.remove.tungsten.routing.policy')"
            type="danger"
            icon="delete" />
        </a-popconfirm>
      </template>
    </a-table>

    <div style="display: block; text-align: right; margin-top: 10px;">
      <a-pagination
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="pageSizeOptions"
        @change="onChangePage"
        @showSizeChange="onChangePageSize"
        showSizeChanger>
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>

    <a-modal
      v-if="showAction"
      :visible="showAction"
      :title="$t('label.add.tungsten.routing.policy')"
      :maskClosable="false"
      :footer="null"
      @cancel="showAction = false"
      v-ctrl-enter="handleSubmit">
      <a-form :form="form" layout="vertical">
        <a-alert type="warning">
          <span slot="message" v-html="$t('message.confirm.add.routing.policy')" />
        </a-alert>
        <a-form-item :label="$t('label.network.routing.policy')">
          <a-select
            :loading="networks.loading"
            v-decorator="['tungstenRoutingPolicy', {
              initialValue: networkSelected,
              rules: [{ required: true, message: $t('message.error.select') }]
            }]">
            <a-select-option v-for="network in networks.opts" :key="network.uuid">{{ network.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <div :span="24" class="action-button">
        <a-button @click="() => { showAction = false }">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'NetworkRoutingPolicyTab',
  components: { TooltipButton },
  mixins: [mixinDevice],
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
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      itemCount: 0,
      dataSource: [],
      networks: {
        loading: false,
        opts: []
      },
      fetchLoading: false,
      deleteLoading: false,
      submitLoading: false,
      showAction: false,
      networkSelected: undefined,
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'name',
        scopedSlots: { customRender: 'name' }
      }, {
        title: this.$t('label.action'),
        dataIndex: 'action',
        scopedSlots: { customRender: 'action' },
        width: 80
      }]
    }
  },
  computed: {
    pageSizeOptions () {
      const sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
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
  methods: {
    fetchData () {
      if (!this.resource.id || !this.resource.zoneid) return
      const params = {}
      params.zoneid = this.resource.zoneid
      params.tungstennetworkuuid = this.resource.id
      params.isattachedtonetwork = false
      params.listAll = true
      params.page = this.page
      params.pagesize = this.pageSize

      this.fetchLoading = true
      api('listTungstenFabricRoutingPolicy', params).then(json => {
        this.dataSource = json?.listtungstenfabricroutingpolicyresponse?.routetable || []
        this.itemCount = json?.listtungstenfabricroutingpolicyresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.fetchLoading = false })
    },
    onShowAction () {
      const params = {}
      params.zoneid = this.resource.zoneid
      params.tungstennetworkuuid = this.resource.id
      params.isattachedtonetwork = false

      this.showAction = true
      this.networks.loading = true
      this.networks.opts = []
      api('listTungstenFabricRoutingPolicy', params).then(json => {
        this.networks.opts = json?.listtungstenfabricroutingpolicyresponse?.routetable || []
        this.networkSelected = this.networks.opts[0]?.uuid || undefined
      }).finally(() => {
        this.networks.loading = false
      })
    },
    handleSubmit () {
      if (this.submitLoading) return
      this.submitLoading = true
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }

        const params = {}
        params.zoneid = this.resource.zoneid
        params.networkuuid = this.resource.id
        params.tungstenroutingpolicyuuid = values.tungstenRoutingPolicy

        const network = this.networks.opts.filter(network => network.uuid === values.tungstenRoutingPolicy)
        const resourceName = network[0]?.name || values.tungstenRoutingPolicy

        api('addTungstenFabricRoutingPolicyToNetwork', params).then(json => {
          this.$pollJob({
            jobId: json.addtungstenfabricroutetabletonetworkresponse.jobid,
            title: this.$t('label.add.tungsten.routing.policy'),
            description: resourceName,
            successMessage: `${this.$t('message.success.add.tungsten.routing.policy')} ${resourceName}`,
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.error.add.tungsten.routing.policy'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
            },
            action: {
              isFetchData: false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.showAction = false
          this.submitLoading = false
        })
      })
    },
    deleteLogicalRouter (record) {
      const params = {}
      params.zoneid = this.resource.zoneid
      params.networkuuid = this.resource.id
      params.tungstennetworkroutetableuuid = record.uuid

      this.deleteLoading = true
      api('removeTungstenFabricRoutingPolicyFromNetwork', params).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricroutingpolicyfromnetworkresponse.jobid,
          title: this.$t('label.action.remove.tungsten.routing.policy'),
          description: record.name || record.uuid,
          successMessage: `${this.$t('message.success.remove.tungsten.routing.policy')} ${record.name || record.uuid}`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.error.remove.tungsten.routing.policy'),
          errorMethod: () => {
            this.fetchData()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
          },
          action: {
            isFetchData: false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.deleteLoading = false })
    },
    onChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    onChangePageSize (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style scoped>
</style>
