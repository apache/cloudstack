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
  <a-form
    id="formLogin"
    class="user-layout-login"
    ref="formLogin"
    :form="form"
    @submit="handleSubmit"
  >
    <a-tabs
      :activeKey="customActiveKey"
      size="large"
      :tabBarStyle="{ textAlign: 'center', borderBottom: 'unset' }"
      @change="handleTabClick"
      :animated="false"
    >
      <a-tab-pane key="cs">
        <span slot="tab">
          <a-icon type="safety" />
          {{ $t('label.login.portal') }}
        </span>
        <a-form-item>
          <a-input
            size="large"
            type="text"
            autoFocus
            :placeholder="$t('label.username')"
            v-decorator="[
              'username',
              {rules: [{ required: true, message: $t('message.error.username') }, { validator: handleUsernameOrEmail }], validateTrigger: 'change'}
            ]"
          >
            <a-icon slot="prefix" type="user" :style="{ color: 'rgba(0,0,0,.25)' }"/>
          </a-input>
        </a-form-item>

        <a-form-item>
          <a-input-password
            size="large"
            type="password"
            autocomplete="false"
            :placeholder="$t('label.password')"
            v-decorator="[
              'password',
              {rules: [{ required: true, message: $t('message.error.password') }], validateTrigger: 'blur'}
            ]"
          >
            <a-icon slot="prefix" type="lock" :style="{ color: 'rgba(0,0,0,.25)' }"/>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-input
            size="large"
            type="text"
            :placeholder="$t('label.domain')"
            v-decorator="[
              'domain',
              {rules: [{ required: false, message: $t('message.error.domain') }], validateTrigger: 'change'}
            ]"
          >
            <a-icon slot="prefix" type="block" :style="{ color: 'rgba(0,0,0,.25)' }"/>
          </a-input>
        </a-form-item>

      </a-tab-pane>
      <a-tab-pane key="saml" :disabled="idps.length === 0">
        <span slot="tab">
          <a-icon type="audit" />
          {{ $t('label.login.single.signon') }}
        </span>
        <a-form-item>
          <a-select v-decorator="['idp', { initialValue: selectedIdp } ]">
            <a-select-option v-for="(idp, idx) in idps" :key="idx" :value="idp.id">
              {{ idp.orgName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-tab-pane>
    </a-tabs>

    <a-form-item>
      <a-button
        size="large"
        type="primary"
        htmlType="submit"
        class="login-button"
        :loading="state.loginBtn"
        :disabled="state.loginBtn"
      >{{ $t('label.login') }}</a-button>
    </a-form-item>
    <translation-menu/>
  </a-form>
</template>

<script>
import { api } from '@/api'
import { mapActions } from 'vuex'
import TranslationMenu from '@/components/header/TranslationMenu'

export default {
  components: {
    TranslationMenu
  },
  data () {
    return {
      idps: [],
      selectedIdp: '',
      customActiveKey: 'cs',
      loginBtn: false,
      loginType: 0,
      form: this.$form.createForm(this),
      state: {
        time: 60,
        loginBtn: false,
        loginType: 0
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    ...mapActions(['Login', 'Logout']),
    fetchData () {
      api('listIdps').then(response => {
        if (response) {
          this.idps = response.listidpsresponse.idp || []
          this.selectedIdp = this.idps[0].id || ''
        }
      })
    },
    // handler
    handleUsernameOrEmail (rule, value, callback) {
      const { state } = this
      const regex = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\.[a-zA-Z0-9_-]{2,3}){1,2})$/
      if (regex.test(value)) {
        state.loginType = 0
      } else {
        state.loginType = 1
      }
      callback()
    },
    handleTabClick (key) {
      this.customActiveKey = key
      // this.form.resetFields()
    },
    handleSubmit (e) {
      e.preventDefault()
      const {
        form: { validateFields },
        state,
        customActiveKey,
        Login
      } = this

      state.loginBtn = true

      const validateFieldsKey = customActiveKey === 'cs' ? ['username', 'password', 'domain'] : ['idp']

      validateFields(validateFieldsKey, { force: true }, (err, values) => {
        if (!err) {
          if (customActiveKey === 'cs') {
            const loginParams = { ...values }
            delete loginParams.username
            loginParams[!state.loginType ? 'email' : 'username'] = values.username
            loginParams.password = values.password
            loginParams.domain = values.domain
            if (!loginParams.domain) {
              loginParams.domain = '/'
            }
            Login(loginParams)
              .then((res) => this.loginSuccess(res))
              .catch(err => {
                this.requestFailed(err)
                state.loginBtn = false
              })
          } else if (customActiveKey === 'saml') {
            state.loginBtn = false
            var samlUrl = this.$config.apiBase + '?command=samlSso'
            if (values.idp) {
              samlUrl += ('&idpid=' + values.idp)
            }
            window.location.href = samlUrl
          }
        } else {
          setTimeout(() => {
            state.loginBtn = false
          }, 600)
        }
      })
    },
    loginSuccess (res) {
      this.$router.push({ path: '/dashboard' }).catch(() => {})
    },
    requestFailed (err) {
      if (err && err.response && err.response.data && err.response.data.loginresponse) {
        const error = err.response.data.loginresponse.errorcode + ': ' + err.response.data.loginresponse.errortext
        this.$message.error(`${this.$t('label.error')} ${error}`)
      } else {
        this.$message.error(this.$t('message.login.failed'))
      }
    }
  }
}
</script>

<style lang="less" scoped>
.user-layout-login {
  min-width: 260px;
  width: 368px;
  margin: 0 auto;

  .mobile & {
    max-width: 368px;
    width: 98%;
  }

  label {
    font-size: 14px;
  }

  button.login-button {
    margin-top: 8px;
    padding: 0 15px;
    font-size: 16px;
    height: 40px;
    width: 100%;
  }

  .user-login-other {
    text-align: left;
    margin-top: 24px;
    line-height: 22px;

    .item-icon {
      font-size: 24px;
      color: rgba(0, 0, 0, 0.2);
      margin-left: 16px;
      vertical-align: middle;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .register {
      float: right;
    }
  }
}
</style>
