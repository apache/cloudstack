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
          placeholder="Search"
          v-model="searchQuery"
          @search="onSearch" />
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
          <template slot="state" slot-scope="text">
            <status :text="text ? text : ''" displayText />
          </template>
          <span slot="action" v-if="record.state===stateAllow" slot-scope="text, record" class="account-button-action">
            <a-tooltip placement="top">
              <template slot="title">
                {{ $t('label.accept.project.invitation') }}
              </template>
              <a-button
                type="success"
                shape="circle"
                icon="check"
                size="small"
                @click="onShowConfirmAcceptInvitation(record)"/>
            </a-tooltip>
            <a-tooltip placement="top">
              <template slot="title">
                {{ $t('label.decline.invitation') }}
              </template>
              <a-button
                type="danger"
                shape="circle"
                icon="close"
                size="small"
                @click="onShowConfirmRevokeInvitation(record)"/>
            </a-tooltip>
          </span>
        </a-table>
        <a-pagination
          class="row-element"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="itemCount"
          :showTotal="total => `Total ${total} items`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger/>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'InvitationsTemplate',
  components: {
    Status
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
        title: this.$t('project'),
        dataIndex: 'project',
        scopedSlots: { customRender: 'project' }
      },
      {
        title: this.$t('domain'),
        dataIndex: 'domain',
        scopedSlots: { customRender: 'domain' }
      },
      {
        title: this.$t('state'),
        dataIndex: 'state',
        width: 130,
        scopedSlots: { customRender: 'state' },
        filters: [
          {
            text: this.$t('Pending'),
            value: 'Pending'
          },
          {
            text: this.$t('Completed'),
            value: 'Completed'
          },
          {
            text: this.$t('Declined'),
            value: 'Declined'
          }
        ],
        filterMultiple: false
      },
      {
        title: this.$t('action'),
        dataIndex: 'action',
        width: 80,
        scopedSlots: { customRender: 'action' }
      }
    ]

    this.page = 1
    this.pageSize = 10
    this.itemCount = 0
  },
  mounted () {
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
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
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
      const title = this.$t('confirmacceptinvitation')

      this.$confirm({
        title: title,
        okText: 'OK',
        okType: 'danger',
        cancelText: 'Cancel',
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

      const loading = this.$message.loading(title + 'in progress for ' + record.project, 0)
      const params = {}

      params.projectid = record.projectid
      params.account = record.account
      params.domainid = record.domainid
      params.accept = state

      api('updateProjectInvitation', params).then(json => {
        const hasJobId = this.checkForAddAsyncJob(json, title, record.project)

        if (hasJobId) {
          this.fetchData()
          this.$emit('refresh-data')
        }
      }).catch(error => {
        // show error
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
      }).finally(() => {
        setTimeout(loading, 1000)
      })
    },
    onShowConfirmRevokeInvitation (record) {
      const self = this
      const title = this.$t('confirmdeclineinvitation')

      this.$confirm({
        title: title,
        okText: 'OK',
        okType: 'danger',
        cancelText: 'Cancel',
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
              this.$store.dispatch('AddAsyncJob', {
                title: title,
                jobid: jobId,
                description: description,
                status: 'progress'
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
  /deep/.ant-table-fixed-right {
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
