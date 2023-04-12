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
  <a-form class="form" layout="vertical" v-ctrl-enter="handleKeyboardSubmit">
    <a-alert class="top-spaced" type="warning">
      <template #message>
        <span v-html="$t('message.migrate.volume')" />
      </template>
    </a-alert>
    <a-form-item style="margin-top: 10px;">
      <template #label>
        <tooltip-label :title="$t('label.storagepool')" :tooltip="$t('message.migrate.volume.tooltip')"/>
      </template>
      <storage-pool-select-view
        ref="storagePoolSelection"
        :resource="resource"
        :suitabilityEnabled="true"
        @change="fetchDiskOfferings"
        @storagePoolsUpdated="handleStoragePoolsChange"
        @select="handleStoragePoolSelect" />
    </a-form-item>
    <div class="top-spaced" v-if="storagePools.length > 0">
      <div v-if="resource.virtualmachineid">
        <p class="modal-form__label" @click="replaceDiskOffering = !replaceDiskOffering" style="cursor:pointer;">
          {{ $t('label.usenewdiskoffering') }}
        </p>
        <a-checkbox v-model:checked="replaceDiskOffering" />

        <template v-if="replaceDiskOffering">
          <p class="modal-form__label">{{ $t('label.newdiskoffering') }}</p>
          <a-select
            :loading="diskOfferingLoading"
            v-model:value="selectedDiskOffering"
            style="width: 100%;"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="(diskOffering, index) in diskOfferings" :value="diskOffering.id" :key="index" :label="diskOffering.displaytext">
              {{ diskOffering.displaytext }}
            </a-select-option>
          </a-select>
        </template>
      </div>
    </div>

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" ref="submit" :disabled="!selectedStoragePool" @click="submitForm">{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import StoragePoolSelectView from '@/components/view/StoragePoolSelectView'

export default {
  name: 'MigrateVolume',
  components: {
    StoragePoolSelectView,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      storagePools: [],
      selectedStoragePool: null,
      diskOfferings: [],
      diskOfferingLoading: false,
      replaceDiskOffering: false,
      selectedDiskOffering: null,
      isSubmitted: false
    }
  },
  watch: {
    replaceDiskOffering (newValue) {
      if (newValue) {
        this.fetchDiskOfferings()
      }
    }
  },
  created () {
    this.fetchStoragePools()
  },
  methods: {
    fetchStoragePools () {
      if (this.resource.virtualmachineid) {
        api('findStoragePoolsForMigration', {
          id: this.resource.id
        }).then(response => {
          this.storagePools = response.findstoragepoolsformigrationresponse.storagepool || []
          if (Array.isArray(this.storagePools) && this.storagePools.length) {
            this.selectedStoragePool = this.storagePools[0].id || ''
            this.fetchDiskOfferings()
          }
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        })
      } else {
        api('listStoragePools', {
          zoneid: this.resource.zoneid
        }).then(response => {
          this.storagePools = response.liststoragepoolsresponse.storagepool || []
          this.storagePools = this.storagePools.filter(pool => { return pool.id !== this.resource.storageid })
          if (Array.isArray(this.storagePools) && this.storagePools.length) {
            this.selectedStoragePool = this.storagePools[0].id || ''
            this.fetchDiskOfferings()
          }
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        })
      }
    },
    fetchDiskOfferings () {
      this.diskOfferingLoading = true
      if (this.resource.virtualmachineid) {
        api('listDiskOfferings', {
          storageid: this.selectedStoragePool.id,
          listall: true
        }).then(response => {
          this.diskOfferings = response.listdiskofferingsresponse.diskoffering
          if (this.diskOfferings) {
            this.selectedDiskOffering = this.diskOfferings[0].id
          }
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        }).finally(() => {
          this.diskOfferingLoading = false
          if (this.diskOfferings.length > 0) {
            this.selectedDiskOffering = this.diskOfferings[0].id
          }
        })
      }
    },
    handleStoragePoolsChange (storagePools) {
      this.storagePools = storagePools
    },
    handleStoragePoolSelect (storagePool) {
      this.selectedStoragePool = storagePool
    },
    handleKeyboardSubmit () {
      if (!this.selectedStoragePool) {
        return
      }
      this.submitForm()
    },
    closeModal () {
      this.$emit('close-action')
    },
    submitForm () {
      if (this.isSubmitted) return
      if (this.storagePools.length === 0) {
        this.closeModal()
        return
      }
      this.isSubmitted = true
      var params = {
        livemigrate: this.resource.vmstate === 'Running',
        storageid: this.selectedStoragePool.id,
        volumeid: this.resource.id
      }
      if (this.replaceDiskOffering) {
        params.newdiskofferingid = this.selectedDiskOffering
      }
      api('migrateVolume', params).then(response => {
        this.$pollJob({
          jobId: response.migratevolumeresponse.jobid,
          successMessage: this.$t('message.success.migrate.volume'),
          errorMessage: this.$t('message.migrate.volume.failed'),
          errorMethod: () => {
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.migrate.volume.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.isSubmitted = false
          }
        })
        this.closeModal()
      }).catch(error => {
        this.$notifyError(error)
        this.isSubmitted = false
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
    width: 80vw;

    @media (min-width: 900px) {
      width: 850px;
    }
  }

  .top-spaced {
    margin-top: 20px;
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
