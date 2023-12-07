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
    style="overflow-y: auto"
    :columns="columns"
    :dataSource="virtualmachines"
    :rowKey="item => item.id"
    :pagination="false"
    :loading="fetchLoading"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.key === 'name'">
        <router-link :to="{ path: '/vnfapp/' + record.id }" >{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'status'">
        <status class="status" :text="record.state" displayText />
      </template>
      <template v-if="column.key === 'vmip'">
        <status class="status" :text="record.vmip" displayText />
      </template>
      <template v-if="column.key === 'templatename'">
        <router-link :to="{ path: '/template/' + record.templateid }" >{{ record.templatename || record.templateid }}</router-link>
      </template>
      <template v-if="column.key === 'osdisplayname'">
        <status class="status" :text="record.osdisplayname" displayText />
      </template>
      <template v-if="column.key === 'hostname'">
        <router-link :to="{ path: '/host/' + record.hostid }" >{{ record.hostname || record.hostid }}</router-link>
      </template>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'VnfAppliancesTab',
  components: {
    Status
  },
  props: {
    resource: {
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
      virtualmachines: [],
      columns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          key: 'status',
          title: this.$t('label.status'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.ipaddress'),
          dataIndex: 'vmip'
        },
        {
          key: 'templatename',
          title: this.$t('label.templatename'),
          dataIndex: 'templatename'
        },
        {
          title: this.$t('label.osdisplayname'),
          dataIndex: 'osdisplayname'
        },
        {
          key: 'hostname',
          title: this.$t('label.hostname'),
          dataIndex: 'hostname'
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      var params = {
        details: 'servoff,tmpl,nics',
        isVnf: true,
        listAll: true
      }
      if (this.$route.fullPath.startsWith('/vpc')) {
        params.vpcid = this.resource.id
      } else {
        params.networkid = this.resource.id
      }
      this.fetchLoading = true
      api('listVirtualMachines', params).then(json => {
        this.virtualmachines = json.listvirtualmachinesresponse.virtualmachine || []
        for (const vm of this.virtualmachines) {
          for (const vmnic of vm.nic) {
            if (vmnic.networkid === this.resource.id) {
              vm.vmip = vmnic.ipaddress
            }
          }
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.status {
  margin-top: -5px;

  &--end {
    margin-left: 5px;
  }
}
</style>
