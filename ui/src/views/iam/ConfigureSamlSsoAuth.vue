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
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      :loading="loading"
      @finish="handleSubmit">
      <a-form-item name="samlEnable" ref="samlEnable" :label="$t('label.samlenable')">
        <a-switch
          v-model:checked="form.samlEnable"
          v-focus="true"
        />
      </a-form-item>
      <a-form-item name="samlEntity" ref="samlEntity" :label="$t('label.samlentity')">
        <a-select
          v-model:value="form.samlEntity"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="idp in idps" :key="idp.id" :label="idp.orgName">
            {{ idp.orgName }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div class="action-button">
        <a-button @click="handleClose">{{ $t('label.close') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'ConfigureSamlSsoAuth',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      idps: [],
      loading: false
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    fetchData () {
      this.IsUserSamlAuthorized()
      this.loading = true
      api('listIdps').then(response => {
        this.idps = response.listidpsresponse.idp || []
      }).finally(() => {
        this.loading = false
      })
    },
    IsUserSamlAuthorized () {
      api('listSamlAuthorization', {
        userid: this.resource.id
      }).then(response => {
        this.form.samlEnable = response.listsamlauthorizationsresponse.samlauthorization[0].status || false
        this.form.samlEntity = response.listsamlauthorizationsresponse.samlauthorization[0].idpid || ''
      })
    },
    handleClose () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        api('authorizeSamlSso', {
          enable: values.samlEnable,
          userid: this.resource.id,
          entityid: values.samlEntity
        }).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: values.samlEnable ? this.$t('label.saml.enable') : this.$t('label.saml.disable'),
            description: values.samlEnable ? `${this.$t('message.success.enable.saml.auth')} ${this.$t('label.for')} ${this.resource.username}`
              : `${this.$t('message.success.disable.saml.auth')} ${this.$t('label.for')} ${this.resource.username}`
          })
          this.handleClose()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>
<style scoped lang="less">
.form-layout {
  width: 75vw;

  @media (min-width: 700px) {
    width: 40vw;
  }
}
</style>
