<template>
  <div style="margin-top: 16px">

    <!--
    <font-awesome-icon :icon="['far', 'bell']" />
    <font-awesome-icon :icon="['fas', 'dharmachakra']" size='2x' />
    <font-awesome-icon :icon="['fab', 'ubuntu']" />
    <font-awesome-icon :icon="['fab', 'ubuntu']" size="lg" />
    <font-awesome-icon :icon="['fab', 'ubuntu']" size="2x" />
    <font-awesome-icon :icon="['fab', 'centos']" size="4x" />
    <font-awesome-icon icon="coffee" />
    -->

    <a-row>
      <a-col :span="16">
        <a-button
          v-for="(action, actionIndex) in actions"
          :key="actionIndex"
          :icon="action.icon"
          :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
          shape="circle"
          style="margin-right: 5px"
          @click="execAction(action)"
        >
        </a-button>
        <a-button
          @click="fetchData()"
          :loading="loading"
          shape="circle"
          icon="reload"
        />
      </a-col>
      <a-col :span="8" v-if="!$route.params || !$route.params.id">
        <a-input-search
          size="default"
          placeholder="Search"
          @search="onSearch"
        >
          <a-icon slot="prefix" type="search" />
        </a-input-search>
      </a-col>
    </a-row>

    <a-drawer
      :title="currentAction.label"
      placement="right"
      width="75%"
      :closable="true"
      @close="closeAction"
      :visible="showAction"
    >
      <a-spin :spinning="currentAction.loading">
        <a-form
          :form="form"
          @submit="handleSubmit"
          layout="vertical" >
          <a-form-item
            v-for="(field, fieldIndex) in currentAction.params"
            :key="fieldIndex"
            :label="field.name"
            :v-bind="field.name">

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
                  {{ opt.name }}
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

          <a-form-item>
            <div
              :style="{
                bottom: 0,
                width: '100%',
                borderTop: '1px solid #e8e8e8',
                paddingTop: '24px',
                textAlign: 'right',
                left: 0,
                background: '#fff',
                borderRadius: '0 0 4px 4px',
              }"
            >
              <a-button
                style="marginRight: 8px"
                @click="closeAction"
              >
                Cancel
              </a-button>
              <a-button
                :loading="currentAction.loading"
                type="primary"
                html-type="submit">
                Submit
              </a-button>
            </div>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-drawer>

    <div v-if="$route.params && $route.params.id">
      <p v-for="(value, key) in getResource($route.params.id)" :key="key">
        <span>{{ key }}: </span>
        <span>{{ value }}</span>
      </p>
    </div>
    <div style="margin-top: 12px" v-else>
      <a-table
        size="small"
        :rowKey="record => record.id"
        :loading="loading"
        :columns="columns"
        :dataSource="items"
        :scroll="{ x: '100%' }"
        :pagination="{ position: 'bottom', size: 'small', showSizeChanger: true }"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        :rowClassName="getRowClassName"
      >
        <template slot="footer">
          <span v-if="hasSelected">
            {{ `Selected ${selectedRowKeys.length} items` }}
          </span>
        </template>

        <a slot="name" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: $route.name + '/' + record.id }">{{ text }}</router-link>
        </a>
        <a slot="username" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: $route.name + '/' + record.id }">{{ text }}</router-link>
        </a>
        <a slot="vmname" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: '/vm/' + record.virtualmachineid }">{{ text }}</router-link>
        </a>
        <template slot="state" slot-scope="text, record">
          <a-tooltip placement="right">
            <template slot='title'>
              {{ text }}
            </template>
            <a-badge :title="text" :status="getBadgeStatus(text)"/>
          </a-tooltip>
        </template>

        <a slot="account" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: '/account/' + record.accountid }">{{ text }}</router-link>
        </a>
        <a slot="domain" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: '/domain/' + record.domainid }">{{ text }}</router-link>
        </a>
        <a slot="zonename" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
        </a>

        <a slot="guestnetworkname" slot-scope="text, record" href="javascript:;">
          <router-link :to="{ path: '/guestnetwork/' + record.guestnetworkid }">{{ text }}</router-link>
        </a>

      </a-table>
    </div>

  </div>

