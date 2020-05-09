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
        <span v-if="record.isready">{{ $t('Yes') }}</span>
        <span v-else>{{ $t('No') }}</span>
      </div>
      <template slot="action" slot-scope="text, record">
        <span style="margin-right: 5px">
          <a-button
            :disabled="!('copyTemplate' in $store.getters.apis)"
            icon="copy"
            shape="circle"
            :loading="copyLoading"
            @click="showCopyTemplate(record)" />
        </span>
        <span style="margin-right: 5px">
          <a-popconfirm
            v-if="'deleteTemplate' in $store.getters.apis"
            placement="topRight"
            title="Delete the template for this zone?"
            :ok-text="$t('Yes')"
            :cancel-text="$t('No')"
            :loading="deleteLoading"
            @confirm="deleteTemplate(record)"
          >
            <a-button
              type="danger"
              icon="delete"
              shape="circle" />
          </a-popconfirm>
        </span>
      </template>
    </a-table>
    <a-pagination
      class="row-element"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="itemCount"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger/>

    <a-modal
      v-if="'copyTemplate' in $store.getters.apis"
      style="top: 20px;"
      :title="$t('label.action.copy.template')"
      :visible="showCopyActionForm"
      :closable="true"
      @ok="handleCopyTemplateSubmit"
      @cancel="onCloseCopyForm"
      :confirmLoading="copyLoading"
      centered>
      <a-spin :spinning="copyLoading">
        <a-form
          :form="form"
          @submit="handleCopyTemplateSubmit"
          layout="vertical">
          <a-form-item :label="$t('zoneid')">
            <a-select
              id="zone-selection"
              mode="multiple"
              placeholder="Select Zones"
              v-decorator="['zoneid', {
                rules: [
                  {
                    required: true,
                    message: 'Please select option'
                  }
                ]
              }]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading">
              <a-select-option v-for="zone in zones" :key="zone.id">
                {{ zone.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'TemplateZones',
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
      deleteLoading: false
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
        title: this.$t('zonename'),
        dataIndex: 'zonename'
      },
      {
        title: this.$t('status'),
        dataIndex: 'status'
      },
      {
        title: this.$t('isready'),
        dataIndex: 'isready',
        scopedSlots: { customRender: 'isready' }
      },
      {
        title: '',
        dataIndex: 'action',
        fixed: 'right',
        width: 100,
        scopedSlots: { customRender: 'action' }
      }
    ]
  },
  mounted () {
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
    deleteTemplate (record) {
      const params = {
        id: record.id,
        zoneid: record.zoneid
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
              this.$router.go(-1)
            } else {
              this.fetchData()
            }
          },
          errorMethod: () => this.fetchData(),
          loadingMessage: `Deleting template ${this.resource.name} in progress`,
          catchMessage: 'Error encountered while fetching async job result'
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.deleteLoading = false
        this.fetchData()
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
    onCloseCopyForm () {
      this.currentRecord = {}
      this.showCopyActionForm = false
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
            loadingMessage: `Copy template ${this.resource.name} in progress`,
            catchMessage: 'Error encountered while fetching async job result'
          })
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.copyLoading = false
          this.$emit('refresh-data')
          this.onCloseCopyForm()
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
