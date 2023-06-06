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
    :ref="formRef"
    :model="form"
    :rules="rules"
    @finish="handleSubmit"
    v-ctrl-enter="handleSubmit"
  >
    <a-tabs
      class="tab-center"
      :activeKey="customActiveKey"
      size="large"
      :tabBarStyle="{ textAlign: 'center', borderBottom: 'unset' }"
      @change="handleTabClick"
      :animated="false"
    >
      <a-tab-pane key="cs">
        <template #tab>
          <span>
            <safety-outlined />
            {{ $t('label.login.portal') }}
          </span>
        </template>
        <a-form-item v-if="$config.multipleServer" name="server" ref="server">
          <a-select
            size="large"
            :placeholder="$t('server')"
            v-model:value="form.server"
            @change="onChangeServer"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option v-for="item in $config.servers" :key="(item.apiHost || '') + item.apiBase" :label="item.name">
              <template #prefix>
                <database-outlined />
              </template>
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="username" name="username">
          <a-input
            size="large"
            type="text"
            v-focus="true"
            :placeholder="$t('label.username')"
            v-model:value="form.username"
          >
            <template #prefix>
              <user-outlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item ref="password" name="password">
          <a-input-password
            size="large"
            type="password"
            autocomplete="false"
            :placeholder="$t('label.password')"
            v-model:value="form.password"
          >
            <template #prefix>
              <lock-outlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item ref="domain" name="domain">
          <a-input
            size="large"
            type="text"
            :placeholder="$t('label.domain')"
            v-model:value="form.domain"
          >
            <template #prefix>
              <block-outlined />
            </template>
          </a-input>
        </a-form-item>
      </a-tab-pane>
      <a-tab-pane key="saml" :disabled="idps.length === 0">
        <template #tab>
          <span>
            <audit-outlined />
            {{ $t('label.login.single.signon') }}
          </span>
        </template>
        <a-form-item v-if="$config.multipleServer" name="server" ref="server">
          <a-select
            size="large"
            :placeholder="$t('server')"
            v-model:value="form.server"
            @change="onChangeServer"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="item in $config.servers" :key="(item.apiHost || '') + item.apiBase" :label="item.name">
              <template #prefix>
                <database-outlined />
              </template>
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="idp" ref="idp">
          <a-select
            v-model:value="form.idp"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="(idp, idx) in idps" :key="idx" :value="idp.id" :label="idp.orgName">
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
        html-type="submit"
        class="login-button"
        :loading="state.loginBtn"
        :disabled="state.loginBtn"
        ref="submit"
        @click="handleSubmit"
      >{{ $t('label.login') }}</a-button>
    </a-form-item>
    <translation-menu/>
  </a-form>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import store from '@/store'
import { mapActions } from 'vuex'
import { sourceToken } from '@/utils/request'
import { SERVER_MANAGER } from '@/store/mutation-types'
import TranslationMenu from '@/components/header/TranslationMenu'

export default {
  components: {
    TranslationMenu
  },
  data () {
    return {
      idps: [],
      customActiveKey: 'cs',
      loginBtn: false,
      loginType: 0,
      state: {
        time: 60,
        loginBtn: false,
        loginType: 0
      },
      server: ''
    }
  },
  created () {
    if (this.$config.multipleServer) {
      this.server = this.$localStorage.get(SERVER_MANAGER) || this.$config.servers[0]
    }
    this.initForm()
    if (store.getters.logoutFlag) {
      sourceToken.init()
      this.fetchData()
    } else {
      this.fetchData()
    }
  },
  methods: {
    ...mapActions(['Login', 'Logout']),
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        server: (this.server.apiHost || '') + this.server.apiBase
      })
      this.rules = reactive({})
      this.setRules()
    },
    setRules () {
      if (this.customActiveKey === 'cs') {
        this.rules.username = [
          {
            required: true,
            message: this.$t('message.error.username'),
            trigger: 'change'
          },
          {
            validator: this.handleUsernameOrEmail,
            trigger: 'change'
          }
        ]
        this.rules.password = [
          {
            required: true,
            message: this.$t('message.error.password'),
            trigger: 'change'
          }
        ]
      } else {
        this.rules.username = []
        this.rules.password = []
      }
    },
    fetchData () {
      api('listIdps').then(response => {
        if (response) {
          this.idps = response.listidpsresponse.idp || []
          this.idps.sort(function (a, b) {
            if (a.orgName < b.orgName) { return -1 }
            if (a.orgName > b.orgName) { return 1 }
            return 0
          })
          this.form.idp = this.idps[0].id || ''
        }
      })
    },
    // handler
    async handleUsernameOrEmail (rule, value) {
      const { state } = this
      const regex = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\.[a-zA-Z0-9_-]{2,3}){1,2})$/
      if (regex.test(value)) {
        state.loginType = 0
      } else {
        state.loginType = 1
      }
      return Promise.resolve()
    },
    handleTabClick (key) {
      this.customActiveKey = key
      this.setRules()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.state.loginBtn) return
      this.formRef.value.validate().then(() => {
        this.state.loginBtn = true

        const values = toRaw(this.form)
        if (this.$config.multipleServer) {
          this.axios.defaults.baseURL = (this.server.apiHost || '') + this.server.apiBase
          store.dispatch('SetServer', this.server)
        }
        if (this.customActiveKey === 'cs') {
          const loginParams = { ...values }
          delete loginParams.username
          loginParams[!this.state.loginType ? 'email' : 'username'] = values.username
          loginParams.password = values.password
          loginParams.domain = values.domain
          if (!loginParams.domain) {
            loginParams.domain = '/'
          }
          this.Login(loginParams)
            .then((res) => this.loginSuccess(res))
            .catch(err => {
              this.requestFailed(err)
              this.state.loginBtn = false
            })
        } else if (this.customActiveKey === 'saml') {
          this.state.loginBtn = false
          var samlUrl = this.$config.apiBase + '?command=samlSso'
          if (values.idp) {
            samlUrl += ('&idpid=' + values.idp)
          }
          window.location.href = samlUrl
        }
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    loginSuccess (res) {
      this.$notification.destroy()
      this.$store.commit('SET_COUNT_NOTIFY', 0)
      if (store.getters.twoFaEnabled === true && store.getters.twoFaProvider !== '' && store.getters.twoFaProvider !== undefined) {
        this.$router.push({ path: '/verify2FA' }).catch(() => {})
      } else if (store.getters.twoFaEnabled === true && (store.getters.twoFaProvider === '' || store.getters.twoFaProvider === undefined)) {
        this.$router.push({ path: '/setup2FA' }).catch(() => {})
      } else {
        this.$store.commit('SET_LOGIN_FLAG', true)
        this.$router.push({ path: '/dashboard' }).catch(() => {})
      }
    },
    requestFailed (err) {
      if (err && err.response && err.response.data && err.response.data.loginresponse) {
        const error = err.response.data.loginresponse.errorcode + ': ' + err.response.data.loginresponse.errortext
        this.$message.error(`${this.$t('label.error')} ${error}`)
      } else {
        this.$message.error(this.$t('message.login.failed'))
      }
    },
    onChangeServer (server) {
      const servers = this.$config.servers || []
      const serverFilter = servers.filter(ser => (ser.apiHost || '') + ser.apiBase === server)
      this.server = serverFilter[0] || {}
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
