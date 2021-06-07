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
    <a-alert type="warning" v-html="resource.backupofferingid ? $t('message.action.destroy.instance.with.backups') : $t('message.action.destroy.instance')" /><br/>
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.allowuserexpungerecovervm">
          <span slot="label">
            {{ $t('label.expunge') }}
            <a-tooltip placement="bottom" :title="apiParams.expunge.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['expunge']" :auto-focus="true" />
        </a-form-item>

        <a-form-item v-if="volumes.length > 0">
          <span slot="label">
            {{ $t('label.delete.volumes') }}
            <a-tooltip placement="bottom" :title="apiParams.volumeids.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            v-decorator="['volumeids']"
            :placeholder="$t('label.delete.volumes')"
            mode="multiple"
            :loading="loading"
            :autoFocus="$store.getters.userInfo.roletype !== 'Admin' && !$store.getters.features.allowuserexpungerecovervm">
            <a-select-option v-for="volume in volumes" :key="volume.id">
              {{ volume.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <p v-else v-html="$t('label.volume.empty')" />

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
  name: 'DestroyVM',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      volumes: [],
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.destroyVirtualMachine || {}
    apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.volumes = []
      this.loading = true
      api('listVolumes', {
        virtualMachineId: this.resource.id,
        type: 'DATADISK',
        details: 'min',
        listall: 'true'
      }).then(json => {
        this.volumes = json.listvolumesresponse.volume || []
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
        this.loading = true

        const params = {
          id: this.resource.id
        }
        if (values.volumeids) {
          params.volumeids = values.volumeids.join(',')
        }
        if (values.expunge) {
          params.expunge = values.expunge
        }

        api('destroyVirtualMachine', params).then(json => {
          const jobId = json.destroyvirtualmachineresponse.jobid
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('label.action.destroy.instance'),
            jobid: jobId,
            description: this.resource.name,
            status: 'progress'
          })
          this.$pollJob({
            jobId,
            loadingMessage: `${this.$t('message.deleting.vm')} ${this.resource.name}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.delete.vm')} ${this.resource.name}`,
            successMethod: () => {
              if (this.$route.path.includes('/vm/') && values.expunge) {
                this.$router.go(-1)
              } else {
                this.parentFetchData()
              }
            },
            action: {
              api: 'destroyVirtualMachine'
            }
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
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
    width: 60vw;

    @media (min-width: 500px) {
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
