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
          <span slot="label">
            {{ $t('label.storageid') }}
            <a-tooltip :title="apiParams.storageid.description" v-if="!(apiParams.hostid && apiParams.hostid.required === false)">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            :loading="loading"
            v-decorator="['storageid', {
              rules: [{ required: true, message: `${this.$t('message.error.required.input')}` }]
            }]">
            <a-select-option v-for="storagePool in storagePools" :key="storagePool.id">
              {{ storagePool.name || storagePool.id }}
            </a-select-option>
          </a-select>
        </a-form-item>

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
  name: 'MigrateVMStorage',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      storagePools: []
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    if (this.$route.meta.name === 'vm') {
      this.apiConfig = this.$store.getters.apis.migrateVirtualMachineWithVolume || {}
      this.apiConfig.params.forEach(param => {
        this.apiParams[param.name] = param
      })
      this.apiConfig = this.$store.getters.apis.migrateVirtualMachine || {}
      this.apiConfig.params.forEach(param => {
        if (!(param.name in this.apiParams)) {
          this.apiParams[param.name] = param
        }
      })
    } else {
      this.apiConfig = this.$store.getters.apis.migrateSystemVm || {}
      this.apiConfig.params.forEach(param => {
        if (!(param.name in this.apiParams)) {
          this.apiParams[param.name] = param
        }
      })
    }
  },
  created () {
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listStoragePools', {
        zoneid: this.resource.zoneid
      }).then(response => {
        if (this.arrayHasItems(response.liststoragepoolsresponse.storagepool)) {
          this.storagePools = response.liststoragepoolsresponse.storagepool
        }
      }).finally(() => {
        this.loading = false
      })
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        var isUserVm = true
        if (this.$route.meta.name !== 'vm') {
          isUserVm = false
        }
        var migrateApi = isUserVm ? 'migrateVirtualMachine' : 'migrateSystemVm'
        if (isUserVm && this.apiParams.hostid && this.apiParams.hostid.required === false) {
          migrateApi = 'migrateVirtualMachineWithVolume'
          var rootVolume = null
          api('listVolumes', {
            listAll: true,
            virtualmachineid: this.resource.id
          }).then(response => {
            var volumes = response.listvolumesresponse.volume
            if (volumes && volumes.length > 0) {
              volumes = volumes.filter(item => item.type === 'ROOT')
              if (volumes && volumes.length > 0) {
                rootVolume = volumes[0]
              }
              if (rootVolume == null) {
                this.$message.error('Failed to find ROOT volume for the VM ' + this.resource.id)
                this.closeAction()
              }
              this.migrateVm(migrateApi, values.storageid, rootVolume.id)
            }
          })
          return
        }
        this.migrateVm(migrateApi, values.storageid, null)
      })
    },
    migrateVm (migrateApi, storageId, rootVolumeId) {
      var params = {
        virtualmachineid: this.resource.id,
        storageid: storageId
      }
      if (rootVolumeId !== null) {
        params = {
          virtualmachineid: this.resource.id,
          'migrateto[0].volume': rootVolumeId,
          'migrateto[0].pool': storageId
        }
      }
      api(migrateApi, params).then(response => {
        var jobId = ''
        if (migrateApi === 'migrateVirtualMachineWithVolume') {
          jobId = response.migratevirtualmachinewithvolumeresponse.jobid
        } else if (migrateApi === 'migrateSystemVm') {
          jobId = response.migratesystemvmresponse.jobid
        } else {
          jobId = response.migratevirtualmachine.jobid
        }
        this.$store.dispatch('AddAsyncJob', {
          title: `${this.$t('label.migrating')} ${this.resource.name}`,
          jobid: jobId,
          description: this.resource.name,
          status: 'progress'
        })
        this.$pollJob({
          jobId: jobId,
          successMessage: `${this.$t('message.success.migrating')} ${this.resource.name}`,
          successMethod: () => {
            this.$parent.$parent.close()
          },
          errorMessage: this.$t('message.migrating.failed'),
          errorMethod: () => {
            this.$parent.$parent.close()
          },
          loadingMessage: `${this.$t('message.migrating.processing')} ${this.resource.name}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.$parent.$parent.close()
          }
        })
        this.$parent.$parent.close()
      }).catch(error => {
        console.error(error)
        this.$message.error(`${this.$t('message.migrating.vm.to.storage.failed')} ${storageId}`)
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
