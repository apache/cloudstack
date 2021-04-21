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
  <a-spin :spinning="loading">
    <a-form class="form" :form="form" @submit="handleSubmit" layout="vertical">
      <div style="margin-bottom: 10px">
        <a-alert type="warning">
          <span slot="message" v-html="$t('message.confirm.attach.disk')" />
        </a-alert>
      </div>
      <a-form-item :label="$t('label.virtualmachineid')">
        <a-select
          autoFocus
          v-decorator="['virtualmachineid', {
            rules: [{ required: true, message: $t('message.error.select') }]
          }]"
          :placeholder="apiParams.virtualmachineid.description">
          <a-select-option v-for="vm in virtualmachines" :key="vm.id">
            {{ vm.name || vm.displayname }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
    <div class="actions">
      <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
    </div>
  </a-spin>
</template>
<script>
import { api } from '@/api'

export default {
  name: 'AttachVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      virtualmachines: [],
      loading: true
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.attachVolume || {}
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
      var params = {
        zoneid: this.resource.zoneid
      }
      if (this.resource.hypervisor && this.resource.hypervisor !== 'None') {
        params.hypervisor = this.resource.hypervisor
      }
      if (this.resource.projectid) {
        params.projectid = this.resource.projectid
      } else {
        params.account = this.resource.account
        params.domainid = this.resource.domainid
      }

      this.loading = true
      var vmStates = ['Running', 'Stopped']
      vmStates.forEach((state) => {
        params.state = state
        api('listVirtualMachines', params).then(response => {
          this.virtualmachines = this.virtualmachines.concat(response.listvirtualmachinesresponse.virtualmachine || [])
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
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

        this.loading = true
        api('attachVolume', {
          id: this.resource.id,
          virtualmachineid: values.virtualmachineid
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('label.action.attach.disk'),
            jobid: response.attachvolumeresponse.jobid,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.attachvolumeresponse.jobid,
            successMethod: () => {
              this.parentFetchData()
            },
            errorMessage: `${this.$t('message.attach.volume.failed')}: ${this.resource.name || this.resource.id}`,
            loadingMessage: `${this.$t('message.attach.volume.progress')}: ${this.resource.name || this.resource.id}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
          this.parentFetchData()
        })
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    width: 400px;
  }
}
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
</style>
