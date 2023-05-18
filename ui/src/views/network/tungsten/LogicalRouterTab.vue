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
      :disabled="!('addTungstenFabricNetworkGatewayToLogicalRouter' in $store.getters.apis)"
      type="dashed"
      style="width: 100%; margin-bottom: 15px"
      @click="onShowAction">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.logical.router') }}
    </a-button>
    <a-table
      size="small"
      :loading="fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
    </a-table>
    <div style="display: block; text-align: right; margin-top: 10px;">
      <a-pagination
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="pageSizeOptions"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>

    <a-modal
      v-if="showAction"
      :visible="showAction"
      :title="$t('label.add.logical.router')"
      :maskClosable="false"
      :footer="null"
      @cancel="showAction = false">
      <div v-ctrl-enter="handleSubmit">
        <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical">
          <a-form-item name="logicalrouteruuid" ref="logicalrouteruuid" :label="$t('label.tungsten.logical.router')">
            <a-select
              :loading="logicalRouters.loading"
              v-model:value="form.logicalrouteruuid">
              <a-select-option v-for="logicalRouter in logicalRouters.opts" :key="logicalRouter.uuid">{{ logicalRouter.name }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>

        <div :span="24" class="action-button">
          <a-button @click="() => { showAction = false }">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'LogicalRouterTab',
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
      fetchLoading: false,
      deleteLoading: false,
      submitLoading: false,
      showAction: false,
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      itemCount: 0,
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'name'
      }, {
        title: this.$t('label.actions'),
        dataIndex: 'actions',
        key: 'actions',
        width: 80
      }],
      dataSource: [],
      logicalRouters: {
        loading: false,
        opts: []
      }
    }
  },
  watch: {
    resource (newData, oldData) {
      if (newData !== oldData) {
        this.fetchData()
      }
    }
  },
  created () {
    this.initForm()
    this.fetchData()
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
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        logicalrouteruuid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      if (Object.keys(this.resource).length === 0) {
        return
      }
      const params = {}
      params.zoneid = this.resource.zoneid
      params.networkuuid = this.resource.id
      params.listAll = true
      params.page = this.page
      params.pagesize = this.pageSize

      this.itemCount = 0
      this.dataSource = []
      this.fetchLoading = true

      api('listTungstenFabricLogicalRouter', params).then(json => {
        this.itemCount = json?.listtungstenfabriclogicalrouterresponse?.count || 0
        this.dataSource = json?.listtungstenfabriclogicalrouterresponse?.logicalrouter || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.fetchLoading = false })
    },
    onShowAction () {
      this.showAction = true
      this.logicalRouters.loading = true
      this.logicalRouters.opts = []
      api('listTungstenFabricLogicalRouter', { zoneid: this.resource.zoneid }).then(json => {
        this.logicalRouters.opts = json?.listtungstenfabriclogicalrouterresponse?.logicalrouter || []
        this.form.logicalrouteruuid = this.logicalRouters.opts[0]?.uuid || null
      }).finally(() => {
        this.logicalRouters.loading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    },
    handleSubmit () {
      if (this.submitLoading) return
      this.submitLoading = true

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        const params = {}
        params.zoneid = this.resource.zoneid
        params.networkuuid = this.resource.id
        params.logicalrouteruuid = values.logicalrouteruuid

        api('addTungstenFabricNetworkGatewayToLogicalRouter', params).then(json => {
          const jobId = json?.addtungstenfabricnetworkgatewaytologicalrouterresponse?.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.add.logical.router'),
            description: values.logicalrouteruuid,
            successMessage: `${this.$t('message.success.add.logical.router')} ${values.logicalrouteruuid}`,
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.error.add.logical.router'),
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
          this.showAction = false
          this.submitLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.submitLoading = false
      })
    }
  }
}
</script>
