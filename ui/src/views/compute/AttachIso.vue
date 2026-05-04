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
      <a-alert
        v-if="!loading && maxSelections === 0"
        type="warning"
        showIcon
        :message="$t('label.iso.name') + ': max reached'"
        style="margin-bottom: 12px;" />
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item
          :label="$t('label.iso.name') + ' (' + form.ids.length + ' / ' + maxSelections + ')'"
          ref="ids"
          name="ids">
          <a-select
            mode="multiple"
            :loading="loading"
            v-model:value="form.ids"
            v-focus="true"
            :disabled="maxSelections === 0"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option
              v-for="iso in isos"
              :key="iso.id"
              :label="iso.displaytext || iso.name"
              :disabled="form.ids.length >= maxSelections && !form.ids.includes(iso.id)">
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
import { getAPI, postAPI } from '@/api'
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
      isos: [],
      maxSelections: 1,
      // Sentinel so the hypervisor cap alone gates the UI until listConfigurations resolves.
      globalCdromCap: Number.MAX_SAFE_INTEGER
    }
  },
  created () {
    this.initForm()
    this.fetchGlobalCap().then(() => this.computeMaxSelections())
    this.fetchData()
  },
  watch: {
    'form.ids' (newVal) {
      if (newVal && newVal.length > this.maxSelections) {
        this.form.ids = newVal.slice(0, this.maxSelections)
        this.$message.warning(this.$t('label.iso.name') + ': max ' + this.maxSelections)
      }
    }
  },
  methods: {
    fetchGlobalCap () {
      return new Promise((resolve) => {
        getAPI('listConfigurations', { name: 'vm.cdrom.max.count' }).then(json => {
          const cfg = json && json.listconfigurationsresponse && json.listconfigurationsresponse.configuration
          if (cfg && cfg.length > 0 && cfg[0].value !== undefined && cfg[0].value !== null) {
            const parsed = parseInt(cfg[0].value, 10)
            if (!isNaN(parsed)) {
              this.globalCdromCap = parsed
            }
          }
        }).catch(() => { /* Sentinel cap remains; hypervisor cap still applies. */ })
          .finally(resolve)
      })
    },
    computeMaxSelections () {
      // Mirrors server-side effectiveMaxCdroms: min(vm.cdrom.max.count, hypervisor cap).
      const hypervisorCap = this.resource.hypervisor === 'KVM' ? 2 : 1
      const effectiveCap = Math.min(this.globalCdromCap, hypervisorCap)
      const alreadyAttached = (this.resource.isos && this.resource.isos.length) ||
        (this.resource.isoid ? 1 : 0)
      this.maxSelections = Math.max(0, effectiveCap - alreadyAttached)
    },
    initForm () {
      this.formRef = ref()
      this.form = reactive({ ids: [] })
      this.rules = reactive({
        ids: [{
          required: true,
          type: 'array',
          min: 1,
          message: `${this.$t('label.required')}`
        }]
      })
    },
    fetchData () {
      const isoFilters = ['featured', 'community', 'selfexecutable']
      this.loading = true
      const promises = []
      isoFilters.forEach((filter) => {
        promises.push(this.fetchIsos(filter))
      })
      Promise.all(promises).then(() => {
        this.isos = _.uniqBy(this.isos, 'id')
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
        isofilter: isoFilter,
        isready: true
      }
      return new Promise((resolve, reject) => {
        getAPI('listIsos', params).then((response) => {
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
        const ids = values.ids || []
        if (ids.length === 0) return

        this.loading = true
        const title = this.$t('label.action.attach.iso')
        // attachIso is single-ISO server-side; fan out one call per selection.
        const sendOne = (isoId) => {
          const params = {
            id: isoId,
            virtualmachineid: this.resource.id
          }
          if (values.forced) {
            params.forced = values.forced
          }
          return new Promise((resolve, reject) => {
            postAPI('attachIso', params).then(json => {
              const jobId = json.attachisoresponse && json.attachisoresponse.jobid
              if (jobId) {
                this.$pollJob({
                  jobId,
                  title,
                  description: isoId,
                  successMessage: `${this.$t('label.action.attach.iso')} ${this.$t('label.success')}`,
                  loadingMessage: `${title} ${this.$t('label.in.progress')}`,
                  catchMessage: this.$t('error.fetching.async.job.result')
                })
              }
              resolve()
            }).catch(reject)
          })
        }

        ids.reduce((p, id) => p.then(() => sendOne(id)), Promise.resolve())
          .then(() => { this.closeAction() })
          .catch(error => { this.$notifyError(error) })
          .finally(() => { this.loading = false })
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
