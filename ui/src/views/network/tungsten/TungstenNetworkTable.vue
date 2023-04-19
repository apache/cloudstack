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
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
        <router-link v-if="apiName === 'listTungstenFabricPolicy'" :to="{ path: '/tungstenpolicy/' + record.uuid, query: { zoneid: resource.zoneid } }" >{{ text }}</router-link>
        <router-link v-else-if="apiName === 'listTungstenFabricApplicationPolicySet'" :to="{ path: '/tungstenpolicyset/' + record.uuid, query: { zoneid: resource.zoneid } }" >{{ text }}</router-link>
        <span v-else>{{ text }}</span>
        </template>
        <template v-if="column.key === 'tungstenvms'">
          <ul v-if="record.tungstenvms.length > 0"><li v-for="item in record.tungstenvms" :key="item.uuid">{{ item.name }}</li></ul>
        </template>
        <template v-if="column.key === 'network'">
          <ul v-if="record.network.length > 0"><li v-for="item in record.network" :key="item.uuid"><span v-if="item.name">{{ item.name }}</span></li></ul>
        </template>
        <template v-if="column.key === 'firewallpolicy'">
          <span v-if="record.firewallpolicy.length > 0">{{ record.firewallpolicy.map(item => item.name).join(',') }}</span>
        </template>
        <template v-if="column.key === 'firewallrule'">
          <span v-if="record.firewallrule.length > 0">{{ record.firewallrule[0].name }}</span>
        </template>
        <template v-if="column.key === 'tungstenroutingpolicyterm'">
          <span v-if="record.tungstenroutingpolicyterm.length > 0">{{ record.tungstenroutingpolicyterm[0].name }}</span>
        </template>
        <template v-if="column.key === 'vm'">
          <ul v-if="record.vm.length > 0"><li v-for="item in record.vm" :key="item.uuid">{{ item.name }}</li></ul>
        </template>
        <template v-if="column.key === 'nic'">
          <ul v-if="record.nic.length > 0"><li v-for="item in record.nic" :key="item.uuid">{{ item.name }}</li></ul>
        </template>
        <template v-if="column.key === 'tag'">
          <div class="tags" v-for="tag in record.tag" :key="tag.uuid">
            <a-tag :key="tag.uuid">{{ tag.name }}</a-tag>
          </div>
        </template>
        <template v-if="column.key === 'actions'">
          <span v-for="(action, index) in actions" :key="index" style="margin-right: 5px">
            <tooltip-button
              v-if="action.dataView && ('show' in action ? action.show(record, $store.getters) : true)"
              :tooltip="$t(action.label)"
              :danger="['delete-outlined', 'DeleteOutlined'].includes(action.icon)"
              :type="(['DeleteOutlined', 'delete-outlined'].includes(action.icon) ? 'primary' : 'default')"
              :icon="action.icon"
              @click="() => execAction(action, record)" />
          </span>
        </template>
      </template>
    </a-table>
    <a-pagination
      class="row-element"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="itemCount"
      :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="pageSizeOptions"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger
      showQuickJumper>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>
</template>

<script>
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'
import QuickView from '@/components/view/QuickView'

export default {
  name: 'TungstenNetworkTable',
  components: { QuickView, TooltipButton },
  props: {
    apiName: {
      type: String,
      default: ''
    },
    dataSource: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    },
    resource: {
      type: Object,
      default: () => {}
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
  computed: {
    pageSizeOptions () {
      const sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
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

  ul {
    padding-left: 15px;
    margin: 0;
  }
}
</style>
