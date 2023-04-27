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
  <div class="tungsten-network-action">
    <action-button
      :actions="actions"
      :dataView="false"
      @exec-action="execAction" />
    <a-input-search
      class="search-input"
      :placeholder="$t('label.search')"
      @search="handleSearch" />
  </div>
</template>

<script>
import ActionButton from '@/components/view/ActionButton'

export default {
  name: 'TungstenNetworkAction',
  components: {
    ActionButton
  },
  props: {
    actions: {
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
    }
  },
  data () {
    return {
      searchQuery: ''
    }
  },
  inject: ['onFetchData', 'onExecAction'],
  methods: {
    handleSearch (keyword) {
      this.searchQuery = keyword
      const query = {}
      query.page = this.page
      query.pageSize = this.pageSize
      query.searchQuery = this.searchQuery
      this.onFetchData(query)
    },
    execAction (action) {
      this.onExecAction(action)
    }
  }
}
</script>

<style scoped lang="less">
.tungsten-network-action {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;

  .search-input {
    width: 250px;
    margin-left: 10px;
  }
}
</style>
