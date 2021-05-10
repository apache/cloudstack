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
          :loading="loading.table"
          :columns="columns"
          :dataSource="dataSource"
          :pagination="false"
          :rowKey="record => record.userid ? record.userid : (record.accountid || record.account)">
          <span slot="user" slot-scope="text, record" v-if="record.userid">
            {{ record.username }}
          </span>
          <span slot="projectrole" slot-scope="text, record" v-if="record.projectroleid">
            {{ getProjectRole(record) }}
          </span>
          <span v-if="imProjectAdmin && dataSource.length > 1" slot="action" slot-scope="text, record" class="account-button-action">
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="record.userid ? $t('label.make.user.project.owner') : $t('label.make.project.owner')"
              v-if="record.role !== owner"
              type="default"
              icon="arrow-up"
              size="small"
              @click="promoteAccount(record)" />
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="record.userid ? $t('label.demote.project.owner.user') : $t('label.demote.project.owner')"
              v-if="updateProjectApi.params.filter(x => x.name === 'swapowner').length > 0 && record.role === owner"
              type="default"
              icon="arrow-down"
              size="small"
              @click="demoteAccount(record)" />
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="record.userid ? $t('label.remove.project.user') : $t('label.remove.project.account')"
              type="danger"
              icon="delete"
              size="small"
              :disabled="!('deleteAccountFromProject' in $store.getters.apis)"
              @click="onShowConfirmDelete(record)" />
          </span>
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
          <template slot="buildOptionText" slot-scope="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'AccountsTab',
  components: {
    TooltipButton
  },
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
      imProjectAdmin: false,
      loading: {
        user: false,
        projectAccount: false,
        roles: false,
        table: false
      },
      page: 1,
      pageSize: 10,
      itemCount: 0,
      users: [],
      projectRoles: [],
      owner: 'Admin',
      role: 'Regular'
    }
  },
  created () {
    this.columns = [
      {
        title: this.$t('label.account'),
        dataIndex: 'account',
        scopedSlots: { customRender: 'account' }
      },
      {
        title: this.$t('label.roletype'),
        dataIndex: 'role',
        scopedSlots: { customRender: 'role' }
      },
      {
        title: this.$t('label.action'),
        dataIndex: 'action',
        scopedSlots: { customRender: 'action' }
      }
    ]
    if (this.isProjectRolesSupported()) {
      this.columns.splice(1, 0, {
        title: this.$t('label.user'),
        dataIndex: 'userid',
        scopedSlots: { customRender: 'user' }
      })
      this.columns.splice(this.columns.length - 1, 0, {
        title: this.$t('label.project.role'),
        dataIndex: 'projectroleid',
        scopedSlots: { customRender: 'projectrole' }
      })
    }
    this.page = 1
    this.pageSize = 10
    this.itemCount = 0
    this.fetchData()
  },
  inject: ['parentFetchData'],
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
      this.updateProjectApi = this.$store.getters.apis.updateProject
      this.fetchUsers()
      this.fetchProjectAccounts(params)
      if (this.isProjectRolesSupported()) {
        this.fetchProjectRoles()
      }
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = 0
      this.pageSize = pageSize
      this.fetchData()
    },
    isLoggedInUserProjectAdmin (user) {
      if (['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)) {
        return true
      }
      // If I'm the logged in user Or if I'm the logged in account And I'm the owner
      if (((user.userid && user.userid === this.$store.getters.userInfo.id) ||
        user.account === this.$store.getters.userInfo.account) &&
        user.role === this.owner) {
        return true
      }
      return false
    },
    isProjectRolesSupported () {
      return ('listProjectRoles' in this.$store.getters.apis)
    },
    getProjectRole (record) {
      const projectRole = this.projectRoles.filter(role => role.id === record.projectroleid)
      return projectRole[0].name || projectRole[0].id || null
    },
    fetchUsers () {
      this.loading.user = true
      api('listUsers', { listall: true }).then(response => {
        this.users = response.listusersresponse.user || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading.user = false
      })
    },
    fetchProjectRoles () {
      this.loading.roles = true
      api('listProjectRoles', { projectId: this.resource.id }).then(response => {
        this.projectRoles = response.listprojectrolesresponse.projectrole || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading.roles = false
      })
    },
    fetchProjectAccounts (params) {
      this.loading.projectAccount = true
      api('listProjectAccounts', params).then(json => {
        const listProjectAccount = json.listprojectaccountsresponse.projectaccount
        const itemCount = json.listprojectaccountsresponse.count
        if (!listProjectAccount || listProjectAccount.length === 0) {
          this.dataSource = []
          return
        }
        for (const projectAccount of listProjectAccount) {
          this.imProjectAdmin = this.isLoggedInUserProjectAdmin(projectAccount)
          if (this.imProjectAdmin) {
            break
          }
        }

        this.itemCount = itemCount
        this.dataSource = listProjectAccount
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading.projectAccount = false
      })
    },
    onMakeProjectOwner (record) {
      const title = this.$t('label.make.project.owner')
      const loading = this.$message.loading(title + `${this.$t('label.in.progress.for')} ` + record.account, 0)
      const params = {}
      params.id = this.resource.id
      params.account = record.account
      this.updateProject(record, params, title, loading)
    },
    promoteAccount (record) {
      var title = this.$t('label.make.project.owner')
      const loading = this.$message.loading(title + `${this.$t('label.in.progress.for')} ` + record.account, 0)
      const params = {}

      params.id = this.resource.id
      if (record.userid) {
        params.userid = record.userid
        // params.accountid = (record.user && record.user[0].accountid) || record.accountid
        title = this.$t('label.make.user.project.owner')
      } else {
        params.account = record.account
      }
      params.roletype = this.owner
      params.swapowner = false
      this.updateProject(record, params, title, loading)
    },
    demoteAccount (record) {
      var title = this.$t('label.demote.project.owner')
      const loading = this.$message.loading(title + `${this.$t('label.in.progress.for')} ` + record.account, 0)
      const params = {}
      if (record.userid) {
        params.userid = record.userid
        // params.accountid = (record.user && record.user[0].accountid) || record.accountid
        title = this.$t('label.demote.project.owner.user')
      } else {
        params.account = record.account
      }

      params.id = this.resource.id
      params.roletype = 'Regular'
      params.swapowner = false
      this.updateProject(record, params, title, loading)
    },
    updateProject (record, params, title, loading) {
      api('updateProject', params).then(json => {
        const hasJobId = this.checkForAddAsyncJob(json, title, record.account)
        if (hasJobId) {
          this.fetchData()
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        setTimeout(loading, 1000)
        this.parentFetchData()
      })
    },
    onShowConfirmDelete (record) {
      const self = this
      const title = `${this.$t('label.deleteconfirm')} ${this.$t('label.account')}`

      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.removeAccount(record)
        }
      })
    },
    removeAccount (record) {
      const title = this.$t('label.remove.project.account')
      const loading = this.$message.loading(title + `${this.$t('label.in.progress.for')} ` + record.account, 0)
      const params = {}
      params.projectid = this.resource.id
      if (record.userid) {
        params.userid = record.userid
        this.deleteOperation('deleteUserFromProject', params, record, title, loading)
      } else {
        params.account = record.account
        this.deleteOperation('deleteAccountFromProject', params, record, title, loading)
      }
    },
    deleteOperation (apiName, params, record, title, loading) {
      api(apiName, params).then(json => {
        const hasJobId = this.checkForAddAsyncJob(json, title, record.account)
        if (hasJobId) {
          this.fetchData()
        }
      }).catch(error => {
        this.$notifyError(error)
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
