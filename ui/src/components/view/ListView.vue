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
    :rowSelection=" enableGroupAction() || $route.name === 'event' ? {selectedRowKeys: selectedRowKeys, onChange: onSelectChange, columnWidth: 30} : null"
    :rowClassName="getRowClassName"
    style="overflow-y: auto"
  >
    <template #customFilterDropdown>
      <div style="padding: 8px" class="filter-dropdown">
        <a-menu>
          <a-menu-item v-for="(column, idx) in columnKeys" :key="idx" @click="updateSelectedColumns(column)">
            <a-checkbox :id="idx.toString()" :checked="selectedColumns.includes(getColumnKey(column))"/>
            {{ $t('label.' + String(getColumTitle(column)).toLowerCase()) }}
          </a-menu-item>
        </a-menu>
      </div>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.key === 'name'">
        <span v-if="['vm'].includes($route.path.split('/')[1])" style="margin-right: 5px">
          <span v-if="record.icon && record.icon.base64image">
            <resource-icon :image="record.icon.base64image" size="1x"/>
          </span>
          <os-logo v-else :osId="record.ostypeid" :osName="record.osdisplayname" size="lg" />
        </span>
        <span style="min-width: 120px" >
          <QuickView
            style="margin-left: 5px"
            :actions="actions"
            :resource="record"
            :enabled="quickViewEnabled() && actions.length > 0 && columns && columns[0].dataIndex === 'name' "
            @exec-action="$parent.execAction"/>
          <span v-if="$route.path.startsWith('/project')" style="margin-right: 5px">
            <tooltip-button type="dashed" size="small" icon="LoginOutlined" @onClick="changeProject(record)" />
          </span>
          <span v-if="$showIcon() && !['vm'].includes($route.path.split('/')[1])" style="margin-right: 5px">
            <resource-icon v-if="$showIcon() && record.icon && record.icon.base64image" :image="record.icon.base64image" size="1x"/>
            <os-logo v-else-if="record.ostypename" :osName="record.ostypename" size="1x" />
            <render-icon v-else-if="typeof $route.meta.icon ==='string'" style="font-size: 16px;" :icon="$route.meta.icon"/>
            <render-icon v-else style="font-size: 16px;" :svgIcon="$route.meta.icon" />
          </span>
          <span v-else :style="{ 'margin-right': record.ostypename ? '5px' : '0' }">
            <os-logo v-if="record.ostypename" :osName="record.ostypename" size="1x" />
          </span>

          <span v-if="record.hasannotations">
            <span v-if="record.id">
              <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
              <router-link :to="{ path: $route.path + '/' + record.id, query: { tab: 'comments' } }"><message-filled style="padding-left: 10px" size="small"/></router-link>
            </span>
            <router-link v-else :to="{ path: $route.path + '/' + record.name }" >{{ text }}</router-link>
          </span>
          <span v-else-if="$route.path.startsWith('/globalsetting')">{{ text }}</span>
          <span v-else-if="$route.path.startsWith('/alert')">
            <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ $t(text.toLowerCase()) }}</router-link>
            <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ $t(text.toLowerCase()) }}</router-link>
          </span>
          <span v-else-if="$route.path.startsWith('/tungstenfabric')">
            <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ $t(text.toLowerCase()) }}</router-link>
            <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ $t(text.toLowerCase()) }}</router-link>
          </span>
          <span v-else-if="isTungstenPath()">
            <router-link :to="{ path: $route.path + '/' + record.uuid, query: { zoneid: record.zoneid } }" v-if="record.uuid && record.zoneid">{{ $t(text.toLowerCase()) }}</router-link>
            <router-link :to="{ path: $route.path + '/' + record.uuid, query: { zoneid: $route.query.zoneid } }" v-else-if="record.uuid && $route.query.zoneid">{{ $t(text.toLowerCase()) }}</router-link>
            <router-link :to="{ path: $route.path }" v-else>{{ $t(text.toLowerCase()) }}</router-link>
          </span>
          <span v-else>
            <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ text }}</router-link>
            <router-link :to="{ path: $route.path + '/' + record.name }" v-else>{{ text }}</router-link>
          </span>
        </span>
      </template>
      <template v-if="column.key === 'templatetype'">
        <router-link :to="{ path: $route.path + '/' + record.templatetype }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'type'">
        <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(text)">{{ $t(text.toLowerCase()) }}</span>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'displayname'">
        <QuickView
          style="margin-left: 5px"
          :actions="actions"
          :resource="record"
          :enabled="quickViewEnabled() && actions.length > 0 && columns && columns[0].dataIndex === 'displayname' "
          @exec-action="$parent.execAction"/>
        <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'username'">
        <span v-if="$showIcon() && !['vm'].includes($route.path.split('/')[1])" style="margin-right: 5px">
          <resource-icon v-if="$showIcon() && record.icon && record.icon.base64image" :image="record.icon.base64image" size="1x"/>
          <user-outlined v-else style="font-size: 16px;" />
        </span>
        <router-link :to="{ path: $route.path + '/' + record.id }" v-if="['/accountuser', '/vpnuser'].includes($route.path)">{{ text }}</router-link>
        <router-link :to="{ path: '/accountuser', query: { username: record.username, domainid: record.domainid } }" v-else-if="$store.getters.userInfo.roletype !== 'User'">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'entityid'">
        <router-link :to="{ path: generateCommentsPath(record), query: { tab: 'comments' } }">{{ record.entityname }}</router-link>
      </template>
      <template v-if="column.key === 'entitytype'">
        {{ generateHumanReadableEntityType(record) }}
      </template>
      <template v-if="column.key === 'adminsonly' && ['Admin'].includes($store.getters.userInfo.roletype)">
        <a-checkbox :checked="record.adminsonly" :value="record.id" v-if="record.userid === $store.getters.userInfo.id" @change="e => updateAdminsOnly(e)" />
        <a-checkbox :checked="record.adminsonly" disabled v-else />
      </template>
      <template v-if="column.key === 'ipaddress'" href="javascript:;">
        <router-link v-if="['/publicip', '/privategw'].includes($route.path)" :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
        <span v-if="record.issourcenat">
          &nbsp;
          <a-tag>source-nat</a-tag>
        </span>
        <span v-if="record.isstaticnat">
          &nbsp;
          <a-tag>static-nat</a-tag>
        </span>
      </template>
      <template v-if="column.key === 'ip6address'" href="javascript:;">
        <span>{{ ipV6Address(text, record) }}</span>
      </template>
      <template v-if="column.key === 'publicip'">
        <router-link v-if="['/autoscalevmgroup'].includes($route.path)" :to="{ path: '/publicip' + '/' + record.publicipid }">{{ text }}</router-link>
        <router-link v-else :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'traffictype'">
        {{ text }}
      </template>
      <template v-if="column.key === 'vmname'">
        <router-link :to="{ path: createPathBasedOnVmType(record.vmtype, record.virtualmachineid) }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'virtualmachinename'">
        <router-link :to="{ path: '/vm/' + record.virtualmachineid }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'hypervisor'">
        <span v-if="$route.name === 'hypervisorcapability'">
        <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
        </span>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'state'">
        <status v-if="$route.path.startsWith('/host')" :text="getHostState(record)" displayText />
        <status v-else :text="text ? text : ''" displayText :styles="{ 'min-width': '80px' }" />
      </template>
      <template v-if="column.key === 'allocationstate'">
        <status :text="text ? text : ''" displayText />
      </template>
      <template v-if="column.key === 'resourcestate'">
        <status :text="text ? text : ''" displayText />
      </template>
      <template v-if="column.key === 'powerstate'">
        <status :text="text ? text : ''" displayText />
      </template>
      <template v-if="column.key === 'agentstate'">
        <status :text="text ? text : ''" displayText />
      </template>
      <template v-if="column.key === 'quotastate'">
        <status :text="text ? text : ''" displayText />
      </template>
      <template v-if="column.key === 'vlan'">
        <a href="javascript:;">
          <router-link v-if="$route.path === '/guestvlans'" :to="{ path: '/guestvlans/' + record.id }">{{ text }}</router-link>
        </a>
      </template>
      <template v-if="column.key === 'guestnetworkname'">
        <router-link :to="{ path: '/guestnetwork/' + record.guestnetworkid }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'associatednetworkname'">
        <router-link :to="{ path: '/guestnetwork/' + record.associatednetworkid }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'vpcname'">
        <router-link :to="{ path: '/vpc/' + record.vpcid }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'hostname'">
        <router-link v-if="record.hostid" :to="{ path: '/host/' + record.hostid }">{{ text }}</router-link>
        <router-link v-else-if="record.hostname" :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'storage'">
        <router-link v-if="record.storageid" :to="{ path: '/storagepool/' + record.storageid }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-for="(value, name) in thresholdMapping" :key="name">
        <template v-if="column.key === name">
          <span>
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
      </template>

      <template v-if="column.key === 'level'">
        <router-link :to="{ path: '/event/' + record.id }">{{ text }}</router-link>
      </template>

      <template v-if="column.key === 'clustername'">
        <router-link :to="{ path: '/cluster/' + record.clusterid }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'podname'">
        <router-link :to="{ path: '/pod/' + record.podid }">{{ text }}</router-link>
      </template>
      <template v-if="column.key === 'account'">
        <template v-if="record.owner">
          <template v-for="(item, idx) in record.owner" :key="idx">
            <span style="margin-right:5px">
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
            v-if="'quota' in record && $router.resolve(`${$route.path}/${record.account}`).matched[0].redirect !== '/exception/404'"
            :to="{ path: `${$route.path}/${record.account}`, query: { account: record.account, domainid: record.domainid, quota: true } }">{{ text }}</router-link>
          <router-link :to="{ path: '/account/' + record.accountid }" v-else-if="record.accountid">{{ text }}</router-link>
          <router-link :to="{ path: '/account', query: { name: record.account, domainid: record.domainid, dataView: true } }" v-else-if="$store.getters.userInfo.roletype !== 'User'">{{ text }}</router-link>
          <span v-else>{{ text }}</span>
        </template>
      </template>
      <template v-if="column.key === 'resource'">
        <resource-label :resourceType="record.resourcetype" :resourceId="record.resourceid" :resourceName="record.resourcename" />
      </template>
      <template v-if="column.key === 'domain'">
        <router-link v-if="record.domainid && !record.domainid.toString().includes(',') && $store.getters.userInfo.roletype !== 'User'" :to="{ path: '/domain/' + record.domainid, query: { tab: 'details' } }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'domainpath'">
        <router-link v-if="record.domainid && !record.domainid.includes(',') && $router.resolve('/domain/' + record.domainid).matched[0].redirect !== '/exception/404'" :to="{ path: '/domain/' + record.domainid, query: { tab: 'details' } }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'zone'">
        <router-link v-if="record.zoneid && !record.zoneid.includes(',') && $router.resolve('/zone/' + record.zoneid).matched[0].redirect !== '/exception/404'" :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'zonename'">
        <router-link v-if="$router.resolve('/zone/' + record.zoneid).matched[0].redirect !== '/exception/404'" :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'rolename'">
        <router-link v-if="record.roleid && $router.resolve('/role/' + record.roleid).matched[0].redirect !== '/exception/404'" :to="{ path: '/role/' + record.roleid }">{{ text }}</router-link>
        <span v-else>{{ text }}</span>
      </template>
      <template v-if="column.key === 'templateversion'">
        <span>  {{ record.version }} </span>
      </template>
      <template v-if="column.key === 'softwareversion'">
        <span>  {{ record.softwareversion ? record.softwareversion : 'N/A' }} </span>
      </template>
      <template v-if="column.key === 'access'">
        <status :text="record.readonly ? 'ReadOnly' : 'ReadWrite'" displayText />
      </template>
      <template v-if="column.key === 'requiresupgrade'">
        <status :text="record.requiresupgrade ? 'warning' : ''" />
        {{ record.requiresupgrade ? 'Yes' : 'No' }}
      </template>
      <template v-if="column.key === 'loadbalancerrule'">
        <span>  {{ record.loadbalancerrule }} </span>
      </template>
      <template v-if="column.key === 'autoscalingenabled'">
        <status :text="record.autoscalingenabled ? 'Enabled' : 'Disabled'" />
        {{ record.autoscalingenabled ? 'Enabled' : 'Disabled' }}
      </template>
      <template v-if="column.key === 'current'">
        <status :text="record.current ? record.current.toString() : 'false'" />
      </template>
      <template v-if="column.key === 'created'">
        {{ $toLocaleDate(text) }}
      </template>
      <template v-if="column.key === 'sent'">
        {{ $toLocaleDate(text) }}
      </template>
      <template v-if="column.key === 'order'">
        <div class="shift-btns">
          <a-tooltip :name="text" placement="top">
            <template #title>{{ $t('label.move.to.top') }}</template>
            <a-button
              shape="round"
              @click="moveItemTop(record)"
              class="shift-btn">
              <DoubleLeftOutlined class="shift-btn shift-btn--rotated" />
            </a-button>
          </a-tooltip>
          <a-tooltip placement="top">
            <template #title>{{ $t('label.move.to.bottom') }}</template>
            <a-button
              shape="round"
              @click="moveItemBottom(record)"
              class="shift-btn">
              <DoubleRightOutlined class="shift-btn shift-btn--rotated" />
            </a-button>
          </a-tooltip>
          <a-tooltip placement="top">
            <template #title>{{ $t('label.move.up.row') }}</template>
            <a-button shape="round" @click="moveItemUp(record)" class="shift-btn">
              <CaretUpOutlined class="shift-btn" />
            </a-button>
          </a-tooltip>
          <a-tooltip placement="top">
            <template #title>{{ $t('label.move.down.row') }}</template>
            <a-button shape="round" @click="moveItemDown(record)" class="shift-btn">
              <CaretDownOutlined class="shift-btn" />
            </a-button>
          </a-tooltip>
        </div>
      </template>

      <template v-if="column.key === 'value'">
        <a-input
          v-if="editableValueKey === record.key"
          v-focus="true"
          :defaultValue="record.value"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-model:value="editableValue"
          @keydown.esc="editableValueKey = null"
          @pressEnter="saveValue(record)">
        </a-input>
        <div v-else style="width: 200px; word-break: break-all">
          {{ text }}
        </div>
      </template>
      <template v-if="column.key === 'actions'">
        <tooltip-button
          :tooltip="$t('label.edit')"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-if="editableValueKey !== record.key"
          icon="edit-outlined"
          @onClick="editValue(record)" />
        <tooltip-button
          :tooltip="$t('label.cancel')"
          @onClick="editableValueKey = null"
          v-if="editableValueKey === record.key"
          iconType="CloseCircleTwoTone"
          iconTwoToneColor="#f5222d" />
        <tooltip-button
          :tooltip="$t('label.ok')"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          @onClick="saveValue(record)"
          v-if="editableValueKey === record.key"
          iconType="CheckCircleTwoTone"
          iconTwoToneColor="#52c41a" />
        <tooltip-button
          :tooltip="$t('label.reset.config.value')"
          @onClick="resetConfig(record)"
          v-if="editableValueKey !== record.key"
          icon="reload-outlined"
          :disabled="!('updateConfiguration' in $store.getters.apis)" />
      </template>
      <template v-if="column.key === 'tariffActions'">
        <tooltip-button
          :tooltip="$t('label.edit')"
          v-if="editableValueKey !== record.key"
          :disabled="!('quotaTariffUpdate' in $store.getters.apis)"
          icon="edit-outlined"
          @onClick="editTariffValue(record)" />
        <slot></slot>
      </template>
    </template>
    <template #footer>
      <span v-if="hasSelected">
        {{ `Selected ${selectedRowKeys.length} items` }}
      </span>
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import QuickView from '@/components/view/QuickView'
import TooltipButton from '@/components/widgets/TooltipButton'
import ResourceIcon from '@/components/view/ResourceIcon'
import ResourceLabel from '@/components/widgets/ResourceLabel'
import { createPathBasedOnVmType } from '@/utils/plugins'

