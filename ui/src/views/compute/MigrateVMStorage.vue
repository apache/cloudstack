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
  <div class="form-layout" v-ctrl-enter="handleKeyboardSubmit">
    <a-alert type="warning">
      <template #message>
        <span v-html="$t('message.migrate.instance.to.ps')" />
      </template>
    </a-alert>
    <a-radio-group
      v-if="migrateVmWithVolumeAllowed"
      :defaultValue="migrateMode"
      @change="e => { handleMigrateModeChange(e.target.value) }">
      <a-radio class="radio-style" :value="1">
        {{ $t('label.migrate.instance.single.storage') }}
      </a-radio>
      <a-radio class="radio-style" :value="2">
        {{ $t('label.migrate.instance.specific.storages') }}
      </a-radio>
    </a-radio-group>
    <div v-if="migrateMode == 1">
      <storage-pool-select-view
        ref="storagePoolSelection"
        :resource="resource"
        @select="handleStoragePoolChange" />
    </div>
    <instance-volumes-storage-pool-select-list-view
      v-else
      :resource="resource"
      @select="handleVolumeToPoolChange" />

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" :disabled="!formSubmitAllowed" @click="submitForm">{{ $t('label.ok') }}</a-button>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import StoragePoolSelectView from '@/components/view/StoragePoolSelectView'
import InstanceVolumesStoragePoolSelectListView from '@/components/view/InstanceVolumesStoragePoolSelectListView'

export default {
  name: 'MigrateVMStorage',
  components: {
    TooltipLabel,
    StoragePoolSelectView,
    InstanceVolumesStoragePoolSelectListView
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      migrateMode: 1,
      selectedPool: {},
      volumeToPoolSelection: []
    }
  },
  beforeCreate () {
    this.migrateVmWithVolumeApiParams = this.$getApiParams('migrateVirtualMachineWithVolume')
  },
  computed: {
    migrateVmWithVolumeAllowed () {
      return this.$route.meta.name === 'vm' && this.migrateVmWithVolumeApiParams.hostid && this.migrateVmWithVolumeApiParams.hostid.required === false
    },
    formSubmitAllowed () {
      return this.migrateMode === 2 ? this.volumeToPoolSelection.length > 0 : this.selectedPool.id
    },
    isSelectedVolumeOnlyClusterStoragePoolVolume () {
      if (this.volumesWithClusterStoragePool.length !== 1) {
        return false
      }
      for (const volume of this.volumesWithClusterStoragePool) {
        if (volume.id === this.selectedVolumeForStoragePoolSelection.id) {
          return true
        }
      }
      return false
    }
  },
  methods: {
    fetchData () {
      if (this.migrateMode === 2) {
        this.fetchVolumes()
      }
    },
    handleMigrateModeChange (value) {
      this.migrateMode = value
      this.selectedPool = {}
      this.volumeToPoolSelection = []
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
    handleStoragePoolChange (storagePool) {
      this.selectedPool = storagePool
    },
    handleVolumeToPoolChange (volumeToPool) {
      this.volumeToPoolSelection = volumeToPool
    },
    handleKeyboardSubmit () {
      if (this.formSubmitAllowed) {
        this.submitForm()
      }
    },
    submitForm () {
      var isUserVm = true
      if (this.$route.meta.name !== 'vm') {
        isUserVm = false
      }
      var migrateApi = isUserVm ? 'migrateVirtualMachine' : 'migrateSystemVm'
      if (isUserVm && this.migrateMode === 2) {
        migrateApi = 'migrateVirtualMachineWithVolume'
        this.migrateVm(migrateApi, null, this.volumeToPoolSelection)
        return
      }
      this.migrateVm(migrateApi, this.selectedPool.id, null)
    },
    migrateVm (migrateApi, storageId, volumeToPool) {
      var params = {
        virtualmachineid: this.resource.id
      }
      if (this.migrateMode === 2) {
        for (var i = 0; i < volumeToPool.length; i++) {
          const mapping = volumeToPool[i]
          params['migrateto[' + i + '].volume'] = mapping.volume
          params['migrateto[' + i + '].pool'] = mapping.pool
        }
      } else {
        params.storageid = storageId
      }
      api(migrateApi, params).then(response => {
        const jobId = response[migrateApi.toLowerCase() + 'response'].jobid
        this.$pollJob({
          title: `${this.$t('label.migrating')} ${this.resource.name}`,
          description: this.resource.name,
          jobId: jobId,
          successMessage: `${this.$t('message.success.migrating')} ${this.resource.name}`,
          successMethod: () => {
            this.closeModal()
          },
          errorMessage: this.$t('message.migrating.failed'),
          errorMethod: () => {
            this.closeModal()
          },
          loadingMessage: `${this.$t('message.migrating.processing')} ${this.resource.name}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.closeModal()
          }
        })
        this.closeModal()
      }).catch(error => {
        console.error(error)
        this.$message.error(`${this.$t('message.migrating.vm.to.storage.failed')} ${storageId}`)
      })
    },
    closeModal () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 900px) {
      width: 850px;
    }
  }

  .top-spaced {
    margin-top: 20px;
  }

  .radio-style {
    display: block;
    margin-left: 10px;
    height: 40px;
    line-height: 40px;
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
