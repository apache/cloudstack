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
        <a-col :span="14" style="padding-left: 6px">
          <breadcrumb :resource="resource">
            <a-tooltip placement="bottom" slot="end">
              <template slot="title">
                {{ "Refresh" }}
              </template>
              <a-button
                style="margin-top: 4px"
                :loading="loading"
                shape="round"
                size="small"
                icon="reload"
                @click="fetchData()">
                {{ "Refresh" }}
              </a-button>
            </a-tooltip>
          </breadcrumb>
        </a-col>
        <a-col :span="10">
          <span style="float: right">
            <action-button
              style="margin-bottom: 5px"
              :loading="loading"
              :actions="actions"
              :selectedRowKeys="selectedRowKeys"
              :dataView="dataView"
              :resource="resource"
              @exec-action="execAction"/>
            <a-input-search
              style="width: 20vw; margin-left: 10px"
              placeholder="Search"
              v-model="searchQuery"
              v-if="!dataView && !treeView"
              allowClear
              @search="onSearch" />
          </span>
        </a-col>
      </a-row>
    </a-card>

    <div v-show="showAction">
      <keep-alive v-if="currentAction.component">
        <a-modal
          :title="$t(currentAction.label)"
          :visible="showAction"
          :closable="true"
          style="top: 20px;"
          @cancel="closeAction"
          :confirmLoading="currentAction.loading"
          :footer="null"
          centered
          width="auto"
        >
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
        :confirmLoading="currentAction.loading"
        centered
      >
        <span slot="title">
          {{ $t(currentAction.label) }}
          <a
            v-if="currentAction.docHelp || $route.meta.docHelp"
            style="margin-left: 5px"
            :href="docBase + '/' + (currentAction.docHelp || $route.meta.docHelp)"
            target="_blank">
            <a-icon type="question-circle-o"></a-icon>
          </a>
        </span>
        <a-spin :spinning="currentAction.loading">
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
                {{ $t(field.name) }}
                <a-tooltip :title="field.description">
                  <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                </a-tooltip>
              </span>

              <span v-if="field.type==='boolean'">
                <a-switch
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please provide input' }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else-if="currentAction.mapping && field.name in currentAction.mapping && currentAction.mapping[field.name].options">
                <a-select
                  :loading="field.loading"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please select option' }]
                  }]"
                  :placeholder="field.description"
                >
                  <a-select-option v-for="(opt, optIndex) in currentAction.mapping[field.name].options" :key="optIndex">
                    {{ opt }}
                  </a-select-option>
                </a-select>
              </span>
              <span v-else-if="field.type==='uuid' || (field.name==='account' && !['addAccountToProject'].includes(currentAction.api)) || field.name==='keypair'">
                <a-select
                  showSearch
                  optionFilterProp="children"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please select option' }]
                  }]"
                  :loading="field.loading"
                  :placeholder="field.description"
                  :filterOption="(input, option) => {
                    return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                >
                  <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                    {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                  </a-select-option>
                </a-select>
              </span>
              <span v-else-if="field.type==='list'">
                <a-select
                  :loading="field.loading"
                  mode="multiple"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please select option' }]
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
                    rules: [{ required: field.required, message: 'Please enter a number' }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else-if="field.name==='password' || field.name==='currentpassword'">
                <a-input-password
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please enter input' }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else-if="field.name==='certificate' || field.name==='privatekey' || field.name==='certchain'">
                <a-textarea
                  rows="2"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please enter input' }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else>
                <a-input
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please enter input' }]
                  }]"
                  :placeholder="field.description" />
              </span>
            </a-form-item>
          </a-form>
        </a-spin>
      </a-modal>
    </div>

    <div v-if="dataView && !treeView">
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
        @refresh="this.fetchData"
        v-if="!treeView" />
      <a-pagination
        class="row-element"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `Total ${total} items`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger
        showQuickJumper
        v-if="!treeView" />
      <tree-view
        v-if="treeView"
        :treeData="treeData"
        :treeSelected="treeSelected"
        :loading="loading"
        :tabs="$route.meta.tabs"
        @change-resource="changeResource"
        :actionData="actionData"/>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import { genericCompare } from '@/utils/sort.js'
import config from '@/config/settings'
import store from '@/store'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import Status from '@/components/widgets/Status'
import ListView from '@/components/view/ListView'
import ResourceView from '@/components/view/ResourceView'
import TreeView from '@/components/view/TreeView'
import ActionButton from '@/components/view/ActionButton'

