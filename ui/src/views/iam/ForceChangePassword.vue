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
  <div class="user-layout-wrapper">
    <div class="container">
      <div class="user-layout-content">
        <a-card :bordered="false" class="force-password-card">
          <template #title>
            <div style="text-align: center; font-size: 18px; font-weight: bold;">
              {{ $t('label.action.change.password') }}
            </div>
            <div v-if="!isSubmitted" style="text-align: center; font-size: 14px; color: #666; margin-top: 5px;">
              {{ $t('message.change.password.required') }}
            </div>
          </template>
          <a-spin :spinning="loading">
          <div v-if="isSubmitted" class="success-state">
            <check-outlined class="success-icon" />
            <div class="success-text">
              {{ $t('message.success.change.password') }}
            </div>
            <div class="success-subtext">
               {{ $t('message.please.login.new.password') }}
            </div>
            <a-button
              type="primary"
              size="large"
              block
              @click="redirectToLogin()"
              style="margin-top: 20px;"
            >
              {{ $t('label.login') }}
            </a-button>
          </div>

          <a-form
            v-else
            :ref="formRef"
            :model="form"
            :rules="rules"
            layout="vertical"
            @finish="handleSubmit"
            v-ctrl-enter="handleSubmit"
          >
            <a-form-item name="currentpassword" :label="$t('label.currentpassword')">
              <a-input-password
                v-model:value="form.currentpassword"
                :placeholder="$t('label.currentpassword')"
                size="large"
                v-focus="true"
              />
            </a-form-item>

            <a-form-item name="password" :label="$t('label.new.password')">
              <a-input-password
                v-model:value="form.password"
                :placeholder="$t('label.new.password')"
                size="large"
              />
            </a-form-item>

            <a-form-item name="confirmpassword" :label="$t('label.confirmpassword')">
              <a-input-password
                v-model:value="form.confirmpassword"
                :placeholder="$t('label.confirmpassword')"
                size="large"
              />
            </a-form-item>

            <a-form-item>
              <a-button
                html-type="submit"
                type="primary"
                size="large"
                block
                :disabled="loading"
                :loading="loading"
                @click="handleSubmit"
              >
                {{ $t('label.ok') }}
              </a-button>
            </a-form-item>

            <div class="actions">
              <a @click="logoutAndRedirectToLogin()">{{ $t('label.logout') }}</a>
            </div>
          </a-form>
          </a-spin>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { postAPI } from '@/api'
import Cookies from 'js-cookie'
import { PASSWORD_CHANGE_REQUIRED } from '@/store/mutation-types'

export default {
  name: 'ForceChangePassword',
  data () {
    return {
      loading: false,
      isSubmitted: false
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.isPasswordChangeRequired()
  },
  computed: {
    rules () {
      return {
        currentpassword: [{ required: true, message: this.$t('message.error.current.password') }],
        password: [
          { required: true, message: this.$t('message.error.new.password') },
          { validator: this.validateNewPassword, trigger: 'change' }
        ],
        confirmpassword: [
          { required: true, message: this.$t('message.error.confirm.password') },
          { validator: this.validateTwoPassword, trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    async validateNewPassword (rule, value) {
      const currentPassword = this.form.currentpassword
      if (!value || value.length === 0) {
        return Promise.resolve()
      }
      // Ensure new password is different from current password
      if (currentPassword && value === currentPassword) {
        return Promise.reject(this.$t('message.error.newpassword.sameascurrent'))
      }
      return Promise.resolve()
    },
    async validateTwoPassword (rule, value) {
      if (!value || value.length === 0) {
        return Promise.resolve()
      } else if (rule.field === 'confirmpassword') {
        const form = this.form
        const messageConfirm = this.$t('message.validate.equalto')
        const passwordVal = form.password
        if (passwordVal && passwordVal !== value) {
          return Promise.reject(messageConfirm)
        } else {
          return Promise.resolve()
        }
      } else {
        return Promise.resolve()
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        this.loading = true
        const values = toRaw(this.form)
        const userId = Cookies.get('userid')

        const params = {
          id: userId,
          password: values.password,
          currentpassword: values.currentpassword
        }
        postAPI('updateUser', params).then(async () => {
          this.$localStorage.remove(PASSWORD_CHANGE_REQUIRED)
          await this.handleLogout()
          this.isSubmitted = true
        }).catch(error => {
          console.error(error)
          this.$message.error(this.$t('message.error.change.password'))
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        console.log('Validation failed:', error)
      })
    },
    async handleLogout () {
      try {
        await this.$store.dispatch('Logout')
      } catch (e) {
        console.error('Logout failed:', e)
      } finally {
        Cookies.remove('userid')
        Cookies.remove('token')
      }
    },
    redirectToLogin () {
      this.$router.replace('/user/login')
    },
    logoutAndRedirectToLogin () {
      this.handleLogout().then(() => {
        this.redirectToLogin()
      })
    },
    async isPasswordChangeRequired () {
      const passwordChangeRequired = this.$localStorage.get(PASSWORD_CHANGE_REQUIRED)
      this.isSubmitted = !passwordChangeRequired
    }
  }
}
</script>

<style scoped lang="less">
.user-layout-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;

  .container {
    width: 100%;
    padding: 16px;

    .user-layout-content {
      display: flex;
      justify-content: center;

      .force-password-card {
        width: 100%;
        max-width: 420px;
        border-radius: 8px;
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
      }
    }
  }
}

.actions {
  text-align: center;
  margin-top: 16px;

  a {
    color: #1890ff;
    transition: color 0.3s;

    &:hover {
      color: #40a9ff;
    }
  }
}

.success-state {
  text-align: center;
  padding: 20px 0;

  .success-icon {
    font-size: 48px;
    color: #52c41a;
    margin-bottom: 16px;
  }

  .success-text {
    font-size: 20px;
    font-weight: 500;
    color: #333;
    margin-bottom: 8px;
  }

  .success-subtext {
    font-size: 14px;
    color: #666;
  }
}
</style>
