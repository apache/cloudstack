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
    <div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('sourcecidr') }}</div>
          <a-input v-model="newRule.cidrlist"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('destcidr') }}</div>
          <a-input v-model="newRule.destcidrlist"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('protocol') }}</div>
          <a-select v-model="newRule.protocol" style="width: 100%;" @change="resetRulePorts">
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
            <a-select-option value="icmp">ICMP</a-select-option>
            <a-select-option value="all">All</a-select-option>
          </a-select>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">{{ $t('startport') }}</div>
          <a-input v-model="newRule.startport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">{{ $t('endport') }}</div>
          <a-input v-model="newRule.endport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">{{ $t('icmptype') }}</div>
          <a-input v-model="newRule.icmptype"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">{{ $t('icmpcode') }}</div>
          <a-input v-model="newRule.icmpcode"></a-input>
        </div>
        <div class="form__item">
          <a-button type="primary" icon="plus" @click="addRule">{{ $t('add') }}</a-button>
        </div>
      </div>
    </div>

    <a-divider/>

    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="egressRules"
      :pagination="false"
      :rowKey="record => record.id">
      <template slot="protocol" slot-scope="record">
        {{ record.protocol | capitalise }}
      </template>
      <template slot="startport" slot-scope="record">
        {{ record.icmptype || record.startport >= 0 ? record.icmptype || record.startport : 'All' }}
      </template>
      <template slot="endport" slot-scope="record">
        {{ record.icmpcode || record.endport >= 0 ? record.icmpcode || record.endport : 'All' }}
      </template>
      <template slot="actions" slot-scope="record">
        <a-button shape="round" type="danger" icon="delete" @click="deleteRule(record)" />
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger/>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'EgressRulesTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: true,
      egressRules: [],
      newRule: {
        protocol: 'tcp',
        cidrlist: null,
        destcidrlist: null,
        networkid: this.resource.id,
        icmptype: null,
        icmpcode: null,
        startport: null,
        endport: null
      },
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('sourcecidr'),
          dataIndex: 'cidrlist'
        },
        {
          title: this.$t('destcidr'),
          dataIndex: 'destcidrlist'
        },
        {
          title: this.$t('protocol'),
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: `ICMP Type / Start Port`,
          scopedSlots: { customRender: 'startport' }
        },
        {
          title: `ICMP Code / End Port`,
          scopedSlots: { customRender: 'endport' }
        },
        {
          title: this.$t('action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  mounted () {
    this.fetchData()
  },
  filters: {
    capitalise: val => {
      if (val === 'all') return 'All'
      return val.toUpperCase()
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listEgressFirewallRules', {
        listAll: true,
        networkid: this.resource.id,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.egressRules = response.listegressfirewallrulesresponse.firewallrule || []
        this.totalCount = response.listegressfirewallrulesresponse.count || 0
      }).finally(() => {
        this.loading = false
      })
    },
    deleteRule (rule) {
      this.loading = true
      api('deleteEgressFirewallRule', { id: rule.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteegressfirewallruleresponse.jobid,
          successMessage: `Successfully removed Egress rule`,
          successMethod: () => this.fetchData(),
          errorMessage: 'Removing Egress rule failed',
          errorMethod: () => this.fetchData(),
          loadingMessage: `Deleting Egress rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => this.fetchData()
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.fetchData()
      })
    },
    addRule () {
      this.loading = true
      api('createEgressFirewallRule', { ...this.newRule }).then(response => {
        this.$pollJob({
          jobId: response.createegressfirewallruleresponse.jobid,
          successMessage: `Successfully added new Egress rule`,
          successMethod: () => {
            this.resetAllRules()
            this.fetchData()
          },
          errorMessage: 'Adding new Egress rule failed',
          errorMethod: () => {
            this.resetAllRules()
            this.fetchData()
          },
          loadingMessage: `Adding new Egress rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.resetAllRules()
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.createegressfirewallruleresponse.errortext
        })
        this.resetAllRules()
        this.fetchData()
      })
    },
    resetAllRules () {
      this.newRule.protocol = 'tcp'
      this.newRule.cidrlist = null
      this.newRule.destcidrlist = null
      this.newRule.networkid = this.resource.id
      this.resetRulePorts()
    },
    resetRulePorts () {
      this.newRule.icmptype = null
      this.newRule.icmpcode = null
      this.newRule.startport = null
      this.newRule.endport = null
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style scoped lang="scss">
  .rule {

    &-container {
      display: flex;
      width: 100%;
      flex-wrap: wrap;
      margin-right: -20px;
      margin-bottom: -10px;
    }

    &__item {
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        flex: 1;
      }

    }

    &__title {
      font-weight: bold;
    }

  }

  .add-btn {
    width: 100%;
    padding-top: 15px;
    padding-bottom: 15px;
    height: auto;
  }

  .add-actions {
    display: flex;
    justify-content: flex-end;
    margin-right: -20px;
    margin-bottom: 20px;

    @media (min-width: 760px) {
      margin-top: 20px;
    }

    button {
      margin-right: 20px;
    }

  }

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;
    align-items: flex-end;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__item {
      display: flex;
      flex-direction: column;
      flex: 1;
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        margin-bottom: 0;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

    }

    &__label {
      font-weight: bold;
    }

  }
  .pagination {
    margin-top: 20px;
  }
</style>
