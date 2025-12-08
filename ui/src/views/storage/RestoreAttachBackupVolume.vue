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
    <a-form
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item name="volumeid" ref="volumeid" :label="$t('label.volume')">
        <a-select
          allowClear
          v-model:value="form.volumeid"
          :loading="volumeOptions.loading"
          v-focus="true"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(opt) in volumeOptions.opts"
            :key="opt.id"
            :label="opt.name">
            {{ opt.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="virtualmachineid" ref="virtualmachineid" :label="$t('label.vm')">
        <a-select
          allowClear
          v-model:value="form.virtualmachineid"
          :loading="virtualMachineOptions.loading"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(opt) in virtualMachineOptions.opts"
            :key="opt.name">
            {{ opt.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button :loading="loading || actionLoading" @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading || actionLoading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'RestoreAttachBackupVolume',
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    resource: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      virtualMachineOptions: {
        loading: false,
        opts: []
      },
      volumeOptions: {
        loading: false,
        opts: []
      },
      actionLoading: false
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  inject: ['parentFetchData'],
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        volumeid: [{ required: true, message: this.$t('message.error.select') }],
        virtualmachineid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchVirtualMachine()
      this.fetchVolumes()
    },
    fetchVirtualMachine () {
      this.virtualMachineOptions.loading = true
      api('listVirtualMachines', { zoneid: this.resource.zoneid }).then(json => {
        this.virtualMachineOptions.opts = json.listvirtualmachinesresponse.virtualmachine || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.virtualMachineOptions.loading = false
      })
    },
    async fetchVolumes () {
      if (!this.resource || Object.keys(this.resource).length === 0) {
        return
      }
      if (!this.resource.volumes || typeof this.resource.volumes !== 'string') {
        return
      }
      const volumes = JSON.parse(this.resource.volumes)
      this.volumeOptions.loading = true
      this.volumeOptions.opts = await volumes.map(volume => {
        return {
          id: volume.uuid,
          name: ['(', volume.type, ') ', volume.uuid].join('')
        }
      })
      this.volumeOptions.loading = false
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.actionLoading) return

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.backupid = this.resource.id
        params.volumeid = values.volumeid
        params.virtualmachineid = this.virtualMachineOptions.opts.filter(opt => opt.name === values.virtualmachineid)[0].id || null

        this.actionLoading = true
        const title = this.$t('label.restore.volume.attach')
        api('restoreVolumeFromBackupAndAttachToVM', params).then(json => {
          const jobId = json.restorevolumefrombackupandattachtovmresponse.jobid || null
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description: values.volumeid,
              loadingMessage: `${title} ${this.$t('label.in.progress.for')} ${this.resource.id}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
            this.closeAction()
          }
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 30vw;

  @media (min-width: 500px) {
    width: 400px;
  }
}
</style>
