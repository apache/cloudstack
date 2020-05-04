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
  <div class="form">
    <div v-if="loading" class="loading">
      <a-icon type="loading"></a-icon>
    </div>

    <div class="form__item">
      <p class="form__label">{{ $t('operation') }}</p>
      <a-select v-model="selectedOperation" defaultValue="Add" @change="fetchData">
        <a-select-option :value="$t('add')">{{ $t('add') }}</a-select-option>
        <a-select-option :value="$t('remove')">{{ $t('remove') }}</a-select-option>
        <a-select-option :value="$t('reset')">{{ $t('reset') }}</a-select-option>
      </a-select>
    </div>

    <template v-if="selectedOperation !== 'Reset'">
      <div class="form__item">
        <p class="form__label">
          <span class="required">*</span>
          {{ $t('shareWith') }}
        </p>
        <a-select v-model="selectedShareWith" defaultValue="Account" @change="fetchData">
          <a-select-option :value="$t('account')">{{ $t('account') }}</a-select-option>
          <a-select-option :value="$t('project')">{{ $t('project') }}</a-select-option>
        </a-select>
      </div>

      <template v-if="selectedShareWith === 'Account'">
        <div class="form__item">
          <p class="form__label">
            {{ $t('account') }}
          </p>
          <div v-if="showAccountSelect">
            <a-select
              mode="multiple"
              placeholder="Select Accounts"
              :value="selectedAccounts"
              @change="handleChange"
              style="width: 100%">
              <a-select-option v-for="account in accountsList" :key="account.name">
                {{ account.name }}</a-select-option>
            </a-select>
          </div>
          <div v-else>
            <a-input v-model="selectedAccountsList" placeholder="Enter comma-separated list of commands"></a-input>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="form__item">
          <p class="form__label">
            {{ $t('project') }}
          </p>
          <a-select
            mode="multiple"
            placeholder="Select Projects"
            :value="selectedProjects"
            @change="handleChange"
            style="width: 100%">
            <a-select-option v-for="project in projectsList" :key="project.name">
              {{ project.name }}</a-select-option>
          </a-select>
        </div>
      </template>
    </template>
    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('Cancel') }}
      </a-button>
      <a-button type="primary" @click="submitData">
        {{ $t('OK') }}
      </a-button>
    </div>
  </div>
</template>
<script>
import { api } from '@/api'

export default {
  name: 'UpdateTemplateIsoPermissions',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      projects: [],
      accounts: [],
      permittedAccounts: [],
      permittedProjects: [],
      selectedAccounts: [],
      selectedProjects: [],
      selectedAccountsList: '',
      selectedOperation: 'Add',
      selectedShareWith: this.$t('account'),
      accountError: false,
      projectError: false,
      showAccountSelect: true,
      loading: false,
      isImageTypeIso: false
    }
  },
  computed: {
    accountsList () {
      return this.accounts.length > 0 ? this.accounts
        .filter(a =>
          this.selectedOperation === 'Add'
            ? !this.permittedAccounts.includes(a.name)
            : this.permittedAccounts.includes(a.name)
        ) : this.accounts
    },
    projectsList () {
      return this.projects > 0 ? this.projects
        .filter(p =>
          this.selectedOperation === 'Add'
            ? !this.permittedProjects.includes(p.id)
            : this.permittedProjects.includes(p.id)
        ) : this.projects
    }
  },
  mounted () {
    this.isImageTypeIso = this.$route.meta.name === 'iso'
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (this.isImageTypeIso) {
        this.fetchIsoPermissions()
      } else {
        this.fetchTemplatePermissions()
      }
      if (this.selectedShareWith === 'Account') {
        this.selectedAccounts = []
        this.fetchAccounts()
      } else {
        this.selectedProjects = []
        this.fetchProjects()
      }
      this.showAccountSelect = this.$store.getters.features.allowuserviewalldomainaccounts || !(this.$store.getters.userInfo.roletype === 'User')
    },
    fetchAccounts () {
      this.loading = true
      api('listAccounts', {
        listall: true
      }).then(response => {
        this.accounts = response.listaccountsresponse.account.filter(account => account.name !== this.resource.account)
      }).finally(e => {
        this.loading = false
      })
    },
    fetchProjects () {
      api('listProjects', {
        details: 'min',
        listall: true
      }).then(response => {
        this.projects = response.listprojectsresponse.project
      }).finally(e => {
        this.loading = false
      })
    },
    fetchTemplatePermissions () {
      this.loading = true
      api('listTemplatePermissions', {
        id: this.resource.id
      }).then(response => {
        const permission = response.listtemplatepermissionsresponse.templatepermission
        if (permission && permission.account) {
          this.permittedAccounts = permission.account
        }
        if (permission && permission.projectids) {
          this.permittedProjects = permission.projectids
        }
      }).finally(e => {
        this.loading = false
      })
    },
    fetchIsoPermissions () {
      this.loading = true
      api('listIsoPermissions', {
        id: this.resource.id
      }).then(response => {
        const permission = response.listtemplatepermissionsresponse.templatepermission
        if (permission && permission.account) {
          this.permittedAccounts = permission.account
        }
        if (permission && permission.projectids) {
          this.permittedProjects = permission.projectids
        }
      }).finally(e => {
        this.loading = false
      })
    },
    handleChange (selectedItems) {
      if (this.selectedOperation === 'Add' || this.selectedOperation === 'Remove') {
        if (this.selectedShareWith === 'Account') {
          this.selectedAccounts = selectedItems
        } else {
          this.selectedProjects = selectedItems
        }
      }
    },
    closeModal () {
      this.$parent.$parent.close()
    },
    submitData () {
      let variableKey = ''
      let variableValue = ''
      if (this.selectedShareWith === 'Account') {
        variableKey = 'accounts'
        if (this.showAccountSelect) {
          variableValue = this.selectedAccounts.map(account => account).join(',')
        } else {
          variableValue = this.selectedAccountsList
        }
      } else {
        variableKey = 'projectids'
        variableValue = this.projects.filter(p => this.selectedProjects.includes(p.name)).map(p => p.id).join(',')
      }
      this.loading = true
      const apiName = this.isImageTypeIso ? 'updateIsoPermissions' : 'updateTemplatePermissions'
      const resourceType = this.isImageTypeIso ? 'ISO' : 'template'
      api(apiName, {
        [variableKey]: variableValue,
        id: this.resource.id,
        ispublic: this.resource.isPublic,
        isextractable: this.resource.isExtractable,
        featured: this.resource.featured,
        op: this.selectedOperation.toLowerCase()
      })
        .then(response => {
          this.$notification.success({
            message: 'Successfully updated ' + resourceType + ' permissions'
          })
        })
        .catch(error => {
          this.$notification.error({
            message: 'Failed to update ' + resourceType + ' permissions',
            description: this.isImageTypeIso
              ? error.response.data.updateisopermissions.errortext
              : error.response.data.updatetemplatepermissions.errortext
          })
        })
        .finally(e => {
          this.loading = false
          this.closeModal()
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
    width: 80vw;

    @media (min-width: 700px) {
      width: 500px;
    }

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
  .actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
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
