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
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-table
          size="small"
          :loading="loading"
          :columns="columns"
          :dataSource="dataSource"
          :pagination="false"
          :rowKey="record => record.accountid || record.account"
        >
          <span slot="action" v-if="record.role!==owner" slot-scope="text, record" class="account-button-action">
            <a-tooltip placement="top">
              <template slot="title">
                {{ $t('label.make.project.owner') }}
              </template>
              <a-button type="default" shape="circle" icon="user" size="small" @click="onMakeProjectOwner(record)" />
            </a-tooltip>
            <a-tooltip placement="top">
              <template slot="title">
                {{ $t('label.remove.project.account') }}
              </template>
              <a-button
                type="danger"
                shape="circle"
                icon="delete"
                size="small"
                @click="onShowConfirmDelete(record)"/>
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

export default {
  name: 'AccountsTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      columns: [],
      dataSource: [],
      loading: false,
      page: 1,
      pageSize: 10,
      itemCount: 0,
      owner: 'Admin'
    }
  },
  created () {
    this.columns = [
      {
        title: this.$t('account'),
        dataIndex: 'account',
        width: '35%',
        scopedSlots: { customRender: 'account' }
      },
      {
        title: this.$t('role'),
        dataIndex: 'role',
        scopedSlots: { customRender: 'role' }
      },
      {
        title: this.$t('action'),
        dataIndex: 'action',
        fixed: 'right',
        width: 100,
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
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      const params = {}
      params.projectId = this.resource.id
      params.page = this.page
      params.pageSize = this.pageSize

      this.loading = true

      api('listProjectAccounts', params).then(json => {
        const listProjectAccount = json.listprojectaccountsresponse.projectaccount
        const itemCount = json.listprojectaccountsresponse.count

        if (!listProjectAccount || listProjectAccount.length === 0) {
          this.dataSource = []
          return
        }

        this.itemCount = itemCount
        this.dataSource = listProjectAccount
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
    onMakeProjectOwner (record) {
      const title = this.$t('label.make.project.owner')
      const loading = this.$message.loading(title + 'in progress for ' + record.account, 0)
      const params = {}

      params.id = this.resource.id
      params.account = record.account

      api('updateProject', params).then(json => {
        const hasJobId = this.checkForAddAsyncJob(json, title, record.account)

        if (hasJobId) {
          this.fetchData()
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
    onShowConfirmDelete (record) {
      const self = this
      let title = this.$t('deleteconfirm')
      title = title.replace('{name}', this.$t('account'))

      this.$confirm({
        title: title,
        okText: 'OK',
        okType: 'danger',
        cancelText: 'Cancel',
        onOk () {
          self.removeAccount(record)
        }
      })
    },
    removeAccount (record) {
      const title = this.$t('label.remove.project.account')
      const loading = this.$message.loading(title + 'in progress for ' + record.account, 0)
      const params = {}

      params.account = record.account
      params.projectid = this.resource.id

      api('deleteAccountFromProject', params).then(json => {
        const hasJobId = this.checkForAddAsyncJob(json, title, record.account)

        if (hasJobId) {
          this.fetchData()
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

  .row-element {
    margin-top: 10px;
    margin-bottom: 10px;
  }

  .account-button-action button {
    margin-right: 5px;
  }
</style>
