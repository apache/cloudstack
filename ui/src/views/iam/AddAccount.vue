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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form :form="form" :loading="loading" @submit="handleSubmit" layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.role')" :tooltip="apiParams.roleid.description"/>
          <a-select
            v-decorator="['roleid', {
              initialValue: selectedRole,
              rules: [{ required: true, message: $t('message.error.select') }] }]"
            :loading="roleLoading"
            :placeholder="apiParams.roleid.description"
            autoFocus>
            <a-select-option v-for="role in roles" :key="role.id">
              {{ role.name + ' (' + role.type + ')' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.username')" :tooltip="apiParams.username.description"/>
          <a-input
            v-decorator="['username', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.username.description" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.password')" :tooltip="apiParams.password.description"/>
              <a-input-password
                v-decorator="['password', {
                  rules: [{ required: true, message: $t('message.error.required.input') }]
                }]"
                :placeholder="apiParams.password.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.confirmpassword')" :tooltip="apiParams.password.description"/>
              <a-input-password
                v-decorator="['confirmpassword', {
                  rules: [
                    { required: true, message: $t('message.error.required.input') },
                    { validator: validateConfirmPassword }
                  ]
                }]"
                :placeholder="apiParams.password.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.email')" :tooltip="apiParams.email.description"/>
          <a-input
            v-decorator="['email', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.email.description" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.firstname')" :tooltip="apiParams.firstname.description"/>
              <a-input
                v-decorator="['firstname', {
                  rules: [{ required: true, message: $t('message.error.required.input') }]
                }]"
                :placeholder="apiParams.firstname.description" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item>
              <tooltip-label slot="label" :title="$t('label.lastname')" :tooltip="apiParams.lastname.description"/>
              <a-input
                v-decorator="['lastname', {
                  rules: [{ required: true, message: $t('message.error.required.input') }]
                }]"
                :placeholder="apiParams.lastname.description" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="this.isAdminOrDomainAdmin()">
          <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          <a-select
            :loading="domainLoading"
            v-decorator="['domainid', {
              initialValue: selectedDomain,
              rules: [{ required: true, message: $t('message.error.select') }] }]"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="domain in domainsList" :key="domain.id">
              {{ domain.path || domain.name || domain.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          <a-input v-decorator="['account']" :placeholder="apiParams.account.description" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.timezone')" :tooltip="apiParams.timezone.description"/>
          <a-select
            showSearch
            v-decorator="['timezone']"
            :loading="timeZoneLoading"
            :placeholder="apiParams.timezone.description">
            <a-select-option v-for="opt in timeZoneMap" :key="opt.id">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
          <a-input
            v-decorator="['networkdomain']"
            :placeholder="apiParams.networkdomain.description" />
        </a-form-item>
        <div v-if="'authorizeSamlSso' in $store.getters.apis">
          <a-form-item :label="$t('label.samlenable')">
            <a-switch v-decorator="['samlenable']" @change="checked => { this.samlEnable = checked }" />
          </a-form-item>
          <a-form-item v-if="samlEnable">
            <tooltip-label slot="label" :title="$t('label.samlentity')" :tooltip="apiParams.entityid.description"/>
            <a-select
              v-decorator="['samlentity', {
                initialValue: selectedIdp,
              }]"
              :loading="idpLoading">
              <a-select-option v-for="(idp, idx) in idps" :key="idx">
                {{ idp.orgName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>
<script>
import { api } from '@/api'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddAccountForm',
  components: {
    TooltipLabel
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)
    return {
      loading: false,
      domainLoading: false,
      domainsList: [],
      selectedDomain: '',
      roleLoading: false,
      roles: [],
      selectedRole: '',
      timeZoneLoading: false,
      timeZoneMap: [],
      samlEnable: false,
      idpLoading: false,
      idps: [],
      selectedIdp: ''
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createAccount', 'authorizeSamlSso')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchDomains()
      this.fetchRoles()
      this.fetchTimeZone()
      if ('listIdps' in this.$store.getters.apis) {
        this.fetchIdps()
      }
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isDomainAdmin () {
      return this.$store.getters.userInfo.roletype === 'DomainAdmin'
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    validateConfirmPassword (rule, value, callback) {
      if (!value || value.length === 0) {
        callback()
      } else if (rule.field === 'confirmpassword') {
        const form = this.form
        const messageConfirm = this.$t('error.password.not.match')
        const passwordVal = form.getFieldValue('password')
        if (passwordVal && passwordVal !== value) {
          callback(messageConfirm)
        } else {
          callback()
        }
      } else {
        callback()
      }
    },
    fetchDomains () {
      this.domainLoading = true
      api('listDomains', {
        listAll: true,
        details: 'min'
      }).then(response => {
        this.domainsList = response.listdomainsresponse.domain || []
        this.selectedDomain = this.domainsList[0].id || ''
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchRoles () {
      this.roleLoading = true
      api('listRoles').then(response => {
        this.roles = response.listrolesresponse.role || []
        this.selectedRole = this.roles[0].id
        if (this.isDomainAdmin()) {
          const userRole = this.roles.filter(role => role.type === 'User')
          if (userRole.length > 0) {
            this.selectedRole = userRole[0].id
          }
        }
      }).finally(() => {
        this.roleLoading = false
      })
    },
    fetchTimeZone (value) {
      this.timeZoneMap = []
      this.timeZoneLoading = true

      timeZone(value).then(json => {
        this.timeZoneMap = json
        this.timeZoneLoading = false
      })
    },
    fetchIdps () {
      this.idpLoading = true
      api('listIdps').then(response => {
        this.idps = response.listidpsresponse.idp || []
        this.selectedIdp = this.idps[0].id || ''
      }).finally(() => {
        this.idpLoading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {
          roleid: values.roleid,
          username: values.username,
          password: values.password,
          email: values.email,
          firstname: values.firstname,
          lastname: values.lastname,
          domainid: values.domainid
        }
        if (this.isValidValueForKey(values, 'account') && values.account.length > 0) {
          params.account = values.account
        }
        if (this.isValidValueForKey(values, 'timezone') && values.timezone.length > 0) {
          params.timezone = values.timezone
        }
        if (this.isValidValueForKey(values, 'networkdomain') && values.networkdomain.length > 0) {
          params.networkdomain = values.networkdomain
        }

        api('createAccount', {}, 'POST', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.create.account'),
            description: `${this.$t('message.success.create.account')} ${params.username}`
          })
          const users = response.createaccountresponse.account.user
          if (values.samlenable && users) {
            for (var i = 0; i < users.length; i++) {
              api('authorizeSamlSso', {
                enable: values.samlenable,
                entityid: values.samlentity,
                userid: users[i].id
              }).then(response => {
                this.$notification.success({
                  message: this.$t('samlenable'),
                  description: this.$t('message.success.enable.saml.auth')
                })
              }).catch(error => {
                this.$notification.error({
                  message: this.$t('message.request.failed'),
                  description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
                  duration: 0
                })
              })
            }
          }
          this.closeAction()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>
<style scoped lang="less">
  .form-layout {
    width: 80vw;
    @media (min-width: 600px) {
      width: 450px;
    }
  }
</style>
