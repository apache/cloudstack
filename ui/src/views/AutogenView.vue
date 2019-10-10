<template>
  <div>
    <breadcrumb v-if="device !== 'desktop'" class="mobile-breadcrumb" />
    <a-row>
      <a-col :span="17">
        <a-tooltip placement="bottom" v-for="(action, actionIndex) in actions" :key="actionIndex" v-if="action.api in $store.getters.apis && ((!dataView && (action.listView || action.groupAction && selectedRowKeys.length > 0)) || (dataView && action.dataView))">
          <template slot="title">
            {{ action.label }}
          </template>
          <a-button
            :icon="action.icon"
            :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
            shape="circle"
            style="margin-right: 5px"
            @click="execAction(action)"
            :disabled="'hidden' in action ? dataView && action.hidden(resource) : false"
          >
          </a-button>
        </a-tooltip>
        <span v-if="!dataView" style="float: right; padding-right: 8px; margin-top: -2px">
          <a-tooltip placement="bottom">
            <template slot="title">
              {{ "Auto-Refresh" }}
            </template>
            <a-switch
              style="margin: 8px;"
              :loading="loading"
              :checked="autoRefresh"
              @change="toggleAutoRefresh"
            />
          </a-tooltip>
          <a-tooltip placement="bottom">
            <template slot="title">
              {{ "Refresh" }}
            </template>
            <a-button
              @click="fetchData()"
              :loading="loading"
              shape="circle"
              icon="reload"
            />
          </a-tooltip>
          <a-button-group style="margin-left: 10px" v-if="!dataView">
            <a-button icon="bars" @click="tableView=true" />
            <a-button icon="appstore" @click="tableView=false" />
          </a-button-group>
        </span>
      </a-col>
      <a-col :span="7">
        <a-tooltip placement="bottom" v-if="dataView">
          <template slot="title">
            {{ "Refresh" }}
          </template>
          <a-button
            style="float: right"
            @click="fetchData()"
            :loading="loading"
            shape="circle"
            icon="reload"
          />
        </a-tooltip>
        <a-tooltip placement="bottom" v-if="dataView">
          <template slot="title">
            {{ "Auto-Refresh" }}
          </template>
          <a-switch
            v-if="dataView"
            style="float: right; margin: 5px;"
            :loading="loading"
            :checked="autoRefresh"
            @change="toggleAutoRefresh"
          />
        </a-tooltip>

        <a-input-search
          size="default"
          placeholder="Search"
          v-model="searchQuery"
          v-if="!dataView"
          @search="onSearch"
        >
          <a-icon slot="prefix" type="search" />
        </a-input-search>
      </a-col>
    </a-row>

    <div v-show="showAction">
      <keep-alive v-if="currentAction.component">
        <a-modal
          :title="currentAction.label"
          :visible="showAction"
          :closable="true"
          style="top: 20px;"
          @cancel="closeAction"
          :confirmLoading="currentAction.loading"
          :footer="null"
          centered
        >
          <component :is="currentAction.component"/></component>
        </a-modal>
      </keep-alive>
      <a-modal
        v-else
        :title="currentAction.label"
        :visible="showAction"
        :closable="true"
        style="top: 20px;"
        @ok="handleSubmit"
        @cancel="closeAction"
        :confirmLoading="currentAction.loading"
        centered
      >
        <a-spin :spinning="currentAction.loading">
          <a-form
            :form="form"
            @submit="handleSubmit"
            layout="vertical" >
            <a-form-item
              v-for="(field, fieldIndex) in currentAction.params"
              :key="fieldIndex"
              :label="$t(field.name)"
              :v-bind="field.name"
              v-if="field.name !== 'id'"
            >

              <span v-if="field.type==='boolean'">
                <a-switch
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please provide input' }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
              <span v-else-if="field.type==='uuid' || field.name==='account'">
                <a-select
                  :loading="field.loading"
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please select option' }]
                  }]"
                  :placeholder="field.description"
                >
                  <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                    {{ opt.name || opt.description }}
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
              <span v-else>
                <a-input
                  v-decorator="[field.name, {
                    rules: [{ required: field.required, message: 'Please enter input' }]
                  }]"
                  :placeholder="field.description"
                />
              </span>
            </a-form-item>
          </a-form>
        </a-spin>
      </a-modal>
    </div>

    <div v-if="dataView">
      <component :is="$route.meta.viewComponent" :resource="resource" v-if="$route.meta.viewComponent"/></component>
      <detail-view :resource="resource" v-else />
    </div>
    <div class="row-element" v-else>
      <list-view
        :loading="loading"
        :columns="columns"
        :items="items"
        v-show="tableView" />
      <card-view
        :items="items"
        :loading="loading"
        v-show="!tableView" />
      <a-pagination
        class="row-element"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `Total ${total} items`"
        :pageSizeOptions="['20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger />
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import store from '@/store'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import CardView from '@/components/widgets/CardView'
import ChartCard from '@/components/chart/ChartCard'
import DataView from '@/components/widgets/DataView'
import ListView from '@/components/widgets/ListView'
import Status from '@/components/widgets/Status'
import DetailView from '@/views/common/DetailView'

