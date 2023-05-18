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
    <p v-html="$t('message.desc.reset.ssh.key.pair')" />
    <a-spin :spinning="loading">

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
          :rowKey="record => record.name"
          :pagination="{showSizeChanger: true, total: total}"
          :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
          @change="handleTableChange"
          @handle-search-filter="handleTableChange"
          style="overflow-y: auto" >

          <template #headerCell="{ column }">
            <template v-if="column.key === 'account'"><user-outlined /> {{ $t('label.account') }}</template>
            <template v-if="column.key === 'domain'"><block-outlined /> {{ $t('label.domain') }}</template>
          </template>

        </a-table>
      </div>

      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>

    </a-spin>
  </a-form>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'

export default {
  name: 'ResetSshKeyPair',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      items: [],
      total: 0,
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.name'),
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') },
          width: '40%'
        },
        {
          key: 'account',
          dataIndex: 'account',
          width: '30%'
        },
        {
          key: 'domain',
          dataIndex: 'domain',
          width: '30%'
        }
      ],
      selectedRowKeys: [],
      options: {
        page: 1,
        pageSize: 10,
        keyword: '',
        response: 'json'
      },
      filter: '',
      loading: false
    }
  },
  created () {
    this.fetchData()
    if (this.resource.keypairs) {
      this.selectedRowKeys = this.resource.keypairs.split(',')
    }
  },
  methods: {
    fetchData () {
      this.loading = true
      this.items = []
      this.total = 0
      api('listSSHKeyPairs', this.options).then(response => {
        this.total = response.listsshkeypairsresponse.count
        if (this.total !== 0) {
          this.items = response.listsshkeypairsresponse.sshkeypair
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
    handleSubmit () {
      if (this.loading) return
      this.loading = true
      api('resetSSHKeyForVirtualMachine', {
        id: this.resource.id,
        keypairs: this.selectedRowKeys.join(',')
      }).then(response => {
        const jobId = response.resetSSHKeyforvirtualmachineresponse.jobid
        const title = `${this.$t('label.reset.ssh.key.pair')}`
        if (jobId) {
          this.$pollJob({
            jobId,
            title,
            description: this.resource.name,
            successMessage: `${title} ${this.$t('label.success')}`,
            loadingMessage: `${title} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }
        this.closeAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    closeAction () {
      this.$emit('close-action')
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
