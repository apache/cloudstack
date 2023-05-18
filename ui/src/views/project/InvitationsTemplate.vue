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
  <div class="row-invitation">
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-input-search
          class="input-search-invitation"
          style="width: unset"
          :placeholder="$t('label.search')"
          v-model:value="searchQuery"
          @search="onSearch"
          v-focus="true" />
      </a-col>
      <a-col :md="24" :lg="24">
        <a-table
          size="small"
          :loading="loading"
          :columns="columns"
          :dataSource="dataSource"
          :pagination="false"
          :rowKey="record => record.id || record.account"
          @change="onChangeTable">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'state'">
              <status :text="text ? text : ''" displayText />
            </template>
            <template v-if="column.key === 'actions'">
              <div v-if="record.state===stateAllow" class="account-button-action">
                <tooltip-button
                  tooltipPlacement="top"
                  :tooltip="$t('label.accept.project.invitation')"
                  icon="check-outlined"
                  size="small"
                  @onClick="onShowConfirmAcceptInvitation(record)"/>
                <tooltip-button
                  tooltipPlacement="top"
                  :tooltip="$t('label.decline.invitation')"
                  type="primary"
                  :danger="true"
                  icon="close-outlined"
                  size="small"
                  @onClick="onShowConfirmRevokeInvitation(record)"/>
              </div>
            </template>
          </template>
        </a-table>
        <a-pagination
          class="row-element"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="itemCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'InvitationsTemplate',
  components: {
    Status,
    TooltipButton
  },
  data () {
    return {
      columns: [],
      dataSource: [],
      listDomains: [],
      loading: false,
      page: 1,
      pageSize: 10,
      itemCount: 0,
      state: undefined,
      domainid: undefined,
      projectid: undefined,
      searchQuery: undefined,
      stateAllow: 'Pending'
    }
  },
  created () {
    this.columns = [
      {
        key: 'project',
        title: this.$t('label.project'),
        dataIndex: 'project'
      },
      {
        key: 'account',
        title: this.$t('label.account'),
        dataIndex: 'account'
      },
      {
        key: 'domain',
        title: this.$t('label.domain'),
        dataIndex: 'domain'
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'state',
        width: 130,
        filters: [
          {
            text: this.$t('state.pending'),
            value: 'Pending'
          },
          {
            text: this.$t('state.completed'),
            value: 'Completed'
          },
          {
            text: this.$t('state.declined'),
            value: 'Declined'
          },
          {
            text: this.$t('state.expired'),
            value: 'Expired'
          }
        ],
        filterMultiple: false
      },
      {
        key: 'actions',
        title: this.$t('label.actions'),
        dataIndex: 'actions',
        width: 80
      }
    ]

    this.page = 1
    this.pageSize = 10
    this.itemCount = 0
    this.apiParams = this.$getApiParams('listProjectInvitations')
    if (this.apiParams.userid) {
      this.columns.splice(2, 0, {
        key: 'user',
        title: this.$t('label.user'),
        dataIndex: 'userid'
      })
    }
    this.fetchData()
  },
  methods: {
    fetchData () {
      const params = {}

      params.page = this.page
      params.pageSize = this.pageSize
      params.state = this.state
      params.domainid = this.domainid
      params.projectid = this.projectid
      params.keyword = this.searchQuery
      params.listAll = true

      this.loading = true
      this.dataSource = []
      this.itemCount = 0

      api('listProjectInvitations', params).then(json => {
        const listProjectInvitations = json.listprojectinvitationsresponse.projectinvitation
        const itemCount = json.listprojectinvitationsresponse.count

        if (!listProjectInvitations || listProjectInvitations.length === 0) {
          return
        }

        this.dataSource = listProjectInvitations
        this.itemCount = itemCount
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
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
    onShowConfirmAcceptInvitation (record) {
      const self = this
      const title = this.$t('label.confirmacceptinvitation')

      this.$confirm({
        title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.updateProjectInvitation(record, true)
        }
      })
    },
    updateProjectInvitation (record, state) {
      let title = ''

      if (state) {
        title = this.$t('label.accept.project.invitation')
      } else {
        title = this.$t('label.decline.invitation')
      }

      const loading = this.$message.loading(title + `${this.$t('label.in.progress.for')} ` + record.project, 0)
      const params = {}

      params.projectid = record.projectid
      if (record.userid && record.userid !== null) {
        params.userid = record.userid
      } else {
        params.account = record.account
      }
      params.domainid = record.domainid
      params.accept = state

      api('updateProjectInvitation', params).then(json => {
        this.$pollJob({
          jobId: json.updateprojectinvitationresponse.jobid,
          title,
          description: record.project,
          successMethod: () => {
            this.fetchData()
            this.$emit('refresh-data')
          },
          errorMethod: () => {
            this.fetchData()
            this.$emit('refresh-data')
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.$emit('refresh-data')
          }
        })
      }).catch(error => {
        // show error
        this.$notifyError(error)
      }).finally(() => {
        setTimeout(loading, 1000)
      })
    },
    onShowConfirmRevokeInvitation (record) {
      const self = this
      const title = this.$t('label.confirmdeclineinvitation')

      this.$confirm({
        title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.updateProjectInvitation(record, false)
        }
      })
    },
    onChangeTable (pagination, filters, sorter) {
      if (!filters || Object.keys(filters).length === 0) {
        return
      }

      this.state = filters.state && filters.state.length > 0 ? filters.state[0] : undefined
      this.domainid = filters.domain && filters.domain.length > 0 ? filters.domain[0] : undefined
      this.projectid = filters.project && filters.project.length > 0 ? filters.project[0] : undefined

      this.fetchData()
    },
    onSearch (value) {
      this.searchQuery = value
      this.fetchData()
    },
    checkForAddAsyncJob (json, title, description) {
      let hasJobId = false

      for (const obj in json) {
        if (obj.includes('response')) {
          for (const res in json[obj]) {
            if (res === 'jobid') {
              hasJobId = true
              const jobId = json[obj][res]
              this.$pollJob({
                title,
                jobId,
                description,
                showLoading: false
              })
            }
          }
        }
      }

      return hasJobId
    }
  }
}
</script>

<style scoped>
  :deep(.ant-table-fixed-right) {
    z-index: 5;
  }

  .row-invitation {
    min-width: 500px;
    max-width: 768px;
  }

  .row-element {
    margin-top: 15px;
    margin-bottom: 15px;
  }

  .account-button-action button {
    margin-right: 5px;
  }

  .input-search-invitation {
    float: right;
    margin-bottom: 10px;
  }
</style>
