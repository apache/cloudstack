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
          <span>{{ record.name }}</span>
        </template>
        <template v-if="column.key === 'offering'">
          <span>
            <a-select
              @change="updateOfferingSelect($event, record.id)"
              v-model:value="defaultDiskOfferings[record.id]"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :style="{ width: '90%' }" >
              <a-select-option v-for="offering in validOfferings[record.id]" :key="offering.id" :label="offering.displaytext">
                {{ offering.displaytext }}
              </a-select-option>
            </a-select>
          </span>
        </template>
        <template v-if="column.key === 'size'">
          <span
            v-if="custom[record.id]">
            <a-input-number
              :defaultValue="record.size"
              :min="items[record.id].size"
              @change="updateCustomDiskSize($event, record.id)"
            />
          </span>
        </template>
        <template v-if="column.key === 'miniops'">
          <span
            v-if="customIops[record.id]">
            <a-input-number
              :defaultValue="record.miniops"
              @change="updateCustomMinIops($event, record.id)"
            />
          </span>
        </template>
        <template v-if="column.key === 'maxiops'">
          <span
            v-if="customIops[record.id]">
            <a-input-number
              :defaultValue="record.maxiops"
              @change="updateCustomMaxIops($event, record.id)"
            />
          </span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { getAPI } from '@/api'

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
      customIops: {},
      columns: [
        {
          key: 'name',
          dataIndex: 'name',
          title: this.$t('label.data.disk'),
          width: '35%'
        },
        {
          key: 'offering',
          dataIndex: 'offering',
          title: this.$t('label.data.disk.offering'),
          width: '25%'
        },
        {
          key: 'size',
          dataIndex: 'size',
          title: this.$t('label.sizegb'),
          width: '15%'
        },
        {
          key: 'miniops',
          dataIndex: 'miniops',
          title: this.$t('label.miniops'),
          width: '10%'
        },
        {
          key: 'maxiops',
          dataIndex: 'maxiops',
          title: this.$t('label.maxiops'),
          width: '10%'
        }
      ],
      loading: false,
      diskOfferings: [],
      validOfferings: [],
      defaultDiskOfferings: [],
      tablerows: {},
      selectedCustomDiskOffering: null,
      values: {
        offering: '',
        size: '',
        miniops: '',
        maxiops: '',
        iscustomizediops: false
      }
    }
  },
  computed: {
    tableSource () {
      return this.tablerows.map(row => {
        var disk = { ...row, disabled: this.diskOfferings && this.diskOfferings.length === 0 }
        var item = this.items.find(item => item.id === row.id)
        disk.name = `${item.name} (${item.size} GB)`
        return disk
      })
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
    this.tablerows = JSON.parse(JSON.stringify(this.items))
  },
  methods: {
    fetchDiskOfferings () {
      this.diskOfferings = []
      this.loading = true
      getAPI('listDiskOfferings', {
        zoneid: this.zoneId,
        listall: true
      }).then(response => {
        this.diskOfferings = response.listdiskofferingsresponse.diskoffering || []
        this.orderDiskOfferings()
        this.setDefaultDiskOfferings()
      }).finally(() => {
        this.loading = false
      })
    },
    setDefaultDiskOfferings () {
      for (const record of this.tablerows) {
        this.defaultDiskOfferings[record.id] = this.diskOfferings.find(x => x.id === record?.diskofferingid) ? record.diskofferingid : null
      }
    },
    orderDiskOfferings () {
      this.loading = true
      this.items.map(x => {
        this.custom[x.id] = this.diskOfferings.find(offering => offering.id === x.diskofferingid)?.iscustomized
        this.customIops[x.id] = this.diskOfferings.find(offering => offering.id === x.diskofferingid)?.iscustomizediops || false
      })
      for (const item of this.items) {
        this.validOfferings[item.id] = this.diskOfferings.filter(x => x.disksize >= item.size || (x.iscustomized))
      }
      this.setDefaultValues()
      this.loading = false
    },
    getDefaultDiskOffering (record) {
      return this.diskOfferings.find(x => x.id === record?.diskofferingid) ? record.diskofferingid : null
    },
    setDefaultValues () {
      this.values = {}
      for (const item of this.items) {
        this.values[item.id] = {
          offering: item.diskofferingid,
          deviceid: item.deviceid,
          size: item.size,
          miniops: item.miniops,
          maxiops: item.maxiops,
          iscustomizediops: this.diskOfferings.find(x => x.id === item.diskofferingid)?.iscustomizediops || false
        }
      }
      this.sendValues()
    },
    updateCustomDiskSize (value, diskId) {
      this.values[diskId].size = value
      this.sendValues()
    },
    updateCustomMinIops (value, diskId) {
      this.values[diskId].miniops = value
      this.sendValues()
    },
    updateCustomMaxIops (value, diskId) {
      this.values[diskId].maxiops = value
      this.sendValues()
    },
    updateOfferingSelect (value, diskId) {
      this.values[diskId].offering = value
      if (this.diskOfferings.find(x => x.id === value)?.iscustomized) {
        this.values[diskId].size = this.items[diskId].size
      } else {
        this.values[diskId].size = Math.max(this.diskOfferings.find(x => x.id === value)?.disksize, this.items[diskId].size)
      }
      this.tablerows[diskId].size = this.values[diskId].size

      this.values[diskId].iscustomizediops = this.diskOfferings.find(x => x.id === value)?.iscustomizediops || false

      if (this.values[diskId].iscustomizediops) {
        this.values[diskId].miniops = this.items[diskId].miniops
        this.values[diskId].maxiops = this.items[diskId].maxiops
        this.tablerows[diskId].miniops = this.items[diskId].miniops
        this.tablerows[diskId].maxiops = this.items[diskId].maxiops
      } else {
        this.values[diskId].miniops = ''
        this.values[diskId].maxiops = ''
        this.tablerows[diskId].miniops = ''
        this.tablerows[diskId].maxiops = ''
      }

      this.custom[diskId] = this.diskOfferings.find(x => x.id === value)?.iscustomized
      this.customIops[diskId] = this.diskOfferings.find(x => x.id === value)?.iscustomizediops || false

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
