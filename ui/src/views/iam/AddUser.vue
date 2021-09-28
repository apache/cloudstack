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
          <tooltip-label slot="label" :title="$t('label.username')" :tooltip="apiParams.username.description"/>
          <a-input
            v-decorator="['username', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.username.description"
            autoFocus/>
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
        <a-form-item v-if="this.isAdminOrDomainAdmin() && !domainid">
          <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          <a-select
            :loading="domainLoading"
            v-decorator="['domainid']"
            :placeholder="apiParams.domainid.description"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="domain in domainsList" :key="domain.id" :label="domain.path || domain.name || domain.description">
              <span>
                <resource-icon v-if="domain && domain.icon" :image="domain.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="block" style="margin-right: 5px" />
                {{ domain.path || domain.name || domain.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="!account">
          <tooltip-label slot="label" :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          <a-select
            v-decorator="['account', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :loading="loadingAccount"
            :placeholder="apiParams.account.description"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="(item, idx) in accountList" :key="idx" :label="item.name">
              <span>
                <resource-icon v-if="item && item.icon" :image="item.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="team" style="margin-right: 5px" />
                {{ item.name }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.timezone')" :tooltip="apiParams.timezone.description"/>
          <a-select
            v-decorator="['timezone']"
            :loading="timeZoneLoading"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="opt in timeZoneMap" :key="opt.id">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="samlAllowed">
          <a-form-item :label="$t('label.samlenable')">
            <a-switch v-decorator="['samlenable']" @change="checked => { this.samlEnable = checked }" />
          </a-form-item>
          <a-form-item v-if="samlEnable">
            <tooltip-label slot="label" :title="$t('label.samlentity')" :tooltip="apiParams.entityid.description"/>
            <a-select
              v-decorator="['samlentity', {
                initialValue: selectedIdp,
              }]"
              :loading="idpLoading"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="idp in idps" :key="idp.id">
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
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddUser',
  components: {
    TooltipLabel,
    ResourceIcon
  },
  data () {
    this.fetchTimeZone = debounce(this.fetchTimeZone, 800)
    return {
      loading: false,
      timeZoneLoading: false,
      timeZoneMap: [],
      domainLoading: false,
      domainsList: [],
      selectedDomain: '',
      samlEnable: false,
      idpLoading: false,
      idps: [],
      selectedIdp: '',
      loadingAccount: false,
      accountList: [],
      account: null,
      domainid: null
    }
  },
  created () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createUser', 'authorizeSamlSso')
    this.fetchData()
  },
  computed: {
    samlAllowed () {
      return 'authorizeSamlSso' in this.$store.getters.apis
    }
  },
  methods: {
    fetchData () {
      this.account = this.$route.query && this.$route.query.account ? this.$route.query.account : null
      this.domainid = this.$route.query && this.$route.query.domainid ? this.$route.query.domainid : null
      if (!this.domianid) {
        this.fetchDomains()
      }
      if (!this.account) {
        this.fetchAccount()
      }
      this.fetchTimeZone()
      if (this.samlAllowed) {
        this.fetchIdps()
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
    fetchAccount () {
      this.accountList = []
      this.loadingAccount = true
      api('listAccounts', { listAll: true, showicon: true }).then(response => {
        this.accountList = response.listaccountsresponse.account || []
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.loadingAccount = false
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
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
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
          username: values.username,
          password: values.password,
          email: values.email,
          firstname: values.firstname,
          lastname: values.lastname,
          accounttype: 0
        }

        if (this.account) {
          params.account = this.account
        } else if (this.accountList[values.account]) {
          params.account = this.accountList[values.account].name
        }

        if (this.domainid) {
          params.domainid = this.domainid
        } else if (values.domainid) {
          params.domainid = values.domainid
        }

        if (this.isValidValueForKey(values, 'timezone') && values.timezone.length > 0) {
          params.timezone = values.timezone
        }

        api('createUser', {}, 'POST', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.create.user'),
            description: `${this.$t('message.success.create.user')} ${params.username}`
          })
          const user = response.createuserresponse.user
          if (values.samlenable && user) {
            api('authorizeSamlSso', {
              enable: values.samlenable,
              entityid: values.samlentity,
              userid: user.id
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
