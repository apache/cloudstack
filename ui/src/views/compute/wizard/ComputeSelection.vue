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
  <a-table
    :columns="columns"
    :dataSource="tableSource"
    :pagination="false"
    :scroll="{x: 0, y: 320}"
    :rowSelection="rowSelection"
    size="middle"
  >
    <span slot="cpuTitle"><a-icon type="appstore" /> {{ $t('cpu') }}</span>
    <span slot="ramTitle"><a-icon type="bulb" /> {{ $t('ram') }}</span>
  </a-table>
</template>

<script>
export default {
  name: 'ComputeSelection',
  props: {
    computeItems: {
      type: Array,
      default: () => []
    },
    value: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      columns: [
        {
          dataIndex: 'name',
          width: '40%'
        },
        {
          dataIndex: 'cpu',
          slots: { title: 'cpuTitle' },
          width: '30%'
        },
        {
          dataIndex: 'ram',
          slots: { title: 'ramTitle' },
          width: '30%'
        }
      ],
      selectedRowKeys: []
    }
  },
  computed: {
    tableSource () {
      return this.computeItems.map((item) => {
        return {
          key: item.id,
          name: item.name,
          cpu: `${item.cpunumber} CPU x ${parseFloat(item.cpuspeed / 1000.0).toFixed(2)} Ghz`,
          ram: `${item.memory} MB`
        }
      })
    },
    rowSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.selectedRowKeys,
        onSelect: (row) => {
          this.$emit('select-compute-item', row.key)
        }
      }
    }
  },
  watch: {
    value (newValue, oldValue) {
      if (newValue && newValue !== oldValue) {
        this.selectedRowKeys = [newValue]
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>

<style lang="less">
  .ant-table-selection-column {
    // Fix for the table header if the row selection use radio buttons instead of checkboxes
    > div:empty {
      width: 16px;
    }
  }
</style>
