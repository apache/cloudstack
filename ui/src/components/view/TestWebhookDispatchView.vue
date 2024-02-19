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
    <div>
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
                      shape="round"
                      :style="computedReloadStyle">
                      <render-icon icon="reload-outlined" />
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
  name: 'TestWebhookDispatchView',
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
      testDispatchInterval: null,
      testDispatchIntervalCouter: 100
    }
  },
  beforeUnmount () {
    if (this.testDispatchInterval) {
      clearInterval(this.testDispatchInterval)
      this.testDispatchIntervalCouter = 100
    }
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
    computedReloadStyle () {
      return 'opacity: ' + (this.testDispatchIntervalCouter / 100.0) + ';'
    }
  },
  methods: {
    testWebhookDispatch () {
      if (this.testDispatchInterval) {
        clearInterval(this.testDispatchInterval)
        this.testDispatchIntervalCouter = 100
      }
      this.response = {}
      this.loading = true
      this.$emit('change-loading', this.loading)
      var params = {}
      if (this.resource) {
        params.id = this.resource.id
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
      api('testWebhookDispatch', params).then(response => {
        this.response = response.testwebhookdispatchresponse.webhookdispatch
        this.$emit('update-success', response.success)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        this.$emit('change-loading', this.loading)
      })
    },
    timedTestWebhookDispatch () {
      const urlPattern = /^(http|https):\/\/[^ "]+$/
      clearTimeout(this.testDispatchInterval)
      this.testDispatchIntervalCouter = 100
      this.testDispatchInterval = setInterval(() => {
        if (!this.payloadUrl || !urlPattern.test(this.payloadUrl)) {
          clearInterval(this.testDispatchInterval)
          this.testDispatchIntervalCouter = 100
          return
        }
        this.testDispatchIntervalCouter = this.testDispatchIntervalCouter - 5
        if (this.testDispatchIntervalCouter <= 0) {
          if (this.payloadUrl && urlPattern.test(this.payloadUrl)) {
            this.testWebhookDispatch()
            return
          }
          clearInterval(this.testDispatchInterval)
          this.testDispatchIntervalCouter = 100
        }
      }, 250)
    }
  }
}
</script>

<style scoped lang="scss">

  .response-details {
    text-align: center;
    margin-bottom: 20px;

    &__name {
      display: flex;
      align-items: center;

      .avatar {
        margin-right: 20px;
        overflow: hidden;
        min-width: 50px;
        cursor: pointer;

        img {
          height: 100%;
          width: 100%;
        }
      }

      .name {
        margin-bottom: 0;
        font-size: 18px;
        line-height: 1;
        word-break: break-all;
        text-align: left;
      }

    }
  }
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
</style>
