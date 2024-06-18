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
      <a-alert type="warning">
        <template #message>{{ $t('message.action.start.instance') }}</template>
      </a-alert>
      <br />
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="id" ref="id">
          <template #label>
            <tooltip-label :title="$t('label.asnrange')" :tooltip="apiParams.id.description"/>
          </template>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @change="handleChangeDomain"
            v-focus="true"
            v-model:value="form.id">
            <a-select-option
              v-for="(asnRange, index) in asnRanges"
              :value="asnRange.id"
              :key="index"
              :label="asnRange.startasn - asnRange.endasn">
              {{ asnRange.startasn.toString() + '-' + asnRange.endasn.toString() }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'DeleteASNRangeForm',
  components: {
    TooltipLabel
  },
  inject: ['parentFetchData'],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      asnRanges: []
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('deleteASNRange')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        id: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.loading = true
      api('listASNRanges', { zoneid: this.resource.id }).then(json => {
        this.asnRanges = json?.listasnrangesresponse?.asnumberrange || []
      }).finally(() => { this.loading = false })
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        api('deleteASNRange', { id: values.id }).then(json => {
          const response = json.deleteasnrangeresponse.success
          if (response === true) {
            this.$notification.success({
              message: this.$t('label.delete.asnrange'),
              description: this.$t('message.delete.asn.range')
            })
          } else {
            this.$notification.error({
              message: this.$t('label.error'),
              description: this.$t('message.error.delete.asnrange')
            })
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        this.closeAction()
        this.parentFetchData()
      })
    }
  }
}
</script>
