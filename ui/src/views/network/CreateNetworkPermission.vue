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
  <a-spin :spinning="loading">
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <div class="form">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleSubmit"
          layout="vertical">
          <a-form-item v-if="isAdminOrDomainAdmin()" name="accountids" ref="accountids">
            <template #label>
              <tooltip-label :title="$t('label.account')" :tooltip="apiParams.accountids.description"/>
            </template>
            <a-select
              v-model:value="form.accountids"
              mode="multiple"
              :loading="accountLoading"
              :placeholder="apiParams.accountids.description"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="(opt, optIndex) in accounts" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="isAdminOrDomainAdmin()" name="projectids" ref="projectids">
            <template #label>
              <tooltip-label :title="$t('label.project')" :tooltip="apiParams.projectids.description"/>
            </template>
            <a-select
              v-model:value="form.projectids"
              mode="multiple"
              :loading="projectLoading"
              :placeholder="apiParams.projectids.description"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="(opt, optIndex) in projects" :key="optIndex" :label="opt.name || opt.description">
                <span>
                  <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined style="margin-right: 5px" />
                  {{ opt.name || opt.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="!isAdminOrDomainAdmin()">
            <template #label>
              <tooltip-label :title="$t('label.accounts')" :tooltip="apiParams.accounts.description"/>
            </template>
            <a-input
              v-model:value="form.accounts"
              :placeholder="apiParams.accounts.description"
              v-focus="true" />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button
              :loading="loading"
              @click="closeAction">
              {{ this.$t('label.cancel') }}
            </a-button>
            <a-button
              :loading="loading"
              type="primary"
              ref="submit"
              @click="handleSubmit">
              {{ this.$t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { isAdminOrDomainAdmin } from '@/role'
import { ref, reactive, toRaw } from 'vue'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateNetworkPermissions',
  components: {
    TooltipLabel,
    ResourceIcon
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      accountLoading: false,
      projectLoading: false,
      accounts: [],
      projects: []
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
    this.apiParams = this.$getApiParams('createNetworkPermissions')
    this.fetchData()
  },
  methods: {
    isAdminOrDomainAdmin () {
      return isAdminOrDomainAdmin()
    },
    async fetchData () {
      this.fetchAccountData()
      this.fetchProjectData()
    },
    fetchAccountData () {
      this.accounts = []
      const params = {}
      params.showicon = true
      params.details = 'min'
      params.domainid = this.resource.domainid
      this.accountLoading = true
      api('listAccounts', params).then(json => {
        const listaccounts = json.listaccountsresponse.account || []
        this.accounts = listaccounts
      }).finally(() => {
        this.accountLoading = false
      })
    },
    fetchProjectData () {
      this.projects = []
      const params = {}
      params.listall = true
      params.showicon = true
      params.details = 'min'
      params.domainid = this.resource.domainid
      this.projectLoading = true
      api('listProjects', params).then(json => {
        const listProjects = json.listprojectsresponse.project || []
        this.projects = listProjects
      }).finally(() => {
        this.projectLoading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.networkid = this.resource.id
        var accountIndexes = values.accountids
        var accountId = null
        if (accountIndexes && accountIndexes.length > 0) {
          var accountIds = []
          for (var i = 0; i < accountIndexes.length; i++) {
            accountIds = accountIds.concat(this.accounts[accountIndexes[i]].id)
          }
          accountId = accountIds.join(',')
        }
        if (accountId) {
          params.accountids = accountId
        }
        var projectIndexes = values.projectids
        var projectId = null
        if (projectIndexes && projectIndexes.length > 0) {
          var projectIds = []
          for (var j = 0; j < projectIndexes.length; j++) {
            projectIds = projectIds.concat(this.projects[projectIndexes[j]].id)
          }
          projectId = projectIds.join(',')
        }
        if (projectId) {
          params.projectids = projectId
        }

        if (values.accounts && values.accounts.length > 0) {
          params.accounts = values.accounts
        }

        this.loading = true

        api('createNetworkPermissions', params)
          .then(() => {
            this.$notification.success({
              message: this.$t('message.success.add.network.permissions')
            })
            this.closeAction()
            this.$emit('refresh-data')
          }).catch(error => {
            this.$notification.error({
              message: `${this.$t('label.error')} ${error.response.status}`,
              description: error.response.data.createnetworkpermissionsresponse.errortext || error.response.data.errorresponse.errortext,
              duration: 0
            })
          }).finally(() => {
            this.loading = false
          })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
.form-layout {
  width: 60vw;

  @media (min-width: 500px) {
    width: 450px;
  }
}
</style>
