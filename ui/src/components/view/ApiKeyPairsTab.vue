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
        v-if="'registerUserKeys' in $store.getters.apis"
        type="dashed"
        style="width: 100%; margin-bottom: 15px"
        @click="onShowAddKeyPair()">
        <template #icon><plus-outlined /></template>
        {{ $t('label.register.api.key') }}
      </a-button>
      <a-button
        v-if="this.selectedRowKeys.length > 0 && ('deleteUserKeys' in $store.getters.apis)"
        type="primary"
        danger
        style="width: 100%; margin-bottom: 15px"
        @click="bulkActionConfirmation()">
        <template #icon><delete-outlined /></template>
        {{ $t('label.action.bulk.delete.api.keys') }}
      </a-button>
      <a-table
        size="small"
        style="overflow-y: auto"
        :columns="columns"
        :dataSource="keypairs"
        :rowKey="item => item.id"
        :rowSelection="rowSelection()"
        :pagination="false" >
        <template #name="{ record }">
          <div>
            <router-link :to="{ path: '/keypair/' + record.id }" >
              {{ record.name }}
            </router-link>
          </div>
        </template>
        <template #apikey="{ record }">
          <strong>
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy')"
              icon="CopyOutlined"
              type="dashed"
              size="small"
              @onClick="$message.success($t('label.copied.clipboard'))"
              :copyResource="record.apikey" />
          </strong>
          <div>
            {{ record.apikey.substring(0, 20) }}...
          </div>
        </template>

        <template #secretkey="{ record }">
          <strong>
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy')"
              icon="CopyOutlined"
              type="dashed"
              size="small"
              @onClick="$message.success($t('label.copied.clipboard'))"
              :copyResource="record.secretkey" />
          </strong>
          <div>
            {{ record.secretkey.substring(0, 20) }}...
          </div>
        </template>

        <template #startdate="{ record }">
          <div> {{ $toLocaleDate(record.startdate) }} </div>
        </template>

        <template #enddate="{ record }">
          <div> {{ $toLocaleDate(record.enddate)}} </div>
        </template>

        <template #created="{ record }">
          <div> {{ $toLocaleDate(record.created) }} </div>
        </template>

      </a-table>
      <a-divider/>
      <a-pagination
        class="row-element pagination"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="totalKeypairs"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>
    <bulk-action-view
      v-if="(showConfirmationAction || showGroupActionModal)"
      :showConfirmationAction="showConfirmationAction"
      :showGroupActionModal="showGroupActionModal"
      :items="keypairs"
      :selectedRowKeys="selectedRowKeys"
      :selectedItems="selectedItems"
      :columns="columns"
      :selectedColumns="selectedColumns"
      action="eraseKeypairs"
      :loading="loading"
      :message="bulkDeleteMessage"
      @group-action="eraseKeypairs"
      @handle-cancel="handleCancelBulk"
      @close-modal="closeModalBulk" />
    <generate-api-key-pair
      :showAddKeyPair="showAddKeyPair"
      :resource="resource"
      @fetch-data="fetchData"
      @refresh-data="handleRefreshData"
      @close-modal="closeModalAddKeyPair" />
  </div>
</template>
<script>
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import BulkActionView from '@/components/view/BulkActionView.vue'
import eventBus from '@/config/eventBus'
import GenerateApiKeyPair from '@/views/iam/GenerateApiKeyPair.vue'

