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
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="currentpassword" ref="currentpassword" v-if="!this.isAdminOrDomainAdmin()">
          <template #label>
            {{ $t('label.currentpassword') }}
            <a-tooltip :title="apiParams.currentpassword.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input-password
            v-model:value="form.currentpassword"
            :placeholder="$t('message.error.current.password')"
            autoFocus />
        </a-form-item>
        <a-form-item name="password" ref="password">
          <template #label>
            {{ $t('label.new.password') }}
            <a-tooltip :title="apiParams.password.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input-password
            v-model:value="form.password"
            :placeholder="$t('label.new.password')"/>
        </a-form-item>
        <a-form-item name="confirmpassword" ref="confirmpassword">
          <template #label>
            {{ $t('label.confirmpassword') }}
            <a-tooltip :title="apiParams.password.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input-password
            v-model:value="form.confirmpassword"
            :placeholder="$t('label.confirmpassword.description')"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" html-type="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
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
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    async validateTwoPassword (rule, value) {
      if (!value || value.length === 0) {
        return Promise.resolve()
      } else if (rule.field === 'confirmpassword') {
        const form = this.form
        const messageConfirm = this.$t('message.validate.equalto')
        const passwordVal = form.getFieldValue('password')
        if (passwordVal && passwordVal !== value) {
          return Promise.reject(messageConfirm)
        } else {
          return Promise.resolve()
        }
      } else {
        return Promise.resolve()
      }
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
