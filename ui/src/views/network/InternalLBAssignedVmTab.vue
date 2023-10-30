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
  <a-spin :spinning="fetchLoading">
    <a-table
      size="small"
      style="overflow-y: auto; margin-bottom: 15px"
      :columns="columns"
      :dataSource="assignedVms"
      :rowKey="item => item.id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'displayname'">
          <router-link :to="{ path: '/vm/' + record.id }">{{ record.displayname || record.name }}</router-link>
        </template>
        <template v-if="column.key === 'ipaddress'">
          <span v-for="nic in record.nic" :key="nic.id">
            <span v-if="nic.networkid === resource.networkid">
              {{ nic.ipaddress }} <br/>
            </span>
          </span>
        </template>
        <template v-if="column.key === 'remove'">
          <tooltip-button
            :tooltip="$t('label.remove.vm.from.lb')"
            type="primary"
            :danger="true"
            icon="delete-outlined"
            @onClick="removeVmFromLB(record)" />
        </template>
      </template>
      <a-divider />
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalInstances"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </a-spin>
</template>
<script>
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'InternalLBAssignedVmTab',
  components: {
    TooltipButton
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      fetchLoading: false,
      assignedVms: [],
      page: 1,
      pageSize: 10,
      totalInstances: 0,
      columns: [
        {
          key: 'displayname',
          title: this.$t('label.name'),
          dataIndex: 'displayname'
        },
        {
          key: 'ipaddress',
          title: this.$t('label.ipaddress'),
          dataIndex: 'ipaddress'
        },
        {
          key: 'remove',
          title: ''
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
      this.fetchLoading = true
      api('listLoadBalancerRuleInstances', {
        id: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.totalInstances = response.listloadbalancerruleinstancesresponse.count || 0
        this.assignedVms = response.listloadbalancerruleinstancesresponse.loadbalancerruleinstance || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    removeVmFromLB (vm) {
      this.fetchLoading = true
      api('removeFromLoadBalancerRule', {
        id: this.resource.id,
        virtualmachineids: vm.id
      }).then(response => {
        this.$pollJob({
          jobId: response.removefromloadbalancerruleresponse.jobid,
          successMessage: `${this.$t('message.success.remove.ip')} ${vm.name} ${this.$t('label.from')} ${this.resource.name}`,
          successMethod: () => {
            this.fetchLoading = false
            this.fetchData()
          },
          errorMessage: `${this.$t('message.failed.to.remove')} ${vm.name} ${this.$t('label.from.lb')}`,
          errorMethod: () => {
            this.fetchLoading = false
            this.fetchData()
          },
          loadingMessage: `${this.$t('label.removing')} ${vm.name} ${this.$t('label.from.lb')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result')
        })
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>
<style lang="scss" scoped>
</style>
