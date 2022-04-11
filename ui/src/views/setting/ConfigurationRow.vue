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
          <template #expandedRowRender="{}" v-if="parentConfigData.length > 0">
            <p style="margin: 0">
              <!-- Add children ConfigurationRow with parent, v-if record.type == 'Boolean' and record.value == true and config has records with parent as record.name -->
              <a-table
                size="small"
                :showHeader="false"
                :columns="parentColumns"
                :dataSource="this.parentConfigData"
                :rowKey="record => record.name"
                :pagination="false"
                :rowClassName="getRowClassName"
                style="overflow-y: auto; margin-left: 10px" >

                <template #displaytext="{ record }">
                  <ConfigurationRow :config="this.parentConfigData" :configrecord="record" :loading="loading" />
                </template>
              </a-table>
            </p>
          </template>
    </a-table>
</template>
<script>
import { api } from '@/api'
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
          slots: { customRender: 'value' },
          width: '29%'
        }
      ],
      parentColumns: [
        {
          title: 'Display Text',
          dataIndex: 'displaytext',
          slots: { customRender: 'displaytext' }
        }
      ],
      parentConfigData: [],
      configrecords: [
        this.configrecord
      ]
    }
  },
  created () {
    this.fetchParentConfigData()
  },
  watch: {
  },
  methods: {
    fetchParentConfigData () {
      if (!this.isBooleanValue()) {
        return
      }
      this.fetchLoading = true
      const params = {
        parent: this.configrecord.name,
        listAll: true
      }
      api('listConfigurations', params).then(response => {
        this.parentConfigData = response.listconfigurationsresponse.configuration
        if (!this.parentConfigData || this.parentConfigData.length === 0) {
          this.parentConfigData = []
        } else {
          console.log(this.parentConfigData)
        }
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    isBooleanValue () {
      return (this.configrecord.type === 'Boolean')
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
