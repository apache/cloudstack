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
  <a-form layout="vertical" >
    <a-form-item :label="$t('label.accounttype')">
      <a-select
        v-model:value="selectedAccountType"
        defaultValue="account"
        autoFocus
        showSearch
        optionFilterProp="label"
        :filterOption="
          (input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }
        "
      >
        <a-select-option :value="$t('label.account')">{{ $t('label.account') }}</a-select-option>
        <a-select-option :value="$t('label.project')">{{ $t('label.project') }}</a-select-option>
      </a-select>
    </a-form-item>
    <a-form-item :label="$t('label.domain')" required>
      <a-select
        @change="changeDomain"
        v-model:value="selectedDomain"
        showSearch
        optionFilterProp="label"
        :filterOption="
          (input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }
        "
      >
        <a-select-option
          v-for="domain in domains"
          :key="domain.name"
          :value="domain.id"
          :label="domain.path || domain.name || domain.description"
        >
          <span>
            <resource-icon
              v-if="domain && domain.icon"
              :image="domain.icon.base64image"
              size="1x"
              style="margin-right: 5px"
            />
            <block-outlined v-else />
            {{ domain.path || domain.name || domain.description }}
          </span>
        </a-select-option>
      </a-select>
    </a-form-item>

    <template v-if="selectedAccountType === $t('label.account')">
      <a-form-item :label="$t('label.account')" required>
        <a-select
          @change="changeAccount"
          v-model:value="selectedAccount"
          showSearch
          optionFilterProp="label"
          :filterOption="
            (input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }
          "
        >
          <a-select-option v-for="account in accounts" :key="account.name" :value="account.name">
            <span>
              <resource-icon
                v-if="account && account.icon"
                :image="account.icon.base64image"
                size="1x"
                style="margin-right: 5px"
              />
              <team-outlined v-else />
              {{ account.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
    </template>

    <template v-else>
      <a-form-item :label="$t('label.project')" required>
        <a-select
          @change="changeProject"
          v-model:value="selectedProject"
          showSearch
          optionFilterProp="label"
          :filterOption="
            (input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }
          "
        >
          <a-select-option v-for="project in projects" :key="project.id" :value="project.id" :label="project.name">
            <span>
              <resource-icon
                v-if="project && project.icon"
                :image="project.icon.base64image"
                size="1x"
                style="margin-right: 5px"
              />
              <project-outlined v-else />
              {{ project.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
    </template>
  </a-form>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon.vue'

export default {
  name: 'OwnershipSelection',
  components: { ResourceIcon },
  data () {
    return {
      domains: [],
      accounts: [],
      projects: [],
      selectedAccountType: this.$t('label.account'),
      selectedDomain: null,
      selectedAccount: null,
      selectedProject: null,
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
      })
        .then((response) => {
          this.domains = response.listdomainsresponse.domain
          this.selectedDomain = this.domains[0].id
          this.fetchAccounts()
          this.fetchProjects()
        })
        .catch((error) => {
          this.$notifyError(error)
        })
        .finally(() => {
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
      })
        .then((response) => {
          this.accounts = response.listaccountsresponse.account
        })
        .catch((error) => {
          this.$notifyError(error)
        })
        .finally(() => {
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
      })
        .then((response) => {
          this.projects = response.listprojectsresponse.project
        })
        .catch((error) => {
          this.$notifyError(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    changeDomain () {
      this.selectedAccount = null
      this.selectedProject = null
      this.fetchAccounts()
      this.fetchProjects()
    },
    changeAccount () {
      this.selectedProject = null
      this.$emit('fetch-owner', this)
    },
    changeProject () {
      this.selectedAccount = null
      this.$emit('fetch-owner', this)
    }
  }
}
</script>
