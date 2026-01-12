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
        @change="changeAccountType"
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
        <a-select-option :value="'Account'">{{ $t('label.account') }}</a-select-option>
        <a-select-option :value="'Project'">{{ $t('label.project') }}</a-select-option>
      </a-select>
    </a-form-item>
    <a-form-item :label="$t('label.domain')" required>
      <infinite-scroll-select
        v-model:value="selectedDomain"
        api="listDomains"
        :apiParams="domainsApiParams"
        resourceType="domain"
        optionValueKey="id"
        optionLabelKey="path"
        defaultIcon="block-outlined"
        @change-option-value="handleDomainChange" />
    </a-form-item>

    <template v-if="selectedAccountType === 'Account'">
      <a-form-item :label="$t('label.account')" required>
        <infinite-scroll-select
          v-model:value="selectedAccount"
          api="listAccounts"
          :apiParams="accountsApiParams"
          resourceType="account"
          optionValueKey="name"
          optionLabelKey="name"
          defaultIcon="team-outlined"
          @change-option-value="handleAccountChange" />
      </a-form-item>
    </template>

    <template v-else>
      <a-form-item :label="$t('label.project')" required>
        <infinite-scroll-select
          v-model:value="selectedProject"
          api="listProjects"
          :apiParams="projectsApiParams"
          resourceType="project"
          optionValueKey="id"
          optionLabelKey="name"
          defaultIcon="project-outlined"
          @change-option-value="handleProjectChange" />
      </a-form-item>
    </template>
  </a-form>
</template>

<script>
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect.vue'

export default {
  name: 'OwnershipSelection',
  components: { InfiniteScrollSelect },
  data () {
    return {
      selectedAccountType: this.$store.getters.project?.id ? 'Project' : 'Account',
      selectedDomain: null,
      selectedAccount: null,
      selectedProject: null,
      selectedDomainOption: null,
      selectedAccountOption: null,
      selectedProjectOption: null
    }
  },
  props: {
    override: {
      type: Object
    }
  },
  computed: {
    domainsApiParams () {
      return {
        listAll: true,
        showicon: true,
        details: 'min'
      }
    },
    accountsApiParams () {
      if (!this.selectedDomain) {
        return null
      }
      return {
        domainid: this.selectedDomain,
        showicon: true,
        state: 'Enabled',
        isrecursive: false
      }
    },
    projectsApiParams () {
      if (!this.selectedDomain) {
        return null
      }
      return {
        domainId: this.selectedDomain,
        state: 'Active',
        showicon: true,
        details: 'min',
        isrecursive: false
      }
    }
  },
  created () {
    const ownerDomainId = this.$store.getters.project?.domainid || this.$store.getters.userInfo.domainid
    if (ownerDomainId) {
      this.selectedDomain = ownerDomainId
    }
  },
  methods: {
    changeAccountType () {
      this.selectedAccount = null
      this.selectedProject = null

      this.handleDomainChange(this.selectedDomain)
    },
    handleDomainChange (domainId) {
      this.selectedDomain = domainId
      this.selectedAccount = null
      this.selectedProject = null

      // Pre-select account if it's the user's domain
      if (this.selectedAccountType === 'Account' &&
          this.selectedDomain === this.$store.getters.userInfo.domainid) {
        this.selectedAccount = this.$store.getters.userInfo.account
      } else if (this.selectedAccountType === 'Project' &&
               this.$store.getters.project?.id &&
               this.selectedDomain === this.$store.getters.project?.domainid) {
        // Pre-select project if applicable
        this.selectedProject = this.$store.getters.project?.id
      }

      this.emitChangeEvent()
    },
    handleAccountChange (accountName) {
      this.selectedAccount = accountName
      this.selectedProject = null
      this.emitChangeEvent()
    },
    handleProjectChange (projectId) {
      this.selectedProject = projectId
      this.selectedAccount = null
      this.emitChangeEvent()
    },
    emitChangeEvent () {
      this.$emit('fetch-owner', this)
    }
  }
}
</script>
