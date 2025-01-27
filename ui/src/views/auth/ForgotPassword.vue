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
    id="formForgotPassword"
    class="user-layout-forgot-password"
    :ref="formRef"
    :model="form"
    :rules="rules"
    @finish="handleSubmit"
    v-ctrl-enter="handleSubmit"
  >
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

    <a-form-item>
      <a-button
        size="large"
        type="primary"
        html-type="submit"
        class="submit-button"
        :loading="submitBtn"
        :disabled="submitBtn"
        ref="submit"
        @click="handleSubmit"
      >{{ $t('label.submit') }}</a-button>
    </a-form-item>
    <a-row justify="space-between">
      <a-col>
      <translation-menu/>
      </a-col>
      <a-col>
        <router-link :to="{ name: 'login' }">
          {{ $t('label.back.login') }}
        </router-link>

      </a-col>
    </a-row>
  </a-form>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import store from '@/store'
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
      customActiveKeyOauth: false,
      submitBtn: false,
      email: '',
      secretcode: '',
      oauthexclude: '',
      server: ''
    }
  },
  created () {
    if (this.$config.multipleServer) {
      this.server = this.$localStorage.get(SERVER_MANAGER) || this.$config.servers[0]
    }
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        server: (this.server.apiHost || '') + this.server.apiBase
      })
      this.rules = {
        username: [{
          required: true,
          message: this.$t('message.error.username'),
          trigger: 'change'
        }]
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.submitBtn) return
      this.formRef.value.validate().then(() => {
        this.submitBtn = true

        const values = toRaw(this.form)
        if (this.$config.multipleServer) {
          this.axios.defaults.baseURL = (this.server.apiHost || '') + this.server.apiBase
          store.dispatch('SetServer', this.server)
        }
        const loginParams = { ...values }
        delete loginParams.username
        loginParams.username = values.username
        loginParams.domain = values.domain
        if (!loginParams.domain) {
          loginParams.domain = '/'
        }
        api('forgotPassword', {}, 'POST', loginParams)
          .finally(() => {
            this.$message.success(this.$t('message.forgot.password.success'))
            this.$router.push({ path: '/login' }).catch(() => {})
          })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    requestFailed (err) {
      if (err && err.response && err.response.data && err.response.data.forgotpasswordresponse) {
        const error = err.response.data.forgotpasswordresponse.errorcode + ': ' + err.response.data.forgotpasswordresponse.errortext
        this.$message.error(`${this.$t('label.error')} ${error}`)
      } else {
        this.$message.error(this.$t('message.password.reset.failed'))
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
.user-layout-forgot-password {
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

  button.submit-button {
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

    .g-btn-wrapper {
      background-color: rgb(221, 75, 57);
      height: 40px;
      width: 80px;
    }
  }
    .center {
     display: flex;
     flex-direction: column;
     justify-content: center;
     align-items: center;
     height: 100px;
    }

    .content {
      margin: 10px auto;
      width: 300px;
    }

    .or {
      text-align: center;
      font-size: 16px;
      background:
        linear-gradient(#CCC 0 0) left,
        linear-gradient(#CCC 0 0) right;
      background-size: 40% 1px;
      background-repeat: no-repeat;
    }
}
</style>
