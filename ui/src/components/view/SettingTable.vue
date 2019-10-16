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
    size="small"
    :scroll="{ x: 'true' }"
    :loading="loading"
    :columns="columns"
    :dataSource="items"
    :rowKey="record => record.id || record.name || record.key"
    :pagination="false"
    :rowClassName="getRowClassName"
    bordered
  >
    <template slot="value" slot-scope="text, record">
      <span style="float: left; margin-right: 5px" v-if="record">
        <a-button size="small" shape="circle">
          <a-icon type="close" />
        </a-button>
        <a-button size="small" shape="circle">
          <a-icon type="edit" />
        </a-button>
      </span>
      <span>{{ text }}</span>
    </template>
  </a-table>
</template>

<script>
export default {
  name: 'SettingTable',
  props: {
    columns: {
      type: Array,
      required: true
    },
    items: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
    }
  },
  methods: {
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    }
  }
}
</script>

<style scoped>
/deep/ .ant-table-thead {
  background-color: #f9f9f9;
}

/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}
</style>
