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
      :disabled="!('addTungstenFabricNetworkStaticRoute' in $store.getters.apis)"
      type="dashed"
      style="width: 100%; margin-bottom: 15px"
      @click="addStaticRoute">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.tungsten.network.static.route') }}
    </a-button>
    <a-table
      size="small"
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
      <template #action="{ record }">
        <a-popconfirm
          v-if="'removeTungstenFabricNetworkStaticRoute' in $store.getters.apis"
          placement="topRight"
          :title="$t('message.action.delete.tungsten.static.route')"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.no')"
          :loading="deleteLoading"
          @confirm="deleteStaticRoute(record)"
        >
          <tooltip-button
            :tooltip="$t('label.action.delete.tungsten.static.route')"
            danger
            type="primary"
            icon="delete-outlined" />
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
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>

    <a-modal
      v-if="addStaticRouteModal"
      :visible="addStaticRouteModal"
      :title="$t('label.add.tungsten.network.static.route')"
      :closable="true"
      :footer="null"
      @cancel="closeAction"
      centered
      width="450px">
      <div v-ctrl-enter="handleSubmit">
        <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical">
          <a-form-item name="routeprefix" ref="routeprefix">
            <template #label>
              <tooltip-label :title="$t('label.routeprefix')" :tooltip="apiParams.routeprefix.description"/>
            </template>
            <a-input
              v-focus="true"
              v-model:value="form.routeprefix"
              :placeholder="apiParams.routeprefix.description"/>
          </a-form-item>
          <a-form-item name="routenexthop" ref="routenexthop">
            <template #label>
            <tooltip-label :title="$t('label.routenexthop')" :tooltip="apiParams.routenexthop.description"/>
            </template>
            <a-input
              v-model:value="form.routenexthop"
              :placeholder="apiParams.routenexthop.description"/>
          </a-form-item>
          <a-form-item name="routenexthoptype" ref="routenexthoptype">
            <template #label>
              <tooltip-label :title="$t('label.routenexthoptype')" :tooltip="apiParams.routenexthoptype.description"/>
            </template>
            <a-select
              v-model:value="form.routenexthoptype"
              :placeholder="apiParams.routenexthoptype.description">
              <a-select-option v-for="item in listRouteNextHopType" :key="item.id">{{ item.description }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="communities" ref="communities">
            <template #label>
              <tooltip-label :title="$t('label.communities')" :tooltip="apiParams.communities.description"/>
            </template>
            <a-select
              mode="tags"
              :token-separators="[',']"
              v-model:value="form.communities"
              :placeholder="apiParams.communities.description">
              <a-select-option v-for="item in listCommunities" :key="item.id">{{ item.name }}</a-select-option>
            </a-select>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button :loading="actionLoading" @click="closeAction"> {{ this.$t('label.cancel') }}</a-button>
            <a-button :loading="actionLoading" type="primary" @click="handleSubmit" ref="submit">{{ this.$t('label.ok') }}</a-button>
          </div>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'TungstenNetworkStaticRoute',
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
          slots: { customRender: 'routeprefix' }
        },
        {
          title: this.$t('label.routenexthop'),
          dataIndex: 'routenexthop',
          slots: { customRender: 'routenexthop' }
        },
        {
          title: this.$t('label.routenexthoptype'),
          dataIndex: 'routenexthoptype',
          slots: { customRender: 'routenexthoptype' }
        },
        {
          title: this.$t('label.communities'),
          dataIndex: 'communities',
          slots: { customRender: 'communities' }
        },
        {
          title: this.$t('label.action'),
          dataIndex: 'action',
          slots: { customRender: 'action' },
          width: 80
        }
      ],
      addStaticRouteModal: false,
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
    this.apiParams = this.$getApiParams('addTungstenFabricNetworkStaticRoute')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        routenexthoptype: 'ip-address'
      })
      this.rules = reactive({
        routeprefix: [{ required: true, message: this.$t('message.error.required.input') }],
        routenexthop: [{ required: true, message: this.$t('message.error.required.input') }],
        routenexthoptype: [{ required: true, message: this.$t('message.error.select') }],
        communities: [{ type: 'array' }]
      })
    },
    fetchData () {
      if (!this.resource.uuid || !('zoneid' in this.$route.query)) return
      this.zoneId = this.$route.query.zoneid || null

      const params = {}
      params.zoneid = this.zoneId
      params.tungstennetworkroutetableuuid = this.resource.uuid
      params.page = this.page
      params.pagesize = this.pageSize

      this.dataSource = []
      this.fetchLoading = true
      api('listTungstenFabricNetworkStaticRoute', params).then(json => {
        this.dataSource = json?.listtungstenfabricnetworkstaticrouteresponse?.networkstaticroute || []
        this.itemCount = json?.listtungstenfabricnetworkstaticrouteresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.fetchLoading = false })
    },
    deleteStaticRoute (record) {
      const params = {}
      params.zoneid = this.zoneId
      params.tungstennetworkroutetableuuid = this.resource.uuid
      params.routeprefix = record.routeprefix

      this.deleteLoading = true
      api('removeTungstenFabricNetworkStaticRoute', params).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricnetworkstaticrouteresponse.jobid,
          title: this.$t('label.action.delete.network.static.route'),
          description: record.routeprefix,
          successMessage: `${this.$t('message.success.delete.network.static.route')} ${record.routeprefix}`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.error.delete.network.static.route'),
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: this.$t('message.loading.delete.network.static.route'),
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
    addStaticRoute () {
      this.addStaticRouteModal = true
    },
    closeAction () {
      this.addStaticRouteModal = false
      this.formRef.value.resetFields()
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        const params = {}
        params.zoneid = this.zoneId
        params.tungstennetworkroutetableuuid = this.resource.uuid
        params.routeprefix = values.routeprefix
        params.routenexthop = values.routenexthop
        params.routenexthoptype = values.routenexthoptype
        params.communities = values.communities ? values.communities.join(',') : null

        this.actionLoading = true
        api('addTungstenFabricNetworkStaticRoute', params).then(json => {
          this.$pollJob({
            jobId: json.addtungstenfabricnetworkstaticrouteresponse.jobid,
            title: this.$t('label.add.tungsten.network.static.route'),
            description: values.routeprefix,
            successMessage: `${this.$t('message.success.add.network.static.route')} ${values.routeprefix}`,
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.error.add.network.static.route'),
            errorMethod: () => {
              this.fetchData()
            },
            loadingMessage: this.$t('message.loading.add.network.static.route'),
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.actionLoading = false
      })
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
