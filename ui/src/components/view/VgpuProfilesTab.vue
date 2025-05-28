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
    <a-button type="primary" @click="createVgpuProfile">
      {{ $t('label.add.vgpu.profile') }}
    </a-button>
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="items"
      :actions="actions"
      :columnKeys="columnKeys"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData" />
    </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'
export default {
  name: 'VgpuProfilesTab',
  components: {
    ListView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      required: true,
      default: 'GpuOffering'
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['name', 'gpucardname', 'vramsize', 'vgpuProfileActions'],
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
    this.actions = [{
      api: 'createVgpuProfile',
      icon: 'plus-outlined',
      label: 'label.add.vgpu.profile',
      listView: true,
      dataView: false,
      args: ['name', 'description', 'vramsize', 'gpucardid']
    }]
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (this.resourceType === 'GpuCard') {
        this.fetchVgpuProfiles()
      } else {
        this.items = this.resource.vgpuprofiles
      }
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
          title: columnKey === 'name' ? this.$t('label.vgpu.profile') : this.$t('label.' + String(columnKey).toLowerCase()),
          dataIndex: columnKey,
          sorter: (a, b) => { return genericCompare(a[columnKey] || '', b[columnKey] || '') }
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    fetchVgpuProfiles () {
      api('listVgpuProfiles', {
        gpucardid: this.resource.id
      }).then(res => {
        this.items = res?.listvgpuprofilesresponse?.vgpuprofile || []
      })
    }
  }
}
</script>
