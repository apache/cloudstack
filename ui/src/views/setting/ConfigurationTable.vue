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
  <div class="config-row-element">
    <a-table
      class="config-list-view"
      size="small"
      :showHeader="false"
      :pagination="false"
      :columns="columns"
      :dataSource="config"
      :rowKey="record => record.name"
      :rowClassName="getRowClassName" >

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <b> {{record.displaytext }} </b> {{ ' (' + record.name + ')' }} <br/> {{ record.description }}
        </template>
        <template v-if="column.key === 'value'">
          <ConfigurationValue :configrecord="record" />
        </template>
      </template>
    </a-table>
    <a-pagination
      class="config-row-element"
      style="margin-top: 10px"
      size="small"
      :current="page"
      :pageSize="pagesize"
      :total="count"
      :showTotal="count => `${$t('label.showing')} ${Math.min(count, 1+((page-1)*pagesize))}-${Math.min(page*pagesize, count)} ${$t('label.of')} ${count} ${$t('label.items')}`"
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
import ConfigurationValue from './ConfigurationValue'

export default {
  components: {
    ConfigurationValue
  },
  name: 'ConfigurationTable',
  props: {
    config: {
      type: Array,
      default: () => { return [] }
    },
    columns: {
      type: Array,
      default: () => { return [] }
    },
    count: {
      type: Number,
      default: 0
    },
    page: {
      type: Number,
      default: 0
    },
    pagesize: {
      type: Number,
      default: 20
    }
  },
  data () {
    return {
      apiName: 'listConfigurations'
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
    changePage (page, pagesize) {
      this.$emit('change-page', page, pagesize)
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
