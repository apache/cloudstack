// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <div>
    <!-- Toolbar for bulk actions -->
    <div style="margin-bottom: 16px;">
      <a-space wrap>
        <!-- Bulk GPU Device Actions -->
        <a-popconfirm
          :title="$t('message.confirm.enable.gpu.devices', { count: selectedDeviceCount })"
          @confirm="bulkEnableGpuDevices"
          okText="Yes"
          cancelText="No"
        >
          <a-button
            type="primary"
            :disabled="!hasSelectedDevices"
          >
            {{ $t('label.gpu.devices.enable') }}
          </a-button>
        </a-popconfirm>

        <a-popconfirm
          :title="$t('message.confirm.disable.gpu.devices', { count: selectedDeviceCount })"
          @confirm="bulkDisableGpuDevices"
          okText="Yes"
          cancelText="No"
        >
          <a-button
            type="primary"
            danger
            :disabled="!hasSelectedDevices"
          >
            {{ $t('label.gpu.devices.disable') }}
          </a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-table
      :loading="tabLoading"
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.id"
      :childrenColumnName="'children'"
      :defaultExpandAllRows="true"
      :rowSelection="{
        selectedRowKeys: selectedGpuDeviceIds,
        onChange: onGpuDeviceSelectionChange,
        getCheckboxProps: record => ({
          disabled: record.state === 'Disabled'
        })
      }"
      :customRow="customRowProps"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'busaddress'">
          <span :style="{ paddingLeft: record.parentgpudeviceid ? '20px' : '0px' }">
            {{ record.busaddress }}
          </span>
        </template>
        <template v-else-if="column.key === 'virtualmachinename'">
          <router-link
            v-if="record.virtualmachinename && record.virtualmachineid"
            :to="{ path: '/vm/' + record.virtualmachineid }"
          >
            {{ record.virtualmachinename }}
          </router-link>
          <span v-else>{{ record.virtualmachinename }}</span>
        </template>
        <template v-else-if="column.key === 'gpucardname'">
          <router-link
            v-if="record.gpucardid"
            :to="{ path: '/gpucard/' + record.gpucardid }"
            :title="record.gpucardname"
            class="text-ellipsis"
          >
            {{ record.gpucardname }}
          </router-link>
          <span
            v-else
            :title="record.gpucardname"
            class="text-ellipsis"
          >{{ record.gpucardname }}</span>
        </template>
        <template v-else-if="column.key === 'vgpuprofilename'">
          <router-link
            v-if="record.vgpuprofileid"
            :to="{ path: '/vgpuprofile/' + record.vgpuprofileid }"
            :title="record.vgpuprofilename"
            class="text-ellipsis"
          >
            {{ record.vgpuprofilename }}
          </router-link>
          <span
            v-else
            :title="record.vgpuprofilename"
            class="text-ellipsis"
          >{{ record.vgpuprofilename }}</span>
        </template>
        <template v-else-if="column.key === 'hostname'">
          <router-link
            v-if="record.hostid"
            :to="{ path: '/host/' + record.hostid }"
            :title="record.hostname"
            class="text-ellipsis"
          >
            {{ record.hostname }}
          </router-link>
          <span
            v-else
            :title="record.hostname"
            class="text-ellipsis"
          >{{ record.hostname }}</span>
        </template>
      </template>

      <!-- Custom Filter Dropdown for Column Selection -->
      <template #customFilterDropdown="{ column }">
        <div
          v-if="column.key === 'columnFilter'"
          style="padding: 8px; min-width: 200px;"
        >
          <div style="margin-bottom: 8px; font-weight: 500;">{{ $t('label.select.columns') }}</div>
          <div style="margin-bottom: 8px;">
            <a-space>
              <a-button
                size="small"
                @click="selectAllColumns"
              >
                {{ $t('label.select.all') }}
              </a-button>
              <a-button
                size="small"
                @click="clearAllColumns"
              >
                {{ $t('label.clear.all') }}
              </a-button>
            </a-space>
          </div>
          <div style="max-height: 200px; overflow-y: auto;">
            <div
              v-for="columnKey in columnKeys"
              :key="columnKey"
              style="margin-bottom: 4px;"
            >
              <a-checkbox
                :checked="selectedColumnKeys.includes(columnKey)"
                @change="updateSelectedColumns(columnKey)"
              >
                {{ $t('label.' + String(columnKey).toLowerCase()) }}
              </a-checkbox>
            </div>
          </div>
        </div>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'

