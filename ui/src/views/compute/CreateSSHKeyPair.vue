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
        v-ctrl-enter="handleSubmit"
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label :title="apiParams.name.description">
            {{ $t('label.name') }}
            <a-tooltip>
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="publickey" ref="publickey">
          <template #label :title="apiParams.publickey.description">
            {{ $t('label.publickey') }}
            <a-tooltip>
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.publickey"
            :placeholder="apiParams.publickey.description"/>
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="isAdminOrDomainAdmin()">
          <template #label :title="apiParams.domainid.description">
            {{ $t('label.domainid') }}
            <a-tooltip>
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="domain-selection"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description"
            @change="val => { handleDomainChanged(domains[val]) }">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label=" opt.path || opt.name || opt.description || ''">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="account" ref="account" v-if="isAdminOrDomainAdmin() && !isObjectEmpty(selectedDomain) && selectedDomain.id !== null">
          <template #label :title="apiParams.account.description">
            {{ $t('label.account') }}
            <a-tooltip>
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.account"
            :placeholder="apiParams.account.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
    <div v-if="isSubmitted">
      <p v-html="$t('message.desc.created.ssh.key.pair')"></p>
      <div :span="24" class="action-button">
        <a-button @click="notifyCopied" v-clipboard:copy="hiddenElement.innerHTML" type="primary">{{ $t('label.copy.clipboard') }}</a-button>
        <a-button @click="downloadKey" type="primary">{{ $t('label.download') }}</a-button>
        <a-button @click="closeAction">{{ $t('label.close') }}</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'

export default {
  name: 'CreateSSHKeyPair',
  mixins: [mixinForm],
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
    this.apiParams = this.$getApiParams('createSSHKeyPair', 'registerSSHKeyPair')
  },
  created () {
    this.initForm()
    this.domains = [
      {
        id: null,
        name: ''
      }
    ]
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }]
      })
    },
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
          this.form.domainid = 0
          this.handleDomainChanged(this.domains[0])
        }
      })
    },
    handleDomainChanged (domain) {
      this.selectedDomain = domain
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
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
            if (json.createsshkeypairresponse?.keypair?.privatekey) {
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
</style>
