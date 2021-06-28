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
      style="overflow-y: auto"
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="false"
      :rowKey="record => record.zoneid">
      <div slot="isready" slot-scope="text, record">
        <span v-if="record.isready">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </div>
      <template slot="action" slot-scope="text, record">
        <tooltip-button
          style="margin-right: 5px"
          :disabled="!('copyTemplate' in $store.getters.apis && record.isready)"
          :title="$t('label.action.copy.template')"
          icon="copy"
          :loading="copyLoading"
          @click="showCopyTemplate(record)" />
        <tooltip-button
          style="margin-right: 5px"
          :disabled="!('deleteTemplate' in $store.getters.apis)"
          :title="$t('label.action.delete.template')"
          type="danger"
          icon="delete"
          @click="onShowDeleteModal(record)"/>
      </template>
    </a-table>
    <a-pagination
      class="row-element"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="itemCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-if="'copyTemplate' in $store.getters.apis"
      style="top: 20px;"
      :title="$t('label.action.copy.template')"
      :visible="showCopyActionForm"
      :closable="true"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @ok="handleCopyTemplateSubmit"
      @cancel="onCloseModal"
      :confirmLoading="copyLoading"
      centered>
      <a-spin :spinning="copyLoading">
        <a-form
          :form="form"
          @submit="handleCopyTemplateSubmit"
          layout="vertical">
          <a-form-item :label="$t('label.zoneid')">
            <a-select
              id="zone-selection"
              mode="multiple"
              :placeholder="$t('label.select.zones')"
              v-decorator="['zoneid', {
                rules: [
                  {
                    required: true,
                    message: `${this.$t('message.error.select')}`
                  }
                ]
              }]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              autoFocus>
              <a-select-option v-for="zone in zones" :key="zone.id">
                {{ zone.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>

    <a-modal
      :title="$t('label.action.delete.template')"
      :visible="showDeleteTemplate"
      :closable="true"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @ok="deleteTemplate"
      @cancel="onCloseModal"
      :confirmLoading="deleteLoading"
      centered>
      <a-spin :spinning="deleteLoading">
        <a-alert :message="$t('message.action.delete.template')" type="warning" />
        <a-form-item :label="$t('label.isforced')" style="margin-bottom: 0;">
          <a-switch v-model="forcedDelete"></a-switch>
        </a-form-item>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'TemplateZones',
  components: {
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
      columns: [],
      dataSource: [],
      page: 1,
      pageSize: 10,
      itemCount: 0,
      fetchLoading: false,
      showCopyActionForm: false,
      currentRecord: {},
      zones: [],
      zoneLoading: false,
      copyLoading: false,
      deleteLoading: false,
      showDeleteTemplate: false,
      forcedDelete: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiConfigParams = (this.$store.getters.apis.copyTemplate && this.$store.getters.apis.copyTemplate.params) || []
    this.apiParams = {}
    this.apiConfigParams.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.columns = [
      {
        title: this.$t('label.zonename'),
        dataIndex: 'zonename'
      },
      {
        title: this.$t('label.status'),
        dataIndex: 'status'
      },
      {
        title: this.$t('label.isready'),
        dataIndex: 'isready',
        scopedSlots: { customRender: 'isready' }
      }
    ]
    if (this.isActionPermitted()) {
      this.columns.push({
        title: '',
        dataIndex: 'action',
        fixed: 'right',
        width: 100,
        scopedSlots: { customRender: 'action' }
      })
    }

    const userInfo = this.$store.getters.userInfo
    if (!['Admin'].includes(userInfo.roletype) &&
      (userInfo.account !== this.resource.account || userInfo.domain !== this.resource.domain)) {
      this.columns = this.columns.filter(col => { return col.dataIndex !== 'status' })
    }
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData) {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      const params = {}
      params.id = this.resource.id
      params.templatefilter = 'executable'
      params.listall = true
      params.page = this.page
      params.pagesize = this.pageSize

      this.dataSource = []
      this.itemCount = 0
      this.fetchLoading = true
      api('listTemplates', params).then(json => {
        this.dataSource = json.listtemplatesresponse.template || []
        this.itemCount = json.listtemplatesresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    },
    isActionPermitted () {
      return (['Admin'].includes(this.$store.getters.userInfo.roletype) || // If admin or owner or belongs to current project
        (this.resource.domainid === this.$store.getters.userInfo.domainid && this.resource.account === this.$store.getters.userInfo.account) ||
        (this.resource.domainid === this.$store.getters.userInfo.domainid && this.resource.projectid && this.$store.getters.project && this.$store.getters.project.id && this.resource.projectid === this.$store.getters.project.id)) &&
        (this.resource.isready || !this.resource.status || this.resource.status.indexOf('Downloaded') === -1) && // Template is ready or downloaded
        this.resource.templatetype !== 'SYSTEM'
    },
    deleteTemplate () {
      const params = {
        id: this.currentRecord.id,
        forced: this.forcedDelete,
        zoneid: this.currentRecord.zoneid
      }
      this.deleteLoading = true
      api('deleteTemplate', params).then(json => {
        const jobId = json.deletetemplateresponse.jobid
        this.$store.dispatch('AddAsyncJob', {
          title: this.$t('label.action.delete.template'),
          jobid: jobId,
          description: this.resource.name,
          status: 'progress'
        })
        const singleZone = (this.dataSource.length === 1)
        this.$pollJob({
          jobId,
          successMethod: result => {
            if (singleZone) {
              const isResourcePage = (this.$route.params && this.$route.params.id)
              if (isResourcePage) {
                this.$router.go(-1)
              }
            } else {
              this.fetchData()
            }
          },
          errorMethod: () => this.fetchData(),
          loadingMessage: `${this.$t('label.deleting.template')} ${this.resource.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result')
        })
        this.onCloseModal()
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.deleteLoading = false
      })
    },
    fetchZoneData () {
      this.zones = []
      this.zoneLoading = true
      api('listZones', { listall: true }).then(json => {
        const zones = json.listzonesresponse.zone || []
        this.zones = [...zones.filter((zone) => this.currentRecord.zoneid !== zone.id)]
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    showCopyTemplate (record) {
      this.currentRecord = record
      this.form.setFieldsValue({
        zoneid: []
      })
      this.fetchZoneData()
      this.showCopyActionForm = true
    },
    onShowDeleteModal (record) {
      this.forcedDelete = false
      this.currentRecord = record
      this.showDeleteTemplate = true
    },
    onCloseModal () {
      this.currentRecord = {}
      this.showCopyActionForm = false
      this.showDeleteTemplate = false
    },
    handleCopyTemplateSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const params = {
          id: this.currentRecord.id,
          sourcezoneid: this.currentRecord.zoneid,
          destzoneids: values.zoneid.join()
        }
        this.copyLoading = true
        api('copyTemplate', params).then(json => {
          const jobId = json.copytemplateresponse.jobid
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('label.action.copy.template'),
            jobid: jobId,
            description: this.resource.name,
            status: 'progress'
          })
          this.$pollJob({
            jobId,
            successMethod: result => {
              this.fetchData()
            },
            errorMethod: () => this.fetchData(),
            loadingMessage: `${this.$t('label.action.copy.template')} ${this.resource.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.copyLoading = false
          this.$emit('refresh-data')
          this.onCloseModal()
          this.fetchData()
        })
      })
    }
  }
}
</script>

<style lang="less" scoped>
.row-element {
  margin-top: 15px;
  margin-bottom: 15px;
}
</style>
