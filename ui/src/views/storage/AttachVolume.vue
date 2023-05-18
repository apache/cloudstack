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
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <div style="margin-bottom: 10px">
        <a-alert type="warning">
          <template #message>
            <div v-html="$t('message.confirm.attach.disk')"></div>
          </template>
        </a-alert>
      </div>
      <a-form-item :label="$t('label.virtualmachineid')" name="virtualmachineid" ref="virtualmachineid">
        <a-select
          v-focus="true"
          v-model:value="form.virtualmachineid"
          :placeholder="apiParams.virtualmachineid.description"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="vm in virtualmachines" :key="vm.id" :label="vm.name || vm.displayname">
            {{ vm.name || vm.displayname }}
          </a-select-option>
        </a-select>
      </a-form-item >
      <a-form-item :label="$t('label.deviceid')">
        <div style="margin-bottom: 10px">
          <a-collapse>
            <a-collapse-panel header="More information about deviceID">
              <a-alert type="warning">
                <template #message>
                  <span v-html="apiParams.deviceid.description" />
                </template>
              </a-alert>
            </a-collapse-panel>
          </a-collapse>
        </div>
        <a-input-number
          v-model:value="form.deviceid"
          style="width: 100%;"
          :min="0"
          :placeholder="$t('label.deviceid')"
        />
      </a-form-item>
    </a-form>
    <div class="actions">
      <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
    </div>
  </a-spin>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'AttachVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      virtualmachines: [],
      loading: true
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('attachVolume')
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
        virtualmachineid: [{ required: true, message: this.$t('message.error.select') }],
        deviceid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
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
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        this.loading = true
        api('attachVolume', {
          id: this.resource.id,
          virtualmachineid: values.virtualmachineid,
          deviceid: values.deviceid
        }).then(response => {
          this.$pollJob({
            jobId: response.attachvolumeresponse.jobid,
            title: this.$t('label.action.attach.disk'),
            description: this.resource.id,
            errorMessage: `${this.$t('message.attach.volume.failed')}: ${this.resource.name || this.resource.id}`,
            loadingMessage: `${this.$t('message.attach.volume.progress')}: ${this.resource.name || this.resource.id}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
