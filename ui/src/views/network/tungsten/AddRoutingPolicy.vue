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
  <div class="form">
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
    <a-form :ref="formRef" :model="form" :rules="rules" class="form-content" >
      <div v-if="currentStep === 0">
        <a-form-item name="name" ref="name" :label="$t('label.name')" v-bind="formItemLayout">
          <a-input
            v-model:value="form.name"
            @change="(e) => changeFieldValue('name', e.target.value)"/>
        </a-form-item>
      </div>
      <div v-else-if="currentStep === 1">
        <routing-policy-terms :formModel="formModel" @onChangeFields="onChangeFields" />
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
        <span>{{ $t('message.error.routing.policy.term') }}</span>
        <div :span="24" class="action-button">
          <a-button @click="showError = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="showError = false">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { api } from '@/api'
import RoutingPolicyTerms from '@/views/network/tungsten/RoutingPolicyTerms'

export default {
  name: 'AddRoutingPolicy',
  components: { RoutingPolicyTerms },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    action: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      actionLoading: false,
      formItemLayout: {
        labelCol: { span: 8 },
        wrapperCol: { span: 12 }
      },
      formModel: {},
      currentStep: 0,
      steps: [{
        name: 'routingpolicy',
        title: 'label.routing.policy'
      }, {
        name: 'routingpolicyterms',
        title: 'label.routing.policy.terms'
      }, {
        name: 'finish',
        title: 'label.finish'
      }],
      prefixList: [],
      showError: false
    }
  },
  created () {
    this.initForm()
  },
  inject: ['onFetchData'],
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    handleBack () {
      this.currentStep--
    },
    handleNext () {
      this.formRef.value.validate().then(() => {
        if (this.currentStep === 1) {
          const valid = this.checkPolicyTermValues()
          if (!valid) {
            this.showError = true
            return
          }
        }
        if (this.currentStep === 2) {
          return this.handleSubmit()
        }
        this.currentStep++
      })
    },
    changeFieldValue (field, value) {
      this.formModel[field] = value
    },
    onChangeFields (formModel) {
      this.formModel = { ...this.formModel, ...formModel }
      this.prefixList = this.formModel?.prefixList || []
    },
    checkPolicyTermValues () {
      let valid = true
      this.prefixList.forEach(item => {
        if (!item.termvalue || item.termtype === 'action') {
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
      if (this.actionLoading) return
      const params = {}
      params.zoneid = this.resource.zoneid
      params.name = this.formModel.name

      this.actionLoading = true
      try {
        const routingPolicy = await this.createTungstenFabricRoutingPolicy(params)
        const routingPolicyTermParams = {}
        routingPolicyTermParams.zoneid = this.resource.zoneid
        routingPolicyTermParams.tungstenroutingpolicyuuid = routingPolicy.uuid
        routingPolicyTermParams.tungstenroutingpolicyfromtermcommunities = this.formModel?.tungstenroutingpolicyfromtermcommunities?.join(',') || ''
        routingPolicyTermParams.tungstenroutingpolicymatchall = this.formModel?.tungstenroutingpolicymatchall || false
        routingPolicyTermParams.tungstenroutingpolicyprotocol = this.formModel?.tungstenroutingpolicyprotocol?.join(',') || ''
        routingPolicyTermParams.tungstenroutingpolicyfromtermprefixlist = this.prefixList?.map(item => [item.prefix, item.prefixtype].join('&')).join(',') || ''
        routingPolicyTermParams.tungstenroutingpolicythentermlist = this.prefixList.map(item => {
          if (item.termtype === 'action') {
            return [item.termvalue, item.termtype, ' '].join('&')
          } else {
            return [' ', item.termtype, item.termvalue].join('&')
          }
        }).join(',') || ''
        const jobId = await this.addTungstenFabricRoutingPolicyTerm(routingPolicyTermParams)
        await this.$pollJob({
          jobId,
          title: this.$t('label.create.tungsten.routing.policy'),
          description: params.name,
          catchMessage: this.$t('error.fetching.async.job.result'),
          successMessage: `${this.$t('message.success.add.tungsten.routing.policy')} ${params.name}`,
          successMethod: () => {
            this.onFetchData()
          },
          errorMethod: () => {
            this.onFetchData()
          },
          catchMethod: () => {
            this.onFetchData()
          },
          action: {
            isFetchData: false
          }
        })
        this.$emit('close-action')
        this.actionLoading = false
      } catch (error) {
        this.$notifyError(error)
        this.$emit('close-action')
        this.onFetchData()
        this.actionLoading = false
      }
    },
    createTungstenFabricRoutingPolicy (params) {
      return new Promise((resolve, reject) => {
        api('createTungstenFabricRoutingPolicy', params).then(json => {
          const routingPolicy = json?.createtungstenfabricroutingpolicyresponse?.routingpolicy
          resolve(routingPolicy)
        }).catch(error => {
          reject(error)
        })
      })
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

<style  lang="less">
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