export default {
  name: 'Resource',
  components: {
    Breadcrumb,
    ChartCard,
    ResourceView,
    ListView,
    TreeView,
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
      docBase: config.docBase,
      loading: false,
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
      treeView: false,
      actions: [],
      treeData: [],
      treeSelected: {},
      actionData: []
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
        this.fetchData()
      }
    },
    '$i18n.locale' (to, from) {
      if (to !== from) {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
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
      this.treeData = []
      this.treeSelected = {}
      var params = { listall: true }
      if (Object.keys(this.$route.query).length > 0) {
        Object.assign(params, this.$route.query)
      } else if (this.$route.meta.params) {
        Object.assign(params, this.$route.meta.params)
      }

      this.treeView = this.$route && this.$route.meta && this.$route.meta.treeView

      if (this.$route && this.$route.params && this.$route.params.id) {
        this.resource = {}
        this.dataView = true
        this.treeView = false
      } else {
        this.dataView = false
      }

      if (this.$route && this.$route.meta && this.$route.meta.permission) {
        this.apiName = this.$route.meta.permission[0]
        if (this.$route.meta.columns) {
          this.columnKeys = this.$route.meta.columns
        }

        if (this.$route.meta.actions) {
          this.actions = this.$route.meta.actions
        }
      }

      if (this.apiName === '' || this.apiName === undefined) {
        return
      }

      if (this.searchQuery !== '') {
        if (this.apiName === 'listRoles') {
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
          title: this.$t(key),
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

      if (!this.treeView) {
        params.page = this.page
        params.pagesize = this.pageSize
      } else {
        const domainId = this.$store.getters.userInfo.domainid
        params.id = domainId
        delete params.treeView
      }

      api(this.apiName, params).then(json => {
        var responseName
        var objectName
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
        this.items = json[responseName][objectName]
        if (!this.items || this.items.length === 0) {
          this.items = []
        }
        if (this.treeView) {
          this.treeData = this.generateTreeData(this.items)
        } else {
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
        }
        if (this.items.length > 0) {
          this.resource = this.items[0]
          this.treeSelected = this.treeView ? this.items[0] : {}
        } else {
          this.resource = {}
          this.treeSelected = {}
        }
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description'],
          duration: 0
        })

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
      this.currentAction.loading = false
      this.showAction = false
      this.currentAction = {}
    },
    execAction (action) {
      this.actionData = []
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
      if (action.args && action.args.length > 0) {
        this.currentAction.paramFields = action.args.map(function (arg) {
          return paramFields.filter(function (param) {
            return param.name.toLowerCase() === arg.toLowerCase()
          })[0]
        })
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
      this.currentAction.loading = false
    },
    listUuidOpts (param) {
      if (this.currentAction.mapping && param.name in this.currentAction.mapping && !this.currentAction.mapping[param.name].api) {
        return
      }
      var paramName = param.name
      var params = { listall: true }
      const possibleName = 'list' + paramName.replace('ids', '').replace('id', '').toLowerCase() + 's'
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
    pollActionCompletion (jobId, action) {
      this.$pollJob({
        jobId,
        successMethod: result => {
          this.fetchData()
          if (action.response) {
            const description = action.response(result.jobresult)
            if (description) {
              this.$notification.info({
                message: action.label,
                description: (<span domPropsInnerHTML={description}></span>),
                duration: 0
              })
            }
          }
        },
        errorMethod: () => this.fetchData(),
        loadingMessage: `${this.$t(action.label)} in progress for ${this.resource.name}`,
        catchMessage: 'Error encountered while fetching async job result',
        action
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        console.log(values)
        if (!err) {
          this.currentAction.loading = true
          const params = {}
          if ('id' in this.resource && this.currentAction.params.map(i => { return i.name }).includes('id')) {
            params.id = this.resource.id
          }
          for (const key in values) {
            const input = values[key]
            for (const param of this.currentAction.params) {
              if (param.name === key) {
                if (input === undefined) {
                  if (param.type === 'boolean') {
                    params[key] = false
                  }
                  break
                }
                if (this.currentAction.mapping && key in this.currentAction.mapping && this.currentAction.mapping[key].options) {
                  params[key] = this.currentAction.mapping[key].options[input]
                } else if (param.type === 'uuid') {
                  params[key] = param.opts[input].id
                } else if (param.type === 'list') {
                  params[key] = input.map(e => { return param.opts[e].id }).reduce((str, name) => { return str + ',' + name })
                } else if (param.name === 'account' || param.name === 'keypair') {
                  if (['addAccountToProject'].includes(this.currentAction.api)) {
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

          var hasJobId = false
          api(this.currentAction.api, params).then(json => {
            // set action data for reload tree-view
            if (this.treeView) {
              this.actionData.push(json)
            }

            for (const obj in json) {
              if (obj.includes('response')) {
                for (const res in json[obj]) {
                  if (res === 'jobid') {
                    this.$store.dispatch('AddAsyncJob', { title: this.$t(this.currentAction.label), jobid: json[obj][res], description: this.resource.name, status: 'progress' })
                    this.pollActionCompletion(json[obj][res], this.currentAction)
                    hasJobId = true
                    break
                  }
                }
                break
              }
            }
            if (this.currentAction.icon === 'delete' && this.dataView) {
              this.$router.go(-1)
            } else {
              if (!hasJobId) {
                this.fetchData()
              }
            }
          }).catch(error => {
            console.log(error)
            this.$notification.error({
              message: 'Request Failed',
              description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
            })
          }).finally(f => {
            this.closeAction()
          })
        }
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
    start () {
      this.loading = true
      this.fetchData()
      setTimeout(() => {
        this.loading = false
        this.selectedRowKeys = []
      }, 1000)
    },
    generateTreeData (treeData) {
      const result = []
      const rootItem = treeData

      rootItem[0].title = rootItem[0].title ? rootItem[0].title : rootItem[0].name
      rootItem[0].key = rootItem[0].id ? rootItem[0].id : 0

      if (!rootItem[0].haschild) {
        rootItem[0].isLeaf = true
      }

      result.push(rootItem[0])
      return result
    },
    changeResource (resource) {
      this.treeSelected = resource
      this.resource = this.treeSelected
    },
    toggleLoading () {
      this.loading = !this.loading
    },
    startLoading () {
      this.loading = true
    },
    finishLoading () {
      this.loading = false
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

.ant-breadcrumb .anticon {
  margin-left: 8px;
}
</style>
