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
        @finish="handleSubmit"
        layout="vertical"
       >
        <a-form-item
          name="srcpool"
          ref="srcpool"
          :label="$t('migrate.from')">
          <a-select
            v-model:value="form.srcpool"
            @change="filterStores"
            :loading="loading"
            v-focus="true"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="store in imageStores"
              :key="store.id"
              :label="store.name || opt.url"> {{ store.name || opt.url }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="destpools"
          ref="destpools"
          :label="$t('migrate.to')">
          <a-select
            v-model:value="form.destpools"
            mode="multiple"
            :loading="loading"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="store in destStores"
              :key="store.id"
              :label="store.name || opt.url"> {{ store.name || opt.url }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="migrationtype" ref="migrationtype" :label="$t('migrationPolicy')">
          <a-select
            v-model:value="form.migrationtype"
            :loading="loading"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="Complete" :label="$t('label.complete')">{{ $t('label.complete') }}</a-select-option>
            <a-select-option value="Balance" :label="$t('label.balance')">{{ $t('label.balance') }}</a-select-option>
          </a-select>
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

export default {
  name: 'MigrateData',
  inject: ['parentFetchData'],
  data () {
    return {
      imageStores: [],
      destStores: [],
      loading: false
    }
  },
  async created () {
    this.initForm()
    await this.fetchImageStores()
    this.filterStores()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        migrationtype: 'Complete'
      })
      this.rules = reactive({
        srcpool: [{ required: true, message: this.$t('message.error.select') }],
        destpools: [{ type: 'array', required: true, message: this.$t('message.select.destination.image.stores') }],
        migrationtype: [{ required: true, message: this.$t('message.select.migration.policy') }]
      })
    },
    fetchImageStores () {
      return new Promise((resolve, reject) => {
        this.loading = true
        api('listImageStores').then(json => {
          this.imageStores = json.listimagestoresresponse.imagestore || []
          this.form.srcpool = this.imageStores[0].id || ''
          resolve(this.imageStores)
        }).catch((error) => {
          reject(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    filterStores () {
      this.destStores = this.imageStores.filter(store => { return store.id !== this.form.srcpool })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
        const loadingJob = this.$message.loading({ content: this.$t('label.migrating.data'), duration: 0 })

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
        }).finally(() => {
          this.loading = false
          setTimeout(loadingJob)
          this.closeAction()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    migrateData (args, title) {
      return new Promise((resolve, reject) => {
        api('migrateSecondaryStorageData', args).then(async json => {
          const jobId = json.migratesecondarystoragedataresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description: this.$t('message.data.migration.progress'),
              successMethod: (result) => resolve(result),
              errorMethod: (result) => reject(result.jobresult.errortext),
              showLoading: false,
              catchMessage: this.$t('error.fetching.async.job.result'),
              catchMethod: () => { this.closeAction() }
            })
          }
        }).catch(error => {
          reject(error)
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
  width: 85vw;

  @media (min-width: 1000px) {
    width: 40vw;
  }
}
</style>
