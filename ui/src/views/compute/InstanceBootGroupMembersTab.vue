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
    <a-button
      type="primary"
      style="width: 100%; margin-bottom: 10px"
      @click="showAddMemberModal"
      :loading="tabLoading"
      :disabled="!('addMemberToInstanceBootGroup' in $store.getters.apis)">
      <template #icon><plus-outlined /></template> {{ $t('label.add.member') }}
    </a-button>
    <a-table
      size="small"
      :columns="columns"
      :dataSource="members"
      :rowKey="item => item.id"
      :pagination="false"
      :loading="tabLoading"
      :rowExpandable="record => record.membertype === 'InstanceGroup'"
      childrenColumnName="__unused_disables_antd_auto_tree_rows">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'membertype'">
          <span>
            <desktop-outlined v-if="text === 'VirtualMachine'" />
            <gold-outlined v-else />
            {{ text === 'VirtualMachine' ? $t('label.virtual.machine') : $t('label.instance.group') }}
            <status v-if="record.memberstate" :text="record.memberstate" style="margin-left: 8px" />
          </span>
        </template>
        <template v-if="column.key === 'membername'">
          <router-link :to="{ path: memberLinkPath(record) }">{{ text }}</router-link>
        </template>
        <template v-if="column.key === 'readiness'">
          <a-tooltip :title="record.readinessmessage">
            <a-tag :color="readinessStatusColor(record.readinessstatus)">{{ record.readinessstatus || 'UNKNOWN' }}</a-tag>
          </a-tooltip>
          <tooltip-button
            v-if="record.readinessmode == 'RuleBased' && isReadinessFailing(record.readinessstatus)"
            :tooltip="$t('label.view.failing.readiness.rules')"
            size="small"
            icon="exclamation-circle-outlined"
            @click="openReadinessRulesModal(record.membertype, record.memberid, record.membername, true)" />
          <div style="font-size: 11px; color: rgba(0, 0, 0, 0.45)">{{ readinessModeLabel(record.readinessmode) }}</div>
        </template>
        <template v-if="column.key === 'actions'">
          <span style="margin-right: 5px">
            <tooltip-button
              :tooltip="$t('label.manage.readiness.rules')"
              icon="check-square-outlined"
              @click="openReadinessRulesModal(record.membertype, record.memberid, record.membername)" />
          </span>
          <span style="margin-right: 5px">
            <tooltip-button
              v-if="'updateInstanceBootGroupMember' in $store.getters.apis"
              :tooltip="$t('label.boot.order')"
              icon="vertical-align-middle-outlined"
              @click="openUpdateOrderModal(record)" />
          </span>
          <span style="margin-right: 5px">
            <a-popconfirm
              v-if="'removeInstanceBootGroupMember' in $store.getters.apis"
              placement="topRight"
              :title="$t('message.confirm.remove.member')"
              :ok-text="$t('label.yes')"
              :cancel-text="$t('label.no')"
              :loading="removeLoading"
              @confirm="removeMember(record)">
              <tooltip-button
                :tooltip="$t('label.remove')"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </span>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <a-table
          size="small"
          style="margin-top: 8px"
          :columns="vmColumns"
          :dataSource="record.children || []"
          :rowKey="item => item.id"
          :pagination="false">
          <template #bodyCell="{ column, text, record: vmRecord }">
            <template v-if="column.key === 'name'">
              <router-link :to="{ path: '/vm/' + vmRecord.id }">{{ text }}</router-link>
            </template>
            <template v-if="column.key === 'state'">
              <status :text="text" displayText />
            </template>
            <template v-if="column.key === 'readiness'">
              <a-tooltip :title="vmRecord.readinessmessage">
                <a-tag :color="readinessStatusColor(vmRecord.readinessstatus)">{{ vmRecord.readinessstatus || 'UNKNOWN' }}</a-tag>
              </a-tooltip>
              <tooltip-button
                v-if="vmRecord.readinessmode == 'RuleBased' && isReadinessFailing(vmRecord.readinessstatus)"
                :tooltip="$t('label.view.failing.readiness.rules')"
                size="small"
                icon="exclamation-circle-outlined"
                @click="openReadinessRulesModal('VirtualMachine', vmRecord.id, vmRecord.name, true)" />
              <div style="font-size: 11px; color: rgba(0, 0, 0, 0.45)">{{ readinessModeLabel(vmRecord.readinessmode) }}</div>
            </template>
            <template v-if="column.key === 'vmactions'">
              <tooltip-button
                :tooltip="$t('label.manage.readiness.rules')"
                icon="check-square-outlined"
                @click="openReadinessRulesModal('VirtualMachine', vmRecord.id, vmRecord.name)" />
            </template>
          </template>
        </a-table>
      </template>
    </a-table>

    <a-modal
      :visible="showAddMember"
      :title="$t('label.add.member')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      :width="600"
      @cancel="closeModals">
      <add-instance-boot-group-member
        :resource="resource"
        @close-action="closeModals" />
    </a-modal>

    <a-modal
      :visible="showUpdateOrder"
      :title="$t('label.boot.order')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      :width="500"
      @cancel="closeModals">
      <update-instance-boot-group-member-order
        :key="memberForUpdate.id"
        :resource="memberForUpdate"
        @close-action="closeModals" />
    </a-modal>

    <a-modal
      :visible="showReadinessRules"
      :title="$t('label.readiness.rules') + (readinessRulesItemName ? (' - ' + readinessRulesItemName) : '')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      :width="800"
      @cancel="closeModals">
      <instance-boot-group-readiness-rules-modal
        v-if="showReadinessRules"
        :bootGroupId="resource.id"
        :itemType="readinessRulesItemType"
        :itemId="readinessRulesItemId"
        :readOnly="readinessRulesForcedReadOnly || !('createInstanceBootGroupReadinessRule' in $store.getters.apis)" />
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import Status from '@/components/widgets/Status'
import AddInstanceBootGroupMember from '@/views/compute/AddInstanceBootGroupMember.vue'
import UpdateInstanceBootGroupMemberOrder from '@/views/compute/UpdateInstanceBootGroupMemberOrder.vue'
import InstanceBootGroupReadinessRulesModal from '@/views/compute/InstanceBootGroupReadinessRulesModal.vue'

