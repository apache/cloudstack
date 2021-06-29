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
    <a-spin :spinning="loading" v-if="!isSubmitted">
      <p v-html="$t('message.desc.create.ssh.key.pair')"></p>
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('label.name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.name') }]
            }]"
            :placeholder="apiParams.name.description"
            autoFocus />
        </a-form-item>
        <a-form-item :label="$t('label.publickey')">
          <a-input
            v-decorator="['publickey', {}]"
            :placeholder="apiParams.publickey.description"/>
        </a-form-item>
        <a-form-item :label="$t('label.domainid')" v-if="this.isAdminOrDomainAdmin()">
          <a-select
            id="domain-selection"
            v-decorator="['domainid', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description"
            @change="val => { this.handleDomainChanged(this.domains[val]) }">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.account')" v-if="this.isAdminOrDomainAdmin() && !this.isObjectEmpty(this.selectedDomain) && this.selectedDomain.id !== null">
          <a-input
            v-decorator="['account', {}]"
            :placeholder="apiParams.account.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
    <div v-if="isSubmitted">
      <p v-html="$t('message.desc.created.ssh.key.pair')"></p>
      <div :span="24" class="action-button">
        <a-button @click="notifyCopied" v-clipboard:copy="hiddenElement.innerHTML" type="primary">{{ $t('label.copy.clipboard') }}</a-button>
        <a-button @click="downloadKey" type="primary">{{ this.$t('label.download') }}</a-button>
        <a-button @click="closeAction">{{ this.$t('label.close') }}</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'CreateSSHKeyPair',
  props: {},
  data () {
    return {
      domains: [],
      domainLoading: false,
      selectedDomain: {},
      loading: false,
      isSubmitted: false,
      hiddenElement: null
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.createSSHKeyPair || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
    this.apiConfig = this.$store.getters.apis.registerSSHKeyPair || {}
    this.apiConfig.params.forEach(param => {
      if (!(param.name in this.apiParams)) {
        this.apiParams[param.name] = param
      }
    })
  },
  created () {
    this.domains = [
      {
        id: null,
        name: ''
      }
    ]
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (this.isAdminOrDomainAdmin()) {
        this.fetchDomainData()
      }
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    fetchDomainData () {
      const params = {}
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listdomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listdomains)
      }).finally(() => {
        this.domainLoading = false
        if (this.arrayHasItems(this.domains)) {
          this.form.setFieldsValue({
            domainid: 0
          })
          this.handleDomainChanged(this.domains[0])
        }
      })
    },
    handleDomainChanged (domain) {
      this.selectedDomain = domain
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {
          name: values.name
        }
        if (this.isValidValueForKey(values, 'domainid') &&
          this.arrayHasItems(this.domains) &&
          this.domains[values.domainid].id !== null) {
          params.domainid = this.domains[values.domainid].id
        }
        if (this.isValidValueForKey(values, 'account') && values.account.length > 0) {
          params.account = values.account
        }
        if (this.isValidValueForKey(values, 'publickey') && values.publickey.length > 0) {
          params.publickey = values.publickey
          api('registerSSHKeyPair', params).then(json => {
            this.$message.success(this.$t('message.success.register.keypair') + ' ' + values.name)
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.$emit('refresh-data')
            this.loading = false
            this.closeAction()
          })
        } else {
          api('createSSHKeyPair', params).then(json => {
            this.$message.success(this.$t('message.success.create.keypair') + ' ' + values.name)
            if (json.createsshkeypairresponse && json.createsshkeypairresponse.keypair && json.createsshkeypairresponse.keypair.privatekey) {
              this.isSubmitted = true
              const key = json.createsshkeypairresponse.keypair.privatekey
              this.hiddenElement = document.createElement('a')
              this.hiddenElement.href = 'data:text/plain;charset=utf-8,' + encodeURI(key)
              this.hiddenElement.innerHTML = key
              this.hiddenElement.target = '_blank'
              this.hiddenElement.download = values.name + '.key'
            }
          }).catch(error => {
            this.$notifyError(error)
          }).finally(() => {
            this.$emit('refresh-data')
            this.loading = false
          })
        }
      })
    },
    downloadKey () {
      this.hiddenElement.click()
    },
    notifyCopied () {
      this.$notification.info({
        message: this.$t('message.success.copy.clipboard')
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 600px) {
      width: 450px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
