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
    <a-form layout="vertical" :form="form">
      <a-form-item :label="$t('label.volume')">
        <a-select
          allowClear
          v-decorator="['volumeid', {
            rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
          }]"
          :loading="volumeOptions.loading"
          autoFocus>
          <a-select-option
            v-for="(opt) in volumeOptions.opts"
            :key="opt.id">
            {{ opt.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="$t('label.vm')">
        <a-select
          showSearch
          allowClear
          v-decorator="['virtualmachineid', {
            rules: [{ required: true, message: `${this.$t('message.error.select')}` }]
          }]"
          :loading="virtualMachineOptions.loading">
          <a-select-option
            v-for="(opt) in virtualMachineOptions.opts"
            :key="opt.name">
            {{ opt.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button :loading="loading || actionLoading" @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading || actionLoading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
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
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  inject: ['parentFetchData'],
  methods: {
    fetchData () {
      this.fetchVirtualMachine()
      this.fetchVolumes()
    },
    fetchVirtualMachine () {
      this.virtualMachineOptions.loading = true
      api('listVirtualMachines', { zoneid: this.resource.zoneid }).then(json => {
        this.virtualMachineOptions.opts = json.listvirtualmachinesresponse.virtualmachine || []
        this.$forceUpdate()
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
      this.$forceUpdate()
    },
    handleSubmit (e) {
      e.preventDefault()

      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
              successMethod: result => {
                const successDescription = result.jobresult.storagebackup.name
                this.$store.dispatch('AddAsyncJob', {
                  title: title,
                  jobid: jobId,
                  description: successDescription,
                  status: 'progress'
                })
                this.parentFetchData()
                this.closeAction()
              },
              loadingMessage: `${title} ${this.$t('label.in.progress.for')} ${this.resource.id}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
          }
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
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

  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
}
</style>
