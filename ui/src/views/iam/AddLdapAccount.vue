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
  <div class="ldap-account-layout" v-ctrl-enter="handleSubmit">
    <a-row :gutter="0">
      <a-col :md="24" :lg="16">
        <a-card :bordered="false">
          <a-input-search
            style="margin-bottom: 10px"
            :placeholder="$t('label.search')"
            v-model:value="searchQuery"
            @search="handleSearch" />
          <a-table
            size="small"
            :loading="listLoading"
            :columns="columns"
            :dataSource="dataSource"
            :rowSelection="{
              columnWidth: 50,
              selectedRowKeys: selectedRowKeys,
              onChange: onSelectChange
            }"
            :rowKey="record => record.username"
            :rowClassName="getRowClassName"
            :pagination="false"
            style="overflow-y: auto"
            :scroll="{ y: '50vh' }"
          />
        </a-card>
      </a-col>
      <a-col :md="24" :lg="8">
        <a-card :bordered="false">
          <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleSubmit"
            layout="vertical">
            <a-form-item :label="$t('label.filterby')" name="selectedFilter" ref="selectedFilter">
              <a-select
                @change="fetchListLdapUsers"
                v-model:value="selectedFilter"
                v-focus="true"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }">
                <a-select-option v-for="opt in filters" :key="opt.id" :label="opt.name">
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('label.domain')" ref="domainid" name="domainid">
              <a-select
                v-model:value="form.domainid"
                :placeholder="apiParams.domainid.description"
                :loading="domainLoading"
                @change="fetchListLdapUsers($event)"
                showSearch
                optionFilterProp="value"
                :filterOption="(input, option) => {
                  return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="opt in listDomains" :key="opt.name">
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('label.account')" ref="account" name="account">
              <a-input
                v-model:value="form.account"
                :placeholder="apiParams.account.description"
              />
            </a-form-item>
            <a-form-item :label="$t('label.role')" ref="roleid" name="roleid">
              <a-select
                v-model:value="form.roleid"
                :placeholder="apiParams.roleid.description"
                :loading="roleLoading"
                showSearch
                optionFilterProp="value"
                :filterOption="(input, option) => {
                  return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }">
                <a-select-option v-for="opt in listRoles" :key="opt.name">
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('label.timezone')" ref="timezone" name="timezone">
              <a-select
                v-model:value="form.timezone"
                :placeholder="apiParams.timezone.description"
                :loading="timeZoneLoading"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }">
                <a-select-option v-for="opt in timeZoneMap" :key="opt.id" :label="opt.name || opt.description">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('label.networkdomain')" ref="networkdomain" name="networkdomain">
              <a-input
                v-model:value="form.networkdomain"
                :placeholder="apiParams.networkdomain.description"
              />
            </a-form-item>
            <a-form-item :label="$t('label.ldap.group.name')" ref="group" name="group">
              <a-input
                v-model:value="form.group"
                :placeholder="apiParams.group.description"
              />
            </a-form-item>
            <div v-if="'authorizeSamlSso' in $store.getters.apis" ref="samlEnable" name="samlEnable">
              <a-form-item :label="$t('label.samlenable')">
                <a-switch v-model:checked="form.samlEnable" @change="handleEntityRule" />
              </a-form-item>
              <a-form-item v-if="form.samlEnable" :label="$t('label.samlentity')" ref="samlEntity" name="samlEntity">
                <a-select
                  v-model:value="form.samlEntity"
                  :placeholder="$t('label.choose.saml.identity')"
                  :loading="loading"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }">
                  <a-select-option v-for="(idp, idx) in listIdps" :key="idx" :label="idp.orgName">
                    {{ idp.orgName }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </div>

            <div class="action-button">
              <a-button @click="handleClose">{{ $t('label.close') }}</a-button>
              <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.add') }}</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { timeZone } from '@/utils/timezone'
import store from '@/store'

export default {
  name: 'AddLdapAccount',
  data () {
    return {
      columns: [],
      dataSource: [],
      oldDataSource: [],
      selectedRowKeys: [],
      listDomains: [],
      listRoles: [],
      timeZoneMap: [],
      listIdps: [],
      filters: [],
      selectedFilter: '',
      listLoading: false,
      timeZoneLoading: false,
      domainLoading: false,
      roleLoading: false,
      loading: false,
      searchQuery: undefined,
      samlEnable: false
    }
  },
  beforeCreate () {
    this.apiLdapCreateAccountConfig = this.$store.getters.apis.ldapCreateAccount || {}
    this.apiImportLdapUsersConfig = this.$store.getters.apis.importLdapUsers || {}
    this.apiParams = {}
    this.apiLdapCreateAccountConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
    this.apiImportLdapUsersConfig.params.forEach(param => {
      if (!this.apiParams || !this.apiParams[param.name]) {
        this.apiParams[param.name] = param
      }
    })
  },
  created () {
    this.selectedRowKeys = []
    this.dataSource = []
    this.listDomains = []
    this.listRoles = []
    this.listIdps = []
    this.columns = [
      {
        key: 'name',
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: 120
      },
      {
        key: 'username',
        title: this.$t('label.username'),
        dataIndex: 'username',
        width: 120
      },
      {
        key: 'email',
        title: this.$t('label.email'),
        dataIndex: 'email'
      },
      {
        key: 'conflictingusersource',
        title: this.$t('label.user.conflict'),
        dataIndex: 'conflictingusersource'
      }
    ]
    this.filters = [
      {
        id: 'NoFilter',
        name: 'No filter'
      },
      {
        id: 'LocalDomain',
        name: 'Local domain'
      },
      {
        id: 'AnyDomain',
        name: 'Any domain'
      },
      {
        id: 'PotentialImport',
        name: 'Potential import'
      }
    ]
    this.selectedFilter = this.filters[0].id
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        domainid: [{ required: true, message: this.$t('message.error.select') }],
        roleid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    async fetchData () {
      this.timeZoneLoading = true
      this.domainLoading = true
      this.roleLoading = true
      this.fetchListLdapUsers()
      const [
        listTimeZone,
        listDomains,
        listRoles,
        listIdps
      ] = await Promise.all([
        this.fetchTimeZone(),
        this.fetchListDomains(),
        this.fetchListRoles(),
        ('listIdps' in this.$store.getters.apis) ? this.fetchIdps() : []
      ]).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.timeZoneLoading = false
        this.domainLoading = false
        this.roleLoading = false
      })
      this.timeZoneMap = listTimeZone && listTimeZone.length > 0 ? listTimeZone : []
      this.listDomains = listDomains && listDomains.length > 0 ? listDomains : []
      this.listRoles = listRoles && listRoles.length > 0 ? listRoles : []
      this.listIdps = listIdps && listIdps.length > 0 ? listIdps : []
    },
    fetchTimeZone (value) {
      return new Promise((resolve, reject) => {
        timeZone(value).then(json => {
          resolve(json)
        }).catch(error => {
          reject(error)
        })
      })
    },
    fetchListLdapUsers (domain) {
      this.listLoading = true
      const params = {}
      params.listtype = 'new'
      params.userfilter = this.selectedFilter
      params.domainid = store.getters.userInfo.domainid
      if (domain) {
        const result = this.listDomains.filter(item => item.name === domain)
        if (result.length > 0) {
          params.domainid = result[0].id
        }
      }
      api('listLdapUsers', params).then(json => {
        const listLdapUsers = json.ldapuserresponse.LdapUser
        if (listLdapUsers) {
          const ldapUserLength = listLdapUsers.length
          for (let i = 0; i < ldapUserLength; i++) {
            listLdapUsers[i].name = [listLdapUsers[i].firstname, listLdapUsers[i].lastname].join(' ')
          }
        }
        this.dataSource = listLdapUsers
        this.oldDataSource = listLdapUsers
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.listLoading = false
      })
    },
    fetchListDomains () {
      return new Promise((resolve, reject) => {
        const params = {}
        api('listDomains', params).then(json => {
          const listDomains = json.listdomainsresponse.domain
          resolve(listDomains)
        }).catch(error => {
          reject(error)
        })
      })
    },
    fetchListRoles () {
      return new Promise((resolve, reject) => {
        const params = {}
        api('listRoles', params).then(json => {
          const listRoles = json.listrolesresponse.role
          resolve(listRoles)
        }).catch(error => {
          reject(error)
        })
      })
    },
    fetchIdps () {
      return new Promise((resolve, reject) => {
        api('listIdps').then(json => {
          const listIdps = json.listidpsresponse.idp || []
          if (listIdps.length !== 0) {
            this.form.samlEntity = listIdps[0].id
          }
          resolve(listIdps)
        }).catch(error => {
          reject(error)
        })
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        let apiName = 'ldapCreateAccount'
        const domain = this.listDomains.filter(item => item.name === values.domainid)
        const role = this.listRoles.filter(item => item.name === values.roleid)
        const promises = []
        const params = {}
        params.domainid = domain[0].id
        params.roleid = role[0].id
        params.account = values.account
        params.timezone = values.timezone
        params.networkdomain = values.networkdomain
        if (values.group && values.group.trim().length > 0) {
          params.group = values.group
          apiName = 'importLdapUsers'
          promises.push(new Promise((resolve, reject) => {
            api(apiName, params).then(json => {
              resolve(json)
            }).catch(error => {
              reject(error)
            })
          }))
        } else {
          this.selectedRowKeys.forEach(username => {
            params.username = username
            promises.push(new Promise((resolve, reject) => {
              api(apiName, params).then(json => {
                resolve(json)
              }).catch(error => {
                reject(error)
              })
            }))
          })
        }
        this.loading = true
        Promise.all(promises).then(responses => {
          for (const response of responses) {
            if (apiName === 'ldapCreateAccount' && values.samlEnable) {
              const users = response.createaccountresponse.account.user
              const entityId = values.samlEntity
              if (users && entityId) {
                this.authorizeUsersForSamlSSO(users, entityId)
              }
            } else if (apiName === 'importLdapUsers' && response.ldapuserresponse && values.samlEnable) {
              this.$notification.error({
                message: this.$t('message.request.failed'),
                description: this.$t('message.error.enable.saml')
              })
            } else {
              if (apiName === 'ldapCreateAccount') {
                this.$notification.success({
                  message: this.$t('label.add.ldap.account'),
                  description: response.createaccountresponse.account.name
                })
              }
            }
          }
          this.$emit('refresh-data')
          this.handleClose()
        }).catch(error => {
          this.$notifyError(error)
          this.$emit('refresh-data')
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleSearch () {
      this.dataSource = this.oldDataSource
      if (!this.searchQuery || this.searchQuery.length === 0) {
        return
      }
      this.dataSource = this.dataSource.filter(item => this.filterLdapUser(item))
    },
    filterLdapUser (item) {
      switch (true) {
        case (item.name && item.name.toLowerCase().indexOf(this.searchQuery.toLowerCase()) > -1):
          return item
        case (item.username && item.username.toLowerCase().indexOf(this.searchQuery.toLowerCase()) > -1):
          return item
        case (item.email && item.email.toLowerCase().indexOf(this.searchQuery.toLowerCase()) > -1):
          return item
        default:
          break
      }
    },
    handleClose () {
      this.$emit('close-action')
    },
    authorizeUsersForSamlSSO (users, entityId) {
      const promises = []
      for (var i = 0; i < users.length; i++) {
        const params = {}
        params.enable = true
        params.userid = users[i].id
        params.entityid = entityId
        promises.push(new Promise((resolve, reject) => {
          api('authorizeSamlSso', params).catch(error => {
            reject(error)
          })
        }))
      }
      Promise.all(promises).catch(error => {
        this.$notifyError(error)
      })
    },
    onSelectChange (selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    },
    handleEntityRule () {
      if (this.form.samlEnable) {
        this.rules.samlEntity = [{ required: true, message: `${this.$t('message.error.select')}` }]
      } else {
        delete this.rules.samlEntity
      }
    }
  }
}
</script>

<style lang="less" scoped>
.ldap-account-layout {
  width: 85vw;

  @media (min-width: 1000px) {
    width: 900px;
  }
}

.card-footer {
  text-align: right;

  button + button {
    margin-left: 8px;
  }
}
</style>
