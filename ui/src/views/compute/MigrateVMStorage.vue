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
    <a-alert type="warning">
      <span slot="message" v-html="$t('message.migrate.instance.to.ps')" />
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
      <a-input-search
        class="top-spaced"
        :placeholder="$t('label.search')"
        v-model="searchQuery"
        @search="fetchPools"
        autoFocus />
      <a-table
        class="top-spaced"
        size="small"
        style="overflow-y: auto"
        :loading="loading"
        :columns="columns"
        :dataSource="storagePools"
        :pagination="false"
        :rowKey="record => record.id">
        <div slot="disksizetotal" slot-scope="record">
          {{ record.disksizetotal | byteToGigabyte }} GB
        </div>
        <div slot="disksizeused" slot-scope="record">
          {{ record.disksizeused | byteToGigabyte }} GB
        </div>
        <div slot="disksizefree" slot-scope="record">
          {{ (record.disksizetotal * 1 - record.disksizeused * 1) | byteToGigabyte }} GB
        </div>
        <template slot="select" slot-scope="record">
          <a-radio
            @click="selectedPool = record"
            :checked="record.id === selectedPool.id"></a-radio>
        </template>
      </a-table>
      <a-pagination
        class="top-spaced"
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
    </div>
    <div v-else>
      <a-table
        class="top-spaced"
        size="small"
        style="overflow-y: auto"
        :loading="vmVolumesLoading"
        :columns="volumeColumns"
        :dataSource="vmVolumes"
        :pagination="false"
        :rowKey="record => record.id">
        <div slot="size" slot-scope="record">
          <span v-if="record.size">
            {{ record.size | byteToGigabyte }} GB
          </span>
        </div>
        <template slot="selectstorage" slot-scope="record">
          <a-input-search
            :readOnly="true"
            :value="record && record.selectedstoragename ? record.selectedstoragename : ''"
            @search="openVolumeStoragePoolSelector(record)" />
        </template>
      </a-table>

      <a-modal
        :visible="!(!selectedVolumeForStoragePoolSelection.id)"
        :title="$t('label.import.instance')"
        :closable="true"
        :maskClosable="false"
        :footer="null"
        :cancelText="$t('label.cancel')"
        @cancel="closeVolumeStoragePoolSelector()"
        centered
        width="auto">
        <volume-storage-pool-selector
          :resource="selectedVolumeForStoragePoolSelection"
          :clusterId="isSelectedVolumeOnlyClusterStoragePoolVolume() ? null : selectedClusterId "
          :autoAssignAllowed="isSelectedVolumeOnlyClusterStoragePoolVolume() ? false : !(!selectedClusterId)"
          :isOpen="!(!selectedVolumeForStoragePoolSelection.id)"
          @close-action="closeVolumeStoragePoolSelector()"
          @select="handleVolumeStoragePoolSelection" />
      </a-modal>
    </div>

    <a-divider />

    <div style="margin-top: 20px; display: flex; justify-content:flex-end;">
      <a-button type="primary" :disabled="this.migrateMode === 1 ? !selectedPool.id : volumeToPoolSelection.length < 1" @click="submitForm">
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
  name: 'MigrateVMStorage',
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
      loading: false,
      migrateMode: 1,
      searchQuery: '',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      storagePools: [],
      selectedPool: {},
      rootVolume: null,
      secondaryVolumes: [],
      secondaryVolumePools: [],
      perVolume: false,
      columns: [
        {
          title: this.$t('label.storageid'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.clusterid'),
          dataIndex: 'clustername'
        },
        {
          title: this.$t('label.podid'),
          dataIndex: 'podname'
        },
        {
          title: this.$t('label.disksizetotal'),
          scopedSlots: { customRender: 'disksizetotal' }
        },
        {
          title: this.$t('label.disksizeused'),
          scopedSlots: { customRender: 'disksizeused' }
        },
        {
          title: this.$t('label.disksizefree'),
          scopedSlots: { customRender: 'disksizefree' }
        },
        {
          title: this.$t('label.select'),
          scopedSlots: { customRender: 'select' }
        }
      ],
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
      selectedClusterId: null,
      volumesWithClusterStoragePool: [],
      volumeToPoolSelection: []
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
  computed: {
    migrateVmWithVolumeAllowed () {
      return this.$route.meta.name === 'vm' && this.apiParams.hostid && this.apiParams.hostid.required === false
    }
  },
  methods: {
    fetchData () {
      if (this.migrateMode === 1) {
        this.fetchPools()
      } else {
        this.fetchVolumes()
      }
    },
    handleMigrateModeChange (value) {
      this.migrateMode = value
      this.fetchData()
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
            volume.selectedstorageid = null
            volume.selectedstoragename = ''
            this.vmVolumes.push(volume)
          }
        }
      }).finally(() => {
        this.vmVolumesLoading = false
      })
    },
    fetchPools () {
      this.loading = true
      api('listStoragePools', {
        zoneid: this.resource.zoneid,
        keyword: this.searchQuery,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        if (this.arrayHasItems(response.liststoragepoolsresponse.storagepool)) {
          this.storagePools = response.liststoragepoolsresponse.storagepool
        }
        this.totalCount = response.liststoragepoolsresponse.count
      }).finally(() => {
        this.loading = false
      })
    },
    fetchSecondaryVolumePools () {
      this.loading = true
      api('listStoragePools', {
        zoneid: this.resource.zoneid
      }).then(response => {
        if (this.arrayHasItems(response.liststoragepoolsresponse.storagepool)) {
          this.secondaryVolumePools = response.liststoragepoolsresponse.storagepool
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
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchPools()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchPools()
    },
    submitForm () {
      this.loading = true
      var isUserVm = true
      if (this.$route.meta.name !== 'vm') {
        isUserVm = false
      }
      var migrateApi = isUserVm ? 'migrateVirtualMachine' : 'migrateSystemVm'
      if (isUserVm && this.migrateMode === 2) {
        migrateApi = 'migrateVirtualMachineWithVolume'
        if (this.volumeToPoolSelection.length < 1) {
          this.$message.error('Failed to find ROOT volume for the VM ' + this.resource.id)
          this.closeAction()
        }
        this.migrateVm(migrateApi, this.selectedPool.id, this.rootVolume.id)
        return
      }
      this.migrateVm(migrateApi, this.selectedPool.id, null)
    },
    migrateVm (migrateApi, storageId, volumeToPool) {
      var params = {
        virtualmachineid: this.resource.id
      }
      if (storageId) {
        params.storageid = storageId
      } else if (volumeToPool) {
        for (var i; i < volumeToPool.length; i++) {
          const mapping = volumeToPool[i]
          params['migrateto[' + i + '].volume'] = mapping.volume
          params['migrateto[' + i + '].pool'] = mapping.pool
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
      this.updateVolumeToStoragePoolSelection()
    },
    updateVolumeToStoragePoolSelection () {
      var clusterId = null
      this.volumeToPoolSelection = []
      this.volumesWithClusterStoragePool = []
      for (const volume of this.vmVolumes) {
        if (volume.selectedstorageid) {
          this.volumeToPoolSelection.push({ volume: volume.id, pool: volume.selectedstorageid })
        }
        if (volume.selectedstorageclusterid) {
          clusterId = volume.selectedstorageclusterid
          this.volumesWithClusterStoragePool.push(volume)
        }
      }
      this.selectedClusterId = clusterId
      for (const volume of this.vmVolumes) {
        if (this.selectedClusterId == null && volume.selectedstorageid === -1) {
          volume.selectedstorageid = null
          volume.selectedstoragename = ''
        }
        if (this.selectedClusterId && volume.selectedstorageid == null) {
          volume.selectedstorageid = -1
          volume.selectedstoragename = this.$t('label.auto.assign')
        }
      }
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
  filters: {
    byteToGigabyte: value => {
      return (value / Math.pow(10, 9)).toFixed(2)
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 800px) {
      width: 700px;
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

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
