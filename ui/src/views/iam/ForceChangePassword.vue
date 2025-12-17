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
            <div style="text-align: center; font-size: 14px; color: #666; margin-top: 5px;">
              {{ $t('message.change.password') }}
            </div>
          </template>

          <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            layout="vertical"
            @finish="handleSubmit"
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
                type="primary"
                size="large"
                block
                :loading="loading"
                @click="handleSubmit"
              >
                {{ $t('label.ok') }}
              </a-button>
            </a-form-item>

            <div class="actions">
              <a @click="handleLogout">{{ $t('label.logout') }}</a>
            </div>
          </a-form>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { postAPI } from '@/api'
import Cookies from 'js-cookie'

export default {
  name: 'ForceChangePassword',
  data () {
    return {
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateUser')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        currentpassword: [{ required: true, message: this.$t('message.error.current.password') }],
        password: [{ required: true, message: this.$t('message.error.new.password') }],
        confirmpassword: [
          { required: true, message: this.$t('message.error.confirm.password') },
          { validator: this.validateTwoPassword }
        ]
      })
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
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
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
        postAPI('updateUser', params).then(() => {
          this.$message.success(this.$t('message.success.change.password'), 5)
          console.log('Password changed successfully.')
          this.handleLogout()
        }).catch(error => {
          console.error(error)
          this.$notification.error({
            message: 'Error',
            description: error.response?.data?.updateuserresponse?.errortext || 'Failed to update password'
          })
          this.$message.error(this.$t('message.error.change.password'))
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        console.log('Validation failed:', error)
      })
    },
    handleLogout () {
      this.$store.dispatch('Logout').then(() => {
        Cookies.remove('userid')
        Cookies.remove('token')
        this.$router.replace({ path: '/user/login' })
      }).catch(err => {
        this.$message.error({
          title: 'Failed to Logout',
          description: err.message
        })
      })
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
    color: #1890ff; /* Ant Design Link Color */
    transition: color 0.3s;

    &:hover {
      color: #40a9ff;
    }
  }
}
</style>
