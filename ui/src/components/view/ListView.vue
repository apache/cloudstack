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
  <a-table
    size="middle"
    :loading="loading"
    :columns="columns"
    :dataSource="items"
    :rowKey="record => record.id || record.name"
    :pagination="false"
    :rowSelection="['vm', 'event', 'alert'].includes($route.name) ? {selectedRowKeys: selectedRowKeys, onChange: onSelectChange} : null"
    :rowClassName="getRowClassName"
  >
    <template slot="footer">
      <span v-if="hasSelected">
        {{ `Selected ${selectedRowKeys.length} items` }}
      </span>
    </template>

    <a slot="name" slot-scope="text, record" href="javascript:;">
      <div style="min-width: 150px; padding-left: 5px">
        <span v-if="$route.path.startsWith('/project')" style="margin-right: 5px">
          <a-button type="dashed" size="small" shape="circle" icon="login" @click="changeProject(record)" />
        </span>
        <console :resource="record" size="small" />
        <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ text }}</router-link>
        <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ text }}</router-link>
      </div>
      <div v-if="$route.meta.related" style="padding-top: 10px; padding-left: 5px; display: inline-flex">
        <span v-for="item in $route.meta.related" :key="item.path">
          <router-link
            v-if="$router.resolve('/' + item.name).route.name !== '404'"
            :to="{ path: '/' + item.name + '?' + item.param + '=' + (item.param === 'account' ? record.name + '&domainid=' + record.domainid : record.id) }">
            <a-tooltip placement="bottom">
              <template slot="title">
                View {{ $t(item.title) }}
              </template>
              <a-button size="small" shape="round" :icon="$router.resolve('/' + item.name).route.meta.icon" />
            </a-tooltip>
          </router-link>
        </span>
      </div>
    </a>
    <a slot="displayname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <a slot="username" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <a slot="ipaddress" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      <span v-if="record.issourcenat">
        &nbsp;
        <a-tag>source-nat</a-tag>
      </span>
    </a>
    <a slot="vmname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/vm/' + record.virtualmachineid }">{{ text }}</router-link>
    </a>
    <template slot="state" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>
    <a slot="guestnetworkname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/guestnetwork/' + record.guestnetworkid }">{{ text }}</router-link>
    </a>
    <a slot="vpcname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/vpc/' + record.vpcid }">{{ text }}</router-link>
    </a>
    <a slot="account" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/account/' + record.accountid }" v-if="record.accountid">{{ text }}</router-link>
      <router-link :to="{ path: '/account', query: { name: record.account, domainid: record.domainid } }" v-else>{{ text }}</router-link>
    </a>
    <a slot="domain" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/domain/' + record.domainid }">{{ text }}</router-link>
    </a>
    <a slot="hostname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/host/' + record.hostid }">{{ text }}</router-link>
    </a>
    <a slot="clustername" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/cluster/' + record.clusterid }">{{ text }}</router-link>
    </a>
    <a slot="podname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/pod/' + record.podid }">{{ text }}</router-link>
    </a>
    <a slot="zonename" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
    </a>

    <template slot="value" slot-scope="text, record">
      <a-input
        v-if="editableValueKey === record.key"
        :defaultValue="record.value"
        v-model="editableValue"
        @keydown.esc="editableValueKey = null"
        @pressEnter="saveValue(record)">
      </a-input>
      <div v-else style="width: 200px; word-break: break-all">
        {{ text }}
      </div>
    </template>
    <template slot="actions" slot-scope="text, record">
      <a-button
        shape="circle"
        v-if="editableValueKey !== record.key"
        icon="edit"
        @click="editValue(record)" />
      <a-button
        shape="circle"
        @click="saveValue(record)"
        v-if="editableValueKey === record.key" >
        <a-icon type="check-circle" theme="twoTone" twoToneColor="#52c41a" />
      </a-button>
      <a-button
        shape="circle"
        size="default"
        @click="editableValueKey = null"
        v-if="editableValueKey === record.key" >
        <a-icon type="close-circle" theme="twoTone" twoToneColor="#f5222d" />
      </a-button>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Console from '@/components/widgets/Console'
import Status from '@/components/widgets/Status'

export default {
  name: 'ListView',
  components: {
    Console,
    Status
  },
  props: {
    columns: {
      type: Array,
      required: true
    },
    items: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      selectedRowKeys: [],
      editableValueKey: null,
      editableValue: ''
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  methods: {
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    },
    onSelectChange (selectedRowKeys) {
      console.log('selectedRowKeys changed: ', selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    },
    changeProject (project) {
      this.$store.dispatch('SetProject', project)
      this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
      this.$message.success(`Switched to "${project.name}"`)
      this.$router.push({ name: 'dashboard' })
    },
    saveValue (record) {
      api('updateConfiguration', {
        name: record.name,
        value: this.editableValue
      }).then(() => {
        this.editableValueKey = null

        this.$message.success('Setting Updated: ' + record.name)
        this.$notification.warning({
          message: 'Status',
          description: 'Please restart your management server(s) for your new settings to take effect.'
        })
      }).catch(error => {
        console.error(error)
        this.$message.error('There was an error saving this setting.')
      })
        .finally(() => {
          this.$emit('refresh')
        })
    },
    editValue (record) {
      this.editableValueKey = record.key
      this.editableValue = record.value
    }
  }
}
</script>

<style scoped>
/deep/ .ant-table-thead {
  background-color: #f9f9f9;
}

/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}
</style>
