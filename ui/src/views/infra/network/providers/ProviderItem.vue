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
    <a-row :gutter="12">
      <a-col :md="24" :lg="24" style="text-align: left">
        <action-button
          :actions="provider.actions"
          :resource="resource"
          :loading="loading"
          @exec-action="handleExecAction"/>
      </a-col>
    </a-row>
    <provider-detail
      style="margin-top: 10px"
      :details="provider.details"
      :nsp="resource"
      :loading="loading" />
    <div
      v-for="(list, index) in listData"
      :key="index">
      <provider-list-view
        style="border-top: 1px solid #ddd; padding-top: 5px;"
        v-if="resource.id"
        :title="list.title"
        :action="currentAction"
        :dataSource="list.data"
        :columns="list.columns"
        :itemCount="list.itemCount"
        :resource="resource"
        :page="page"
        :pageSize="pageSize"
        :loading="loading || list.loading" />
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import ActionButton from '@/components/view/ActionButton'
import ProviderDetail from '@/views/infra/network/providers/ProviderDetail'
import ProviderListView from '@/views/infra/network/providers/ProviderListView'

export default {
  name: 'ProviderItem',
  components: {
    ActionButton,
    ProviderDetail,
    ProviderListView
  },
  props: {
    itemNsp: {
      type: Object,
      required: true
    },
    nsp: {
      type: Object,
      default: () => {}
    },
    resourceId: {
      type: String,
      default: () => ''
    },
    zoneId: {
      type: String,
      default: () => ''
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      provider: {},
      listData: {},
      resource: {},
      currentAction: {},
      page: 1,
      pageSize: 10,
      itemCount: 0
    }
  },
  provide () {
    return {
      providerChangePage: this.changePage
    }
  },
  inject: ['provideSetNsp', 'provideExecuteAction'],
  watch: {
    nsp: {
      deep: true,
      handler () {
        this.fetchData()
      }
    }
  },
  created () {
    if (!this.resourceId || !this.zoneId) {
      return
    }
    this.fetchData()
  },
  methods: {
    async fetchData () {
      if (!this.nsp || Object.keys(this.nsp).length === 0) {
        this.resource = {
          name: this.itemNsp.title,
          state: 'Disabled',
          physicalnetworkid: this.resourceId,
          zoneid: this.zoneId
        }
      } else {
        this.resource = this.nsp
        this.resource.zoneid = this.zoneId
      }
      if (this.itemNsp && Object.keys(this.itemNsp).length > 0) {
        this.provider = this.itemNsp
        this.provideSetNsp(this.resource)

        if (!this.provider.lists || this.provider.lists.length === 0) {
          return
        }
        this.provider.lists.map(this.fetchOptions)
      }
    },
    async fetchOptions (args) {
      if (!args || Object.keys(args).length === 0) {
        return
      }

      const params = {}
      if (args.mapping) {
        Object.keys(args.mapping).map(key => {
          params[key] = args.mapping[key]?.value(this.resource) || null
        })
      }
      params.page = this.page
      params.pageSize = this.pageSize

      let length = args.columns.length
      if (args.columns.includes('actions')) {
        length--
      }
      const columns = args.columns.map(col => {
        if (col === 'actions') {
          return {
            key: col,
            title: this.$t('label.' + col),
            dataIndex: col,
            width: 80,
            fixed: 'right'
          }
        }
        const width = 100 / (length) + '%'
        return {
          key: col,
          title: this.$t('label.' + col),
          width: width,
          dataIndex: col
        }
      })

      this.listData[args.title] = {
        title: args.title,
        columns: columns,
        data: [],
        itemCount: 0,
        loading: true
      }

      try {
        const listResult = await this.executeApi(args.api, params)
        this.listData[args.title].data = listResult.data
        this.listData[args.title].itemCount = listResult.itemCount
        this.listData[args.title].loading = false
      } catch (error) {
        this.listData[args.title].loading = false
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: (error.response?.headers?.['x-description']) || error.message
        })
      }
    },
    executeApi (apiName, params) {
      return new Promise((resolve, reject) => {
        api(apiName, params).then(json => {
          let responseName
          let objectName
          let itemCount = 0
          const result = {
            data: [],
            itemCount: 0
          }
          for (const key in json) {
            if (key.includes('response') || key.includes(apiName)) {
              responseName = key
              break
            }
          }
          if (!responseName) {
            resolve(result)
            return
          }
          for (const key in json[responseName]) {
            if (key === 'count') {
              itemCount = json[responseName].count
              continue
            }
            objectName = key
            break
          }
          result.data = json[responseName][objectName] || []
          result.itemCount = itemCount
          resolve(result)
        }).catch(e => {
          reject(e)
        })
      })
    },
    handleExecAction (action) {
      this.currentAction = action
      this.provideExecuteAction(action)
    },
    changePage (listName, page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      const listItem = this.provider.lists.filter(provider => provider.title === listName)[0] || {}
      this.fetchOptions(listItem)
    }
  }
}
</script>

<style scoped>
</style>
