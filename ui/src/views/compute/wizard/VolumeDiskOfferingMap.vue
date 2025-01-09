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
      :scroll="{ y: 225 }" >

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span>{{ record.displaytext || record.name }}</span>
        </template>
        <template v-if="column.key === 'offering'">
          <span
            style="width: 50%"
            v-if="validOfferings[record.id] && validOfferings[record.id].length > 0">
            <a-select
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
        <template v-if="column.key === 'size'">
          <span
            style="width: 50%"
            v-if="custom[record.id]">
            <a-input-number
              :min="1"
              :max="1000"
              :defaultValue="record.size"
              @change="updateCustomDiskSize($event, record.id)"
            />
          </span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'VolumeDiskOfferingMap',
  components: {
  },
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
      custom: {},
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
        },
        {
          key: 'size',
          title: this.$t('label.sizegb')
        }
      ],
      loading: false,
      diskOfferings: [],
      validOfferings: {},
      selectedCustomDiskOffering: null,
      values: {
        offering: '',
        size: ''
      }
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
    isOfferingCustom () {
      return 'true'
    }
  },
  watch: {
    items: {
      deep: true,
      handler (newItem, oldItem) {
        if (newItem === oldItem) return
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
        this.orderDiskOfferings()
      }).finally(() => {
        this.loading = false
      })
    },
    orderDiskOfferings () {
      this.loading = true
      this.validOfferings = {}
      for (const item of this.items) {
        this.validOfferings[item.id] = this.diskOfferings.filter(x => x.disksize >= item.size || (x.iscustomized))
      }
      this.diskOfferings.map(x => {
        this.custom[x.id] = x.iscustomized
      })
      this.setDefaultValues()
      this.loading = false
    },
    setDefaultValues () {
      this.values = {}
      for (const item of this.items) {
        this.values[item.id] = {
          offering: this.validOfferings[item.id]?.[0]?.id || '',
          size: this.validOfferings[item.id]?.[0]?.disksize || ''
        }
      }
      this.sendValues()
    },
    updateCustomDiskSize (value, diskId) {
      this.values[diskId].size = value
      this.sendValues()
    },
    updateOfferingSelect (value, diskId) {
      this.values[diskId].offering = value
      this.custom[diskId] = this.diskOfferings.find(x => x.id === value)?.iscustomized
      this.sendValues()
    },
    sendValues () {
      const data = {}
      for (var x in this.values) {
        data[x] = this.values[x]
      }
      this.$emit('select-volumes-disk-offering', data)
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }
</style>