export default {
  name: 'GPUTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    },
    resourceType: {
      type: String,
      required: true,
      default: 'Host'
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['busaddress', 'gpucardname', 'vgpuprofilename', 'gpudevicetype', 'resourcestate', 'state', 'virtualmachinename'],
      selectedColumnKeys: [],
      columns: [],
      items: [],
      selectedGpuDeviceIds: []
    }
  },
  computed: {
    hasSelectedDevices () {
      return this.selectedGpuDeviceIds.length > 0
    },
    selectedDeviceCount () {
      return this.selectedGpuDeviceIds.length
    }
  },
  created () {
    this.selectedColumnKeys = this.columnKeys
    this.updateColumns()
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchGPUDevices()
      }
    }
  },
  methods: {
    validateBulkOperation () {
      if (this.selectedGpuDeviceIds.length === 0) {
        this.$notification.warning({
          message: this.$t('label.warning'),
          description: this.$t('message.please.select.gpu.devices')
        })
        return false
      }
      return true
    },
    handleBulkOperationSuccess (messageKey, count) {
      this.$notification.success({
        message: this.$t('label.success'),
        description: this.$t(messageKey, { count })
      })
      this.selectedGpuDeviceIds = []
      this.fetchGPUDevices()
    },
    fetchData () {
      this.fetchGPUDevices()
    },
    fetchGPUDevices () {
      this.items = []
      if (!this.resource.id) {
        return
      }
      const params = {}
      if (this.resourceType === 'Host') {
        params.hostid = this.resource.id
      }
      this.tabLoading = true
      api('listGpuDevices', params).then(json => {
        const devices = json?.listgpudevicesresponse?.gpudevice || []
        this.items = this.buildGpuTree(devices)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    buildGpuTree (devices) {
      // Separate parent devices and vGPUs
      const parentDevices = []
      const vgpuDevices = []
      for (const device of devices) {
        if (device.parentgpudeviceid) {
          vgpuDevices.push(device)
        } else {
          parentDevices.push(device)
        }
      }

      // Group vGPUs by their parent ID
      const vgpusByParent = {}
      vgpuDevices.forEach(vgpu => {
        const parentId = vgpu.parentgpudeviceid
        if (!vgpusByParent[parentId]) {
          vgpusByParent[parentId] = []
        }
        vgpusByParent[parentId].push(vgpu)
      })

      // Build tree structure
      const treeData = parentDevices.map(parent => {
        const children = vgpusByParent[parent.id] || []
        return {
          ...parent,
          children: children.length > 0 ? children : undefined
        }
      })

      return treeData
    },
    updateSelectedColumns (key) {
      if (this.selectedColumnKeys.includes(key)) {
        this.selectedColumnKeys = this.selectedColumnKeys.filter(x => x !== key)
      } else {
        this.selectedColumnKeys.push(key)
      }
      this.updateColumns()
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        this.columns.push({
          key: columnKey,
          title: this.$t('label.' + String(columnKey).toLowerCase()),
          dataIndex: columnKey,
          sorter: (a, b) => { return genericCompare(a[columnKey] || '', b[columnKey] || '') }
        })
      }
      // Add column filter as the last column
      this.columns.push({
        key: 'columnFilter',
        title: null,
        dataIndex: 'columnFilter',
        customFilterDropdown: true,
        onFilter: () => true
      })
    },
    onGpuDeviceSelectionChange (keys) {
      this.selectedGpuDeviceIds = keys
    },
    customRowProps (record) {
      return {
        class: record.parentgpudeviceid ? 'vgpu-row' : 'parent-gpu-row'
      }
    },
    selectAllColumns () {
      this.selectedColumnKeys = this.columnKeys
      this.updateColumns()
    },
    clearAllColumns () {
      this.selectedColumnKeys = []
      this.updateColumns()
    },
    bulkEnableGpuDevices () {
      if (!this.validateBulkOperation()) return

      api('enableGpuDevice', {
        ids: this.selectedGpuDeviceIds.join(',')
      }).then(() => {
        this.handleBulkOperationSuccess('message.success.enable.gpu.devices', this.selectedGpuDeviceIds.length)
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    bulkDisableGpuDevices () {
      if (!this.validateBulkOperation()) return

      api('disableGpuDevice', {
        ids: this.selectedGpuDeviceIds.join(',')
      }).then(() => {
        this.handleBulkOperationSuccess('message.success.disable.gpu.devices', this.selectedGpuDeviceIds.length)
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  }
}
</script>

<style scoped>
/* Background colors for GPU device types */
:deep(.parent-gpu-row) {
  background-color: #fafafa;
}

:deep(.vgpu-row) {
  background-color: #f0f8ff;
}

:deep(.parent-gpu-row:hover) {
  background-color: #f5f5f5 !important;
}

:deep(.vgpu-row:hover) {
  background-color: #e6f4ff !important;
}

/* Text truncation for long names */
:deep(.ant-table-tbody .ant-table-cell) {
  max-width: 200px;
}

:deep(.ant-table-tbody .ant-table-cell:has([data-column="gpucardname"]),
  .ant-table-tbody .ant-table-cell:has([data-column="vgpuprofilename"]),
  .ant-table-tbody .ant-table-cell:has([data-column="hostname"])) {
  max-width: 150px;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  display: inline-block;
}
</style>
