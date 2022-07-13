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

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span>{{ record.displaytext || record.name }}</span>
          <div v-if="record.meta">
            <div v-for="meta in record.meta" :key="meta.key">
              <a-tag style="margin-top: 5px" :key="meta.key">{{ meta.key + ': ' + meta.value }}</a-tag>
            </div>
          </div>
        </template>
        <template v-if="column.key === 'offering'">
          <span
            style="width: 50%"
            v-if="validOfferings[record.id] && validOfferings[record.id].length > 0">
            <check-box-select-pair
              v-if="selectedCustomDiskOffering!=null"
              layout="vertical"
              :resourceKey="record.id"
              :selectOptions="validOfferings[record.id]"
              :checkBoxLabel="autoSelectLabel"
              :defaultCheckBoxValue="true"
              :reversed="true"
              @handle-checkselectpair-change="updateOfferingCheckPairSelect" />
            <a-select
              v-else
              @change="updateOfferingSelect($event, record.id)"
              :defaultValue="validOfferings[record.id][0].id"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="offering in validOfferings[record.id]" :key="offering.id" :label="offering.displaytext">
                {{ offering.displaytext }}
              </a-select-option>
            </a-select>
          </span>
          <span v-else style="width: 50%">
            {{ $t('label.no.matching.offering') }}
          </span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'

export default {
  name: 'MultiDiskSelection',
  components: {
    CheckBoxSelectPair
  },
  props: {
    items: {
      type: Array,
      default: () => []
    },
    zoneId: {
      type: String,
      default: () => ''
    },
    selectionEnabled: {
      type: Boolean,
      default: true
    },
    customOfferingsAllowed: {
      type: Boolean,
      default: false
    },
    autoSelectCustomOffering: {
      type: Boolean,
      default: false
    },
    autoSelectLabel: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      columns: [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.data.disk')
        },
        {
          key: 'offering',
          dataIndex: 'offering',
          title: this.$t('label.data.disk.offering')
        }
      ],
      loading: false,
      selectedRowKeys: [],
      diskOfferings: [],
      validOfferings: {},
      selectedCustomDiskOffering: null,
      values: {}
    }
  },
  computed: {
    tableSource () {
      return this.items.map(item => {
        var disk = { ...item, disabled: this.validOfferings[item.id] && this.validOfferings[item.id].length === 0 }
        disk.name = `${item.name} (${item.size} GB)`
        return disk
      })
    },
    rowSelection () {
      if (this.selectionEnabled === true) {
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
      return null
    }
  },
  watch: {
    items: {
      deep: true,
      handler (newItem, oldItem) {
        if (newItem === oldItem) return
        this.selectedRowKeys = []
        this.fetchDiskOfferings()
      }
    },
    zoneId (newData) {
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
        if (!this.customOfferingsAllowed) {
          this.diskOfferings = this.diskOfferings.filter(x => !x.iscustomized)
        }
        this.orderDiskOfferings()
      }).finally(() => {
        this.loading = false
      })
    },
    orderDiskOfferings () {
      this.loading = true
      this.validOfferings = {}
      if (this.customOfferingsAllowed && this.autoSelectCustomOffering) {
        this.selectedCustomDiskOffering = this.diskOfferings.filter(x => x.iscustomized)?.[0]
      }
      for (const item of this.items) {
        this.validOfferings[item.id] = this.diskOfferings.filter(x => x.disksize >= item.size || (this.customOfferingsAllowed && x.iscustomized))
      }
      this.setDefaultValues()
      this.loading = false
    },
    setDefaultValues () {
      this.values = {}
      for (const item of this.items) {
        this.values[item.id] = this.selectedCustomDiskOffering?.id || this.validOfferings[item.id]?.[0]?.id || ''
      }
      this.sendValues()
    },
    updateOfferingCheckPairSelect (diskId, checked, value) {
      if (this.selectedCustomDiskOffering) {
        this.values[diskId] = checked ? this.selectedCustomDiskOffering.id : value
        this.sendValues()
      }
    },
    updateOfferingSelect (value, diskId) {
      this.values[diskId] = value
      this.sendValues()
    },
    sendValues () {
      const data = {}
      if (this.selectionEnabled) {
        this.selectedRowKeys.map(x => {
          data[x] = this.values[x]
        })
      } else {
        for (var x in this.values) {
          data[x] = this.values[x]
        }
      }
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
