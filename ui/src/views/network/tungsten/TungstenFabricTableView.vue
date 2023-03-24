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
      :apiName="apiName"
      :loading="loading || fetchLoading"
      :resource="resource"
      :columns="columns"
      :dataSource="dataSource"
      :searchQuery="searchQuery"
      :page="page"
      :page-size="pageSize"
      :item-count="itemCount"
      :actions="detailAction"/>
    <div v-if="showAddModal">
      <keep-alive v-if="currentAction.component">
        <a-modal
          :visible="showAddModal"
          :closable="true"
          :maskClosable="false"
          style="top: 20px;"
          @cancel="closeAction"
          :confirmLoading="fetchLoading"
          :footer="null"
          centered
          width="auto"
        >
          <template #title>
            {{ $t(currentAction.title || currentAction.label) }}
          </template>
          <component
            :is="currentAction.component"
            :resource="resource"
            :loading="loading"
            :action="{currentAction}"
            v-bind="{currentAction}"
            @close-action="closeAction" />
        </a-modal>
      </keep-alive>
      <a-modal
        v-else
        :closable="true"
        :maskClosable="false"
        style="top: 20px;"
        :visible="showAddModal"
        :confirm-loading="fetchLoading"
        :footer="null"
        centered
        width="auto"
        @cancel="closeAction">
        <template #title>
          {{ $t(currentAction.label) }}
        </template>
        <div v-ctrl-enter="handleSubmit">
          <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical" class="form-layout">
            <div v-for="(field, index) in currentAction.fields" :key="field.name">
              <a-form-item
                :name="field.name"
                :ref="field.name"
                v-if="!currentAction.mapping || !field.name in currentAction.mapping">
                <template #label>
                  <tooltip-label
                    :title="'label' in field ? $t(field.label) : $t('label.' + field.name)"
                    :tooltip="apiParams[field.name].description" />
                </template>
                <a-select
                  v-if="field.type==='uuid'"
                  v-focus="index === 0"
                  :mode="field.multiple ? 'multiple' : null"
                  v-model:value="form[field.name]"
                  :loading="field.loading"
                  :placeholder="apiParams[field.name].description"
                  @change="(value) => handleChangeUuid(field.name, field.opts, value)">
                  <a-select-option v-for="opt in field.opts" :key="opt.uuid || opt.id || opt.name">
                    {{ opt.name || opt.displayName || opt.description }}
                  </a-select-option>
                </a-select>
                <a-input-number
                  style="width: 100%"
                  v-else-if="field.type === 'number'"
                  v-focus="index === 0"
                  v-model:value="form[field.name]"
                  :placeholder="apiParams[field.name].description"/>
                <a-input
                  v-else
                  v-focus="index === 0"
                  v-model:value="form[field.name]"
                  :placeholder="apiParams[field.name].description"/>
              </a-form-item>
            </div>

            <div :span="24" class="action-button">
              <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
              <a-button type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
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
      pageSize: this.$store.getters.defaultListViewPageSize,
      itemCount: 0,
      searchQuery: '',
      dataSource: [],
      fetchLoading: false,
      showAddModal: false,
      currentAction: {},
      listAction: [],
      detailAction: [],
      tagType: ''
    }
  },
  provide: function () {
    return {
      onFetchData: this.fetchData,
      onExecAction: this.execAction
    }
  },
  created () {
    this.listAction = this.actions.filter(action => action.listView)
    this.detailAction = this.actions.filter(action => action.dataView)
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
      this.rules = reactive({})

      if (!this.currentAction?.fields || this.currentAction.fields.length === 0) {
        return
      }
      this.currentAction.fields.forEach((field, index) => {
        this.form[field.name] = field?.value || null
        switch (field.type) {
          case 'uuid':
            if (field.multiple) this.form[field.name] = []
            this.rules[field.name] = [{ required: field.required, message: this.$t('message.error.select') }]
            break
          case 'number':
            this.rules[field.name] = [{ required: field.required, message: this.$t('message.error.required.input') }]
            break
          default:
            this.rules[field.name] = [{ required: field.required, message: this.$t('message.error.required.input') }]
            break
        }
      })
    },
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
      params.keyword = this.searchQuery
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
          params.listAll = true
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
      this.initForm()
      if (this.currentAction.component) {
        this.showAddModal = true
        return
      }
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
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        // this.fetchLoading = true
        const params = {}
        params.zoneid = this.resource.zoneid

        for (const key in values) {
          let inputVal = values[key]
          if (Array.isArray(values[key])) {
            inputVal = values[key].join(',')
          }
          if (key === 'tagtype') {
            params[key] = this.tagType
            continue
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

          let resourceName = values.name
          if (this.currentAction.api === 'createTungstenFabricTag') {
            resourceName = [values.tagtype, values.tagvalue].join('=')
          }

          this.$pollJob({
            jobId,
            title: this.$t(this.currentAction.label),
            description: resourceName,
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
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.fetchLoading = false
          this.closeAction()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
        const resourceName = record.name || record.uuid || record.id
        this.$pollJob({
          jobId,
          title: this.$t(this.currentAction.label),
          description: resourceName,
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
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    closeAction () {
      this.showAddModal = false
    },
    handleChangeUuid (field, data, uuid) {
      this.tagType = ''
      if (field !== 'tagtype') {
        return
      }
      const match = data.filter(item => item.uuid === uuid)
      if (match && match.length > 0) {
        this.tagType = match[0].name
      }
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
