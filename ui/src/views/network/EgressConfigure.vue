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
          <div class="form__label">Source CIDR</div>
          <a-input v-model="newRule.cidrlist"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">Destination CIDR</div>
          <a-input v-model="newRule.destcidrlist"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">Protocol</div>
          <a-select v-model="newRule.protocol" style="width: 100%;" @change="resetRulePorts">
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
            <a-select-option value="icmp">ICMP</a-select-option>
            <a-select-option value="all">All</a-select-option>
          </a-select>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">Start Port</div>
          <a-input v-model="newRule.startport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">End Port</div>
          <a-input v-model="newRule.endport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">ICMP Type</div>
          <a-input v-model="newRule.icmptype"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">ICMP Code</div>
          <a-input v-model="newRule.icmpcode"></a-input>
        </div>
        <div class="form__item">
          <a-button type="primary" icon="plus" @click="addRule">{{ $t('add') }}</a-button>
        </div>
      </div>
    </div>

    <a-divider/>

    <a-list :loading="loading" style="min-height: 25px;">
      <a-list-item v-for="rule in egressRules" :key="rule.id" class="rule">
        <div class="rule-container">
          <div class="rule__item">
            <div class="rule__title">Source CIDR</div>
            <div>{{ rule.cidrlist }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">Destination CIDR</div>
            <div>{{ rule.destcidrlist }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">Protocol</div>
            <div>{{ rule.protocol | capitalise }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">{{ rule.protocol === 'icmp' ? 'ICMP Type' : 'Start Port' }}</div>
            <div>{{ rule.icmptype || rule.startport >= 0 ? rule.icmptype || rule.startport : 'All' }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">{{ rule.protocol === 'icmp' ? 'ICMP Code' : 'End Port' }}</div>
            <div>{{ rule.icmpcode || rule.endport >= 0 ? rule.icmpcode || rule.endport : 'All' }}</div>
          </div>
          <div slot="actions">
            <a-button shape="round" type="danger" icon="delete" @click="deleteRule(rule)" />
          </div>
        </div>
      </a-list-item>
    </a-list>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
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
      }
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
        networkid: this.resource.id
      }).then(response => {
        this.egressRules = response.listegressfirewallrulesresponse.firewallrule
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
</style>
