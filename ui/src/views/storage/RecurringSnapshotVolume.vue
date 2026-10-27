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
  <div class="snapshot-layout">
    <div v-if="!isVolumeResource" class="volume-selection">
      <a-form layout="vertical">
        <a-form-item :label="$t('label.volume')" required>
          <a-select
            v-model:value="selectedVolumeId"
            :placeholder="$t('label.select.volume')"
            :loading="volumesLoading"
            show-search
            :filter-option="filterOption"
            @change="onVolumeChange"
          >
            <a-select-option
              v-for="volume in volumes"
              :key="volume.id"
              :value="volume.id"
            >
              {{ volume.name }} ({{ volume.sizegb }}GB)
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </div>
    <a-tabs v-if="currentVolumeResource && currentVolumeResource.id" defaultActiveKey="1" :animated="false">
      <a-tab-pane :tab="$t('label.schedule')" key="1">
        <FormSchedule
          :loading="loading"
          :resource="currentVolumeResource"
          :dataSource="dataSource"
          :resourceType="'Volume'"
          @close-action="closeAction"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.action.recurring.snapshot')" key="2">
        <ScheduledSnapshots
          :loading="loading"
          :resource="currentVolumeResource"
          :dataSource="dataSource"
          @refresh="handleRefresh"
          @close-action="closeAction"/>
      </a-tab-pane>
    </a-tabs>
    <div v-if="!currentVolumeResource || !currentVolumeResource.id" class="no-volume-selected">
      <div class="empty-state">
        <p>{{ $t('message.select.volume.to.continue') }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import FormSchedule from '@/views/storage/FormSchedule'
import ScheduledSnapshots from '@/views/storage/ScheduledSnapshots'

export default {
  name: 'RecurringSnapshotVolume',
  components: {
    FormSchedule,
    ScheduledSnapshots
  },
  props: {
    resource: {
      type: Object,
      required: false,
      default: () => null
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      dataSource: [],
      volumes: [],
      volumesLoading: false,
      selectedVolumeId: null,
      selectedVolume: null
    }
  },
  computed: {
    resourceType () {
      if (!this.resource) return 'none'
      if (this.resource.type === 'ROOT' || this.resource.type === 'DATADISK' ||
          this.resource.state === 'Ready' || this.resource.state === 'Allocated' ||
          this.resource.sizegb !== undefined) {
        return 'volume'
      }
      if (this.resource.intervaltype !== undefined || this.resource.schedule !== undefined) {
        return 'snapshotpolicy'
      }

      return 'unknown'
    },

    isVolumeResource () {
      return this.resourceType === 'volume'
    },

    currentVolumeResource () {
      if (this.isVolumeResource) {
        return this.resource
      } else {
        return this.selectedVolume
      }
    }
  },
  created () {
    if (this.isVolumeResource) {
      this.fetchData()
    } else {
      this.fetchVolumes()
    }
  },
  methods: {
    async fetchVolumes () {
      this.volumesLoading = true
      try {
        const response = await getAPI('listVolumes', { listAll: true })
        const volumes = response.listvolumesresponse.volume || []
        this.volumes = volumes.filter(volume => {
          return volume.state === 'Ready' &&
                (volume.hypervisor !== 'KVM' ||
                  (['Stopped', 'Destroyed'].includes(volume.vmstate)) ||
                  (this.$store.getters.features.kvmsnapshotenabled))
        })
      } catch (error) {
        this.$message.error(this.$t('message.error.fetch.volumes'))
        console.error('Error fetching volumes:', error)
      } finally {
        this.volumesLoading = false
      }
    },
    onVolumeChange (volumeId) {
      const volume = this.volumes.find(v => v.id === volumeId)
      if (volume) {
        this.selectedVolume = volume
        this.selectedVolumeId = volumeId
        this.dataSource = []
        this.fetchData()
      }
    },
    fetchData () {
      const volumeResource = this.currentVolumeResource
      if (!volumeResource || !volumeResource.id) {
        return
      }

      const params = {
        volumeid: volumeResource.id,
        listAll: true
      }

      this.dataSource = []
      this.loading = true

      getAPI('listSnapshotPolicies', params).then(json => {
        this.loading = false
        const listSnapshotPolicies = json.listsnapshotpoliciesresponse.snapshotpolicy
        if (listSnapshotPolicies && listSnapshotPolicies.length > 0) {
          this.dataSource = listSnapshotPolicies
        }
      }).catch(error => {
        this.loading = false
        this.$message.error(this.$t('message.error.fetch.snapshot.policies'))
        console.error('Error fetching snapshot policies:', error)
      })
    },
    handleRefresh () {
      this.fetchData()
      this.parentFetchData()
    },
    closeAction () {
      this.fetchData()
      this.$emit('close-action')
    },
    filterOption (input, option) {
      return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }
  }
}
</script>

<style lang="less" scoped>
.snapshot-layout {
  max-width: 800px;

  .volume-selection {
    margin-bottom: 20px;
    padding: 16px;
    border: 1px solid #d9d9d9;
    border-radius: 6px;
    background-color: #fafafa;
    .ant-form-item {
      margin-bottom: 0;
      .ant-select {
        width: 100%;
        min-width: 400px;
      }
    }
  }

  .current-volume-info {
    margin-bottom: 16px;
  }

  .no-volume-selected {
    text-align: center;
    padding: 40px 20px;
  }
}
</style>
