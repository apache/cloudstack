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
      <div v-if="storagePools.length > 0">
        <a-alert type="warning">
          <span slot="message" v-html="$t('message.migrate.volume')" />
        </a-alert>
        <p class="modal-form__label">{{ $t('label.storagepool') }}</p>
        <a-select
          v-model="selectedStoragePool"
          style="width: 100%;"
          :autoFocus="storagePools.length > 0">
          <a-select-option v-for="(storagePool, index) in storagePools" :value="storagePool.id" :key="index">
            {{ storagePool.name }} <span v-if="resource.virtualmachineid">{{ storagePool.suitableformigration ? `(${$t('label.suitable')})` : `(${$t('label.not.suitable')})` }}</span>
          </a-select-option>
        </a-select>
        <template v-if="this.resource.virtualmachineid">
          <p class="modal-form__label" @click="replaceDiskOffering = !replaceDiskOffering" style="cursor:pointer;">
            {{ $t('label.usenewdiskoffering') }}
          </p>
          <a-checkbox v-model="replaceDiskOffering" />

          <template v-if="replaceDiskOffering">
            <p class="modal-form__label">{{ $t('label.newdiskoffering') }}</p>
            <a-select v-model="selectedDiskOffering" style="width: 100%;">
              <a-select-option v-for="(diskOffering, index) in diskOfferings" :value="diskOffering.id" :key="index">
                {{ diskOffering.displaytext }}
              </a-select-option>
            </a-select>
          </template>
        </template>
      </div>
      <a-alert style="margin-top: 15px" type="warning" v-else>
        <span slot="message" v-html="$t('message.no.primary.stores')" />
      </a-alert>
    </div>

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('label.cancel') }}
      </a-button>
      <a-button type="primary" @click="submitMigrateVolume">
        {{ $t('label.ok') }}
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
  inject: ['parentFetchData'],
  data () {
    return {
      storagePools: [],
      selectedStoragePool: null,
      diskOfferings: [],
      replaceDiskOffering: false,
      selectedDiskOffering: null
    }
  },
  created () {
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
          if (Array.isArray(this.storagePools) && this.storagePools.length) {
            this.selectedStoragePool = this.storagePools[0].id || ''
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
          }
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
      if (this.storagePools.length === 0) {
        this.closeModal()
        return
      }
      api('migrateVolume', {
        livemigrate: this.resource.vmstate === 'Running',
        storageid: this.selectedStoragePool,
        volumeid: this.resource.id,
        newdiskofferingid: this.replaceDiskOffering ? this.selectedDiskOffering : null
      }).then(response => {
        this.$pollJob({
          jobId: response.migratevolumeresponse.jobid,
          successMessage: this.$t('message.success.migrate.volume'),
          successMethod: () => {
            this.parentFetchData()
          },
          errorMessage: this.$t('message.migrate.volume.failed'),
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.migrate.volume.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
          }
        })
        this.closeModal()
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .migrate-volume-container {
    width: 85vw;

    @media (min-width: 760px) {
      width: 500px;
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
