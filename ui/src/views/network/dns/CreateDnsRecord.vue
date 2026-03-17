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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical">

        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label
              :title="$t('label.name')"
              :tooltip="$t('label.dns.record.name.tooltip')" />
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="$t('placeholder.dns.record.name')"
            v-focus="true" />
        </a-form-item>

        <a-form-item name="type" ref="type">
          <template #label>
            <tooltip-label
              :title="$t('label.type')"
              :tooltip="$t('label.dns.record.type.tooltip')" />
          </template>
          <a-select
            v-model:value="form.type"
            :placeholder="$t('placeholder.dns.record.type')"
            showSearch>
            <a-select-option
              v-for="rtype in recordTypes"
              :key="rtype"
              :value="rtype">
              {{ rtype }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="contents" ref="contents">
          <template #label>
            <tooltip-label
              :title="$t('label.contents')"
              :tooltip="$t('label.dns.record.contents.tooltip')" />
          </template>
          <a-select
            v-model:value="form.contents"
            mode="tags"
            :placeholder="$t('placeholder.dns.record.contents')"
            style="width: 100%"
            :token-separators="[',']" />
        </a-form-item>

        <a-form-item name="ttl" ref="ttl">
          <template #label>
            <tooltip-label
              :title="$t('label.ttl')"
              :tooltip="$t('label.dns.record.ttl.tooltip')" />
          </template>
          <a-input-number
            v-model:value="form.ttl"
            :min="1"
            :max="2147483647"
            style="width: 100%" />
        </a-form-item>

        <div class="action-button">
          <a-button @click="closeAction">
            {{ $t('label.cancel') }}
          </a-button>
          <a-button
            type="primary"
            :loading="loading"
            @click="handleSubmit">
            {{ $t('label.ok') }}
          </a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateDnsRecord',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      form: {
        name: '',
        type: 'A',
        contents: [],
        ttl: 3600
      },
      rules: {},
      recordTypes: ['A', 'AAAA', 'CNAME', 'MX', 'TXT', 'SRV', 'PTR', 'NS']
    }
  },
  created () {
    this.rules = {
      name: [{ required: true, message: this.$t('message.error.required.input') }],
      type: [{ required: true, message: this.$t('message.error.required.input') }],
      contents: [{ required: true, type: 'array', min: 1, message: this.$t('message.error.required.input') }],
      ttl: [{ required: true, message: this.$t('message.error.required.input') }]
    }
  },
  methods: {
    async handleSubmit () {
      if (this.loading) return
      try {
        await this.$refs.formRef.validate()
      } catch (error) {
        if (error.errorFields && error.errorFields.length > 0) {
          this.$refs.formRef.scrollToField(error.errorFields[0].name)
        }
        return
      }
      this.loading = true
      try {
        const params = {
          dnszoneid: this.resource.id,
          name: this.form.name,
          type: this.form.type,
          contents: this.form.contents.join(','),
          ttl: this.form.ttl
        }
        const response = await postAPI('createDnsRecord', params)
        const jobId = response.creatednsrecordresponse.jobid
        if (!jobId) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: 'Failed to get jobid for createDnsRecord',
            duration: 0
          })
        }
        await this.$pollJob({
          jobId: jobId,
          title: this.$t('label.dns.create.record'),
          description: this.$t('label.creating.dns.record'),
          successMethod: () => {
            this.$notification.success({
              message: this.$t('label.dns.create.record'),
              description: this.$t('message.success.create.dns.record')
            })
          },
          loadingMessage: `${this.$t('label.dns.create.record')} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: { isFetchData: false }
        })
        this.$emit('refresh-data')
        this.closeAction()
      } catch (error) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: error?.response?.headers['x-description'] || error.message,
          duration: 0
        })
      } finally {
        this.loading = false
      }
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 600px) {
    width: 450px;
  }
}

.action-button {
  text-align: right;
  margin-top: 20px;
}
.action-button button {
  margin-left: 8px;
}
</style>