</template>

<script>
import { api } from '@/api'
import store from '@/store'

export default {
  name: 'Resource',
  data () {
    return {
      apiName: '',
      loading: false,
      columns: [],
      items: [],
      selectedRowKeys: [],
      currentAction: {},
      showAction: false,
      actions: []
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
      if (to.path === this.$route.path) {
        this.fetchData()
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    fetchData (search = '') {
      this.routeName = this.$route.name
      if (!this.routeName) {
        this.routeName = this.$route.matched[this.$route.matched.length - 1].parent.name
      }
      this.apiName = ''
      this.actions = []
      this.columns = []
      this.columnKeys = []
      var params = { listall: true }
      console.log(this.$route)
      if (Object.keys(this.$route.query).length > 0) {
        Object.assign(params, this.$route.query)
      } else if (this.$route.meta.params) {
        Object.assign(params, this.$route.meta.params)
      }
      if (search !== '') {
        params['keyword'] = search
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
      if (!this.columnKeys.includes('name')) {
        this.columnKeys = ['name', ...this.columnKeys]
      }
      var counter = 0
      for (const key of this.columnKeys) {
        this.columns.push({
          title: key,
          dataIndex: key,
          key: counter++,
          scopedSlots: { customRender: key },
          sorter: (a, b) => a[key].length - b[key].length
        })
      }

      this.loading = true
      if (this.$route.params && this.$route.params.id) {
        params['id'] = this.$route.params.id
      }
      api(this.apiName, params).then(json => {
        this.loading = false
        var responseName
        var objectName
        for (const key in json) {
          if (key.includes('response')) {
            responseName = key
            break
          }
        }
        for (const key in json[responseName]) {
          if (key === 'count') continue
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
      })
    },
    getResource (id) {
      var res = {}
      for (const item of this.items) {
        if (item.id === id) {
          res = item
          break
        }
      }
      return res
    },
    onSearch (value) {
      this.fetchData(value)
    },
    closeAction () {
      this.currentAction.loading = false
      this.showAction = false
      this.currentAction = {}
    },
    execAction (action) {
      this.currentAction = action
      this.currentAction['params'] = store.getters.apis[this.currentAction.api]['params']
      this.currentAction['params'].sort(function (a, b) {
        if (a.name === 'name' && b.name !== 'name') { return -1 }
        if (a.name !== 'name' && b.name === 'name') { return -1 }
        if (a.name === 'id') { return -1 }
        if (a.name < b.name) { return -1 }
        if (a.name > b.name) { return 1 }
        return 0
      })
      for (var param of this.currentAction['params']) {
        if (param.type === 'uuid' || param.name === 'account') {
          this.listUuidOpts(param)
        }
      }
      this.showAction = true
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

          api(this.currentAction.api, params).then(json => {
            console.log(json)
            this.closeAction()
          }).catch(function (error) {
            console.log(error)
            this.closeAction()
          }).then(function () {
          })
        }
      })
    },
    getRowClassName(record, index) {
      if (index % 2 !== 0) {
        return 'light-row'
      }
      return 'dark-row'
    },
    getBadgeStatus(state) {
      var status = "default"
      switch (state) {
        case "Running":
        case "Ready":
        case "Up":
        case "BackedUp":
          status = "success"
          break
        case "Stopped":
          status = "error"
          break
        case "Migrating":
        case "Starting":
          status = "processing"
          break
        case "Alert":
        case "Allocated":
          status = "warning"
          break
      }
      return status
    },
    start () {
      this.loading = true
      this.fetchData()
      setTimeout(() => {
        this.loading = false
        this.selectedRowKeys = []
      }, 1000)
    },
    onSelectChange (selectedRowKeys) {
      console.log('selectedRowKeys changed: ', selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    }
  }
}
</script>

<style>

.ant-badge-status-dot {
  width: 12px;
  height: 12px;
}

.light-row {
  background-color: #fff;
}

.dark-row {
  background-color: #f5f5f5;
}

</style>
