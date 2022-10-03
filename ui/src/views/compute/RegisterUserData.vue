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
      <p v-html="$t('message.desc.register.user.data')"></p>
      <a-form
        v-ctrl-enter="handleSubmit"
        :form="form"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.name.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.userdata')" :tooltip="apiParams.userdata.description"/>
          <a-textarea
            v-decorator="['userdata']"
            :placeholder="apiParams.userdata.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.userdataparams')" :tooltip="apiParams.params.description"/>
          <a-select
            mode="tags"
            v-decorator="['params']"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.params.description">
            <a-select-option v-for="opt in params" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="isAdminOrDomainAdmin()">
          <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          <a-select
            id="domain-selection"
            v-decorator="['domainid']"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description"
            @change="val => { handleDomainChanged(domains[val]) }">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="isAdminOrDomainAdmin() && !isObjectEmpty(selectedDomain) && selectedDomain.id !== null">
          <tooltip-label :slot="label" :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          <a-input
            v-decorator="['account']"
            :placeholder="apiParams.account.description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
    <div v-if="isSubmitted">
      <p v-html="$t('message.desc.registered.user.data')"></p>
      <div :span="24" class="action-button">
        <a-button @click="notifyCopied" v-clipboard:copy="hiddenElement.innerHTML" type="primary">{{ $t('label.copy.clipboard') }}</a-button>
        <a-button @click="downloadKey" type="primary">{{ $t('label.download') }}</a-button>
        <a-button @click="closeAction">{{ $t('label.close') }}</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'RegisterUserData',
  props: {},
  components: {
    TooltipLabel
  },
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
    this.apiParams = this.$getApiParams('registerUserData')
    this.form = this.$form.createForm(this)
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
          this.form.domainid = 0
          this.handleDomainChanged(this.domains[0])
        }
      })
    },
    handleDomainChanged (domain) {
      this.selectedDomain = domain
    },
    sanitizeReverse (value) {
      const reversedValue = value
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')

      return reversedValue
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFieldsAndScroll((err, values) => {
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
        params.userdata = encodeURIComponent(btoa(this.sanitizeReverse(values.userdata)))

        if (values.params != null && values.params.length > 0) {
          var userdataparams = values.params.join(',')
          params.params = userdataparams
        }

        api('registerUserData', params).then(json => {
          this.$message.success(this.$t('message.success.register.user.data') + ' ' + values.name)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.$emit('refresh-data')
          this.loading = false
          this.closeAction()
        })
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
