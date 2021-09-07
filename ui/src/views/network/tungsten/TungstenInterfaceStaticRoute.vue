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
    <a-table
      size="small"
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
      <template slot="action" slot-scope="text, record">
        <a-popconfirm
          v-if="'removeTungstenFabricInterfaceStaticRoute' in $store.getters.apis"
          placement="topRight"
          :title="$t('message.action.delete.interface.static.route')"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.no')"
          :loading="deleteLoading"
          @confirm="deleteStaticRoute(record)"
        >
          <tooltip-button
            :tooltip="$t('label.action.delete.interface.static.route')"
            type="danger"
            icon="delete" />
        </a-popconfirm>
      </template>
    </a-table>

    <div style="display: block; text-align: right; margin-top: 10px;">
      <a-pagination
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="pageSizeOptions"
        @change="onChangePage"
        @showSizeChange="onChangePageSize"
        showSizeChanger>
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'TungstenInterfaceStaticRoute',
  components: { TooltipButton },
  mixins: [mixinDevice],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      zoneId: null,
      fetchLoading: false,
      deleteLoading: false,
      dataSource: [],
      itemCount: 0,
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      columns: [
        {
          title: this.$t('label.routeprefix'),
          dataIndex: 'routeprefix',
          scopedSlots: { customRender: 'routeprefix' }
        },
        {
          title: this.$t('label.communities'),
          dataIndex: 'communities',
          scopedSlots: { customRender: 'communities' }
        },
        {
          title: this.$t('label.action'),
          dataIndex: 'action',
          scopedSlots: { customRender: 'action' },
          width: 80
        }
      ]
    }
  },
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
  watch: {
    resource () {
      this.fetchData()
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (!this.resource.uuid || !('zoneid' in this.$route.query)) return
      this.zoneId = this.$route.query.zoneid || null

      const params = {}
      params.zoneid = this.zoneId
      params.tungsteninterfaceroutetableuuid = this.resource.uuid
      params.page = this.page
      params.pagesize = this.pageSize

      this.dataSource = []
      this.fetchLoading = true
      api('listTungstenFabricInterfaceStaticRoute', params).then(json => {
        this.dataSource = json?.listtungstenfabricinterfacestaticrouteresponse?.interfacestaticroute || []
        this.itemCount = json?.listtungstenfabricinterfacestaticrouteresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.fetchLoading = false })
    },
    deleteStaticRoute (record) {
      const params = {}
      params.zoneid = this.zoneId
      params.tungsteninterfaceroutetableuuid = this.resource.uuid
      params.routeprefix = record.routeprefix

      this.deleteLoading = true
      api('removeTungstenFabricInterfaceStaticRoute', params).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricinterfacestaticrouteresponse.jobid,
          title: this.$t('label.action.delete.interface.static.route'),
          description: record.routeprefix,
          successMessage: `${this.$t('message.success.delete.interface.static.route')} ${record.routeprefix}`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.error.delete.interface.static.route'),
          loadingMessage: this.$t('message.loading.delete.interface.static.route'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
          },
          action: {
            isFetchData: false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.deleteLoading = false })
    },
    onChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    onChangePageSize (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style scoped>
</style>
