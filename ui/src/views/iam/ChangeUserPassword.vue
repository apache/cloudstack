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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="currentpassword" ref="currentpassword" v-if="!isAdminOrDomainAdmin()">
          <template #label>
            <tooltip-label :title="$t('label.currentpassword')" :tooltip="apiParams.currentpassword.description"/>
          </template>
          <a-input-password
            v-model:value="form.currentpassword"
            :placeholder="$t('message.error.current.password')"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="password" ref="password">
          <template #label>
            <tooltip-label :title="$t('label.new.password')" :tooltip="apiParams.password.description"/>
          </template>
          <a-input-password
            v-model:value="form.password"
            :placeholder="$t('label.new.password')"/>
        </a-form-item>
        <a-form-item name="confirmpassword" ref="confirmpassword">
          <template #label>
            <tooltip-label :title="$t('label.confirmpassword')" :tooltip="apiParams.password.description"/>
          </template>
          <a-input-password
            v-model:value="form.confirmpassword"
            :placeholder="$t('label.confirmpassword.description')"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ChangeUserPassword',
  components: {
    TooltipLabel
  },
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
</style>