export default {
  name: 'InstanceBootGroupMembersTab',
  components: {
    TooltipButton,
    Status,
    AddInstanceBootGroupMember,
    UpdateInstanceBootGroupMemberOrder,
    InstanceBootGroupReadinessRulesModal
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      removeLoading: false,
      members: [],
      showAddMember: false,
      showUpdateOrder: false,
      memberForUpdate: {},
      showReadinessRules: false,
      readinessRulesItemType: 'VirtualMachine',
      readinessRulesItemId: null,
      readinessRulesItemName: '',
      readinessRulesForcedReadOnly: false,
      columns: [
        { key: 'order', title: this.$t('label.boot.order'), dataIndex: 'order' },
        { key: 'membername', title: this.$t('label.name'), dataIndex: 'membername' },
        { key: 'membertype', title: this.$t('label.member.type'), dataIndex: 'membertype', width: 200 },
        { key: 'readiness', title: this.$t('label.readiness'), dataIndex: 'readinessstatus' },
        { key: 'actions', title: this.$t('label.actions') }
      ],
      vmColumns: [
        { key: 'name', title: this.$t('label.name'), dataIndex: 'name' },
        { key: 'state', title: this.$t('label.state'), dataIndex: 'state' },
        { key: 'readiness', title: this.$t('label.readiness'), dataIndex: 'readinessstatus' },
        { key: 'vmactions', title: this.$t('label.actions') }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchMembers()
    },
    fetchMembers () {
      this.members = []
      if (!this.resource.id) {
        return
      }
      this.tabLoading = true
      // readiness/children are opt-in: computing readiness is not free, and children (the VMs of an
      // InstanceGroup member) are only needed for the expandable rows, so listInstanceBootGroupMembers
      // only includes them when explicitly requested
      getAPI('listInstanceBootGroupMembers', { bootgroupid: this.resource.id, listall: true, details: 'readiness,children' }).then(json => {
        const members = json?.listinstancebootgroupmembersresponse?.instancebootgroupmember || []
        this.members = members.slice().sort((a, b) => a.order - b.order)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    readinessStatusColor (status) {
      switch (status) {
        case 'Ready': return 'success'
        case 'NotReady': return 'warning'
        case 'Error': return 'error'
        default: return 'default'
      }
    },
    isReadinessFailing (status) {
      return status === 'Error' || status === 'NotReady'
    },
    readinessModeLabel (mode) {
      switch (mode) {
        case 'None': return this.$t('label.readiness.mode.no.readiness')
        case 'ChildDependent': return this.$t('label.readiness.mode.child.dependent')
        case 'RuleBased': return this.$t('label.readiness.mode.rule.based')
        default: return ''
      }
    },
    memberLinkPath (record) {
      return record.membertype === 'VirtualMachine' ? ('/vm/' + record.memberid) : ('/vmgroup/' + record.memberid)
    },
    showAddMemberModal () {
      this.showAddMember = true
    },
    openUpdateOrderModal (record) {
      this.memberForUpdate = record
      this.showUpdateOrder = true
    },
    // forceReadOnly is set when opened from the failing-readiness shortcut in the readiness column —
    // that entry point is for checking why a rule is failing, not for managing rules, regardless of
    // whether the caller otherwise has permission to add/delete them.
    openReadinessRulesModal (itemType, itemId, itemName, forceReadOnly = false) {
      this.readinessRulesItemType = itemType
      this.readinessRulesItemId = itemId
      this.readinessRulesItemName = itemName
      this.readinessRulesForcedReadOnly = forceReadOnly
      this.showReadinessRules = true
    },
    closeModals () {
      this.showAddMember = false
      this.showUpdateOrder = false
      this.showReadinessRules = false
      this.memberForUpdate = {}
      this.readinessRulesForcedReadOnly = false
      this.fetchMembers()
    },
    removeMember (record) {
      this.removeLoading = true
      postAPI('removeInstanceBootGroupMember', { id: record.id }).then(() => {
        this.$notification.success({
          message: this.$t('label.remove'),
          description: this.$t('message.success.remove.member')
        })
        this.fetchMembers()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.removeLoading = false
      })
    }
  }
}
</script>
