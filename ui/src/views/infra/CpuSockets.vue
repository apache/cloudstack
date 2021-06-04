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
    <a-card class="breadcrumb-card">
      <a-row>
        <a-col :span="24" style="padding-left: 12px">
          <breadcrumb>
            <a-tooltip placement="bottom" slot="end">
              <template slot="title">{{ $t('label.refresh') }}</template>
              <a-button
                style="margin-top: 4px"
                :loading="loading"
                shape="round"
                size="small"
                icon="reload"
                @click="fetchData()"
              >{{ $t('label.refresh') }}</a-button>
            </a-tooltip>
          </breadcrumb>
        </a-col>
      </a-row>
    </a-card>

    <div class="row-element">
      <list-view
        :columns="columns"
        :items="items"
        :loading="loading"
        @refresh="this.fetchData" />
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import ListView from '@/components/view/ListView.vue'

export default {
  name: 'CpuSockets',
  components: {
    ListView,
    Breadcrumb
  },
  provide: function () {
    return {
      parentFetchData: this.fetchData,
      parentToggleLoading: () => { this.loading = !this.loading }
    }
  },
  data () {
    return {
      loading: false,
      items: [],
      data: {},
      columns: []
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    '$i18n.locale' (to, from) {
      if (to !== from) {
        this.fetchData()
      }
    }
  },
  methods: {
    pushData (hypervisor) {
      if (['BareMetal', 'LXC'].includes(hypervisor)) {
        this.data[hypervisor].cpusockets = 'N/A'
      }
      if (hypervisor === 'Hyperv') {
        this.data[hypervisor].name = 'Hyper-V'
      }
      this.items.push(this.data[hypervisor])
    },
    callListHostsWithPage (hypervisor, currentPage) {
      this.loading = true
      const pageSize = 100
      api('listHosts', {
        type: 'routing',
        details: 'min',
        hypervisor: hypervisor,
        page: currentPage,
        pagesize: pageSize
      }).then(json => {
        if (json.listhostsresponse.count === undefined) {
          this.pushData(hypervisor)
          return
        }

        this.data[hypervisor].hosts = json.listhostsresponse.count
        this.data[hypervisor].currentHosts += json.listhostsresponse.host.length

        for (const host of json.listhostsresponse.host) {
          if (host.cpusockets !== undefined && isNaN(host.cpusockets) === false) {
            this.data[hypervisor].cpusockets += host.cpusockets
          }
        }

        if (this.data[hypervisor].currentHosts < this.data[hypervisor].hosts) {
          this.callListHostsWithPage(hypervisor, currentPage + 1)
        } else {
          this.pushData(hypervisor)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    fetchData () {
      this.columns = []
      this.columns.push({
        dataIndex: 'name',
        title: this.$t('label.hypervisor'),
        sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
      })

      this.columns.push({
        dataIndex: 'hosts',
        title: this.$t('label.hosts'),
        sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
      })
      this.columns.push({
        dataIndex: 'cpusockets',
        title: this.$t('label.cpu.sockets'),
        sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
      })

      this.items = []
      this.data = {}
      const hypervisors = ['BareMetal', 'Hyperv', 'KVM', 'LXC', 'Ovm3', 'Simulator', 'VMware', 'XenServer']
      for (const hypervisor of hypervisors) {
        this.data[hypervisor] = {
          name: hypervisor,
          hosts: 0,
          cpusockets: 0,
          currentHosts: 0
        }
        this.callListHostsWithPage(hypervisor, 1)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.breadcrumb-card {
  margin-left: -24px;
  margin-right: -24px;
  margin-top: -18px;
  margin-bottom: 12px;
}

.row-element {
  margin-top: 10px;
  margin-bottom: 10px;
}

.ant-breadcrumb {
  vertical-align: text-bottom;
}

.ant-breadcrumb .anticon {
  margin-left: 8px;
}
</style>
