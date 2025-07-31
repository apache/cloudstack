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
    :model="form"
    :ref="formRef"
    :rules="rules"
    @finish="handleSubmit"
    layout="vertical">
    <a-form-item v-if="vms.length > 0" name="nodeids" ref="nodeids">
      <template #label>
        <tooltip-label :title="$t('label.remove.nodes')" :tooltip="apiParams.nodeids.description"/>
      </template>
      <a-select
        v-model:value="form.nodeids"
        :placeholder="$t('label.remove.nodes')"
        mode="multiple"
        :loading="loading"
        showSearch
        optionFilterProp="label"
        :filterOption="(input, option) => {
          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }" >
        <a-select-option v-for="vm in vms" :key="vm.id" :label="vm.name">
          {{ vm.name }}
        </a-select-option>
      </a-select>
    </a-form-item>
    <p v-else v-html="$t('label.vms.remove.empty')" />

    <div :span="24" class="action-button">
      <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
      <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
</a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddNodesToKubernetesCluster',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      vms: [],
      loading: false
    }
  },
  inject: ['parentFetchData'],
  beforeCreate () {
    this.apiParams = this.$getApiParams('removeNodesFromKubernetesCluster')
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({
      nodeids: [{ type: 'array' }]
    })
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchClusterVms()
    },
    async fetchClusterVms () {
      this.loading = true
      this.vms = this.resource.virtualmachines.filter(vm => vm.isexternalnode === true) || []
      this.loading = false
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const values = toRaw(this.form)
        const params = {
          id: this.resource.id
        }
        if (values.nodeids) {
          params.nodeids = values.nodeids.join(',')
        }
        this.loading = true
        try {
          const jobId = await this.removeNodesFromKubernetesCluster(params)
          await this.$pollJob({
            jobId,
            title: this.$t('label.action.remove.nodes.from.kubernetes.cluster'),
            description: this.resource.name,
            loadingMessage: `${this.$t('message.removing.nodes.from.cluster')} ${this.resource.name}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.remove.nodes.from.cluster')} ${this.resource.name}`,
            successMethod: () => {
              this.parentFetchData()
            },
            action: {
              isFetchData: false
            }
          })
          this.closeAction()
          this.loading = false
        } catch (error) {
          await this.$notifyError(error)
          this.closeAction()
          this.loading = false
        }
      })
    },
    removeNodesFromKubernetesCluster (params) {
      return new Promise((resolve, reject) => {
        postAPI('removeNodesFromKubernetesCluster', params).then(json => {
          const jobId = json.removenodesfromkubernetesclusterresponse.jobid
          return resolve(jobId)
        }).catch(error => {
          return reject(error)
        })
      })
    }
  }
}
</script>
<style lang="scss" scoped></style>
