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
    <a-card class="breadcrumb-card">
      <a-row>
        <a-col :span="device === 'mobile' ? 24 : 12" style="padding-left: 12px">
          <breadcrumb :resource="resource">
            <span slot="end">
              <a-button
                :loading="loading"
                style="margin-bottom: 5px"
                shape="round"
                size="small"
                icon="reload"
                @click="fetchData()">
                {{ $t('label.refresh') }}
              </a-button>
              <a-switch
                v-if="!dataView && ['vm', 'volume', 'zone', 'cluster', 'host', 'storagepool'].includes($route.name)"
                style="margin-left: 8px"
                :checked-children="$t('label.metrics')"
                :un-checked-children="$t('label.metrics')"
                :checked="$store.getters.metrics"
                @change="(checked, event) => { $store.dispatch('SetMetrics', checked) }"/>
              <a-tooltip placement="right">
                <template slot="title">
                  {{ $t('label.filterby') }}
                </template>
                <a-select
                  v-if="filters && filters.length > 0"
                  :placeholder="$t('label.filterby')"
                  :value="$t('label.' + selectedFilter)"
                  style="min-width: 100px; margin-left: 10px"
                  @change="changeFilter">
                  <a-icon slot="suffixIcon" type="filter" />
                  <a-select-option v-for="filter in filters" :key="filter">
                    {{ $t('label.' + filter) }}
                  </a-select-option>
                </a-select>
              </a-tooltip>
            </span>
          </breadcrumb>
        </a-col>
        <a-col
          :span="device === 'mobile' ? 24 : 12"
          :style="device === 'mobile' ? { float: 'right', 'margin-top': '12px', 'margin-bottom': '-6px', display: 'table' } : { float: 'right', display: 'table', 'margin-bottom': '-6px' }" >
          <action-button
            :style="dataView ? { float: device === 'mobile' ? 'left' : 'right' } : { 'margin-right': '10px', display: 'inline-flex' }"
            :loading="loading"
            :actions="actions"
            :selectedRowKeys="selectedRowKeys"
            :dataView="dataView"
            :resource="resource"
            @exec-action="execAction"/>
          <a-input-search
            v-if="!dataView"
            style="width: 100%; display: table-cell"
            :placeholder="$t('label.search')"
            v-model="searchQuery"
            allowClear
            @search="onSearch" />
        </a-col>
      </a-row>
    </a-card>

    <div v-show="showAction">
      <keep-alive v-if="currentAction.component">
        <a-modal
          :visible="showAction"
          :closable="true"
          style="top: 20px;"
          @cancel="closeAction"
          :confirmLoading="actionLoading"
          :footer="null"
          centered
          width="auto"
        >
          <span slot="title">
            {{ $t(currentAction.label) }}
            <a
              v-if="currentAction.docHelp || $route.meta.docHelp"
              style="margin-left: 5px"
              :href="$config.docBase + '/' + (currentAction.docHelp || $route.meta.docHelp)"
              target="_blank">
              <a-icon type="question-circle-o"></a-icon>
            </a>
          </span>
          <component
            :is="currentAction.component"
            :resource="resource"
            :loading="loading"
            :action="{currentAction}"
            v-bind="{currentAction}"
            @refresh-data="fetchData"
            @poll-action="pollActionCompletion"
            @close-action="closeAction"/>
        </a-modal>
      </keep-alive>
      <a-modal
        v-else
        :visible="showAction"
        :closable="true"
        style="top: 20px;"
        @ok="handleSubmit"
        @cancel="closeAction"
        :confirmLoading="actionLoading"
        centered
      >
        <span slot="title">
          {{ $t(currentAction.label) }}
          <a
            v-if="currentAction.docHelp || $route.meta.docHelp"
            style="margin-left: 5px"
            :href="$config.docBase + '/' + (currentAction.docHelp || $route.meta.docHelp)"
            target="_blank">
            <a-icon type="question-circle-o"></a-icon>
          </a>
        </span>
        <a-spin :spinning="actionLoading">
          <span v-if="currentAction.message">
            <a-alert type="warning">
              <span slot="message" v-html="$t(currentAction.message)" />
            </a-alert>
            <br v-if="currentAction.paramFields.length > 0"/>
          </span>
          <a-form
            :form="form"
            @submit="handleSubmit"
            layout="vertical" >
            <a-form-item
              v-for="(field, fieldIndex) in currentAction.paramFields"
              :key="fieldIndex"
              :v-bind="field.name"
              v-if="!(currentAction.mapping && field.name in currentAction.mapping && currentAction.mapping[field.name].value)"
            >
              <span slot="label">
                {{ $t('label.' + field.name) }}
                <a-tooltip :title="field.description">
                  <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                </a-tooltip>
              </span>

              <span v-if="field.type==='boolean'">
                <a-switch
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.required.input')}` }]
                  }]"
                  v-model="formModel[field.name]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else-if="currentAction.mapping && field.name in currentAction.mapping && currentAction.mapping[field.name].options">
                <a-select
                  :loading="field.loading"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.select')}` }]
                  }]"
                  :placeholder="field.description"
                >
                  <a-select-option :key="null">{{ }}</a-select-option>
                  <a-select-option v-for="(opt, optIndex) in currentAction.mapping[field.name].options" :key="optIndex">
                    {{ opt }}
                  </a-select-option>
                </a-select>
              </span>
              <span
                v-else-if="field.name==='keypair' ||
                  (field.name==='account' && !['addAccountToProject', 'createAccount'].includes(currentAction.api))">
                <a-select
                  showSearch
                  optionFilterProp="children"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.select')}` }]
                  }]"
                  :loading="field.loading"
                  :placeholder="field.description"
                  :filterOption="(input, option) => {
                    return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                >
                  <a-select-option :key="null">{{ }}</a-select-option>
                  <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                    {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                  </a-select-option>
                </a-select>
              </span>
              <span
                v-else-if="field.type==='uuid'">
                <a-select
                  showSearch
                  optionFilterProp="children"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.select')}` }]
                  }]"
                  :loading="field.loading"
                  :placeholder="field.description"
                  :filterOption="(input, option) => {
                    return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                >
                  <a-select-option :key="null">{{ }}</a-select-option>
                  <a-select-option v-for="opt in field.opts" :key="opt.id">
                    {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                  </a-select-option>
                </a-select>
              </span>
              <span v-else-if="field.type==='list'">
                <a-select
                  :loading="field.loading"
                  mode="multiple"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.select')}` }]
                  }]"
                  :placeholder="field.description"
                >
                  <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                    {{ opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </span>
              <span v-else-if="field.type==='long'">
                <a-input-number
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.validate.number')}` }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else-if="field.name==='password' || field.name==='currentpassword' || field.name==='confirmpassword'">
                <a-input-password
                  v-decorator="[field.name, {
                    rules: [
                      {
                        required: field.required,
                        message: `${$t('message.error.required.input')}`
                      },
                      {
                        validator: validateTwoPassword
                      }
                    ]
                  }]"
                  :placeholder="field.description"
                  @blur="($event) => handleConfirmBlur($event, field.name)"
                />
              </span>
              <span v-else-if="field.name==='certificate' || field.name==='privatekey' || field.name==='certchain'">
                <a-textarea
                  rows="2"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.required.input')}` }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else>
                <a-input
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.required.input')}` }]
                  }]"
                  :placeholder="field.description" />
              </span>
            </a-form-item>
          </a-form>
        </a-spin>
      </a-modal>
    </div>

    <div v-if="dataView">
      <resource-view
        :resource="resource"
        :loading="loading"
        :tabs="$route.meta.tabs" />
    </div>
    <div class="row-element" v-else>
      <list-view
        :loading="loading"
        :columns="columns"
        :items="items"
        @refresh="this.fetchData" />
      <a-pagination
        class="row-element"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `Total ${total} items`"
        :pageSizeOptions="['10', '20', '40', '80', '100', '500']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger
        showQuickJumper />
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import { genericCompare } from '@/utils/sort.js'
import store from '@/store'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import Status from '@/components/widgets/Status'
import ListView from '@/components/view/ListView'
import ResourceView from '@/components/view/ResourceView'
import ActionButton from '@/components/view/ActionButton'

