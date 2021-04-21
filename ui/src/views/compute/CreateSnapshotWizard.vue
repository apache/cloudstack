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
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <span slot="label" :title="apiParams.volumeid.description">
            {{ $t('label.volumeid') }}
            <a-tooltip>
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            showSearch
            allowClear
            v-decorator="['volumeid', {
              rules: [{ required: true, message: $t('message.error.select') }]
            }]"
            @change="onChangeVolume"
            :placeholder="apiParams.volumeid.description"
            autoFocus>
            <a-select-option
              v-for="volume in listVolumes"
              :key="volume.id">
              {{ volume.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label" :title="apiParams.name.description">
            {{ $t('label.name') }}
            <a-tooltip>
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['name']"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item v-if="isQuiesceVm">
          <span slot="label" :title="apiParams.quiescevm.description">
            {{ $t('label.quiescevm') }}
            <a-tooltip>
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['quiescevm', { initialValue: false }]"/>
        </a-form-item>
        <a-form-item v-if="!supportsStorageSnapshot">
          <span slot="label" :title="apiParams.asyncbackup.description">
            {{ $t('label.asyncbackup') }}
            <a-tooltip>
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['asyncbackup', { initialValue: false }]"/>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'CreateSnapshotWizard',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      isQuiesceVm: false,
      supportsStorageSnapshot: false,
      listVolumes: []
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.createSnapshot || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true

      api('listVolumes', { virtualMachineId: this.resource.id })
        .then(json => {
          this.listVolumes = json.listvolumesresponse.volume || []
        })
        .catch(e => {})
        .finally(() => { this.loading = false })
    },
    handleSubmit (e) {
      e.preventDefault()

      this.form.validateFields((err, values) => {
        if (err) return

        const params = {}
        params.volumeid = values.volumeid
        params.name = values.name
        params.asyncbackup = false
        if (values.asyncbackup) {
          params.asyncbackup = values.asyncbackup
        }
        params.quiescevm = values.quiescevm

        const title = this.$t('label.action.vmstoragesnapshot.create')
        const description = values.name || values.volumeid

        this.loading = true

        api('createSnapshot', params)
          .then(json => {
            const jobId = json.createsnapshotresponse.jobid
            if (jobId) {
              this.$pollJob({
                jobId,
                successMethod: result => {
                  const volumeId = result.jobresult.snapshot.volumeid
                  const snapshotId = result.jobresult.snapshot.id
                  const message = `${this.$t('label.create.snapshot.for.volume')} ${volumeId} ${this.$t('label.with.snapshotid')} ${snapshotId}`
                  this.$notification.success({
                    message: message,
                    duration: 0
                  })
                },
                loadingMessage: `${title} ${this.$t('label.in.progress')}`,
                catchMessage: this.$t('error.fetching.async.job.result')
              })
              this.$store.dispatch('AddAsyncJob', {
                title: title,
                jobid: jobId,
                description: description,
                status: 'progress'
              })
            }
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.loading = false
            this.closeAction()
          })
      })
    },
    onChangeVolume (volumeId) {
      const volumeFilter = this.listVolumes.filter(volume => volume.id === volumeId)
      if (volumeFilter && volumeFilter.length > 0) {
        this.isQuiesceVm = volumeFilter[0].quiescevm
        this.supportsStorageSnapshot = volumeFilter[0].supportsstoragesnapshot
      }
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;
  @media (min-width: 600px) {
    width: 450px;
  }
}
.action-button {
  text-align: right;
  button {
    margin-right: 5px;
  }
}
</style>
