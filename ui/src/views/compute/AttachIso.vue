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
        <a-form-item :label="$t('label.iso.name')" ref="id" name="id">
          <a-select
            :loading="loading"
            v-model:value="form.id"
            v-focus="true"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option v-for="iso in isos" :key="iso.id" :label="iso.displaytext || iso.name">
              {{ iso.displaytext || iso.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          :label="$t('label.forced')"
          v-if="resource && resource.hypervisor === 'VMware'"
          ref="forced"
          name="forced">
          <a-switch v-model:checked="form.forced" v-focus="true" />
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'

export default {
  name: 'AttachIso',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      isos: []
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        id: [{ required: true, message: `${this.$t('label.required')}` }]
      })
    },
    fetchData () {
      const isoFiters = ['featured', 'community', 'selfexecutable']
      this.loading = true
      const promises = []
      isoFiters.forEach((filter) => {
        promises.push(this.fetchIsos(filter))
      })
      Promise.all(promises).then(() => {
        this.isos = _.uniqBy(this.isos, 'id')
        if (this.isos.length > 0) {
          this.form.id = this.isos[0].id
        }
      }).catch((error) => {
        console.log(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchIsos (isoFilter) {
      const params = {
        listall: true,
        zoneid: this.resource.zoneid,
        isready: true,
        isofilter: isoFilter
      }
      return new Promise((resolve, reject) => {
        api('listIsos', params).then((response) => {
          const isos = response.listisosresponse.iso || []
          this.isos.push(...isos)
          resolve(response)
        }).catch((error) => {
          reject(error)
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {
          id: values.id,
          virtualmachineid: this.resource.id
        }

        if (values.forced) {
          params.forced = values.forced
        }

        this.loading = true
        const title = this.$t('label.action.attach.iso')
        api('attachIso', params).then(json => {
          const jobId = json.attachisoresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description: values.id,
              successMessage: `${this.$t('label.action.attach.iso')} ${this.$t('label.success')}`,
              loadingMessage: `${title} ${this.$t('label.in.progress')}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
          }
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
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
