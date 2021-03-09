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
      <a-form :form="form" @submit="handleSubmit" layout="vertical">
        <a-form-item
          :label="$t('migrate.from')">
          <a-select
            v-decorator="['srcpool', {
              initialValue: selectedStore,
              rules: [
                {
                  required: true,
                  message: $t('message.error.select'),
                }]
            }]"
            :loading="loading"
            @change="val => { selectedStore = val }"
            autoFocus
          >
            <a-select-option
              v-for="store in imageStores"
              :key="store.id"
            >{{ store.name || opt.url }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          :label="$t('migrate.to')">
          <a-select
            v-decorator="['destpools', {
              rules: [
                {
                  required: true,
                  message: $t('message.select.destination.image.stores'),
                }]
            }]"
            mode="multiple"
            :loading="loading"
          >
            <a-select-option
              v-for="store in imageStores"
              v-if="store.id !== selectedStore"
              :key="store.id"
            >{{ store.name || opt.url }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('migrationPolicy')">
          <a-select
            v-decorator="['migrationtype', {
              initialValue: 'Complete',
              rules: [
                {
                  required: true,
                  message: $t('message.select.migration.policy'),
                }]
            }]"
            :loading="loading"
          >
            <a-select-option value="Complete">{{ $t('label.complete') }}</a-select-option>
            <a-select-option value="Balance">{{ $t('label.balance') }}</a-select-option>
          </a-select>
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
  name: 'MigrateData',
  inject: ['parentFetchData'],
  data () {
    return {
      imageStores: [],
      loading: false,
      selectedStore: ''
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchImageStores()
  },
  methods: {
    fetchImageStores () {
      this.loading = true
      api('listImageStores').then(json => {
        this.imageStores = json.listimagestoresresponse.imagestore || []
        this.selectedStore = this.imageStores[0].id || ''
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          if (key === 'destpools') {
            params[key] = input.join(',')
          } else {
            params[key] = input
          }
        }

        const title = this.$t('message.data.migration')
        this.loading = true

        const result = this.migrateData(params, title)
        result.then(json => {
          const result = json.jobresult
          const success = result.imagestore.success || false
          const message = result.imagestore.message || ''
          if (success) {
            this.$notification.success({
              message: title,
              description: message
            })
          } else {
            this.$notification.error({
              message: title,
              description: message,
              duration: 0
            })
          }
          this.parentFetchData()
          this.closeAction()
        }).catch(error => {
          console.log(error)
        })
        setTimeout(() => {
          this.$message.loading({ content: this.$t('label.migrating.data'), duration: 1 })
          this.loading = false
          this.closeAction()
        }, 200)
      })
    },
    migrateData (args, title) {
      return new Promise((resolve, reject) => {
        api('migrateSecondaryStorageData', args).then(async json => {
          const jobId = json.migratesecondarystoragedataresponse.jobid
          if (jobId) {
            this.$store.dispatch('AddAsyncJob', {
              title: title,
              jobid: jobId,
              description: this.$t('message.data.migration.progress'),
              status: 'progress',
              silent: true
            })
            const result = await this.pollJob(jobId, title)
            if (result.jobstatus === 2) {
              reject(result.jobresult.errortext)
              return
            }
            resolve(result)
          }
        }).catch(error => {
          reject(error)
        })
      })
    },
    async pollJob (jobId, title) {
      return new Promise(resolve => {
        const asyncJobInterval = setInterval(() => {
          api('queryAsyncJobResult', { jobId }).then(async json => {
            const result = json.queryasyncjobresultresponse
            if (result.jobstatus === 0) {
              return
            }
            clearInterval(asyncJobInterval)
            resolve(result)
          })
        }, 1000)
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
  width: 85vw;

  @media (min-width: 1000px) {
    width: 40vw;
  }
}

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
