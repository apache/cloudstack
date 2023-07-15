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
    <a-spin :spinning="loading">
      <a-button
        :disabled="!('applyTungstenFabricTag' in $store.getters.apis)"
        type="dashed"
        style="width: 100%; margin-bottom: 15px"
        @click="applyTag">
        <template #icon><plus-outlined /></template>
        {{ $t('label.apply.tungsten.tag') }}
      </a-button>
      <a-table
        size="small"
        style="overflow-y: auto"
        :loading="fetchLoading"
        :columns="columns"
        :dataSource="dataSource"
        :rowKey="item => item.uuid"
        :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-popconfirm
              :title="$t('message.delete.tungsten.tag')"
              @confirm="removeTag(record.uuid)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')">
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.remove.tag')"
                danger
                type="primary"
                icon="delete-outlined"
                :loading="deleteLoading" />
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-divider/>
      <a-pagination
        class="row-element pagination"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="totalCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>

    <a-modal
      v-if="tagModal"
      :visible="tagModal"
      :title="$t('label.add.tungsten.firewall.policy')"
      :closable="true"
      :footer="null"
      @cancel="closeAction"
      centered
      width="450px">
      <div v-ctrl-enter="handleSubmit">
        <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical">
          <a-form-item name="taguuid" ref="taguuid">
            <template #label>
              <tooltip-label :title="$t('label.taguuid')" :tooltip="apiParams.taguuid.description"/>
            </template>
            <a-select
              v-focus="true"
              :loading="tagSrc.loading"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              v-model:value="form.taguuid"
              :placeholder="apiParams.taguuid.description">
              <a-select-option v-for="opt in tagSrc.opts" :key="opt.uuid" :label="opt.name">{{ opt.name }}</a-select-option>
            </a-select>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button ref="submit" type="primary" @click="handleSubmit" :loading="addLoading">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'FirewallTagTab',
  components: {
    TooltipLabel,
    TooltipButton
  },
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
      zoneId: undefined,
      fetchLoading: false,
      deleteLoading: false,
      addLoading: false,
      tagModal: false,
      dataSource: [],
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'name'
      }, {
        title: this.$t('label.actions'),
        key: 'actions',
        width: 80
      }],
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      totalCount: 0,
      tagSrc: {
        loading: false,
        opts: []
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('applyTungstenFabricTag')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        taguuid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData  () {
      if (!this.resource.uuid || !('zoneid' in this.$route.query)) {
        return
      }
      this.zoneId = this.$route.query.zoneid
      this.fetchLoading = true
      this.dataSource = []
      this.totalCount = 0
      api('listTungstenFabricTag', {
        zoneid: this.zoneId,
        applicationpolicysetuuid: this.resource.uuid,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.dataSource = json?.listtungstenfabrictagresponse?.tag || []
        this.totalCount = json?.listtungstenfabrictagresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchTag () {
      this.tagSrc.loading = true
      this.tagSrc.opts = []
      api('listTungstenFabricTag', { zoneid: this.zoneId }).then(json => {
        this.tagSrc.opts = json?.listtungstenfabrictagresponse?.tag || []
      }).finally(() => {
        this.tagSrc.loading = true
      })
    },
    applyTag () {
      this.tagModal = true
      this.fetchTag()
    },
    handleSubmit () {
      if (this.addLoading) return
      this.addLoading = true
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.applicationpolicysetuuid = this.resource.uuid
        params.zoneid = this.zoneId
        params.taguuid = values.taguuid

        const tag = this.tagSrc.opts.filter(tag => tag.uuid === params.taguuid)
        const description = tag[0]?.name || values.taguuid

        api('applyTungstenFabricTag', params).then(json => {
          this.$pollJob({
            jobId: json.applytungstenfabrictagresponse.jobid,
            title: this.$t('label.apply.tungsten.tag'),
            description,
            successMethod: () => {
              this.fetchData()
              this.addLoading = false
              this.tagModal = false
            },
            errorMethod: () => {
              this.fetchData()
              this.addLoading = false
            },
            loadingMessage: this.$t('message.adding.tag'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
              this.addLoading = false
            },
            action: {
              isFetchData: false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.addLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.addLoading = false
      })
    },
    removeTag (uuid) {
      if (this.deleteLoading) return
      this.deleteLoading = true
      const params = {}
      params.zoneid = this.zoneId
      params.applicationpolicysetuuid = this.resource.uuid
      params.taguuid = uuid

      api('removeTungstenFabricTag', params).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabrictagresponse.jobid,
          title: this.$t('label.delete.tungsten.firewall.policy'),
          description: uuid,
          successMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          errorMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          loadingMessage: this.$t('message.deleting.firewall.policy'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          action: {
            isFetchData: false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.deleteLoading = false
      })
    },
    closeAction () {
      this.tagModal = false
      if (this.formRef?.value) {
        this.formRef.value.resetFields()
      }
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
    }
  }
}
</script>
