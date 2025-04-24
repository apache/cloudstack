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
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="items"
      :actions="actions"
      :columnKeys="columnKeys"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData"
      @enable-gpu-device="enableGpuDevice"
      @disable-gpu-device="disableGpuDevice" />
    </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'
export default {
  name: 'GPUTab',
  components: {
    ListView
  },
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
        this.items = []
        this.items = json?.listgpudevicesresponse?.gpudevice || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
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
