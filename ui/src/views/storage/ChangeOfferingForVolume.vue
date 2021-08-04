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
  <div class="change-offering-for-volume-container">
    <div class="modal-form">
      <a-alert type="warning">
        <span slot="message" v-html="$t('message.confirm.change.offering.for.volume')" />
      </a-alert>
      <p class="modal-form__label">{{ $t('label.newdiskoffering') }}</p>
      <a-select v-model="selectedDiskOffering" style="width: 100%;">
        <a-select-option v-for="(diskOffering, index) in diskOfferings" :value="diskOffering.id" :key="index">
          {{ diskOffering.displaytext }}
        </a-select-option>
      </a-select>
      <p class="modal-form__label" @click="autoMigrate = !autoMigrate" style="cursor:pointer;">
        {{ $t('label.automigrate.volume') }}
      </p>
      <a-checkbox v-model="autoMigrate" />
    </div>

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('label.cancel') }}
      </a-button>
      <a-button type="primary" @click="submitChangeOfferingForVolume">
        {{ $t('label.ok') }}
      </a-button>
    </div>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ChangeOfferingForVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      diskOfferings: [],
      autoMigrate: true,
      selectedDiskOffering: null,
      customDiskOffering: false,
      loading: false,
      isCustomizedDiskIOps: false
    }
  },
  created () {
    this.fetchDiskOfferings()
  },
  methods: {
    fetchDiskOfferings () {
      api('listDiskOfferings', {
        listall: true
      }).then(response => {
        this.diskOfferings = response.listdiskofferingsresponse.diskoffering
        this.selectedDiskOffering = this.diskOfferings[0].id
        this.customDiskOffering = this.diskOfferings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.diskOfferings[0]?.iscustomizediops || false
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    closeModal () {
      this.$parent.$parent.close()
    },
    submitChangeOfferingForVolume () {
      api('changeOfferingForVolume', {
        diskofferingid: this.selectedDiskOffering,
        id: this.resource.id,
        automigrate: this.autoMigrate
      }).then(response => {
        this.$pollJob({
          jobId: response.changeofferingforvolumeresponse.jobid,
          successMessage: this.$t('message.change.offering.for.volume'),
          successMethod: () => {
            this.parentFetchData()
          },
          errorMessage: this.$t('message.change.offering.for.volume.failed'),
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.change.offering.for.volume.processing'),
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
  .change-offering-for-volume-container {
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
