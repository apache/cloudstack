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
    <a-form :form="form" @submit="handleSubmit" layout="vertical" :loading="loading">
      <a-form-item :label="$t('label.samlenable')">
        <a-switch
          v-decorator="['samlEnable', {
            initialValue: isSamlEnabled
          }]"
          :checked="isSamlEnabled"
          @change="val => { isSamlEnabled = val }"
          autoFocus
        />
      </a-form-item>
      <a-form-item :label="$t('label.samlentity')">
        <a-select
          v-decorator="['samlEntity', {
            initialValue: selectedIdp,
          }]">
          <a-select-option v-for="(idp, idx) in idps" :key="idx">
            {{ idp.orgName }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div class="card-footer">
        <a-button @click="handleClose">{{ $t('label.close') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>
<script>
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
      selectedIdp: '',
      idps: [],
      isSamlEnabled: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  methods: {
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
        this.isSamlEnabled = response.listsamlauthorizationsresponse.samlauthorization[0].status || false
        this.selectedIdp = response.listsamlauthorizationsresponse.samlauthorization[0].idpid || ''
      })
    },
    handleClose () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        api('authorizeSamlSso', {
          enable: values.samlEnable,
          userid: this.resource.id,
          entityid: values.samlEntity
        }).then(response => {
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
.card-footer {
  text-align: right;

  button + button {
    margin-left: 8px;
  }
}
</style>
