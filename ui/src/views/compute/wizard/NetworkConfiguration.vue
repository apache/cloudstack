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
    :dataSource="items"
    :pagination="{showSizeChanger: true}"
    :rowSelection="rowSelection"
    :rowKey="record => record.id"
    size="middle"
  >
    <template v-slot:ipAddress="text">
      <a-input
        :value="text"
      ></a-input>
    </template>
    <template v-slot:macAddress="text">
      <a-input
        :value="text"
      ></a-input>
    </template>
  </a-table>
</template>

<script>
export default {
  name: 'NetworkConfiguration',
  props: {
    items: {
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
          title: this.$t('defaultNetwork'),
          width: '40%'
        },
        {
          dataIndex: 'ip',
          title: this.$t('ip'),
          width: '30%',
          scopedSlots: { customRender: 'ipAddress' }
        },
        {
          dataIndex: 'mac',
          title: this.$t('macaddress'),
          width: '30%',
          scopedSlots: { customRender: 'macAddress' }
        }
      ],
      selectedRowKeys: []
    }
  },
  computed: {
    rowSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.selectedRowKeys,
        onSelect: (row) => {
          this.$emit('select-default-network-item', row.key)
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
