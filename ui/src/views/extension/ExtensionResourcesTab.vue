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
    <a-table
      size="small"
      :columns="columns"
      :dataSource="extensionResources"
      :rowKey="item => item.id"
      :pagination="false"
      :rowExpandable="(record) => record.details && Object.keys(record.details).length > 0">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          <router-link :to="{ path: '/' + record.type.toLowerCase() + '/' + record.id }">
            {{ text }}
          </router-link>
        </template>
        <template v-if="['created'].includes(column.key)">
          {{ text && $toLocaleDate(text) }}
        </template>
        <template v-if="column.key === 'actions'">
          <span style="margin-right: 5px">
            <tooltip-button
              :tooltip="$t('label.action.update.extension.resource')"
              type="default"
              icon="edit-outlined"
              @onClick="openUpdateModal(record)" />
          </span>
          <span style="margin-right: 5px">
            <a-popconfirm
              v-if="'unregisterExtension' in $store.getters.apis"
              placement="topRight"
              :title="$t('message.action.unregister.extension.resource')"
              :ok-text="$t('label.yes')"
              :cancel-text="$t('label.no')"
              :loading="unregisterLoading"
              @confirm="unregisterExtension(record)"
            >
              <tooltip-button
                :tooltip="$t('label.action.unregister.extension.resource')"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </span>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <strong>{{ $t('label.details') }}</strong>
        <object-list-table
          :data-map="record.details" />
      </template>
    </a-table>
    <a-modal
      v-if="updateModalVisible"
      :visible="updateModalVisible"
      :width="600"
      :title="$t('label.action.update.extension.resource')"
      :closable="true"
      :footer="null"
      @cancel="closeUpdateModal">
      <update-registered-extension
        :resource="resource"
        :extension-resource="selectedResource"
        @refresh-data="handleRefreshData"
        @close-action="closeUpdateModal" />
    </a-modal>
  </div>
</template>

<script>
import { postAPI } from '@/api'
import eventBus from '@/config/eventBus'
import ObjectListTable from '@/components/view/ObjectListTable.vue'
import TooltipButton from '@/components/widgets/TooltipButton'
import UpdateRegisteredExtension from '@/views/extension/UpdateRegisteredExtension'

export default {
  name: 'ExtensionResourcesTab',
  components: {
    ObjectListTable,
    TooltipButton,
    UpdateRegisteredExtension
  },
  inject: ['parentFetchData'],
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
          title: this.$t('label.type'),
          dataIndex: 'type'
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
      unregisterLoading: false,
      updateModalVisible: false,
      selectedResource: null
    }
  },
  computed: {
    extensionResources () {
      return this.resource?.resources || []
    }
  },
  methods: {
    openUpdateModal (record) {
      this.selectedResource = record
      this.updateModalVisible = true
    },
    closeUpdateModal () {
      this.updateModalVisible = false
      this.selectedResource = null
    },
    handleRefreshData () {
      if (this.parentFetchData) {
        this.parentFetchData()
      }
    },
    unregisterExtension (record) {
      const params = {
        extensionid: this.resource.id,
        resourceid: record.id,
        resourcetype: record.type
      }
      this.unregisterLoading = true
      postAPI('unregisterExtension', params).then(() => {
        eventBus.emit('async-job-complete', null)
        this.$notification.success({
          message: this.$t('label.unregister.extension'),
          description: this.$t('message.success.unregister.extension')
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.unregisterLoading = false
      })
    }
  }
}
</script>
