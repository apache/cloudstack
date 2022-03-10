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
    :columns="isOrderUpdatable() ? columns : columns.filter(x => x.dataIndex !== 'order')"
    :dataSource="items"
    :rowKey="(record, idx) => record.id || record.name || record.usageType || idx + '-' + Math.random()"
    :pagination="false"
    :rowSelection=" enableGroupAction() || $route.name === 'event' ? {selectedRowKeys: selectedRowKeys, onChange: onSelectChange} : null"
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
      <span v-if="['vm'].includes($route.path.split('/')[1])">
        <span v-if="record.icon && record.icon.base64image">
          <resource-icon :image="record.icon.base64image" size="1x" style="margin-right: 5px"/>
        </span>
        <os-logo v-else :osId="record.ostypeid" :osName="record.osdisplayname" size="lg" style="margin-right: 5px" />
      </span>
      <span style="min-width: 120px" >
        <QuickView
          style="margin-left: 5px"
          :actions="actions"
          :resource="record"
          :enabled="quickViewEnabled() && actions.length > 0 && columns && columns[0].dataIndex === 'name' "
          @exec-action="$parent.execAction"/>
        <span v-if="$route.path.startsWith('/project')" style="margin-right: 5px">
          <tooltip-button type="dashed" size="small" icon="login" @click="changeProject(record)" />
        </span>
        <span v-if="$showIcon() && !['vm'].includes($route.path.split('/')[1])">
          <resource-icon v-if="$showIcon() && record.icon && record.icon.base64image" :image="record.icon.base64image" size="1x" style="margin-right: 5px"/>
          <os-logo v-else-if="record.ostypename" :osName="record.ostypename" size="1x" style="margin-right: 5px" />
          <a-icon v-else-if="typeof $route.meta.icon ==='string'" style="font-size: 16px; margin-right: 5px" :type="$route.meta.icon"/>
          <a-icon v-else style="font-size: 16px; margin-right: 5px" :component="$route.meta.icon" />
        </span>
        <span v-else>
          <os-logo v-if="record.ostypename" :osName="record.ostypename" size="1x" style="margin-right: 5px" />
        </span>

        <span v-if="record.hasannotations">
          <span v-if="record.id">
            <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
            <router-link :to="{ path: $route.path + '/' + record.id + '?tab=comments' }"><a-icon style="padding-left: 10px" size="small" type="message" theme="filled"/></router-link>
          </span>
          <router-link v-else :to="{ path: $route.path + '/' + record.name }" >{{ text }}</router-link>
        </span>
        <span v-else-if="$route.path.startsWith('/globalsetting')">{{ text }}</span>
        <span v-else-if="$route.path.startsWith('/alert')">
          <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ $t(text.toLowerCase()) }}</router-link>
          <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ $t(text.toLowerCase()) }}</router-link>
        </span>
        <span v-else>
          <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id && $route.path !== '/ssh'">{{ text }}</router-link>
          <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ text }}</router-link>
        </span>
      </span>
    </span>
    <a slot="templatetype" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.templatetype }">{{ text }}</router-link>
    </a>
    <template slot="type" slot-scope="text">
      <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(text)">{{ $t(text.toLowerCase()) }}</span>
      <span v-else>{{ text }}</span>
    </template>
    <a slot="displayname" slot-scope="text, record" href="javascript:;">
      <QuickView
        style="margin-left: 5px"
        :actions="actions"
        :resource="record"
        :enabled="quickViewEnabled() && actions.length > 0 && columns && columns[0].dataIndex === 'displayname' "
        @exec-action="$parent.execAction"/>
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <span slot="username" slot-scope="text, record" href="javascript:;">
      <span v-if="$showIcon() && !['vm'].includes($route.path.split('/')[1])">
        <resource-icon v-if="$showIcon() && record.icon && record.icon.base64image" :image="record.icon.base64image" size="1x" style="margin-right: 5px"/>
        <a-icon v-else style="font-size: 16px; margin-right: 5px" type="user" />
      </span>
      <router-link :to="{ path: $route.path + '/' + record.id }" v-if="['/accountuser', '/vpnuser'].includes($route.path)">{{ text }}</router-link>
      <router-link :to="{ path: '/accountuser', query: { username: record.username, domainid: record.domainid } }" v-else-if="$store.getters.userInfo.roletype !== 'User'">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </span>
    <span slot="entityid" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: generateCommentsPath(record) }">{{ record.entityname }}</router-link>
    </span>
    <span slot="entitytype" slot-scope="text, record" href="javascript:;">
      {{ generateHumanReadableEntityType(record) }}
    </span>
    <span slot="adminsonly" v-if="['Admin'].includes($store.getters.userInfo.roletype)" slot-scope="text, record" href="javascript:;">
      <a-checkbox :checked="record.adminsonly" :value="record.id" v-if="record.userid === $store.getters.userInfo.id" @change="e => updateAdminsOnly(e)" />
      <a-checkbox :checked="record.adminsonly" disabled v-else />
    </span>
    <span slot="ipaddress" slot-scope="text, record" href="javascript:;">
      <router-link v-if="['/publicip', '/privategw'].includes($route.path)" :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
      <span v-if="record.issourcenat">
        &nbsp;
        <a-tag>source-nat</a-tag>
      </span>
    </span>
    <span slot="ip6address" slot-scope="text, record" href="javascript:;">
      <span>{{ ipV6Address(text, record) }}</span>
    </span>
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
    <template slot="state" slot-scope="text, record">
      <status v-if="$route.path.startsWith('/host')" :text="getHostState(record)" displayText />
      <status v-else :text="text ? text : ''" displayText style="min-width: 80px" />
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

    <template v-for="(value, name) in thresholdMapping" :slot="name" slot-scope="text, record" href="javascript:;">
      <span :key="name">
        <span v-if="record[value.disable]" class="alert-disable-threshold">
          {{ text }}
        </span>
        <span v-else-if="record[value.notification]" class="alert-notification-threshold">
          {{ text }}
        </span>
        <span style="padding: 10%;" v-else>
          {{ text }}
        </span>
      </span>
    </template>

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
      <template v-if="record.owner">
        <template v-for="(item,idx) in record.owner">
          <span style="margin-right:5px" :key="idx">
            <span v-if="$store.getters.userInfo.roletype !== 'User'">
              <router-link v-if="'user' in item" :to="{ path: '/accountuser', query: { username: item.user, domainid: record.domainid }}">{{ item.account + '(' + item.user + ')' }}</router-link>
              <router-link v-else :to="{ path: '/account', query: { name: item.account, domainid: record.domainid, dataView: true } }">{{ item.account }}</router-link>
            </span>
            <span v-else>{{ item.user ? item.account + '(' + item.user + ')' : item.account }}</span>
          </span>
        </template>
      </template>
      <template v-if="text && !text.startsWith('PrjAcct-')">
        <router-link
          v-if="'quota' in record && $router.resolve(`${$route.path}/${record.account}`) !== '404'"
          :to="{ path: `${$route.path}/${record.account}`, query: { account: record.account, domainid: record.domainid, quota: true } }">{{ text }}</router-link>
        <router-link :to="{ path: '/account/' + record.accountid }" v-else-if="record.accountid">{{ text }}</router-link>
        <router-link :to="{ path: '/account', query: { name: record.account, domainid: record.domainid, dataView: true } }" v-else-if="$store.getters.userInfo.roletype !== 'User'">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
    </span>
    <span slot="domain" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.domainid && !record.domainid.toString().includes(',') && $store.getters.userInfo.roletype !== 'User'" :to="{ path: '/domain/' + record.domainid + '?tab=details' }">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </span>
    <span slot="domainpath" slot-scope="text, record" href="javascript:;">
      <router-link v-if="record.domainid && !record.domainid.includes(',') && $router.resolve('/domain/' + record.domainid).route.name !== '404'" :to="{ path: '/domain/' + record.domainid + '?tab=details' }">{{ text }}</router-link>
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
    <a slot="readonly" slot-scope="text, record">
      <status :text="record.readonly ? 'ReadOnly' : 'ReadWrite'" displayText />
    </a>
    <span slot="autoscalingenabled" slot-scope="text, record">
      <status :text="record.autoscalingenabled ? 'Enabled' : 'Disabled'" />
      {{ record.autoscalingenabled ? 'Enabled' : 'Disabled' }}
    </span>
    <span slot="current" slot-scope="text, record">
      <status :text="record.current ? record.current.toString() : 'false'" />
    </span>
    <span slot="created" slot-scope="text">
      {{ $toLocaleDate(text) }}
    </span>
    <span slot="sent" slot-scope="text">
      {{ $toLocaleDate(text) }}
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
        :autoFocus="true"
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
      <tooltip-button
        :tooltip="$t('label.edit')"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        v-if="editableValueKey !== record.key"
        icon="edit"
        @click="editValue(record)" />
      <tooltip-button
        :tooltip="$t('label.cancel')"
        @click="editableValueKey = null"
        v-if="editableValueKey === record.key"
        iconType="close-circle"
        iconTwoToneColor="#f5222d" />
      <tooltip-button
        :tooltip="$t('label.ok')"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        @click="saveValue(record)"
        v-if="editableValueKey === record.key"
        iconType="check-circle"
        iconTwoToneColor="#52c41a" />
      <tooltip-button
        :tooltip="$t('label.reset.config.value')"
        @click="resetConfig(record)"
        v-if="editableValueKey !== record.key"
        icon="reload"
        :disabled="!('updateConfiguration' in $store.getters.apis)" />
    </template>
    <template slot="tariffActions" slot-scope="text, record">
      <tooltip-button
        :tooltip="$t('label.edit')"
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
import QuickView from '@/components/view/QuickView'
import TooltipButton from '@/components/widgets/TooltipButton'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ListView',
  components: {
    Console,
    OsLogo,
    Status,
    InfoCard,
    QuickView,
    TooltipButton,
    ResourceIcon
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
    },
    actions: {
      type: Array,
      default: () => []
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  data () {
    return {
      selectedRowKeys: [],
      editableValueKey: null,
      editableValue: '',
      resourceIcon: '',
      thresholdMapping: {
        cpuused: {
          notification: 'cputhreshold',
          disable: 'cpudisablethreshold'
        },
        cpuallocated: {
          notification: 'cpuallocatedthreshold',
          disable: 'cpuallocateddisablethreshold'
        },
        memoryused: {
          notification: 'memorythreshold',
          disable: 'memorydisablethreshold'
        },
        memoryallocated: {
          notification: 'memoryallocatedthreshold',
          disable: 'memoryallocateddisablethreshold'
        },
        cpuusedghz: {
          notification: 'cputhreshold',
          disable: 'cpudisablethreshold'
        },
        cpuallocatedghz: {
          notification: 'cpuallocatedthreshold',
          disable: 'cpuallocateddisablethreshold'
        },
        memoryusedgb: {
          notification: 'memorythreshold',
          disable: 'memorydisablethreshold'
        },
        memoryallocatedgb: {
          notification: 'memoryallocatedthreshold',
          disable: 'memoryallocateddisablethreshold'
        },
        disksizeusedgb: {
          notification: 'storageusagethreshold',
          disable: 'storageusagedisablethreshold'
        },
        disksizeallocatedgb: {
          notification: 'storageallocatedthreshold',
          disable: 'storageallocateddisablethreshold'
        }
      }
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  methods: {
    quickViewEnabled () {
      return new RegExp(['/vm', '/kubernetes', '/ssh', '/vmgroup', '/affinitygroup',
        '/volume', '/snapshot', '/vmsnapshot', '/backup',
        '/guestnetwork', '/vpc', '/vpncustomergateway',
        '/template', '/iso',
        '/project', '/account',
        '/zone', '/pod', '/cluster', '/host', '/storagepool', '/imagestore', '/systemvm', '/router', '/ilbvm', '/annotation',
        '/computeoffering', '/systemoffering', '/diskoffering', '/backupoffering', '/networkoffering', '/vpcoffering'].join('|'))
        .test(this.$route.path)
    },
    enableGroupAction () {
      return ['vm', 'alert', 'vmgroup', 'ssh', 'affinitygroup', 'volume', 'snapshot',
        'vmsnapshot', 'guestnetwork', 'vpc', 'publicip', 'vpnuser', 'vpncustomergateway',
        'project', 'account', 'systemvm', 'router', 'computeoffering', 'systemoffering',
        'diskoffering', 'backupoffering', 'networkoffering', 'vpcoffering', 'ilbvm', 'kubernetes', 'comment'
      ].includes(this.$route.name)
    },
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
        this.$store.dispatch('RefreshFeatures')
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
    resetConfig (item) {
      api('resetConfiguration', {
        name: item.name
      }).then(() => {
        const message = `${this.$t('label.setting')} ${item.name} ${this.$t('label.reset.config.value')}`
        this.$message.success(message)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.reset.config'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.reset.config')
        })
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

      return new Promise((resolve, reject) => {
        api(apiString, {
          id,
          sortKey: index
        }).then((response) => {
          resolve(response)
        }).catch((reason) => {
          reject(reason)
        })
      })
    },
    updateOrder (data) {
      const promises = []
      data.forEach((item, index) => {
        promises.push(this.handleUpdateOrder(item.id, index + 1))
      })
      Promise.all(promises).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.parentToggleLoading()
        this.parentFetchData()
      })
    },
    moveItemUp (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === 0) return
      data.splice(index - 1, 0, data.splice(index, 1)[0])
      this.updateOrder(data)
    },
    moveItemDown (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === data.length - 1) return
      data.splice(index + 1, 0, data.splice(index, 1)[0])
      this.updateOrder(data)
    },
    moveItemTop (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === 0) return
      data.unshift(data.splice(index, 1)[0])
      this.updateOrder(data)
    },
    moveItemBottom (record) {
      const data = this.items
      const index = data.findIndex(item => item.id === record.id)
      if (index === data.length - 1) return
      data.push(data.splice(index, 1)[0])
      this.updateOrder(data)
    },
    editTariffValue (record) {
      this.$emit('edit-tariff-action', true, record)
    },
    ipV6Address (text, record) {
      if (!record || !record.nic || record.nic.length === 0) {
        return ''
      }

      return record.nic.filter(e => { return e.ip6address }).map(e => { return e.ip6address }).join(', ') || text
    },
    generateCommentsPath (record) {
      return '/' + this.entityTypeToPath(record.entitytype) + '/' + record.entityid + '?tab=comments'
    },
    generateHumanReadableEntityType (record) {
      switch (record.entitytype) {
        case 'VM' : return 'Virtual Machine'
        case 'HOST' : return 'Host'
        case 'VOLUME' : return 'Volume'
        case 'SNAPSHOT' : return 'Snapshot'
        case 'VM_SNAPSHOT' : return 'VM Snapshot'
        case 'INSTANCE_GROUP' : return 'Instance Group'
        case 'NETWORK' : return 'Network'
        case 'VPC' : return 'VPC'
        case 'PUBLIC_IP_ADDRESS' : return 'Public IP Address'
        case 'VPN_CUSTOMER_GATEWAY' : return 'VPC Customer Gateway'
        case 'TEMPLATE' : return 'Template'
        case 'ISO' : return 'ISO'
        case 'SSH_KEYPAIR' : return 'SSH Key Pair'
        case 'DOMAIN' : return 'Domain'
        case 'SERVICE_OFFERING' : return 'Service Offfering'
        case 'DISK_OFFERING' : return 'Disk Offering'
        case 'NETWORK_OFFERING' : return 'Network Offering'
        case 'POD' : return 'Pod'
        case 'ZONE' : return 'Zone'
        case 'CLUSTER' : return 'Cluster'
        case 'PRIMARY_STORAGE' : return 'Primary Storage'
        case 'SECONDARY_STORAGE' : return 'Secondary Storage'
        case 'VR' : return 'Virtual Router'
        case 'SYSTEM_VM' : return 'System VM'
        case 'KUBERNETES_CLUSTER': return 'Kubernetes Cluster'
        default: return record.entitytype.toLowerCase().replace('_', '')
      }
    },
    entityTypeToPath (entitytype) {
      switch (entitytype) {
        case 'VM' : return 'vm'
        case 'HOST' : return 'host'
        case 'VOLUME' : return 'volume'
        case 'SNAPSHOT' : return 'snapshot'
        case 'VM_SNAPSHOT' : return 'vmsnapshot'
        case 'INSTANCE_GROUP' : return 'vmgroup'
        case 'NETWORK' : return 'guestnetwork'
        case 'VPC' : return 'vpc'
        case 'PUBLIC_IP_ADDRESS' : return 'publicip'
        case 'VPN_CUSTOMER_GATEWAY' : return 'vpncustomergateway'
        case 'TEMPLATE' : return 'template'
        case 'ISO' : return 'iso'
        case 'SSH_KEYPAIR' : return 'ssh'
        case 'DOMAIN' : return 'domain'
        case 'SERVICE_OFFERING' : return 'computeoffering'
        case 'DISK_OFFERING' : return 'diskoffering'
        case 'NETWORK_OFFERING' : return 'networkoffering'
        case 'POD' : return 'pod'
        case 'ZONE' : return 'zone'
        case 'CLUSTER' : return 'cluster'
        case 'PRIMARY_STORAGE' : return 'storagepool'
        case 'SECONDARY_STORAGE' : return 'imagestore'
        case 'VR' : return 'router'
        case 'SYSTEM_VM' : return 'systemvm'
        case 'KUBERNETES_CLUSTER': return 'kubernetes'
        default: return entitytype.toLowerCase().replace('_', '')
      }
    },
    updateAdminsOnly (e) {
      api('updateAnnotationVisibility', {
        id: e.target.value,
        adminsonly: e.target.checked
      }).finally(() => {
        const data = this.items
        const index = data.findIndex(item => item.id === e.target.value)
        const elem = data[index]
        elem.adminsonly = e.target.checked
      })
    },
    getHostState (host) {
      if (host && host.hypervisor === 'KVM' && host.state === 'Up' && host.details && host.details.secured !== 'true') {
        return 'Unsecure'
      }
      return host.state
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

/deep/ .ant-table-tbody>tr>td, .ant-table-thead>tr>th {
  overflow-wrap: anywhere;
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

  .alert-notification-threshold {
    background-color: rgba(255, 231, 175, 0.75);
    color: #e87900;
    padding: 10%;
  }

  .alert-disable-threshold {
    background-color: rgba(255, 190, 190, 0.75);
    color: #f50000;
    padding: 10%;
  }
</style>
