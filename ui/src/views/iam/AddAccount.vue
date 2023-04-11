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
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        :loading="loading"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item ref="roleid" name="roleid">
          <template #label>
            <tooltip-label :title="$t('label.role')" :tooltip="apiParams.roleid.description"/>
          </template>
          <a-select
            v-model:value="form.roleid"
            :loading="roleLoading"
            :placeholder="apiParams.roleid.description"
            v-focus="true"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option v-for="role in roles" :key="role.id" :label="role.name + ' (' + role.type + ')'">
              {{ role.name + ' (' + role.type + ')' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="username" name="username">
          <template #label>
            <tooltip-label :title="$t('label.username')" :tooltip="apiParams.username.description"/>
          </template>
          <a-input
            v-model:value="form.username"
            :placeholder="apiParams.username.description" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item ref="password" name="password">
              <template #label>
                <tooltip-label :title="$t('label.password')" :tooltip="apiParams.password.description"/>
              </template>
              <a-input-password
                v-model:value="form.password"
                :placeholder="apiParams.password.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item ref="confirmpassword" name="confirmpassword">
              <template #label>
                <tooltip-label :title="$t('label.confirmpassword')" :tooltip="apiParams.password.description"/>
              </template>
              <a-input-password
                v-model:value="form.confirmpassword"
                :placeholder="apiParams.password.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item ref="email" name="email">
          <template #label>
            <tooltip-label :title="$t('label.email')" :tooltip="apiParams.email.description"/>
          </template>
          <a-input
            v-model:value="form.email"
            :placeholder="apiParams.email.description" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item ref="firstname" name="firstname">
              <template #label>
                <tooltip-label :title="$t('label.firstname')" :tooltip="apiParams.firstname.description"/>
              </template>
              <a-input
                v-model:value="form.firstname"
                :placeholder="apiParams.firstname.description" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="12">
            <a-form-item ref="lastname" name="lastname">
              <template #label>
                <tooltip-label :title="$t('label.lastname')" :tooltip="apiParams.lastname.description"/>
              </template>
              <a-input
                v-model:value="form.lastname"
                :placeholder="apiParams.lastname.description" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="isAdminOrDomainAdmin()" ref="domainid" name="domainid">
          <template #label>
            <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          </template>
          <a-select
            :loading="domainLoading"
            v-model:value="form.domainid"
            :placeholder="apiParams.domainid.description"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="domain in domainsList" :key="domain.id" :label="domain.path || domain.name || domain.description">
              <span>
                <resource-icon v-if="domain && domain.icon" :image="domain.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px"/>
                {{ domain.path || domain.name || domain.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="account" name="account">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          </template>
          <a-input v-model:value="form.account" :placeholder="apiParams.account.description" />
        </a-form-item>
        <a-form-item ref="timezone" name="timezone">
          <template #label>
            <tooltip-label :title="$t('label.timezone')" :tooltip="apiParams.timezone.description"/>
          </template>
          <a-select
            v-model:value="form.timezone"
            :loading="timeZoneLoading"
            :placeholder="apiParams.timezone.description"
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
        <a-form-item ref="networkdomain" name="networkdomain">
          <template #label>
            <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
          </template>
          <a-input
            v-model:value="form.networkdomain"
            :placeholder="apiParams.networkdomain.description" />
        </a-form-item>
        <div v-if="samlAllowed">
          <a-form-item :label="$t('label.samlenable')" ref="samlenable" name="samlenable">
            <a-switch v-model:checked="form.samlenable" />
          </a-form-item>
          <a-form-item v-if="form.samlenable" ref="samlentity" name="samlentity">
            <template #label>
              <tooltip-label :title="$t('label.samlentity')" :tooltip="apiParams.entityid.description"/>
            </template>
            <a-select
              v-model:value="form.samlentity"
              :loading="idpLoading"
              :placeholder="apiParams.entityid.description"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }">
              <a-select-option v-for="idp in idps" :key="idp.id" :label="idp.orgName">
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
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { timeZone } from '@/utils/timezone'
import debounce from 'lodash/debounce'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddAccountForm',
  components: {
    TooltipLabel,
    ResourceIcon
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)
    return {
      loading: false,
      domainLoading: false,
      domainsList: [],
      roleLoading: false,
      roles: [],
      timeZoneLoading: false,
      timeZoneMap: [],
      idpLoading: false,
      idps: []
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createAccount', 'authorizeSamlSso')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  computed: {
    samlAllowed () {
      return 'authorizeSamlSso' in this.$store.getters.apis
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        domainid: this.$store.getters.userInfo.domainid
      })
      this.rules = reactive({
        roleid: [{ required: true, message: this.$t('message.error.select') }],
        username: [{ required: true, message: this.$t('message.error.required.input') }],
        password: [{ required: true, message: this.$t('message.error.required.input') }],
        confirmpassword: [
          { required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateConfirmPassword }
        ],
        email: [{ required: true, message: this.$t('message.error.required.input') }],
        firstname: [{ required: true, message: this.$t('message.error.required.input') }],
        lastname: [{ required: true, message: this.$t('message.error.required.input') }],
        domain: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchDomains()
      this.fetchRoles()
      this.fetchTimeZone()
      if (this.samlAllowed) {
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
    async validateConfirmPassword (rule, value) {
      if (!value || value.length === 0) {
        return Promise.resolve()
      } else if (rule.field === 'confirmpassword') {
        const messageConfirm = this.$t('error.password.not.match')
        const passwordVal = this.form.password
        if (passwordVal && passwordVal !== value) {
          return Promise.reject(messageConfirm)
        } else {
          return Promise.resolve()
        }
      } else {
        return Promise.resolve()
      }
    },
    fetchDomains () {
      this.domainLoading = true
      api('listDomains', {
        listAll: true,
        showicon: true,
        details: 'min'
      }).then(response => {
        this.domainsList = response.listdomainsresponse.domain || []
        this.form.domain = this.domainsList[0].id || ''
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
        this.form.roleid = this.roles[0].id
        if (this.isDomainAdmin()) {
          const userRole = this.roles.filter(role => role.type === 'User')
          if (userRole.length > 0) {
            this.form.roleid = userRole[0].id
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
        this.form.samlentity = this.idps[0].id || ''
      }).finally(() => {
        this.idpLoading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

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
                  message: this.$t('label.samlenable'),
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
