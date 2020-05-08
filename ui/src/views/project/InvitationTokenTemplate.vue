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
  <div class="row-project-invitation">
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('projectid')">
          <a-input
            v-decorator="['projectid', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('project.projectid.description')"
          />
        </a-form-item>
        <a-form-item :label="$t('token')">
          <a-input
            v-decorator="['token', {
              rules: [{ required: true, message: 'Please enter input' }]
            }]"
            :placeholder="$t('project.token.description')"
          />
        </a-form-item>
        <div class="card-footer">
          <!-- ToDo extract as component -->
          <a-button @click="() => $emit('close-action')">{{ this.$t('cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'InvitationTokenTemplate',
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  data () {
    return {
      loading: false
    }
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()

      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        const title = this.$t('label.accept.project.invitation')
        const description = this.$t('projectid') + ' ' + values.projectid
        const loading = this.$message.loading(title + 'in progress for ' + description, 0)

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
              this.$store.dispatch('AddAsyncJob', {
                title: title,
                jobid: jobId,
                description: description,
                status: 'progress'
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

.card-footer {
  text-align: right;

  button + button {
    margin-left: 8px;
  }
}
</style>
