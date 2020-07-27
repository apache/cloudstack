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
    size="small"
    :loading="loading"
    :columns="fetchColumns()"
    :dataSource="items"
    :rowKey="record => record.id || record.name || record.usageType"
    :pagination="false"
    :rowSelection="['vm', 'event', 'alert'].includes($route.name) ? {selectedRowKeys: selectedRowKeys, onChange: onSelectChange} : null"
    :rowClassName="getRowClassName"
    style="overflow-y: auto"
  >
    <template slot="footer">
      <span v-if="hasSelected">
        {{ `Selected ${selectedRowKeys.length} items` }}
      </span>
    </template>

    <!--
    <div slot="expandedRowRender" slot-scope="resource">
      <info-card :resource="resource" style="margin-left: 0px; width: 50%">
        <div slot="actions" style="padding-top: 12px">
          <a-tooltip
            v-for="(action, actionIndex) in $route.meta.actions"
            :key="actionIndex"
            placement="bottom">
            <template slot="title">
              {{ $t(action.label) }}
            </template>
            <a-button
              v-if="action.api in $store.getters.apis && action.dataView &&
                ('show' in action ? action.show(resource, $store.getters.userInfo) : true)"
              :icon="action.icon"
              :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
              shape="circle"
              style="margin-right: 5px; margin-top: 12px"
              @click="$parent.execAction(action)"
            >
            </a-button>
          </a-tooltip>
        </div>
      </info-card>
    </div>
    -->

    <span slot="name" slot-scope="text, record">
      <div style="min-width: 120px">
        <span v-if="$route.path.startsWith('/project')" style="margin-right: 5px">
          <a-button type="dashed" size="small" shape="circle" icon="login" @click="changeProject(record)" />
        </span>
        <os-logo v-if="record.ostypename" :osName="record.ostypename" size="1x" style="margin-right: 5px" />
        <console :resource="record" size="small" style="margin-right: 5px" />

        <span v-if="$route.path.startsWith('/globalsetting')">{{ text }}</span>
        <span v-if="$route.path.startsWith('/alert')">
          <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ $t(text.toLowerCase()) }}</router-link>
          <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ $t(text.toLowerCase()) }}</router-link>
        </span>
        <span v-else>
          <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ text }}</router-link>
          <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ text }}</router-link>
        </span>
      </div>
    </span>
    <a slot="templatetype" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.templatetype }">{{ text }}</router-link>
    </a>
    <template slot="type" slot-scope="text">
      <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(text)">{{ $t(text.toLowerCase()) }}</span>
      <span v-else>{{ text }}</span>
    </template>
    <a slot="displayname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <span slot="username" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }" v-if="['/accountuser', '/vpnuser'].includes($route.path)">{{ text }}</router-link>
      <router-link :to="{ path: '/accountuser', query: { username: record.username, domainid: record.domainid } }" v-else-if="$store.getters.userInfo.roletype !== 'User'">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </span>
    <a slot="ipaddress" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      <span v-if="record.issourcenat">
        &nbsp;
        <a-tag>source-nat</a-tag>
      </span>
    </a>
    <a slot="publicip" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <span slot="traffictype" slot-scope="text" href="javascript:;">
      {{ text }}
    </span>
    <a slot="vmname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/vm/' + record.virtualmachineid }">{{ text }}</router-link>
    </a>
    <a slot="virtualmachinename" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/vm/' + record.virtualmachineid }">{{ text }}</router-link>
    </a>
    <span slot="hypervisor" slot-scope="text, record">
      <span v-if="$route.name === 'hypervisorcapability'">
        <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      </span>
      <span v-else>{{ text }}</span>
    </span>
    <template slot="state" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>
    <template slot="allocationstate" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>
    <template slot="resourcestate" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>
    <template slot="powerstate" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>
    <template slot="agentstate" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>
    <a slot="guestnetworkname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/guestnetwork/' + record.guestnetworkid }">{{ text }}</router-link>
    </a>
    <a slot="associatednetworkname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/guestnetwork/' + record.associatednetworkid }">{{ text }}</router-link>
    </a>
    <a slot="vpcname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/vpc/' + record.vpcid }">{{ text }}</router-link>
    </a>
    <a slot="hostname" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.hostid" :to="{ path: '/host/' + record.hostid }">{{ text }}</router-link>
      <router-link v-else-if="record.hostname" :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </a>
    <a slot="storage" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.storageid" :to="{ path: '/storagepool/' + record.storageid }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </a>

    <a slot="level" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/event/' + record.id }">{{ text }}</router-link>
    </a>

    <a slot="clustername" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/cluster/' + record.clusterid }">{{ text }}</router-link>
    </a>
    <a slot="podname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/pod/' + record.podid }">{{ text }}</router-link>
    </a>
    <span slot="account" slot-scope="text, record">
      <template v-if="text && !text.startsWith('PrjAcct-')">
        <router-link
          v-if="'quota' in record && $router.resolve(`${$route.path}/${record.account}`) !== '404'"
          :to="{ path: `${$route.path}/${record.account}`, query: { account: record.account, domainid: record.domainid, quota: true } }">{{ text }}</router-link>
        <router-link :to="{ path: '/account/' + record.accountid }" v-else-if="record.accountid">{{ text }}</router-link>
        <router-link :to="{ path: '/account', query: { name: record.account, domainid: record.domainid } }" v-else-if="$store.getters.userInfo.roletype !== 'User'">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
    </span>
    <span slot="domain" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.domainid && !record.domainid.toString().includes(',') && $store.getters.userInfo.roletype !== 'User'" :to="{ path: '/domain/' + record.domainid }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </span>
    <span slot="domainpath" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.domainid && !record.domainid.includes(',') && $router.resolve('/domain/' + record.domainid).route.name !== '404'" :to="{ path: '/domain/' + record.domainid }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </span>
    <a slot="zone" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.zoneid && !record.zoneid.includes(',') && $router.resolve('/zone/' + record.zoneid).route.name !== '404'" :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </a>
    <span slot="zonename" slot-scope="text, record">
      <router-link v-if="$router.resolve('/zone/' + record.zoneid).route.name !== '404'" :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </span>

    <div slot="order" slot-scope="text, record" class="shift-btns">
      <a-tooltip placement="top">
        <template slot="title">{{ $t('label.move.to.top') }}</template>
        <a-button
          shape="round"
          @click="moveItemTop(record)"
          class="shift-btn">
          <a-icon type="double-left" class="shift-btn shift-btn--rotated" />
        </a-button>
      </a-tooltip>
      <a-tooltip placement="top">
        <template slot="title">{{ $t('label.move.to.bottom') }}</template>
        <a-button
          shape="round"
          @click="moveItemBottom(record)"
          class="shift-btn">
          <a-icon type="double-right" class="shift-btn shift-btn--rotated" />
        </a-button>
      </a-tooltip>
      <a-tooltip placement="top">
        <template slot="title">{{ $t('label.move.up.row') }}</template>
        <a-button shape="round" @click="moveItemUp(record)" class="shift-btn">
          <a-icon type="caret-up" class="shift-btn" />
        </a-button>
      </a-tooltip>
      <a-tooltip placement="top">
        <template slot="title">{{ $t('label.move.down.row') }}</template>
        <a-button shape="round" @click="moveItemDown(record)" class="shift-btn">
          <a-icon type="caret-down" class="shift-btn" />
        </a-button>
      </a-tooltip>
    </div>

    <template slot="value" slot-scope="text, record">
      <a-input
        v-if="editableValueKey === record.key"
        :defaultValue="record.value"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
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
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        v-if="editableValueKey !== record.key"
        icon="edit"
        @click="editValue(record)" />
      <a-button
        shape="circle"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
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
    <template slot="tariffActions" slot-scope="text, record">
      <a-button
        shape="circle"
        v-if="editableValueKey !== record.key"
        :disabled="!('quotaTariffUpdate' in $store.getters.apis)"
        icon="edit"
        @click="editTariffValue(record)" />
      <slot></slot>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Console from '@/components/widgets/Console'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import InfoCard from '@/components/view/InfoCard'