export default {
  name: 'Resource',
  components: {
    Breadcrumb,
    CardView,
    ChartCard,
    DataView,
    DetailView,
    ListView,
    Status
  },
  mixins: [mixinDevice],
  data () {
    return {
      apiName: '',
      loading: false,
      autoRefresh: false,
      columns: [],
      items: [],
      itemCount: 0,
      page: 1,
      pageSize: 20,
      searchQuery: '',
      resource: {},
      selectedRowKeys: [],
      currentAction: {},
      showAction: false,
      dataView: false,
      actions: [],
      tableView: true
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    '$route' (to, from) {
      if (to.fullPath !== from.fullPath && !to.fullPath.includes('action/')) {
        this.page = 1
        this.fetchData()
      }
    },
    '$i18n.locale' (to, from) {
      if (to !== from) {
        this.fetchData()
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    fetchData () {
      this.routeName = this.$route.name
      if (!this.routeName) {
        this.routeName = this.$route.matched[this.$route.matched.length - 1].parent.name
      }
      this.apiName = ''
      this.actions = []
      this.columns = []
      this.columnKeys = []
      var params = { listall: true }
      if (Object.keys(this.$route.query).length > 0) {
        Object.assign(params, this.$route.query)
      } else if (this.$route.meta.params) {
        Object.assign(params, this.$route.meta.params)
      }

      if (this.searchQuery !== '') {
        params['keyword'] = this.searchQuery
      }

      if (this.$route && this.$route.params && this.$route.params.id) {
        this.dataView = true
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

      if (!this.columnKeys || this.columnKeys.length === 0) {
        for (const field of store.getters.apis[this.apiName]['response']) {
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

      for (var key of this.columnKeys) {
        if (typeof key === 'object') {
          key = Object.keys(key)[0]
        }
        this.columns.push({
          title: this.$t(key),
          dataIndex: key,
          width: key === 'state' ? '8%' : '',
          scopedSlots: { customRender: key },
          sorter: (a, b) => String(a[key]).length - String(b[key]).length
        })
      }

      this.loading = true
      if (this.$route.params && this.$route.params.id) {
        params['id'] = this.$route.params.id
      }
      params['page'] = this.page
      params['pagesize'] = this.pageSize
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
            this.itemCount = json[responseName]['count']
            continue
          }
          objectName = key
          break
        }
        this.items = json[responseName][objectName]
        if (!this.items || this.items.length === 0) {
          this.items = []
        }
        for (let idx = 0; idx < this.items.length; idx++) {
          this.items[idx]['key'] = idx
        }
        if (this.items.length > 0) {
          this.resource = this.items[0]
        } else {
          this.resource = {}
        }
      }).catch(error => {
        // handle error
        this.$notification['error']({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
        if (error.response.status === 431) {
          this.$router.push({ path: '/exception/404' })
        }
      }).finally(f => {
        this.loading = false
      })
    },
    onSearch (value) {
      this.searchQuery = value
      this.fetchData()
    },
    toggleAutoRefresh () {
      this.autoRefresh = !this.autoRefresh
      this.doRefresh()
    },
    doRefresh () {
      if (!this.autoRefresh) {
        return
      }
      const doRefresh = this.doRefresh
      const fetchData = this.fetchData
      setTimeout(function () {
        fetchData()
        doRefresh()
      }, 5000)
    },
    closeAction () {
      this.currentAction.loading = false
      this.showAction = false
      this.currentAction = {}
    },
    execAction (action) {
      if (action.component && action.api && !action.popup) {
        this.$router.push({ name: action.api })
        return
      }
      this.currentAction = action
      var params = store.getters.apis[this.currentAction.api]['params']
      params.sort(function (a, b) {
        if (a.name === 'name' && b.name !== 'name') { return -1 }
        if (a.name !== 'name' && b.name === 'name') { return -1 }
        if (a.name === 'id') { return -1 }
        if (a.name < b.name) { return -1 }
        if (a.name > b.name) { return 1 }
        return 0
      })
      if (action.args && action.args.length > 0) {
        this.currentAction['params'] = action.args.map(function (arg) {
          return params.filter(function (param) {
            return param.name === arg
          })[0]
        })
      } else {
        this.currentAction['params'] = params
      }

      this.showAction = true
      for (const param of this.currentAction['params']) {
        if (param.type === 'uuid' || param.name === 'account') {
          this.listUuidOpts(param)
        }
      }
      this.currentAction.loading = false
    },
    listUuidOpts (param) {
      var paramName = param.name
      const possibleName = 'list' + paramName.replace('id', '').toLowerCase() + 's'
      var possibleApi
      if (paramName === 'id') {
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
      var params = { listall: true }
      if (possibleApi === 'listTemplates') {
        params['templatefilter'] = 'executable'
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
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          this.currentAction.loading = true
          const params = {}
          for (const key in values) {
            const input = values[key]
            for (const param of this.currentAction['params']) {
              if (param.name === key) {
                if (input === undefined) {
                  if (param.type === 'boolean') {
                    params[key] = false
                  }
                  break
                }
                if (param.type === 'uuid') {
                  params[key] = param.opts[input]['id']
                } else {
                  params[key] = input
                }
                break
              }
            }
          }

          if ('id' in this.resource) {
            params['id'] = this.resource['id']
          }

          const closeAction = this.closeAction
          const showError = this.$notification['error']
          api(this.currentAction.api, params).then(json => {
            for (const obj in json) {
              if (obj.includes('response')) {
                for (const res in json[obj]) {
                  if (res === 'jobid') {
                    this.$store.dispatch('AddAsyncJob', { 'title': this.currentAction.label, 'jobid': json[obj][res], 'description': this.resource.name, 'status': 'progress' })
                    break
                  }
                }
                break
              }
            }
            closeAction()
          }).catch(function (error) {
            closeAction()
            console.log(error)
            showError({
              message: 'Request Failed',
              description: error.response.headers['x-description']
            })
          })

          const fetchData = this.fetchData
          setTimeout(function () {
            fetchData()
          }, 2500)
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
    }
  }
}
</script>

<style scoped>

.mobile-breadcrumb {
  margin-left: -16px;
  margin-right: -16px;
  margin-top: -16px;
  padding-bottom: 16px;
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
