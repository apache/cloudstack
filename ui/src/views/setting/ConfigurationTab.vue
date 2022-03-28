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
      size="middle"
      :showHeader="false"
      :columns="columns"
      :dataSource="this.configdata"
      :rowKey="record => record.name"
      :pagination="false"
      :rowClassName="getRowClassName"
      style="overflow-y: auto; margin-left: 10px" >

      <!-- Record without parent only on top, v-if record.parent == null -->
      <template #displaytext="{ record }">
        <ConfigurationRow :config="this.configdata" :configrecord="record" :loading="loading" />
      </template>
      <!-- Add children ConfigurationRow with parent, v-if record.type == 'Boolean' and record.value == true and config has records with parent as record.name -->
    </a-table>
</template>

<script>
import { api } from '@/api'
import ConfigurationRow from './ConfigurationRow'

export default {
  name: 'ConfigurationTab',
  components: {
    ConfigurationRow
  },
  props: {
    config: {
      type: Object,
      required: true
    },
    group: {
      type: String,
      required: true
    },
    subgroup: {
      type: String,
      required: false
    },
    parent: {
      type: String,
      required: false
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      tabLoading: false,
      columns: [
        {
          title: 'Display Text',
          dataIndex: 'displaytext',
          slots: { customRender: 'displaytext' }
        }
      ],
      apiName: 'listConfigurations',
      configdata: []
    }
  },
  created () {
    this.fetchConfigurationDataByGroup()
  },
  watch: {
  },
  methods: {
    fetchConfigurationDataByGroup () {
      this.tabLoading = true
      const params = {
        group: this.group,
        subgroup: this.subgroup,
        pagesize: -1
      }
      console.log('group name: ' + this.group)
      api('listConfigurations', params).then(response => {
        this.configdata = response.listconfigurationsresponse.configuration
        console.log(this.configdata)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.tabLoading = false
      })
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
