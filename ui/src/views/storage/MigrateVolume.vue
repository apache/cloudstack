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
  <div class="migrate-volume-container">

    <div class="modal-form">
      <p class="modal-form__label">{{ $t('storagePool') }}</p>
      <a-select v-model="selectedStoragePool" style="width: 100%;">
        <a-select-option v-for="(storagePool, index) in storagePools" :value="storagePool.id" :key="index">
          {{ storagePool.name }} <span v-if="resource.virtualmachineid">{{ storagePool.suitableformigration ? '(Suitable)' : '(Not Suitable)' }}</span>
        </a-select-option>
      </a-select>
      <template v-if="this.resource.virtualmachineid">
        <p class="modal-form__label" @click="replaceDiskOffering = !replaceDiskOffering" style="cursor:pointer;">
          {{ $t('useNewDiskOffering') }}
        </p>
        <a-checkbox v-model="replaceDiskOffering" />

        <template v-if="replaceDiskOffering">
          <p class="modal-form__label">{{ $t('newDiskOffering') }}</p>
          <a-select v-model="selectedDiskOffering" style="width: 100%;">
            <a-select-option v-for="(diskOffering, index) in diskOfferings" :value="diskOffering.id" :key="index">
              {{ diskOffering.displaytext }}
            </a-select-option>
          </a-select>
        </template>

      </template>
    </div>

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('Cancel') }}
      </a-button>
      <a-button type="primary" @click="submitMigrateVolume">
        {{ $t('OK') }}
      </a-button>
    </div>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'MigrateVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  data () {
    return {
      storagePools: [],
      selectedStoragePool: null,
      diskOfferings: [],
      replaceDiskOffering: !!this.resource.virtualmachineid,
      selectedDiskOffering: null
    }
  },
  mounted () {
    this.fetchStoragePools()
    this.resource.virtualmachineid && this.fetchDiskOfferings()
  },
  methods: {
    fetchStoragePools () {
      if (this.resource.virtualmachineid) {
        api('findStoragePoolsForMigration', {
          id: this.resource.id
        }).then(response => {
          this.storagePools = response.findstoragepoolsformigrationresponse.storagepool || []
          this.selectedStoragePool = this.storagePools[0].id || ''
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        })
      } else {
        api('listStoragePools', {
          zoneid: this.resource.zoneid
        }).then(response => {
          this.storagePools = response.liststoragepoolsresponse.storagepool || []
          this.selectedStoragePool = this.storagePools[0].id || ''
        }).catch(error => {
          this.$notifyError(error)
          this.closeModal()
        })
      }
    },
    fetchDiskOfferings () {
      api('listDiskOfferings', {
        listall: true
      }).then(response => {
        this.diskOfferings = response.listdiskofferingsresponse.diskoffering
        this.selectedDiskOffering = this.diskOfferings[0].id
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    closeModal () {
      this.$parent.$parent.close()
    },
    submitMigrateVolume () {
      this.closeModal()
      this.parentToggleLoading()
      api('migrateVolume', {
        livemigrate: this.resource.vmstate === 'Running',
        storageid: this.selectedStoragePool,
        volumeid: this.resource.id,
        newdiskofferingid: this.replaceDiskOffering ? this.selectedDiskOffering : null
      }).then(response => {
        this.$pollJob({
          jobId: response.migratevolumeresponse.jobid,
          successMessage: `Successfully migrated volume`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: 'Migrating volume failed',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: `Migrating volume...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .migrate-volume-container {
    width: 95vw;
    max-width: 100%;

    @media (min-width: 760px) {
      width: 50vw;
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

  .modal-form {
    margin-top: -20px;

    &__label {
      margin-top: 10px;
      margin-bottom: 5px;
    }

  }
</style>
