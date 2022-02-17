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
    <a-button
      :disabled="!('addTungstenFabricInterfaceStaticRoute' in $store.getters.apis)"
      type="dashed"
      icon="plus"
      style="width: 100%; margin-bottom: 15px"
      @click="addInterfaceStaticRoute">
      {{ $t('label.add.tungsten.interface.static.route') }}
    </a-button>
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

    <a-modal
      v-if="interfaceStaticRouteModal"
      :visible="interfaceStaticRouteModal"
      :title="$t('label.add.tungsten.network.static.route')"
      :closable="true"
      :footer="null"
      @cancel="closeAction"
      v-ctrl-enter="handleSubmit"
      centered
      width="450px">
      <a-form :form="form" layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.routeprefix')" :tooltip="apiParams.interfacerouteprefix.description"/>
          <a-input
            :auto-focus="true"
            v-decorator="['interfacerouteprefix', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.interfacerouteprefix.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.communities')" :tooltip="apiParams.interfacecommunities.description"/>
          <a-select
            mode="tags"
            :token-separators="[',']"
            v-decorator="['interfacecommunities', { rules: [{ type: 'array' }] }]"
            :placeholder="apiParams.interfacecommunities.description">
            <a-select-option v-for="item in listCommunities" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button :loading="actionLoading" @click="closeAction"> {{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="actionLoading" type="primary" @click="handleSubmit" ref="submit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'TungstenInterfaceStaticRoute',
  components: { TooltipButton, TooltipLabel },
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
      actionLoading: false,
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
      ],
      interfaceStaticRouteModal: false,
      listRouteNextHopType: [{
        id: 'ip-address',
        description: 'ip-address'
      }],
      listCommunities: [{
        id: 'no-export',
        name: 'no-export'
      }, {
        id: 'no-export-subconfed',
        name: 'no-export-subconfed'
      }, {
        id: 'no-advertise',
        name: 'no-advertise'
      }, {
        id: 'no-reoriginate',
        name: 'no-reoriginate'
      }]
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
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('addTungstenFabricInterfaceStaticRoute')
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
          errorMethod: () => {
            this.fetchData()
          },
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
    addInterfaceStaticRoute () {
      this.interfaceStaticRouteModal = true
    },
    handleSubmit () {
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }

        const params = {}
        params.zoneid = this.zoneId
        params.tungsteninterfaceroutetableuuid = this.resource.uuid
        params.interfacerouteprefix = values.interfacerouteprefix
        params.interfacecommunities = values.interfacecommunities ? values.interfacecommunities.join(',') : null

        this.actionLoading = true
        api('addTungstenFabricInterfaceStaticRoute', params).then(json => {
          this.$pollJob({
            jobId: json.addtungstenfabricinterfacestaticrouteresponse.jobid,
            title: this.$t('label.add.tungsten.interface.static.route'),
            description: values.routeprefix,
            successMessage: `${this.$t('message.success.add.interface.static.route')} ${values.routeprefix}`,
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.error.add.interface.static.route'),
            errorMethod: () => {
              this.fetchData()
            },
            loadingMessage: this.$t('message.loading.add.interface.static.route'),
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
        }).finally(() => {
          this.actionLoading = false
          this.closeAction()
        })
      })
    },
    closeAction () {
      this.interfaceStaticRouteModal = false
      this.form.resetFields()
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
