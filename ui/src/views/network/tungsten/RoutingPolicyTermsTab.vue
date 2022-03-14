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
    <a-button
      :disabled="!('addTungstenFabricRoutingPolicyTerm' in $store.getters.apis)"
      type="dashed"
      style="width: 100%; margin-bottom: 15px"
      @click="onShowAction">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.routing.policy') }}
    </a-button>
    <a-table
      size="small"
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
    </a-table>
    <a-modal
      v-if="showAction"
      :visible="showAction"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      :title="$t('label.add.routing.policy')"
      @cancel="closeAction"
      style="top: 20px;"
      width="auto"
      centered>
      <div class="form" v-ctrl-enter="handleSubmit">
        <a-form class="form-content">
          <routing-policy-terms
            :formModel="formModel"
            @onChangeFields="onChangeFields"
            @showError="() => { showError = true }"
            @closeError="() => { showError = false }" />
        </a-form>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
    <a-modal
      :visible="showError"
      :title="`${$t('label.error')}!`"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showError = false }"
      v-ctrl-enter="() => { showError = false }"
      centered
    >
      <span>{{ $t('message.error.routing.policy.term') }}</span>
      <div :span="24" class="action-button">
        <a-button @click="showError = false">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="showError = false">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import RoutingPolicyTerms from '@/views/network/tungsten/RoutingPolicyTerms'

export default {
  name: 'RoutingPolicyTermsTab',
  components: { RoutingPolicyTerms },
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
      showAction: false,
      showError: false,
      formModel: {},
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'tungstenroutingpolicytermname'
      }],
      dataSource: [],
      prefixList: [],
      zoneId: null
    }
  },
  watch: {
    resource () {
      this.dataSource = this.resource?.tungstenroutingpolicyterm || []
    }
  },
  created () {
    this.zoneId = this.$route?.query?.zoneid || null
    this.dataSource = this.resource?.tungstenroutingpolicyterm || []
  },
  methods: {
    onShowAction () {
      this.showAction = true
    },
    onChangeFields (formModel, prefixList) {
      this.formModel = { ...this.formModel, ...formModel }
      this.prefixList = this.formModel?.prefixList || []
    },
    closeAction () {
      this.formModel = {}
      this.showAction = false
    },
    checkPolicyTermValues () {
      let valid = true
      if (this.prefixList) return valid
      this.prefixList.forEach(item => {
        if (!item.termvalue) {
          return true
        }

        if (!/^(\d+)([:])(\d+)$/.test(item.termvalue)) {
          valid = false
          return false
        }
      })

      return valid
    },
    async handleSubmit () {
      this.showError = false
      if (!this.checkPolicyTermValues()) {
        this.showError = true
        return
      }

      const params = {}
      params.zoneid = this.zoneId
      params.tungstenroutingpolicyuuid = this.resource.uuid
      params.tungstenroutingpolicyfromtermcommunities = this.formModel?.tungstenroutingpolicyfromtermcommunities?.join(',') || ''
      params.tungstenroutingpolicymatchall = this.formModel?.tungstenroutingpolicymatchall || false
      params.tungstenroutingpolicyprotocol = this.formModel?.tungstenroutingpolicyprotocol?.join(',') || ''
      params.tungstenroutingpolicyfromtermprefixlist = this.prefixList.length > 0 ? this.prefixList.map(item => [item.prefix, item.prefixtype].join('&')).join(',') : ''
      params.tungstenroutingpolicythentermlist = this.prefixList.length > 0 ? this.prefixList.map(item => [item.termtype, item.termvalue].join('&')).join(',') : ''

      const jobId = await this.addTungstenFabricRoutingPolicyTerm(params)
      await this.$pollJob({
        jobId,
        title: this.$t('label.add.routing.policy'),
        description: this.resource.name,
        catchMessage: this.$t('error.fetching.async.job.result'),
        successMessage: `${this.$t('message.success.add.tungsten.routing.policy')} ${this.resource.name}`
      })
      this.closeAction()
    },
    addTungstenFabricRoutingPolicyTerm (params) {
      return new Promise((resolve, reject) => {
        api('addTungstenFabricRoutingPolicyTerm', params).then(json => {
          const jobId = json?.addtungstenfabricroutingpolicytermresponse?.jobid
          resolve(jobId)
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>

<style scoped lang="less">
.form {
  width: 100%;

  @media (min-width: 1000px) {
    width: 700px;
  }

  .form-content {
    background-color: inherit;
    vertical-align: center;
    padding: 8px;
    padding-top: 16px;
    margin-top: 8px;
    overflow-y: auto;

    :deep(.has-error) {
      .ant-form-explain {
        text-align: left;
      }
    }

    :deep(.ant-form-item-control) {
      text-align: left;
    }
  }
}
</style>
