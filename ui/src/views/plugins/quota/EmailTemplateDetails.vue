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
  <a-spin :spinning="loading || loading">
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-form-item :label="$t('label.templatesubject')">
          <a-textarea v-model="formModel.templatesubject" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-form-item :label="$t('label.templatebody')">
          <a-textarea v-model="formModel.templatebody" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-form-item :label="$t('label.last.updated')">
          <label>{{ resource.last_updated }}</label>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-button
          style="float: right; margin-left: 10px;"
          :disabled="!('quotaEmailTemplateUpdate' in $store.getters.apis)"
          :loading="loading"
          type="primary"
          @click="handleSubmit">{{ $t('label.apply') }}</a-button>
        <a-button
          style="float: right;"
          :disabled="!('quotaEmailTemplateUpdate' in $store.getters.apis)"
          :loading="loading"
          type="default"
          @click="() => { $router.go(-1) }">{{ $t('label.cancel') }}</a-button>
      </a-col>
    </a-row>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'EmailTemplateDetails',
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  data () {
    return {
      resource: {},
      formModel: {
        templatesubject: null,
        templatebody: null
      },
      loading: false
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      const params = {}
      params.templatetype = this.$route.params.id

      api('quotaEmailTemplateList', params).then(json => {
        const listTemplates = json.quotaemailtemplatelistresponse.quotaemailtemplate || []
        this.resource = listTemplates && listTemplates.length > 0 ? listTemplates[0] : {}
        this.preFillDataValues()
      }).catch(e => {
        this.$notifyError(e)
      }).finally(() => {
        this.loading = false
      })
    },
    preFillDataValues () {
      console.log(this.resource)
      this.formModel.templatesubject = this.resource.templatesubject || null
      this.formModel.templatebody = this.resource.templatebody || null
    },
    handleSubmit () {
      const params = {}
      params.templatesubject = this.formModel.templatesubject
      params.templatebody = this.formModel.templatebody
      params.templatetype = this.resource.templatetype

      this.loading = true

      api('quotaEmailTemplateUpdate', params).then(json => {
        this.$message.success(this.$t('label.quota.email.edit') + ' - ' + this.resource.templatetype)
        this.$router.go(-1)
      }).catch(e => {
        this.$notifyError(e)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
