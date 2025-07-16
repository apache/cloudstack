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
      <div v-if="vms.length > 0">
        <a-form-item name="nodeids" ref="nodeids">
          <template #label>
            <tooltip-label :title="$t('label.add.nodes')" :tooltip="apiParams.nodeids.description"/>
          </template>
          <a-select
            v-model:value="form.nodeids"
            :placeholder="$t('label.add.nodes')"
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
        <a-form-item name="mountcksiso" ref="mountcksiso">
          <a-checkbox v-model:checked="form.mountcksiso">
            {{ $t('label.mount.cks.iso.on.vr') }}
          </a-checkbox>
        </a-form-item>
        <a-form-item name="manualupgrade" ref="manualupgrade">
          <a-checkbox v-model:checked="form.manualupgrade">
            {{ $t('label.cks.cluster.node.manual.upgrade') }}
          </a-checkbox>
        </a-form-item>
      </div>
      <p v-else v-html="$t('label.vms.empty')" />

      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
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
    this.apiParams = this.$getApiParams('addNodesToKubernetesCluster')
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
      this.fetchVms()
    },
    async fetchVms () {
      this.loading = true
      this.vms = await this.callListVms(this.resource.accountid, this.resource.domainid)
      const cksVms = this.resource.virtualmachines.map(vm => vm.id)
      this.vms = this.vms.filter(vm => !cksVms.includes(vm.id))
      this.loading = false
    },
    callListVms (accountId, domainId) {
      return new Promise((resolve) => {
        this.volumes = []
        getAPI('listVirtualMachines', {
          accountId: accountId,
          domainId: domainId,
          details: 'min',
          listall: 'true',
          networkid: this.resource.networkid
        }).then(json => {
          const vms = json.listvirtualmachinesresponse.virtualmachine || []
          resolve(vms)
        })
      })
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
        if (values.mountcksiso) {
          params.mountcksisoonvr = values.mountcksiso
        }
        if (values.manualupgrade) {
          params.manualupgrade = values.manualupgrade
        }
        this.loading = true
        try {
          const jobId = await this.addNodesToKubernetesCluster(params)
          await this.$pollJob({
            jobId,
            title: this.$t('label.action.add.nodes.to.kubernetes.cluster'),
            description: this.resource.name,
            loadingMessage: `${this.$t('message.adding.nodes.to.cluster')} ${this.resource.name}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.add.nodes.to.cluster')} ${this.resource.name}`,
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
    addNodesToKubernetesCluster (params) {
      return new Promise((resolve, reject) => {
        postAPI('addNodesToKubernetesCluster', params).then(json => {
          const jobId = json.addnodestokubernetesclusterresponse.jobid
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
