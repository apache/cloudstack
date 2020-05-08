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
    <p v-html="$t('message.assign.instance.another')"></p>

    <div class="form">

      <div v-if="loading" class="loading">
        <a-icon type="loading" style="color: #1890ff;"></a-icon>
      </div>

      <div class="form__item">
        <p class="form__label">{{ $t('accounttype') }}</p>
        <a-select v-model="selectedAccountType" defaultValue="account">
          <a-select-option :value="$t('account')">{{ $t('account') }}</a-select-option>
          <a-select-option :value="$t('project')">{{ $t('project') }}</a-select-option>
        </a-select>
      </div>

      <div class="form__item">
        <p class="form__label"><span class="required">*</span>{{ $t('domain') }}</p>
        <a-select @change="changeDomain" v-model="selectedDomain" :defaultValue="selectedDomain">
          <a-select-option v-for="domain in domains" :key="domain.name" :value="domain.id">
            {{ domain.path }}
          </a-select-option>
        </a-select>
      </div>

      <template v-if="selectedAccountType === 'Account'">
        <div class="form__item">
          <p class="form__label"><span class="required">*</span>{{ $t('account') }}</p>
          <a-select @change="changeAccount" v-model="selectedAccount">
            <a-select-option v-for="account in accounts" :key="account.name" :value="account.name">
              {{ account.name }}
            </a-select-option>
          </a-select>
          <span v-if="accountError" class="required">{{ $t('required') }}</span>
        </div>
      </template>

      <template v-else>
        <div class="form__item">
          <p class="form__label"><span class="required">*</span>{{ $t('project') }}</p>
          <a-select @change="changeProject" v-model="selectedProject">
            <a-select-option v-for="project in projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </a-select-option>
          </a-select>
          <span v-if="projectError" class="required">{{ $t('required') }}</span>
        </div>
      </template>

      <div class="form__item">
        <p class="form__label">{{ $t('network') }}</p>
        <a-select v-model="selectedNetwork">
          <a-select-option v-for="network in networks" :key="network.id" :value="network.id">
            {{ network.name ? network.name : '-' }}
          </a-select-option>
        </a-select>
      </div>

      <a-button type="primary" class="submit-btn" @click="submitData">
        {{ $t('submit') }}
      </a-button>

    </div>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'AssignInstance',
  props: {
    resource: {
      type: Object,
      required: true
    }
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
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listDomains', {
        response: 'json',
        listAll: true,
        details: 'min'
      }).then(response => {
        this.domains = response.listdomainsresponse.domain
        this.selectedDomain = this.domains[0].id
        this.fetchAccounts()
        this.fetchProjects()
      })
    },
    fetchAccounts () {
      this.loading = true
      api('listAccounts', {
        response: 'json',
        domainId: this.selectedDomain,
        state: 'Enabled',
        listAll: true
      }).then(response => {
        this.accounts = response.listaccountsresponse.account
        this.loading = false
      })
    },
    fetchProjects () {
      this.loading = true
      api('listProjects', {
        response: 'json',
        domainId: this.selectedDomain,
        state: 'Active',
        details: 'min',
        listAll: true
      }).then(response => {
        this.projects = response.listprojectsresponse.project
        this.loading = false
      })
    },
    fetchNetworks () {
      this.loading = true
      api('listNetworks', {
        response: 'json',
        domainId: this.selectedDomain,
        listAll: true,
        isrecursive: false,
        account: this.selectedAccount,
        projectid: this.selectedProject
      }).then(response => {
        this.networks = response.listnetworksresponse.network
        this.loading = false
      })
    },
    changeDomain () {
      this.selectedAccount = null
      this.fetchAccounts()
      this.fetchProjects()
    },
    changeAccount () {
      this.selectedProject = null
      this.fetchNetworks()
    },
    changeProject () {
      this.selectedAccount = null
      this.fetchNetworks()
    },
    submitData () {
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
          message: 'Successfully assigned instance'
        })
        this.loading = false
        this.$parent.$parent.close()
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
        this.$parent.$parent.close()
        this.parentFetchData()
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
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
