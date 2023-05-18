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
  <div>
    <a-table
      class="top-spaced"
      size="small"
      style="max-height: 250px; overflow-y: auto"
      :loading="volumesLoading"
      :columns="volumeColumns"
      :dataSource="volumes"
      :pagination="false"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'size'">
          <span v-if="record.size">
            {{ $bytesToHumanReadableSize(record.size) }}
          </span>
        </template>
        <template v-if="column.key === 'selectedstorage'">
          <span>{{ record.selectedstoragename || '' }}</span>
        </template>
        <template v-if="column.key === 'select'">
          <div style="display: flex; justify-content: flex-end;"><a-button @click="openVolumeStoragePoolSelector(record)">{{ record.selectedstorageid ? $t('label.change') : $t('label.select') }}</a-button></div>
        </template>
      </template>
    </a-table>

    <a-modal
      :visible="!(!selectedVolumeForStoragePoolSelection.id)"
      :title="$t('label.select.ps')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      :cancelText="$t('label.cancel')"
      @cancel="closeVolumeStoragePoolSelector()"
      centered
      width="auto">
      <volume-storage-pool-select-form
        :resource="selectedVolumeForStoragePoolSelection"
        :clusterId="storagePoolsClusterId"
        :autoAssignAllowed="storagePoolsClusterId != null"
        :isOpen="!(!selectedVolumeForStoragePoolSelection.id)"
        @close-action="closeVolumeStoragePoolSelector()"
        @select="handleVolumeStoragePoolSelection" />
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import VolumeStoragePoolSelectForm from '@/components/view/VolumeStoragePoolSelectForm'

export default {
  name: 'InstanceVolumesStoragePoolSelectListView',
  components: {
    VolumeStoragePoolSelectForm
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    clusterId: {
      type: String,
      required: false,
      default: null
    }
  },
  data () {
    return {
      volumes: [],
      volumesLoading: false,
      volumeColumns: [
        {
          key: 'name',
          title: this.$t('label.volumeid'),
          dataIndex: 'name'
        },
        {
          key: 'type',
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          key: 'size',
          title: this.$t('label.size')
        },
        {
          key: 'selectedstorage',
          title: this.$t('label.storage')
        },
        {
          key: 'select',
          title: ''
        }
      ],
      selectedVolumeForStoragePoolSelection: {},
      selectedClusterId: null,
      volumesWithClusterStoragePool: []
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
    this.fetchVolumes()
  },
  computed: {
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
    },
    storagePoolsClusterId () {
      if (this.clusterId) {
        return this.clusterId
      }
      return this.isSelectedVolumeOnlyClusterStoragePoolVolume ? null : this.selectedClusterId
    }
  },
  methods: {
    fetchVolumes () {
      this.volumesLoading = true
      this.volumes = []
      api('listVolumes', {
        listAll: true,
        virtualmachineid: this.resource.id
      }).then(response => {
        var volumes = response.listvolumesresponse.volume
        if (volumes && volumes.length > 0) {
          volumes.sort((a, b) => {
            return b.type.localeCompare(a.type)
          })
          this.volumes = volumes
        }
      }).finally(() => {
        this.resetSelection()
        this.volumesLoading = false
      })
    },
    resetSelection () {
      var volumes = this.volumes
      this.volumes = []
      for (var volume of volumes) {
        if (this.clusterId) {
          volume.selectedstorageid = -1
          volume.selectedstoragename = this.$t('label.auto.assign')
        } else {
          volume.selectedstorageid = null
          volume.selectedstoragename = ''
        }
        delete volume.selectedstorageclusterid
      }
      this.volumes = volumes
      this.updateVolumeToStoragePoolSelection()
    },
    openVolumeStoragePoolSelector (volume) {
      this.selectedVolumeForStoragePoolSelection = volume
    },
    closeVolumeStoragePoolSelector () {
      this.selectedVolumeForStoragePoolSelection = {}
    },
    handleVolumeStoragePoolSelection (volumeId, storagePool) {
      for (const volume of this.volumes) {
        if (volume.id === volumeId) {
          volume.selectedstorageid = storagePool.id
          volume.selectedstoragename = storagePool.name
          volume.selectedstorageclusterid = storagePool.clusterid
          break
        }
      }
      this.updateVolumeToStoragePoolSelection()
    },
    updateVolumeToStoragePoolSelection () {
      var clusterId = null
      this.volumeToPoolSelection = []
      this.volumesWithClusterStoragePool = []
      for (const volume of this.volumes) {
        if (volume.selectedstorageid && volume.selectedstorageid !== -1) {
          this.volumeToPoolSelection.push({ volume: volume.id, pool: volume.selectedstorageid })
        }
        if (!this.clusterId && volume.selectedstorageclusterid) {
          clusterId = volume.selectedstorageclusterid
          this.volumesWithClusterStoragePool.push(volume)
        }
      }
      if (!this.clusterId) {
        this.selectedClusterId = clusterId
        for (const volume of this.volumes) {
          if (this.selectedClusterId == null && volume.selectedstorageid === -1) {
            volume.selectedstorageid = null
            volume.selectedstoragename = ''
          }
          if (this.selectedClusterId && volume.selectedstorageid == null) {
            volume.selectedstorageid = -1
            volume.selectedstoragename = this.$t('label.auto.assign')
          }
        }
      }
      this.$emit('select', this.volumeToPoolSelection)
    }
  }
}
</script>

<style scoped lang="less">
  .top-spaced {
    margin-top: 20px;
  }
</style>
