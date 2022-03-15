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
  <div>
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="events"
      :actions="actions"
      :columnKeys="columnKeys"
      :selectedColumns="columnKeys"
      ref="listview"
      @selection-change="onRowSelectionChange"
      @refresh="this.fetchData"/>
      <a-pagination
        class="row-element"
        style="margin-top: 10px"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="totalCount"
        :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="pageSizeOptions"
        @change="changePage"
        @showSizeChange="changePage"
        showSizeChanger
        showQuickJumper>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'

export default {
  name: 'EventsTab',
  components: {
    ListView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['level', 'type', 'state', 'username', 'account', 'domain', 'created'],
      columns: [],
      cols: [],
      events: [],
      actions: [],
      page: 1,
      pageSize: 20,
      totalCount: 0,
      resourceTypeValue: ''
    }
  },
  computed: {
    pageSizeOptions () {
      var sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
  created () {
    this.pageSize = this.pageSizeOptions[0] * 1
    for (var columnKey of this.columnKeys) {
      this.columns.push({
        title: this.$t('label.' + String(columnKey).toLowerCase()),
        dataIndex: columnKey,
        slots: { customRender: columnKey },
        sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
      })
    }
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchEvents()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchEvents()
    },
    fetchEvents () {
      this.events = []
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        resourceid: this.resource.id,
        resourcetype: this.resourceType,
        listall: true
      }
      this.tabLoading = true
      api('listEvents', params).then(json => {
        this.events = []
        this.totalCount = json?.listeventsresponse?.count || 0
        this.events = json?.listeventsresponse?.event || []
        this.tabLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>
