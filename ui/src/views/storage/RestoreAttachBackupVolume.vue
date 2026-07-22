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
      <a-form-item name="volumeid" ref="volumeid">
        <template #label>
          <tooltip-label :title="$t('label.volume')" :tooltip="apiParams.volumeid?.description"/>
        </template>
        <a-select
          allowClear
          v-model:value="form.volumeid"
          :loading="volumeOptions.loading"
          v-focus="true"
          showSearch
          optionFilterProp="label"
          :placeholder="apiParams.volumeid?.description"
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
      <a-form-item name="virtualmachineid" ref="virtualmachineid">
        <template #label>
          <tooltip-label :title="$t('label.vm')" :tooltip="apiParams.virtualmachineid?.description"/>
        </template>
        <a-select
          allowClear
          v-model:value="form.virtualmachineid"
          :loading="virtualMachineOptions.loading"
          showSearch
          optionFilterProp="value"
          :placeholder="apiParams.virtualmachineid?.description"
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
      <a-form-item name="quickRestore" ref="quickRestore" >
        <template #label>
          <tooltip-label :title="$t('label.quickrestore')" :tooltip="apiParams.quickrestore?.description"/>
        </template>
        <a-switch v-model:checked="form.quickRestore" />
      </a-form-item>
      <a-form-item name="hostId" ref="hostId" v-if="isAdmin()">
        <template #label>
          <tooltip-label :title="$t('label.hostid')" :tooltip="apiParams.hostid?.description"/>
        </template>
        <a-select
          allowClear
          v-model:value="form.hostId"
          :loading="hostOptions.loading"
          showSearch
          optionFilterProp="value"
          :placeholder="apiParams.hostid?.description"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().includes(input.toLowerCase())
          }" >
          <a-select-option
            v-for="(host) in hostOptions.opts"
            :key="host.id">
            {{ host.name }}
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
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'
import { isAdmin } from '@/role'

export default {
  name: 'RestoreAttachBackupVolume',
  components: { TooltipLabel },
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
      hostOptions: {
        loading: false,
        opts: []
      },
      actionLoading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('restoreVolumeFromBackupAndAttachToVM')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  inject: ['parentFetchData'],
  methods: {
    isAdmin,
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        quickRestore: false
      })
      this.rules = reactive({
        volumeid: [{ required: true, message: this.$t('message.error.select') }],
        virtualmachineid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchVirtualMachine()
      this.fetchVolumes()
      this.fetchHosts()
    },
    fetchVirtualMachine () {
      this.virtualMachineOptions.loading = true
      getAPI('listVirtualMachines', { zoneid: this.resource.zoneid }).then(json => {
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
    async fetchHosts () {
      this.hostOptions.loading = true
      const args = {
        zoneid: this.resource.zoneid,
        type: 'routing',
        resourcestate: 'enabled',
        state: 'up'
      }
      const hosts = (await getAPI('listHosts', args))?.listhostsresponse?.host ?? []
      this.hostOptions.opts = hosts.map((host) => {
        return {
          id: host.id,
          name: host.name
        }
      })
      this.hostOptions.loading = false
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
        params.quickrestore = values.quickRestore
        params.hostid = values.hostId

        this.actionLoading = true
        const title = this.$t('label.restore.volume.attach')
        postAPI('restoreVolumeFromBackupAndAttachToVM', params).then(json => {
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
