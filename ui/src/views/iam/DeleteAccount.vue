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
    class="form"
    :ref="formRef"
    :model="form"
    :rules="rules"
    layout="vertical"
    @finish="handleSubmit"
    v-ctrl-enter="handleSubmit"
   >
    <div style="margin-bottom: 10px">
      <a-alert type="warning">
        <template #message>
          <div v-html="$t('message.delete.account.warning')"></div>
        </template>
    </a-alert>
    </div>
    <div style="margin-bottom: 10px">
      <a-alert>
        <template #message>
          <div v-html="$t('message.delete.account.confirm')"></div>
        </template>
      </a-alert>
    </div>
    <a-form-item name="name" ref="name">
      <a-input
        v-model:value="form.name"
        :placeholder="$t('label.enter.account.name')"
        style="width: 100%"/>
    </a-form-item>
    <p v-if="error" class="error">{{ error }}</p>
    <div :span="24" class="actions">
      <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
import { ref, reactive } from 'vue'
import { api } from '@/api'
export default {
  name: 'DeleteAccount',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      error: ''
    }
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('label.required') }]
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      this.formRef.value.validate().then(async () => {
        if (this.form.name !== this.resource.name) {
          this.error = `${this.$t('message.error.account.delete.name.mismatch')}`
          return
        }
        api('deleteAccount', {
          id: this.resource.id
        }).then(response => {
          this.$pollJob({
            jobId: response.deleteaccountresponse.jobid,
            title: this.$t('label.action.delete.account'),
            description: this.resource.id,
            successMessage: `${this.$t('message.delete.account.success')} - ${this.resource.name}`,
            errorMessage: `${this.$t('message.delete.account.failed')} - ${this.resource.name}`,
            loadingMessage: `${this.$t('message.delete.account.processing')} - ${this.resource.name}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;
  @media (min-width: 500px) {
    width: 400px;
  }
}
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
.error {
  color: red;
  margin-top: 10px;
}
</style>