export default {
  name: 'ListView',
  components: {
    Console,
    OsLogo,
    Status,
    InfoCard
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
  inject: ['parentFetchData', 'parentToggleLoading', 'parentEditTariffAction'],
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
    fetchColumns () {
      if (this.isOrderUpdatable()) {
        return this.columns
      }
      return this.columns.filter(x => x.dataIndex !== 'order')
    },
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    changeProject (project) {
      this.$store.dispatch('SetProject', project)
      this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
      this.$message.success(this.$t('message.switch.to') + ' ' + project.name)
      this.$router.push({ name: 'dashboard' })
    },
    saveValue (record) {
      api('updateConfiguration', {
        name: record.name,
        value: this.editableValue
      }).then(json => {
        this.editableValueKey = null

        this.$message.success(`${this.$t('message.setting.updated')} ${record.name}`)
        if (json.updateconfigurationresponse &&
          json.updateconfigurationresponse.configuration &&
          !json.updateconfigurationresponse.configuration.isdynamic &&
          ['Admin'].includes(this.$store.getters.userInfo.roletype)) {
          this.$notification.warning({
            message: this.$t('label.status'),
            description: this.$t('message.restart.mgmt.server')
          })
        }
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.save.setting'))
      }).finally(() => {
        this.$emit('refresh')
      })
    },
    editValue (record) {
      this.editableValueKey = record.key
      this.editableValue = record.value
    },
    getUpdateApi () {
      let apiString = ''
      switch (this.$route.name) {
        case 'template':
          apiString = 'updateTemplate'
          break
        case 'iso':
          apiString = 'updateIso'
          break
        case 'zone':
          apiString = 'updateZone'
          break
        case 'computeoffering':
        case 'systemoffering':
          apiString = 'updateServiceOffering'
          break
        case 'diskoffering':
          apiString = 'updateDiskOffering'
          break
        case 'networkoffering':
          apiString = 'updateNetworkOffering'
          break
        case 'vpcoffering':
          apiString = 'updateVPCOffering'
          break
        default:
          apiString = 'updateTemplate'
      }
      return apiString
    },
    isOrderUpdatable () {
      return this.getUpdateApi() in this.$store.getters.apis
    },
    handleUpdateOrder (id, index) {
      this.parentToggleLoading()
      const apiString = this.getUpdateApi()

      api(apiString, {
        id,
        sortKey: index
      }).catch(error => {
        console.error(error)
      }).finally(() => {
        this.parentFetchData()
        this.parentToggleLoading()
      })
    },
    moveItemUp (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === 0) return

      data.splice(index - 1, 0, data.splice(index, 1)[0])

      data.forEach((item, index) => {
        this.handleUpdateOrder(item.id, index + 1)
      })
    },
    moveItemDown (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === data.length - 1) return

      data.splice(index + 1, 0, data.splice(index, 1)[0])

      data.forEach((item, index) => {
        this.handleUpdateOrder(item.id, index + 1)
      })
    },
    moveItemTop (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === 0) return

      data.unshift(data.splice(index, 1)[0])

      data.forEach((item, index) => {
        this.handleUpdateOrder(item.id, index + 1)
      })
    },
    moveItemBottom (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === data.length - 1) return

      data.push(data.splice(index, 1)[0])

      data.forEach((item, index) => {
        this.handleUpdateOrder(item.id, index + 1)
      })
    },
    editTariffValue (record) {
      this.parentEditTariffAction(true, record)
    }
  }
}
</script>

<style scoped>
/deep/ .ant-table-thead {
  background-color: #f9f9f9;
}

/deep/ .ant-table-small > .ant-table-content > .ant-table-body {
  margin: 0;
}

/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}
</style>

<style scoped lang="scss">
  .shift-btns {
    display: flex;
  }
  .shift-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    font-size: 12px;

    &:not(:last-child) {
      margin-right: 5px;
    }

    &--rotated {
      font-size: 10px;
      transform: rotate(90deg);
    }

  }
</style>
