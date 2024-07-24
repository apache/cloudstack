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
  <div>
  </div>
</template>

<script>
import store from '@/store'
import { mapActions } from 'vuex'
import { api } from '@/api'
import { OAUTH_DOMAIN, OAUTH_PROVIDER } from '@/store/mutation-types'

export default {
  name: 'VerifyOauth',
  data () {
    return {
      state: {
        time: 60,
        loginBtn: false,
        loginType: 0
      }
    }
  },
  created () {
    this.verifyOauth()
  },
  methods: {
    ...mapActions(['Login', 'Logout', 'OauthLogin']),
    verifyOauth () {
      const params = new URLSearchParams(window.location.search)
      const code = params.get('code')
      const provider = this.$localStorage.get(OAUTH_PROVIDER)
      this.state.loginBtn = true
      api('verifyOAuthCodeAndGetUser', { provider: provider, secretcode: code }).then(response => {
        const email = response.verifyoauthcodeandgetuserresponse.oauthemail.email
        const loginParams = {}
        loginParams.email = email
        loginParams.provider = provider
        loginParams.secretcode = code
        loginParams.domain = this.$localStorage.get(OAUTH_DOMAIN)
        this.OauthLogin(loginParams)
          .then((res) => this.loginSuccess(res))
          .catch(err => {
            this.requestFailed(err)
            this.state.loginBtn = false
          })
      }).catch(err => {
        this.requestFailed(err)
        this.state.loginBtn = false
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
      this.$store.dispatch('Logout').then(() => {
        this.$router.replace({ path: '/user/login' })
      })
      if (err && err.response && err.response.data && err.response.data.oauthloginresponse) {
        const error = err.response.data.oauthloginresponse.errorcode + ': ' + err.response.data.oauthloginresponse.errortext
        this.$message.error(`${this.$t('label.error')} ${error}`)
      } else {
        this.$message.error(this.$t('message.login.failed'))
      }
    }
  }
}
</script>
