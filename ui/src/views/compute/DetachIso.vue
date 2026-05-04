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
        <a-form-item
          :label="$t('label.iso.name') + ' (' + form.ids.length + ' / ' + attached.length + ')'"
          ref="ids"
          name="ids">
          <a-select
            mode="multiple"
            :loading="loading"
            v-model:value="form.ids"
            v-focus="true">
            <a-select-option
              v-for="iso in attached"
              :key="iso.id"
              :label="iso.displaytext || iso.name">
              {{ (iso.displaytext || iso.name) + ' (' + slotLabel(iso.deviceseq) + ')' }}
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
import { postAPI } from '@/api'

export default {
  name: 'DetachIso',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      attached: []
    }
  },
  created () {
    this.initForm()
    this.populateAttached()
  },
  methods: {
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
    populateAttached () {
      // Prefer the multi-ISO array from listVirtualMachines (FR283); fall back to the legacy single isoid.
      if (this.resource.isos && this.resource.isos.length > 0) {
        this.attached = [...this.resource.isos].sort((a, b) => (a.deviceseq || 0) - (b.deviceseq || 0))
      } else if (this.resource.isoid) {
        this.attached = [{
          id: this.resource.isoid,
          name: this.resource.isoname,
          displaytext: this.resource.isodisplaytext,
          deviceseq: 3
        }]
      }
      // If only one is attached, pre-select it so the user just clicks OK.
      if (this.attached.length === 1) {
        this.form.ids = [this.attached[0].id]
      }
    },
    slotLabel (deviceseq) {
      // Map device_seq to its libvirt label so users can see which drive they're detaching.
      // 3 -> hdc, 4 -> hdd, 5 -> hde ... matches LibvirtVMDef.getDevLabel for IDE bus.
      if (typeof deviceseq !== 'number') return ''
      const offset = deviceseq - 1
      return 'hd' + String.fromCharCode('a'.charCodeAt(0) + offset)
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
        const title = this.$t('label.action.detach.iso')
        // CloudStack's detachIso API is single-ISO. We fan out one call per selected ISO and
        // surface the first error if any.
        const sendOne = (isoId) => {
          const params = {
            virtualmachineid: this.resource.id
          }
          // Always send id when we have it (multi-attached VMs require it server-side; single-attached
          // tolerates it just fine).
          if (this.attached.length > 1 || ids.length > 1) {
            params.id = isoId
          } else if (this.attached.length === 1) {
            // Single attached and the user picked it: omit id for back-compat with older servers.
          } else {
            params.id = isoId
          }
          if (values.forced) {
            params.forced = values.forced
          }
          return new Promise((resolve, reject) => {
            postAPI('detachIso', params).then(json => {
              const jobId = json.detachisoresponse && json.detachisoresponse.jobid
              if (jobId) {
                this.$pollJob({
                  jobId,
                  title,
                  description: isoId,
                  successMessage: `${this.$t('label.action.detach.iso')} ${this.$t('label.success')}`,
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
