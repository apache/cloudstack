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
  <div class="ldap-account-layout">
    <a-row :gutter="0">
      <a-col :md="24" :lg="16">
        <a-card :bordered="false">
          <a-input-search
            style="margin-bottom: 10px"
            placeholder="Search"
            v-model="searchQuery"
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
            :form="form"
            @submit="handleSubmit"
            layout="vertical"
          >
            <a-form-item :label="$t('domain')">
              <a-select
                showSearch
                v-decorator="['domainid', {
                  rules: [{ required: true, message: 'Please select option' }]
                }]"
                :placeholder="apiParams.domainid.description"
                :loading="domainLoading">
                <a-select-option v-for="opt in listDomains" :key="opt.name">
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('account')">
              <a-input
                v-decorator="['account']"
                :placeholder="apiParams.account.description"
              />
            </a-form-item>
            <a-form-item :label="$t('role')">
              <a-select
                showSearch
                v-decorator="['roleid', {
                  rules: [{ required: true, message: 'Please select option' }]
                }]"
                :placeholder="apiParams.roleid.description"
                :loading="roleLoading">
                <a-select-option v-for="opt in listRoles" :key="opt.name">
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('timezone')">
              <a-select
                showSearch
                v-decorator="['timezone']"
                :placeholder="apiParams.timezone.description"
                :loading="timeZoneLoading">
                <a-select-option v-for="opt in timeZoneMap" :key="opt.id">
                  {{ opt.name || opt.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="$t('networkdomain')">
              <a-input
                v-decorator="['networkdomain']"
                :placeholder="apiParams.networkdomain.description"
              />
            </a-form-item>
            <a-form-item :label="$t('group')">
              <a-input
                v-decorator="['group']"
                :placeholder="apiParams.group.description"
              />
            </a-form-item>
            <div class="card-footer">
              <a-button @click="handleClose">{{ $t('Close') }}</a-button>
              <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('add') }}</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'
import { timeZone } from '@/utils/timezone'

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
      listLoading: false,
      timeZoneLoading: false,
      domainLoading: false,
      roleLoading: false,
      loading: false,
      searchQuery: undefined
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
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
    this.columns = [
      {
        title: this.$t('name'),
        dataIndex: 'name',
        width: 120,
        scopedSlots: { customRender: 'name' }
      },
      {
        title: this.$t('username'),
        dataIndex: 'username',
        width: 120,
        scopedSlots: { customRender: 'username' }
      },
      {
        title: this.$t('email'),
        dataIndex: 'email',
        scopedSlots: { customRender: 'email' }
      }
    ]
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    async fetchData () {
      this.listLoading = true
      this.timeZoneLoading = true
      this.domainLoading = true
      this.roleLoading = true
      const [
        listTimeZone,
        listLdapUsers,
        listDomains,
        listRoles
      ] = await Promise.all([
        this.fetchTimeZone(),
        this.fetchListLdapUsers(),
        this.fetchListDomains(),
        this.fetchListRoles()
      ]).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.listLoading = false
        this.timeZoneLoading = false
        this.domainLoading = false
        this.roleLoading = false
      })
      this.timeZoneMap = listTimeZone && listTimeZone.length > 0 ? listTimeZone : []
      this.listDomains = listDomains && listDomains.length > 0 ? listDomains : []
      this.listRoles = listRoles && listRoles.length > 0 ? listRoles : []
      this.dataSource = listLdapUsers
      this.oldDataSource = listLdapUsers
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
    fetchListLdapUsers () {
      return new Promise((resolve, reject) => {
        const params = {}
        params.listtype = 'new'
        api('listLdapUsers', params).then(json => {
          const listLdapUsers = json.ldapuserresponse.LdapUser
          if (listLdapUsers) {
            const ldapUserLength = listLdapUsers.length
            for (let i = 0; i < ldapUserLength; i++) {
              listLdapUsers[i].name = [listLdapUsers[i].firstname, listLdapUsers[i].lastname].join(' ')
            }
          }
          resolve(listLdapUsers)
        }).catch(error => {
          reject(error)
        })
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
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err || this.selectedRowKeys.length === 0) {
          return
        }
        let apiName = 'ldapCreateAccount'
        const domain = this.listDomains.filter(item => item.name === values.domainid)
        const role = this.listRoles.filter(item => item.name === values.roleid)
        const promises = []
        const params = {}
        params.domainid = domain[0].id
        params.roleid = role[0].id
        params.timezone = values.timezone
        params.networkdomain = values.networkdomain
        params.group = values.group
        if (params.group && params.group.trim().length > 0) {
          apiName = 'importLdapUsers'
        }
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
        this.loading = true
        Promise.all(promises).then(response => {
          for (let i = 0; i < response.length; i++) {
            if (apiName === 'ldapCreateAccount' && values.samlEnable) {
              const users = response.createaccountresponse.account.user
              const entity = values.samlEntity
              if (users && entity) {
                this.authorizeUsersForSamlSSO(users, entity)
              }
            } else if (apiName === 'importLdapUsers' && response.ldapuserresponse && values.samlEnable) {
              this.$notification.error({
                message: 'Request Failed',
                description: 'Unable to find users IDs to enable SAML Single Sign On, kindly enable it manually.'
              })
            } else {
              if (apiName === 'ldapCreateAccount') {
                this.$notification.success({
                  message: this.$t('label.add.ldap.account'),
                  description: response[i].createaccountresponse.account.name
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
    authorizeUsersForSamlSSO (users, entity) {
      const promises = []
      for (var i = 0; i < users.length; i++) {
        const params = {}
        params.enable = true
        params.userid = users[i].id
        params.entityid = entity
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

/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}

</style>
