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
  <div class="form-layout" v-ctrl-enter="submitForm">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        class="form"
        layout="vertical">
        <a-form-item name="payload" ref="payload">
          <template #label>
            <tooltip-label :title="$t('label.payload')" :tooltip="apiParams.payload.description"/>
          </template>
            <a-textarea
                :rows="3"
                v-model:value="form.payload"
                :placeholder="apiParams.payload.description" />
        </a-form-item>
        <test-webhook-delivery-view
          ref="dispatchview"
          :resource="resource"
          :payload="form.payload"
          @change-loading="updateLoading" />
        <a-divider />

        <div class="actions">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" :disabled="loading" @click="submitForm">{{ $t('label.test') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import Status from '@/components/widgets/Status'
import TestWebhookDeliveryView from '@/components/view/TestWebhookDeliveryView'

export default {
  name: 'TestWebhookDelivery',
  mixins: [mixinForm],
  components: {
    Status,
    TooltipLabel,
    TestWebhookDeliveryView
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      response: {},
      loading: false
    }
  },
  watch: {
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('executeWebhookDelivery')
  },
  created () {
    this.initForm()
  },
  computed: {
    responseDuration () {
      if (!this.response.startdate || !this.response.enddate) {
        return ''
      }
      var start = Date.parse(this.response.startdate)
      var end = Date.parse(this.response.enddate)
      return (end - start) + ''
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
    },
    closeModal () {
      this.$emit('close-action')
    },
    submitForm (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        this.$refs.dispatchview.testWebhookDelivery()
      })
    },
    updateLoading (value) {
      this.loading = value
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
    width: 80vw;

    @media (min-width: 600px) {
      max-width: 550px;
    }
  }

  .top-spaced {
    margin-top: 20px;
  }

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

  .actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;

    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }
</style>
