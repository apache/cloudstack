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
        v-if="canCreate"
        type="dashed"
        style="width: 100%; margin-bottom: 15px"
        @click="showCreateToken = true">
        <template #icon><plus-outlined /></template>
        {{ $t('label.action.create.mcp.token') }}
      </a-button>
      <a-table
        size="small"
        style="overflow-y: auto"
        :columns="columns"
        :dataSource="tokens"
        :rowKey="item => item.id"
        :pagination="false" >
        <template #enddate="{ record }">
          <div> {{ record.enddate ? $toLocaleDate(record.enddate) : $t('label.never') }} </div>
        </template>
        <template #created="{ record }">
          <div> {{ $toLocaleDate(record.created) }} </div>
        </template>
        <template #actions="{ record }">
          <a-popconfirm
            v-if="'revokeMcpToken' in $store.getters.apis"
            :title="$t('message.confirm.revoke.mcp.token')"
            :okText="$t('label.yes')"
            :cancelText="$t('label.no')"
            @confirm="revokeToken(record)">
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.action.revoke.mcp.token')"
              type="primary"
              :danger="true"
              icon="delete-outlined" />
          </a-popconfirm>
        </template>
      </a-table>
      <a-divider/>
      <a-pagination
        class="row-element pagination"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="totalTokens"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePage"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>
    <generate-mcp-token
      :showModal="showCreateToken"
      @fetch-data="fetchData"
      @close-modal="showCreateToken = false" />
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import GenerateMcpToken from '@/views/iam/GenerateMcpToken.vue'

export default {
  name: 'McpTokensTab',
  components: {
    TooltipButton,
    GenerateMcpToken
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
      tokens: [],
      page: 1,
      pageSize: 10,
      totalTokens: 0,
      showCreateToken: false,
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.description'),
          dataIndex: 'description'
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
        },
        {
          title: this.$t('label.actions'),
          slots: { customRender: 'actions' }
        }
      ]
    }
  },
  computed: {
    // A token is always created for the logged in user
    canCreate () {
      return 'createMcpToken' in this.$store.getters.apis && this.resource.id === this.$store.getters.userInfo.id
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
        page: this.page,
        pagesize: this.pageSize,
        userid: this.resource.id
      }
      this.fetchLoading = true
      getAPI('listMcpTokens', params).then(json => {
        this.totalTokens = json.listmcptokensresponse.count || 0
        this.tokens = json.listmcptokensresponse.mcptoken || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    revokeToken (token) {
      this.fetchLoading = true
      postAPI('revokeMcpToken', { id: token.id }).then(() => {
        this.$message.success(this.$t('message.success.revoke.mcp.token'))
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchData()
      })
    }
  }
}
</script>
