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
    <a-table
      size="small"
      :columns="columns"
      :dataSource="rules"
      :rowKey="item => item.id"
      :pagination="false"
      :loading="tabLoading"
      :scroll="{ x: readOnly ? 700 : 950 }">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'ruletype'">
          {{ $t('label.' + text.toLowerCase()) }}
          <a-tag v-if="record.inherited" color="blue">{{ $t('label.inherited') }}</a-tag>
        </template>
        <template v-if="column.key === 'name'">
          <a-input v-if="editingRuleId === record.id" v-model:value="editForm.name" size="small" />
          <span v-else>{{ text }}</span>
        </template>
        <template v-if="column.key === 'details'">
          <template v-if="detailEntries(record).length">
            <a-tag v-for="entry in detailEntries(record)" :key="entry.key">{{ entry.key }}: {{ entry.value }}</a-tag>
          </template>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(text)">{{ text || 'UNKNOWN' }}</a-tag>
        </template>
        <template v-if="column.key === 'enabled'">
          <a-switch v-if="editingRuleId === record.id" v-model:checked="editForm.enabled" size="small" />
          <status v-else :text="text ? 'true' : 'false'" displayText />
        </template>
        <template v-if="column.key === 'statusmessage'">
          <a-tooltip :title="text">
            <div class="readiness-rule-message">{{ text }}</div>
          </a-tooltip>
          <div v-if="record.checkedon" class="readiness-rule-checkedon">{{ $t('label.last.checked') }}: {{ $toLocaleDate(record.checkedon) }}</div>
        </template>
        <template v-if="column.key === 'actions'">
          <template v-if="editingRuleId === record.id">
            <tooltip-button
              :tooltip="$t('label.save')"
              type="primary"
              icon="check-outlined"
              style="margin-right: 5px"
              :loading="editSubmitLoading"
              @click="saveEditRule(record)" />
            <tooltip-button
              :tooltip="$t('label.cancel')"
              icon="close-outlined"
              @click="cancelEditRule" />
          </template>
          <template v-else-if="record.inherited">
            <tooltip-button
              :tooltip="$t('message.inherited.readiness.rule')"
              :disabled="true"
              icon="delete-outlined" />
          </template>
          <template v-else>
            <tooltip-button
              :tooltip="$t('label.edit.readiness.rule')"
              icon="edit-outlined"
              style="margin-right: 5px"
              :disabled="editingRuleId !== null"
              @click="startEditRule(record)" />
            <a-popconfirm
              placement="topRight"
              :title="$t('message.confirm.delete.readiness.rule')"
              :ok-text="$t('label.yes')"
              :cancel-text="$t('label.no')"
              @confirm="deleteRule(record)">
              <tooltip-button
                :tooltip="$t('label.delete')"
                type="primary"
                :danger="true"
                :disabled="editingRuleId !== null"
                icon="delete-outlined" />
            </a-popconfirm>
          </template>
        </template>
      </template>
    </a-table>

    <template v-if="!readOnly">
      <a-divider />

      <a-form
        v-if="availableRuleTypes.length"
        :ref="formRef"
        :model="form"
        :rules="formRules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="ruletype" ref="ruletype">
          <template #label>
            <tooltip-label :title="$t('label.rule.type')" :tooltip="apiParams.ruletype.description"/>
          </template>
          <span v-if="isGuestLivenessSelected">
            <a-alert type="warning">
              <template #message>
                <span v-html="$t('message.readiness.guest.liveness.warning')" />
              </template>
            </a-alert>
            <br/>
          </span>
          <a-select v-model:value="form.ruletype" @change="onRuleTypeChange">
            <a-select-option v-for="type in availableRuleTypes" :key="type" :value="type">{{ $t('label.' + type.toLowerCase()) }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input v-model:value="form.name" :placeholder="apiParams.name.description" />
        </a-form-item>
        <template v-if="form.ruletype === 'PortCheck'">
          <a-form-item name="port" ref="port" :label="$t('label.port')">
            <a-input-number v-model:value="form.port" style="width: 100%" :min="1" :max="65535" />
          </a-form-item>
          <a-form-item name="protocol" ref="protocol" :label="$t('label.protocol')">
            <a-select v-model:value="form.protocol">
              <a-select-option value="tcp">tcp</a-select-option>
            </a-select>
          </a-form-item>
        </template>
        <template v-if="form.ruletype === 'MemberQuorum'">
          <a-form-item name="thresholdtype" ref="thresholdtype" :label="$t('label.threshold.type')">
            <a-select v-model:value="form.thresholdtype">
              <a-select-option value="COUNT">COUNT</a-select-option>
              <a-select-option value="PERCENTAGE">PERCENTAGE</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="thresholdvalue" ref="thresholdvalue" :label="$t('label.threshold.value')">
            <a-input-number v-model:value="form.thresholdvalue" style="width: 100%" :min="0" />
          </a-form-item>
        </template>
        <div :span="24" class="action-button">
          <a-button :loading="submitLoading" type="primary" @click="handleSubmit">{{ $t('label.add.readiness.rule') }}</a-button>
        </div>
      </a-form>
      <a-alert v-else type="info" :message="$t('message.no.more.readiness.rule.types')" show-icon />
    </template>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import Status from '@/components/widgets/Status'

export default {
  name: 'InstanceBootGroupReadinessRulesModal',
  components: {
    TooltipButton,
    TooltipLabel,
    Status
  },
  props: {
    bootGroupId: {
      type: String,
      required: true
    },
    itemType: {
      type: String,
      required: true
    },
    itemId: {
      type: String,
      required: true
    },
    /** Drops the delete action and add-rule form, leaving just the rule/status table. */
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      tabLoading: false,
      submitLoading: false,
      rules: [],
      editingRuleId: null,
      editForm: { name: '', enabled: true },
      editSubmitLoading: false
    }
  },
  computed: {
    columns () {
      const columns = [
        { key: 'ruletype', title: this.$t('label.rule.type'), dataIndex: 'ruletype', width: 140 },
        { key: 'name', title: this.$t('label.name'), dataIndex: 'name', width: 120 },
        { key: 'details', title: this.$t('label.details'), dataIndex: 'details', width: 180 },
        { key: 'enabled', title: this.$t('label.enabled'), dataIndex: 'enabled', width: 90 },
        { key: 'status', title: this.$t('label.status'), dataIndex: 'readinessstatus', width: 100 },
        { key: 'statusmessage', title: this.$t('label.message'), dataIndex: 'statusmessage', width: 220 }
      ]
      if (!this.readOnly) {
        columns.push({ key: 'actions', title: this.$t('label.actions'), width: 130 })
      }
      return columns
    },
    /**
     * Ping/GuestAgentLiveness are singleton regardless of item type; MemberQuorum is singleton but
     * only applies to an InstanceGroup. PortCheck allows multiple (different ports).
     */
    singletonRuleTypes () {
      return this.itemType === 'VirtualMachine' ? ['Ping', 'GuestAgentLiveness'] : ['Ping', 'GuestAgentLiveness', 'MemberQuorum']
    },
    availableRuleTypes () {
      const allRuleTypes = this.itemType === 'VirtualMachine'
        ? ['Ping', 'GuestAgentLiveness', 'PortCheck']
        : ['Ping', 'GuestAgentLiveness', 'PortCheck', 'MemberQuorum']
      const existingRuleTypes = this.rules.map(rule => rule.ruletype)
      return allRuleTypes.filter(type => !this.singletonRuleTypes.includes(type) || !existingRuleTypes.includes(type))
    },
    isGuestLivenessSelected () {
      return this.form.ruletype === 'GuestAgentLiveness'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createInstanceBootGroupReadinessRule')
  },
  created () {
    this.initForm()
    this.fetchRules()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        ruletype: this.availableRuleTypes[0],
        name: undefined,
        port: 80,
        protocol: 'tcp',
        thresholdtype: 'PERCENTAGE',
        thresholdvalue: 100
      })
      this.formRules = reactive({
        ruletype: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    /** No-op: the conditional fields are driven directly off form.ruletype. */
    onRuleTypeChange () {
    },
    fetchRules () {
      this.tabLoading = true
      const params = { bootgroupid: this.bootGroupId }
      if (this.itemType === 'VirtualMachine') {
        params.virtualmachineid = this.itemId
      } else {
        params.instancegroupid = this.itemId
      }
      getAPI('listInstanceBootGroupReadinessRules', params).then(json => {
        this.rules = json?.listinstancebootgroupreadinessrulesresponse?.instancebootgroupreadinessrule || []
        if (!this.availableRuleTypes.includes(this.form.ruletype)) {
          this.form.ruletype = this.availableRuleTypes[0]
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.tabLoading = false
      })
    },
    /**
     * Collapses MemberQuorum's threshold pair and PortCheck's port/protocol into a single tag each;
     * anything else falls back to one tag per raw detail key.
     */
    detailEntries (record) {
      const details = record.details || {}
      if (record.ruletype === 'MemberQuorum' && details.threshold_type) {
        return [{ key: details.threshold_type.toUpperCase(), value: details.threshold_value }]
      }
      if (record.ruletype === 'PortCheck' && details.port) {
        return [{ key: 'PORT', value: `${details.port}/${(details.protocol || 'tcp').toUpperCase()}` }]
      }
      return Object.keys(details).map(key => ({ key: key.toUpperCase(), value: details[key] }))
    },
    statusColor (status) {
      switch (status) {
        case 'Ready': return 'success'
        case 'NotReady': return 'warning'
        case 'Error': return 'error'
        default: return 'default'
      }
    },
    handleSubmit (e) {
      if (e && e.preventDefault) e.preventDefault()
      if (this.submitLoading) return
      this.formRef.value.validate().then(() => {
        this.submitLoading = true
        const params = {
          bootgroupid: this.bootGroupId,
          ruletype: this.form.ruletype
        }
        if (this.itemType === 'VirtualMachine') {
          params.virtualmachineid = this.itemId
        } else {
          params.instancegroupid = this.itemId
        }
        if (this.form.name) {
          params.name = this.form.name
        }
        const details = {}
        if (this.form.ruletype === 'PortCheck') {
          details.port = this.form.port
          details.protocol = this.form.protocol
        } else if (this.form.ruletype === 'MemberQuorum') {
          details.threshold_type = this.form.thresholdtype
          details.threshold_value = this.form.thresholdvalue
        }
        Object.keys(details).forEach((key) => {
          params[`details[0].${key}`] = String(details[key])
        })
        postAPI('createInstanceBootGroupReadinessRule', params).then(() => {
          this.$notification.success({
            message: this.$t('label.add.readiness.rule'),
            description: this.$t('message.success.add.readiness.rule')
          })
          this.fetchRules()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.submitLoading = false
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    deleteRule (record) {
      postAPI('deleteInstanceBootGroupReadinessRule', { id: record.id }).then(() => {
        this.$notification.success({
          message: this.$t('label.delete'),
          description: this.$t('message.success.delete.readiness.rule')
        })
        this.fetchRules()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    /**
     * Only name/enabled are editable — rule type and details are immutable after creation and are
     * instead changed by removing and re-adding the rule. Edited in place in the table row.
     */
    startEditRule (record) {
      this.editingRuleId = record.id
      this.editForm.name = record.name
      this.editForm.enabled = record.enabled
    },
    cancelEditRule () {
      this.editingRuleId = null
    },
    saveEditRule (record) {
      if (this.editSubmitLoading) return
      this.editSubmitLoading = true
      postAPI('updateInstanceBootGroupReadinessRule', {
        id: record.id,
        name: this.editForm.name,
        enabled: this.editForm.enabled
      }).then(() => {
        this.$notification.success({
          message: this.$t('label.edit.readiness.rule'),
          description: this.$t('message.success.edit.readiness.rule')
        })
        this.editingRuleId = null
        this.fetchRules()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.editSubmitLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .readiness-rule-message {
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .readiness-rule-checkedon {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    white-space: nowrap;
  }
</style>
