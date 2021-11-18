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
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical"
        :scrollToFirstError="true">
        <a-form-item name="storageid" ref="storageid">
          <template #label>
            <tooltip-label :title="$t('label.storageid')" :tooltip="apiParams.storageid ? apiParams.storageid.description : ''"/>
          </template>
          <a-select
            :loading="loading"
            v-model:value="form.storageid"
            :placeholder="apiParams.storageid ? apiParams.storageid.description : ''"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option v-for="storagePool in storagePools" :key="storagePool.id">
              {{ storagePool.name || storagePool.id }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import { ref, reactive, toRaw } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'MigrateVMStorage',
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
      loading: false,
      storagePools: []
    }
  },
  beforeCreate () {
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
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({ storageid: [{ required: true, message: this.$t('message.error.required.input') }] })
    },
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
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
        this.$pollJob({
          title: `${this.$t('label.migrating')} ${this.resource.name}`,
          description: this.resource.name,
          jobId: jobId,
          successMessage: `${this.$t('message.success.migrating')} ${this.resource.name}`,
          successMethod: () => {
            this.$emit('close-action')
          },
          errorMessage: this.$t('message.migrating.failed'),
          errorMethod: () => {
            this.$emit('close-action')
          },
          loadingMessage: `${this.$t('message.migrating.processing')} ${this.resource.name}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.$emit('close-action')
          }
        })
        this.$emit('close-action')
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
