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
      :loading="tabLoading"
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.id"
      :childrenColumnName="'children'"
      :defaultExpandAllRows="true"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'gpuDeviceActions'">
          <a-space>
            <a-button
              v-if="record.state === 'Disabled'"
              type="primary"
              size="small"
              @click="enableGpuDevice(record)"
            >
              {{ $t('label.enable') }}
            </a-button>
            <a-button
              v-if="record.state === 'Free'"
              type="primary"
              danger
              size="small"
              @click="disableGpuDevice(record)"
            >
              {{ $t('label.disable') }}
            </a-button>
          </a-space>
        </template>
        <template v-else-if="column.key === 'busaddress'">
          <span :style="{ paddingLeft: record.parentgpudeviceid ? '20px' : '0px' }">
            {{ record.busaddress }}
          </span>
        </template>
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
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['busaddress', 'gpucardname', 'vgpuprofilename', 'virtualmachinename', 'state'],
      selectedColumnKeys: [],
      columns: [],
      cols: [],
      items: [],
      actions: []
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
    fetchData () {
      this.fetchGPUDevices()
    },
    fetchGPUDevices () {
      this.items = []
      if (!this.resource.id) {
        return
      }
      const params = {
        hostid: this.resource.id
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
      this.columns.push({
        key: 'gpuDeviceActions',
        title: this.$t('label.actions'),
        dataIndex: 'gpuDeviceActions'
      })
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    enableGpuDevice (record) {
      api('enableGpuDevice', {
        id: record.id
      }).then(json => {
        this.fetchGPUDevices()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    disableGpuDevice (record) {
      api('disableGpuDevice', {
        id: record.id
      }).then(json => {
        this.fetchGPUDevices()
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  }
}
</script>
