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
        :columns="innerColumns"
        :showHeader="false"
        :dataSource="configrecords"
        :pagination="false"
        :bordered="false"
        :defaultExpandAllRows="true"
        :rowKey="record => record.name">
          <template #name="{ record }">
            <b> {{record.displaytext }} </b> {{ ' (' + record.name + ')' }} <br/> {{ record.description }}
          </template>
          <template #value="{ record }">
            <ConfigurationValue :configrecord="record" :loading="loading" />
          </template>
          <template #expandedRowRender="{ record }" v-if="configrecord.type==='Boolean'">
             <!-- Add children ConfigurationRow with parent, v-if record.type == 'Boolean' and record.value == true and config has records with parent as record.name -->
            {{record.displaytext }}
            <ConfigurationValue :configrecord="record" :loading="loading" />
          </template>
    </a-table>
</template>
<script>
import ConfigurationValue from './ConfigurationValue'

export default {
  name: 'ConfigurationRow',
  components: {
    ConfigurationValue
  },
  props: {
    config: {
      type: Object,
      required: true
    },
    configrecord: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      fetchLoading: false,
      innerColumns: [
        {
          title: 'name',
          dataIndex: 'name',
          slots: { customRender: 'name' }
        },
        {
          title: 'value',
          dataIndex: 'value',
          slots: { customRender: 'value' }
        }
      ],
      configrecords: [
        this.configrecord
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
  },
  methods: {
    fetchData () {
      this.fetchLoading = false
    },
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    }
  }
}
</script>
