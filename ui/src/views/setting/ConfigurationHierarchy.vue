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
    :showHeader="false"
    :columns="columns"
    :dataSource="config"
    :rowKey="record => record.name"
    :pagination="false"
    :rowClassName="getRowClassName"
    style="overflow-y: auto; margin-left: 10px" >

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
    <span :style="hierarchyExists ? 'padding-left: 0px;' : 'padding-left: 25px;'">
      <b><span v-if="record.parent">└─ &nbsp;</span>{{record.displaytext }} </b> {{ ' (' + record.name + ')' }}
    </span>
    <br/>
    <span :style="record.parent ? 'padding-left: 50px; display:block' : 'padding-left: 25px; display:block'">{{ record.description }}</span>
      </template>
      <template v-if="column.key === 'value'">
        <ConfigurationValue :configrecord="record" />
      </template>
    </template>
  </a-table>
</template>

<script>
import ConfigurationValue from './ConfigurationValue'

export default {
  name: 'ConfigurationHierarchy',
  components: {
    ConfigurationValue
  },
  props: {
    config: {
      type: Array,
      default: () => { return [] }
    },
    columns: {
      type: Array,
      default: () => { return [] }
    }
  },
  computed: {
    hierarchyExists () {
      for (var c of this.config) {
        if (c.children) {
          return true
        }
      }
      return false
    }
  },
  data () {
    return {
      apiName: 'listConfigurations',
      configdata: []
    }
  },
  methods: {
    getRowClassName (record, index) {
      if (record.parent) {
        return 'child-row'
      }
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    }
  }
}
</script>
