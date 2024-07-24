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
  <span class="header-notice-opener" v-if="showSwitcher">
    <a-select
      class="domain-select"
      :loading="loading"
      :defaultValue="currentAccount"
      :value="currentAccount"
      showSearch
      optionFilterProp="label"
      :filterOption="(input, option) => {
        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
      }"
      @change="changeAccount"
      @focus="fetchData" >

      <template #suffixIcon>
        <a-tooltip placement="bottom">
          <template #title>
            <span>{{ $t('label.domain') }}</span>
          </template>
          <span class="custom-suffix-icon">
            <BlockOutlined v-if="!loading" class="ant-select-suffix" />
            <LoadingOutlined v-else />
          </span>
        </a-tooltip>
      </template>

      <a-select-option v-for="(account, index) in samlAccounts" :key="index" :label="`${account.accountName} (${account.domainName})`">
        {{ `${account.accountName} (${account.domainName})` }}
      </a-select-option>
    </a-select>
  </span>
</template>

<script>
import store from '@/store'
import { api } from '@/api'
import _ from 'lodash'

export default {
  name: 'SamlDomainSwitcher',
  data () {
    return {
      showSwitcher: false,
      samlAccounts: [],
      currentAccount: '',
      loading: false
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      var page = 1
      const samlAccounts = []
      const getNextPage = () => {
        this.loading = true
        api('listAndSwitchSamlAccount', { details: 'min', page: page, pageSize: 500 }).then(json => {
          if (json && json.listandswitchsamlaccountresponse && json.listandswitchsamlaccountresponse.samluseraccount) {
            samlAccounts.push(...json.listandswitchsamlaccountresponse.samluseraccount)
          }
          if (samlAccounts.length < json.listandswitchsamlaccountresponse.count) {
            page++
            getNextPage()
          }
        }).catch(error => {
          console.log(error)
        }).finally(() => {
          if (samlAccounts.length < 2) {
            this.showSwitcher = false
            return
          }
          this.samlAccounts = _.orderBy(samlAccounts, ['domainPath'], ['asc'])
          const currentAccount = this.samlAccounts.filter(x => {
            return x.userId === store.getters.userInfo.id
          })[0]
          this.currentAccount = `${currentAccount.accountName} (${currentAccount.domainName})`
          this.loading = false
          this.showSwitcher = true
        })
      }
      getNextPage()
    },
    changeAccount (index) {
      const account = this.samlAccounts[index]
      api('listAndSwitchSamlAccount', {}, 'POST', {
        userid: account.userId,
        domainid: account.domainId
      }).then(response => {
        store.dispatch('GetInfo', true).then(() => {
          this.$message.success(`Switched to "${account.accountName} (${account.domainPath})"`)
          this.$router.go()
        })
      })
    }
  }
}
</script>

<style lang="less" scoped>
.domain {
  &-select {
    width: 20vw;
  }

  &-icon {
    font-size: 20px;
    line-height: 1;
    padding-top: 5px;
    padding-right: 5px;
  }
}

.custom-suffix-icon {
  font-size: 20px;
  position: absolute;
  top: 0;
  right: 0;
  margin-top: -8px;
}
</style>
