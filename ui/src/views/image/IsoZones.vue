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
      v-if="(('deleteIso' in $store.getters.apis) && this.selectedItems.length > 0)"
      type="primary"
      :danger="true"
      style="width: 100%; margin-bottom: 15px"
      @click="bulkActionConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ $t(message.title) }}
    </a-button>
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="false"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      :rowKey="record => record.zoneid">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'zonename'">
          <span v-if="fetchZoneIcon(record.zoneid)">
            <resource-icon :image="zoneIcon" size="1x" style="margin-right: 5px"/>
          </span>
          <global-outlined v-else style="margin-right: 5px" />
          <span> {{ record.zonename }} </span>
        </template>
        <template v-if="column.key === 'isready'">
          <span v-if="record.isready">{{ $t('label.yes') }}</span>
          <span v-else>{{ $t('label.no') }}</span>
        </template>
        <template v-if="column.key === 'actions'">
          <span style="margin-right: 5px">
            <tooltip-button
              :tooltip="$t('label.action.copy.iso')"
              :disabled="!('copyIso' in $store.getters.apis && record.isready)"
              icon="copy-outlined"
              :loading="copyLoading"
              @click="showCopyIso(record)" />
          </span>
          <span style="margin-right: 5px">
            <a-popconfirm
              v-if="'deleteIso' in $store.getters.apis"
              placement="topRight"
              :title="$t('message.action.delete.iso')"
              :ok-text="$t('label.yes')"
              :cancel-text="$t('label.no')"
              :loading="deleteLoading"
              @confirm="deleteIso(record)"
            >
              <tooltip-button
                :tooltip="$t('label.action.delete.iso')"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </span>
        </template>
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
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-if="'copyIso' in $store.getters.apis"
      style="top: 20px;"
      :title="$t('label.action.copy.iso')"
      :visible="showCopyActionForm"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      :confirmLoading="copyLoading"
      @cancel="onCloseCopyForm"
      centered>
      <a-spin :spinning="copyLoading">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleCopyIsoSubmit"
          v-ctrl-enter="handleCopyIsoSubmit">
          <a-form-item ref="zoneid" name="zoneid" :label="$t('label.zoneid')">
            <a-select
              id="zone-selection"
              mode="multiple"
              :placeholder="$t('label.select.zones')"
              v-model:value="form.zoneid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="zoneLoading"
              v-focus="true">
              <a-select-option v-for="zone in zones" :key="zone.id" :label="zone.name">
                <div>
                  <span v-if="zone.icon && zone.icon.base64image">
                    <resource-icon :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                  </span>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ zone.name }}
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button @click="onCloseCopyForm">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="handleCopyIsoSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>
    <bulk-action-view
      v-if="showConfirmationAction || showGroupActionModal"
      :showConfirmationAction="showConfirmationAction"
      :showGroupActionModal="showGroupActionModal"
      :items="dataSource"
      :selectedRowKeys="selectedRowKeys"
      :selectedItems="selectedItems"
      :columns="columns"
      :selectedColumns="selectedColumns"
      action="deleteIso"
      :loading="loading"
      :message="message"
      @group-action="deleteIsos"
      @handle-cancel="handleCancel"
      @close-modal="closeModal" />
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import OsLogo from '@/components/widgets/OsLogo'
import ResourceIcon from '@/components/view/ResourceIcon'
import BulkActionView from '@/components/view/BulkActionView'
import eventBus from '@/config/eventBus'

