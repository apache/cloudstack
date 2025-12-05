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
    <a-form
      :ref="formRef"
      :model="form"
      :loading="loading"
      layout="vertical"
      @finish="handleSubmit">
      <a-alert :message="$t('message.extension.sync')" type="warning" banner style="margin-bottom: 20px;" />
      <a-form-item ref="sourcemanagementserverid" name="sourcemanagementserverid">
        <template #label>
          <tooltip-label :title="$t('label.sourcemanagementserverid')" :tooltip="apiParams.sourcemanagementserverid.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.sourcemanagementserverid"
          :placeholder="apiParams.sourcemanagementserverid.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in managementServers.opts" :key="opt.id" :label="opt.description || opt.id">
            {{ opt.name || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="targetmanagementserverids" name="targetmanagementserverids">
        <template #label>
          <tooltip-label :title="$t('label.targetmanagementserverids')" :tooltip="apiParams.targetmanagementserverids.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.targetmanagementserverids"
          :placeholder="apiParams.targetmanagementserverids.description"
          mode="multiple"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in targetManagementServers" :key="opt.id" :label="opt.name || opt.description">
            {{ opt.name || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'SyncExtension',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    TooltipLabel
  },
  data () {
    return {
      managementServers: {
        loading: false,
        opts: []
      },
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('syncExtension')
  },
  created () {
    this.initForm()
    this.fetchManagementServers()
  },
  computed: {
    targetManagementServers () {
      return this.managementServers.opts.filter(opt => opt.id !== this.form.sourcemanagementserverid)
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        sourcemanagementserverid: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    fetchManagementServers () {
      this.managementServers.opts = []
      this.managementServers.loading = true
      const params = {
        state: 'Up'
      }
      const api = 'listManagementServers'
      getAPI(api, params).then(json => {
        this.managementServers.opts = json?.[api.toLowerCase() + 'response']?.managementserver || []
      }).finally(() => {
        this.managementServers.loading = false
        this.form.sourcemanagementserverid = this.managementServers?.opts?.[0]?.id || null
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          id: this.resource.id,
          sourcemanagementserverid: values.sourcemanagementserverid
        }
        if (values.targetmanagementserverids) {
          params.targetmanagementserverids = values.targetmanagementserverids.join(',')
        }
        postAPI('syncExtension', params).then(response => {
          this.$pollJob({
            jobId: response.syncextensionresponse.jobid,
            title: this.$t('label.sync.extension'),
            description: this.resource.id,
            successMessage: this.$t('message.success.sync.extension'),
            successMethod: () => {
              this.dedicatedDomainId = null
            },
            errorMessage: this.$t('message.error.sync.extension'),
            loadingMessage: this.$t('message.processing.sync.extension'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.parentFetchData()
            }
          })
        }).finally(() => {
          this.loading = false
          this.closeAction()
        })
      }).catch(error => {
        this.$notifyError(error)
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
  width: 80vw;
  @media (min-width: 600px) {
    width: 550px;
  }
}
</style>