export default {
  name: 'ListView',
  components: {
    OsLogo,
    Status,
    QuickView,
    TooltipButton,
    ResourceIcon,
    ResourceLabel
  },
  props: {
    columns: {
      type: Array,
      required: true
    },
    columnKeys: {
      type: Array,
      default: () => []
    },
    selectedColumns: {
      type: Array,
      default: () => []
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
    isTungstenPath () {
      return ['/tungstennetworkroutertable', '/tungstenpolicy', '/tungsteninterfaceroutertable',
        '/tungstenpolicyset', '/tungstenroutingpolicy', '/firewallrule', '/tungstenfirewallpolicy'].includes(this.$route.path)
    },
    createPathBasedOnVmType: createPathBasedOnVmType,
    quickViewEnabled () {
      return new RegExp(['/vm', '/kubernetes', '/ssh', '/userdata', '/vmgroup', '/affinitygroup', '/autoscalevmgroup',
        '/volume', '/snapshot', '/vmsnapshot', '/backup',
        '/guestnetwork', '/vpc', '/vpncustomergateway',
        '/template', '/iso',
        '/project', '/account',
        '/zone', '/pod', '/cluster', '/host', '/storagepool', '/imagestore', '/systemvm', '/router', '/ilbvm', '/annotation',
        '/computeoffering', '/systemoffering', '/diskoffering', '/backupoffering', '/networkoffering', '/vpcoffering',
        '/tungstenfabric'].join('|'))
        .test(this.$route.path)
    },
    enableGroupAction () {
      return ['vm', 'alert', 'vmgroup', 'ssh', 'userdata', 'affinitygroup', 'autoscalevmgroup', 'volume', 'snapshot',
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
      if (this.entityTypeToPath(record.entitytype) === 'ssh') {
        return '/' + this.entityTypeToPath(record.entitytype) + '/' + record.entityname
      }
      return '/' + this.entityTypeToPath(record.entitytype) + '/' + record.entityid
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
        case 'AUTOSCALE_VM_GROUP': return 'AutoScale VM group'
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
        case 'AUTOSCALE_VM_GROUP': return 'autoscalevmgroup'
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
    },
    getColumnKey (name) {
      if (typeof name === 'object') {
        name = Object.keys(name).includes('field') ? name.field : name.customTitle
      }
      return name
    },
    getColumTitle (name) {
      if (typeof name === 'object') {
        name = Object.keys(name).includes('customTitle') ? name.customTitle : name.field
      }
      return name
    },
    updateSelectedColumns (name) {
      this.$emit('update-selected-columns', name)
    }
  }
}
</script>

<style>
:deep(.ant-table-thead) {
  background-color: #f9f9f9;
}

:deep(.ant-table-small) > .ant-table-content > .ant-table-body {
  margin: 0;
}

:deep(.ant-table-tbody)>tr>td, :deep(.ant-table-thead)>tr>th {
  overflow-wrap: anywhere;
}

.filter-dropdown .ant-menu-vertical {
  border: none;
}

.filter-dropdown .ant-menu:not(.ant-menu-horizontal) .ant-menu-item-selected {
  background-color: transparent;
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
