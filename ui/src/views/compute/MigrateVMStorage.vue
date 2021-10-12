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
    <a-form-item>
      <tooltip-label slot="label" :title="$t('label.per.volume')" :tooltip="$t('message.per.volume.migration')"/>
      <a-switch v-decorator="['pervolume']" @change="handlePerVolumeChange" />
    </a-form-item>
    <a-form v-if="perVolume">
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.volume.to.storage.pool.map')" :tooltip="$t('message.volume.to.storage.pool.map')"/>
        <div scroll-to="last-child">
          <a-list itemLayout="horizontal" :dataSource="secondaryVolumes">
            <a-list-item slot="renderItem" slot-scope="item">
              <check-box-select-pair
                v-decorator="['volume.'+item.name, {}]"
                :resourceKey="item.id"
                :checkBoxLabel="item.name +' (' + toGB(item.size) + ' GB)'"
                :checkBoxDecorator="'volume.' + item.name"
                :selectOptions="secondaryVolumePools"
                :selectDecorator="item.name + '.pool'"
                @handle-checkselectpair-change="handleVolumePoolChange"/>
            </a-list-item>
          </a-list>
        </div>
      </a-form-item>
    </a-form>

    <a-divider />

    <div style="margin-top: 20px; display: flex; justify-content:flex-end;">
      <a-button type="primary" :disabled="!selectedPool.id" @click="submitForm">
        {{ $t('label.ok') }}
      </a-button>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'

export default {
  name: 'MigrateVMStorage',
  components: {
    TooltipLabel,
    CheckBoxSelectPair
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
      ]
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
  methods: {
    fetchData () {
      this.fetchVolumes()
    },
    fetchVolumes () {
      this.loading = true
      this.rootVolume = null
      api('listVolumes', {
        listAll: true,
        virtualmachineid: this.resource.id
      }).then(response => {
        var volumes = response.listvolumesresponse.volume
        if (volumes && volumes.length > 0) {
          var rootVolumes = volumes.filter(item => item.type === 'ROOT')
          if (rootVolumes && rootVolumes.length > 0) {
            this.rootVolume = rootVolumes[0]
            this.secondaryVolumes = volumes.filter(item => item.id !== this.rootVolume.id)
          }
        }
      }).finally(() => {
        if (this.rootVolume != null) {
          this.fetchPools()
          this.fetchSecondaryVolumePools()
        }
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
    handlePerVolumeChange (checked) {
      this.perVolume = checked
      if (this.perVolume) {
        this.fetchSecondaryVolumePools()
      }
    },
    handleVolumePoolChange (v1, v2, v3) {
    },
    submitForm () {
      this.loading = true
      var isUserVm = true
      if (this.$route.meta.name !== 'vm') {
        isUserVm = false
      }
      var migrateApi = isUserVm ? 'migrateVirtualMachine' : 'migrateSystemVm'
      if (isUserVm && this.apiParams.hostid && this.apiParams.hostid.required === false) {
        migrateApi = 'migrateVirtualMachineWithVolume'
        if (this.rootVolume == null) {
          this.$message.error('Failed to find ROOT volume for the VM ' + this.resource.id)
          this.closeAction()
        }
        this.migrateVm(migrateApi, this.selectedPool.id, this.rootVolume.id)
        return
      }
      this.migrateVm(migrateApi, this.selectedPool.id, null)
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

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