export default {
  name: 'ApiKeyPairsTab',
  components: {
    TooltipButton,
    BulkActionView,
    GenerateApiKeyPair
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
      keypairs: [],
      page: 1,
      pageSize: 10,
      totalKeypairs: 0,
      selectedRowKeys: [],
      selectedItems: [],
      selectedColumns: [],
      filterColumns: ['Action'],
      showConfirmationAction: false,
      showAddKeyPair: false,
      showGroupActionModal: false,
      bulkDeleteMessage: {
        title: this.$t('label.action.bulk.delete.api.keys'),
        confirmMessage: this.$t('label.confirm.delete.api.keys')
      },
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          slots: { customRender: 'name' }
        },
        {
          title: this.$t('label.apikey'),
          dataIndex: 'apikey',
          slots: { customRender: 'apikey' }
        },
        {
          title: this.$t('label.secretkey'),
          dataIndex: 'secretkey',
          slots: { customRender: 'secretkey' }
        },
        {
          title: this.$t('label.start.date'),
          dataIndex: 'startdate',
          slots: { customRender: 'startdate' }
        },
        {
          title: this.$t('label.end.date'),
          dataIndex: 'enddate',
          slots: { customRender: 'enddate' }
        },
        {
          title: this.$t('label.created'),
          dataIndex: 'created',
          slots: { customRender: 'created' }
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
  inject: ['parentFetchData'],
  methods: {
    fetchData () {
      const params = {
        listall: true,
        page: this.page,
        pagesize: this.pageSize,
        userid: this.resource.id
      }
      this.fetchLoading = true
      getAPI('listUserKeys', params).then(json => {
        this.totalKeypairs = json.listuserkeysresponse.count || 0
        this.keypairs = json.listuserkeysresponse.userapikey || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
      this.selectedItems = (this.keypairs.filter(function (item) {
        return selection.indexOf(item.id) !== -1
      }))
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
    },
    onShowAddKeyPair () {
      this.showAddKeyPair = true
    },
    eraseKeypairs () {
      this.selectedColumns.splice(0, 0, {
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        slots: { customRender: 'status' },
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      if (this.selectedRowKeys.length > 0) {
        this.showGroupActionModal = true
      }
      this.deleteKeypairs(this.selectedItems)
    },
    async deleteKeypairs (keypairs) {
      if (!keypairs || keypairs.length === 0) {
        this.fetchLoading = false
        return
      }

      this.fetchLoading = true
      try {
        await Promise.all(keypairs.map(async keypair => {
          try {
            const jobId = await this.deleteKeyPair({
              keypairid: keypair.id
            })
            await this.$pollJob({
              jobId,
              action: {
                isFetchData: false
              },
              successMethod: () => {
                eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: keypair.id, state: 'success' })
              },
              catchMethod: () => {
                eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: keypair.id, state: 'failed' })
              }
            })
          } catch (e) {
            eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: keypair.id, state: 'failed' })
          }
        }))
      } finally {
        this.fetchLoading = false
      }
    },
    async deleteKeyPair (args) {
      const response = await postAPI('deleteUserKeys', args)
      return response.deleteuserkeysresponse.jobid
    },
    bulkActionConfirmation () {
      this.showConfirmationAction = true
      this.selectedColumns = this.columns.filter(column => {
        return !this.filterColumns.includes(column.title)
      })
      this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
    },
    handleCancelBulk () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.selectedColumns = []
      this.selectedRowKeys = []
      this.parentFetchData()
    },
    closeModalBulk () {
      this.showConfirmationAction = false
    },
    closeModalAddKeyPair () {
      this.showAddKeyPair = false
    },
    handleRefreshData () {
      this.$emit('refresh-data')
    },
    rowSelection () {
      if ('deleteUserKeys' in this.$store.getters.apis) {
        return {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.setSelection
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.list {
  max-height: 95vh;
  width: 95vw;
  overflow-y: scroll;
  margin: -24px;

  @media (min-width: 1000px) {
    max-height: 70vh;
    width: 900px;
  }

  &__header,
  &__footer {
    padding: 20px;
  }

  &__header {
    display: flex;

    .ant-select {
      min-width: 200px;
    }

    &__col {

      &:not(:last-child) {
        margin-right: 20px;
      }

      &--full {
        flex: 1;
      }

    }

  }

  &__footer {
    display: flex;
    justify-content: flex-end;

    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }

  &__item {
    padding-right: 20px;
    padding-left: 20px;

    &--selected {
      background-color: #e6f7ff;
    }

  }

  &__title {
    font-weight: bold;
  }

  &__outer-container {
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  &__container {
    display: flex;
    flex-direction: column;
    width: 100%;
    cursor: pointer;

    @media (min-width: 760px) {
      flex-direction: row;
      align-items: center;
    }

  }

  &__row {
    margin-bottom: 10px;

    @media (min-width: 760px) {
      margin-right: 20px;
      margin-bottom: 0;
    }
  }

  &__radio {
    display: flex;
    justify-content: flex-end;
  }

}
</style>
