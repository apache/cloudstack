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
                @click="fetchData({ irefresh: true })">
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
                  v-if="!dataView && filters && filters.length > 0"
                  :placeholder="$t('label.filterby')"
                  :value="$route.query.filter || (projectView && $route.name === 'vm' ||
                    ['Admin', 'DomainAdmin'].includes($store.getters.userInfo.roletype) && ['vm', 'iso', 'template'].includes($route.name)
                    ? 'all' : ['guestnetwork'].includes($route.name) ? 'all' : 'self')"
                  style="min-width: 100px; margin-left: 10px"
                  @change="changeFilter">
                  <a-icon slot="suffixIcon" type="filter" />
                  <a-select-option v-if="['Admin', 'DomainAdmin'].includes($store.getters.userInfo.roletype) && ['vm', 'iso', 'template'].includes($route.name)" key="all">
                    {{ $t('label.all') }}
                  </a-select-option>
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
          <slot name="action" v-if="dataView && $route.path.startsWith('/publicip')"></slot>
          <action-button
            v-else
            :style="dataView ? { float: device === 'mobile' ? 'left' : 'right' } : { 'margin-right': '10px', display: 'inline-flex' }"
            :loading="loading"
            :actions="actions"
            :selectedRowKeys="selectedRowKeys"
            :dataView="dataView"
            :resource="resource"
            @exec-action="(action) => execAction(action, action.groupAction && !dataView)"/>
          <search-view
            v-if="!dataView"
            :searchFilters="searchFilters"
            :searchParams="searchParams"
            :apiName="apiName"/>
        </a-col>
      </a-row>
    </a-card>

    <div v-show="showAction">
      <keep-alive v-if="currentAction.component && (!currentAction.groupAction || this.selectedRowKeys.length === 0)">
        <a-modal
          :visible="showAction"
          :closable="true"
          :maskClosable="false"
          :cancelText="$t('label.cancel')"
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
        :maskClosable="false"
        :okText="$t('label.ok')"
        :cancelText="$t('label.cancel')"
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
                  :autoFocus="fieldIndex === firstIndex"
                />
              </span>
              <span v-else-if="currentAction.mapping && field.name in currentAction.mapping && currentAction.mapping[field.name].options">
                <a-select
                  :loading="field.loading"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.select')}` }]
                  }]"
                  :placeholder="field.description"
                  :autoFocus="fieldIndex === firstIndex"
                >
                  <a-select-option key="" >{{ }}</a-select-option>
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
                  :autoFocus="fieldIndex === firstIndex"
                >
                  <a-select-option key="">{{ }}</a-select-option>
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
                  :autoFocus="fieldIndex === firstIndex"
                >
                  <a-select-option key="">{{ }}</a-select-option>
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
                  :autoFocus="fieldIndex === firstIndex"
                >
                  <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                    {{ opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description }}
                  </a-select-option>
                </a-select>
              </span>
              <span v-else-if="field.type==='long'">
                <a-input-number
                  :autoFocus="fieldIndex === firstIndex"
                  style="width: 100%;"
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
                  :autoFocus="fieldIndex === firstIndex"
                />
              </span>
              <span v-else-if="field.name==='certificate' || field.name==='privatekey' || field.name==='certchain'">
                <a-textarea
                  rows="2"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: `${$t('message.error.required.input')}` }]
                  }]"
                  :placeholder="field.description"
                  :autoFocus="fieldIndex === firstIndex"
                />
              </span>
              <span v-else>
                <a-input
                  :autoFocus="fieldIndex === firstIndex"
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
      <slot name="resource" v-if="$route.path.startsWith('/quotasummary') || $route.path.startsWith('/publicip')"></slot>
      <resource-view
        v-else
        :resource="resource"
        :loading="loading"
        :tabs="$route.meta.tabs" />
    </div>
    <div class="row-element" v-else>
      <list-view
        :loading="loading"
        :columns="columns"
        :items="items"
        :actions="actions"
        ref="listview"
        @selection-change="onRowSelectionChange"
        @refresh="this.fetchData" />
      <a-pagination
        class="row-element"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="device === 'desktop' ? ['20', '50', '100', '200'] : ['10', '20', '50', '100', '200']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger
        showQuickJumper>
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import { genericCompare } from '@/utils/sort.js'
import store from '@/store'
import eventBus from '@/config/eventBus'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import Status from '@/components/widgets/Status'
import ListView from '@/components/view/ListView'
import ResourceView from '@/components/view/ResourceView'
import ActionButton from '@/components/view/ActionButton'
import SearchView from '@/components/view/SearchView'

