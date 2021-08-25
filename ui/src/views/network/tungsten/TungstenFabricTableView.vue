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
    <tungsten-network-action
      :actions="listAction"
      :page="page"
      :pageSize="pageSize" />
    <tungsten-network-table
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :searchQuery="searchQuery"
      :page="page"
      :page-size="pageSize"
      :item-count="itemCount"
      :actions="detailAction"/>
    <a-modal
      v-if="showAddModal"
      :closable="true"
      :maskClosable="false"
      style="top: 20px;"
      :visible="showAddModal"
      :confirm-loading="fetchLoading"
      :footer="null"
      centered
      width="auto"
      @cancel="closeAction">
      <span slot="title">
        {{ $t(currentAction.label) }}
      </span>
      <a-form :form="form" layout="vertical" class="form-layout">
        <a-form-item
          v-for="(field, index) in currentAction.fields"
          :key="field.name"
          v-if="!currentAction.mapping || !field.name in currentAction.mapping">
          <tooltip-label
            slot="label"
            :title="$t('label.' + field.name)"
            :tooltip="apiParams[field.name].description" />
          <a-select
            v-if="field.type==='uuid'"
            :auto-focus="index === 0"
            :mode="field.multiple ? 'multiple' : 'default'"
            v-decorator="[field.name, {
              rules: [{
                required: field.required,
                message: $t('message.error.required.input')
              }]
            }]"
            :loading="field.loading"
            :placeholder="apiParams[field.name].description">
            <a-select-option v-for="opt in field.opts" :key="opt.uuid || opt.id || opt.name">
              {{ opt.name || opt.displayName || opt.description }}
            </a-select-option>
          </a-select>
          <a-input-number
            style="width: 100%"
            v-else-if="field.type === 'number'"
            :auto-focus="index === 0"
            v-decorator="[field.name, {
              initialValue: 0,
              rules: [{
                required: field.required,
                message: $t('message.error.required.input')
              }]
            }]"
            :placeholder="apiParams[field.name].description"/>
          <a-input
            v-else
            :auto-focus="index === 0"
            v-decorator="[field.name, {
              rules: [{
                required: field.required,
                message: $t('message.error.required.input')
              }]
            }]"
            :placeholder="apiParams[field.name].description"/>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import TungstenNetworkAction from '@/views/network/tungsten/TungstenNetworkAction'
