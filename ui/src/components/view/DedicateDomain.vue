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
  <div class="form">
    <div class="form__item" :class="{'error': domainError}">
      <a-spin :spinning="domainsLoading">
        <p class="form__label">{{ $t('label.domain') }}<span class="required">*</span></p>
        <p class="required required-label">{{ $t('label.required') }}</p>
        <a-select
          style="width: 100%"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          @change="handleChangeDomain"
          v-focus="true"
          v-model:value="domainId">
          <a-select-option
            v-for="(domain, index) in domainsList"
            :value="domain.id"
            :key="index"
            :label="domain.path || domain.name || domain.description">
            {{ domain.path || domain.name || domain.description }}
          </a-select-option>
        </a-select>
      </a-spin>
    </div>
    <div class="form__item" v-if="accountsList">
      <p class="form__label">{{ $t('label.account') }}</p>
      <a-select
        style="width: 100%"
        @change="handleChangeAccount"
        showSearch
        optionFilterProp="value"
        :filterOption="(input, option) => {
          return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }" >
        <a-select-option v-for="(account, index) in accountsList" :value="account.name" :key="index">
          {{ account.name }}
        </a-select-option>
      </a-select>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'DedicateDomain',
  props: {
    error: {
      type: Boolean,
      requried: true
    }
  },
  data () {
    return {
      domainsLoading: false,
      domainId: null,
      accountsList: null,
      domainsList: null,
      domainError: false
    }
  },
  watch: {
    error () {
      this.domainError = this.error
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.domainsLoading = true
      api('listDomains', {
        listAll: true,
        details: 'min'
      }).then(response => {
        this.domainsList = response.listdomainsresponse.domain

        if (this.domainsList[0]) {
          this.domainId = this.domainsList[0].id
          this.handleChangeDomain(this.domainId)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    fetchAccounts () {
      api('listAccounts', {
        domainid: this.domainId
      }).then(response => {
        this.accountsList = response.listaccountsresponse.account || []
        if (this.accountsList && this.accountsList.length === 0) {
          this.handleChangeAccount(null)
        }
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    handleChangeDomain (e) {
      this.$emit('domainChange', e)
      this.domainError = false
      this.fetchAccounts()
    },
    handleChangeAccount (e) {
      this.$emit('accountChange', e)
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
    &__item {
      margin-bottom: 20px;
    }

    &__label {
      font-weight: bold;
      margin-bottom: 5px;
    }
  }

  .required {
    color: #ff0000;
    font-size: 12px;

    &-label {
      display: none;
    }
  }

  .error {
    .required-label {
      display: block;
    }
  }
</style>
