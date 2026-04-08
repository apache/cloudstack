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
    <div class="add-row">
      <a-form
        :ref="addFormRef"
        :model="addFilterForm"
        :rules="addFormRules"
        @finish="addFilter"
        layout="vertical"
        class="add-filter-form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item name="mode" ref="mode">
              <template #label>
                <tooltip-label :title="$t('label.mode')" :tooltip="addFilterApiParams.mode.description"/>
              </template>
              <a-radio-group v-model:value="addFilterForm.mode" button-style="solid">
                <a-radio-button value="include">{{ $t('label.include') }}</a-radio-button>
                <a-radio-button value="exclude">{{ $t('label.exclude') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item name="matchtype" ref="matchtype">
              <template #label>
                <tooltip-label :title="$t('label.matchtype')" :tooltip="addFilterApiParams.matchtype.description"/>
              </template>
              <a-select
                style="margin-left: 0"
                v-model:value="addFilterForm.matchtype"
                placeholder="Select match type"
                allow-clear>
                <a-select-option value="exact">{{ $t('label.exact') }}</a-select-option>
                <a-select-option value="prefix">{{ $t('label.prefix') }}</a-select-option>
                <a-select-option value="suffix">{{ $t('label.suffix') }}</a-select-option>
                <a-select-option value="contains">{{ $t('label.contains') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" style="margin-top: 8px;">
          <a-col :span="24">
            <a-form-item name="value" ref="value">
              <template #label>
                <tooltip-label :title="$t('label.value')" :tooltip="addFilterApiParams.value.description"/>
              </template>
              <a-input v-model:value="addFilterForm.value" placeholder="Enter filter value" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="margin-top: 16px;">
          <a-col :span="24" style="text-align: right;">
            <a-space>
              <a-button @click="resetAddFilterForm">{{ $t('label.reset') }}</a-button>
              <a-button type="primary" ref="submit" @click="addFilter">{{ $t('label.add') }}</a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <a-divider />
    <a-button
      v-if="('deleteWebhookFilter' in $store.getters.apis) && (selectedRowKeys && selectedRowKeys.length > 0)"
      type="danger"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="clearOrDeleteFiltersConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ (selectedRowKeys && selectedRowKeys.length > 0) ? $t('label.action.delete.webhook.filters') : $t('label.action.clear.webhook.filters') }}
    </a-button>
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="filters"
      :actions="actions"
      :columnKeys="columnKeys"
      :explicitlyAllowRowSelection="true"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData"
      @selection-change="updateSelectedRows"/>
    <a-pagination
      class="row-element"
      style="margin-top: 10px"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="pageSizeOptions"
      @change="changePage"
      @showSizeChange="changePage"
      showSizeChanger
      showQuickJumper>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import { genericCompare } from '@/utils/sort.js'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import ListView from '@/components/view/ListView'

export default {
  name: 'WebhookFiltersTab',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    ListView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['value', 'type', 'mode', 'matchtype'],
      selectedColumnKeys: ['value', 'mode', 'matchtype'],
      selectedRowKeys: [],
      columns: [],
      cols: [],
      filters: [],
      actions: [
        {
          api: 'deleteWebhookFilter',
          icon: 'delete-outlined',
          label: 'label.delete.webhook.filter',
          message: 'message.delete.webhook.filter',
          dataView: true,
          popup: true
        }
      ],
      page: 1,
      pageSize: 20,
      totalCount: 0
    }
  },
  computed: {
    pageSizeOptions () {
      var sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
  beforeCreate () {
    this.addFilterApiParams = this.$getApiParams('addWebhookFilter')
  },
  created () {
    this.updateColumns()
    this.pageSize = this.pageSizeOptions[0] * 1
    this.initAddFilterForm()
    this.fetchData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      if ('listview' in this.$refs && this.$refs.listview) {
        this.$refs.listview.resetSelection()
      }
      this.fetchFilters()
    },
    fetchFilters () {
      this.filters = []
      if (!this.resource.id) {
        return
      }
      const params = {
        page: this.page,
        pagesize: this.pageSize,
        webhookid: this.resource.id,
        listall: true
      }
      this.tabLoading = true
      getAPI('listWebhookFilters', params).then(json => {
        this.filters = []
        this.totalCount = json?.listwebhookfiltersresponse?.count || 0
        this.filters = json?.listwebhookfiltersresponse?.webhookfilter || []
        this.tabLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchFilters()
    },
    updateSelectedColumns (key) {
      if (this.selectedColumnKeys.includes(key)) {
        this.selectedColumnKeys = this.selectedColumnKeys.filter(x => x !== key)
      } else {
        this.selectedColumnKeys.push(key)
      }
      this.updateColumns()
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        const key = columnKey
        if (!this.selectedColumnKeys.includes(key)) continue
        var title = this.$t('label.' + String(key).toLowerCase())
        this.columns.push({
          key: key,
          title: title,
          dataIndex: key,
          sorter: (a, b) => { return genericCompare(a[key] || '', b[key] || '') }
        })
      }
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    initAddFilterForm () {
      this.addFormRef = ref()
      this.addFilterForm = reactive({
        mode: 'include',
        matchtype: 'exact',
        value: null
      })
      this.addFormRules = reactive({
        value: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    resetAddFilterForm () {
      if (this.addFormRef.value) {
        this.addFormRef.value.resetFields()
      }
    },
    addFilter (e) {
      e.preventDefault()
      if (this.tabLoading) return
      console.log('Adding webhook filter with form:', this.addFilterForm)
      this.addFormRef.value.validate().then(() => {
        const formRaw = toRaw(this.addFilterForm)
        const values = this.handleRemoveFields(formRaw)
        const params = {
          webhookid: this.resource.id,
          mode: values.mode,
          matchtype: values.matchtype,
          value: values.value
        }
        console.log('Adding webhook filter with params:', params)
        this.tabLoading = true
        postAPI('addWebhookFilter', params).then(json => {
          this.$notification.success({
            message: this.$t('label.add.webhook.filter'),
            description: this.$t('message.success.add.webhook.filter')
          })
          setTimeout(() => {
            this.resetAddFilterForm()
          }, 250)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.tabLoading = false
          this.fetchFilters()
        })
      })
    },
    updateSelectedRows (keys) {
      this.selectedRowKeys = keys
    },
    clearOrDeleteFiltersConfirmation () {
      const self = this
      const title = (this.selectedRowKeys && this.selectedRowKeys.length > 0)
        ? this.$t('label.action.delete.webhook.filters')
        : this.$t('label.action.clear.webhook.filters')
      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          if (self.selectedRowKeys && self.selectedRowKeys.length > 0) {
            self.deletedSelectedFilters()
            return
          }
          self.clearFilters()
        }
      })
    },
    deletedSelectedFilters () {
      const promises = []
      this.selectedRowKeys.forEach(id => {
        const params = {
          id: id
        }
        promises.push(new Promise((resolve, reject) => {
          postAPI('deleteWebhookFilter', params).then(json => {
            return resolve(id)
          }).catch(error => {
            return reject(error)
          })
        }))
      })
      const msg = this.$t('label.action.delete.webhook.filters')
      this.$message.info({
        content: msg,
        duration: 3
      })
      this.tabLoading = true
      Promise.all(promises).finally(() => {
        this.tabLoading = false
        this.fetchData()
      })
    },
    clearFilters () {
      const params = {
        webhookid: this.resource.id
      }
      this.tabLoading = true
      postAPI('deleteWebhookFilter', params).then(json => {
        this.$message.success(this.$t('message.success.clear.webhook.filters'))
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    deleteFilterConfirmation (item) {
      const self = this
      this.$confirm({
        title: this.$t('label.delete.webhook.filter'),
        okText: this.$t('label.ok'),
        okType: 'primary',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.deleteFilter(item)
        }
      })
    },
    deleteFilter (item) {
      const params = {
        id: item.id
      }
      this.tabLoading = true
      postAPI('deleteWebhookFilter', params).then(json => {
        const message = `${this.$t('message.success.delete')} ${this.$t('label.webhook.filter')}`
        this.$message.success(message)
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    execAction (action) {
      if (action.api === 'deleteWebhookFilter') {
        this.deleteFilterConfirmation(action.resource)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.ant-tag {
  padding: 0 7px 0 0;
}
.ant-select {
  margin-left: 10px;
}
.info-icon {
  margin: 0 10px 0 5px;
}
.filter-row {
  margin-bottom: 2.5%;
}
.filter-row-inner {
  margin-top: 3%;
}
</style>
