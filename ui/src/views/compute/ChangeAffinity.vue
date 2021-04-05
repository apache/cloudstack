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
  <a-form class="form">

    <p v-html="$t('message.select.affinity.groups')" />

    <div v-if="loading" class="loading">
      <a-icon type="loading" style="color: #1890ff;" />
    </div>

    <div class="form__item">
      <a-input-search
        style="margin-bottom: 10px;"
        :placeholder="$t('label.search')"
        v-model="filter"
        @search="handleSearch"
        autoFocus />
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
      <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
      <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
    </div>

  </a-form>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'

export default {
  name: 'ChangeAffinity',
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
          dataIndex: 'name',
          title: this.$t('label.name'),
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        },
        {
          dataIndex: 'type',
          title: this.$t('label.type'),
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        },
        {
          dataIndex: 'description',
          title: this.$t('label.description'),
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
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
    for (const group of this.resource.affinitygroup) {
      this.selectedRowKeys.push(group.id)
    }
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      this.items = []
      this.total = 0
      api('listAffinityGroups', {
        keyword: this.options.keyword,
        domainid: this.resource.domainid,
        accountid: this.resource.accountid,
        page: this.options.page,
        pageSize: this.options.pageSize,
        response: 'json'
      }).then(response => {
        this.total = response.listaffinitygroupsresponse.count
        if (this.total !== 0) {
          this.items = response.listaffinitygroupsresponse.affinitygroup
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
      this.loading = true
      api('updateVMAffinityGroup', {
        id: this.resource.id,
        affinitygroupids: this.selectedRowKeys.join(',')
      }).then(response => {
        this.$notification.success({
          message: this.$t('message.success.change.affinity.group')
        })
        this.$parent.$parent.close()
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

.action-button {
  text-align: right;
  margin-top: 10px;
  button {
    margin-right: 5px;
  }
}
</style>
