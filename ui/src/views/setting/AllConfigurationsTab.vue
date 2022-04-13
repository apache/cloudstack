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
    <div class="config-row-element">
      <a-table
        class="config-list-view"
        size="small"
        :pagination="false"
        :loading="tabLoading"
        :columns="columns"
        :items="items"
        :dataSource="items"
        :columnKeys="columnKeys"
        :rowKey="record => record.name"
        :rowClassName="getRowClassName"
        @refresh="this.fetchConfigData">
        <template #value="{ record }">
           <ConfigurationValue :configrecord="record" :loading="tabLoading" />
        </template>
      </a-table>
      <a-pagination
        class="config-row-element"
        style="margin-top: 10px"
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
  </div>
</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'
import TooltipButton from '@/components/widgets/TooltipButton'
import ConfigurationValue from './ConfigurationValue'

export default {
  components: {
    ListView,
    TooltipButton,
    ConfigurationValue
  },
  name: 'AllConfigurationsTab',
  props: {
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      apiName: 'listConfigurations',
      columns: [
        {
          title: 'Name',
          dataIndex: 'name',
          slots: { customRender: 'name' },
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        },
        {
          title: 'Description',
          dataIndex: 'description',
          slots: { customRender: 'description' },
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') },
          width: '30%'
        },
        {
          title: 'Category',
          dataIndex: 'category',
          slots: { customRender: 'category' },
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        },
        {
          title: 'Value',
          dataIndex: 'value',
          slots: { customRender: 'value' }
        }
      ],
      columnKeys: this.columns,
      items: [],
      itemCount: 0,
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      editableValueKey: null,
      editableValue: '',
      tabLoading: this.loading,
      filter: ''
    }
  },
  created () {
    this.fetchConfigData()
  },
  watch: {
    '$route' (to, from) {
      if (to.fullPath !== from.fullPath && !to.fullPath.includes('action/')) {
        if ('page' in to.query) {
          this.page = Number(to.query.page)
          this.pageSize = Number(to.query.pagesize)
        } else {
          this.page = 1
        }
        this.itemCount = 0
        this.fetchConfigData()
      }
    },
    '$i18n.locale' (to, from) {
      if (to !== from) {
        this.fetchConfigData()
      }
    }
  },
  computed: {
    pageSizeOptions () {
      var sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
  methods: {
    fetchConfigData (callback) {
      this.tabLoading = true
      const params = {
        listAll: true
      }
      if (Object.keys(this.$route.query).length > 0) {
        if ('page' in this.$route.query) {
          this.page = Number(this.$route.query.page)
        }
        if ('pagesize' in this.$route.query) {
          this.pagesize = Number(this.$route.query.pagesize)
        }
        Object.assign(params, this.$route.query)
      }
      if (this.filter) {
        params.keyword = this.filter
      }
      this.columnKeys = [...new Set(this.columnKeys)]
      this.columnKeys.sort(function (a, b) {
        if (a === 'name' && b !== 'name') { return -1 }
        if (a < b) { return -1 }
        if (a > b) { return 1 }
        return 0
      })
      if ('listview' in this.$refs && this.$refs.listview) {
        this.$refs.listview.resetSelection()
      }
      params.page = this.page
      params.pagesize = this.pageSize
      api('listConfigurations', params).then(response => {
        this.items = response.listconfigurationsresponse.configuration
        if (!this.items || this.items.length === 0) {
          this.items = []
        }
        this.itemCount = response.listconfigurationsresponse.count
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.tabLoading = false
        if (!callback) return
        callback()
      })
    },
    changePage (page, pageSize) {
      const query = Object.assign({}, this.$route.query)
      query.page = page
      query.pagesize = pageSize
      this.$router.push({ query })
    },
    changePageSize (currentPage, pageSize) {
      const query = Object.assign({}, this.$route.query)
      query.page = currentPage
      query.pagesize = pageSize
      this.$router.push({ query })
    },
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'config-light-row'
      }
      return 'config-dark-row'
    }
  }
}
</script>

<style scoped lang="scss">
  .list {
    clear:both;
  }
  .editable-value {

    @media (min-width: 760px) {
      text-align: right;
      margin-left: 40px;
      margin-right: -40px;
    }

  }
  .item {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__content {
      width: 100%;
      display: block;
      word-break: break-all;

      @media (min-width: 760px) {
        width: auto;
      }

    }

  }
  .action {
    margin-top: 20px;
    margin-left: -12px;

    @media (min-width: 480px) {
      margin-left: -24px;
    }

    @media (min-width: 760px) {
      margin-top: 0;
      margin-left: 0;
    }

  }

  .value {
    margin-top: 20px;

    @media (min-width: 760px) {
      margin-top: 0;
    }

  }

  .config-light-row {
    background-color: #fff;
  }

  .config-dark-row {
    background-color: #f9f9f9;
  }

  .config-row-element {
    margin-bottom: 10px;
  }

  .config-list-view {
    overflow-y: auto;
    display: block;
    width: 100%;
    margin-top: 20px;
  }

</style>
