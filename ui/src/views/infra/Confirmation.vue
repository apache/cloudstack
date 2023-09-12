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
  <a-spin :spinning="loading">
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
      >
        <a-alert type="error">
          <template #message>
            <span v-html="$t(action.currentAction.message)" />
          </template>
        </a-alert>
        <a-alert type="warning" style="margin-top: 10px">
          <template #message>
            <span>{{ $t('message.confirm.type') }} "{{ action.currentAction.confirmationText }}"</span>
          </template>
        </a-alert>
        <a-form-item ref="confirmation" name="confirmation" style="margin-top: 10px">
            <a-input v-model:value="form.confirmation" />
        </a-form-item>
      </a-form>

      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>

    </div>
  </a-spin>
</template>

<script>

import { api } from '@/api'
import { ref, reactive } from 'vue'

export default {
  name: 'Confirmation',
  props: {
    resource: {
      type: Object,
      required: true
    },
    action: {
      type: Object,
      default: () => {}
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false
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
        confirmation: [{
          validator: this.checkConfirmation,
          message: this.$t('message.error.confirm.text')
        }]
      })
    },
    async checkConfirmation (rule, value) {
      if (value && value === 'SHUTDOWN') {
        return Promise.resolve()
      }
      return Promise.reject(rule.message)
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        this.loading = true
        const params = { managementserverid: this.resource.id }
        api(this.action.currentAction.api, params).then(() => {
          this.$message.success(this.$t(this.action.currentAction.label) + ' : ' + this.resource.name)
          this.closeAction()
          this.parentFetchData()
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
<style lang="scss" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }
}

.form {
  margin: 10px 0;
}
</style>
