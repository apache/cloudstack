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
      type="primary"
      style="width: 100%; margin-bottom: 10px"
      @click="showAddCustomActionModal"
      :loading="tabLoading"
      :disabled="!('addCustomAction' in $store.getters.apis)">
      <template #icon><plus-outlined /></template> {{ $t('label.add.custom.action') }}
    </a-button>
    <a-table
      size="small"
      :columns="columns"
      :dataSource="extensionCustomActions"
      :rowKey="item => item.id"
      :pagination="false"
      :rowExpandable="(record) => record.parameters && Object.keys(record.parameters).length > 0">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          <router-link :to="{ path: '/customaction/' + record.id }">
            {{ text }}
          </router-link>
        </template>
        <template v-if="['enabled'].includes(column.key)">
          <status :text="text ? 'Enabled' : 'Disabled'" displayText />
        </template>
        <template v-if="['created'].includes(column.key)">
          {{ text && $toLocaleDate(text) }}
        </template>
        <template v-if="column.key === 'actions'">
          <span style="margin-right: 5px">
            <tooltip-button
              v-if="'updateCustomAction' in $store.getters.apis"
              :tooltip="$t('label.update.custom.action')"
              icon="edit-outlined"
              @click="openUpdateCustomActionModal(record)" />
          </span>
          <span style="margin-right: 5px">
            <a-popconfirm
              v-if="'updateCustomAction' in $store.getters.apis"
              placement="topRight"
              :title="record.enabled ? $t('message.confirm.disable.custom.action') : $t('message.confirm.enable.custom.action')"
              :ok-text="$t('label.yes')"
              :cancel-text="$t('label.no')"
              :loading="updateLoading"
              @confirm="updateCustomAction(record)"
            >
              <tooltip-button
                :tooltip="record.enabled ? $t('label.disable.custom.action') : $t('label.enable.custom.action')"
                :icon="record.enabled ? 'pause-circle-outlined' : 'play-circle-outlined'" />
            </a-popconfirm>
          </span>
          <span style="margin-right: 5px">
            <a-popconfirm
              v-if="'deleteCustomAction' in $store.getters.apis"
              placement="topRight"
              :title="$t('message.action.delete.custom.action')"
              :ok-text="$t('label.yes')"
              :cancel-text="$t('label.no')"
              :loading="deleteLoading"
              @confirm="deleteCustomAction(record)"
            >
              <tooltip-button
                :tooltip="$t('label.delete.custom.action')"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </span>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <strong>{{ $t('label.parameters') }}</strong>
        <object-list-table
          :showHeader="true"
          :data-array="record.parameters" />
      </template>
    </a-table>

    <a-modal
      style="min-width: 1000px;"
      :visible="showAddCustomAction"
      :title="$t('label.add.custom.action')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeModals">
      <add-custom-action :extension="resource" @close-action="closeModals" />
    </a-modal>

    <a-modal
      style="min-width: 1000px;"
      :visible="showUpdateCustomAction"
      :title="$t('label.update.custom.action')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeModals">
      <update-custom-action
      :key="customActionForUpdate.id"
      :resource="customActionForUpdate"
      @close-action="closeModals" />
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import ObjectListTable from '@/components/view/ObjectListTable.vue'
import TooltipButton from '@/components/widgets/TooltipButton'
import AddCustomAction from '@/views/extension/AddCustomAction.vue'
import UpdateCustomAction from '@/views/extension/UpdateCustomAction.vue'
import Status from '@/components/widgets/Status'

export default {
  name: 'ExtensionCustomActionsTab',
  components: {
    ObjectListTable,
    TooltipButton,
    AddCustomAction,
    UpdateCustomAction,
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      columns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          key: 'enabled',
          title: this.$t('label.state'),
          dataIndex: 'enabled'
        },
        {
          title: this.$t('label.resourcetype'),
          dataIndex: 'resourcetype'
        },
        {
          key: 'created',
          title: this.$t('label.created'),
          dataIndex: 'created'
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ],
      page: 1,
      pageSize: 20,
      totalCount: 0,
      showAddCustomAction: false,
      updateLoading: false,
      deleteLoading: false,
      showUpdateCustomAction: false,
      customActionForUpdate: {}
    }
  },
  computed: {
    extensionResources () {
      return this.resource?.resources || []
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchCustomActions()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchCustomActions()
    },
    fetchCustomActions () {
      this.extensionCustomActions = []
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        extensionid: this.resource.id,
        listall: true
      }
      this.tabLoading = true
      getAPI('listCustomActions', params).then(json => {
        this.extensionCustomActions = []
        this.totalCount = json?.listcustomactionsresponse?.count || 0
        this.extensionCustomActions = json?.listcustomactionsresponse?.extensioncustomaction || []
        this.tabLoading = false
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    showAddCustomActionModal () {
      this.showAddCustomAction = true
    },
    openUpdateCustomActionModal (action) {
      this.customActionForUpdate = action
      this.showUpdateCustomAction = true
    },
    closeModals () {
      this.showAddCustomAction = false
      this.showUpdateCustomAction = false
      this.customActionForUpdate = {}
    },
    updateCustomAction (record) {
      const params = {
        id: record.id,
        enabled: !record.enabled
      }
      this.updateLoading = true
      postAPI('updateCustomAction', params).then(json => {
        this.fetchCustomActions()
        this.$notification.success({
          message: record.enabled ? this.$t('label.disable.custom.action') : this.$t('label.enable.custom.action'),
          description: this.$t('message.success.update.custom.action')
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.updateLoading = false
      })
    },
    deleteCustomAction (record) {
      const params = {
        id: record.id
      }
      this.deleteLoading = true
      postAPI('deleteCustomAction', params).then(json => {
        this.fetchCustomActions()
        this.$notification.success({
          message: this.$t('label.delete.custom.action'),
          description: this.$t('message.success.delete.custom.action')
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.deleteLoading = false
      })
    }
  }
}
</script>