export default {
  name: 'IsoZones',
  components: {
    TooltipButton,
    OsLogo,
    ResourceIcon,
    BulkActionView
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
      selectedRowKeys: [],
      showGroupActionModal: false,
      selectedItems: [],
      selectedColumns: [],
      filterColumns: ['Status', 'Ready'],
      showConfirmationAction: false,
      redirectOnFinish: true,
      message: {
        title: this.$t('label.action.bulk.delete.isos'),
        confirmMessage: this.$t('label.confirm.delete.isos')
      },
      modalWidth: '30vw'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('copyIso')
  },
  created () {
    this.initForm()
    this.columns = [
      {
        key: 'zonename',
        title: this.$t('label.zonename'),
        dataIndex: 'zonename'
      },
      {
        title: this.$t('label.status'),
        dataIndex: 'status'
      },
      {
        key: 'isready',
        title: this.$t('label.isready'),
        dataIndex: 'isready'
      }
    ]
    if (this.isActionPermitted()) {
      this.columns.push({
        key: 'actions',
        title: '',
        dataIndex: 'actions',
        fixed: 'right',
        width: 100
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
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        zoneid: [{ type: 'array', required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      const params = {}
      params.id = this.resource.id
      params.isofilter = 'executable'
      params.listall = true
      params.page = this.page
      params.pagesize = this.pageSize

      this.dataSource = []
      this.itemCount = 0
      this.fetchLoading = true
      api('listIsos', params).then(json => {
        this.dataSource = json.listisosresponse.iso || []
        this.itemCount = json.listisosresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
      this.fetchZoneData()
    },
    fetchZoneIcon (zoneid) {
      const zoneItem = this.zones.filter(zone => zone.id === zoneid)
      if (zoneItem?.[0]?.icon?.base64image) {
        this.zoneIcon = zoneItem[0].icon.base64image
        return true
      }
      return false
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
        (this.resource.isready || !this.resource.status || this.resource.status.indexOf('Downloaded') === -1) && // Iso is ready or downloaded
        this.resource.account !== 'system'
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
      this.selectedItems = (this.dataSource.filter(function (item) {
        return selection.indexOf(item.zoneid) !== -1
      }))
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    bulkActionConfirmation () {
      this.showConfirmationAction = true
      this.selectedColumns = this.columns.filter(column => {
        return !this.filterColumns.includes(column.title)
      })
      this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
    },
    handleCancel () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.selectedColumns = []
      this.selectedRowKeys = []
      this.fetchData()
      if (this.dataSource.length === 0) {
        this.moveToPreviousView()
        this.redirectOnFinish = false
      }
    },
    async moveToPreviousView () {
      const lastPath = this.$router.currentRoute.value.fullPath
      const navigationResult = await this.$router.go(-1)
      if (navigationResult !== undefined || this.$router.currentRoute.value.fullPath === lastPath) {
        this.$router.go(-1)
      }
    },
    deleteIsos (e) {
      this.showConfirmationAction = false
      this.selectedColumns.splice(0, 0, {
        key: 'status',
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      if (this.selectedRowKeys.length > 0) {
        this.showGroupActionModal = true
      }
      for (const iso of this.selectedItems) {
        this.deleteIso(iso)
      }
    },
    deleteIso (record) {
      const params = {
        id: record.id,
        zoneid: record.zoneid
      }
      this.deleteLoading = true
      api('deleteIso', params).then(json => {
        const jobId = json.deleteisoresponse.jobid
        eventBus.emit('update-job-details', { jobId, resourceId: null })
        const singleZone = (this.dataSource.length === 1)
        this.redirectOnFinish = true
        this.$pollJob({
          jobId,
          title: this.$t('label.action.delete.iso'),
          description: this.resource.name,
          successMethod: result => {
            if (singleZone) {
              if (this.selectedItems.length === 0 && this.redirectOnFinish) {
                this.moveToPreviousView()
              }
            } else {
              if (this.selectedItems.length === 0) {
                this.fetchData()
              }
            }
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: record.zoneid, state: 'success' })
              if (this.selectedItems.length === this.zones.length && this.redirectOnFinish) {
                this.moveToPreviousView()
              }
            }
          },
          errorMethod: () => {
            if (this.selectedItems.length === 0) {
              this.fetchData()
            }
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: record.zoneid, state: 'failed' })
            }
          },
          showLoading: !(this.selectedItems.length > 0 && this.showGroupActionModal),
          loadingMessage: `${this.$t('label.deleting.iso')} ${this.resource.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          bulkAction: this.selectedItems.length > 0 && this.showGroupActionModal
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
      api('listZones', { showicon: true }).then(json => {
        const zones = json.listzonesresponse.zone || []
        this.zones = [...zones.filter((zone) => this.currentRecord.zoneid !== zone.id)]
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    showCopyIso (record) {
      this.currentRecord = record
      this.form.zoneid = []
      this.fetchZoneData()
      this.showCopyActionForm = true
    },
    onCloseCopyForm () {
      this.currentRecord = {}
      this.showCopyActionForm = false
    },
    handleCopyIsoSubmit (e) {
      e.preventDefault()
      if (this.copyLoading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {
          id: this.currentRecord.id,
          sourcezoneid: this.currentRecord.zoneid,
          destzoneids: values.zoneid.join()
        }
        this.copyLoading = true
        api('copyIso', params).then(json => {
          const jobId = json.copytemplateresponse.jobid
          eventBus.emit('update-job-details', { jobId, resourceId: null })
          this.$pollJob({
            jobId,
            title: this.$t('label.action.copy.iso'),
            description: this.resource.name,
            successMethod: result => {
              this.fetchData()
            },
            errorMethod: () => this.fetchData(),
            loadingMessage: `${this.$t('label.action.copy.iso')} ${this.resource.name} ${this.$t('label.in.progress')}`,
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
          this.onCloseCopyForm()
          this.fetchData()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.showConfirmationAction = false
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