export default {
  name: 'Resource',
  components: {
    Breadcrumb,
    ChartCard,
    ResourceView,
    ListView,
    Status,
    ActionButton
  },
  mixins: [mixinDevice],
  provide: function () {
    return {
      parentFetchData: this.fetchData,
      parentToggleLoading: this.toggleLoading,
      parentStartLoading: this.startLoading,
      parentFinishLoading: this.finishLoading
    }
  },
  data () {
    return {
      apiName: '',
      loading: false,
      actionLoading: false,
      columns: [],
      items: [],
      itemCount: 0,
      page: 1,
      pageSize: 10,
      searchQuery: '',
      resource: {},
      selectedRowKeys: [],
      currentAction: {},
      showAction: false,
      dataView: false,
      selectedFilter: '',
      filters: [],
      actions: [],
      formModel: {},
      confirmDirty: false
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.currentPath = this.$route.fullPath
    this.fetchData()
    if ('projectid' in this.$route.query) {
      this.switchProject(this.$route.query.projectid)
    }
  },
  beforeRouteUpdate (to, from, next) {
    this.currentPath = this.$route.fullPath
    next()
  },
  beforeRouteLeave (to, from, next) {
    this.currentPath = this.$route.fullPath
    next()
  },
  watch: {
    '$route' (to, from) {
      if (to.fullPath !== from.fullPath && !to.fullPath.includes('action/')) {
        this.searchQuery = ''
        this.page = 1
        this.itemCount = 0
        this.selectedFilter = ''
        this.fetchData()
        if ('projectid' in to.query) {
          this.switchProject(to.query.projectid)
        }
      }
    },
    '$i18n.locale' (to, from) {
      if (to !== from) {
        this.fetchData()
      }
    },
    '$store.getters.metrics' (oldVal, newVal) {
      this.fetchData()
    }
  },
  methods: {
    switchProject (projectId) {
      if (!projectId || !projectId.length || projectId.length !== 36) {
        return
      }
      api('listProjects', { id: projectId, listall: true, details: 'min' }).then(json => {
        if (!json || !json.listprojectsresponse || !json.listprojectsresponse.project) return
        const project = json.listprojectsresponse.project[0]
        this.$store.dispatch('SetProject', project)
        this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
        this.$message.success(`Switched to "${project.name}"`)
        const query = Object.assign({}, this.$route.query)
        delete query.projectid
        this.$router.replace({ query })
      })
    },
    fetchData (params = { listall: true }) {
      if (this.routeName !== this.$route.name) {
        this.routeName = this.$route.name
        this.items = []
      }
      if (!this.routeName) {
        this.routeName = this.$route.matched[this.$route.matched.length - 1].parent.name
      }
      this.apiName = ''
      this.actions = []
      this.filters = this.$route.meta.filters || []
      this.columns = []
      this.columnKeys = []
      if (Object.keys(this.$route.query).length > 0) {
        Object.assign(params, this.$route.query)
      } else if (this.$route.meta.params) {
        Object.assign(params, this.$route.meta.params)
      }

      if (this.$route && this.$route.params && this.$route.params.id) {
        this.resource = {}
        this.dataView = true
      } else {
        this.dataView = false
      }

      if (this.$route && this.$route.meta && this.$route.meta.permission) {
        this.apiName = this.$route.meta.permission[0]
        if (this.$route.meta.columns) {
          const columns = this.$route.meta.columns
          if (columns && typeof columns === 'function') {
            this.columnKeys = columns()
          } else {
            this.columnKeys = columns
          }
        }

        if (this.$route.meta.actions) {
          this.actions = this.$route.meta.actions
        }
      }

      if (this.apiName === '' || this.apiName === undefined) {
        return
      }

      if (['listTemplates', 'listIsos', 'listVirtualMachinesMetrics'].includes(this.apiName) && !this.dataView) {
        if (['Admin'].includes(this.$store.getters.userInfo.roletype) || this.apiName === 'listVirtualMachinesMetrics') {
          this.filters = ['all', ...this.filters]
          if (this.selectedFilter === '') {
            this.selectedFilter = 'all'
          }
        }
        if (this.selectedFilter === '') {
          this.selectedFilter = 'self'
        }
      }

      if (this.selectedFilter && this.filters.length > 0) {
        if (this.$route.path.startsWith('/template')) {
          params.templatefilter = this.selectedFilter
        } else if (this.$route.path.startsWith('/iso')) {
          params.isofilter = this.selectedFilter
        } else if (this.$route.path.startsWith('/vm')) {
          if (this.selectedFilter === 'self') {
            params.account = this.$store.getters.userInfo.account
            params.domainid = this.$store.getters.userInfo.domainid
          } else if (['running', 'stopped'].includes(this.selectedFilter)) {
            params.state = this.selectedFilter
          }
        }
      }

      if (this.searchQuery !== '') {
        if (this.apiName === 'listRoles') {
          params.name = this.searchQuery
        } else if (this.apiName === 'quotaEmailTemplateList') {
          params.templatetype = this.searchQuery
        } else if (this.apiName === 'listConfigurations') {
          params.name = this.searchQuery
        } else {
          params.keyword = this.searchQuery
        }
      }

      if (!this.columnKeys || this.columnKeys.length === 0) {
        for (const field of store.getters.apis[this.apiName].response) {
          this.columnKeys.push(field.name)
        }
        this.columnKeys = [...new Set(this.columnKeys)]
        this.columnKeys.sort(function (a, b) {
          if (a === 'name' && b !== 'name') { return -1 }
          if (a < b) { return -1 }
          if (a > b) { return 1 }
          return 0
        })
      }

      const customRender = {}
      for (var columnKey of this.columnKeys) {
        var key = columnKey
        if (typeof columnKey === 'object') {
          key = Object.keys(columnKey)[0]
          customRender[key] = columnKey[key]
        }
        this.columns.push({
          title: this.$t('label.' + String(key).toLowerCase()),
          dataIndex: key,
          scopedSlots: { customRender: key },
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        })
      }

      this.loading = true
      if (this.$route.params && this.$route.params.id) {
        params.id = this.$route.params.id
        if (this.$route.path.startsWith('/ssh/')) {
          params.name = this.$route.params.id
        } else if (this.$route.path.startsWith('/ldapsetting/')) {
          params.hostname = this.$route.params.id
        }
      }

      params.page = this.page
      params.pagesize = this.pageSize

      api(this.apiName, params).then(json => {
        var responseName
        var objectName
        for (const key in json) {
          if (key.includes('response')) {
            responseName = key
            break
          }
        }
        this.itemCount = 0
        for (const key in json[responseName]) {
          if (key === 'count') {
            this.itemCount = json[responseName].count
            continue
          }
          objectName = key
          break
        }
        this.items = json[responseName][objectName]
        if (!this.items || this.items.length === 0) {
          this.items = []
        }
        if (['listTemplates', 'listIsos'].includes(this.apiName) && this.items.length > 1) {
          this.items = [...new Map(this.items.map(x => [x.id, x])).values()]
        }
        for (let idx = 0; idx < this.items.length; idx++) {
          this.items[idx].key = idx
          for (const key in customRender) {
            const func = customRender[key]
            if (func && typeof func === 'function') {
              this.items[idx][key] = func(this.items[idx])
            }
          }
          if (this.$route.path.startsWith('/ssh')) {
            this.items[idx].id = this.items[idx].name
          } else if (this.$route.path.startsWith('/ldapsetting')) {
            this.items[idx].id = this.items[idx].hostname
          }
        }
        if (this.items.length > 0) {
          this.resource = this.items[0]
        }
      }).catch(error => {
        this.$notifyError(error)

        if ([401, 405].includes(error.response.status)) {
          this.$router.push({ path: '/exception/403' })
        }

        if ([430, 431, 432].includes(error.response.status)) {
          this.$router.push({ path: '/exception/404' })
        }

        if ([530, 531, 532, 533, 534, 535, 536, 537].includes(error.response.status)) {
          this.$router.push({ path: '/exception/500' })
        }
      }).finally(f => {
        this.loading = false
      })
    },
    onSearch (value) {
      this.searchQuery = value
      this.page = 1
      this.fetchData()
    },
    closeAction () {
      this.actionLoading = false
      this.showAction = false
      this.currentAction = {}
    },
    execAction (action) {
      const self = this
      this.form = this.$form.createForm(this)
      this.formModel = {}
      if (action.component && action.api && !action.popup) {
        this.$router.push({ name: action.api })
        return
      }
      this.currentAction = action
      this.currentAction.params = store.getters.apis[this.currentAction.api].params
      var paramFields = this.currentAction.params
      paramFields.sort(function (a, b) {
        if (a.name === 'name' && b.name !== 'name') { return -1 }
        if (a.name !== 'name' && b.name === 'name') { return -1 }
        if (a.name === 'id') { return -1 }
        if (a.name < b.name) { return -1 }
        if (a.name > b.name) { return 1 }
        return 0
      })
      this.currentAction.paramFields = []
      if ('args' in action) {
        var args = action.args
        if (typeof action.args === 'function') {
          args = action.args(action.resource, this.$store.getters)
        }
        if (args.length > 0) {
          this.currentAction.paramFields = args.map(function (arg) {
            if (arg === 'confirmpassword') {
              return {
                type: 'password',
                name: 'confirmpassword',
                required: true,
                description: self.$t('label.confirmpassword.description')
              }
            }
            return paramFields.filter(function (param) {
              return param.name.toLowerCase() === arg.toLowerCase()
            })[0]
          })
        }
      }

      this.showAction = true
      for (const param of this.currentAction.paramFields) {
        if (param.type === 'list' && ['tags', 'hosttags'].includes(param.name)) {
          param.type = 'string'
        }
        if (param.type === 'uuid' || param.type === 'list' || param.name === 'account' || (this.currentAction.mapping && param.name in this.currentAction.mapping)) {
          this.listUuidOpts(param)
        }
      }
      this.actionLoading = false
      if (action.dataView && ['copy', 'edit'].includes(action.icon)) {
        this.fillEditFormFieldValues()
      }
    },
    listUuidOpts (param) {
      if (this.currentAction.mapping && param.name in this.currentAction.mapping && !this.currentAction.mapping[param.name].api) {
        return
      }
      var paramName = param.name
      var extractedParamName = paramName.replace('ids', '').replace('id', '').toLowerCase()
      var params = { listall: true }
      const possibleName = 'list' + extractedParamName + 's'
      var possibleApi
      if (this.currentAction.mapping && param.name in this.currentAction.mapping && this.currentAction.mapping[param.name].api) {
        possibleApi = this.currentAction.mapping[param.name].api
        if (this.currentAction.mapping[param.name].params) {
          const customParams = this.currentAction.mapping[param.name].params(this.resource)
          if (customParams) {
            params = { ...params, ...customParams }
          }
        }
      } else if (paramName === 'id') {
        possibleApi = this.apiName
      } else {
        for (const api in store.getters.apis) {
          if (api.toLowerCase().startsWith(possibleName)) {
            possibleApi = api
            break
          }
        }
      }
      if (!possibleApi) {
        return
      }
      param.loading = true
      param.opts = []
      if (possibleApi === 'listTemplates') {
        params.templatefilter = 'executable'
      } else if (possibleApi === 'listIsos') {
        params.isofilter = 'executable'
      } else if (possibleApi === 'listHosts') {
        params.type = 'routing'
      }
      api(possibleApi, params).then(json => {
        param.loading = false
        for (const obj in json) {
          if (obj.includes('response')) {
            for (const res in json[obj]) {
              if (res === 'count') {
                continue
              }
              param.opts = json[obj][res]
              if (['listTemplates', 'listIsos'].includes(possibleApi)) {
                param.opts = [...new Map(param.opts.map(x => [x.id, x])).values()]
              }
              this.$forceUpdate()
              break
            }
            break
          }
        }
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      }).then(function () {
      })
    },
    pollActionCompletion (jobId, action, resourceName) {
      this.$pollJob({
        jobId,
        name: resourceName,
        successMethod: result => {
          this.fetchData()
          if (action.response) {
            const description = action.response(result.jobresult)
            if (description) {
              this.$notification.info({
                message: this.$t(action.label),
                description: (<span domPropsInnerHTML={description}></span>),
                duration: 0
              })
            }
          }
        },
        errorMethod: () => this.fetchData(),
        loadingMessage: `${this.$t(action.label)} - ${resourceName}`,
        catchMessage: 'Error encountered while fetching async job result',
        action
      })
    },
    fillEditFormFieldValues () {
      const form = this.form
      this.currentAction.paramFields.map(field => {
        let fieldValue = null
        let fieldName = null
        if (field.type === 'list' || field.name === 'account') {
          fieldName = field.name.replace('ids', 'name').replace('id', 'name')
        } else {
          fieldName = field.name
        }
        fieldValue = this.resource[fieldName] ? this.resource[fieldName] : null
        if (fieldValue) {
          form.getFieldDecorator(field.name, { initialValue: fieldValue })
          this.formModel[field.name] = fieldValue
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        console.log(values)
        if (!err) {
          const params = {}
          if ('id' in this.resource && this.currentAction.params.map(i => { return i.name }).includes('id')) {
            params.id = this.resource.id
          }
          for (const key in values) {
            const input = values[key]
            for (const param of this.currentAction.params) {
              if (param.name !== key) {
                continue
              }
              if (input === undefined || input === null) {
                if (param.type === 'boolean') {
                  params[key] = false
                }
                break
              }
              if (this.currentAction.mapping && key in this.currentAction.mapping && this.currentAction.mapping[key].options) {
                params[key] = this.currentAction.mapping[key].options[input]
              } else if (param.type === 'list') {
                params[key] = input.map(e => { return param.opts[e].id }).reduce((str, name) => { return str + ',' + name })
              } else if (param.name === 'account' || param.name === 'keypair') {
                if (['addAccountToProject', 'createAccount'].includes(this.currentAction.api)) {
                  params[key] = input
                } else {
                  params[key] = param.opts[input].name
                }
              } else {
                params[key] = input
              }
              break
            }
          }

          for (const key in this.currentAction.defaultArgs) {
            if (!params[key]) {
              params[key] = this.currentAction.defaultArgs[key]
            }
          }

          if (this.currentAction.mapping) {
            for (const key in this.currentAction.mapping) {
              if (!this.currentAction.mapping[key].value) {
                continue
              }
              params[key] = this.currentAction.mapping[key].value(this.resource, params)
            }
          }

          console.log(this.currentAction)
          console.log(this.resource)
          console.log(params)

          const resourceName = params.displayname || params.displaytext || params.name || params.hostname || params.username || params.ipaddress || params.virtualmachinename || this.resource.name

          var hasJobId = false
          this.actionLoading = true
          api(this.currentAction.api, params).then(json => {
            for (const obj in json) {
              if (obj.includes('response')) {
                for (const res in json[obj]) {
                  if (res === 'jobid') {
                    this.$store.dispatch('AddAsyncJob', { title: this.$t(this.currentAction.label), jobid: json[obj][res], description: resourceName, status: 'progress' })
                    this.pollActionCompletion(json[obj][res], this.currentAction, resourceName)
                    hasJobId = true
                    break
                  } else {
                    this.$message.success({
                      content: this.$t(this.currentAction.label) + (resourceName ? ' - ' + resourceName : ''),
                      key: this.currentAction.label + resourceName,
                      duration: 2
                    })
                  }
                }
                break
              }
            }
            if ((this.currentAction.icon === 'delete' || ['archiveEvents', 'archiveAlerts'].includes(this.currentAction.api)) && this.dataView) {
              this.$router.go(-1)
            } else {
              if (!hasJobId) {
                this.fetchData()
              }
            }
          }).catch(error => {
            console.log(error)
            this.$notifyError(error)
          }).finally(f => {
            this.actionLoading = false
            this.closeAction()
          })
        }
      })
    },
    changeFilter (filter) {
      this.selectedFilter = filter
      this.page = 1
      this.fetchData()
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
    start () {
      this.loading = true
      this.fetchData()
      setTimeout(() => {
        this.loading = false
        this.selectedRowKeys = []
      }, 1000)
    },
    toggleLoading () {
      this.loading = !this.loading
    },
    startLoading () {
      this.loading = true
    },
    finishLoading () {
      this.loading = false
    },
    handleConfirmBlur (e, name) {
      if (name !== 'confirmpassword') {
        return
      }
      const value = e.target.value
      this.confirmDirty = this.confirmDirty || !!value
    },
    validateTwoPassword (rule, value, callback) {
      if (!value || value.length === 0) {
        callback()
      } else if (rule.field === 'confirmpassword') {
        const form = this.form
        const messageConfirm = this.$t('message.validate.equalto')
        const passwordVal = form.getFieldValue('password')
        if (passwordVal && passwordVal !== value) {
          callback(messageConfirm)
        } else {
          callback()
        }
      } else if (rule.field === 'password') {
        const form = this.form
        const confirmPasswordVal = form.getFieldValue('confirmpassword')
        if (!confirmPasswordVal || confirmPasswordVal.length === 0) {
          callback()
        } else if (value && this.confirmDirty) {
          form.validateFields(['confirmpassword'], { force: true })
          callback()
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
  }
}
</script>

<style scoped>

.breadcrumb-card {
  margin-left: -24px;
  margin-right: -24px;
  margin-top: -16px;
  margin-bottom: 12px;
}

.row-element {
  margin-top: 10px;
  margin-bottom: 10px;
}

.ant-breadcrumb {
  vertical-align: text-bottom;
}
</style>
