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
  <div class="center">
    <a-form>
      <img
        v-if="$config.banner"
        :src="$config.banner"
        class="user-layout-logo"
        alt="logo">
      <h1 style="text-align: center; font-size: 24px; color: gray"> {{ $t('label.two.factor.authentication') }} </h1>
      <p v-if="$store.getters.twoFaProvider === 'totp'" style="text-align: center; font-size: 16px;" v-html="$t('message.two.fa.auth.totp')"></p>
      <p v-if="$store.getters.twoFaProvider === 'staticpin'" style="text-align: center; font-size: 16px;" v-html="$t('message.two.fa.auth.staticpin')"></p>
      <br />
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="code" ref="code" style="text-align: center;">
          <a-input-password
            style="width: 500px"
            v-model:value="form.code"
            placeholder="xxxxxx" />
        </a-form-item>
        <br/>
        <div :span="24" class="center-align top-padding">
          <a-button
            :loading="loading"
            ref="submit"
            type="primary"
            :disabled="buttonstate"
            class="center-align"
            @click="handleSubmit">{{ $t('label.verify') }}
          </a-button>
        </div>

      </a-form>
    </a-form>
  </div>
</template>
<script>

import { api } from '@/api'
import { ref, reactive, toRaw } from 'vue'

export default {
  name: 'VerifyTwoFa',
  data () {
    return {
      twoFAresponse: false,
      buttonstate: false
    }
  },
  created () {
    this.initForm()
  },
  mounted () {
    this.$nextTick(() => {
      this.focusInput()
    })
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        code: [{ required: true, message: this.$t('message.error.authentication.code') }]
      })
    },
    focusInput () {
      const inputElement = this.$refs.code.$el.querySelector('input[type=password]')
      if (inputElement) {
        inputElement.focus()
      }
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        if (values.code !== null) {
          this.buttonstate = true
        }
        api('validateUserTwoFactorAuthenticationCode', { codefor2fa: values.code }).then(response => {
          this.twoFAresponse = true
          if (this.twoFAresponse) {
            this.$notification.destroy()
            this.$store.commit('SET_COUNT_NOTIFY', 0)
            this.$store.commit('SET_LOGIN_FLAG', true)
            this.$router.push({ path: '/dashboard' }).catch(() => {})

            this.$message.success({
              content: `${this.$t('label.action.verify.two.factor.authentication')}`,
              duration: 2
            })
            this.$emit('refresh-data')
          }
        }).catch(() => {
          this.$store.dispatch('Logout').then(() => {
            this.$router.replace({ path: '/user/login' })
          })
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.two.factor.authorization.failed')
          })
        })
      })
    }
  }
}
</script>
<style lang="less" scoped>
  .center {
    position: fixed;
    top: 42.5%;
    left: 50%;
    -webkit-transform: translate(-50%, -50%);

    background-color: transparent;
    padding: 70px 50px 70px 50px;
    z-index: 100;
  }
  .center-align {
    display: block;
    margin-left: auto;
    margin-right: auto;
  }
  .top-padding {
    padding-top: 35px;
  }
  .note {
    text-align: center;
    color: grey;
    padding-top: 10px;
  }

  .user-layout {
    height: 100%;

    &-container {
      padding: 3rem 0;
      width: 100%;

      @media (min-height:600px) {
        padding: 0;
        position: relative;
        top: 50%;
        transform: translateY(-50%);
        margin-top: -50px;
      }
    }

    &-logo {
      border-style: none;
      margin: 0 auto 2rem;
      display: block;

      .mobile & {
        max-width: 300px;
        margin-bottom: 1rem;
      }
    }

    &-footer {
      display: flex;
      flex-direction: column;
      position: absolute;
      bottom: 20px;
      text-align: center;
      width: 100%;

      @media (max-height: 600px) {
        position: relative;
        margin-top: 50px;
      }

      label {
        width: 368px;
        font-weight: 500;
        margin: 0 auto;
      }
    }
  }
</style>