import TungstenNetworkTable from '@/views/network/tungsten/TungstenNetworkTable'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'TungstenFabricTableView',
  components: {
    TungstenNetworkAction,
    TungstenNetworkTable,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    apiName: {
      type: String,
      required: true
    },
    actions: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      page: 1,
      pageSize: 20,
      itemCount: 0,
      searchQuery: '',
      dataSource: [],
      fetchLoading: false,
      showAddModal: false,
      currentAction: {},
      listAction: [],
      detailAction: []
    }
  },
  provide: function () {
    return {
      onFetchData: this.fetchData,
      onExecAction: this.execAction
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.listAction = this.actions.filter(action => action.listView)
    this.detailAction = this.actions.filter(action => action.dataView)
    this.fetchData()
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  methods: {
    fetchData (args = {}) {
      if (!this.resource || !this.resource.zoneid) {
        return false
      }
      const params = {}
      if (Object.keys(args).length > 0) {
        this.page = args.page || 1
        this.pageSize = args.pageSize || 20
        this.searchQuery = args.searchQuery || ''
      }
      params.listAll = true
      params.page = this.page
      params.pagesize = this.pageSize
      params.query = this.searchQuery
      params.zoneid = this.resource.zoneid

      this.itemCount = 0
      this.dataSource = []
      this.fetchLoading = true

      api(this.apiName, params).then(json => {
        let responseName
        let objectName
        for (const key in json) {
          if (key.includes('response')) {
            responseName = key
            break
          }
        }
        for (const key in json[responseName]) {
          if (key === 'count') {
            this.itemCount = json[responseName].count
            continue
          }
          objectName = key
          break
        }
        this.dataSource = json[responseName][objectName] || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchOptions (record) {
      const uuidApiFields = this.currentAction.fields.filter(field => field.type === 'uuid' && field.api)
      const uuidFields = this.currentAction.fields.filter(field => field.type === 'uuid' && typeof field.optGet === 'function')
      if (uuidApiFields.length > 0) {
        uuidApiFields.forEach(field => {
          const params = {}
          params.zoneid = this.resource.zoneid
          if (field.args) {
            for (const arg in field.args) {
              if (typeof field.args[arg] === 'function') {
                params[arg] = field.args[arg](record)
              }
            }
          }

          const fieldIndex = this.currentAction.fields.findIndex(item => item.name === field.name)
          this.currentAction.fields[fieldIndex].loading = true

          api(field.api, params).then(json => {
            let responseName
            let objectName
            for (const key in json) {
              if (key.includes('response')) {
                responseName = key
                break
              }
            }
            for (const key in json[responseName]) {
              if (key === 'count') {
                continue
              }
              objectName = key
              break
            }
            this.currentAction.fields[fieldIndex].opts = json[responseName][objectName] || []
          }).finally(() => {
            this.currentAction.fields[fieldIndex].loading = false
          })
        })
      }
      if (uuidFields.length > 0) {
        uuidFields.forEach(field => {
          const fieldIndex = this.currentAction.fields.findIndex(item => item.name === field.name)
          this.currentAction.fields[fieldIndex].opts = this.currentAction.fields[fieldIndex].optGet(record) || []
        })
      }
    },
    execAction (action, record) {
      this.currentAction = action
      this.currentAction.record = record
      if (this.currentAction.popup) {
        this.fetchOptions(record)
        this.apiParams = this.$getApiParams(this.currentAction.api)
        this.showAddModal = true
        return
      }
      if (this.currentAction.confirm) {
        const self = this
        this.$confirm({
          title: this.$t(this.currentAction.message),
          okText: this.$t('label.ok'),
          okType: 'danger',
          cancelText: this.$t('label.cancel'),
          onOk () {
            self.onRemoveAction(record)
          }
        })
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((error, values) => {
        if (error) {
          return false
        }

        // this.fetchLoading = true
        const params = {}
        params.zoneid = this.resource.zoneid

        for (const key in values) {
          let inputVal = values[key]
          if (Array.isArray(values[key])) {
            inputVal = values[key].join(',')
          }
          params[key] = inputVal
        }
        if (this.currentAction.args) {
          for (const arg in this.currentAction.args) {
            if (typeof this.currentAction.args[arg] === 'function') {
              params[arg] = this.currentAction.args[arg](this.currentAction.record)
            }
          }
        }
        api(this.currentAction.api, params).then(json => {
          const jsonResponseName = [this.currentAction.api, 'response'].join('').toLowerCase()
          const jobId = json[jsonResponseName].jobid
          if (!jobId) {
            this.fetchData()
            return
          }
          this.$pollJob({
            jobId,
            title: this.$t(this.currentAction.label),
            description: values.name,
            successMessage: `${this.$t('label.success')}`,
            successMethod: () => {
              this.fetchData()
            },
            loadingMessage: `${this.$t(this.currentAction.label)} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            action: {
              isFetchData: false
            }
          })
        }).finally(() => {
          this.fetchLoading = false
          this.closeAction()
        })
      })
    },
    onRemoveAction (record) {
      const params = {}
      params.zoneid = this.resource.zoneid
      for (const field in this.currentAction.args) {
        if (typeof this.currentAction.args[field] === 'function') {
          params[field] = this.currentAction.args[field](record)
        }
      }

      this.fetchLoading = true
      api(this.currentAction.api, params).then(json => {
        const jsonResponseName = [this.currentAction.api, 'response'].join('').toLowerCase()
        const jobId = json[jsonResponseName].jobid
        this.$pollJob({
          jobId,
          title: this.$t(this.currentAction.label),
          description: record.name,
          successMessage: `${this.$t('label.success')}`,
          successMethod: () => {
            this.fetchData()
          },
          loadingMessage: `${this.$t(this.currentAction.label)} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action: {
            isFetchData: false
          }
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    closeAction () {
      this.showAddModal = false
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;

  @media (min-width: 600px) {
    width: 450px;
  }
}
</style>
