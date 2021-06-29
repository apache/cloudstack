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
  <div class="form-layout">
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item v-if="!this.isAdminOrDomainAdmin()">
          <span slot="label">
            {{ $t('label.currentpassword') }}
            <a-tooltip :title="apiParams.currentpassword.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input-password
            v-decorator="['currentpassword', {
              rules: [{ required: true, message: $t('message.error.current.password') }]
            }]"
            :placeholder="$t('message.error.current.password')"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.new.password') }}
            <a-tooltip :title="apiParams.password.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input-password
            v-decorator="['password', {
              rules: [{ required: true, message: $t('message.error.new.password') }]
            }]"
            :placeholder="$t('label.new.password')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.confirmpassword') }}
            <a-tooltip :title="apiParams.password.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input-password
            v-decorator="['confirmpassword', {
              rules: [
                {
                  required: true,
                  message: $t('message.error.confirm.password')
                },
                {
                  validator: validateTwoPassword
                }
              ]
            }]"
            :placeholder="$t('label.confirmpassword.description')"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ChangeUserPassword',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    this.apiConfig = this.$store.getters.apis.updateUser || {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  methods: {
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    validateTwoPassword (rule, value, callback) {
      if (!value || value.length === 0) {
        callback()
      } else if (rule.field === 'confirmpassword') {
        const form = this.form
        const messageConfirm = this.$t('message.validate.equalto')
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
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {
          id: this.resource.id,
          password: values.password
        }
        if (this.isValidValueForKey(values, 'currentpassword') && values.currentpassword.length > 0) {
          params.currentpassword = values.currentpassword
        }
        api('updateUser', {}, 'POST', params).then(json => {
          this.$notification.success({
            message: this.$t('label.action.change.password'),
            description: `${this.$t('message.success.change.password')} ${this.resource.username}`
          })
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
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

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
