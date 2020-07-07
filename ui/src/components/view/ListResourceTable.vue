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
  <a-table
    size="small"
    :columns="fetchColumns()"
    :dataSource="items"
    :rowKey="item => item.id"
    :loading="loading"
    :pagination="false" >

    <template v-if="routerlink" :slot="routerlink.name" slot-scope="text, item">
      <router-link :to="{ path: routerlink.prefix + item.id }" v-if="routerlink.prefix">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </template>
    <template slot="state" slot-scope="text">
      <status :text="text ? text : ''" />{{ text }}
    </template>

  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'ListResourceTable',
  components: {
    Status
  },
  props: {
    apiName: {
      type: String,
      required: true
    },
    routerlink: {
      type: Object,
      default: () => {}
    },
    params: {
      type: Object,
      required: true
    },
    columns: {
      type: Array,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      items: []
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      var params = this.params
      params.listall = true
      params.response = 'json'
      params.details = 'min'
      api(this.apiName, this.params).then(json => {
        var responseName
        var objectName
        for (const key in json) {
          if (key.includes('response')) {
            responseName = key
            break
          }
        }
        for (const key in json[responseName]) {
          if (key === 'count') {
            continue
          }
          objectName = key
          break
        }
        this.items = json[responseName][objectName]
        if (!this.items || this.items.length === 0) {
          this.items = []
        }
      }).finally(() => {
        this.loading = false
      })
    },
    fetchColumns () {
      var columns = []
      for (const col of this.columns) {
        columns.push({
          dataIndex: col,
          title: this.$t('label.' + col),
          scopedSlots: { customRender: col }
        })
      }
      return columns
    }
  }
}
</script>
