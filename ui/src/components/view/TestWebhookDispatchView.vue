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
    <div v-if="response.id">
      <a-divider />
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
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'TestWebhookDispatchView',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object
    },
    payload: {
      type: String
    },
    payloadurl: {
      type: String
    },
    sslverification: {
      type: Boolean,
      default: false
    },
    secretkey: {
      type: String
    }
  },
  data () {
    return {
      response: {}
    }
  },
  computed: {
    responseDuration () {
      if (!this.response.startdate || !this.response.enddate) {
        return ''
      }
      var duration = Date.parse(this.response.enddate) - Date.parse(this.response.startdate)
      return (duration > 0 ? duration / 1000.0 : 0) + ''
    }
  },
  methods: {
    testWebhookDispatch () {
      this.$emit('change-loading', true)
      var params = {
        id: this.resource.id
      }
      if (this.payload) {
        params.payload = this.payload
      }
      if (this.payloadurl) {
        params.payloadurl = this.payloadurl
      }
      if (this.ssllverification) {
        params.payload = this.ssllverification
      }
      if (this.secretkey) {
        params.secretkey = this.secretkey
      }
      api('testWebhookDispatch', params).then(response => {
        this.response = response.testwebhookdispatchresponse.webhookdispatch
        this.$emit('update-success', response.success)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        this.$emit('change-loading', false)
      })
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
