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
    <div class="form">

      <div v-if="loading" class="loading">
        <a-icon type="loading" style="color: #1890ff;"></a-icon>
      </div>

      <a-alert type="warning" style="margin-bottom: 20px">
        <span slot="message" v-html="$t('message.assign.instance.another')"></span>
      </a-alert>

      <div class="form__item">
        <p class="form__label">{{ $t('label.accounttype') }}</p>
        <a-select v-model="selectedAccountType" defaultValue="account" autoFocus>
          <a-select-option :value="$t('label.account')">{{ $t('label.account') }}</a-select-option>
          <a-select-option :value="$t('label.project')">{{ $t('label.project') }}</a-select-option>
        </a-select>
      </div>

      <div class="form__item">
        <p class="form__label"><span class="required">*</span>{{ $t('label.domain') }}</p>
        <a-select @change="changeDomain" v-model="selectedDomain" :defaultValue="selectedDomain">
          <a-select-option v-for="domain in domains" :key="domain.name" :value="domain.id">
            {{ domain.path || domain.name || domain.description }}
          </a-select-option>
        </a-select>
      </div>

      <template v-if="selectedAccountType === 'Account'">
        <div class="form__item">
          <p class="form__label"><span class="required">*</span>{{ $t('label.account') }}</p>
          <a-select @change="changeAccount" v-model="selectedAccount">
            <a-select-option v-for="account in accounts" :key="account.name" :value="account.name">
              {{ account.name }}
            </a-select-option>
          </a-select>
          <span v-if="accountError" class="required">{{ $t('label.required') }}</span>
        </div>
      </template>

      <template v-else>
        <div class="form__item">
          <p class="form__label"><span class="required">*</span>{{ $t('label.project') }}</p>
          <a-select @change="changeProject" v-model="selectedProject">
            <a-select-option v-for="project in projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </a-select-option>
          </a-select>
          <span v-if="projectError" class="required">{{ $t('label.required') }}</span>
        </div>
      </template>

      <div class="form__item">
        <p class="form__label">{{ $t('label.network') }}</p>
        <a-select v-model="selectedNetwork">
          <a-select-option v-for="network in networks" :key="network.id" :value="network.id">
            {{ network.name ? network.name : '-' }}
          </a-select-option>
        </a-select>
      </div>

      <div class="submit-btn">
        <a-button @click="closeAction">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button type="primary" @click="submitData">
          {{ $t('label.submit') }}
        </a-button>
      </div>

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
  created () {
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
        state: 'Enabled',
        isrecursive: false
      }).then(response => {
        this.accounts = response.listaccountsresponse.account
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
        details: 'min',
        isrecursive: false
      }).then(response => {
        this.projects = response.listprojectsresponse.project
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
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
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
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
    closeAction () {
      this.$emit('close-action')
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
          message: this.$t('label.loadbalancerinstance')
        })
        this.$parent.$parent.close()
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
