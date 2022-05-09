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
  <a-spin :spinning="rowLoading">
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
            <ConfigurationValue :configrecord="record" :loading="rowLoading" :configDisabled="configDisabled" @change-config="onChangeConfig" />
          </template>

          <template #expandedRowRender="{}" v-if="childrenConfigData.length > 0">
            <a-table
              size="small"
              :showHeader="false"
              :columns="childrenColumns"
              :dataSource="childrenConfigData"
              :rowKey="record => record.name"
              :pagination="false"
              :rowClassName="getRowClassName"
              style="overflow-y: auto; margin-left: 10px" >

              <template #displaytext="{ record }">
                <ConfigurationRow :config="this.childrenConfigData" :configrecord="record" :loading="rowLoading" :configDisabled="!childrenConfigEnabled" />
              </template>
            </a-table>
          </template>
    </a-table>
  </a-spin>
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
    },
    configDisabled: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      rowLoading: this.loading,
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
      childrenColumns: [
        {
          title: 'Display Text',
          dataIndex: 'displaytext',
          slots: { customRender: 'displaytext' }
        }
      ],
      childrenConfigData: [],
      childrenConfigEnabled: true,
      configrecords: [
        this.configrecord
      ]
    }
  },
  created () {
    this.fetchChildrenConfigData()
  },
  watch: {
  },
  methods: {
    fetchChildrenConfigData () {
      if (!this.isBooleanValue()) {
        this.rowLoading = false
        return
      }
      this.rowLoading = true
      this.childrenConfigEnabled = (this.configrecord.value === 'true')
      const params = {
        parent: this.configrecord.name,
        listAll: true
      }
      api('listConfigurations', params).then(response => {
        this.childrenConfigData = response.listconfigurationsresponse.configuration
        if (!this.childrenConfigData || this.childrenConfigData.length === 0) {
          this.childrenConfigData = []
        } else {
          console.log(this.childrenConfigData)
        }
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.rowLoading = false
      })
    },
    isBooleanValue () {
      return (this.configrecord.type === 'Boolean')
    },
    onChangeConfig (opts) {
      console.log('onChangeConfig')
      console.log(opts)
      if (this.isBooleanValue() && opts && Object.keys(opts).length > 0) {
        if ('value' in opts) {
          this.childrenConfigEnabled = opts.value
        }
      }
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
