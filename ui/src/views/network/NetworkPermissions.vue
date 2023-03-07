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
        shape="round"
        style="float: right;margin-bottom: 10px; z-index: 8"
        @click="() => { showCreateForm = true }">
        <template #icon><plus-outlined /></template>
        {{ $t('label.add.network.permission') }}
      </a-button>
      <a-button
        shape="round"
        style="float: right;margin-bottom: 10px; z-index: 8"
        @click="showResetPermissionModal = true"
        :disabled="!('createNetworkPermissions' in $store.getters.apis)">
        <template #icon><minus-outlined /></template>
        {{ $t('label.action.reset.network.permissions') }}
      </a-button>
      <br />
      <br />

      <a-table
        size="small"
        style="overflow-y: auto; width: 100%;"
        :columns="columns"
        :dataSource="networkpermissions"
        :rowKey="item => item.id"
        :pagination="false" >

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-popconfirm
              :title="$t('message.confirm.remove.network.permission')"
              @confirm="removeNetworkPermission(record.accountid, record.projectid)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')" >
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.action.delete.network.permission')"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </template>
        </template>

      </a-table>
      <a-divider/>
    </a-spin>
    <a-modal
      v-if="showCreateForm"
      :visible="showCreateForm"
      :title="$t('label.add.network.permission')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showCreateForm = false }"
      centered
      width="auto">
      <CreateNetworkPermission
        :resource="resource"
        @refresh-data="fetchData"
        @close-action="showCreateForm = false" />
    </a-modal>
    <a-modal
      :visible="showResetPermissionModal"
      :title="$t('label.action.reset.network.permissions')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="showResetPermissionModal = false"
      centered
      width="auto">
      {{ $t('message.confirm.reset.network.permissions') }}
      <div :span="24" class="action-button">
        <a-button @click="showResetPermissionModal = false">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="resetNetworkPermission">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>
<script>
import { api } from '@/api'
import CreateNetworkPermission from '@/views/network/CreateNetworkPermission'
import TooltipButton from '@/components/widgets/TooltipButton'
export default {
  name: 'NetworkPermissions',
  components: {
    CreateNetworkPermission,
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
      showResetPermissionModal: false,
      fetchLoading: false,
      showCreateForm: false,
      total: 0,
      networkpermissions: [],
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.domain'),
          dataIndex: 'domain'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.project'),
          dataIndex: 'project'
        },
        {
          key: 'actions',
          title: ''
        }
      ]
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
  methods: {
    fetchData () {
      const params = {
        networkid: this.resource.id
      }
      this.fetchLoading = true
      api('listNetworkPermissions', params).then(json => {
        this.total = json.listnetworkpermissionsresponse.count || 0
        this.networkpermissions = json.listnetworkpermissionsresponse.networkpermission || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    removeNetworkPermission (accountId, projectId) {
      const params = {
        networkid: this.resource.id
      }
      if (projectId) {
        params.projectids = projectId
      } else {
        params.accountids = accountId
      }
      api('removeNetworkPermissions', params).then(json => {
        this.$notification.success({
          message: this.$t('message.success.remove.network.permissions')
        })
      }).finally(() => {
        this.fetchData()
      })
    },
    resetNetworkPermission () {
      api('resetNetworkPermissions', {
        networkid: this.resource.id
      }).then(json => {
        this.$notification.success({
          message: this.$t('message.success.reset.network.permissions')
        })
      }).finally(() => {
        this.fetchData()
        this.showResetPermissionModal = false
      })
    }
  }
}
</script>
