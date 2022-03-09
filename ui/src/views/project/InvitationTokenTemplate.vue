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
  <div class="row-project-invitation" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
       >
        <a-form-item ref="projectid" name="projectid" :label="$t('label.projectid')">
          <a-input
            v-model:value="form.projectid"
            :placeholder="apiParams.projectid.description"
            v-focus="true"
          />
        </a-form-item>
        <a-form-item ref="token" name="token" :label="$t('label.token')">
          <a-input
            v-model:value="form.token"
            :placeholder="apiParams.token.description"
          />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="() => $emit('close-action')">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'InvitationTokenTemplate',
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateProjectInvitation')
  },
  data () {
    return {
      loading: false
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({
      projectid: [{ required: true, message: this.$t('message.error.required.input') }],
      token: [{ required: true, message: this.$t('message.error.required.input') }]
    })
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        const title = this.$t('label.accept.project.invitation')
        const description = this.$t('label.projectid') + ' ' + values.projectid
        const loading = this.$message.loading(title + `${this.$t('label.in.progress.for')} ` + description, 0)

        this.loading = true

        api('updateProjectInvitation', values).then(json => {
          this.checkForAddAsyncJob(json, title, description)
          this.$emit('close-action')
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.$emit('refresh-data')
          this.loading = false
          setTimeout(loading, 1000)
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    checkForAddAsyncJob (json, title, description) {
      let hasJobId = false

      for (const obj in json) {
        if (obj.includes('response')) {
          for (const res in json[obj]) {
            if (res === 'jobid') {
              hasJobId = true
              const jobId = json[obj][res]
              this.$pollJob({
                title,
                jobId,
                description,
                showLoading: false
              })
            }
          }
        }
      }

      return hasJobId
    }
  }
}
</script>

<style lang="less" scoped>
.row-project-invitation {
  min-width: 450px;
}
</style>