export default {
  name: 'Resource',
  components: {
    Breadcrumb,
    ChartCard,
    ResourceView,
    ListView,
    Status,
    ActionButton,
    SearchView
  },
  mixins: [mixinDevice],
  provide: function () {
    return {
      parentFetchData: this.fetchData,
      parentToggleLoading: this.toggleLoading,
      parentStartLoading: this.startLoading,
      parentFinishLoading: this.finishLoading,
      parentSearch: this.onSearch,
      parentChangeFilter: this.changeFilter,
      parentChangeResource: this.changeResource,
      parentPollActionCompletion: this.pollActionCompletion,
      parentEditTariffAction: () => {}
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
      resource: {},
      selectedRowKeys: [],
      currentAction: {},
      showAction: false,
      dataView: false,
      projectView: false,
      selectedFilter: '',
      filters: [],
      searchFilters: [],
      searchParams: {},
      actions: [],
      formModel: {},
      confirmDirty: false,
      firstIndex: 0
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    eventBus.$on('vm-refresh-data', () => {
      if (this.$route.path === '/vm' || this.$route.path.includes('/vm/')) {
        this.fetchData()
      }
    })
    eventBus.$on('async-job-complete', (action) => {
      if (this.$route.path.includes('/vm/')) {
        if (action && 'api' in action && ['destroyVirtualMachine'].includes(action.api)) {
          return
        }
      }
      this.fetchData()
    })
    eventBus.$on('exec-action', (action, isGroupAction) => {
      this.execAction(action, isGroupAction)
    })

    if (this.device === 'desktop') {
      this.pageSize = 20
    }
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
        if ('page' in to.query) {
          this.page = Number(to.query.page)
          this.pageSize = Number(to.query.pagesize)
        } else {
          this.page = 1
          this.pageSize = (this.device === 'desktop' ? 20 : 10)
        }
        this.itemCount = 0
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
        this.$message.success(`${this.$t('message.switch.to')} "${project.name}"`)
        const query = Object.assign({}, this.$route.query)
        delete query.projectid
        this.$router.replace({ query })
      })
    },
    fetchData (params = {}) {
      if (this.$route.name === 'deployVirtualMachine') {
        return
      }
      if (this.routeName !== this.$route.name) {
        this.routeName = this.$route.name
        this.items = []
      }
      if (!this.routeName) {
        this.routeName = this.$route.matched[this.$route.matched.length - 1].parent.name
      }
      this.apiName = ''
      this.actions = []
      this.columns = []
      this.columnKeys = []
      const refreshed = ('irefresh' in params)

      params.listall = true
      if (this.$route.meta.params) {
        Object.assign(params, this.$route.meta.params)
      }
      if (['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype) &&
        'templatefilter' in params && this.routeName === 'template') {
        params.templatefilter = 'all'
      }
      if (['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype) &&
        'isofilter' in params && this.routeName === 'iso') {
        params.isofilter = 'all'
      }
      if (Object.keys(this.$route.query).length > 0) {
        if ('page' in this.$route.query) {
          this.page = Number(this.$route.query.page)
        }
        if ('pagesize' in this.$route.query) {
          this.pagesize = Number(this.$route.query.pagesize)
        }
        Object.assign(params, this.$route.query)
      }
      delete params.q
      delete params.filter
      delete params.irefresh

      this.searchFilters = this.$route && this.$route.meta && this.$route.meta.searchFilters
      this.filters = this.$route && this.$route.meta && this.$route.meta.filters
      if (typeof this.filters === 'function') {
        this.filters = this.filters()
      }

      this.projectView = Boolean(store.getters.project && store.getters.project.id)

      if ((this.$route && this.$route.params && this.$route.params.id) || this.$route.query.dataView) {
        this.dataView = true
        if (!refreshed) {
          this.resource = {}
          this.$emit('change-resource', this.resource)
        }
      } else {
        this.dataView = false
      }

      if ('listview' in this.$refs && this.$refs.listview) {
        this.$refs.listview.resetSelection()
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
        let key = columnKey
        let title = columnKey
        if (typeof columnKey === 'object') {
          if ('customTitle' in columnKey && 'field' in columnKey) {
            key = columnKey.field
            title = columnKey.customTitle
            customRender[key] = columnKey[key]
          } else {
            key = Object.keys(columnKey)[0]
            title = Object.keys(columnKey)[0]
            customRender[key] = columnKey[key]
          }
        }
        this.columns.push({
          title: this.$t('label.' + String(title).toLowerCase()),
          dataIndex: key,
          scopedSlots: { customRender: key },
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') }
        })
      }

      if (['listTemplates', 'listIsos'].includes(this.apiName) && this.dataView) {
        delete params.showunique
      }

      this.loading = true
      if (this.$route.params && this.$route.params.id) {
        params.id = this.$route.params.id
        if (this.$route.path.startsWith('/ssh/')) {
          params.name = this.$route.params.id
        } else if (this.$route.path.startsWith('/vmsnapshot/')) {
          params.vmsnapshotid = this.$route.params.id
        } else if (this.$route.path.startsWith('/ldapsetting/')) {
          params.hostname = this.$route.params.id
        }
      }

      params.page = this.page
      params.pagesize = this.pageSize
      this.searchParams = params
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

        if (this.apiName === 'listProjects' && this.items.length > 0) {
          this.columns.map(col => {
            if (col.title === 'Account') {
              col.title = this.$t('label.project.owner')
            }
          })
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
          this.$emit('change-resource', this.resource)
        } else {
          if (this.dataView) {
            this.$router.push({ path: '/exception/404' })
          }
        }
      }).catch(error => {
        if ([401].includes(error.response.status)) {
          return
        }

        if (Object.keys(this.searchParams).length > 0) {
          this.itemCount = 0
          this.items = []
          this.$message.error({
            content: error.response.headers['x-description'],
            duration: 5
          })
          return
        }

        this.$notifyError(error)

        if ([405].includes(error.response.status)) {
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
    closeAction () {
      this.actionLoading = false
      this.showAction = false
      this.currentAction = {}
    },
    onRowSelectionChange (selection) {
      this.selectedRowKeys = selection
    },
    execAction (action, isGroupAction) {
      const self = this
      this.form = this.$form.createForm(this)
      this.formModel = {}
      if (action.component && action.api && !action.popup) {
        this.$router.push({ name: action.api })
        return
      }
      this.currentAction = action
      this.currentAction.params = store.getters.apis[this.currentAction.api].params
      this.resource = action.resource
      this.$emit('change-resource', this.resource)
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
      if ('message' in action) {
        var message = action.message
        if (typeof action.message === 'function') {
          message = action.message(action.resource)
        }
        action.message = message
      }
      if ('args' in action) {
        var args = action.args
        if (typeof action.args === 'function') {
          args = action.args(action.resource, this.$store.getters, isGroupAction)
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
      this.getFirstIndexFocus()

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
      if (action.dataView && ['copy', 'edit', 'share-alt'].includes(action.icon)) {
        this.fillEditFormFieldValues()
      }
    },
    getFirstIndexFocus () {
      this.firstIndex = 0
      for (let fieldIndex = 0; fieldIndex < this.currentAction.paramFields.length; fieldIndex++) {
        const field = this.currentAction.paramFields[fieldIndex]
        if (!(this.currentAction.mapping && field.name in this.currentAction.mapping && this.currentAction.mapping[field.name].value)) {
          this.firstIndex = fieldIndex
          break
        }
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
              if (this.currentAction.mapping && this.currentAction.mapping[param.name] && this.currentAction.mapping[param.name].filter) {
                const filter = this.currentAction.mapping[param.name].filter
                param.opts = json[obj][res].filter(filter)
              }
              if (['listTemplates', 'listIsos'].includes(possibleApi)) {
                param.opts = [...new Map(param.opts.map(x => [x.id, x])).values()]
              }
              break
            }
            break
          }
        }
        this.$forceUpdate()
      }).catch(function (error) {
        console.log(error)
        param.loading = false
      }).then(function () {
      })
    },
    pollActionCompletion (jobId, action, resourceName, showLoading = true) {
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
          if ('successMethod' in action) {
            action.successMethod(this, result)
          }
        },
        errorMethod: () => this.fetchData(),
        loadingMessage: `${this.$t(action.label)} - ${resourceName}`,
        showLoading: showLoading,
        catchMessage: this.$t('error.fetching.async.job.result'),
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
      if (!this.dataView && this.currentAction.groupAction && this.selectedRowKeys.length > 0) {
        this.form.validateFields((err, values) => {
          if (!err) {
            this.actionLoading = true
            const itemsNameMap = {}
            this.items.map(x => {
              itemsNameMap[x.id] = x.name || x.displaytext || x.id
            })
            const paramsList = this.currentAction.groupMap(this.selectedRowKeys, values)
            for (const params of paramsList) {
              var resourceName = itemsNameMap[params.id]
              // Using a method for this since it's an async call and don't want wrong prarms to be passed
              this.callGroupApi(params, resourceName)
            }
            this.$message.info({
              content: this.$t(this.currentAction.label),
              key: this.currentAction.label,
              duration: 3
            })
            setTimeout(() => {
              this.actionLoading = false
              this.closeAction()
              this.fetchData()
            }, 500)
          }
        })
      } else {
        this.execSubmit(e)
      }
    },
    callGroupApi (params, resourceName) {
      const action = this.currentAction
      api(action.api, params).then(json => {
        this.handleResponse(json, resourceName, action, false)
      }).catch(error => {
        if ([401].includes(error.response.status)) {
          return
        }
        this.$notifyError(error)
      })
    },
    handleResponse (response, resourceName, action, showLoading = true) {
      for (const obj in response) {
        if (obj.includes('response')) {
          if (response[obj].jobid) {
            const jobid = response[obj].jobid
            this.$store.dispatch('AddAsyncJob', { title: this.$t(action.label), jobid: jobid, description: resourceName, status: 'progress' })
            this.pollActionCompletion(jobid, action, resourceName, showLoading)
            return true
          } else {
            var message = action.successMessage ? this.$t(action.successMessage) : this.$t(action.label) +
              (resourceName ? ' - ' + resourceName : '')
            var duration = 2
            if (action.additionalMessage) {
              message = message + ' - ' + this.$t(action.successMessage)
              duration = 5
            }
            this.$message.success({
              content: message,
              key: action.label + resourceName,
              duration: duration
            })
          }
          break
        }
      }
      if (['addLdapConfiguration', 'deleteLdapConfiguration'].includes(action.api)) {
        this.$store.dispatch('UpdateConfiguration')
      }
      return false
    },
    execSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const params = {}
        const action = this.currentAction
        if ('id' in this.resource && action.params.map(i => { return i.name }).includes('id')) {
          params.id = this.resource.id
        }
        for (const key in values) {
          const input = values[key]
          for (const param of action.params) {
            if (param.name !== key) {
              continue
            }
            if (!input === undefined || input === null ||
              (input === '' && !['updateStoragePool', 'updateHost', 'updatePhysicalNetwork', 'updateDiskOffering', 'updateNetworkOffering'].includes(action.api))) {
              if (param.type === 'boolean') {
                params[key] = false
              }
              break
            }
            if (!input && input !== 0 && !['tags'].includes(key)) {
              continue
            }
            if (action.mapping && key in action.mapping && action.mapping[key].options) {
              params[key] = action.mapping[key].options[input]
            } else if (param.type === 'list') {
              params[key] = input.map(e => { return param.opts[e].id }).reduce((str, name) => { return str + ',' + name })
            } else if (param.name === 'account' || param.name === 'keypair') {
              if (['addAccountToProject', 'createAccount'].includes(action.api)) {
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

        for (const key in action.defaultArgs) {
          if (!params[key]) {
            params[key] = action.defaultArgs[key]
          }
        }

        if (!this.projectView || !['uploadSslCert'].includes(action.api)) {
          if (action.mapping) {
            for (const key in action.mapping) {
              if (!action.mapping[key].value) {
                continue
              }
              params[key] = action.mapping[key].value(this.resource, params)
            }
          }
        }

        const resourceName = params.displayname || params.displaytext || params.name || params.hostname || params.username ||
          params.ipaddress || params.virtualmachinename || this.resource.name || this.resource.ipaddress || this.resource.id

        var hasJobId = false
        this.actionLoading = true
        let args = null
        if (action.post) {
          args = [action.api, {}, 'POST', params]
        } else {
          args = [action.api, params]
        }
        api(...args).then(json => {
          hasJobId = this.handleResponse(json, resourceName, action)
          if ((action.icon === 'delete' || ['archiveEvents', 'archiveAlerts', 'unmanageVirtualMachine'].includes(action.api)) && this.dataView) {
            this.$router.go(-1)
          } else {
            if (!hasJobId) {
              this.fetchData()
            }
          }
          this.closeAction()
        }).catch(error => {
          if ([401].includes(error.response.status)) {
            return
          }

          console.log(error)
          this.$notifyError(error)
        }).finally(f => {
          this.actionLoading = false
        })
      })
    },
    changeFilter (filter) {
      const query = Object.assign({}, this.$route.query)
      delete query.templatefilter
      delete query.isofilter
      delete query.account
      delete query.domainid
      delete query.state
      if (this.$route.name === 'template') {
        query.templatefilter = filter
      } else if (this.$route.name === 'iso') {
        query.isofilter = filter
      } else if (this.$route.name === 'guestnetwork') {
        if (filter === 'all') {
          delete query.type
        } else {
          query.type = filter
        }
      } else if (this.$route.name === 'vm') {
        if (filter === 'self') {
          query.account = this.$store.getters.userInfo.account
          query.domainid = this.$store.getters.userInfo.domainid
        } else if (['running', 'stopped'].includes(filter)) {
          query.state = filter
        }
      }
      query.filter = filter
      query.page = 1
      query.pagesize = this.pageSize
      this.$router.push({ query })
    },
    onSearch (opts) {
      const query = Object.assign({}, this.$route.query)
      for (const key in this.searchParams) {
        delete query[key]
      }
      delete query.name
      delete query.templatetype
      delete query.keyword
      delete query.q
      this.searchParams = {}
      if (opts && Object.keys(opts).length > 0) {
        this.searchParams = opts
        if ('searchQuery' in opts) {
          const value = opts.searchQuery
          if (value && value.length > 0) {
            if (this.$route.name === 'role') {
              query.name = value
            } else if (this.$route.name === 'quotaemailtemplate') {
              query.templatetype = value
            } else if (this.$route.name === 'globalsetting') {
              query.name = value
            } else {
              query.keyword = value
            }
            query.q = value
          }
          this.searchParams = {}
        } else {
          Object.assign(query, opts)
        }
      }
      query.page = 1
      query.pagesize = this.pageSize
      if (JSON.stringify(query) === JSON.stringify(this.$route.query)) {
        this.fetchData(query)
        return
      }
      this.$router.push({ query })
    },
    changePage (page, pageSize) {
      const query = Object.assign({}, this.$route.query)
      query.page = page
      query.pagesize = pageSize
      this.$router.push({ query })
    },
    changePageSize (currentPage, pageSize) {
      const query = Object.assign({}, this.$route.query)
      query.page = currentPage
      query.pagesize = pageSize
      this.$router.push({ query })
    },
    changeResource (resource) {
      this.resource = resource
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
