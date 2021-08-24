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
  <div class="tungsten-network-table">
    <a-table
      size="middle"
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="false"
      :rowKey="(record, idx) => record.id || record.name || idx + '-' + Math.random()"
      :scroll="{ y: 350 }">
      <template slot="name" slot-scope="text, record">
        <QuickView
          :actions="actions"
          :enabled="true"
          :resource="record"
          @exec-action="(action) => execAction(action, record)"/>
        {{ text }}
      </template>
      <template slot="tungstenvms" slot-scope="text, record">
        <span>{{ record.tungstenvms.map(item => item.name).join(',') }}</span>
      </template>
      <template slot="network" slot-scope="text, record">
        <span>{{ record.network.map(item => item.name).join(',') }}</span>
      </template>
      <template slot="firewallpolicy" slot-scope="text, record">
        <span>{{ record.firewallpolicy.map(item => item.name).join(',') }}</span>
      </template>
      <template slot="firewallrule" slot-scope="text, record">
        <span>{{ record.firewallrule.map(item => item.name).join(',') }}</span>
      </template>
      <template slot="vm" slot-scope="text, record">
        <span>{{ record.vm.map(item => item.name).join(',') }}</span>
      </template>
      <template slot="nic" slot-scope="text, record">
        <span>{{ record.nic.map(item => item.name).join(',') }}</span>
      </template>
      <template slot="tag" slot-scope="text, record">
        <div class="tags" v-for="(tag, index) in record.tag" :key="index">
          <a-tag :key="index">{{ tag.key }} = {{ tag.value }}</a-tag>
        </div>
      </template>
    </a-table>
    <a-pagination
      class="row-element"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="itemCount"
      :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="device === 'desktop' ? ['20', '50', '100', '200'] : ['10', '20', '50', '100', '200']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger
      showQuickJumper>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>
</template>

<script>
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/view/TooltipButton'
import QuickView from '@/components/view/QuickView'
export default {
  name: 'TungstenNetworkTable',
  components: { QuickView, TooltipButton },
  props: {
    dataSource: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    },
    page: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 20
    },
    itemCount: {
      type: Number,
      default: 0
    },
    loading: {
      type: Boolean,
      default: false
    },
    searchQuery: {
      type: String,
      default: ''
    },
    actions: {
      type: Array,
      default: () => []
    }
  },
  mixins: [mixinDevice],
  inject: ['onFetchData', 'onExecAction'],
  methods: {
    changePage (page, pageSize) {
      const query = {}
      query.page = page
      query.pageSize = pageSize
      query.searchQuery = this.searchQuery
      this.onFetchData(query)
    },
    changePageSize (currentPage, pageSize) {
      const query = {}
      query.page = currentPage
      query.pageSize = pageSize
      query.searchQuery = this.searchQuery
      this.onFetchData(query)
    },
    execAction (action, record) {
      this.onExecAction(action, record)
    }
  }
}
</script>

<style scoped lang="less">
.tungsten-network-table {
  margin-top: 20px;

  .row-element {
    margin-top: 20px;
    text-align: end;
  }
}
</style>
