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
        layout="vertical">
        <a-form-item :label="$t('label.iso.name')">
          <a-select
            :loading="loading"
            v-decorator="['id', {
              initialValue: this.selectedIso,
              rules: [{ required: true, message: `${this.$t('label.required')}`}]
            }]"
            autoFocus>
            <a-select-option v-for="iso in isos" :key="iso.id">
              {{ iso.displaytext || iso.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.forced')" v-if="resource && resource.hypervisor === 'VMware'">
          <a-switch v-decorator="['forced']" :auto-focus="true" />
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>
<script>
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
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      selectedIso: '',
      isos: []
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  methods: {
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
          this.selectedIso = this.isos[0].id
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
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
              successMethod: result => {
                this.$store.dispatch('AddAsyncJob', {
                  title: title,
                  jobid: jobId,
                  status: this.$t('progress')
                })
                this.parentFetchData()
              },
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

.action-button {
  text-align: right;
  button {
    margin-right: 5px;
  }
}
</style>
