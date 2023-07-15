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
    <a-tabs class="form-layout">
      <a-tab-pane
        key="1"
        :tab="$t('label.action.project.add.account')"
        v-ctrl-enter="addAccountToProject">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
         >
          <a-form-item name="account" ref="account">
            <template #label>
              <tooltip-label :title="$t('label.account')" :tooltip="apiParams.addAccountToProject.account.description"/>
            </template>
            <a-input
              v-model:value="form.account"
              :placeholder="apiParams.addAccountToProject.account.description"
              v-focus="true" />
          </a-form-item>
          <a-form-item name="email" ref="email">
            <template #label>
              <tooltip-label :title="$t('label.email')" :tooltip="apiParams.addAccountToProject.email.description"/>
            </template>
            <a-input
              v-model:value="form.email"
              :placeholder="apiParams.addAccountToProject.email.description"></a-input>
          </a-form-item>
          <a-form-item name="projectroleid" ref="projectroleid" v-if="apiParams.addAccountToProject.projectroleid">
            <template #label>
              <tooltip-label :title="$t('label.project.role')" :tooltip="apiParams.addAccountToProject.projectroleid.description"/>
            </template>
            <a-select
              v-model:value="form.projectroleid"
              :loading="loading"
              :placeholder="apiParams.addAccountToProject.projectroleid.description"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="role in projectRoles" :key="role.id" :label="role.name">
                {{ role.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="roletype" ref="roletype" v-if="apiParams.addAccountToProject.roletype">
            <template #label>
              <tooltip-label :title="$t('label.roletype')" :tooltip="apiParams.addAccountToProject.roletype.description"/>
            </template>
            <a-select
              v-model:value="form.roletype"
              :placeholder="apiParams.addAccountToProject.roletype.description"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option value="Admin">Admin</a-select-option>
              <a-select-option value="Regular">Regular</a-select-option>
            </a-select>
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="addAccountToProject" :loading="loading">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-tab-pane>
      <a-tab-pane
        key="2"
        :tab="$t('label.action.project.add.user')"
        v-if="apiParams.addUserToProject"
        v-ctrl-enter="addUserToProject">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
         >
          <a-alert type="warning" style="margin-bottom: 20px">
            <template #message>
              <div v-html="$t('message.add.user.to.project')"></div>
            </template>
          </a-alert>
          <a-form-item name="username" ref="username">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="apiParams.addUserToProject.username.description"/>
            </template>
            <a-input
              v-model:value="form.username"
              :placeholder="apiParams.addUserToProject.username.description"
              v-focus="true" />
          </a-form-item>
          <a-form-item name="email" ref="email">
            <template #label>
              <tooltip-label :title="$t('label.email')" :tooltip="apiParams.addUserToProject.email.description"/>
            </template>
            <a-input
              v-model:value="form.email"
              :placeholder="apiParams.addUserToProject.email.description"></a-input>
          </a-form-item>
          <a-form-item name="projectroleid" ref="projectroleid">
            <template #label>
              <tooltip-label :title="$t('label.project.role')" :tooltip="apiParams.addUserToProject.roletype.description"/>
            </template>
            <a-select
              v-model:value="form.projectroleid"
              :loading="loading"
              :placeholder="apiParams.addUserToProject.roletype.description"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="role in projectRoles" :key="role.id" :label="role.name">
                {{ role.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="roletype" ref="roletype">
            <template #label>
              <tooltip-label :title="$t('label.roletype')" :tooltip="apiParams.addUserToProject.roletype.description"/>
            </template>
            <a-select
              v-model:value="form.roletype"
              :placeholder="apiParams.addUserToProject.roletype.description"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option value="Admin">Admin</a-select-option>
              <a-select-option value="Regular">Regular</a-select-option>
            </a-select>
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="addUserToProject" :loading="loading">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddAccountOrUserToProject',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      users: [],
      accounts: [],
      projectRoles: [],
      selectedUser: null,
      selectedAccount: null,
      loading: false,
      load: {
        users: false,
        accounts: false,
        projectRoles: false
      }
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  beforeCreate () {
    const apis = ['addAccountToProject']
    if ('addUserToProject' in this.$store.getters.apis) {
      apis.push('addUserToProject')
    }
    this.apiParams = {}
    for (var api of apis) {
      this.apiParams[api] = this.$getApiParams(api)
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    fetchData () {
      this.fetchUsers()
      this.fetchAccounts()
      if (this.isProjectRolesSupported()) {
        this.fetchProjectRoles()
      }
    },
    fetchUsers () {
      this.load.users = true
      api('listUsers', { listall: true }).then(response => {
        this.users = response.listusersresponse.user ? response.listusersresponse.user : []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.load.users = false
      })
    },
    fetchAccounts () {
      this.load.accounts = true
      api('listAccounts', {
        domainid: this.resource.domainid
      }).then(response => {
        this.accounts = response.listaccountsresponse.account || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.load.accounts = false
      })
    },
    fetchProjectRoles () {
      this.load.projectRoles = true
      api('listProjectRoles', {
        projectid: this.resource.id
      }).then(response => {
        this.projectRoles = response.listprojectrolesresponse.projectrole || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.load.projectRoles = false
      })
    },
    isProjectRolesSupported () {
      return ('listProjectRoles' in this.$store.getters.apis)
    },
    addAccountToProject (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        var params = {
          projectid: this.resource.id
        }
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          params[key] = input
        }
        api('addAccountToProject', params).then(response => {
          this.$pollJob({
            jobId: response.addaccounttoprojectresponse.jobid,
            successMessage: `Successfully added account ${params.account} to project`,
            errorMessage: `Failed to add account: ${params.account} to project`,
            loadingMessage: `Adding Account: ${params.account} to project...`,
            catchMessage: 'Error encountered while fetching async job result'
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    addUserToProject (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        this.loading = true
        var params = {
          projectid: this.resource.id
        }
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          params[key] = input
        }
        api('addUserToProject', params).then(response => {
          this.$pollJob({
            jobId: response.addusertoprojectresponse.jobid,
            successMessage: `Successfully added user ${params.username} to project`,
            errorMessage: `Failed to add user: ${params.username} to project`,
            loadingMessage: `Adding User ${params.username} to project...`,
            catchMessage: 'Error encountered while fetching async job result'
          })
          this.closeAction()
        }).catch(error => {
          console.log('catch')
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>
<style lang="scss" scoped>
  .form-layout {
    width: 80vw;

    @media (min-width: 600px) {
      width: 450px;
    }
  }
</style>
