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
  <div v-ctrl-enter="submitForm">
    <a-alert type="error">
      <template #message>
        <span v-html="$t('message.migrate.resource.to.ss')" />
      </template>
    </a-alert>
    <image-store-selector
      :zoneid="zoneid"
      :srcImageStoreId="srcImageStoreId"
      @select="handleImageStoreChange" />
    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" @click="submitForm">{{ $t('label.ok') }}</a-button>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import ImageStoreSelector from '@/components/view/ImageStoreSelectView'

export default {
  name: 'MigrateImageStoreResource',
  components: {
    TooltipLabel,
    ImageStoreSelector
  },
  props: {
    snapshotIdsToMigrate: {
      type: Array,
      default: () => [],
      required: false
    },
    templateIdsToMigrate: {
      type: Array,
      default: () => [],
      required: false
    },
    sourceImageStore: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      selectedStore: null,
      destinationStoreList: []
    }
  },
  beforeCreate () {
    this.zoneid = this.sourceImageStore.zoneid
    this.srcImageStoreId = this.sourceImageStore.id
  },
  computed: {},
  methods: {
    fetchDestinationStores () {
      api('listImageStores', {
        zoneid: this.zoneid
      }).then(response => {
        this.destinationStoreList = response.listimagestoresresponse.imagestore
      }).catch(error => {
        console.error(error)
      })
    },
    handleImageStoreChange (value) {
      this.selectedStore = value
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
      this.selectedStore = storagePool
    },
    handleVolumeToPoolChange (volumeToPool) {
      this.volumeToPoolSelection = volumeToPool
    },
    submitForm () {
      this.migrateResources(this.selectedStore.id)
    },
    migrateResources (destStoreId) {
      var params = {
        srcpool: this.sourceImageStore.id,
        destpool: destStoreId
      }
      params.templates = this.templateIdsToMigrate.join(',')
      params.snapshots = this.snapshotIdsToMigrate.join(',')

      api('migrateResourceToAnotherSecondaryStorage', params).then(response => {
        const jobId = response.migrateresourcetoanothersecondarystorageresponse.jobid
        this.$pollJob({
          title: this.$t('label.migrating.data'),
          description: '',
          jobId: jobId,
          successMessage: this.$t('message.success.migration'),
          successMethod: () => {
            this.closeModal()
          },
          errorMessage: this.$t('message.migrating.failed'),
          errorMethod: () => {
            this.closeModal()
          },
          loadingMessage: this.$t('label.migrating'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.closeModal()
          }
        })
        this.closeModal()
      }).catch(error => {
        console.error(error)
        this.$message.error(`${this.$t('message.migrating.vm.to.storage.failed')} ${this.selectedStore.id}`)
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
