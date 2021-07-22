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
      :loading="loading"
      :columns="columns"
      :dataSource="tableSource"
      :rowKey="record => record.id"
      :pagination="false"
      :rowSelection="rowSelection"
      :scroll="{ y: 225 }" >

      <span slot="offering" slot-scope="text, record">
        <a-select
          autoFocus
          v-if="validOfferings[record.id] && validOfferings[record.id].length > 0"
          @change="updateOffering($event, record.id)"
          :defaultValue="validOfferings[record.id][0].id">
          <a-select-option v-for="offering in validOfferings[record.id]" :key="offering.id">
            {{ offering.displaytext }}
          </a-select-option>
        </a-select>
        <span v-else>
          {{ $t('label.no.matching.offering') }}
        </span>
      </span>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'MultiDiskSelection',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    zoneId: {
      type: String,
      default: () => ''
    }
  },
  data () {
    return {
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.data.disk')
        },
        {
          dataIndex: 'offering',
          title: this.$t('label.data.disk.offering'),
          scopedSlots: { customRender: 'offering' }
        }
      ],
      loading: false,
      selectedRowKeys: [],
      diskOfferings: [],
      validOfferings: {},
      values: {}
    }
  },
  computed: {
    tableSource () {
      return this.items.map(item => {
        return {
          id: item.id,
          name: `${item.name} (${item.size} GB)`,
          disabled: this.validOfferings[item.id] && this.validOfferings[item.id].length === 0
        }
      })
    },
    rowSelection () {
      return {
        type: 'checkbox',
        selectedRowKeys: this.selectedRowKeys,
        getCheckboxProps: record => ({
          props: {
            disabled: record.disabled
          }
        }),
        onChange: (rows) => {
          this.selectedRowKeys = rows
          this.sendValues()
        }
      }
    }
  },
  watch: {
    items (newData, oldData) {
      this.items = newData
      this.selectedRowKeys = []
      this.fetchDiskOfferings()
    },
    zoneId (newData) {
      this.zoneId = newData
      this.fetchDiskOfferings()
    }
  },
  created () {
    this.fetchDiskOfferings()
  },
  methods: {
    fetchDiskOfferings () {
      this.diskOfferings = []
      this.loading = true
      api('listDiskOfferings', {
        zoneid: this.zoneId,
        listall: true
      }).then(response => {
        this.diskOfferings = response.listdiskofferingsresponse.diskoffering || []
        this.diskOfferings = this.diskOfferings.filter(x => !x.iscustomized)
        this.orderDiskOfferings()
      }).finally(() => {
        this.loading = false
      })
    },
    orderDiskOfferings () {
      this.loading = true
      this.validOfferings = {}
      for (const item of this.items) {
        this.validOfferings[item.id] = this.diskOfferings.filter(x => x.disksize >= item.size)
      }
      this.setDefaultValues()
      this.loading = false
    },
    setDefaultValues () {
      this.values = {}
      for (const item of this.items) {
        this.values[item.id] = this.validOfferings[item.id].length > 0 ? this.validOfferings[item.id][0].id : ''
      }
    },
    updateOffering (value, templateid) {
      this.values[templateid] = value
      this.sendValues()
    },
    sendValues () {
      const data = {}
      this.selectedRowKeys.map(x => {
        data[x] = this.values[x]
      })
      this.$emit('select-multi-disk-offering', data)
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
