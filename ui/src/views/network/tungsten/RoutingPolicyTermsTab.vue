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
        <a-steps
          ref="zoneStep"
          labelPlacement="vertical"
          size="small"
          :current="currentStep">
          <a-step
            v-for="(item, index) in steps"
            :key="item.name"
            :title="$t(item.title)"
            :ref="`step${index}`">
          </a-step>
        </a-steps>
        <a-form class="form-content">
          <div v-if="currentStep === 0">
            <routing-policy-terms
              :formModel="formModel"
              :errors="errors"
              @onChangeFields="onChangeFields" />
          </div>
          <div v-else-if="currentStep === 1">
            <routing-policy-terms
              :termThen="true"
              :formModel="formModel"
              :errors="errors"
              @onChangeFields="onChangeFields" />
          </div>
          <div v-else>
            <a-alert type="info" :message="$t('message.add.tungsten.routing.policy.available')"></a-alert>
          </div>
        </a-form>
        <div class="form-action" :span="24">
          <a-button @click="handleBack" class="button-back" v-if="currentStep > 0">
            {{ $t('label.previous') }}
          </a-button>
          <a-button ref="submit" type="primary" @click="handleNext" class="button-next">
            <poweroff-outlined v-if="currentStep === 2" />
            <span v-if="currentStep < 2">{{ $t('label.next') }}</span>
            <span v-else>{{ $t('label.create.routing.policy') }}</span>
          </a-button>
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
      centered
    >
      <div v-ctrl-enter="() => { showError = false }">
        <span>{{ errorMessage }}</span>
        <div :span="24" class="action-button">
          <a-button @click="showError = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="showError = false">{{ $t('label.ok') }}</a-button>
        </div>
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
      currentStep: 0,
      steps: [{
        name: 'routingpolicyterms',
        title: 'label.routing.policy.terms'
      }, {
        name: 'routingpolicytermsthen',
        title: 'label.routing.policy.terms.then'
      }, {
        name: 'finish',
        title: 'label.finish'
      }],
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'tungstenroutingpolicytermname'
      }],
      dataSource: [],
      prefixList: [],
      termsList: [],
      zoneId: null,
      errors: {
        prefix: [],
        termvalue: []
      },
      errorMessage: ''
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
    onChangeFields (formModel) {
      this.formModel = { ...this.formModel, ...formModel }
      this.prefixList = this.formModel?.prefixList || []
      this.termsList = this.formModel?.termsList || []
    },
    closeAction () {
      this.formModel = {}
      this.showAction = false
    },
    checkPolicyPrefix () {
      this.errors.prefix = []
      this.prefixList.forEach((item, index) => {
        if (!item.prefix) {
          this.errors.prefix.push(index)
        }
      })
    },
    checkPolicyTermValues () {
      let valid = true
      this.errors.termvalue = []
      this.termsList.forEach((item, index) => {
        if (!item.termvalue) {
          this.errors.termvalue.push(index)
          return true
        }

        if (item.termvalue && item.termtype === 'action') {
          return true
        }

        if (!/^(\d+)([:])(\d+)$/.test(item.termvalue)) {
          valid = false
        }
      })

      return valid
    },
    handleBack () {
      this.currentStep--
    },
    handleNext () {
      this.checkPolicyPrefix()
      const valid = this.checkPolicyTermValues()
      if (this.errors.prefix.length > 0 || this.errors.termvalue.length > 0) {
        this.showError = true
        this.errorMessage = this.$t('message.error.required.input')
        return
      }
      if (!valid) {
        this.showError = true
        this.errorMessage = this.$t('message.error.routing.policy.term')
        return
      }
      if (this.currentStep === 2) {
        return this.handleSubmit()
      }
      this.currentStep++
    },
    async handleSubmit () {
      const params = {}
      params.zoneid = this.zoneId
      params.tungstenroutingpolicyuuid = this.resource.uuid
      params.tungstenroutingpolicyfromtermcommunities = this.formModel?.tungstenroutingpolicyfromtermcommunities?.join(',') || ''
      params.tungstenroutingpolicymatchall = this.formModel?.tungstenroutingpolicymatchall || false
      params.tungstenroutingpolicyprotocol = this.formModel?.tungstenroutingpolicyprotocol?.join(',') || ''
      params.tungstenroutingpolicyfromtermprefixlist = this.prefixList.length > 0 ? this.prefixList.map(item => [item.prefix, item.prefixtype].join('&')).join(',') : ''
      params.tungstenroutingpolicythentermlist = this.termsList.length > 0 ? this.termsList.map(item => {
        if (item.termtype === 'action') {
          return [item.termtype, item.termvalue, ' '].join('&')
        } else {
          return [' ', item.termtype, item.termvalue].join('&')
        }
      }).join(',') : ''

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

  .form-action {
    position: relative;
    margin-top: 16px;
    height: 35px;
  }

  .button-next {
    position: absolute;
    right: 0;
  }
}
</style>
