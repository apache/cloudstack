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
  <a-form class="form" v-ctrl-enter="handleSubmit">

    <p v-html="$t('message.select.bgp.peers')" />

    <div v-if="loading" class="loading">
      <loading-outlined style="color: #1890ff;" />
    </div>

    <div class="form__item">
      <a-input-search
        style="margin-bottom: 10px;"
        :placeholder="$t('label.search')"
        v-model:value="filter"
        @search="handleSearch"
        v-focus="true" />
    </div>

    <div class="form__item">
      <a-table
        size="small"
        :loading="loading"
        :columns="columns"
        :dataSource="items"
        :rowKey="record => record.id || record.name"
        :pagination="{showSizeChanger: true, total: total}"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        @change="handleTableChange"
        @handle-search-filter="handleTableChange"
        style="overflow-y: auto"/>
    </div>

    <div :span="24" class="action-button">
      <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
      <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
    </div>

  </a-form>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'

export default {
  name: 'ChangeBgpPeersForVpc',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      items: [],
      total: 0,
      columns: [
        {
          dataIndex: 'asnumber',
          title: this.$t('label.asnumber'),
          sorter: (a, b) => genericCompare(a?.asnumber || '', b?.asnumber || '')
        },
        {
          dataIndex: 'ipaddress',
          title: this.$t('label.ipaddress'),
          sorter: (a, b) => genericCompare(a?.ipaddress || '', b?.ipaddress || '')
        },
        {
          dataIndex: 'ip6address',
          title: this.$t('label.ip6address'),
          sorter: (a, b) => genericCompare(a?.ip6address || '', b?.ip6address || '')
        }
      ],
      selectedRowKeys: [],
      options: {
        page: 1,
        pageSize: 10,
        keyword: ''
      },
      filter: '',
      loading: false
    }
  },
  created () {
    if (this.resource.bgppeers) {
      for (const bgppeer of this.resource.bgppeers) {
        this.selectedRowKeys.push(bgppeer.id)
      }
    }
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      this.items = []
      this.total = 0
      api('listBgpPeers', {
        keyword: this.options.keyword,
        zoneid: this.resource.zoneid,
        domainid: this.resource.domainid,
        account: this.resource.account,
        projectid: this.resource.projectid,
        page: this.options.page,
        pageSize: this.options.pageSize,
        response: 'json'
      }).then(response => {
        this.total = response.listbgppeersresponse?.count || 0
        if (this.total !== 0) {
          this.items = response.listbgppeersresponse.bgppeer
        }
      }).finally(() => {
        this.loading = false
      })
    },
    onSelectChange (selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    handleSearch (keyword) {
      this.filter = keyword
      this.options.keyword = keyword
      this.fetchData()
    },
    handleTableChange (pagination) {
      this.options.page = pagination.current
      this.options.pageSize = pagination.pageSize
      this.fetchData()
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit () {
      if (this.loading) return
      this.loading = true
      api('changeBgpPeersForVpc', {
        vpcid: this.resource.id,
        bgppeerids: this.selectedRowKeys.join(',')
      }).then(response => {
        this.$pollJob({
          jobId: response.changebgppeersforvpcresponse.jobid,
          successMessage: this.$t('message.success.change.bgp.peers'),
          successMethod: () => {
          },
          errorMessage: this.$t('message.update.failed'),
          errorMethod: () => {
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
          }
        })
        this.$emit('close-action')
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="scss">
.form {
  width: 90vw;
  @media (min-width: 800px) {
    width: 45vw;
  }
}
</style>
