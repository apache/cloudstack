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
    <div class="form" v-ctrl-enter="submitData">

      <div v-if="loading" class="loading">
        <loading-outlined style="color: #1890ff;" />
      </div>

      <a-alert type="warning" style="margin-bottom: 20px">
        <template #message>
          <label v-html="$t('message.assign.instance.another')"></label>
        </template>
      </a-alert>

      <div class="form__item">
        <p class="form__label">{{ $t('label.accounttype') }}</p>
        <a-select
          v-model:value="selectedAccountType"
          v-focus="true"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }">
          <a-select-option :value="$t('label.account')">{{ $t('label.account') }}</a-select-option>
          <a-select-option :value="$t('label.project')">{{ $t('label.project') }}</a-select-option>
        </a-select>
      </div>

      <div class="form__item">
        <p class="form__label"><span class="required">*</span>{{ $t('label.domain') }}</p>
        <a-select
          @change="changeDomain"
          v-model:value="selectedDomain"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="domain in domains" :key="domain.name" :value="domain.id" :label="domain.path || domain.name || domain.description">
            <span>
              <resource-icon v-if="domain && domain.icon" :image="domain.icon.base64image" size="1x" style="margin-right: 5px"/>
              <block-outlined v-else style="margin-right: 5px" />
              {{ domain.path || domain.name || domain.description }}
            </span>
          </a-select-option>
        </a-select>
      </div>

      <template v-if="selectedAccountType === $t('label.account')">
        <div class="form__item">
          <p class="form__label"><span class="required">*</span>{{ $t('label.account') }}</p>
          <a-select
            @change="changeAccount"
            v-model:value="selectedAccount"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="account in accounts" :key="account.name" :value="account.name">
              <span>
                <resource-icon v-if="account && account.icon" :image="account.icon.base64image" size="1x" style="margin-right: 5px"/>
                <team-outlined v-else style="margin-right: 5px" />
                {{ account.name }}
              </span>
            </a-select-option>
          </a-select>
          <span v-if="accountError" class="required">{{ $t('label.required') }}</span>
        </div>
      </template>

      <template v-else>
        <div class="form__item">
          <p class="form__label"><span class="required">*</span>{{ $t('label.project') }}</p>
          <a-select
            @change="changeProject"
            v-model:value="selectedProject"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="project in projects" :key="project.id" :value="project.id" :label="project.name">
              <span>
                <resource-icon v-if="project && project.icon" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
                <project-outlined v-else style="margin-right: 5px" />
                {{ project.name }}
              </span>
            </a-select-option>
          </a-select>
          <span v-if="projectError" class="required">{{ $t('label.required') }}</span>
        </div>
      </template>

      <div class="form__item">
        <p class="form__label">{{ $t('label.network') }}</p>
        <a-select
          v-model:value="selectedNetwork"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="network in networks" :key="network.id" :value="network.id" :label="network.name ? network.name : '-'">
            <span>
              <resource-icon v-if="network && network.icon" :image="network.icon.base64image" size="1x" style="margin-right: 5px"/>
              <apartment-outlined v-else style="margin-right: 5px" />
              {{ network.name ? network.name : '-' }}
            </span>
          </a-select-option>
        </a-select>
      </div>

      <div class="submit-btn">
        <a-button @click="closeAction">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button type="primary" @click="submitData" ref="submit">
          {{ $t('label.submit') }}
        </a-button>
      </div>

    </div>

  </div>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'AssignInstance',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon
  },
  inject: ['parentFetchData'],
  data () {
    return {
      domains: [],
      accounts: [],
      projects: [],
      networks: [],
      selectedAccountType: 'Account',
      selectedDomain: null,
      selectedAccount: null,
      selectedProject: null,
      selectedNetwork: null,
      accountError: false,
      projectError: false,
      loading: false
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listDomains', {
        response: 'json',
        listAll: true,
        showicon: true,
        details: 'min'
      }).then(response => {
        this.domains = response.listdomainsresponse.domain || []
        this.selectedDomain = this.domains[0].id
        this.fetchAccounts()
        this.fetchProjects()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchAccounts () {
      this.loading = true
      api('listAccounts', {
        response: 'json',
        domainId: this.selectedDomain,
        showicon: true,
        state: 'Enabled',
        isrecursive: false
      }).then(response => {
        this.accounts = response.listaccountsresponse.account || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchProjects () {
      this.loading = true
      api('listProjects', {
        response: 'json',
        domainId: this.selectedDomain,
        state: 'Active',
        showicon: true,
        details: 'min',
        isrecursive: false
      }).then(response => {
        this.projects = response.listprojectsresponse.project || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchNetworks () {
      this.loading = true
      var params = {
        domainId: this.selectedDomain,
        listAll: true,
        isrecursive: false,
        showicon: true
      }
      if (this.selectedProject != null) {
        params.projectid = this.selectedProject
      } else {
        params.account = this.selectedAccount
        params.ignoreproject = true
      }
      api('listNetworks', params).then(response => {
        this.networks = response.listnetworksresponse.network || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    changeDomain () {
      this.selectedAccount = null
      this.selectedProject = null
      this.selectedNetwork = null
      this.fetchAccounts()
      this.fetchProjects()
    },
    changeAccount () {
      this.selectedProject = null
      this.selectedNetwork = null
      this.fetchNetworks()
    },
    changeProject () {
      this.selectedAccount = null
      this.selectedNetwork = null
      this.fetchNetworks()
    },
    closeAction () {
      this.$emit('close-action')
    },
    submitData () {
      if (this.loading) return

      let variableKey = ''
      let variableValue = ''

      if (this.selectedAccountType === 'Account') {
        if (!this.selectedAccount) {
          this.accountError = true
          return
        }
        variableKey = 'account'
        variableValue = this.selectedAccount
      } else if (this.selectedAccountType === 'Project') {
        if (!this.selectedProject) {
          this.projectError = true
          return
        }
        variableKey = 'projectid'
        variableValue = this.selectedProject
      }

      this.loading = true
      api('assignVirtualMachine', {
        response: 'json',
        virtualmachineid: this.resource.id,
        domainid: this.selectedDomain,
        [variableKey]: variableValue,
        networkids: this.selectedNetwork
      }).then(response => {
        this.$notification.success({
          message: this.$t('label.loadbalancerinstance')
        })
        this.$emit('close-action')
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
    width: 85vw;

    @media (min-width: 760px) {
      width: 500px;
    }

    display: flex;
    flex-direction: column;

    &__item {
      display: flex;
      flex-direction: column;
      width: 100%;
      margin-bottom: 10px;
    }

    &__label {
      display: flex;
      font-weight: bold;
      margin-bottom: 5px;
    }

  }

  .submit-btn {
    margin-top: 10px;
    align-self: flex-end;

    button {
      margin-left: 10px;
    }
  }

  .required {
    margin-right: 2px;
    color: red;
    font-size: 0.7rem;
  }

  .loading {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 3rem;
  }
</style>
