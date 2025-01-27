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
  <div class="form-layout">
    <div v-if="(resource || payloadUrl)">
      <a-divider />
      <a-collapse
        v-model:activeKey="collapseKey">
        <a-collapse-panel
          :showArrow="isResponseNotEmpty"
          key="1"
          :collapsible="(resource || payloadUrl) ? 'enabled' : 'disabled'">
          <template #header>
            <a-row style="width: 100%;">
              <a-col :span="22">
                <tooltip-label :title="$t('label.status')" v-if="isNotShowStatus"/>
                <status class="status" :text="response.success ? 'success' : 'error'" displayText v-else/>
              </a-col>
              <a-col :span="2">
                <div v-if="showActions">
                  <a-spin :spinning="loading" size="small" v-if="loading" />
                  <div v-else>
                    <a-button
                      type="primary"
                      size="small"
                      @click.stop="testWebhookDelivery">
                      <render-icon icon="reload-outlined" />
                      <div class="ant-btn__progress-overlay" :style="computedOverlayStyle"></div>
                    </a-button>
                  </div>
                </div>
              </a-col>
            </a-row>
          </template>
          <div v-if="isResponseNotEmpty">
            <a-row>
              <a-col :span="8">
                <div class="response-detail-item" v-if="('success' in response)">
                  <div class="response-detail-item__label"><strong>{{ $t('label.success') }}</strong></div>
                  <div class="response-detail-item__details">
                    <status class="status" :text="response.success ? 'success' : 'error'"/>
                  </div>
                </div>
              </a-col>
              <a-col :span="8">
                <div class="response-detail-item" v-if="response.startdate && response.enddate">
                  <div class="response-detail-item__label"><strong>{{ $t('label.duration') }}</strong></div>
                  <div class="response-detail-item__details">
                    {{ responseDuration }}
                  </div>
                </div>
              </a-col>
              <a-col :span="8">
                <div class="response-detail-item" v-if="response.managementserverid">
                  <div class="response-detail-item__label"><strong>{{ $t('label.managementserverid') }}</strong></div>
                  <div class="response-detail-item__details">
                    {{ response.managementservername }}
                  </div>
                </div>
              </a-col>
            </a-row>
            <a-alert
              :type="response.success ? 'success' : 'error'"
              :showIcon="true"
              :message="$t('label.response')"
              :description="response.response ? response.response : 'Empty response'" />
            </div>
        </a-collapse-panel>
      </a-collapse>
      <a-divider />
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import Status from '@/components/widgets/Status'

export default {
  name: 'TestWebhookDeliveryView',
  components: {
    TooltipLabel,
    Status
  },
  props: {
    resource: {
      type: Object
    },
    payload: {
      type: String
    },
    payloadUrl: {
      type: String
    },
    sslVerification: {
      type: Boolean,
      default: false
    },
    secretKey: {
      type: String
    },
    showActions: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      response: {},
      collapseKey: undefined,
      loading: false,
      testDeliveryInterval: null,
      testDeliveryIntervalCouter: 0
    }
  },
  beforeCreate () {
    this.timedDeliveryWait = 4000
  },
  beforeUnmount () {
    this.resetTestDeliveryInterval()
  },
  computed: {
    isResponseNotEmpty () {
      return this.response && Object.keys(this.response).length > 0
    },
    isNotShowStatus () {
      return !this.isResponseNotEmpty ||
        this.collapseKey === '1' ||
        (Array.isArray(this.collapseKey) &&
        this.collapseKey.length > 0 &&
        this.collapseKey[0] === '1')
    },
    responseDuration () {
      if (!this.response.startdate || !this.response.enddate) {
        return ''
      }
      var duration = Date.parse(this.response.enddate) - Date.parse(this.response.startdate)
      return (duration > 0 ? duration / 1000.0 : 0) + ''
    },
    computedOverlayStyle () {
      var opacity = this.testDeliveryIntervalCouter <= 10.0 ? 0 : 0.3
      var width = this.testDeliveryIntervalCouter
      return 'opacity: ' + opacity + '; width: ' + width + '%;'
    }
  },
  methods: {
    validateUrl (url) {
      const urlPattern = /^(http|https):\/\/[^ "]+$/
      urlPattern.test(url)
    },
    resetTestDeliveryInterval () {
      if (this.testDeliveryInterval) {
        clearInterval(this.testDeliveryInterval)
      }
      this.testDeliveryIntervalCouter = 0
    },
    testWebhookDelivery () {
      this.resetTestDeliveryInterval()
      this.response = {}
      this.loading = true
      this.$emit('change-loading', this.loading)
      var params = {}
      if (this.resource) {
        params.webhookid = this.resource.id
      }
      if (this.payload) {
        params.payload = this.payload
      }
      if (this.payloadUrl) {
        params.payloadUrl = this.payloadUrl
      }
      if (this.sslVerification) {
        params.payload = this.sslVerification
      }
      if (this.secretKey) {
        params.secretKey = this.secretKey
      }
      api('executeWebhookDelivery', params).then(response => {
        this.response = response.executewebhookdeliveryresponse.webhookdelivery
        this.$emit('update-success', response.success)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        this.$emit('change-loading', this.loading)
      })
    },
    getNormalizedPayloadUrl () {
      if (!this.payloadUrl || this.payloadUrl === '' || this.validateUrl(this.payloadUrl)) {
        return this.payloadUrl
      }
      return 'http://' + this.payloadUrl
    },
    executeTestWebhookDeliveryOrReset () {
      const url = this.getNormalizedPayloadUrl()
      if (url) {
        this.testWebhookDelivery()
        return
      }
      this.resetTestDeliveryInterval()
    },
    timedTestWebhookDelivery () {
      const url = this.getNormalizedPayloadUrl()
      this.resetTestDeliveryInterval()
      this.testDeliveryInterval = setInterval(() => {
        if (!url) {
          this.resetTestDeliveryInterval()
          return
        }
        this.testDeliveryIntervalCouter = this.testDeliveryIntervalCouter + 1
        if (this.testDeliveryIntervalCouter >= 100) {
          this.executeTestWebhookDeliveryOrReset()
        }
      }, this.timedDeliveryWait / 100)
    }
  }
}
</script>

<style scoped lang="scss">
  .response-detail-item {
    margin-bottom: 20px;
    word-break: break-all;

    &__details {
      display: flex;
      align-items: center;

      &--start {
        align-items: flex-start;

        i {
          margin-top: 4px;
        }

      }

    }

    .anticon {
      margin-right: 10px;
    }

    &__label {
      margin-bottom: 5px;
      font-weight: bold;
    }
  }

  .ant-btn .ant-btn__progress-overlay {
    position: absolute;
    width: 100%;
    height: 100%;
    z-index: 5;
    opacity: 0.3;
    transition: all 0s ease;
    position: absolute;
    left: 0;
    top: 0;
    background-color: #666;
  }
</style>
