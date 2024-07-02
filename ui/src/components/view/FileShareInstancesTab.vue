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
    class="table"
    size="small"
    :columns="vmColumns"
    :dataSource="virtualmachines"
    :rowKey="item => item.id"
    :pagination="false"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'" :name="text">
          <router-link :to="{ path: '/systemvm/' + record.id }">{{ record.name }}</router-link>
        </template>
        <template v-if="column.key === 'status'">
          <status class="status" :text="record.state" displayText />
        </template>
      </template>
    </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'FileShareAccessTab',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      instanceLoading: false,
      virtualmachines: []
    }
  },
  created () {
    this.vmColumns = [
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
        title: this.$t('label.zonename'),
        dataIndex: 'zonename'
      }
    ]
    this.fetchInstances()
  },
  methods: {
    fetchInstances () {
      this.instanceLoading = true
      this.loading = true
      var params = {
        id: this.resource.virtualmachineid,
        systemvmtype: 'storagefsvm',
        listall: true
      }
      api('listSystemVms', params).then(json => {
        this.virtualmachines = json.listsystemvmsresponse.systemvm || []
        this.virtualmachines.map(x => { x.ipaddress = x.nic[0].ipaddress })
      }).finally(() => {
        this.loading = false
      })
      this.instanceLoading = false
    }
  }
}
</script>

<style lang="css" scoped>
.title {
  font-weight: bold;
  margin-bottom: 14px;
  font-size: 16px; /* Increased font size */
}

.content {
  font-size: 16px; /* Increased font size */
}
</style>
