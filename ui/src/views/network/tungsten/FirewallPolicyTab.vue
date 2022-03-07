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
    <a-spin :spinning="loading">
      <a-button
        :disabled="!('createTungstenFabricFirewallPolicy' in $store.getters.apis)"
        type="dashed"
        icon="plus"
        style="width: 100%; margin-bottom: 15px"
        @click="addFirewallPolicy">
        {{ $t('label.add.tungsten.firewall.policy') }}
      </a-button>
      <a-table
        size="small"
        style="overflow-y: auto"
        :loading="fetchLoading"
        :columns="columns"
        :dataSource="dataSource"
        :rowKey="item => item.uuid"
        :pagination="false">
        <template slot="name" slot-scope="text, record">
          <router-link :to="{ path: '/firewallrule/' + record.uuid, query: { zoneid: zoneId } }">{{ text }}</router-link>
        </template>
        <template slot="firewallrule" slot-scope="text, record">
          <span v-if="record.firewallrule.length > 0">{{ record.firewallrule[0].name }}</span>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-popconfirm
            :title="$t('label.confirm.delete.tungsten.firewall.policy')"
            @confirm="removeFirewallRule(record.uuid)"
            :okText="$t('label.yes')"
            :cancelText="$t('label.no')">
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.delete.tungsten.firewall.policy')"
              type="danger"
              icon="delete"
              :loading="deleteLoading" />
          </a-popconfirm>
        </template>
      </a-table>
      <div style="display: block; text-align: right; margin-top: 10px;">
        <a-pagination
          class="row-element pagination"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="totalCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template slot="buildOptionText" slot-scope="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </div>
    </a-spin>

    <a-modal
      v-if="firewallPolicyModal"
      :visible="firewallPolicyModal"
      :title="$t('label.add.tungsten.firewall.policy')"
      :closable="true"
      :footer="null"
      @cancel="closeAction"
      v-ctrl-enter="submitFirewallPolicy"
      centered
      width="450px">
      <a-form :form="form">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            :auto-focus="true"
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.sequence')" :tooltip="apiParams.sequence.description"/>
          <a-input
            v-decorator="['sequence', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.sequence.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button ref="submit" type="primary" @click="submitFirewallPolicy" :loading="addLoading">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'FirewallPolicyTab',
  components: {
    TooltipLabel,
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
      zoneId: undefined,
      fetchLoading: false,
      firewallPolicyModal: false,
      addLoading: false,
      deleteLoading: false,
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'name',
        scopedSlots: { customRender: 'name' }
      }, {
        title: this.$t('label.firewallrule'),
        dataIndex: 'firewallrule',
        scopedSlots: { customRender: 'firewallrule' }
      }, {
        title: this.$t('label.action'),
        scopedSlots: { customRender: 'action' },
        width: 80
      }],
      dataSource: [],
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      totalCount: 0
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createTungstenFabricFirewallRule')
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      if (!this.resource.uuid || !('zoneid' in this.$route.query)) {
        return
      }
      this.zoneId = this.$route.query.zoneid
      this.fetchLoading = true
      this.dataSource = []
      this.totalCount = 0
      api('listTungstenFabricFirewallPolicy', {
        zoneid: this.zoneId,
        applicationpolicysetuuid: this.resource.uuid,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.dataSource = json?.listtungstenfabricfirewallpolicyresponse?.firewallpolicy || []
        this.totalCount = json?.listtungstenfabricfirewallpolicyresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    addFirewallPolicy () {
      this.firewallPolicyModal = true
    },
    submitFirewallPolicy () {
      if (this.addLoading) return
      this.addLoading = true
      this.form.validateFieldsAndScroll((err, values) => {
        if (err) return
        const params = {}
        params.applicationpolicysetuuid = this.resource.uuid
        params.zoneid = this.zoneId
        params.name = values.name
        params.sequence = values.sequence

        api('createTungstenFabricFirewallPolicy', params).then(json => {
          this.$pollJob({
            jobId: json.createtungstenfabricfirewallpolicyresponse.jobid,
            title: this.$t('label.add.tungsten.firewall.policy'),
            description: params.name,
            successMethod: () => {
              this.fetchData()
              this.addLoading = false
              this.firewallPolicyModal = false
            },
            errorMethod: () => {
              this.fetchData()
              this.addLoading = false
            },
            loadingMessage: this.$t('message.adding.firewall.policy'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
              this.addLoading = false
            },
            action: {
              isFetchData: false
            }
          })
        })
      })
    },
    removeFirewallRule (uuid) {
      if (this.deleteLoading) return
      this.deleteLoading = true
      const params = {}
      params.zoneid = this.zoneId
      params.firewallpolicyuuid = uuid

      api('deleteTungstenFabricFirewallPolicy', params).then(json => {
        this.$pollJob({
          jobId: json.deletetungstenfabricfirewallpolicyresponse.jobid,
          title: this.$t('label.delete.tungsten.firewall.policy'),
          description: params.firewallpolicyuuid,
          successMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          errorMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          loadingMessage: this.$t('message.deleting.firewall.policy'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          action: {
            isFetchData: false
          }
        })
      })
    },
    closeAction () {
      this.firewallPolicyModal = false
      this.form.resetFields()
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
