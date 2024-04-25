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
    <a-form-item :label="$t('label.owner.type')">
      <a-select
        @change="changeDomain"
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
          @change="emitChangeEvent"
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
          @change="emitChangeEvent"
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
      selectedAccountType: this.$store.getters.project?.id ? this.$t('label.project') : this.$t('label.account'),
      selectedDomain: null,
      selectedAccount: null,
      selectedProject: null,
      loading: false
    }
  },
  props: {
    override: {
      type: Object
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
          if (this.override) {
            this.domains = this.domains.filter(item => this.override.domains.has(item.id))
          }
          if (this.domains.length === 0) {
            this.selectedDomain = null
            this.selectedProject = null
            this.selectedAccount = null
            return
          }
          const domainIds = this.domains?.map(domain => domain.id)
          const ownerDomainId = this.$store.getters.project?.domainid || this.$store.getters.userInfo.domainid
          this.selectedDomain = domainIds?.includes(ownerDomainId) ? ownerDomainId : this.domains?.[0]?.id
          this.changeDomain()
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
          this.accounts = response.listaccountsresponse.account || []
          if (this.override?.accounts && this.accounts) {
            this.accounts = this.accounts.filter(item => this.override.accounts.has(item.name))
          }
          const accountNames = this.accounts.map(account => account.name)
          if (this.selectedDomain === this.$store.getters.userInfo.domainid && accountNames.includes(this.$store.getters.userInfo.account)) {
            this.selectedAccount = this.$store.getters.userInfo.account
          } else {
            this.selectedAccount = this.accounts?.[0]?.name
          }
          this.selectedProject = null
          this.emitChangeEvent()
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
          if (this.override?.projects && this.projects) {
            this.projects = this.projects.filter(item => this.override.projects.has(item.id))
          }
          this.selectedProject = this.projects?.map(project => project.id)?.includes(this.$store.getters.project?.id) ? this.$store.getters.project?.id : this.projects?.[0]?.id
          this.selectedAccount = null
          this.emitChangeEvent()
        })
        .catch((error) => {
          this.$notifyError(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    changeDomain () {
      if (this.selectedAccountType === this.$t('label.account')) {
        this.fetchAccounts()
      } else {
        this.fetchProjects()
      }
    },
    emitChangeEvent () {
      this.$emit('fetch-owner', this)
    }
  }
}
</script>
