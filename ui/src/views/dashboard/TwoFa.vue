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
  <a-form>
    <img
      v-if="$config.banner"
      :style="{
        width: $config.theme['@banner-width'],
        height: $config.theme['@banner-height']
      }"
      :src="$config.banner"
      class="user-layout-logo"
      alt="logo">
    <h1 style="text-align: center; font-size: 24px; color: gray"> {{ $t('label.two.factor.authentication') }} </h1>
    <br />
    <br />
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      layout="vertical">
      <a-form-item name="secretkey" ref="secretkey">
        <a-input
          class="center-align"
          style="width: 400px"
          v-model:value="form.secretkey"
          placeholder="secret key" />
      </a-form-item>
      <div :span="24" class="center-align top-padding">
          <a-button
            :loading="loading"
            ref="submit"
            type="primary"
            class="center-align"
            @click="handleSubmit">{{ $t('label.verify') }}
          </a-button>
        </div>
      <div class="note"> {{ $t('message.two.fa.auth') }} </div>
    </a-form>
  </a-form>
</template>
<script>

import { api } from '@/api'
import { ref, reactive, toRaw } from 'vue'

export default {
  name: 'TwoFa',
  data () {
    return {
      twoFAresponse: false
    }
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        secretkey: [{ required: true, message: this.$t('message.error.secret.key') }]
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        console.log(values.secretkey)
        api('validateUserTwoFactorAuthenticationCode', { '2facode': values.secretkey }).then(response => {
          this.twoFAresponse = true
          if (this.twoFAresponse) {
            this.$notification.destroy()
            this.$store.commit('SET_COUNT_NOTIFY', 0)
            this.$store.commit('SET_LOGIN_FLAG', true)
            this.$router.push({ path: '/dashboard' }).catch(() => {})

            this.$message.success({
              content: `${this.$t('label.action.enable.two.factor.authentication')}`,
              duration: 2
            })
            this.$emit('refresh-data')
          }
          console.log(response)
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        })
      })
    }
  }
}
</script>
<style lang="less" scoped>
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
