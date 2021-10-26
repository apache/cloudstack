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
  <div class="form" v-ctrl-enter="submitForm">
    <a-alert type="warning">
      <span slot="message" v-html="$t('message.migrate.instance.to.host')" />
    </a-alert>
    <a-input-search
      class="top-spaced"
      :placeholder="$t('label.search')"
      v-model="searchQuery"
      @search="fetchData"
      autoFocus />
    <a-table
      class="top-spaced"
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="hosts"
      :pagination="false"
      :rowKey="record => record.id">
      <div slot="suitability" slot-scope="record">
        <a-icon
          class="host-item__suitability-icon"
          type="check-circle"
          theme="twoTone"
          twoToneColor="#52c41a"
          v-if="record.suitableformigration" />
        <a-icon
          class="host-item__suitability-icon"
          type="close-circle"
          theme="twoTone"
          twoToneColor="#f5222d"
          v-else />
      </div>
      <div slot="memused" slot-scope="record">
        <span v-if="record.memoryused">
          {{ record.memoryused | byteToGigabyte }} GB
        </span>
      </div>
      <div slot="memoryallocatedpercentage" slot-scope="record">
        {{ record.memoryallocatedpercentage }}
      </div>
      <div slot="cluster" slot-scope="record">
        {{ record.clustername }}
      </div>
      <div slot="pod" slot-scope="record">
        {{ record.podname }}
      </div>
      <div slot="requiresstoragemigration" slot-scope="record">
        {{ record.requiresStorageMotion ? $t('label.yes') : $t('label.no') }}
      </div>
      <template slot="select" slot-scope="record">
        <a-radio
          class="host-item__radio"
          @click="handleSelectedHostChange(record)"
          :checked="record.id === selectedHost.id"
          :disabled="!record.suitableformigration"></a-radio>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-form-item
      v-if="isUserVm"
      class="top-spaced">
      <tooltip-label slot="label" :title="$t('label.migrate.with.storage')" :tooltip="$t('message.migrate.with.storage')"/>
      <a-switch
        v-decorator="['migratewithstorage']"
        @change="handleMigrateWithStorageChange"
        :disabled="!selectedHost || !selectedHost.id || selectedHost.id === -1" />
    </a-form-item>
    <a-table
      class="top-spaced"
      size="small"
      style="overflow-y: auto"
      :loading="vmVolumesLoading"
      :columns="volumeColumns"
      :dataSource="vmVolumes"
      :pagination="false"
      :rowKey="record => record.id"
      v-if="migrateWithStorage">
      <div slot="size" slot-scope="record">
        <span v-if="record.size">
          {{ record.size | byteToGigabyte }} GB
        </span>
      </div>
      <template slot="selectstorage" slot-scope="record">
        <a-input-search
          :readOnly="true"
          :disabled="!selectedHost.id"
          :value="record.selectedstoragename"
          @search="openVolumeStoragePoolSelector(record)" />
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
      <volume-storage-pool-selector
        :resource="selectedVolumeForStoragePoolSelection"
        :clusterId="selectedHost.id ? selectedHost.clusterid : null"
        :autoAssignAllowed="true"
        :isOpen="!(!selectedVolumeForStoragePoolSelection.id)"
        @close-action="closeVolumeStoragePoolSelector()"
        @select="handleVolumeStoragePoolSelection" />
    </a-modal>

    <a-divider />

    <div style="margin-top: 20px; display: flex; justify-content:flex-end;">
      <a-button type="primary" ref="submit" :disabled="!selectedHost.id" @click="submitForm">
        {{ $t('label.ok') }}
      </a-button>
    </div>
  </div>

</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import VolumeStoragePoolSelector from '@/views/VolumeStoragePoolSelector'

