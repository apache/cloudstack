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
      :disabled="!('applyTungstenFabricPolicy' in $store.getters.apis)"
      type="dashed"
      style="width: 100%; margin-bottom: 15px"
      @click="onShowAction">
      <template #icon><plus-outlined /></template>
      {{ $t('label.apply.tungsten.network.policy') }}
    </a-button>
    <a-table
      size="small"
      :loading="fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a-popconfirm
            v-if="'removeTungstenFabricPolicy' in $store.getters.apis"
            placement="topRight"
            :title="$t('message.confirm.remove.network.policy')"
            :ok-text="$t('label.yes')"
            :cancel-text="$t('label.no')"
            :loading="deleteLoading"
            @confirm="removeNetworkPolicy(record)"
          >
            <tooltip-button
              :tooltip="$t('label.action.remove.network.policy')"
              danger
              type="primary"
              icon="delete-outlined" />
          </a-popconfirm>
        </template>
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
      :title="$t('label.apply.tungsten.network.policy')"
      :maskClosable="false"
      :footer="null"
      @cancel="showAction = false">
      <div v-ctrl-enter="handleSubmit">
        <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical">
          <a-form-item name="policyuuid" ref="policyuuid">
            <template #label>
              <tooltip-label :title="$t('label.policyuuid')" :tooltip="apiParams.policyuuid.description"/>
            </template>
            <a-select
              :loading="networks.loading"
              :placeholder="apiParams.policyuuid.description"
              v-model:value="form.policyuuid">
              <a-select-option v-for="network in networks.opts" :key="network.uuid">{{ network.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="majorsequence" ref="majorsequence">
            <template #label>
              <tooltip-label :title="$t('label.majorsequence')" :tooltip="apiParams.majorsequence.description"/>
            </template>
            <a-input v-model:value="form.majorsequence" :placeholder="apiParams.majorsequence.description" />
          </a-form-item>
          <a-form-item name="minorsequence" ref="minorsequence">
            <template #label>
              <tooltip-label :title="$t('label.minorsequence')" :tooltip="apiParams.minorsequence.description"/>
            </template>
            <a-input v-model:value="form.minorsequence" :placeholder="apiParams.minorsequence.description" />
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
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'NetworkPolicyTab',
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
      networks: {
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
  beforeCreate () {
    this.apiParams = this.$getApiParams('applyTungstenFabricPolicy')
  },
  created () {
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
      this.form = reactive({
        majorsequence: 0,
        minorsequence: 0
      })
      this.rules = reactive({
        policyuuid: [{ required: true, message: this.$t('message.error.select') }],
        majorsequence: [{ required: true, message: this.$t('message.error.required.input') }],
        minorsequence: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    initRemoveForm () {
      this.removeRef = ref()
      this.removeForm = reactive({
        policyuuid: this.networks.opts[0]?.uuid || null
      })
      this.removeRules = reactive({
        policyuuid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      if (Object.keys(this.resource).length === 0) {
        return
      }
      const params = {}
      params.zoneid = this.resource.zoneid
      params.networkid = this.resource.id
      params.listAll = true
      params.page = this.page
      params.pagesize = this.pageSize

      this.itemCount = 0
      this.dataSource = []
      this.fetchLoading = true

      api('listTungstenFabricPolicy', params).then(json => {
        this.itemCount = json?.listtungstenfabricpolicyresponse?.count || 0
        this.dataSource = json?.listtungstenfabricpolicyresponse?.policy || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.fetchLoading = false })
    },
    onShowAction () {
      this.initForm()
      this.showAction = true
      this.networks.loading = true
      this.networks.opts = []
      api('listTungstenFabricPolicy', { listall: true, zoneid: this.resource.zoneid }).then(json => {
        this.networks.opts = json?.listtungstenfabricpolicyresponse?.policy || []
        this.form.networkuuid = this.networks.opts[0]?.uuid || null
      }).finally(() => {
        this.networks.loading = false
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
        params.policyuuid = values.policyuuid
        params.majorsequence = values.majorsequence
        params.minorsequence = values.minorsequence

        const match = this.networks.opts.filter(network => network.uuid === values.policyuuid)
        const resourceName = match[0]?.name || values.policyuuid

        api('applyTungstenFabricPolicy', params).then(json => {
          const jobId = json?.applytungstenfabricpolicyresponse?.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.apply.tungsten.network.policy'),
            description: resourceName,
            successMessage: this.$t('message.success.apply.network.policy'),
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.error.apply.network.policy'),
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
    },
    removeNetworkPolicy (record) {
      if (this.deleteLoading) return
      this.deleteLoading = true
      const params = {}
      params.zoneid = this.resource.zoneid
      params.networkuuid = this.resource.id
      params.policyuuid = record.uuid

      api('removeTungstenFabricPolicy', params).then(json => {
        const jobId = json?.removetungstenfabricpolicyresponse?.jobid
        this.$pollJob({
          jobId,
          title: this.$t('label.action.remove.network.policy'),
          description: record.name || record.uuid,
          successMessage: this.$t('message.success.remove.network.policy'),
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.error.remove.network.policy'),
          errorMethod: () => {
            this.fetchData()
          },
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
    }
  }
}
</script>
