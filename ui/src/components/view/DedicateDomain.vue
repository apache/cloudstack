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
      <p class="form__label">{{ $t('label.domain') }}<span class="required">*</span></p>
      <p class="required required-label">{{ $t('label.required') }}</p>
      <infinite-scroll-select
        style="width: 100%"
        v-model:value="domainId"
        api="listDomains"
        :apiParams="domainsApiParams"
        resourceType="domain"
        optionValueKey="id"
        optionLabelKey="path"
        defaultIcon="block-outlined"
        v-focus="true"
        @change-option-value="handleChangeDomain" />
    </div>
    <div class="form__item">
      <p class="form__label">{{ $t('label.account') }}</p>
      <infinite-scroll-select
        style="width: 100%"
        v-model:value="selectedAccount"
        api="listAccounts"
        :apiParams="accountsApiParams"
        resourceType="account"
        optionValueKey="name"
        optionLabelKey="name"
        defaultIcon="team-outlined"
        @change-option-value="handleChangeAccount" />
    </div>
  </div>
</template>

<script>
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect.vue'

export default {
  name: 'DedicateDomain',
  components: {
    InfiniteScrollSelect
  },
  props: {
    error: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      domainId: null,
      selectedAccount: null,
      domainError: false
    }
  },
  computed: {
    domainsApiParams () {
      return {
        listall: true,
        details: 'min'
      }
    },
    accountsApiParams () {
      if (!this.domainId) {
        return {
          listall: true,
          showicon: true
        }
      }
      return {
        showicon: true,
        domainid: this.domainId
      }
    }
  },
  watch: {
    error () {
      this.domainError = this.error
    }
  },
  created () {
  },
  methods: {
    handleChangeDomain (domainId) {
      this.domainId = domainId
      this.selectedAccount = null
      this.$emit('domainChange', domainId)
      this.domainError = false
    },
    handleChangeAccount (accountName) {
      this.selectedAccount = accountName
      this.$emit('accountChange', accountName)
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