export default {
  name: 'VMMigrateWizard',
  components: {
    TooltipLabel,
    VolumeStoragePoolSelector
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: true,
      hosts: [],
      selectedHost: {},
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.hostid'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.suitability'),
          scopedSlots: { customRender: 'suitability' }
        },
        {
          title: this.$t('label.cpuused'),
          dataIndex: 'cpuused'
        },
        {
          title: this.$t('label.memoryallocated'),
          scopedSlots: { customRender: 'memoryallocatedpercentage' }
        },
        {
          title: this.$t('label.memused'),
          scopedSlots: { customRender: 'memused' }
        },
        {
          title: this.$t('label.cluster'),
          scopedSlots: { customRender: 'cluster' }
        },
        {
          title: this.$t('label.pod'),
          scopedSlots: { customRender: 'pod' }
        },
        {
          title: this.$t('label.storage.migration.required'),
          scopedSlots: { customRender: 'requiresstoragemigration' }
        },
        {
          title: this.$t('label.select'),
          scopedSlots: { customRender: 'select' }
        }
      ],
      migrateWithStorage: false,
      vmVolumes: [],
      vmVolumesLoading: false,
      volumeColumns: [
        {
          title: this.$t('label.volumeid'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.size'),
          scopedSlots: { customRender: 'size' }
        },
        {
          title: this.$t('label.storage'),
          scopedSlots: { customRender: 'selectstorage' }
        }
      ],
      selectedVolumeForStoragePoolSelection: {},
      volumeToPoolSelection: []
    }
  },
  created () {
    this.fetchData()
  },
  computed: {
    isUserVm () {
      return this.$route.meta.name === 'vm'
    }
  },
  methods: {
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchData () {
      this.loading = true
      api('findHostsForMigration', {
        virtualmachineid: this.resource.id,
        keyword: this.searchQuery,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.hosts = response.findhostsformigrationresponse.host || []
        this.hosts.sort((a, b) => {
          return b.suitableformigration - a.suitableformigration
        })
        for (const key in this.hosts) {
          if (this.hosts[key].suitableformigration && !this.hosts[key].requiresstoragemigration) {
            this.hosts.unshift({ id: -1, name: this.$t('label.migrate.auto.select'), suitableformigration: true, requiresstoragemigration: false })
            break
          }
        }
        this.totalCount = response.findhostsformigrationresponse.count
      }).catch(error => {
        this.$message.error(`${this.$t('message.load.host.failed')}: ${error}`)
      }).finally(() => {
        this.loading = false
      })
    },
    submitForm () {
      if (this.loading) return
      this.loading = true
      var migrateApi = this.isUserVm
        ? (this.selectedHost.requiresStorageMotion || this.volumeToPoolSelection.length > 0) ? 'migrateVirtualMachineWithVolume' : 'migrateVirtualMachine'
        : 'migrateSystemVm'
      var migrateParams = this.selectedHost.id === -1 ? { autoselect: true, virtualmachineid: this.resource.id }
        : { hostid: this.selectedHost.id, virtualmachineid: this.resource.id }
      if (migrateApi === 'migrateVirtualMachineWithVolume' && this.volumeToPoolSelection.length > 0) {
        for (var i; i < this.volumeToPoolSelection.length; i++) {
          const mapping = this.volumeToPoolSelection[i]
          migrateParams['migrateto[' + i + '].volume'] = mapping.volume
          migrateParams['migrateto[' + i + '].pool'] = mapping.pool
        }
      }
      api(migrateApi, migrateParams).then(response => {
        const jobid = this.isUserVm
          ? this.selectedHost.requiresStorageMotion ? response.migratevirtualmachinewithvolumeresponse.jobid : response.migratevirtualmachineresponse.jobid
          : response.migratesystemvmresponse.jobid
        this.$pollJob({
          jobId: jobid,
          title: `${this.$t('label.migrating')} ${this.resource.name}`,
          description: this.resource.name,
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
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
          duration: 0
        })
      }).finally(() => {
        this.loading = false
      })
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    },
    handleSelectedHostChange (host) {
      this.selectedHost = host
      this.selectedVolumeForStoragePoolSelection = {}
      this.volumeToPoolSelection = []
      for (const volume of this.vmVolumes) {
        volume.selectedstorageid = -1
        volume.selectedstoragename = this.$t('label.auto.assign')
      }
    },
    handleMigrateWithStorageChange (checked) {
      this.migrateWithStorage = checked
      if (this.migrateWithStorage) {
        this.fetchVolumes()
      }
    },
    fetchVolumes () {
      this.vmVolumesLoading = true
      this.vmVolumes = []
      api('listVolumes', {
        listAll: true,
        virtualmachineid: this.resource.id
      }).then(response => {
        var volumes = response.listvolumesresponse.volume
        if (volumes && volumes.length > 0) {
          volumes.sort((a, b) => {
            return b.type.localeCompare(a.type)
          })
          for (const volume of volumes) {
            volume.selectedstorageid = -1
            volume.selectedstoragename = this.$t('label.auto.assign')
            this.vmVolumes.push(volume)
          }
        }
      }).finally(() => {
        this.vmVolumesLoading = false
      })
    },
    toGB (value) {
      return (value / (1024 * 1024 * 1024)).toFixed(2)
    },
    openVolumeStoragePoolSelector (volume) {
      this.selectedVolumeForStoragePoolSelection = volume
    },
    closeVolumeStoragePoolSelector () {
      this.selectedVolumeForStoragePoolSelection = {}
    },
    handleVolumeStoragePoolSelection (volumeId, storagePool) {
      for (const volume of this.vmVolumes) {
        if (volume.id === volumeId) {
          volume.selectedstorageid = storagePool.id
          volume.selectedstoragename = storagePool.name
          volume.selectedstorageclusterid = storagePool.clusterid
          break
        }
      }
      this.volumeToPoolSelection = []
      for (const volume of this.vmVolumes) {
        if (volume.selectedstorageid) {
          this.volumeToPoolSelection.push({ volume: volume.id, pool: volume.selectedstorageid })
        }
      }
    }
  },
  filters: {
    byteToGigabyte: value => {
      return (value / Math.pow(10, 9)).toFixed(2)
    }
  }
}
</script>

<style scoped lang="scss">

  .form {
    width: 95vw;
    @media (min-width: 900px) {
      width: 850px;
    }
  }

  .host-item {
    padding-right: 20px;
    padding-bottom: 0;
    padding-left: 20px;

    &--selected {
      background-color: #e6f7ff;
    }

    &__row {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 760px) {
        flex-direction: row;
      }
    }

    &__value {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      flex: 1;
      margin-bottom: 10px;

      &--small {

        @media (min-width: 760px) {
          flex: none;
          margin-right: 40px;
          margin-left: 40px;
        }
      }
    }

    &__title {
      font-weight: bold;
    }

    &__suitability-icon {
      margin-top: 5px;
    }

    &__radio {
      display: flex;
      align-items: center;
    }

  }

  .top-spaced {
    margin-top: 20px;
  }

  .pagination {
    margin-top: 20px;
  }
</style>
