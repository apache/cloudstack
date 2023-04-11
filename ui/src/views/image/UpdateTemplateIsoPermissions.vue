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
  <div class="form" v-ctrl-enter="submitData">
    <div v-if="loading" class="loading">
      <loading-outlined />
    </div>

    <div class="form__item">
      <p class="form__label">{{ $t('label.operation') }}</p>
      <a-select
        v-model:value="selectedOperation"
        :defaultValue="$t('label.add')"
        @change="fetchData"
        v-focus="true"
        showSearch
        optionFilterProp="value"
        :filterOption="(input, option) => {
          return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }" >
        <a-select-option :value="$t('label.add')">{{ $t('label.add') }}</a-select-option>
        <a-select-option :value="$t('label.remove')">{{ $t('label.remove') }}</a-select-option>
        <a-select-option :value="$t('label.reset')">{{ $t('label.reset') }}</a-select-option>
      </a-select>
    </div>

    <template v-if="selectedOperation !== $t('label.reset')">
      <div class="form__item">
        <p class="form__label">
          <span class="required">*</span>
          {{ $t('label.sharewith') }}
        </p>
        <a-select
          v-model:value="selectedShareWith"
          :defaultValue="$t('label.account')"
          @change="fetchData"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }">
          <a-select-option :value="$t('label.account')">{{ $t('label.account') }}</a-select-option>
          <a-select-option :value="$t('label.project')">{{ $t('label.project') }}</a-select-option>
        </a-select>
      </div>

      <template v-if="selectedShareWith === $t('label.account')">
        <div class="form__item">
          <p class="form__label">
            {{ $t('label.account') }}
          </p>
          <div v-if="showAccountSelect">
            <a-select
              mode="multiple"
              placeholder="Select Accounts"
              v-model:value="selectedAccounts"
              @change="handleChange"
              style="width: 100%"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="account in accountsList" :key="account.name" :label="account.name">
                <span>
                  <resource-icon v-if="account.icon" :image="account.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <team-outlined v-else style="margin-right: 5px" />
                  {{ account.name }}
                </span>
              </a-select-option>
            </a-select>
          </div>
          <div v-else>
            <a-input v-model:value="selectedAccountsList" :placeholder="$t('label.comma.separated.list.description')"></a-input>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="form__item">
          <p class="form__label">
            {{ $t('label.project') }}
          </p>
          <a-select
            mode="multiple"
            :placeholder="$t('label.select.projects')"
            v-model:value="selectedProjects"
            @change="handleChange"
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="project in projectsList" :key="project.name" :label="project.name">
              <span>
                <resource-icon v-if="project.icon" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
                <project-outlined v-else style="margin-right: 5px" />
                {{ project.name }}
              </span>
            </a-select-option>
          </a-select>
        </div>
      </template>
    </template>

    <div :span="24" class="action-button">
      <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" ref="submit" @click="submitData">{{ $t('label.ok') }}</a-button>
    </div>
  </div>
</template>
<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'UpdateTemplateIsoPermissions',
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
      projects: [],
      accounts: [],
      permittedAccounts: [],
      permittedProjects: [],
      selectedAccounts: [],
      selectedProjects: [],
      selectedAccountsList: '',
      selectedOperation: this.$t('label.add'),
      selectedShareWith: this.$t('label.account'),
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
          this.selectedOperation === this.$t('label.add')
            ? !this.permittedAccounts.includes(a.name)
            : this.permittedAccounts.includes(a.name)
        ) : this.accounts
    },
    projectsList () {
      return this.projects > 0 ? this.projects
        .filter(p =>
          this.selectedOperation === this.$t('label.add')
            ? !this.permittedProjects.includes(p.id)
            : this.permittedProjects.includes(p.id)
        ) : this.projects
    }
  },
  created () {
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
      if (this.selectedShareWith === this.$t('label.account')) {
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
        domainid: this.resource.domainid,
        showicon: true
      }).then(response => {
        this.accounts = response.listaccountsresponse.account.filter(account => account.name !== this.resource.account)
      }).finally(e => {
        this.loading = false
      })
    },
    fetchProjects () {
      api('listProjects', {
        details: 'min',
        showicon: true,
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
      if (this.selectedOperation === this.$t('label.add') || this.selectedOperation === this.$t('label.remove')) {
        if (this.selectedShareWith === this.$t('label.account')) {
          this.selectedAccounts = selectedItems
        } else {
          this.selectedProjects = selectedItems
        }
      }
    },
    closeModal () {
      this.$emit('close-action')
    },
    submitData () {
      if (this.loading) return
      let variableKey = ''
      let variableValue = ''
      if (this.selectedShareWith === this.$t('label.account')) {
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
      }).then(response => {
        this.$notification.success({
          message: `${this.$t('label.success.updated')} ${resourceType} ${this.$t('label.permissions')}`
        })
        this.closeModal()
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(e => {
        this.loading = false
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
